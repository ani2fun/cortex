---
title: Network policies keep Postgres internal
summary: Default-deny on the database namespace, then allow-by-namespace-label so only namespaces tagged `kakde.eu/postgresql-access=true` can dial port 5432. A clean three-line policy that survives every "wait, why can't this pod reach Postgres" debug session.
---

## Why default-deny

Without a `NetworkPolicy`, **every pod in the cluster can reach every Service.** That's the Kubernetes default, and it's wrong for a database namespace.

The right model: pods can only reach Postgres if they prove they should be able to. Two choices for "prove they should":

| Approach | Selector | Cost |
|---|---|---|
| **Per-pod label** | `app=keycloak` | Every app has to opt in. Lots of policies. |
| **Per-namespace label** | `kakde.eu/postgresql-access=true` | Coarser. But namespace-level access is a sensible boundary in a homelab. |

We use the second. It's the simplest mental model: *namespaces* get database access, not individual pods.

## The two policies

```d2
direction: right

dbns: databases-prod namespace {
  shape: rectangle

  pods: postgresql-0 pod
  default_deny: NetworkPolicy\npostgresql-default-deny-ingress\n→ explicit ingress required
  allow: NetworkPolicy\npostgresql-allow-from-selected-namespaces\n→ allow if NS has\nkakde.eu/postgresql-access=true
}

apps: apps namespace\nlabel: kakde.eu/postgresql-access=true {
  shape: rectangle
  app: app pod
}

identity: identity namespace\nlabel: kakde.eu/postgresql-access=true {
  shape: rectangle
  kc: keycloak pod
}

random: monitoring namespace\nNO label {
  shape: rectangle
  prom: prometheus pod
}

apps.app -> dbns.pods: ✓ allowed (NS labeled)
identity.kc -> dbns.pods: ✓ allowed (NS labeled)
random.prom -> dbns.pods: ✗ blocked (default deny)
```

Two policies, applied in order. The first turns the database namespace into "no ingress unless allowed." The second adds the allow-list. Calico evaluates them together.

### Policy 1: default-deny-ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: postgresql-default-deny-ingress
  namespace: databases-prod
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/name: postgresql
  policyTypes:
    - Ingress
```

This says: "for pods labeled `app.kubernetes.io/name: postgresql` in this namespace, only allow ingress that's explicitly permitted by *some other policy.*" Since this is the only policy at this point, the effect is: no traffic in.

(Note: it doesn't say `policyTypes: [Egress]`. Outbound is unaffected. The Postgres pod can still reach DNS, the API server, anywhere it wants. That's intentional — locking down egress on a database is rarely useful.)

### Policy 2: allow-from-labeled-namespaces

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: postgresql-allow-from-selected-namespaces
  namespace: databases-prod
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/name: postgresql
  policyTypes:
    - Ingress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: databases-prod    # same-namespace
        - namespaceSelector:
            matchLabels:
              kakde.eu/postgresql-access: "true"             # opted-in namespaces
      ports:
        - protocol: TCP
          port: 5432
```

Two `from` clauses, OR'd:

1. **`kubernetes.io/metadata.name: databases-prod`** — pods *inside* the databases-prod namespace can reach the Postgres pod. Self-traffic. Useful for things like a backup CronJob that lives in the same namespace.
2. **`kakde.eu/postgresql-access: "true"`** — any namespace with this label can reach the Postgres pod *on port 5432*. Other ports (none, but in principle) would still be blocked.

## Apply

```bash
kubectl apply -f deploy/postgresql/networkpolicy.yaml

# Confirm
kubectl -n databases-prod get networkpolicy
# postgresql-default-deny-ingress              5s
# postgresql-allow-from-selected-namespaces    5s
```

## Label the namespaces that need access

```bash
# The apps namespace (where most workloads live) gets access
kubectl label namespace apps        kakde.eu/postgresql-access=true --overwrite

# Identity (for Keycloak) gets access
kubectl label namespace identity    kakde.eu/postgresql-access=true --overwrite

# Anywhere else: don't label. They're denied by default.
```

Useful command to audit who can reach the database:

```bash
kubectl get namespaces -l kakde.eu/postgresql-access=true
# NAME       STATUS  AGE
# apps       Active  10d
# identity   Active  3d
```

If a fourth namespace creeps onto that list because someone got "PR-merged-in-a-hurry"-happy, audit-then-decide-then-label is the workflow.

## Test the policy

```bash
# 1. From inside an opted-in namespace — should work
kubectl run -n apps --rm -it test-allowed --image=postgres:17.9 --restart=Never -- \
  pg_isready -h postgresql.databases-prod.svc.cluster.local -p 5432
# postgresql.databases-prod.svc.cluster.local:5432 - accepting connections

# 2. From inside a non-opted-in namespace — should hang (timed out)
kubectl create namespace testns 2>/dev/null || true
kubectl run -n testns --rm -it test-blocked --image=postgres:17.9 --restart=Never -- \
  pg_isready -h postgresql.databases-prod.svc.cluster.local -p 5432 -t 5
# pg_isready: error: timeout expired
```

Both expected. The blocked test takes ~5 seconds (the TCP connect timeout) — there's no ICMP unreachable from a NetworkPolicy drop; the connection just hangs.

## Common mistakes

- **Forgetting the namespace label on `apps`.** Symptom: app pods can't reach Postgres, all backends crash with "connection timed out". Fix: `kubectl label namespace apps kakde.eu/postgresql-access=true --overwrite`.
- **Egress policy that breaks DNS.** A "deny all egress except to Postgres" policy on the app namespace seems clever until DNS resolution itself fails. We didn't write one of those; resist the urge until you really need it.
- **NetworkPolicy not enforced.** With Flannel, NetworkPolicy is silently ignored. Calico enforces. We picked Calico for exactly this reason. If your policies don't work, double-check `kubectl get crd` shows Calico's installation.
- **Cross-port confusion.** The `ports` list in the allow policy says only `:5432`. If we exposed Postgres on `:5433` for some reason, that port would be denied. Match what's actually in the Service.

## What about egress *to* Postgres from outside the cluster?

The whole homelab's mental model is "Postgres has no public exposure." Network policy enforces it inside the cluster; the home router enforces it from outside (no port-forward for `:5432` ever). Both layers are independent.

If you need to access Postgres from your laptop for debugging:

```bash
# Port-forward through the cluster
kubectl -n databases-prod port-forward svc/postgresql 5432:5432

# In another terminal
psql 'postgresql://postgres:<password>@127.0.0.1:5432/postgres'
```

`kubectl port-forward` runs over the Kubernetes API, which you already reach over the WireGuard mesh. Authenticated, encrypted, mesh-only. **No NetworkPolicy bypass needed** — you're talking to the apiserver, not the Postgres Service directly.

## What you should have now

- Two NetworkPolicy resources in `databases-prod`
- The `apps` namespace labeled `kakde.eu/postgresql-access=true`
- The `identity` namespace labeled the same
- A successful `pg_isready` from a labeled namespace
- A timed-out `pg_isready` from an un-labeled namespace
- A working `kubectl port-forward` for laptop debugging

The next chapter is the first real consumer of Postgres: Keycloak.

→ Next: [Keycloak as the identity plane](/cortex/homelab-from-scratch/stateful-services/keycloak-as-the-identity-plane)
