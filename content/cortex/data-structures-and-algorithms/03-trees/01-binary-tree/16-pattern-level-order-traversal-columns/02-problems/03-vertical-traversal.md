---
title: "Vertical Traversal"
summary: "Return all nodes grouped by column (top-to-bottom within each column), as a list-of-lists ordered by column from left to right."
prereqs:
  - 16-pattern-level-order-traversal-columns/01-pattern
difficulty: medium
kind: problem
topics: [level-order-traversal, binary-tree]
---

# Problem 3 — Vertical traversal

## Problem Statement

Return *all* nodes grouped by column (top-to-bottom within each column), as a list-of-lists ordered by column from left to right.

Instead of storing one value per column (top or bottom view), *append* to a list per column. BFS top-to-bottom order means the per-column list is already sorted top-to-bottom for free.

## Examples

**Example 1:**
```
Input:  root = [3, 9, 20, 4, 11, 15, 7]
Output: [[4], [9], [3, 11, 15], [20], [7]]
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, 6, null, null, 3, 2]
Output: [[8, 3], [1, 6], [4, 2]]
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `O(n)` time, `O(n)` space

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

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

class Solution:
    def vertical_traversal(self, root):
        # Your code goes here — BFS carrying (node, col); append to cols[c];
        # return [cols[c] for c in range(min, max+1)]
        return []

root = build_tree(json.loads(input()))
print(Solution().vertical_traversal(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        public List<List<Integer>> verticalTraversal(TreeNode root) {
            // Your code goes here — BFS with Map.entry(node, col);
            // TreeMap<Integer, List<Integer>> for sorted, all-nodes-per-column
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().verticalTraversal(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[3, 9, 20, 4, 11, 15, 7]" }
  ],
  "cases": [
    { "args": { "root": "[3, 9, 20, 4, 11, 15, 7]" }, "expected": "[[4], [9], [3, 11, 15], [20], [7]]" },
    { "args": { "root": "[1, 8, 4, null, 6, null, null, 3, 2]" }, "expected": "[[8, 3], [1, 6], [4, 2]]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[1]" }, "expected": "[[1]]" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "[[3], [2], [1]]" },
    { "args": { "root": "[1, null, 2, null, 3]" }, "expected": "[[1], [2], [3]]" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "[[2], [1], [3]]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

BFS carrying `(node, col)`. Append every node's value to `cols[col]`. Because BFS processes by increasing depth, each column list fills top-to-bottom automatically — no sorting needed within a column. Iterate `range(min_col, max_col+1)` (Python) or a `TreeMap` (Java) so columns come out left-to-right.

```python solution time=O(n) space=O(n)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

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

class Solution:
    def vertical_traversal(self, root):
        if root is None:
            return []
        cols = {}
        q = deque([(root, 0)])
        while q:
            node, c = q.popleft()
            if c not in cols: cols[c] = []
            cols[c].append(node.val)
            if node.left:  q.append((node.left,  c - 1))
            if node.right: q.append((node.right, c + 1))
        return [cols[c] for c in range(min(cols), max(cols) + 1)]

root = build_tree(json.loads(input()))
print(Solution().vertical_traversal(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        public List<List<Integer>> verticalTraversal(TreeNode root) {
            List<List<Integer>> out = new ArrayList<>();
            if (root == null) return out;
            Map<Integer, List<Integer>> cols = new TreeMap<>();
            Deque<Map.Entry<TreeNode, Integer>> q = new ArrayDeque<>();
            q.add(Map.entry(root, 0));
            while (!q.isEmpty()) {
                var e = q.poll();
                TreeNode node = e.getKey(); int c = e.getValue();
                cols.computeIfAbsent(c, k -> new ArrayList<>()).add(node.val);
                if (node.left  != null) q.add(Map.entry(node.left,  c - 1));
                if (node.right != null) q.add(Map.entry(node.right, c + 1));
            }
            out.addAll(cols.values());
            return out;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().verticalTraversal(root));
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
