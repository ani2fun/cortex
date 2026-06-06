---
title: "Only Set Bit"
summary: "Given a 32-bit integer num, find the position (1-indexed) of the only set bit. If num has more than one set bit, return -1."
prereqs:
  - 02-pattern-set-bit-finder/01-pattern
difficulty: easy
---

# Only Set Bit

## The Problem

Given a 32-bit integer `num`, find the position (1-indexed) of the only set bit. If `num` has more than one set bit, return `-1`.

```
Input:  num = 16
Output: 5                    Binary 0001 0000 — only bit 5 is set

Input:  num = 2
Output: 2                    Binary 0010

Input:  num = 10
Output: -1                   Binary 1010 — two set bits
```

<details>
<summary><h2>The Recurrence</h2></summary>


Two steps:

1. **Validate**: `(num & (num - 1)) != 0` ⇒ more than one set bit ⇒ return `-1`.
2. **Find position**: take `log₂(num) + 1`. (For a power of 2, `log₂` is an integer; the +1 converts to 1-indexing.)

> *Pause. Why does step 1 reject zero correctly?*

For `num = 0`, `num - 1` wraps to `-1` (or all-1s in two's-complement), so `num & (num - 1) = 0`. The first test passes — it thinks zero has "≤ 1 set bit" (true). Then `log₂(0)` is undefined. Most implementations either special-case `num == 0` or rely on the platform's `log` returning `-inf` and the conversion landing somewhere harmless. The C++ original assumes `num > 0` per the problem; we'll guard explicitly here.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=array
import math

class Solution:
    def only_set_bit(self, num: int) -> int:

        # Check if num is not a power of 2 (has more than one set bit)
        if num & (num - 1):

            # Return -1 if num is not a power of 2
            return -1

        # Calculate the position of the set bit by taking the base-2
        # logarithm of num and adding 1
        return int(math.log2(num) + 1)


# Examples from the problem statement
print(Solution().only_set_bit(16))    # 5
print(Solution().only_set_bit(2))     # 2
print(Solution().only_set_bit(10))    # -1

# Edge cases
print(Solution().only_set_bit(1))     # 1
print(Solution().only_set_bit(4))     # 3
print(Solution().only_set_bit(8))     # 4
print(Solution().only_set_bit(7))     # -1
print(Solution().only_set_bit(3))     # -1
```

```java run viz=array
public class Main {
    static class Solution {
        public int onlySetBit(int num) {

            // Check if num is not a power of 2 (has more than one set bit)
            if ((num & (num - 1)) != 0) {

                // Return -1 if num is not a power of 2
                return -1;
            }

            // Calculate the position of the set bit by taking the base-2
            // logarithm of num and adding 1
            return (int) (Math.log(num) / Math.log(2)) + 1;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().onlySetBit(16));    // 5
        System.out.println(new Solution().onlySetBit(2));     // 2
        System.out.println(new Solution().onlySetBit(10));    // -1

        // Edge cases
        System.out.println(new Solution().onlySetBit(1));     // 1
        System.out.println(new Solution().onlySetBit(4));     // 3
        System.out.println(new Solution().onlySetBit(8));     // 4
        System.out.println(new Solution().onlySetBit(7));     // -1
        System.out.println(new Solution().onlySetBit(3));     // -1
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(1)` — constant-time bitwise check + log |
| Space | `O(1)` |

</details>
