---
title: "Recovery Validation"
summary: "Given a sorted array recoveryCodes and an array attempts, return true if any attempt is in the recovery codes list. Must run in O(N log N) where N = len(attempts), M = len(recoveryCodes)."
prereqs:
  - 08-pattern-binary-search/01-pattern
difficulty: medium
---

# Recovery Validation

A list of valid recovery codes (sorted) and a list of attempts. Did *any* attempt succeed?

## The Problem

Given a sorted array `recoveryCodes` and an array `attempts`, return `true` if any attempt is in the recovery codes list. **Must run in `O(N log N)`** where `N = len(attempts)`, `M = len(recoveryCodes)`.

```
Input:  recoveryCodes = [1, 4, 7], attempts = [2, 4]
Output: true   (4 is in recovery codes)

Input:  recoveryCodes = [5, 9, 11, 12], attempts = [2, 9, 12]
Output: true

Input:  recoveryCodes = [1, 2, 3], attempts = [5, 6]
Output: false
```

<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Linear scan over `attempts`; binary-search each one against `recoveryCodes`. Total: `O(N log M)`. Stop on first match.


```python run viz=array
from typing import List

class Solution:
    def binary_search(self, arr: List[int], target: int) -> int:

        # Starting index of the search range
        low: int = 0

        # Ending index of the search range
        high: int = len(arr) - 1

        while low <= high:

            # Calculate the middle index
            mid: int = (low + high) // 2

            # Found the target, return the index
            if arr[mid] == target:
                return mid

            # If the arr[mid] is less than the target, adjust the search
            # range to the right half
            if arr[mid] < target:
                low = mid + 1

            # Else if the arr[mid] is greater than the target, adjust
            # the search range to the left half
            else:
                high = mid - 1

        # Target not found in the array
        return -1

    def recovery_validation(
        self, recovery_codes: List[int], attempts: List[int]
    ) -> bool:

        # Iterate through each attempted recovery code
        for attempt in attempts:

            # If the recovery code is acceptable, return true
            if self.binary_search(recovery_codes, attempt) != -1:
                return True

        # No acceptable recovery code found, return false
        return False


# Examples from the problem statement
print(Solution().recovery_validation([1, 4, 7], [2, 4]))         # True
print(Solution().recovery_validation([5, 9, 11, 12], [2, 9, 12]))  # True
print(Solution().recovery_validation([1, 2, 3], [5, 6]))         # False

# Edge cases
print(Solution().recovery_validation([1, 2, 3], []))              # False — no attempts
print(Solution().recovery_validation([5], [5]))                   # True  — single code match
print(Solution().recovery_validation([5], [6]))                   # False — single code miss
print(Solution().recovery_validation([1, 3, 5, 7], [2, 4, 6]))   # False — all misses
print(Solution().recovery_validation([10], [10, 20, 30]))         # True  — first attempt matches
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        private int binarySearch(int[] arr, int target) {

            // Starting index of the search range
            int low = 0;

            // Ending index of the search range
            int high = arr.length - 1;

            while (low <= high) {

                // Calculate the middle index
                int mid = (low + high) / 2;

                // Found the target, return the index
                if (arr[mid] == target) {
                    return mid;
                }

                // If the arr[mid] is less than the target, adjust the search
                // range to the right half
                else if (arr[mid] < target) {
                    low = mid + 1;
                }

                // Else if the arr[mid] is greater than the target, adjust
                // the search range to the left half
                else {
                    high = mid - 1;
                }
            }

            // Target not found in the array
            return -1;
        }

        public boolean recoveryValidation(
            int[] recoveryCodes,
            int[] attempts
        ) {

            // Iterate through each attempted recovery code
            for (int attempt : attempts) {

                // If the recovery code is acceptable, return true
                if (binarySearch(recoveryCodes, attempt) != -1) {
                    return true;
                }
            }

            // No acceptable recovery code found, return false
            return false;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().recoveryValidation(new int[]{1, 4, 7}, new int[]{2, 4}));           // true
        System.out.println(new Solution().recoveryValidation(new int[]{5, 9, 11, 12}, new int[]{2, 9, 12}));  // true
        System.out.println(new Solution().recoveryValidation(new int[]{1, 2, 3}, new int[]{5, 6}));           // false

        // Edge cases
        System.out.println(new Solution().recoveryValidation(new int[]{1, 2, 3}, new int[]{}));               // false — no attempts
        System.out.println(new Solution().recoveryValidation(new int[]{5}, new int[]{5}));                    // true  — single code match
        System.out.println(new Solution().recoveryValidation(new int[]{5}, new int[]{6}));                    // false — single code miss
        System.out.println(new Solution().recoveryValidation(new int[]{1, 3, 5, 7}, new int[]{2, 4, 6}));    // false — all misses
        System.out.println(new Solution().recoveryValidation(new int[]{10}, new int[]{10, 20, 30}));          // true  — first attempt matches
    }
}
```

### Complexity

`O(N log M)` time where `N = len(attempts)`, `M = len(recoveryCodes)`. `O(1)` space.

</details>
