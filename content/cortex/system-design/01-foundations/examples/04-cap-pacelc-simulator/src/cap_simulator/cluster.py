"""Three-replica KV store with injectable partitions and two consistency modes.

The whole simulator is small and deterministic on purpose — every step in a
scenario is a method call on ``Cluster``, so a unit test or a scripted demo
can step through it one operation at a time.
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import Optional


class Mode(Enum):
    """Consistency mode for a Cluster.

    - ``CP``: writes need a quorum; partitioned minority refuses writes.
    - ``AP``: writes are accepted locally and reconciled on heal (LWW).
    """
    CP = "CP"
    AP = "AP"


@dataclass
class WriteResult:
    """Outcome of a write call. ``ok`` is False when CP refuses for no quorum."""
    ok: bool
    reason: str = ""
    acked_by: list[str] = field(default_factory=list)


@dataclass
class ReadResult:
    """Outcome of a read call."""
    ok: bool
    value: Optional[str] = None
    timestamp: int = 0
    served_by: str = ""
    note: str = ""


class _Node:
    """A single replica. Holds a per-key value and a per-key Lamport timestamp."""

    def __init__(self, node_id: str):
        self.id = node_id
        # key → (value, timestamp). Timestamp is a Lamport-style logical clock.
        self.store: dict[str, tuple[str, int]] = {}

    def local_write(self, key: str, value: str, ts: int) -> None:
        """Write locally if the incoming timestamp is newer (last-writer-wins)."""
        existing = self.store.get(key)
        if existing is None or ts > existing[1]:
            self.store[key] = (value, ts)

    def local_read(self, key: str) -> tuple[Optional[str], int]:
        """Return (value, timestamp) for ``key`` on this node, or (None, 0)."""
        v = self.store.get(key)
        return (None, 0) if v is None else v


class Cluster:
    """Three-node KV cluster with a network model that supports partitions.

    The network is symmetric: a partition between A and B drops every message
    in *both* directions, but does not affect A↔C or B↔C. This matches what
    happens when, e.g., a single inter-AZ link drops.
    """

    def __init__(self, node_ids: list[str], mode: Mode = Mode.CP):
        if len(node_ids) < 3 or len(node_ids) % 2 != 1:
            # Quorum logic below assumes an odd cluster size of ≥ 3.
            raise ValueError("Cluster requires an odd node count ≥ 3")
        self.nodes = {nid: _Node(nid) for nid in node_ids}
        self.mode = mode
        # Partitions are stored as an unordered set of frozensets {A, B}.
        # A frozenset {A, B} ∈ partitions  ⇔  A ↔ B link is broken.
        self.partitions: set[frozenset[str]] = set()
        # A monotonically-increasing counter used as a Lamport timestamp.
        # In a real system this would be a hybrid logical clock; this is enough
        # to demonstrate last-writer-wins behaviour deterministically.
        self._clock = 0
        self.event_log: list[str] = []

    # -- Network model -----------------------------------------------------

    def partition(self, a: str, b: str) -> None:
        """Drop all messages between nodes ``a`` and ``b`` until heal()."""
        self.partitions.add(frozenset({a, b}))
        self.event_log.append(f"partition introduced between {a} and {b}")

    def heal(self) -> None:
        """Heal all partitions and reconcile state across nodes (AP only)."""
        self.partitions.clear()
        self.event_log.append("network healed")
        if self.mode is Mode.AP:
            self._reconcile_ap()

    def _can_reach(self, src: str, dst: str) -> bool:
        """True if a message from ``src`` to ``dst`` can be delivered now."""
        return src == dst or frozenset({src, dst}) not in self.partitions

    def _reachable_from(self, src: str) -> list[str]:
        """All node ids reachable from ``src`` (including itself)."""
        return [nid for nid in self.nodes if self._can_reach(src, nid)]

    # -- Operations --------------------------------------------------------

    def write(self, key: str, value: str, coordinator: str) -> WriteResult:
        """Issue a write through ``coordinator``.

        - In CP mode the coordinator collects acks from the majority of all
          nodes (across the *full* cluster, not just the reachable set). If a
          partition prevents a majority, the write is refused.
        - In AP mode the coordinator writes locally and replicates to whatever
          peers it can reach; a partitioned minority can still accept writes.
        """
        self._clock += 1
        ts = self._clock
        reachable = self._reachable_from(coordinator)
        majority = len(self.nodes) // 2 + 1

        if self.mode is Mode.CP:
            if len(reachable) < majority:
                # Not enough peers to form a quorum — refuse the write so we
                # never commit something we cannot guarantee is durable.
                self.event_log.append(
                    f"CP write {key}={value!r} via {coordinator} REFUSED "
                    f"(reachable={reachable}, need {majority})"
                )
                return WriteResult(ok=False, reason="no quorum", acked_by=[])
            # Quorum write: replicate to every reachable peer (which includes
            # the coordinator). Quorum is guaranteed because reachable ≥ maj.
            for nid in reachable:
                self.nodes[nid].local_write(key, value, ts)
            self.event_log.append(
                f"CP write {key}={value!r} via {coordinator} ACKed by {reachable}"
            )
            return WriteResult(ok=True, acked_by=list(reachable))

        # AP mode: accept the write locally, opportunistically replicate to
        # whatever peers we can reach. Stale replicas reconcile on heal.
        for nid in reachable:
            self.nodes[nid].local_write(key, value, ts)
        self.event_log.append(
            f"AP write {key}={value!r} via {coordinator} ACKed by {reachable}"
        )
        return WriteResult(ok=True, acked_by=list(reachable))

    def read(self, key: str, coordinator: str) -> ReadResult:
        """Issue a read through ``coordinator``.

        For pedagogical clarity, the read returns the local value of the
        coordinator. A real system would do a quorum read in CP mode; we
        simplify so the AP staleness scenarios are obvious in the output.
        """
        node = self.nodes[coordinator]
        value, ts = node.local_read(key)
        if value is None:
            return ReadResult(
                ok=False,
                served_by=coordinator,
                note="key not present on this replica",
            )
        return ReadResult(ok=True, value=value, timestamp=ts, served_by=coordinator)

    # -- Reconciliation ---------------------------------------------------

    def _reconcile_ap(self) -> None:
        """Last-writer-wins reconciliation across all nodes after heal.

        For each key, find the (value, ts) with the highest timestamp across
        all replicas, and copy it to every replica. This is the simplest
        possible AP reconciliation and demonstrates *why* clocks matter: if
        timestamps were per-node wall clocks with skew, we could lose writes.
        """
        all_keys = {k for n in self.nodes.values() for k in n.store}
        for key in all_keys:
            best_value: Optional[str] = None
            best_ts = -1
            for n in self.nodes.values():
                v, ts = n.local_read(key)
                if v is not None and ts > best_ts:
                    best_value = v
                    best_ts = ts
            if best_value is not None:
                for n in self.nodes.values():
                    n.local_write(key, best_value, best_ts)
        self.event_log.append("AP reconciled all keys via last-writer-wins")

    # -- Diagnostics ------------------------------------------------------

    def snapshot(self) -> dict[str, dict[str, tuple[str, int]]]:
        """Return a copy of every node's store; useful for asserting in tests."""
        return {nid: dict(node.store) for nid, node in self.nodes.items()}
