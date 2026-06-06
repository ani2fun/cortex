---
title: Pin Traefik to the edge
summary: Traefik with `hostNetwork: true` so it binds the edge node's :80 and :443 directly. The toleration that lets it past the edge taint, the `Recreate` strategy that fixes the surge-pod deadlock you'd otherwise hit on every rollout.
---

## Why Traefik specifically

Three credible options for "the ingress controller in front of everything": **NGINX Ingress**, **Traefik**, **Envoy/Contour**.

Traefik wins on this homelab for two prosaic reasons:

1. **Single binary, single container.** Traefik is one Go binary that handles ingress, TLS termination, basic load balancing, file-based and CRD-based config, and request logging. NGINX Ingress is a wrapper around NGINX (more layers; the wrapper occasionally lags). Envoy/Contour is two containers (Envoy + the control plane).
2. **Idiomatic with Kubernetes Ingress *and* its own CRDs.** Most apps in this book use the standard `Ingress` resource — boring, well-known, portable. The handful that need fancy routing can drop into Traefik's `IngressRoute` CRD without changing controllers.

If you're starting today and have no preferences, NGINX Ingress is also fine; the chapter would only change in three lines. Traefik is what the cluster behind these docs runs.

## The pinning trick

```d2
direction: down

internet: Internet :443

vm1: vm-1 — kakde.eu/edge=true:NoSchedule {
  shape: rectangle

  hn: hostNetwork: true {
    shape: rectangle
    p80: :80 (host)
    p443: :443 (host)
  }

  pod: Traefik pod {
    shape: rectangle
    p80c: :80 (container)
    p443c: :443 (container)
    args: --providers.kubernetesingress=true
  }
}

internet -> vm1.hn.p443: incoming TLS
vm1.hn.p443 -> vm1.pod.p443c: same byte stream\n(no kube-proxy NAT)

others: ms-1 / wk-1 / wk-2 — no toleration {
  shape: rectangle
}

vm1.pod -> others: forwards traffic to backend Services\nvia cluster network
```

Three things make this work:

1. **`hostNetwork: true`** — the Pod shares the host's network namespace. So Traefik binds to *the edge VM's* `:80` and `:443`, exactly like a normal systemd-managed nginx would.
2. **`nodeSelector: kakde.eu/edge: "true"`** — the Pod will only schedule on a node with that label.
3. **`tolerations: kakde.eu/edge=true:NoSchedule`** — and it ignores the `NoSchedule` taint we put on the edge node in the previous chapter. So nothing else schedules there, but Traefik can.

Result: exactly one Traefik pod, exactly on `vm-1`, exactly on the host's `:443`.

## The deployment

The relevant excerpt from the manifest the cluster runs:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: traefik
  namespace: traefik
spec:
  replicas: 1
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 0          # ← critical, see below
      maxUnavailable: 1
  template:
    spec:
      hostNetwork: true
      dnsPolicy: ClusterFirstWithHostNet
      nodeSelector:
        kakde.eu/edge: "true"
      tolerations:
        - key: kakde.eu/edge
          operator: Equal
          value: "true"
          effect: NoSchedule
      containers:
        - name: traefik
          image: traefik:v2.11
          args:
            - --providers.kubernetesingress=true
            - --providers.kubernetesingress.ingressclass=traefik
            - --providers.kubernetescrd=true
            - --entrypoints.web.address=:80
            - --entrypoints.websecure.address=:443
            - --entrypoints.web.http.redirections.entrypoint.to=websecure
            - --entrypoints.web.http.redirections.entrypoint.scheme=https
            - --providers.kubernetesingress.ingressendpoint.ip=198.51.100.25
          ports:
            - name: web
              containerPort: 80
            - name: websecure
              containerPort: 443
          securityContext:
            allowPrivilegeEscalation: false
            capabilities:
              drop: [ALL]
              add: [NET_BIND_SERVICE]
```

Three things worth highlighting:

- **`dnsPolicy: ClusterFirstWithHostNet`**. With `hostNetwork: true`, the default `ClusterFirst` DNS policy doesn't work — the pod is on the host network, where the cluster's CoreDNS isn't reachable by name. `ClusterFirstWithHostNet` makes DNS work as if the pod were on the cluster network, which is what we want.
- **The HTTP→HTTPS redirect** — two args turn HTTP requests on `:80` into 301s pointing at `:443`. Boring, what every site does.
- **`NET_BIND_SERVICE` capability** — required to let an unprivileged container bind to a port < 1024.

## The footgun: surge-pod deadlock

The first time you `kubectl apply` an updated Deployment, Kubernetes wants to run the *new* Pod first (so there's no downtime), then drain the *old* Pod. With Traefik on `hostNetwork: true`, that doesn't work: the new Pod can't bind to `:443` because the old Pod is already there.

Symptom: a rollout that hangs forever, with `kubectl describe deploy traefik` showing the new ReplicaSet stuck at "0/1 ready" and the old one at "1/1 ready", forever.

The fix is the line `maxSurge: 0`. With `maxSurge: 0` and `maxUnavailable: 1`, Kubernetes deletes the old pod *first*, then creates the new one. There's a ~5-second window of downtime per rollout, but rollouts complete cleanly.

Alternative: `strategy.type: Recreate` does the same thing (delete old, create new) and is arguably more obvious. Either works.

## Apply and watch

```bash
# Create the namespace + the manifests
kubectl apply -f https://raw.githubusercontent.com/<your-fork>/infra/main/k8s-cluster/platform/traefik/

# Watch the pod come up on the edge
kubectl -n traefik get pods -o wide -w
# traefik-7f8...   0/1   Pending           ...
# traefik-7f8...   0/1   ContainerCreating ...
# traefik-7f8...   1/1   Running           ctb-edge-1
```

If it stays `Pending`, `kubectl describe pod` will say why. The two most common reasons:

1. **No node with the right label.** `kubectl get nodes -L kakde.eu/edge` should show `true` next to your edge node.
2. **The taint mismatch.** Check the toleration block matches the taint exactly (key, value, effect — including the `Equal` operator).

## Confirm Traefik is bound

From your laptop, with `vm-1`'s public IP:

```bash
curl -sI http://198.51.100.25
# HTTP/1.1 308 Permanent Redirect
# Location: https://198.51.100.25/
```

The redirect tells you Traefik has bound `:80` and is serving the redirect-to-HTTPS rule. 308 (not 301) is correct — Traefik uses 308 to preserve the request method on redirect.

```bash
curl -kI https://198.51.100.25
# HTTP/2 404
# server: traefik
```

A 404 with `server: traefik` means HTTPS terminated at Traefik (using its self-signed default cert) and there's no Ingress matching this hostname yet. That's exactly right: we haven't created any Ingresses. Adding `whoami` in three chapters will turn the 404 into a 200.

## What you should have now

- A `traefik` namespace
- One Pod called `traefik-<hash>` on `ctb-edge-1`, status `Running`
- `curl http://198.51.100.25` redirects to `https://`
- `curl -k https://198.51.100.25` returns a 404 with `server: traefik`
- Other workers and `ms-1` are *not* running Traefik (only the edge is)

The next chapter locks down the edge so Traefik is *only* what's reachable from the public internet.

→ Next: [Router and edge firewall](/cortex/homelab-from-scratch/the-edge-router-and-edge-firewall)
