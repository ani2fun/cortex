---
title: "Sort People by Height"
summary: "Given people = [[h_i, k_i], ...] where h_i is the i-th person's height and k_i is the number of people standing in front of them whose height is ≥ h_i. Reorder people so the resulting queue satisfies "
prereqs:
  - 13-pattern-custom-compare/01-pattern
difficulty: easy
---

# Sort People by Height

A two-step problem: first sort by a custom rule, then *reconstruct* the order based on a per-element index. The custom compare gets us the initial sort; a clever insertion gets us the final answer.

---

## The Problem

Given `people = [[h_i, k_i], ...]` where `h_i` is the i-th person's height and `k_i` is the number of people standing in front of them whose height is `≥ h_i`. Reorder `people` so the resulting queue satisfies these `k_i` constraints.

```
Input:  people = [[5, 1], [5, 0]]
Output: [[5, 0], [5, 1]]

Input:  people = [[1, 4], [2, 3], [3, 2], [4, 1], [5, 0]]
Output: [[5, 0], [4, 1], [3, 2], [2, 3], [1, 4]]
```

---

<details>
<summary><h2>The Two-Step Algorithm</h2></summary>


**Step 1 — sort.** Sort `people` by height descending, breaking ties by `k` ascending. After this, the tallest people are first; among same-height people, the one with smaller `k` is first.

**Step 2 — reconstruct.** Insert each person from the sorted list into a result array at index `k`. Because we process tallest first, by the time person `i` is inserted, exactly `k_i` people of `≥ height` are already in the result — they're the only ones we've inserted, and there's exactly `k_i` of them in front of position `k_i`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=array viz-root=people
from typing import List

class Solution:
    def sort_people_by_height(
        self, people: list[list[int]]
    ) -> list[list[int]]:

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


# Examples from the problem statement
print(Solution().sort_people_by_height([[5, 1], [5, 0]]))                              # [[5, 0], [5, 1]]
print(Solution().sort_people_by_height([[1, 4], [2, 3], [3, 2], [4, 1], [5, 0]]))     # [[5, 0], [4, 1], [3, 2], [2, 3], [1, 4]]
print(Solution().sort_people_by_height([[5, 0], [4, 1], [3, 2], [2, 3], [1, 4]]))     # [[5, 0], [4, 1], [3, 2], [2, 3], [1, 4]]

# Edge cases
print(Solution().sort_people_by_height([[1, 0]]))                                      # [[1, 0]]
print(Solution().sort_people_by_height([[7, 0], [4, 4], [7, 1], [5, 0], [6, 1], [5, 2]]))  # [[7, 0], [7, 1], [6, 1], [5, 0], [5, 2], [4, 4]]
```

```java run viz=array viz-root=people
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
        // Examples from the problem statement
        int[][] r1 = new Solution().sortPeopleByHeight(new int[][]{{5, 1}, {5, 0}});
        System.out.println(Arrays.deepToString(r1));  // [[5, 0], [5, 1]]

        int[][] r2 = new Solution().sortPeopleByHeight(new int[][]{{1, 4}, {2, 3}, {3, 2}, {4, 1}, {5, 0}});
        System.out.println(Arrays.deepToString(r2));  // [[5, 0], [4, 1], [3, 2], [2, 3], [1, 4]]

        int[][] r3 = new Solution().sortPeopleByHeight(new int[][]{{5, 0}, {4, 1}, {3, 2}, {2, 3}, {1, 4}});
        System.out.println(Arrays.deepToString(r3));  // [[5, 0], [4, 1], [3, 2], [2, 3], [1, 4]]

        // Edge cases
        int[][] r4 = new Solution().sortPeopleByHeight(new int[][]{{1, 0}});
        System.out.println(Arrays.deepToString(r4));  // [[1, 0]]

        int[][] r5 = new Solution().sortPeopleByHeight(new int[][]{{7, 0}, {4, 4}, {7, 1}, {5, 0}, {6, 1}, {5, 2}});
        System.out.println(Arrays.deepToString(r5));  // [[7, 0], [7, 1], [6, 1], [5, 0], [5, 2], [4, 4]]
    }
}
```

The custom compare is the `(-height, k)` sort key. The reconstruction is the second clever step. The Python and Java implementations follow the same recipe.

### Complexity

- Step 1 (sort): `O(n log n)`.
- Step 2 (reconstruct): `O(n²)` worst case because each `insert` at an arbitrary index is `O(n)`.

Total: `O(n²)`. Better data structures (Fenwick tree, balanced BST) can reduce step 2 to `O(n log n)`.

</details>
