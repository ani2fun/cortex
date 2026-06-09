---
title: "Pattern: Merge"
summary: "Weave two sorted lists into one by repeatedly splicing the smaller current head onto a dummy-headed output, then attaching whatever's left. The sorted inputs make the choice trivial — O(m+n) time, O(1) extra space, no copying."
prereqs:
  - 02-linear-structures/03-singly-linked-list/01-what-is-a-linked-list
---

# Pattern: Merge

## Why It Exists

You have two **sorted** lists and want one sorted list containing all their nodes. This is the combine step of merge sort, and the shape of "merge two sorted streams" everywhere.

The naive route ignores the gift you were given: dump every value into an array, sort it (`O((m+n) log(m+n))`), and rebuild a list — extra space *and* a sort you didn't need. The realization: the inputs are **already sorted**, so the next node of the answer is always whichever of the two current heads is smaller. There's no searching and no sorting — just *look at two fronts, take the smaller, advance*. Splice nodes instead of copying values and it's `O(m+n)` time, `O(1)` extra space.

## See It Work

Merge `1→3→5` with `2→4→6`. At each step, take the smaller of the two heads. Run it.

```python run viz=linked-list viz-root=head viz-kind=list-single
class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

def to_list(node):
    out = []
    while node:
        out.append(node.val); node = node.next
    return out

a = ListNode(1, ListNode(3, ListNode(5)))   # 1 → 3 → 5
b = ListNode(2, ListNode(4, ListNode(6)))   # 2 → 4 → 6

dummy = ListNode(0)                          # a stand-in head — no special case for the first pick
tail = dummy
while a and b:
    if a.val <= b.val:                       # take the smaller front; <= keeps it stable
        tail.next = a; a = a.next
    else:
        tail.next = b; b = b.next
    tail = tail.next                         # extend the output
tail.next = a if a else b                    # one list is empty; attach the other's leftover wholesale

print(to_list(dummy.next))                   # [1, 2, 3, 4, 5, 6]
```

## How It Works

Two ideas carry it:

1. **A dummy head + a `tail` pointer.** The dummy is a throwaway node the output grows from, so the *first* splice is no different from the rest — no "is this the first node?" special case. `tail` always points at the last node of the output-so-far.
2. **The selector loop.** While *both* inputs have nodes, compare their heads, splice the smaller onto `tail`, and advance that input. When one input runs dry, the other is already sorted and ≥ everything placed — so attach its entire remaining tail in one move.

```mermaid
flowchart TB
  I["dummy → output; tail = dummy"] --> Q{"both a and b non-empty?"}
  Q -->|"yes"| C["tail.next = min(a, b); advance that list; tail = tail.next"]
  C --> Q
  Q -->|"no"| R["tail.next = whichever list still has nodes"]
  R --> D(["return dummy.next"])
```

<p align="center"><strong>compare the two heads, splice the smaller onto the output's tail, advance that list; when one empties, attach the rest of the other.</strong></p>

Each node is spliced exactly once, so it's **`O(m + n)` time, `O(1)` extra space** (the result reuses the input nodes). Using `<=` rather than `<` makes the merge **stable** — when values tie, the node from `a` goes first, preserving original order, which matters when you merge records keyed on one field.

### Key Takeaway

Merge two sorted lists by repeatedly splicing the smaller of the two heads onto a dummy-headed output, then attaching the leftover when one side empties. Sorted inputs make the choice trivial → `O(m+n)` time, `O(1)` space; `<=` keeps it stable.

## Trace It

Merging `a = 1→3→5` and `b = 2→4→6`:

| step | `a` head | `b` head | take | output so far |
|---|---|---|---|---|
| 1 | `1` | `2` | `1` (a) | `1` |
| 2 | `3` | `2` | `2` (b) | `1→2` |
| 3 | `3` | `4` | `3` (a) | `1→2→3` |
| 4 | `5` | `4` | `4` (b) | `1→2→3→4` |
| 5 | `5` | `6` | `5` (a) | `1→2→3→4→5` |
| — | `∅` | `6` | attach `b` | `1→2→3→4→5→6` |

Before you read on: at step 5, `a` empties (its `5` was just taken) while `b` still holds `6`. Why can the loop *stop comparing* and just attach the rest of `b` instead of continuing node by node?

Because `b` is sorted and every node still in it is `≥` everything already placed — `6` is larger than the `5` we just appended, and anything after it (none here) would be larger still. There's nothing left to compare *against*, so node-by-node splicing would do the same work as one pointer assignment. Attaching the leftover tail wholesale is correct precisely because each input was sorted to begin with — the same property that made the selector trivial.

## Your Turn

The reusable merge of two sorted lists:

```python run viz=linked-list viz-root=head viz-kind=list-single
class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

def merge_two(a, b):
    dummy = ListNode(0)
    tail = dummy
    while a and b:
        if a.val <= b.val:
            tail.next = a; a = a.next
        else:
            tail.next = b; b = b.next
        tail = tail.next
    tail.next = a if a else b        # attach the remaining tail
    return dummy.next

def to_list(node):
    out = []
    while node:
        out.append(node.val); node = node.next
    return out

a = ListNode(1, ListNode(2, ListNode(7, ListNode(8))))
b = ListNode(3, ListNode(4))
print(to_list(merge_two(a, b)))      # [1, 2, 3, 4, 7, 8]
```

```java run viz=linked-list viz-root=head viz-kind=list-single
public class Main {
  static class ListNode { int val; ListNode next; ListNode(int v){ val = v; } ListNode(int v, ListNode n){ val = v; next = n; } }

  static ListNode mergeTwo(ListNode a, ListNode b) {
    ListNode dummy = new ListNode(0), tail = dummy;
    while (a != null && b != null) {
      if (a.val <= b.val) { tail.next = a; a = a.next; }
      else                { tail.next = b; b = b.next; }
      tail = tail.next;
    }
    tail.next = (a != null) ? a : b;   // attach the remaining tail
    return dummy.next;
  }

  public static void main(String[] args) {
    ListNode a = new ListNode(1, new ListNode(2, new ListNode(7, new ListNode(8))));
    ListNode b = new ListNode(3, new ListNode(4));
    StringBuilder sb = new StringBuilder("[");
    for (ListNode c = mergeTwo(a, b); c != null; c = c.next) sb.append(c.val).append(c.next != null ? ", " : "");
    System.out.println(sb.append("]"));   // [1, 2, 3, 4, 7, 8]
  }
}
```

Drill the family in **Practice** — [Alternate Node Fusion](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-merge/problems/alternate-node-fusion), [Merge Sorted Lists](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-merge/problems/merge-sorted-lists), [Merge Sorted Lists II](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-merge/problems/merge-sorted-lists-ii), and [List Addition](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-merge/problems/list-addition).

## Reflect & Connect

The dummy-head + tail-splice skeleton is the reusable core; the variants only change the *selector*:

- **Sorted merge** (smaller head wins), **alternate fusion** (interleave regardless of value — selector just alternates), **add two numbers** (walk both, sum digits with a carry — a merge with arithmetic instead of comparison).
- **Merge K sorted lists** scales the idea — either fold pairwise (`O(N log k)`) or pull the global minimum from a [heap](/cortex/data-structures-and-algorithms/trees/heap/what-is-a-heap) of the `k` heads. Same "take the smallest front" instinct, more fronts.
- **It's the inverse of split, and the heart of merge sort** — split a list at the middle (fast/slow), sort each half, then *merge* them back. The whole-list version of "combine sorted pieces" is exactly this pattern called recursively.

**Prerequisites:** [What Is a Linked List?](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/what-is-a-linked-list).
**What's next:** combine split, reverse, and merge into one restructuring — [Reorder](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-reorder/pattern).

## Recall

> **Mnemonic:** *Dummy + tail. While both: splice the smaller head, advance it. One empties ⇒ attach the other's leftover. `<=` is stable.*

| | |
|---|---|
| Dummy head | output grows from it → first splice is not a special case |
| Selector | `a.val <= b.val ? take a : take b`, then `tail = tail.next` |
| Leftover | when one list empties, attach the other's remaining tail wholesale |
| Cost | `O(m + n)` time, `O(1)` extra space (nodes reused) |

<details>
<summary><strong>Q:</strong> Why is merging sorted lists `O(m+n)` and not `O((m+n) log(m+n))`?</summary>

**A:** Sorted inputs make the next node always the smaller of two heads — no sort or search, just one linear pass.

</details>
<details>
<summary><strong>Q:</strong> What does the dummy head buy you?</summary>

**A:** The first node is spliced like any other — no "is the output empty yet?" special case.

</details>
<details>
<summary><strong>Q:</strong> Why attach the leftover tail in one step instead of looping?</summary>

**A:** The remaining list is sorted and ≥ everything already placed, so there's nothing left to compare — a single pointer assignment suffices.

</details>
<details>
<summary><strong>Q:</strong> Why `<=` rather than `<`?</summary>

**A:** On ties it takes the node from `a` first, keeping the merge stable (original order preserved).

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, 4th ed., §2.3.1 — the `MERGE` procedure (the combine step of merge sort) and its `O(m+n)` bound.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §2.2 — merging and merge sort; stability.
- "Merge two sorted lists" with a dummy head and splice selector is the standard result; both runnable blocks are verified by running (`[1,2,3,4,5,6]` and `[1,2,3,4,7,8]`).
