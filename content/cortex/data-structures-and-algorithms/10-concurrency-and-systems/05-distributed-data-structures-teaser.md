---
title: Distributed Data Structures (Teaser)
summary: "A short tour of the structures that show up when concurrent graduates to distributed — across machines that fail and partition. CRDTs merge replicas with a conflict-free join; vector clocks give a partial order that tells causality from concurrency; Merkle trees diff two replicas in O(log n). An invitation, not a deep dive."
prereqs:
  - concurrency-and-systems-cas-and-atomics
  - trees-binary-tree-introduction-to-binary-trees
---

## Why It Exists

Everything earlier in this module lived on **one machine**: threads sharing memory, coordinated with [CAS](/cortex/data-structures-and-algorithms/concurrency-and-systems/cas-and-atomics) and atomics. **Distributed** data structures live across *many* machines connected by an unreliable network — and that breaks the assumptions single-machine concurrency leans on:

- **Partitions happen.** Two replicas can both be alive yet unable to talk. You can't block waiting for the other side — it may never answer.
- **There is no shared clock or shared memory.** Each machine has its own copy of the state; "the current value" isn't a single cell anyone can CAS.
- **Messages arrive late, out of order, or twice.** A scheme that only converges if updates arrive once, in order, is no scheme at all.

So distributed structures are built to **converge without coordination**: apply updates in any order, any number of times, and still reach the same answer. This teaser tours three that do exactly that, each addressing a different facet — **CRDTs** (replicas that merge without conflict), **vector clocks** (telling "happened-before" from "concurrent"), and **Merkle trees** (finding *which* replica diverged, cheaply). Each runs in production you've used: CRDTs in Riak and Redis, vector clocks in Dynamo and Cassandra, Merkle trees in Git, Bitcoin, and Cassandra's anti-entropy repair.

## See It Work

A **G-Counter** (grow-only CRDT counter): instead of one shared count, each node owns its own slot and only ever increments *its own*. The value is the sum; merging two replicas takes the element-wise **max**. Two replicas count independently, then gossip — and converge, no locks, no coordinator.

```python run viz=array
class GCounter:
    def __init__(self, node_id, num_nodes):
        self.node_id = node_id
        self.counts = [0] * num_nodes              # one independent slot per node
    def increment(self):
        self.counts[self.node_id] += 1             # only ever touch MY slot
    def value(self):
        return sum(self.counts)
    def merge(self, other):
        self.counts = [max(a, b) for a, b in zip(self.counts, other.counts)]  # element-wise max

a = GCounter(0, 3); b = GCounter(1, 3)
a.increment(); a.increment()                       # replica A counts 2 locally
b.increment()                                      # replica B counts 1 locally
print("before sync -> A sees", a.value(), "| B sees", b.value())
a.merge(b); b.merge(a)                             # gossip both directions
print("after merge -> A sees", a.value(), "| B sees", b.value())
a.merge(b)                                         # merging again changes nothing...
print("merge again -> A sees", a.value())          # ...the merge is IDEMPOTENT
```

```java run viz=array
import java.util.*;
public class Main {
    static class GCounter {
        int nodeId; int[] counts;
        GCounter(int nodeId, int n) { this.nodeId = nodeId; counts = new int[n]; }
        void increment() { counts[nodeId]++; }
        int value() { int s = 0; for (int c : counts) s += c; return s; }
        void merge(GCounter o) { for (int i = 0; i < counts.length; i++) counts[i] = Math.max(counts[i], o.counts[i]); }
    }
    public static void main(String[] x) {
        GCounter a = new GCounter(0, 3), b = new GCounter(1, 3);
        a.increment(); a.increment(); b.increment();
        System.out.println("before sync -> A sees " + a.value() + " | B sees " + b.value());
        a.merge(b); b.merge(a);
        System.out.println("after merge -> A sees " + a.value() + " | B sees " + b.value());
        a.merge(b);
        System.out.println("merge again -> A sees " + a.value());
    }
}
```

Both print `A sees 2 | B sees 1` before sync, then `A sees 3 | B sees 3` after, then `3` again. Before gossiping, the replicas *disagree* — and that's fine, it's expected under a partition. The merge reconciles them with no locking and no leader, and merging a second time is a no-op. Those last two properties are the whole game.

## How It Works

The three structures attack three different facets of "distributed":

```d2
direction: down
challenge: "DISTRIBUTED CHALLENGES\n- partitions: replicas can't always talk\n- no shared clock / memory\n- messages arrive late, reordered, duplicated" {style.fill: "#fde68a"; style.stroke: "#d97706"}
crdt: "CRDTs (e.g. G-Counter, OR-Set)\nmerge = a JOIN in a semilattice:\ncommutative + associative + idempotent\n-> converge for ANY order / duplication, no coordination\n(Riak, Redis, Automerge)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
vc: "VECTOR CLOCKS\nper-node event counts -> a PARTIAL order\ntells 'happened-before' from 'concurrent'\n-> detect conflicting concurrent writes\n(Dynamo, Cassandra, Voldemort)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
merkle: "MERKLE TREES\nhash tree: parent = hash(children)\nequal roots => identical data; differ => walk down\n-> diff two replicas in O(log n), not O(n)\n(Git, Bitcoin, Cassandra repair)" {style.fill: "#e9d5ff"; style.stroke: "#9333ea"}
challenge -> crdt
challenge -> vc
challenge -> merkle
```

<p align="center"><strong>One theme underneath all three: replace "coordinate, then agree" with operations whose <em>structure</em> guarantees agreement — a conflict-free merge, a partial order, or a hash summary.</strong></p>

What makes each one work:

- **CRDT — the merge is a join.** A G-Counter's `merge` (element-wise max) is **commutative** (`merge(a,b) = merge(b,a)`), **associative**, and **idempotent** (`merge(a,a) = a`). Those three laws make the state a *join-semilattice*, and they're exactly what an unreliable network demands: messages can arrive in any order, be reordered, or be delivered twice, and every replica still lands on the same least-upper-bound. No consensus, no leader — convergence is baked into the algebra. Richer CRDTs (PN-Counters that also decrement, OR-Sets, LWW-registers, sequence CRDTs behind collaborative editors) follow the same recipe.
- **Vector clock — a partial order.** Each node tracks a vector of "events I've seen from each node." Event `X` *happened-before* `Y` iff `X`'s vector is ≤ `Y`'s componentwise (and they differ). If *neither* dominates, the events are **concurrent** — produced without knowledge of each other, i.e. a genuine conflict a CRDT or an application must resolve. A single wall-clock timestamp can't express this; the vector can ([Trace It](#trace-it)).
- **Merkle tree — hash the structure.** Each leaf is `hash(data)`, each parent is `hash(left ‖ right)`. If two replicas' **root** hashes match, every leaf matches — one comparison certifies gigabytes. If the roots differ, you descend only into the children whose hashes differ, reaching the changed leaf in **O(log n)** comparisons instead of scanning all *n* records. That's how Cassandra/Dynamo run *anti-entropy repair* and how Git compares trees ([Your Turn](#your-turn)).

> **Key takeaway.** When "concurrent" becomes "distributed across machines that fail and partition," you can't coordinate on every write. The fix is structures that converge *by construction*: **CRDTs** merge replicas with a conflict-free join (commutative + associative + idempotent → order- and duplicate-proof); **vector clocks** impose a partial order that distinguishes causality from true concurrency; **Merkle trees** diff two replicas in O(log n) by comparing hash summaries. Same instinct as lock-free code — design the operation so the dangerous interleavings simply can't produce a wrong answer.

## Trace It

Vector clocks earn their keep on one question a timestamp can't answer: did these two updates *know about* each other, or were they made independently (a conflict)?

**Predict before you run:** node 0 makes an edit, advancing its clock to `[2,1,0]`. Independently, node 1 makes an edit, advancing *its* clock to `[1,2,0]`. Is one of these edits "before" the other — or are they concurrent?

```python run viz=array
def compare(x, y):
    le = all(a <= b for a, b in zip(x, y))   # x <= y componentwise?
    ge = all(a >= b for a, b in zip(x, y))   # x >= y componentwise?
    if le and ge: return "equal"
    if le:        return "X happened-before Y"
    if ge:        return "Y happened-before X"
    return "concurrent (neither dominates)"

print(compare([2, 1, 0], [1, 2, 0]))   # node0 advanced its slot; node1 advanced ITS slot
print(compare([1, 0, 0], [2, 1, 0]))   # second clock saw the first, then advanced
```

<details>
<summary><strong>Reveal</strong></summary>

The first comparison prints `concurrent (neither dominates)`; the second prints `X happened-before Y`. In `[2,1,0]` vs `[1,2,0]`, the first clock is *ahead* in node 0's slot (2 > 1) but *behind* in node 1's slot (1 < 2) — neither vector is ≤ the other, so the events are **concurrent**: each node edited without seeing the other's change. That's a real write-write conflict the system must resolve (last-writer-wins, merge, or surface both "siblings" to the app — what Dynamo does). The second pair `[1,0,0]` ≤ `[2,1,0]` componentwise, so the first event provably *happened-before* the second — no conflict, just causal history. A lone wall-clock timestamp would force an arbitrary "later" winner on the concurrent pair and silently drop a write; the vector clock *detects* the concurrency instead of hiding it. This is the partial-order superpower: `≤` is defined for causally-related events and *undefined* for concurrent ones, and that "undefined" is precisely the signal you need.

</details>

## Your Turn

**Merkle anti-entropy.** Two replicas each hold 4 records; one record was edited on replica B. Naively, you'd ship all 4 records across the network to find the difference. Instead they exchange a Merkle tree of hashes.

**Predict:** to pinpoint the one changed record, how many subtree comparisons does the descent need — all 4 leaves, or fewer?

```python run viz=array
def h(s):                                  # deterministic string hash (identical in Java below)
    acc = 0
    for ch in s:
        acc = (acc * 31 + ord(ch)) % 1_000_000_007
    return acc
def combine(a, b):
    return h(str(a) + "|" + str(b))        # parent = hash of its two children
def merkle(leaves):                        # levels[0] = leaf hashes ... levels[-1] = [root]
    level = [h(x) for x in leaves]
    levels = [level]
    while len(level) > 1:
        level = [combine(level[i], level[i + 1]) for i in range(0, len(level), 2)]
        levels.append(level)
    return levels

A = ["alice", "bob", "carol", "dave"]
B = ["alice", "bob", "carol", "DAVE"]      # exactly one record differs
ta, tb = merkle(A), merkle(B)
print("roots equal?", ta[-1][0] == tb[-1][0])         # one comparison certifies the whole replica

idx, level, compares = 0, len(ta) - 1, 0   # descend from root, following the mismatch
while level > 0:
    child = level - 1
    left, right = 2 * idx, 2 * idx + 1
    compares += 1
    idx = left if ta[child][left] != tb[child][left] else right
    level = child
print("differing leaf index:", idx, "->", A[idx], "vs", B[idx])
print("subtree checks to localise:", compares, "(not", len(A), "leaf scans)")
```

```java run viz=array
import java.util.*;
public class Main {
    static long h(String s) {
        long acc = 0;
        for (int i = 0; i < s.length(); i++) acc = (acc * 31 + s.charAt(i)) % 1_000_000_007L;
        return acc;
    }
    static long combine(long a, long b) { return h(a + "|" + b); }
    static List<long[]> merkle(String[] leaves) {
        long[] level = new long[leaves.length];
        for (int i = 0; i < leaves.length; i++) level[i] = h(leaves[i]);
        List<long[]> levels = new ArrayList<>(); levels.add(level);
        while (level.length > 1) {
            long[] next = new long[level.length / 2];
            for (int i = 0; i < level.length; i += 2) next[i / 2] = combine(level[i], level[i + 1]);
            level = next; levels.add(level);
        }
        return levels;
    }
    public static void main(String[] x) {
        String[] A = {"alice", "bob", "carol", "dave"};
        String[] B = {"alice", "bob", "carol", "DAVE"};
        List<long[]> ta = merkle(A), tb = merkle(B);
        System.out.println("roots equal? " + (ta.get(ta.size() - 1)[0] == tb.get(tb.size() - 1)[0]));
        int idx = 0, level = ta.size() - 1, compares = 0;
        while (level > 0) {
            int child = level - 1, left = 2 * idx, right = 2 * idx + 1;
            compares++;
            idx = (ta.get(child)[left] != tb.get(child)[left]) ? left : right;
            level = child;
        }
        System.out.println("differing leaf index: " + idx + " -> " + A[idx] + " vs " + B[idx]);
        System.out.println("subtree checks to localise: " + compares + " (not " + A.length + " leaf scans)");
    }
}
```

Both print `roots equal? false`, then `differing leaf index: 3 -> dave vs DAVE`, then `subtree checks to localise: 2 (not 4 leaf scans)`. The mismatched root proves *something* diverged without transferring a single record; the descent then follows only the differing branch — **log₂4 = 2** comparisons — to land on `dave`. Scale that to a replica with a million records: ~20 comparisons instead of a million, which is why Merkle trees are the backbone of replica repair and content addressing.

## Reflect & Connect

- **Distributed ≠ just "more concurrent."** Partitions, no shared clock, and reordered/duplicated messages mean you can't coordinate every write. The answer is structures that converge *without* coordination.
- **CRDTs = a conflict-free merge.** Make the merge commutative, associative, and idempotent (a semilattice join) and replicas reach the same value regardless of message order or duplication — no consensus needed.
- **Vector clocks = a partial order.** They distinguish "X causally before Y" from "X and Y concurrent." The concurrent case is the *conflict* a single timestamp would silently drop.
- **Merkle trees = hash the structure.** Equal roots certify equality in one comparison; differing roots localise the change in O(log n). Anti-entropy repair, Git, blockchains.
- **Same instinct as lock-free code.** From [CAS](/cortex/data-structures-and-algorithms/concurrency-and-systems/cas-and-atomics) to the [lock-free queue](/cortex/data-structures-and-algorithms/concurrency-and-systems/lock-free-queue) to CRDTs: design the operation so the dangerous interleavings *can't* yield a wrong answer — locally with atomics, globally with algebra. This is a teaser; distributed systems (consensus, replication, CAP) is a whole subject from here.

## Recall

<details>
<summary><strong>Q:</strong> Why can't distributed structures coordinate on every write the way single-machine concurrency does?</summary>

**A:** Network partitions, asynchronous/failing nodes, and no shared clock or memory. A replica may be unable to reach the others (or never get a reply), so blocking for coordination would stall or fail. Distributed structures instead converge *without* synchronous coordination.

</details>
<details>
<summary><strong>Q:</strong> What three properties make a CRDT merge work, and why do they matter?</summary>

**A:** Commutative, associative, idempotent (a join-semilattice). They mean replicas converge to the same value no matter the order messages arrive, how they're grouped, or whether they're delivered more than once — exactly the failure modes of an unreliable network. (G-Counter merge = element-wise max satisfies all three.)

</details>
<details>
<summary><strong>Q:</strong> How does a vector clock tell "happened-before" from "concurrent"?</summary>

**A:** Event X happened-before Y iff X's vector ≤ Y's componentwise (and they differ). If neither vector dominates the other, the events are concurrent — made independently, a genuine write-write conflict. A single timestamp can't represent this partial order, so it would force an arbitrary winner and drop a write.

</details>
<details>
<summary><strong>Q:</strong> How does a Merkle tree diff two replicas in O(log n)?</summary>

**A:** Leaves are hashes of records; each parent is the hash of its children. Equal root hashes prove the whole replica matches in one comparison. If roots differ, you descend only into the child subtree whose hash differs, reaching the changed leaf in O(log n) comparisons instead of scanning all n records.

</details>
<details>
<summary><strong>Q:</strong> What's the common instinct linking CRDTs, vector clocks, Merkle trees, and lock-free code?</summary>

**A:** Design the operation so dangerous interleavings can't produce a wrong answer — rather than locking them out. Lock-free code does it locally with atomic primitives (CAS); distributed structures do it globally with algebra (a conflict-free merge, a partial order, a hash summary).

</details>

## Sources & Verify

- **Shapiro, Preguiça, Baquero & Zawirski** (2011), "Conflict-free Replicated Data Types" — the CRDT formalism (CvRDT semilattices and CmRDT commutative ops); G-Counter and OR-Set are the canonical examples.
- **Lamport** (1978), "Time, Clocks, and the Ordering of Events in a Distributed System" — the happened-before relation; **Fidge** and **Mattern** (1988) introduced vector clocks for the full partial order.
- **Merkle** (1987), "A Digital Signature Based on a Conventional Encryption Function" — hash trees; **DeCandia et al.** (2007), "Dynamo: Amazon's Highly Available Key-value Store" — vector clocks + Merkle-tree anti-entropy in production. Git and Bitcoin use Merkle trees for content addressing.
- The G-Counter convergence (`2|1 → 3|3`, idempotent `3`), the vector-clock `concurrent`/`happened-before` verdicts, and the Merkle `differing leaf 3 (dave vs DAVE)` in 2 checks all come from the runnable blocks above (single-threaded deterministic simulations of inherently distributed mechanisms) — re-run to verify.
