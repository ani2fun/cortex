---
title: "Parity Checker II — Set-Bit Parity"
summary: "Given an integer num, return "odd" if its set bit count is odd, "even" otherwise. (This is *bit parity*, distinct from numerical parity from earlier.)"
prereqs:
  - 06-pattern-applications/01-pattern
difficulty: medium
kind: problem
topics: [applications, bit-manipulation]
---

# Parity Checker II — Set-Bit Parity

Count the set bits — Kernighan's loop strips one per iteration in `O(set bits)`.

## Problem Statement

Given a non-negative integer `num`, return `"odd"` if its **set bit count** is odd, `"even"` otherwise. (This is *bit parity*, distinct from numerical parity from earlier.)

## Examples

**Example 1**
```
Input:  num = 10
Output: even
Explanation: 10 in binary is 1010 — 2 set bits, count is even.
```

**Example 2**
```
Input:  num = 13
Output: odd
Explanation: 13 in binary is 1101 — 3 set bits, count is odd.
```

**Example 3**
```
Input:  num = 1
Output: odd
Explanation: 1 in binary is 1 — 1 set bit, count is odd.
```

## Constraints

- `0 ≤ num ≤ 2^31 - 1` — non-negative 32-bit integer.

```python run viz=array
class Solution:
    def parity_checker_ii(self, num: int) -> str:
        # Your code goes here
        return ""


num = int(input())
print(Solution().parity_checker_ii(num))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public String parityCheckerII(int num) {
            // Your code goes here
            return "";
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().parityCheckerII(num));
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
    { "args": { "num": "13" }, "expected": "odd" },
    { "args": { "num": "1" }, "expected": "odd" },
    { "args": { "num": "0" }, "expected": "even" },
    { "args": { "num": "7" }, "expected": "odd" },
    { "args": { "num": "15" }, "expected": "even" },
    { "args": { "num": "3" }, "expected": "even" },
    { "args": { "num": "5" }, "expected": "even" }
  ]
}
```

<details>
<summary><h2>The Recurrence</h2></summary>


Use **Brian Kernighan's algorithm** from lesson 4. Each `num & (num - 1)` clears one set bit; toggle a parity flag each iteration. After the loop, the flag's final state is the parity.

```
parity = false
while num != 0:
    parity = not parity
    num = num & (num - 1)
return "odd" if parity else "even"
```

> *Pause. Why iterate <code>n & (n - 1)</code> instead of just shifting and counting <code>n & 1</code>?*

Both work. Kernighan's runs in O(set-bit count); the shift-and-count loop runs in O(bit-width). For sparse integers (few set bits), Kernighan's is much faster. CPUs also expose a `popcount` instruction that's faster than either; in production, prefer the intrinsic. The manual version here illustrates the technique.

</details>
<details>
<summary><h2>The Solution</h2></summary>

```python solution time=O(set bits) space=O(1)
class Solution:
    def parity_checker_ii(self, num: int) -> str:

        # Initialize the parity flag as False (even).
        parity: bool = False

        while num:

            # Toggle the parity flag for every 1 encountered.
            parity = not parity

            # Clear the least significant bit (LSB) of num.
            num = num & (num - 1)

        # If the parity flag is True, return "odd".
        if parity:
            return "odd"

        # If the parity flag is False, return "even".
        else:
            return "even"


num = int(input())
print(Solution().parity_checker_ii(num))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public String parityCheckerII(int num) {

            // Initialize the parity flag as false (even).
            boolean parity = false;

            while (num != 0) {

                // Toggle the parity flag for every 1 encountered.
                parity = !parity;

                // Clear the least significant bit (LSB) of num.
                num = num & (num - 1);
            }

            // If the parity flag is true, return "odd".
            if (parity) {
                return "odd";

                // If the parity flag is false, return "even".
            } else {
                return "even";
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().parityCheckerII(num));
    }
}
```

</details>
