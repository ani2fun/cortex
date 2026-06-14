---
title: "Parity Checker"
summary: "Given an integer, return "odd" if it's odd, "even" if it's even."
prereqs:
  - 06-pattern-applications/01-pattern
difficulty: easy
kind: problem
topics: [applications, bit-manipulation]
---

# Parity Checker

The least significant bit is all you need — `n & 1` is the parity test.

## Problem Statement

Given an integer, return `"odd"` if it's odd, `"even"` if it's even.

## Examples

**Example 1**
```
Input:  num = 10
Output: even
Explanation: 10 in binary is 1010; LSB is 0, so it's even.
```

**Example 2**
```
Input:  num = 9
Output: odd
Explanation: 9 in binary is 1001; LSB is 1, so it's odd.
```

**Example 3**
```
Input:  num = -1
Output: odd
Explanation: -1 in two's complement has all bits set; LSB is 1, so it's odd.
```

## Constraints

- `-2^31 ≤ num ≤ 2^31 - 1` — ordinary signed 32-bit integer.
- `num & 1` is parity-correct for negative numbers in two's complement.

```python run viz=array
class Solution:
    def parity_checker(self, num: int) -> str:
        # Your code goes here
        return ""


num = int(input())
print(Solution().parity_checker(num))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public String parityChecker(int num) {
            // Your code goes here
            return "";
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().parityChecker(num));
    }
}
```

```testcases
{
  "args": [
    { "id": "num", "label": "num", "type": "int", "placeholder": "10" }
  ],
  "cases": [
    { "args": { "num": "10" }, "expected": "even" },
    { "args": { "num": "9" }, "expected": "odd" },
    { "args": { "num": "1" }, "expected": "odd" },
    { "args": { "num": "0" }, "expected": "even" },
    { "args": { "num": "-1" }, "expected": "odd" },
    { "args": { "num": "-2" }, "expected": "even" }
  ]
}
```

<details>
<summary><h2>The Recurrence</h2></summary>


The least significant bit *is* the parity. `n & 1` returns 1 for odd numbers, 0 for even.

```
parity = "odd" if (n & 1) else "even"
```

> *Pause. Why does this work for negative numbers in two's complement? Predict.*

In two's complement, `-1`'s bit pattern is all-1s, `-2`'s is all-1s except the LSB, and so on. The LSB still alternates between 0 (even) and 1 (odd) as the magnitude grows — same as for positives. So `n & 1` is parity-preserving for any signed integer.

Compare to `n % 2`: for negative numbers in C and similar languages, `(-3) % 2 = -1` (signed remainder), which fails the `== 1` test. `n & 1` always returns `0` or `1`. Use the bitwise check; sidestep the language-specific signed-modulo trap.

</details>
<details>
<summary><h2>The Solution</h2></summary>

```python solution time=O(1) space=O(1)
class Solution:
    def parity_checker(self, num: int) -> str:

        # Bitwise AND operation with 1 to check if num is odd
        if num & 1:

            # If num is odd, return "odd"
            return "odd"
        else:

            # If num is even, return "even"
            return "even"


num = int(input())
print(Solution().parity_checker(num))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public String parityChecker(int num) {

            // Bitwise AND operation with 1 to check if num is odd
            if ((num & 1) == 1) {

                // If num is odd, return "odd"
                return "odd";
            } else {

                // If num is even, return "even"
                return "even";
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().parityChecker(num));
    }
}
```

</details>
