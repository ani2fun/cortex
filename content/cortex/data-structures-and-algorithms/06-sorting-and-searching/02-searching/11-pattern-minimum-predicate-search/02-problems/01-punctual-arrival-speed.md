---
title: "Punctual Arrival Speed"
summary: "Given bus distances distance[] and time budget hour, find the minimum integer speed at which all buses must travel for arrival within hour. Each bus departs at the next integer time after the previous"
prereqs:
  - 11-pattern-minimum-predicate-search/01-pattern
difficulty: medium
---

# Punctual Arrival Speed

## The Problem

Given bus distances `distance[]` and time budget `hour`, find the minimum integer speed at which all buses must travel for arrival within `hour`. Each bus departs at the next integer time after the previous bus arrives. Return `-1` if impossible.

```
Input:  distance = [1, 3, 5], hour = 2.5
Output: 10

Input:  distance = [1, 4, 9], hour = 6
Output: 3

Input:  distance = [1, 8, 10], hour = 2
Output: -1
```

<details>
<summary><h2>The Solution</h2></summary>


Predicate `can_reach(speed)`: simulate the rides; for the last leg, partial time counts; for prior legs, you must round up to the next integer. Binary-search speed in `[1, 10^7]`.


```python run viz=array viz-root=distance
import math
from typing import List

class Solution:

    # Predicate: checks if it's possible to reach the destination on
    # time with a given speed
    def can_reach_on_time(
        self, distance: List[int], hour: float, speed: int
    ) -> bool:
        total_time = 0

        # Calculate the total time required to reach each checkpoint
        for i in range(len(distance) - 1):
            total_time += math.ceil(distance[i] / speed)

        # Add the time required to reach the final destination
        total_time += distance[-1] / speed

        # Check if the total time is less than or equal to the given hour
        return total_time <= hour

    def punctual_arrival_speed(
        self, distance: List[int], hour: float
    ) -> int:

        # Initialise the search space for speed with low as 1
        low: int = 1

        # Initialise high to a large value (1e7) as per problem
        # constraints
        high: int = int(1e7)

        # Perform binary search to find the minimum speed required to
        # reach the destination on time
        while low < high:

            # Find the middle speed to check if it is possible to reach
            # the destination on time
            mid = low + (high - low) // 2

            # mid is a possible speed, update the result and search
            # for a smaller speed
            if self.can_reach_on_time(distance, hour, mid):

                # Try to find a smaller speed
                high = mid

            # mid is not a possible speed, search for a larger speed
            else:

                # Try to find a larger speed
                low = mid + 1

        # After the search, low is the candidate minimum speed
        # Check if it actually works, as it could be possible that no
        # speed allows reaching on time
        if not self.can_reach_on_time(distance, hour, low):
            return -1

        # Return the minimum speed found
        return low


# Examples from the problem statement
print(Solution().punctual_arrival_speed([1, 3, 5], 2.5))   # 10
print(Solution().punctual_arrival_speed([1, 4, 9], 6))     # 3
print(Solution().punctual_arrival_speed([1, 8, 10], 2))    # -1

# Edge cases
print(Solution().punctual_arrival_speed([1], 1))           # 1   (single ride, exactly on time)
print(Solution().punctual_arrival_speed([5], 1))           # 5   (single ride, speed = distance)
print(Solution().punctual_arrival_speed([1, 1, 1], 3))     # 1   (minimal speed)
print(Solution().punctual_arrival_speed([1, 2], 1.9))      # -1  (impossible — need integer depart)
print(Solution().punctual_arrival_speed([10, 10, 10], 5))  # 10  (last ride is exact)
```

```java run viz=array viz-root=distance
public class Main {
    static class Solution {

        // Predicate: checks if it's possible to reach the destination on
        // time with a given speed
        private boolean canReachOnTime(
            int[] distance,
            double hour,
            int speed
        ) {
            double totalTime = 0;

            // Calculate the total time required to reach each checkpoint
            for (int i = 0; i < distance.length - 1; i++) {
                totalTime += Math.ceil((double) distance[i] / speed);
            }

            // Add the time required to reach the final destination
            totalTime += (double) distance[distance.length - 1] / speed;

            // Check if the total time is less than or equal to the given
            // hour
            return totalTime <= hour;
        }

        public int punctualArrivalSpeed(int[] distance, double hour) {

            // Initialise the search space for speed with low as 1
            int low = 1;

            // Initialise high to a large value (1e7) as per problem
            // constraints
            int high = (int) 1e7;

            // Perform binary search to find the minimum speed required to
            // reach the destination on time
            while (low < high) {

                // Find the middle speed to check if it is possible to reach
                // the destination on time
                int mid = low + (high - low) / 2;

                // mid is a possible speed, update the result and search
                // for a smaller speed
                if (canReachOnTime(distance, hour, mid)) {

                    // Try to find a smaller speed
                    high = mid;
                }

                // mid is not a possible speed, search for a larger speed
                else {

                    // Try to find a larger speed
                    low = mid + 1;
                }
            }

            // After the search, low is the candidate minimum speed
            // Check if it actually works, as it could be possible that no
            // speed allows reaching on time
            if (!canReachOnTime(distance, hour, low)) {
                return -1;
            }

            // Return the minimum speed found
            return low;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().punctualArrivalSpeed(new int[]{1, 3, 5}, 2.5));   // 10
        System.out.println(new Solution().punctualArrivalSpeed(new int[]{1, 4, 9}, 6));     // 3
        System.out.println(new Solution().punctualArrivalSpeed(new int[]{1, 8, 10}, 2));    // -1

        // Edge cases
        System.out.println(new Solution().punctualArrivalSpeed(new int[]{1}, 1));           // 1
        System.out.println(new Solution().punctualArrivalSpeed(new int[]{5}, 1));           // 5
        System.out.println(new Solution().punctualArrivalSpeed(new int[]{1, 1, 1}, 3));     // 1
        System.out.println(new Solution().punctualArrivalSpeed(new int[]{1, 2}, 1.9));      // -1
        System.out.println(new Solution().punctualArrivalSpeed(new int[]{10, 10, 10}, 5));  // 10
    }
}
```

</details>
