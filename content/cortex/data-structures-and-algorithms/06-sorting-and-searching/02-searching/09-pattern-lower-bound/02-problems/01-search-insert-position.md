---
title: "Search Insert Position"
summary: "Given a sorted array arr and target, return target's index if present; otherwise return the index where target would be inserted to keep the array sorted."
prereqs:
  - 09-pattern-lower-bound/01-pattern
difficulty: easy
kind: problem
topics: [lower-bound, searching]
---

# Search Insert Position

Direct lower-bound application.

## Problem Statement

Given a sorted array `arr` and `target`, return target's index if present; otherwise return the index where target would be inserted to keep the array sorted.

## Examples

**Example 1**
```
Input:  arr = [1, 2, 3, 4, 5, 6], target = 3
Output: 2
Explanation: 3 is at index 2 in the array.
```

**Example 2**
```
Input:  arr = [1, 2, 7, 8, 9, 10], target = 3
Output: 2
Explanation: 3 is not present; it would be inserted before the 7 at index 2.
```

## Constraints

- `1 ≤ arr.length ≤ 10^4`
- `-10^4 ≤ arr[i], target ≤ 10^4`
- `arr` is sorted in ascending order with distinct values.

```python run viz=array
import ast
from typing import List

class Solution:
    def search_insert_position(self, arr: List[int], target: int) -> int:
        # Your code goes here — lower_bound: half-open [lo, hi),
        # move lo=mid+1 while arr[mid] < target, else hi=mid; return lo.
        return -1

arr = ast.literal_eval(input())
target = int(input())
print(Solution().search_insert_position(arr, target))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int searchInsertPosition(int[] arr, int target) {
            // Your code goes here — lower_bound: half-open [lo, hi),
            // move lo=mid+1 while arr[mid] < target, else hi=mid; return lo.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().searchInsertPosition(arr, target));
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 2, 3, 4, 5, 6]" },
    { "id": "target", "label": "target", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "arr": "[1, 2, 3, 4, 5, 6]", "target": "3" }, "expected": "2" },
    { "args": { "arr": "[1, 2, 7, 8, 9, 10]", "target": "3" }, "expected": "2" },
    { "args": { "arr": "[1, 2, 7, 9, 10, 11]", "target": "8" }, "expected": "3" },
    { "args": { "arr": "[5]", "target": "5" }, "expected": "0" },
    { "args": { "arr": "[5]", "target": "3" }, "expected": "0" },
    { "args": { "arr": "[5]", "target": "7" }, "expected": "1" },
    { "args": { "arr": "[1, 2, 3, 4, 5, 6]", "target": "7" }, "expected": "6" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The insert position is exactly the lower bound: the first index where `arr[i] >= target`. If `target` is already in the array, lower bound lands on its first occurrence; if it's absent, lower bound lands on the first element larger than it — which is exactly where `target` would slot in to keep the array sorted. One call, two jobs, no special-casing.

</details>
<details>
<summary><h2>The Solution</h2></summary>

Just lower bound.

```python solution time=O(log n) space=O(1)
import ast
from typing import List

class Solution:
    def lower_bound(self, arr: List[int], target: int) -> int:

        # Initialise starting index to 0
        low: int = 0

        # Initialise ending index to len(arr) instead of len(arr) - 1
        # to cover the entire array as if all elements in the array are less
        # than target, the lower bound index would be equal to len(arr)
        high: int = len(arr)

        # 'high' is exclusive (can be len(arr)), so we use 'low < high' instead
        # of 'low <= high'. This loop finds the first index where the element is
        # >= the target without going out of bounds.
        while low < high:

            # Find the middle index
            mid: int = low + (high - low) // 2

            # If arr[mid] is less than arr[target], then find in
            # right subarray
            if arr[mid] < target:
                low = mid + 1

            # If arr[mid] is greater than or equal to target, then it may
            # be the answer. So, instead of high = mid - 1, we do high = mid
            # to include mid in the next search space
            else:
                high = mid

        # Return the lower bound index, it could be equal to len(arr)
        # if all elements are less than target
        return low

    def search_insert_position(self, arr: List[int], target: int) -> int:
        return self.lower_bound(arr, target)


arr = ast.literal_eval(input())
target = int(input())
print(Solution().search_insert_position(arr, target))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private int lowerBound(int[] arr, int target) {

            // Initialise starting index to 0
            int low = 0;

            // Initialise ending index to arr.length instead of arr.length -
            // 1 to cover the entire array as if all elements in the array
            // are less than target, the lower bound index would be equal to
            // arr.length
            int high = arr.length;

            // 'high' is exclusive (can be arr.length), so we use 'low <
            // high' instead of 'low <= high'. This loop finds the first
            // index where the element is
            // >= the target without going out of bounds.
            while (low < high) {

                // Find the middle index
                int mid = low + (high - low) / 2;

                // If arr[mid] is less than arr[target], then find in
                // right subarray
                if (arr[mid] < target) {
                    low = mid + 1;
                }

                // If arr[mid] is greater than or equal to target, then it
                // may be the answer. So, instead of high = mid - 1, we do
                // high = mid to include mid in the next search space
                else {
                    high = mid;
                }
            }

            // Return the lower bound index, it could be equal to arr.length
            // if all elements are less than target
            return low;
        }

        public int searchInsertPosition(int[] arr, int target) {
            return lowerBound(arr, target);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().searchInsertPosition(arr, target));
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

`O(log n)` time, `O(1)` space.

</details>
