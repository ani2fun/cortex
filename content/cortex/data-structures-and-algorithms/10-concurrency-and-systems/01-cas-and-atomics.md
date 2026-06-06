---
title: CAS and Atomics
summary: "Compare-And-Swap — the one hardware primitive under every lock-free structure. CAS(addr, expected, new) atomically writes new only if the value is still expected, else fails; the universal pattern is an optimistic read-compute-CAS-retry loop. Its classic trap is ABA, fixed with a version stamp."
prereqs:
  - foundations-memory-model-and-cache
---

## Why It Exists

Two threads run `count++` on a shared counter. That's three steps — read, add, write — and the threads interleave: both read `5`, both write `6`, and one increment vanishes. A mutex around the increment fixes it, but locks are heavy (hundreds of cycles to acquire, and on a hot variable the lock itself becomes the bottleneck) and they *block* — a thread holding the lock that gets descheduled stalls everyone.

**Compare-And-Swap (CAS)** is the hardware primitive that lets you skip the lock. `CAS(address, expected, new)` does, *as one indivisible instruction*: read the value at `address`; if it equals `expected`, store `new` and report success; otherwise change nothing and report failure. The whole read-compare-write happens atomically, so no other thread can sneak in between. Every lock-free data structure — counters, stacks, queues, hash maps — is built on it, via one universal pattern: **read the current value, compute the new one, and CAS it in; if the CAS fails (someone else got there first), re-read and retry.** No locks, no blocking, guaranteed system-wide progress.

## See It Work

CAS succeeds only if the value is *still* what you expected. (Python has no real CAS — the GIL serialises bytecode — so we *simulate* the semantics with a function; Java's `java.util.concurrent.atomic` gives the genuine hardware-backed operation.)

```python run viz=array
def cas(cell, expected, new):                          # SIMULATED: real CAS is one atomic HW instruction
    if cell[0] == expected:                            # compare...
        cell[0] = new                                  # ...and swap, indivisibly
        return True
    return False                                       # value changed first -> fail, change nothing

cell = [0]
print(cas(cell, 0, 1), cell[0])                        # True 1   (0 was there, now 1)
print(cas(cell, 0, 5), cell[0])                        # False 1  (current is 1, not 0 -> no change)
print(cas(cell, 1, 2), cell[0])                        # True 2

def atomic_inc(cell):                                  # lock-free increment: read, compute, CAS, retry
    while True:
        old = cell[0]
        if cas(cell, old, old + 1):
            return cell[0]
print(atomic_inc(cell))                                # 3
```

```java run viz=array
import java.util.concurrent.atomic.AtomicInteger;
public class Main {
    public static void main(String[] args) {
        AtomicInteger cell = new AtomicInteger(0);     // REAL CAS via compareAndSet
        System.out.println(cell.compareAndSet(0, 1) + " " + cell.get());   // true 1
        System.out.println(cell.compareAndSet(0, 5) + " " + cell.get());   // false 1
        System.out.println(cell.compareAndSet(1, 2) + " " + cell.get());   // true 2
        int old;
        do { old = cell.get(); } while (!cell.compareAndSet(old, old + 1));  // retry loop
        System.out.println(cell.get());                // 3
    }
}
```

Both print `true 1`, `false 1`, `true 2`, then `3`. The second CAS fails because the value moved to `1` — exactly the protection you want: if another thread changed the value out from under you, your CAS *refuses* to clobber it, and you retry with the fresh value. Single-threaded here it's deterministic; under real contention the retry loop is what makes the increment correct without a lock.

## How It Works

The retry loop is the heartbeat of lock-free programming. Read a snapshot, compute off it, and let CAS adjudicate whether your snapshot was still current:

```d2
direction: down
read: "1. old = load(address)   (take a snapshot)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
compute: "2. new = f(old)         (compute off the snapshot)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
cas: "3. CAS(address, old, new)\n   atomic: write new ONLY IF value is still old" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
ok: "success -> done\n(no one changed it since your snapshot)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
retry: "failure -> LOOP back to step 1\n(someone won the race; re-read and recompute)" {style.fill: "#fecaca"; style.stroke: "#dc2626"}
read -> compute
compute -> cas
cas -> ok
cas -> retry
retry -> read
```

<p align="center"><strong>Optimistic concurrency: assume no conflict, compute off a snapshot, and let CAS verify the snapshot was still valid at write time. A failed CAS means a conflict happened — re-read and retry. No locks, no blocking.</strong></p>

Three load-bearing facts:

- **CAS is atomic read-compare-write; the retry loop builds everything else.** A single CAS gives you a lock-free *update* of one word. Wrap it in `while not CAS(...): re-read` and you get atomic increment, atomic-max, push onto a lock-free stack, and so on. A failed CAS isn't an error — it's the signal that a competitor committed first, so you fold in their change and try again. This is **lock-free**: some thread always makes progress (the winner), no one blocks waiting on a lock.
- **Memory ordering makes the writes *visible*.** CAS guarantees atomicity, but on multicore, one thread's writes aren't automatically seen by another in program order — caches and compilers reorder. Atomics carry a *memory ordering* (acquire on reads, release on writes, or full sequential consistency) that pins down what other threads must observe. Get the ordering wrong and a correct-looking CAS algorithm still races; "atomic" and "ordered" are separate guarantees you need both of.
- **CAS compares the *value*, not the *history* — hence ABA.** CAS can't tell that a value left and came back. If it reads `A`, someone changes it `A→B→A`, and then your CAS for `A` *succeeds*, you've been fooled: the world changed even though the value looks identical (a freed-and-reused pointer is the classic disaster). The [Trace It](#trace-it) shows it, and the standard fix is a **version stamp** (CAS the pair `(value, counter)`, bumping the counter on every change) — or hardware **LL/SC** (load-linked / store-conditional on ARM/POWER), where the store fails if the location was written *at all* since the load, even back to the same value.

> **Key takeaway.** `CAS(addr, expected, new)` atomically writes `new` only if the value is still `expected`. The universal lock-free pattern is **read → compute → CAS → retry-on-failure** (optimistic concurrency, no locks). Two caveats: you need correct **memory ordering** for cross-thread visibility, and CAS's value-only check causes the **ABA problem**, fixed with a version stamp (or LL/SC).

## Trace It

CAS feels bulletproof — "it only writes if nothing changed" — but it checks *equality of value*, not *absence of change*. That gap has a name and a famous failure mode.

**Predict before you run:** a thread reads value `A` and is paused. While it's paused, other threads change the location `A → B → A`. The thread wakes and does `CAS(expected=A, new=X)`. Does the CAS **succeed** — and is succeeding the *correct* behaviour?

```python run viz=array
def cas(cell, expected, new):
    if cell[0] == expected:
        cell[0] = new
        return True
    return False

# Naive value-only CAS
cell = ["A"]
old = cell[0]                                          # thread T snapshots 'A', then is preempted
cas(cell, "A", "B"); cas(cell, "B", "A")              # others do A -> B -> A while T sleeps
print("naive CAS(expected 'A') succeeds:", cas(cell, old, "X"))

# Versioned CAS: compare a (value, version) stamp instead
def vcas(cell, expected, new_val):
    if cell[0] == expected:
        cell[0] = (new_val, cell[0][1] + 1)            # bump version on every successful swap
        return True
    return False

vcell = [("A", 0)]
old_v = vcell[0]                                       # ('A', 0)
vcas(vcell, ("A", 0), "B"); vcas(vcell, ("B", 1), "A")   # A@0 -> B@1 -> A@2
print("versioned CAS(expected ('A',0)) succeeds:", vcas(vcell, old_v, "X"))
```

<details>
<summary><strong>Reveal</strong></summary>

The naive CAS **succeeds** (`True`) — and that's the **bug**. The value is `A` again, which matches what the thread expected, so CAS happily writes `X`. But the location *did* change while the thread slept: it went `A → B → A`. If `A` were a pointer to a node, that node might have been popped, freed, and a *different* node reallocated at the same address — so the CAS "succeeds" against a stale, meaningless `A` and corrupts the structure. This is the **ABA problem**: CAS compares values, and a value that leaves and returns is indistinguishable from one that never moved.

The versioned CAS **fails** (`False`), correctly. By stamping every value with a monotonically increasing version, `A@0` and `A@2` are *different* even though the visible value `A` is the same — so the thread expecting `(A, 0)` is told "no, things changed, re-read." That's the standard fix (Java's `AtomicStampedReference` does exactly this; lock-free algorithms also use hazard pointers or epoch/RCU reclamation, [coming up](/cortex/data-structures-and-algorithms/concurrency-and-systems-rcu-and-hazard-pointers)). The deeper lesson: "the value is unchanged" is *not* the same as "nothing happened," and lock-free code has to close that gap explicitly — with a counter, a stamp, or LL/SC hardware that fails on *any* intervening write.

</details>

## Your Turn

**Lock-free update-to-max** — atomically raise a shared value to `x` only if `x` is larger, with no lock. It's the read-compute-CAS-retry loop with a comparison: read the current max, bail if `x` isn't bigger, else CAS it in (retrying if someone else updated first).

```python run viz=array
def cas(cell, expected, new):
    if cell[0] == expected:
        cell[0] = new
        return True
    return False

def atomic_max(cell, x):
    while True:
        old = cell[0]
        if x <= old:
            return old                                 # already >= x: nothing to do
        if cas(cell, old, x):                          # try to raise it; retry if a competitor won
            return x

cell = [0]
print(atomic_max(cell, 5))     # 5
print(atomic_max(cell, 3))     # 5   (3 isn't larger -> no update)
print(atomic_max(cell, 9))     # 9
```

```java run viz=array
import java.util.concurrent.atomic.AtomicInteger;
public class Main {
    static int atomicMax(AtomicInteger cell, int x) {
        while (true) {
            int old = cell.get();
            if (x <= old) return old;
            if (cell.compareAndSet(old, x)) return x;
        }
    }
    public static void main(String[] args) {
        AtomicInteger cell = new AtomicInteger(0);
        System.out.println(atomicMax(cell, 5));   // 5
        System.out.println(atomicMax(cell, 3));   // 5
        System.out.println(atomicMax(cell, 9));   // 9
    }
}
```

Both print `5`, `5`, `9`. The middle call returns early because `3 ≤ 5` — no wasted CAS. The pattern generalises: any read-modify-write you can express as "compute a candidate, then commit it if the input hasn't moved" becomes lock-free this way. (`AtomicInteger` ships `updateAndGet`/`accumulateAndGet`, which *are* this loop wrapped up.) Single-threaded the CAS never fails; the loop earns its keep only under contention, where a losing thread silently re-reads and tries again — making progress, never blocking.

## Reflect & Connect

- **One primitive, one pattern.** `CAS(addr, expected, new)` plus the read-compute-CAS-retry loop is the foundation of all lock-free programming. A failed CAS means "a competitor committed; re-read and retry" — optimistic concurrency, no locks.
- **Atomic ≠ ordered.** CAS gives atomicity; *memory ordering* (acquire/release/seq-cst) gives cross-thread visibility. Lock-free correctness needs both, and the ordering bugs are the subtle ones.
- **ABA: value-equality isn't change-absence.** CAS can be fooled by `A→B→A`. Fix with a version stamp (`AtomicStampedReference`), hazard pointers / [RCU](/cortex/data-structures-and-algorithms/concurrency-and-systems-rcu-and-hazard-pointers), or LL/SC hardware that fails on *any* intervening write.
- **Lock-free vs lock-based.** Locks block (a descheduled holder stalls everyone); lock-free guarantees *some* thread always progresses. CAS trades the lock's simplicity for non-blocking progress and the retry-loop discipline.
- **It's the base of the next lessons.** Treiber's [lock-free queue/stack](/cortex/data-structures-and-algorithms/concurrency-and-systems-lock-free-queue) and [concurrent hash maps](/cortex/data-structures-and-algorithms/concurrency-and-systems-concurrent-hash-map) are CAS retry loops on a `head`/bucket pointer. Master CAS + ABA and the rest of Part 10 is variations on this theme.

## Recall

<details>
<summary><strong>Q:</strong> What does <code>CAS(addr, expected, new)</code> do, atomically?</summary>

**A:** It reads the value at `addr`; if it equals `expected`, it stores `new` and returns success; otherwise it changes nothing and returns failure. The entire read-compare-write is one indivisible operation.

</details>
<details>
<summary><strong>Q:</strong> What is the universal lock-free pattern built on CAS?</summary>

**A:** Read the current value (snapshot), compute the new value off it, then `CAS(value, old_snapshot, new)`. On failure (someone changed it first), re-read and retry. Optimistic concurrency — no locks, non-blocking.

</details>
<details>
<summary><strong>Q:</strong> Why isn't atomicity enough — what else does lock-free code need?</summary>

**A:** Correct *memory ordering*. CAS guarantees the operation is atomic, but multicore caches/compilers can reorder other reads and writes; acquire/release/sequential-consistency orderings pin down what other threads must observe. Atomic and ordered are separate guarantees.

</details>
<details>
<summary><strong>Q:</strong> What is the ABA problem?</summary>

**A:** CAS compares values, not histories. If a location goes `A → B → A`, a CAS expecting `A` succeeds even though the location changed in between — dangerous when `A` is a pointer that was freed and reused. CAS can't distinguish "never changed" from "changed and changed back."

</details>
<details>
<summary><strong>Q:</strong> How do you fix ABA?</summary>

**A:** Attach a monotonically increasing version stamp and CAS the `(value, version)` pair (e.g. `AtomicStampedReference`), so `A@0 ≠ A@2`. Alternatives: hazard pointers / RCU reclamation, or LL/SC hardware whose store-conditional fails on *any* intervening write.

</details>

## Sources & Verify

- **Herlihy & Shavit**, *The Art of Multiprocessor Programming* (2nd ed., 2020) — CAS, lock-free progress, the ABA problem, and memory consistency.
- **Treiber** (1986), "Systems programming: coping with parallelism" — the CAS-based lock-free stack; **Java** `java.util.concurrent.atomic` (`AtomicInteger`, `AtomicStampedReference`, `VarHandle`) and **C++** `std::atomic<T>::compare_exchange_*` are the production APIs.
- The `true/false/true` CAS results, the `atomic_inc → 3`, the naive-vs-versioned ABA `True`/`False`, and the `5/5/9` atomic-max above come from the runnable blocks — the Python ones *simulate* CAS (Python has no real atomic due to the GIL); the Java ones use genuine hardware-backed atomics. Re-run to verify.
