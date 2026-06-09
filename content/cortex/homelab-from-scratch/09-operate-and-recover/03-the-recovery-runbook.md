---
title: The recovery runbook
summary: From cold metal to a running cluster in roughly six hours, layer by layer — Ubuntu, WireGuard, K3s, Calico, platform plane, data, apps. Each layer with a verification gate before the next one starts. The runbook you hope you never use, written so you can use it on no sleep.
---

## When you'll need this

- A worker died and you bought new hardware.
- The home was relocated and the network reset.
- You upgraded Kubernetes and rolled forward into a corner you can't roll back from.
- Worst case: every disk died simultaneously.

The runbook is the same in all four cases. The only difference is whether you're rebuilding *one* node or *all* of them. The procedure is layered, and you can stop at whatever layer has the right state already.

## The stack, layer by layer

```d2
direction: up

L0: Layer 0 — Ubuntu host\nfresh install, hostname, SSH key, network config {
  shape: rectangle
}
L1: Layer 1 — WireGuard mesh\nkeys, wg0.conf, wg-quick {
  shape: rectangle
}
L2: Layer 2 — K3s server + Calico {
  shape: rectangle
}
L3: Layer 3 — K3s agents joined {
  shape: rectangle
}
L4: Layer 4 — Platform plane\nSealed Secrets, cert-manager, Argo CD, Traefik {
  shape: rectangle
}
L5: Layer 5 — Stateful services\nPostgres restored from dump, Keycloak realm imported {
  shape: rectangle
}
L6: Layer 6 — Workloads\nArgo CD app-of-apps applies the rest {
  shape: rectangle
}
done: ✓ Production back up

L0 -> L1
L1 -> L2
L2 -> L3
L3 -> L4
L4 -> L5
L5 -> L6
L6 -> done
```

Each arrow is a gate. Don't proceed up to the next layer until the current one passes its check. **Recover in order; each layer assumes the layers below it work.**

## Pre-flight (do this before touching any node)

On your laptop, while the cluster is still sort-of running (or shortly after):

```bash
# 1. Tooling
which ssh kubectl helm kubeseal jq openssl curl dig nc nft

# 2. SSH config has aliases for ms-1, wk-1, wk-2, vm-1 → root@<ip>
ssh -G ms-1 | grep -E 'hostname|user'

# 3. Backup files within reach
ls -la ~/backups/postgres/postgres-backup-*.tar.gz | tail -3
ls -la ~/backups/sealed-secrets-master-key-*.yaml  | tail -3
ls -la ~/backups/keycloak/realm-export*.tar.gz     | tail -3

# 4. Password manager open with:
#    - WireGuard private keys (4 entries)
#    - Postgres superuser + app-DB passwords
#    - Keycloak admin password
#    - Cloudflare API token
#    - GitHub PAT (for the GHA workflow that bumps manifests)

# 5. Repo at the snapshot revision
cd ~/work/infra
git fetch origin && git checkout main
git rev-parse HEAD
```

Total time: ~10 minutes. Skipping any of this turns a 6-hour rebuild into a 12-hour one.

## Layer 0 — Ubuntu hosts

For each node:

1. Install Ubuntu 24.04 LTS Server (chapter [Install Ubuntu 24.04](/cortex/homelab-from-scratch/the-nodes/install-ubuntu-24-04)).
2. Set hostname during install: `ms-1`, `wk-1`, `wk-2`, or `ctb-edge-1`.
3. Install your SSH public key for `root` (or `ubuntu`).
4. Set static IPs on the home boxes.
5. Run `prepare-host.sh` from the infra repo (chapter [Baseline host prep](/cortex/homelab-from-scratch/the-nodes/baseline-host-prep)).

**Gate.** All four nodes pass `swapon --show` (empty), `lsmod | grep -E 'br_netfilter|vxlan'`, and accept SSH from your laptop.

**Time budget**: ~30 min/node, parallel = ~30 min total if you have a USB stick per node.

## Layer 1 — WireGuard

For each node, with private keys from your password manager:

1. `mkdir -p /etc/wireguard && chmod 700 /etc/wireguard`
2. Restore `wg0.key` from the password manager.
3. Apply the per-node `wg0.conf` from the infra repo.
4. `systemctl enable --now wg-quick@wg0`

The `vm-1` config is unique because it must point at the home WAN IP (which may have changed if you're rebuilding after relocation).

**Gate.** `wg show` reports recent handshakes for every peer. `ping 172.27.15.X` works between every pair of nodes.

**Time budget**: ~10 min/node, parallel = ~15 min total.

## Layer 2 — K3s server + Calico

On `ms-1`:

```bash
# Install K3s server (chapter "Install the control-plane")
bash k8s-cluster/bootstrap/k3s/install-server-ms-1.sh

# Install Calico (chapter "Swap Flannel for Calico")
bash k8s-cluster/bootstrap/k3s/install-calico.sh
```

**Gate.** `kubectl get nodes` shows `ms-1 Ready`. `kubectl -n calico-system get pods` shows `calico-node-<random>` Running.

**Time budget**: ~5 min.

## Layer 3 — K3s agents

For each of `wk-1`, `wk-2`, `vm-1`:

```bash
# K3S_TOKEN from /var/lib/rancher/k3s/server/node-token on ms-1
export K3S_TOKEN='<token>'
bash k8s-cluster/bootstrap/k3s/install-agent-<node>.sh
```

Then on `ms-1`:

```bash
bash k8s-cluster/bootstrap/k3s/apply-node-placement.sh
```

**Gate.** `kubectl get nodes` shows all four `Ready`. Labels on each node match what placement expects.

**Time budget**: ~10 min total.

## Layer 4 — Platform plane

Order matters. Sealed Secrets first because everything else needs to apply secrets from Git.

```bash
# 4a. Sealed Secrets controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.33.1/controller.yaml

# 4b. Restore the master key
kubectl apply -f ~/backups/sealed-secrets-master-key-<ts>.yaml
kubectl -n kube-system delete pod -l app.kubernetes.io/name=sealed-secrets-controller

# 4c. cert-manager
helm repo add jetstack https://charts.jetstack.io && helm repo update
helm install cert-manager jetstack/cert-manager \
  -n cert-manager --create-namespace --version v1.19.1 --set installCRDs=true

# 4d. Cloudflare API token Secret + ClusterIssuers (from Git)
kubectl apply -f k8s-cluster/platform/cert-manager/

# 4e. Traefik (chapter "Pin Traefik to the edge")
kubectl apply -f k8s-cluster/platform/traefik/

# 4f. Argo CD
bash k8s-cluster/platform/argocd/install-argocd.sh
bash k8s-cluster/platform/argocd/configure-argocd.sh
```

**Gate.** `https://argocd.homelab.example` returns 200 with a real Let's Encrypt cert. cert-manager has issued certs for `whoami`, `argocd`, `keycloak` (or whatever your committed Ingresses are).

**Time budget**: ~30 min.

## Layer 5 — Stateful services

Postgres' StatefulSet won't have any data on it yet. Apply the manifests, then restore.

```bash
# 5a. Apply Postgres manifests
kubectl apply -f k8s-cluster/platform/postgresql/

# 5b. Wait for postgresql-0 to become Ready (will be empty)
kubectl -n databases-prod wait pod/postgresql-0 --for=condition=Ready --timeout=5m

# 5c. Restore the dump
scripts/dr/postgres-restore.sh ~/backups/postgres/postgres-backup-<ts>.tar.gz

# 5d. Apply Keycloak manifests
kubectl apply -f k8s-cluster/apps/keycloak/

# 5e. Wait for Keycloak to come up on the restored database
kubectl -n identity wait pod -l app=keycloak --for=condition=Ready --timeout=5m

# 5f. Realm import is optional — the Postgres restore already brought it back
```

**Gate.** `psql 'postgresql://postgres:<pw>@postgresql.databases-prod.svc.cluster.local:5432/keycloak' -c '\dt'` lists Keycloak tables. `https://keycloak.homelab.example` shows the login page.

**Time budget**: ~20 min.

## Layer 6 — Workloads

Apply the root Argo CD Application:

```bash
kubectl apply -f argocd/apps/argocd-apps.yaml
```

Argo CD reads `argocd/apps/`, applies every child Application, and within ~5 minutes the entire workload layer is back.

**Gate.** `argocd app list` shows every app `Synced` and `Healthy`. Every public URL returns 200 with a valid cert.

**Time budget**: ~10 min.

## Total recovery time

| Layer | Time |
|---|---|
| Pre-flight | 10 min |
| L0 — Ubuntu | 30 min |
| L1 — WireGuard | 15 min |
| L2 — K3s + Calico | 5 min |
| L3 — agents | 10 min |
| L4 — platform | 30 min |
| L5 — data | 20 min |
| L6 — workloads | 10 min |
| **Total** | **~2 h** parallelised, ~6 h serial |

The 2-hour figure assumes you have a USB stick per home node, can run the OS installs in parallel, and have done this before. The 6-hour figure assumes you're tired and reading the runbook for the first time.

Don't try to be clever. Each gate exists because the layer above breaks in confusing ways if it isn't met. Walking through the layers slowly is faster than skipping ahead and unwinding.

## Practice (or you don't really have a runbook)

The trustworthy version of this chapter is the one you've actually executed end-to-end. The practical version: rebuild `vm-1` from scratch every six months as a fire drill. It's the cheapest node, easiest to nuke, and exercises layers 0, 1, 3, 4 (the edge-specific bits).

Tag a calendar reminder. Future you will thank present you.

→ Next: [Where to grow from here](/cortex/homelab-from-scratch/operate-and-recover/where-to-grow-from-here)
