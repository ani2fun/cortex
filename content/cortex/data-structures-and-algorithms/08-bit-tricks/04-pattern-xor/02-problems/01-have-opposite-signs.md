---
title: "Have Opposite Signs"
summary: "Given two 32-bit signed integers num1 and num2, return true if they have opposite signs (one positive, one negative), false otherwise. Treat 0 as positive."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: easy
kind: problem
topics: [xor, bit-manipulation]
---

# Have Opposite Signs

Two numbers differ in sign exactly when their top bits differ — and XOR is the operator that lights up wherever two bits disagree.

## Problem Statement

Given two 32-bit signed integers `num1` and `num2`, return `true` if they have opposite signs (one positive, one negative), `false` otherwise. Treat `0` as positive.

## Examples

**Example 1**
```
Input:  num1 = 10, num2 = -1
Output: true
Explanation: One positive, one negative — opposite signs.
```

**Example 2**
```
Input:  num1 = 9, num2 = 1
Output: false
Explanation: Both positive — same sign.
```

**Example 3**
```
Input:  num1 = 0, num2 = -1
Output: true
Explanation: 0 counts as positive, so 0 and -1 have opposite signs.
```

## Constraints

- `-2^31 ≤ num1, num2 ≤ 2^31 - 1` — ordinary signed 32-bit integers (no values above `2^31 - 1`, so they parse directly as `int`).
- `0` is treated as positive.

```python run viz=array
class Solution:
    def have_opposite_signs(self, num1: int, num2: int) -> bool:
        # Your code goes here
        return False


num1 = int(input())
num2 = int(input())
print("true" if Solution().have_opposite_signs(num1, num2) else "false")
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public boolean haveOppositeSigns(int num1, int num2) {
            // Your code goes here
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num1 = Integer.parseInt(sc.nextLine().trim());
        int num2 = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().haveOppositeSigns(num1, num2));
    }
}
```

```testcases
{
  "args": [
    { "id": "num1", "label": "num1", "type": "int", "placeholder": "10" },
    { "id": "num2", "label": "num2", "type": "int", "placeholder": "-1" }
  ],
  "cases": [
    { "args": { "num1": "10", "num2": "-1" }, "expected": "true" },
    { "args": { "num1": "9", "num2": "1" }, "expected": "false" },
    { "args": { "num1": "-5", "num2": "-10" }, "expected": "false" },
    { "args": { "num1": "0", "num2": "0" }, "expected": "false" },
    { "args": { "num1": "0", "num2": "-1" }, "expected": "true" }
  ]
}
```

<details>
<summary><h2>The Recurrence</h2></summary>


In two's complement, the highest bit is the sign bit (1 ⇒ negative, 0 ⇒ non-negative). XORing two numbers gives a result with sign-bit set iff the two original sign-bits differ. A negative result therefore signals opposite signs.

```
opposite = (num1 ^ num2) < 0
```

> *Pause. Why does <code>(num1 ^ num2) < 0</code> work despite ignoring the lower 31 bits?*

Because in two's complement, "less than zero" is determined by the sign bit alone. Lower bits could be anything — the comparison only inspects the top bit. The XOR's top bit equals 1 iff the inputs' top bits differ. So this becomes a pure sign-bit XOR, expressed as a comparison.

</details>
<details>
<summary><h2>The Solution</h2></summary>

### The Solution

XOR the two numbers and inspect the sign of the result — a single `^` and a comparison. Because the inputs are ordinary in-range `int`s, Python's `^` and `< 0` agree bit-for-bit with Java's; only the boolean *print* differs, so Python emits `"true"`/`"false"` explicitly to match Java's native lowercase.

```python solution time=O(1) space=O(1)
class Solution:
    def have_opposite_signs(self, num1: int, num2: int) -> bool:

        # XOR operation (^) will result in a negative number
        # only if the signs of n1 and n2 are different.
        return (num1 ^ num2) < 0


num1 = int(input())
num2 = int(input())
print("true" if Solution().have_opposite_signs(num1, num2) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public boolean haveOppositeSigns(int num1, int num2) {

            // XOR operation (^) will result in a negative number
            // only if the signs of n1 and n2 are different.
            return ((num1 ^ num2) < 0);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num1 = Integer.parseInt(sc.nextLine().trim());
        int num2 = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().haveOppositeSigns(num1, num2));
    }
}
```

</details>
