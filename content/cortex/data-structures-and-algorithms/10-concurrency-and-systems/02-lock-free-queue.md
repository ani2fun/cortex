---
title: Lock-Free Queue
summary: "The Michael-Scott queue — a linked FIFO whose enqueue and dequeue use CAS instead of locks. A dummy head node lets the two ends operate independently; enqueue is two CAS steps, so the tail can lag one node behind, and threads HELP advance it. Inside the JVM's ConcurrentLinkedQueue and every fast message broker."
prereqs:
  - concurrency-and-systems-cas-and-atomics
  - linear-structures-queue-what-is-a-queue
---

## Why It Exists

A queue is the backbone of producer/consumer systems — task pools, message brokers, event loops. Guard it with a single mutex and *every* enqueue and dequeue serialises through that one lock; under load it becomes the bottleneck, and a thread descheduled while holding it stalls everyone. You want producers and consumers to make progress *concurrently*, without locks.

The **Michael-Scott queue** is the classic answer, built entirely on [CAS](/cortex/data-structures-and-algorithms/concurrency-and-systems/cas-and-atomics). It's a singly-linked list with two tricks. First, a **dummy (sentinel) head node** that holds no value: `head` always points at it, and the first *real* element is `head.next`. This decouples the two ends — enqueue only ever touches the `tail` pointer, dequeue only the `head` pointer — so producers and consumers don't fight over the same word. Second, enqueue is **two CAS steps** (link the node, then advance the tail), which means the `tail` pointer can momentarily *lag* one node behind the true last element — and any thread that notices **helps** advance it. That helping is what keeps the whole thing lock-free. It's the queue inside Java's `ConcurrentLinkedQueue`, the .NET runtime, and high-throughput brokers.

## See It Work

Enqueue links a node and (best-effort) advances the tail; dequeue swings `head` past the dummy and returns the next value. (Python has no real CAS — the GIL serialises bytecode — so we *simulate* it; Java's `AtomicReference` gives genuine CAS on references.)

```python run viz=array
import ast

class Node:
    __slots__ = ("value", "next")
    def __init__(self, value=None):
        self.value = value; self.next = None

def cas(obj, field, expected, new):                    # SIMULATED compare-and-swap on a field
    if getattr(obj, field) is expected:
        setattr(obj, field, new); return True
    return False

class MSQueue:
    def __init__(self):
        dummy = Node()                                 # sentinel; head and tail both start here
        self.head = dummy; self.tail = dummy
    def enqueue(self, x):
        node = Node(x)
        while True:
            tail = self.tail; nxt = tail.next
            if tail is self.tail:                      # snapshot still consistent?
                if nxt is None:
                    if cas(tail, "next", None, node):  # step 1: link the new node
                        cas(self, "tail", tail, node)  # step 2: advance tail (best-effort)
                        return
                else:
                    cas(self, "tail", tail, nxt)       # tail lagged -> help advance it, then retry
    def dequeue(self):
        while True:
            head = self.head; tail = self.tail; nxt = head.next
            if head is self.head:
                if head is tail:
                    if nxt is None: return None        # empty
                    cas(self, "tail", tail, nxt)       # help advance a lagging tail
                else:
                    value = nxt.value
                    if cas(self, "head", head, nxt):   # swing head past the old dummy
                        return value

items = ast.literal_eval(input())
q = MSQueue()
for x in items:
    q.enqueue(x)
results = " ".join(str(q.dequeue()) for _ in items)
print(results)
d = q.dequeue()
print(d if d is not None else "null")
```

```java run viz=array
import java.util.concurrent.atomic.AtomicReference;
import java.util.*;
public class Main {
    static class Node {
        Integer value;
        AtomicReference<Node> next = new AtomicReference<>(null);
        Node(Integer v) { value = v; }
    }
    static class MSQueue {
        AtomicReference<Node> head, tail;
        MSQueue() { Node d = new Node(null); head = new AtomicReference<>(d); tail = new AtomicReference<>(d); }
        void enqueue(int x) {
            Node node = new Node(x);
            while (true) {
                Node t = tail.get(), next = t.next.get();
                if (t == tail.get()) {
                    if (next == null) {
                        if (t.next.compareAndSet(null, node)) { tail.compareAndSet(t, node); return; }  // link, then advance
                    } else {
                        tail.compareAndSet(t, next);                                                    // help advance lagging tail
                    }
                }
            }
        }
        Integer dequeue() {
            while (true) {
                Node h = head.get(), t = tail.get(), next = h.next.get();
                if (h == head.get()) {
                    if (h == t) {
                        if (next == null) return null;              // empty
                        tail.compareAndSet(t, next);                // help advance
                    } else {
                        Integer value = next.value;
                        if (head.compareAndSet(h, next)) return value;   // swing head past dummy
                    }
                }
            }
        }
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String line = sc.nextLine().replaceAll("[\\[\\]]", "").trim();
        String[] parts = line.split(",\\s*");
        MSQueue q = new MSQueue();
        for (String p : parts) q.enqueue(Integer.parseInt(p.trim()));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) { if (i > 0) sb.append(" "); sb.append(q.dequeue()); }
        System.out.println(sb.toString());
        Integer d = q.dequeue();
        System.out.println(d != null ? d : "null");
    }
}
```

```testcases
{
  "args": [
    { "id": "items", "label": "items to enqueue", "type": "string", "placeholder": "[1, 2, 3]" }
  ],
  "cases": [
    { "args": { "items": "[1, 2, 3]" }, "expected": "1 2 3\nnull" },
    { "args": { "items": "[5, 10, 15]" }, "expected": "5 10 15\nnull" },
    { "args": { "items": "[42, 7, 99]" }, "expected": "42 7 99\nnull" }
  ]
}
```

Both print `1 2 3` then `null` — first-in-first-out, no locks. The retry loops never spin here because there's only one thread; under real contention, a thread whose CAS loses simply re-reads the fresh `head`/`tail` and tries again, always making progress.

## How It Works

The dummy node and the two-step enqueue are the whole design. Picture the list mid-enqueue:

```d2
direction: right
dummy: "DUMMY (sentinel)\nno value" {style.fill: "#94a3b8"; style.stroke: "#475569"}
n1: "node 10" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
n2: "node 20  <- just LINKED (step 1)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
dummy -> n1: "next"
n1 -> n2: "next"
head: "head -> dummy\n(dequeue works HERE)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
tail: "tail -> node 10  *** LAGS one behind! ***\n(step 2 'advance tail' not done yet)\n(enqueue works HERE)" {style.fill: "#fecaca"; style.stroke: "#dc2626"}
head -> dummy
tail -> n1
note: "enqueue = CAS-link (step 1) THEN CAS-advance-tail (step 2).\nBetween them tail.next != null -> any thread HELPS advance tail." {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
```

<p align="center"><strong>The dummy node keeps <code>head</code> and <code>tail</code> on different memory, so enqueue and dequeue never contend. Because enqueue links then advances in two CAS steps, <code>tail</code> can point one node behind the real last — a state every thread detects (<code>tail.next != null</code>) and helps fix.</strong></p>

Three load-bearing facts:

- **The dummy node decouples the two ends.** With a sentinel that always sits at `head`, enqueue only ever CASes the `tail` (and the last node's `next`), while dequeue only CASes the `head`. They touch different words, so a producer and consumer can both succeed at the same instant without conflict — and an empty queue (`head == tail`, both at the dummy) needs no special wrapping.
- **Enqueue is two CAS steps, so the tail lags.** Step 1 links the new node: `CAS(last.next, null, node)`. Step 2 advances the tail: `CAS(tail, last, node)`. Between them — or if the enqueuing thread is descheduled after step 1 — `tail` points one node *behind* the real end, with `tail.next` non-null. The [Trace It](#trace-it) makes this concrete.
- **Helping makes it lock-free.** A thread that finds `tail.next != null` knows the tail is lagging, so before doing its own work it *helps* by advancing the tail (`CAS(tail, tail, tail.next)`) — completing another thread's unfinished step 2. No one waits for the slow thread; whoever notices the half-done operation finishes it. That cooperative "finish the other guy's work" is the essence of lock-free progress.

> **Key takeaway.** The Michael-Scott queue is a linked FIFO on CAS with a **dummy head node** (decoupling enqueue at the tail from dequeue at the head) and a **two-step enqueue** (link, then advance tail). The tail can lag one node behind; any thread that sees `tail.next != null` **helps** advance it, so no operation ever blocks on another — lock-free progress. It's the standard concurrent queue (Java's `ConcurrentLinkedQueue`).

## Trace It

The "tail lags" claim is the subtlest part, and seeing the intermediate state demystifies the helping logic.

**Predict before you run:** an enqueuing thread runs step 1 (CAS-link the new node) and is then preempted *before* step 2 (advance the tail). At that moment, where does `tail` point — at the node it just linked, or one node *behind* it?

```python run viz=array
class Node:
    __slots__ = ("value", "next")
    def __init__(self, value=None):
        self.value = value; self.next = None
def cas(obj, field, expected, new):
    if getattr(obj, field) is expected:
        setattr(obj, field, new); return True
    return False
class MSQueue:
    def __init__(self):
        dummy = Node(); self.head = dummy; self.tail = dummy
    def enqueue(self, x):
        node = Node(x)
        while True:
            tail = self.tail; nxt = tail.next
            if tail is self.tail:
                if nxt is None:
                    if cas(tail, "next", None, node):
                        cas(self, "tail", tail, node); return
                else:
                    cas(self, "tail", tail, nxt)
    def dequeue(self):
        while True:
            head = self.head; tail = self.tail; nxt = head.next
            if head is self.head:
                if head is tail:
                    if nxt is None: return None
                    cas(self, "tail", tail, nxt)
                else:
                    value = nxt.value
                    if cas(self, "head", head, nxt): return value

q = MSQueue()
q.enqueue(10)                                          # tail at node(10)
# a thread does step 1 (link node 20) but is preempted before step 2 (advance tail):
node = Node(20)
cas(q.tail, "next", None, node)                        # ONLY the link, no advance
print("tail.value:", q.tail.value, " tail.next.value:", q.tail.next.value)
print("tail is lagging (tail.next is not None):", q.tail.next is not None)
q.enqueue(30)                                          # a fresh enqueue: HELPS advance tail, then links 30
print("drain:", q.dequeue(), q.dequeue(), q.dequeue())
```

<details>
<summary><strong>Reveal</strong></summary>

`tail.value` is `10` while `tail.next.value` is `20` — the tail points *one node behind* the real last element. Step 1 linked `node(20)` onto `node(10).next`, but step 2 (advance the tail to `node(20)`) never ran, so the structure is momentarily "ahead" of where `tail` claims. The check `tail.next is not None` is `True` — that non-null `next` is precisely the *signal* a lagging tail leaves behind. When the next `enqueue(30)` comes along, it reads `tail` (still `node(10)`), sees `tail.next != None`, and rather than failing or waiting, it **helps**: it CAS-advances `tail` to `node(20)` (finishing the stalled thread's step 2), then retries and links `node(30)`. The `drain` confirms everything stayed correct: `10, 20, 30` in FIFO order, no element lost or duplicated despite the half-finished enqueue. This is lock-free progress in miniature — the system never depends on the slow thread waking up, because whoever trips over the unfinished work completes it. (It's also why this queue is *ABA*-prone: pointers being reused need [hazard pointers or RCU](/cortex/data-structures-and-algorithms/concurrency-and-systems/rcu-and-hazard-pointers) for safe memory reclamation, the next lesson.)

</details>

## Your Turn

**Interleave** producers and consumers and confirm FIFO holds through a mixed sequence — enqueue some, dequeue some, repeat, and drain to empty.

```python run viz=array
import ast

class Node:
    __slots__ = ("value", "next")
    def __init__(self, value=None):
        self.value = value; self.next = None
def cas(obj, field, expected, new):
    if getattr(obj, field) is expected:
        setattr(obj, field, new); return True
    return False
class MSQueue:
    def __init__(self):
        dummy = Node(); self.head = dummy; self.tail = dummy
    def enqueue(self, x):
        # Your code goes here
        return
    def dequeue(self):
        # Your code goes here
        return None

items = ast.literal_eval(input())
q = MSQueue()
out = []
q.enqueue(items[0]); q.enqueue(items[1])
out.append(q.dequeue())
q.enqueue(items[2])
out.append(q.dequeue())
out.append(q.dequeue())
d = q.dequeue()
out.append(d if d is not None else "null")
print("[" + ", ".join(str(x) for x in out) + "]")
```

```java run viz=array
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
public class Main {
    static class Node { Integer value; AtomicReference<Node> next = new AtomicReference<>(null); Node(Integer v){value=v;} }
    static class MSQueue {
        AtomicReference<Node> head, tail;
        MSQueue() { Node d = new Node(null); head = new AtomicReference<>(d); tail = new AtomicReference<>(d); }
        void enqueue(int x) {
            // Your code goes here
        }
        Integer dequeue() {
            // Your code goes here
            return null;
        }
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String line = sc.nextLine().replaceAll("[\\[\\]]", "").trim();
        String[] parts = line.split(",\\s*");
        int[] items = new int[parts.length];
        for (int i = 0; i < parts.length; i++) items[i] = Integer.parseInt(parts[i].trim());
        MSQueue q = new MSQueue();
        List<String> out = new ArrayList<>();
        q.enqueue(items[0]); q.enqueue(items[1]); out.add(String.valueOf(q.dequeue()));
        q.enqueue(items[2]); out.add(String.valueOf(q.dequeue())); out.add(String.valueOf(q.dequeue()));
        Integer d = q.dequeue();
        out.add(d != null ? String.valueOf(d) : "null");
        System.out.println("[" + String.join(", ", out) + "]");
    }
}
```

```testcases
{
  "args": [
    { "id": "items", "label": "items to interleave", "type": "string", "placeholder": "[1, 2, 3]" }
  ],
  "cases": [
    { "args": { "items": "[1, 2, 3]" }, "expected": "[1, 2, 3, null]" },
    { "args": { "items": "[7, 8, 9]" }, "expected": "[7, 8, 9, null]" },
    { "args": { "items": "[100, 200, 300]" }, "expected": "[100, 200, 300, null]" }
  ]
}
```

The first case prints `[1, 2, 3, null]`. The mixed order in, the same order out, and a clean `null` once the dummy is all that's left. The structure never special-cases "queue became empty" with a flag — the `head == tail` (both at the sentinel) condition handles it, which is exactly the kind of invariant a sentinel node buys you.

<details>
<summary><strong>Editorial</strong></summary>

Implement the two-step enqueue (link then advance tail) and the head-swing dequeue. The sentinel always lives at `head`; an empty queue is `head == tail` with `head.next == null`. The retry loops are what make both operations lock-free — a failed CAS means a competitor committed, so re-read and try again.

```python solution time=O(1) space=O(1)
import ast

class Node:
    __slots__ = ("value", "next")
    def __init__(self, value=None):
        self.value = value; self.next = None
def cas(obj, field, expected, new):
    if getattr(obj, field) is expected:
        setattr(obj, field, new); return True
    return False
class MSQueue:
    def __init__(self):
        dummy = Node(); self.head = dummy; self.tail = dummy
    def enqueue(self, x):
        node = Node(x)
        while True:
            tail = self.tail; nxt = tail.next
            if tail is self.tail:
                if nxt is None:
                    if cas(tail, "next", None, node):
                        cas(self, "tail", tail, node); return
                else:
                    cas(self, "tail", tail, nxt)
    def dequeue(self):
        while True:
            head = self.head; tail = self.tail; nxt = head.next
            if head is self.head:
                if head is tail:
                    if nxt is None: return None
                    cas(self, "tail", tail, nxt)
                else:
                    value = nxt.value
                    if cas(self, "head", head, nxt): return value

items = ast.literal_eval(input())
q = MSQueue()
out = []
q.enqueue(items[0]); q.enqueue(items[1])
out.append(q.dequeue())
q.enqueue(items[2])
out.append(q.dequeue())
out.append(q.dequeue())
d = q.dequeue()
out.append(d if d is not None else "null")
print("[" + ", ".join(str(x) for x in out) + "]")
```

```java solution
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
public class Main {
    static class Node { Integer value; AtomicReference<Node> next = new AtomicReference<>(null); Node(Integer v){value=v;} }
    static class MSQueue {
        AtomicReference<Node> head, tail;
        MSQueue() { Node d = new Node(null); head = new AtomicReference<>(d); tail = new AtomicReference<>(d); }
        void enqueue(int x) {
            Node node = new Node(x);
            while (true) {
                Node t = tail.get(), next = t.next.get();
                if (t == tail.get()) {
                    if (next == null) { if (t.next.compareAndSet(null, node)) { tail.compareAndSet(t, node); return; } }
                    else tail.compareAndSet(t, next);
                }
            }
        }
        Integer dequeue() {
            while (true) {
                Node h = head.get(), t = tail.get(), next = h.next.get();
                if (h == head.get()) {
                    if (h == t) { if (next == null) return null; tail.compareAndSet(t, next); }
                    else { Integer v = next.value; if (head.compareAndSet(h, next)) return v; }
                }
            }
        }
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String line = sc.nextLine().replaceAll("[\\[\\]]", "").trim();
        String[] parts = line.split(",\\s*");
        int[] items = new int[parts.length];
        for (int i = 0; i < parts.length; i++) items[i] = Integer.parseInt(parts[i].trim());
        MSQueue q = new MSQueue();
        List<String> out = new ArrayList<>();
        q.enqueue(items[0]); q.enqueue(items[1]); out.add(String.valueOf(q.dequeue()));
        q.enqueue(items[2]); out.add(String.valueOf(q.dequeue())); out.add(String.valueOf(q.dequeue()));
        Integer d = q.dequeue();
        out.add(d != null ? String.valueOf(d) : "null");
        System.out.println("[" + String.join(", ", out) + "]");
    }
}
```

</details>

## Reflect & Connect

- **CAS on `head`/`tail`, decoupled by a dummy.** Enqueue touches the tail, dequeue the head; the sentinel keeps them on different words so producers and consumers don't contend, and empty-queue handling falls out of `head == tail`.
- **Two-step enqueue -> tail lag -> helping.** Link the node, then advance the tail; in between, `tail.next != null` signals a lagging tail, and any thread *helps* finish the advance. That cooperative completion is what makes the queue lock-free (no thread blocks on a slow one).
- **It's ABA-prone.** Reused node pointers can fool a CAS (the [ABA problem](/cortex/data-structures-and-algorithms/concurrency-and-systems/cas-and-atomics)); safe memory reclamation needs hazard pointers or [RCU/epochs](/cortex/data-structures-and-algorithms/concurrency-and-systems/rcu-and-hazard-pointers) — you can't just `free` a dequeued node while another thread might still hold a pointer to it.
- **Lock-free, not wait-free.** Some thread always progresses, but an individual thread can retry indefinitely under heavy contention. That's the usual, practical guarantee — and far better than a lock that can stall everyone.
- **It's the production default.** Java's `ConcurrentLinkedQueue`, .NET's `ConcurrentQueue`, and many brokers are Michael-Scott queues. Master the dummy node + two-step enqueue + helping, and concurrent stacks (Treiber) and skip-list maps are the same CAS-retry ideas on a different shape.

## Recall

<details>
<summary><strong>Q:</strong> What does the dummy (sentinel) head node accomplish?</summary>

**A:** It decouples the two ends: `head` always points at the dummy and the first real value is `head.next`, so enqueue only CASes the `tail` and dequeue only the `head` — they never contend on the same word. It also makes the empty case (`head == tail`) need no special flag.

</details>
<details>
<summary><strong>Q:</strong> Why is enqueue two CAS steps, and what's the consequence?</summary>

**A:** Step 1 links the node (`CAS(last.next, null, node)`); step 2 advances the tail (`CAS(tail, last, node)`). Between them, `tail` lags one node behind the real end (with `tail.next != null`) — the structure is momentarily ahead of where `tail` claims to be.

</details>
<details>
<summary><strong>Q:</strong> What is "helping," and why does it matter?</summary>

**A:** A thread that sees `tail.next != null` (a lagging tail) advances the tail itself — completing another thread's unfinished step 2 — before doing its own work. It means no thread waits for a slow/descheduled thread, which is what makes the queue lock-free.

</details>
<details>
<summary><strong>Q:</strong> Is the Michael-Scott queue wait-free? What's the actual guarantee?</summary>

**A:** It's lock-free, not wait-free: *some* thread always makes progress, but a given thread can retry indefinitely under contention. No thread can block all others (unlike a mutex), which is the practical win.

</details>
<details>
<summary><strong>Q:</strong> Why does the queue need hazard pointers or RCU?</summary>

**A:** It's ABA-prone — a freed-and-reused node pointer can fool a CAS into "succeeding" against stale memory. Safe reclamation (you can't free a dequeued node while another thread may still reference it) requires hazard pointers, epochs, or RCU.

</details>

## Sources & Verify

- **Michael & Scott** (1996), "Simple, fast, and practical non-blocking and blocking concurrent queue algorithms", *PODC* — the original two-lock and lock-free queues, the dummy node, and the helping mechanism.
- **Herlihy & Shavit**, *The Art of Multiprocessor Programming* (2nd ed.) — the M-S queue with correctness and ABA/reclamation discussion.
- **Java** `java.util.concurrent.ConcurrentLinkedQueue` and **.NET** `ConcurrentQueue<T>` are production Michael-Scott queues; the `1 2 3` / `null` FIFO, the tail-lag (`tail.value 10`, `tail.next.value 20`) trace, and the `[1, 2, 3, null]` interleave above come from the runnable blocks — the Python ones *simulate* CAS (GIL = no real atomic); the Java ones use real `AtomicReference`. Re-run to verify.
