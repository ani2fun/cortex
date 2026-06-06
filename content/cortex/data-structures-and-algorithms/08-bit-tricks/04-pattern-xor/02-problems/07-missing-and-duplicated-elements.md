---
title: "Missing and Duplicated Elements"
summary: "An array of size n should contain elements from 1 to n, but one element is missing and another is duplicated (appears twice). Return both — in any order."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: hard
---

# Missing and Duplicated Elements

## The Problem

An array of size `n` should contain elements from `1` to `n`, but one element is missing and another is duplicated (appears twice). Return both — in any order.

```
Input:  [1, 5, 2, 4, 2]    →  [2, 3]   (2 is duplicated, 3 is missing)
Input:  [2, 4, 1, 3, 6, 6] →  [5, 6]   (6 is duplicated, 5 is missing)
Input:  [1, 1]             →  [1, 2]
```

<details>
<summary><h2>The Recurrence — Same Trick as "Two Odd Elements"</h2></summary>


XOR the array together with `1..n`. Most elements cancel; only the missing and the duplicated survive — with the duplicated appearing 2× in the array and 1× in `1..n` (3 total → survives), and the missing appearing 0× in the array and 1× in `1..n` (1 total → survives). Result is `missing ^ duplicated`.

Now we have two unknowns and one equation — not enough. Apply the **odd-occurring-element II partition trick**: isolate any differing bit, split into two buckets, XOR each bucket against the array *and* against `1..n`. Each bucket isolates exactly one of the two values.

Final step: figure out which value is the missing one (it's *not* in the array) and which is the duplicate. Linear scan to disambiguate.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array viz-root=arr
from typing import List
import math

class Solution:
    def missing_and_duplicated_elements(
        self, arr: List[int]
    ) -> List[int]:
        n: int = len(arr)

        result: int = n

        # XOR all the elements of the array with their indices and n
        # The result will be the XOR of the missing and duplicate numbers
        for i in range(n):
            result = result ^ arr[i] ^ i

        num1: int = 0
        num2: int = 0

        # Find the rightmost set bit position in the result
        rightmost_set_bit_pos: int = int(math.log2(result & -result))

        # XOR all the elements of the array based on the rightmost set
        # bit position
        for num in arr:

            # The numbers with the rightmost set bit as 1 will XOR with
            # num1
            if num & (1 << rightmost_set_bit_pos):
                num1 = num1 ^ num

            # The numbers with the rightmost set bit as 0 will XOR with
            # num2
            else:
                num2 = num2 ^ num

        # XOR all the numbers from 1 to n based on the rightmost set bit
        # position
        for i in range(1, n + 1):

            # The numbers with the rightmost set bit as 1 will XOR with
            # num1
            if i & (1 << rightmost_set_bit_pos):
                num1 = num1 ^ i

            # The numbers with the rightmost set bit as 0 will XOR with
            # num2
            else:
                num2 = num2 ^ i

        # Check if num1 is missing in the array
        # If it is missing, return [num2, num1], else return [num1, num2]
        if num1 not in arr:
            return [num2, num1]

        return [num1, num2]


# Examples from the problem statement
print(Solution().missing_and_duplicated_elements([1, 5, 2, 4, 2]))       # [2, 3]
print(Solution().missing_and_duplicated_elements([2, 4, 1, 3, 6, 6]))    # [5, 6]
print(Solution().missing_and_duplicated_elements([1, 1]))                 # [1, 2]

# Edge cases
print(Solution().missing_and_duplicated_elements([2, 2]))                 # [1, 2]
print(Solution().missing_and_duplicated_elements([1, 3, 3]))              # [2, 3]
print(Solution().missing_and_duplicated_elements([3, 1, 2, 4, 4]))        # [5, 4]
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> missingAndDuplicatedElements(int[] arr) {
            int n = arr.length;

            int result = n;

            // XOR all the elements of the array with their indices and n
            // The result will be the XOR of the missing and duplicate
            // numbers
            for (int i = 0; i < n; i++) {
                result = result ^ arr[i] ^ i;
            }

            int num1 = 0, num2 = 0;

            // Find the rightmost set bit position in the result
            int rightMostSetBitPos = Integer.numberOfTrailingZeros(result);

            // XOR all the elements of the array based on the rightmost set
            // bit position
            for (int num : arr) {

                // The numbers with the rightmost set bit as 1 will XOR with
                // num1
                if ((num & (1 << rightMostSetBitPos)) != 0) {
                    num1 = num1 ^ num;
                }

                // The numbers with the rightmost set bit as 0 will XOR with
                // num2
                else {
                    num2 = num2 ^ num;
                }
            }

            // XOR all the numbers from 1 to n based on the rightmost set bit
            // position
            for (int i = 1; i <= n; i++) {

                // The numbers with the rightmost set bit as 1 will XOR with
                // num1
                if ((i & (1 << rightMostSetBitPos)) != 0) {
                    num1 = num1 ^ i;
                }

                // The numbers with the rightmost set bit as 0 will XOR with
                // num2
                else {
                    num2 = num2 ^ i;
                }
            }

            // Linear search for the missing element
            boolean isNum1Found = false;
            for (int num : arr) {
                if (num == num1) {
                    isNum1Found = true;
                    break;
                }
            }

            if (!isNum1Found) {
                return List.of(num2, num1);
            }

            return List.of(num1, num2);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().missingAndDuplicatedElements(new int[]{1, 5, 2, 4, 2}));       // [2, 3]
        System.out.println(new Solution().missingAndDuplicatedElements(new int[]{2, 4, 1, 3, 6, 6}));    // [5, 6]
        System.out.println(new Solution().missingAndDuplicatedElements(new int[]{1, 1}));                 // [1, 2]

        // Edge cases
        System.out.println(new Solution().missingAndDuplicatedElements(new int[]{2, 2}));                 // [1, 2]
        System.out.println(new Solution().missingAndDuplicatedElements(new int[]{1, 3, 3}));              // [2, 3]
        System.out.println(new Solution().missingAndDuplicatedElements(new int[]{3, 1, 2, 4, 4}));        // [5, 4]
    }
}
```

</details>
