---
title: "Rightmost Set Bit"
summary: "Given a 32-bit integer num, find the 1-indexed position of its rightmost (lowest) set bit."
prereqs:
  - 02-pattern-set-bit-finder/01-pattern
difficulty: easy
kind: problem
topics: [set-bit-finder, bit-manipulation]
---

# Rightmost Set Bit

Isolate the rightmost set bit with `n & -n`, then count how many times you shift right until it falls off — that count is the 1-indexed position.

## Problem Statement

Given a 32-bit integer `num`, find the 1-indexed position of its rightmost (lowest) set bit.

## Examples

**Example 1**
```
Input:  num = 10
Output: 2
Explanation: Binary 1010 — lowest set bit is at position 2.
```

**Example 2**
```
Input:  num = 16
Output: 5
Explanation: Binary 0001 0000 — bit 5 is the only (and lowest) set bit.
```

**Example 3**
```
Input:  num = 17
Output: 1
Explanation: Binary 0001 0001 — bit 1 is the rightmost set bit.
```

## Constraints

- `1 ≤ num ≤ 2^31 - 1` — positive non-negative integers; inputs are kept non-negative so `x & -x` agrees across Python and Java.
- The result is always in `[1, 32]`.

```python run viz=array
class Solution:
    def rightmost_set_bit(self, num: int) -> int:
        # Your code goes here
        return 0


num = int(input())
print(Solution().rightmost_set_bit(num))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int rightmostSetBit(int num) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().rightmostSetBit(num));
    }
}
```

```testcases
{
  "args": [
    { "id": "num", "label": "num", "type": "int", "placeholder": "10" }
  ],
  "cases": [
    { "args": { "num": "10" }, "expected": "2" },
    { "args": { "num": "16" }, "expected": "5" },
    { "args": { "num": "17" }, "expected": "1" },
    { "args": { "num": "12" }, "expected": "3" },
    { "args": { "num": "1" },  "expected": "1" }
  ]
}
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

Fast-path the LSB case, then isolate the rightmost set bit via XOR and count right-shifts until it drops to zero. Non-negative inputs keep `x & -x` consistent between Python's arbitrary-precision and Java's 32-bit signed `int`; plain `int` output, no masking needed.

```python solution time=O(1) space=O(1)
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


num = int(input())
print(Solution().rightmost_set_bit(num))
```

```java solution
import java.util.*;

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
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().rightmostSetBit(num));
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(p)` where `p` is the position of the rightmost set bit — bounded by the bit-width (32 for an `int`), so `O(1)` for fixed-width integers |
| Space | `O(1)` |

</details>
