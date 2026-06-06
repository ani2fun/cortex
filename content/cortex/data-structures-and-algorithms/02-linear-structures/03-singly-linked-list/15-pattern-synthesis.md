---
title: "Pattern Synthesis: Singly Linked List"
summary: "When to use which pattern, common confusions, and synthesis problems for singly linked list."
prereqs:
  - 02-linear-structures/03-singly-linked-list/13-pattern-reorder/03-memorize
---

# Pattern Synthesis: Singly Linked List

Every singly-linked-list pattern in this chapter solves a specific *shape* of problem. The hard part is not learning each pattern in isolation — it is choosing the right one when a fresh problem lands on your desk. This synthesis chapter exists for exactly that moment: read the problem, run the decision tree, confirm with the comparison table, then write code.

The seven patterns in scope:

- **Reversal** — flip a contiguous run of `next` pointers in place with three pointers.
- **Reversal Subproblem** — outer driver chunks the list; the inner reversal flips each chunk.
- **Sliding Window Traversal** — two pointers chained `k` apart march in lockstep down the list.
- **Fast-and-Slow Pointers** — two pointers at different *speeds* (1 vs 2) locate a proportional position.
- **Split** — one walker routes nodes to `k` output chains using a per-node classifier.
- **Merge** — many input heads feed one output chain via an `O(1)` selector.
- **Reorder** — split by `f1`, then merge by `f2` — a two-pass pipeline over the same nodes.

---

## When to Use Which Pattern

The first question is the *direction of the rewrite*, because that splits the seven patterns into three families almost immediately. The second question is the *cardinality* of inputs and outputs, plus whether you need a *position* or a *structural change*. From those two answers, the right pattern usually falls out.

### The decision tree (prose form)

Walk top to bottom; stop at the first match.

1. **Does the answer change the *structure* of one list (some `next` pointers must be rewritten)?**
   - The rewrite covers a single contiguous segment — full list, prefix, suffix, or positional range → **Reversal**.
   - The rewrite decomposes into many chunks, each itself a segment reversal — pairwise, every `k`, increasing chunks, alternate chunks → **Reversal Subproblem**.
   - The rewrite is "route every node to one of several output chains" → **Split**.
   - The rewrite is "combine several input chains into one" → **Merge**.
   - The rewrite is "rearrange one chain into a new order, by composing the previous two" → **Reorder**.

2. **Does the answer point to a *node* in one list without changing structure?**
   - The target node is at a **fixed offset** from the tail (or from another cursor) — `k`-th from end, last `k`, gap of `k` → **Sliding Window Traversal**.
   - The target node is at a **proportional position** — the middle, the `1 / k` point, the boundary between halves → **Fast-and-Slow Pointers**.

3. If nothing above matches, you are probably outside this chapter — try hashing, a stack-based pattern, or a different data structure entirely.

### Decision walkthrough — every pattern, once

Run the tree on a one-line problem for each of the seven patterns to confirm the branches resolve cleanly.

- **"Reverse the entire linked list in place."** → Step 1 yes; single segment → **Reversal**.
- **"Swap every two adjacent nodes."** → Step 1 yes; many fixed-size chunks → **Reversal Subproblem**.
- **"Remove the `k`-th node from the end."** → Step 2 yes; fixed offset from the tail → **Sliding Window Traversal**.
- **"Find the middle node in one pass."** → Step 2 yes; proportional position → **Fast-and-Slow Pointers**.
- **"Split the list into even-indexed and odd-indexed nodes."** → Step 1 yes; one input, `k` outputs → **Split**.
- **"Merge two sorted lists into one."** → Step 1 yes; `k` inputs, one output → **Merge**.
- **"Reorder `L0 → L1 → … → Ln−1` to `L0 → Ln−1 → L1 → Ln−2 → …`."** → Step 1 yes; split then merge over the same nodes → **Reorder**.

So the key idea is: direction-of-rewrite narrows to one of two branches; cardinality and position-vs-structure pick the leaf.

---

## Pattern Comparison Table

All seven patterns at a glance. `n` is the input list length, `k` is a problem-specific constant (chunk size, output bucket count, or offset from the tail).

| Pattern | Problem shape | Key signal | Time | Space | Confused with |
|---|---|---|---|---|---|
| **Reversal** | Flip one contiguous segment of `next` pointers in place | "Reverse the list", "reverse between `left` and `right`", "is this a palindrome" | `O(n)` | `O(1)` | Reversal Subproblem (one segment vs many?), Reorder (single pass vs split-then-merge?) |
| **Reversal Subproblem** | Many segment reversals chained by a boundary walk | "Swap every pair", "reverse in groups of `k`", "reverse increasing chunks", "reverse alternate segments" | `O(n)` | `O(1)` | Reversal (one segment or many?), Reorder (two phases vs many chunks?) |
| **Sliding Window Traversal** | Read a node at a *fixed gap* from another pointer | "`k`-th from end", "last `k`", "trim the `n`-th from end", "rotate by `k`" | `O(n)` | `O(1)` | Fast-and-Slow (gap is fixed or growing?), Reversal (find a node or change the chain?) |
| **Fast-and-Slow Pointers** | Locate a *proportional* position in one pass | "Find the middle", "split in half", "detect a cycle", "palindrome via reverse half" | `O(n)` | `O(1)` | Sliding Window Traversal (fixed offset or proportional position?), Reorder (find a midpoint or rebuild the chain?) |
| **Split** | One input chain to `k` output chains via a per-node classifier | "Split by parity", "partition by predicate", "round-robin distribute", "`k`-way split" | `O(n)` | `O(k)` for dummies | Merge (route out or read in?), Reorder (split alone or split-then-merge?) |
| **Merge** | `k` input chains to one output chain via an `O(1)` selector | "Merge two sorted lists", "alternate fusion", "add two numbers as lists", "`k`-way merge" | `O(n + m)` | `O(1)` splice / `O(max)` allocate | Split (read in or route out?), Reorder (merge alone or split-then-merge?) |
| **Reorder** | Rearrange one chain in place by composing split and merge | "Reorder list", "zig-zag shuffle", "interleave first half with reversed second", "move last to front" | `O(n)` | `O(1)` | Reversal (rebuild order or flip a segment?), Split (split only or split-then-merge?), Merge (merge only or split-then-merge?) |

---

## Common Confusions

Five pairs trip readers up most often. Each subsection states the surface similarity, the one question that cuts through it, and the symptom that betrays a wrong choice.

### Reversal vs Reversal Subproblem

**Why they look the same.** Both rewrite `next` pointers in place using the three-pointer flip from pattern 07. Both run in `O(n)` time and `O(1)` space. The inner loop body is *identical*.

**The distinguishing test.** Does the problem ask for *one* contiguous segment to be reversed, or for *many* segments to be reversed in sequence by an outer driver?

**Telltale symptom of wrong choice.** You wrote a single three-pointer sweep for "reverse the list in groups of `k`." The first chunk reverses correctly; the rest is either untouched or dangling. The missing piece is the outer chunk-boundary loop plus the seam stitch — that is the subproblem pattern's whole job.

### Sliding Window Traversal vs Fast-and-Slow Pointers

**Why they look the same.** Both walk the list once with two forward-moving pointers. Both run in `O(n)` time and `O(1)` space. Both return a *position* rather than a structural change.

**The distinguishing test.** Is the gap between the two pointers *fixed at `k`* throughout the walk, or does the gap *grow proportionally* because the two pointers move at different speeds?

**Telltale symptom of wrong choice.** You used a 2:1 speed ratio to find "the `k`-th node from the end." The slow pointer lands near `n / 2`, not at distance `k` from the tail. Conversely, you chained two pointers `k` apart to find "the middle." The trailer ends up `k` away from the leader, not at `n / 2`. The fix is to switch the gap type: fixed-distance for offset-from-tail, fixed-speed-ratio for proportional-position.

### Split vs Merge

**Why they look the same.** Both use a `dummy` head plus a moving `tail` pointer, both walk in `O(1)` work per node, and both produce linked-list outputs by re-linking input nodes (never copying).

**The distinguishing test.** Does the per-step decision read *one input head* and pick a *bucket*, or does it read *several input heads* and pick a *winner*?

**Telltale symptom of wrong choice.** You called a merge skeleton on a "split by parity" problem. The comparator has only one input head to look at; there is no second list to merge with. Conversely, you tried to split-route for "merge two sorted lists." There is no per-node classifier, only a per-pair selector. Surface signal: split has *one* `current` walker; merge has *one cursor per input*.

### Merge vs Reorder

**Why they look the same.** Both terminate with one output chain stitched onto a dummy head. Reorder's *second pass* is literally a merge — it consumes the two split sub-lists.

**The distinguishing test.** Is the input a set of *already-separate* lists (merge), or is the input a *single* list that you have to split first before merging (reorder)?

**Telltale symptom of wrong choice.** You called the merge skeleton on a single input list. The loop terminates immediately because there is no second input. The missing piece is the split pass that produces the two sub-lists. Reorder is "split, then merge"; merge alone is the second half of that pipeline run on inputs you did not have to build.

### Reorder vs Reversal

**Why they look the same.** Both rebuild one input chain into a new order. Both keep the same node identities and only rewrite `next` pointers. Both run in `O(n)` time and `O(1)` space.

**The distinguishing test.** Is the target order a single *flipped segment* (positional inversion), or is it a *woven* pattern that interleaves nodes from non-adjacent original positions?

**Telltale symptom of wrong choice.** You reached for a single three-pointer reversal sweep on "reorder list to `L0 → Ln−1 → L1 → Ln−2 → …`." The output is the list reversed, not zig-zagged. The signal you missed is the *interleave*. The front half and the reversed back half must be woven, not flipped — that is the split-then-merge pipeline.

---

## Synthesis Problems

Three problems where two or more patterns *seem* to fit. The right call is non-obvious from the statement alone; the wrong call costs you correctness or a complexity tier.

### Problem 1 — Palindrome Checker

**Statement.** Given the head of a singly linked list, return `true` if the values read forwards equal the values read backwards. (See [Palindrome Checker](10-pattern-fast-and-slow-pointers/02-problems/04-palindrome-checker.md).)

**Why multiple patterns seem viable.**

- **Reversal alone** seems viable because "read backwards" suggests flipping the list.
- **Sliding Window Traversal** seems viable because comparing position `i` against position `n − 1 − i` *looks* like a fixed-offset relationship.
- **Fast-and-Slow Pointers** combined with reversal is the actual winner.

**Winner.** Fast-and-Slow Pointers (to locate the midpoint) + Reversal (applied to the back half).

**Why winner wins.** Sliding-window traversal does not fit because the offset `n − 1 − i` is *not constant*. It depends on `i`, so there is no fixed gap to chain. Reversal alone cannot answer the comparison either — flipping the whole list rebuilds the chain but tells you nothing about value matches. The decisive move is to split the list at its proportional midpoint. That is exactly what fast-and-slow does in one pass without measuring length. Once the back half is reversed in place, comparison degenerates to two parallel forward walks. One walker starts from the original head, the other from the reversed back-half head, and the loop runs until the shorter one ends. Total: `O(n)` time, `O(1)` space, with no second traversal to count length.

### Problem 2 — Reorder List

**Statement.** Given `L0 → L1 → … → Ln−1`, rearrange the list in place to `L0 → Ln−1 → L1 → Ln−2 → L2 → Ln−3 → …`. (See [Relocate Node](13-pattern-reorder/02-problems/01-relocate-node.md) for the chapter's worked variant.)

**Why multiple patterns seem viable.**

- **Reversal** seems viable because the back half of the target output is the original back half *reversed*.
- **Merge** seems viable because the output interleaves two sources, and interleaving is exactly what an alternate-fusion merge does.
- **Reorder** is the actual pattern — and it composes the previous two.

**Winner.** Reorder — specifically, fast-and-slow to find the midpoint, reversal on the back half, and an alternate-fusion merge to weave the two halves.

**Why winner wins.** Reversal alone cannot answer this — flipping the whole list gives `Ln−1 → Ln−2 → …`, not the zig-zag. Merge alone cannot answer it either — there are no pre-existing two input lists, so the merge has nothing to merge. The reorder pattern is named after exactly this composition. The split pass uses fast-and-slow to cut the list at the midpoint. The back half is reversed in place (reversal pattern reused as a primitive). The merge pass alternates one node from the front half and one from the reversed back half. Three sub-patterns chained, `O(n)` time and `O(1)` space. The whole pipeline fits the reorder template's "split by `f1`, weave by `f2`" framing: `f1` is "cut at the midpoint and reverse the second half," `f2` is "alternate fusion."

### Problem 3 — Remove the `k`-th Node From the End

**Statement.** Given the head of a singly linked list and an integer `k`, remove the `k`-th node from the end of the list in one pass and return the modified head. (See [Trim N-th Node](09-pattern-sliding-window-traversal/02-problems/02-trim-nth-node.md).)

**Why multiple patterns seem viable.**

- **Fast-and-Slow Pointers** seems viable because the two-pointer skeleton is the same as middle-finding — `slow` and `fast` walking the list together.
- **Two passes** (first measure length, then walk to position `n − k`) seems viable because the math is straightforward.
- **Sliding Window Traversal** is the actual winner.

**Winner.** Sliding Window Traversal — `start` and `end` chained `k` apart, advance together until `end` lands on the tail.

**Why winner wins.** Fast-and-Slow does not fit because the answer is at a *fixed* distance from the tail, not a proportional one. The 2:1 speed ratio lands `slow` at `n / 2`, which is *not* the predecessor of the `k`-th from end. The two-pass solution works but the problem stipulates a single pass, so it loses on the constraint. Sliding-window traversal is purpose-built for "fixed offset from the tail." Prime `end` to be `k` nodes ahead, then advance both `start` and `end` in lockstep until `end.next` is `null`. At that moment `start` is parked on the predecessor of the node to delete. A single splice (`start.next = start.next.next`) finishes the job. Total: `O(n)` time, `O(1)` space, exactly one traversal. <!-- VERIFY: trim-nth-node uses `start`/`end` with the standard gap-of-`k` priming variant. -->

---

## How to Use This Chapter

Treat the decision tree as the first pass and the comparison table as the second pass. If both still point to two viable patterns, walk the Common Confusions for the matching pair and run its distinguishing test on the input. If a synthesis-style problem feels like every pattern fits, that is the signal to slow down. The right pattern is usually the one whose *answer shape* matches the question, not the one whose surface notation matches the input. For singly linked lists specifically, the deciding question is almost always "does the answer change structure, or only point at a node?" Every other choice follows from that.
