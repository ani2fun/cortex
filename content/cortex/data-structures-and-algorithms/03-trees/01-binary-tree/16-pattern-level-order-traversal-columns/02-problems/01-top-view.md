---
title: "Top View"
summary: "Return the values of nodes visible from above, ordered left-to-right by column. Each column's top is the first node BFS encounters in that column."
prereqs:
  - 16-pattern-level-order-traversal-columns/01-pattern
difficulty: medium
kind: problem
topics: [level-order-traversal, binary-tree]
---

# Problem 1 — Top view

## Problem Statement

Return the values of nodes visible *from above*, ordered left-to-right by column.

A column's "top" is the *first* node BFS encounters in that column (BFS processes shallower nodes before deeper ones, so the first node in any column is its highest one).

The trick: when we visit a node and its column is **not yet** in the map, record it; otherwise skip. BFS guarantees the first arrival at any column is the topmost one.

> *Predict before reading on — would a depth-first traversal work for top view?*
>
> Not directly. DFS visits nodes in *recursion order*, not depth order, so the first node DFS hits in column −1 isn't necessarily the topmost. You'd need to remember each node's *depth* and only update the per-column entry when you find a *shallower* node — which is more work than just using BFS, where the first arrival is automatically the topmost.

## Examples

**Example 1:**
```
Input:  root = [3, 9, 20, 4, 11, 15, 7]
Output: [4, 9, 3, 20, 7]
```

**Example 2:**
```
Input:  root = [1, 2, 3, 4, null, null, 7, 9]
Output: [9, 4, 2, 1, 3, 7]
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
    def top_view(self, root):
        # Your code goes here — BFS carrying (node, col); record first arrival
        # per column; return [seen[c] for c in range(min, max+1)]
        return []

root = build_tree(json.loads(input()))
print(Solution().top_view(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        public List<Integer> topView(TreeNode root) {
            // Your code goes here — BFS with Map.entry(node, col);
            // TreeMap for sorted output; putIfAbsent for first-wins
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().topView(root));
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
    { "args": { "root": "[3, 9, 20, 4, 11, 15, 7]" }, "expected": "[4, 9, 3, 20, 7]" },
    { "args": { "root": "[1, 2, 3, 4, null, null, 7, 9]" }, "expected": "[9, 4, 2, 1, 3, 7]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[1]" }, "expected": "[1]" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "[3, 2, 1]" },
    { "args": { "root": "[1, null, 2, null, 3]" }, "expected": "[1, 2, 3]" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "[2, 1, 3]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

BFS carrying `(node, col)`. At each node, record `col → node.val` only if `col` hasn't been seen yet — the first BFS arrival per column is always the shallowest (top) node in that column. Collect into a `TreeMap` (Java) or iterate `range(min, max+1)` (Python) to read left→right.

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
    def top_view(self, root):
        if root is None:
            return []
        seen, q = {}, deque([(root, 0)])
        while q:
            node, c = q.popleft()
            if c not in seen:
                seen[c] = node.val      # first BFS arrival = topmost in column
            if node.left:  q.append((node.left,  c - 1))
            if node.right: q.append((node.right, c + 1))
        return [seen[c] for c in range(min(seen), max(seen) + 1)]

root = build_tree(json.loads(input()))
print(Solution().top_view(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        public List<Integer> topView(TreeNode root) {
            List<Integer> out = new ArrayList<>();
            if (root == null) return out;
            Map<Integer, Integer> cols = new TreeMap<>();
            Deque<Map.Entry<TreeNode, Integer>> q = new ArrayDeque<>();
            q.add(Map.entry(root, 0));
            while (!q.isEmpty()) {
                var e = q.poll();
                TreeNode node = e.getKey(); int c = e.getValue();
                if (!cols.containsKey(c)) cols.put(c, node.val);   // first BFS = top
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
        System.out.println(new Solution().topView(root));
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
