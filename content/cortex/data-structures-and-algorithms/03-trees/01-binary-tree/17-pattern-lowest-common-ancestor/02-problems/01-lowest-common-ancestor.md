---
title: "Lowest Common Ancestor"
summary: "Given a binary tree and two node values, find their lowest common ancestor — the deepest node that has both as descendants."
prereqs:
  - 17-pattern-lowest-common-ancestor/01-pattern
difficulty: medium
kind: problem
topics: [lowest-common-ancestor, binary-tree]
---

# Lowest common ancestor

## Problem Statement

Given the `root` of a binary tree and two distinct node values `p` and `q` (both guaranteed to exist in the tree, and all node values are distinct), return the **value of their lowest common ancestor (LCA)** — the deepest node in the tree that has both `p` and `q` as descendants (a node is a descendant of itself).

This is the generic LCA algorithm: one postorder pass that lets each target *bubble up*. A node reports a target if it *is* a target or if a target was found in one of its subtrees. The first node that sees a target surface from **both** of its sides is the LCA.

## Examples

**Example 1:**
```
Input:  root = [3, 5, 1, 6, 2, 0, 8, null, null, 7, 4],  p = 5,  q = 1
Output: 3
```

**Example 2:**
```
Input:  root = [3, 5, 1, 6, 2, 0, 8, null, null, 7, 4],  p = 5,  q = 4
Output: 5       (4 is in 5's subtree, so 5 is its own ancestor here)
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "root = [1, 2, 3],  p = 2,  q = 3",
  "options": ["1", "2", "3", "null"],
  "answer": "1"
}
```

## Constraints

- `1 ≤ number of nodes ≤ 10⁴`
- All node values are distinct; `p` and `q` both exist in the tree
- One postorder pass — `O(n)` time, `O(h)` recursion stack

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def lca(self, node, p_val, q_val):
        # Your code goes here — postorder: None → None; if node is a target,
        # return it; recurse both sides; if BOTH sides return non-None this node
        # is the LCA; otherwise pass up whichever side found something.
        pass

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
p_val = int(input())                     # first target node value
q_val = int(input())                     # second target node value
result = Solution().lca(root, p_val, q_val)
print(result.val if result is not None else "null")
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        TreeNode lca(TreeNode node, int pVal, int qVal) {
            // Your code goes here — postorder: null → null; if node is a target,
            // return it; recurse both sides; if BOTH sides return non-null this node
            // is the LCA; otherwise pass up whichever side found something.
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int pVal = Integer.parseInt(sc.nextLine().trim());
        int qVal = Integer.parseInt(sc.nextLine().trim());
        TreeNode result = new Solution().lca(root, pVal, qVal);
        System.out.println(result != null ? result.val : "null");
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[3, 5, 1, 6, 2, 0, 8, null, null, 7, 4]" },
    { "id": "p", "label": "p", "type": "int", "placeholder": "5" },
    { "id": "q", "label": "q", "type": "int", "placeholder": "1" }
  ],
  "cases": [
    { "args": { "root": "[3, 5, 1, 6, 2, 0, 8, null, null, 7, 4]", "p": "5", "q": "1" }, "expected": "3" },
    { "args": { "root": "[3, 5, 1, 6, 2, 0, 8, null, null, 7, 4]", "p": "5", "q": "4" }, "expected": "5" },
    { "args": { "root": "[3, 5, 1, 6, 2, 0, 8, null, null, 7, 4]", "p": "7", "q": "4" }, "expected": "2" },
    { "args": { "root": "[1, 2, 3]", "p": "2", "q": "3" }, "expected": "1" },
    { "args": { "root": "[1, 2, 3]", "p": "2", "q": "2" }, "expected": "2" },
    { "args": { "root": "[1, 2]", "p": "1", "q": "2" }, "expected": "1" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A single postorder recursion. The base case `None` reports "no target here." If the current node is one of the targets, it returns itself — that target now bubbles upward. Otherwise it asks both subtrees; if **both** come back non-`None`, the two targets were found on opposite sides, so this node is their lowest common ancestor. If only one side reports a target, that result is passed up unchanged (the LCA is somewhere above, or this side holds both). `O(n)` time, `O(h)` stack.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def lca(self, node, p_val, q_val):
        if node is None:
            return None
        if node.val == p_val or node.val == q_val:
            return node                      # hit a target → bubble it up
        left = self.lca(node.left, p_val, q_val)
        right = self.lca(node.right, p_val, q_val)
        if left and right:
            return node                      # targets from BOTH sides → this is the LCA
        return left or right                 # both on one side (or neither) → pass up

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
p_val = int(input())                     # first target node value
q_val = int(input())                     # second target node value
result = Solution().lca(root, p_val, q_val)
print(result.val if result is not None else "null")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        TreeNode lca(TreeNode node, int pVal, int qVal) {
            if (node == null) return null;
            if (node.val == pVal || node.val == qVal) return node;   // bubble a target up
            TreeNode left = lca(node.left, pVal, qVal), right = lca(node.right, pVal, qVal);
            if (left != null && right != null) return node;          // split → LCA
            return left != null ? left : right;                      // pass up the one side
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int pVal = Integer.parseInt(sc.nextLine().trim());
        int qVal = Integer.parseInt(sc.nextLine().trim());
        TreeNode result = new Solution().lca(root, pVal, qVal);
        System.out.println(result != null ? result.val : "null");
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
