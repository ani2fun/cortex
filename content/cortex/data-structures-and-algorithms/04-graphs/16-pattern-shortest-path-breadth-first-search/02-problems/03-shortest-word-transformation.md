---
title: "Shortest Word Transformation"
summary: "Given two words source and target and a wordList, find the minimum number of words in a transformation chain source → s1 → s2 → … → target where each consecutive pair differs by exactly one letter."
prereqs:
  - 16-pattern-shortest-path-breadth-first-search/01-pattern
difficulty: hard
kind: problem
topics: [shortest-path-bfs, implicit-graph]
---

# Problem: Shortest Word Transformation

## Problem Statement

Given two words `source` and `target` and a `wordList`, find the minimum number of words in a transformation chain `source → s1 → s2 → … → target`, where:

- Each consecutive pair differs by exactly **one** letter.
- Every word `s1, s2, …, target` must be in `wordList`.

Return `0` if no transformation exists.

## Examples

**Example 1:**
```
Input:  source = "hit", target = "cog",
        wordList = ["hot", "dot", "dog", "lot", "log", "cog"]
Output: 5   (hit → hot → dot → dog → cog)
```

**Example 2:**
```
Input:  source = "hit", target = "mad",
        wordList = ["hot", "dot", "dog", "lot", "log", "cog"]
Output: 0   (target not reachable)
```

## Constraints

- `1 ≤ source.length, target.length ≤ 10`
- All words have the same length
- All words consist of lowercase English letters
- `0 ≤ wordList.length ≤ 5000`
- `0 ≤ O(N × L × 26)` time — per-position character substitution BFS

```python run viz=graph viz-root=queue
import ast

class Solution:
    def shortest_word_transformation(self, source, target, word_list):
        # Your code goes here — the graph is implicit: nodes are words, edges connect
        # words that differ by exactly one character. BFS from source; level = chain length.
        # Return 0 if target is unreachable or not in word_list.
        return 0

source = input()
target = input()
word_list = ast.literal_eval(input())
print(Solution().shortest_word_transformation(source, target, word_list))
```

```java run viz=graph viz-root=queue
import java.util.*;

public class Main {
    static class Solution {
        public int shortestWordTransformation(String source, String target, List<String> wordList) {
            // Your code goes here — the graph is implicit: nodes are words, edges connect
            // words that differ by exactly one character. BFS from source; level = chain length.
            // Return 0 if target is unreachable or not in wordList.
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String source = sc.nextLine().trim();
        String target = sc.nextLine().trim();
        List<String> wordList = parseStringList(sc.nextLine().trim());
        System.out.println(new Solution().shortestWordTransformation(source, target, wordList));
    }

    // Parses ["hot","dot","dog"] → List<String>
    static List<String> parseStringList(String line) {
        List<String> result = new ArrayList<>();
        String inner = line.trim();
        if (inner.equals("[]")) return result;
        inner = inner.substring(1, inner.length() - 1);  // strip outer [ ]
        String[] parts = inner.split(",");
        for (String p : parts) {
            String word = p.trim().replaceAll("^\"|\"$", "");
            if (!word.isEmpty()) result.add(word);
        }
        return result;
    }
}
```

```testcases
{
  "args": [
    { "id": "source", "label": "source", "type": "string", "placeholder": "hit" },
    { "id": "target", "label": "target", "type": "string", "placeholder": "cog" },
    { "id": "word_list", "label": "wordList", "type": "string[]", "placeholder": "[\"hot\", \"dot\", \"dog\", \"lot\", \"log\", \"cog\"]" }
  ],
  "cases": [
    { "args": { "source": "hit", "target": "cog", "word_list": "[\"hot\", \"dot\", \"dog\", \"lot\", \"log\", \"cog\"]" }, "expected": "5" },
    { "args": { "source": "hit", "target": "mad", "word_list": "[\"hot\", \"dot\", \"dog\", \"lot\", \"log\", \"cog\"]" }, "expected": "0" },
    { "args": { "source": "hit", "target": "hot", "word_list": "[\"hot\"]" }, "expected": "2" },
    { "args": { "source": "abc", "target": "abc", "word_list": "[\"abc\"]" }, "expected": "1" },
    { "args": { "source": "a", "target": "c", "word_list": "[\"b\", \"c\"]" }, "expected": "2" }
  ]
}
```

<details>
<summary>Editorial</summary>

The word graph is implicit: each word is a node and an edge exists between words that differ by exactly one character. Because every transformation costs 1 step (unweighted), BFS finds the shortest chain. Process one level at a time — all words reachable in `level` steps before advancing to `level + 1`. When the target is dequeued, `level` is the answer. Generate neighbours by substituting each character position with every letter `a–z`; check membership in a `wordSet` in O(1). If the target is not in `wordList`, return `0` immediately.

Note: the chain counts words, not edges, so `source` contributes 1 to the count — that's why a one-step transformation returns 2, not 1.

```python solution time=O(N*L*26) space=O(N*L)
import ast
from collections import deque

class Solution:
    def shortest_word_transformation(self, source, target, word_list):
        word_set = set(word_list)
        if target not in word_set:
            return 0
        queue = deque([source])
        visited = {source}
        level = 1
        while queue:
            for _ in range(len(queue)):
                current_word = queue.popleft()
                if current_word == target:
                    return level
                for j in range(len(current_word)):
                    original_char = current_word[j]
                    for ch in "abcdefghijklmnopqrstuvwxyz":
                        if ch == original_char:
                            continue
                        new_word = current_word[:j] + ch + current_word[j + 1:]
                        if new_word in word_set and new_word not in visited:
                            queue.append(new_word)
                            visited.add(new_word)
            level += 1
        return 0

source = input()
target = input()
word_list = ast.literal_eval(input())
print(Solution().shortest_word_transformation(source, target, word_list))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int shortestWordTransformation(String source, String target, List<String> wordList) {
            Set<String> wordSet = new HashSet<>(wordList);
            if (!wordSet.contains(target)) return 0;
            Queue<String> queue = new LinkedList<>();
            queue.add(source);
            Set<String> visited = new HashSet<>();
            visited.add(source);
            int level = 1;
            while (!queue.isEmpty()) {
                int levelSize = queue.size();
                for (int i = 0; i < levelSize; i++) {
                    String currentWord = queue.poll();
                    if (currentWord.equals(target)) return level;
                    for (int j = 0; j < currentWord.length(); j++) {
                        char originalChar = currentWord.charAt(j);
                        for (char ch = 'a'; ch <= 'z'; ch++) {
                            if (ch == originalChar) continue;
                            String newWord = currentWord.substring(0, j) + ch + currentWord.substring(j + 1);
                            if (wordSet.contains(newWord) && !visited.contains(newWord)) {
                                queue.add(newWord);
                                visited.add(newWord);
                            }
                        }
                    }
                }
                level++;
            }
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String source = sc.nextLine().trim();
        String target = sc.nextLine().trim();
        List<String> wordList = parseStringList(sc.nextLine().trim());
        System.out.println(new Solution().shortestWordTransformation(source, target, wordList));
    }

    static List<String> parseStringList(String line) {
        List<String> result = new ArrayList<>();
        String inner = line.trim();
        if (inner.equals("[]")) return result;
        inner = inner.substring(1, inner.length() - 1);
        String[] parts = inner.split(",");
        for (String p : parts) {
            String word = p.trim().replaceAll("^\"|\"$", "");
            if (!word.isEmpty()) result.add(word);
        }
        return result;
    }
}
```

</details>
