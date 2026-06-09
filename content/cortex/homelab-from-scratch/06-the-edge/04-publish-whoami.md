---
title: Publish whoami
summary: Three small YAMLs — Deployment, Service, Ingress — and `https://whoami.homelab.example` returns a JSON envelope showing your request reached the cluster, the cert is valid, and TLS terminated where you wanted it to. The "first deploy" moment.
---

## What `whoami` is

A tiny Go binary from the Traefik project. It listens on `:80` and returns one HTTP response per request: a plain-text dump of the request headers, the receiving hostname, the client IP. **3 MB image.** No database, no state, no dependencies.

It's the simplest thing that exercises the full ingress path: DNS → edge → Traefik → Service → endpoint → pod. If whoami works, the pipeline is correct, and every other app you deploy from here on follows the same shape.

## The request flow

```d2
direction: down

browser: Browser
dns: DNS\nhomelab.example zone\nat Cloudflare
edge: vm-1 (edge)\n198.51.100.25 {
  shape: rectangle
  traefik: Traefik :443
  cert: Cert from\ncert-manager
}
mesh: WireGuard mesh
worker: wk-1 or wk-2 {
  shape: rectangle
  service: Service whoami\n10.43.x.x:80
  pod: Pod whoami\n10.42.x.x:80
}

browser -> dns: A whoami.homelab.example?
dns -> browser: 198.51.100.25
browser -> edge.traefik: TLS to whoami.homelab.example
edge.cert -> edge.traefik: cert for whoami.homelab.example
edge.traefik -> mesh: cluster network
mesh -> worker.service: ClusterIP routing
worker.service -> worker.pod: kube-proxy
worker.pod -> browser: 200 + headers dump
```

Six hops, each in a different layer of the stack. When something doesn't work, this is the diagram you debug against.

## The three manifests

A namespace, a Deployment, a Service, an Ingress. Apply in order.

### Namespace

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: apps
```

The `apps` namespace is where every workload lives. Nothing fancy yet — labels and policies come later.

### Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: whoami
  namespace: apps
spec:
  replicas: 1
  selector:
    matchLabels:
      app: whoami
  template:
    metadata:
      labels:
        app: whoami
    spec:
      containers:
        - name: whoami
          image: traefik/whoami:latest
          ports:
            - name: http
              containerPort: 80
```

One replica, no resource requests, no probes. We're keeping the smallest YAML that works. (For a real workload you'd add liveness/readiness probes and resource requests; for whoami, the defaults are fine.)

### Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: whoami
  namespace: apps
spec:
  selector:
    app: whoami
  ports:
    - name: http
      port: 80
      targetPort: 80
```

A `ClusterIP` Service (the default type when `type` isn't specified). `kube-proxy` makes this address reachable from any pod in the cluster. Traefik will dial this address rather than the pod IP directly — that way, scaling whoami to 5 replicas later doesn't require any Ingress edits.

### Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: whoami
  namespace: apps
  annotations:
    traefik.ingress.kubernetes.io/router.entrypoints: websecure
    traefik.ingress.kubernetes.io/router.tls: "true"
    cert-manager.io/cluster-issuer: letsencrypt-prod-dns01
spec:
  ingressClassName: traefik
  rules:
    - host: whoami.homelab.example
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: whoami
                port:
                  number: 80
  tls:
    - hosts:
        - whoami.homelab.example
      secretName: whoami-homelab-example-tls
```

This is the document worth understanding line by line.

| Field | What it does |
|---|---|
| `ingressClassName: traefik` | Tells the Kubernetes ingress system "Traefik is the controller for this Ingress." Without this, the ingress is created but no controller picks it up. |
| `traefik.ingress.kubernetes.io/router.entrypoints: websecure` | Traefik-specific hint: only attach this rule to the `:443` entrypoint (not `:80`). The HTTP→HTTPS redirect we set up earlier already covers `:80`. |
| `traefik.ingress.kubernetes.io/router.tls: "true"` | Tell Traefik to terminate TLS for this rule. (Implied by `ingress.tls`, but explicit beats implicit when both syntaxes exist.) |
| `cert-manager.io/cluster-issuer: letsencrypt-prod-dns01` | The annotation that triggers cert-manager. cert-manager watches Ingresses for this annotation, creates a `Certificate` resource for the hostnames in `spec.tls`, issues the cert, writes the Secret. |
| `spec.tls.secretName: whoami-homelab-example-tls` | The name of the Secret cert-manager will write the cert into. Traefik mounts this Secret automatically. |
| `spec.rules.host: whoami.homelab.example` | The hostname this rule matches. Traefik compares the SNI on the TLS handshake; if it matches, this Ingress' rule wins. |
| `spec.rules.paths.backend.service` | The Service to forward to. Traefik resolves the Service's endpoints in real-time. |

Apply all three:

```bash
kubectl apply -f /tmp/whoami-namespace.yaml
kubectl apply -f /tmp/whoami-deployment.yaml
kubectl apply -f /tmp/whoami-service.yaml
kubectl apply -f /tmp/whoami-ingress.yaml
```

## Watch the cert get issued

```bash
# Watch the Certificate get created and issued
kubectl get certificate -n apps -w
# NAME                       READY   SECRET                          AGE
# whoami-homelab-example     False   whoami-homelab-example-tls      5s
# whoami-homelab-example     False   whoami-homelab-example-tls      30s
# whoami-homelab-example     True    whoami-homelab-example-tls      55s
```

(If you don't see a Certificate appearing, it means cert-manager isn't watching the Ingress class — `kubectl get clusterissuers` should show ready.)

`Ready: True` means the cert is issued and the Secret has been written. Traefik picks up the Secret automatically (because it watches kube secrets across namespaces by default).

## The smoke test

From your laptop:

```bash
# 1. DNS resolves to the edge
dig whoami.homelab.example +short
# 198.51.100.25

# 2. HTTPS responds, with a trusted cert
curl -sS -o /dev/null -w '%{http_code} %{ssl_verify_result}\n' https://whoami.homelab.example
# 200 0

# 3. The body is whoami's plain-text response
curl -sS https://whoami.homelab.example
# Hostname: whoami-7d8...
# IP: 127.0.0.1
# IP: 10.42.2.5
# RemoteAddr: 10.42.0.1:51234
# GET / HTTP/1.1
# Host: whoami.homelab.example
# User-Agent: curl/8.6.0
# Accept: */*
# Accept-Encoding: gzip
# X-Forwarded-For: <your-laptop-ip>
# X-Forwarded-Host: whoami.homelab.example
# X-Forwarded-Port: 443
# X-Forwarded-Proto: https
# X-Real-Ip: <your-laptop-ip>
```

The `X-Forwarded-Proto: https` confirms TLS terminated at Traefik, not at whoami. The `RemoteAddr` (a pod IP, `10.42.0.1`) is `vm-1`'s side of the cluster network — proof the request went through Traefik and was forwarded over the cluster network to the whoami pod on a different node.

If the second curl returned `200 0`, you've built a complete homelab ingress pipeline. The `0` means OpenSSL verified the cert against the system trust store — i.e., it's a real trusted cert.

## What if it doesn't work?

A debug ladder, in order:

| Symptom | Likely cause |
|---|---|
| `dig` returns nothing | The wildcard A record at Cloudflare isn't there yet, or proxy mode is on. Check the Cloudflare DNS panel. |
| `curl` hangs | Edge firewall isn't allowing `:443`. Re-run `nft list table inet edge_guardrail`. |
| `curl` returns `404 Not Found, server: traefik` | Traefik is up but no Ingress matched. Check `kubectl get ingress -A` and that `host` matches the hostname. |
| `curl --insecure` works but `curl` fails with cert error | The cert isn't issued yet, or it's a staging cert. `kubectl describe certificate whoami-... -n apps`. |
| 504 / 502 | The Service has no endpoints. `kubectl get endpoints whoami -n apps` should list the pod IP. |

That covers ~95% of first-deploy failures.

## What you should have now

- A `whoami` namespace and Deployment/Service/Ingress in it
- One pod Running on `wk-1` or `wk-2`
- A Certificate `whoami-homelab-example` with `Ready: True`
- `https://whoami.homelab.example` returns a 200 with a trusted cert and the whoami JSON envelope

Every app you deploy from here on follows this exact pattern: namespace, Deployment, Service, Ingress. The next section automates the deployment side via GitOps.

→ Next: [Sealed Secrets](/cortex/homelab-from-scratch/secrets-and-gitops/sealed-secrets)
