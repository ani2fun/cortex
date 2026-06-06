---
title: Where things are allowed to run
summary: Labels and taints decide where every workload lives. The edge gets a NoSchedule taint that only Traefik tolerates; Postgres pins to wk-1; Argo CD pins to wk-2. A four-node placement matrix you'll consult every time you add a service.
---

## Two mechanisms, one job

Kubernetes has two complementary ways to control where a Pod runs:

| Mechanism | Whose decision | Default behaviour |
|---|---|---|
| **Node label + nodeSelector / nodeAffinity** | The Pod opts *in* to a node | Pod runs anywhere if it doesn't ask |
| **Node taint + Pod toleration** | The Pod must explicitly opt *out* of a default rejection | Pod can't run on a tainted node unless it tolerates the taint |

Use **labels** to express *preference*: "Postgres prefers `wk-1`." Use **taints** to express *exclusion*: "the edge rejects everything except Traefik."

The cluster ends up with a small handful of each. Once they're set, the rest of the YAML in this book just works — every Deployment either says nothing (and lands on a worker) or has a brief `nodeSelector` + `tolerations` block that references these labels.

## The placement we're aiming for

```d2
direction: down

ms1: ms-1 — server\nTaint: node-role.kubernetes.io/control-plane:NoSchedule {
  shape: rectangle
  cp: ✓ kube-apiserver\n✓ kube-controller-manager\n✓ kube-scheduler\n✗ app pods (default-tainted)
}

wk1: wk-1 — worker\nLabel: kakde.eu/postgresql=true {
  shape: rectangle
  pods: ✓ Postgres (pinned)\n✓ general app pods
}

wk2: wk-2 — worker\nLabel: workload=argocd {
  shape: rectangle
  pods2: ✓ Argo CD (pinned)\n✓ general app pods
}

vm1: ctb-edge-1 — edge\nLabel: kakde.eu/edge=true\nTaint: kakde.eu/edge=true:NoSchedule {
  shape: rectangle
  edge: ✓ Traefik (tolerates taint)\n✗ everything else (taint rejects)
}
```

Five rules in plain English:

1. **`ms-1` is a control-plane.** Kubernetes auto-applies `node-role.kubernetes.io/control-plane=true:NoSchedule`. App pods land elsewhere automatically. We don't add or remove this taint.
2. **`vm-1` is the edge.** We add the taint `kakde.eu/edge=true:NoSchedule` so nothing schedules there by default. We add the matching label `kakde.eu/edge=true` so Traefik's nodeSelector lands it specifically there.
3. **`wk-1` runs Postgres.** We add the label `kakde.eu/postgresql=true`. Postgres' StatefulSet uses `nodeSelector: kakde.eu/postgresql=true` to pin to this node. No taint — other apps can run here too.
4. **`wk-2` runs Argo CD.** Label `workload=argocd`. Argo CD's nodeSelector pins to this node. Other apps still can.
5. **Other apps**: no labels, no tolerations. They land on `wk-1` or `wk-2`, whichever has room.

## Apply the labels and taints

A small script that does it all idempotently:

```bash
export KUBECONFIG=~/.kube/homelab.yaml

# Re-assert the default control-plane taint (in case it was removed)
kubectl taint nodes ms-1 node-role.kubernetes.io/control-plane=true:NoSchedule --overwrite || true

# Edge taint + label
kubectl taint nodes ctb-edge-1 kakde.eu/edge=true:NoSchedule --overwrite

# Role labels (descriptive — don't gate scheduling, but make get-nodes self-documenting)
kubectl label node ms-1       homelab.kakde.eu/role=server  --overwrite
kubectl label node wk-1       homelab.kakde.eu/role=worker  --overwrite
kubectl label node wk-2       homelab.kakde.eu/role=worker  --overwrite
kubectl label node ctb-edge-1 homelab.kakde.eu/role=edge    --overwrite
kubectl label node ctb-edge-1 kakde.eu/edge=true            --overwrite

# Workload-pin labels
kubectl label node wk-1 kakde.eu/postgresql=true --overwrite
kubectl label node wk-2 workload=argocd          --overwrite

# Confirm
kubectl get nodes --show-labels
kubectl describe node ctb-edge-1 | grep -E '(Taints|Labels)'
```

`kubectl get nodes --show-labels` is dense but readable; `kubectl describe node ctb-edge-1 | grep Taints` is the quickest way to confirm the edge taint is in place.

## What a workload's YAML looks like

The placement-aware bits are short. Two examples.

### Traefik on the edge

```yaml
spec:
  template:
    spec:
      hostNetwork: true               # bind directly to host :80, :443
      nodeSelector:
        kakde.eu/edge: "true"         # only schedule on edge nodes
      tolerations:
        - key: kakde.eu/edge          # tolerate the NoSchedule taint
          operator: Equal
          value: "true"
          effect: NoSchedule
```

### Postgres on `wk-1`

```yaml
spec:
  template:
    spec:
      nodeSelector:
        kakde.eu/postgresql: "true"   # only schedule on wk-1
      # No toleration — wk-1 has no taint.
```

### Argo CD on `wk-2`

```yaml
spec:
  template:
    spec:
      nodeSelector:
        workload: argocd
```

That's the whole pattern. Most apps don't need any of this — they happily land on whichever worker has CPU.

## Why bother?

A reasonable question for a small cluster. Three reasons:

- **Failure isolation.** When `wk-1` reboots, Postgres goes down (briefly) but Argo CD stays up. Without pinning, both might be on the same node.
- **Storage co-location.** Postgres' PVC is on `wk-1`'s disk via local-path storage. The pod *must* run there or it can't see its data. Pinning is a hard requirement, not a preference.
- **Predictable upgrades.** When you upgrade Kubernetes, you cordon nodes one at a time. Knowing that `wk-2` only runs Argo CD makes "drain `wk-2`, upgrade, uncordon" a low-risk operation.

In a 30-node cluster you'd use anti-affinity rules to spread load probabilistically. In a four-node homelab, hard pinning is simpler and equivalent.

## When pinning becomes a problem

If you outgrow this and pinning starts hurting:

- **Postgres → CloudNativePG operator.** A real Postgres operator manages multi-instance failover; the pinning becomes "this StatefulSet runs on any node with `storage=ssd` and CloudNativePG handles the rest."
- **Argo CD → multi-replica.** Modern Argo CD can run multiple replicas behind a single Service; you'd remove the pin.
- **Add `wk-3`, `wk-4`.** More workers means more places for "general" apps to land, which means the pinned ones have proportionally less impact.

For now, four nodes and three pins is the right granularity.

## Verify

```bash
# All pinned workloads should report no schedulability problems
kubectl describe node wk-1 | grep -E 'Taints|kakde.eu'
# Taints:    <none>
# Labels:    homelab.kakde.eu/role=worker
#            kakde.eu/postgresql=true

kubectl describe node ctb-edge-1 | grep -E 'Taints|kakde.eu'
# Taints:    kakde.eu/edge=true:NoSchedule
# Labels:    homelab.kakde.eu/role=edge
#            kakde.eu/edge=true

# A throwaway pod should NOT land on the edge unless it tolerates the taint
kubectl run notolerate --image=alpine --command -- sleep 1d
kubectl get pod notolerate -o wide
# notolerate ... <not edge node>
kubectl delete pod notolerate
```

When pinning is right, you can read `kubectl get pods -A -o wide` and predict which node every pod is on without looking. That's the goal.

→ Next: [Pin Traefik to the edge](/cortex/homelab-from-scratch/the-edge-pin-traefik-to-the-edge)
