---
title: "Reverse Bits"
summary: "Given a 32-bit unsigned integer num, return the integer formed by reversing its 32 bits — bit 1 becomes bit 32, bit 2 becomes bit 31, and so on."
prereqs:
  - 03-pattern-restructuring/01-pattern
difficulty: medium
kind: problem
topics: [restructuring, bit-manipulation]
---

# Reverse Bits

Reversal is the simplest *restructuring* move: walk all 32 positions and mirror each one end-to-end. The loop count is bound to the bit-width, not to the value.

## Problem Statement

Given a 32-bit unsigned integer `num`, return the integer formed by reversing its 32 bits — bit 1 becomes bit 32, bit 2 becomes bit 31, and so on.

## Examples

**Example 1**
```
Input:  num = 28
Output: 939524096
Explanation: 00000000 00000000 00000000 00011100  reversed end-to-end is
             00111000 00000000 00000000 00000000  = 939524096.
```

**Example 2**
```
Input:  num = 1
Output: 2147483648
Explanation: The lowest bit migrates to the highest position (bit 32).
```

## Constraints

- `0 ≤ num ≤ 2^32 - 1` — `num` is treated as an unsigned 32-bit value, so the result can exceed `2^31` (it occupies the full unsigned range `0 .. 4294967295`).
- The reversal always processes exactly 32 bit positions, regardless of `num`'s magnitude.

```python run viz=array
class Solution:
    def reverse_bits(self, num: int) -> int:
        # Your code goes here
        return 0


num = int(input())
print(Solution().reverse_bits(num))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int reverseBits(int num) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // num can be up to 2^32 - 1 (> Integer.MAX_VALUE): read as long, cast
        // to int so the bit pattern matches Python's value exactly.
        int num = (int) Long.parseLong(sc.nextLine().trim());
        // Print the unsigned interpretation (0 .. 2^32-1) to match Python.
        System.out.println(Integer.toUnsignedLong(new Solution().reverseBits(num)));
    }
}
```

```testcases
{
  "args": [
    { "id": "num", "label": "num", "type": "int", "placeholder": "28" }
  ],
  "cases": [
    { "args": { "num": "28" }, "expected": "939524096" },
    { "args": { "num": "1" }, "expected": "2147483648" },
    { "args": { "num": "2147483648" }, "expected": "1" },
    { "args": { "num": "0" }, "expected": "0" },
    { "args": { "num": "4294967295" }, "expected": "4294967295" }
  ]
}
```

<details>
<summary><h2>The Recurrence — LSB Extract, Append</h2></summary>


Build the reversed integer one bit at a time. For 32 iterations:
1. Shift `result` left by 1 — make room for one more bit at the bottom.
2. Take the LSB of `num` (`num & 1`) and OR it into `result`'s new bottom slot.
3. Shift `num` right by 1 — discard the bit we just consumed.

After 32 rounds, `num`'s original bit 1 has migrated all the way up to bit 32 in `result`, bit 2 to bit 31, etc. — exactly the reverse layout.

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#777777"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
flowchart LR
  S0["result = 0"]
  S0 --> S1["1. result <<= 1<br/>(make slot for new bit)"]
  S1 --> S2["2. result |= num & 1<br/>(append LSB of num)"]
  S2 --> S3["3. num >>= 1<br/>(discard consumed bit)"]
  S3 -->|"32 iterations"| S0
```

<p align="center"><strong>One iteration extracts <code>num</code>'s LSB and appends it as <code>result</code>'s new LSB. After 32 iterations, the original bit order is reversed end-to-end.</strong></p>

> *Pause. Why 32 iterations exactly? Predict the consequence of stopping early.*

Because the integer has 32 bits — every position must be processed for the reversal to land bits at the correct *symmetric* positions. Stopping at, say, 16 iterations would only reverse the lower half and leave the upper half zero. The loop count is bound to the bit-width, not to `num`'s actual content.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Walk all 32 positions, peeling `num`'s lowest bit each round and stacking it onto `result`. The Python result is naturally a non-negative value in `0 .. 2^32-1`; in Java the same 32-bit pattern is a signed `int`, so the driver prints `Integer.toUnsignedLong(result)` to render the unsigned value — and reads `num` via `(int) Long.parseLong` so inputs above `2^31` (like `2147483648`) survive the cast as the right bit pattern.

```python solution time=O(1) space=O(1)
class Solution:
    def reverse_bits(self, num: int) -> int:

        # Initialize the variable to store the reversed bits
        result: int = 0

        for _ in range(32):

            # Left shift the result by 1 to make room for the next bit
            result = result << 1

            # Get the least significant bit of num using bitwise AND with
            # 1
            bit: int = num & 1

            # Add the bit to the result
            result = result + bit

            # Right shift num by 1 to discard the least significant bit
            num = num >> 1

        # Return the reversed bits
        return result


num = int(input())
print(Solution().reverse_bits(num))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int reverseBits(int num) {

            // Initialize the variable to store the reversed bits
            int result = 0;

            for (int i = 0; i < 32; i++) {

                // Left shift the result by 1 to make room for the next bit
                result = result << 1;

                // Get the least significant bit of num using bitwise AND
                // with 1
                int bit = num & 1;

                // Add the bit to the result
                result = result + bit;

                // Right shift num by 1 to discard the least significant bit
                num = num >> 1;
            }

            // Return the reversed bits
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // num can be up to 2^32 - 1 (> Integer.MAX_VALUE): read as long, cast
        // to int so the bit pattern matches Python's value exactly.
        int num = (int) Long.parseLong(sc.nextLine().trim());
        // Print the unsigned interpretation (0 .. 2^32-1) to match Python.
        System.out.println(Integer.toUnsignedLong(new Solution().reverseBits(num)));
    }
}
```

<details>
<summary><strong>Trace — num = 28 (0b11100)</strong></summary>

```
Initial: result = 0, num = 0b11100

Iter  num         num & 1   result <<= 1  result |= bit  num >>= 1
0     ...11100    0         0             0              ...01110
1     ...01110    0         0             0              ...00111
2     ...00111    1         0             1              ...00011
3     ...00011    1         10            11             ...00001
4     ...00001    1         110           111            ...00000
5–31: num is 0, so we keep shifting result left, appending 0s.

After iteration 31:
  result has 28's lowest 5 bits (11100, processed in reverse order)
  pushed to the top of a 32-bit space.
  result = 0b00111000 00000000 00000000 00000000 = 939524096 ✓
```

</details>

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(32) = O(1)` — fixed bit-width loop |
| Space | `O(1)` |

</details>
<details>
<summary><h2>Faster Alternative — Divide and Conquer</h2></summary>


For hot loops, the divide-and-conquer approach uses 5 swap stages with magic masks:
1. Swap adjacent bits with masks `0x55555555` and `0xAAAAAAAA`.
2. Swap adjacent pairs with `0x33333333` and `0xCCCCCCCC`.
3. Swap adjacent nibbles with `0x0F0F0F0F` and `0xF0F0F0F0`.
4. Swap adjacent bytes (or use byte-swap intrinsic).
5. Swap halves.

5 ops total, no loop, ~6× faster on most CPUs. Beyond this lesson but worth knowing.

</details>
