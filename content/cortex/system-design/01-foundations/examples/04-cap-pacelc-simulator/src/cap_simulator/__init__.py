"""CAP / PACELC simulator — a tiny 3-node KV store.

The simulator exists to make CAP and PACELC trade-offs *experiential*. You
write to one node, partition the network, read from another, and watch what
happens.

Two consistency modes:

- ``Mode.CP``  — writes require a quorum (majority) of nodes; partitioned
                minority refuses writes. Reads always see the latest committed
                value. The system *sacrifices availability* during partitions.
- ``Mode.AP``  — writes are accepted by any reachable node and reconciled
                later via last-writer-wins (using a logical Lamport-style
                timestamp). The system stays available during partitions but
                can return *stale* reads while a partition is active.

The simulator is single-process and deterministic so the test suite can
encode the (mode × partition × operation) matrix exactly.
"""

from cap_simulator.cluster import Cluster, Mode

__all__ = ["Cluster", "Mode"]
