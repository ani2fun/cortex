---
title: "Trip Completion Frenzy"
summary: "Given an array times[] (each plane's per-trip duration) and totalTrips, find the minimum time required for the planes (operating independently) to complete totalTrips trips total."
prereqs:
  - 11-pattern-minimum-predicate-search/01-pattern
difficulty: hard
kind: problem
topics: [minimum-predicate-search, searching]
---

# Trip Completion Frenzy

## Problem Statement

Given an array `times[]` (each plane's per-trip duration) and `totalTrips`, find the minimum time required for the planes (operating independently) to complete `totalTrips` trips total.

```
Input:  times = [3, 4, 5], totalTrips = 4
Output: 6

Input:  times = [1, 2, 3], totalTrips = 5
Output: 3

Input:  times = [1], totalTrips = 5
Output: 5
```

---

## Examples

**Example 1**
```
Input:  times = [3, 4, 5], totalTrips = 4
Output: 6
Explanation: At time 6 the planes complete 2 + 1 + 1 = 4 trips (floor(6/3)=2, floor(6/4)=1, floor(6/5)=1), meeting the target.
```

**Example 2**
```
Input:  times = [1, 2, 3], totalTrips = 5
Output: 3
Explanation: At time 3 the planes complete 3 + 1 + 1 = 5 trips (floor(3/1)=3, floor(3/2)=1, floor(3/3)=1), exactly meeting the target.
```

## Constraints

- `1 ≤ times.length ≤ 10^5`
- `1 ≤ times[i] ≤ 10^7`
- `1 ≤ totalTrips ≤ 10^7`

```python run viz=array viz-root=times
import ast

class Solution:
    def trip_completion_frenzy(self, times, total_trips):
        # Your code goes here — binary-search the time in [0, max(times) * totalTrips].
        # For a candidate time t, each plane with duration d completes t // d trips.
        # Return the minimum t at which the total trips >= totalTrips.
        return -1


times = ast.literal_eval(input())
total_trips = int(input())
print(Solution().trip_completion_frenzy(times, total_trips))
```

```java run viz=array viz-root=times
import java.util.*;

public class Main {
    static class Solution {
        public int tripCompletionFrenzy(int[] times, int totalTrips) {
            // Your code goes here — binary-search the time in [0, max(times) * totalTrips].
            // For a candidate time t, each plane with duration d completes t / d trips.
            // Return the minimum t at which the total trips >= totalTrips.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] times = parseIntArray(sc.nextLine());
        int totalTrips = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().tripCompletionFrenzy(times, totalTrips));
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
    { "id": "times", "label": "times", "type": "int[]", "placeholder": "[3, 4, 5]" },
    { "id": "totalTrips", "label": "totalTrips", "type": "int", "placeholder": "4" }
  ],
  "cases": [
    { "args": { "times": "[3, 4, 5]", "totalTrips": "4" }, "expected": "6" },
    { "args": { "times": "[1, 2, 3]", "totalTrips": "5" }, "expected": "3" },
    { "args": { "times": "[1]", "totalTrips": "5" }, "expected": "5" },
    { "args": { "times": "[1]", "totalTrips": "1" }, "expected": "1" },
    { "args": { "times": "[2]", "totalTrips": "3" }, "expected": "6" },
    { "args": { "times": "[1, 1]", "totalTrips": "4" }, "expected": "2" },
    { "args": { "times": "[5, 10]", "totalTrips": "1" }, "expected": "5" },
    { "args": { "times": "[1, 2, 3]", "totalTrips": "10" }, "expected": "6" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The number of trips completed is monotone in time — the more time elapses, the more trips every plane can fit. This gives a clean `false…false, true…true` boundary over candidate times, which binary search can exploit. The answer range is `[0, max(times) × totalTrips]` (the slowest plane finishing all trips alone), and the predicate is `sum(t // times[i]) >= totalTrips`. Finding the first time that crosses the threshold is a standard lower-bound search.

</details>
<details>
<summary><h2>The Solution</h2></summary>


Predicate: "in time `t`, can we complete `totalTrips`?" — each plane finishes `t / times[i]` trips. Sum and check ≥ totalTrips. Binary-search `t` in `[0, max(times) * totalTrips]` (the slowest plane finishing every trip alone is a safe upper bound).


```python solution time=O(n log(max*totalTrips)) space=O(1)
import ast
from typing import List

class Solution:

    # Predicate: checks if it's possible to complete at least
    # 'totalTrips' in 'time' time
    def can_complete_trips(
        self, times: List[int], total_trips: int, time: int
    ) -> bool:
        trips_completed = 0
        for t in times:

            # Calculate how many trips each plane can complete in 'time'
            trips_completed += time // t

        # Check if the total trips are enough
        return trips_completed >= total_trips

    def trip_completion_frenzy(
        self, times: List[int], total_trips: int
    ) -> int:

        # The minimum time required is 0
        low = 0

        # The high boundary is the maximum time taken by any plane
        high = max(times) * total_trips

        while low < high:

            # Calculate the middle time
            mid = low + (high - low) // 2

            # If we can complete the trips in 'mid' time, try for
            # smaller time
            if self.can_complete_trips(times, total_trips, mid):

                # Try a smaller time
                high = mid

            # If we can't complete the trips, try larger time
            else:

                # Try a larger time
                low = mid + 1

        # After the search, low is the minimum time required
        return low


times = ast.literal_eval(input())
total_trips = int(input())
print(Solution().trip_completion_frenzy(times, total_trips))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {

        // Predicate: checks if it's possible to complete at least
        // 'totalTrips' in 'time' time
        private boolean canCompleteTrips(
            int[] times,
            int totalTrips,
            int time
        ) {
            int tripsCompleted = 0;
            for (int t : times) {

                // Calculate how many trips each plane can complete in 'time'
                tripsCompleted += time / t;
            }

            // Check if the total trips are enough
            return tripsCompleted >= totalTrips;
        }

        public int tripCompletionFrenzy(int[] times, int totalTrips) {

            // The minimum time required is 0
            int low = 0;

            // The high boundary is the maximum time taken by any plane
            int high = Arrays.stream(times).max().getAsInt() * totalTrips;

            while (low < high) {

                // Calculate the middle time
                int mid = low + (high - low) / 2;

                // If we can complete the trips in 'mid' time, try for
                // smaller time
                if (canCompleteTrips(times, totalTrips, mid)) {

                    // Try a smaller time
                    high = mid;
                }

                // If we can't complete the trips, try larger time
                else {

                    // Try a larger time
                    low = mid + 1;
                }
            }

            // After the search, low is the minimum time required
            return low;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] times = parseIntArray(sc.nextLine());
        int totalTrips = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().tripCompletionFrenzy(times, totalTrips));
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


The minimum-predicate-search pattern: when you're optimizing for the smallest value satisfying a monotonic predicate, binary-search the value range. The four problems showed four different predicate functions — "can-reach," "can-achieve-penalty," "can-ship," "can-complete-trips" — each independently a creative greedy/simulation, but all sharing the same outer binary-search shell.

The next lesson (the Maximum Predicate Search Pattern lesson) is the dual: **maximum-predicate-search** — when you're optimizing for the largest value satisfying a predicate that's true-then-false (instead of false-then-true).

</details>
