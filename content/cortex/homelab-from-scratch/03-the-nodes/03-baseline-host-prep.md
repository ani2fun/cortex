---
title: Baseline host prep
summary: The kernel-level prep every Kubernetes installer assumes you've already done — packages, modules, sysctl, swap-off, time sync, hostname mapping. The boring layer that breaks everything when it isn't right.
---

## What "baseline" means

Kubernetes makes assumptions about the kernel it's running on. So does Calico. So does WireGuard. None of these assumptions are *documented in one place* — they're scattered across installer scripts, release notes, and bug reports. This chapter pulls them together.

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#64748b"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
flowchart LR
  Pkg[Packages]
  Mod[Kernel modules]
  Sys[sysctl knobs]
  Swap[Swap off]
  NTP[Time sync]
  Host[/etc/hostname<br/>and /etc/hosts]
  Done((Ready for<br/>WireGuard))

  Pkg --> Mod --> Sys --> Swap --> NTP --> Host --> Done
```

You'll do these steps once per node. The order matters: packages first because some of them install kernel modules; modules before sysctl so the WireGuard / VXLAN sysctls have an interface to bind to; swap off before installing K3s so the kubelet doesn't refuse to start; time sync before TLS so cert validation works.

The infra repo ships a script — `prepare-host.sh` — that does all of this in 200 lines. We'll walk through what it does in pieces so you understand what's happening, but you can also just run it and trust the output.

## 1. Packages

```bash
apt-get update
apt-get install -y \
  wireguard wireguard-tools \
  iptables iptables-persistent nftables conntrack ebtables ipset \
  curl jq socat ca-certificates gnupg dnsutils htop tcpdump \
  fail2ban unattended-upgrades
```

Three groups:

- **Networking**: `wireguard` for the mesh, `iptables` and `nftables` because Kubernetes will install rules into both, `conntrack` and `ipset` because Calico uses them.
- **Toolbox**: `curl`, `jq`, `socat`, `dnsutils` (`dig`, `nslookup`), `htop`, `tcpdump` — the things you'll reach for at debug time.
- **Hygiene**: `fail2ban` to slow down brute-force SSH, `unattended-upgrades` to apply security patches automatically.

`socat` deserves a mention: K3s uses it for `kubectl port-forward` and `kubectl exec`. Forgetting it leaves a confusing "Error: unable to upgrade connection" message.

## 2. Kernel modules

K3s + Calico need three modules loaded at boot:

```bash
cat > /etc/modules-load.d/k3s-calico.conf <<'EOF'
br_netfilter
vxlan
overlay
EOF

# Load them now too, so we don't need to reboot
modprobe br_netfilter vxlan overlay
```

What each does:

- **`br_netfilter`** — lets iptables rules apply to traffic on Linux bridges. Kubernetes service VIPs route through bridges; without this module, NetworkPolicy and kube-proxy rules silently miss traffic.
- **`vxlan`** — the encapsulation Calico uses on this cluster. Without it, Calico falls back to BIRD/BGP, which is great in datacentres and bad in homelabs.
- **`overlay`** — the OverlayFS filesystem driver containerd uses for image layers. Should be loaded by default, but explicit beats hopeful.

## 3. sysctl

Two files, in this order:

```bash
# /etc/sysctl.d/99-k3s-calico.conf
net.ipv4.ip_forward=1
net.ipv6.conf.all.forwarding=1
net.bridge.bridge-nf-call-iptables=1
net.bridge.bridge-nf-call-ip6tables=1
```

```bash
# /etc/sysctl.d/99-wireguard.conf
net.ipv4.conf.all.rp_filter=2
net.ipv4.conf.default.rp_filter=2
```

Then:

```bash
sysctl --system
```

What each knob does:

| Knob | Why |
|---|---|
| `net.ipv4.ip_forward` | Lets the host route packets between interfaces. K3s nodes route between pods, the WireGuard interface, and the LAN — this is non-negotiable. |
| `net.bridge.bridge-nf-call-iptables` | Same as `br_netfilter` module: makes bridge traffic visible to iptables. |
| `net.ipv4.conf.all.rp_filter=2` | "Loose" reverse-path filtering. The default `1` (strict) drops packets that arrive on an interface they wouldn't have left through, which kills asymmetric paths through WireGuard + Calico. **Do not** set this to `0` — that's "off entirely" and slightly less secure than `2`. |

The `rp_filter=2` value is the single most important sysctl in this whole stack. If your WireGuard mesh comes up but pods can't reach each other across nodes, this is the first thing to check.

## 4. Swap off

Kubernetes 1.22+ supports swap, but K3s' default config still assumes off. Easier to just turn it off than to debug later:

```bash
swapoff -a
sed -i.bak -E 's/^([^#].*\sswap\s.*)$/# \1   # disabled by host prep/' /etc/fstab
```

Confirm: `free -h` should show `Swap: 0B 0B 0B`.

## 5. Time sync

Critical for TLS — a clock 5 minutes off makes Let's Encrypt certificates "not valid yet" and authentication tokens silently invalid.

```bash
systemctl enable --now systemd-timesyncd
timedatectl set-timezone Europe/Paris   # or your timezone
timedatectl status | grep -E 'Time zone|System clock'
```

If `systemctl status systemd-timesyncd` shows `Status: "Idle."`, the service is healthy and just doesn't have anything to do this minute. The relevant output is `NTP service: active` and `System clock synchronized: yes`.

Some nodes (the cluster behind these docs has `wk-2` on chronyd) prefer `chrony` over `timesyncd`. They're equivalent for our purposes — pick `timesyncd` for simplicity.

## 6. Hostname and /etc/hosts

`/etc/hostname` should already match what the installer set:

```bash
cat /etc/hostname
# ms-1
```

`/etc/hosts` should map your hostname to `127.0.1.1`:

```
127.0.0.1 localhost
127.0.1.1 ms-1
```

Why? Some Kubernetes components resolve their own hostname at startup. If `getent hosts ms-1` returns nothing, things go wrong in confusing ways. The Ubuntu installer usually sets this correctly, but `/etc/hosts` is the kind of file that gets edited carelessly later — re-check it now.

## The `prepare-host.sh` shortcut

The infra repo's `prepare-host.sh` does all of the above plus a few cloud-edge-specific bits (IPv6 sysctls, an SSH dropin, a per-node firewall systemd unit you'll learn about in the next chapter). Recommended workflow:

```bash
# On each of ms-1, wk-1, wk-2, ctb-edge-1:
git clone https://github.com/<you>/infra.git
cd infra/k8s-cluster/bootstrap/host-prep
bash prepare-host.sh
```

It's idempotent — safe to re-run if you change something.

## Verify

```bash
# All three modules loaded
lsmod | grep -E '^(br_netfilter|vxlan|overlay)'

# Sysctl knobs are right
sysctl net.ipv4.ip_forward net.bridge.bridge-nf-call-iptables net.ipv4.conf.all.rp_filter

# Swap is off
swapon --show
# (no output)

# Time is synced
timedatectl status | grep 'System clock synchronized'
# System clock synchronized: yes
```

All four nodes should pass these checks before we touch WireGuard.

→ Next: [SSH and firewall hardening](/cortex/homelab-from-scratch/the-nodes-ssh-and-firewall-hardening)
