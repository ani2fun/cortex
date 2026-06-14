---
title: "Only Set Bit"
summary: "Given a 32-bit integer num, find the position (1-indexed) of the only set bit. If num has more than one set bit, return -1."
prereqs:
  - 02-pattern-set-bit-finder/01-pattern
difficulty: easy
kind: problem
topics: [set-bit-finder, bit-manipulation]
---

# Only Set Bit

Use `n & (n - 1)` to validate that exactly one bit is set, then find its 1-indexed position.

## Problem Statement

Given a 32-bit integer `num`, find the position (1-indexed) of the only set bit. If `num` has more than one set bit, return `-1`.

## Examples

**Example 1**
```
Input:  num = 16
Output: 5
Explanation: Binary 0001 0000 — only bit 5 is set.
```

**Example 2**
```
Input:  num = 2
Output: 2
Explanation: Binary 0010 — only bit 2 is set.
```

**Example 3**
```
Input:  num = 10
Output: -1
Explanation: Binary 1010 — two set bits, so no unique position.
```

## Constraints

- `1 ≤ num ≤ 2^31 - 1` — positive integers only; `num = 0` has no set bits and is excluded.
- Return `-1` if `num` has more than one set bit.

```python run viz=array
import math

class Solution:
    def only_set_bit(self, num: int) -> int:
        # Your code goes here
        return 0


num = int(input())
print(Solution().only_set_bit(num))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int onlySetBit(int num) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().onlySetBit(num));
    }
}
```

```testcases
{
  "args": [
    { "id": "num", "label": "num", "type": "int", "placeholder": "16" }
  ],
  "cases": [
    { "args": { "num": "16" }, "expected": "5" },
    { "args": { "num": "2" },  "expected": "2" },
    { "args": { "num": "10" }, "expected": "-1" },
    { "args": { "num": "1" },  "expected": "1" },
    { "args": { "num": "4" },  "expected": "3" }
  ]
}
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

Check `num & (num - 1)`: if non-zero, more than one bit is set — return `-1`. Otherwise `num` is a power of 2, so its 1-indexed position is `log₂(num) + 1`. Both languages stay in-range for non-negative inputs; plain `int` I/O, no masking needed.

```python solution time=O(1) space=O(1)
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


num = int(input())
print(Solution().only_set_bit(num))
```

```java solution
import java.util.*;

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
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().onlySetBit(num));
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(1)` — constant-time bitwise check + log |
| Space | `O(1)` |

</details>
