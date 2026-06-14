---
title: "Calculate Square Root"
summary: "Given a non-negative integer num, return its integer square root (floor)."
prereqs:
  - 12-pattern-maximum-predicate-search/01-pattern
difficulty: easy
kind: problem
topics: [maximum-predicate-search, searching]
---

# Calculate Square Root

## Problem Statement

Given a non-negative integer `num`, return its integer square root (floor). Do not use any built-in square root function.

## Examples

**Example 1**
```
Input:  num = 4
Output: 2
Explanation: sqrt(4) = 2 exactly, so the floor is 2.
```

**Example 2**
```
Input:  num = 5
Output: 2
Explanation: sqrt(5) ≈ 2.236; the floor is 2.
```

## Constraints

- `0 ≤ num ≤ 2^31 - 1`
- Use integer arithmetic only (avoid `x*x` overflow with `x <= num // x`).

```python run viz=array
class Solution:
    def calculate_square_root(self, num: int) -> int:
        # Your code goes here — binary-search x in [1, num].
        # Predicate: x <= num // x (avoids overflow, equivalent to x*x <= num).
        # Return the largest x that satisfies the predicate; return 0 for num=0.
        return -1

num = int(input())
print(Solution().calculate_square_root(num))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int calculateSquareRoot(int num) {
            // Your code goes here — binary-search x in [1, num].
            // Predicate: x <= num / x (avoids overflow, equivalent to x*x <= num).
            // Return the largest x that satisfies it; return 0 for num=0.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().calculateSquareRoot(num));
    }
}
```

```testcases
{
  "args": [
    { "id": "num", "label": "num", "type": "int", "placeholder": "8" }
  ],
  "cases": [
    { "args": { "num": "4" }, "expected": "2" },
    { "args": { "num": "5" }, "expected": "2" },
    { "args": { "num": "50" }, "expected": "7" },
    { "args": { "num": "0" }, "expected": "0" },
    { "args": { "num": "1" }, "expected": "1" },
    { "args": { "num": "9" }, "expected": "3" },
    { "args": { "num": "100" }, "expected": "10" },
    { "args": { "num": "99" }, "expected": "9" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The integer square root is the largest `x` such that `x * x ≤ num` — a classic maximum-predicate problem. Feasibility (`x² ≤ num`) is monotone-decreasing in `x`: true for small values, false once `x` is too large. Binary-search `[1, num]` for the last `x` satisfying the predicate, tracking the best. Use `x ≤ num // x` (integer division) instead of `x * x ≤ num` to avoid overflow on large inputs.

</details>
<details>
<summary><h2>The Solution</h2></summary>

Binary-search `x` in `[1, num]`. Predicate: `x * x <= num` (use `x <= num / x` to avoid overflow).

```python solution time=O(log n) space=O(1)
class Solution:

    # Predicate: checks if square of mid is less than or equal to num
    def is_square(self, mid: int, num: int) -> bool:
        return mid <= num // mid

    def calculate_square_root(self, num: int) -> int:
        if num == 0:
            return 0

        # Lowest possible square root
        low = 1

        # Highest possible square root
        high = num

        while low < high:

            # Calculate the middle value by adding 1 to get upper mid to
            # prevent infinite loop when low and high are adjacent
            mid = low + (high - low + 1) // 2

            # If the square of mid is less than or equal to num, low is
            # a valid candidate. Update the lower boundary to mid
            if self.is_square(mid, num):
                low = mid

            # If the square of mid is greater than num, update the
            # higher boundary
            else:
                high = mid - 1

        # Return the last valid candidate rounded down to
        # the nearest integer
        return low


num = int(input())
print(Solution().calculate_square_root(num))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {

        // Predicate: checks if square of mid is less than or equal to num
        private boolean isSquare(int mid, int num) {

            // avoid overflow
            return mid <= num / mid;
        }

        public int calculateSquareRoot(int num) {
            if (num == 0) {
                return 0;
            }

            // Lowest possible square root
            int low = 1;

            // Highest possible square root
            int high = num;

            while (low < high) {

                // Calculate the middle value by adding 1 to get upper mid to
                // prevent infinite loop when low and high are adjacent
                int mid = low + (high - low + 1) / 2;

                // If the square of mid is less than or equal to num, low is
                // a valid candidate. Update the lower boundary to mid
                if (isSquare(mid, num)) {
                    low = mid;
                }

                // If the square of mid is greater than num, update the
                // higher boundary
                else {
                    high = mid - 1;
                }
            }

            // Return the last valid candidate rounded down to
            // the nearest integer
            return low;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().calculateSquareRoot(num));
    }
}
```

</details>
