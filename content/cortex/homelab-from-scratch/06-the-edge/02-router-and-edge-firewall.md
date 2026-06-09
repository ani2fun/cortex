---
title: Router and edge firewall
summary: At the home router, you forward almost nothing. At the cloud edge, an nftables systemd unit allows :22, :80, :443, and the WireGuard UDP port — and rejects everything else. Defence in depth, two layers deep, with the rules in plain text.
---

## The two firewalls

```d2
direction: down

internet: Public internet {
  shape: cloud
}

router: Home router\n203.0.113.10\nport-forward UDP only {
  shape: rectangle
  fwd: 51820 → wk-1\n51821 → ms-1\n51822 → wk-2
}

vm1: vm-1 (cloud edge) {
  shape: rectangle
  nft: nftables\nedge_guardrail table\npriority -50\nDEFAULT DROP
  ipt: iptables drops\non eth0 only\nbelt-and-braces
  apps: sshd\ntraefik\nwg-quick
}

internet -> router: only UDP for WireGuard
router -> wk-1: forwarded
internet -> vm1.nft
vm1.nft -> vm1.ipt: passes filter
vm1.ipt -> vm1.apps
```

Two boxes, two firewalls, two responsibilities:

- **Home router** — explicitly forwards three UDP ports (the WireGuard handshake ports for the three home boxes) and **nothing else**. Default behaviour is reject. There is no public access to anything inside the home network.
- **Cloud edge** — the only machine with a real public IP. Its firewall is the strictest, with a default-drop allowlist that explicitly accepts only what the homelab needs.

## The home router

If you got through [Bring up the mesh](/cortex/homelab-from-scratch/private-mesh/bring-up-the-mesh), this is already done. To recap:

| Public port | Protocol | Internal IP | Internal port |
|---|---|---|---|
| `51820` | UDP | `192.168.15.3` (`wk-1`) | `51820` |
| `51821` | UDP | `192.168.15.2` (`ms-1`) | `51820` |
| `51822` | UDP | `192.168.15.4` (`wk-2`) | `51820` |

That is the *entire* router config. No port-forwards for SSH, the K3s API, web traffic, or anything else. The router's default behaviour ("reject all inbound") covers it.

If you've ever opened ports for media servers, security cameras, smart home stuff — close them. The homelab will run fine; the things you opened them for can move behind the WireGuard mesh and admin from your laptop's WireGuard peer instead.

## The cloud edge — layer 1 (nftables allowlist)

The primary defence on `vm-1` is an nftables ruleset that runs at boot via a systemd unit. It's idempotent — re-running it deletes the old table and reapplies.

The script (lifted from `infra/k8s-cluster/platform/traefik/edge-guardrail.sh`):

```bash
#!/usr/bin/env bash
set -euo pipefail

PUB_IF="${PUB_IF:-eth0}"

if nft list table inet edge_guardrail >/dev/null 2>&1; then
  nft delete table inet edge_guardrail
fi

nft -f - <<NFT
add table inet edge_guardrail
add chain inet edge_guardrail input { type filter hook input priority -50; policy drop; }

add rule inet edge_guardrail input iifname "lo" accept
add rule inet edge_guardrail input ct state established,related accept
add rule inet edge_guardrail input iifname != "$PUB_IF" accept

add rule inet edge_guardrail input iifname "$PUB_IF" ip protocol icmp accept
add rule inet edge_guardrail input iifname "$PUB_IF" ip6 nexthdr icmpv6 accept
add rule inet edge_guardrail input iifname "$PUB_IF" udp dport 51820 accept
add rule inet edge_guardrail input iifname "$PUB_IF" tcp dport 22 accept
add rule inet edge_guardrail input iifname "$PUB_IF" tcp dport { 80, 443 } accept
add rule inet edge_guardrail input iifname "$PUB_IF" counter drop
NFT
```

Reading it line by line:

- **`add table inet edge_guardrail`** — a new nftables table, scope IPv4+IPv6, name `edge_guardrail`. Separate table so it doesn't conflict with whatever else is in `nft`.
- **`add chain inet edge_guardrail input { type filter hook input priority -50; policy drop; }`** — the chain hooks into the kernel's `input` netfilter hook (so it sees every packet destined for this host). Priority `-50` means it runs *before* the standard `filter` chain (priority 0), so it gets first say. Default policy: **drop**. Anything not explicitly allowed below this line is silently dropped.
- **`iifname "lo" accept`** — loopback always allowed. Many local services need it.
- **`ct state established,related accept`** — return packets for outbound connections always allowed. Without this, you can't make outbound HTTP requests because the responses are rejected.
- **`iifname != "$PUB_IF" accept`** — packets arriving on any interface that isn't the public one (typically `wg0`, `lo`, `cni0`, the calico-vxlan interface) are accepted. The strict allowlist applies *only to the public interface*.
- **`iifname "$PUB_IF" ip protocol icmp accept`** — ICMP (ping) on the public interface, allowed. Helps with debugging.
- **`iifname "$PUB_IF" udp dport 51820 accept`** — WireGuard handshake / data, the way `vm-1` peers with the home nodes.
- **`iifname "$PUB_IF" tcp dport 22 accept`** — SSH from the public internet. We could lock this down to a specific source IP via the allowlist env file, but starting permissive and tightening later is easier.
- **`iifname "$PUB_IF" tcp dport { 80, 443 } accept`** — HTTP and HTTPS. Traefik binds these.
- **`iifname "$PUB_IF" counter drop`** — anything else on the public interface is dropped, with a counter so you can see if anything's hitting it.

That's it. **Four allowed ports on the public interface: `22`, `80`, `443`, `51820/udp`.** Everything else is dropped.

## The cloud edge — layer 2 (iptables drops)

`prepare-host.sh` from chapter 3 installed a second tiny script that explicitly drops K8s-internal ports on `eth0`:

- **`tcp/10250`** — kubelet. Anyone who reaches it can `kubectl exec` into containers on this node, bypassing apiserver auth.
- **`udp/4789`** — Calico VXLAN. Leaving it open lets a remote attacker inject pod-network packets.
- **`tcp/30000-32767`** — the NodePort range. We don't use NodePorts, but blocking the range is free.

Why have *both* layers? Because each is independent. If the nftables table is somehow flushed (a misconfigured `nft -f config` you typed in a panic, a stale shell history command), the iptables drops still hold. Belt and braces. The cluster behind these docs has both layers; nothing's been compromised in years.

### Why nftables for the allowlist and iptables for the drops?

A reasonable question — they're functionally overlapping. The split is operational, not technical:

- **nftables for the allowlist** because it's the modern Linux packet-filter (replaces the iptables/ip6tables/arptables/ebtables zoo with one syntax) and the allowlist is the *primary* control. Its language is more expressive — you can write `tcp dport { 80, 443 }` instead of two separate rules. Atomic ruleset reloads. Native counters. We want our most-edited firewall config in the more pleasant tool.
- **iptables for the belt-and-braces drops** because Kubernetes itself installs iptables rules (kube-proxy still uses iptables on most distros, including K3s). Adding our drops in the same chain machinery means there's no risk of "nftables and iptables disagree about which packet wins" — the iptables rules sit alongside kube-proxy's, in the same evaluation order. Less surprising.

If you wanted one tool, all-nftables is the right answer in 2026 (and Kubernetes is gradually migrating). For this homelab, "one tool per role, with each role clearly justified" is more legible than "one tool everywhere with mixed-purpose chains."

## Verify from outside the network

From a machine that *isn't* on your home LAN — your phone tethered to mobile data is fine — port-scan the cloud edge:

```bash
# Install nmap if you don't have it (laptop)
nmap -Pn -p 22,53,80,443,4789,6443,10250,30000-32767,51820 198.51.100.25
```

You should see only `22/tcp`, `80/tcp`, `443/tcp`, and `51820/udp` as `open` (and even that last one is "open|filtered" since UDP scanning is unreliable). Everything else: `filtered`.

```bash
# More telling: try kubelet directly
curl -k https://198.51.100.25:10250/healthz --max-time 3
# curl: (28) Connection timed out

# Try the K3s API
curl -k https://198.51.100.25:6443/healthz --max-time 3
# curl: (28) Connection timed out

# Both should time out.
```

That's the end-state: the cluster's control plane is invisible from the public internet. The only way in is through Traefik on `:443`.

## Don't run a port scan against your own home WAN

A common temptation: "let me check that my home WAN doesn't have anything open." Tools that do this from the cloud edge (`nmap 203.0.113.10` from `vm-1`) often produce misleading output because the edge sees its own outbound NAT, not what the public sees.

The right test is from a *third* machine — a friend's network, your phone on cellular — running:

```bash
nmap -Pn -p 1-1000,6443,10250 203.0.113.10
```

Every port should be `filtered`. The home WAN should look completely silent except for the three forwarded WireGuard ports (`51820/udp`, `51821/udp`, `51822/udp`), which only respond to a valid WireGuard handshake.

## What you should have now

- The home router forwards exactly three UDP ports
- `vm-1`'s nftables `edge_guardrail` chain is active, default-drop, allowing 22/80/443/51820-udp on the public interface
- `vm-1`'s iptables drops are active for kubelet, VXLAN, and NodePorts on eth0
- An external port scan of your home WAN sees almost nothing
- An external port scan of the cloud edge sees only the four allowed ports

The next chapter lets us issue real Let's Encrypt certificates for any subdomain we want — using the API token we already have.

→ Next: [TLS on autopilot](/cortex/homelab-from-scratch/the-edge/tls-on-autopilot)
