---
title: "Build Staircase"
summary: "Given n coins, build a staircase where the ith stair needs i coins. Return the number of complete stairs."
prereqs:
  - 12-pattern-maximum-predicate-search/01-pattern
difficulty: medium
kind: problem
topics: [maximum-predicate-search, searching]
---

# Build Staircase

## Problem Statement

Given `n` coins, build a staircase where the `i`th stair needs `i` coins. Return the number of complete stairs you can build.

## Examples

**Example 1**
```
Input:  n = 6
Output: 3
Explanation: 1 + 2 + 3 = 6 coins, so 3 complete stairs.
```

**Example 2**
```
Input:  n = 5
Output: 2
Explanation: 1 + 2 = 3 coins builds 2 stairs; the 3rd stair needs 3 more coins (only 2 remain).
```

## Constraints

- `1 ≤ n ≤ 2^31 - 1`
- The answer fits in a 32-bit integer.

```python run viz=array
class Solution:
    def build_staircase(self, n: int) -> int:
        # Your code goes here — binary-search k in [0, n].
        # Predicate: k*(k+1)//2 <= n.
        # Return the largest k satisfying the predicate.
        return -1

n = int(input())
print(Solution().build_staircase(n))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int buildStaircase(int n) {
            // Your code goes here — binary-search k in [0, n].
            // Predicate: k*(k+1)/2 <= n.
            // Return the largest k satisfying the predicate.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().buildStaircase(n));
    }
}
```

```testcases
{
  "args": [
    { "id": "n", "label": "n", "type": "int", "placeholder": "6" }
  ],
  "cases": [
    { "args": { "n": "6" }, "expected": "3" },
    { "args": { "n": "5" }, "expected": "2" },
    { "args": { "n": "7" }, "expected": "3" },
    { "args": { "n": "0" }, "expected": "0" },
    { "args": { "n": "1" }, "expected": "1" },
    { "args": { "n": "3" }, "expected": "2" },
    { "args": { "n": "10" }, "expected": "4" },
    { "args": { "n": "11" }, "expected": "4" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The number of coins to build `k` stairs is `k*(k+1)/2` — the triangular number formula. This grows monotonically in `k`, so the predicate `k*(k+1)/2 ≤ n` is true for small `k` and false once `k` is too large. Binary-search `[0, n]` for the largest `k` that fits, recording the best candidate as the search moves right on success. The `+1` in the midpoint formula prevents an infinite loop when `lo` and `hi` are adjacent.

</details>
<details>
<summary><h2>The Solution</h2></summary>

Binary-search `k` in `[0, n]`. Predicate: `k(k+1)/2 <= n`.

```python solution time=O(log n) space=O(1)
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


n = int(input())
print(Solution().build_staircase(n))
```

```java solution
import java.util.*;

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
        Scanner sc = new Scanner(System.in);
        int n = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().buildStaircase(n));
    }
}
```

</details>
