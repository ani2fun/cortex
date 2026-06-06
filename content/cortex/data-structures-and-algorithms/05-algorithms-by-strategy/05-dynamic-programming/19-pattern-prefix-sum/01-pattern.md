---
title: "Pattern: Prefix Sum (2D)"
summary: "Precompute cumulative sums once so any range or rectangle sum is O(1). In 1-D, range[l,r] = prefix[r+1] - prefix[l]; in 2-D, a rectangle sum is a four-corner inclusion-exclusion. Pair it with a hashmap and you count subarrays summing to K in one pass."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/01-linear-dp
---

## Why It Exists

You're asked the sum of a subarray — or a submatrix — over and over, for different ranges. Re-adding the elements each time is `O(n)` (or `O(m·n)`) per query; do it `q` times and you've paid `O(q·n)`. **Prefix sums** trade a one-time precompute for `O(1)` queries forever after: store cumulative totals, then every range answer is a subtraction.

In 1-D, `prefix[i]` = sum of the first `i` elements, and `sum(l..r) = prefix[r+1] − prefix[l]`. In 2-D, `P[i][j]` = sum of the rectangle from the origin to `(i-1, j-1)`, and any rectangle sum is a **four-corner inclusion-exclusion**. The same precompute, paired with a hashmap of prefix counts, also answers "how many subarrays sum to exactly `K`?" in one pass — the technique you met in the [hash-table prefix-sum pattern](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-prefix-sum-pattern), now generalised to ranges and rectangles.

## See It Work

**Range Sum Query 2D** ([LeetCode 304](https://leetcode.com/problems/range-sum-query-2d-immutable/)) — build a prefix matrix once (`O(m·n)`), then answer any rectangle sum in `O(1)`. The `+1` padding row and column let every cell read its neighbours without boundary checks.

```python run viz=grid
def build_2d(matrix):
    m, n = len(matrix), len(matrix[0])
    P = [[0] * (n + 1) for _ in range(m + 1)]            # padded so row 0 / col 0 are zero
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            P[i][j] = matrix[i-1][j-1] + P[i-1][j] + P[i][j-1] - P[i-1][j-1]   # add top & left, remove overlap
    return P

def sum_region(P, r1, c1, r2, c2):
    return P[r2+1][c2+1] - P[r1][c2+1] - P[r2+1][c1] + P[r1][c1]   # four-corner inclusion-exclusion

M = [[3, 0, 1, 4, 2], [5, 6, 3, 2, 1], [1, 2, 0, 1, 5], [4, 1, 0, 1, 7], [1, 0, 3, 0, 5]]
P = build_2d(M)
print(sum_region(P, 2, 1, 4, 3))   # 8
print(sum_region(P, 1, 1, 2, 2))   # 11
```

```java run viz=grid
public class Main {
    static int[][] build2D(int[][] matrix) {
        int m = matrix.length, n = matrix[0].length;
        int[][] P = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                P[i][j] = matrix[i-1][j-1] + P[i-1][j] + P[i][j-1] - P[i-1][j-1];
        return P;
    }
    static int sumRegion(int[][] P, int r1, int c1, int r2, int c2) {
        return P[r2+1][c2+1] - P[r1][c2+1] - P[r2+1][c1] + P[r1][c1];
    }
    public static void main(String[] args) {
        int[][] M = {{3,0,1,4,2}, {5,6,3,2,1}, {1,2,0,1,5}, {4,1,0,1,7}, {1,0,3,0,5}};
        int[][] P = build2D(M);
        System.out.println(sumRegion(P, 2, 1, 4, 3));   // 8
        System.out.println(sumRegion(P, 1, 1, 2, 2));   // 11
    }
}
```

Both print `8` then `11`. After the one-time build, each rectangle sum is four array reads — independent of the rectangle's size. Build `O(m·n)`, query `O(1)`.

## How It Works

Both build and query are **inclusion-exclusion**: a cell's cumulative sum is its value plus the block above plus the block to the left, minus the top-left block that those two share.

```d2
direction: down
query: "rectangle sum (r1,c1)..(r2,c2)\n= P[r2+1][c2+1]   (whole block to bottom-right corner)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
sub1: "- P[r1][c2+1]   (strip above the rectangle)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
sub2: "- P[r2+1][c1]   (strip left of the rectangle)" {style.fill: "#fbcfe8"; style.stroke: "#db2777"}
add: "+ P[r1][c1]   (top-left block subtracted TWICE -> add it back)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
query -> sub1
query -> sub2
sub1 -> add
sub2 -> add
```

<p align="center"><strong>The four-corner formula: take the big block to the bottom-right corner, subtract the strip above and the strip to the left — then <em>add back</em> the top-left block, because the two strips both removed it.</strong></p>

Three ideas carry the pattern:

- **Precompute once, query `O(1)` forever.** The whole point is amortisation: pay `O(n)` (1-D) or `O(m·n)` (2-D) up front so each of many range queries costs constant time. If you query a fixed array once, prefix sums don't help; for repeated queries they're transformative.
- **Padding kills the edge cases.** A leading `0` (1-D) or a zero row + column (2-D) means `prefix[l]` and `P[r1][·]` are always valid — no "is this the first element?" branch. The padded index is why the formulas have those `+1`s.
- **The hashmap variant counts subarrays.** "How many subarrays sum to `K`?" rewrites as "for each prefix `p`, how many earlier prefixes equal `p − K`?" A running hashmap of prefix counts, seeded `{0: 1}` (the empty prefix), answers it in one pass — the 1-D prefix sum fused with counting. ([Your Turn](#your-turn).)

> **Key takeaway.** Prefix sums precompute cumulative totals so range queries are subtractions: 1-D `sum(l..r) = prefix[r+1] − prefix[l]`; 2-D rectangle sum = `P[r2+1][c2+1] − P[r1][c2+1] − P[r2+1][c1] + P[r1][c1]` (four-corner inclusion-exclusion). Padding removes boundary cases; a `{0:1}`-seeded hashmap of prefixes counts subarrays summing to `K`. Build `O(n)`/`O(mn)`, query `O(1)`.

## Trace It

The 2-D query has *four* terms, and the easy mistake is to write three — subtract the top strip and the left strip and stop. But those two strips overlap, and the overlap is the top-left block.

**Predict before you run:** you compute a rectangle sum as `P[r2+1][c2+1] − P[r1][c2+1] − P[r2+1][c1]` and forget the final `+ P[r1][c1]`. Is the result too big or too small — and by exactly how much?

```python run viz=grid
def build_2d(matrix):
    m, n = len(matrix), len(matrix[0])
    P = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            P[i][j] = matrix[i-1][j-1] + P[i-1][j] + P[i][j-1] - P[i-1][j-1]
    return P

def sum_region(P, r1, c1, r2, c2):                       # correct: four terms
    return P[r2+1][c2+1] - P[r1][c2+1] - P[r2+1][c1] + P[r1][c1]

def sum_region_buggy(P, r1, c1, r2, c2):                 # bug: missing + P[r1][c1]
    return P[r2+1][c2+1] - P[r1][c2+1] - P[r2+1][c1]

M = [[3, 0, 1, 4, 2], [5, 6, 3, 2, 1], [1, 2, 0, 1, 5], [4, 1, 0, 1, 7], [1, 0, 3, 0, 5]]
P = build_2d(M)
print("correct:", sum_region(P, 2, 1, 4, 3))
print("buggy:  ", sum_region_buggy(P, 2, 1, 4, 3))
print("missing term P[2][1] =", P[2][1])
```

<details>
<summary><strong>Reveal</strong></summary>

Correct is `8`; the buggy three-term version returns `0` — too small by exactly `P[2][1] = 8`. Here's the geometry: `P[r2+1][c2+1]` is the entire block from the origin to the rectangle's bottom-right corner. Subtracting `P[r1][c2+1]` removes everything *above* the rectangle, and subtracting `P[r2+1][c1]` removes everything *to its left*. But the top-left block (origin to `(r1-1, c1-1)`) lies in *both* of those regions, so it gets subtracted **twice**. Adding `P[r1][c1]` back puts it in exactly once — standard inclusion-exclusion. Drop the add-back and you undercount by precisely that corner block; in this case `P[2][1]` happens to equal the answer, so the bug zeroes it out. The lesson generalises: any time you combine overlapping cumulative regions, the overlaps must be added back, and forgetting a single one silently corrupts every query.

</details>

## Your Turn

**Subarray Sum Equals K** ([LeetCode 560](https://leetcode.com/problems/subarray-sum-equals-k/)) — count the contiguous subarrays summing to exactly `K`. The 1-D prefix sum fused with a hashmap: a subarray `(l..r)` sums to `K` iff `prefix[r] − prefix[l-1] = K`, i.e. `prefix[l-1] = prefix[r] − K`. So as you sweep, count how many earlier prefixes equal `currentPrefix − K`.

```python run viz=array
from collections import defaultdict

def subarray_sum(nums, k):
    seen = defaultdict(int)
    seen[0] = 1                                          # the empty prefix (sum 0) — the seed
    prefix = count = 0
    for x in nums:
        prefix += x
        count += seen[prefix - k]                        # earlier prefixes that close a sum-k subarray
        seen[prefix] += 1
    return count

print(subarray_sum([1, 1, 1], 2))   # 2
print(subarray_sum([1, 2, 3], 3))   # 2
```

```java run viz=array
import java.util.*;
public class Main {
    static int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> seen = new HashMap<>();
        seen.put(0, 1);                                  // the seed
        int prefix = 0, count = 0;
        for (int x : nums) {
            prefix += x;
            count += seen.getOrDefault(prefix - k, 0);
            seen.merge(prefix, 1, Integer::sum);
        }
        return count;
    }
    public static void main(String[] args) {
        System.out.println(subarraySum(new int[]{1, 1, 1}, 2));   // 2
        System.out.println(subarraySum(new int[]{1, 2, 3}, 3));   // 2
    }
}
```

Both print `2`. In `[1,1,1]` with `K=2`, the subarrays `[0,1]` and `[1,2]` qualify; in `[1,2,3]` with `K=3`, it's `[1,2]` and `[2,3]`... wait — `[3]` and `[1,2]`. The `{0: 1}` seed is what lets a subarray that *starts at index 0* count (its closing prefix equals `K`, and `prefix − K = 0` is already in the map). This is the prefix-sum idea at its most powerful — `O(n)` time, one pass, no nested loop — and it's the [hash-table prefix-sum pattern](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-prefix-sum-pattern) you've seen, viewed through the range lens.

## Reflect & Connect

- **Precompute, then subtract.** The whole pattern is "cumulative totals make range queries `O(1)`." 1-D is one subtraction; 2-D is four-corner inclusion-exclusion. Worth it only when you query repeatedly.
- **Inclusion-exclusion is the 2-D crux.** Overlapping cumulative regions double-count their intersection; add it back. Four terms, not three — the most common 2-D prefix bug (it silently undercounts).
- **Padding removes branches.** A leading zero (1-D) or zero row/column (2-D) keeps every index valid, which is why the formulas carry `+1` offsets.
- **The hashmap fusion answers "count subarrays summing to K".** Seed `{0: 1}`, track prefix counts, look up `prefix − K`. The seed is essential (it counts subarrays anchored at the start) — the same load-bearing base case as [subset sum](/cortex/data-structures-and-algorithms/algorithms-by-strategy-dynamic-programming-pattern-subset-sum-pattern)'s `dp[0]`.
- **When the array changes, escalate.** Prefix sums assume the data is *static* — one update invalidates `O(n)` of the prefix array. For mutable ranges, use a [Fenwick (BIT)](/cortex/data-structures-and-algorithms/trees-fenwick-tree-introduction-to-fenwick-trees) or [segment tree](/cortex/data-structures-and-algorithms/trees-segment-tree-introduction-to-segment-trees) — `O(log n)` query *and* update.

## Recall

<details>
<summary><strong>Q:</strong> What is the 1-D prefix-sum range formula, and what does it cost?</summary>

**A:** `prefix[i]` = sum of the first `i` elements; `sum(l..r) = prefix[r+1] − prefix[l]`. Build `O(n)` once, then each range query is `O(1)`.

</details>
<details>
<summary><strong>Q:</strong> What is the 2-D rectangle-sum formula?</summary>

**A:** `P[r2+1][c2+1] − P[r1][c2+1] − P[r2+1][c1] + P[r1][c1]` — the big corner block, minus the strips above and to the left, plus the top-left block back (it was subtracted twice). Build `O(m·n)`, query `O(1)`.

</details>
<details>
<summary><strong>Q:</strong> Why does forgetting the <code>+ P[r1][c1]</code> term undercount?</summary>

**A:** The top strip and the left strip both contain the top-left block, so subtracting both removes that block twice. Without the add-back you undercount by exactly that corner's cumulative sum.

</details>
<details>
<summary><strong>Q:</strong> How does prefix sum count subarrays summing to <code>K</code>?</summary>

**A:** A subarray sums to `K` iff `prefix[r] − prefix[l-1] = K`. Sweep once, and for each prefix add the count of earlier prefixes equal to `prefix − K` (a hashmap seeded `{0: 1}` for the empty prefix). `O(n)`, one pass.

</details>
<details>
<summary><strong>Q:</strong> When should you NOT use a prefix-sum array?</summary>

**A:** When the underlying data is mutable — a single element update invalidates `O(n)` of the prefix array. Use a Fenwick tree or segment tree for `O(log n)` query-and-update instead, or when you only query once (no amortisation benefit).

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed. — cumulative sums and the inclusion-exclusion principle underlying range/rectangle queries.
- **LeetCode** 303 (Range Sum Query Immutable), 304 (Range Sum Query 2D), 560 (Subarray Sum Equals K) are the canonical drills; the `8`/`11` rectangle sums, the correct-vs-buggy `8`/`0` (missing add-back), and the `2`/`2` subarray counts above all come from the runnable blocks — re-run to verify.
