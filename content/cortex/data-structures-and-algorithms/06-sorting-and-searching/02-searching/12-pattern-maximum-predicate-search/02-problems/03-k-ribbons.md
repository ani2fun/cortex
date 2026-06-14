---
title: "K Ribbons"
summary: "Array of ribbons. Cut them to produce at least k pieces of equal length. Return the *maximum* such length, or 0 if impossible."
prereqs:
  - 12-pattern-maximum-predicate-search/01-pattern
difficulty: medium
kind: problem
topics: [maximum-predicate-search, searching]
---

# K Ribbons

## Problem Statement

Given an array of ribbon lengths, cut them into pieces of equal length to produce at least `k` pieces. Return the *maximum* possible piece length, or `0` if impossible.

## Examples

**Example 1**
```
Input:  ribbons = [9, 7, 5], k = 3
Output: 5
Explanation: Cut each ribbon at length 5: from 9 get 1 piece (9//5=1), from 7 get 1 piece (7//5=1), from 5 get 1 piece — total 3 pieces. Length 6 gives only 9//6=1, 7//6=1, 5//6=0 = 2 pieces, not enough.
```

**Example 2**
```
Input:  ribbons = [9, 7, 5], k = 4
Output: 4
Explanation: At length 4: 9//4=2, 7//4=1, 5//4=1 = 4 pieces. At length 5 only 3 pieces fit.
```

## Constraints

- `1 ≤ ribbons.length ≤ 10^5`
- `1 ≤ ribbons[i] ≤ 10^5`
- `1 ≤ k ≤ 10^9`

```python run viz=array viz-root=ribbons
import ast
from typing import List

class Solution:
    def k_ribbons(self, ribbons: List[int], k: int) -> int:
        # Your code goes here — binary-search length in [1, 1e7].
        # Predicate: sum(r // length for r in ribbons) >= k.
        # Return the largest feasible length, or 0 if none works.
        return -1

ribbons = ast.literal_eval(input())
k = int(input())
print(Solution().k_ribbons(ribbons, k))
```

```java run viz=array viz-root=ribbons
import java.util.*;

public class Main {
    static class Solution {
        public int kRibbons(int[] ribbons, int k) {
            // Your code goes here — binary-search length in [1, 1e7].
            // Predicate: sum of r/length for each ribbon >= k.
            // Return the largest feasible length, or 0 if none works.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] ribbons = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kRibbons(ribbons, k));
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
    { "id": "ribbons", "label": "ribbons", "type": "int[]", "placeholder": "[9, 7, 5]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "ribbons": "[9, 7, 5]", "k": "3" }, "expected": "5" },
    { "args": { "ribbons": "[9, 7, 5]", "k": "4" }, "expected": "4" },
    { "args": { "ribbons": "[9, 7, 5]", "k": "30" }, "expected": "0" },
    { "args": { "ribbons": "[1]", "k": "1" }, "expected": "1" },
    { "args": { "ribbons": "[10]", "k": "1" }, "expected": "10" },
    { "args": { "ribbons": "[10]", "k": "3" }, "expected": "3" },
    { "args": { "ribbons": "[5, 5, 5]", "k": "3" }, "expected": "5" },
    { "args": { "ribbons": "[1, 2, 3]", "k": "10" }, "expected": "0" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

As the cut length grows, the total number of pieces you can get decreases monotonically — longer pieces means fewer cuts fit. So the predicate "can we get at least `k` pieces at length `L`?" is `true` for small `L` and flips to `false` once `L` is too large. Binary-search `[1, 10^7]` for the last length where the predicate holds, tracking the best. After the loop, re-check: if even `low` doesn't work, no valid length exists and the answer is `0`.

</details>
<details>
<summary><h2>The Solution</h2></summary>

Predicate: "can we cut at least `k` ribbons of length `length`?" — sum of `r // length` for each ribbon. Binary-search `length` in `[1, 10^7]` (a safe ceiling from the problem constraints — wider than `max(ribbons)` but still `O(log range)`). After the loop, re-check the predicate at `low` to distinguish "no length works" (return `0`) from a real answer.

```python solution time=O(n log(1e7)) space=O(1)
import ast
from typing import List

class Solution:

    # Predicate: checks if it's possible to cut at least 'k' ribbons of
    # length 'length'
    def can_cut(self, ribbons: List[int], length: int, k: int) -> bool:
        count = 0

        # Count how many pieces of 'length' we can cut from each ribbon
        for ribbon in ribbons:
            count += ribbon // length

        # Return true if we can cut at least 'k' ribbons of this length
        return count >= k

    def k_ribbons(self, ribbons: List[int], k: int) -> int:

        # Initialize the search range for ribbon lengths
        low = 1

        # Initialize the search range for ribbon lengths
        high = int(1e7)

        while low < high:

            # Calculate the middle value by adding 1 to get upper mid to
            # prevent infinite loop when low and high are adjacent
            mid = low + (high - low + 1) // 2

            # If we can cut at least 'k' ribbons of length 'mid' it is a
            # possible answer, so update the lower boundary to mid
            if self.can_cut(ribbons, mid, k):

                # Try to find a larger length
                low = mid

            # Otherwise, we can't cut 'k' ribbons of length 'mid' from
            # the given ribbons array.
            else:

                # Try to find a smaller length
                high = mid - 1

        # After the search, low is the maximum length we can cut
        # Check if we can actually cut at least 'k' ribbons of this
        # length
        if not self.can_cut(ribbons, low, k):
            return 0

        # Return the maximum ribbon length that can be obtained
        return low


ribbons = ast.literal_eval(input())
k = int(input())
print(Solution().k_ribbons(ribbons, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {

        // Predicate: checks if it's possible to cut at least 'k' ribbons of
        // length 'length'
        private boolean canCut(int[] ribbons, int length, int k) {
            int count = 0;

            // Count how many pieces of 'length' we can cut from each ribbon
            for (int ribbon : ribbons) {
                count += ribbon / length;
            }

            // Return true if we can cut at least 'k' ribbons of this length
            return count >= k;
        }

        public int kRibbons(int[] ribbons, int k) {

            // Initialize the search range for ribbon lengths
            int low = 1;

            // Initialize the search range for ribbon lengths
            int high = (int) 1e7;

            while (low < high) {

                // Calculate the middle value by adding 1 to get upper mid to
                // prevent infinite loop when low and high are adjacent
                int mid = low + (high - low + 1) / 2;

                // If we can cut at least 'k' ribbons of length 'mid' it is a
                // possible answer, so update the lower boundary to mid
                if (canCut(ribbons, mid, k)) {

                    // Try to find a larger length
                    low = mid;
                }

                // Otherwise, we can't cut 'k' ribbons of length 'mid' from
                // the given ribbons array.
                else {

                    // Try to find a smaller length
                    high = mid - 1;
                }
            }

            // After the search, low is the maximum length we can cut
            // Check if we can actually cut at least 'k' ribbons of this
            // length
            if (!canCut(ribbons, low, k)) {
                return 0;
            }

            // Return the maximum ribbon length that can be obtained
            return low;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] ribbons = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kRibbons(ribbons, k));
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

</details>
