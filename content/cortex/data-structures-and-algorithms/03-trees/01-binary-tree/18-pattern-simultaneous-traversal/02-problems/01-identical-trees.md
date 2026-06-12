---
title: "Identical Trees"
summary: "Given two binary trees, return true if they have the same shape and the same value at every position."
prereqs:
  - 18-pattern-simultaneous-traversal/01-pattern
difficulty: easy
kind: problem
topics: [simultaneous-traversal, binary-tree]
---

# Identical trees

## Problem Statement

Given the roots of two binary trees `a` and `b`, return `true` if and only if they are **identical** — the same shape and the same value at every position — and `false` otherwise.

This is the base case of the simultaneous-traversal pattern: walk both trees in lockstep, pairing the *same* positions. At each step the two nodes must agree on existence (both present or both absent) and, when present, on value; then the left subtrees must match each other and the right subtrees must match each other.

## Examples

**Example 1:**
```
Input:  a = [1, 2, 3],  b = [1, 2, 3]
Output: true
```

**Example 2:**
```
Input:  a = [1, 2],  b = [1, null, 2]
Output: false       (one tree's child is a left child, the other's is a right child)
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "a = [1, 2, 3],  b = [1, 2, 4]",
  "options": ["true", "false"],
  "answer": "false"
}
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴` per tree
- `-10⁴ ≤ node.val ≤ 10⁴`
- One simultaneous pass — `O(n)` time, `O(h)` recursion stack

```python run viz=binary-tree viz-root=a
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def is_same(self, a, b):
        # Your code goes here — pair the SAME positions in both trees:
        # both None → match; exactly one None → differ; values differ → differ;
        # otherwise recurse left-with-left and right-with-right.
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

a = build_tree(json.loads(input()))   # the first tree's level-order values
b = build_tree(json.loads(input()))   # the second tree's level-order values
print("true" if Solution().is_same(a, b) else "false")
```

```java run viz=binary-tree viz-root=a
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean isSame(TreeNode a, TreeNode b) {
            // Your code goes here — pair the SAME positions in both trees:
            // both null → match; exactly one null → differ; values differ → differ;
            // otherwise recurse left-with-left and right-with-right.
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode a = buildTree(parseIntegerArray(sc.nextLine()));
        TreeNode b = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().isSame(a, b));
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
    { "id": "a", "label": "a", "type": "tree", "placeholder": "[1, 2, 3]" },
    { "id": "b", "label": "b", "type": "tree", "placeholder": "[1, 2, 3]" }
  ],
  "cases": [
    { "args": { "a": "[1, 2, 3]", "b": "[1, 2, 3]" }, "expected": "true" },
    { "args": { "a": "[1, 2]", "b": "[1, null, 2]" }, "expected": "false" },
    { "args": { "a": "[1, 2, 3]", "b": "[1, 2, 4]" }, "expected": "false" },
    { "args": { "a": "[]", "b": "[]" }, "expected": "true" },
    { "args": { "a": "[1]", "b": "[1]" }, "expected": "true" },
    { "args": { "a": "[1, 2, 3, 4]", "b": "[1, 2, 3, null, 4]" }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Recurse over both trees at once, always pairing the *same* position in each. Three base checks resolve every structural case: if both nodes are `None` the position matches; if exactly one is `None` the shapes differ; if the values differ they are not identical. Past those, the answer is the conjunction of "left subtrees identical" **and** "right subtrees identical" — the pairing never crosses sides. One pass, `O(n)` time and `O(h)` stack.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def is_same(self, a, b):
        if a is None and b is None:    # both empty here → match
            return True
        if a is None or b is None:     # exactly one empty → shapes differ
            return False
        if a.val != b.val:             # values differ → not identical
            return False
        return self.is_same(a.left, b.left) and self.is_same(a.right, b.right)

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

a = build_tree(json.loads(input()))   # the first tree's level-order values
b = build_tree(json.loads(input()))   # the second tree's level-order values
print("true" if Solution().is_same(a, b) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean isSame(TreeNode a, TreeNode b) {
            if (a == null && b == null) return true;   // both empty → match
            if (a == null || b == null) return false;  // one empty → shapes differ
            if (a.val != b.val) return false;          // values differ
            return isSame(a.left, b.left) && isSame(a.right, b.right);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode a = buildTree(parseIntegerArray(sc.nextLine()));
        TreeNode b = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().isSame(a, b));
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
