---
title: "Duplicate Element"
summary: "You're given an array of size n containing elements from 1 to n - 1. Exactly one element appears twice; everyone else appears once. Find the duplicate."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: medium
kind: problem
topics: [xor, bit-manipulation]
---

# Duplicate Element

XOR the array against the index range `1..n-1` — every unique element cancels, leaving the duplicate.

## Problem Statement

You're given an array of size `n` containing elements from `1` to `n - 1`. Exactly one element appears twice; everyone else appears once. Find the duplicate.

## Examples

**Example 1**
```
Input:  [1, 4, 3, 2, 2]
Output: 2
Explanation: 2 appears twice; all others appear once.
```

**Example 2**
```
Input:  [4, 1, 5, 3, 2, 5]
Output: 5
Explanation: 5 appears twice; all others appear once.
```

**Example 3**
```
Input:  [1, 1]
Output: 1
Explanation: Array of size 2 with elements from 1 to 1; 1 is the duplicate.
```

## Constraints

- `2 ≤ arr.length = n`
- Elements are from `1` to `n - 1` inclusive.
- Exactly one element is duplicated; all others appear exactly once.

```python run viz=array viz-root=arr
import ast
from typing import List

class Solution:
    def duplicate_element(self, arr: List[int]) -> int:
        # Your code goes here
        return 0


arr = ast.literal_eval(input())
print(Solution().duplicate_element(arr))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public int duplicateElement(int[] arr) {
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
        System.out.println(new Solution().duplicateElement(arr));
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 4, 3, 2, 2]" }
  ],
  "cases": [
    { "args": { "arr": "[1, 4, 3, 2, 2]" }, "expected": "2" },
    { "args": { "arr": "[4, 1, 5, 3, 2, 5]" }, "expected": "5" },
    { "args": { "arr": "[1, 1]" }, "expected": "1" },
    { "args": { "arr": "[2, 1, 2]" }, "expected": "2" },
    { "args": { "arr": "[3, 1, 2, 3]" }, "expected": "3" },
    { "args": { "arr": "[1, 2, 3, 3]" }, "expected": "3" },
    { "args": { "arr": "[4, 3, 2, 4, 1]" }, "expected": "4" }
  ]
}
```

<details>
<summary><h2>The Recurrence — XOR Array Against XOR of `1..n-1`</h2></summary>

XOR all array elements together; XOR `1, 2, …, n-1` together; XOR those two results.

- The unique elements appear once in the array and once in `1..n-1`, so they cancel.
- The duplicate appears *twice* in the array and once in `1..n-1` — three appearances total → it survives (odd count).

```
result = (arr[0] ^ arr[1] ^ ... ^ arr[n-1]) ^ (1 ^ 2 ^ ... ^ (n-1))
       = duplicate
```

</details>
<details>
<summary><h2>The Solution</h2></summary>

### The Solution

XOR all array elements, then XOR with `1..n-1`. Every unique element cancels (appears once in each group); the duplicate appears twice in the array and once in the range — three times total, so it survives. `O(n)` time, `O(1)` space.

```python solution time=O(n) space=O(1)
import ast
from typing import List

class Solution:
    def duplicate_element(self, arr: List[int]) -> int:
        n: int = len(arr)
        num: int = 0

        # take xor of all array elements
        for i in range(n):
            num ^= arr[i]

        # take xor of numbers from 1 to `n-1`
        for i in range(1, n):
            num ^= i

        # same elements will cancel each other as a ^ a = 0,
        # 0 ^ 0 = 0 and a ^ 0 = a

        # num will contain the missing number
        return num


arr = ast.literal_eval(input())
print(Solution().duplicate_element(arr))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int duplicateElement(int[] arr) {
            int n = arr.length;
            int num = 0;

            // take xor of all array elements
            for (int i = 0; i < n; i++) {
                num ^= arr[i];
            }

            // take xor of numbers from 1 to `n-1`
            for (int i = 1; i <= n - 1; i++) {
                num ^= i;
            }

            // same elements will cancel each other as a ^ a = 0,
            // 0 ^ 0 = 0 and a ^ 0 = a

            // num will contain the missing number
            return num;
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
        System.out.println(new Solution().duplicateElement(arr));
    }
}
```

</details>
