"""Behaviour matrix tests for the Cluster.

The matrix we want to lock down:

  mode × partition shape × operation → expected outcome

All tests are deterministic because the simulator is single-process and uses
a logical clock instead of wall time.
"""

import pytest

from cap_simulator import Cluster, Mode


def make_cluster(mode: Mode) -> Cluster:
    return Cluster(["A", "B", "C"], mode=mode)


# ---------------------------------------------------------------------------
# CP mode
# ---------------------------------------------------------------------------

def test_cp_write_succeeds_with_full_connectivity() -> None:
    c = make_cluster(Mode.CP)
    r = c.write("x", "1", coordinator="A")
    assert r.ok is True
    # In a 3-node cluster the quorum is 2, but with full connectivity we get
    # all 3 acks because the coordinator replicates to every reachable peer.
    assert set(r.acked_by) == {"A", "B", "C"}


def test_cp_read_after_write_sees_value_on_any_node() -> None:
    c = make_cluster(Mode.CP)
    c.write("x", "1", coordinator="A")
    for nid in ("A", "B", "C"):
        r = c.read("x", coordinator=nid)
        assert r.ok and r.value == "1", f"node {nid} did not see committed write"


def test_cp_minority_partition_refuses_writes() -> None:
    """A is alone (partitioned from B and C) → cannot reach quorum."""
    c = make_cluster(Mode.CP)
    c.write("x", "1", coordinator="A")
    c.partition("A", "B")
    c.partition("A", "C")

    r = c.write("x", "2", coordinator="A")
    assert r.ok is False
    assert "no quorum" in r.reason

    # B should still hold the previously-committed value.
    rB = c.read("x", coordinator="B")
    assert rB.ok and rB.value == "1"


def test_cp_majority_partition_accepts_writes() -> None:
    """A and B are on the majority side; C is alone — write through A succeeds."""
    c = make_cluster(Mode.CP)
    c.write("x", "1", coordinator="A")
    c.partition("A", "C")
    c.partition("B", "C")

    r = c.write("x", "2", coordinator="A")
    assert r.ok is True
    assert set(r.acked_by) == {"A", "B"}

    # The minority replica (C) does not see the new write; it still holds x=1.
    rC = c.read("x", coordinator="C")
    assert rC.ok and rC.value == "1"


# ---------------------------------------------------------------------------
# AP mode
# ---------------------------------------------------------------------------

def test_ap_partitioned_minority_can_still_write() -> None:
    """Both sides of a partition accept writes — divergence until heal."""
    c = make_cluster(Mode.AP)
    c.write("x", "1", coordinator="A")
    c.partition("A", "B")
    c.partition("A", "C")

    rA = c.write("x", "fromA", coordinator="A")
    rB = c.write("x", "fromBC", coordinator="B")
    assert rA.ok and rB.ok

    # Reads on opposite sides of the partition see *different* values.
    assert c.read("x", coordinator="A").value == "fromA"
    assert c.read("x", coordinator="B").value == "fromBC"


def test_ap_heal_converges_via_last_writer_wins() -> None:
    """After heal, every replica should hold the latest-timestamped write."""
    c = make_cluster(Mode.AP)
    c.write("x", "1", coordinator="A")          # ts=1
    c.partition("A", "B")
    c.partition("A", "C")
    c.write("x", "fromA",  coordinator="A")     # ts=2
    c.write("x", "fromBC", coordinator="B")     # ts=3 — latest

    c.heal()

    snap = c.snapshot()
    for nid in ("A", "B", "C"):
        v, ts = snap[nid]["x"]
        assert v == "fromBC", f"node {nid} did not converge after heal"
        assert ts == 3, f"node {nid} did not adopt latest timestamp"


# ---------------------------------------------------------------------------
# Robustness checks
# ---------------------------------------------------------------------------

def test_even_cluster_size_rejected() -> None:
    with pytest.raises(ValueError):
        Cluster(["A", "B"], mode=Mode.CP)


def test_partition_is_symmetric() -> None:
    """A↔B partition blocks both directions; A↔C remains intact."""
    c = make_cluster(Mode.CP)
    c.partition("A", "B")
    # Even though we wrote partition(A, B), reachability from B → A is also broken.
    # Reachability from A → C is unaffected.
    assert c._can_reach("A", "B") is False
    assert c._can_reach("B", "A") is False
    assert c._can_reach("A", "C") is True
    assert c._can_reach("C", "A") is True
