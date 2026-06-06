---
title: "Minimum Shipping Capacity"
summary: "Given package weights weights[] (in order) and days, find the minimum ship capacity that allows all packages to be shipped within days. Packages must be shipped in input order."
prereqs:
  - 11-pattern-minimum-predicate-search/01-pattern
difficulty: medium
---

# Minimum Shipping Capacity

## The Problem

Given package weights `weights[]` (in order) and `days`, find the minimum ship capacity that allows all packages to be shipped within `days`. Packages must be shipped in input order.

```
Input:  weights = [20, 10, 25, 35], days = 3
Output: 35

Input:  weights = [20, 10, 40, 30], days = 3
Output: 40

Input:  weights = [6, 3, 9], days = 3
Output: 18
```

<details>
<summary><h2>The Solution</h2></summary>


Predicate: "can we ship within `days` days at capacity `cap`?" — greedy: sum weights into a bucket; when adding next would exceed `cap`, start a new day. Count days. Binary-search `cap` in `[max(weights), sum(weights)]`.


```python run viz=array viz-root=weights
from typing import List

class Solution:

    # Predicate: checks if it's possible to ship all packages within D days
    # given a maximum ship capacity
    def can_ship_capacity(
        self, weights: List[int], days: int, capacity: int
    ) -> bool:
        days_required = 1
        current_load = 0

        for weight in weights:

            # If a single package exceeds capacity, it's impossible
            if weight > capacity:
                return False

            # If adding this package doesn't exceed capacity, add it to
            # current day
            if current_load + weight <= capacity:
                current_load += weight
            else:

                # Otherwise, start a new day and put this package there
                days_required += 1
                current_load = weight

        # Return true if all packages can be shipped within D days
        return days_required <= days

    def minimum_shipping_capacity(
        self, weights: List[int], days: int
    ) -> int:

        # Minimum possible capacity is at least the heaviest package
        low = max(weights)

        # Maximum possible capacity is sum of all packages
        high = sum(weights)

        while low < high:

            # Calculate the middle capacity
            mid = low + (high - low) // 2

            # If it's possible, this is a potential answer
            # so try to find a smaller capacity
            if self.can_ship_capacity(weights, days, mid):

                # Try a smaller capacity
                high = mid

            # If it's not possible to ship with this capacity, try a
            # larger one
            else:

                # Try a larger capacity
                low = mid + 1

        # low is the minimum capacity to ship within D days
        return low


# Examples from the problem statement
print(Solution().minimum_shipping_capacity([20, 10, 25, 35], 3))  # 35
print(Solution().minimum_shipping_capacity([20, 10, 40, 30], 3))  # 40
print(Solution().minimum_shipping_capacity([6, 3, 9], 3))         # 18

# Edge cases
print(Solution().minimum_shipping_capacity([1], 1))               # 1   (single package)
print(Solution().minimum_shipping_capacity([5], 1))               # 5   (single heavy package)
print(Solution().minimum_shipping_capacity([1, 2, 3, 4, 5], 5))   # 5   (one package per day)
print(Solution().minimum_shipping_capacity([1, 2, 3, 4, 5], 1))   # 15  (all in one day)
print(Solution().minimum_shipping_capacity([3, 2, 2, 4, 1, 4], 3))  # 6
```

```java run viz=array viz-root=weights
import java.util.*;

public class Main {
    static class Solution {

        // Predicate: checks if it's possible to ship all packages within D
        // days given a maximum ship capacity
        private boolean canShipCapacity(
            int[] weights,
            int days,
            int capacity
        ) {
            int daysRequired = 1;
            int currentLoad = 0;

            for (int weight : weights) {

                // If a single package exceeds capacity, it's impossible
                if (weight > capacity) {
                    return false;
                }

                // If adding this package doesn't exceed capacity, add it to
                // current day
                if (currentLoad + weight <= capacity) {
                    currentLoad += weight;
                }

                // Otherwise, start a new day and put this package there
                else {
                    daysRequired++;
                    currentLoad = weight;
                }
            }

            // Return true if all packages can be shipped within D days
            return daysRequired <= days;
        }

        public int minimumShippingCapacity(int[] weights, int days) {

            // Minimum possible capacity is at least the heaviest package
            int low = Arrays.stream(weights).max().getAsInt();

            // Maximum possible capacity is sum of all packages
            int high = Arrays.stream(weights).sum();

            while (low < high) {

                // Calculate the middle capacity
                int mid = low + (high - low) / 2;

                // If it's possible, this is a potential answer
                // so try to find a smaller capacity
                if (canShipCapacity(weights, days, mid)) {

                    // Try a smaller capacity
                    high = mid;
                }

                // If it's not possible to ship with this capacity, try a
                // larger one
                else {

                    // Try a larger capacity
                    low = mid + 1;
                }
            }

            // low is the minimum capacity to ship within D days
            return low;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().minimumShippingCapacity(new int[]{20, 10, 25, 35}, 3));  // 35
        System.out.println(new Solution().minimumShippingCapacity(new int[]{20, 10, 40, 30}, 3));  // 40
        System.out.println(new Solution().minimumShippingCapacity(new int[]{6, 3, 9}, 3));         // 18

        // Edge cases
        System.out.println(new Solution().minimumShippingCapacity(new int[]{1}, 1));               // 1
        System.out.println(new Solution().minimumShippingCapacity(new int[]{5}, 1));               // 5
        System.out.println(new Solution().minimumShippingCapacity(new int[]{1, 2, 3, 4, 5}, 5));   // 5
        System.out.println(new Solution().minimumShippingCapacity(new int[]{1, 2, 3, 4, 5}, 1));   // 15
        System.out.println(new Solution().minimumShippingCapacity(new int[]{3, 2, 2, 4, 1, 4}, 3));  // 6
    }
}
```

</details>
