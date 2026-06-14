---
title: "Unset Kth Bit"
summary: "Given num and k, return num with its kth bit forced to 0. If the bit was already 0, the value is unchanged."
prereqs:
  - 01-pattern-kth-bit/01-pattern
difficulty: easy
kind: problem
topics: [kth-bit, bit-manipulation]
---

# Unset Kth Bit

Invert a mask that has a single `1` at position `k`, then AND with the number — that bit becomes `0`, and every other bit is untouched.

## Problem Statement

Given `num` and `k`, return `num` with its kth bit forced to 0. If the bit was already 0, the value is unchanged.

## Examples

**Example 1**
```
Input:  num = 1, k = 1
Output: 0
Explanation: 0001 → 0000 — bit 1 cleared.
```

**Example 2**
```
Input:  num = 2, k = 1
Output: 2
Explanation: 0010 — bit 1 already 0; no change.
```

**Example 3**
```
Input:  num = 3, k = 1
Output: 2
Explanation: 0011 → 0010 — bit 1 cleared.
```

## Constraints

- `0 ≤ num ≤ 2^31 - 1` — non-negative 32-bit integer.
- `1 ≤ k ≤ 31` — 1-indexed bit position from the LSB.

```python run viz=array
class Solution:
    def unset_kth_bit(self, num: int, k: int) -> int:
        # Your code goes here
        return 0


num = int(input())
k = int(input())
print(Solution().unset_kth_bit(num, k))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int unsetKthBit(int num, int k) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().unsetKthBit(num, k));
    }
}
```

```testcases
{
  "args": [
    { "id": "num", "label": "num", "type": "int", "placeholder": "1" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "1" }
  ],
  "cases": [
    { "args": { "num": "1", "k": "1" }, "expected": "0" },
    { "args": { "num": "2", "k": "1" }, "expected": "2" },
    { "args": { "num": "3", "k": "1" }, "expected": "2" },
    { "args": { "num": "7", "k": "2" }, "expected": "5" },
    { "args": { "num": "7", "k": "3" }, "expected": "3" },
    { "args": { "num": "15", "k": "4" }, "expected": "7" }
  ]
}
```

<details>
<summary><h2>The Recurrence</h2></summary>


Build mask `1 << (k - 1)`. **Invert** it with `~` so every bit is 1 *except* bit k. AND with `num`: every bit of `num` survives except bit k, which is forced to 0 (because `0 AND anything = 0`).

```
result = num & ~(1 << (k - 1))
```

> *Pause. Why invert the mask first? Predict the failure if you skip the <code>~</code>.*

Without the `~`, you'd be ANDing `num` against a mask that's 0 everywhere except bit k. That zeros out *every* bit except bit k — exact opposite of what you want. The `~` flips every bit so the AND now preserves *all* bits except the one you want to clear.

</details>
<details>
<summary><h2>The Solution</h2></summary>

### The Solution

AND `num` with `~(1 << (k - 1))` — the inverted mask has a `0` only at position k, so every other bit passes through and bit k is zeroed.

```python solution time=O(1) space=O(1)
class Solution:
    def unset_kth_bit(self, num: int, k: int) -> int:

        # To turn off the Kth bit of a number, we can use bitwise
        # manipulation. We create a mask by left-shifting 1 by (k-1)
        # positions and then taking its bitwise complement. Then, we
        # perform bitwise AND between the given number and the mask to
        # turn off the Kth bit.
        return num & ~(1 << (k - 1))


num = int(input())
k = int(input())
print(Solution().unset_kth_bit(num, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int unsetKthBit(int num, int k) {

            // To turn off the Kth bit of a number, we can use bitwise
            // manipulation. We create a mask by left-shifting 1 by (k-1)
            // positions and then taking its bitwise complement. Then, we
            // perform bitwise AND between the given number and the mask to
            // turn off the Kth bit.

            return num & ~(1 << (k - 1));
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().unsetKthBit(num, k));
    }
}
```

</details>
