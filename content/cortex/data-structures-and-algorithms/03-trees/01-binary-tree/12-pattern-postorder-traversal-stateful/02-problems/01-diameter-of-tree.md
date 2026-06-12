---
title: "Diameter of Tree"
summary: "Find the length of the longest path (in edges) between any two nodes in a binary tree."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: medium
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Diameter of Tree

## Problem Statement

Given the **root** of a binary tree, return the length of the **diameter** — the longest path between any two nodes, measured in number of edges. The path may or may not pass through the root.

The diameter is found by tracking the maximum of `left_height + right_height` at every node, while returning height upward. The answer is the accumulator, not the root's return value.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, 5]
Output: 3
```
Longest path: `4 → 2 → 1 → 3` (3 edges).

**Example 2:**
```
Input:  root = [1, 2]
Output: 1
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-100 ≤ node.val ≤ 100`

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def diameter_of_tree(self, root):
        # Your code goes here — track best = max(lh + rh) in an outer variable;
        # return 1 + max(lh, rh) upward. Return best after the traversal.
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
print(Solution().diameter_of_tree(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int diameterOfTree(TreeNode root) {
            // Your code goes here — track best = max(lh + rh) in a field;
            // return 1 + max(lh, rh) upward. Return best after the traversal.
            return 0;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().diameterOfTree(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, 5]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, 5]" }, "expected": "3" },
    { "args": { "root": "[1, 2]" }, "expected": "1" },
    { "args": { "root": "[1]" }, "expected": "0" },
    { "args": { "root": "[]" }, "expected": "0" },
    { "args": { "root": "[1, 2, 3, 4, 5, null, 6]" }, "expected": "4" },
    { "args": { "root": "[-10, 9, 20, null, null, 15, 7]" }, "expected": "3" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Already covered in the pattern page. Each call returns *height* (edges downward); each call updates `best = max(best, leftHeight + rightHeight)` — the path through this node. The answer is the global `best`, not the root's return value.

The implementation is exactly the generic template. The lesson here is *what to choose* as the feed-up vs the global, not how to type the code.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def diameter_of_tree(self, root):
        self.best = 0
        def height(node):
            if node is None:
                return 0
            lh = height(node.left)
            rh = height(node.right)
            self.best = max(self.best, lh + rh)
            return 1 + max(lh, rh)
        height(root)
        return self.best

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
print(Solution().diameter_of_tree(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int best = 0;

        private int height(TreeNode node) {
            if (node == null) return 0;
            int lh = height(node.left);
            int rh = height(node.right);
            best = Math.max(best, lh + rh);
            return 1 + Math.max(lh, rh);
        }

        int diameterOfTree(TreeNode root) {
            best = 0;
            height(root);
            return best;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().diameterOfTree(root));
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
