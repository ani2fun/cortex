---
title: "K Most Frequent Elements"
summary: "Given an array arr and a positive integer k, return the k most frequent elements (in any order)."
prereqs:
  - 12-pattern-quickselect/01-pattern
difficulty: medium
kind: problem
topics: [quickselect, sorting]
---

# K Most Frequent Elements

The final pattern — quickselect on a *derived* array. Build a frequency map, extract unique elements, quickselect by frequency.

## Problem Statement

Given an array `arr` and a positive integer `k`, return the `k` most frequent elements (sorted ascending).

## Examples

**Example 1**
```
Input:  arr = [1, 2, 2, 3, 3, 3], k = 2
Output: [2, 3]
Explanation: 3 appears 3 times, 2 appears 2 times, 1 appears 1 time. Top 2 by frequency: [2, 3].
```

**Example 2**
```
Input:  arr = [1, 5, 6, 6], k = 1
Output: [6]
Explanation: 6 appears 2 times — the highest frequency. Top 1: [6].
```

## Constraints

- `1 ≤ arr.length ≤ 10^4`
- `-10^4 ≤ arr[i] ≤ 10^4`
- `1 ≤ k ≤` number of unique elements in `arr`
- Test cases have no tie at the k-th frequency boundary (the result set is unambiguous).
- Output is sorted ascending.

```python run viz=array viz-root=unique
import ast
import random
from typing import List, Dict

class Solution:
    def partition(
        self, unique: List[int], left: int, right: int, frequency: Dict[int, int]
    ) -> int:
        # Your code goes here — partition by frequency descending.
        return left

    def quickselect(
        self, unique: List[int], left: int, right: int, k: int, frequency: Dict[int, int]
    ) -> None:
        # Your code goes here
        pass

    def k_most_frequent_elements(self, arr: List[int], k: int) -> List[int]:
        # Your code goes here — build freq map, collect uniques, quickselect,
        # return unique[:k].
        return []


arr = ast.literal_eval(input())
k = int(input())
print(sorted(Solution().k_most_frequent_elements(arr, k)))
```

```java run viz=array viz-root=unique
import java.util.*;

public class Main {
    static class Solution {
        private void swap(int[] arr, int i, int j) {
            int temp = arr[i]; arr[i] = arr[j]; arr[j] = temp;
        }

        private int partition(int[] unique, int left, int right, Map<Integer, Integer> frequency) {
            // Your code goes here — partition by frequency descending.
            return left;
        }

        private void quickselect(int[] unique, int left, int right, int k, Map<Integer, Integer> frequency) {
            // Your code goes here
        }

        public int[] kMostFrequentElements(int[] arr, int k) {
            // Your code goes here — build freq map, collect uniques,
            // quickselect, return unique[0..k].
            return new int[0];
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        int[] r = new Solution().kMostFrequentElements(arr, k);
        Arrays.sort(r);
        System.out.println(Arrays.toString(r));
    }

    // "[1, 2, 2, 3, 3, 3]" → {1, 2, 2, 3, 3, 3}
    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 2, 2, 3, 3, 3]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "arr": "[1, 2, 2, 3, 3, 3]", "k": "2" }, "expected": "[2, 3]" },
    { "args": { "arr": "[1, 5, 6, 6]", "k": "1" }, "expected": "[6]" },
    { "args": { "arr": "[1]", "k": "1" }, "expected": "[1]" },
    { "args": { "arr": "[4, 4, 4, 4]", "k": "1" }, "expected": "[4]" },
    { "args": { "arr": "[5, 5, 3, 3, 1]", "k": "2" }, "expected": "[3, 5]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

Quickselect normally partitions by the element's raw value. Here the "value" we care about is the element's *frequency*. Build a frequency map in `O(n)`, collect the unique keys into a list, then run quickselect where the comparison is `frequency[a] > frequency[b]` (most frequent goes left). Once the pivot lands at index `k-1`, the first `k` slots hold the top-k by frequency. Since the partition uses a random pivot, the returned slice is in arbitrary order — sort it before returning.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Two phases:
1. **Build a frequency map** in `O(n)`, then collect the *unique* values into a `unique` list.
2. **Quickselect over `unique`** — the partition reads each element's frequency through the map and routes *more frequent* elements to the left. Once the pivot lands at index `k - 1`, the first `k` slots of `unique` hold the k most frequent values.

```python solution time=O(n) space=O(n)
import ast
import random
from typing import List, Dict

class Solution:

    def partition(
        self,
        unique: List[int],
        left: int,
        right: int,
        frequency: Dict[int, int],
    ) -> int:
        pivot = left + random.randint(0, right - left)
        pivot_freq = frequency[unique[pivot]]
        unique[pivot], unique[right] = unique[right], unique[pivot]
        next_higher_frequency_index = left
        for i in range(left, right):
            if frequency[unique[i]] > pivot_freq:
                unique[next_higher_frequency_index], unique[i] = (
                    unique[i],
                    unique[next_higher_frequency_index],
                )
                next_higher_frequency_index += 1
        unique[right], unique[next_higher_frequency_index] = (
            unique[next_higher_frequency_index],
            unique[right],
        )
        return next_higher_frequency_index

    def quickselect(
        self,
        unique: List[int],
        left: int,
        right: int,
        k: int,
        frequency: Dict[int, int],
    ) -> None:
        if left == right:
            return
        pivot = self.partition(unique, left, right, frequency)
        if k - 1 == pivot:
            return
        elif pivot > k - 1:
            self.quickselect(unique, left, pivot - 1, k, frequency)
        else:
            self.quickselect(unique, pivot + 1, right, k, frequency)

    def k_most_frequent_elements(
        self, arr: List[int], k: int
    ) -> List[int]:
        frequency = {}
        for n in arr:
            frequency[n] = frequency.get(n, 0) + 1
        unique = list(frequency.keys())
        self.quickselect(unique, 0, len(unique) - 1, k, frequency)
        return unique[:k]


arr = ast.literal_eval(input())
k = int(input())
print(sorted(Solution().k_most_frequent_elements(arr, k)))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {

        private void swap(int[] arr, int i, int j) {
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }

        private int partition(
            int[] unique,
            int left,
            int right,
            Map<Integer, Integer> frequency
        ) {
            Random rand = new Random();
            int pivot = left + rand.nextInt(right - left + 1);
            int pivotFreq = frequency.get(unique[pivot]);
            swap(unique, pivot, right);
            int nextHigherFrequencyIndex = left;
            for (int i = left; i < right; i++) {
                if (frequency.get(unique[i]) > pivotFreq) {
                    swap(unique, nextHigherFrequencyIndex, i);
                    nextHigherFrequencyIndex += 1;
                }
            }
            swap(unique, nextHigherFrequencyIndex, right);
            return nextHigherFrequencyIndex;
        }

        private void quickselect(
            int[] unique,
            int left,
            int right,
            int k,
            Map<Integer, Integer> frequency
        ) {
            if (left == right) {
                return;
            }
            int pivot = partition(unique, left, right, frequency);
            if (k - 1 == pivot) {
                return;
            } else if (pivot > k - 1) {
                quickselect(unique, left, pivot - 1, k, frequency);
            } else {
                quickselect(unique, pivot + 1, right, k, frequency);
            }
        }

        public int[] kMostFrequentElements(int[] arr, int k) {
            Map<Integer, Integer> frequency = new HashMap<>();
            for (int n : arr) {
                frequency.put(n, frequency.getOrDefault(n, 0) + 1);
            }
            int[] unique = new int[frequency.size()];
            int idx = 0;
            for (var element : frequency.entrySet()) {
                unique[idx++] = element.getKey();
            }
            quickselect(unique, 0, unique.length - 1, k, frequency);
            return Arrays.copyOfRange(unique, 0, k);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        int[] r = new Solution().kMostFrequentElements(arr, k);
        Arrays.sort(r);
        System.out.println(Arrays.toString(r));
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

The structure mirrors basic quickselect, with one change: the partition no longer compares `arr[i] < pivot_value` — it compares `frequency[unique[i]] > pivot_freq`, reading each element's score out of the frequency map. Routing *more frequent* values left (a `>` test) makes the k-th *most* frequent — rather than k-th smallest — land at index `k - 1`, so `unique[:k]` is the answer. Both Python and Java sort before printing to produce deterministic output regardless of random-pivot ordering.

### Complexity

| Resource | Cost |
|---|---|
| **Time (frequency build)** | `O(n)` — one pass over `arr`. |
| **Time (quickselect)** | `O(u)` average where `u = number of unique elements ≤ n`. |
| **Total time** | `O(n + u)` average. |
| **Space** | `O(u)` for the frequency map and the `unique` list; `O(log u)` average for the recursion stack. |

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
