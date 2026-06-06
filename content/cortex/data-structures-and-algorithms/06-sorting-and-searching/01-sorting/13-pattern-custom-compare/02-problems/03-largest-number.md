---
title: "Largest Number"
summary: "Given non-negative integers arr, return the largest number formable by concatenating them, as a string."
prereqs:
  - 13-pattern-custom-compare/01-pattern
difficulty: medium
---

# Largest Number

Concatenate numbers to form the largest possible number. The custom compare here is *non-obvious*: we don't sort by value or by digit count. We sort by which order produces a larger concatenation.

---

## The Problem

Given non-negative integers `arr`, return the largest number formable by concatenating them, as a string.

```
Input:  arr = [200, 3]
Output: "3200"   (3+200 vs 200+3: "3200" > "2003")

Input:  arr = [200, 8, 1, 3]
Output: "832001"

Input:  arr = [50, 20, 10, 5]
Output: "5502010"
```

---

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
<summary><h2>The Solution</h2></summary>


```python run viz=array viz-root=arr
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


# Examples from the problem statement
print(Solution().largest_number([200, 3]))          # 3200
print(Solution().largest_number([200, 8, 1, 3]))    # 832001
print(Solution().largest_number([50, 20, 10, 5]))   # 5502010

# Edge cases
print(Solution().largest_number([0, 0]))            # 0
print(Solution().largest_number([0]))               # 0
print(Solution().largest_number([1]))               # 1
print(Solution().largest_number([9, 1]))            # 91
print(Solution().largest_number([10, 2]))           # 210
```

```java run viz=array viz-root=arr
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
        // Examples from the problem statement
        System.out.println(new Solution().largestNumber(new int[]{200, 3}));         // 3200
        System.out.println(new Solution().largestNumber(new int[]{200, 8, 1, 3}));   // 832001
        System.out.println(new Solution().largestNumber(new int[]{50, 20, 10, 5}));  // 5502010

        // Edge cases
        System.out.println(new Solution().largestNumber(new int[]{0, 0}));           // 0
        System.out.println(new Solution().largestNumber(new int[]{0}));              // 0
        System.out.println(new Solution().largestNumber(new int[]{1}));              // 1
        System.out.println(new Solution().largestNumber(new int[]{9, 1}));           // 91
        System.out.println(new Solution().largestNumber(new int[]{10, 2}));          // 210
    }
}
```

Both implementations follow this pattern: convert numbers to strings, sort with a comparator that compares `a+b` vs `b+a`, concatenate, handle the all-zeros edge case.

</details>
<details>
<summary><h2>Why the Edge Case?</h2></summary>


If `arr = [0, 0]`, the sorted concatenation is `"00"` — but the largest number formable is `"0"`, not `"00"`. The check `if result[0] == "0"` catches this.

</details>
