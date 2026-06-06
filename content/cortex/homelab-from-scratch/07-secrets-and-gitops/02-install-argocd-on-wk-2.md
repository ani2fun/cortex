---
title: Install Argo CD on wk-2
summary: Install Argo CD into the `argocd` namespace, pin every component to `wk-2` with a node selector, expose the UI at `https://argocd.homelab.example` via a standard Kubernetes Ingress (not the Traefik IngressRoute CRD — boring is reliable).
---

## Argo CD's job

Argo CD watches a Git repo. When the repo changes, it applies the diff to the cluster. When the cluster drifts (someone `kubectl edit`s a thing), Argo CD pulls it back to match Git. That's the whole product.

```d2
direction: down

git: GitHub: ani2fun/infra {
  shape: cylinder
}

argocd: argocd namespace on wk-2 {
  shape: rectangle
  server: argocd-server\n(UI + API)
  rs: repo-server\nclones git
  app: application-controller\napplies + reconciles
  redis: redis\n(cache)
  cmw: applicationset-controller\ndex-server\nnotifications
}

cluster: rest of cluster {
  shape: rectangle
  ns_apps: apps namespace\n+ workloads
  ns_id: identity\n+ keycloak
  ns_db: databases-prod\n+ postgres
}

git -> argocd.rs: pull main
argocd.rs -> argocd.app: parsed manifests
argocd.app -> cluster: kubectl apply
cluster -> argocd.app: status reports
argocd.app -> argocd.server: live state
argocd.server -> internet: HTTPS UI
```

### Why Argo CD over Flux

The two credible GitOps engines for Kubernetes are **Argo CD** and **Flux v2**. Both pull from Git, both reconcile drift, both have been stable for years. The choice is mostly ergonomics:

| | **Argo CD** | **Flux v2** |
|---|---|---|
| **UI** | First-class web UI at `argocd.homelab.example`. Click into an app, see the live tree, diff against Git, sync manually | CLI-and-CRDs only by default; you bolt on `weave-gitops` or a separate dashboard |
| **Control surface** | Application CRDs + the UI; humans operate by clicking | Source / Kustomization / HelmRelease CRDs; humans operate by `kubectl apply` |
| **App-of-apps pattern** | Native and idiomatic (we use it in the next chapter) | ApplicationSets exist but have a different mental model |
| **Multi-tenancy** | Project-scoped RBAC | Namespace-scoped RBAC |
| **Footprint** | ~7 pods (server, repo, app-controller, dex, redis, etc.) | ~5 pods (source, kustomize, helm, notification, image-automation controllers) |

Both are good. **For a homelab, Argo CD wins on the UI alone** — when something's out of sync at 11 p.m., clicking around a tree is faster than `flux get sources` + `flux logs`. For a multi-team production deployment with strong CLI culture, Flux is equally defensible.

The cluster behind these docs uses Argo CD; this chapter assumes it. Migrating to Flux later is an evening of work — the workload manifests in `deploy/` don't change.

Six components in one namespace; we'll pin all of them to `wk-2`.

## Why pin

Argo CD doesn't *need* to be on a specific node. We pin it so:

- Reboots are predictable. When `wk-2` goes down, Argo CD goes with it; the rest of the cluster (Postgres on `wk-1`, Traefik on `vm-1`, scheduler on `ms-1`) keeps running. We know exactly which workload depends on `wk-2`.
- Resource usage is predictable. Argo CD's repo-server and application-controller burn CPU during sync waves. Keeping them off the node Postgres lives on means database performance isn't affected by Argo CD activity.
- Operational consistency. Every time you debug an Argo CD issue, you SSH to the same machine.

## Install the upstream manifests

Argo CD ships an "all-in-one" manifest that creates the namespace, CRDs, RBAC, and Deployments for every component:

```bash
export KUBECONFIG=~/.kube/homelab.yaml

kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

The `stable` channel is the latest GA. To pin a specific version (recommended for production):

```bash
ARGO_VERSION="v3.3.3"
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/${ARGO_VERSION}/manifests/install.yaml
```

Watch the namespace come up:

```bash
kubectl -n argocd get pods
# argocd-application-controller-0           1/1   Running
# argocd-applicationset-controller-...      1/1   Running
# argocd-dex-server-...                     1/1   Running
# argocd-notifications-controller-...       1/1   Running
# argocd-redis-...                          1/1   Running
# argocd-repo-server-...                    1/1   Running
# argocd-server-...                         1/1   Running
```

Seven pods. Each does a piece of the job listed in the diagram above.

## Pin to wk-2

Patch each Deployment/StatefulSet to add a `nodeSelector`:

```bash
kubectl label node wk-2 workload=argocd --overwrite

# Deployments — six of them
for d in argocd-applicationset-controller argocd-dex-server argocd-notifications-controller \
         argocd-redis argocd-repo-server argocd-server; do
  kubectl -n argocd patch deployment "$d" --type merge \
    -p '{"spec":{"template":{"spec":{"nodeSelector":{"workload":"argocd"}}}}}'
done

# StatefulSet — one
kubectl -n argocd patch statefulset argocd-application-controller --type merge \
  -p '{"spec":{"template":{"spec":{"nodeSelector":{"workload":"argocd"}}}}}'
```

After patching, every pod in `argocd` re-rolls and lands on `wk-2`. Confirm:

```bash
kubectl -n argocd get pods -o wide
# All NODE columns should read wk-2.
```

## Expose the UI via Traefik

Argo CD's `argocd-server` Service listens internally on `:80` (HTTP) and `:443` (its own self-signed TLS). The cleanest pattern: turn off the internal TLS, let Traefik terminate the Let's Encrypt cert, route plain HTTP between Traefik and `argocd-server` over the cluster network.

```bash
# Turn off Argo CD's internal TLS — Traefik handles it
kubectl -n argocd patch configmap argocd-cmd-params-cm --type merge \
  -p '{"data":{"server.insecure":"true"}}'
kubectl -n argocd rollout restart deployment argocd-server
```

Then the Ingress:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-server
  namespace: argocd
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod-dns01
    kubernetes.io/ingress.class: traefik
    traefik.ingress.kubernetes.io/router.entrypoints: websecure
    traefik.ingress.kubernetes.io/router.tls: "true"
spec:
  ingressClassName: traefik
  rules:
    - host: argocd.homelab.example
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: argocd-server
                port:
                  number: 80
  tls:
    - hosts:
        - argocd.homelab.example
      secretName: argocd-homelab-example-tls
```

(Note: both `ingressClassName` *and* the `kubernetes.io/ingress.class` annotation. Some Traefik versions require both; harmless to have both.)

```bash
kubectl apply -f /tmp/argocd-ingress.yaml

# Wait for the cert
kubectl -n argocd get certificate
# argocd-homelab-example-tls   True   ...

# Smoke test
curl -sI https://argocd.homelab.example
# HTTP/2 200
```

## Get the admin password and log in

The first-boot admin password is auto-generated and stored as a Secret:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath='{.data.password}' | base64 -d
# (some random password)
```

Log in at `https://argocd.homelab.example` with username `admin` and that password. **Change the password immediately**, in *User Info* → *Update Password*.

After changing it, delete the bootstrap secret — it's no longer authoritative:

```bash
kubectl -n argocd delete secret argocd-initial-admin-secret
```

## CLI access

The `argocd` CLI is useful for scripting:

```bash
# macOS
brew install argocd

# Login (uses the admin user)
argocd login argocd.homelab.example
# username: admin
# password: <the new one you just set>

# List apps (none yet)
argocd app list
# (empty)
```

## What you should have now

- An `argocd` namespace with all seven Argo CD pods Running on `wk-2`
- The UI reachable at `https://argocd.homelab.example` with a trusted cert
- Admin password changed from the bootstrap value
- The `argocd` CLI logged in from your laptop

Next we'll show Argo CD what to deploy.

→ Next: [The app-of-apps pattern](/cortex/homelab-from-scratch/secrets-and-gitops-the-app-of-apps-pattern)
