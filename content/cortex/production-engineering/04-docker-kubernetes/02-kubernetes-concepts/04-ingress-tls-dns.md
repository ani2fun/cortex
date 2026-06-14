---
title: '8. Ingress, TLS & DNS'
summary: A ClusterIP Service is internal-only. To let the public internet reach cortex.kakde.eu over HTTPS, you need an Ingress (HTTP routing from outside), automatic TLS certificates, and DNS pointing at the cluster.
---

# 8. Ingress, TLS & DNS

## TL;DR

> A Service (Chapter 7) is reachable only *inside* the cluster. To expose your app to the *public internet* over HTTPS, three things cooperate. **Ingress** is a Kubernetes object declaring HTTP(S) routing rules — "host `cortex.kakde.eu` → the `cortex` Service" — implemented by an **Ingress Controller** (Cortex uses **Traefik**) that acts as the cluster's edge reverse proxy. **TLS** certificates are issued *automatically* by **cert-manager** from Let's Encrypt, so HTTPS "just works" and renews itself. **DNS** points `cortex.kakde.eu` at the cluster's edge IP. Together they turn an internal Service into a public, encrypted website.

## 1. Motivation

You have a stable internal address (`cortex` Service) but no way for a person's browser to reach it — it lives inside the cluster's private network. Getting an app *onto the public internet*, *securely*, traditionally meant a pile of fiddly, error-prone work: stand up a reverse proxy (nginx/HAProxy), write its config to route hostnames to backends, obtain a TLS certificate (generate a CSR, validate domain ownership, install it), set up DNS, and — the part everyone forgets — *renew the certificate before it expires* (expired certs are a perennial cause of outages). Each piece is a manual ritual that drifts and breaks.

Kubernetes makes the whole thing *declarative and automatic*. You write an **Ingress** object — a few lines saying "this hostname routes to this Service, with TLS" — and the cluster's controllers do the rest: the **Ingress Controller** programs the edge proxy to route accordingly, and **cert-manager** *watches* your Ingress, automatically requests a Let's Encrypt certificate, proves domain ownership, installs it, and *renews it forever*. You declare *intent* ("serve `cortex.kakde.eu` over HTTPS, routing to the `cortex` Service") and the machinery realizes it — including the perpetual cert-renewal that humans reliably forget. This is the final hop that turns "a Service running in a cluster" into "a real website with a padlock," and it's the layer Part 4 has been building toward.

## 2. Intuition (Analogy)

A **large office building's front entrance.** Inside, departments have internal extensions (Services) you can't dial from outside. To let the public in, the building has a single **main entrance with a security desk** (the Ingress Controller / reverse proxy): visitors arrive there, state who they're visiting ("I'm here for `cortex.kakde.eu`"), and the desk *directs them to the right department*. The building's **street address** is in the public directory so people can find it (DNS). And the entrance has a **verified identity badge** — a notarized certificate proving "this really is the building you think it is" — that a service *automatically renews* before it lapses (TLS via cert-manager).

The **Ingress** object is the *instructions posted at the security desk*: "visitors for `cortex.kakde.eu` → the Cortex department." The desk itself (Traefik) enforces them; the public directory (DNS) gets people to the door; and the auto-renewing badge (cert-manager + Let's Encrypt) proves the building's identity over an encrypted channel. Four pieces, one seamless "the public can securely reach the right internal department."

## 3. Formal Definition

- **Ingress.** A Kubernetes object declaring **HTTP(S) routing rules**: which **host** and **path** map to which **Service** and port. It's just a *declaration* — it does nothing without a controller to implement it.
- **Ingress Controller.** The component that *implements* Ingress objects by acting as the cluster's **edge reverse proxy / load balancer** — watching Ingress objects and configuring itself to route accordingly. Cortex uses **Traefik** (others: nginx-ingress, HAProxy). It's the actual front door traffic flows through.
- **TLS (HTTPS).** Encryption + identity for web traffic. The Ingress references a **Secret** holding the certificate + key; the controller terminates TLS at the edge.
- **cert-manager.** A controller that *automates* certificate lifecycle: it watches Ingresses (and `Certificate` objects), requests certs from an issuer (**Let's Encrypt** via ACME), proves domain ownership (HTTP-01 or **DNS-01** challenge), stores the cert in a Secret, and **auto-renews** before expiry. Set it up once; certs never expire on you again.
- **ClusterIssuer.** A cert-manager object naming *where* certs come from (e.g. `letsencrypt-prod-dns01`) — referenced from an Ingress annotation.
- **DNS.** Maps the human hostname (`cortex.kakde.eu`) to the cluster edge's public IP, so browsers know where to send the request. (Managed at your DNS provider, e.g. Cloudflare in [Homelab from Scratch](/cortex/homelab-from-scratch/domain-and-dns/move-dns-to-cloudflare).)
- **The full path:** browser → DNS resolves the host → request hits the cluster edge → **Ingress Controller** matches the host/path rule → routes to the **Service** → load-balances to a ready **Pod** (Chapters 6–7).

> Ingress declares HTTP(S) routing (host → Service); an Ingress Controller (Traefik) implements it as the edge proxy; cert-manager auto-issues and renews TLS certs from Let's Encrypt; DNS points the hostname at the edge. Declarative, automatic, encrypted public access.

## 4. Worked Example — Cortex's Ingress

Cortex's [`ingress.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/ingress.yaml) is the entire "make it a public HTTPS site" declaration:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: cortex
  annotations:
    kubernetes.io/ingress.class: traefik
    traefik.ingress.kubernetes.io/router.tls: "true"
    cert-manager.io/cluster-issuer: letsencrypt-prod-dns01   # ← cert-manager auto-issues the cert
spec:
  ingressClassName: traefik                                  # ← implemented by Traefik
  tls:
    - hosts: [cortex.kakde.eu]
      secretName: cortex-kakde-eu-tls                        # ← cert-manager writes the cert here
  rules:
    - host: cortex.kakde.eu                                  # ← public hostname (DNS points here)
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: cortex                                 # ← routes to the Chapter 7 Service...
                port:
                  number: 80                                 # ...on its port 80
```

Trace it end to end. A browser asks DNS for `cortex.kakde.eu`; DNS returns the cluster edge's IP. The request arrives at **Traefik** (the Ingress Controller, `ingressClassName: traefik`). Traefik reads this Ingress's rule — host `cortex.kakde.eu`, path `/` → Service `cortex:80` — and forwards the request to the `cortex` Service, which load-balances to a ready Cortex Pod (Chapter 7). Meanwhile, the `cert-manager.io/cluster-issuer` annotation told **cert-manager** to obtain a Let's Encrypt certificate for `cortex.kakde.eu` (proving ownership via a DNS-01 challenge), store it in the `cortex-kakde-eu-tls` Secret, and *keep renewing it* — so Traefik terminates HTTPS with a valid, auto-renewing cert. The reader sees `https://cortex.kakde.eu` with a padlock, served by an internal Pod they can't address directly. That entire production-grade setup — public DNS, HTTPS, auto-renewing certs, edge routing, internal load-balancing — is *declared* in this one short file. (Standing up Traefik and cert-manager themselves is cluster setup, covered in [Homelab from Scratch → The Edge](/cortex/homelab-from-scratch/the-edge/tls-on-autopilot).)

## 5. Build It

Run this. It models the request path from public hostname to internal Pod, and the cert-manager auto-renewal that keeps HTTPS valid.

```python run
# DNS: hostname -> cluster edge IP.
DNS = {"cortex.kakde.eu": "203.0.113.10"}     # the edge / Ingress Controller IP

# Ingress rules: (host, path-prefix) -> (service, port).
INGRESS = [("cortex.kakde.eu", "/", ("cortex", 80))]

# Service -> ready pods (from Chapter 7).
SERVICES = {"cortex": ["10.1.0.5:8080", "10.1.0.6:8080"]}

import itertools
_rr = itertools.count()

def request(url):
    host, path = url.replace("https://", "").split("/", 1); path = "/" + path
    # 1. DNS
    edge_ip = DNS.get(host)
    if not edge_ip: return f"DNS failure: {host} not found"
    print(f"   DNS: {host} -> {edge_ip} (cluster edge)")
    # 2. Ingress Controller matches host + path -> Service
    match = next((svc for (h, p, svc) in INGRESS if h == host and path.startswith(p)), None)
    if not match: return f"404: no ingress rule for {host}{path}"
    svc, port = match
    print(f"   Ingress (Traefik): {host}{path} -> Service {svc}:{port}")
    # 3. Service load-balances to a ready Pod
    pods = SERVICES[svc]
    pod = pods[next(_rr) % len(pods)]
    return f"   Service {svc} -> Pod {pod}  =>  200 OK (served by an internal Pod)"

print("GET https://cortex.kakde.eu/cortex/production-engineering")
print(request("https://cortex.kakde.eu/cortex/production-engineering"))

# cert-manager: certs auto-renew before expiry (no human, no outage).
def cert_days_left(issued_day, now_day, lifetime=90, renew_before=30):
    left = lifetime - (now_day - issued_day)
    if left <= renew_before:
        print(f"\n   cert has {left}d left (<= {renew_before}d) -> cert-manager RENEWS automatically")
        return 90
    return left
cert_days_left(issued_day=0, now_day=65)      # 25 days left -> auto-renew
```

**Now break it.** Remove `"cortex.kakde.eu"` from `DNS` and re-request — you get a DNS failure, because *no Ingress rule matters if the hostname doesn't resolve to the cluster*. DNS, Ingress, TLS, and the Service are a *chain*; break any link and the site is unreachable. Then comment out the `cert_days_left` auto-renew and imagine 90 days passing — without cert-manager, the certificate expires and every visitor gets a scary browser warning (a classic real-world outage). The whole point of this stack is that *each link is declared once and maintained automatically* — especially the cert renewal humans always forget.

## 6. Trade-offs & Complexity

| Ingress + cert-manager (declarative) | Manual reverse proxy + certs |
|---|---|
| Routing declared in one object | Hand-written proxy config |
| TLS auto-issued AND auto-renewed | Manual cert requests + renewals (expire!) |
| Add a host = add a few lines | Reconfigure and reload the proxy |
| Standard, portable across clusters | Bespoke, snowflake setup |
| Controllers to install + understand | Familiar but fragile |

The cost is upfront cluster setup — you must install and configure an Ingress Controller (Traefik) and cert-manager, plus get DNS pointing correctly, which is genuinely involved (an entire section of the Homelab book). And there are sharp edges: DNS propagation delays, ACME challenge failures, path-vs-host routing subtleties. But once it's running, exposing a *new* app is trivial (a small Ingress object), and the most failure-prone part of public web serving — TLS certificate renewal — becomes invisible and automatic. For anything serving real users over HTTPS, this stack is the standard, and the alternative (hand-managed proxies and certs) is strictly worse at scale.

## 7. Edge Cases & Failure Modes

- **Expired/missing certificate.** If cert-manager can't complete its challenge (DNS misconfigured, rate-limited by Let's Encrypt), no valid cert is issued and visitors get TLS errors. Check cert-manager's `Certificate`/`Order` status when HTTPS breaks.
- **DNS not pointing at the edge.** The Ingress is perfect but the hostname resolves elsewhere (or nowhere) → unreachable. DNS is a separate system you must configure to point at the cluster's edge IP.
- **No Ingress Controller installed.** An Ingress object with no controller to implement it does *nothing* — a common "I created an Ingress but it doesn't work" confusion. The controller (Traefik) is what makes Ingress real.
- **Host/path rule mismatch.** A typo'd host or wrong `pathType` routes to a 404 or the wrong backend. The Ingress is precise; small errors silently misroute.

## 8. Practice

> **Exercise 1 — Trace the hop.** List, in order, every component a request to `https://cortex.kakde.eu` passes through, from DNS to the container.

<details>
<summary><strong>Answer</strong></summary>

The full path is a *chain* — break any link and the site is unreachable (§3, §5). In order:

1. **DNS.** The browser asks DNS for `cortex.kakde.eu`; it resolves to the **cluster edge's public IP** (the address you configured at your DNS provider to point at the cluster).
2. **The cluster edge → Ingress Controller (Traefik).** The request arrives at the edge, where Traefik — the reverse proxy implementing your Ingress objects — receives it and **terminates TLS** using the auto-issued certificate (so the encrypted HTTPS connection ends here, with a valid padlock).
3. **Ingress rule match.** Traefik reads the **Ingress** object's rule — host `cortex.kakde.eu`, path `/` — and resolves it to the backend: Service `cortex:80`.
4. **Service.** The `cortex` **ClusterIP Service** (Chapter 7) receives the request and **load-balances** it across its ready endpoints.
5. **Pod / container.** The Service forwards to a **ready Pod**'s container port (`8080`), where Cortex actually serves the response.

So: **browser → DNS → cluster edge (Traefik, TLS terminated) → Ingress rule → Service → ready Pod's container.** Each layer is declared once and maintained automatically; the reader just sees `https://cortex.kakde.eu` with a padlock, served by an internal Pod they can't address directly.

</details>

> **Exercise 2 — Who renews the cert?** Explain what cert-manager does and why automatic renewal matters. What outage does it prevent?

<details>
<summary><strong>Answer</strong></summary>

**What cert-manager does:** it's a controller that *automates the entire TLS certificate lifecycle* (§3). It **watches** your Ingresses (and `Certificate` objects), **requests** a certificate from an issuer — **Let's Encrypt** via ACME, named by the `ClusterIssuer` (e.g. `letsencrypt-prod-dns01`) — **proves domain ownership** (an HTTP-01 or DNS-01 challenge), **stores** the issued cert + key in a Kubernetes **Secret** (here `cortex-kakde-eu-tls`), and then **auto-renews** it before it expires. You set it up once and reference it with one annotation; you never touch a cert again.

**Why automatic renewal matters / what outage it prevents:** Let's Encrypt certs are short-lived (≈90 days), and a certificate that lapses causes every visitor's browser to show a **scary TLS/security warning** — effectively taking the site down for normal users. Expired certificates are a *classic, perennial real-world outage* precisely because manual renewal is the step humans reliably forget (§1, §5). cert-manager makes that failure mode *impossible*: it renews well ahead of expiry (e.g. 30 days out), with no human and no downtime — turning the most failure-prone part of public HTTPS into something invisible.

</details>

> **Exercise 3 — Add a subdomain.** You want `blog.kakde.eu` to serve a different app. What do you change (Ingress, DNS, cert) and what stays the same (the controller, cert-manager)?

<details>
<summary><strong>Answer</strong></summary>

The whole design's payoff is that the *machinery* is installed once and only a small *declaration* changes per app (§6).

**What you add/change:**

- **DNS.** Add a record for `blog.kakde.eu` pointing at the *same* cluster edge IP (the new hostname must resolve to the cluster, or no Ingress rule matters — §7).
- **An Ingress object** for the blog: host `blog.kakde.eu`, path `/` → the blog's **Service** and port, with the same `cert-manager.io/cluster-issuer` annotation and a `tls:` block naming a new Secret (e.g. `blog-kakde-eu-tls`). A few lines.
- **A certificate** for `blog.kakde.eu` — but you don't do this *by hand*: cert-manager sees the new Ingress, completes the ACME challenge, and issues + stores the new cert automatically. So "the cert" is really just the Secret name you reference; cert-manager fills it.
- (And of course the **blog app itself** — its Deployment and Service.)

**What stays the same (installed once, reused):**

- **The Ingress Controller (Traefik)** — the edge proxy is already running; it just picks up the new Ingress rule.
- **cert-manager and the `ClusterIssuer`** — already configured; they handle the new cert with no extra setup.

That asymmetry is the point: standing up the controller + cert-manager is the one-time hard part; *exposing each additional app* is then trivial — a DNS record and a short Ingress, with TLS issued and renewed for you.

</details>

```quiz
{
  "prompt": "How does the public internet reach Cortex at https://cortex.kakde.eu, given the app's Service is internal (ClusterIP)?",
  "input": "Choose one:",
  "options": [
    "An Ingress declares the host→Service routing, an Ingress Controller (Traefik) implements it as the edge proxy, cert-manager auto-issues+renews the TLS cert (Let's Encrypt), and DNS points the hostname at the cluster edge",
    "The ClusterIP Service is directly exposed to the internet",
    "Each Pod gets a public IP and DNS name",
    "Kubernetes emails the page to the user"
  ],
  "answer": "An Ingress declares the host→Service routing, an Ingress Controller (Traefik) implements it as the edge proxy, cert-manager auto-issues+renews the TLS cert (Let's Encrypt), and DNS points the hostname at the cluster edge"
}
```

## In the Wild

- **[Kubernetes — Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/)** & **[Ingress Controllers](https://kubernetes.io/docs/concepts/services-networking/ingress-controllers/)** — the object and what implements it.
- **[cert-manager docs](https://cert-manager.io/docs/)** & **[Traefik & Kubernetes](https://doc.traefik.io/traefik/providers/kubernetes-ingress/)** — automatic TLS and the edge controller Cortex uses.
- **[Cortex `ingress.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/ingress.yaml)** + **[Homelab from Scratch — TLS on autopilot](/cortex/homelab-from-scratch/the-edge/tls-on-autopilot)** — the real Ingress and the cluster edge that serves it.

---

**Next:** the Deployment, Service, and Ingress all need *configuration* — database URLs, the Keycloak issuer, secrets like the DB password. Kubernetes has dedicated objects for config and secrets. → [9. Config and Secrets](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/config-and-secrets)
