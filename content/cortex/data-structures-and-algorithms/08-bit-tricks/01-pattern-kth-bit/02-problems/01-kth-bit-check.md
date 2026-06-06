---
title: "Kth Bit Check"
summary: "Given a 32-bit signed integer num and a non-negative integer k, return true if the kth bit of num is set, otherwise false."
prereqs:
  - 01-pattern-kth-bit/01-pattern
difficulty: easy
---

# Kth Bit Check

## The Problem

Given a 32-bit signed integer `num` and a non-negative integer `k`, return `true` if the kth bit of `num` is set, otherwise `false`.

```
Input:  num = 1, k = 1
Output: true                  Binary 0001 — bit 1 is set

Input:  num = 3, k = 2
Output: true                  Binary 0011 — bit 2 is set

Input:  num = 2, k = 1
Output: false                 Binary 0010 — bit 1 is unset
```

<details>
<summary><h2>The Recurrence</h2></summary>


Build mask `1 << (k - 1)` (only bit k is 1). AND with `num` zeroes out every bit *except* bit k. The result is non-zero iff bit k was 1.

```
result = (num & (1 << (k - 1))) != 0
```

> *Pause. Why compare to <code>!= 0</code> instead of <code>== 1</code>? Predict the failure case.*

Because `num & mask` keeps bit k *in its original position*, not at bit 1. For `k = 3` the result is either `0` or `4` — never `1`. Comparing against `1` would falsely return `false` whenever bit k > 1. The non-zero check works for every `k`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=array
class Solution:
    def kth_bit_check(self, num: int, k: int) -> bool:

        # To check if the Kth bit is set in a number, we can use bitwise
        # AND operation. We create a mask by left-shifting 1 by (k-1)
        # positions. Then, we perform bitwise AND between the given
        # number and the mask. If the result is not zero, it means the
        # Kth bit is set.

        return (num & (1 << (k - 1))) != 0


# Examples from the problem statement
print(Solution().kth_bit_check(1, 1))    # True
print(Solution().kth_bit_check(3, 2))    # True
print(Solution().kth_bit_check(2, 1))    # False

# Edge cases
print(Solution().kth_bit_check(0, 1))    # False
print(Solution().kth_bit_check(1, 2))    # False
print(Solution().kth_bit_check(4, 3))    # True
print(Solution().kth_bit_check(7, 3))    # True
print(Solution().kth_bit_check(8, 4))    # True
```

```java run viz=array
public class Main {
    static class Solution {
        public boolean kthBitCheck(int num, int k) {

            // To check if the Kth bit is set in a number, we can use bitwise
            // AND operation. We create a mask by left-shifting 1 by (k-1)
            // positions. Then, we perform bitwise AND between the given
            // number and the mask. If the result is not zero, it means the
            // Kth bit is set.

            return (num & (1 << (k - 1))) != 0;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().kthBitCheck(1, 1));    // true
        System.out.println(new Solution().kthBitCheck(3, 2));    // true
        System.out.println(new Solution().kthBitCheck(2, 1));    // false

        // Edge cases
        System.out.println(new Solution().kthBitCheck(0, 1));    // false
        System.out.println(new Solution().kthBitCheck(1, 2));    // false
        System.out.println(new Solution().kthBitCheck(4, 3));    // true
        System.out.println(new Solution().kthBitCheck(7, 3));    // true
        System.out.println(new Solution().kthBitCheck(8, 4));    // true
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(1)` — three operations: shift, AND, compare |
| Space | `O(1)` |

</details>
