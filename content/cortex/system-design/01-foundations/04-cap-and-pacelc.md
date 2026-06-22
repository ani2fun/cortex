---
title: '4. The CAP theorem and PACELC, honestly'
summary: CAP is a partition-time choice; PACELC names the trade-off you pay every other day. Demystified with a runnable simulator and an inline partition widget.
---

# 4. The CAP theorem and PACELC, honestly

## TL;DR
> The honest one-line version of CAP is **not** "pick two of three." It is: *when a network partition cuts your replicas apart, you must choose — be **consistent** (refuse to answer rather than risk a stale or divergent answer) or stay **available** (answer on every side and reconcile later).* That's the whole theorem. It says **nothing** about the 99%+ of the time the network is healthy. PACELC fills that gap: *if a **P**artition, choose **A** or **C**; **E**lse, choose **L**atency or **C**onsistency.* The "Else" is the trade-off you actually pay every day. We will *feel* both halves with a runnable simulator before we touch the math — and we'll be honest about how little CAP, even stated correctly, really buys you.

## 1. Motivation

In **2000**, Eric Brewer gave a keynote at PODC titled *Towards Robust Distributed Systems*. In that talk he stated what he called the "CAP conjecture": you cannot have all three of consistency, availability, and partition-tolerance simultaneously. In 2002, [Gilbert and Lynch proved it](https://users.ece.cmu.edu/~adrian/731-sp04/readings/GL-cap.pdf) (formally, for asynchronous networks).

For the next ten years, every blog post explaining CAP got it slightly wrong. The most common error: *"Pick two of three"*. This is **not what CAP says**. As Brewer himself wrote in [his 2012 clarification](https://www.infoq.com/articles/cap-twelve-years-later-how-the-rules-have-changed/), partitions are not a choice — they happen whether you want them or not. The real choice is *what your system does **during** a partition*.

Then in **2010**, Daniel Abadi at Yale published a [blog post](https://dbmsmusings.blogspot.com/2010/04/problems-with-cap-and-yahoos-little.html) (later formalised in a 2012 IEEE Computer paper) pointing out that CAP only describes the system's behaviour during the rare event of a partition — but says nothing about its behaviour the rest of the time. The rest of the time, you trade *latency* against *consistency* — every replication mechanism, every quorum-vs-eventually-consistent choice, every async-vs-sync-replication knob. That trade-off is **PACELC**: *if a Partition, choose Availability or Consistency; **Else**, choose Latency or Consistency*.

The PACELC version is the one a senior engineer carries around. CAP is the version you find on whiteboards. We will teach the senior version.

One more piece of honesty up front, because most courses skip it. *Designing Data-Intensive Applications* (2nd ed.) is blunt: the CAP theorem, **even stated correctly**, is "of very narrow scope" and "has little practical value for designing systems." Why so harsh? Three reasons, all worth internalising:

1. **It freezes one consistency model.** CAP's "C" means *linearizability* — the single strongest model (more on this below). It says nothing about the weaker-but-still-useful models (read-your-writes, monotonic reads, causal, bounded staleness) that real "eventually consistent" systems actually offer. The design space is a spectrum; CAP sees two points on it.
2. **It addresses one fault, and a rare one.** CAP is *only* about network partitions. By Google's own incident data, partitions cause **fewer than 8% of incidents** — dead nodes, slow nodes, GC pauses, bad deploys, and overload dominate. A theorem about the 8% case is not where most of your reliability lives.
3. **CP/AP is not even exhaustive.** Plenty of well-designed systems are *neither* — because CAP's formal definition of "availability" is idiosyncratic and doesn't match what engineers usually mean by available. We'll return to this trap in §7.

So why teach it at all? Because the *vocabulary* (CP, AP, "what happens during a partition?") is the lingua franca of every design review, and because the **underlying trade-off is real** — it just lives mostly in PACELC's "Else," not in CAP's "P." Learn the words; carry the PACELC framing; don't let anyone win an argument by quoting "pick two of three" at you.

## 2. Intuition (Analogy)

Imagine **two siblings sharing a single Google Doc**. They are usually at the same kitchen table but occasionally one of them takes a road trip to a cabin with no internet. Each of them types ideas into the doc.

Now consider three operating modes:

**Mode A — "Strict editor":** every keystroke either reaches the other sibling within 50 ms or **no keystroke is allowed at all**. The cabin sibling cannot type anything until they are back online. The doc never has conflicts. *Consistent and unavailable on the road.* This is **CP**.

**Mode B — "Permissive editor":** both siblings keep typing whether they can sync or not. When the cabin sibling returns, the doc has two divergent versions and somebody — Google's CRDT engine, or the sibling — has to merge them. Neither sibling was ever blocked. *Available, but the doc is briefly inconsistent.* This is **AP**.

**Mode C — "Pretend the road trip will not happen":** "Of course we are always online! Why are you asking?" → cabin sibling types into a frozen window, comes back to find their changes lost, and never trusts the editor again. This is the *non-existent* option that bad architecture diagrams keep promising. **There is no third mode.**

Now extend the analogy. Even when *both siblings are at the kitchen table* — no road trip, no partition, *perfect* network — the editor still has a choice every keystroke:

> *"Do I send this keystroke to the other sibling and wait for their ACK before showing it on the screen? Or do I show it immediately and let the ACK arrive a few milliseconds later?"*

The first option is **slower but always consistent** (you see what they see). The second is **faster but briefly inconsistent** (your screen leads theirs). That is **PACELC's *Else*** — the choice you pay every day, not just during the rare cabin trip.

**Why is the partition choice *forced*?** Put yourself on the kitchen-table side during the road trip. You type a word. You cannot tell, from where you sit, whether the cabin sibling (a) has crashed, (b) is fine but unreachable, or (c) is busily typing a *conflicting* word right now. All three look identical: silence. You have exactly two honest moves. Wait for an acknowledgement that may never come (you stay correct but freeze — **CP**), or proceed without it and merge later (you stay responsive but may diverge — **AP**). There is no third move, because *no message can travel faster than the broken link*. The trade-off isn't a design smell you can engineer away; it's a property of the universe — uncertainty under silence. This is exactly why DDIA reframes "pick two of three" as the sharper **"either consistent or available when partitioned."** A more reliable network forces the choice *less often*; it never abolishes it.

## 3. Formal Definition

### CAP, precisely

For an *asynchronous network model* (Gilbert & Lynch's setting):

| Letter | Property | Plain English |
|---|---|---|
| **C — Consistency** | *Linearizability specifically* — the system behaves as if there is a single copy of the data and every read sees the most recently completed write. | "If I write `x = 5` and then read `x`, I get `5`. Guaranteed. Across all replicas." |
| **A — Availability** | Every request to a non-failing node returns a (non-error) response — *in CAP's formal sense*, which is stricter and stranger than the everyday meaning. | "Every healthy server answers every request, eventually." |
| **P — Partition tolerance** | The system continues to operate even when arbitrary messages between nodes are lost. | "When the network drops messages, the system does not just give up." |

> **The "C" is narrower than it looks.** CAP's consistency is *exactly* linearizability — the strongest model, where the whole cluster pretends to be one copy. It is **not** the database "C" in ACID, and it is **not** the umbrella term "strong consistency." Everything weaker — read-your-writes, monotonic reads, causal, bounded staleness — is invisible to CAP, which lumps it all under "not C." That spectrum is the subject of [Lesson 13 — Consistency models](/cortex/system-design/building-blocks/consistency-models); CAP only sees its two endpoints.

**The theorem:** in any system that may experience a partition, you cannot guarantee both C (linearizability) and A simultaneously.

**The misunderstanding:** "Pick two of three." Wrong. *P is not a choice.* Networks partition. Every distributed system that spans more than one machine *must* tolerate P. So the live choice is between:

- **CP**: refuse to answer (or answer with an error) when consistency cannot be guaranteed during a partition. *Sacrifices availability.*
- **AP**: keep answering on every partitioned side; accept that different nodes may temporarily disagree. *Sacrifices linearizability.*

Saying "we are CA" is saying "we do not run on a network," which means you are running on a single machine and CAP does not apply. (Some single-machine systems are sold as "CA" for marketing reasons. Roll your eyes when you read this.)

**But "CP or AP" is not even a clean dichotomy** — and this is the part the whiteboard cartoon never admits. DDIA flags two more cracks:

- *CAP's "availability" is idiosyncratic.* The formal definition demands that **every** non-failing node answer **every** request. Most real "highly available" systems don't promise that (a node that has lost quorum may correctly return an error yet still be a fault-tolerant, well-run system). So plenty of perfectly good systems satisfy *neither* CP nor AP under CAP's strict letter.
- *The labels are coarse.* A real database makes the C-vs-A choice *per operation* and *per consistency level*, not once for the whole system. DynamoDB serves eventually-consistent reads (AP-flavoured) and strongly-consistent reads (CP-flavoured) from the **same** table; MongoDB's behaviour depends on your `writeConcern` and `readConcern`. A single "CP/AP" sticker on a logo is a lossy summary at best.

### PACELC, precisely

Daniel Abadi's extension:

> *If there is a Partition (P), how does the system trade off **A**vailability vs **C**onsistency?*
> *Else, how does it trade off **L**atency vs **C**onsistency?*

A system gets a two-letter classification — one for each scenario:

| System | PACELC class | Meaning |
|---|---|---|
| Spanner, FoundationDB | **PC + EC** | Refuse during partition; prioritise consistency over latency normally. |
| MongoDB (default) | **PA + EC** | Available during a partition (the majority side keeps a primary); consistency over latency normally (default w:majority since 5.0). |
| Cassandra, DynamoDB | **PA + EL** | Stay available during partition; prioritise low latency over strong consistency normally. |
| ZooKeeper, etcd | **PC + EC** | Used as the "boring, correct" coordinator everywhere. |
| MySQL with async replication | **PA + EL** | The async replica may fall behind, even with no partition. |
| MySQL with sync replication | **PC + EC** | Replicas always caught up; writes pay the cross-replica latency. |

Notice the table classifies *configurations*, not just product names. "MySQL with async replication" and "MySQL with sync replication" are the **same binary** in different clothes — the PACELC class is a property of how you *operate* the system, not the logo on the box. The same is true of Postgres (sync vs async streaming), MongoDB (`w:1` vs `w:majority`), and Cassandra (`ONE` vs `QUORUM` vs `ALL`). When someone says "Cassandra is AP," they mean *Cassandra as usually configured*; crank it to `QUORUM`/`QUORUM` and you've moved its "E" toward C and paid the latency for it.

Two stable engineering instincts emerge:

1. **PC + EC** systems are for *truth-of-record* data — money, identity, inventory, unique usernames, sequence numbers, anything where the answer "I am not sure" is preferable to a wrong answer.
2. **PA + EL** systems are for *experience* data — likes, views, presence, search, recommendations, anything where "stale by 30 seconds" is fine and "unavailable for 30 seconds" is a disaster.

That mapping isn't arbitrary. It tracks DDIA's own list of where linearizability is genuinely *required*: leader election (only one leader, ever), uniqueness constraints (only one person gets the username), and "never go negative / never oversell / never double-book" invariants. Each of those needs a single up-to-date value every node agrees on — which is exactly what a partition can take away. The experience-data cases need none of that, so they keep availability instead.

Most real companies run **both kinds side by side**. We will see this directly in [Capstone 43 (news feed)](/cortex/system-design/capstones/news-feed) and [Capstone 49 (payments)](/cortex/system-design/capstones/payment-system).

## 4. Worked Example

Let's design two services and pick CAP/PACELC for each.

### Service 1 — Like-counting on social posts

- "Wrong by one like for 30 seconds": nobody dies.
- "Refuse to count likes during a partition": users complain, retention drops.

**Pick AP** during partition. **Pick L** (low latency) normally. → **PA + EL**.

Real-world: **Cassandra / DynamoDB**, configured for low quorums. *What actually happens under a partition:* each side keeps accepting reads and writes against whatever replicas it can still reach. Two sides may record different values for the same key; on heal, the cluster reconciles — Cassandra and DynamoDB default to **last-writer-wins by timestamp**, which means one of the concurrent writes is silently dropped. For a like counter that's fine (and many teams use a CRDT counter so *both* increments survive). Twitter, Instagram, and Reddit all run Cassandra-class stores for exactly this kind of high-volume, loss-tolerant experience data.

### Service 2 — Bank account balance

- "Wrong by $100 for 30 seconds": regulator visit; potential fraud.
- "Refuse the transfer for 30 seconds during a regional partition": annoying, but customers retry.

**Pick CP** during partition. **Pick C** (strong) normally. → **PC + EC**.

Real-world: **Spanner / CockroachDB / etcd**. *What actually happens under a partition:* writes go through a consensus group (Paxos in Spanner, Raft in CockroachDB and etcd). The side that still holds a **majority** of replicas keeps committing; the **minority** side cannot reach quorum, so it *refuses* writes — and refuses linearizable reads too — rather than risk handing back a value the majority has already moved past. The balance is never wrong; it is, for those cut-off clients, briefly *unavailable*. That is the CP bargain made concrete: "I'm not sure" beats "$100 that isn't there." Modern fintech ledgers run on exactly these Spanner-class stores.

### Service 3 — Mixed system (the realistic case)

A modern e-commerce site runs **both at once**:

- Product catalogue (read-heavy, "stale by 5 min is fine") → **PA + EL**
- Inventory counter ("can never sell what we don't have") → **PC + EC**
- Cart state (per-user, never multi-region contended) → **PC + EC**
- Recommendations (read-heavy, eventual is fine) → **PA + EL**
- Order placement (atomic, money involved) → **PC + EC**
- Reviews / ratings (eventual, never urgent) → **PA + EL**

The site is not a single CAP/PACELC class. It is a *portfolio* of stores, each chosen for its own workload.

> **Friction prompt — before reading on:**
> A messaging app needs to deliver messages between two users. The product manager says: "*messages should never appear out of order, and the app should always be available, and we have users in 50 countries.*" Which property must give? *(Hint: it is the one most users do not realise they will accept.)*

<details>
<summary><strong>Solution</strong></summary>

Strict global ordering of messages requires *consensus across regions for every message* — at minimum 100–250 ms of cross-region RTT per message. That kills both latency and per-region availability during a regional outage.

WhatsApp, Slack, and iMessage all give up **strict global ordering** in favour of *causal ordering* (messages within one conversation are ordered; messages across conversations may be reordered briefly). This is the [right amount of consistency](https://jepsen.io/consistency) for the actual user experience and lets the system stay AP during partitions.

The PM did not realise they were asking for the impossible. Your job was to translate.

</details>

## 5. Build It

The lesson ships with a full runnable simulator at [`examples/04-cap-pacelc-simulator/`](https://github.com/ani2fun/codefolio/tree/main/content/cortex/system-design/01-foundations/examples/04-cap-pacelc-simulator). It is a 3-node KV store with **injectable partitions** and **two pluggable consistency modes (CP / AP)**. The C4 Container view below — rendered live from [`c4/cp-cluster.c4`](https://github.com/ani2fun/codefolio/blob/main/content/cortex/system-design/01-foundations/c4/cp-cluster.c4) — shows the topology the simulator implements:

<iframe
  src="/c4/view/foundations_cp_cluster_containers"
  width="100%"
  height="520"
  style="border: 1px solid var(--border, #2b2b2b); border-radius: 8px;"
  loading="lazy"
  title="CP cluster — three-replica quorum KV store"
></iframe>

> Three replicas of the quorum KV store; every arrow is a "replicate write & await ack" edge — the edge that gets blocked by an injected partition.

Before cloning the repo, walk through the four canonical scenarios inline — same cluster, same partitions, same modes, same conclusions. Pick a scenario tab, then step through frame-by-frame. Watch the `REFUSED` badge appear on the minority side in CP mode, and watch the split-brain divergence in AP mode get reconciled by last-writer-wins after the heal.

```d3 widget=partition-simulator
{
  "title": "Three-node KV cluster — pick a scenario, step through frame-by-frame",
  "scenarios": [
    {
      "name": "CP — minority refuses on partition",
      "mode": "CP",
      "steps": [
        { "msg": "Initial state. All three replicas committed x=v0 via quorum.",
          "nodes": { "A": "v0", "B": "v0", "C": "v0" },
          "links": [["A","B"], ["A","C"], ["B","C"]] },
        { "msg": "Network partitions: {A} | {B, C}. The edges A↔B and A↔C are dropped.",
          "nodes": { "A": "v0", "B": "v0", "C": "v0" },
          "links": [["B","C"]] },
        { "msg": "Write x=v1 via A — REFUSED. A is alone (1 of 3 reachable); no majority.",
          "nodes": { "A": "v0", "B": "v0", "C": "v0" },
          "links": [["B","C"]],
          "refused": ["A"] },
        { "msg": "Write x=v1 via B — committed. B,C are a majority of 2.",
          "nodes": { "A": "v0", "B": "v1", "C": "v1" },
          "links": [["B","C"]] },
        { "msg": "Partition heals. A catches up; cluster converges on x=v1 cleanly.",
          "nodes": { "A": "v1", "B": "v1", "C": "v1" },
          "links": [["A","B"], ["A","C"], ["B","C"]] }
      ]
    },
    {
      "name": "AP — both sides accept, then LWW reconciles",
      "mode": "AP",
      "steps": [
        { "msg": "Initial state. All three replicas committed x=v0.",
          "nodes": { "A": "v0", "B": "v0", "C": "v0" },
          "links": [["A","B"], ["A","C"], ["B","C"]] },
        { "msg": "Network partitions: {A} | {B, C}.",
          "nodes": { "A": "v0", "B": "v0", "C": "v0" },
          "links": [["B","C"]] },
        { "msg": "Write via A — accepted locally (no quorum required in AP).",
          "nodes": { "A": "alice", "B": "v0", "C": "v0" },
          "links": [["B","C"]] },
        { "msg": "Write via B — accepted, replicated to C. Split-brain: A and {B,C} disagree.",
          "nodes": { "A": "alice", "B": "bob", "C": "bob" },
          "links": [["B","C"]] },
        { "msg": "Heal. Last-writer-wins: B's write happened later (ts=3 > A's ts=2), so bob wins. alice is silently lost.",
          "nodes": { "A": "bob", "B": "bob", "C": "bob" },
          "links": [["A","B"], ["A","C"], ["B","C"]] }
      ]
    },
    {
      "name": "CP — majority side keeps writing",
      "mode": "CP",
      "steps": [
        { "msg": "Initial state. All three replicas committed x=v0.",
          "nodes": { "A": "v0", "B": "v0", "C": "v0" },
          "links": [["A","B"], ["A","C"], ["B","C"]] },
        { "msg": "Network partitions: {A, B} | {C}.",
          "nodes": { "A": "v0", "B": "v0", "C": "v0" },
          "links": [["A","B"]] },
        { "msg": "Write x=v1 via A — committed. A,B are 2 of 3 = majority.",
          "nodes": { "A": "v1", "B": "v1", "C": "v0" },
          "links": [["A","B"]] },
        { "msg": "Read x via C — returns stale v0. C is on the minority side; CP says strong on the majority, not everywhere.",
          "nodes": { "A": "v1", "B": "v1", "C": "v0" },
          "links": [["A","B"]] },
        { "msg": "Heal. C catches up via replication; cluster is consistent again.",
          "nodes": { "A": "v1", "B": "v1", "C": "v1" },
          "links": [["A","B"], ["A","C"], ["B","C"]] }
      ]
    },
    {
      "name": "CP — happy path, no partition",
      "mode": "CP",
      "steps": [
        { "msg": "Initial state. All three replicas committed x=v0.",
          "nodes": { "A": "v0", "B": "v0", "C": "v0" },
          "links": [["A","B"], ["A","C"], ["B","C"]] },
        { "msg": "Write x=v1 via A — committed. All three replicas are reachable; quorum trivially met.",
          "nodes": { "A": "v1", "B": "v1", "C": "v1" },
          "links": [["A","B"], ["A","C"], ["B","C"]] },
        { "msg": "Read x via B — returns v1. CP guarantees linearizable reads across replicas.",
          "nodes": { "A": "v1", "B": "v1", "C": "v1" },
          "links": [["A","B"], ["A","C"], ["B","C"]] }
      ]
    }
  ]
}
```

Run it locally:

```bash
git clone https://github.com/ani2fun/codefolio.git
cd codefolio/content/cortex/system-design/01-foundations/examples/04-cap-pacelc-simulator
just test                      # 8 tests, locks down the (mode × partition × op) matrix
just demo                      # 8 narrated scenarios — read the output line by line
```

A taste of the demo output:

```
=== Scenario 4 — AP mode, partition {A | B,C} — both sides keep writing ===
  ✓  write via A (lone)                                      acked_by=['A']
  ✓  write via B (with C)                                    acked_by=['B', 'C']
  ✓  read  x   via A                                         value=from_A_side
  ✓  read  x   via B                                         value=from_BC_side   ← divergence!
  AP keeps everyone writeable, but the cluster has *split-brain* until heal.

=== Scenario 5 — AP heal reconciles via last-writer-wins ===
  ✓  after heal — node A                                     value=from_BC_side (ts=3)
  ✓  after heal — node B                                     value=from_BC_side (ts=3)
  ✓  after heal — node C                                     value=from_BC_side (ts=3)
  After heal, all nodes converge on the *latest* timestamped write.
```

The core abstraction is a 200-line `Cluster` class. Inline:

```python run
# Pseudocode of the simulator's cluster — see the repo for the full version.
from enum import Enum

class Mode(Enum):
    CP = "CP"   # writes need a quorum; minority refuses on partition
    AP = "AP"   # both sides accept; reconcile via last-writer-wins on heal

def write(cluster, key, value, coordinator):
    # 1. Find peers we can still talk to (a partition drops some pairs).
    reachable = cluster.reachable_from(coordinator)
    majority  = len(cluster.nodes) // 2 + 1

    if cluster.mode is Mode.CP:
        if len(reachable) < majority:
            # Cannot guarantee durability — refuse rather than commit something
            # that might disagree with the majority side later.
            return "refused: no quorum"
        # Replicate to every reachable peer (≥ majority by definition here).
        for n in reachable:
            n.local_write(key, value, ts=cluster.next_clock())
        return f"committed (acked by {reachable})"

    # AP: write locally + opportunistically replicate to whoever is reachable.
    for n in reachable:
        n.local_write(key, value, ts=cluster.next_clock())
    return f"accepted locally (acked by {reachable})"
```

**Now break it.** Open `src/cap_simulator/demo.py` and add a 9th scenario:

> Start in AP mode. Write `x=1` via A. Partition {A | B,C}. Write `x=alice` via A and `x=bob` via B. Heal. *Predict* the value before reading it. Then run.

You will discover that **last-writer-wins silently lost one of the writes** — even though both writes returned success. That is the price of AP, made visible. In production, you would defend against it with vector clocks, CRDTs, or application-level conflict resolution. We will see CRDTs in detail when distributed-patterns lessons cover consistency models and conflict-free replicated data types.

## 6. Trade-offs & Complexity

| Property | CP (strong) | AP (eventual) |
|---|---|---|
| **Behaviour during partition** | Minority side rejects writes (and often reads) | Both sides accept reads and writes |
| **Behaviour after heal** | Nothing to reconcile (only one side wrote) | Reconciliation: LWW, vector clocks, or CRDTs |
| **Write latency (no partition)** | quorum-bound: ~max p99 of (n/2 + 1) replicas | local-bound: ~p99 of one disk write |
| **Read consistency** | Linearizable (with linearizable read enabled) | Eventually consistent; bounded staleness |
| **Operational complexity** | Lower — at most one truth | Higher — application must handle conflicts |
| **Use cases** | Money, inventory, identity, sequence numbers | Likes, views, presence, search, recommendations |
| **Examples** | Spanner, FoundationDB, etcd, ZooKeeper, CockroachDB | Cassandra, DynamoDB, Riak, Voldemort |

**The cost of CP, normally:** every write waits for a quorum. In a 3-node cluster across the same datacentre, that adds maybe 1 ms. Across regions (~50–250 ms RTT), it adds a *lot*. This is why globally-distributed strongly-consistent systems (Spanner) are expensive engineering — they need atomic clocks (TrueTime) to bound the cost.

**The cost of AP, after a partition:** the application must handle conflicts. "Last-writer-wins" is the simplest strategy and the easiest to lose data with. Vector clocks let the application *detect* a conflict and decide. CRDTs let the application *avoid* the conflict by design (counters, sets, registers with merge functions). All three are real engineering you have to invest in.

### Why the trade-off *cannot* be engineered away

It's tempting to assume that "consistent and fast" is just an optimisation problem waiting for a clever enough algorithm. It isn't, and it's worth understanding *why* at three levels of depth.

**1. The intuition (silence is ambiguous).** A replica that needs to answer a request, but can't reach its peers, faces the ambiguity from the analogy: a peer that is *dead*, a peer that is *slow*, and a peer that is *busy writing a conflicting value* all look identical — silence. To be linearizable, the replica must resolve that ambiguity before answering, which means **waiting to hear from enough peers**. To be available, it must answer **without** waiting. You cannot do both at once. That's the partition case (CAP's "P").

**2. The surprising part — it costs you even with a *perfect* network.** Here is the insight that elevates PACELC from "nice footnote" to "the real story." Attiya and Welch proved a hard lower bound: the response time of a linearizable read or write is **at least proportional to the uncertainty in network delay**. Not the *average* delay — the *uncertainty* (the spread between fastest and slowest plausible message). Real networks have highly variable delay, so linearizable operations are *inherently* high-latency — **all the time, not only during a partition.** A faster algorithm for linearizability provably does not exist; weaker consistency models, by contrast, can be served from a local replica in microseconds. This lower bound *is* PACELC's "Else" expressed as a theorem: choosing C over L isn't a missed optimisation, it's paying for a guarantee that has a non-negotiable price.

**3. The analogy that makes it click — your laptop already chose AP.** You don't need a datacentre to see this trade-off; it's running inside the CPU you're reading this on. **RAM on a modern multi-core machine is not linearizable.** Each core has its own cache and store buffer; when one core writes a memory address, a second core reading that address moments later is *not* guaranteed to see the new value unless you insert an explicit memory barrier. Why would hardware designers give up linearizability on a *single chip with reliable wires*? Not for fault tolerance — for **speed**. Going to the cache is vastly faster than coordinating across cores. The lesson generalises exactly: most distributed databases that drop linearizability do so for the *same reason your CPU does* — performance — and only secondarily for partition survival. "Strong consistency is slow" is not a database quirk; it's a property of any system with more than one copy of the data.

### The footgun even experts hit: "quorum" ≠ "linearizable"

The simulator above ties CP to "got a quorum." That's a useful teaching model, but be careful in real life: **a Dynamo-style quorum (`w + r > n`) does not, by itself, give you linearizability.** DDIA walks through a concrete race (its Figure 10-6): a write to all three replicas is in flight; reader A's quorum catches one updated node and sees the new value; reader B, whose request *starts after A's finishes*, hits a different quorum and still sees the **old** value. The overlap condition `w + r > n` is satisfied — yet a later read returned an *older* result. Not linearizable. Making quorums truly linearizable requires extra machinery (synchronous read-repair, read-before-write), at a latency cost. And LWW-by-wall-clock systems (Cassandra, ScyllaDB) are "almost certainly nonlinearizable" regardless, because clock skew means timestamps don't faithfully order events. Translation: *the consistency level on the tin is a claim to verify, not a fact to assume* — which is precisely why [Jepsen](https://jepsen.io/analyses) exists.

## 7. Edge Cases & Failure Modes

- **Believing CAP applies to single-datacentre systems.** Inside one datacentre, partitions are extremely rare. CAP *technically* still applies but in practice you tune for latency, not partitions. PACELC's "Else" tells the real story.
- **Calling a system "CA".** Either it is single-machine (CAP irrelevant), or it is lying about its partition behaviour (most common — the system silently fails closed during a partition and the docs do not say so).
- **Treating "CP vs AP" as exhaustive.** It isn't. Because CAP's formal "availability" is so strict, many fault-tolerant systems are *neither* CP nor AP, and most real databases pick C-vs-A *per operation* (DynamoDB strong vs eventual reads; MongoDB by `read/writeConcern`). Forcing a single sticker onto a logo loses the information that actually matters. Ask "what does *this operation* at *this consistency level* do during a partition?" — not "is the database CP or AP?"
- **Over-anchoring on CAP itself.** CAP is a conversation-starter, not a design tool; DDIA calls it "of mostly historical interest." It models *one* fault (partitions, <8% of real incidents) and *one* consistency model (linearizability). If a design review spends more time arguing CP-vs-AP than discussing slow nodes, overload, failover, and the *normal-day* latency/consistency knob (PACELC's "Else"), it's optimising the footnote and ignoring the chapter.
- **Confusing eventual consistency with "any old answer".** Real eventual-consistency systems guarantee *bounded staleness* and *monotonic reads* if you ask for them. We will get into this in [Lesson 13 — Consistency models](/cortex/system-design/building-blocks/consistency-models). If a vendor says "eventually consistent" and cannot quote a staleness bound, walk away.
- **Forgetting that "linearizable" reads are an *option* in CP systems.** Many CP systems (etcd, Spanner) offer "stale read" or "follower read" knobs that trade some consistency for lower latency. Knowing they exist is what distinguishes a senior from a junior using the same database.
- **Designing for the partition that never happens.** If partitions in your network occur once a year for 30 seconds, designing the entire architecture around the rare partition is a bad investment. Design for the *common* case (no partition, low latency), but have a *plan* for partition (drain shedding, fail-closed, alerting).
- **Sync replication != CP.** A system that "synchronously replicates" but does not require a quorum of acks (just one) is *not* CP — a single failure can lose the write.
- **Two-phase commit is not magic.** 2PC blocks on the slowest participant and does not survive coordinator failure cleanly. Modern alternatives — Raft, Paxos, Spanner's Paxos+TrueTime, Calvin — are what production uses. We cover them in [Lesson 14](/cortex/system-design/building-blocks/consensus-paxos-and-raft).

## 8. Practice

> **Exercise 1 — Classify five systems.**
> For each, name its **PACELC** class and a one-sentence justification: PostgreSQL with sync streaming replication; Redis with Sentinel and async replication; DynamoDB with strongly-consistent reads enabled; etcd; Memcached.
>
> <details>
> <summary>Solution</summary>
>
> - **PostgreSQL sync streaming**: PC + EC. Sync ack delays the write; minority can be cut off if the primary fails over.
> - **Redis with Sentinel + async repl**: PA + EL. Async replica can lag; failover may lose the last few writes.
> - **DynamoDB with strongly-consistent reads**: PC + EC for the strong path (it really does refuse / fail over rather than serve stale on partition); PA + EL is the *default* read mode.
> - **etcd**: PC + EC. Raft-based; refuses minority writes; serves linearizable reads via leader.
> - **Memcached**: barely qualifies as a distributed store — there is no replication, so partition behaviour is "the cache is just gone for that shard". Pragmatically AP-flavoured.
>
> </details>

> **Exercise 2 — Add a 9th scenario to the simulator.**
> Edit `src/cap_simulator/demo.py` and add a scenario that demonstrates **lost writes under last-writer-wins**: in AP mode, partition the cluster, write a different value from each side, heal, and observe which write survived. Use the Lamport timestamps printed in the snapshot.
>
> Now: which write would survive if the *earlier* timestamp had been generated *after* the later one — for example because the two clocks drifted? *(Hint: this is exactly why production AP systems use vector clocks or CRDTs instead of plain LWW.)*

> **Exercise 3 — The PM conversation.**
> A product manager asks: *"Can we make this auth system globally available with sub-50 ms response time everywhere, with strong consistency on session tokens?"*
>
> Write a 3-paragraph response (in plain English, no jargon) explaining (a) why all three together are not possible, (b) which combination most auth systems actually pick (and why), and (c) what would have to be true for the PM's full ask to become possible.
>
> <details>
> <summary>Solution sketch</summary>
>
> All three together require global consensus on every write within ~50 ms — but cross-region RTT alone is 100–250 ms. Physics blocks the goal.
>
> Most auth systems pick **regional CP + global eventual sync**: sessions are strongly consistent within a region, eventually replicated to other regions for read. A user logging in from another continent gets directed back to their home region, or a new session is issued and the old one is invalidated lazily. AWS IAM, Google Identity, and Azure AD all do approximately this.
>
> The PM's full ask becomes possible only with (1) sub-50 ms cross-region RTT (does not exist), or (2) acceptance of stale tokens (loses strong consistency), or (3) per-region tokens that do not need to be globally agreed (changes the security model). Pick whichever the product can actually live with.
>
> </details>

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Eric Brewer — CAP twelve years later](https://www.infoq.com/articles/cap-twelve-years-later-how-the-rules-have-changed/)** (2012) — the author of CAP corrects the most common misunderstanding of his own theorem. Required reading.

- **[Daniel Abadi — Problems with CAP, and Yahoo's little known NoSQL system](https://dbmsmusings.blogspot.com/2010/04/problems-with-cap-and-yahoos-little.html)** (2010) — the post that introduced PACELC and reframed the conversation.

- **[Martin Kleppmann — A Critique of the CAP Theorem](https://arxiv.org/abs/1509.05393)** (2015) — the rigorous academic case (by DDIA's author) for *why* CAP's definitions of consistency and availability are too narrow to be useful as a design tool. The source of the "mostly historical interest" verdict this lesson takes seriously.

- **[Kyle Kingsbury — Jepsen analysis archive](https://jepsen.io/analyses)** — the gold standard for actually measuring how each major distributed database behaves under partition. Pick any analysis (MongoDB, Cassandra, etcd, CockroachDB) and watch how a real expert *probes* the consistency claims.

- **[Google — Spanner: Google's Globally-Distributed Database](https://research.google/pubs/spanner-googles-globally-distributed-database/)** (2012) — the paper that introduced TrueTime and showed that *with* hardware support, you can get globally-distributed strong consistency at low cost. Read it after Lesson 14.

- **[Werner Vogels — Eventually Consistent](https://www.allthingsdistributed.com/2008/12/eventually_consistent.html)** (2008) — Amazon CTO's classic essay on what "eventual consistency" actually means and what the practical sub-models are (read-your-writes, monotonic reads, etc.).

---

**Next:** the *normal day* trade-off PACELC pointed at — **latency vs throughput** — and the two ridiculously simple equations (Little's Law and the USL) that explain why doubling servers does not double throughput. → [Lesson 5 — Latency, throughput, and the USL](/cortex/system-design/foundations/latency-throughput-usl)
