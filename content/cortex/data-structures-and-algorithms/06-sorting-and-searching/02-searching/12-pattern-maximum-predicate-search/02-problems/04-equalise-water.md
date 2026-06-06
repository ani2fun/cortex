---
title: "Equalise Water"
summary: "Given an array of bucket water amounts and a loss% for transfers, find the maximum equal-water level achievable across all buckets."
prereqs:
  - 12-pattern-maximum-predicate-search/01-pattern
difficulty: hard
---

# Equalise Water

## The Problem

Given an array of bucket water amounts and a `loss%` for transfers, find the maximum equal-water level achievable across all buckets.

```
Input:  buckets = [1, 5, 10], loss = 20
Output: 5.00000

Input:  buckets = [2, 4, 6], loss = 50
Output: 3.50000

Input:  buckets = [10, 10, 10, 10], loss = 40
Output: 10.00000
```

<details>
<summary><h2>The Solution</h2></summary>


Binary-search the target water level (scaled to avoid floating-point precision). Predicate: total available excess (after loss) ≥ total deficit. Use integer arithmetic with a scale factor of `1e5`.


```python run viz=array viz-root=buckets
from typing import List

class Solution:

    # Scale factor to avoid floating-point precision issues while
    # performing binary search. We scale all water amounts by 1e5
    # and perform integer arithmetic instead of floating-point.
    SCALE: int = int(1e5)

    # Predicate: checks if it's possible to make all buckets contain at
    # least 'target' liters of water
    def can_achieve_target(
        self, buckets: List[int], loss: float, target: int
    ) -> bool:
        total_excess = 0
        total_deficit = 0

        for water in buckets:

            # Scale the water amount to avoid floating-point precision
            water *= self.SCALE

            # If water in the bucket is more than the target, calculate
            # the excess
            if water > target:

                # Water that can be effectively transferred
                total_excess += ((water - target) * (100 - loss)) / 100
            else:

                # Water needed to fill this bucket
                total_deficit += target - water

        # We can achieve the target if total_excess is greater than or
        # equal to total_deficit
        return total_excess >= total_deficit

    def equalise_water(self, buckets: List[int], loss: float) -> float:

        # Binary search range is [0, max(bucket) * SCALE]
        low = 0
        high = max(buckets) * self.SCALE

        # Tolerance factor of 1e-5
        while low < high:

            # Calculate the middle target by adding 1 to get upper mid to
            # prevent infinite loop when low and high are adjacent
            mid = low + (high - low + 1) // 2

            # If we can achieve this target, this is a potential answer
            # So update the lower boundary to mid
            if self.can_achieve_target(buckets, loss, mid):

                # Try a larger target
                low = mid
            else:

                # Try a smaller target
                high = mid - 1

        return high / self.SCALE


# Examples from the problem statement
print(Solution().equalise_water([1, 5, 10], 20))       # 5.0
print(Solution().equalise_water([2, 4, 6], 50))        # 3.5
print(Solution().equalise_water([10, 10, 10, 10], 40)) # 10.0

# Edge cases
print(Solution().equalise_water([5], 0))               # 5.0  (single bucket)
print(Solution().equalise_water([5], 100))             # 5.0  (single bucket, any loss)
print(Solution().equalise_water([1, 1], 0))            # 1.0  (already equal, no loss)
print(Solution().equalise_water([0, 10], 0))           # 5.0  (no loss, perfect split)
print(Solution().equalise_water([0, 10], 100))         # 0.0  (100% loss — can't transfer)
```

```java run viz=array viz-root=buckets
import java.util.*;

public class Main {
    static class Solution {

        // Scale factor to avoid floating-point precision issues while
        // performing binary search. We scale all water amounts by 1e5
        // and perform integer arithmetic instead of floating-point.
        private final int SCALE = 100000;

        // Predicate: checks if it's possible to make all buckets contain at
        // least 'target' liters of water
        private boolean canAchieveTarget(
            int[] buckets,
            double loss,
            int target
        ) {
            long totalExcess = 0;
            long totalDeficit = 0;

            for (long water : buckets) {

                // Scale the water amount to avoid floating-point precision
                water *= SCALE;

                // If water in the bucket is more than the target, calculate
                // the excess
                if (water > target) {

                    // Water that can be effectively transferred
                    totalExcess +=
                    ((water - target) * (100 - (long) loss)) / 100;
                }

                // If water in the bucket is less than the target, calculate
                // the deficit
                else {

                    // Water needed to fill this bucket
                    totalDeficit += target - water;
                }
            }

            // We can achieve the target if totalExcess is greater than or
            // equal to totalDeficit
            return totalExcess >= totalDeficit;
        }

        public double equaliseWater(int[] buckets, double loss) {

            // Binary search range is [0, max(bucket) * SCALE]
            int low = 0;
            int high = Arrays.stream(buckets).max().getAsInt() * SCALE;

            // Tolerance factor of 1e-5
            while (low < high) {

                // Calculate the middle target by adding 1 to get upper mid
                // to prevent infinite loop when low and high are adjacent
                int mid = low + (high - low + 1) / 2;

                // If we can achieve this target, this is a potential answer
                // So update the lower boundary to mid
                if (canAchieveTarget(buckets, loss, mid)) {

                    // Try a larger target
                    low = mid;
                }

                // If we can't achieve this target, try for a smaller target
                else {
                    high = mid - 1;
                }
            }

            return (double) high / SCALE;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().equaliseWater(new int[]{1, 5, 10}, 20));       // 5.0
        System.out.println(new Solution().equaliseWater(new int[]{2, 4, 6}, 50));        // 3.5
        System.out.println(new Solution().equaliseWater(new int[]{10, 10, 10, 10}, 40)); // 10.0

        // Edge cases
        System.out.println(new Solution().equaliseWater(new int[]{5}, 0));               // 5.0
        System.out.println(new Solution().equaliseWater(new int[]{5}, 100));             // 5.0
        System.out.println(new Solution().equaliseWater(new int[]{1, 1}, 0));            // 1.0
        System.out.println(new Solution().equaliseWater(new int[]{0, 10}, 0));           // 5.0
        System.out.println(new Solution().equaliseWater(new int[]{0, 10}, 100));         // 0.0
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Maximum-predicate-search is the dual of the Minimum Predicate Search Pattern lesson. Same algorithm shell, mirrored direction; the `+ 1` in the mid calculation prevents the infinite-loop pitfall when `low` and `high` become adjacent. The four problems showed integer square root (predicate: `mid² ≤ num`), staircase building (`k(k+1)/2 ≤ n`), ribbon cutting, and water equalisation.

This closes the searching section. You came in with linear scan; you leave with binary search and its variants (lower bound, upper bound), 2D extensions (matrix search, staircase), broken-input handling (rotated array), and the binary-search-on-the-answer family (predicate search) — covering practically every searching problem you'll encounter.

The next major topic is **dynamic programming**. DP builds on memoization (introduced in the Recursion section) and on this section's "binary search on the answer" mindset: many DP problems can be reformulated as predicate searches, and many predicate searches benefit from DP-style state caching inside their predicate.

</details>
