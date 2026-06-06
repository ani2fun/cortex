---
title: "Have Opposite Signs"
summary: "Given two 32-bit signed integers num1 and num2, return true if they have opposite signs (one positive, one negative), false otherwise. Treat 0 as positive."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: easy
---

# Have Opposite Signs

## The Problem

Given two 32-bit signed integers `num1` and `num2`, return `true` if they have opposite signs (one positive, one negative), `false` otherwise. Treat `0` as positive.

```
Input:  num1 = 10, num2 = -1     →  true
Input:  num1 = 2, num2 = -3      →  true
Input:  num1 = 9, num2 = 1       →  false
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



```python run viz=array
class Solution:
    def have_opposite_signs(self, num1: int, num2: int) -> bool:

        # XOR operation (^) will result in a negative number
        # only if the signs of n1 and n2 are different.
        return (num1 ^ num2) < 0


# Examples from the problem statement
print(Solution().have_opposite_signs(10, -1))     # True
print(Solution().have_opposite_signs(2, -3))      # True
print(Solution().have_opposite_signs(9, 1))       # False

# Edge cases
print(Solution().have_opposite_signs(-5, -10))    # False
print(Solution().have_opposite_signs(0, 0))       # False
print(Solution().have_opposite_signs(0, -1))      # False
print(Solution().have_opposite_signs(1, -1))      # True
print(Solution().have_opposite_signs(-1, 1))      # True
```

```java run viz=array
public class Main {
    static class Solution {
        public boolean haveOppositeSigns(int num1, int num2) {

            // XOR operation (^) will result in a negative number
            // only if the signs of n1 and n2 are different.
            return ((num1 ^ num2) < 0);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().haveOppositeSigns(10, -1));     // true
        System.out.println(new Solution().haveOppositeSigns(2, -3));      // true
        System.out.println(new Solution().haveOppositeSigns(9, 1));       // false

        // Edge cases
        System.out.println(new Solution().haveOppositeSigns(-5, -10));    // false
        System.out.println(new Solution().haveOppositeSigns(0, 0));       // false
        System.out.println(new Solution().haveOppositeSigns(0, -1));      // false
        System.out.println(new Solution().haveOppositeSigns(1, -1));      // true
        System.out.println(new Solution().haveOppositeSigns(-1, 1));      // true
    }
}
```

</details>
