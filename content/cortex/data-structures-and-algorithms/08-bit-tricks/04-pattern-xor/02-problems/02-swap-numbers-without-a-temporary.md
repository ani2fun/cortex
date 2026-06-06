---
title: "Swap Numbers Without a Temporary"
summary: "Given two integers, swap their values *in place* without using a third (temporary) variable."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: easy
---

# Swap Numbers Without a Temporary

## The Problem

Given two integers, swap their values *in place* without using a third (temporary) variable.

```
Input:  num1 = 10, num2 = 1   →  num1 = 1, num2 = 10
Input:  num1 = 9, num2 = 1    →  num1 = 1, num2 = 9
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



```python run viz=array
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

        # Print the swap_numbersped values
        print(str(num1) + ", " + str(num2))


# Examples from the problem statement
Solution().swap_numbers(10, 1)     # 1, 10
Solution().swap_numbers(2, 3)      # 3, 2
Solution().swap_numbers(9, 1)      # 1, 9

# Edge cases
Solution().swap_numbers(0, 0)      # 0, 0
Solution().swap_numbers(5, 5)      # 5, 5
Solution().swap_numbers(-1, 1)     # 1, -1
Solution().swap_numbers(0, 7)      # 7, 0
```

```java run viz=array
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

            // Print the swapNumbersped values
            System.out.println(num1 + ", " + num2);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        new Solution().swapNumbers(10, 1);     // 1, 10
        new Solution().swapNumbers(2, 3);      // 3, 2
        new Solution().swapNumbers(9, 1);      // 1, 9

        // Edge cases
        new Solution().swapNumbers(0, 0);      // 0, 0
        new Solution().swapNumbers(5, 5);      // 5, 5
        new Solution().swapNumbers(-1, 1);     // 1, -1
        new Solution().swapNumbers(0, 7);      // 7, 0
    }
}
```

</details>
