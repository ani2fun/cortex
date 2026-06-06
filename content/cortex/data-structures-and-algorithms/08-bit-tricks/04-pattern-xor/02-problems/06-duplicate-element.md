---
title: "Duplicate Element"
summary: "You're given an array of size n containing elements from 1 to n - 1. Exactly one element appears twice; everyone else appears once. Find the duplicate."
prereqs:
  - 04-pattern-xor/01-pattern
difficulty: medium
---

# Duplicate Element

## The Problem

You're given an array of size `n` containing elements from `1` to `n - 1`. Exactly one element appears twice; everyone else appears once. Find the duplicate.

```
Input:  [1, 4, 3, 2, 2]    →  2
Input:  [4, 1, 5, 3, 2, 5] →  5
Input:  [1, 1]             →  1
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



```python run viz=array viz-root=arr
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


# Examples from the problem statement
print(Solution().duplicate_element([1, 4, 3, 2, 2]))     # 2
print(Solution().duplicate_element([4, 1, 5, 3, 2, 5]))  # 5
print(Solution().duplicate_element([1, 1]))               # 1

# Edge cases
print(Solution().duplicate_element([2, 1, 2]))            # 2
print(Solution().duplicate_element([3, 1, 2, 3]))         # 3
print(Solution().duplicate_element([1, 2, 3, 3]))         # 3
print(Solution().duplicate_element([2, 2]))               # 2
```

```java run viz=array viz-root=arr
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

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().duplicateElement(new int[]{1, 4, 3, 2, 2}));     // 2
        System.out.println(new Solution().duplicateElement(new int[]{4, 1, 5, 3, 2, 5}));  // 5
        System.out.println(new Solution().duplicateElement(new int[]{1, 1}));               // 1

        // Edge cases
        System.out.println(new Solution().duplicateElement(new int[]{2, 1, 2}));            // 2
        System.out.println(new Solution().duplicateElement(new int[]{3, 1, 2, 3}));         // 3
        System.out.println(new Solution().duplicateElement(new int[]{1, 2, 3, 3}));         // 3
        System.out.println(new Solution().duplicateElement(new int[]{2, 2}));               // 2
    }
}
```

</details>
