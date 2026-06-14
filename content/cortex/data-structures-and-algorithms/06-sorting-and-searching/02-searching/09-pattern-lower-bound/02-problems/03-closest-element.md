---
title: "Closest Element"
summary: "Given a sorted array and target, return the closest element. Ties broken by smaller value."
prereqs:
  - 09-pattern-lower-bound/01-pattern
difficulty: medium
kind: problem
topics: [lower-bound, searching]
---

# Closest Element

Use lower bound to find the threshold position; the answer is either at the threshold or just before it.

## Problem Statement

Given a sorted array and target, return the closest element. Ties broken by smaller value.

## Examples

**Example 1**
```
Input:  arr = [1, 2, 3, 4, 5, 6], target = 4
Output: 4
Explanation: 4 is in the array; the closest element is 4 itself.
```

**Example 2**
```
Input:  arr = [2, 4, 6, 8, 10, 12], target = 5
Output: 4
Explanation: 4 and 6 are equidistant from 5; ties broken by smaller value, so 4 wins.
```

## Constraints

- `1 ≤ arr.length ≤ 10^4`
- `-10^4 ≤ arr[i], target ≤ 10^4`
- `arr` is sorted in ascending order with distinct values.

```python run viz=array
import ast
from typing import List

class Solution:
    def closest_element(self, arr: List[int], target: int) -> int:
        # Your code goes here — lower_bound(target) gives index i with
        # arr[i] >= target; answer is arr[i] or arr[i-1], whichever is closer
        # (ties: smaller wins). Handle edge cases (i=0 or i=len).
        return -1

arr = ast.literal_eval(input())
target = int(input())
print(Solution().closest_element(arr, target))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int closestElement(int[] arr, int target) {
            // Your code goes here — lowerBound(target) gives index i with
            // arr[i] >= target; answer is arr[i] or arr[i-1], whichever is
            // closer (ties: smaller wins). Handle edge cases (i=0 or i=arr.length).
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().closestElement(arr, target));
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
    { "id": "target", "label": "target", "type": "int", "placeholder": "4" }
  ],
  "cases": [
    { "args": { "arr": "[1, 2, 3, 4, 5, 6]", "target": "4" }, "expected": "4" },
    { "args": { "arr": "[2, 4, 6, 8, 10, 12]", "target": "5" }, "expected": "4" },
    { "args": { "arr": "[1, 10]", "target": "7" }, "expected": "10" },
    { "args": { "arr": "[5]", "target": "5" }, "expected": "5" },
    { "args": { "arr": "[5]", "target": "1" }, "expected": "5" },
    { "args": { "arr": "[5]", "target": "9" }, "expected": "5" },
    { "args": { "arr": "[1, 2, 3, 4, 5, 6]", "target": "0" }, "expected": "1" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

`lower_bound(target)` splits the sorted array at the first element `>= target`. The closest element must be either that element (`arr[i]`) or its left neighbour (`arr[i-1]`), because every other element is farther away. Comparing just these two candidates — handling the boundary cases where `i == 0` or `i == len` — gives the answer in `O(log n)` total.

</details>
<details>
<summary><h2>The Solution</h2></summary>

`lower_bound(target)` gives the smallest index `i` with `arr[i] >= target`. The closest element is either `arr[i]` or `arr[i - 1]` — compare distances.

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

    def closest_element(self, arr: List[int], target: int) -> int:

        # Return -1 if the array is empty
        if not arr:
            return -1

        lower_bound_index = self.lower_bound(arr, target)

        # If lower bound index is 0, return the first element
        if lower_bound_index == 0:
            return arr[0]

        # If lower bound index is equal to the size of the array,
        # return the last element
        elif lower_bound_index == len(arr):
            return arr[-1]

        # Else, return the element which is closest to the target
        # among the two closest elements
        else:

            # Get the element strictly less than target
            lower_element = arr[lower_bound_index - 1]

            # Get the element greater than or equal to target
            upper_element = arr[lower_bound_index]

            # Return the closest element
            if target - lower_element <= upper_element - target:
                return lower_element
            else:
                return upper_element


arr = ast.literal_eval(input())
target = int(input())
print(Solution().closest_element(arr, target))
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

        public int closestElement(int[] arr, int target) {

            // Return -1 if the array is empty
            if (arr.length == 0) {
                return -1;
            }

            int lowerBoundIndex = lowerBound(arr, target);

            // If lower bound index is 0, return the first element
            if (lowerBoundIndex == 0) {
                return arr[0];
            }

            // If lower bound index is equal to the size of the array,
            // return the last element
            else if (lowerBoundIndex == arr.length) {
                return arr[arr.length - 1];
            }

            // Else, return the element which is closest to the target
            // among the two closest elements
            else {

                // Get the element strictly less than target
                int lowerElement = arr[lowerBoundIndex - 1];

                // Get the element greater than or equal to target
                int upperElement = arr[lowerBoundIndex];

                // Return the closest element
                if (target - lowerElement <= upperElement - target) {
                    return lowerElement;
                } else {
                    return upperElement;
                }
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().closestElement(arr, target));
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
