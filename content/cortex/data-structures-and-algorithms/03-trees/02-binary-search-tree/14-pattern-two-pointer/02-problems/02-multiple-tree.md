---
title: "Multiple Tree"
summary: "Given the root of a BST, return true if for every pair of nodes formed by taking one from the start and one from the end of the in-order traversal, the end node's value is a positive multiple of the start node's value."
prereqs:
  - 14-pattern-two-pointer/01-pattern
difficulty: medium
kind: problem
topics: [two-pointer, binary-search-tree]
---

# Multiple tree

## Problem Statement

Given the **root** of a BST, return `true` if for **every** pair of nodes formed by taking one from the start and one from the end of the in-order traversal, the *end* node's value is a positive multiple of the *start* node's value. Return `false` otherwise.

## Examples

**Example 1:**
```
Input:  root = [4, 2, 6, 1, null, null, 7]
Output: true
Explanation: Sorted: [1, 2, 4, 6, 7]. Pairs: (1, 7), (2, 6), (4, 4). Each right % left == 0.
```

**Example 2:**
```
Input:  root = [2, 1, 5, null, null, 3, 7]
Output: false
Explanation: Sorted: [1, 2, 3, 5, 7]. Pair (2, 5) fails (5 % 2 ≠ 0).
```

## Constraints

- `1 ≤ number of nodes ≤ 10⁴`
- `1 ≤ node.val ≤ 10⁵`
- All node values are unique (BST property)

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def multiple_tree(self, root):
        # Your code goes here — use two BST iterators; for each converging pair
        # check right.val % left.val == 0; advance both; return False on first failure.
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
print("true" if Solution().multiple_tree(root) else "false")
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean multipleTree(TreeNode root) {
            // Your code goes here — use two BST iterators; for each converging pair
            // check rightNode.val % leftNode.val == 0; advance both; return false on failure.
            return true;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().multipleTree(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[4, 2, 6, 1, null, null, 7]" }
  ],
  "cases": [
    { "args": { "root": "[4, 2, 6, 1, null, null, 7]" }, "expected": "true" },
    { "args": { "root": "[2, 1, 5, null, null, 3, 7]" }, "expected": "false" },
    { "args": { "root": "[6]" }, "expected": "true" },
    { "args": { "root": "[3, 1, 6]" }, "expected": "true" },
    { "args": { "root": "[4, 2, 6, 1, null, null, 9]" }, "expected": "true" },
    { "args": { "root": "[6, 2, 12, 1, null, null, 24]" }, "expected": "true" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Same shape as two-sum, but the predicate is `right.val % left.val == 0`, and we always advance **both** pointers (each iteration consumes a unique pair from the outermost ends inward). Stop early on any failure; if all pairs pass, return `true`.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class ForwardBstIterator:
    def __init__(self, root):
        self.stack = []
        self._push_all_left(root)

    def _push_all_left(self, node):
        while node:
            self.stack.append(node)
            node = node.left

    def has_next(self):
        return bool(self.stack)

    def next(self):
        node = self.stack.pop()
        self._push_all_left(node.right)
        return node

class ReverseBstIterator:
    def __init__(self, root):
        self.stack = []
        self._push_all_right(root)

    def _push_all_right(self, node):
        while node:
            self.stack.append(node)
            node = node.right

    def has_next(self):
        return bool(self.stack)

    def next(self):
        node = self.stack.pop()
        self._push_all_right(node.left)
        return node

class Solution:
    def multiple_tree(self, root):
        if not root:
            return False
        left_iterator = ForwardBstIterator(root)
        right_iterator = ReverseBstIterator(root)
        left_node = left_iterator.next()
        right_node = right_iterator.next()
        while (left_node is not None and right_node is not None
               and left_node.val < right_node.val):
            if right_node.val % left_node.val != 0:
                return False
            left_node = left_iterator.next()
            right_node = right_iterator.next()
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
print("true" if Solution().multiple_tree(root) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class ForwardBstIterator {
        private Deque<TreeNode> stack = new ArrayDeque<>();
        ForwardBstIterator(TreeNode root) { pushAllLeft(root); }
        private void pushAllLeft(TreeNode node) {
            while (node != null) { stack.push(node); node = node.left; }
        }
        boolean hasNext() { return !stack.isEmpty(); }
        TreeNode next() {
            TreeNode node = stack.pop();
            pushAllLeft(node.right);
            return node;
        }
    }

    static class ReverseBstIterator {
        private Deque<TreeNode> stack = new ArrayDeque<>();
        ReverseBstIterator(TreeNode root) { pushAllRight(root); }
        private void pushAllRight(TreeNode node) {
            while (node != null) { stack.push(node); node = node.right; }
        }
        boolean hasNext() { return !stack.isEmpty(); }
        TreeNode next() {
            TreeNode node = stack.pop();
            pushAllRight(node.left);
            return node;
        }
    }

    static class Solution {
        boolean multipleTree(TreeNode root) {
            if (root == null) return false;
            ForwardBstIterator leftIterator = new ForwardBstIterator(root);
            ReverseBstIterator rightIterator = new ReverseBstIterator(root);
            TreeNode leftNode = leftIterator.next();
            TreeNode rightNode = rightIterator.next();
            while (leftNode != null && rightNode != null && leftNode.val < rightNode.val) {
                if (rightNode.val % leftNode.val != 0) return false;
                leftNode = leftIterator.next();
                rightNode = rightIterator.next();
            }
            return true;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().multipleTree(root));
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
