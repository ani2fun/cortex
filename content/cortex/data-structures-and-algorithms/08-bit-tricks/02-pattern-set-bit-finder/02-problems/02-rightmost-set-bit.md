---
title: "Rightmost Set Bit"
summary: "Given a 32-bit integer num, find the 1-indexed position of its rightmost (lowest) set bit."
prereqs:
  - 02-pattern-set-bit-finder/01-pattern
difficulty: easy
---

# Rightmost Set Bit

## The Problem

Given a 32-bit integer `num`, find the 1-indexed position of its rightmost (lowest) set bit.

```
Input:  num = 10
Output: 2                   Binary 1010 — lowest set bit is at position 2

Input:  num = 16
Output: 5                   Binary 0001 0000 — bit 5 is the only set bit

Input:  num = 17
Output: 1                   Binary 0001 0001 — bit 1 is the rightmost set bit
```

<details>
<summary><h2>The Recurrence</h2></summary>


Three steps:

1. **Fast path** — if the LSB is set (`num & 1`), bit 1 is already the rightmost set bit; return 1 immediately.
2. **Isolate the rightmost set bit** — `num & (num - 1)` clears it, and XOR-ing that against `num` leaves just the rightmost set bit standing. (Equivalent to `num & -num`, but written in terms of the lesson's primary `n & (n - 1)` identity.)
3. **Find its position by right-shifting** — repeatedly shift the isolated bit one position to the right and count iterations until it falls off. The final count is the 1-indexed position.

```
if num & 1: return 1
num = num ^ (num & (num - 1))     # isolate rightmost set bit
index = 0
while num != 0:
    num >>= 1
    index += 1
return index
```

> *Pause. Why does <code>num ^ (num & (num - 1))</code> isolate the lowest set bit? Predict before reading on.*

`num & (num - 1)` clears the rightmost set bit and leaves every other bit untouched. XOR-ing that with the original `num` cancels every bit they share — i.e. every higher set bit — and the only bit that differs is the one that was cleared. The result has a 1 in exactly that position. (Alternative single-step form: `num & -num`, which uses two's complement to achieve the same isolation.)

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=array
class Solution:
    def rightmost_set_bit(self, num: int) -> int:

        # Check if the least significant bit is set (num & 1)
        # If it is set, return 1 as the rightmost set bit position
        if num & 1:
            return 1

        # Clear the rightmost set bit using XOR operation
        num = num ^ (num & (num - 1))

        # Variable to store the index of the rightmost set bit
        index: int = 0

        # Iterate until num becomes zero
        while num:

            # Right shift num by 1 to check the next bit
            num = num >> 1

            # Increment the index by 1
            index += 1

        # Return the index of the rightmost set bit
        return index


# Examples from the problem statement
print(Solution().rightmost_set_bit(10))    # 2
print(Solution().rightmost_set_bit(16))    # 5
print(Solution().rightmost_set_bit(17))    # 1

# Edge cases
print(Solution().rightmost_set_bit(1))     # 1
print(Solution().rightmost_set_bit(2))     # 2
print(Solution().rightmost_set_bit(4))     # 3
print(Solution().rightmost_set_bit(8))     # 4
print(Solution().rightmost_set_bit(12))    # 3
```

```java run viz=array
public class Main {
    static class Solution {
        public int rightmostSetBit(int num) {

            // Check if the least significant bit is set (num & 1)
            // If it is set, return 1 as the rightmost set bit position
            if ((num & 1) != 0) {
                return 1;
            }

            // Clear the rightmost set bit using XOR operation
            num = num ^ (num & (num - 1));

            // Variable to store the index of the rightmost set bit
            int index = 0;

            // Iterate until num becomes zero
            while (num != 0) {

                // Right shift num by 1 to check the next bit
                num = num >> 1;

                // Increment the index by 1
                index++;
            }

            // Return the index of the rightmost set bit
            return index;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().rightmostSetBit(10));    // 2
        System.out.println(new Solution().rightmostSetBit(16));    // 5
        System.out.println(new Solution().rightmostSetBit(17));    // 1

        // Edge cases
        System.out.println(new Solution().rightmostSetBit(1));     // 1
        System.out.println(new Solution().rightmostSetBit(2));     // 2
        System.out.println(new Solution().rightmostSetBit(4));     // 3
        System.out.println(new Solution().rightmostSetBit(8));     // 4
        System.out.println(new Solution().rightmostSetBit(12));    // 3
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(p)` where `p` is the position of the rightmost set bit — bounded by the bit-width (32 for an `int`), so `O(1)` for fixed-width integers |
| Space | `O(1)` |

</details>
