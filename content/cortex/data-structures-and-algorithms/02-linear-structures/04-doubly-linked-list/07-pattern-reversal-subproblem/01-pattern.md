---
title: "Pattern: Reversal as a Subproblem"
summary: "Reverse a doubly list in bounded chunks — pairwise swap, groups of k, alternating runs. Each chunk is the per-node prev/next swap; the new work is re-stitching both seams, and a doubly seam has twice the links — four boundary pointers per chunk."
prereqs:
  - 02-linear-structures/04-doubly-linked-list/06-pattern-reversal/01-pattern
---

# Pattern: Reversal as a Subproblem

## Why It Exists

You can reverse a whole doubly list with one swap per node. But the useful problems want it reversed **in pieces**: swap every adjacent pair, reverse every group of `k`, flip alternating runs. The chunk reversal itself is the same swap-`prev`/`next`-per-node move you already know.

What's genuinely new is the **stitching**. When you reverse a chunk in the middle of the list, its first node becomes its last, so you must reconnect it to its neighbours — and in a doubly list *every seam carries two links*. The node before the chunk needs its `next` updated and the chunk's new head needs its `prev` updated; likewise on the far side. That's **four boundary pointers per chunk**, versus two in the singly case. Miss one and the list reads correctly forward but is broken backward (or vice versa).

## See It Work

Reverse `1 ⇄ 2 ⇄ 3 ⇄ 4 ⇄ 5` in groups of `k = 2` (a pairwise swap) → `2 ⇄ 1 ⇄ 4 ⇄ 3 ⇄ 5`. Run it, then **Visualise** each pair flip and re-stitch.

> ▶ Run it, then click **Visualise** — each full group's nodes swap their two pointers, then the four seam links reconnect; the short tail is left alone.

```python run viz=linked-list viz-root=head viz-kind=list-double
class Node:
    def __init__(self, val):
        self.val = val
        self.prev = None
        self.next = None

def reverse_k_group(head, k):
    n, node = 0, head
    while node:                                       # count nodes → only full groups
        n += 1; node = node.next
    dummy = Node(0); dummy.next = head; head.prev = dummy
    before = dummy
    while n >= k:
        first = before.next                           # group's first node → becomes its tail
        cur, last = first, None
        for _ in range(k):
            cur.prev, cur.next = cur.next, cur.prev    # swap one node (doubly reversal)
            last = cur
            cur = cur.prev                             # advance via old next (now in prev)
        after, new_head = cur, last
        before.next = new_head; new_head.prev = before     # seam 1 — both links
        first.next = after
        if after is not None:
            after.prev = first                              # seam 2 — both links
        before = first
        n -= k
    head = dummy.next; head.prev = None                # detach the dummy
    return head

nodes = [Node(v) for v in (1, 2, 3, 4, 5)]
for i in range(4):
    nodes[i].next = nodes[i + 1]; nodes[i + 1].prev = nodes[i]
head = reverse_k_group(nodes[0], 2)

vals = []
node = head
while node:
    vals.append(node.val)
    node = node.next
print(vals)                                           # [2, 1, 4, 3, 5]
```

## How It Works

A dummy node in front gives every group a uniform predecessor (no head special case). Then, per group:

1. **Swap within the chunk.** Run the doubly per-node swap `k` times: each node trades `prev` and `next`, and you advance via the old `next` (now sitting in `prev`). After `k` swaps, the chunk's order is reversed internally.
2. **Re-stitch both seams.** `before` is the node before the chunk; `after` is the node after it. Reconnect all four pointers: `before.next = new_head`, `new_head.prev = before`, `first.next = after`, `after.prev = first` (where `first` is the old first node, now the chunk's tail).
3. **Advance** `before` to `first` (the chunk's new tail) and repeat while a full `k` remain.

```mermaid
flowchart TB
  C["count n; dummy → head; before = dummy"] --> Q{"n ≥ k?"}
  Q -->|"no — short tail"| D(["detach dummy; return head"])
  Q -->|"yes"| R["swap prev/next on k nodes"]
  R --> S["re-stitch 4 seam pointers:<br/>before↔new_head, old_first↔after"]
  S --> A["before = old_first; n −= k"]
  A --> Q
```

<p align="center"><strong>per full group: swap each node's two pointers, then reconnect the four seam links (before↔new-head and old-first↔after). The short remainder is untouched.</strong></p>

Each node is touched a constant number of times, so it's **`O(n)` time, `O(1)` space**. The recurring doubly hazard: it's easy to fix the `next` chain and forget the matching `prev`, leaving a list that walks fine forward but corrupt backward — always reconnect *both* directions at every seam.

### Key Takeaway

Reverse a doubly list in chunks by swapping each node's `prev`/`next` within the chunk, then re-stitching **four** seam pointers (both links on each side). A dummy removes the head special case; counting first leaves a short tail untouched — `O(n)` time, `O(1)` space.

## Trace It

`k = 2` over `1⇄2⇄3⇄4⇄5` (`n = 5`):

| `n` | group | after swap (internal) | four seams stitched | list so far |
|---|---|---|---|---|
| 5 | `1,2` | `2⇄1` | dummy↔2, 1↔3 | `2⇄1⇄3⇄4⇄5` |
| 3 | `3,4` | `4⇄3` | 1↔4, 3↔5 | `2⇄1⇄4⇄3⇄5` |
| 1 | — (`1<k`) | — | — | `2⇄1⇄4⇄3⇄5` |

Before you read on: the singly version re-stitched **two** pointers per chunk; here it's **four**. Which two are the extra ones, and what breaks if you skip them?

The extra two are the **backward** links: `new_head.prev = before` and `after.prev = first`. The singly list has no `prev`, so it never needed them. If you stitch only the `next` links (as you would in the singly case) and forget the `prev` links, a forward walk prints the right answer — `2⇄1⇄4⇄3⇄5` reads correctly via `next` — but walking backward from the tail follows stale `prev` pointers into the wrong nodes. The bug hides until something traverses the list in reverse. In a doubly list, *every* seam fix is two assignments.

## Your Turn

The reusable doubly reverse-in-groups-of-`k` (`k = 2` is a pairwise swap):

```python run viz=linked-list viz-root=head viz-kind=list-double
class Node:
    def __init__(self, val):
        self.val = val
        self.prev = None
        self.next = None

def reverse_k_group(head, k):
    n, node = 0, head
    while node:
        n += 1; node = node.next
    dummy = Node(0); dummy.next = head; head.prev = dummy
    before = dummy
    while n >= k:
        first = before.next
        cur, last = first, None
        for _ in range(k):
            cur.prev, cur.next = cur.next, cur.prev    # swap
            last = cur
            cur = cur.prev
        after, new_head = cur, last
        before.next = new_head; new_head.prev = before # seam 1
        first.next = after
        if after is not None:
            after.prev = first                         # seam 2
        before = first
        n -= k
    head = dummy.next; head.prev = None
    return head

nodes = [Node(v) for v in (1, 2, 3, 4, 5)]
for i in range(4):
    nodes[i].next = nodes[i + 1]; nodes[i + 1].prev = nodes[i]
out, node = [], reverse_k_group(nodes[0], 3)
while node:
    out.append(node.val); node = node.next
print(out)                                             # [3, 2, 1, 4, 5]
```

```java run viz=linked-list viz-root=head viz-kind=list-double
public class Main {
  static class Node { int val; Node prev, next; Node(int v){ val = v; } }

  static Node reverseKGroup(Node head, int k) {
    int n = 0;
    for (Node x = head; x != null; x = x.next) n++;
    Node dummy = new Node(0); dummy.next = head; head.prev = dummy;
    Node before = dummy;
    while (n >= k) {
      Node first = before.next, cur = first, last = null;
      for (int i = 0; i < k; i++) {
        Node t = cur.next; cur.next = cur.prev; cur.prev = t;   // swap
        last = cur; cur = cur.prev;
      }
      Node after = cur, newHead = last;
      before.next = newHead; newHead.prev = before;             // seam 1
      first.next = after;
      if (after != null) after.prev = first;                    // seam 2
      before = first; n -= k;
    }
    head = dummy.next; head.prev = null;
    return head;
  }

  public static void main(String[] args) {
    Node[] nd = new Node[5];
    for (int i = 0; i < 5; i++) nd[i] = new Node(i + 1);
    for (int i = 0; i < 4; i++) { nd[i].next = nd[i + 1]; nd[i + 1].prev = nd[i]; }
    StringBuilder sb = new StringBuilder("[");
    for (Node c = reverseKGroup(nd[0], 3); c != null; c = c.next) sb.append(c.val).append(c.next != null ? ", " : "");
    System.out.println(sb.append("]"));   // [3, 2, 1, 4, 5]
  }
}
```

Drill the family in **Practice** — [Pairwise Swap](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-subproblem-problems-pairwise-swap), [Reverse K Segments](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-subproblem-problems-reverse-k-segments), [Reverse Increasing Groups](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-subproblem-problems-reverse-increasing-groups), and [Reverse Alternate Segments](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-subproblem-problems-reverse-alternate-segments).

## Reflect & Connect

The chunk-reversal skeleton is the same across structures; the doubly twist is the doubled bookkeeping:

- **The family** — pairwise swap (`k = 2`), reverse-`k`-group (fixed `k`), increasing groups (`1, 2, 3, …`), alternate segments (reverse one run, skip the next). Only the group-size rule changes.
- **Four pointers, not two** — the transferable doubly lesson: every seam is two assignments. The cheapest reliable check is to walk the result *backward* from the tail and confirm it mirrors the forward walk — exactly the kind of bug a forward-only test misses.
- **The trade-off, again** — the second pointer made whole-list reversal simpler but makes chunk *stitching* heavier. That's the recurring doubly bargain: more links to maintain, more capability per node.

**Prerequisites:** [Reversal](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-pattern).
**What's next:** use the two outward pointers to converge from both ends — [Two Pointers](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-two-pointers-pattern).

## Recall

> **Mnemonic:** *Dummy in front; per group swap `prev`/`next` on `k` nodes, then stitch FOUR seams (both links each side). Count first; short tail untouched.*

| | |
|---|---|
| Chunk reversal | swap `prev`/`next` per node, advance via old `next` (in `prev`) |
| Seams | four pointers: `before↔new_head`, `old_first↔after` |
| Dummy | uniform predecessor → no head special case (detach at the end) |
| Doubly hazard | fixing only `next` leaves `prev` stale → broken backward walk |
| Cost | `O(n)` time, `O(1)` space |

<details>
<summary><strong>Q:</strong> How does doubly chunk-reversal differ from the singly version?</summary>

**A:** Each seam needs both a `next` and a `prev` reconnected — four boundary pointers per chunk instead of two.

</details>
<details>
<summary><strong>Q:</strong> What's the classic bug, and how do you catch it?</summary>

**A:** Fixing `next` but not `prev`; catch it by walking backward from the tail and checking it mirrors the forward walk.

</details>
<details>
<summary><strong>Q:</strong> Why a dummy node?</summary>

**A:** It gives the first group a real predecessor, so reversing the head is not a special case.

</details>
<details>
<summary><strong>Q:</strong> Why count `n` and gate on `n ≥ k`?</summary>

**A:** So only full groups reverse and a short remainder is left untouched.

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, 4th ed., §10.2 — doubly linked lists; sentinel nodes and pointer manipulation.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §1.3 — linked structures and in-place restructuring.
- Reverse-in-`k`-groups on a doubly list is a standard exercise; both runnable blocks are verified by running (`k=2 ⇒ [2,1,4,3,5]`, `k=3 ⇒ [3,2,1,4,5]`), with backward `prev` links checked consistent.
