---
title: "Odd-Occurring Element II"
summary: "Same setup, but two elements occur an odd number of times. Return both, in any order."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: medium
kind: problem
topics: [xor, bit-manipulation]
---

# Odd-Occurring Element II

When two values survive the XOR, a single differing bit is enough to partition them apart.

## Problem Statement

Same setup as Odd-Occurring Element, but **two** elements occur an odd number of times. Return both, sorted ascending.

## Examples

**Example 1**
```
Input:  [2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1, 5]
Output: [2, 5]
Explanation: 2 appears 3 times and 5 appears 1 time — both odd; rest cancel.
```

**Example 2**
```
Input:  [1, 2, 1, 1, 2, 3, 1, 3, 1, 3]
Output: [1, 3]
Explanation: 1 appears 5 times (odd) and 3 appears 3 times (odd).
```

**Example 3**
```
Input:  [1, 2]
Output: [1, 2]
Explanation: Both appear once — both odd-occurring.
```

## Constraints

- `2 ≤ arr.length` — at least two elements.
- All elements are non-negative integers.
- Exactly two elements have odd occurrence counts; they are distinct.

```python run viz=array viz-root=arr
import ast
from typing import List

class Solution:
    def odd_occurring_element_ii(self, arr: List[int]) -> List[int]:
        # Your code goes here
        return []


arr = ast.literal_eval(input())
print(sorted(Solution().odd_occurring_element_ii(arr)))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> oddOccurringElementII(int[] arr) {
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
        List<Integer> result = new Solution().oddOccurringElementII(arr);
        Collections.sort(result);
        System.out.println(result);
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1, 5]" }
  ],
  "cases": [
    { "args": { "arr": "[2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1, 5]" }, "expected": "[2, 5]" },
    { "args": { "arr": "[1, 2, 1, 1, 2, 3, 1, 3, 1, 3]" }, "expected": "[1, 3]" },
    { "args": { "arr": "[1, 2]" }, "expected": "[1, 2]" },
    { "args": { "arr": "[3, 5]" }, "expected": "[3, 5]" },
    { "args": { "arr": "[7, 7, 7, 4]" }, "expected": "[4, 7]" },
    { "args": { "arr": "[0, 1, 2, 0, 2, 3]" }, "expected": "[1, 3]" }
  ]
}
```

<details>
<summary><h2>The Recurrence — Partition by a Differing Bit</h2></summary>

XOR everything: result = `a ^ b`, where `a, b` are the two odd-occurring elements.

But `a ^ b` is a single number; we need to *separate* them. Find any bit where `a` and `b` differ — that bit must be set in `a ^ b`. The lowest such bit is `(a ^ b) & -(a ^ b)`.

Now partition the array by that bit: elements with the bit set XOR to `a`; elements without XOR to `b`. (Inside each partition, all even-count elements still cancel pairwise — they're either entirely in one bucket or the other.)

```d2
direction: right
flow: "Partition the array by the lowest bit where a and b differ" {
  grid-rows: 1
  grid-columns: 3
  grid-gap: 20
  s1: |md
    **Step 1**
    XOR all → `a ^ b`
  |
  s2: |md
    **Step 2**
    Isolate lowest set bit of `a ^ b`
  |
  s3: |md
    **Step 3**
    Partition; XOR each bucket → `a` and `b`
  |
}
```

<p align="center"><strong>Two passes total. The lowest differing bit cleanly splits <code>a</code> from <code>b</code>; even-count elements stay paired inside their bucket.</strong></p>

> *Pause. Why is the lowest differing bit guaranteed to exist?*

Because `a != b` (otherwise they'd be the same element). At least one bit must differ; the *lowest* such bit is just the lowest set bit of `a ^ b`. If `a == b` had been allowed, `a ^ b = 0` and isolation would fail — but the problem rules out that case.

</details>
<details>
<summary><h2>The Solution</h2></summary>

### The Solution

Two passes: first XOR-all to get `a ^ b`; then partition by the lowest differing bit and XOR each bucket to recover `a` and `b` separately. The solution sorts before returning so the output is deterministic.

```python solution time=O(n) space=O(1)
import ast
from typing import List

class Solution:
    def odd_occurring_element_ii(self, arr: List[int]) -> List[int]:
        result: int = 0

        # Finding the XOR of all elements in the array
        for val in arr:
            result = result ^ val

        # Finding the position of the rightmost set bit in the result
        rightMostSetBitPos: int = result & -result

        num1: int = 0
        num2: int = 0

        # Splitting the array into two subarrays based on the rightmost
        # set bit
        for num in arr:

            # If the rightmost set bit is set in the number
            if num & rightMostSetBitPos:

                # XOR the number with num1 to find the first odd
                # occurring element
                num1 = num1 ^ num
            else:

                # XOR the number with num2 to find the second odd
                # occurring element
                num2 = num2 ^ num

        # Return the two odd occurring elements as a list
        return [num1, num2]


arr = ast.literal_eval(input())
print(sorted(Solution().odd_occurring_element_ii(arr)))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> oddOccurringElementII(int[] arr) {
            int result = 0;

            // Finding the XOR of all elements in the array
            for (int val : arr) {
                result = result ^ val;
            }

            // Finding the position of the rightmost set bit in the result
            int rightMostSetBitPos = Integer.numberOfTrailingZeros(
                result & -result
            );

            int num1 = 0, num2 = 0;

            // Splitting the array into two subarrays based on the rightmost
            // set bit
            for (int num : arr) {

                // If the rightmost set bit is set in the number
                if ((num & (1 << rightMostSetBitPos)) != 0) {

                    // XOR the number with num1 to find the first odd
                    // occurring element
                    num1 = num1 ^ num;
                } else {

                    // XOR the number with num2 to find the second odd
                    // occurring element
                    num2 = num2 ^ num;
                }
            }

            List<Integer> resultArr = new ArrayList<>();

            // Adding the two odd occurring elements to the result list
            resultArr.add(num1);
            resultArr.add(num2);

            // Return the two odd occurring elements
            return resultArr;
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
        List<Integer> r = new Solution().oddOccurringElementII(arr);
        Collections.sort(r);
        System.out.println(r);
    }
}
```

</details>
