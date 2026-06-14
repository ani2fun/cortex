---
title: "First and Last Position"
summary: "Given a sorted array and target, return [first_index, last_index] of target, or [-1, -1] if absent."
prereqs:
  - 09-pattern-lower-bound/01-pattern
difficulty: medium
kind: problem
topics: [lower-bound, searching]
---

# First and Last Position

Two `O(log n)` queries — `lower_bound(target)` for the first index, `lower_bound(target + 1) - 1` for the last.

## Problem Statement

Given a sorted array and target, return `[first_index, last_index]` of target, or `[-1, -1]` if absent.

## Examples

**Example 1**
```
Input:  arr = [1, 2, 2, 2, 3, 4], target = 2
Output: [1, 3]
Explanation: 2 first appears at index 1 and last appears at index 3.
```

**Example 2**
```
Input:  arr = [1, 2, 2, 2, 3, 4], target = 5
Output: [-1, -1]
Explanation: 5 is not in the array, so both bounds are -1.
```

## Constraints

- `0 ≤ arr.length ≤ 10^5`
- `-10^9 ≤ arr[i], target ≤ 10^9`
- `arr` is sorted in non-decreasing order.

```python run viz=array viz-root=result
import ast
from typing import List

class Solution:
    def first_and_last_position(
        self, arr: List[int], target: int
    ) -> List[int]:
        # Your code goes here — call lower_bound(target) for first,
        # lower_bound(target+1) - 1 for last; return [-1,-1] if absent.
        return [-1, -1]

arr = ast.literal_eval(input())
target = int(input())
print(Solution().first_and_last_position(arr, target))
```

```java run viz=array viz-root=result
import java.util.*;

public class Main {
    static class Solution {
        public int[] firstAndLastPosition(int[] arr, int target) {
            // Your code goes here — call lowerBound(target) for first,
            // lowerBound(target+1) - 1 for last; return {-1,-1} if absent.
            return new int[]{-1, -1};
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(Arrays.toString(new Solution().firstAndLastPosition(arr, target)));
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 2, 2, 2, 3, 4]" },
    { "id": "target", "label": "target", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "arr": "[1, 2, 2, 2, 3, 4]", "target": "2" }, "expected": "[1, 3]" },
    { "args": { "arr": "[1, 2, 2, 2, 3, 4]", "target": "3" }, "expected": "[4, 4]" },
    { "args": { "arr": "[1, 2, 2, 2, 3, 4]", "target": "5" }, "expected": "[-1, -1]" },
    { "args": { "arr": "[5]", "target": "5" }, "expected": "[0, 0]" },
    { "args": { "arr": "[5]", "target": "3" }, "expected": "[-1, -1]" },
    { "args": { "arr": "[2, 2, 2, 2]", "target": "2" }, "expected": "[0, 3]" },
    { "args": { "arr": "[1, 2, 2, 2, 3, 4]", "target": "1" }, "expected": "[0, 0]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

Two lower-bound calls bracket the run. `lower_bound(target)` lands at the first element `>= target`; if that element *is* target, it's the first occurrence. `lower_bound(target + 1) - 1` lands one step left of the first element `> target`, which is the last occurrence. If the first call lands out-of-bounds or on a different value, target is absent. Both calls are `O(log n)`, and the trick of querying `target + 1` instead of writing a separate upper-bound routine keeps the code minimal.

</details>
<details>
<summary><h2>The Solution</h2></summary>

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

    def first_and_last_position(
        self, arr: List[int], target: int
    ) -> List[int]:

        # Initialize the result list with -1 values
        result = [-1, -1]

        # Find the first occurrence of target
        first: int = self.lower_bound(arr, target)

        # Find the lower bound of target+1 and subtract 1 to get the
        # last occurrence of target
        last: int = self.lower_bound(arr, target + 1) - 1

        # Check if the lower bound index is within the list bounds and if
        # the value is the target
        if first < len(arr) and arr[first] == target:

            # Return the range [first, last]
            return [first, last]

        # Return the default result list
        return result


arr = ast.literal_eval(input())
target = int(input())
print(Solution().first_and_last_position(arr, target))
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

        public int[] firstAndLastPosition(int[] arr, int target) {

            // Initialize the result array with -1 values
            int[] result = new int[] { -1, -1 };

            // Find the first occurrence of target
            int first = lowerBound(arr, target);

            // Find the lower bound of target+1 and subtract 1 to get the
            // last occurrence of target
            int last = lowerBound(arr, target + 1) - 1;

            // Check if the lower bound index is within the array bounds and
            // if the value is the target
            if (first < arr.length && arr[first] == target) {

                // Return the range [first, last]
                return new int[] { first, last };
            }

            // Return the default result array
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(Arrays.toString(new Solution().firstAndLastPosition(arr, target)));
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
