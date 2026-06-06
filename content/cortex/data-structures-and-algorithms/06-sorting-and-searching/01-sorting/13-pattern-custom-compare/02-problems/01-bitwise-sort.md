---
title: "Bitwise Sort"
summary: "Given an integer array arr, sort it ascending by the number of 1s in each element's binary representation. For elements with the same bit-count, sort by value."
prereqs:
  - 13-pattern-custom-compare/01-pattern
difficulty: easy
---

# Bitwise Sort

Sort numbers by the count of `1` bits in their binary representation. Ties broken by value.

---

## The Problem

Given an integer array `arr`, sort it ascending by the number of `1`s in each element's binary representation. For elements with the same bit-count, sort by value.

```
Input:  arr = [7, 10, 12, 18, 26]
Output: [10, 12, 18, 7, 26]
Explanation:
  10 = 1010 → 2 ones
  12 = 1100 → 2 ones
  18 = 10010 → 2 ones
  7  = 111   → 3 ones
  26 = 11010 → 3 ones
  Sorted by (ones, value): (2,10), (2,12), (2,18), (3,7), (3,26)

Input:  arr = [3, 7, 10, 18, 2, 9, 15, 31]
Output: [2, 3, 9, 10, 18, 7, 15, 31]

Input:  arr = [1, 2, 4, 8, 16]
Output: [1, 2, 4, 8, 16]   (each has exactly 1 bit; sort by value)
```

---

<details>
<summary><h2>The Custom Compare</h2></summary>


Transform: `t(n) = (popcount(n), n)`. Sort by `t` ascending.

`popcount(n)` is the number of set bits — many languages have a built-in (`__builtin_popcount` in C/C++, `Integer.bitCount` in Java, `bin(n).count("1")` in Python).

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array viz-root=arr
from typing import List

class Solution:
    def bitwise_sort(self, arr: List[int]) -> None:

        # Sort the array using custom key
        arr.sort(
            key=lambda num: (

                # Count set bits in 'num' using built-in function
                bin(num).count("1"),

                # If set bits are equal, sort by the actual number
                num,
            )
        )


# Examples from the problem statement
a1 = [7, 10, 12, 18, 26]
Solution().bitwise_sort(a1); print(a1)            # [10, 12, 18, 7, 26]

a2 = [3, 7, 10, 18, 2, 9, 15, 31]
Solution().bitwise_sort(a2); print(a2)            # [2, 3, 9, 10, 18, 7, 15, 31]

a3 = [1, 2, 4, 8, 16]
Solution().bitwise_sort(a3); print(a3)            # [1, 2, 4, 8, 16]

# Edge cases
a4: List[int] = []                                # empty array
Solution().bitwise_sort(a4); print(a4)            # []

a5 = [5]                                          # single element
Solution().bitwise_sort(a5); print(a5)            # [5]

a6 = [3, 1]                                       # two elements
Solution().bitwise_sort(a6); print(a6)            # [1, 3]

a7 = [7, 7, 7]                                    # all duplicates
Solution().bitwise_sort(a7); print(a7)            # [7, 7, 7]

a8 = [0, 1, 2, 3]                                 # includes zero
Solution().bitwise_sort(a8); print(a8)            # [0, 1, 2, 3]
```

```java run viz=array viz-root=arr
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
        // Examples from the problem statement
        int[] a1 = {7, 10, 12, 18, 26};
        new Solution().bitwiseSort(a1);
        System.out.println(Arrays.toString(a1));  // [10, 12, 18, 7, 26]

        int[] a2 = {3, 7, 10, 18, 2, 9, 15, 31};
        new Solution().bitwiseSort(a2);
        System.out.println(Arrays.toString(a2));  // [2, 3, 9, 10, 18, 7, 15, 31]

        int[] a3 = {1, 2, 4, 8, 16};
        new Solution().bitwiseSort(a3);
        System.out.println(Arrays.toString(a3));  // [1, 2, 4, 8, 16]

        // Edge cases
        int[] a4 = {};                            // empty array
        new Solution().bitwiseSort(a4);
        System.out.println(Arrays.toString(a4));  // []

        int[] a5 = {5};                           // single element
        new Solution().bitwiseSort(a5);
        System.out.println(Arrays.toString(a5));  // [5]

        int[] a6 = {3, 1};                        // two elements
        new Solution().bitwiseSort(a6);
        System.out.println(Arrays.toString(a6));  // [1, 3]

        int[] a7 = {7, 7, 7};                     // all duplicates
        new Solution().bitwiseSort(a7);
        System.out.println(Arrays.toString(a7));  // [7, 7, 7]

        int[] a8 = {0, 1, 2, 3};                  // includes zero
        new Solution().bitwiseSort(a8);
        System.out.println(Arrays.toString(a8));  // [0, 1, 2, 3]
    }
}
```

</details>
