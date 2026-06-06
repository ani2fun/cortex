---
title: "Memorize: Reorder"
summary: "Pattern recognition triggers, template code, common mistakes, and quick recall."
prereqs:
  - 09-pattern-reorder/01-pattern
---

# Memorize: Reorder

## In a Hurry?

- **One-Line Idea**: Reorder a DLL in place by splitting its nodes into `k` temporary sub-lists with a classifier `f1`, then weaving them back together with a selector `f2` — every splice writes both `next` AND its mirror `prev`.
- **Complexities**: `O(n)` time, `O(1)` extra space. `n` is the input list's length; the only allocations are a constant number of dummy heads.
- **When to Use**: The problem rearranges the nodes of one DLL in place, and the target order can be expressed as "route by `O(1)` classifier, then weave by `O(1)` selector".

---

## One-Line Mnemonic

**"Split by `f1`, weave by `f2`, mirror every link."**

Every reorder variant on a DLL — parity-order, value-partition, relocate-node, zig-zag shuffle — is the same two-pass pipeline. Pick `f1` (the classifier that routes input nodes into buckets), pick `f2` (the selector that picks which bucket contributes the next output node), and write *both* directions on every splice. The pipeline body is identical across variants; the two functions plus the mirror-write discipline are the whole problem.

---

## Real-World Analogy

Picture a postal sorting room with one input belt and two outgoing slots. A clerk reads each envelope as it arrives, applies a rule (the classifier `f1`) — for example "if the zip code is odd, send left; otherwise send right" — and drops the envelope into the chosen slot. Each drop staples a tag onto the back of the envelope pointing at its left-hand neighbour in the pile (the mirror update). Once the input belt is empty, both piles are linked in arrival order in both directions. A second clerk then picks up the piles and walks them in lockstep, applying a different rule (the selector `f2`) — for example "always take from the left pile until it's empty, then take the rest from the right" — and lays the envelopes onto an output belt. Each placement also writes the back-tag to the previous envelope on the belt. The same envelopes leave the room, but the order they leave in is dictated entirely by `f1` and `f2`, and every envelope still knows who's behind it in the line.

---

## Visual Summary

```mermaid
flowchart LR
  before["doubly list"] --> rule["regroup by a rule<br/>(odd/even positions · partition by x)"]
  rule --> after["relink prev + next for the new order"]
  style after fill:#bbf7d0,stroke:#16a34a
```

<p align="center"><strong>Regroup a doubly list by position or pivot like its singly cousin, but maintain both links as you splice — every moved node needs its prev and next repaired. O(n), in place.</strong></p>

---

## Pattern Recognition Triggers

The pattern fits when **all four** answers are "yes" — the same diagnostic that gates each problem in the section.

- The problem **rearranges the nodes of one input DLL in place**, producing an output that contains the same nodes in a different order with both `prev` and `next` chains intact.
- The target order can be expressed as **"route nodes by an `O(1)` classifier `f1`, then weave them by an `O(1)` selector `f2`"** — without sorting, without random access, without re-reading the list multiple times.
- The resulting sub-lists are **bounded in count** (typically two, occasionally `k`) and consumable in one forward pass during the merge.
- `O(1)` extra space is **sufficient** for the buckets and the merge state. The only allocations should be dummy heads — one per bucket plus one for the output.

Common surface signals: "reorder in place," "group by parity," "partition around a value," "move the last node to the front," "zig-zag the list," "shuffle by index," "interleave first half with reversed second half" — with the extra hint of a doubly-linked input so the answer must keep `prev` honest end-to-end.

---

## Don't Confuse With

| | **Reorder (this pattern)** | **DLL Reversal (pattern 06)** | **Reversal Subproblem (pattern 07)** |
|---|---|---|---|
| **Problem shape** | "Rearrange the nodes of one input DLL in place into a new order, by composing split and merge." | "Flip the entire list end-to-end — `prev` ↔ `next` on every node." | "Reverse a *contiguous segment* of the list and re-stitch the four boundary links." |
| **Number of segments touched** | All nodes — distributed across `k` buckets, then concatenated/woven. | All nodes — one global pass. | A bounded sub-range — the rest of the list is untouched. |
| **Splice shape per node** | `tail.next = node; node.prev = tail` — every attach is a paired write. | One paired write per node: `current.prev, current.next = current.next, current.prev`. | Boundary stitches at `start.prev`, `start.next`, `end.prev`, `end.next` — eight writes total around the segment. |
| **Per-step decision** | `f1` for the split phase, `f2` for the merge phase. Two `O(1)` functions, one per pass. | None — every node gets the same swap. | None — the segment is determined by `start` and `end`. |
| **When this goes wrong** | The reorder pipeline produces a partially-corrupt chain — likely the split pass forgot to terminate one bucket with `null` in either direction, so the merge pass walks past the bucket end into stale input nodes, or the new head still points back at a throwaway dummy. Symptom: forward print looks fine, but backward print explodes or loops. Always set `bucket_tail.next = null` AND `bucket_head.prev = null` after the split pass. | The forward chain looks reversed but the backward chain is corrupt — likely you swapped `next` first and then read `next` to advance, walking off in the wrong direction. Wrong pattern direction; revisit "snapshot `next` first, swap second, advance third." | The segment is reversed in isolation but the surrounding list is detached — likely you forgot one of the four boundary writes (e.g. `right_bound.prev = start` or `left_bound.next = end`). Wrong pattern direction; revisit the four-corner boundary checklist. |

The three patterns share the dummy-head + tail idiom and the mirror-write discipline; what differs is whether you split-then-merge (reorder), flip everything (full reversal), or surgically flip a segment (reversal subproblem).

---

## Template Code

```python
# Reorder — generic split-then-merge pipeline for a doubly linked list.
# Swap out `classify` (f1) and `select_winner` (f2) to specialise to a
# concrete reorder variant. Every splice writes both `next` AND its
# mirror `prev` — the only difference from the singly-linked template.
from typing import Optional


class ListNode:
    def __init__(self, val=0, prev=None, next=None):
        self.val = val
        self.prev = prev
        self.next = next


def reorder(head: Optional[ListNode]) -> Optional[ListNode]:
    """
    Two-pass reorder on a DLL: split into two buckets by f1, then merge by f2.
    Generalises to k buckets by widening the cursors below.
    """
    if head is None or head.next is None:
        return head                                # 1. trivial inputs

    # --- Split pass (uses f1 = classify) ---
    dummyA = ListNode()
    tailA = dummyA                                 # 2. bucket A
    dummyB = ListNode()
    tailB = dummyB                                 # 3. bucket B

    current = head
    counter = 1                                    # 4. optional state for f1
    while current is not None:
        if classify(current, counter):             # 5. f1 — O(1) classifier
            tailA.next = current
            current.prev = tailA                   # 6. MIRROR — DLL-specific
            tailA = current
        else:
            tailB.next = current
            current.prev = tailB                   # 6. MIRROR — DLL-specific
            tailB = current
        current = current.next                     # 7. advance the input cursor
        counter += 1

    tailA.next = None                              # 8. terminate both buckets
    tailB.next = None                              #    in the forward direction
    if dummyA.next is not None:
        dummyA.next.prev = None                    # 9. terminate both buckets
    if dummyB.next is not None:
        dummyB.next.prev = None                    #    in the back direction

    headA, headB = dummyA.next, dummyB.next        # 10. real bucket heads

    # --- Merge pass (uses f2 = select_winner) ---
    dummy = ListNode()
    tail = dummy
    currentA, currentB = headA, headB
    while currentA is not None and currentB is not None:
        if select_winner(currentA, currentB):      # 11. f2 — O(1) selector
            winner, currentA = currentA, currentA.next
        else:
            winner, currentB = currentB, currentB.next
        tail.next = winner
        winner.prev = tail                         # 12. MIRROR — DLL-specific
        tail = winner

    drain = currentA if currentA is not None else currentB
    tail.next = drain
    if drain is not None:
        drain.prev = tail                          # 13. MIRROR on drain

    new_head = dummy.next
    if new_head is not None:
        new_head.prev = None                       # 14. sever the dummy's back-link
    return new_head                                # 15. skip the dummy
```

The two knobs are `classify` (line 5 — `counter % 2 == 1` for parity-order, `node.val < X` for value-partition) and `select_winner` (line 11 — a flipping boolean for alternate-fuse, plain concatenation by setting `tail.next = headB` / `headB.prev = tail` after walking headA's tail for simpler variants). The DLL-specific lines are 6, 9, 12, 13, and 14 — the mirror writes that keep `prev` honest. Drop any one and the forward chain still prints fine; the backward chain silently breaks.

---

## Common Mistakes

- **Forgetting the mirror `prev` write on the bucket splices**:
  - *What*: writing `tailA.next = current` but not `current.prev = tailA` inside the split loop. The forward chain of bucket A is built correctly, but every node's `prev` still points at its original predecessor in the input — usually a node now living in bucket B.
  - *Why*: the singly-linked template only needs one pointer per attach. Muscle memory carries the one-line splice into DLL code and silently drops the mirror.
  - *Fix*: write splices as paired statements: `tail.next = current; current.prev = tail`. Treat the two lines as one indivisible step. Code reviews on DLL code should grep for unpaired `\.next = ` lines.
- **Forgetting to null the new heads' `prev` after the split terminate step**:
  - *What*: writing `tailA.next = null; tailB.next = null` at the end of the split, but leaving `dummyA.next.prev` and `dummyB.next.prev` pointing at the throwaway dummies. Forward traversal from each bucket head looks fine; backward traversal from any merged-list interior node steps into a stale dummy.
  - *Why*: the dummies were always there to anchor the *forward* splice. They linger as silent back-link targets when the loop ends because nothing rewrites them.
  - *Fix*: pair the forward terminator with a backward terminator: after `tailA.next = null`, write `if dummyA.next is not None: dummyA.next.prev = None`. Same for bucket B. Same for the output dummy after the merge pass.
- **Forgetting to sever both directions when cutting the list at a boundary**:
  - *What*: in shuffle-list and relocate-node, writing only `slow.next = null` to cut the list in half. The first half's forward chain stops at the cut, but `slow.next`'s original successor still has `prev` pointing back into the first half — a dangling reference across the cut.
  - *Why*: on a singly-linked list one write severs the connection. On a DLL the connection is two writes wide.
  - *Fix*: cut both directions explicitly. The odd-length shuffle cut is two lines: `slow.next.prev = null; slow.next = null`. The even-length cut is the mirror: `slow.prev.next = null; slow.prev = null`. Always two writes.
- **Reading `current.next` after the splice instead of before**:
  - *What*: writing `tailA.next = current; tailA = current; current = current.next` and noticing that `current.next` was *never* overwritten — so the read still works. But the *next* iteration's splice overwrites it, and the input cursor jumps forward by an extra node.
  - *Why*: on a DLL the splice rewrites `current.prev` (which the loop doesn't read) but leaves `current.next` alone, so the bug stays latent on small inputs. It surfaces only on larger inputs where the unread input nodes diverge.
  - *Fix*: read first, splice second, advance third — same as singly-linked. Treat `current = current.next` as the very last statement in the loop body.
- **Forgetting to compose `f1` correctly when the variant pre-processes a sub-list**:
  - *What*: in shuffle-list, treating `f1` as a one-line classifier when it's actually a small pipeline (split at the middle with the DLL two-direction cut, then reverse the second half with `swap(prev, next)`). Trying to inline it as `index % 2` produces the wrong sub-lists.
  - *Why*: most reorder variants have an `f1` that fits in one expression, but a few — the composite reorders — have an `f1` that itself uses earlier patterns. Pattern-matching on the simpler shape silently picks the wrong decomposition.
  - *Fix*: when the target order has "first half / reversed second half" structure, write `f1` as two steps: split at the middle (sever both directions), then reverse the second half (one `swap(prev, next)` per node). Then `f2` is plain alternate-fuse with the mirror wired. Each step is a single-pass walk you've already mastered.

---

## Minimum Viable Example

Reorder `A = 1 ⇄ 2 ⇄ 3 ⇄ 4` into parity-order (`f1 = counter % 2`, `f2 = concatenate`):

```
Split (with mirror on every attach):
  ⊙_o → 1 ⇄ 3 → null    (odd-indexed bucket; 1.prev = null after terminate)
  ⊙_e → 2 ⇄ 4 → null    (even-indexed bucket; 2.prev = null after terminate)
Merge (concatenate, with mirror):
  walk odd tail to 3; set 3.next = 2 AND 2.prev = 3.
Result: 1 ⇄ 3 ⇄ 2 ⇄ 4 → null  (head.prev = null end-to-end)
```

Four nodes, one split pass plus one tail walk, zero allocations beyond the two dummies — the complete pattern in five lines, mirrors honest.

---

## Quick Recall

**Q: What is the time and space complexity of the reorder pipeline on a DLL?**
A: `O(n)` time for the split pass plus `O(n)` for the merge pass (`O(n)` total) and `O(1)` extra space (a constant number of dummy heads and cursors, regardless of input size). The mirror writes add a constant factor to each step; they don't change the asymptotic envelope.

**Q: What are the two knobs that customise a reorder variant?**
A: `f1` — the `O(1)` classifier that routes input nodes into temporary buckets during the split pass — and `f2` — the `O(1)` selector that picks which bucket contributes the next output node during the merge pass. On a DLL both knobs run inside a pipeline that wires `prev` on every attach.

**Q: Why does the split pass need to terminate each bucket in both directions?**
A: Because the bucket nodes' `next` fields still point into the input chain unless explicitly overwritten, and the bucket heads' `prev` fields still point at the throwaway dummies. Without `tail.next = null` the merge pass walks past the bucket end into stale nodes; without `head.prev = null` any backward traversal from inside the merged output steps into a dummy that the GC otherwise reclaims.

**Q: When does the merge step degenerate to plain concatenation on a DLL?**
A: When `f2` is "drain bucket A entirely, then drain bucket B". Walk bucket A to its tail, set `tail.next = head_of_B` AND `head_of_B.prev = tail`, and return `head_of_A`. Used by parity-order, value-partition, and relocate-node. The mirror write is the DLL upgrade over the singly-linked version.

**Q: How does the reorder pattern relate to the DLL reversal pattern (pattern 06)?**
A: Pattern 06 is a *primitive* — flip every node's two pointer fields in one stroke. The reorder pattern *uses* pattern 06 as a sub-routine inside shuffle-list's `f1` (split at the middle, then reverse the second half). Other reorder variants don't reverse anything; they just split and concat. The relationship is composition: reversal is a tool the reorder pipeline reaches for when the target shape demands it.

**Q: What is the most expensive reorder variant on a DLL?**
A: Shuffle-list. Its `f1` is a composite of two earlier primitives — find-the-middle (fast-and-slow) and reverse-the-second-half (DLL `swap(prev, next)`) — before the merge selector ever runs. Even so, the total cost is `O(n)` time and `O(1)` extra space, because all three sub-passes are single-pass and disjoint. The DLL upgrade over the singly-linked version is one extra pointer write per attach in every sub-pass.
