"""
Lesson 11 — Replication lag demo.

Writes a sequence of counter updates to the primary, then immediately
queries each one from the replica, measuring how often the replica has
not yet caught up. Prints the lag distribution. The whole point: even
in a healthy idle cluster on localhost, sync streaming replication has
a measurable lag — and that lag is the "stale read" hazard the
ReplicationLagSimulator widget in the lesson body makes visceral.
"""

from __future__ import annotations

import os
import time

import psycopg

PRIMARY_DSN = os.environ["PRIMARY_DSN"]
REPLICA_DSN = os.environ["REPLICA_DSN"]
ROUNDS = 20


def main() -> None:
    print("--- waiting for replica to sync the initial schema ---")
    # The primary's init script created the `counters` table. The
    # replica should see it within a few hundred milliseconds.
    deadline = time.time() + 30
    while True:
        try:
            with psycopg.connect(REPLICA_DSN) as conn, conn.cursor() as cur:
                cur.execute("SELECT 1 FROM counters LIMIT 1")
                cur.fetchall()
            break
        except Exception:  # noqa: BLE001
            if time.time() > deadline:
                raise
            time.sleep(0.5)

    print("--- writing to primary, immediately reading from replica ---")
    stale_count = 0
    lags_us = []
    with (
        psycopg.connect(PRIMARY_DSN, autocommit=True) as primary,
        psycopg.connect(REPLICA_DSN) as replica,
    ):
        for i in range(1, ROUNDS + 1):
            label = f"write-{i}"
            with primary.cursor() as cur:
                cur.execute("INSERT INTO counters (label, n) VALUES (%s, %s)", (label, i))

            wrote_at = time.perf_counter()
            # Immediately read the row back from the replica.
            with replica.cursor() as cur:
                replica.rollback()  # ensure we see committed data per our isolation level
                cur.execute("SELECT n FROM counters WHERE label = %s", (label,))
                row = cur.fetchone()
            read_at = time.perf_counter()
            elapsed_us = int((read_at - wrote_at) * 1_000_000)
            lags_us.append(elapsed_us)

            if row is None:
                stale_count += 1
                print(f"  W{i:02d}: stale — replica had not yet caught up (read after {elapsed_us} µs)")
            else:
                print(f"  W{i:02d}: fresh — replica saw the write after {elapsed_us} µs")

    lags_us.sort()
    p50 = lags_us[len(lags_us) // 2]
    p99 = lags_us[int(len(lags_us) * 0.99)] if len(lags_us) >= 100 else lags_us[-1]
    print()
    print(f"--- summary over {ROUNDS} rounds ---")
    print(f"    stale reads:        {stale_count} / {ROUNDS}")
    print(f"    primary→replica:    p50 {p50} µs, max {max(lags_us)} µs")
    print(f"    in production:      cross-AZ lag is ~1–5 ms; cross-region is ~50–500 ms")


if __name__ == "__main__":
    main()
