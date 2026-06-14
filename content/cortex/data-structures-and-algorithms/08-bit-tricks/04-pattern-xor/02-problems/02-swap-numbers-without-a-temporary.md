---
title: "Swap Numbers Without a Temporary"
summary: "Given two integers, swap their values *in place* without using a third (temporary) variable."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: easy
kind: problem
topics: [xor, bit-manipulation]
---

# Swap Numbers Without a Temporary

Three XOR operations exchange two integers in place — no scratch variable needed.

## Problem Statement

Given two integers, swap their values *in place* without using a third (temporary) variable. Print the swapped pair.

## Examples

**Example 1**
```
Input:  num1 = 10, num2 = 1
Output: 1, 10
Explanation: After swapping, num1 becomes 1 and num2 becomes 10.
```

**Example 2**
```
Input:  num1 = 9, num2 = 1
Output: 1, 9
Explanation: After swapping, num1 becomes 1 and num2 becomes 9.
```

**Example 3**
```
Input:  num1 = -1, num2 = 1
Output: 1, -1
Explanation: XOR swap is sign-safe — works for negative integers too.
```

## Constraints

- `-2^31 ≤ num1, num2 ≤ 2^31 - 1` — ordinary signed 32-bit integers.
- The two values may be equal (swap is a no-op; output is the same pair).

```python run viz=array
class Solution:
    def swap_numbers(self, num1: int, num2: int) -> None:
        # Your code goes here
        print(str(num1) + ", " + str(num2))


num1 = int(input())
num2 = int(input())
Solution().swap_numbers(num1, num2)
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public void swapNumbers(int num1, int num2) {
            // Your code goes here
            System.out.println(num1 + ", " + num2);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num1 = Integer.parseInt(sc.nextLine().trim());
        int num2 = Integer.parseInt(sc.nextLine().trim());
        new Solution().swapNumbers(num1, num2);
    }
}
```

```testcases
{
  "args": [
    { "id": "num1", "label": "num1", "type": "int", "placeholder": "10" },
    { "id": "num2", "label": "num2", "type": "int", "placeholder": "1" }
  ],
  "cases": [
    { "args": { "num1": "10", "num2": "1" }, "expected": "1, 10" },
    { "args": { "num1": "9", "num2": "1" }, "expected": "1, 9" },
    { "args": { "num1": "-1", "num2": "1" }, "expected": "1, -1" },
    { "args": { "num1": "0", "num2": "0" }, "expected": "0, 0" },
    { "args": { "num1": "5", "num2": "5" }, "expected": "5, 5" },
    { "args": { "num1": "0", "num2": "7" }, "expected": "7, 0" }
  ]
}
```

<details>
<summary><h2>The Recurrence — Three XORs</h2></summary>

```
num1 = num1 ^ num2          (now num1 holds num1 ^ num2)
num2 = num1 ^ num2          (= num1 ^ num2 ^ num2 = num1, the original)
num1 = num1 ^ num2          (= (num1 ^ num2) ^ original_num1 = original_num2)
```

Each step relies on the self-inverse property: applying the same XOR twice cancels back to the input.

> *Pause. What happens if <code>num1</code> and <code>num2</code> are at the same memory location? Predict.*

Disaster. Step 1 sets the location to `x ^ x = 0`. Step 2 sets it to `0 ^ 0 = 0`. Step 3 same. You get `0, 0` instead of the original values. So XOR-swap is *only* safe when the two operands are guaranteed distinct memory locations. Add a guard `if num1 != num2` (or by-pointer-equality) when in doubt — the original C++ code does this.

</details>
<details>
<summary><h2>The Solution</h2></summary>

### The Solution

Three XOR assignments swap two values without a temporary. The guard `if num1 != num2` protects against same-location aliasing. Because both inputs are ordinary in-range `int`s, `^` behaves identically in Python and Java.

```python solution time=O(1) space=O(1)
class Solution:
    def swap_numbers(self, num1: int, num2: int) -> None:

        # Check if the numbers are already equal
        if num1 != num2:

            # Perform XOR swap_numbers algorithm
            # Step 1: Perform XOR operation to store the XOR of num1 and
            # num2 in num1
            num1 = num1 ^ num2

            # Step 2: Perform XOR operation to store the XOR of updated
            # num1 and original num2 in num2
            num2 = num1 ^ num2

            # Step 3: Perform XOR operation to store the XOR of updated
            # num1 and updated num2 in num1
            num1 = num1 ^ num2

        # Print the swapped values
        print(str(num1) + ", " + str(num2))


num1 = int(input())
num2 = int(input())
Solution().swap_numbers(num1, num2)
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public void swapNumbers(int num1, int num2) {

            // Check if the numbers are already equal
            if (num1 != num2) {

                // Perform XOR swapNumbers algorithm
                // Step 1: Perform XOR operation to store the XOR of num1 and
                // num2 in num1
                num1 = num1 ^ num2;

                // Step 2: Perform XOR operation to store the XOR of updated
                // num1 and original num2 in num2
                num2 = num1 ^ num2;

                // Step 3: Perform XOR operation to store the XOR of updated
                // num1 and updated num2 in num1
                num1 = num1 ^ num2;
            }

            // Print the swapped values
            System.out.println(num1 + ", " + num2);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num1 = Integer.parseInt(sc.nextLine().trim());
        int num2 = Integer.parseInt(sc.nextLine().trim());
        new Solution().swapNumbers(num1, num2);
    }
}
```

</details>
