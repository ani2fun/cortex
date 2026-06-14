---
title: "Kth Bit Check"
summary: "Given a 32-bit signed integer num and a non-negative integer k, return true if the kth bit of num is set, otherwise false."
prereqs:
  - 01-pattern-kth-bit/01-pattern
difficulty: easy
kind: problem
topics: [kth-bit, bit-manipulation]
---

# Kth Bit Check

Build a mask with a single `1` at position `k`, AND it with the number, and the result is non-zero only when that bit is set.

## Problem Statement

Given a 32-bit signed integer `num` and a non-negative integer `k`, return `true` if the kth bit of `num` is set, otherwise `false`.

## Examples

**Example 1**
```
Input:  num = 1, k = 1
Output: true
Explanation: Binary 0001 — bit 1 is set.
```

**Example 2**
```
Input:  num = 3, k = 2
Output: true
Explanation: Binary 0011 — bit 2 is set.
```

**Example 3**
```
Input:  num = 2, k = 1
Output: false
Explanation: Binary 0010 — bit 1 is unset.
```

## Constraints

- `0 ≤ num ≤ 2^31 - 1` — non-negative 32-bit integer.
- `1 ≤ k ≤ 31` — 1-indexed bit position from the LSB.

```python run viz=array
class Solution:
    def kth_bit_check(self, num: int, k: int) -> bool:
        # Your code goes here
        return False


num = int(input())
k = int(input())
print("true" if Solution().kth_bit_check(num, k) else "false")
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public boolean kthBitCheck(int num, int k) {
            // Your code goes here
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kthBitCheck(num, k));
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
    { "args": { "num": "1", "k": "1" }, "expected": "true" },
    { "args": { "num": "3", "k": "2" }, "expected": "true" },
    { "args": { "num": "2", "k": "1" }, "expected": "false" },
    { "args": { "num": "0", "k": "1" }, "expected": "false" },
    { "args": { "num": "4", "k": "3" }, "expected": "true" },
    { "args": { "num": "8", "k": "4" }, "expected": "true" }
  ]
}
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

AND with the mask and compare to zero — a single `&` and a comparison. Python prints `"true"`/`"false"` explicitly so the output matches Java's native lowercase boolean.

```python solution time=O(1) space=O(1)
class Solution:
    def kth_bit_check(self, num: int, k: int) -> bool:

        # To check if the Kth bit is set in a number, we can use bitwise
        # AND operation. We create a mask by left-shifting 1 by (k-1)
        # positions. Then, we perform bitwise AND between the given
        # number and the mask. If the result is not zero, it means the
        # Kth bit is set.

        return (num & (1 << (k - 1))) != 0


num = int(input())
k = int(input())
print("true" if Solution().kth_bit_check(num, k) else "false")
```

```java solution
import java.util.*;

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
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kthBitCheck(num, k));
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(1)` — three operations: shift, AND, compare |
| Space | `O(1)` |

</details>
