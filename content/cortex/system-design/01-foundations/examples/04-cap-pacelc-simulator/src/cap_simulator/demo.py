"""Eight scenarios demonstrating CAP and PACELC behaviour.

Run with ``just demo`` (or ``uv run python -m cap_simulator.demo``).
"""

from cap_simulator import Cluster, Mode


def banner(title: str) -> None:
    print()
    print("=" * 70)
    print(title)
    print("=" * 70)


def show(label: str, ok: bool, detail: str = "") -> None:
    mark = "✓" if ok else "✗"
    print(f"  {mark}  {label:<55} {detail}")


def scenario_1_cp_no_partition() -> None:
    banner("Scenario 1 — CP mode, no partition (the happy path)")
    c = Cluster(["A", "B", "C"], mode=Mode.CP)
    r = c.write("x", "1", coordinator="A")
    show(f"write x=1 via A", r.ok, f"acked_by={r.acked_by}")
    r2 = c.read("x", coordinator="B")
    show(f"read  x   via B", r2.ok, f"value={r2.value}")
    print("  Both nodes see x=1. The system is consistent.")


def scenario_2_cp_partition_minority_refuses() -> None:
    banner("Scenario 2 — CP mode, partition {A | B,C} — minority refuses writes")
    c = Cluster(["A", "B", "C"], mode=Mode.CP)
    c.write("x", "1", coordinator="A")
    c.partition("A", "B")
    c.partition("A", "C")
    r = c.write("x", "2", coordinator="A")  # A is alone — no majority reachable
    show("write x=2 via A (partitioned minority)", r.ok, f"reason={r.reason}")
    r2 = c.read("x", coordinator="B")
    show("read  x   via B (majority side)", r2.ok, f"value={r2.value}")
    print("  CP refuses the minority write; reads on the majority see x=1.")


def scenario_3_cp_partition_majority_accepts() -> None:
    banner("Scenario 3 — CP mode, partition {A,B | C} — majority side keeps writing")
    c = Cluster(["A", "B", "C"], mode=Mode.CP)
    c.write("x", "1", coordinator="A")
    c.partition("A", "C")
    c.partition("B", "C")
    r = c.write("x", "2", coordinator="A")  # A has B as a peer → quorum
    show("write x=2 via A (majority side)", r.ok, f"acked_by={r.acked_by}")
    r2 = c.read("x", coordinator="C")
    show("read  x   via C (partitioned minority)", r2.ok, f"value={r2.value}")
    print("  C is on the minority side and is reading stale data — but CP guarantees")
    print("  it cannot have *committed* anything inconsistent.")


def scenario_4_ap_partition_both_sides_accept() -> None:
    banner("Scenario 4 — AP mode, partition {A | B,C} — both sides keep writing")
    c = Cluster(["A", "B", "C"], mode=Mode.AP)
    c.write("x", "1", coordinator="A")
    c.partition("A", "B")
    c.partition("A", "C")
    rA = c.write("x", "from_A_side", coordinator="A")
    rB = c.write("x", "from_BC_side", coordinator="B")
    show("write via A (lone)", rA.ok, f"acked_by={rA.acked_by}")
    show("write via B (with C)", rB.ok, f"acked_by={rB.acked_by}")
    rA_read = c.read("x", coordinator="A")
    rB_read = c.read("x", coordinator="B")
    show("read  x   via A", rA_read.ok, f"value={rA_read.value}")
    show("read  x   via B", rB_read.ok, f"value={rB_read.value}   ← divergence!")
    print("  AP keeps everyone writeable, but the cluster has *split-brain* until heal.")


def scenario_5_ap_heal_reconciles() -> None:
    banner("Scenario 5 — AP heal reconciles via last-writer-wins")
    c = Cluster(["A", "B", "C"], mode=Mode.AP)
    c.write("x", "1", coordinator="A")
    c.partition("A", "B")
    c.partition("A", "C")
    c.write("x", "from_A_side", coordinator="A")
    c.write("x", "from_BC_side", coordinator="B")
    c.heal()
    snap = c.snapshot()
    for nid in ("A", "B", "C"):
        v, ts = snap[nid]["x"]
        show(f"after heal — node {nid}", True, f"value={v} (ts={ts})")
    print("  After heal, all nodes converge on the *latest* timestamped write.")
    print("  The earlier divergent write is silently overwritten — that is what")
    print("  'last-writer-wins' literally means. Real systems usually resolve")
    print("  conflicts more carefully (CRDTs, vector clocks, custom merge fns).")


def scenario_6_pacelc_else_latency() -> None:
    banner("Scenario 6 — PACELC: even with no partition, CP costs latency")
    print("  In CP mode, every write requires a quorum ack — that means")
    print("  *every write waits for the slowest of (n/2 + 1) nodes*.")
    print()
    print("  In a 3-node CP cluster:")
    print("    write_latency_p99 = max_p99 of any 2 of 3 replicas")
    print()
    print("  In AP mode, a write returns as soon as the coordinator has it locally.")
    print("    write_latency_p99 ≈ p99 of one local disk write")
    print()
    print("  This is the *Else* in PACELC: the latency tax of CP, even on a")
    print("  perfectly healthy network. Every CP system pays it on every write.")
    print("  CAP only cares about partitions; PACELC also names this normal-day cost.")


def scenario_7_minority_read_staleness() -> None:
    banner("Scenario 7 — CP partition: minority can still *read* (stale)")
    c = Cluster(["A", "B", "C"], mode=Mode.CP)
    c.write("x", "1", coordinator="A")  # all three see x=1
    c.partition("A", "C")
    c.partition("B", "C")
    c.write("x", "2", coordinator="A")  # majority commits x=2; C never sees it
    rC = c.read("x", coordinator="C")
    show("read x via C (partitioned minority)", rC.ok, f"value={rC.value}")
    print("  C does not refuse reads — it just returns the last value it knows about.")
    print("  This is why most CP systems offer 'linearizable read' as an *option*,")
    print("  not the default. Linearizable reads also need a quorum and pay the cost.")


def scenario_8_real_world_choice() -> None:
    banner("Scenario 8 — Picking CP or AP for your workload (the actual decision)")
    print("  Workload: 'count likes on a post'.")
    print("    Wrong by 1 like for 30 seconds: nobody dies.")
    print("    Refuse to count likes during a regional partition: people get mad.")
    print("    → Pick AP. (Twitter, Instagram, Facebook all do.)")
    print()
    print("  Workload: 'transfer money between bank accounts'.")
    print("    Wrong by $100 for 30 seconds: regulator pays you a visit.")
    print("    Refuse the transfer during a partition: customer retries in 5 min.")
    print("    → Pick CP. (Every payment system does. See Lesson 44 — Stripe.)")
    print()
    print("  The CAP / PACELC choice is *workload-level*, not company-level.")
    print("  Most real companies run *both kinds of systems side by side*.")


def main() -> None:
    scenario_1_cp_no_partition()
    scenario_2_cp_partition_minority_refuses()
    scenario_3_cp_partition_majority_accepts()
    scenario_4_ap_partition_both_sides_accept()
    scenario_5_ap_heal_reconciles()
    scenario_6_pacelc_else_latency()
    scenario_7_minority_read_staleness()
    scenario_8_real_world_choice()


if __name__ == "__main__":
    main()
