---
title: RCU and Hazard Pointers
summary: "How lock-free code frees memory safely — the unsolved problem from the lock-free queue. Hazard pointers publish a per-thread 'I'm using this' marker so a node is freed only when no marker references it; RCU defers freeing until a grace period (all pre-existing readers finish). Both run the Linux kernel."
prereqs:
  - concurrency-and-systems-cas-and-atomics
  - concurrency-and-systems-lock-free-queue
---

## Why It Exists

The [lock-free queue](/cortex/data-structures-and-algorithms/concurrency-and-systems/lock-free-queue) left a loose end: after you dequeue a node, *when can you free it?* Another thread may have read that node's pointer a microsecond before you unlinked it, and it's about to dereference it. Free too early and that thread reads freed — possibly *reallocated* — memory: a **use-after-free**, and the root cause of the [ABA problem](/cortex/data-structures-and-algorithms/concurrency-and-systems/cas-and-atomics) (a freed node's address gets reused, fooling a CAS).

In a garbage-collected language this is the GC's job — it won't reclaim an object anyone can still reach. But in C, C++, or the kernel, *you* must reclaim manually *and* safely, without a stop-the-world GC. Two schemes dominate. **Hazard pointers**: each thread publishes the pointer it's currently dereferencing into a shared slot; before freeing a node, you scan every thread's slots and free only if no one is using it. **RCU (Read-Copy-Update)**: readers mark a lightweight "read-side critical section"; an updater unlinks the old node and then waits for a **grace period** — until every reader that *started before the unlink* has finished — after which no one can hold the old pointer, so freeing is safe. Both are everywhere in the Linux kernel and in lock-free libraries.

## See It Work

Hazard pointers in miniature: a reader publishes the node it holds; `retire` frees a node only if it isn't hazarded, else defers it for a later `scan`. (Single-threaded simulation for determinism; real implementations run this across threads with atomic slots.)

```python run viz=array
class Node:
    def __init__(self, value): self.value = value; self.freed = False

class Reclaimer:
    def __init__(self):
        self.hazards = set()                               # addresses threads have published
        self.retired = []                                  # nodes waiting to be freed
    def publish(self, node): self.hazards.add(id(node))    # a reader: "I'm using this node"
    def clear(self, node): self.hazards.discard(id(node))  # ...done with it
    def _free(self, node): node.freed = True; node.value = "<REUSED>"   # simulate free + reuse
    def retire(self, node):                                # want to free node now
        if id(node) in self.hazards:
            self.retired.append(node); return "deferred"   # someone's using it -> wait
        self._free(node); return "freed"
    def scan(self):                                        # retry the deferred frees
        keep = []
        for n in self.retired:
            if id(n) in self.hazards: keep.append(n)
            else: self._free(n)
        self.retired = keep

r = Reclaimer()
x = Node(42)
r.publish(x)                                               # a reader is holding x
print(r.retire(x), x.freed)                                # deferred False  (can't free a hazarded node)
r.clear(x)                                                 # reader finishes
r.scan()
print(x.freed)                                             # True  (now safe to free)
```

```java run viz=array
import java.util.*;
public class Main {
    static class Node { Object value; boolean freed = false; Node(Object v) { value = v; } }
    static class Reclaimer {
        Set<Node> hazards = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Node> retired = new ArrayList<>();
        void publish(Node n) { hazards.add(n); }           // "I'm using this"
        void clear(Node n) { hazards.remove(n); }
        void free(Node n) { n.freed = true; n.value = "<REUSED>"; }
        String retire(Node n) { if (hazards.contains(n)) { retired.add(n); return "deferred"; } free(n); return "freed"; }
        void scan() {
            List<Node> keep = new ArrayList<>();
            for (Node n : retired) { if (hazards.contains(n)) keep.add(n); else free(n); }
            retired = keep;
        }
    }
    public static void main(String[] args) {
        Reclaimer r = new Reclaimer();
        Node x = new Node(42);
        r.publish(x);
        System.out.println(r.retire(x) + " " + x.freed);   // deferred false
        r.clear(x); r.scan();
        System.out.println(x.freed);                       // true
    }
}
```

Both print `deferred False`/`deferred false` then `True`/`true`. While a reader's hazard pointer references `x`, `retire` *refuses* to free it and queues it; once the reader clears its hazard and a later `scan` runs, the node is finally reclaimed. The node never gets freed out from under a live reader.

## How It Works

Both schemes answer "is anyone still using this node?" — one by *publishing usage*, the other by *waiting out the readers*:

```d2
direction: down
problem: "RECLAMATION PROBLEM: a thread unlinks node X,\nbut another may still hold X's pointer.\nFree too early -> use-after-free / ABA." {style.fill: "#fecaca"; style.stroke: "#dc2626"}
hp: "HAZARD POINTERS\nreader: publish(X) before dereferencing\nreclaimer: free X only if X is in NO thread's slot\n-> per-access cost, but BOUNDED retired memory" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
rcu: "RCU (Read-Copy-Update)\nreader: cheap read-side critical section (no per-node marker)\nupdater: unlink X, then wait a GRACE PERIOD\n(all readers active at unlink time exit) -> free X\n-> readers nearly free, reclamation DEFERRED/batched" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
problem -> hp
problem -> rcu
```

<p align="center"><strong>Hazard pointers make readers <em>announce</em> what they hold, so the reclaimer frees only unreferenced nodes (bounded memory, per-access cost). RCU makes readers cheap and instead <em>waits out</em> a grace period — until every reader that could have seen the old node has finished — then frees in a batch.</strong></p>

Three load-bearing facts:

- **The grace period is "all pre-existing readers are gone."** In RCU, an updater unlinks the node so *new* readers can't reach it, then calls `synchronize_rcu()` which blocks until every reader that was *already inside a read-side critical section at unlink time* has exited. Once that snapshot of readers has drained, no one can possibly hold the old pointer, so freeing is safe. Crucially, a reader that *enters after* the unlink doesn't extend the wait — it never saw the old node ([Your Turn](#your-turn)).
- **Hazard pointers publish, then the reclaimer scans.** Before dereferencing a node, a thread writes its address into a per-thread hazard slot (and re-checks the node is still linked). To free, the reclaimer collects all threads' hazard slots and frees only addresses absent from that set; the rest stay on a retire list and are retried later. This caps the number of un-freed nodes at roughly (threads × hazards-per-thread).
- **They trade reader cost against memory.** RCU readers are almost free (often just a compiler barrier — no atomic writes per access), but reclamation is *deferred* and batched, so freed memory lags and a stalled reader can delay a grace period indefinitely. Hazard pointers pay an atomic store per protected access, but bound the retired memory tightly and free promptly. RCU suits read-mostly kernel data; hazard pointers suit user-space structures needing tight memory.

> **Key takeaway.** Lock-free structures can't free a node while a concurrent reader may hold its pointer (use-after-free, the ABA root). **Hazard pointers** publish per-thread "in-use" markers; the reclaimer frees only un-referenced nodes (bounded memory, per-access cost). **RCU** keeps readers cheap and defers freeing until a **grace period** — all readers active at unlink time have exited. Both give safe manual reclamation without a GC; both run the Linux kernel.

## Trace It

The danger is abstract until you watch a free land on a node someone's still reading.

**Predict before you run:** a lock-free pop returns node `x` (value `42`) and the popper frees it *immediately*. A concurrent reader grabbed `x`'s pointer a moment earlier and is about to read `x.value`. With **no** reclamation scheme, what does that reader see — `42`, or something else?

```python run viz=array
class Node:
    def __init__(self, value): self.value = value; self.freed = False
class Reclaimer:
    def __init__(self): self.hazards = set(); self.retired = []
    def publish(self, node): self.hazards.add(id(node))
    def clear(self, node): self.hazards.discard(id(node))
    def _free(self, node): node.freed = True; node.value = "<REUSED>"   # the allocator reuses the slot
    def retire(self, node):
        if id(node) in self.hazards:
            self.retired.append(node); return "deferred"
        self._free(node); return "freed"

# UNPROTECTED: free immediately while a reader still holds the pointer
x = Node(42)
reader_ref = x                                             # a concurrent reader grabbed the pointer
Reclaimer().retire(x)                                      # popper frees right away (no hazard published)
print("unprotected reader sees:", reader_ref.value)

# PROTECTED: the reader publishes a hazard pointer BEFORE the free
x = Node(42)
reader_ref = x
r = Reclaimer(); r.publish(x)                              # reader announces it holds x
print("retire result:", r.retire(x))                       # deferred
print("protected reader sees:", reader_ref.value)
```

<details>
<summary><strong>Reveal</strong></summary>

The **unprotected** reader sees `<REUSED>`, not `42` — a textbook **use-after-free**. The popper freed `x` the instant it dequeued it, the allocator handed that memory slot to something else (here simulated by overwriting the value), and the concurrent reader — still holding the old pointer — dereferenced reallocated memory and read garbage. In real C this is undefined behaviour: a crash, a silent data corruption, or worse, the exact mechanism behind the [ABA problem](/cortex/data-structures-and-algorithms/concurrency-and-systems/cas-and-atomics) (the reused address fools a later CAS into "succeeding"). The **protected** reader sees `42`, correctly, because it published a hazard pointer *before* dereferencing; `retire` saw the hazard, returned `deferred`, and left `x` intact until the reader finishes and clears its marker. That's the entire job of these schemes: bridge the gap between "logically removed from the structure" and "physically safe to free." A node can be unlinked (no new reader can find it) yet still be *held* by an old reader — and you must not free it until that gap closes, which hazard pointers detect explicitly and RCU waits out via the grace period.

</details>

## Your Turn

**RCU grace period.** A reader enters a read-side critical section; an updater unlinks a node and must wait until every reader *active at that moment* exits before freeing. Show that a reader entering *after* the unlink doesn't delay the free — it can't have seen the old node.

```python run viz=array
class RCU:
    def __init__(self): self.active = {}; self.next_id = 0
    def read_lock(self):
        rid = self.next_id; self.next_id += 1; self.active[rid] = True; return rid
    def read_unlock(self, rid): self.active.pop(rid, None)
    def synchronize(self):                                 # grace period: snapshot the readers we must wait for
        return set(self.active)

rcu = RCU()
a = rcu.read_lock()                                        # reader A enters (might hold the old node)
grace = rcu.synchronize()                                  # updater unlinks, then waits for {A} to exit
b = rcu.read_lock()                                        # reader B enters AFTER the unlink -> can't see old node

def can_free():
    return grace.isdisjoint(rcu.active)                    # safe once every snapshot-reader has exited

print("A in, B in:", can_free())                           # False  (A from the snapshot is still active)
rcu.read_unlock(a)                                         # A exits -> grace period complete
print("A out, B in:", can_free())                          # True   (B was not in the snapshot, so it's irrelevant)
```

```java run viz=array
import java.util.*;
public class Main {
    static class RCU {
        Set<Integer> active = new HashSet<>(); int nextId = 0;
        int readLock() { int id = nextId++; active.add(id); return id; }
        void readUnlock(int id) { active.remove(id); }
        Set<Integer> synchronize() { return new HashSet<>(active); }   // grace-period snapshot
    }
    public static void main(String[] args) {
        RCU rcu = new RCU();
        int a = rcu.readLock();                            // reader A enters
        Set<Integer> grace = rcu.synchronize();            // wait for {A}
        int b = rcu.readLock();                            // reader B enters AFTER unlink
        System.out.println("A in, B in: " + Collections.disjoint(grace, rcu.active));   // false
        rcu.readUnlock(a);                                 // A exits
        System.out.println("A out, B in: " + Collections.disjoint(grace, rcu.active));  // true
    }
}
```

Both print `False`/`false` then `True`/`true`. While reader `A` (captured in the grace-period snapshot) is active, the free must wait. The moment `A` exits, the grace period is complete and freeing is safe — *even though reader `B` is still running*, because `B` entered after the unlink and so could never have obtained the old node's pointer. That's the elegance of RCU: readers pay almost nothing, and the updater only ever waits for the bounded set of readers that were already in flight.

## Reflect & Connect

- **The reclamation gap.** A node can be *unlinked* (no new reader finds it) yet still *held* by an old reader. Freeing in that gap is use-after-free — the ABA root. Safe reclamation closes the gap.
- **Hazard pointers: publish then scan.** Readers announce the pointer they hold; the reclaimer frees only addresses no one published. Per-access atomic cost, but tightly bounded retired memory.
- **RCU: cheap readers, grace-period free.** Readers mark a critical section almost for free; the updater unlinks, then waits until all readers active at unlink time exit, then frees in a batch. A late-arriving reader never delays it.
- **The trade-off.** RCU minimises reader cost but defers/batches reclamation (a stalled reader stalls a grace period); hazard pointers bound memory and free promptly but pay per protected access. Read-mostly kernel data → RCU; user-space with tight memory → hazard pointers.
- **It completes Part 10.** [CAS](/cortex/data-structures-and-algorithms/concurrency-and-systems/cas-and-atomics) builds the [lock-free queue](/cortex/data-structures-and-algorithms/concurrency-and-systems/lock-free-queue) and [concurrent map](/cortex/data-structures-and-algorithms/concurrency-and-systems/concurrent-hash-map); RCU/hazard pointers make freeing their nodes *safe*. Without them, every lock-free structure leaks or crashes — which is why GC languages, where this is automatic, can ignore the whole problem.

## Recall

<details>
<summary><strong>Q:</strong> What problem do RCU and hazard pointers solve?</summary>

**A:** Safe memory reclamation in lock-free code: when can you free a node that a concurrent reader might still hold a pointer to? Freeing too early is a use-after-free (and the root of the ABA problem). They defer/guard the free until no reader holds the node.

</details>
<details>
<summary><strong>Q:</strong> How do hazard pointers work?</summary>

**A:** Before dereferencing a node, a thread publishes its address into a per-thread hazard slot. To free a node, the reclaimer scans all threads' hazard slots and frees only nodes that appear in none; the rest are retired and retried later. Memory un-freed is bounded by (threads × hazards each).

</details>
<details>
<summary><strong>Q:</strong> What is an RCU grace period?</summary>

**A:** After an updater unlinks a node, the interval until every reader that was *active at unlink time* has exited its read-side critical section. Once that snapshot of readers drains, no one can hold the old pointer, so freeing is safe. Readers entering after the unlink don't extend it.

</details>
<details>
<summary><strong>Q:</strong> Why doesn't a reader that enters after the unlink delay an RCU free?</summary>

**A:** Because the node was already unlinked, a reader entering afterward can't reach it — it never obtains the old pointer. Only readers active *before* the unlink could be holding it, so only they (the grace-period snapshot) must finish.

</details>
<details>
<summary><strong>Q:</strong> What's the main trade-off between RCU and hazard pointers?</summary>

**A:** RCU makes reads nearly free (no per-access atomic) but defers and batches reclamation (a stalled reader can hold up a grace period and memory). Hazard pointers pay an atomic store per protected access but bound retired memory and free promptly. Read-mostly → RCU; tight memory → hazard pointers.

</details>

## Sources & Verify

- **McKenney et al.**, "Read-Copy Update" (and the Linux kernel RCU documentation) — grace periods, quiescent states, and `synchronize_rcu`.
- **Michael** (2004), "Hazard Pointers: Safe Memory Reclamation for Lock-Free Objects", *IEEE TPDS* — the original hazard-pointer scheme and its bounded-memory guarantee.
- **Herlihy & Shavit**, *The Art of Multiprocessor Programming* (2nd ed.) — memory reclamation in lock-free structures; **folly**/`std::hazard_pointer` (C++26) and **liburcu** are production implementations. The `deferred`/`True` hazard demo, the use-after-free `<REUSED>` vs protected `42`, and the RCU `False`/`True` grace-period checks above come from the runnable blocks (single-threaded *simulations* of inherently concurrent mechanisms) — re-run to verify.
