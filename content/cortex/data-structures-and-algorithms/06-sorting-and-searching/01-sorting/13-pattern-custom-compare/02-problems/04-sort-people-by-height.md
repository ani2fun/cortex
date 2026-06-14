---
title: "Sort People by Height"
summary: "Given people = [[h_i, k_i], ...] where h_i is a height and k_i is how many people of height ≥ h_i must stand in front, reorder people so the queue satisfies every k constraint."
prereqs:
  - 13-pattern-custom-compare/01-pattern
difficulty: easy
kind: problem
topics: [custom-compare, sorting]
---

# Sort People by Height

A two-step problem: first sort by a custom rule, then *reconstruct* the order based on a per-element index. The custom compare gets us the initial sort; a clever insertion gets us the final answer.

## Problem Statement

Given `people = [[h_i, k_i], ...]` where `h_i` is the i-th person's height and `k_i` is the number of people standing in front of them whose height is `≥ h_i`. Reorder `people` so the resulting queue satisfies these `k_i` constraints.

```
Input:  people = [[5, 1], [5, 0]]
Output: [[5, 0], [5, 1]]

Input:  people = [[1, 4], [2, 3], [3, 2], [4, 1], [5, 0]]
Output: [[5, 0], [4, 1], [3, 2], [2, 3], [1, 4]]
```

---

## Examples

**Example 1**
```
Input:  people = [[5, 1], [5, 0]]
Output: [[5, 0], [5, 1]]
Explanation: Both are height 5. The person with k=0 (nobody taller-or-equal in front) goes first; the one with k=1 follows.
```

**Example 2**
```
Input:  people = [[1, 4], [2, 3], [3, 2], [4, 1], [5, 0]]
Output: [[5, 0], [4, 1], [3, 2], [2, 3], [1, 4]]
Explanation: Each person i must have exactly k_i taller-or-equal people ahead — here a strictly descending queue.
```

## Constraints

- `1 ≤ people.length ≤ 2000`
- `0 ≤ h_i ≤ 10^6`
- `0 ≤ k_i < people.length`
- The input is guaranteed to admit a valid reconstruction.

```python run viz=array viz-root=people
import ast
from typing import List

class Solution:
    def sort_people_by_height(
        self, people: List[List[int]]
    ) -> List[List[int]]:
        # Your code goes here — sort by height descending, ties by k ascending;
        # then insert each person into the result at index k. Return the queue.
        return people


people = ast.literal_eval(input())   # the test case's people [[h, k], ...]
print(Solution().sort_people_by_height(people))
```

```java run viz=array viz-root=people
import java.util.*;

public class Main {
    static class Solution {
        public int[][] sortPeopleByHeight(int[][] people) {
            // Your code goes here — sort by height descending, ties by k
            // ascending; then insert each person at index k. Return the queue.
            return people;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] people = parseIntMatrix(sc.nextLine());
        System.out.println(Arrays.deepToString(new Solution().sortPeopleByHeight(people)));
    }

    // "[[5, 1], [5, 0]]" → {{5,1},{5,0}} — reads the test case's people
    static int[][] parseIntMatrix(String line) {
        String trimmed = line.trim();
        if (trimmed.equals("[]") || trimmed.equals("[[]]")) return new int[0][];
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        String[] rows = inner.split("\\],\\s*\\[");
        int[][] mat = new int[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r].replaceAll("[\\[\\]\\s]", "");
            if (row.isEmpty()) { mat[r] = new int[0]; continue; }
            String[] parts = row.split(",");
            mat[r] = new int[parts.length];
            for (int c = 0; c < parts.length; c++) mat[r][c] = Integer.parseInt(parts[c].trim());
        }
        return mat;
    }
}
```

```testcases
{
  "args": [
    { "id": "people", "label": "people", "type": "int[][]", "placeholder": "[[5, 1], [5, 0]]" }
  ],
  "cases": [
    { "args": { "people": "[[5, 1], [5, 0]]" }, "expected": "[[5, 0], [5, 1]]" },
    { "args": { "people": "[[1, 4], [2, 3], [3, 2], [4, 1], [5, 0]]" }, "expected": "[[5, 0], [4, 1], [3, 2], [2, 3], [1, 4]]" },
    { "args": { "people": "[[1, 0]]" }, "expected": "[[1, 0]]" },
    { "args": { "people": "[[7, 0], [4, 4], [7, 1], [5, 0], [6, 1], [5, 2]]" }, "expected": "[[5, 0], [7, 0], [5, 2], [6, 1], [4, 4], [7, 1]]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The `k` constraint counts only people of *height ≥ mine*, so a person's position depends solely on the taller-or-equal crowd — the shorter people are invisible to them. That's the unlock: process people **tallest first**. When you place person `i`, everyone already placed is taller-or-equal, so dropping them in at index `k_i` makes exactly `k_i` qualifying people sit ahead of them — and inserting a shorter person later never disturbs that count, because shorter people don't count toward anyone's `k`.

</details>
<details>
<summary><h2>The Two-Step Algorithm</h2></summary>


**Step 1 — sort.** Sort `people` by height descending, breaking ties by `k` ascending. After this, the tallest people are first; among same-height people, the one with smaller `k` is first.

**Step 2 — reconstruct.** Insert each person from the sorted list into a result array at index `k`. Because we process tallest first, by the time person `i` is inserted, exactly `k_i` people of `≥ height` are already in the result — they're the only ones we've inserted, and there's exactly `k_i` of them in front of position `k_i`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(n^2) space=O(n)
import ast
from typing import List

class Solution:
    def sort_people_by_height(
        self, people: List[List[int]]
    ) -> List[List[int]]:

        # Step 1: Sort the people array using a custom lambda comparator
        # Sort by height descending, then k ascending
        people.sort(key=lambda x: (-x[0], x[1]))

        # Step 2: Reconstruct the queue by inserting people at their
        # respective positions
        result = []
        for person in people:

            # Insert at index person[1]
            result.insert(person[1], person)

        return result


people = ast.literal_eval(input())   # the test case's people [[h, k], ...]
print(Solution().sort_people_by_height(people))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int[][] sortPeopleByHeight(int[][] people) {

            // Step 1: Sort the people array
            // Sort by height in descending order,
            // if heights are equal, sort by 'number of people in front' in
            // ascending order
            Arrays.sort(
                people,
                (person1, person2) -> {
                    if (person1[0] != person2[0]) {

                        // Descending order by height
                        return person2[0] - person1[0];
                    } else {

                        // Ascending order by k
                        return person1[1] - person2[1];
                    }
                }
            );

            // Step 2: Reconstruct the queue
            // Insert each person at the index equal to 'number of people in
            // front'
            int[][] result = new int[people.length][2];

            // current size of the result array
            int size = 0;

            for (int[] person : people) {

                // Shift elements to make space for insertion at person[1]
                for (int j = size; j > person[1]; j--) {
                    result[j] = result[j - 1];
                }

                // Insert the person at index person[1]
                result[person[1]] = person;
                size++;
            }

            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] people = parseIntMatrix(sc.nextLine());
        System.out.println(Arrays.deepToString(new Solution().sortPeopleByHeight(people)));
    }

    // "[[5, 1], [5, 0]]" → {{5,1},{5,0}} — reads the test case's people
    static int[][] parseIntMatrix(String line) {
        String trimmed = line.trim();
        if (trimmed.equals("[]") || trimmed.equals("[[]]")) return new int[0][];
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        String[] rows = inner.split("\\],\\s*\\[");
        int[][] mat = new int[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r].replaceAll("[\\[\\]\\s]", "");
            if (row.isEmpty()) { mat[r] = new int[0]; continue; }
            String[] parts = row.split(",");
            mat[r] = new int[parts.length];
            for (int c = 0; c < parts.length; c++) mat[r][c] = Integer.parseInt(parts[c].trim());
        }
        return mat;
    }
}
```

The custom compare is the `(-height, k)` sort key. The reconstruction is the second clever step. The Python and Java implementations follow the same recipe.

### Complexity

- Step 1 (sort): `O(n log n)`.
- Step 2 (reconstruct): `O(n²)` worst case because each `insert` at an arbitrary index is `O(n)`.

Total: `O(n²)`. Better data structures (Fenwick tree, balanced BST) can reduce step 2 to `O(n log n)`.

</details>
