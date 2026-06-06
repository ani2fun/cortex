---
title: "Build Staircase"
summary: "Given n coins, build a staircase where the ith stair needs i coins. Return the number of complete stairs."
prereqs:
  - 12-pattern-maximum-predicate-search/01-pattern
difficulty: medium
---

# Build Staircase

## The Problem

Given `n` coins, build a staircase where the `i`th stair needs `i` coins. Return the number of complete stairs.

```
Input:  n = 6
Output: 3   (1 + 2 + 3 = 6)

Input:  n = 5
Output: 2   (1 + 2 = 3; can't build 3rd stair)

Input:  n = 7
Output: 3   (1 + 2 + 3 = 6; 1 coin left over, not enough for 4th)
```

<details>
<summary><h2>The Solution</h2></summary>


Binary-search `k` in `[0, n]`. Predicate: `k(k+1)/2 <= n`.


```python run viz=array
class Solution:

    # Predicate: Checks if mid rows can fit within n blocks
    def can_build(self, mid: int, n: int) -> bool:

        # sum of first mid natural numbers: mid*(mid+1)/2
        return mid * (mid + 1) // 2 <= n

    def build_staircase(self, n: int) -> int:

        # Lowest possible value for a complete row
        low = 0

        # Highest possible value for a complete row
        high = n

        while low < high:

            # Calculate the middle value by adding 1 to get upper mid to
            # prevent infinite loop when low and high are adjacent
            mid = low + (high - low + 1) // 2

            # If we can build mid rows, this is a possible answer
            # Update the lower boundary to mid
            if self.can_build(mid, n):
                low = mid

            # The sum is larger, search in the left half
            else:
                high = mid - 1

        # Return the largest complete row smaller than the given sum
        return low


# Examples from the problem statement
print(Solution().build_staircase(6))   # 3
print(Solution().build_staircase(5))   # 2
print(Solution().build_staircase(7))   # 3

# Edge cases
print(Solution().build_staircase(0))   # 0   (no coins)
print(Solution().build_staircase(1))   # 1   (exactly one stair)
print(Solution().build_staircase(2))   # 1   (1 coin short of 2 stairs)
print(Solution().build_staircase(3))   # 2   (1+2=3 coins)
print(Solution().build_staircase(10))  # 4   (1+2+3+4=10)
print(Solution().build_staircase(11))  # 4   (11 coins, 5th stair needs 5)
```

```java run viz=array
public class Main {
    static class Solution {

        // Predicate: Checks if mid rows can fit within n blocks
        private boolean canBuild(int mid, int n) {

            // sum of first mid natural numbers: mid*(mid+1)/2
            return (mid * (mid + 1)) / 2 <= n;
        }

        public int buildStaircase(int n) {

            // Lowest possible value for a complete row
            int low = 0;

            // Highest possible value for a complete row
            int high = n;

            while (low < high) {

                // Calculate the middle value by adding 1 to get upper mid to
                // prevent infinite loop when low and high are adjacent
                int mid = low + (high - low + 1) / 2;

                // If we can build mid rows, this is a possible answer
                // Update the lower boundary to mid
                if (canBuild(mid, n)) {
                    low = mid;
                }

                // The sum is larger, search in the left half
                else {
                    high = mid - 1;
                }
            }

            // Return the largest complete row smaller than the given sum
            return low;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().buildStaircase(6));   // 3
        System.out.println(new Solution().buildStaircase(5));   // 2
        System.out.println(new Solution().buildStaircase(7));   // 3

        // Edge cases
        System.out.println(new Solution().buildStaircase(0));   // 0
        System.out.println(new Solution().buildStaircase(1));   // 1
        System.out.println(new Solution().buildStaircase(2));   // 1
        System.out.println(new Solution().buildStaircase(3));   // 2
        System.out.println(new Solution().buildStaircase(10));  // 4
        System.out.println(new Solution().buildStaircase(11));  // 4
    }
}
```

</details>
