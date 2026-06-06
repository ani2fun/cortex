---
title: Why a private mesh?
summary: The exposure model that makes the whole homelab safe to run — only the edge sees the internet, and the home boxes only ever talk to each other through an encrypted tunnel. The before-and-after diagrams that explain why this is worth the effort.
---

## The problem with "just port-forward your router"

The naïve homelab design looks like this:

```d2
direction: right

internet: Internet {
  shape: cloud
}

router: Home router {
  shape: rectangle
}

ms1: ms-1 {
  shape: rectangle
  k3s: K3s API :6443
  ssh: sshd :22
}

wk1: wk-1 {
  shape: rectangle
  pg: Postgres :5432
  ssh1: sshd :22
}

internet -> router: any port the user opened
router -> ms1.k3s: forwarded :6443
router -> ms1.ssh: forwarded :22
router -> wk1.pg: forwarded :5432
router -> wk1.ssh1: forwarded :22
```

Every service you want to reach from outside the LAN gets a port-forward. The K3s API, SSH on each box, Postgres for that database client you keep meaning to set up. Each port-forward is a hole punched in your home network's outer wall — and there's no second wall behind it.

A few problems:

- **Each port becomes an attack surface.** SSH brute-force is mostly neutered, but the K3s API isn't designed to be public; if a CVE lands tomorrow, your cluster is vulnerable until you patch.
- **Port allocation gets weird fast.** You can't have two boxes serving SSH on `:22` to the public internet — you end up with `:2222` for one and `:2223` for another, and now nothing has a memorable address.
- **Your home IP leaks.** Every service tells the world where you live. Connecting from a coffee shop becomes a privacy decision.
- **Compromise of one service compromises adjacent ones.** A pod with a CVE on `wk-1` is one `kubectl exec` away from your local network's printers, light switches, and the laptop your kid does homework on.

## The mesh design

```d2
direction: right

internet: Internet {
  shape: cloud
}

router: Home router {
  shape: rectangle
}

mesh: WireGuard mesh — 172.27.15.0/24 {
  shape: package
  ms1: ms-1 (172.27.15.12)
  wk1: wk-1 (172.27.15.11)
  wk2: wk-2 (172.27.15.13)
  vm1: vm-1 (172.27.15.31)
}

vm1pub: vm-1 public IP

internet -> vm1pub: HTTPS :443\nSSH :22\nWG :51820/udp
vm1pub -> mesh.vm1: terminates here
router -> mesh.wk1: WG only :51820/udp
router -> mesh.ms1: WG only :51821/udp
router -> mesh.wk2: WG only :51822/udp
mesh -> mesh: every node reaches every other node\nonly through the tunnel
```

Three concrete differences:

1. **Only the cloud edge `vm-1` is on the public internet.** The home boxes are reachable from the public internet *only* on UDP `:51820/51821/51822` — the WireGuard handshake ports. Every other port is closed at the home router.
2. **WireGuard verifies every packet's identity before forwarding.** A peer can't even speak to the kernel's WireGuard module without presenting a valid pre-shared public key. It's not just "encrypted" — it's "authenticated *before* we'll spend a single CPU cycle decoding the packet".
3. **The mesh is a flat private network.** From inside, every node reaches every other node on `172.27.15.x`. Your laptop, when WireGuard'd in, gets `172.27.15.50` and can also reach everything. The complexity of "where am I, what subnet does this service live on" goes away.

## What this buys you

Concretely:

- **Kubernetes-API-grade access control for free.** `kubectl` reaches the API server at `172.27.15.12:6443`. From outside the mesh, that IP is unreachable. There's no public Kubernetes API, ever.
- **One public attack surface, easy to harden.** `vm-1` runs Traefik on `:80` and `:443`, sshd on `:22`, WireGuard on `:51820/udp`. Four ports, all justified, all behind explicit allowlist rules. Compare to "port-forward whatever you need" — at five services that's already 5+ unrelated public ports, with overlapping firewall rules.
- **No services discoverable.** Your home WAN IP appears, in port scans, to host *only* an unsigned UDP service that ignores all packets without a valid handshake. That's about as quiet as you can be on the public internet.
- **Network failures fail safe.** If WireGuard is down, the cluster nodes can't reach each other and *nothing* externally-reachable degrades into a worse state — there are no fallback paths. Kubernetes notices the node-not-ready state, reschedules pods if it can, and waits.

## What it costs you

Honestly: not much.

- **One extra hop of latency.** WireGuard adds ~0.1 ms over a LAN. Over the WireGuard mesh between `vm-1` and `wk-1`, it's the round-trip through your ISP, plus the UDP encryption/decryption — usually under 30 ms even on residential connections. Negligible for HTTP, perceptible only on chatty databases (which is why Postgres lives on a home node, not in the cloud).
- **One extra interface to debug.** When something doesn't work, the answer is sometimes "the WireGuard interface is broken" rather than "the application is broken." Chapter 4 of this section is about exactly that.
- **MTU math.** Every layer of encapsulation eats bytes from the maximum payload. WireGuard adds 60 bytes of overhead, leaving 1420 bytes of MTU. Calico VXLAN over WireGuard adds another 50, leaving 1370. We'll cover this in [Swap Flannel for Calico](/cortex/homelab-from-scratch/kubernetes-base-swap-flannel-for-calico).

The only unconditional cost is **one extra concept to understand** — WireGuard. Once you have, you'll never want to run a homelab without it.

## What WireGuard *is*, briefly

A Linux kernel module that does point-to-point UDP-based VPN with strong opinions:

- **No daemon to misconfigure.** The kernel module reads `/etc/wireguard/wg0.conf` once and stays in the kernel. No BIRD, no IPSec policy database, no negotiation phase to debug.
- **No identity beyond a public key.** Two keys are friends or they aren't. There's no PKI, no certificate authority, no CRL.
- **Cryptographic primitives chosen for safety, not negotiability.** ChaCha20-Poly1305 only. Curve25519 only. No "downgrade attack" surface because there's nothing to downgrade *to*.
- **One UDP port per peer, per direction.** Trivial to firewall.

If you've used OpenVPN, you'll find WireGuard about a tenth as much config and ten times faster. If you've used IPSec — congratulations, you're now allowed to forget all of it.

## What you'll have at the end of this section

- Four `wg0.conf` files, one per node.
- A four-peer mesh where `wg show` reports a recent handshake on every peer.
- Every node able to ping every other node by `172.27.15.x` address.
- Confidence that nothing on the home boxes is reachable from outside the mesh, except the WireGuard handshake itself.

→ Next: [WireGuard keys and the IP plan](/cortex/homelab-from-scratch/private-mesh-wireguard-keys-and-ip-plan)
