"""
Lesson 7 — Load balancing hands-on backend.

A trivially small FastAPI app that returns its own identity, request
count, and uptime. The compose file spins up three replicas of this
exact image with different `SERVER_TAG` env vars; NGINX in front of
them round-robins by default. The reader's job is to curl the LB in
a loop and watch the distribution + how it changes when one backend
is stopped.

There is intentionally no business logic. The point of the example is
the LB's behaviour, not the app.
"""

from __future__ import annotations

import os
import time

from fastapi import FastAPI

app = FastAPI()

SERVER_TAG = os.environ.get("SERVER_TAG", "py-?")
STARTED_AT = time.time()
_request_count = 0


@app.get("/")
def root() -> dict:
    """Identify which replica answered, how many calls it has seen, how long it has been up."""
    global _request_count
    _request_count += 1
    return {
        "server": SERVER_TAG,
        "request_count": _request_count,
        "uptime_sec": round(time.time() - STARTED_AT, 1),
    }


@app.get("/healthz")
def healthz() -> dict:
    """Cheap liveness endpoint — NGINX Plus would poll this; the open-source build does passive checks."""
    return {"status": "ok", "server": SERVER_TAG}


@app.get("/slow")
def slow() -> dict:
    """A 2-second sleep — useful for demonstrating least-connections vs round-robin under uneven load."""
    time.sleep(2)
    return {"server": SERVER_TAG, "kind": "slow"}
