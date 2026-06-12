---
title: "BST Validator"
summary: "Given the root of a binary tree, return true if the tree is a valid BST (every in-order adjacent pair is strictly increasing), false otherwise."
prereqs:
  - 11-pattern-sorted-traversal/01-pattern
difficulty: medium
kind: problem
topics: [sorted-traversal, binary-search-tree]
---

# BST validator

## Problem Statement

Given the **root** of a binary tree, return `true` if the tree is a valid BST, `false` otherwise. A valid BST has these properties:

- Every node has a unique key.
- The left subtree contains only values strictly less than the node.
- The right subtree contains only values strictly greater than the node.
- Both subtrees are themselves BSTs.

## Examples

**Example 1:**
```
Input:  root = [4, 2, 5, 1, 3, null, 6]
Output: true
```

**Example 2:**
```
Input:  root = [9, 5, 12, 4, null, null, 11]
Output: false
```
Node `11` is in the right subtree of `12` but `11 < 12` — rule violated.

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-2³¹ ≤ node.val ≤ 2³¹ − 1`

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def bst_validator(self, root):
        # Your code goes here — iterative in-order; track prev value;
        # return False if node.val <= prev at any step. Return True if all pass.
        return True

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
print("true" if Solution().bst_validator(root) else "false")
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        public boolean bstValidator(TreeNode root) {
            // Your code goes here — iterative in-order; Integer prev = null;
            // return false if node.val <= prev. Return true if all pairs pass.
            return true;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().bstValidator(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[4, 2, 5, 1, 3, null, 6]" }
  ],
  "cases": [
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]" }, "expected": "true" },
    { "args": { "root": "[9, 5, 12, 4, null, null, 11]" }, "expected": "false" },
    { "args": { "root": "[]" }, "expected": "true" },
    { "args": { "root": "[5]" }, "expected": "true" },
    { "args": { "root": "[5, 5]" }, "expected": "false" },
    { "args": { "root": "[10, null, 8]" }, "expected": "false" },
    { "args": { "root": "[4, 2, 6, 1, 3, 5, 7]" }, "expected": "true" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

A valid BST has a **strictly increasing** in-order traversal. So this is just: walk in-order, keep the previous value, and at every step assert `prev < current`. The moment any pair fails, the tree is invalid.

This is dramatically simpler than the recursive `(min, max)` bounds technique you may have seen — the in-order trick reduces tree validity to *list monotonicity*, which is a one-liner.

</details>
<details>
<summary><h2>Solution</h2></summary>

An in-order traversal of a valid BST produces a strictly increasing sequence. We walk iteratively, tracking the previous value, and immediately return `false` the moment the current node is not strictly greater. An empty tree (or single node) is trivially valid.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def bst_validator(self, root):
        prev = None
        stack, node = [], root
        while stack or node:
            while node:
                stack.append(node); node = node.left
            node = stack.pop()
            if prev is not None and node.val <= prev:   # not strictly increasing → invalid
                return False
            prev = node.val
            node = node.right
        return True

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
print("true" if Solution().bst_validator(root) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        public boolean bstValidator(TreeNode root) {
            Deque<TreeNode> stack = new ArrayDeque<>();
            TreeNode node = root; Integer prev = null;
            while (!stack.isEmpty() || node != null) {
                while (node != null) { stack.push(node); node = node.left; }
                node = stack.pop();
                if (prev != null && node.val <= prev) return false;   // must strictly increase
                prev = node.val;
                node = node.right;
            }
            return true;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().bstValidator(root));
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
