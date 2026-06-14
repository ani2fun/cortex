---
title: "Limit Count"
summary: "Sorted array, integer k. Return the count of elements ≤ k."
prereqs:
  - 10-pattern-upper-bound/01-pattern
difficulty: easy
kind: problem
topics: [upper-bound, searching]
---

# Limit Count

## Problem Statement

Sorted array `arr` and integer `k`. Return the count of elements `≤ k`.

```
Input:  arr = [1, 3, 5, 8, 9], k = 7
Output: 3

Input:  arr = [1, 2, 2, 2, 3, 4], k = 3
Output: 5

Input:  arr = [1, 2, 2, 2, 3, 4], k = 8
Output: 6
```

## Examples

**Example 1**
```
Input:  arr = [1, 3, 5, 8, 9], k = 7
Output: 3
Explanation: Elements 1, 3, 5 are ≤ 7; elements 8 and 9 are not.
```

**Example 2**
```
Input:  arr = [1, 2, 2, 2, 3, 4], k = 3
Output: 5
Explanation: All elements up to and including the three 2s and the 3 qualify; only 4 does not.
```

## Constraints

- `1 ≤ arr.length ≤ 10^5`
- `-10^9 ≤ arr[i] ≤ 10^9`, array is sorted in non-decreasing order
- `-10^9 ≤ k ≤ 10^9`

```python run viz=array
import ast
from typing import List

class Solution:
    def limit_count(self, arr: List[int], k: int) -> int:
        # Your code goes here — upper_bound(arr, k) gives the first index
        # strictly greater than k, which equals the count of elements ≤ k.
        return 0

arr = ast.literal_eval(input())
k = int(input())
print(Solution().limit_count(arr, k))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int limitCount(int[] arr, int k) {
            // Your code goes here — upper_bound(arr, k) gives the first index
            // strictly greater than k, which equals the count of elements ≤ k.
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().limitCount(arr, k));
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

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 3, 5, 8, 9]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "7" }
  ],
  "cases": [
    { "args": { "arr": "[1, 3, 5, 8, 9]", "k": "7" }, "expected": "3" },
    { "args": { "arr": "[1, 2, 2, 2, 3, 4]", "k": "3" }, "expected": "5" },
    { "args": { "arr": "[1, 2, 2, 2, 3, 4]", "k": "8" }, "expected": "6" },
    { "args": { "arr": "[5]", "k": "5" }, "expected": "1" },
    { "args": { "arr": "[5]", "k": "3" }, "expected": "0" },
    { "args": { "arr": "[1, 2, 2, 2, 3, 4]", "k": "0" }, "expected": "0" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The count of elements `≤ k` in a sorted array is exactly the index of the first element `> k` — because everything before that index satisfies the condition. Upper bound computes that boundary index in `O(log n)` by searching with `arr[mid] <= target` to step past equal elements. No loop over elements needed; the count falls out of the search position itself.

</details>
<details>
<summary><h2>The Solution</h2></summary>


The count of elements `≤ k` is exactly `upper_bound(arr, k)` — that's the first index strictly greater than `k`, which equals the number of elements `≤ k`.


```python solution time=O(log n) space=O(1)
import ast
from typing import List

class Solution:
    def upper_bound(self, arr: List[int], target: int) -> int:

        # Initialise starting index to 0
        low: int = 0

        # Initialise ending index to len(arr) instead of len(arr) - 1
        # to cover the entire array as if all elements in the array are less
        # than target, the upper bound index would be equal to len(arr)
        high: int = len(arr)

        # 'high' is exclusive (can be len(arr)), so we use 'low < high' instead
        # of 'low <= high'. This loop finds the first index where the element is
        # > the target without going out of bounds.
        while low < high:

            # Find the middle index
            mid: int = low + (high - low) // 2

            # If arr[mid] is less than or equal to target, then find
            # in the right subarray
            if arr[mid] <= target:
                low = mid + 1

            # If arr[mid] is greater than the target, then it may be the answer.
            # So, instead of high = mid - 1, we do high = mid to include mid in
            # the next search space
            else:
                high = mid

        # Return the upper bound index, it could be equal to arr.length
        # if all elements are less than target
        return low

    def limit_count(self, arr: List[int], k: int) -> int:

        # The number of elements <= k is given by the
        # upper bound index of k
        return self.upper_bound(arr, k)


arr = ast.literal_eval(input())
k = int(input())
print(Solution().limit_count(arr, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private int upperBound(int[] arr, int target) {

            // Initialise starting index to 0
            int low = 0;

            // Initialise ending index to arr.length instead of arr.length -
            // 1 to cover the entire array as if all elements in the array
            // are less than target, the upper bound index would be equal to
            // arr.length
            int high = arr.length;

            // 'high' is exclusive (can be arr.length), so we use 'low <
            // high' instead of 'low <= high'. This loop finds the first
            // index where the element is > the target without going out of
            // bounds.
            while (low < high) {

                // Find the middle index
                int mid = low + (high - low) / 2;

                // If arr[mid] is less than or equal to target, then find
                // in the right subarray
                if (arr[mid] <= target) {
                    low = mid + 1;
                }

                // If arr[mid] is greater than the target, then it may be the
                // answer. So, instead of high = mid - 1, we do high = mid to
                // include mid in the next search space
                else {
                    high = mid;
                }
            }

            // Return the upper bound index, it could be equal to arr.length
            // if all elements are less than target
            return low;
        }

        public int limitCount(int[] arr, int k) {

            // The number of elements <= k is given by the
            // upper bound index of k
            return upperBound(arr, k);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().limitCount(arr, k));
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

</details>
