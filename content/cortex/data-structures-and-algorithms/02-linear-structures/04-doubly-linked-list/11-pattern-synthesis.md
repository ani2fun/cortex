---
title: "Pattern Synthesis: Doubly Linked List"
summary: "When to use which pattern, common confusions, and synthesis problems for doubly linked list."
prereqs:
  - 02-linear-structures/04-doubly-linked-list/09-pattern-reorder/03-memorize
---

# Pattern Synthesis: Doubly Linked List

Every doubly-linked-list pattern in this chapter solves a specific *shape* of problem on top of the same invariant: every link is two pointers, never one. The hard part is not learning each pattern in isolation — it is choosing the right one when a fresh problem lands on your desk. This synthesis chapter exists for exactly that moment: read the problem, run the decision tree, confirm with the comparison table, then write code.

The four patterns in scope:

- **Reversal** — walk a single contiguous segment and swap each node's `prev` and `next` in one stroke, then stitch four boundary links.
- **Reversal Subproblem** — outer driver chunks the list; the inner reversal helper flips each chunk and re-stitches both directions at every seam.
- **Two Pointers** — `left` walks forward from `head` via `.next`, `right` walks backward from `tail` via `.prev`, converging inward in `O(1)` per step because the `prev` field makes the backward hop free.
- **Reorder** — split the chain into temporary sub-lists by an `O(1)` classifier `f1`, then weave them back together by an `O(1)` selector `f2` — every splice writes both `next` AND its mirror `prev`.

---

## When to Use Which Pattern

The first question is whether the answer *changes the structure* of the chain or only *points at a node*. That single question splits the four patterns into two families. The second question is the *cardinality* of the rewrite — one segment, many segments, or all nodes re-routed through buckets. From those two answers the right pattern usually falls out.

### The decision tree (prose form)

Walk top to bottom; stop at the first match.

1. **Does the answer change the *structure* of one list (some `prev`/`next` pointers must be rewritten)?**
   - The rewrite covers a single contiguous segment — full list, prefix, suffix, or positional range `[left, right]` → **Reversal**.
   - The rewrite decomposes into many chunks, each itself a segment reversal — pairwise, every `k`, increasing chunks, alternate chunks → **Reversal Subproblem**.
   - The rewrite re-routes every node through `k` temporary buckets and then weaves them back together — group by parity, partition by value, relocate a single node, zig-zag shuffle → **Reorder**.

2. **Does the answer compare or pair *two nodes at once* without changing structure?**
   - Pointers start at opposite ends (`left = head`, `right = tail`) and converge inward — palindrome, paired-sum on a sorted DLL, fix-one-reduce three-sum → **Two Pointers**.

3. If nothing above matches, you are probably outside this chapter — try hashing, a stack-based pattern, or a different data structure entirely.

### Decision walkthrough — every pattern, once

Run the tree on a one-line problem for each of the four patterns to confirm the branches resolve cleanly.

- **"Reverse the doubly linked list between positions `left` and `right`."** → Step 1 yes; a single contiguous segment → **Reversal**.
- **"Reverse the doubly linked list in groups of `k`."** → Step 1 yes; many fixed-size chunks chained by an outer driver → **Reversal Subproblem**.
- **"Find a pair summing to `target` in a sorted doubly linked list."** → Step 1 no; Step 2 yes — pair of nodes, opposite ends, converging → **Two Pointers**.
- **"Reorder a doubly linked list into parity order (odd-indexed nodes first, then even-indexed)."** → Step 1 yes; every node re-routed through two buckets then concatenated → **Reorder**.

So the key idea is: structure-or-position narrows to one of two branches; cardinality (one segment vs many vs all-through-buckets) picks the leaf of the structural branch.

---

## Pattern Comparison Table

All four patterns at a glance. `n` is the input list length, `k` is a problem-specific constant (chunk size in the subproblem pattern, bucket count in the reorder pattern).

| Pattern | Problem shape | Key signal | Time | Space | Confused with |
|---|---|---|---|---|---|
| **Reversal** | Flip one contiguous segment in place by swapping `prev`/`next` per node | "Reverse the list", "reverse first/last `k`", "reverse between `left` and `right`", "is this DLL a palindrome via reverse" | `O(n)` | `O(1)` | Reversal Subproblem (one segment vs many?), Reorder (single sweep vs split-then-merge?) |
| **Reversal Subproblem** | Many segment reversals chained by an outer chunk-boundary walk and bidirectional seam stitches | "Swap adjacent pairs", "reverse in groups of `k`", "reverse increasing chunks", "reverse alternate segments" | `O(n)` | `O(1)` | Reversal (one segment or many?), Reorder (chunked flips vs split-then-merge?) |
| **Two Pointers** | Read a *pair* of nodes from opposite ends and converge inward, `O(1)` work per step | "Sorted DLL + pair", "palindrome check", "two-sum on a sorted DLL", "fix-one-reduce three-sum" | `O(n)` | `O(1)` | Reversal (compare a pair or rewrite a segment?), Reorder (pair-walk or split-then-merge?) |
| **Reorder** | Re-route every node through `k` buckets via `f1`, then weave them back via `f2` — every splice writes both directions | "Group by parity", "partition by value", "relocate a node", "zig-zag shuffle", "interleave first half with reversed second half" | `O(n)` | `O(1)` | Reversal (rebuild via buckets or flip a segment?), Reversal Subproblem (split-then-merge or chunked flips?) |

---

## Common Confusions

Four pairs trip readers up most often. Each subsection states the surface similarity, the one question that cuts through it, and the symptom that betrays a wrong choice.

### Reversal vs Reversal Subproblem

**Why they look the same.** Both rewrite `prev` and `next` pointers in place using the per-node swap from pattern 06. Both run in `O(n)` time and `O(1)` space. The inner swap loop body is *identical* — `reversal_subproblem` calls `reverse(start, end)` once per chunk.

**The distinguishing test.** Does the problem ask for *one* contiguous segment to be reversed, or for *many* segments to be reversed in sequence by an outer driver?

**Telltale symptom of wrong choice.** You wrote a single per-node swap sweep for "reverse the DLL in groups of `k`." The first chunk reverses correctly; the rest is either untouched or its backward chain is broken at the chunk seams. The missing pieces are the outer chunk-boundary loop, the bidirectional seam stitch after every chunk reversal, and the post-hoc `end.prev == None` head-promotion check — that whole bundle is the subproblem pattern's job.

### Reversal vs Two Pointers

**Why they look the same.** Both walk a doubly linked list with two cursors and finish in `O(n)` time, `O(1)` space. The reversal pattern in its segment form captures `leftBound = start.prev` and `rightBound = end.next` before the loop; the two-pointer pattern names them `left` and `right`. Surface notation alone won't separate them.

**The distinguishing test.** Does each iteration *rewrite* a node's pointer fields (reversal), or does each iteration only *read* a node's value and *advance* the cursors (two pointers)?

**Telltale symptom of wrong choice.** You used the two-pointer skeleton on "reverse between positions `left` and `right`." The pointers converge correctly but no `prev`/`next` is ever swapped, so the chain is unchanged at the end. Conversely, you wrote a per-node `current.prev, current.next = current.next, current.prev` swap inside a "palindrome check via mirror comparison" — and now the list has been *destructively* flipped instead of inspected. The fix is to switch the inner loop's intent: structure-changing → reversal, pure-comparison → two pointers.

### Two Pointers vs Reorder

**Why they look the same.** Both terminate with one output that depends on reading two cursors in lockstep. Reorder's *merge pass* reads `currentA` and `currentB` exactly the way two pointers reads `left` and `right`, advancing whichever cursor `f2` selects.

**The distinguishing test.** Are the two cursors walking *the same list from opposite ends* (two pointers), or are they walking *two already-separated sub-lists that the previous pass built* (reorder's merge phase)?

**Telltale symptom of wrong choice.** You called the two-pointer skeleton on "parity-order this list." The loop starts with `left = head` and `right = tail`, but there's nothing to compare — the output order depends on each node's *index* in the original chain, not on a relationship between mirrored ends. Conversely, you tried to write the split-then-merge pipeline for "is this DLL a palindrome." There are no buckets to route nodes into; the answer is a single boolean read out of one inward walk. Surface signal: two pointers has *one* list and *opposite-end* starts; reorder has *one input that becomes two* and merges them in a *second pass*.

### Reorder vs Reversal Subproblem

**Why they look the same.** Both can be described as "rearrange every node in place into a new order with `O(1)` extra space." Both touch every node in the list, not just a sub-range. Both rely heavily on the mirror-write discipline.

**The distinguishing test.** Is the rearrangement a sequence of *contiguous-segment flips* (subproblem), or is it a *route-through-buckets-then-weave* pipeline (reorder)?

**Telltale symptom of wrong choice.** You reached for the chunk-by-chunk reversal driver on "interleave the first half with the reversed second half." Each chunk reverses fine in isolation, but the target order isn't local — node `i` from the front half needs to land next to node `n − 1 − i` from the back half, which no per-chunk flip will produce. Conversely, you set up dummy heads and bucket cursors for "reverse the list in groups of `k`." The classifier `f1` has nothing meaningful to compute — every node belongs in "the current chunk" — so the split phase is just re-emitting the input. The decisive question is: does the answer come from *flipping segments* or from *re-routing through buckets and weaving back*?

---

## Synthesis Problems

Three problems where two or more patterns *seem* to fit. The right call is non-obvious from the statement alone; the wrong call costs you correctness or a complexity tier.

### Problem 1 — Palindrome Check on a Sorted DLL

**Statement.** Given the head of a doubly linked list, return `true` if the values read from `head` to `tail` equal the values read from `tail` to `head`. (See [Palindrome Number](08-pattern-two-pointers/02-problems/01-palindrome-number.md).)

**Why multiple patterns seem viable.**

- **Reversal** seems viable because "read backwards" suggests flipping the back half and walking two pointers forward against it, the way the singly-linked chapter solves it.
- **Reorder** seems viable because the problem looks like "split at the middle, reverse the second half, then compare" — a pipeline very close to the reorder template.
- **Two Pointers** is the actual winner on a DLL.

**Winner.** Two Pointers — `left = head`, `right = tail`, converge inward comparing values until they meet or cross.

**Why winner wins.** The doubly linked list already encodes both directions, so there is no need to *reconstruct* the reverse walk by flipping the back half. The `prev` field makes `right = right.prev` free, which is exactly the operation a singly linked list cannot do in `O(1)`. Reversal alone over-engineers the problem: it pays an `O(n)` destructive rewrite just to read what `right.prev` already gives for free, and then has to flip it back to leave the list intact. Reorder is even more elaborate — it splits, transforms, and merges when the question only asks for a comparison. The two-pointer pattern reads each pair in `O(1)`, runs in `O(n)` time and `O(1)` space, and never mutates a single pointer field. The DLL's whole reason to exist over the SLL is that this problem becomes one inward sweep with no auxiliary structure.

### Problem 2 — Move the Last Node to the Front

**Statement.** Given the head of a doubly linked list, detach the last node and re-insert it as the new head, returning the new head. (See [Relocate Node](09-pattern-reorder/02-problems/01-relocate-node.md).)

**Why multiple patterns seem viable.**

- **Reversal** seems viable because the back-most node moving to the front *looks like* a one-step rotation, which a full-list reversal almost achieves.
- **Reorder** is the actual pattern — the operation is "classify the last node into bucket A, everything else into bucket B, then concat A onto B with mirror writes."
- **Two Pointers** seems plausible because you might walk `left` from the head and `right` from the tail and "swap" — but the answer is a re-link, not a value swap.

**Winner.** Reorder — specifically, the relocate-node specialisation where `f1` peels off the tail and `f2` is plain concatenation, with every splice paired by its mirror `prev` write.

**Why winner wins.** Reversal does the wrong thing — flipping the whole list produces `tail → … → head`, not `tail → head → second → … → second-to-last`. Two pointers cannot help either, because the problem is structural (re-link a node), not comparative (read a pair of values). The reorder pattern fits because the answer decomposes cleanly into a classifier ("is this the tail?" → bucket A; otherwise → bucket B) and a selector ("drain A first, then drain B"). The DLL twist is that every splice is paired: `tailA.next = oldTail; oldTail.prev = tailA`, then `oldTail.next = headB; headB.prev = oldTail`, and finally `newHead.prev = None`. Forget any one of those mirror writes and the forward chain looks correct while the backward chain steps into a stale node from before the relocation. Total cost: one `O(n)` walk to find the tail, then `O(1)` to splice, in `O(1)` extra space.

### Problem 3 — Reverse Every Pair of Adjacent Nodes

**Statement.** Given the head of a doubly linked list, swap every two adjacent nodes so the list `1 ⇄ 2 ⇄ 3 ⇄ 4` becomes `2 ⇄ 1 ⇄ 4 ⇄ 3`, and return the new head. (See [Pairwise Swap](07-pattern-reversal-subproblem/02-problems/01-pairwise-swap.md).)

**Why multiple patterns seem viable.**

- **Reversal** seems viable because each two-node block is itself a segment reversal, and the pattern's `reverse(start, end)` helper handles a segment of two cleanly.
- **Reorder** seems viable because the target order *looks* like an interleave — even-indexed and odd-indexed nodes alternating in pairs — which is exactly what a reorder pipeline produces.
- **Reversal Subproblem** is the actual winner.

**Winner.** Reversal Subproblem — outer driver walks two nodes at a time, inner `reverse(start, end)` flips each pair, bidirectional seam stitches re-attach the chunks.

**Why winner wins.** A single reversal call only solves one pair — calling it once leaves the other `n/2 − 1` pairs un-swapped. Reorder is technically expressive enough (`f1 = index % 2`, `f2 = alternate`) but it builds two full-length sub-lists for no reason: the target order is *local*, each pair is independent of every other pair, and the work doesn't need a global split phase. The subproblem pattern is purpose-built for "decompose into chunks of `k`, reverse each, restitch." The chunk size is `k = 2`; the outer driver detects the first chunk post-hoc via `end.prev == None` and promotes the new head; every subsequent chunk re-attaches to the previous chunk's tail via the bidirectional seam (`leftBound.next = end`, `end.prev = leftBound`, `start.next = rightBound`, `rightBound.prev = start`). Total cost: `O(n)` time, `O(1)` space, one outer pass plus a constant-work helper call per pair.

---

## Doubly-Linked-List-Specific Synthesis

Every pattern in this chapter shares the same upgrade over its singly-linked sibling: the `prev` field changes both the *cost* and the *bookkeeping* of the rewrite. The two effects pull in opposite directions, and the trade is visible in every pattern.

The cost upgrade is the headline:

- **Reversal** loses the three-pointer dance — one swap per node replaces the snapshot-flip-step sequence.
- **Reversal Subproblem** loses the explicit `leftBound` cache in the driver — the helper reads `start.prev` directly because the structure carries it.
- **Two Pointers** gains the `right.prev` backward step in `O(1)`, which is the entire reason the pattern even fits a linked list. On a singly linked list, "two pointers from opposite ends" is not a viable shape.
- **Reorder** loses the "walk to find the predecessor before unlinking" cost on every move — `node.prev` is one dereference away, so any splice that needs a predecessor finishes in constant time.

The bookkeeping cost is the counterweight:

- Every link is two pointer writes, not one. **"Set `a.next = b`"** always pairs with **"set `b.prev = a`"** — across reversal swaps, subproblem seam stitches, two-pointer guards (no rewriting, but the trivial-case guards still test both ends), and reorder splices.
- Every boundary write needs a `null` guard on its mirror. The four-corner stitch becomes the canonical pattern: `start.next = right_bound; if right_bound: right_bound.prev = start` is one inseparable two-line block, and the parallel `end.prev = left_bound; if left_bound: left_bound.next = end` is the other.
- Every cut severs both directions. On a singly linked list a cut is one write; on a DLL a cut is two — `slow.next.prev = None` then `slow.next = None`.
- Every termination needs *two* sentinels — odd-length lists meet (`left == right`), even-length lists cross (`left.prev == right`). The two-pointer pattern owns this guard explicitly; the other patterns inherit a quieter version of it through `head.prev == None` and `tail.next == None`.

So the tradeoff is: every DLL pattern is *simpler* in one direction (no auxiliary state for the back-link) and *more bookkeeping-heavy* in another (every write is paired). The discipline that catches every DLL bug is to write splices as two-line blocks and to assert `a.next.prev == a` mentally after every structural change.

---

## Practice Progression

A reasonable order for first-time readers and a reasonable order for spaced review are different. The first-time order builds primitives bottom-up; the review order tests pattern recognition from the top down.

**First-time order** (one problem per pattern, learn the mirror-write discipline first):

1. [Reverse a List](06-pattern-reversal/02-problems/01-reverse-a-list.md) — the per-node `prev`/`next` swap in its simplest setting, no boundary stitches because both bounds are `None`.
2. [Two Sum](08-pattern-two-pointers/02-problems/02-two-sum.md) — the only chapter pattern that doesn't rewrite anything, so it lets the reader internalise `left.next` / `right.prev` lockstep without juggling structural edits.
3. [Pairwise Swap](07-pattern-reversal-subproblem/02-problems/01-pairwise-swap.md) — the smallest subproblem-pattern instance (chunk size 2), which forces the four-boundary seam stitch and the `end.prev == None` head-promotion check.
4. [Parity Order](09-pattern-reorder/02-problems/02-parity-order.md) — the cleanest reorder variant, where `f1 = counter % 2` and `f2 = concatenate` and every splice exercises the mirror `prev` write.

**Review order** (one harder problem per pattern, exercises the patterns under pressure):

1. [Reverse the Given Segment](06-pattern-reversal/02-problems/04-reverse-the-given-segment.md) — the four-corner stitch under maximum stress: any of the four boundary writes can be forgotten without breaking the forward chain.
2. [Approximate Three Sum](08-pattern-two-pointers/02-problems/04-approximate-three-sum.md) — outer pin plus inner two-pointer scan, with the running-`closestSum` tracker stretching the loop body.
3. [Reverse Alternate Segments](07-pattern-reversal-subproblem/02-problems/04-reverse-alternate-segments.md) — the toggling outer driver plus the asymmetric `start = end; start = start.next` advance in the skip branch.
4. [Shuffle List](09-pattern-reorder/02-problems/04-shuffle-list.md) — the only reorder variant whose `f1` is a composite (split at the middle with a two-direction cut, then reverse the second half), which makes every DLL invariant relevant in one problem.

If both passes still leave a pattern feeling shaky, the corresponding `03-memorize.md` is the single best re-read — each one ends with a Quick Recall section that distils the pattern to one fact per question.

---

## How to Use This Chapter

Treat the decision tree as the first pass and the comparison table as the second pass. If both still point to two viable patterns, walk the Common Confusions for the matching pair and run its distinguishing test on the input. If a synthesis-style problem feels like every pattern fits, that is the signal to slow down. The right pattern is usually the one whose *answer shape* matches the question, not the one whose surface notation matches the input. For doubly linked lists specifically, two questions decide almost every call: "does the answer change structure, or only read a pair of nodes?" and — if structural — "is the rewrite one segment, many segments, or every node re-routed through buckets?" Every other choice follows from those two.
