---
title: "Kth Smallest Element"
summary: "Given an array arr and a positive integer k, return the k-th smallest element."
prereqs:
  - 12-pattern-quickselect/01-pattern
difficulty: medium
kind: problem
topics: [quickselect, sorting]
---

# Kth Smallest Element

The canonical top-K problem. Find the k-th smallest element of an array. The **bounded-size max-heap** is one classical answer; **quickselect** is the other, and it's what we'll implement here — one partition step puts the pivot at its final sorted position, then we recurse on only the half that contains index `k - 1` until the pivot lands there.

## Problem Statement

Given an array `arr` and a positive integer `k`, return the k-th smallest element.

## Examples

**Example 1**
```
Input:  arr = [5, 4, 2, 8], k = 2
Output: 4
Explanation: Sorted: [2, 4, 5, 8]. The 2nd smallest is 4.
```

**Example 2**
```
Input:  arr = [1, 2, 3, 4, 5], k = 5
Output: 5
Explanation: Sorted: [1, 2, 3, 4, 5]. The 5th smallest is 5 (the largest).
```

## Constraints

- `1 ≤ arr.length ≤ 10^4`
- `-10^4 ≤ arr[i] ≤ 10^4`
- `1 ≤ k ≤ arr.length`

```python run viz=array
import ast
import random
from typing import List

class Solution:
    def partition(self, arr: List[int], left: int, right: int) -> int:
        # Your code goes here — partition arr[left..right] and return
        # the pivot's final index.
        return left

    def quickselect(self, arr: List[int], left: int, right: int, k: int) -> None:
        # Your code goes here — recurse on the side holding index k-1.
        pass

    def kth_smallest_elements(self, arr: List[int], k: int) -> int:
        # Your code goes here — call quickselect then return arr[k-1].
        return -1


arr = ast.literal_eval(input())
k = int(input())
print(Solution().kth_smallest_elements(arr, k))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        private Random rand = new Random();

        private void swap(int[] arr, int i, int j) {
            int temp = arr[i]; arr[i] = arr[j]; arr[j] = temp;
        }

        private int partition(int[] arr, int left, int right) {
            // Your code goes here — random pivot, Lomuto-style partition,
            // return pivot's final index.
            return left;
        }

        private void quickselect(int[] arr, int left, int right, int k) {
            // Your code goes here — recurse on the side holding index k-1.
        }

        public int kthSmallestElement(int[] arr, int k) {
            // Your code goes here — call quickselect then return arr[k-1].
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kthSmallestElement(arr, k));
    }

    // "[5, 4, 2, 8]" → {5, 4, 2, 8}
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[5, 4, 2, 8]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "arr": "[5, 4, 2, 8]", "k": "2" }, "expected": "4" },
    { "args": { "arr": "[1, 2, 3, 4, 5]", "k": "5" }, "expected": "5" },
    { "args": { "arr": "[7, 5, 9]", "k": "3" }, "expected": "9" },
    { "args": { "arr": "[10, 5, 3, 8, 1]", "k": "3" }, "expected": "5" },
    { "args": { "arr": "[4, 4, 4]", "k": "2" }, "expected": "4" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

After a single partition pass, the pivot lands at its exact final sorted rank. If that rank equals `k-1`, we're done without looking at another element. If the rank is higher, the answer is in the left segment; if lower, in the right. Each step discards half the remaining work, turning `O(n log n)` sort into `O(n)` average selection.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Run quickselect with `k` interpreted as a 1-based rank, so the target sorted position is index `k - 1`. The `partition` helper picks a random pivot, swaps it to the end, scans `[left, right)` routing every value smaller than the pivot to a sliding `next_smaller_index`, then drops the pivot at that index — its final sorted position. The `quickselect` driver compares the returned pivot index against `k - 1`: equal means we're done, larger means recurse on the left half, smaller means recurse on the right half. Once it returns, `arr[k - 1]` holds the k-th smallest element.

```python solution time=O(n) space=O(log n)
import ast
import random
from typing import List

class Solution:

    def partition(self, arr: List[int], left: int, right: int) -> int:
        pivot: int = left + random.randint(0, right - left)
        pivot_value: int = arr[pivot]
        arr[pivot], arr[right] = arr[right], arr[pivot]
        next_smaller_index: int = left
        for i in range(left, right):
            if arr[i] < pivot_value:
                arr[next_smaller_index], arr[i] = (
                    arr[i],
                    arr[next_smaller_index],
                )
                next_smaller_index += 1
        arr[next_smaller_index], arr[right] = (
            arr[right],
            arr[next_smaller_index],
        )
        return next_smaller_index

    def quickselect(
        self, arr: List[int], left: int, right: int, k: int
    ) -> None:
        if left >= right:
            return
        pivot: int = self.partition(arr, left, right)
        if pivot == k - 1:
            return
        elif pivot > k - 1:
            self.quickselect(arr, left, pivot - 1, k)
        else:
            self.quickselect(arr, pivot + 1, right, k)

    def kth_smallest_elements(self, arr: List[int], k: int) -> int:
        n: int = len(arr)
        self.quickselect(arr, 0, n - 1, k)
        return arr[k - 1]


arr = ast.literal_eval(input())
k = int(input())
print(Solution().kth_smallest_elements(arr, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private Random rand = new Random();

        private void swap(int[] arr, int i, int j) {
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }

        private int partition(int[] arr, int left, int right) {
            int pivot = left + rand.nextInt(right - left + 1);
            int pivotValue = arr[pivot];
            swap(arr, pivot, right);
            int nextSmallerIndex = left;
            for (int i = left; i < right; i++) {
                if (arr[i] < pivotValue) {
                    swap(arr, nextSmallerIndex, i);
                    nextSmallerIndex++;
                }
            }
            swap(arr, nextSmallerIndex, right);
            return nextSmallerIndex;
        }

        private void quickselect(int[] arr, int left, int right, int k) {
            if (left >= right) {
                return;
            }
            int pivot = partition(arr, left, right);
            if (pivot == k - 1) {
                return;
            } else if (pivot > k - 1) {
                quickselect(arr, left, pivot - 1, k);
            } else {
                quickselect(arr, pivot + 1, right, k);
            }
        }

        public int kthSmallestElement(int[] arr, int k) {
            int n = arr.length;
            quickselect(arr, 0, n - 1, k);
            return arr[k - 1];
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kthSmallestElement(arr, k));
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

### Complexity

| Resource | Cost |
|---|---|
| **Time** | `O(n)` average — each partition is `O(n)` and the search space halves each recursion, summing to `2n`. `O(n²)` worst case on maximally unbalanced pivots. |
| **Space** | `O(1)` for the in-place partition, plus `O(log n)` average for the recursion stack. |

For `k << n`, quickselect's `O(n)` average beats a full `O(n log n)` sort and the `O(n log k)` bounded-heap alternative; the random pivot makes the `O(n²)` worst case practically unreachable.

</details>
