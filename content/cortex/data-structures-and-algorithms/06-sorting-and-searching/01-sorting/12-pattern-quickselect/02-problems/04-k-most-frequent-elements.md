---
title: "K Most Frequent Elements"
summary: "Given an array arr and a positive integer k, return the k most frequent elements (in any order)."
prereqs:
  - 12-pattern-quickselect/01-pattern
difficulty: medium
---

# K Most Frequent Elements

The final pattern — quickselect on a *derived* array. Build a frequency map, extract unique elements, quickselect by frequency.

---

## The Problem

Given an array `arr` and a positive integer `k`, return the `k` most frequent elements (in any order).

```
Input:  arr = [1, 2, 2, 3, 3, 3], k = 2
Output: [2, 3]      (3 appears 3 times, 2 appears 2 times)

Input:  arr = [1, 5, 6, 6], k = 1
Output: [6]

Input:  arr = [1], k = 1
Output: [1]
```

---

<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Two phases:
1. **Build a frequency map** in `O(n)`, then collect the *unique* values into a `unique` list.
2. **Quickselect over `unique`** — but the comparison is no longer on the raw value. The partition reads each element's frequency through the map and routes *more frequent* elements to the left. Once the pivot lands at index `k - 1`, the first `k` slots of `unique` hold the k most frequent values.

This is quickselect on a *derived* array: the elements being partitioned are the unique values, but the score driving every comparison is `frequency[value]`. The partition's `>` comparison (more frequent goes left) means the recursion converges on the k highest-frequency values rather than the k smallest.

```python run viz=array viz-root=unique
import random
from typing import List, Dict

class Solution:

    # Partition function to rearrange the elements based on their
    # frequency
    def partition(
        self,
        unique: List[int],
        left: int,
        right: int,
        frequency: Dict[int, int],
    ) -> int:

        # Random pivot index
        pivot = left + random.randint(0, right - left)

        # 1. Ge the frequency of the pivot element
        pivot_freq = frequency[unique[pivot]]

        # Move pivot to the end
        unique[pivot], unique[right] = unique[right], unique[pivot]

        # 2. Move all more frequent elements to the left
        next_higher_frequency_index = left
        for i in range(left, right):

            # If the frequency of the current element is greater than
            # the frequency of the pivot element, swap them
            if frequency[unique[i]] > pivot_freq:
                unique[next_higher_frequency_index], unique[i] = (
                    unique[i],
                    unique[next_higher_frequency_index],
                )
                next_higher_frequency_index += 1

        # 3. Move pivot to its final position
        unique[right], unique[next_higher_frequency_index] = (
            unique[next_higher_frequency_index],
            unique[right],
        )
        return next_higher_frequency_index

    # Quickselect to find the k-th most frequent element
    def quickselect(
        self,
        unique: List[int],
        left: int,
        right: int,
        k: int,
        frequency: Dict[int, int],
    ) -> None:

        # Only one element left in the range
        if left == right:
            return

        # Partition the array and get the pivot index
        pivot = self.partition(unique, left, right, frequency)

        # If the pivot is at the k-th position (in 0-indexed)
        if k - 1 == pivot:
            return

        # If pivot is greater than k - 1, search in the left half
        elif pivot > k - 1:
            self.quickselect(unique, left, pivot - 1, k, frequency)

        # If k is greater than the pivot index, search in the right half
        else:
            self.quickselect(unique, pivot + 1, right, k, frequency)

    def k_most_frequent_elements(
        self, arr: List[int], k: int
    ) -> List[int]:

        # Hash map to store frequency of each element
        frequency = {}

        # Step 1: Count frequency of each element
        for n in arr:
            frequency[n] = frequency.get(n, 0) + 1

        # List to keep track of unique elements
        unique = list(frequency.keys())

        # Step 3: Find the k-th most frequent element
        # We want the k-th largest element by frequency
        self.quickselect(unique, 0, len(unique) - 1, k, frequency)

        # Step 4: Return the top k frequent elements
        return unique[:k]


print(sorted(Solution().k_most_frequent_elements([1, 2, 2, 3, 3, 3], 2)))  # [2, 3]
print(sorted(Solution().k_most_frequent_elements([1, 5, 6, 6], 1)))        # [6]
print(sorted(Solution().k_most_frequent_elements([1], 1)))                  # [1]
print(sorted(Solution().k_most_frequent_elements([4, 4, 4, 4], 1)))        # [4]
print(sorted(Solution().k_most_frequent_elements([1, 2, 3], 3)))            # [1, 2, 3]
print(sorted(Solution().k_most_frequent_elements([5, 5, 3, 3, 1], 2)))     # [3, 5]
```

```java run viz=array viz-root=unique
import java.util.*;

public class Main {
    static class Solution {

        // Helper method to swap elements in the array
        private void swap(int[] arr, int i, int j) {
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }

        // Partition function to rearrange the elements based on their
        // frequency
        private int partition(
            int[] unique,
            int left,
            int right,
            Map<Integer, Integer> frequency
        ) {

            // Random pivot index
            Random rand = new Random();
            int pivot = left + rand.nextInt(right - left + 1);

            // 1. Get the frequency of the pivot element
            int pivotFreq = frequency.get(unique[pivot]);

            // Move pivot to the end
            swap(unique, pivot, right);

            // 2. Move all more frequent elements to the left
            int nextHigherFrequencyIndex = left;
            for (int i = left; i < right; i++) {

                // If the frequency of the current element is greater than
                // the frequency of the pivot element, swap them
                if (frequency.get(unique[i]) > pivotFreq) {
                    swap(unique, nextHigherFrequencyIndex, i);
                    nextHigherFrequencyIndex += 1;
                }
            }

            // 3. Move pivot to its final position
            swap(unique, nextHigherFrequencyIndex, right);
            return nextHigherFrequencyIndex;
        }

        // Quickselect to find the k-th most frequent element
        private void quickselect(
            int[] unique,
            int left,
            int right,
            int k,
            Map<Integer, Integer> frequency
        ) {

            // Only one element left in the range
            if (left == right) {
                return;
            }

            // Partition the array and get the pivot index
            int pivot = partition(unique, left, right, frequency);

            // If the pivot is at the k-th position (in 0-indexed)
            if (k - 1 == pivot) {
                return;
            }

            // If pivot is greater than k - 1, search in the left half
            else if (pivot > k - 1) {
                quickselect(unique, left, pivot - 1, k, frequency);
            }

            // If k is greater than the pivot index, search in the right half
            else {
                quickselect(unique, pivot + 1, right, k, frequency);
            }
        }

        public int[] kMostFrequentElements(int[] arr, int k) {

            // Hash map to store frequency of each element
            Map<Integer, Integer> frequency = new HashMap<>();

            // Step 1: Count frequency of each element
            for (int n : arr) {
                frequency.put(n, frequency.getOrDefault(n, 0) + 1);
            }

            // List to keep track of unique elements
            int[] unique = new int[frequency.size()];
            int idx = 0;

            // Step 2: Store the unique elements in the array
            for (var element : frequency.entrySet()) {
                unique[idx++] = element.getKey();
            }

            // Step 3: Find the k-th most frequent element
            // We want the k-th largest element by frequency
            quickselect(unique, 0, unique.length - 1, k, frequency);

            // Step 4: Return the top k frequent elements
            return Arrays.copyOfRange(unique, 0, k);
        }
    }

    public static void main(String[] args) {
        int[] r1 = new Solution().kMostFrequentElements(new int[]{1, 2, 2, 3, 3, 3}, 2);
        Arrays.sort(r1); System.out.println(Arrays.toString(r1));   // [2, 3]

        int[] r2 = new Solution().kMostFrequentElements(new int[]{1, 5, 6, 6}, 1);
        Arrays.sort(r2); System.out.println(Arrays.toString(r2));   // [6]

        int[] r3 = new Solution().kMostFrequentElements(new int[]{1}, 1);
        Arrays.sort(r3); System.out.println(Arrays.toString(r3));   // [1]

        int[] r4 = new Solution().kMostFrequentElements(new int[]{4, 4, 4, 4}, 1);
        Arrays.sort(r4); System.out.println(Arrays.toString(r4));   // [4]

        int[] r5 = new Solution().kMostFrequentElements(new int[]{1, 2, 3}, 3);
        Arrays.sort(r5); System.out.println(Arrays.toString(r5));   // [1, 2, 3]

        int[] r6 = new Solution().kMostFrequentElements(new int[]{5, 5, 3, 3, 1}, 2);
        Arrays.sort(r6); System.out.println(Arrays.toString(r6));   // [3, 5]
    }
}
```

The structure mirrors basic quickselect, with one change: the partition no longer compares `arr[i] < pivot_value` — it compares `frequency[unique[i]] > pivot_freq`, reading each element's score out of the frequency map. Routing *more frequent* values left (a `>` test) makes the k-th *most* frequent — rather than k-th smallest — land at index `k - 1`, so `unique[:k]` is the answer.

### Complexity

| Resource | Cost |
|---|---|
| **Time (frequency build)** | `O(n)` — one pass over `arr`. |
| **Time (quickselect)** | `O(u)` average — each partition is `O(u)` and the search space halves each recursion; `O(u²)` worst case, where `u = number of unique elements ≤ n`. |
| **Total time** | `O(n + u)` average. |
| **Space** | `O(u)` for the frequency map and the `unique` list; `O(log u)` average for the recursion stack. |

For inputs with many unique elements (`u ≈ n`), this is `O(n)` average — the random pivot makes the `O(u²)` worst case practically unreachable. The map lookup inside the partition is `O(1)`, so reading frequencies adds no asymptotic cost.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Quickselect is one algorithm with a hundred faces. Find the k-th smallest, the median, the k closest to a target, the k most frequent — they're all the same recursion with a different comparison function. The pattern: **derive a score for each element, partition by score, recurse on the half that contains position k**.

This pattern shows up everywhere — top-K queries in databases, percentile computations in statistics, ranking systems in recommendation engines. Once you can spot it, you stop sorting full arrays just to look at one position.

The next lesson is the final pattern in the sorting section: **custom compare**. Sometimes the elements aren't simple integers — they're records, tuples, objects with multiple fields. The sort key is an expression, not a value. We'll see how to abstract the comparison out of the algorithm so any sort can handle any comparison rule.

**Transfer challenge — try before the Custom Compare lesson:** Write a function that returns the *k smallest* elements of an array, sorted ascending. Quickselect gets you the partition (`arr[0..k-1]` are the k smallest, but in arbitrary order). What additional step makes the output sorted? What's the total time?

<details>
<summary><strong>Answer — open after you've thought about it</strong></summary>

```python run viz=array
class Solution:
    def k_smallest_sorted(self, arr, k):
        self._quickselect(arr, 0, len(arr) - 1, k)
        return sorted(arr[:k])
```

Quickselect: `O(n)` average. Sorting the first k elements: `O(k log k)`. Total: `O(n + k log k)`.

For `k << n`, this is `O(n)` — a strict improvement over full sort's `O(n log n)`. For `k = n`, it's `O(n log n)` — same as full sort. The break-even is `k ≈ n / log n`.

This pattern (partition + small sort) is exactly what `numpy.partition()` + `numpy.sort()[:k]` does internally. **You just rediscovered the optimal top-K-sorted algorithm.**

</details>

</details>
