---
title: "Punctual Arrival Speed"
summary: "Given bus distances distance[] and a time budget hour, find the minimum integer speed at which all buses arrive within hour. Each bus departs at the next integer time after the previous arrives; return -1 if impossible."
prereqs:
  - 11-pattern-minimum-predicate-search/01-pattern
difficulty: medium
kind: problem
topics: [minimum-predicate-search, searching]
---

# Punctual Arrival Speed

## Problem Statement

Given bus distances `distance[]` and time budget `hour`, find the minimum integer speed at which all buses must travel for arrival within `hour`. Each bus departs at the next integer time after the previous bus arrives. Return `-1` if impossible.

```
Input:  distance = [1, 3, 5], hour = 2.5
Output: 10

Input:  distance = [1, 4, 9], hour = 6
Output: 3

Input:  distance = [1, 8, 10], hour = 2
Output: -1
```

---

## Examples

**Example 1**
```
Input:  distance = [1, 3, 5], hour = 2.5
Output: 10
Explanation: At speed 10 the first two legs round up to 1h each (ceil(1/10), ceil(3/10)) and the last leg takes 5/10 = 0.5h → 2.5h total, exactly on budget.
```

**Example 2**
```
Input:  distance = [1, 8, 10], hour = 2
Output: -1
Explanation: The two forced integer departures alone cost ≥ 2h before the final leg, so no speed fits within 2h.
```

## Constraints

- `1 ≤ distance.length ≤ 10^5`
- `1 ≤ distance[i] ≤ 10^5`
- `1 ≤ hour ≤ 10^9` (a real number with at most two decimal places)
- The answer, if it exists, is at most `10^7`.

```python run viz=array viz-root=distance
import ast
from typing import List

class Solution:
    def punctual_arrival_speed(
        self, distance: List[int], hour: float
    ) -> int:
        # Your code goes here — binary-search the integer speed in [1, 1e7].
        # For a candidate speed, every leg but the last rounds its time up to a
        # whole hour. Return the minimum feasible speed, or -1 if none works.
        return -1


distance = ast.literal_eval(input())   # the test case's distances
hour = float(input())                  # the test case's time budget
print(Solution().punctual_arrival_speed(distance, hour))
```

```java run viz=array viz-root=distance
import java.util.*;

public class Main {
    static class Solution {
        public int punctualArrivalSpeed(int[] distance, double hour) {
            // Your code goes here — binary-search the integer speed in [1, 1e7].
            // For a candidate speed, every leg but the last rounds its time up
            // to a whole hour. Return the minimum feasible speed, or -1 if none.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] distance = parseIntArray(sc.nextLine());
        double hour = Double.parseDouble(sc.nextLine().trim());
        System.out.println(new Solution().punctualArrivalSpeed(distance, hour));
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
    { "id": "distance", "label": "distance", "type": "int[]", "placeholder": "[1, 3, 5]" },
    { "id": "hour", "label": "hour", "type": "number", "placeholder": "2.5" }
  ],
  "cases": [
    { "args": { "distance": "[1, 3, 5]", "hour": "2.5" }, "expected": "10" },
    { "args": { "distance": "[1, 4, 9]", "hour": "6" }, "expected": "3" },
    { "args": { "distance": "[1, 8, 10]", "hour": "2" }, "expected": "-1" },
    { "args": { "distance": "[5]", "hour": "1" }, "expected": "5" },
    { "args": { "distance": "[1, 2]", "hour": "1.9" }, "expected": "3" },
    { "args": { "distance": "[10, 10, 10]", "hour": "5" }, "expected": "10" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


Feasibility is **monotonic in speed**: if some speed gets every bus there in time, so does any faster speed; if a speed is too slow, so is everything slower. That `false…false true…true` boundary over the candidate speeds is exactly the structure binary search exploits — search the integer speed in `[1, 10^7]` for the first one that satisfies the time budget. The only subtlety is the cost model: every leg but the last must wait for an integer departure, so its travel time rounds **up** to a whole hour; the final leg counts its fractional time. If even the maximum speed can't fit, the forced integer waits make it impossible — return `-1`.

</details>
<details>
<summary><h2>The Solution</h2></summary>


Predicate `can_reach(speed)`: simulate the rides; for the last leg, partial time counts; for prior legs, you must round up to the next integer. Binary-search speed in `[1, 10^7]`.


```python solution time=O(n log(1e7)) space=O(1)
import ast
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


distance = ast.literal_eval(input())   # the test case's distances
hour = float(input())                  # the test case's time budget
print(Solution().punctual_arrival_speed(distance, hour))
```

```java solution
import java.util.*;

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
        Scanner sc = new Scanner(System.in);
        int[] distance = parseIntArray(sc.nextLine());
        double hour = Double.parseDouble(sc.nextLine().trim());
        System.out.println(new Solution().punctualArrivalSpeed(distance, hour));
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
