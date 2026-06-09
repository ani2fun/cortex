---
title: "Pattern: Two Pointers Reduction"
summary: "When a pair-finding problem looks O(n²), reshape it — usually by sorting — so two converging pointers can solve it in one O(n) pass, the sum itself telling you which pointer to move."
prereqs:
  - 02-linear-structures/01-arrays/04-pattern-two-pointers/01-pattern
---

# Pattern: Two Pointers Reduction

## Why It Exists

You've met two pointers when the array was already set up for them — reverse, palindrome, walk inward. But the classic interview question is sneakier: *given an array, find two numbers that add up to a target.* The obvious answer checks every pair — `O(n²)`.

The plain two-pointer move doesn't obviously apply: the numbers are in random order, so "move inward from both ends" tells you nothing. But change one thing — **sort the array first** — and suddenly the ends mean something: the left end is the smallest value, the right end the largest. Now the *sum* of the two ends tells you exactly which pointer to move. That one-time reshape is the **reduction**: massage the problem until the base two-pointer pass solves it.

## See It Work

On a *sorted* array, walk one pointer from each end and let the running sum steer them. Run it, then **Visualise** the converge.

> ▶ Run it, then click **Visualise** — `left` rises and `right` falls based on whether the sum is under or over the target.

```python run viz=array viz-root=arr
arr = [2, 4, 5, 8, 9]          # sorted ascending
target = 13
left, right = 0, len(arr) - 1
while left < right:
    s = arr[left] + arr[right]
    if s == target:
        print(arr[left], arr[right]); break   # 4 9
    elif s < target:
        left += 1                              # too small → raise the low end
    else:
        right -= 1                             # too big → lower the high end
```

## How It Works

The reduction is two moves: **reshape, then run the base pattern.** For pair-sum, the reshape is a sort; the run is two pointers converging, steered by a simple rule:

- `sum < target` → the pair is too small, and `arr[left]` is the smallest value left, so it can never reach the target with anything smaller — **discard it: `left += 1`**.
- `sum > target` → too big; `arr[right]` is the largest, hopeless with anything bigger — **discard it: `right -= 1`**.
- `sum == target` → found.

```mermaid
flowchart TB
  S["sum = arr[left] + arr[right]"] --> Q{"compare to target"}
  Q -->|"sum &lt; target"| L["too small → left++ (need a bigger value)"]
  Q -->|"sum &gt; target"| R["too big → right-- (need a smaller value)"]
  Q -->|"sum == target"| F["found the pair ✓"]
```

<p align="center"><strong>on a sorted array the sum tells you which wall to move: too small, raise the floor (<code>left++</code>); too big, lower the ceiling (<code>right--</code>).</strong></p>

What makes it correct is the **invariant**: every time you move a pointer, the value you discard could not have been in *any* remaining valid pair — so nothing is missed. Each element is visited at most once, so the scan is **`O(n)` time, `O(1)` space**; the sort makes the whole thing `O(n log n)`. You collapsed `O(n²)` to `O(n log n)` by paying once to sort.

When do you reach for it? Run the recognition checklist: **(1)** order doesn't matter (so sorting is allowed), **(2)** you need *two* items at once, and **(3)** traversing from both ends becomes meaningful once sorted. If sorting unlocks (3), it's almost certainly a reduction.

### Key Takeaway

Reshape a pair-finding problem (usually by sorting) so the two ends carry meaning, then converge: the sum says which pointer to move, turning `O(n²)` into one `O(n)` sweep.

## Trace It

Two Sum on `[2, 4, 5, 8, 9]`, target `13`:

| `left` | `right` | `arr[left] + arr[right]` | vs 13 | move |
|---|---|---|---|---|
| 0 (`2`) | 4 (`9`) | 11 | too small | `left++` |
| 1 (`4`) | 4 (`9`) | 13 | equal | **found `4, 9`** |

Before you read on: when the first step discarded `arr[left] = 2`, why was it safe to never look at `2` again?

Because `9` was already the *largest* value available, and `2 + 9` fell short of `13`. Pairing `2` with anything smaller than `9` only makes the sum smaller — so no pair containing `2` could ever reach `13`. Discarding it loses nothing. That's the invariant doing the pruning, one element per step.

## Your Turn

The reusable shape — return the indices of a pair that sums to the target, or nothing:

```python run viz=array
def two_sum_sorted(arr, target):
    left, right = 0, len(arr) - 1
    while left < right:
        s = arr[left] + arr[right]
        if s == target:
            return (left, right)
        if s < target:
            left += 1
        else:
            right -= 1
    return None

print(two_sum_sorted([2, 4, 5, 8, 9], 13))   # (1, 4) → values 4 and 9
print(two_sum_sorted([1, 3, 6, 10], 100))     # None
```

```java run viz=array
import java.util.Arrays;

public class Main {
  static int[] twoSumSorted(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left < right) {
      int s = arr[left] + arr[right];
      if (s == target) return new int[]{left, right};
      if (s < target) left++;
      else right--;
    }
    return null;
  }
  public static void main(String[] args) {
    System.out.println(Arrays.toString(twoSumSorted(new int[]{2, 4, 5, 8, 9}, 13)));  // [1, 4]
    System.out.println(Arrays.toString(twoSumSorted(new int[]{1, 3, 6, 10}, 100)));   // null
  }
}
```

Now drill the family in this section's **Practice** — start with [Two Sum](/cortex/data-structures-and-algorithms/linear-structures/arrays/pattern-two-pointers-reduction/problems/two-sum), then [Largest Container](/cortex/data-structures-and-algorithms/linear-structures/arrays/pattern-two-pointers-reduction/problems/largest-container).

## Reflect & Connect

Reduction is the bridge from "two pointers as a trick" to "two pointers as a tool you *engineer* a problem toward." The family:

- **Sort-then-converge** — the default: Two Sum, and its cousins where you sort to make the ends meaningful.
- **Greedy reduction without sorting** — sometimes the move rule comes from a different argument. In *Largest Container*, you can't sort (positions matter), but moving the *shorter* wall inward is provably safe — same converging skeleton, a greedy justification.
- **Fix-one-then-reduce** — 3Sum and 4Sum (next section) fix one element and run this reduction on the rest.

The tradeoff worth knowing: Two Sum also has a *hash-table* solution that's `O(n)` time without sorting — but `O(n)` space. The two-pointer reduction is `O(1)` space and keeps the array sorted for free, which is why it wins when the input is already sorted or sorting is cheap.

**Prerequisites:** [Two Pointers](/cortex/data-structures-and-algorithms/linear-structures/arrays/pattern-two-pointers/pattern) and [Measuring Cost](/cortex/data-structures-and-algorithms/foundations/measuring-cost).
**What's next:** two pointers as one step *inside* a bigger algorithm — [Two Pointers Subproblem](/cortex/data-structures-and-algorithms/linear-structures/arrays/pattern-two-pointers-subproblem/pattern).

## Recall

> **Mnemonic:** *Sort so the ends mean something, then converge — sum too small raises `left`, too big lowers `right`.*

| | |
|---|---|
| Reshape | sort (or find a greedy move rule) so both ends carry meaning |
| Move rule | `sum < target` → `left++`; `sum > target` → `right--`; equal → found |
| Cost | `O(n)` scan, `O(1)` space; `O(n log n)` including the sort |
| Invariant | a discarded end can't belong to any remaining valid pair |

<details>
<summary><strong>Q:</strong> What does the "reduction" reshape, and why?</summary>

**A:** It sorts (usually) so the left end is the min and the right the max — making the sum a reliable signal for which pointer to move.

</details>
<details>
<summary><strong>Q:</strong> Given `sum < target`, which pointer moves and why?</summary>

**A:** `left++` — `arr[left]` is the smallest value and can't reach the target even with the current largest, so discard it.

</details>
<details>
<summary><strong>Q:</strong> Total time and space?</summary>

**A:** `O(n log n)` time (the sort dominates the `O(n)` scan), `O(1)` extra space.

</details>
<details>
<summary><strong>Q:</strong> When does the hash-table Two Sum beat this?</summary>

**A:** When the array isn't sorted and `O(n)` extra space is acceptable — it's `O(n)` time without sorting.

</details>

## Sources & Verify

- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §2.1 / §1.4 — sorting plus the two-pointer scan; the `O(n log n)` reduction of pair-finding.
- **cp-algorithms.com**, "Two Pointers Method" — the sorted-array sum technique and the discard-invariant argument.
- The four-question recognition checklist and the Largest-Container greedy variant are this section's framing; both runnable blocks are verified by running, and the invariant follows from the sorted order.
