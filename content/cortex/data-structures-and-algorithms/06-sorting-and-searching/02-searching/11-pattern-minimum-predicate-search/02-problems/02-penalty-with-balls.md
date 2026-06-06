---
title: "Penalty with Balls"
summary: "Given bag sizes bags[] and maxOperations, you can split any bag into two non-zero parts (counts as one operation). Minimize the largest bag size after at most maxOperations splits."
prereqs:
  - 11-pattern-minimum-predicate-search/01-pattern
difficulty: medium
---

# Penalty With Balls

## The Problem

Given bag sizes `bags[]` and `maxOperations`, you can split any bag into two non-zero parts (counts as one operation). Minimize the largest bag size after at most `maxOperations` splits.

```
Input:  bags = [9, 7, 6], maxOperations = 3
Output: 5

Input:  bags = [4, 8], maxOperations = 1
Output: 4

Input:  bags = [4, 2], maxOperations = 4
Output: 1
```

<details>
<summary><h2>The Solution</h2></summary>


Predicate: "can we achieve max-bag-size = penalty?" — a bag of size `b > penalty` needs `(b - 1) // penalty` splits. Sum and check against `maxOperations`. Binary-search penalty in `[1, max(bags)]`.


```python run viz=array viz-root=bags
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


# Examples from the problem statement
print(Solution().penalty_with_balls([9, 7, 6], 3))   # 5
print(Solution().penalty_with_balls([4, 8], 1))      # 4
print(Solution().penalty_with_balls([4, 2], 4))      # 1

# Edge cases
print(Solution().penalty_with_balls([1], 0))         # 1   (single bag, no ops)
print(Solution().penalty_with_balls([1], 5))         # 1   (already size 1)
print(Solution().penalty_with_balls([10], 1))        # 5   (split once: [5,5])
print(Solution().penalty_with_balls([6, 6], 0))      # 6   (no operations)
print(Solution().penalty_with_balls([2, 4, 6], 6))   # 1   (enough ops for penalty=1)
```

```java run viz=array viz-root=bags
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
        // Examples from the problem statement
        System.out.println(new Solution().penaltyWithBalls(new int[]{9, 7, 6}, 3));   // 5
        System.out.println(new Solution().penaltyWithBalls(new int[]{4, 8}, 1));      // 4
        System.out.println(new Solution().penaltyWithBalls(new int[]{4, 2}, 4));      // 1

        // Edge cases
        System.out.println(new Solution().penaltyWithBalls(new int[]{1}, 0));         // 1
        System.out.println(new Solution().penaltyWithBalls(new int[]{1}, 5));         // 1
        System.out.println(new Solution().penaltyWithBalls(new int[]{10}, 1));        // 5
        System.out.println(new Solution().penaltyWithBalls(new int[]{6, 6}, 0));      // 6
        System.out.println(new Solution().penaltyWithBalls(new int[]{2, 4, 6}, 6));   // 1
    }
}
```

</details>
