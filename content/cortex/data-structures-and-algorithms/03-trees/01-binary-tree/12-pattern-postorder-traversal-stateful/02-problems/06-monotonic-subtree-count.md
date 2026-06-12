---
title: "Monotonic Subtree Count"
summary: "Count subtrees that are entirely mono-valued — every node in the subtree has the same value."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: hard
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Monotonic Subtree Count

## Problem Statement

Given the **root** of a binary tree, count the number of subtrees that are **entirely mono-valued** — every node in the subtree has the same value.

Each call returns whether *its* subtree is mono-valued; along the way, increment a global counter when it is. A subtree is mono-valued iff: both children's subtrees are mono-valued, *and* both children (if they exist) have the same value as the current node.

## Examples

**Example 1:**
```
Input:  root = [1, 1, 5, 1, null, null, 5]
Output: 4
```
The four mono-valued subtrees: three leaves (`1`, `1`, `5`) and the left subtree rooted at depth 1 (`[1, 1]`).

**Example 2:**
```
Input:  root = [3, 8, 1, 8, null, 1, 1]
Output: 5
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-1000 ≤ node.val ≤ 1000`

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def __init__(self):
        self.subtree_count = 0

    def monotonic_subtree_count(self, root):
        # Your code goes here — postorder: return True if this subtree is mono-valued;
        # increment self.subtree_count when it is.
        def is_monotonic(node):
            return True
        is_monotonic(root)
        return self.subtree_count

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
print(Solution().monotonic_subtree_count(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int subtreeCount = 0;

        private boolean isMonotonic(TreeNode root) {
            // Your code goes here — postorder: return true if this subtree is mono-valued;
            // increment subtreeCount when it is.
            return true;
        }

        int monotonicSubtreeCount(TreeNode root) {
            subtreeCount = 0;
            isMonotonic(root);
            return subtreeCount;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().monotonicSubtreeCount(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 1, 5, 1, null, null, 5]" }
  ],
  "cases": [
    { "args": { "root": "[1, 1, 5, 1, null, null, 5]" }, "expected": "4" },
    { "args": { "root": "[3, 8, 1, 8, null, 1, 1]" }, "expected": "5" },
    { "args": { "root": "[]" }, "expected": "0" },
    { "args": { "root": "[7]" }, "expected": "1" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "2" },
    { "args": { "root": "[2, 2, 2]" }, "expected": "3" },
    { "args": { "root": "[1, 1, 1, 1, 1, 1, 1]" }, "expected": "7" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A leaf is always mono-valued. An internal node is mono-valued iff both subtrees are mono-valued AND both children (if present) share the same value as the node. The counter increments when the node qualifies; the boolean return propagates the property upward.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def __init__(self):
        self.subtree_count = 0

    def is_monotonic_subtree(self, root):
        # An empty node is trivially monotonic
        if not root:
            return True

        # Check if the left child is monotonic
        left_monotonic = self.is_monotonic_subtree(root.left)

        # Check if the right child is monotonic
        right_monotonic = self.is_monotonic_subtree(root.right)

        # If either left or right subtree is not monotonic, return False
        if not left_monotonic or not right_monotonic:
            return False

        # If the left child exists and does not have the same value,
        # return False
        if root.left and root.left.val != root.val:
            return False

        # If the right child exists and does not have the same value,
        # return False
        if root.right and root.right.val != root.val:
            return False

        # This node and its children form a monotonic subtree
        self.subtree_count += 1
        return True

    def monotonic_subtree_count(self, root):
        self.is_monotonic_subtree(root)
        return self.subtree_count

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
print(Solution().monotonic_subtree_count(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int subtreeCount = 0;

        private boolean isMonotonicSubtree(TreeNode root) {
            // An empty node is trivially monotonic
            if (root == null) {
                return true;
            }

            // Check if the left child is monotonic
            boolean leftMonotonic = isMonotonicSubtree(root.left);

            // Check if the right child is monotonic
            boolean rightMonotonic = isMonotonicSubtree(root.right);

            // If either left or right subtree is not monotonic, return false
            if (!leftMonotonic || !rightMonotonic) {
                return false;
            }

            // If the left child exists and does not have the same value,
            // return false
            if (root.left != null && root.left.val != root.val) {
                return false;
            }

            // If the right child exists and does not have the same value,
            // return false
            if (root.right != null && root.right.val != root.val) {
                return false;
            }

            // This node and its children form a monotonic subtree
            subtreeCount++;
            return true;
        }

        int monotonicSubtreeCount(TreeNode root) {
            subtreeCount = 0;
            isMonotonicSubtree(root);
            return subtreeCount;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().monotonicSubtreeCount(root));
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
