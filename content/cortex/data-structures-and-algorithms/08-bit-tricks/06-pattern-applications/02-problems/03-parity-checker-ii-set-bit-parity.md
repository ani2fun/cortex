---
title: "Parity Checker II — Set-Bit Parity"
summary: "Given an integer num, return "odd" if its set bit count is odd, "even" otherwise. (This is *bit parity*, distinct from numerical parity from earlier.)"
prereqs:
  - 06-pattern-applications/01-pattern
difficulty: medium
---

# Parity Checker II — Set-Bit Parity

## The Problem

Given an integer `num`, return `"odd"` if its **set bit count** is odd, `"even"` otherwise. (This is *bit parity*, distinct from numerical parity from earlier.)

```
Input:  num = 10   →  "even"   Binary 1010 — 2 set bits → even
Input:  num = 13   →  "odd"    Binary 1101 — 3 set bits → odd
Input:  num = 1    →  "odd"    1 set bit → odd
```

<details>
<summary><h2>The Recurrence</h2></summary>


Use **Brian Kernighan's algorithm** from lesson 4. Each `num & (num - 1)` clears one set bit; toggle a parity flag each iteration. After the loop, the flag's final state is the parity.

```
parity = false
while num != 0:
    parity = not parity
    num = num & (num - 1)
return "odd" if parity else "even"
```

> *Pause. Why iterate <code>n & (n - 1)</code> instead of just shifting and counting <code>n & 1</code>?*

Both work. Kernighan's runs in O(set-bit count); the shift-and-count loop runs in O(bit-width). For sparse integers (few set bits), Kernighan's is much faster. CPUs also expose a `popcount` instruction that's faster than either; in production, prefer the intrinsic. The manual version here illustrates the technique.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array
class Solution:
    def parity_checker_ii(self, num: int) -> str:

        # Initialize the parity flag as False (even).
        parity: bool = False

        while num:

            # Toggle the parity flag for every 1 encountered.
            parity = not parity

            # Clear the least significant bit (LSB) of num.
            num = num & (num - 1)

        # If the parity flag is True, return "odd".
        if parity:
            return "odd"

        # If the parity flag is False, return "even".
        else:
            return "even"


# Examples from the problem statement
print(Solution().parity_checker_ii(10))    # even
print(Solution().parity_checker_ii(13))    # odd
print(Solution().parity_checker_ii(1))     # odd

# Edge cases
print(Solution().parity_checker_ii(0))     # even
print(Solution().parity_checker_ii(7))     # odd
print(Solution().parity_checker_ii(15))    # even
print(Solution().parity_checker_ii(3))     # even
print(Solution().parity_checker_ii(5))     # even
```

```java run viz=array
public class Main {
    static class Solution {
        public String parityCheckerII(int num) {

            // Initialize the parity flag as false (even).
            boolean parity = false;

            while (num != 0) {

                // Toggle the parity flag for every 1 encountered.
                parity = !parity;

                // Clear the least significant bit (LSB) of num.
                num = num & (num - 1);
            }

            // If the parity flag is true, return "odd".
            if (parity) {
                return "odd";

                // If the parity flag is false, return "even".
            } else {
                return "even";
            }
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().parityCheckerII(10));    // even
        System.out.println(new Solution().parityCheckerII(13));    // odd
        System.out.println(new Solution().parityCheckerII(1));     // odd

        // Edge cases
        System.out.println(new Solution().parityCheckerII(0));     // even
        System.out.println(new Solution().parityCheckerII(7));     // odd
        System.out.println(new Solution().parityCheckerII(15));    // even
        System.out.println(new Solution().parityCheckerII(3));     // even
        System.out.println(new Solution().parityCheckerII(5));     // even
    }
}
```

</details>
