---
title: "Memorize: Reversal (Subproblem)"
summary: "Pattern recognition triggers, template code, common mistakes, and quick recall."
prereqs:
  - 08-pattern-reversal-subproblem/01-pattern
---

# Memorize: Reversal (Subproblem)

## In a Hurry?

- **One-Line Idea**: Decompose the rewrite into chunk-sized segment reversals, drive the chunk boundary with an outer counter (fixed `k`, growing `1, 2, 3, …`, or a toggling flag), and stitch the reversed pieces back together with cached `left_bound`/`start`/`end` pointers.
- **Complexities**: `O(n)` time, `O(1)` space, where `n` is the length of the linked list. Each node is touched a constant number of times across the outer boundary walk and the inner reversal call.
- **When to Use**: The problem asks for partial-list flips — swap every two adjacent nodes, reverse every chunk of `k`, reverse increasing-size runs, or reverse alternate segments — and the rewrite must operate by editing `next` pointers in place with `O(1)` auxiliary memory.

---

## One-Line Mnemonic

**"Slice, flip, stitch, slide."**

The image is a four-step dance per chunk: slice the next chunk by finding its `end`, flip it with the segment-reversal primitive, stitch the reversed head onto the previous chunk's tail, then slide the boundary pointers forward to the next chunk.

---

## Real-World Analogy

Picture a row of train cars on a single track and a crew that has been told to flip the order of cars inside specific carriages — every pair, every block of three, every alternate block. They cannot lift cars off the track; they can only rewire the couplings. The crew walks from the front, marks the start and end of the current group, uncouples the trailing connection so the rest of the train does not get dragged along, reverses the couplings inside the group, then re-couples the reversed group's new front car to the back of the previous group. They never go back; once a group is done they slide forward and repeat. The chapter's `reverse(start, end)` helper is the rewiring routine; the outer loop is the crew walking the train; the boundary pointers are the chalk marks they keep on the rails.

---

## Visual Summary

```mermaid
flowchart LR
  prim["full-list reversal primitive"] --> piece["apply to a sub-range:<br/>swap pairs · reverse first k · reverse [i..j]"]
  piece --> stitch["re-link the reversed piece<br/>to the untouched prefix and suffix"]
  style stitch fill:#bbf7d0,stroke:#16a34a
```

<p align="center"><strong>Reuse the three-pointer reversal on a slice, not the whole list — reverse the first k, swap adjacent pairs, flip a middle segment — then stitch the reversed piece back to its neighbours.</strong></p>

---

## Pattern Recognition Triggers

The pattern fits when **all four** answers are "yes" — the same diagnostic that gates each problem in the section.

- The rewrite can be **decomposed** into a sequence of contiguous-chunk reversals — fixed-size chunks (pairwise swap, reverse-k-segments), growing chunks (reverse-increasing-groups), or conditionally-reversed chunks (reverse-alternate-segments).
- At least one subproblem is a **segment reversal** that the standard `reverse(start, end)` helper from chapter pattern 07 can solve in `O(k)`.
- The algorithm only needs to **walk each node a constant number of times** — no random access from `head`, no per-chunk re-traversal.
- Each chunk's boundary is **computable from local state** — the chunk size (`k`, growing counter, toggling flag) plus the cached `left_bound`/`start` pointers from the previous iteration.

Common surface signals: "swap every two adjacent nodes," "reverse the list in groups of `k`," "reverse alternate segments of size `k`," "reverse the first half / second half / a sublist `[i..j]`," "reverse `1, 2, 3, …` nodes in increasing chunks."

---

## Don't Confuse With

| | **Reversal Subproblem (this pattern)** | **Full-List Reversal (chapter pattern 07)** |
|---|---|---|
| **Problem shape** | "Reverse every chunk of `k`," "swap adjacent pairs," "reverse alternate segments" | "Reverse the entire linked list" |
| **Outer structure** | Outer loop iterates chunks; per-chunk `start`/`end`/`left_bound` cached | No outer loop — a single sweep of three pointers (`prev`, `curr`, `next`) over the whole list |
| **Inner reversal** | Invoked once per chunk via `reverse(start, end)` helper | Inlined into the single sweep — no helper needed |
| **Seam stitching** | Required — `left_bound.next = reversed_head` after every chunk; first chunk also updates the global `head` | None — the whole list is one segment; the only seam is the new `head` |
| **Complexity** | `O(n)` time, `O(1)` space, same as full-list reversal but with a higher constant factor from the extra boundary walks | `O(n)` time, `O(1)` space, with the lowest constant factor — one pointer-flip per node |
| **When this goes wrong** | The output is partly correct but adjacent chunks lose their connection — seams missing, intermediate nodes orphaned, or the global `head` not updated → you forgot the seam-stitch step or the `left_bound is None` branch. | The output is fully reversed when the problem asked for partial reversals → wrong pattern; the problem decomposes into segment reversals, not one whole-list flip (move to subproblem pattern). |

The full-list reversal is the *primitive* the subproblem pattern calls — every subproblem-pattern solution invokes it once per chunk. The outer driver is what differentiates them.

---

## Template Code

```python
# Reversal-subproblem — generic skeleton for chunk-by-chunk in-place rewrites.
# The outer driver decides chunk size (fixed k / growing counter / toggle);
# the inner segment-reversal primitive and the seam stitch are reused unchanged.
from typing import Optional


class ListNode:
    def __init__(self, val=0, nxt=None):
        self.val = val
        self.next = nxt


def reverse(start: ListNode, end: ListNode) -> ListNode:
    # Flip next pointers between start and end in place.
    # right_bound caches end.next so the reversed tail re-attaches automatically.
    current = start
    right_bound = end.next
    previous = right_bound
    while current is not right_bound:
        next_node = current.next
        current.next = previous
        previous = current
        current = next_node
    return previous                                     # new head of the reversed chunk


def reversal_subproblem(head: Optional[ListNode], k: int) -> Optional[ListNode]:
    if head is None or head.next is None or k == 1:
        return head                                     # 1. trivial-case guards

    length = 0
    cur = head
    while cur is not None:                              # 2. precompute length
        length += 1
        cur = cur.next
    total_segments = length // k

    start = head                                        # 3. boundary init
    left_bound: Optional[ListNode] = None

    for _ in range(total_segments):                     # 4. outer driver
        end = start
        for _ in range(k - 1):                          #    advance to chunk end
            end = end.next

        reversed_head = reverse(start, end)             # 5. inner reversal

        if left_bound is None:                          # 6. seam stitch
            head = reversed_head
        else:
            left_bound.next = reversed_head

        left_bound = start                              # 7. slide boundary forward
        start = left_bound.next

    return head
```

The five knobs are: the **trivial-case guards** (`None`, single node, `k == 1`), the **outer driver shape** (fixed `total_segments`, growing `group_size`, or a toggling `should_reverse` flag), the **chunk-end walk** (`k − 1` hops, or `group_size − 1` hops), the **conditional reversal call** (always run, or guarded by the toggle), and the **seam stitch** (`left_bound.next = reversed_head` for all chunks except the first, which updates the global `head`).

---

## Common Mistakes

- **Forgetting the `left_bound is None` branch for the first chunk**:
  - *What*: writing `left_bound.next = reversed_head` unconditionally. The first iteration crashes with a `NoneType` access, or — worse, if you initialise `left_bound = head` defensively — silently corrupts the global `head` pointer.
  - *Why*: there is no predecessor before the first chunk's head; the global `head` itself must be updated instead. Initialising `left_bound = None` and branching on it is what tells the seam-stitch step which path to take.
  - *Fix*: keep `left_bound = None` at start, and use `if left_bound is None: head = reversed_head else: left_bound.next = reversed_head` inside the loop.
- **Setting `left_bound = end` instead of `left_bound = start` after a reverse**:
  - *What*: after calling `reverse(start, end)`, updating `left_bound = end`. The next iteration's seam stitch then writes through the wrong node and the chunk-tail re-attachment goes to the wrong place.
  - *Why*: after the reversal, the chunk's tail is the *old `start`*, not the old `end`. The old `start` now sits at the end of the chunk in the new list ordering — that's the natural `left_bound` for the next chunk.
  - *Fix*: `left_bound = start` after a reverse; `left_bound = end` only in the skip branch of reverse-alternate-segments (where no reversal happened and `end` is still the chunk's tail).
- **Caching `right_bound` AFTER starting the reversal**:
  - *What*: inside `reverse(start, end)`, reading `end.next` partway through the link-flip loop. Once the first `current.next = previous` runs, `end.next` may already point at a node inside the chunk and the loop never terminates.
  - *Why*: the segment-reversal primitive depends on a stable `right_bound` to know when to stop and what the reversed tail should point at. Cache it once before the loop.
  - *Fix*: `right_bound = end.next` and `previous = right_bound` are the first two lines of `reverse`, before the loop starts.
- **Re-walking the list from `head` to find each chunk's `end`**:
  - *What*: writing `end = get_node_at_position(head, group_offset)` where `group_offset` is the chunk's absolute index. The outer cost balloons to `O(n²)` because every chunk re-traverses the prefix.
  - *Why*: the local `start` pointer already sits at the chunk's first node — advancing from `start` by `k − 1` hops is `O(k)`, not `O(n)`.
  - *Fix*: `end = get_node_at_position(start, k)` walks from the local `start`, keeping the per-chunk work proportional to the chunk size.
- **Not toggling `should_reverse` before the next iteration (reverse-alternate-segments)**:
  - *What*: writing `should_reverse = True` inside the reverse branch and `should_reverse = False` inside the skip branch instead of toggling once at the end of the loop body. The output reverses every chunk (or skips every chunk) instead of alternating.
  - *Why*: the toggle is a per-iteration switch — its job is to flip the flag exactly once per chunk, not to assert what the flag was on entry.
  - *Fix*: `should_reverse = not should_reverse` at the bottom of the loop body, after the conditional reversal and the boundary slide.

---

## Minimum Viable Example

Pairwise swap on `head = [1, 2, 3, 4]` (the simplest chunk size, `k = 2`):

```
start = 1, left_bound = None
  → end = 2 → reverse(1, 2) → 2 → 1 → 3 → 4 → head = 2 → left_bound = 1 → start = 3
start = 3, left_bound = 1
  → end = 4 → reverse(3, 4) → 2 → 1 → 4 → 3 → 1.next = 4 → left_bound = 3 → start = None

Result: [2, 1, 4, 3]
```

Four nodes, two outer iterations, two seam stitches, the pattern in twelve lines.

---

## Quick Recall

**Q: What is the time complexity of any problem in this pattern?**
A: `O(n)` time and `O(1)` space — each node is touched a constant number of times by the chunk-end walk and the inner reversal.

**Q: Why is `left_bound` initialised to `None` instead of `head`?**
A: `None` is the sentinel that tells the seam-stitch step "this is the first chunk — update the global `head` instead of writing through a predecessor."

**Q: After a chunk reversal, which node is the new `left_bound`?**
A: The old `start`. After the reversal it sits at the end of the chunk in the new list ordering, which is exactly the predecessor of the next chunk's head.

**Q: How does the outer driver change between the four variants?**
A: Pairwise swap uses a `while start and start.next` guard; reverse-k-segments uses a fixed `total_segments = length / k` counter; reverse-increasing-groups uses a growing `group_size` with a `length >= group_size` check; reverse-alternate-segments uses a fixed `total_segments` counter plus a toggling `should_reverse` flag.

**Q: Why is `right_bound` cached as `end.next` before the reversal starts?**
A: Once the loop flips `end.next`, the original successor is lost. Caching it before the loop lets `previous = right_bound` initialise correctly so the reversed chunk's tail already points at the next chunk's head.

**Q: What should you reach for first if the problem says "reverse every two/three/k adjacent nodes in place with `O(1)` space"?**
A: The four-boundary skeleton — `start`, `end`, `left_bound`, `right_bound` — wrapped around the shared `reverse(start, end)` helper. The only thing that varies is the outer driver's rule for advancing `end` and toggling reversal.
