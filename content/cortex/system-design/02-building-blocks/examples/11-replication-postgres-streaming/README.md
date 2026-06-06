# Replication lag — Postgres primary + streaming replica

The companion runnable for [Lesson 11 — Replication](../../11-replication.md).
One Postgres 16 primary + one streaming replica wired up via `pg_basebackup`
and `wal_level=replica`. A small Python demo writes a sequence of counter
rows to the primary and immediately tries to read each one back from the
replica, measuring how often the replica had not yet caught up.

## What you need

- Docker Desktop (`docker compose` v2)

## Bring it up and run the demo

```sh
docker compose up --build demo
```

You should see something like:

```
--- writing to primary, immediately reading from replica ---
  W01: fresh — replica saw the write after 2453 µs
  W02: fresh — replica saw the write after 1908 µs
  W03: stale — replica had not yet caught up (read after 312 µs)
  W04: fresh — replica saw the write after 4102 µs
  ...
--- summary over 20 rounds ---
    stale reads:        2 / 20
    primary→replica:    p50 2104 µs, max 8392 µs
    in production:      cross-AZ lag is ~1–5 ms; cross-region is ~50–500 ms
```

Even on localhost, with both Postgres servers sharing the same Docker
host, replication is occasionally too slow to see your own write at
the moment you read it. **This is the "read-your-writes" hazard the
widget in the lesson body makes visceral.**

## Inspect replication state

After the demo runs, you can connect to the primary and see what the
replication slot looks like:

```sh
docker compose exec primary psql -U lesson11 -d lesson11 -c "SELECT application_name, state, sync_state, write_lag, flush_lag, replay_lag FROM pg_stat_replication;"
```

The replica's WAL position and the primary's current WAL position are
both visible, and the difference (in bytes) is what monitoring systems
alert on.

## Tear down

```sh
docker compose down -v
```

## What this teaches

- **Streaming replication is not instant.** Even on localhost the
  replica trails the primary by hundreds of microseconds. Across a
  network it's milliseconds; across a region it's tens to hundreds of
  milliseconds.
- **A read immediately after a write may miss it.** That's why
  "read-your-writes" patterns route the read for a short TTL after a
  write back to the primary (or use a session-pinned replica with a
  monotonic-read counter).
- **The lag IS the data you monitor.** `pg_stat_replication` exposes
  `replay_lag` in seconds; the alert threshold depends on the
  workload's tolerance for staleness, but if it exceeds a few seconds
  on a primary running steady, something is wrong.
