---
title: "Unset Kth Bit"
summary: "Given num and k, return num with its kth bit forced to 0. If the bit was already 0, the value is unchanged."
prereqs:
  - 01-pattern-kth-bit/01-pattern
difficulty: easy
---

# Unset Kth Bit

## The Problem

Given `num` and `k`, return `num` with its kth bit forced to 0. If the bit was already 0, the value is unchanged.

```
Input:  num = 1, k = 1
Output: 0                        0001 → 0000

Input:  num = 2, k = 1
Output: 2                        0010 — bit 1 already 0; no change

Input:  num = 3, k = 1
Output: 2                        0011 → 0010
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



```python run viz=array
class Solution:
    def unset_kth_bit(self, num: int, k: int) -> int:

        # To turn off the Kth bit of a number, we can use bitwise
        # manipulation. We create a mask by left-shifting 1 by (k-1)
        # positions and then taking its bitwise complement. Then, we
        # perform bitwise AND between the given number and the mask to
        # turn off the Kth bit.
        return num & ~(1 << (k - 1))


# Examples from the problem statement
print(Solution().unset_kth_bit(1, 1))    # 0
print(Solution().unset_kth_bit(2, 1))    # 2
print(Solution().unset_kth_bit(3, 1))    # 2

# Edge cases
print(Solution().unset_kth_bit(0, 1))    # 0
print(Solution().unset_kth_bit(7, 2))    # 5
print(Solution().unset_kth_bit(7, 3))    # 3
print(Solution().unset_kth_bit(15, 4))   # 7
print(Solution().unset_kth_bit(8, 4))    # 0
```

```java run viz=array
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
        // Examples from the problem statement
        System.out.println(new Solution().unsetKthBit(1, 1));    // 0
        System.out.println(new Solution().unsetKthBit(2, 1));    // 2
        System.out.println(new Solution().unsetKthBit(3, 1));    // 2

        // Edge cases
        System.out.println(new Solution().unsetKthBit(0, 1));    // 0
        System.out.println(new Solution().unsetKthBit(7, 2));    // 5
        System.out.println(new Solution().unsetKthBit(7, 3));    // 3
        System.out.println(new Solution().unsetKthBit(15, 4));   // 7
        System.out.println(new Solution().unsetKthBit(8, 4));    // 0
    }
}
```

</details>
