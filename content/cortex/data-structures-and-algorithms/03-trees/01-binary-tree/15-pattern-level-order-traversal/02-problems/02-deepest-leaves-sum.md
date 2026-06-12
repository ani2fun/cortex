---
title: "Deepest Leaves Sum"
summary: "Return the sum of the values of the leaves on the deepest level of the tree."
prereqs:
  - 15-pattern-level-order-traversal/01-pattern
difficulty: easy
kind: problem
topics: [level-order-traversal, binary-tree]
---

# Problem 2 — Deepest leaves sum

## Problem Statement

Return the sum of the values of the leaves on the deepest level of the tree.

Same shape as level-sum, but instead of recording every level we just *overwrite* a single `levelSum` variable each iteration. After the loop ends, `levelSum` holds the sum of the deepest level. (Note: every node on the deepest level is a leaf.)

## Examples

**Example 1:**
```
Input:  root = [1, 2, 1, 7, null, null, 1]
Output: 8
```

**Example 2:**
```
Input:  root = [1, 6, 5, null, null, 2, 7]
Output: 9
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `1 ≤ node.val ≤ 100`
- Return `0` for an empty tree.

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def deepest_leaves_sum(self, root):
        # Your code goes here — BFS with snapshot n = len(q); overwrite a single
        # level_sum each round. Return the last value computed.
        return 0

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
print(Solution().deepest_leaves_sum(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int deepestLeavesSum(TreeNode root) {
            // Your code goes here — BFS with snapshot n = queue.size(); overwrite a
            // single levelSum each round. Return the last value computed.
            return 0;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().deepestLeavesSum(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 1, 7, null, null, 1]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 1, 7, null, null, 1]" }, "expected": "8" },
    { "args": { "root": "[1, 6, 5, null, null, 2, 7]" }, "expected": "9" },
    { "args": { "root": "[]" }, "expected": "0" },
    { "args": { "root": "[7]" }, "expected": "7" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "3" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "5" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]" }, "expected": "22" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Same BFS skeleton as level-sum, but instead of appending each level's total we overwrite a single `level_sum` variable. After the loop exits, the variable holds the sum of the last (deepest) level.

```python solution time=O(n) space=O(w)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def deepest_leaves_sum(self, root):
        if not root:
            return 0
        q = deque([root])
        level_sum = 0
        while q:
            n = len(q)
            level_sum = 0
            for _ in range(n):
                node = q.popleft()
                level_sum += node.val
                if node.left:  q.append(node.left)
                if node.right: q.append(node.right)
        return level_sum

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
print(Solution().deepest_leaves_sum(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int deepestLeavesSum(TreeNode root) {
            if (root == null) return 0;
            Deque<TreeNode> q = new ArrayDeque<>();
            q.add(root);
            int levelSum = 0;
            while (!q.isEmpty()) {
                int n = q.size();
                levelSum = 0;
                for (int i = 0; i < n; i++) {
                    TreeNode node = q.poll();
                    levelSum += node.val;
                    if (node.left != null)  q.add(node.left);
                    if (node.right != null) q.add(node.right);
                }
            }
            return levelSum;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().deepestLeavesSum(root));
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
