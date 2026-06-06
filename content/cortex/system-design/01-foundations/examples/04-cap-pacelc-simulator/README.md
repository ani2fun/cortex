# CAP / PACELC simulator

A tiny three-node key-value store with **injectable network partitions** and **two pluggable consistency modes**. Use it to *feel* the trade-offs that the CAP and PACELC theorems describe — not just read about them.

## What it shows you

The simulator runs three replicas (`A`, `B`, `C`) of a key-value store. You issue reads and writes through a client that picks a coordinator at random. You can:

- **Inject a partition** between any pair of replicas — the simulated network drops every message between them.
- **Switch consistency mode**:
  - **CP mode**: writes require a majority (quorum) acknowledgement. During a partition, the minority side **refuses writes** ("availability sacrificed") to keep state consistent.
  - **AP mode**: every replica accepts writes locally and reconciles later (last-writer-wins). During a partition, *all* nodes stay available, but reads on different sides of the partition can disagree ("consistency sacrificed").

When you see two different reads return two different values for the same key, you have just experienced what "eventually consistent" really means.

## Quick start

```bash
just up      # not needed — the simulator runs in-process
just test    # runs the test suite (CP mode + AP mode + partition scenarios)
just demo    # runs the human-readable demo (8 scenarios)
```

## Run a scenario manually

```bash
uv run python -m cap_simulator.demo
```

You will see output like:

```
=== Scenario 1: CP mode, no partition ===
client → write x=1 (coordinator=A)            ✓ ack from majority {A,B,C}
client → read  x   (coordinator=B)            ✓ value=1

=== Scenario 4: CP mode, partition {A | B,C} ===
client → write x=2 (coordinator=A)            ✗ NO QUORUM — write refused
client → read  x   (coordinator=B)            ✓ value=1   (last committed)

=== Scenario 5: AP mode, partition {A | B,C} ===
client → write x=3 (coordinator=A)            ✓ accepted locally on A
client → read  x   (coordinator=A)            ✓ value=3
client → read  x   (coordinator=B)            ✓ value=1   ← STALE (partition)
client → heal partition                       → reconcile: B,C learn x=3
client → read  x   (coordinator=B)            ✓ value=3
```

## How to extend it

The scenario script (`src/cap_simulator/demo.py`) is the easiest place to start. Add your own scenarios by composing:

```python
from cap_simulator import Cluster, Mode

cluster = Cluster(node_ids=["A", "B", "C"], mode=Mode.CP)
cluster.write("key", "value", coordinator="A")
cluster.partition("A", "B")            # inject partition
cluster.write("key", "v2", coordinator="A")  # CP: refused
cluster.heal()                         # restore network
```

## Project layout

```
04-cap-pacelc-simulator/
├── README.md
├── justfile
├── pyproject.toml
├── src/cap_simulator/
│   ├── __init__.py        ← Cluster + Mode
│   ├── node.py            ← Replica with vector clock
│   ├── network.py         ← Injectable partition layer
│   └── demo.py            ← The 8 scenarios from the lesson
└── tests/
    └── test_cluster.py    ← Behaviour matrix: (mode × partition × op)
```

## Why this is small on purpose

This is a **teaching simulator**, not a production database. It is single-process, deterministic, and ~250 lines. Every line is meant to be readable. The price is that it does not implement real Raft / Paxos — for that, see [Lesson 14](../../../2.building-blocks/14-consensus-paxos-and-raft.md).
