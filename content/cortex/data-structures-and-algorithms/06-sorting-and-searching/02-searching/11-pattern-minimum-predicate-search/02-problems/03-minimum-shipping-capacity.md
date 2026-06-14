---
title: "Minimum Shipping Capacity"
summary: "Given package weights weights[] (in order) and days, find the minimum ship capacity that allows all packages to be shipped within days. Packages must be shipped in input order."
prereqs:
  - 11-pattern-minimum-predicate-search/01-pattern
difficulty: medium
kind: problem
topics: [minimum-predicate-search, searching]
---

# Minimum Shipping Capacity

## Problem Statement

Given package weights `weights[]` (in order) and `days`, find the minimum ship capacity that allows all packages to be shipped within `days`. Packages must be shipped in input order.

```
Input:  weights = [20, 10, 25, 35], days = 3
Output: 35

Input:  weights = [20, 10, 40, 30], days = 3
Output: 40

Input:  weights = [6, 3, 9], days = 3
Output: 9   (was 18 in source — corrected by execution)
```

---

## Examples

**Example 1**
```
Input:  weights = [20, 10, 25, 35], days = 3
Output: 35
Explanation: With capacity 35 we can load [20,10] on day 1, [25] on day 2, [35] on day 3 — exactly 3 days.
```

**Example 2**
```
Input:  weights = [6, 3, 9], days = 3
Output: 9
Explanation: With capacity 9 we load [6,3] on day 1 (sum=9 ≤ 9) and [9] on day 2 — only 2 days needed, well within the 3-day budget. The minimum feasible capacity is 9 (the heaviest package, which must fit in a single shipment).
```

## Constraints

- `1 ≤ weights.length ≤ 5 × 10^4`
- `1 ≤ weights[i] ≤ 500`
- `1 ≤ days ≤ weights.length`

```python run viz=array viz-root=weights
import ast

class Solution:
    def minimum_shipping_capacity(self, weights, days):
        # Your code goes here — binary-search the capacity in [max(weights), sum(weights)].
        # For a candidate capacity, greedily pack packages in order; when the next
        # package would overflow, start a new day. Return the minimum feasible capacity.
        return -1


weights = ast.literal_eval(input())
days = int(input())
print(Solution().minimum_shipping_capacity(weights, days))
```

```java run viz=array viz-root=weights
import java.util.*;

public class Main {
    static class Solution {
        public int minimumShippingCapacity(int[] weights, int days) {
            // Your code goes here — binary-search the capacity in [max(weights), sum(weights)].
            // For a candidate capacity, greedily pack packages in order; when the next
            // package would overflow, start a new day. Return the minimum feasible capacity.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] weights = parseIntArray(sc.nextLine());
        int days = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().minimumShippingCapacity(weights, days));
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
    { "id": "weights", "label": "weights", "type": "int[]", "placeholder": "[20, 10, 25, 35]" },
    { "id": "days", "label": "days", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "weights": "[20, 10, 25, 35]", "days": "3" }, "expected": "35" },
    { "args": { "weights": "[20, 10, 40, 30]", "days": "3" }, "expected": "40" },
    { "args": { "weights": "[6, 3, 9]", "days": "3" }, "expected": "9" },
    { "args": { "weights": "[1]", "days": "1" }, "expected": "1" },
    { "args": { "weights": "[5]", "days": "1" }, "expected": "5" },
    { "args": { "weights": "[1, 2, 3, 4, 5]", "days": "5" }, "expected": "5" },
    { "args": { "weights": "[1, 2, 3, 4, 5]", "days": "1" }, "expected": "15" },
    { "args": { "weights": "[3, 2, 2, 4, 1, 4]", "days": "3" }, "expected": "6" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The capacity needed is monotone: below the minimum feasible capacity we can't finish in time, above it we can — a clean `false…false, true…true` structure. Binary-search `[max(weights), sum(weights)]`: the lower bound because every package must fit on a single day, and the upper bound because one day suffices for everything. For each candidate capacity, a greedy simulation (keep adding packages until the next would overflow, then start a new day) tells us how many days are needed.

</details>
<details>
<summary><h2>The Solution</h2></summary>


Predicate: "can we ship within `days` days at capacity `cap`?" — greedy: sum weights into a bucket; when adding next would exceed `cap`, start a new day. Count days. Binary-search `cap` in `[max(weights), sum(weights)]`.


```python solution time=O(n log(sum(weights))) space=O(1)
import ast
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


weights = ast.literal_eval(input())
days = int(input())
print(Solution().minimum_shipping_capacity(weights, days))
```

```java solution
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
        Scanner sc = new Scanner(System.in);
        int[] weights = parseIntArray(sc.nextLine());
        int days = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().minimumShippingCapacity(weights, days));
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
