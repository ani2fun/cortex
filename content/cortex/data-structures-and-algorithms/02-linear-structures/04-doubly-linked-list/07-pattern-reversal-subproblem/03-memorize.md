---
title: "Memorize: Reversal (Subproblem)"
summary: "Pattern recognition triggers, template code, common mistakes, and quick recall."
prereqs:
  - 07-pattern-reversal-subproblem/01-pattern
---

# Memorize: Reversal (Subproblem)

## In a Hurry?

- **One-Line Idea**: Decompose the rewrite into chunk-sized segment reversals, drive the chunk boundary with an outer counter (fixed `k`, growing `1, 2, 3, …`, or a toggling flag), and reuse the lesson-5 bidirectional `reverse(start, end)` helper — which swaps `prev`/`next` per node and re-stitches the four boundary links — for every chunk.
- **Complexities**: `O(n)` time, `O(1)` space, where `n` is the length of the doubly linked list. Each node is touched a constant number of times across the outer boundary walk and the inner reversal call.
- **When to Use**: The problem asks for partial-list flips — swap every two adjacent nodes, reverse every chunk of `k`, reverse increasing-size runs, or reverse alternate segments — and the rewrite must operate by editing `prev` and `next` pointers in place with `O(1)` auxiliary memory.

---

## One-Line Mnemonic

**"Slice, swap-both, stitch-both, slide."**

The image is a four-step dance per chunk: slice the next chunk by walking `end` from `start`, swap `prev` and `next` on every node inside the chunk, stitch the four boundary links bidirectionally (`leftBound.next ↔ end`, `start.next ↔ rightBound`), then slide the boundary forward to the next chunk. The "both" in steps 2 and 3 is the doubly-linked twist — every operation that the singly-linked version does once, this version does in both directions.

---

## Real-World Analogy

Picture a row of two-way train cars connected by both a forward coupling and a backward coupling at each junction. A crew has been told to flip the order of cars inside specific carriages — every pair, every block of three, every alternate block. They cannot lift cars off the track; they can only rewire both couplings at each junction. The crew walks from the front, marks the start and end of the current group, uncouples the trailing connections in both directions so the rest of the train does not get dragged along, reverses the couplings inside the group — every car has its forward coupling and backward coupling swapped — then re-couples the reversed group's new front car to the back of the previous group on both rails. They never go back; once a group is done they slide forward and repeat. The chapter's `reverse(start, end)` helper is the bidirectional rewiring routine; the outer loop is the crew walking the train; the boundary pointers are the chalk marks they keep on the rails.

---

## Visual Summary

```mermaid
flowchart LR
  prim["whole-list swap-per-node primitive"] --> seg["apply to a segment:<br/>swap pairs · reverse k · reverse [i..j]"]
  seg --> relink["repair BOTH links (prev and next)<br/>at each boundary"]
  style relink fill:#bbf7d0,stroke:#16a34a
```

<p align="center"><strong>Reverse a doubly list in pieces, then re-stitch — but unlike the singly case you must repair both prev and next at each boundary. The two-way links make the in-place segment flip cheap.</strong></p>

---

## Pattern Recognition Triggers

The pattern fits when **all four** answers are "yes" — the same diagnostic that gates each problem in the section.

- The rewrite can be **decomposed** into a sequence of contiguous-chunk reversals — fixed-size chunks (pairwise swap, reverse-k-segments), growing chunks (reverse-increasing-groups), or conditionally-reversed chunks (reverse-alternate-segments).
- At least one subproblem is a **segment reversal** on a doubly linked list that the standard `reverse(start, end)` helper from chapter pattern 06 can solve in `O(k)` while keeping `prev` and `next` consistent.
- The algorithm only needs to **walk each node a constant number of times** — no random access from `head`, no per-chunk re-traversal.
- Each chunk's boundary is **computable from local state** — the chunk size (`k`, growing counter, toggling flag) plus the implicit `start.prev`/`end.next` reads inside the helper.

Common surface signals: "swap every two adjacent nodes," "reverse the list in groups of `k`," "reverse alternate segments of size `k`," "reverse the first half / second half / a sublist `[i..j]`," "reverse `1, 2, 3, …` nodes in increasing chunks."

---

## Don't Confuse With

| | **Reversal Subproblem on DLL (this pattern)** | **Full-List Reversal on DLL (chapter pattern 06)** |
|---|---|---|
| **Problem shape** | "Reverse every chunk of `k`," "swap adjacent pairs," "reverse alternate segments" | "Reverse the entire doubly linked list" |
| **Outer structure** | Outer loop iterates chunks; per-chunk `start`/`end` walked locally; first-chunk detected post-hoc via `end.prev == None` | No outer loop — a single sweep that swaps `prev`/`next` on every node, returning the old tail as the new head |
| **Inner reversal** | Invoked once per chunk via `reverse(start, end)` helper; bidirectional boundary stitch after the per-node swap loop | Inlined into the single sweep — no helper, no seam, no boundary stitch beyond setting the final `head` |
| **Seam stitching** | Required — four boundary links re-attached bidirectionally after every chunk; first chunk also updates the global `head` | None — the whole list is one segment; the only adjustment is `head = old tail` |
| **Complexity** | `O(n)` time, `O(1)` space, same as full-list reversal but with a higher constant factor from the extra boundary walks and the four-pointer seam stitch per chunk | `O(n)` time, `O(1)` space, with the lowest constant factor — one `prev`/`next` swap per node |
| **When this goes wrong** | The forward chain looks correct but the backward chain is broken at the seams, or the global `head` is buried mid-list → you forgot the bidirectional seam stitch inside the helper or skipped the `end.prev == None` head-promotion check. | The output is fully reversed when the problem asked for partial reversals → wrong pattern; the problem decomposes into segment reversals, not one whole-list flip (move to subproblem pattern). |

The full-list reversal is the *primitive* the subproblem pattern calls — every subproblem-pattern solution invokes it once per chunk. The outer driver is what differentiates them.

---

## Template Code

```python
# Reversal-subproblem on a doubly linked list — generic skeleton for
# chunk-by-chunk in-place rewrites. The outer driver decides chunk size
# (fixed k / growing counter / toggle); the inner bidirectional reversal
# primitive and the post-hoc first-chunk detection are reused unchanged.
from typing import Optional


class ListNode:
    def __init__(self, val=0, prev=None, nxt=None):
        self.val = val
        self.prev = prev
        self.next = nxt


def reverse(start: ListNode, end: ListNode) -> None:
    # Swap prev/next on every node in [start, end], then re-stitch
    # the four boundary links bidirectionally so neither chain breaks.
    if start is None or start == end:
        return                                            # size-1 chunk no-op

    left_bound = start.prev                               # cache before flipping
    right_bound = end.next
    current = start

    while current is not right_bound:                     # per-node prev/next swap
        next_node = current.next
        current.prev, current.next = current.next, current.prev
        current = next_node

    start.next = right_bound                              # bidirectional seam
    if right_bound is not None:
        right_bound.prev = start
    end.prev = left_bound
    if left_bound is not None:
        left_bound.next = end


def reversal_subproblem(head: Optional[ListNode], k: int) -> Optional[ListNode]:
    if head is None or head.next is None or k == 1:
        return head                                       # 1. trivial-case guards

    length = 0
    cur = head
    while cur is not None:                                # 2. precompute length
        length += 1
        cur = cur.next
    total_segments = length // k

    start = head                                          # 3. boundary init

    for _ in range(total_segments):                       # 4. outer driver
        end = start
        for _ in range(k - 1):                            #    advance to chunk end
            end = end.next

        reverse(start, end)                               # 5. inner reversal

        if end.prev is None:                              # 6. first-chunk seam
            head = end

        start = start.next                                # 7. slide boundary forward

    return head
```

The five knobs are: the **trivial-case guards** (`None`, single node, `k == 1`), the **outer driver shape** (fixed `total_segments`, growing `group_size`, or a toggling `should_reverse` flag), the **chunk-end walk** (`k − 1` hops, or `group_size − 1` hops), the **conditional reversal call** (always run, or guarded by the toggle — and in the skip branch `start = end` before the `start = start.next` advance), and the **first-chunk seam** (`end.prev == None` is the post-hoc signal that updates the global `head`).

---

## Common Mistakes

- **Swapping only `next` and forgetting `prev`**:
  - *What*: writing `current.next = previous` (the singly-linked move) inside the per-node loop. The forward chain ends up reversed correctly while every `prev` pointer still points at the old neighbour.
  - *Why*: a doubly linked list has two pointers per node and both must flip together. Swapping only one direction leaves the structure inconsistent — forward traversal looks correct but reverse traversal walks back into the un-reversed prefix.
  - *Fix*: swap atomically with `current.prev, current.next = current.next, current.prev` so both pointers flip in lockstep.
- **Forgetting the bidirectional boundary stitch after the per-node loop**:
  - *What*: ending `reverse` immediately after the per-node swap loop. The chunk's interior is consistent, but the four boundary links (`leftBound.next`, `end.prev`, `start.next`, `rightBound.prev`) still point at the old neighbours.
  - *Why*: the per-node loop only touches nodes strictly inside `[start, end]`. The boundary nodes (`leftBound`, `rightBound`) live outside the chunk and their pointers into the chunk must be rewritten explicitly, in both directions.
  - *Fix*: after the loop, set `start.next = right_bound`, `right_bound.prev = start`, `end.prev = left_bound`, `left_bound.next = end` (each guarded if the boundary is `None`).
- **Reading `right_bound = end.next` AFTER starting the reversal**:
  - *What*: inside `reverse(start, end)`, reading `end.next` partway through the per-node loop. Once the first swap runs on `end`, `end.next` already points at the old `prev`-side neighbour and the loop body either crashes or walks the wrong direction.
  - *Why*: the helper depends on a stable `right_bound` to know when to stop and what the reversed tail should bidirectionally stitch to. Cache it once before the loop.
  - *Fix*: `left_bound = start.prev` and `right_bound = end.next` are the first two lines of `reverse`, before the per-node loop starts.
- **Skipping the `end.prev == None` head-promotion check**:
  - *What*: never reassigning `head` after a reversal. The function still works for chunks 2..N (the previous chunk's tail re-attaches via the bidirectional stitch), but the first chunk's new head is buried mid-list and the returned `head` points at the old first node, which is now the chunk's tail.
  - *Why*: the only chunk whose new head has no predecessor is the first one. The post-hoc check `end.prev == None` is the cheapest way to detect it without a separate `leftBound` sentinel.
  - *Fix*: after `reverse(start, end)`, write `if end is not None and end.prev is None: head = end` before advancing `start`.
- **Using `start = start.next` after a skipped chunk (reverse-alternate-segments)**:
  - *What*: writing `start = start.next` in both branches of the conditional. In the reverse branch this lands on the next chunk's head (because the old `start` is now the chunk's tail); in the skip branch this lands on the *second node of the just-skipped chunk* instead of past it.
  - *Why*: after a reverse, `start` is the chunk's tail and `start.next` is the next chunk's head. After a skip, `start` is still the chunk's head and needs to walk past the entire chunk first.
  - *Fix*: in the skip branch, first set `start = end` so `start` becomes the chunk's tail, then the shared `start = start.next` advance lands on the next chunk's head.

---

## Minimum Viable Example

Pairwise swap on `head = [1 ↔ 2 ↔ 3 ↔ 4]` (the simplest chunk size, `k = 2`):

```
start = 1
  → end = 2 → reverse(1, 2): swap prev/next on both; stitch 1.next = 3, 3.prev = 1, 2.prev = None
              list: 2 ↔ 1 ↔ 3 ↔ 4  →  end.prev == None → head = 2
              start = start.next = 3
start = 3
  → end = 4 → reverse(3, 4): swap prev/next on both; stitch 3.next = None, 4.prev = 1, 1.next = 4
              list: 2 ↔ 1 ↔ 4 ↔ 3  →  end.prev != None (4.prev = 1) → head stays 2
              start = start.next = None  →  loop exits

Result: [2, 1, 4, 3] — both chains intact
```

Four nodes, two outer iterations, two bidirectional seams, the pattern in twelve lines.

---

## Quick Recall

**Q: What is the time complexity of any problem in this pattern?**
A: `O(n)` time and `O(1)` space — each node is touched a constant number of times by the chunk-end walk and the inner bidirectional reversal.

**Q: Why is there no explicit `leftBound` variable in the doubly-linked driver?**
A: The `reverse` helper reads `start.prev` directly to find the predecessor — the doubly-linked structure already carries it. The singly-linked version needs an external cache because nodes have no `prev` pointer.

**Q: How does the doubly-linked driver detect the first chunk?**
A: Post-hoc, via `end.prev == None` after the reversal. The only chunk whose new head has no predecessor is the first one — every other chunk's new head is stitched to the previous chunk's tail in both directions.

**Q: How does the outer driver change between the four variants?**
A: Pairwise swap uses a `while start and start.next` guard; reverse-k-segments uses a fixed `total_segments = length / k` counter; reverse-increasing-groups uses a growing `group_size` with a `length >= group_size` check; reverse-alternate-segments uses a fixed `total_segments` counter plus a toggling `should_reverse` flag and an asymmetric `start` advance (`start = end` before `start = start.next` in the skip branch).

**Q: Why are `left_bound = start.prev` and `right_bound = end.next` cached at the top of `reverse`?**
A: Once the per-node loop swaps pointers on `start` and `end`, their original neighbours are unrecoverable. Caching both at the top lets the bidirectional seam stitch at the bottom of `reverse` rewrite all four boundary links correctly.

**Q: What should you reach for first if the problem says "reverse every two/three/k adjacent nodes in a doubly linked list with `O(1)` space"?**
A: The four-boundary skeleton — `start`, `end`, with `leftBound`/`rightBound` cached implicitly inside the helper — wrapped around the shared bidirectional `reverse(start, end)`. The only thing that varies is the outer driver's rule for advancing `end` and toggling reversal.
