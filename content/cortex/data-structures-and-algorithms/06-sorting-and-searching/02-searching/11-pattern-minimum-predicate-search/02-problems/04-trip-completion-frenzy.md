---
title: "Trip Completion Frenzy"
summary: "Given an array times[] (each plane's per-trip duration) and totalTrips, find the minimum time required for the planes (operating independently) to complete totalTrips trips total."
prereqs:
  - 11-pattern-minimum-predicate-search/01-pattern
difficulty: hard
---

# Trip Completion Frenzy

## The Problem

Given an array `times[]` (each plane's per-trip duration) and `totalTrips`, find the minimum time required for the planes (operating independently) to complete `totalTrips` trips total.

```
Input:  times = [3, 4, 5], totalTrips = 4
Output: 6

Input:  times = [1, 2, 3], totalTrips = 5
Output: 3

Input:  times = [1], totalTrips = 5
Output: 5
```

<details>
<summary><h2>The Solution</h2></summary>


Predicate: "in time `t`, can we complete `totalTrips`?" — each plane finishes `t / times[i]` trips. Sum and check ≥ totalTrips. Binary-search `t` in `[0, max(times) * totalTrips]` (the slowest plane finishing every trip alone is a safe upper bound).


```python run viz=array viz-root=times
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


# Examples from the problem statement
print(Solution().trip_completion_frenzy([3, 4, 5], 4))   # 6
print(Solution().trip_completion_frenzy([1, 2, 3], 5))   # 3
print(Solution().trip_completion_frenzy([1], 5))         # 5

# Edge cases
print(Solution().trip_completion_frenzy([1], 1))         # 1   (single plane, one trip)
print(Solution().trip_completion_frenzy([2], 3))         # 6   (single slow plane)
print(Solution().trip_completion_frenzy([1, 1], 4))      # 2   (two identical fast planes)
print(Solution().trip_completion_frenzy([5, 10], 1))     # 5   (fastest plane handles single trip)
print(Solution().trip_completion_frenzy([1, 2, 3], 10))  # 6
```

```java run viz=array viz-root=times
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
        // Examples from the problem statement
        System.out.println(new Solution().tripCompletionFrenzy(new int[]{3, 4, 5}, 4));   // 6
        System.out.println(new Solution().tripCompletionFrenzy(new int[]{1, 2, 3}, 5));   // 3
        System.out.println(new Solution().tripCompletionFrenzy(new int[]{1}, 5));         // 5

        // Edge cases
        System.out.println(new Solution().tripCompletionFrenzy(new int[]{1}, 1));         // 1
        System.out.println(new Solution().tripCompletionFrenzy(new int[]{2}, 3));         // 6
        System.out.println(new Solution().tripCompletionFrenzy(new int[]{1, 1}, 4));      // 2
        System.out.println(new Solution().tripCompletionFrenzy(new int[]{5, 10}, 1));     // 5
        System.out.println(new Solution().tripCompletionFrenzy(new int[]{1, 2, 3}, 10));  // 6
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The minimum-predicate-search pattern: when you're optimizing for the smallest value satisfying a monotonic predicate, binary-search the value range. The four problems showed four different predicate functions — "can-reach," "can-achieve-penalty," "can-ship," "can-complete-trips" — each independently a creative greedy/simulation, but all sharing the same outer binary-search shell.

The next lesson (the Maximum Predicate Search Pattern lesson) is the dual: **maximum-predicate-search** — when you're optimizing for the largest value satisfying a predicate that's true-then-false (instead of false-then-true).

</details>
