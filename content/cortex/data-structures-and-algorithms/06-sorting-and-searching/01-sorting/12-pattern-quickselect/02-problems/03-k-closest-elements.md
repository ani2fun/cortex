---
title: "K Closest Elements"
summary: "Given an array arr, an integer k, and a target target, return the k closest elements to target. Closeness is measured by |x - target|; ties broken by smaller value first."
prereqs:
  - 12-pattern-quickselect/01-pattern
difficulty: medium
kind: problem
topics: [quickselect, sorting]
---

# K Closest Elements

Quickselect's partition step compares against a pivot. Change *what* you compare and you can find the k-th most-anything: closest to a target, brightest, oldest, etc.

## Problem Statement

Given an array `arr`, an integer `k`, and a target `target`, return the `k` closest elements to `target` (sorted ascending). Closeness is measured by `|x - target|`; ties broken by smaller value first.

## Examples

**Example 1**
```
Input:  arr = [1, 2, 3, 4, 5, 6], k = 3, target = 4
Output: [3, 4, 5]
Explanation: Distances: |1-4|=3, |2-4|=2, |3-4|=1, |4-4|=0, |5-4|=1, |6-4|=2.
             The 3 closest are 4 (d=0), 3 and 5 (d=1). Sorted: [3, 4, 5].
```

**Example 2**
```
Input:  arr = [1, 5, 8, 10, 12, 13], k = 3, target = 10
Output: [8, 10, 12]
Explanation: Distances: |8-10|=2, |10-10|=0, |12-10|=2. No ties at the boundary.
```

## Constraints

- `1 ≤ arr.length ≤ 10^4`
- `1 ≤ k ≤ arr.length`
- `-10^4 ≤ arr[i], target ≤ 10^4`
- Output is sorted ascending. Tie-break: smaller value ranks closer.

```python run viz=array
import ast
import random
from typing import List

class Solution:
    def partition(self, arr: List[int], left: int, right: int, target: int) -> int:
        # Your code goes here — partition by distance to target,
        # tie-break by smaller value.
        return left

    def quickselect(self, arr: List[int], left: int, right: int, k: int, target: int) -> None:
        # Your code goes here
        pass

    def k_closest_elements(self, arr: List[int], k: int, target: int) -> List[int]:
        # Your code goes here — quickselect then return sorted(arr[:k]).
        return []


arr = ast.literal_eval(input())
k = int(input())
target = int(input())
print(sorted(Solution().k_closest_elements(arr, k, target)))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        private void swap(int[] arr, int i, int j) {
            int temp = arr[i]; arr[i] = arr[j]; arr[j] = temp;
        }

        private int partition(int[] arr, int left, int right, int target) {
            // Your code goes here — distance-based partition,
            // tie-break by smaller value.
            return left;
        }

        private void quickselect(int[] arr, int left, int right, int k, int target) {
            // Your code goes here
        }

        public int[] kClosestElements(int[] arr, int k, int target) {
            // Your code goes here — quickselect then sort and return arr[0..k].
            return new int[0];
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        int target = Integer.parseInt(sc.nextLine().trim());
        int[] r = new Solution().kClosestElements(arr, k, target);
        Arrays.sort(r);
        System.out.println(Arrays.toString(r));
    }

    // "[1, 2, 3, 4, 5, 6]" → {1, 2, 3, 4, 5, 6}
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 2, 3, 4, 5, 6]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "3" },
    { "id": "target", "label": "target", "type": "int", "placeholder": "4" }
  ],
  "cases": [
    { "args": { "arr": "[1, 2, 3, 4, 5, 6]", "k": "3", "target": "4" }, "expected": "[3, 4, 5]" },
    { "args": { "arr": "[1, 4, 5, 6, 7, 8]", "k": "4", "target": "3" }, "expected": "[1, 4, 5, 6]" },
    { "args": { "arr": "[1, 5, 8, 10, 12, 13]", "k": "3", "target": "10" }, "expected": "[8, 10, 12]" },
    { "args": { "arr": "[1, 10, 20]", "k": "2", "target": "15" }, "expected": "[10, 20]" },
    { "args": { "arr": "[1, 2]", "k": "1", "target": "2" }, "expected": "[2]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The same quickselect algorithm works here — just swap the comparison function. Instead of comparing raw values, compare distances `|x - target|`. After partitioning by distance, the `k` closest elements land in the first `k` slots (in arbitrary order). Since random pivots make the result non-deterministic in ordering, sort those `k` elements before returning. The tie-break `(|x-target|, x)` inside the partition ensures the partition itself is deterministic even when two elements are equidistant.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Replace the partition's "compare elements directly" with "compare distance-to-target." Everything else is identical.

```python solution time=O(n) space=O(log n)
import ast
import random
from typing import List

class Solution:

    def partition(
        self, arr: List[int], left: int, right: int, target: int
    ) -> int:
        pivot = left + random.randint(0, right - left)
        pivot_val = arr[pivot]
        pivot_diff = abs(pivot_val - target)
        arr[pivot], arr[right] = arr[right], arr[pivot]
        next_closest_index = left
        for i in range(left, right):
            if abs(arr[i] - target) < pivot_diff or (
                abs(arr[i] - target) == pivot_diff and arr[i] < pivot_val
            ):
                arr[next_closest_index], arr[i] = (
                    arr[i],
                    arr[next_closest_index],
                )
                next_closest_index += 1
        arr[next_closest_index], arr[right] = (
            arr[right],
            arr[next_closest_index],
        )
        return next_closest_index

    def quickselect(
        self, arr: List[int], left: int, right: int, k: int, target: int
    ) -> None:
        if left == right:
            return
        pivot = self.partition(arr, left, right, target)
        if k - 1 == pivot:
            return
        elif pivot > k - 1:
            self.quickselect(arr, left, pivot - 1, k, target)
        else:
            self.quickselect(arr, pivot + 1, right, k, target)

    def k_closest_elements(
        self, arr: List[int], k: int, target: int
    ) -> List[int]:
        self.quickselect(arr, 0, len(arr) - 1, k, target)
        return arr[:k]


arr = ast.literal_eval(input())
k = int(input())
target = int(input())
print(sorted(Solution().k_closest_elements(arr, k, target)))
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

        private int partition(int[] arr, int left, int right, int target) {
            Random rand = new Random();
            int pivot = left + rand.nextInt(right - left + 1);
            int pivotVal = arr[pivot];
            int pivotDiff = Math.abs(pivotVal - target);
            swap(arr, pivot, right);
            int nextClosestIndex = left;
            for (int i = left; i < right; i++) {
                if (
                    Math.abs(arr[i] - target) < pivotDiff ||
                    (Math.abs(arr[i] - target) == pivotDiff &&
                        arr[i] < pivotVal)
                ) {
                    swap(arr, nextClosestIndex, i);
                    nextClosestIndex++;
                }
            }
            swap(arr, nextClosestIndex, right);
            return nextClosestIndex;
        }

        private void quickselect(
            int[] arr, int left, int right, int k, int target
        ) {
            if (left == right) {
                return;
            }
            int pivot = partition(arr, left, right, target);
            if (k - 1 == pivot) {
                return;
            } else if (pivot > k - 1) {
                quickselect(arr, left, pivot - 1, k, target);
            } else {
                quickselect(arr, pivot + 1, right, k, target);
            }
        }

        public int[] kClosestElements(int[] arr, int k, int target) {
            quickselect(arr, 0, arr.length - 1, k, target);
            return Arrays.copyOfRange(arr, 0, k);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        int target = Integer.parseInt(sc.nextLine().trim());
        int[] r = new Solution().kClosestElements(arr, k, target);
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

The partition compares with the score-tuple `(|x - target|, x)` instead of the raw value `x`: an element wins the swap if its distance to the target is strictly less than the pivot's, or — on a tie — its value is strictly less than the pivot's. The structure of `quickselect` and the recursive driver is unchanged from the basic version; once `pivot == k - 1`, the first `k` slots of `arr` hold the k closest elements (in arbitrary order). Both Python and Java sort the result before printing to canonicalize random-pivot output.

### Complexity

`O(n)` average — same as basic quickselect. Computing `abs(arr[i] - target)` is `O(1)`, so the partition is still linear.

</details>
