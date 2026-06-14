---
title: "Practice — Mixed DP Problems"
summary: "Seven dynamic-programming problems, one from each family the section covered — the meta-skill is naming the pattern before deriving the recurrence. Full Python and Java implementations for each."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/01-linear-dp
---

# 19. Practice — Mixed DP Problems

You've seen 18 lessons covering an array of DP shapes — linear DP, longest-subsequence variants, palindrome problems, partition problems, the knapsack family, game-theoretic adversarial DP, split-point interval DP, and three meta-patterns (edit-distance, subset-sum, 2D-grid, prefix-sum). The patterns reappear in disguise across hundreds of competitive-programming and interview problems. The goal of this final lesson is mileage: seven problems, each from a different family, with full implementations.

Each problem in this set was chosen to test pattern recognition. Before reading the solution, see if you can identify which pattern it is — that recognition is the meta-skill the entire section was building toward. **You won't always get the recurrence right on the first try, but you should *always* be able to name the family it belongs to within seconds.** That's the bar.

## Table of contents

1. [Covering Distance](#covering-distance)
2. [Reachability Check](#reachability-check)
3. [Longest Bitonic Subsequence](#longest-bitonic-subsequence)
4. [Longest Alternating Subsequence](#longest-alternating-subsequence)
5. [Pattern as Subsequence](#pattern-as-subsequence)
6. [Shortest Common Supersequence](#shortest-common-supersequence)
7. [Longest Repeated Subsequence](#longest-repeated-subsequence)
8. [Key Takeaway](#key-takeaway)

***

# Covering Distance

> **Pattern:** Linear DP, count-aggregator

## The Problem

Given a positive integer `distance`, count the ways to cover it using steps of size 1, 2, or 3.

```
Input:  distance = 3
Output: 4               (1+1+1), (1+2), (2+1), (3)

Input:  distance = 2
Output: 2               (1+1), (2)

Input:  distance = 1
Output: 1               (1)
```

<details>
<summary><h2>The Recurrence</h2></summary>


`dp[i]` = ways to reach distance `i`. The last step is 1, 2, or 3 — sum the ways from each predecessor:
```
dp[i] = dp[i - 1] + dp[i - 2] + dp[i - 3]
```
With `dp[0] = 1` (one way to "stand still") and missing predecessors treated as 0. (Note this is a Tribonacci-flavoured recurrence — same shape as Fibonacci with one extra step.)

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array viz-root=dp
from typing import List

class Solution:
    def covering_distance(self, distance: int) -> int:

        # Create a list to store the number of ways to reach each
        # distance
        dp: List[int] = [0] * (distance + 1)

        # There is only one way to reach distance 0, which is to not move
        dp[0] = 1

        # Iterate through each distance from 1 to 'dist'
        for i in range(1, distance + 1):

            # The number of ways to reach the current distance is
            # initially the same as the number of ways to reach the
            # previous distance
            dp[i] = dp[i - 1]

            # Check if it is possible to take a step of 2 units
            if i >= 2:

                # If it is possible, add the number of ways to reach the
                # distance 'i - 2' to the current number of ways
                dp[i] += dp[i - 2]

            # Check if it is possible to take a step of 3 units
            if i >= 3:

                # If it is possible, add the number of ways to reach the
                # distance 'i - 3' to the current number of ways
                dp[i] += dp[i - 3]

        # The final element of the 'dp' list will contain the total
        # number of ways to reach the given distance 'dist'
        return dp[distance]


distance = int(input())
print(Solution().covering_distance(distance))
```

```java run viz=array viz-root=dp
import java.util.*;

public class Main {
    static class Solution {
        public int coveringDistance(int distance) {

            // Create an array to store the number of ways to reach each
            // distance
            int[] dp = new int[distance + 1];

            // There is only one way to reach distance 0, which is to not
            // move
            dp[0] = 1;

            // Iterate through each distance from 1 to 'dist'
            for (int i = 1; i <= distance; i++) {

                // The number of ways to reach the current distance is
                // initially the same as the number of ways to reach the
                // previous distance
                dp[i] = dp[i - 1];

                // Check if it is possible to take a step of 2 units
                if (i >= 2) {

                    // If it is possible, add the number of ways to reach the
                    // distance 'i - 2' to the current number of ways
                    dp[i] += dp[i - 2];
                }

                // Check if it is possible to take a step of 3 units
                if (i >= 3) {

                    // If it is possible, add the number of ways to reach the
                    // distance 'i - 3' to the current number of ways
                    dp[i] += dp[i - 3];
                }
            }

            // The final element of the 'dp' array will contain the total
            // number of ways to reach the given distance 'dist'
            return dp[distance];
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int distance = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().coveringDistance(distance));
    }
}
```

```testcases
{
  "args": [
    { "id": "distance", "label": "distance", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "distance": "3" }, "expected": "4" },
    { "args": { "distance": "2" }, "expected": "2" },
    { "args": { "distance": "1" }, "expected": "1" },
    { "args": { "distance": "5" }, "expected": "13" },
    { "args": { "distance": "10" }, "expected": "274" }
  ]
}
```

</details>


***

# Reachability Check

> **Pattern:** Linear DP, boolean

## The Problem

Given an array `arr` where `arr[i]` is the maximum forward jump from index `i`, return `true` if the last index is reachable from index 0.

```
Input:  arr = [1, 5, 8, 9]
Output: true               0 → 1 → 3

Input:  arr = [2, 0, 1, 1]
Output: true               0 → 2 → 3

Input:  arr = [2, 0, 0, 1]
Output: false              Stuck at index 1 with arr[1] = 0
```

<details>
<summary><h2>The Recurrence</h2></summary>


`dp[i]` = whether the end is reachable from index `i`. Working backward, `dp[n - 1] = true`. For `i < n - 1`, `dp[i] = OR over j ∈ [i+1, i + arr[i]] of dp[j]`.

(There's a slick O(n) greedy alternative — track the farthest reachable index — but the DP form generalises better.)

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array viz-root=dp
from typing import List
import ast

class Solution:
    def reachability_check(self, arr: List[int]) -> bool:
        n: int = len(arr)
        dp: List[bool] = [False] * n
        dp[n - 1] = True

        # Starting from the second-to-last element and moving towards the
        # first element
        for i in range(n - 2, -1, -1):

            # Determine the maximum index we can jump to from the current
            # position
            max_jump: int = min(i + arr[i], n - 1)

            # Check all possible indices we can reach from the current
            # position
            for j in range(i + 1, max_jump + 1):

                # If we can reach an index that is marked as true in dp,
                # set dp[i] to true
                if dp[j]:
                    dp[i] = True
                    break

        # Return whether we can reach the first element (index 0)
        # starting from the last element
        return dp[0]


arr = ast.literal_eval(input())
result = Solution().reachability_check(arr)
print("true" if result else "false")
```

```java run viz=array viz-root=dp
import java.util.*;

public class Main {
    static class Solution {
        public boolean reachabilityCheck(int[] arr) {
            int n = arr.length;
            boolean[] dp = new boolean[n];
            dp[n - 1] = true;

            // Starting from the second-to-last element and moving towards
            // the first element
            for (int i = n - 2; i >= 0; i--) {

                // Determine the maximum index we can jump to from the
                // current position
                int maxJump = Math.min(i + arr[i], n - 1);

                // Check all possible indices we can reach from the current
                // position
                for (int j = i + 1; j <= maxJump; j++) {

                    // If we can reach an index that is marked as true in dp,
                    // set dp[i] to true
                    if (dp[j]) {
                        dp[i] = true;
                        break;
                    }
                }
            }

            // Return whether we can reach the first element (index 0)
            // starting from the last element
            return dp[0];
        }
    }

    static int[] parseIntArray(String s) {
        s = s.trim();
        if (s.equals("[]")) return new int[0];
        s = s.substring(1, s.length() - 1);
        String[] parts = s.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
        return arr;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine().trim());
        System.out.println(new Solution().reachabilityCheck(arr));
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 5, 8, 9]" }
  ],
  "cases": [
    { "args": { "arr": "[1, 5, 8, 9]" }, "expected": "true" },
    { "args": { "arr": "[2, 0, 1, 1]" }, "expected": "true" },
    { "args": { "arr": "[2, 0, 0, 1]" }, "expected": "false" },
    { "args": { "arr": "[0]" }, "expected": "true" },
    { "args": { "arr": "[0, 1]" }, "expected": "false" }
  ]
}
```

</details>


***

# Longest Bitonic Subsequence

> **Pattern:** LIS variant — two passes (LIS + LDS)

## The Problem

A *bitonic* subsequence rises then falls (e.g. `1, 3, 5, 9, 8, 6`). Find the length of the longest bitonic subsequence in `arr`.

```
Input:  arr = [1, 7, 3, 5, 9, 8, 6]
Output: 6                            [1, 3, 5, 9, 8, 6]

Input:  arr = [1, 2, 3]
Output: 3                            Already increasing — bitonic with empty descent

Input:  arr = [3, 2, 1]
Output: 3                            Already decreasing — bitonic with empty ascent
```

<details>
<summary><h2>The Recurrence</h2></summary>


For each peak candidate `i`, the bitonic length through `i` = `LIS_ending_at(i) + LDS_starting_at(i) - 1` (the peak is counted twice). Compute LIS forward and LDS backward; max over all `i`.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array viz-root=arr
from typing import List
import ast

class Solution:
    def longest_bitonic_subsequence(self, arr: List[int]) -> int:
        n: int = len(arr)

        # List to store the lengths of the longest increasing
        # subsequences
        increasing: List[int] = [1] * n

        # List to store the lengths of the longest decreasing
        # subsequences
        decreasing: List[int] = [1] * n

        # Calculate the lengths of the longest increasing subsequences
        for i in range(1, n):
            for j in range(i):

                # If the current element is greater than the previous
                # element and the length of the increasing subsequence
                # ending at the previous element plus one is greater
                # than the current length of the increasing subsequence,
                # update the current length of the increasing
                # subsequence
                if arr[i] > arr[j] and increasing[i] < increasing[j] + 1:
                    increasing[i] = increasing[j] + 1

        # Calculate the lengths of the longest decreasing subsequences
        for i in range(n - 2, -1, -1):
            for j in range(n - 1, i, -1):

                # If the current element is greater than the next
                # element and the length of the decreasing subsequence
                # starting at the next element plus one is greater than
                # the current length of the decreasing subsequence,
                # update the current length of the decreasing
                # subsequence
                if arr[i] > arr[j] and decreasing[i] < decreasing[j] + 1:
                    decreasing[i] = decreasing[j] + 1

        max_bitonic_len: int = 0

        # Calculate the length of the bitonic subsequence by adding the
        # lengths of the increasing and decreasing subsequences and
        # subtracting 1 because the peak element is counted twice
        for i in range(n):
            bitonic_len = increasing[i] + decreasing[i] - 1

            # Update the maximum bitonic length if a longer bitonic
            # subsequence is found
            if bitonic_len > max_bitonic_len:
                max_bitonic_len = bitonic_len

        return max_bitonic_len


arr = ast.literal_eval(input())
print(Solution().longest_bitonic_subsequence(arr))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public int longestBitonicSubsequence(int[] arr) {
            int n = arr.length;

            // Array to store the lengths of the longest increasing
            // subsequences
            int[] increasing = new int[n];

            // Array to store the lengths of the longest decreasing
            // subsequences
            int[] decreasing = new int[n];

            Arrays.fill(increasing, 1);
            Arrays.fill(decreasing, 1);

            // Calculate the lengths of the longest increasing subsequences
            for (int i = 1; i < n; i++) {
                for (int j = 0; j < i; j++) {

                    // If the current element is greater than the previous
                    // element and the length of the increasing subsequence
                    // ending at the previous element plus one is greater
                    // than the current length of the increasing subsequence,
                    // update the current length of the increasing
                    // subsequence
                    if (
                        arr[i] > arr[j] && increasing[i] < increasing[j] + 1
                    ) increasing[i] = increasing[j] + 1;
                }
            }

            // Calculate the lengths of the longest decreasing subsequences
            for (int i = n - 2; i >= 0; i--) {
                for (int j = n - 1; j > i; j--) {

                    // If the current element is greater than the next
                    // element and the length of the decreasing subsequence
                    // starting at the next element plus one is greater than
                    // the current length of the decreasing subsequence,
                    // update the current length of the decreasing
                    // subsequence
                    if (
                        arr[i] > arr[j] && decreasing[i] < decreasing[j] + 1
                    ) decreasing[i] = decreasing[j] + 1;
                }
            }

            int maxBitonicLen = 0;

            // Calculate the length of the bitonic subsequence by adding the
            // lengths of the increasing and decreasing subsequences and
            // subtracting 1 because the peak element is counted twice
            for (int i = 0; i < n; i++) {
                int bitonicLen = increasing[i] + decreasing[i] - 1;

                // Update the maximum bitonic length if a longer bitonic
                // subsequence is found
                if (bitonicLen > maxBitonicLen) maxBitonicLen = bitonicLen;
            }

            return maxBitonicLen;
        }
    }

    static int[] parseIntArray(String s) {
        s = s.trim();
        if (s.equals("[]")) return new int[0];
        s = s.substring(1, s.length() - 1);
        String[] parts = s.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
        return arr;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine().trim());
        System.out.println(new Solution().longestBitonicSubsequence(arr));
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 7, 3, 5, 9, 8, 6]" }
  ],
  "cases": [
    { "args": { "arr": "[1, 7, 3, 5, 9, 8, 6]" }, "expected": "6" },
    { "args": { "arr": "[1, 2, 3]" }, "expected": "3" },
    { "args": { "arr": "[3, 2, 1]" }, "expected": "3" },
    { "args": { "arr": "[1, 3, 1]" }, "expected": "3" },
    { "args": { "arr": "[5, 5, 5, 5]" }, "expected": "1" }
  ]
}
```

</details>


***

# Longest Alternating Subsequence

> **Pattern:** LIS-style 2D DP — track parity of last move

## The Problem

An *alternating* subsequence has neighbours that strictly alternate up/down (`a < b > c < d`, etc.). Find the length of the longest alternating subsequence in `arr`.

```
Input:  arr = [1, 7, 3, 5, 4, 8, 6]
Output: 7                          The whole array is alternating

Input:  arr = [1, 4, 5, 3]
Output: 3                          [1, 4, 3] or [1, 5, 3]

Input:  arr = [3, 2, 1]
Output: 2                          Just [3, 2] or [2, 1]
```

<details>
<summary><h2>The Recurrence</h2></summary>


`dp[i][0]` = longest alternating subseq ending at `i` with the last move being a *decrease*. `dp[i][1]` = ending at `i` with last move *increase*. Both initialise to 1.

For each `i`, scan all `j < i`:
- `arr[j] < arr[i]` → extending an "ending in decrease" subseq with an increase: `dp[i][1] = max(dp[i][1], dp[j][0] + 1)`.
- `arr[j] > arr[i]` → extending an "ending in increase" subseq with a decrease: `dp[i][0] = max(dp[i][0], dp[j][1] + 1)`.

Answer: max over `i` of `max(dp[i][0], dp[i][1])`.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=grid viz-root=dp
from typing import List
import ast

class Solution:
    def longest_alternating_subsequence(self, arr: List[int]) -> int:
        n: int = len(arr)
        if n <= 1:

            # If the array has only 0 or 1 element, the length of the
            # longest alternating subsequence is equal to the number of
            # elements in the array.
            return n

        # dp[i][0] represents the length of the longest alternating
        # subsequence ending at index i, with the last element being
        # smaller than its previous element. dp[i][1] represents the
        # length of the longest alternating subsequence ending at index
        # i, with the last element being greater than its previous
        # element.
        dp: List[List[int]] = [[1, 1] for _ in range(n)]

        # Initialize the maximum length to 1 since we have at least one
        # element in the array.
        max_length: int = 1

        for i in range(1, n):
            for j in range(i):
                if arr[j] < arr[i]:

                    # If arr[j] < arr[i], it means we can add arr[i] to
                    # the subsequence ending at index j to form a longer
                    # subsequence ending at index i, with the last
                    # element being greater.
                    dp[i][1] = max(dp[i][1], dp[j][0] + 1)
                elif arr[j] > arr[i]:

                    # If arr[j] > arr[i], it means we can add arr[i] to
                    # the subsequence ending at index j to form a longer
                    # subsequence ending at index i, with the last
                    # element being smaller.
                    dp[i][0] = max(dp[i][0], dp[j][1] + 1)

            # Update the maximum length with the maximum of dp[i][0] and
            # dp[i][1] (the longest alternating subsequences ending at
            # index i).
            max_length = max(max_length, max(dp[i][0], dp[i][1]))

        # Return the maximum length of the longest alternating
        # subsequence.
        return max_length


arr = ast.literal_eval(input())
print(Solution().longest_alternating_subsequence(arr))
```

```java run viz=grid viz-root=dp
import java.util.*;

public class Main {
    static class Solution {
        public int longestAlternatingSubsequence(int[] arr) {
            int n = arr.length;
            if (n <= 1) {

                // If the array has only 0 or 1 element, the length of the
                // longest alternating subsequence is equal to the number of
                // elements in the array.
                return n;
            }

            // dp[i][0] represents the length of the longest alternating
            // subsequence ending at index i, with the last element being
            // smaller than its previous element. dp[i][1] represents the
            // length of the longest alternating subsequence ending at index
            // i, with the last element being greater than its previous
            // element.
            int[][] dp = new int[n][2];

            // Initialize the maximum length to 1 since we have at least one
            // element in the array.
            int maxLength = 1;

            for (int i = 0; i < n; i++) {

                // Initialize all values to 1
                Arrays.fill(dp[i], 1);
            }

            for (int i = 1; i < n; i++) {
                for (int j = 0; j < i; j++) {
                    if (arr[j] < arr[i]) {

                        // If arr[j] < arr[i], it means we can add arr[i] to
                        // the subsequence ending at index j to form a longer
                        // subsequence ending at index i, with the last
                        // element being greater.
                        dp[i][1] = Math.max(dp[i][1], dp[j][0] + 1);
                    } else if (arr[j] > arr[i]) {

                        // If arr[j] > arr[i], it means we can add arr[i] to
                        // the subsequence ending at index j to form a longer
                        // subsequence ending at index i, with the last
                        // element being smaller.
                        dp[i][0] = Math.max(dp[i][0], dp[j][1] + 1);
                    }
                }

                // Update the maximum length with the maximum of dp[i][0] and
                // dp[i][1] (the longest alternating subsequences ending at
                // index i).
                maxLength = Math.max(
                    maxLength,
                    Math.max(dp[i][0], dp[i][1])
                );
            }

            // Return the maximum length of the longest alternating
            // subsequence.
            return maxLength;
        }
    }

    static int[] parseIntArray(String s) {
        s = s.trim();
        if (s.equals("[]")) return new int[0];
        s = s.substring(1, s.length() - 1);
        String[] parts = s.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
        return arr;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine().trim());
        System.out.println(new Solution().longestAlternatingSubsequence(arr));
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 7, 3, 5, 4, 8, 6]" }
  ],
  "cases": [
    { "args": { "arr": "[1, 7, 3, 5, 4, 8, 6]" }, "expected": "7" },
    { "args": { "arr": "[1, 4, 5, 3]" }, "expected": "3" },
    { "args": { "arr": "[3, 2, 1]" }, "expected": "2" },
    { "args": { "arr": "[5, 5, 5]" }, "expected": "1" },
    { "args": { "arr": "[1, 2, 1, 2, 1]" }, "expected": "5" }
  ]
}
```

</details>


***

# Pattern as Subsequence

> **Pattern:** Edit-distance pattern — count of subsequence matches

## The Problem

Given strings `s` and `pattern`, count the number of *distinct* subsequences of `s` that equal `pattern`.

```
Input:  s = "abacdebgc", pattern = "abc"
Output: 4

Input:  s = "xyzabc", pattern = "xzc"
Output: 1

Input:  s = "abc", pattern = "def"
Output: 0
```

<details>
<summary><h2>The Recurrence</h2></summary>


`dp[i][j]` = ways to form `pattern[0..i-1]` as a subsequence of `s[0..j-1]`. Two cases:
- `pattern[i-1] == s[j-1]`: include the match (`dp[i-1][j-1]`) or skip `s[j-1]` (`dp[i][j-1]`). Sum.
- Else: `dp[i][j] = dp[i][j-1]`.

Base case: `dp[0][j] = 1` for all `j` — there's exactly one way to form the empty pattern (pick nothing).

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=grid viz-root=dp
from typing import List

class Solution:
    def pattern_as_subsequence(self, s: str, pattern: str) -> int:

        # Length of pattern
        m: int = len(pattern)

        # Length of string s
        n: int = len(s)

        # Create a 2D list dp to store the dynamic programming values
        dp: List[List[int]] = [[0] * (n + 1) for _ in range(m + 1)]

        # Initialize the base case where pattern is empty (i = 0)
        # If string s is empty (j = 0), there is one subsequence pattern
        # (empty pattern) Otherwise, if string s is not empty, there are
        # no subsequence patterns since pattern is empty
        for i in range(n + 1):
            dp[0][i] = 1

        # Fill the dp table using dynamic programming
        for i in range(1, m + 1):
            for j in range(1, n + 1):
                if pattern[i - 1] == s[j - 1]:

                    # If the current characters match, we have two
                    # choices:
                    # 1. Include the current characters in both pattern
                    # and string, so the count is dp[i - 1][j - 1]
                    # 2. Exclude the current character from the pattern,
                    # so the count is dp[i][j - 1]
                    dp[i][j] = dp[i - 1][j - 1] + dp[i][j - 1]
                else:

                    # If the current characters don't match, we can only
                    # exclude the current character from the string The
                    # count remains the same as the previous count
                    # without considering the current character
                    dp[i][j] = dp[i][j - 1]

        # The final count is stored in dp[m][n], which represents the
        # number of subsequence patterns
        return dp[m][n]


s = input()
pattern = input()
print(Solution().pattern_as_subsequence(s, pattern))
```

```java run viz=grid viz-root=dp
import java.util.*;

public class Main {
    static class Solution {
        public int patternAsSubsequence(String s, String pattern) {

            // Length of pattern
            int m = pattern.length();

            // Length of string s
            int n = s.length();

            // Create a 2D array dp to store the dynamic programming values
            int[][] dp = new int[m + 1][n + 1];

            // Initialize the base case where pattern is empty (i = 0)
            // If string s is empty (j = 0), there is one subsequence pattern
            // (empty pattern) Otherwise, if string s is not empty, there are
            // no subsequence patterns since pattern is empty
            for (int i = 0; i <= n; i++) {
                dp[0][i] = 1;
            }

            // Fill the dp table using dynamic programming
            for (int i = 1; i <= m; i++) {
                for (int j = 1; j <= n; j++) {
                    if (pattern.charAt(i - 1) == s.charAt(j - 1)) {

                        // If the current characters match, we have two
                        // choices:
                        // 1. Include the current characters in both pattern
                        // and string, so the count is dp[i - 1][j - 1]
                        // 2. Exclude the current character from the pattern,
                        // so the count is dp[i][j - 1]
                        dp[i][j] = dp[i - 1][j - 1] + dp[i][j - 1];
                    } else {

                        // If the current characters don't match, we can only
                        // exclude the current character from the string The
                        // count remains the same as the previous count
                        // without considering the current character
                        dp[i][j] = dp[i][j - 1];
                    }
                }
            }

            // The final count is stored in dp[m][n], which represents the
            // number of subsequence patterns
            return dp[m][n];
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        String pattern = sc.nextLine();
        System.out.println(new Solution().patternAsSubsequence(s, pattern));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "abacdebgc" },
    { "id": "pattern", "label": "pattern", "type": "string", "placeholder": "abc" }
  ],
  "cases": [
    { "args": { "s": "abacdebgc", "pattern": "abc" }, "expected": "4" },
    { "args": { "s": "xyzabc", "pattern": "xzc" }, "expected": "1" },
    { "args": { "s": "abc", "pattern": "def" }, "expected": "0" },
    { "args": { "s": "aaa", "pattern": "a" }, "expected": "3" },
    { "args": { "s": "abc", "pattern": "abc" }, "expected": "1" }
  ]
}
```

</details>


***

# Shortest Common Supersequence

> **Pattern:** Edit-distance pattern — minimum-length supersequence

## The Problem

Given two strings, return the *length* of the shortest string that contains both as subsequences.

```
Input:  s1 = "abc", s2 = "abe"
Output: 4                          E.g. "abce"

Input:  s1 = "lmn", s2 = "opq"
Output: 6                          No shared chars; concat works

Input:  s1 = "aab", s2 = "aab"
Output: 3                          They're identical
```

<details>
<summary><h2>The Recurrence</h2></summary>


`dp[i][j]` = length of the shortest common supersequence of `s1[0..i-1]` and `s2[0..j-1]`. Match keeps both characters once; mismatch grows by one (forced to take one or the other):
```
dp[i][j] = dp[i-1][j-1] + 1                     if s1[i-1] == s2[j-1]
         = min(dp[i-1][j], dp[i][j-1]) + 1      otherwise
```
Base cases: `dp[i][0] = i`, `dp[0][j] = j` (an empty side forces taking everything from the other).

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=grid viz-root=dp
from typing import List

class Solution:
    def shortest_common_supersequence(self, s1: str, s2: str) -> int:
        n: int = len(s1)
        m: int = len(s2)

        # Create a 2D array to store the dynamic programming values
        dp: List[List[int]] = [[0] * (m + 1) for _ in range(n + 1)]

        # Initialize the base cases
        # If one of the strings is empty, the length of the shortest
        # common supersequence is the length of the other string
        for i in range(n + 1):
            dp[i][0] = i
        for j in range(m + 1):
            dp[0][j] = j

        # Calculate the lengths of the shortest common supersequences
        for i in range(1, n + 1):
            for j in range(1, m + 1):

                # If the characters at the current positions are equal,
                # the length of the supersequence is one more than the
                # length of the supersequence without these characters
                if s1[i - 1] == s2[j - 1]:
                    dp[i][j] = dp[i - 1][j - 1] + 1
                else:

                    # If the characters are different, we have two
                    # choices:
                    # 1. Include the current character from s1 and find
                    # the shortest supersequence for the remaining
                    # characters
                    # 2. Include the current character from s2 and find
                    # the shortest supersequence for the remaining
                    # characters We choose the option that results in
                    # the minimum length
                    dp[i][j] = min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)

        # Return the length of the shortest common supersequence
        return dp[n][m]


s1 = input()
s2 = input()
print(Solution().shortest_common_supersequence(s1, s2))
```

```java run viz=grid viz-root=dp
import java.util.*;

public class Main {
    static class Solution {
        public int shortestCommonSupersequence(String s1, String s2) {
            int n = s1.length();
            int m = s2.length();

            // Create a 2D array to store the dynamic programming values
            int[][] dp = new int[n + 1][m + 1];

            // Initialize the base cases
            // If one of the strings is empty, the length of the shortest
            // common supersequence is the length of the other string
            for (int i = 0; i <= n; i++) {
                dp[i][0] = i;
            }
            for (int j = 0; j <= m; j++) {
                dp[0][j] = j;
            }

            // Calculate the lengths of the shortest common supersequences
            for (int i = 1; i <= n; i++) {
                for (int j = 1; j <= m; j++) {

                    // If the characters at the current positions are equal,
                    // the length of the supersequence is one more than the
                    // length of the supersequence without these characters
                    if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                        dp[i][j] = dp[i - 1][j - 1] + 1;
                    } else {

                        // If the characters are different, we have two
                        // choices:
                        // 1. Include the current character from s1 and find
                        // the shortest supersequence for the remaining
                        // characters
                        // 2. Include the current character from s2 and find
                        // the shortest supersequence for the remaining
                        // characters We choose the option that results in
                        // the minimum length
                        dp[i][j] = Math.min(
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1
                        );
                    }
                }
            }

            // Return the length of the shortest common supersequence
            return dp[n][m];
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s1 = sc.nextLine();
        String s2 = sc.nextLine();
        System.out.println(new Solution().shortestCommonSupersequence(s1, s2));
    }
}
```

```testcases
{
  "args": [
    { "id": "s1", "label": "s1", "type": "string", "placeholder": "abc" },
    { "id": "s2", "label": "s2", "type": "string", "placeholder": "abe" }
  ],
  "cases": [
    { "args": { "s1": "abc", "s2": "abe" }, "expected": "4" },
    { "args": { "s1": "lmn", "s2": "opq" }, "expected": "6" },
    { "args": { "s1": "aab", "s2": "aab" }, "expected": "3" },
    { "args": { "s1": "a", "s2": "a" }, "expected": "1" },
    { "args": { "s1": "a", "s2": "b" }, "expected": "2" }
  ]
}
```

</details>


***

# Longest Repeated Subsequence

> **Pattern:** LCS variant — string compared with itself, off-diagonal

## The Problem

Find the longest subsequence of `s` that appears at least *twice* using disjoint indices (the two occurrences must use different positions in `s`).

```
Input:  s = "abxcdalbc"
Output: "abc"                The "abc" appears as positions [0, 1, 4] AND [0, 1, 8] — wait, those overlap.
                             Actually: position [0, 1, 4] for first, [5, 6, 8] for second — disjoint indices.

Input:  s = "xyzlynkz"
Output: "yz"

Input:  s = "abbcc"
Output: "bc"
```

<details>
<summary><h2>The Recurrence</h2></summary>


Compute LCS of `s` with itself, but require `i != j` to enforce disjoint positions:
```
dp[i][j] = dp[i-1][j-1] + 1               if s[i-1] == s[j-1] AND i != j
         = max(dp[i-1][j], dp[i][j-1])    otherwise
```
Then backtrack to reconstruct the subsequence string.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=graph viz-root=dp
from typing import List

class Solution:
    def longest_repeated_subsequence(self, s: str) -> str:
        n: int = len(s)

        # Initialize a 2D list for dynamic programming
        dp: List[List[int]] = [[0] * (n + 1) for _ in range(n + 1)]

        # Iterate over the characters of the string
        for i in range(1, n + 1):

            # Iterate over the characters of the string
            for j in range(1, n + 1):

                # If the characters at positions i and j are equal
                # (excluding the same position)
                if s[i - 1] == s[j - 1] and i != j:

                    # Increment the longest repeating subsequence length
                    # by 1
                    dp[i][j] = 1 + dp[i - 1][j - 1]
                else:

                    # If the characters are not equal, take the maximum
                    # of the previous subsequence lengths
                    dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])

        # Initialize an empty string to store the longest repeating
        # subsequence
        lrs: str = ""

        # Start from the bottom right corner of the DP matrix
        i, j = n, n

        # Traverse back to reconstruct the longest repeating subsequence
        while i > 0 and j > 0:

            # If the current cell value is one more than the diagonal
            # cell
            if dp[i][j] == dp[i - 1][j - 1] + 1:

                # Append the character to the front of the subsequence
                # string
                lrs = s[i - 1] + lrs

                # Move diagonally up-left
                i -= 1
                j -= 1

            # If the current cell value is equal to the cell above
            elif dp[i][j] == dp[i - 1][j]:

                # Move up
                i -= 1

            # If the current cell value is equal to the cell on the left
            else:

                # Move left
                j -= 1

        # Return the longest repeating subsequence
        return lrs


s = input()
print(Solution().longest_repeated_subsequence(s))
```

```java run viz=graph viz-root=dp
import java.util.*;

public class Main {
    static class Solution {
        public String longestRepeatedSubsequence(String s) {
            int n = s.length();

            // Initialize a 2D array for dynamic programming
            int[][] dp = new int[n + 1][n + 1];

            // Iterate over the characters of the string
            for (int i = 1; i <= n; i++) {

                // Iterate over the characters of the string
                for (int j = 1; j <= n; j++) {

                    // If the characters at positions i and j are equal
                    // (excluding the same position)
                    if (s.charAt(i - 1) == s.charAt(j - 1) && i != j) {

                        // Increment the longest repeating subsequence length
                        // by 1
                        dp[i][j] = 1 + dp[i - 1][j - 1];
                    } else {

                        // If the characters are not equal, take the maximum
                        // of the previous subsequence lengths
                        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                    }
                }
            }

            // Initialize an empty string to store the longest repeating
            // subsequence
            StringBuilder lrs = new StringBuilder();

            // Start from the bottom right corner of the DP matrix
            int i = n, j = n;

            // Traverse back to reconstruct the longest repeating subsequence
            while (i > 0 && j > 0) {

                // If the current cell value is one more than the diagonal
                // cell
                if (dp[i][j] == dp[i - 1][j - 1] + 1) {

                    // Append the character to the front of the subsequence
                    // string
                    lrs.insert(0, s.charAt(i - 1));

                    // Move diagonally up-left
                    i--;
                    j--;

                    // If the current cell value is equal to the cell above
                } else if (dp[i][j] == dp[i - 1][j]) {

                    // Move up
                    i--;

                    // If the current cell value is equal to the cell on the
                    // left
                } else {

                    // Move left
                    j--;
                }
            }

            // Return the longest repeating subsequence
            return lrs.toString();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        System.out.println(new Solution().longestRepeatedSubsequence(s));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "abxcdalbc" }
  ],
  "cases": [
    { "args": { "s": "abxcdalbc" }, "expected": "abc" },
    { "args": { "s": "xyzlynkz" }, "expected": "yz" },
    { "args": { "s": "abbcc" }, "expected": "bc" },
    { "args": { "s": "aa" }, "expected": "a" },
    { "args": { "s": "aabb" }, "expected": "ab" }
  ]
}
```

</details>


***

## Key Takeaway

These seven problems map onto the patterns the entire section built:

| Problem | Pattern |
|---|---|
| Covering distance | Linear DP, count-aggregator |
| Reachability check | Linear DP, boolean |
| Longest bitonic subsequence | LIS twice (forward + backward) |
| Longest alternating subsequence | LIS-style 2D state on parity |
| Pattern as subsequence | Edit-distance pattern, count |
| Shortest common supersequence | Edit-distance pattern, min-length |
| Longest repeated subsequence | LCS variant on `s` vs `s`, off-diagonal |

If you wrote even five of these correctly without looking at the solution, you've internalised what this section was teaching. The rest is pattern-matching practice — and the more problems you see, the faster the recognition becomes. The DP archetypes don't expand much past what we've covered: linear, interval, prefix-keyed, knapsack, grid, prefix-sum, and adversarial-game DP cover the vast majority of polynomial-time decision and optimisation problems on sequences and grids.

**You didn't just complete a chapter on dynamic programming. You learned that DP is a *toolkit*, not a single technique — half a dozen recurrence shapes that recombine endlessly. Every new DP problem you'll encounter is a remix of these primitives, with one or two twists. Spot the family, identify the state, choose the aggregator, and the recurrence assembles itself. That recognition reflex — built one lesson at a time — is the skill that makes someone a strong dynamic-programming thinker for life.**

The next sections in this DSA series move beyond DP — bit manipulation, advanced graph algorithms, string matching automata. Bring the same pattern-recognition habit there, and the learning curve flattens dramatically.
