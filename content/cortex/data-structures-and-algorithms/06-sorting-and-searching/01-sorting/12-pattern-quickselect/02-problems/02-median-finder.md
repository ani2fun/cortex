---
title: "Median Finder"
summary: "Return the median of arr. For odd-length arrays, the middle element. For even-length arrays, the floor of the two middle elements' average."
prereqs:
  - 12-pattern-quickselect/01-pattern
difficulty: medium
kind: problem
topics: [quickselect, sorting]
---

# Median Finder

The median is the middle element. For odd `n`, it's the `(n/2 + 1)`-th smallest. For even `n`, it's the *floor* of the average of the two middles. Either way, it's a quickselect problem.

## Problem Statement

Return the median of `arr`. For odd-length arrays, the middle element. For even-length arrays, the truncated integer of the two middle elements' average (truncation toward zero, not floor).

## Examples

**Example 1**
```
Input:  arr = [5, 4, 2, 8, 9]
Output: 5
Explanation: Sorted: [2, 4, 5, 8, 9]. Middle element (index 2) is 5.
```

**Example 2**
```
Input:  arr = [5, 8, 1, 2]
Output: 3
Explanation: Sorted: [1, 2, 5, 8]. Two middles are 2 and 5; int((2+5)/2) = 3.
```

## Constraints

- `1 ≤ arr.length ≤ 10^4`
- `-10^4 ≤ arr[i] ≤ 10^4`
- Average of two middle elements is computed as `int((a + b) / 2)` — truncation toward zero (not floor), so `[-3, -4] → -3`.

```python run viz=array
import ast
import random
from typing import List

class Solution:
    def partition(self, arr: List[int], left: int, right: int) -> int:
        # Your code goes here
        return left

    def quickselect(self, arr: List[int], left: int, right: int, k: int) -> int:
        # Your code goes here (0-based k)
        return arr[left]

    def find_median(self, arr: List[int]) -> int:
        # Your code goes here — one call for odd n, two calls for even n.
        return -1


arr = ast.literal_eval(input())
print(Solution().find_median(arr))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        private Random rand = new Random();

        private void swap(int[] arr, int i, int j) {
            int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }

        private int partition(int[] arr, int left, int right) {
            // Your code goes here
            return left;
        }

        private int quickselect(int[] arr, int left, int right, int k) {
            // Your code goes here (0-based k)
            return arr[left];
        }

        public int findMedian(int[] arr) {
            // Your code goes here — one call for odd n, two calls for even n.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        System.out.println(new Solution().findMedian(arr));
    }

    // "[5, 4, 2, 8, 9]" → {5, 4, 2, 8, 9}
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[5, 4, 2, 8, 9]" }
  ],
  "cases": [
    { "args": { "arr": "[5, 4, 2, 8, 9]" }, "expected": "5" },
    { "args": { "arr": "[5, 8, 1, 2]" }, "expected": "3" },
    { "args": { "arr": "[-3, -4]" }, "expected": "-3" },
    { "args": { "arr": "[1, 2]" }, "expected": "1" },
    { "args": { "arr": "[3, 1, 4, 1, 5]" }, "expected": "3" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The median is just a specific order statistic: for odd `n` it's rank `n//2`, for even `n` it's the average of ranks `n//2 - 1` and `n//2`. Quickselect finds any rank in `O(n)` average — so the median is one or two quickselect calls rather than a full sort. The only subtlety is the even-length average: use `int((a+b)/2)` (truncation toward zero) so that `[-3,-4]` gives `-3`, not `-4`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

The trick: for odd `n`, one quickselect call. For even `n`, two calls — one for `n/2 - 1`, one for `n/2`. Take the truncated integer of their average. The `quickselect` here uses a 0-based target index throughout.

```python solution time=O(n) space=O(log n)
import ast
import random
from typing import List

class Solution:

    def partition(self, arr: List[int], left: int, right: int) -> int:
        pivot = left + random.randint(0, right - left)
        pivot_value = arr[pivot]
        arr[pivot], arr[right] = arr[right], arr[pivot]
        next_smaller_index = left
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
    ) -> int:
        if left >= right:
            return arr[left]
        pivot = self.partition(arr, left, right)
        if pivot == k:
            return arr[pivot]
        elif pivot > k:
            return self.quickselect(arr, left, pivot - 1, k)
        else:
            return self.quickselect(arr, pivot + 1, right, k)

    def find_median(self, arr: List[int]) -> int:
        n = len(arr)
        if n % 2 == 1:
            return self.quickselect(arr, 0, n - 1, n // 2)
        left_mid = self.quickselect(arr, 0, n - 1, n // 2 - 1)
        right_mid = self.quickselect(arr, 0, n - 1, n // 2)
        return int((left_mid + right_mid) / 2)


arr = ast.literal_eval(input())
print(Solution().find_median(arr))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private Random rand = new Random();

        private void swap(int[] arr, int i, int j) {
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
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

        private int quickselect(int[] arr, int left, int right, int k) {
            if (left >= right) {
                return arr[left];
            }
            int pivot = partition(arr, left, right);
            if (pivot == k) {
                return arr[pivot];
            } else if (pivot > k) {
                return quickselect(arr, left, pivot - 1, k);
            } else {
                return quickselect(arr, pivot + 1, right, k);
            }
        }

        public int findMedian(int[] arr) {
            int n = arr.length;
            if (n % 2 == 1) {
                return quickselect(arr, 0, n - 1, n / 2);
            }
            int leftMid = quickselect(arr, 0, n - 1, n / 2 - 1);
            int rightMid = quickselect(arr, 0, n - 1, n / 2);
            return (leftMid + rightMid) / 2;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        System.out.println(new Solution().findMedian(arr));
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

The implementation has three pieces: a Lomuto-style `partition` that picks a random pivot, scans `[left, right)` and routes values smaller than the pivot to a sliding `next_smaller_index`, then drops the pivot at its final position; a `quickselect` driver that recurses on whichever side of the pivot contains the target index; and the `find_median` wrapper that chooses how many calls to make based on parity. The recursion treats `k` as a 0-based index throughout — that's why the base case checks `pivot == k` instead of `pivot == k - 1`. For even-length arrays the truncated average is computed as `int((left_mid + right_mid) / 2)` in Python and `(leftMid + rightMid) / 2` in Java — both truncate toward zero (matching the `[-3, -4] → -3` example above).

### Complexity

| Resource | Cost |
|---|---|
| **Time** | `O(n)` average for both odd-n (one call) and even-n (two calls). |
| **Space (stack)** | `O(log n)` average. |

</details>
