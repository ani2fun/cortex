---
title: '20. Identity-aware rate limiting'
summary: The same identity that authorizes a request also decides how it's metered. Anonymous callers share a per-IP budget; signed-in users get their own per-user budget. See how the verified identity routes straight into the rate limiter.
---

# 20. Identity-aware rate limiting

## TL;DR

> Running code costs CPU, so Cortex meters it. The verified identity from Chapter 19 decides *which bucket* a request spends from: **anonymous callers are limited per source IP**; **signed-in users are limited per user**. The limiter is a Redis-backed token-bucket keyed by either `ip:<addr>` or `user:<sub>`. This is the *reward* half of ADR-0013's gate — sign in and you get your own, more generous budget instead of sharing one with everyone behind your IP.

## 1. Motivation

Any endpoint that does real work for anonymous strangers is a free-resource faucet, and faucets get abused. Cortex's `/api/run` ships code to a sandbox that burns CPU — leave it unmetered and one script can starve everyone. So it's rate-limited.

But *how you key the limit* is itself an identity decision, and it closes the ADR-0013 loop. Anonymous users can't be told apart except by IP — a coarse, shared signal (everyone behind a school's NAT looks like one caller). Signed-in users have a *stable identity* (`sub`), so they can each get their *own* budget. That asymmetry is both a security measure (anonymous abuse is contained to an IP) and a gentle incentive (sign in → stop sharing a budget with strangers). Identity doesn't just authorize; it meters.

## 2. Intuition (Analogy)

A coffee shop with free refills.

- **Anonymous:** there's *one* refill pitcher per *table*. Everyone who sits at table 7 (your IP) shares it. If a stranger at your table guzzles it, you're dry — coarse, shared, and a little unfair, but it stops any single table from draining the kitchen.
- **Members (signed in):** each member gets their *own* named cup with its *own* refill allowance, tracked by their membership id (`sub`), no matter which table they sit at. Their usage is theirs alone.

Becoming a member doesn't just unlock the espresso machine (editing/submitting) — it also gets you off the shared table pitcher. That's exactly Cortex: signing in moves you from a shared per-IP bucket to a private per-user one.

## 3. Formal Definition

- **Token bucket** — a classic rate-limit algorithm: a bucket holds N tokens; each request spends one; tokens refill over time. Empty bucket → request is throttled (HTTP 429). Cortex implements a **fixed-window** variant in **Redis** (so the limit is shared across all server replicas — any pod sees the same count).
- **Bucket key** — *what* the limit is counted against. Cortex chooses the key from the verified identity:
  - **Anonymous** → `ip:<client-ip>` (from the `X-Real-IP` header at the edge).
  - **Signed in** → `user:<sub>` (the stable subject id).
- **The fork** — `auth.verifyOptional(token)` returns `Some(claims)` or `None`; that result selects the key. So the *same* verification that authorizes the request also meters it.
- **Disabled auth** — when `AUTH_ENABLED=false`, the limiter is a **no-op** (dev convenience, Chapter 19).

## 4. Worked Example — the fork in action

```d2
direction: right

req: "POST /api/run\nAuthorization: Bearer? " {shape: rectangle}
verify: "auth.verifyOptional(token)" {shape: hexagon}
anon: "None (anonymous)" {shape: rectangle}
user: "Some(claims)" {shape: rectangle}
ipb: "Redis bucket\nkey = ip:1.2.3.4" {shape: cylinder}
userb: "Redis bucket\nkey = user:f1c2" {shape: cylinder}
ok: "allowed / 429" {shape: oval}

req -> verify
verify -> anon: "no / no token"
verify -> user: "valid token"
anon -> ipb: "spend 1 (shared per IP)"
user -> userb: "spend 1 (private per user)"
ipb -> ok
userb -> ok
```

The client side cooperates: recall from Chapter 18 that `ApiClient.runCode` attaches `Authorization: Bearer <token>` *only when signed in*. So an anonymous browser sends no token → server's `verifyOptional` returns `None` → `ip:` bucket; a signed-in browser sends the token → `Some(claims)` → `user:<sub>` bucket. The whole per-IP-vs-per-user behavior emerges from that one header decision meeting that one verification fork. As the per-user budget runs low, the SPA even slides in an hourly-quota notice under the code block (ADR-0013).

## 5. Build It

Run this. It's a token-bucket limiter with the identity fork — watch anonymous callers behind one IP share a budget while signed-in users each get their own.

```python run
class Limiter:
    def __init__(self, capacity):
        self.capacity = capacity
        self.used = {}                      # key -> count (a fixed window)

    def key_for(self, identity, ip):
        # THE FORK: identity decides the bucket.
        return f"user:{identity}" if identity else f"ip:{ip}"

    def allow(self, identity, ip):
        k = self.key_for(identity, ip)
        self.used[k] = self.used.get(k, 0) + 1
        ok = self.used[k] <= self.capacity
        return ok, k, self.used[k]

lim = Limiter(capacity=3)

# Two ANONYMOUS users behind the SAME office IP share one bucket:
for who in ["anonA", "anonB", "anonA", "anonB"]:
    ok, k, n = lim.allow(identity=None, ip="203.0.113.9")
    print(f"anon {who}: {'OK ' if ok else '429'} bucket={k} count={n}")

print()
# A SIGNED-IN user gets their OWN bucket, unaffected by the shared IP traffic:
for i in range(4):
    ok, k, n = lim.allow(identity="f1c2-ada", ip="203.0.113.9")
    print(f"user ada : {'OK ' if ok else '429'} bucket={k} count={n}")
```

**Now break it.** Notice the two anonymous users hit **429** fast — they're sharing the `ip:203.0.113.9` bucket (4 requests > capacity 3). The signed-in user has a *separate* `user:f1c2-ada` bucket. Now change the signed-in calls to pass `identity=None` (simulate the client forgetting to attach the token). Suddenly the user is dumped back into the shared IP bucket and throttled alongside strangers — which is exactly what happens if `ApiClient.runCode` *doesn't* send the bearer header. The header and the fork must agree.

## 6. Trade-offs & Complexity

| Per-IP (anonymous) | Per-user (signed in) |
|---|---|
| No identity needed | Needs a verified `sub` |
| Coarse — NAT shares a budget | Precise — one budget per person |
| Easy to evade (rotate IPs) | Hard to evade (needs an account) |
| Right for untrusted anonymous traffic | Right for accountable users |

Using *both*, keyed by identity, gives the best of each: anonymous abuse is contained per-IP (accepting NAT unfairness as the price of openness), while honest signed-in users get fair, private budgets. The cost is a Redis dependency and the small machinery to choose a key — trivial next to the abuse it prevents. (Redis is shared across replicas so the limit holds cluster-wide — the same stateless-scale theme as Chapter 11.)

## 7. Edge Cases & Failure Modes

- **Trusting a spoofable client IP.** The limiter keys on `X-Real-IP` set by the *trusted edge proxy*. If an untrusted client could set that header, it could forge IPs to dodge limits. Only trust it from your own ingress.
- **NAT collateral damage.** A whole office behind one IP shares the anonymous budget; one heavy user throttles the rest. The fix is exactly the gate: sign in for a per-user budget.
- **Redis down.** Decide fail-open (allow, lose limiting) or fail-closed (deny, lose availability) deliberately. Either way, surface it.
- **Forgetting the header.** If the client omits the bearer token when signed in, the user silently falls back to the per-IP bucket — a confusing "why am I being throttled?" bug. Client and server must agree.

## 8. Practice

> **Exercise 1 — Pick the key.** For each request, state the bucket key: (a) anonymous from `1.2.3.4`; (b) signed-in user `sub=abcd` from `1.2.3.4`; (c) two anonymous users behind the same office NAT.

<details>
<summary><strong>Answer</strong></summary>

Apply the fork from §3 — `verifyOptional` returns `None` → key on IP; `Some(claims)` → key on `sub`:

- **(a) anonymous from `1.2.3.4`** → **`ip:1.2.3.4`**. No identity, so the only signal is the source IP (read from the trusted edge's `X-Real-IP`).
- **(b) signed-in `sub=abcd` from `1.2.3.4`** → **`user:abcd`**. A stable identity exists, so the IP is *ignored* — this user's budget is private and follows them across IPs.
- **(c) two anonymous users behind the same office NAT** → **both get `ip:<that-one-NAT-address>`** — i.e. they **share a single bucket**. This is the NAT collateral-damage case: one heavy user can throttle the other, and the fix is precisely the gate — sign in to move from the shared `ip:` bucket to a private `user:` one.

The rule in one line: signed-in keys on `sub` and ignores IP; anonymous keys on IP, and everyone sharing an IP shares the budget.

</details>

> **Exercise 2 — Close the loop.** Connect Chapter 18's `runCode` header logic to this chapter's fork. What single client-side decision determines which bucket a request lands in?

<details>
<summary><strong>Answer</strong></summary>

The single decision is: **does `ApiClient.runCode` attach the `Authorization: Bearer <token>` header?** From Chapter 18, it does so **iff the `AuthStore` status is `Authed`** (anonymous sends no header). That one choice propagates straight through the server's fork:

- **Header present** → `auth.verifyOptional(token)` returns **`Some(claims)`** → key `user:<sub>` → private per-user bucket.
- **No header** → `verifyOptional` returns **`None`** → key `ip:<addr>` → shared per-IP bucket.

So the per-IP-vs-per-user behavior isn't decided by the limiter in isolation — it's the client's "attach the header iff signed in" decision *meeting* the server's `verifyOptional` fork. The two must agree: if a signed-in client forgot the header, the user would be silently demoted to the shared IP bucket and throttled alongside strangers — the confusing "why am I being throttled?" bug from §7.

</details>

> **Exercise 3 — Defend the IP header.** Explain why the limiter must read the client IP from the edge proxy's header and not trust an arbitrary client-supplied value. What abuse does that prevent?

<details>
<summary><strong>Answer</strong></summary>

The anonymous bucket key *is* the client IP, so **whoever controls that value controls the budget**. If the server trusted a client-supplied header (e.g. an inbound `X-Forwarded-For` the caller set themselves), an attacker could put a **fresh, fake IP on every request** — landing each one in a brand-new `ip:` bucket that's never near its limit. That defeats per-IP metering entirely: unlimited anonymous runs, exactly the free-CPU faucet the limiter exists to close.

The fix is trust boundary discipline: the IP must come from **`X-Real-IP` set by Cortex's own trusted edge proxy/ingress**, which overwrites any client-supplied value with the real connecting address. The principle generalizes — a rate-limit key is only as trustworthy as its source, so you may only key on a value an attacker *cannot* forge (an identity from a verified token, or a header stamped by infrastructure you control). Per-user metering is sturdier for the same reason: `sub` comes from a signature-verified token, which can't be spoofed at all.

</details>

```quiz
{
  "prompt": "How does Cortex meter a signed-in user's code runs differently from an anonymous visitor's?",
  "input": "Choose one:",
  "options": [
    "Signed-in users get a private per-user (sub) bucket; anonymous users share a per-IP bucket",
    "Signed-in users have no limit at all",
    "Both share a single global bucket",
    "Anonymous users get a per-user bucket"
  ],
  "answer": "Signed-in users get a private per-user (sub) bucket; anonymous users share a per-IP bucket"
}
```

## In the Wild

- **[Stripe — Scaling your API with rate limiters](https://stripe.com/blog/rate-limiters)** — token buckets and layered limits in production, from a team that meters billions of requests.
- **[Cloudflare — How we built rate limiting](https://blog.cloudflare.com/counting-things-a-lot-of-different-things/)** — distributed counting (the Redis-shared-across-replicas problem) at scale.
- **[Cortex `ApiClient.runCode`](https://github.com/ani2fun/cortex/blob/main/client/src/main/scala/cortex/client/api/ApiClient.scala)** — the client decision (attach the bearer header iff signed in) that selects the bucket.

---

**Next:** we've built the whole integration. Before the capstone, a security sweep — the attacks this design must withstand and the misconfigurations that quietly defeat it. → [21. Hardening & failure modes](/cortex/production-engineering/iam-keycloak-oauth/cortex-integration/hardening-and-failure-modes)
