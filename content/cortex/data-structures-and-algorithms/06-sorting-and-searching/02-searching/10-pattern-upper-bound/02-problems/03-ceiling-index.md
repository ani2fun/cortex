---
title: "Ceiling Index"
summary: "Sorted array arr and a list of queries. For each query, return the smallest index i with arr[i] > query. Return -1 for queries with no such index."
prereqs:
  - 10-pattern-upper-bound/01-pattern
difficulty: medium
kind: problem
topics: [upper-bound, searching]
---

# Ceiling Index

## Problem Statement

Sorted array `arr` and a list of `queries`. For each query, return the smallest index `i` with `arr[i] > query`. Return `-1` for queries with no such index.

## Examples

**Example 1**
```
Input:  arr = [1, 4, 7], queries = [2, 4]
Output: [1, 2]
Explanation: For query 2: arr[1]=4 is the first element > 2, index 1. For query 4: arr[2]=7 is the first element > 4, index 2.
```

**Example 2**
```
Input:  arr = [5], queries = [6]
Output: [-1]
Explanation: No element in arr is greater than 6; return -1 for this query.
```

## Constraints

- `1 ≤ arr.length ≤ 10^4`
- `1 ≤ queries.length ≤ 10^4`
- `-10^9 ≤ arr[i], queries[j] ≤ 10^9`
- `arr` is sorted in ascending order.

```python run viz=array viz-root=result
import ast
from typing import List

class Solution:
    def ceiling_index(self, arr: List[int], queries: List[int]) -> List[int]:
        # Your code goes here — for each query, run upper_bound(arr, query)
        # to find the first index where arr[i] > query; if result == len(arr)
        # append -1, otherwise append the index.
        return []

arr = ast.literal_eval(input())
queries = ast.literal_eval(input())
print(Solution().ceiling_index(arr, queries))
```

```java run viz=array viz-root=result
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> ceilingIndex(int[] arr, int[] queries) {
            // Your code goes here — for each query, run upperBound(arr, query)
            // to find the first index where arr[i] > query; if result == arr.length
            // add -1, otherwise add the index.
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int[] queries = parseIntArray(sc.nextLine());
        System.out.println(new Solution().ceilingIndex(arr, queries));
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 4, 7]" },
    { "id": "queries", "label": "queries", "type": "int[]", "placeholder": "[2, 4]" }
  ],
  "cases": [
    { "args": { "arr": "[1, 4, 7]", "queries": "[2, 4]" }, "expected": "[1, 2]" },
    { "args": { "arr": "[5]", "queries": "[2]" }, "expected": "[0]" },
    { "args": { "arr": "[5]", "queries": "[6]" }, "expected": "[-1]" },
    { "args": { "arr": "[1, 4, 7]", "queries": "[7]" }, "expected": "[-1]" },
    { "args": { "arr": "[1, 4, 7]", "queries": "[0]" }, "expected": "[0]" },
    { "args": { "arr": "[2, 2, 2]", "queries": "[1]" }, "expected": "[0]" },
    { "args": { "arr": "[2, 2, 2]", "queries": "[2]" }, "expected": "[-1]" },
    { "args": { "arr": "[1, 3, 5, 7]", "queries": "[2, 4, 6, 8]" }, "expected": "[1, 2, 3, -1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

`upper_bound(query)` finds the first index where `arr[i] > query` in `O(log n)`. That is exactly the definition of the ceiling index. Run it once per query; if the result equals `len(arr)` there is no element larger than the query, so return `-1`; otherwise return the index directly. The problem is a direct application of upper bound with no additional logic.

</details>
<details>
<summary><h2>The Solution</h2></summary>


Run `upper_bound` for each query. Return `-1` if the result is `n`.


```python solution time=O(Q log N) space=O(Q)
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

    def ceiling_index(
        self, arr: List[int], queries: List[int]
    ) -> List[int]:

        # Resultant list to store ceiling indices for each query
        result: List[int] = []

        # Iterate through each query
        for query in queries:

            # Find the upper bound index for the query
            upperBoundIndex: int = self.upper_bound(arr, query)

            # If upperBoundIndex is equal to length of arr, there is no
            # element greater than or equal to query
            if upperBoundIndex == len(arr):
                result.append(-1)

            # Otherwise, upperBoundIndex is the ceiling index
            else:
                result.append(upperBoundIndex)

        # Return the final result list containing ceiling indices for all
        # queries
        return result


arr = ast.literal_eval(input())
queries = ast.literal_eval(input())
print(Solution().ceiling_index(arr, queries))
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

        public List<Integer> ceilingIndex(int[] arr, int[] queries) {

            // Result list to store ceiling index for each query
            List<Integer> result = new ArrayList<>();

            // Iterate through each query
            for (int query : queries) {

                // Find the upper bound index for the current query
                int upperBoundIndex = upperBound(arr, query);

                // If upperBoundIndex is equal to arr.size(), it means there
                // is no element greater than or equal to query, so we append
                // -1
                if (upperBoundIndex == arr.length) {
                    result.add(-1);
                }

                // Otherwise, upperBoundIndex is the ceiling index
                else {
                    result.add(upperBoundIndex);
                }
            }

            // Return the result list containing ceiling indices for all
            // queries
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int[] queries = parseIntArray(sc.nextLine());
        System.out.println(new Solution().ceilingIndex(arr, queries));
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
