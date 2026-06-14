---
title: "Toggle Count"
summary: "Given two integers num1 and num2, return the number of bits that need to be flipped to convert one into the other."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: easy
kind: problem
topics: [xor, bit-manipulation]
---

# Toggle Count

XOR lights up exactly the positions where two bit-strings differ — count those lit positions and you have the toggle count.

## Problem Statement

Given two non-negative integers `num1` and `num2`, return the number of bits that need to be flipped to convert one into the other.

## Examples

**Example 1**
```
Input:  num1 = 10, num2 = 1
Output: 3
Explanation: Binary 1010 vs 0001 — bits 0, 1, and 3 differ (3 positions).
```

**Example 2**
```
Input:  num1 = 2, num2 = 3
Output: 1
Explanation: Binary 10 vs 11 — only the LSB differs.
```

**Example 3**
```
Input:  num1 = 0, num2 = 7
Output: 3
Explanation: 000 vs 111 — all three low bits differ.
```

## Constraints

- `0 ≤ num1, num2 ≤ 2^31 - 1` — non-negative signed 32-bit integers.

```python run viz=array
class Solution:
    def toggle_count(self, num1: int, num2: int) -> int:
        # Your code goes here
        return 0


num1 = int(input())
num2 = int(input())
print(Solution().toggle_count(num1, num2))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int toggleCount(int num1, int num2) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num1 = Integer.parseInt(sc.nextLine().trim());
        int num2 = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().toggleCount(num1, num2));
    }
}
```

```testcases
{
  "args": [
    { "id": "num1", "label": "num1", "type": "int", "placeholder": "10" },
    { "id": "num2", "label": "num2", "type": "int", "placeholder": "1" }
  ],
  "cases": [
    { "args": { "num1": "10", "num2": "1" }, "expected": "3" },
    { "args": { "num1": "2", "num2": "3" }, "expected": "1" },
    { "args": { "num1": "9", "num2": "1" }, "expected": "1" },
    { "args": { "num1": "0", "num2": "0" }, "expected": "0" },
    { "args": { "num1": "5", "num2": "5" }, "expected": "0" },
    { "args": { "num1": "0", "num2": "7" }, "expected": "3" },
    { "args": { "num1": "15", "num2": "0" }, "expected": "4" },
    { "args": { "num1": "7", "num2": "1" }, "expected": "2" }
  ]
}
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

### The Solution

XOR the two numbers (marking each differing bit), then count set bits with Brian Kernighan's algorithm. Both inputs are non-negative in-range `int`s, so `^` and `& (n-1)` agree in Python and Java.

```python solution time=O(1) space=O(1)
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


num1 = int(input())
num2 = int(input())
print(Solution().toggle_count(num1, num2))
```

```java solution
import java.util.*;

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
        Scanner sc = new Scanner(System.in);
        int num1 = Integer.parseInt(sc.nextLine().trim());
        int num2 = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().toggleCount(num1, num2));
    }
}
```

</details>
