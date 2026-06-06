---
title: Architecture at a glance
summary: The four-layer mental model — private mesh, Kubernetes base, platform plane, workloads — and the node-placement matrix you'll consult every time you add a new app. Plus the one constraint that decides every other decision in this book.
---

## The four layers

Every piece of infrastructure in this book belongs to one of four layers. They build on each other. You can't skip one — but if you understand each layer's *job*, the order to install them in becomes obvious.

```d2
direction: down

layer4: 4. Workloads {
  whoami: whoami
  apps: your apps...
}

layer3: 3. Platform plane {
  traefik: Traefik (edge)
  certmgr: cert-manager
  argocd: Argo CD
  sealed: Sealed Secrets
  postgres: PostgreSQL
  keycloak: Keycloak
}

layer2: 2. Kubernetes base {
  k3s: K3s server + agents
  calico: Calico CNI
}

layer1: 1. Private mesh {
  wireguard: WireGuard mesh — 172.27.15.0/24
  hosts: Ubuntu 24.04 hosts
}

layer4 -> layer3: scheduled by
layer3 -> layer2: runs on
layer2 -> layer1: rides on
```

- **Layer 1 — private mesh.** Four Ubuntu hosts wired into one WireGuard tunnel. From this layer's view, "the network" is `172.27.15.0/24` and nothing else. The home NAT and the cloud edge's public IP exist only as bootstrap details — once the mesh is up, both ends pretend they're on the same LAN.
- **Layer 2 — Kubernetes base.** K3s server on `ms-1`, K3s agents on `wk-1`, `wk-2`, `vm-1`. The default Flannel is replaced with Calico because we want NetworkPolicy support and a CNI that survives the homelab's eventual scale. Cluster CIDR `10.42.0.0/16`, service CIDR `10.43.0.0/16`. The control-plane advertises itself on its WireGuard IP, which is the trick that lets the cloud edge be a member.
- **Layer 3 — platform plane.** The infrastructure your apps need to be ergonomic: an ingress controller (Traefik), automatic certificate issuance (cert-manager), a GitOps engine (Argo CD), a way to commit secrets to Git safely (Sealed Secrets), a database (Postgres), an identity provider (Keycloak). Every one of these gets a chapter.
- **Layer 4 — workloads.** Your code. The first one is `whoami`; the second one is whatever you're here to build. Once layer 3 is in place, deploying a workload is a Kustomize-overlay PR plus an Argo CD `Application`.

## The node-placement matrix

The four-node split is the most consequential decision you'll make. Once it's set, half the YAML in this book follows mechanically.

| Workload class | `ms-1` (server) | `wk-1` (worker) | `wk-2` (worker) | `vm-1` (edge) |
|---|---|---|---|---|
| **Control-plane** | ✅ alone | ❌ | ❌ | ❌ |
| **Edge ingress (Traefik)** | ❌ | ❌ | ❌ | ✅ pinned |
| **Database (Postgres)** | ❌ | ✅ pinned | ❌ | ❌ |
| **GitOps (Argo CD)** | ❌ | ❌ | ✅ pinned | ❌ |
| **Application pods** | ❌ taint | ✅ | ✅ | ❌ taint |

The rules in plain English:

1. **`ms-1` runs the K3s control-plane and nothing else.** The Kubernetes default `node-role.kubernetes.io/control-plane=true:NoSchedule` taint takes care of this. App pods land on the workers.
2. **`vm-1` is taintet `kakde.eu/edge=true:NoSchedule`.** Only Traefik tolerates that taint, so only Traefik runs there. The edge stays small and predictable: one ingress controller, no surprise neighbours.
3. **Postgres pins to `wk-1`** via the label `kakde.eu/postgresql=true`. Local-path storage means a Postgres pod that doesn't move; pinning the workload to the node where its volume lives is the simplest way to make that work.
4. **Argo CD pins to `wk-2`** via the label `workload=argocd`. There's no deep reason it has to be `wk-2` rather than `wk-1` — but separating the GitOps engine from the database means a worker reboot only takes one of them down at a time.
5. **Other apps default to `wk-1` or `wk-2`.** The scheduler picks whichever has room. If a future app needs pinning, it gets its own label.

You'll set those labels and taints in [Where things are allowed to run](/cortex/homelab-from-scratch/kubernetes-base-where-things-are-allowed-to-run). The placement story isn't an afterthought — it's the design.

## The one constraint

There is exactly one rule the rest of this book rotates around: **only `vm-1` is on the public internet.**

| Node | Public IP | LAN IP | WireGuard IP |
|---|---|---|---|
| `ms-1` | none | `192.168.15.2` | `172.27.15.12` |
| `wk-1` | none | `192.168.15.3` | `172.27.15.11` |
| `wk-2` | none | `192.168.15.4` | `172.27.15.13` |
| `vm-1` | `198.51.100.25` | none | `172.27.15.31` |

Public traffic enters at `vm-1:443`, terminates TLS in Traefik, then leaves the edge node *only* over the WireGuard interface. The home boxes never receive a packet from the public internet — not on port 6443 (the K3s API), not on port 22, not on port 80. If they did, the homelab would have an attack surface roughly the size of the public internet, and that's not what we're building.

This rule decides:

- The edge node's nftables ruleset (covered in [Router and edge firewall](/cortex/homelab-from-scratch/the-edge-router-and-edge-firewall)).
- Which CIDR cert-manager talks to Cloudflare from (it's the edge, of course — DNS-01 traffic exits via the WireGuard tunnel and out vm-1).
- Why Argo CD is exposed *via Traefik* and not by, say, a NodePort or a `hostPort: 8443` shortcut.
- Why the K3s API server is bound to the WireGuard IP and never the LAN IP — kubectl from outside the home reaches it via the mesh, not by punching a hole in the home firewall.

If you ever find yourself thinking *"can I just open port X on the home router for this?"*, the answer is no. There is exactly one public surface, and it lives on a 5-euro VM you can rebuild in 20 minutes.

→ Next: [Prerequisites & shopping list](/cortex/homelab-from-scratch/foundations-prerequisites-and-shopping-list)
