"""
Lesson 12 — sharding strategies, in-memory demo.

No Docker; no database. Just Python. We generate a Zipfian-skewed
workload of N requests, distribute them across S shards under four
partitioning strategies (range, hash, directory, hash + virtual), and
print per-shard load + hot-shard factor (max / mean). Run with python
3.11+:

    python shard_demo.py

The aim is exactly the same as the HotShardSimulator widget in the
lesson body — but in 100 lines of script so you can tweak the skew,
shard count, and virtual-node count locally and see the numbers move.
"""

from __future__ import annotations

import hashlib
import random
from collections import defaultdict
from typing import Callable

REQUESTS = 100_000
SHARDS = 8
KEYS = 1_000           # universe of distinct keys
SKEW = 1.2             # Zipf exponent: 0 = uniform; 1.0 = mildly skewed; 2.0 = one key dominates
VIRTUAL_PER_SHARD = 50 # for the hash+virtual strategy


def murmur32(s: str) -> int:
    # Plain stdlib doesn't ship Murmur, but MD5 mod 2^32 is more than uniform enough for the demo.
    return int.from_bytes(hashlib.md5(s.encode()).digest()[:4], "big")


def zipf_keys(n: int, universe: int, skew: float) -> list[str]:
    """Generate n requests against `universe` keys, weighted by Zipf with the given skew."""
    weights = [1.0 / ((r + 1) ** skew) for r in range(universe)]
    total = sum(weights)
    weights = [w / total for w in weights]
    rng = random.Random(42)
    return rng.choices([f"key-{i}" for i in range(universe)], weights=weights, k=n)


def shard_of_range(key: str, shards: int, keys: int) -> int:
    # Range partition: key-0..key-(keys/shards-1) → shard 0, etc.
    idx = int(key.split("-")[1])
    return min(shards - 1, idx * shards // keys)


def shard_of_hash(key: str, shards: int, _keys: int) -> int:
    return murmur32(key) % shards


def shard_of_directory(key: str, shards: int, _keys: int, table: dict[str, int]) -> int:
    # Directory partition: explicit table that the application maintains.
    return table[key]


def shard_of_hash_virtual(key: str, shards: int, _keys: int, virtual_per: int) -> int:
    total_v = shards * virtual_per
    v_node = murmur32(key) % total_v
    return v_node // virtual_per


def summarise(name: str, shards: int, picker: Callable[[str], int], reqs: list[str]) -> None:
    load = [0] * shards
    for k in reqs:
        load[picker(k)] += 1
    total = sum(load)
    pct = [100 * x / total for x in load]
    hot = max(load) / (total / shards)
    print(f"\n--- {name} ---")
    print("  shard  requests   pct")
    for i, (lo, p) in enumerate(zip(load, pct)):
        bar = "█" * int(p / 2)
        print(f"  {i:>5}  {lo:>8}  {p:>4.1f}%  {bar}")
    print(f"  hot-shard factor: {hot:.2f}× (1.0 = perfectly even)")


def main() -> None:
    requests = zipf_keys(REQUESTS, KEYS, SKEW)

    summarise("Range partition", SHARDS, lambda k: shard_of_range(k, SHARDS, KEYS), requests)
    summarise("Hash partition", SHARDS, lambda k: shard_of_hash(k, SHARDS, KEYS), requests)

    # Directory partition: assign the top-10% of keys (by frequency) to alternating shards
    # to break up hot keys, the rest by hash.
    freq: dict[str, int] = defaultdict(int)
    for r in requests:
        freq[r] += 1
    hot_keys = [k for k, _ in sorted(freq.items(), key=lambda x: -x[1])[: KEYS // 10]]
    table: dict[str, int] = {}
    for i, k in enumerate(hot_keys):
        table[k] = i % SHARDS  # round-robin the hot keys
    for k in [f"key-{i}" for i in range(KEYS)]:
        if k not in table:
            table[k] = murmur32(k) % SHARDS
    summarise(
        "Directory partition (hot keys round-robin'd)",
        SHARDS,
        lambda k: shard_of_directory(k, SHARDS, KEYS, table),
        requests,
    )

    summarise(
        f"Hash + virtual ({VIRTUAL_PER_SHARD}/shard)",
        SHARDS,
        lambda k: shard_of_hash_virtual(k, SHARDS, KEYS, VIRTUAL_PER_SHARD),
        requests,
    )

    print()
    print("--- Try ---")
    print(f"  SKEW = {SKEW}  (try 0.5, 1.5, 2.0)")
    print(f"  SHARDS = {SHARDS}  (try 4, 16)")
    print(f"  VIRTUAL_PER_SHARD = {VIRTUAL_PER_SHARD}  (try 1, 10, 200)")


if __name__ == "__main__":
    main()
