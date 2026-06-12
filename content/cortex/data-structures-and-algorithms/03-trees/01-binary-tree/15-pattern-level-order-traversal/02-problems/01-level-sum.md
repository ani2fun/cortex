---
title: "Level Sum"
summary: "Return a list where the i-th entry is the sum of all node values at level i."
prereqs:
  - 15-pattern-level-order-traversal/01-pattern
difficulty: easy
kind: problem
topics: [level-order-traversal, binary-tree]
---

# Problem 1 — Level sum

## Problem Statement

Return a list where the *i*-th entry is the sum of all node values at level *i*.

Apply the template directly: at the top of each outer-loop iteration, accumulate `levelSum = 0`; in the inner loop, add each node's value; after the inner loop, append `levelSum` to the output.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, null, null, 7]
Output: [1, 5, 11]
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 7]
Output: [1, 12, 9]
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def level_sum(self, root):
        # Your code goes here — BFS with snapshot n = len(q); accumulate the
        # sum for each level, append to the output list. Return the list.
        return []

def build_tree(values):              # [1, 2, 3, null, 4] level-order → root
    if not values:
        return None
    root = TreeNode(values[0])
    queue = deque([root])
    i = 1
    while queue and i < len(values):
        node = queue.popleft()
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.left = TreeNode(v); queue.append(node.left)
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.right = TreeNode(v); queue.append(node.right)
    return root

root = build_tree(json.loads(input()))   # the test case's level-order values
print(Solution().level_sum(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        List<Integer> levelSum(TreeNode root) {
            // Your code goes here — BFS with snapshot n = queue.size(); accumulate
            // the sum for each level, add to the output list. Return the list.
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().levelSum(root));
    }

    static TreeNode buildTree(Integer[] values) {   // [1, 2, 3, null, 4] level-order → root
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.add(root);
        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            TreeNode node = queue.poll();
            if (i < values.length) {
                Integer v = values[i++];
                if (v != null) { node.left = new TreeNode(v); queue.add(node.left); }
            }
            if (i < values.length) {
                Integer v = values[i++];
                if (v != null) { node.right = new TreeNode(v); queue.add(node.right); }
            }
        }
        return root;
    }

    // "[1, 2, null, 4]" → {1, 2, null, 4} — reads the test case's level-order values
    static Integer[] parseIntegerArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new Integer[0];
        String[] parts = inner.split(",");
        Integer[] out = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++)
            out[i] = parts[i].equals("null") ? null : Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, null, null, 7]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]" }, "expected": "[1, 5, 11]" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]" }, "expected": "[1, 12, 9]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[42]" }, "expected": "[42]" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "[1, 2, 3]" },
    { "args": { "root": "[5, 5, 5, 5, 5, 5, 5]" }, "expected": "[5, 10, 20]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A standard level-order loop: at the top of each outer iteration snapshot `n = len(q)`, reset `level_sum = 0`, drain `n` nodes adding each value, then append the total.

```python solution time=O(n) space=O(w)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def level_sum(self, root):
        level_sums = []
        if not root:
            return level_sums
        q = deque([root])
        while q:
            n = len(q)
            level_sum = 0
            for _ in range(n):
                node = q.popleft()
                level_sum += node.val
                if node.left:  q.append(node.left)
                if node.right: q.append(node.right)
            level_sums.append(level_sum)
        return level_sums

def build_tree(values):              # [1, 2, 3, null, 4] level-order → root
    if not values:
        return None
    root = TreeNode(values[0])
    queue = deque([root])
    i = 1
    while queue and i < len(values):
        node = queue.popleft()
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.left = TreeNode(v); queue.append(node.left)
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.right = TreeNode(v); queue.append(node.right)
    return root

root = build_tree(json.loads(input()))   # the test case's level-order values
print(Solution().level_sum(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        List<Integer> levelSum(TreeNode root) {
            List<Integer> levelSums = new ArrayList<>();
            if (root == null) return levelSums;
            Deque<TreeNode> q = new ArrayDeque<>();
            q.add(root);
            while (!q.isEmpty()) {
                int n = q.size();
                int levelSum = 0;
                for (int i = 0; i < n; i++) {
                    TreeNode node = q.poll();
                    levelSum += node.val;
                    if (node.left != null)  q.add(node.left);
                    if (node.right != null) q.add(node.right);
                }
                levelSums.add(levelSum);
            }
            return levelSums;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().levelSum(root));
    }

    static TreeNode buildTree(Integer[] values) {   // [1, 2, 3, null, 4] level-order → root
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.add(root);
        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            TreeNode node = queue.poll();
            if (i < values.length) {
                Integer v = values[i++];
                if (v != null) { node.left = new TreeNode(v); queue.add(node.left); }
            }
            if (i < values.length) {
                Integer v = values[i++];
                if (v != null) { node.right = new TreeNode(v); queue.add(node.right); }
            }
        }
        return root;
    }

    // "[1, 2, null, 4]" → {1, 2, null, 4} — reads the test case's level-order values
    static Integer[] parseIntegerArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new Integer[0];
        String[] parts = inner.split(",");
        Integer[] out = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++)
            out[i] = parts[i].equals("null") ? null : Integer.parseInt(parts[i]);
        return out;
    }
}
```

</details>
