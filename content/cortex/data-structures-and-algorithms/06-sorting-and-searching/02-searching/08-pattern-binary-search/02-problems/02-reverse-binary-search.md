---
title: "Reverse Binary Search"
summary: "Given a descending-sorted array arr and target, return target's index, or -1."
prereqs:
  - 08-pattern-binary-search/01-pattern
difficulty: medium
kind: problem
topics: [binary-search, searching]
---

# Reverse Binary Search

## Problem Statement

Given a descending-sorted array `arr` and `target`, return target's index, or `-1`.

## Examples

**Example 1**
```
Input:  arr = [6, 5, 4, 3, 2, 1], target = 3
Output: 3
Explanation: 3 is at index 3. The array is descending, so "look left" when arr[mid] < target.
```

**Example 2**
```
Input:  arr = [6, 5, 4, 3, 2, 1], target = 10
Output: -1
Explanation: 10 is not in the array; the search space is exhausted.
```

## Constraints

- `0 ≤ arr.length ≤ 10^5`
- `-10^9 ≤ arr[i] ≤ 10^9`
- `arr` is sorted in strictly descending order.
- `-10^9 ≤ target ≤ 10^9`

```python run viz=array
import ast
from typing import List

class Solution:
    def reverse_binary_search(self, arr: List[int], target: int) -> int:
        # Your code goes here — same as binary search but flip the comparisons:
        # if arr[mid] < target, go LEFT (hi = mid - 1); else go RIGHT (lo = mid + 1).
        return -1

arr = ast.literal_eval(input())
target = int(input())
print(Solution().reverse_binary_search(arr, target))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int reverseBinarySearch(int[] arr, int target) {
            // Your code goes here — same as binary search but flip the comparisons:
            // if arr[mid] < target, go LEFT (high = mid - 1); else go RIGHT (low = mid + 1).
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().reverseBinarySearch(arr, target));
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[6, 5, 4, 3, 2, 1]" },
    { "id": "target", "label": "target", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "arr": "[6, 5, 4, 3, 2, 1]", "target": "3" }, "expected": "3" },
    { "args": { "arr": "[6, 5, 4, 3, 2, 1]", "target": "6" }, "expected": "0" },
    { "args": { "arr": "[6, 5, 4, 3, 2, 1]", "target": "10" }, "expected": "-1" },
    { "args": { "arr": "[5]", "target": "5" }, "expected": "0" },
    { "args": { "arr": "[5]", "target": "3" }, "expected": "-1" },
    { "args": { "arr": "[6, 5, 4, 3, 2, 1]", "target": "1" }, "expected": "5" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The array is sorted — just in the opposite direction. The standard binary search template stays intact; only the meaning of `arr[mid] < target` flips. In ascending order that means "target is to the right"; in descending order it means "target is to the left" (larger values live there). One comparison swap is all it takes, and the `O(log n)` guarantee is unchanged.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

The skeleton is identical to plain binary search — only the comparison logic flips. In ascending order: `arr[mid] < target` means "look right." In descending order: `arr[mid] < target` means "look *left*" (because larger values are on the left).


```python solution time=O(log n) space=O(1)
import ast
from typing import List

class Solution:
    def reverse_binary_search(self, arr: List[int], target: int) -> int:

        # Starting index of the search range (leftmost element)
        low = 0

        # Ending index of the search range (rightmost element)
        high = len(arr) - 1

        while low <= high:

            # Calculate the middle index to avoid potential overflow
            mid = low + (high - low) // 2

            # Found the target, return the index
            if arr[mid] == target:
                return mid

            # Since the array is sorted in descending order:
            # If arr[mid] is smaller than the target,
            # move to the left half (where larger elements are)
            elif arr[mid] < target:
                high = mid - 1

            # Else if arr[mid] is greater than the target,
            # move to the right half (where smaller elements are)
            else:
                low = mid + 1

        # Target not found in the array
        return -1


arr = ast.literal_eval(input())
target = int(input())
print(Solution().reverse_binary_search(arr, target))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int reverseBinarySearch(int[] arr, int target) {

            // Starting index of the search range (leftmost element)
            int low = 0;

            // Ending index of the search range (rightmost element)
            int high = arr.length - 1;

            while (low <= high) {

                // Calculate the middle index to avoid potential overflow
                int mid = low + (high - low) / 2;

                // Found the target, return the index
                if (arr[mid] == target) {
                    return mid;
                }

                // Since the array is sorted in descending order:
                // If arr[mid] is smaller than the target,
                // move to the left half (where larger elements are)
                else if (arr[mid] < target) {
                    high = mid - 1;
                }

                // Else if arr[mid] is greater than the target,
                // move to the right half (where smaller elements are)
                else {
                    low = mid + 1;
                }
            }

            // Target not found in the array
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().reverseBinarySearch(arr, target));
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

`O(log n)` time, `O(1)` space.

</details>
