---
title: "Bitwise Sort"
summary: "Given an integer array arr, sort it ascending by the number of 1s in each element's binary representation. For elements with the same bit-count, sort by value."
prereqs:
  - 13-pattern-custom-compare/01-pattern
difficulty: easy
kind: problem
topics: [custom-compare, sorting]
---

# Bitwise Sort

Sort numbers by the count of `1` bits in their binary representation. Ties broken by value.

## Problem Statement

Given an integer array `arr`, sort it ascending by the number of `1`s in each element's binary representation. For elements with the same bit-count, sort by value.

```
Input:  arr = [7, 10, 12, 18, 26]
Output: [10, 12, 18, 7, 26]

Input:  arr = [3, 7, 10, 18, 2, 9, 15, 31]
Output: [2, 3, 9, 10, 18, 7, 15, 31]
```

## Examples

**Example 1**
```
Input:  arr = [7, 10, 12, 18, 26]
Output: [10, 12, 18, 7, 26]
Explanation: 10=1010(2 ones), 12=1100(2 ones), 18=10010(2 ones), 7=111(3 ones), 26=11010(3 ones). Sorted by (ones, value): (2,10),(2,12),(2,18),(3,7),(3,26).
```

**Example 2**
```
Input:  arr = [3, 7, 10, 18, 2, 9, 15, 31]
Output: [2, 3, 9, 10, 18, 7, 15, 31]
Explanation: 2=10(1 one), 3=11(2), 9=1001(2), 10=1010(2), 18=10010(2), 7=111(3), 15=1111(4), 31=11111(5). Sorted by (ones, value).
```

## Constraints

- `1 ≤ arr.length ≤ 500`
- `0 ≤ arr[i] ≤ 10^4`

```python run viz=array viz-root=arr
import ast

class Solution:
    def bitwise_sort(self, arr):
        # Your code goes here — sort by (popcount, value) ascending.
        # Use bin(n).count("1") for the bit count.
        return arr

arr = ast.literal_eval(input())
Solution().bitwise_sort(arr)
print(arr)
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public void bitwiseSort(int[] arr) {
            // Your code goes here — sort by (Integer.bitCount(n), n) ascending.
            // Convert to a list, sort with a lambda, copy back.
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        new Solution().bitwiseSort(arr);
        System.out.println(Arrays.toString(arr));
    }

    // "[7, 10, 12]" → {7, 10, 12}
    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[7, 10, 12, 18, 26]" }
  ],
  "cases": [
    { "args": { "arr": "[7, 10, 12, 18, 26]" }, "expected": "[10, 12, 18, 7, 26]" },
    { "args": { "arr": "[3, 7, 10, 18, 2, 9, 15, 31]" }, "expected": "[2, 3, 9, 10, 18, 7, 15, 31]" },
    { "args": { "arr": "[1, 2, 4, 8, 16]" }, "expected": "[1, 2, 4, 8, 16]" },
    { "args": { "arr": "[0, 1, 2, 3]" }, "expected": "[0, 1, 2, 3]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The key insight is that "number of 1 bits" (`popcount`) is a self-contained attribute of each element — making this a clean key-function sort, not a pairwise comparator. Map each element to `(popcount(n), n)` so ties in bit-count fall back to ascending value. The built-in `bin(n).count("1")` (Python) or `Integer.bitCount(n)` (Java) handles the count; the sort does the rest.

</details>
<details>
<summary><h2>The Custom Compare</h2></summary>

Transform: `t(n) = (popcount(n), n)`. Sort by `t` ascending.

`popcount(n)` is the number of set bits — many languages have a built-in (`__builtin_popcount` in C/C++, `Integer.bitCount` in Java, `bin(n).count("1")` in Python).

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(n log n) space=O(n)
import ast

class Solution:
    def bitwise_sort(self, arr):

        # Sort the array using custom key
        arr.sort(
            key=lambda num: (

                # Count set bits in 'num' using built-in function
                bin(num).count("1"),

                # If set bits are equal, sort by the actual number
                num,
            )
        )


arr = ast.literal_eval(input())
Solution().bitwise_sort(arr)
print(arr)
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public void bitwiseSort(int[] arr) {

            // Convert array to a list for sorting
            List<Integer> list = new ArrayList<>();
            for (int num : arr) {
                list.add(num);
            }

            // Sort using a lambda comparator
            list.sort((num1, num2) -> {

                // Count set bits in 'a' using Integer.bitCount()
                int setBitCountNum1 = Integer.bitCount(num1);

                // Count set bits in 'b' using Integer.bitCount()
                int setBitCountNum2 = Integer.bitCount(num2);

                // Sort based on set bit count, then numerically if equal
                if (setBitCountNum1 == setBitCountNum2) {
                    return num1 - num2;
                }

                return setBitCountNum1 - setBitCountNum2;
            });

            // Copy sorted values back into the array
            for (int i = 0; i < arr.length; i++) {
                arr[i] = list.get(i);
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        new Solution().bitwiseSort(arr);
        System.out.println(Arrays.toString(arr));
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

### Complexity

- Time: `O(n log n)` — standard sort, key evaluated once per element.
- Space: `O(n)` — the key tuples (Python) or the temporary list (Java).

</details>
