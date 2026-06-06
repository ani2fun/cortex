# Load balancing — NGINX + 3 Python backends

The companion runnable for [Lesson 7 — Load balancing](../../07-load-balancing.md).
Three identical FastAPI replicas sit behind one NGINX reverse proxy. The
lesson body has you bring the stack up, hammer the LB with `curl`, watch the
distribution, then stop one backend and watch what NGINX does. There is no
business logic — the entire point is the LB's behaviour.

## What you need

- Docker Desktop (or any `docker compose` v2 install)
- `curl` + `jq` (the failure-induction one-liner pipes JSON)

## Bring it up

```sh
docker compose up --build
```

The LB exposes port `8087` on your host (the backends are only reachable via
service-name DNS inside the compose network — that's the whole point).

## Step 1 — Confirm round-robin

```sh
for i in $(seq 1 30); do
  curl -s localhost:8087/ | jq -r .server
done | sort | uniq -c
```

You should see roughly `10 py-1`, `10 py-2`, `10 py-3`. NGINX's default
upstream policy is round-robin, so 30 requests across 3 backends lands ~10
each — modulo client-side keep-alive reuse skewing things by a few.

## Step 2 — Induce a failure

In another terminal, stop one backend mid-flight:

```sh
docker compose stop py-2
```

Re-run the curl loop. NGINX will:

1. Send a few requests to `py-2` and notice the connection refused.
2. After `max_fails=2` failures within `fail_timeout=10s`, mark `py-2` as
   down and stop sending traffic to it.
3. The retry policy (`proxy_next_upstream error timeout`) means *the user's
   request still succeeds* — NGINX silently retries on the next upstream.
4. After 10 s, NGINX puts `py-2` back in rotation. If it's still down it
   gets evicted again; if it's back, traffic resumes.

```sh
docker compose start py-2
```

Watch the distribution recover to roughly even.

## Step 3 — Switch the algorithm

Edit `nginx.conf` and uncomment one of the alternative policies inside the
`upstream py_pool` block:

```nginx
upstream py_pool {
    least_conn;           # active-request-count weighted
    server py-1:8000 ...;
    server py-2:8000 ...;
    server py-3:8000 ...;
}
```

Reload NGINX (a config-change reload is hot — no connections are dropped):

```sh
docker compose exec lb nginx -s reload
```

Send half your traffic to `/slow` (which sleeps 2 seconds) and half to `/`:

```sh
for i in $(seq 1 30); do (curl -s localhost:8087/slow >/dev/null &); curl -s localhost:8087/; done
```

Under `least_conn`, NGINX preferentially routes the fast `/` requests to
backends that don't have an outstanding `/slow`. Under round-robin, the
fast requests still go to the busy backends in turn — and you'll see
their `request_count` climb to comparable values but their effective
throughput drops.

## Tear down

```sh
docker compose down
```

## What this teaches

- **L7 load balancing in 60 lines of config.** NGINX terminates the
  HTTP connection, looks at the request, and chooses an upstream. The
  reader sees the entire decision live in `nginx.conf`.
- **Health checks are not magic.** Open-source NGINX does passive
  health checking only — it discovers a dead backend by *trying it
  and failing*, then evicts for `fail_timeout`. NGINX Plus (commercial)
  adds active probes. The trade-off is real and worth showing.
- **Algorithm choice matters under uneven load.** Round-robin is fine
  when every request takes the same time. Once you have `/slow`-style
  variance, `least_conn` wins.
- **Sticky sessions are a smell.** `ip_hash` and `hash $request_uri
  consistent` are alternative policies — useful for session affinity
  or cache-warmth — but they defeat the LB's ability to redistribute.
  The widget in the lesson body shows the consistent-hashing math; this
  example shows it being one line of config to actually use it.
