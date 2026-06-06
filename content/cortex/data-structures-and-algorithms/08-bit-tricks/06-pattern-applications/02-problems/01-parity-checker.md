---
title: "Parity Checker"
summary: "Given an integer, return "odd" if it's odd, "even" if it's even."
prereqs:
  - 06-pattern-applications/01-pattern
difficulty: easy
---

# Parity Checker

## The Problem

Given an integer, return `"odd"` if it's odd, `"even"` if it's even.

```
Input:  num = 10  →  "even"
Input:  num = 9   →  "odd"
Input:  num = 1   →  "odd"
```

<details>
<summary><h2>The Recurrence</h2></summary>


The least significant bit *is* the parity. `n & 1` returns 1 for odd numbers, 0 for even.

```
parity = "odd" if (n & 1) else "even"
```

> *Pause. Why does this work for negative numbers in two's complement? Predict.*

In two's complement, `-1`'s bit pattern is all-1s, `-2`'s is all-1s except the LSB, and so on. The LSB still alternates between 0 (even) and 1 (odd) as the magnitude grows — same as for positives. So `n & 1` is parity-preserving for any signed integer.

Compare to `n % 2`: for negative numbers in C and similar languages, `(-3) % 2 = -1` (signed remainder), which fails the `== 1` test. `n & 1` always returns `0` or `1`. Use the bitwise check; sidestep the language-specific signed-modulo trap.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array
class Solution:
    def parity_checker(self, num: int) -> str:

        # Bitwise AND operation with 1 to check if num is odd
        if num & 1:

            # If num is odd, return "odd"
            return "odd"
        else:

            # If num is even, return "even"
            return "even"


# Examples from the problem statement
print(Solution().parity_checker(10))     # even
print(Solution().parity_checker(9))      # odd
print(Solution().parity_checker(1))      # odd

# Edge cases
print(Solution().parity_checker(0))      # even
print(Solution().parity_checker(2))      # even
print(Solution().parity_checker(-1))     # odd
print(Solution().parity_checker(-2))     # even
print(Solution().parity_checker(100))    # even
```

```java run viz=array
public class Main {
    static class Solution {
        public String parityChecker(int num) {

            // Bitwise AND operation with 1 to check if num is odd
            if ((num & 1) == 1) {

                // If num is odd, return "odd"
                return "odd";
            } else {

                // If num is even, return "even"
                return "even";
            }
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().parityChecker(10));     // even
        System.out.println(new Solution().parityChecker(9));      // odd
        System.out.println(new Solution().parityChecker(1));      // odd

        // Edge cases
        System.out.println(new Solution().parityChecker(0));      // even
        System.out.println(new Solution().parityChecker(2));      // even
        System.out.println(new Solution().parityChecker(-1));     // odd
        System.out.println(new Solution().parityChecker(-2));     // even
        System.out.println(new Solution().parityChecker(100));    // even
    }
}
```

</details>
