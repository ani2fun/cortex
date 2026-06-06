# Sharding strategies, side by side

The companion runnable for [Lesson 12 — Sharding and partitioning](../../12-sharding-and-partitioning.md).
No Docker, no database — just one Python script. We generate a Zipfian-
skewed workload, distribute it across `S` shards under four different
partitioning strategies, and print per-shard load + the *hot-shard factor*
(max / mean) for each.

## Run

```sh
python shard_demo.py
```

You'll see output like (with default skew 1.2, 8 shards, 100 000 requests):

```
--- Range partition ---
  shard  requests   pct
      0    19834   19.8%  █████████
      1    11272   11.3%  █████
      2     8124    8.1%  ████
      ...
      7     5731    5.7%  ██
  hot-shard factor: 1.59× (1.0 = perfectly even)

--- Hash partition ---
  shard  requests   pct
      0    12503   12.5%  ██████
      1    12489   12.5%  ██████
      ...
      7    12498   12.5%  ██████
  hot-shard factor: 1.01× (1.0 = perfectly even)

--- Directory partition (hot keys round-robin'd) ---
  shard  requests   pct
      ...
  hot-shard factor: 1.05×

--- Hash + virtual (50/shard) ---
  shard  requests   pct
  hot-shard factor: 1.02×
```

Try editing the constants at the top of `shard_demo.py`:

- `SKEW = 1.5` and watch range partition's hot-shard factor climb to 3–4×.
- `SHARDS = 4` and watch hash partition's factor stay near 1.0×.
- `VIRTUAL_PER_SHARD = 1` and observe that hash+virtual degenerates to plain hash.

## What this teaches

- **Range partitioning naturally hot-spots on the lowest range** when the workload is Zipfian (low-numbered keys are more popular).
- **Hash partitioning is uniform under any workload** at scale — that's the property MurmurHash3 gives you. This is why almost every distributed datastore defaults to hash.
- **Directory partitioning lets you fix specific hot keys by hand** but requires the application to maintain the map. Useful for the long tail.
- **Virtual shards smooth out the rare hash-partition hiccup** when the shard count is small and the law-of-large-numbers hasn't fully kicked in yet.
