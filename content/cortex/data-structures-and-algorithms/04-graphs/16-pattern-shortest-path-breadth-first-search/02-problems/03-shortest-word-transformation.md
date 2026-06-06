---
title: "Shortest Word Transformation"
summary: "Given two words source and target and a wordList, find the minimum number of words in a transformation chain source → s1 → s2 → … → target, where:"
prereqs:
  - 16-pattern-shortest-path-breadth-first-search/01-pattern
difficulty: hard
---

# Problem: Shortest Word Transformation

## The Problem

Given two words `source` and `target` and a `wordList`, find the minimum number of words in a transformation chain `source → s1 → s2 → … → target`, where:

- Each consecutive pair differs by exactly one letter.
- Every word `s1, s2, …, target` is in `wordList`.

Return 0 if no transformation exists.

```
Input:  source = "hit", target = "cog", wordList = ["hot", "dot", "dog", "lot", "log", "cog"]
Output: 5  (hit → hot → dot → dog → cog)
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


The graph is **implicit**:

- Node = a word.
- Edge = "differ by exactly one letter".

The graph is unweighted (each transformation costs 1), and we want minimum hops. **BFS shortest path.**

The trick is generating neighbours efficiently. Two main approaches:

1. **Per-position substitution.** For each character position, try every other letter; check if the mutated word is in `wordList`. O(L × 26) neighbours per word, where L = word length.
2. **Pattern keys.** Pre-build a map from "h*t" → ["hit", "hot", "hat", …]. Each word has L neighbour-pattern keys.

We'll use approach 1 — simpler to code, still fast for typical inputs. The implementation builds the adjacency on the fly during BFS.

</details>
<details>
<summary><h2>The Solution (per-position substitution)</h2></summary>



```python run viz=graph viz-root=queue
from collections import deque
from typing import List

class Solution:
    def shortest_word_transformation(
        self, source: str, target: str, word_list: List[str]
    ) -> int:

        # Create a set to store the words for efficient lookup
        word_set = set(word_list)

        # If the target word is not in the word set, return 0
        if target not in word_set:
            return 0

        # Create a queue for BFS traversal
        queue = deque([source])

        # Create a visited set to keep track of visited words
        visited = set([source])

        # Starting level is 1 (source word is at level 1)
        level = 1

        while queue:
            level_size = len(queue)

            # Traverse all words at the current level
            for _ in range(level_size):
                current_word = queue.popleft()

                # Check if the current word matches the target
                if current_word == target:
                    return level

                # Generate all possible adjacent words
                for j in range(len(current_word)):
                    original_char = current_word[j]
                    for ch in "abcdefghijklmnopqrstuvwxyz":

                        # Skip if the character is the same as the
                        # original
                        if ch == original_char:
                            continue

                        # Change the character at position j
                        new_word = (
                            current_word[:j] + ch + current_word[j + 1:]
                        )

                        # Check if the new word is in the word set and
                        # has not been visited
                        if (
                            new_word in word_set
                            and new_word not in visited
                        ):
                            queue.append(new_word)
                            visited.add(new_word)

            # Increment the level after traversing all words at the
            # current level
            level += 1

        # No transformation sequence exists, return 0
        return 0


# Examples from the problem statement
print(Solution().shortest_word_transformation("hit", "cog", ["hot","dot","dog","lot","log","cog"]))  # 5
print(Solution().shortest_word_transformation("hit", "mad", ["hot","dot","dog","lot","log","cog"]))  # 0
print(Solution().shortest_word_transformation("red", "tax", ["tan","apple","banana","blue","rose"]))  # 0

# Edge cases
print(Solution().shortest_word_transformation("hit", "hot", ["hot"]))                                # 2 — one step
print(Solution().shortest_word_transformation("abc", "abc", ["abc"]))                                # 1 — source equals target
print(Solution().shortest_word_transformation("hit", "cog", []))                                     # 0 — empty word list
print(Solution().shortest_word_transformation("a", "c", ["b","c"]))                                  # 2 — single-char chain
```

```java run viz=graph viz-root=queue
import java.util.*;

public class Main {
    static class Solution {
        public int shortestWordTransformation(
            String source,
            String target,
            List<String> wordList
        ) {

            // Create a set to store the words for efficient lookup
            Set<String> wordSet = new HashSet<>(wordList);

            // If the target word is not in the word set, return 0
            if (!wordSet.contains(target)) {
                return 0;
            }

            // Create a queue for BFS traversal
            Queue<String> queue = new LinkedList<>();
            queue.add(source);

            // Create a visited set to keep track of visited words
            Set<String> visited = new HashSet<>();
            visited.add(source);

            // Starting level is 1 (source word is at level 1)
            int level = 1;

            while (!queue.isEmpty()) {
                int levelSize = queue.size();

                // Traverse all words at the current level
                for (int i = 0; i < levelSize; i++) {
                    String currentWord = queue.poll();

                    // Check if the current word matches the target
                    if (currentWord.equals(target)) {
                        return level;
                    }

                    // Generate all possible adjacent words
                    for (int j = 0; j < currentWord.length(); j++) {
                        char originalChar = currentWord.charAt(j);

                        for (char ch = 'a'; ch <= 'z'; ch++) {

                            // Skip if the character is the same as the
                            // original
                            if (ch == originalChar) continue;

                            // Change the character at position j
                            String newWord =
                                currentWord.substring(0, j) +
                                ch +
                                currentWord.substring(j + 1);

                            // Check if the new word is in the word set and
                            // has not been visited
                            if (
                                wordSet.contains(newWord) &&
                                !visited.contains(newWord)
                            ) {
                                queue.add(newWord);
                                visited.add(newWord);
                            }
                        }
                    }
                }

                // Increment the level after traversing all words at the
                // current level
                level++;
            }

            // No transformation sequence exists, return 0
            return 0;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.shortestWordTransformation("hit", "cog", List.of("hot","dot","dog","lot","log","cog")));  // 5
        System.out.println(sol.shortestWordTransformation("hit", "mad", List.of("hot","dot","dog","lot","log","cog")));  // 0
        System.out.println(sol.shortestWordTransformation("red", "tax", List.of("tan","apple","banana","blue","rose")));  // 0

        // Edge cases
        System.out.println(sol.shortestWordTransformation("hit", "hot", List.of("hot")));         // 2
        System.out.println(sol.shortestWordTransformation("abc", "abc", List.of("abc")));         // 1
        System.out.println(sol.shortestWordTransformation("hit", "cog", new ArrayList<>()));      // 0
        System.out.println(sol.shortestWordTransformation("a", "c", List.of("b","c")));           // 2
    }
}
```

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Problem | Time | Space |
|---|---|---|
| Minimum steps in a grid | O(R × C) | O(R × C) |
| Nearest distance (multi-source) | O(R × C) | O(R × C) |
| Shortest word transformation | O(N × L × 26) | O(N × L) |

Where N = words, L = word length. The word problem is dominated by the per-word neighbour generation (L positions × 26 letters).

</details>
