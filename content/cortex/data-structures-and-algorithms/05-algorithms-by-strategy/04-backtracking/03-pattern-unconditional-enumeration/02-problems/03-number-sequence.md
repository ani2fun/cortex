---
title: "Number Sequence"
summary: "Given non-negative integers n and k, return all sequences of length n whose elements are integers in [1, k]. Sequences may repeat values; order may be any."
prereqs:
  - 03-pattern-unconditional-enumeration/01-pattern
difficulty: medium
kind: problem
topics: [unconditional-enumeration, backtracking]
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

## Examples

**Example 1**
```
Input:  n = 2, k = 2
Output: [[1, 1], [1, 2], [2, 1], [2, 2]]
Explanation: Depth 2, branching factor 2 → 2² = 4 leaves, all recorded.
```

**Example 2**
```
Input:  n = 0, k = 3
Output: [[]]
Explanation: n = 0 means zero slots → one leaf, the empty sequence.
```

```quiz
{
  "prompt": "How many sequences does number_sequence(n, k) produce?",
  "options": ["n + k", "n × k", "k^n", "n^k"],
  "answer": "k^n"
}
```

## Constraints

- `0 ≤ n ≤ 6`
- `0 ≤ k ≤ 6`
- When `k = 0` the result is `[]` (no choices, no leaves).

```python run viz=array viz-root=sequences
from typing import List

class Solution:
    def number_sequence(self, n: int, k: int) -> List[List[int]]:
        # Your code goes here — backtrack over n slots;
        # at each slot try choices 1..k; record a copy at the leaf.
        return []

n = int(input())     # the test case's n
k = int(input())     # the test case's k
print(Solution().number_sequence(n, k))
```

```java run viz=array viz-root=sequences
import java.util.*;

public class Main {
    static class Solution {
        public List<List<Integer>> numberSequence(int n, int k) {
            // Your code goes here — backtrack over n slots;
            // at each slot try choices 1..k; record a copy at the leaf.
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().numberSequence(n, k));
    }
}
```

```testcases
{
  "args": [
    { "id": "n", "label": "n", "type": "int", "placeholder": "2" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "n": "2", "k": "2" }, "expected": "[[1, 1], [1, 2], [2, 1], [2, 2]]" },
    { "args": { "n": "3", "k": "1" }, "expected": "[[1, 1, 1]]" },
    { "args": { "n": "1", "k": "4" }, "expected": "[[1], [2], [3], [4]]" },
    { "args": { "n": "0", "k": "3" }, "expected": "[[]]" },
    { "args": { "n": "1", "k": "1" }, "expected": "[[1]]" }
  ]
}
```

<details>
<summary><h2>What Does the State Space Tree Look Like?</h2></summary>


Depth `n`, branching factor `k`, every leaf valid. `k^n` total leaves — exactly the generic enumeration template.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(n · k^n) space=O(n)
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


n = int(input())     # the test case's n
k = int(input())     # the test case's k
print(Solution().number_sequence(n, k))
```

```java solution
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
        Scanner sc = new Scanner(System.in);
        int n = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().numberSequence(n, k));
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
