---
title: SSH and firewall hardening
summary: Disable password SSH, draft the iptables baseline you'll layer the cluster on top of, and lock the K3s API to the WireGuard interface only. The boring controls that block 99% of casual attacks.
---

## What "hardening" means at this stage

Three concrete things, in order:

1. **SSH should accept keys, never passwords.** Both at install time (already done in the previous chapter) and at runtime, defended by an explicit sshd dropin.
2. **The default-deny posture on each host's INPUT chain.** Allow established connections, allow loopback, allow SSH from anywhere, and the K3s API only from WireGuard — drop everything else.
3. **The cloud edge gets one extra lockdown.** The public NIC explicitly drops Kubernetes-internal ports (kubelet 10250, VXLAN 4789, NodePorts) regardless of any other rule that might forget to.

The cluster behind these docs runs all three. None of them are exotic. They're systemd units that survive reboots and run early in the boot sequence — the `prepare-host.sh` script from the previous chapter installs them.

```d2
direction: down

internet: Public internet (only sees vm-1)

vm1: vm-1 (cloud edge) {
  shape: rectangle
  eth0: eth0 (public)
  wg0: wg0 (mesh peer)
  nft: nftables\nallowlist
  drops: iptables drops\n10250, 4789, 30000-32767
  sshd: sshd
  traefik: Traefik\n:80, :443
}

home: home boxes (ms-1, wk-1, wk-2) {
  shape: rectangle
  wg0home: wg0 (mesh)
  ipt: iptables\nINPUT chain
  api: K3s API :6443\n(ms-1 only)
}

internet -> vm1.eth0: SSH :22, HTTPS :443, HTTP :80, WG :51820/udp
vm1.eth0 -> vm1.nft: passes
vm1.eth0 -> vm1.drops: passes
vm1.nft -> vm1.sshd: :22 allow
vm1.nft -> vm1.traefik: :80 :443 allow
vm1.wg0 -> home.wg0home: encrypted
home.wg0home -> home.ipt: passes
home.ipt -> home.api: :6443 from 172.27.15.0/24 only
```

## The sshd dropin

`prepare-host.sh` installs `/etc/ssh/sshd_config.d/99-root-login.conf` on the home boxes:

```ini
PermitRootLogin prohibit-password
PasswordAuthentication no
KbdInteractiveAuthentication no
PubkeyAuthentication yes
```

Four lines. They mean:

- **`PermitRootLogin prohibit-password`** — root can log in, but only with a key, never with a password.
- **`PasswordAuthentication no`** — no user account can authenticate with a password.
- **`KbdInteractiveAuthentication no`** — no PAM password fallback either (this is the one OpenSSH defaults that catches people).
- **`PubkeyAuthentication yes`** — key-based auth is the only path in.

Apply with `systemctl reload ssh`. Confirm with `sshd -T | grep -E '^(permitrootlogin|passwordauthentication|kbdinteractive)'` — every line should be `no` or `prohibit-password`.

The cloud edge has slightly different defaults (`60-cloudimg-settings.conf`) shipped by the cloud-init image, with `PasswordAuthentication no` already on. We append the same hardening.

**Footgun:** never run `sshd -t` to validate, then `systemctl restart ssh` — restart kicks live sessions. Use `systemctl reload ssh` so existing connections survive while new connections pick up the new config.

## The home box host firewall

For `ms-1` the script installs `/usr/local/sbin/homelab-fw-ms1.sh` and a systemd unit that runs it at boot:

```bash
# Allow established/related, loopback, SSH
iptables -I INPUT 1 -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
iptables -I INPUT 2 -i lo -j ACCEPT
iptables -I INPUT 3 -p tcp --dport 22 -j ACCEPT

# Allow the K3s API only from inside the mesh, drop it elsewhere
iptables -I INPUT 4 -i wg0 -s 172.27.15.0/24 -p tcp --dport 6443 -j ACCEPT
iptables -A INPUT       -p tcp --dport 6443 -j DROP
```

Why each rule:

- **`ESTABLISHED,RELATED`** — return traffic for connections we initiated. Without this, nothing works.
- **`-i lo`** — loopback. Many local services bind to `127.0.0.1`; this lets them talk to themselves.
- **`--dport 22`** — SSH from anywhere. Could be tightened to "only from the mesh", but in practice you want to be able to SSH from your laptop on the LAN before the mesh is up; locking SSH to wg0 on a fresh install is how you brick a node.
- **`-i wg0 ... --dport 6443 -j ACCEPT`** then **`-A INPUT --dport 6443 -j DROP`** — the K3s API is reachable on `ms-1:6443`, but only via the WireGuard interface. From the LAN or the public internet, the port is closed. This is the most security-relevant rule on the cluster.

Workers (`wk-1`, `wk-2`) don't need this — the K3s API isn't on them. They keep the default Ubuntu permissive INPUT chain and rely on the home router blocking everything inbound.

`iptables-persistent` (installed in the previous chapter) saves these rules to `/etc/iptables/rules.v4` so they survive reboots. The systemd unit re-applies them anyway, belt-and-braces.

## The cloud edge — defence in depth

`vm-1` gets two layers:

### Layer 1: nftables allowlist (covered later)

The strict allowlist on `eth0` is `edge-guardrail.sh`, installed when we deploy Traefik in [Router and edge firewall](/cortex/homelab-from-scratch/the-edge/router-and-edge-firewall). It's the primary defence: only `:22, :80, :443, :51820/udp` are accepted on the public NIC. Everything else is dropped.

### Layer 2: belt-and-braces iptables drops

`prepare-host.sh` installs `/usr/local/sbin/homelab-fw-edge.sh` as a fallback:

```bash
# IPv4: block kubelet, VXLAN, NodePorts on PUBLIC iface only
iptables -I INPUT 1 -i eth0 -p tcp --dport 10250 -j DROP
iptables -I INPUT 1 -i eth0 -p udp --dport 4789  -j DROP
iptables -I INPUT 1 -i eth0 -p tcp --dport 30000:32767 -j DROP
iptables -I INPUT 1 -i eth0 -p udp --dport 30000:32767 -j DROP

# IPv6: same idea
ip6tables -I INPUT 1 -i eth0 -p tcp --dport 10250 -j DROP
ip6tables -I INPUT 1 -i eth0 -p udp --dport 4789  -j DROP
ip6tables -I INPUT 1 -i eth0 -p tcp --dport 30000:32767 -j DROP
ip6tables -I INPUT 1 -i eth0 -p udp --dport 30000:32767 -j DROP
```

Why duplicate work? Because **defence in depth means assuming each layer will eventually be misconfigured**. If someone (you, me, six months from now, half-asleep) accidentally flushes the nftables allowlist, the iptables drops still hold. Two layers means twice as many independent things have to fail before kubelet (which on the edge runs as root, with a JSON RPC API) is reachable from the internet.

`10250` is kubelet — anyone who reaches it can `exec` into containers. `4789` is VXLAN — leaving it open from the internet means anyone can inject pod traffic into your cluster. `30000-32767` is the NodePort range; we don't use NodePorts, but leaving them blocked costs nothing.

## What about fail2ban?

`prepare-host.sh` installs the package but doesn't enable any jails. Two reasons:

1. **SSH brute-force is already neutered.** No password auth means the only attacks worth blocking are ones that successfully steal a key — fail2ban doesn't help against those.
2. **fail2ban can lock you out of your own homelab.** Misconfigured patterns happily ban your laptop's IP after one curl request.

You can configure it later if you want — the standard `sshd` jail is harmless. But it's not load-bearing here.

## Verify

```bash
# 1. SSH config is locked down
sshd -T | grep -E '^(permitrootlogin|passwordauthentication|pubkeyauthentication|kbdinteractive)'
# permitrootlogin prohibit-password
# passwordauthentication no
# pubkeyauthentication yes
# kbdinteractiveauthentication no

# 2. INPUT chain on ms-1 has the right rules
iptables -L INPUT -n -v --line-numbers
# (look for lines accepting wg0 + 6443, then a final DROP for 6443)

# 3. K3s API closed from the LAN (run from your laptop)
nc -zv 192.168.15.2 6443 -w 3
# nc: connect to 192.168.15.2 port 6443 (tcp) timed out: Operation now in progress
```

The third test is the meaningful one. From the LAN, the API server should be unreachable. From inside the mesh (after we set it up), it'll be reachable on `172.27.15.12:6443`. That's the entire security model in two `nc` outputs.

→ Next: [Why a private mesh?](/cortex/homelab-from-scratch/private-mesh/why-a-private-mesh)
