---
title: "Power of 2"
summary: "Given an integer, return true if it's a positive power of 2 (1, 2, 4, 8, …); else false."
prereqs:
  - 06-pattern-applications/01-pattern
difficulty: easy
kind: problem
topics: [applications, bit-manipulation]
---

# Power of 2

A power of 2 has exactly one set bit — and `n & (n-1)` clears it in a single operation.

## Problem Statement

Given an integer, return `true` if it's a positive power of 2 (1, 2, 4, 8, …); else `false`.

## Examples

**Example 1**
```
Input:  num = 1
Output: true
Explanation: 1 = 2^0 — exactly one set bit.
```

**Example 2**
```
Input:  num = 8
Output: true
Explanation: 8 = 2^3 in binary is 1000 — exactly one set bit.
```

**Example 3**
```
Input:  num = 3
Output: false
Explanation: 3 in binary is 11 — two set bits, not a power of 2.
```

## Constraints

- `-2^31 ≤ num ≤ 2^31 - 1` — ordinary signed 32-bit integer.
- `num ≤ 0` is always `false` (0 is not a power of 2; negatives are not).

```python run viz=array
class Solution:
    def power_of2(self, num: int) -> bool:
        # Your code goes here
        return False


num = int(input())
print("true" if Solution().power_of2(num) else "false")
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public boolean powerOf2(int num) {
            // Your code goes here
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().powerOf2(num));
    }
}
```

```testcases
{
  "args": [
    { "id": "num", "label": "num", "type": "int", "placeholder": "8" }
  ],
  "cases": [
    { "args": { "num": "1" }, "expected": "true" },
    { "args": { "num": "8" }, "expected": "true" },
    { "args": { "num": "3" }, "expected": "false" },
    { "args": { "num": "0" }, "expected": "false" },
    { "args": { "num": "-1" }, "expected": "false" },
    { "args": { "num": "2" }, "expected": "true" },
    { "args": { "num": "16" }, "expected": "true" },
    { "args": { "num": "6" }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>The Recurrence</h2></summary>


A power of 2 has *exactly one* set bit. From lesson 2, `(n & (n - 1)) == 0` exactly when `n` has zero or one set bits. Combine with `n > 0` to exclude zero (which has zero set bits):

```
is_power_of_2 = n > 0 and (n & (n - 1)) == 0
```

> *Pause. Why does <code>n > 0</code> matter? What does <code>(0 & -1) == 0</code> evaluate to?*

In two's complement, `0 - 1 = -1` (all bits 1). `0 & -1 = 0`. Without the `n > 0` guard, the function would return `true` for `n = 0` — but 0 isn't a power of 2 (`2^k > 0` for any integer k). The guard plugs that hole.

</details>
<details>
<summary><h2>The Solution</h2></summary>

```python solution time=O(1) space=O(1)
class Solution:
    def power_of2(self, num: int) -> bool:

        # Check if the number is positive
        # and if the bitwise AND of num and (num - 1) is zero
        # If both conditions are true, return True, otherwise False
        return num > 0 and (num & (num - 1)) == 0


num = int(input())
print("true" if Solution().power_of2(num) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public boolean powerOf2(int num) {

            // Check if the number is positive
            // and if the bitwise AND of num and (num - 1) is zero
            // If both conditions are true, return true, otherwise false
            return num > 0 && (num & (num - 1)) == 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().powerOf2(num));
    }
}
```

</details>
