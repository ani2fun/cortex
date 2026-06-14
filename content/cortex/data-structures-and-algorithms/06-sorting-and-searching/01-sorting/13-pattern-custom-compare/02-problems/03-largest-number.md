---
title: "Largest Number"
summary: "Given non-negative integers arr, return the largest number formable by concatenating them, as a string."
prereqs:
  - 13-pattern-custom-compare/01-pattern
difficulty: medium
kind: problem
topics: [custom-compare, sorting]
---

# Largest Number

Concatenate numbers to form the largest possible number. The custom compare here is *non-obvious*: we don't sort by value or by digit count. We sort by which order produces a larger concatenation.

## Problem Statement

Given non-negative integers `arr`, return the largest number formable by concatenating them, as a string.

```
Input:  arr = [200, 3]
Output: "3200"

Input:  arr = [200, 8, 1, 3]
Output: "832001"
```

## Examples

**Example 1**
```
Input:  arr = [200, 3]
Output: "3200"
Explanation: "3200" > "2003" because placing 3 before 200 yields a larger concatenation.
```

**Example 2**
```
Input:  arr = [200, 8, 1, 3]
Output: "832001"
Explanation: Sorted by a+b vs b+a comparator: 8, 3, 200, 1 → "8" + "3" + "200" + "1" = "832001".
```

## Constraints

- `1 ≤ arr.length ≤ 100`
- `0 ≤ arr[i] ≤ 10^9`

```python run viz=array viz-root=arr
import ast

class Solution:
    def largest_number(self, arr):
        # Your code goes here — convert to strings, sort with a+b vs b+a
        # comparator (descending), join, handle all-zeros edge case.
        return ""

arr = ast.literal_eval(input())
print(Solution().largest_number(arr))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public String largestNumber(int[] arr) {
            // Your code goes here — convert to Integer[], sort with
            // (b+a).compareTo(a+b), join, handle all-zeros edge case.
            return "";
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        System.out.println(new Solution().largestNumber(arr));
    }

    // "[200, 3]" → {200, 3}
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[200, 3]" }
  ],
  "cases": [
    { "args": { "arr": "[200, 3]" }, "expected": "3200" },
    { "args": { "arr": "[200, 8, 1, 3]" }, "expected": "832001" },
    { "args": { "arr": "[50, 20, 10, 5]" }, "expected": "5502010" },
    { "args": { "arr": "[0, 0]" }, "expected": "0" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The insight is that "which number should come first?" is a pairwise property — `a` beats `b` if the string `a+b` is lexicographically larger than `b+a`. A per-element key can't express this (it depends on the other element), so you need a comparator. This comparator is provably transitive, making it a valid total order. The edge case: if all numbers are zero, the concatenation is `"00…0"` — return `"0"` instead.

</details>
<details>
<summary><h2>The Custom Compare</h2></summary>

The non-obvious insight: to maximise the concatenation `a + b` vs `b + a`, compare the two string concatenations directly.

```
cmp(a, b) = +1 if str(a)+str(b) < str(b)+str(a)    # b should come first
            -1 if str(a)+str(b) > str(b)+str(a)    # a should come first
             0 if equal
```

This pairwise greedy is correct because of a (non-trivial) transitivity argument: if `(a+b) > (b+a)` and `(b+c) > (c+b)`, then `(a+c) > (c+a)`. We won't prove it here; the result is that this comparator works.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(n log n) space=O(n)
import ast
from typing import List

class Solution:
    def largest_number(self, arr: List[int]) -> str:

        # Sort the numbers using the custom comparator
        arr.sort(key=lambda x: str(x) * 10, reverse=True)

        # Edge case: If the largest number is '0', return "0"
        # (e.g., [0, 0])
        if arr[0] == 0:
            return "0"

        # Concatenate the sorted numbers
        return "".join(map(str, arr))


arr = ast.literal_eval(input())
print(Solution().largest_number(arr))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public String largestNumber(int[] arr) {

            // Convert int[] to Integer[] for lambda comparator
            Integer[] nums = Arrays
                .stream(arr)
                .boxed()
                .toArray(Integer[]::new);

            // Sort the numbers using a lambda comparator
            Arrays.sort(
                nums,
                (num1, num2) -> {
                    String num1Str = String.valueOf(num1);
                    String num2Str = String.valueOf(num2);

                    // Larger concatenation comes first
                    return (num2Str + num1Str).compareTo(num1Str + num2Str);
                }
            );

            // Edge case: If the largest number is '0', return "0"
            // (e.g., [0, 0])
            if (nums[0] == 0) {
                return "0";
            }

            // Concatenate the sorted numbers
            StringBuilder result = new StringBuilder();
            for (int num : nums) {
                result.append(num);
            }

            return result.toString();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        System.out.println(new Solution().largestNumber(arr));
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

Both implementations follow this pattern: convert numbers to strings, sort with a comparator that compares `a+b` vs `b+a`, concatenate, handle the all-zeros edge case.

### Complexity

- Time: `O(n log n)` — the sort dominates; each comparison costs `O(L)` where `L` is the max digit length.
- Space: `O(n)` — the string array and the output.

</details>
<details>
<summary><h2>Why the Edge Case?</h2></summary>

If `arr = [0, 0]`, the sorted concatenation is `"00"` — but the largest number formable is `"0"`, not `"00"`. The check `if result[0] == "0"` catches this.

</details>
