---
title: "K Ribbons"
summary: "Array of ribbons. Cut them to produce at least k pieces of equal length. Return the *maximum* such length, or 0 if impossible."
prereqs:
  - 12-pattern-maximum-predicate-search/01-pattern
difficulty: medium
---

# K Ribbons

## The Problem

Array of ribbons. Cut them to produce at least `k` pieces of equal length. Return the *maximum* such length, or `0` if impossible.

```
Input:  ribbons = [9, 7, 5], k = 3
Output: 5

Input:  ribbons = [9, 7, 5], k = 4
Output: 4

Input:  ribbons = [9, 7, 5], k = 30
Output: 0
```

<details>
<summary><h2>The Solution</h2></summary>


Predicate: "can we cut at least `k` ribbons of length `length`?" — sum of `r // length` for each ribbon. Binary-search `length` in `[1, 10^7]` (a safe ceiling from the problem constraints — wider than `max(ribbons)` but still `O(log range)`). After the loop, re-check the predicate at `low` to distinguish "no length works" (return `0`) from a real answer.


```python run viz=array viz-root=ribbons
from typing import List

class Solution:

    # Predicate: checks if it's possible to cut at least 'k' ribbons of
    # length 'length'
    def can_cut(self, ribbons: List[int], length: int, k: int) -> bool:
        count = 0

        # Count how many pieces of 'length' we can cut from each ribbon
        for ribbon in ribbons:
            count += ribbon // length

        # Return true if we can cut at least 'k' ribbons of this length
        return count >= k

    def k_ribbons(self, ribbons: List[int], k: int) -> int:

        # Initialize the search range for ribbon lengths
        low = 1

        # Initialize the search range for ribbon lengths
        high = int(1e7)

        while low < high:

            # Calculate the middle value by adding 1 to get upper mid to
            # prevent infinite loop when low and high are adjacent
            mid = low + (high - low + 1) // 2

            # If we can cut at least 'k' ribbons of length 'mid' it is a
            # possible answer, so update the lower boundary to mid
            if self.can_cut(ribbons, mid, k):

                # Try to find a larger length
                low = mid

            # Otherwise, we can't cut 'k' ribbons of length 'mid' from
            # the given ribbons array.
            else:

                # Try to find a smaller length
                high = mid - 1

        # After the search, low is the maximum length we can cut
        # Check if we can actually cut at least 'k' ribbons of this
        # length
        if not self.can_cut(ribbons, low, k):
            return 0

        # Return the maximum ribbon length that can be obtained
        return low


# Examples from the problem statement
print(Solution().k_ribbons([9, 7, 5], 3))    # 5
print(Solution().k_ribbons([9, 7, 5], 4))    # 4
print(Solution().k_ribbons([9, 7, 5], 30))   # 0

# Edge cases
print(Solution().k_ribbons([1], 1))          # 1   (single ribbon, single cut)
print(Solution().k_ribbons([10], 1))         # 10  (whole ribbon)
print(Solution().k_ribbons([10], 3))         # 3   (10//3=3 ribbons of length 3)
print(Solution().k_ribbons([5, 5, 5], 3))    # 5   (each ribbon exactly k length)
print(Solution().k_ribbons([1, 2, 3], 10))   # 0   (not enough total length)
```

```java run viz=array viz-root=ribbons
public class Main {
    static class Solution {

        // Predicate: checks if it's possible to cut at least 'k' ribbons of
        // length 'length'
        private boolean canCut(int[] ribbons, int length, int k) {
            int count = 0;

            // Count how many pieces of 'length' we can cut from each ribbon
            for (int ribbon : ribbons) {
                count += ribbon / length;
            }

            // Return true if we can cut at least 'k' ribbons of this length
            return count >= k;
        }

        public int kRibbons(int[] ribbons, int k) {

            // Initialize the search range for ribbon lengths
            int low = 1;

            // Initialize the search range for ribbon lengths
            int high = (int) 1e7;

            while (low < high) {

                // Calculate the middle value by adding 1 to get upper mid to
                // prevent infinite loop when low and high are adjacent
                int mid = low + (high - low + 1) / 2;

                // If we can cut at least 'k' ribbons of length 'mid' it is a
                // possible answer, so update the lower boundary to mid
                if (canCut(ribbons, mid, k)) {

                    // Try to find a larger length
                    low = mid;
                }

                // Otherwise, we can't cut 'k' ribbons of length 'mid' from
                // the given ribbons array.
                else {

                    // Try to find a smaller length
                    high = mid - 1;
                }
            }

            // After the search, low is the maximum length we can cut
            // Check if we can actually cut at least 'k' ribbons of this
            // length
            if (!canCut(ribbons, low, k)) {
                return 0;
            }

            // Return the maximum ribbon length that can be obtained
            return low;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().kRibbons(new int[]{9, 7, 5}, 3));    // 5
        System.out.println(new Solution().kRibbons(new int[]{9, 7, 5}, 4));    // 4
        System.out.println(new Solution().kRibbons(new int[]{9, 7, 5}, 30));   // 0

        // Edge cases
        System.out.println(new Solution().kRibbons(new int[]{1}, 1));          // 1
        System.out.println(new Solution().kRibbons(new int[]{10}, 1));         // 10
        System.out.println(new Solution().kRibbons(new int[]{10}, 3));         // 3
        System.out.println(new Solution().kRibbons(new int[]{5, 5, 5}, 3));    // 5
        System.out.println(new Solution().kRibbons(new int[]{1, 2, 3}, 10));   // 0
    }
}
```

</details>
