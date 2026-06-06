# Cache stampede — Redis read-through, with and without coalescing

The companion runnable for [Lesson 8 — Caching](../../08-caching.md). One
FastAPI app, one Redis, one simulated slow origin (a 500-ms `time.sleep`).
Two endpoints read the same conceptual key — `/no-coalesce` is the naive
read-through that stampedes under concurrency, and `/coalesced` is the
same code path with a Redis `SETNX` lease so only one caller hits the
origin per cold key.

## What you need

- Docker Desktop (or any `docker compose` v2 install)
- `curl` and either `xargs -P` (built into bash/zsh) or `hey` / `vegeta` (a
  load tester — `brew install hey` works on macOS)

## Bring it up

```sh
docker compose up --build
```

The app exposes port `8088` on your host.

## Step 1 — Establish the cold-cache baseline

```sh
# Flush the cache and the origin-hit counter so each experiment starts fresh.
curl -s -X POST localhost:8088/reset
```

## Step 2 — Stampede the naive endpoint

Fire 50 concurrent requests at `/no-coalesce` on a cold cache:

```sh
seq 50 | xargs -P 50 -I {} curl -s -o /dev/null localhost:8088/no-coalesce
curl -s localhost:8088/origin-hits | jq
# {"origin_hits": 50}
```

Every one of the 50 callers missed the cache and called the slow origin in
parallel. The origin saw **50 fetches** to populate **one key**.

## Step 3 — Try the coalesced endpoint

Reset, then fire the same 50 concurrent requests at `/coalesced`:

```sh
curl -s -X POST localhost:8088/reset
seq 50 | xargs -P 50 -I {} curl -s -o /dev/null localhost:8088/coalesced
curl -s localhost:8088/origin-hits | jq
# {"origin_hits": 1}
```

The origin saw **1 fetch**. The other 49 callers waited on the Redis
SETNX lock until the lock holder wrote the value, then read it from
cache. Every caller still waited roughly 500 ms (the origin's latency),
but the *origin* only did 500 ms of work, not 25 seconds of it.

## Step 4 — Observe the limit

Re-run step 3 with concurrency 500:

```sh
curl -s -X POST localhost:8088/reset
seq 500 | xargs -P 500 -I {} curl -s -o /dev/null localhost:8088/coalesced
curl -s localhost:8088/origin-hits | jq
# {"origin_hits": 1}
```

Still 1 origin fetch. The coalescing constant is *N*-independent — it's
1 regardless of how many concurrent callers stampede the cold key.

## Step 5 — TTL-expiry stampede

Wait 30 seconds for the cache to expire (`CACHE_TTL_SEC=30` in the
compose file), then re-fire:

```sh
sleep 31
curl -s -X POST localhost:8088/reset
seq 100 | xargs -P 100 -I {} curl -s -o /dev/null localhost:8088/no-coalesce
curl -s localhost:8088/origin-hits | jq
```

Same shape as step 2. Note that TTL expiry is the canonical stampede
trigger in production — far more common than a literal cold start, and
the reason every production cache layer wants either coalescing, jittered
TTLs, or stale-while-revalidate.

## Tear down

```sh
docker compose down
```

## What this teaches

- **Naive read-through stampedes.** A simple `if cache.has(key): return
  cache.get(key); else: cache.set(key, origin.fetch(key))` is *fine* when
  request volume is low. Under concurrency on a cold (or just-expired)
  key, every concurrent caller misses and every miss hits the origin in
  parallel.
- **Coalescing is one Redis primitive.** The lock holder pattern is a few
  lines of code; the win is N×, where N is the concurrency at the
  moment the key was cold. For a popular key with a 60-second TTL on a
  10 k req/s service, N can be in the hundreds.
- **Coalescing does not lower user-visible latency.** All callers still
  wait for the in-flight fetch. The win is on the *origin*, not the user.
  If you also want to lower user-visible latency on cache misses, you
  want **stale-while-revalidate** instead — serve the expired value while
  asynchronously refreshing it.
- **The lock must have its own TTL.** A crashed lock holder must not
  deadlock all the waiters; bounded poll loops + lock TTL > origin
  latency = no permanent stalls.

## Variations worth trying

- Swap `CACHE_TTL_SEC` for a much smaller value (e.g. 2) and observe how
  often the stampede recurs without coalescing.
- Add **TTL jitter** in the naive endpoint (`r.setex(KEY, TTL + random.randint(-5, 5), value)`)
  and observe how it spreads expiries across time, smoothing the
  origin's view.
- Implement **stale-while-revalidate**: keep two TTLs (fresh + stale).
  Serve `cache.get()` whenever it exists, and asynchronously refresh in
  the background if it's between fresh and stale.
