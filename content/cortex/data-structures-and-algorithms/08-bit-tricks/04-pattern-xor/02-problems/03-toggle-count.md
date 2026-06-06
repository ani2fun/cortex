---
title: "Toggle Count"
summary: "Given two integers num1 and num2, return the number of bits that need to be flipped to convert one into the other."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: easy
---

# Toggle Count

## The Problem

Given two integers `num1` and `num2`, return the number of bits that need to be flipped to convert one into the other.

```
Input:  num1 = 10, num2 = 1   →  3       Binary 1010 vs 0001 — 3 differing positions
Input:  num1 = 2, num2 = 3    →  1       Binary 10 vs 11 — only LSB differs
```

<details>
<summary><h2>The Recurrence — XOR Then Popcount</h2></summary>


`num1 ^ num2` has a 1 in every position where `num1` and `num2` *differ*. Counting set bits in the XOR gives the number of differing positions — exactly the toggle count.

The set-bit count uses **Brian Kernighan's algorithm**: repeatedly clear the lowest set bit (`n & (n - 1)`) until zero, counting iterations. Faster than scanning all 32 positions when the bit count is small.

> *Pause. Why is Brian Kernighan's algorithm faster than a 32-position scan?*

It runs in **O(set-bit count)** rather than O(bit-width). For sparse integers (few bits set), this is much faster — `n = 1` runs one iteration vs. 32. For dense integers it's about the same. CPUs also have a `popcount` instruction that's faster still; we use the manual version here for clarity.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array
class Solution:
    def toggle_count(self, num1: int, num2: int) -> int:

        # take XOR of num1 and num2 and store in num
        num: int = num1 ^ num2

        # Using Brian Kernighan's algorithm to count set bits

        # count stores the total bits set in num
        count: int = 0
        while num:

            # clear the least significant bit set
            num = num & (num - 1)
            count += 1

        return count


# Examples from the problem statement
print(Solution().toggle_count(10, 1))     # 3
print(Solution().toggle_count(2, 3))      # 1
print(Solution().toggle_count(9, 1))      # 1

# Edge cases
print(Solution().toggle_count(0, 0))      # 0
print(Solution().toggle_count(5, 5))      # 0
print(Solution().toggle_count(0, 7))      # 3
print(Solution().toggle_count(15, 0))     # 4
print(Solution().toggle_count(7, 1))      # 2
```

```java run viz=array
public class Main {
    static class Solution {
        public int toggleCount(int num1, int num2) {

            // take XOR of num1 and num2 and store in num
            int num = num1 ^ num2;

            // Using Brian Kernighan's algorithm to count set bits

            // count stores the total bits set in num
            int count = 0;
            while (num != 0) {

                // clear the least significant bit set
                num = num & (num - 1);
                count++;
            }

            return count;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().toggleCount(10, 1));     // 3
        System.out.println(new Solution().toggleCount(2, 3));      // 1
        System.out.println(new Solution().toggleCount(9, 1));      // 1

        // Edge cases
        System.out.println(new Solution().toggleCount(0, 0));      // 0
        System.out.println(new Solution().toggleCount(5, 5));      // 0
        System.out.println(new Solution().toggleCount(0, 7));      // 3
        System.out.println(new Solution().toggleCount(15, 0));     // 4
        System.out.println(new Solution().toggleCount(7, 1));      // 2
    }
}
```

</details>
