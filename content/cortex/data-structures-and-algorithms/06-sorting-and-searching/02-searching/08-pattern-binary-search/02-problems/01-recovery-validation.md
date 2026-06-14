---
title: "Recovery Validation"
summary: "Given a sorted array recoveryCodes and an array attempts, return true if any attempt is in the recovery codes list. Must run in O(N log N) where N = len(attempts), M = len(recoveryCodes)."
prereqs:
  - 08-pattern-binary-search/01-pattern
difficulty: medium
kind: problem
topics: [binary-search, searching]
---

# Recovery Validation

## Problem Statement

Given a sorted array `recoveryCodes` and an array `attempts`, return `true` if any attempt is in the recovery codes list. **Must run in `O(N log M)`** where `N = len(attempts)`, `M = len(recoveryCodes)`.

## Examples

**Example 1**
```
Input:  recoveryCodes = [1, 4, 7], attempts = [2, 4]
Output: true
Explanation: 4 is found in recoveryCodes via binary search, so at least one attempt succeeded.
```

**Example 2**
```
Input:  recoveryCodes = [1, 2, 3], attempts = [5, 6]
Output: false
Explanation: Neither 5 nor 6 is present in recoveryCodes; binary search returns -1 for each.
```

## Constraints

- `1 ≤ recoveryCodes.length ≤ 10^5`
- `0 ≤ attempts.length ≤ 10^5`
- `-10^9 ≤ recoveryCodes[i], attempts[i] ≤ 10^9`
- `recoveryCodes` is sorted in ascending order.

```python run viz=array
import ast
from typing import List

class Solution:
    def recovery_validation(
        self, recovery_codes: List[int], attempts: List[int]
    ) -> bool:
        # Your code goes here — for each attempt, binary-search recoveryCodes.
        # Return True on the first match, False if none found.
        return False

recovery_codes = ast.literal_eval(input())
attempts = ast.literal_eval(input())
print("true" if Solution().recovery_validation(recovery_codes, attempts) else "false")
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public boolean recoveryValidation(int[] recoveryCodes, int[] attempts) {
            // Your code goes here — for each attempt, binary-search recoveryCodes.
            // Return true on the first match, false if none found.
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] recoveryCodes = parseIntArray(sc.nextLine());
        int[] attempts = parseIntArray(sc.nextLine());
        System.out.println(new Solution().recoveryValidation(recoveryCodes, attempts));
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

```testcases
{
  "args": [
    { "id": "recoveryCodes", "label": "recoveryCodes", "type": "int[]", "placeholder": "[1, 4, 7]" },
    { "id": "attempts", "label": "attempts", "type": "int[]", "placeholder": "[2, 4]" }
  ],
  "cases": [
    { "args": { "recoveryCodes": "[1, 4, 7]", "attempts": "[2, 4]" }, "expected": "true" },
    { "args": { "recoveryCodes": "[5, 9, 11, 12]", "attempts": "[2, 9, 12]" }, "expected": "true" },
    { "args": { "recoveryCodes": "[1, 2, 3]", "attempts": "[5, 6]" }, "expected": "false" },
    { "args": { "recoveryCodes": "[1, 2, 3]", "attempts": "[]" }, "expected": "false" },
    { "args": { "recoveryCodes": "[5]", "attempts": "[5]" }, "expected": "true" },
    { "args": { "recoveryCodes": "[1, 3, 5, 7]", "attempts": "[2, 4, 6]" }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The recovery codes are already sorted — that's the signal to binary-search rather than scan. For each attempt, binary-search `recoveryCodes` in `O(log M)` and return `true` on the first hit. The loop over `N` attempts drives the total to `O(N log M)`, which beats the naive `O(N × M)` linear scan. The key insight is that the sorted structure of `recoveryCodes` is an asset: it lets you eliminate half the candidate codes per comparison rather than checking them one by one.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Linear scan over `attempts`; binary-search each one against `recoveryCodes`. Total: `O(N log M)`. Stop on first match.


```python solution time=O(N log M) space=O(1)
import ast
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


recovery_codes = ast.literal_eval(input())
attempts = ast.literal_eval(input())
print("true" if Solution().recovery_validation(recovery_codes, attempts) else "false")
```

```java solution
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
        Scanner sc = new Scanner(System.in);
        int[] recoveryCodes = parseIntArray(sc.nextLine());
        int[] attempts = parseIntArray(sc.nextLine());
        System.out.println(new Solution().recoveryValidation(recoveryCodes, attempts));
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

`O(N log M)` time where `N = len(attempts)`, `M = len(recoveryCodes)`. `O(1)` space.

</details>
