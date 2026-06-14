---
title: "Equalise Water"
summary: "Given an array of bucket water amounts and a loss% for transfers, find the maximum equal-water level achievable across all buckets."
prereqs:
  - 12-pattern-maximum-predicate-search/01-pattern
difficulty: hard
kind: problem
topics: [maximum-predicate-search, searching]
---

# Equalise Water

## Problem Statement

Given an array of bucket water amounts and an integer `loss` (percentage lost during each transfer), find the maximum equal-water level achievable across all buckets. Water can be poured from buckets that have more than the target into buckets that have less, but `loss%` is wasted in each such transfer.

## Examples

**Example 1**
```
Input:  buckets = [1, 5, 10], loss = 20
Output: 5.0
Explanation: The two full buckets (5 and 10) have excess. Excess from bucket[2] after 20% loss
             can fill bucket[0]'s deficit at level 5.0.
```

**Example 2**
```
Input:  buckets = [2, 4, 6], loss = 50
Output: 3.5
Explanation: At 3.5, bucket[2] has 2.5 excess → after 50% loss only 1.25 usable;
             deficit = (3.5-2) + (3.5-4) = 1.5 + 0 = 1.5 — checking shows 3.5 is the highest feasible level.
```

## Constraints

- `1 ≤ buckets.length ≤ 10^4`
- `0 ≤ buckets[i] ≤ 10^5`
- `0 ≤ loss ≤ 99` (integer percentage)
- The answer is accurate to `10^-5`.

```python run viz=array viz-root=buckets
import ast
from typing import List

class Solution:
    def equalise_water(self, buckets: List[int], loss: int) -> float:
        # Your code goes here — binary-search the target level scaled by 1e5
        # to stay in integer space. Predicate: total_excess*(1-loss/100) >= total_deficit.
        return -1.0

buckets = ast.literal_eval(input())
loss = int(input())
print(Solution().equalise_water(buckets, loss))
```

```java run viz=array viz-root=buckets
import java.util.*;

public class Main {
    static class Solution {
        public double equaliseWater(int[] buckets, int loss) {
            // Your code goes here — binary-search the target level scaled by 1e5
            // to stay in integer space. Predicate: totalExcess*(1-loss/100) >= totalDeficit.
            return -1.0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] buckets = parseIntArray(sc.nextLine());
        int loss = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().equaliseWater(buckets, loss));
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
    { "id": "buckets", "label": "buckets", "type": "int[]", "placeholder": "[1, 5, 10]" },
    { "id": "loss", "label": "loss %", "type": "int", "placeholder": "20" }
  ],
  "cases": [
    { "args": { "buckets": "[1, 5, 10]", "loss": "20" }, "expected": "5.0" },
    { "args": { "buckets": "[2, 4, 6]", "loss": "50" }, "expected": "3.5" },
    { "args": { "buckets": "[10, 10, 10, 10]", "loss": "40" }, "expected": "10.0" },
    { "args": { "buckets": "[5]", "loss": "0" }, "expected": "5.0" },
    { "args": { "buckets": "[5]", "loss": "100" }, "expected": "5.0" },
    { "args": { "buckets": "[1, 1]", "loss": "0" }, "expected": "1.0" },
    { "args": { "buckets": "[0, 10]", "loss": "0" }, "expected": "5.0" },
    { "args": { "buckets": "[0, 10]", "loss": "100" }, "expected": "0.0" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The question is: what is the highest level `T` such that the water available from buckets above `T` (after `loss%` transfer waste) covers all the deficit from buckets below `T`? As `T` grows, deficits grow and surpluses shrink — feasibility is monotone-decreasing. Binary-search the answer in integer space by scaling by `10^5` (avoiding floating-point drift), then divide back at the end. The predicate runs in `O(n)` and the binary search over `max(buckets)*10^5` steps costs `O(log(max*10^5))`.

</details>
<details>
<summary><h2>The Solution</h2></summary>

Binary-search the target water level (scaled to avoid floating-point precision). Predicate: total available excess (after loss) ≥ total deficit. Use integer arithmetic with a scale factor of `1e5`.

```python solution time=O(n log(max*1e5)) space=O(1)
import ast
from typing import List

class Solution:

    # Scale factor to avoid floating-point precision issues while
    # performing binary search. We scale all water amounts by 1e5
    # and perform integer arithmetic instead of floating-point.
    SCALE: int = int(1e5)

    # Predicate: checks if it's possible to make all buckets contain at
    # least 'target' liters of water
    def can_achieve_target(
        self, buckets: List[int], loss: int, target: int
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

    def equalise_water(self, buckets: List[int], loss: int) -> float:

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


buckets = ast.literal_eval(input())
loss = int(input())
print(Solution().equalise_water(buckets, loss))
```

```java solution
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
            int loss,
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

        public double equaliseWater(int[] buckets, int loss) {

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
        Scanner sc = new Scanner(System.in);
        int[] buckets = parseIntArray(sc.nextLine());
        int loss = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().equaliseWater(buckets, loss));
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
<details>
<summary><h2>Key Takeaway</h2></summary>

Maximum-predicate-search is the dual of the Minimum Predicate Search Pattern lesson. Same algorithm shell, mirrored direction; the `+ 1` in the mid calculation prevents the infinite-loop pitfall when `low` and `high` become adjacent. The four problems showed integer square root (predicate: `mid² ≤ num`), staircase building (`k(k+1)/2 ≤ n`), ribbon cutting, and water equalisation.

This closes the searching section. You came in with linear scan; you leave with binary search and its variants (lower bound, upper bound), 2D extensions (matrix search, staircase), broken-input handling (rotated array), and the binary-search-on-the-answer family (predicate search) — covering practically every searching problem you'll encounter.

The next major topic is **dynamic programming**. DP builds on memoization (introduced in the Recursion section) and on this section's "binary search on the answer" mindset: many DP problems can be reformulated as predicate searches, and many predicate searches benefit from DP-style state caching inside their predicate.

</details>
