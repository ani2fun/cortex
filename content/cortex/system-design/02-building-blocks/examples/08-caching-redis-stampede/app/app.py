"""
Lesson 8 — Cache stampede hands-on.

One FastAPI app, one Redis, one simulated slow origin (a `time.sleep`).
Two endpoints that read the same conceptual key:

  /no-coalesce   — naive read-through. Cache miss → every concurrent
                   caller hits the origin in parallel. Stampede.
  /coalesced     — same read-through, but the first miss takes a Redis
                   SETNX lock with TTL. The other callers spin-poll on
                   the cache until the lock holder writes it back, then
                   read it. Origin sees exactly 1 fetch per cold key.

The `/origin-hits` endpoint reports a process-local counter — how many
times the slow-origin function ran. The reader compares that across
the two endpoints under the same concurrency. See ./README.md for the
hey / xargs recipe.
"""

from __future__ import annotations

import asyncio
import os
import time
from typing import Optional

import redis
from fastapi import FastAPI, HTTPException

app = FastAPI()

REDIS_HOST = os.environ.get("REDIS_HOST", "redis")
REDIS_PORT = int(os.environ.get("REDIS_PORT", "6379"))
ORIGIN_LATENCY_MS = int(os.environ.get("ORIGIN_LATENCY_MS", "500"))
CACHE_TTL_SEC = int(os.environ.get("CACHE_TTL_SEC", "30"))
LOCK_TTL_SEC = max(2, ORIGIN_LATENCY_MS // 1000 + 2)
CACHE_KEY = "hot:item:42"
LOCK_KEY = "lock:hot:item:42"

r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)

# Process-local counter of how many times the simulated slow origin ran.
# In production this would be a metric; for the lesson it's just a number
# the reader can curl.
_origin_hits = 0


def fetch_from_origin() -> str:
    """Pretend to hit the system of record. Sleeps to simulate slow query."""
    global _origin_hits
    _origin_hits += 1
    time.sleep(ORIGIN_LATENCY_MS / 1000.0)
    return f"value-at-{int(time.time())}"


@app.get("/no-coalesce")
def no_coalesce() -> dict:
    """Naive read-through. Stampedes under concurrency on a cold key."""
    cached = r.get(CACHE_KEY)
    if cached is not None:
        return {"value": cached, "from": "cache"}
    value = fetch_from_origin()
    r.setex(CACHE_KEY, CACHE_TTL_SEC, value)
    return {"value": value, "from": "origin"}


@app.get("/coalesced")
def coalesced() -> dict:
    """
    Read-through with Redis SETNX lease. First miss takes the lock and
    fetches; the rest spin-poll on the cache until the value appears,
    then return it. The lock has its own TTL so a crashed lock-holder
    doesn't deadlock the others.

    The poll loop is bounded by `LOCK_TTL_SEC` so a wedged origin still
    surfaces as a 504 rather than a hang.
    """
    cached = r.get(CACHE_KEY)
    if cached is not None:
        return {"value": cached, "from": "cache"}

    # Try to become the lock holder.
    got_lock = r.set(LOCK_KEY, "1", nx=True, ex=LOCK_TTL_SEC)
    if got_lock:
        try:
            value = fetch_from_origin()
            r.setex(CACHE_KEY, CACHE_TTL_SEC, value)
            return {"value": value, "from": "origin"}
        finally:
            r.delete(LOCK_KEY)
    else:
        # Someone else is fetching. Spin-poll until the cache is filled.
        deadline = time.time() + LOCK_TTL_SEC
        while time.time() < deadline:
            cached = r.get(CACHE_KEY)
            if cached is not None:
                return {"value": cached, "from": "cache-after-wait"}
            time.sleep(0.020)
        raise HTTPException(status_code=504, detail="lock holder did not fill cache in time")


@app.get("/origin-hits")
def origin_hits() -> dict:
    """How many times has the slow origin actually run since process start?"""
    return {"origin_hits": _origin_hits}


@app.post("/reset")
def reset() -> dict:
    """Reset the counter and flush the cache so the reader can re-run the experiment cleanly."""
    global _origin_hits
    _origin_hits = 0
    r.delete(CACHE_KEY)
    r.delete(LOCK_KEY)
    return {"reset": True}
