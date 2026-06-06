---
title: "Odd-Occurring Element"
summary: "Given an array where every element appears an even number of times *except one* that appears an odd number of times, find the odd-occurring element. Required: O(n) time, O(1) space."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: easy
---

# Odd-Occurring Element

## The Problem

Given an array where every element appears an even number of times *except one* that appears an odd number of times, find the odd-occurring element. Required: O(n) time, O(1) space.

```
Input:  [2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1]   →  2     (the 2 appears 3 times — odd)
Input:  [1, 2, 1, 1, 2, 1, 1]                →  1
Input:  [6, 7, 6, 7, 6, 7, 6]                →  7
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



```python run viz=array viz-root=arr
from typing import List

class Solution:
    def odd_occurring_element(self, arr: List[int]) -> int:
        result: int = 0
        for val in arr:

            # Perform bitwise XOR operation with each element
            result ^= val

        # Return the result (element with odd occurrences)
        return result


# Examples from the problem statement
print(Solution().odd_occurring_element([2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1]))    # 2
print(Solution().odd_occurring_element([1, 2, 1, 1, 2, 1, 1]))                 # 1
print(Solution().odd_occurring_element([6, 7, 6, 7, 6, 7, 6]))                 # 7

# Edge cases
print(Solution().odd_occurring_element([5]))                                    # 5
print(Solution().odd_occurring_element([3, 3, 3]))                              # 3
print(Solution().odd_occurring_element([1, 1, 2, 2, 3]))                        # 3
print(Solution().odd_occurring_element([0, 0, 0]))                              # 0
```

```java run viz=array viz-root=arr
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

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().oddOccurringElement(new int[]{2, 2, 2, 1, 3, 1, 4, 3, 1, 4, 1}));    // 2
        System.out.println(new Solution().oddOccurringElement(new int[]{1, 2, 1, 1, 2, 1, 1}));                 // 1
        System.out.println(new Solution().oddOccurringElement(new int[]{6, 7, 6, 7, 6, 7, 6}));                 // 7

        // Edge cases
        System.out.println(new Solution().oddOccurringElement(new int[]{5}));                                    // 5
        System.out.println(new Solution().oddOccurringElement(new int[]{3, 3, 3}));                              // 3
        System.out.println(new Solution().oddOccurringElement(new int[]{1, 1, 2, 2, 3}));                        // 3
        System.out.println(new Solution().oddOccurringElement(new int[]{0, 0, 0}));                              // 0
    }
}
```

</details>
