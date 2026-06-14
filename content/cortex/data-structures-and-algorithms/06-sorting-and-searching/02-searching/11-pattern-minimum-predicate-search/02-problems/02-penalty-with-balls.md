---
title: "Penalty with Balls"
summary: "Given bag sizes bags[] and maxOperations, you can split any bag into two non-zero parts (counts as one operation). Minimize the largest bag size after at most maxOperations splits."
prereqs:
  - 11-pattern-minimum-predicate-search/01-pattern
difficulty: medium
kind: problem
topics: [minimum-predicate-search, searching]
---

# Penalty With Balls

## Problem Statement

Given bag sizes `bags[]` and `maxOperations`, you can split any bag into two non-zero parts (counts as one operation). Minimize the largest bag size after at most `maxOperations` splits.

```
Input:  bags = [9, 7, 6], maxOperations = 3
Output: 5

Input:  bags = [4, 8], maxOperations = 1
Output: 4

Input:  bags = [4, 2], maxOperations = 4
Output: 1
```

---

## Examples

**Example 1**
```
Input:  bags = [9, 7, 6], maxOperations = 3
Output: 5
Explanation: Split 9 → (5,4) and 7 → (5,2) and 6 → (5,1) using 3 operations; the largest bag is now 5.
```

**Example 2**
```
Input:  bags = [4, 8], maxOperations = 1
Output: 4
Explanation: With one split we can break 8 → (4,4); the largest remaining bag is 4.
```

## Constraints

- `1 ≤ bags.length ≤ 10^5`
- `1 ≤ bags[i] ≤ 10^9`
- `1 ≤ maxOperations ≤ 10^9`

```python run viz=array viz-root=bags
import ast

class Solution:
    def penalty_with_balls(self, bags, max_operations):
        # Your code goes here — binary-search the penalty (max bag size) in [1, max(bags)].
        # For a candidate penalty, count how many splits are needed to reduce every bag
        # to at most that size. Return the minimum feasible penalty.
        return -1


bags = ast.literal_eval(input())
max_operations = int(input())
print(Solution().penalty_with_balls(bags, max_operations))
```

```java run viz=array viz-root=bags
import java.util.*;

public class Main {
    static class Solution {
        public int penaltyWithBalls(int[] bags, int maxOperations) {
            // Your code goes here — binary-search the penalty (max bag size) in [1, max(bags)].
            // For a candidate penalty, count how many splits are needed to reduce every bag
            // to at most that size. Return the minimum feasible penalty.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] bags = parseIntArray(sc.nextLine());
        int maxOperations = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().penaltyWithBalls(bags, maxOperations));
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
    { "id": "bags", "label": "bags", "type": "int[]", "placeholder": "[9, 7, 6]" },
    { "id": "maxOperations", "label": "maxOperations", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "bags": "[9, 7, 6]", "maxOperations": "3" }, "expected": "5" },
    { "args": { "bags": "[4, 8]", "maxOperations": "1" }, "expected": "4" },
    { "args": { "bags": "[4, 2]", "maxOperations": "4" }, "expected": "1" },
    { "args": { "bags": "[1]", "maxOperations": "0" }, "expected": "1" },
    { "args": { "bags": "[1]", "maxOperations": "5" }, "expected": "1" },
    { "args": { "bags": "[10]", "maxOperations": "1" }, "expected": "5" },
    { "args": { "bags": "[6, 6]", "maxOperations": "0" }, "expected": "6" },
    { "args": { "bags": "[2, 4, 6]", "maxOperations": "6" }, "expected": "2" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The largest possible bag size after operations is monotone: if we can achieve a maximum bag size of `p`, we can also achieve any larger size with fewer or equal operations. This `false…false, true…true` shape over candidate penalties lets us binary-search `[1, max(bags)]` for the smallest feasible penalty. For each candidate, a bag of size `b > penalty` needs exactly `ceil(b / penalty) - 1 = (b-1) // penalty` splits, and we check whether the total fits within `maxOperations`.

</details>
<details>
<summary><h2>The Solution</h2></summary>


Predicate: "can we achieve max-bag-size = penalty?" — a bag of size `b > penalty` needs `(b - 1) // penalty` splits. Sum and check against `maxOperations`. Binary-search penalty in `[1, max(bags)]`.


```python solution time=O(n log(max(bags))) space=O(1)
import ast
from typing import List

class Solution:

    # Predicate: checks if it's possible to achieve a given penalty
    # (max number of balls in any bag)
    def can_achieve_penalty(
        self, bags: List[int], max_operations: int, penalty: int
    ) -> bool:
        operations = 0
        for balls in bags:

            # If a bag has more than 'penalty' balls, we need to split
            # it. The number of splits required for a bag with 'balls'
            # is (balls - 1) / penalty
            if balls > penalty:

                # This is the number of splits required
                operations += (balls - 1) // penalty

        # Check if we can do the splits within max_operations
        return operations <= max_operations

    def penalty_with_balls(
        self, bags: List[int], max_operations: int
    ) -> int:

        # The minimum penalty is at least 1 ball in a bag
        low = 1

        # The maximum penalty is the maximum number of balls in a
        # single bag
        high = max(bags)

        while low < high:

            # Calculate the middle penalty
            mid = low + (high - low) // 2

            # If we can achieve this penalty, this is a potential answer
            # so try to find a smaller one
            if self.can_achieve_penalty(bags, max_operations, mid):

                # Try a smaller penalty
                high = mid

            # If we can't achieve this penalty, try a larger one
            else:

                # Try a larger penalty
                low = mid + 1

        # After the search, low is the minimum penalty achievable
        return low


bags = ast.literal_eval(input())
max_operations = int(input())
print(Solution().penalty_with_balls(bags, max_operations))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {

        // Predicate: checks if it's possible to achieve a given penalty
        // (max number of balls in any bag)
        private boolean canAchievePenalty(
            int[] bags,
            int maxOperations,
            int penalty
        ) {
            int operations = 0;
            for (int balls : bags) {

                // If a bag has more than 'penalty' balls, we need to split
                // it. The number of splits required for a bag with 'balls'
                // is (balls - 1) / penalty
                if (balls > penalty) {

                    // This is the number of splits required
                    operations += (balls - 1) / penalty;
                }
            }

            // Check if we can do the splits within maxOperations
            return operations <= maxOperations;
        }

        public int penaltyWithBalls(int[] bags, int maxOperations) {

            // The minimum penalty is at least 1 ball in a bag
            int low = 1;

            // The maximum penalty is the maximum number of balls in a
            // single bag
            int high = Arrays.stream(bags).max().getAsInt();

            while (low < high) {

                // Calculate the middle penalty
                int mid = low + (high - low) / 2;

                // If we can achieve this penalty, this is a potential answer
                // so try to find a smaller one
                if (canAchievePenalty(bags, maxOperations, mid)) {

                    // Try a smaller penalty
                    high = mid;
                }

                // If we can't achieve this penalty, try a larger one
                else {

                    // Try a larger penalty
                    low = mid + 1;
                }
            }

            // After the search, low is the minimum penalty achievable
            return low;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] bags = parseIntArray(sc.nextLine());
        int maxOperations = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().penaltyWithBalls(bags, maxOperations));
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
