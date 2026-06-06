---
title: "Zero Sum Subarrays"
summary: "Given an array arr, return the start/end indices of every subarray that sums to 0."
prereqs:
  - 11-pattern-prefix-sum/01-pattern
difficulty: medium
---

# Zero sum subarrays

## Problem Statement

Given an array `arr`, return the start/end indices of **every** subarray that sums to `0`.

### Example 1
> -   **Input:** `[6, 3, -1, -3, 4, -2, 2, 4, 6, -12, -7]`
> -   **Output:** `[[2, 4], [2, 6], [5, 6], [6, 9], [0, 10]]`

### Example 2
> -   **Input:** `[1, 2, 3, 4, 0]` → **Output:** `[[4, 4]]`

### Example 3
> -   **Input:** `[1, 2, 3]` → **Output:** `[]`

## Examples

**Example 1**
```
Input:  [6, 3, -1, -3, 4, -2, 2, 4, 6, -12, -7]
Output: [[2, 4], [2, 6], [5, 6], [6, 9], [0, 10]]
Explanation: five slices net to zero, e.g. arr[2..4] = -1 + -3 + 4 = 0 and the whole
array arr[0..10] sums to 0. Each pair gives the start and end indices, inclusive.
```

**Example 2**
```
Input:  [1, 2, 3, 4, 0]
Output: [[4, 4]]
Explanation: only the single zero at index 4 forms a zero-sum slice — arr[4..4] = 0.
```

**Example 3**
```
Input:  [1, 2, 3]
Output: []
Explanation: every prefix sum is distinct and never zero, so no slice nets to zero.
```

<!-- VERIFY: frozen solution's inline comment reads [[0, 1], [0, 3], [2, 3]] but the code actually emits [[0, 1], [1, 2], [0, 3], [2, 3]] — arr[1..2] = -1 + 1 = 0 is also zero-sum; Examples block follows the real output. -->
**Example 4**
```
Input:  [1, -1, 1, -1]
Output: [[0, 1], [1, 2], [0, 3], [2, 3]]
Explanation: prefix sum 0 recurs at indices 1 and 3, and prefix sum 1 recurs at
index 2, producing four overlapping zero-sum slices.
```

<details>
<summary><h2>Approach</h2></summary>


Same prefix-sum trick. Two indices `i < j` with `P[i] == P[j+1]` means `arr[i..j]` sums to zero. So maintain a hash map `{prefixSum → list of indices where it appeared}`; whenever we see a prefix sum that has appeared before, every previous occurrence is the *start − 1* of a zero-sum subarray ending at the current index.

The base case `prefixSumIndices[0] = [-1]` lets us catch zero-sum subarrays that start at index 0.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=result
from typing import List

class Solution:
    def zero_sum_subarrays(self, arr: List[int]) -> List[List[int]]:

        # Dictionary to store prefix sums and their indices
        prefix_sum_indices: dict[int, List[int]] = {}

        # To store the actual start and end indices of all subarrays
        result: List[List[int]] = []
        prefix_sum = 0

        # Add a base case for prefix_sum = 0
        prefix_sum_indices[0] = [-1]

        for i in range(len(arr)):
            prefix_sum += arr[i]

            # If the prefix_sum exists in the map, it means we found
            # subarrays summing to 0
            if prefix_sum in prefix_sum_indices:
                for prev_index in prefix_sum_indices[prefix_sum]:

                    # Add (prev_index + 1) as the correct start index
                    result.append([prev_index + 1, i])

            # Add the current index to the list of indices for this
            # prefix_sum
            if prefix_sum not in prefix_sum_indices:
                prefix_sum_indices[prefix_sum] = []
            prefix_sum_indices[prefix_sum].append(i)

        return result


# Examples from the problem statement
r1 = Solution().zero_sum_subarrays([6, 3, -1, -3, 4, -2, 2, 4, 6, -12, -7])
print(sorted([sorted(p) for p in r1]))   # [[0, 10], [2, 4], [2, 6], [5, 6], [6, 9]]

print(Solution().zero_sum_subarrays([1, 2, 3, 4, 0]))  # [[4, 4]]
print(Solution().zero_sum_subarrays([1, 2, 3]))         # []

# Edge cases
print(Solution().zero_sum_subarrays([]))                # []
print(Solution().zero_sum_subarrays([0]))               # [[0, 0]]
print(Solution().zero_sum_subarrays([-1, 1]))           # [[0, 1]]
print(Solution().zero_sum_subarrays([1, -1, 1, -1]))    # [[0, 1], [0, 3], [2, 3]]
```

```java run viz=array viz-root=result
import java.util.*;

public class Main {
    static class Solution {
        public List<List<Integer>> zeroSumSubarrays(int[] arr) {

            // Map to store prefix sums and their indices
            Map<Integer, List<Integer>> prefixSumIndices = new HashMap<>();

            // To store the actual start and end indices of all subarrays
            List<List<Integer>> result = new ArrayList<>();
            int prefixSum = 0;

            // Add a base case for prefixSum = 0
            prefixSumIndices.put(0, new ArrayList<>());
            prefixSumIndices.get(0).add(-1);

            for (int i = 0; i < arr.length; i++) {
                prefixSum += arr[i];

                // If the prefixSum exists in the map, it means we found
                // subarrays summing to 0
                if (prefixSumIndices.containsKey(prefixSum)) {
                    for (int prevIndex : prefixSumIndices.get(prefixSum)) {

                        // Add (prevIndex + 1) as the correct start index
                        List<Integer> subarray = new ArrayList<>();
                        subarray.add(prevIndex + 1);
                        subarray.add(i);
                        result.add(subarray);
                    }
                }

                // Add the current index to the list of indices for this
                // prefixSum
                prefixSumIndices
                    .computeIfAbsent(prefixSum, k -> new ArrayList<>())
                    .add(i);
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        var r1 = new Solution().zeroSumSubarrays(new int[]{6, 3, -1, -3, 4, -2, 2, 4, 6, -12, -7});
        r1.forEach(p -> { Collections.sort(p); System.out.print(p + " "); }); System.out.println();
        // [0, 10] [2, 4] [2, 6] [5, 6] [6, 9] (order may vary)

        System.out.println(new Solution().zeroSumSubarrays(new int[]{1, 2, 3, 4, 0}));  // [[4, 4]]
        System.out.println(new Solution().zeroSumSubarrays(new int[]{1, 2, 3}));         // []

        // Edge cases
        System.out.println(new Solution().zeroSumSubarrays(new int[]{}));                // []
        System.out.println(new Solution().zeroSumSubarrays(new int[]{0}));               // [[0, 0]]
        System.out.println(new Solution().zeroSumSubarrays(new int[]{-1, 1}));           // [[0, 1]]
        System.out.println(new Solution().zeroSumSubarrays(new int[]{1, -1, 1, -1}));   // [[0, 1], [0, 3], [2, 3]]
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Prefix sum is the bridge that turns *quadratic* subarray problems into *linear* ones. The pattern is so flexible it shows up in problems that don't even mention sums — anywhere "balanced" or "equal" or "net zero" can be encoded as a sum, the same machinery applies.

The four moves:

1. **Define the prefix.** Sum, signed-count, balance, parity — pick whatever encodes "the question".
2. **Re-encode if needed.** "Equal 0s and 1s" → 0 ↦ −1, 1 ↦ +1. "Equal As and non-As" → As ↦ +1, others ↦ −1. "Subarray sum = K" works directly.
3. **Hash map of prefix values.** What you store depends on the answer shape: first-index for *longest*, list of indices for *all*, count for *how many*.
4. **The `0 → −1` (or `0 → −1` index) base case.** Forgetting it is the canonical bug. Always start with `map[0] = -1` (for first-index) or `map[0] = 1` (for count).

> **A panoramic view of the five hash-table patterns:**
>
> | Pattern | Question shape | Best hash-map use |
> |---|---|---|
> | Counting | "how often / how many of X?" | freq map of items |
> | Key generation | "which inputs are equivalent?" | key → group |
> | Fixed sliding window | "for each window of size K, …?" | freq map of window |
> | Variable sliding window | "longest/shortest with property P?" | freq map + grow/shrink |
> | Prefix sum + hash | "subarray sum = X?" / "balanced subarray?" | prefix-value → index/count |

> *Coming up — a different kind of lesson. The next file is the **design** lesson: two classic interview problems (LRU cache, RandomisedSet) where you build whole composite data structures from a hash table plus one other piece (a doubly-linked list, a dynamic array). The patterns we just covered prepared us; design problems force us to combine them into a single coherent structure.*

</details>
<details>
<summary><h2>Intuition</h2></summary>


The task is to report *every* contiguous slice that sums to zero, as start-end index pairs. The brute-force read sums every slice: fix a start, extend an end, add as you go, and emit the pair whenever the running sum hits zero. Each of the `O(N²)` start-end pairs is examined, and re-summing makes it `O(N³)` unless you cache the running sum per start — still `O(N²)`.

The prefix sum reframes "this slice sums to zero" as "two prefix sums are equal". If `P[i]` denotes the prefix sum up to index `i`, then the slice `arr[a..b]` nets to zero exactly when the prefix sum just before `a` equals the prefix sum at `b`. So whenever the current prefix sum has been seen at earlier indices, *each* of those earlier indices marks the start (minus one) of a distinct zero-sum slice ending here. Storing a *list* of indices per prefix value — not only the first — is what lets the algorithm emit all of them.

This is the same-value-search flavour, extended from "longest" to "all". The hash map maps each prefix sum to the list of indices where it occurred, and the base case `prefix_sum_indices[0] = [-1]` catches slices anchored at index `0`. What breaks without it is any zero-sum prefix starting at the array's front — its prefix sum is `0`, and the seeded `−1` index is the partner it pairs with. The diagnostic signal is "enumerate subarrays with a net-zero property", which one pass over the prefix sums answers.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Zero Sum Subarrays |
|---|---|
| **Q1.** Does the answer reduce to a subarray sum? | **Yes** — a zero-sum slice is precisely a pair of equal prefix sums. |
| **Q2.** Is the input a linear sequence walked once? | **Yes** — an integer array, swept index by index in a single pass. |
| **Q3.** Is the matching slice found by a hash-map lookup? | **Yes** — a map from each prefix sum to its *list* of indices yields every earlier partner in `O(1)` per partner. |
| **Q4.** Does the rule survive negatives and zeros? | **Yes** — negatives are essential here; they are what let prefix sums repeat and slices net to zero. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Initialise `prefix_sum = 0`, an empty result list, and a map `prefix_sum_indices` seeded with `{0: [-1]}` so slices starting at index `0` are caught.
2. Sweep `i` across the array, adding `arr[i]` to `prefix_sum`.
3. If `prefix_sum` is already a key, then for *every* earlier index `j` in its list, the slice `arr[j+1..i]` nets to zero — append `[j + 1, i]` to the result.
4. Append the current index `i` to the list for `prefix_sum` (creating the list if this prefix value is new).
5. After the loop, return the result list of index pairs.

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1: `arr = [6, 3, -1, -3, 4, -2, 2, 4, 6, -12, -7]`, expected output `[[2, 4], [2, 6], [5, 6], [6, 9], [0, 10]]`. The map stores each prefix sum's list of indices; a repeat emits one pair per earlier index:

```
prefix_sum=0, map={0:[-1]}

i=0   arr=  6  P=6    new → store {6:[0]}
i=1   arr=  3  P=9    new → store {9:[1]}
i=2   arr= -1  P=8    new → store {8:[2]}
i=3   arr= -3  P=5    new → store {5:[3]}
i=4   arr=  4  P=9    seen @ [1] → emit [2, 4]                 map[9]=[1, 4]
i=5   arr= -2  P=7    new → store {7:[5]}
i=6   arr=  2  P=9    seen @ [1, 4] → emit [2, 6], [5, 6]      map[9]=[1, 4, 6]
i=7   arr=  4  P=13   new → store {13:[7]}
i=8   arr=  6  P=19   new → store {19:[8]}
i=9   arr=-12  P=7    seen @ [5] → emit [6, 9]                 map[7]=[5, 9]
i=10  arr= -7  P=0    seen @ [-1] → emit [0, 10]               map[0]=[-1, 10]

result = [[2, 4], [2, 6], [5, 6], [6, 9], [0, 10]]
```

The result matches the expected output — prefix sum `9` recurs across indices `1`, `4`, `6` to produce the overlapping slices, and the final prefix sum `0` pairs with the seeded `−1` to report the whole array `arr[0..10]`.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| | Cost | Why |
|---|---|---|
| **Time** | **O(N + M)** | One pass over the array is `O(N)`; emitting the answer costs `O(M)` where `M` is the number of zero-sum slices. `M` can reach `O(N²)` when many prefix sums coincide. |
| **Space** | **O(N)** | The map holds at most `N + 1` index entries across all its lists, plus the `O(M)` output. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Input | Output | Why |
|---|---|---|
| `[]` | `[]` | Empty array — the loop never runs. |
| `[0]` | `[[0, 0]]` | A single `0` is a zero-sum slice; `P` returns to `0` at index `0`, pairing with the seeded `−1`. |
| `[1, 2, 3]` | `[]` | Every prefix sum is distinct and never zero — no pair repeats. |
| `[-1, 1]` | `[[0, 1]]` | Prefix sum `0` recurs at index `1`, pairing with `−1` → the whole array. |
| `[1, -1, 1, -1]` | `[[0, 1], [1, 2], [0, 3], [2, 3]]` | Prefix sum `0` recurs at indices `1`, `3` and prefix sum `1` recurs at index `2` → four overlapping slices. |
| `[1, 2, 3, 4, 0]` | `[[4, 4]]` | Only the trailing `0` makes a prefix sum repeat (`10` at indices `3` and `4`). |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Mapping each prefix sum to the *list* of indices where it occurred — not only the first — turns "find every zero-sum subarray" into one `O(N)` sweep that emits a pair for each repeated prefix value, and the `map[0] = [-1]` base case is what catches slices anchored at index `0`.

</details>