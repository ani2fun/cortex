---
title: "Is It a Perfect Binary Tree"
summary: "Given the root of a binary tree, return true if and only if every internal node has two children and every leaf is at the same depth."
prereqs:
  - 11-pattern-postorder-traversal-stateless/01-pattern
difficulty: medium
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Is It a Perfect Binary Tree?

## Problem Statement

Return `true` iff every internal node has two children **and** every leaf is at the same depth.

A clean two-pass approach:

1. Find the depth of the leftmost leaf — that's where every leaf must sit.
2. Recursively check: every leaf is at that depth; every internal node has two children.

A one-pass approach also exists (return both `(isPerfect, height)` from each call), but that's the *stateful* postorder pattern from the next lesson. The two-pass version below is pure stateless.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, null, null, 7]
Output: false
```

**Example 2:**
```
Input:  root = [1, 8, 4, 3, 5, 2, 7]
Output: true
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `O(n)` time, `O(h)` recursion stack

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def perfect_binary_tree(self, root):
        # Your code goes here — find leftmost depth, then check every leaf
        # is at that depth and every internal node has two children.
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
print("true" if Solution().perfect_binary_tree(root) else "false")
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean perfectBinaryTree(TreeNode root) {
            // Your code goes here — find leftmost depth, then check every leaf
            // is at that depth and every internal node has two children.
            return true;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().perfectBinaryTree(root));
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
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]" }, "expected": "false" },
    { "args": { "root": "[1, 8, 4, 3, 5, 2, 7]" }, "expected": "true" },
    { "args": { "root": "[]" }, "expected": "true" },
    { "args": { "root": "[5]" }, "expected": "true" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "true" },
    { "args": { "root": "[1, 2, null]" }, "expected": "false" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]" }, "expected": "true" },
    { "args": { "root": "[1, 2, 3, 4, 5]" }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

First find the leftmost-leaf depth with a simple iterative walk down `left` pointers. Then a recursive check verifies two invariants at every node: leaves are at exactly that depth, and internal nodes have two children. The recursive call takes `(root, depth, level)` — `level` is the current depth (0 at root), `depth` is the target leaf depth.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def find_depth(self, root):
        depth = 0
        while root:
            depth += 1
            root = root.left
        return depth

    def is_perfect_binary_tree(self, root, depth, level):

        # An empty tree is a perfect binary tree
        if not root:
            return True

        # If it is a leaf node, check if it is at the correct depth
        if not root.left and not root.right:
            return depth == level + 1

        # If an internal node has only one child, it's not a perfect
        # binary tree
        if not root.left or not root.right:
            return False

        # Recursively check the left and right subtrees
        is_left_subtree_perfect = self.is_perfect_binary_tree(
            root.left, depth, level + 1
        )
        is_right_subtree_perfect = self.is_perfect_binary_tree(
            root.right, depth, level + 1
        )

        # Return true if both subtrees are perfect
        return is_left_subtree_perfect and is_right_subtree_perfect

    def perfect_binary_tree(self, root):

        # An empty tree is a perfect binary tree
        if not root:
            return True

        # Find the depth of the leftmost leaf
        depth = self.find_depth(root)

        # Check if the tree is perfect
        return self.is_perfect_binary_tree(root, depth, 0)

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
print("true" if Solution().perfect_binary_tree(root) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int findDepth(TreeNode root) {
            int depth = 0;
            while (root != null) {
                depth++;
                root = root.left;
            }
            return depth;
        }

        private boolean isPerfectBinaryTree(TreeNode root, int depth, int level) {

            // An empty tree is a perfect binary tree
            if (root == null) {
                return true;
            }

            // If it is a leaf node, check if it is at the correct depth
            if (root.left == null && root.right == null) {
                return depth == level + 1;
            }

            // If an internal node has only one child, it's not a perfect
            // binary tree
            if (root.left == null || root.right == null) {
                return false;
            }

            // Recursively check the left and right subtrees
            boolean isLeftSubtreePerfect = isPerfectBinaryTree(root.left, depth, level + 1);
            boolean isRightSubtreePerfect = isPerfectBinaryTree(root.right, depth, level + 1);

            // Return true if both subtrees are perfect
            return isLeftSubtreePerfect && isRightSubtreePerfect;
        }

        boolean perfectBinaryTree(TreeNode root) {

            // An empty tree is a perfect binary tree
            if (root == null) {
                return true;
            }

            // Find the depth of the leftmost leaf
            int depth = findDepth(root);

            // Check if the tree is perfect
            return isPerfectBinaryTree(root, depth, 0);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().perfectBinaryTree(root));
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
