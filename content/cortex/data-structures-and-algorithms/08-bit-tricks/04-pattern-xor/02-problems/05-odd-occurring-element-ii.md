---
title: "Odd-Occurring Element II"
summary: "Same setup, but two elements occur an odd number of times. Return both, in any order."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: medium
---

# Odd-Occurring Element II

## The Problem

Same setup, but **two** elements occur an odd number of times. Return both, in any order.

```
Input:  [2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1, 5]   →  [2, 5]
Input:  [1, 2, 1, 1, 2, 3, 1, 3, 1, 3]          →  [1, 3]
Input:  [1, 2]                                  →  [1, 2]
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



```python run viz=array viz-root=arr
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


# Examples from the problem statement
print(sorted(Solution().odd_occurring_element_ii([2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1, 5])))    # [2, 5]
print(sorted(Solution().odd_occurring_element_ii([1, 2, 1, 1, 2, 3, 1, 3, 1, 3])))           # [1, 3]
print(sorted(Solution().odd_occurring_element_ii([1, 2])))                                    # [1, 2]

# Edge cases
print(sorted(Solution().odd_occurring_element_ii([3, 5])))                                    # [3, 5]
print(sorted(Solution().odd_occurring_element_ii([7, 7, 7, 4])))                              # [4, 7]
print(sorted(Solution().odd_occurring_element_ii([0, 1, 0, 0, 1, 1, 2, 2, 2, 3])))           # [1, 3]
```

```java run viz=array viz-root=arr
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

    public static void main(String[] args) {
        // Examples from the problem statement
        List<Integer> r1 = new Solution().oddOccurringElementII(new int[]{2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1, 5});
        Collections.sort(r1); System.out.println(r1);    // [2, 5]

        List<Integer> r2 = new Solution().oddOccurringElementII(new int[]{1, 2, 1, 1, 2, 3, 1, 3, 1, 3});
        Collections.sort(r2); System.out.println(r2);    // [1, 3]

        List<Integer> r3 = new Solution().oddOccurringElementII(new int[]{1, 2});
        Collections.sort(r3); System.out.println(r3);    // [1, 2]

        // Edge cases
        List<Integer> r4 = new Solution().oddOccurringElementII(new int[]{3, 5});
        Collections.sort(r4); System.out.println(r4);    // [3, 5]

        List<Integer> r5 = new Solution().oddOccurringElementII(new int[]{7, 7, 7, 4});
        Collections.sort(r5); System.out.println(r5);    // [4, 7]

        List<Integer> r6 = new Solution().oddOccurringElementII(new int[]{0, 1, 0, 0, 1, 1, 2, 2, 2, 3});
        Collections.sort(r6); System.out.println(r6);    // [1, 3]
    }
}
```

</details>
