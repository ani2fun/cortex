---
title: "Ceiling Index"
summary: "Sorted array arr and a list of queries. For each query, return the smallest index i with arr[i] > query. Return -1 for queries with no such index."
prereqs:
  - 10-pattern-upper-bound/01-pattern
difficulty: medium
---

# Ceiling Index

## The Problem

Sorted array `arr` and a list of `queries`. For each query, return the smallest index `i` with `arr[i] > query`. Return `-1` for queries with no such index.

```
Input:  arr = [1, 4, 7], queries = [2, 4]
Output: [1, 2]

Input:  arr = [5], queries = [2]
Output: [0]

Input:  arr = [5], queries = [6]
Output: [-1]
```

<details>
<summary><h2>The Solution</h2></summary>


Run `upper_bound` for each query. Return `-1` if the result is `n`.


```python run viz=array viz-root=result
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


# Examples from the problem statement
print(Solution().ceiling_index([1, 4, 7], [2, 4]))  # [1, 2]
print(Solution().ceiling_index([5], [2]))            # [0]
print(Solution().ceiling_index([5], [6]))            # [-1]

# Edge cases
print(Solution().ceiling_index([1, 4, 7], [7]))      # [-1]  (query equals last element)
print(Solution().ceiling_index([1, 4, 7], [0]))      # [0]   (query less than all)
print(Solution().ceiling_index([2, 2, 2], [1]))      # [0]   (duplicates, query below)
print(Solution().ceiling_index([2, 2, 2], [2]))      # [-1]  (duplicates, all equal — no strictly greater)
print(Solution().ceiling_index([1, 3, 5, 7], [2, 4, 6, 8]))  # [1, 2, 3, -1]
```

```java run viz=array viz-root=result
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
        // Examples from the problem statement
        System.out.println(new Solution().ceilingIndex(new int[]{1, 4, 7}, new int[]{2, 4}));  // [1, 2]
        System.out.println(new Solution().ceilingIndex(new int[]{5}, new int[]{2}));            // [0]
        System.out.println(new Solution().ceilingIndex(new int[]{5}, new int[]{6}));            // [-1]

        // Edge cases
        System.out.println(new Solution().ceilingIndex(new int[]{1, 4, 7}, new int[]{7}));      // [-1]
        System.out.println(new Solution().ceilingIndex(new int[]{1, 4, 7}, new int[]{0}));      // [0]
        System.out.println(new Solution().ceilingIndex(new int[]{2, 2, 2}, new int[]{1}));      // [0]
        System.out.println(new Solution().ceilingIndex(new int[]{2, 2, 2}, new int[]{2}));      // [-1]
        System.out.println(new Solution().ceilingIndex(new int[]{1, 3, 5, 7}, new int[]{2, 4, 6, 8}));  // [1, 2, 3, -1]
    }
}
```

</details>
