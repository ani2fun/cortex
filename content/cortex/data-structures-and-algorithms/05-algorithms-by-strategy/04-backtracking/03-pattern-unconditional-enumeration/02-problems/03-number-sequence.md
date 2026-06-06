---
title: "Number Sequence"
summary: "Given non-negative integers n and k, return all sequences of length n whose elements are integers in [1, k]. Sequences may repeat values; order may be any."
prereqs:
  - 03-pattern-unconditional-enumeration/01-pattern
difficulty: medium
---

# Number Sequence

Both slot count and branching factor become parameters. This is the most general unconditional-enumeration shape in this section.

---

## The Problem

Given non-negative integers `n` and `k`, return all sequences of length `n` whose elements are integers in `[1, k]`. Sequences may repeat values; order may be any.

```
Input:  n = 2, k = 2
Output: [[1,1], [1,2], [2,1], [2,2]]

Input:  n = 3, k = 1
Output: [[1,1,1]]

Input:  n = 1, k = 4
Output: [[1], [2], [3], [4]]
```

---

<details>
<summary><h2>What Does the State Space Tree Look Like?</h2></summary>


Depth `n`, branching factor `k`, every leaf valid. `k^n` total leaves — exactly the generic enumeration template.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=graph viz-root=sequences
from typing import List

class Solution:
    def generate_sequence(
        self,
        n: int,
        k: int,
        index: int,
        current_sequence: List[int],
        sequences: List[List[int]],
    ) -> None:

        # If the current sequence has reached length n (solution state)
        if index == n:

            # Add the complete sequence to the result
            sequences.append(current_sequence.copy())

            # Return to continue exploring other possibilities
            return

        # Get all possible choices for the current position
        # (numbers 1..k)
        for choice in range(1, k + 1):

            # Add current number to the current sequence (make choice)
            current_sequence.append(choice)

            # Recurse to fill the next position in the sequence
            self.generate_sequence(
                n, k, index + 1, current_sequence, sequences
            )

            # Backtrack by removing the last added number (revert
            # choice)
            current_sequence.pop()

    def number_sequence(self, n: int, k: int) -> List[List[int]]:

        # Stores all generated sequences (solution states)
        sequences: List[List[int]] = []

        # Stores the current sequence being built (state)
        current_sequence: List[int] = []

        # Generate all sequences using backtracking
        self.generate_sequence(n, k, 0, current_sequence, sequences)

        # Return the list containing all sequences
        return sequences


# Examples from the problem statement
print(Solution().number_sequence(2, 2))   # [[1, 1], [1, 2], [2, 1], [2, 2]]
print(Solution().number_sequence(3, 1))   # [[1, 1, 1]]
print(Solution().number_sequence(1, 4))   # [[1], [2], [3], [4]]

# Edge cases
print(Solution().number_sequence(0, 3))   # [[]]
print(Solution().number_sequence(1, 1))   # [[1]]
print(len(Solution().number_sequence(3, 3)))  # 27
```

```java run viz=graph viz-root=sequences
import java.util.*;

public class Main {
    static class Solution {
        private void generateSequence(
            int n,
            int k,
            int index,
            List<Integer> currentSequence,
            List<List<Integer>> sequences
        ) {

            // If the current sequence has reached length n (solution state)
            if (index == n) {

                // Add the complete sequence to the result
                sequences.add(new ArrayList<>(currentSequence));

                // Return to continue exploring other possibilities
                return;
            }

            // Get all possible choices for the current position
            // (numbers 1..k)
            for (int choice = 1; choice <= k; choice++) {

                // Add current number to the current sequence (make choice)
                currentSequence.add(choice);

                // Recurse to fill the next position in the sequence
                generateSequence(
                    n,
                    k,
                    index + 1,
                    currentSequence,
                    sequences
                );

                // Backtrack by removing the last added number (revert
                // choice)
                currentSequence.remove(currentSequence.size() - 1);
            }
        }

        public List<List<Integer>> numberSequence(int n, int k) {

            // Stores all generated sequences (solution states)
            List<List<Integer>> sequences = new ArrayList<>();

            // Stores the current sequence being built (state)
            List<Integer> currentSequence = new ArrayList<>();

            // Generate all sequences using backtracking
            generateSequence(n, k, 0, currentSequence, sequences);

            // Return the list containing all sequences
            return sequences;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().numberSequence(2, 2));   // [[1, 1], [1, 2], [2, 1], [2, 2]]
        System.out.println(new Solution().numberSequence(3, 1));   // [[1, 1, 1]]
        System.out.println(new Solution().numberSequence(1, 4));   // [[1], [2], [3], [4]]

        // Edge cases
        System.out.println(new Solution().numberSequence(0, 3));   // [[]]
        System.out.println(new Solution().numberSequence(1, 1));   // [[1]]
        System.out.println(new Solution().numberSequence(3, 3).size());  // 27
    }
}
```

### Complexity Analysis

| Resource | Cost |
|---|---|
| **Time** | `O(n · k^n)` |
| **Space (output)** | `O(n · k^n)` |
| **Space (stack)** | `O(n)` |

### Edge Cases

| Case | Example | Expected |
|---|---|---|
| `n = 0` | `n = 0, k = 5` | `[[]]` |
| `k = 0` | `n = 2, k = 0` | `[]` (no choices, no leaves) |
| `n = 1` | `n = 1, k = 4` | `[[1], [2], [3], [4]]` |
| Largish | `n = 6, k = 4` | 4096 sequences |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Number Sequence is the cleanest demonstration of unconditional enumeration's general shape: depth-`n`, `k`-ary tree, every leaf valid. The next problem maps each slot to a *different* choice set instead of a uniform `[1, k]`.

</details>
