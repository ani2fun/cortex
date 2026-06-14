---
title: "Missing and Duplicated Elements"
summary: "An array of size n should contain elements from 1 to n, but one element is missing and another is duplicated (appears twice). Return both — in any order."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: hard
kind: problem
topics: [xor, bit-manipulation]
---

# Missing and Duplicated Elements

XOR the array with the full index range to isolate two unknowns, then partition by a differing bit to separate them.

## Problem Statement

An array of size `n` should contain elements from `1` to `n`, but one element is missing and another is duplicated (appears twice). Return `[duplicate, missing]`.

## Examples

**Example 1**
```
Input:  [1, 5, 2, 4, 2]
Output: [2, 3]
Explanation: 2 is duplicated, 3 is missing.
```

**Example 2**
```
Input:  [2, 4, 1, 3, 6, 6]
Output: [6, 5]
Explanation: 6 is duplicated, 5 is missing.
```

**Example 3**
```
Input:  [1, 1]
Output: [1, 2]
Explanation: 1 is duplicated, 2 is missing.
```

## Constraints

- `2 ≤ arr.length = n`
- Elements are intended to be from `1` to `n` inclusive.
- Exactly one element is duplicated and exactly one is missing.

```python run viz=array viz-root=arr
import ast, math
from typing import List

class Solution:
    def missing_and_duplicated_elements(self, arr: List[int]) -> List[int]:
        # Your code goes here
        return []


arr = ast.literal_eval(input())
print(Solution().missing_and_duplicated_elements(arr))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> missingAndDuplicatedElements(int[] arr) {
            // Your code goes here
            return new ArrayList<>();
        }
    }

    static int[] parseIntArray(String s) {
        s = s.trim();
        if (s.equals("[]")) return new int[0];
        s = s.substring(1, s.length() - 1);
        String[] parts = s.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
        return arr;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine().trim());
        System.out.println(new Solution().missingAndDuplicatedElements(arr));
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 5, 2, 4, 2]" }
  ],
  "cases": [
    { "args": { "arr": "[1, 5, 2, 4, 2]" }, "expected": "[2, 3]" },
    { "args": { "arr": "[2, 4, 1, 3, 6, 6]" }, "expected": "[6, 5]" },
    { "args": { "arr": "[1, 1]" }, "expected": "[1, 2]" },
    { "args": { "arr": "[2, 2]" }, "expected": "[2, 1]" },
    { "args": { "arr": "[1, 3, 3]" }, "expected": "[3, 2]" },
    { "args": { "arr": "[3, 1, 2, 4, 4]" }, "expected": "[4, 5]" }
  ]
}
```

<details>
<summary><h2>The Recurrence — Same Trick as "Two Odd Elements"</h2></summary>

XOR the array together with `1..n`. Most elements cancel; only the missing and the duplicated survive — with the duplicated appearing 2× in the array and 1× in `1..n` (3 total → survives), and the missing appearing 0× in the array and 1× in `1..n` (1 total → survives). Result is `missing ^ duplicated`.

Now we have two unknowns and one equation — not enough. Apply the **odd-occurring-element II partition trick**: isolate any differing bit, split into two buckets, XOR each bucket against the array *and* against `1..n`. Each bucket isolates exactly one of the two values.

Final step: figure out which value is the missing one (it's *not* in the array) and which is the duplicate. Linear scan to disambiguate.

</details>
<details>
<summary><h2>The Solution</h2></summary>

### The Solution

XOR-all (array + indices) surfaces `missing ^ duplicated`. A single partition pass recovers both values; a final linear scan identifies which is the duplicate (the one present in the array). Identical algorithm in both languages; output order matches execution.

```python solution time=O(n) space=O(1)
import ast, math
from typing import List

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


arr = ast.literal_eval(input())
print(Solution().missing_and_duplicated_elements(arr))
```

```java solution
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

    static int[] parseIntArray(String s) {
        s = s.trim();
        if (s.equals("[]")) return new int[0];
        s = s.substring(1, s.length() - 1);
        String[] parts = s.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
        return arr;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine().trim());
        System.out.println(new Solution().missingAndDuplicatedElements(arr));
    }
}
```

</details>
