---
title: "Pattern Synthesis: Arrays"
summary: "When to use which pattern, common confusions, and synthesis problems for arrays."
prereqs:
  - 02-linear-structures/01-arrays/11-pattern-maximum-overlap/03-memorize
---

# Pattern Synthesis: Arrays

Every array pattern in this chapter solves a specific *shape* of problem. The hard part is not learning each pattern in isolation — it is choosing the right one when a fresh problem lands on your desk. This synthesis chapter exists for exactly that moment: read the problem, run the decision tree, confirm with the comparison table, then write code.

The eight patterns in scope:

- **Two Pointers** — converging from both ends of a single sorted-or-symmetric array.
- **Two Pointers Reduction** — a transform (usually sorting) first, then converge.
- **Two Pointers Subproblem** — outer driver fixes one or two elements, inner two-pointer solves the rest.
- **Simultaneous Traversal** — two arrays, two pointers, one comparison per step.
- **Fixed Sliding Window** — slide a window of known size `k`, aggregate updated in `O(1)`.
- **Variable Sliding Window** — window grows or shrinks to satisfy a monotonic condition.
- **Interval Merging** — sort by `start`, sweep, extend-or-append overlapping ranges.
- **Maximum Overlap** — split each interval into `+1`/`−1` events, sweep, track the peak counter.

---

## When to Use Which Pattern

The first question is always the input shape, because that eliminates entire branches of the tree. The second question is the *answer* shape — a pair, a subarray, a scalar count, a list of intervals. From those two answers, the right pattern usually falls out.

### The decision tree (prose form)

Walk top to bottom; stop at the first match.

1. **Is the input a set of `[start, end]` intervals on a 1-D axis?**
   - If the question is "what does the union look like?" or "do any conflict?" → **Interval Merging**.
   - If the question is "how many are active at the peak moment?" or "minimum resources / rooms needed?" → **Maximum Overlap**.
   - If neither — e.g. you need a specific scheduling order — leave this chapter for a greedy pattern.

2. **Does the input hand you two separate sequences that must be processed together?**
   - "Merge", "intersection", "is `s` a subsequence of `t`?" → **Simultaneous Traversal**.

3. **Is the answer a contiguous subarray (longest / shortest / count / aggregate) of a single array?**
   - Window size is *given* as `k`, and you need the aggregate over every window of that size → **Fixed Sliding Window**.
   - Window size is *not given* — the answer is whichever subarray satisfies a monotonic condition → **Variable Sliding Window**.

4. **Does the answer pick a specific pair / triplet / quadruplet of values inside one array?**
   - The array is already sorted (or palindromic), and you read `arr[left]` and `arr[right]` directly → **Two Pointers** (direct).
   - The array is unsorted but order does not matter — sort first, then converge → **Two Pointers Reduction**.
   - The answer needs `k ≥ 3` elements, or the problem decomposes into a sequence of segment operations → **Two Pointers Subproblem**.

5. If nothing above matches, you are probably outside this chapter — try hashing, prefix sums, monotonic stacks, or a different data structure entirely.

### Decision walkthrough — every pattern, once

Run the tree on a one-line problem for each of the eight patterns to confirm the branches resolve cleanly.

- **"Reverse an array in place."** → Step 1 no, step 2 no, step 3 no, step 4 yes; array is symmetric; direct → **Two Pointers**.
- **"Find two numbers in an unsorted array summing to `target`."** → Step 4 yes; array unsorted; sort first → **Two Pointers Reduction**.
- **"Find all unique triplets summing to zero."** → Step 4 yes; `k = 3`; fix one element, two-pointer the rest → **Two Pointers Subproblem**.
- **"Merge two sorted arrays into one."** → Step 2 yes; two sequences, lock-step → **Simultaneous Traversal**.
- **"Maximum average of any contiguous subarray of size `k`."** → Step 3 yes; size is given as `k` → **Fixed Sliding Window**.
- **"Longest subarray with sum ≤ `K`."** → Step 3 yes; size is not given, monotonic condition → **Variable Sliding Window**.
- **"Merge all overlapping intervals."** → Step 1 yes; question is about the union → **Interval Merging**.
- **"Minimum meeting rooms to host every meeting."** → Step 1 yes; question is peak concurrency → **Maximum Overlap**.

So the key idea is: input shape narrows to one or two patterns; the answer shape picks between them.

---

## Pattern Comparison Table

All eight patterns at a glance. `n` is the input array length; `N` and `M` are the lengths of two input arrays in simultaneous traversal; `k` is a problem-specific constant.

| Pattern | Problem shape | Key signal | Time | Space | Confused with |
|---|---|---|---|---|---|
| **Two Pointers** | Process pairs from opposite ends of one sorted-or-symmetric array | "Reverse in place", "is it a palindrome", "find a pair on a sorted array" | `O(n)` | `O(1)` | Two Pointers Reduction (skipped sort?), Variable Sliding Window (window vs pair?) |
| **Two Pointers Reduction** | Find a pair on an unsorted array where order does not matter | "Two Sum on unsorted input", "largest container", "closest pair to target" | `O(n log n)` if sort needed, else `O(n)` | `O(1)` | Two Pointers (already sorted?), Two Pointers Subproblem (need `k ≥ 3`?) |
| **Two Pointers Subproblem** | Find all `k`-tuples on an array, or transform via segment operations | "All triplets summing to zero", "k-Sum for `k ≥ 3`", "rotate array by `k`" | `O(n^(k − 1))` for k-Sum; `O(n)` for sequence-style | `O(1)` working + `O(k)` output | Two Pointers Reduction (only one outer level?) |
| **Simultaneous Traversal** | Two separate sequences processed in lock-step | "Merge", "intersection", "is `s` a subsequence of `t`?" | `O(N + M)` | `O(1)` working | Two Pointers (one array or two?) |
| **Fixed Sliding Window** | Aggregate over every contiguous subarray of size `k` | "Maximum sum / average of any subarray of size `k`", "count windows satisfying X" | `O(n)` | `O(1)` | Variable Sliding Window (is `k` given?) |
| **Variable Sliding Window** | Longest / shortest / count of subarrays satisfying a monotonic condition | "Longest subarray with at most `K` distinct", "shortest subarray summing to `≥ S`" | `O(n)` | `O(1)` working | Fixed Sliding Window (is the size fixed?), Two Pointers (pair vs subarray?) |
| **Interval Merging** | Union or conflict detection across `[start, end]` intervals | "Merge overlapping meetings", "do any conflict?", "free-time gaps" | `O(n log n)` | `O(n)` | Maximum Overlap (output is intervals or a count?) |
| **Maximum Overlap** | Peak concurrency across `[start, end]` intervals | "Minimum rooms", "peak concurrent users", "maximum load at any moment" | `O(n log n)` | `O(n)` | Interval Merging (output is a count or intervals?) |

---

## Common Confusions

Four pairs trip readers up most often. Each subsection states the surface similarity, the one question that cuts through it, and the symptom that betrays the wrong choice.

### Two Pointers (direct) vs Two Pointers Reduction

**Why they look the same.** Both use the same skeleton — `left = 0`, `right = n − 1`, converge until they meet — and both run in `O(n)` once the loop starts.

**The distinguishing test.** Is the input array *already* sorted (or palindromically symmetric for the operation you are performing)?

**Telltale symptom of wrong choice.** You move a pointer expecting the running sum to decrease, but it bounces randomly — the array was not sorted, so the move was not decisive. Sort first; that is the reduction step.

### Two Pointers Reduction vs Two Pointers Subproblem

**Why they look the same.** Both sort, both run a converging two-pointer sweep, both target value-based answers.

**The distinguishing test.** Does the answer need exactly *two* elements, or `k ≥ 3` elements (or a segment-level decomposition)?

**Telltale symptom of wrong choice.** You wrote one sort plus one two-pointer pass for a Three Sum problem, and now you cannot enumerate all triplets without a third index — that third index is the missing outer loop of the subproblem pattern.

### Two Pointers vs Simultaneous Traversal

**Why they look the same.** Both use two index variables walking through array data.

**The distinguishing test.** Do the two indices belong to the *same* sequence or to *two different* sequences?

**Telltale symptom of wrong choice.** You forced `left` and `right` onto two separate arrays, they happened to converge correctly on a symmetric example, then returned the wrong answer on every asymmetric input. One sequence wants two pointers; two sequences want simultaneous traversal.

### Fixed Sliding Window vs Variable Sliding Window

**Why they look the same.** Both maintain a `[start, end]` window over a single array and update an aggregate as elements enter and leave.

**The distinguishing test.** Is the window size given as a constant `k` in the input, or is it determined by a condition the window must satisfy?

**Telltale symptom of wrong choice.** You started writing an inner `while` to shrink the window past size `k` — fixed never shrinks past `k`; that inner loop only belongs to variable. Conversely, you guarded `if end − start + 1 > k` for an unknown `k` — variable does not pin a size.

### Interval Merging vs Maximum Overlap

**Why they look the same.** Both take `[start, end]` intervals as input and both sort before sweeping.

**The distinguishing test.** Does the problem ask for a *list of intervals* back, or a *scalar count* of how many are active at the busiest moment?

**Telltale symptom of wrong choice.** You returned `3` merged blocks when the problem asked "what is the maximum number of meetings happening at once?" — the answer to that question is `5`, the peak counter, not the block count. Or you returned a single integer when the problem expected a list of merged ranges back.

### Variable Sliding Window vs Two Pointers Reduction

**Why they look the same.** Both walk two indices that move forward, and both rely on a monotonic property of the input.

**The distinguishing test.** Is the answer a *contiguous subarray* (`[start..end]` slice) or a *pair of values* picked from two non-adjacent indices?

**Telltale symptom of wrong choice.** You kept growing and shrinking the window for a "find two values summing to target" problem and the pointers never crossed — the answer is a pair, not a subarray, so the loop has no natural exit.

---

## Synthesis Problems

Three problems where two or more patterns *seem* to fit. The right call is non-obvious from the statement alone; the wrong call costs you correctness or a complexity tier.

### Problem 1 — Largest Container

**Statement.** Given an array `heights` where `heights[i]` is the height of a vertical wall at index `i`, find two walls that with the x-axis form a container holding the maximum water area, `min(heights[i], heights[j]) × (j − i)`. (See [Largest Container](05-pattern-two-pointers-reduction/02-problems/04-largest-container.md).)

**Why multiple patterns seem viable.**

- **Variable Sliding Window** seems viable because the answer involves a span between two indices and you might imagine "growing" or "shrinking" that span.
- **Two Pointers Reduction with sort** seems viable because Two Sum-style problems on unsorted arrays usually start with a sort, and this asks for a pair.
- **Two Pointers (greedy, no sort)** is the actual winner.

**Winner.** Two Pointers Reduction — *greedy* variant, no sort.

**Why winner wins.** Sorting destroys index information, but the area formula uses `j − i` — original indices matter. So a sort-then-converge plan is structurally wrong. A sliding window does not fit either: the answer is a pair `(i, j)`, not a contiguous slice, and shrinking the window from one side does not improve the aggregate monotonically. The decisive move comes from a *greedy invariant* on the original array: at each step, the shorter wall caps the height, so moving the shorter pointer inward is the only move that can ever increase the area. That converts the problem to a converging two-pointer sweep in `O(n)` time and `O(1)` space, with the reduction supplied by the greedy argument rather than by a sort.

### Problem 2 — Minimum Number of Intervals to Remove

**Statement.** Given a list of intervals, return the minimum number to remove so the rest are non-overlapping. (See [Remove Intervals](11-pattern-maximum-overlap/02-problems/02-remove-intervals.md).)

**Why multiple patterns seem viable.**

- **Interval Merging** seems viable because removing intervals to eliminate overlap feels like the inverse of merging overlapping ones.
- **Maximum Overlap** seems viable because "how many overlap at once" sounds like exactly the peak-counter pattern.
- The file lives under the Maximum Overlap pattern in this chapter — that is the answer.

**Winner.** Maximum Overlap.

**Why winner wins.** Interval merging would tell you which spans cover which ranges, but the answer here is a *count* of intervals to delete, not a list of merged blocks. Maximum Overlap goes one level deeper: at each peak of the running counter, every interval contributing to that peak — except one — must be removed for the remaining set to fit a single timeline. The peak counter directly bounds the deletions, while merged output throws that information away. The sort plus event sweep is `O(n log n)` time and `O(n)` space.

### Problem 3 — Longest Subarray Whose Sum is at Most `K`

**Statement.** Given a non-negative integer array `arr` and an integer `K`, return the length of the longest contiguous subarray whose sum is at most `K`.

**Why multiple patterns seem viable.**

- **Two Pointers Reduction** seems viable because a sorted array plus a "sum at most `K`" predicate is a classic Two Sum cousin.
- **Fixed Sliding Window** seems viable because you might fix a guess `k`, check whether *some* size-`k` window satisfies the bound, and binary-search the size.
- **Variable Sliding Window** is the natural fit.

**Winner.** Variable Sliding Window.

**Why winner wins.** Sorting is forbidden because contiguity in the original array order is the entire requirement — sorted input has no "subarray" any more. Fixed sliding window does not fit because `k` is the *unknown* the problem solves for, not an input. Variable sliding window plugs in directly: expand `end` to grow the sum, shrink `start` whenever the sum exceeds `K`, and record the largest valid `end − start + 1` along the way. Non-negative entries give the required monotonic property — adding an element can only grow the sum, removing one can only shrink it — so the contraction step is decisive. Runs in `O(n)` time and `O(1)` working space.

---

## How to Use This Chapter

Treat the decision tree as the first pass and the comparison table as the second pass. If both still point to two viable patterns, walk the Common Confusions for the matching pair and run its distinguishing test on the input. If a synthesis-style problem feels like every pattern fits, that is the signal to slow down — the right pattern is usually the one whose *answer shape* matches the question, not the one whose surface notation matches the input.
