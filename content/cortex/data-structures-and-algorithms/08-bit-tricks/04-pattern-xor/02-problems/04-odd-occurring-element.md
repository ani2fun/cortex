---
title: "Odd-Occurring Element"
summary: "Given an array where every element appears an even number of times *except one* that appears an odd number of times, find the odd-occurring element. Required: O(n) time, O(1) space."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: easy
kind: problem
topics: [xor, bit-manipulation]
---

# Odd-Occurring Element

XOR cancels every even-count value — the single odd-count element is all that survives.

## Problem Statement

Given an array where every element appears an even number of times *except one* that appears an odd number of times, find the odd-occurring element. Required: O(n) time, O(1) space.

## Examples

**Example 1**
```
Input:  [2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1]
Output: 2
Explanation: 2 appears 3 times (odd); all other elements appear an even number of times.
```

**Example 2**
```
Input:  [1, 2, 1, 1, 2, 1, 1]
Output: 1
Explanation: 1 appears 5 times (odd); 2 appears 2 times (even, cancels).
```

**Example 3**
```
Input:  [6, 7, 6, 7, 6, 7, 6]
Output: 7
Explanation: 7 appears 3 times (odd); 6 appears 4 times (even, cancels).
```

## Constraints

- `1 ≤ arr.length` — at least one element.
- All elements are non-negative integers.
- Exactly one element has an odd occurrence count.

```python run viz=array viz-root=arr
import ast
from typing import List

class Solution:
    def odd_occurring_element(self, arr: List[int]) -> int:
        # Your code goes here
        return 0


arr = ast.literal_eval(input())
print(Solution().odd_occurring_element(arr))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public int oddOccurringElement(int[] arr) {
            // Your code goes here
            return 0;
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
        System.out.println(new Solution().oddOccurringElement(arr));
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1]" }
  ],
  "cases": [
    { "args": { "arr": "[2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1]" }, "expected": "2" },
    { "args": { "arr": "[1, 2, 1, 1, 2, 1, 1]" }, "expected": "1" },
    { "args": { "arr": "[6, 7, 6, 7, 6, 7, 6]" }, "expected": "7" },
    { "args": { "arr": "[5]" }, "expected": "5" },
    { "args": { "arr": "[3, 3, 3]" }, "expected": "3" },
    { "args": { "arr": "[1, 1, 2, 2, 3]" }, "expected": "3" },
    { "args": { "arr": "[0, 0, 0]" }, "expected": "0" }
  ]
}
```

<details>
<summary><h2>The Recurrence — XOR All</h2></summary>

XOR every element. Pairs cancel; the odd-out survives. One linear pass; one accumulator variable.

```
result = arr[0] ^ arr[1] ^ ... ^ arr[n-1]
```

</details>
<details>
<summary><h2>The Solution</h2></summary>

### The Solution

XOR every element into a single accumulator. Even-count values pair off to `0` and vanish; the single odd-count value is left. One pass, one variable — `O(n)` time, `O(1)` space.

```python solution time=O(n) space=O(1)
import ast
from typing import List

class Solution:
    def odd_occurring_element(self, arr: List[int]) -> int:
        result: int = 0
        for val in arr:

            # Perform bitwise XOR operation with each element
            result ^= val

        # Return the result (element with odd occurrences)
        return result


arr = ast.literal_eval(input())
print(Solution().odd_occurring_element(arr))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int oddOccurringElement(int[] arr) {
            int result = 0;
            for (int val : arr) {

                // Perform bitwise XOR operation with each element
                result = result ^ val;
            }

            // Return the result (element with odd occurrences)
            return result;
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
        System.out.println(new Solution().oddOccurringElement(arr));
    }
}
```

</details>
