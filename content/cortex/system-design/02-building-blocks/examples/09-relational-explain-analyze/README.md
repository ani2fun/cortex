# EXPLAIN ANALYZE — what the planner picks, and why

The companion runnable for [Lesson 9 — Relational databases](../../09-relational-databases.md).
One Postgres 16, one Python seed script that creates a `users` table with
100,000 rows, then runs the same `WHERE email = ?` query twice — first
without an index (the planner picks a sequential scan), then with one
(the planner picks an index scan). The reader compares the two
`EXPLAIN ANALYZE` outputs and reads them line by line.

## What you need

- Docker Desktop (`docker compose` v2)
- *Optional:* `psql` on your host, so you can connect and play after the seed
  runs (the compose file exposes Postgres at `localhost:55432`)

## Bring it up and run the demo

```sh
docker compose up --build seed
```

The `seed` service runs once and exits. You'll see two `EXPLAIN ANALYZE`
blocks back to back. The first will look like:

```
Seq Scan on users  (cost=0.00..2334.00 rows=1 width=29) (actual time=8.4..32.1 rows=1 loops=1)
  Filter: (email = 'needle@codefolio.dev'::text)
  Rows Removed by Filter: 99999
  Buffers: shared hit=834
Planning Time: 0.071 ms
Execution Time: 32.183 ms
```

The second:

```
Index Scan using users_email_idx on users  (cost=0.42..8.44 rows=1 width=29) (actual time=0.041..0.046 rows=1 loops=1)
  Index Cond: (email = 'needle@codefolio.dev'::text)
  Buffers: shared hit=4
Planning Time: 0.182 ms
Execution Time: 0.087 ms
```

The two numbers worth memorising — **32 ms vs 0.09 ms** for the same
query on the same data, a ~350× speedup; and **834 page reads vs 4** for
the access cost (this scales linearly with table size for the seq scan;
the index scan stays at 3–4 pages indefinitely, exactly as the
`BTreeWalker` widget shows).

## Read the plan, line by line

```
Seq Scan on users
```
The plan node — what Postgres did. *Seq Scan* = read the table front to
back. Other nodes you'll see: *Index Scan* (walk a B-tree, hit the heap
for the matched rows), *Bitmap Heap Scan* (gather index hits, then read
the heap in disk order), *Hash Join*, *Merge Join*, *Nested Loop*.

```
(cost=0.00..2334.00 rows=1 width=29)
```
The **cost** is the planner's *estimate*, in abstract units (a single
disk page read is ~1.0). `0.00` is start-up cost; `2334.00` is total cost
to return all rows. `rows=1` is the estimated row count; `width=29` is
the average row width in bytes. The planner uses these to compare plans.

```
(actual time=8.4..32.1 rows=1 loops=1)
```
The **actual** measurements — first time-to-first-row and total time, in
ms. `rows=1 loops=1` means the node executed once and returned one row.
Big mismatch between `cost` and `actual` is your signal that the
planner's stats are stale (`ANALYZE` it).

```
Buffers: shared hit=834
```
Page reads served from `shared_buffers` (the in-memory cache). `shared
read=...` would mean reads from disk. The seq scan touched 834 pages —
that's the entire `users` table.

```
Rows Removed by Filter: 99999
```
99,999 rows examined and thrown away to find the one match. *This is the
work the seq scan is doing.* The index scan does zero of this work.

## Play interactively

After the seed has run, connect via `psql`:

```sh
docker compose exec db psql -U lesson9 -d lesson9
```

Try these:

```sql
-- Compare the two plans directly.
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'needle@codefolio.dev';

-- See what the planner does when the predicate is `LIKE` (NOT equality).
EXPLAIN ANALYZE SELECT * FROM users WHERE email LIKE 'a%';

-- Range queries on an indexed column — the planner picks Index Scan
-- iff the range is selective enough.
EXPLAIN ANALYZE SELECT id FROM users WHERE email BETWEEN 'a' AND 'b';
EXPLAIN ANALYZE SELECT id FROM users WHERE email BETWEEN 'a' AND 'z';

-- Force the planner to ignore the index — useful when comparing plans.
SET enable_indexscan = off;
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'needle@codefolio.dev';
RESET enable_indexscan;
```

The `BETWEEN 'a' AND 'b'` query is interesting — the planner will pick
the index because the range is selective. The `BETWEEN 'a' AND 'z'`
query is not — the planner correctly picks a seq scan because reading
the whole table is cheaper than walking the index *and then* the heap
for nearly every row.

## Tear down

```sh
docker compose down -v   # -v drops the Postgres volume
```

## What this teaches

- **The planner is an optimiser, not a fixed strategy.** It picks
  between Seq Scan / Index Scan / Bitmap Heap Scan / etc. based on
  estimated cost. Get the stats right (run `ANALYZE`) and the planner
  is usually right.
- **`EXPLAIN ANALYZE` is the canonical "why is my query slow"
  diagnostic.** Read the plan top-down (or rather, leaves-up — the
  innermost node runs first). Big mismatches between `cost` estimates
  and `actual` time are stale-stats issues.
- **Index scans get you constant 3–4 page reads regardless of table
  size.** Seq scans get you linear page reads. The asymmetry compounds
  with growth — at 100k rows it's 350×; at 100 M rows the seq scan
  doesn't finish.
