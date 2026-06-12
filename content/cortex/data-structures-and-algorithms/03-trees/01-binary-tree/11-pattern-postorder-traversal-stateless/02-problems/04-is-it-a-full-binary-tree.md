---
title: "Is It a Full Binary Tree"
summary: "Given the root of a binary tree, return true if and only if every node has either zero or two children."
prereqs:
  - 11-pattern-postorder-traversal-stateless/01-pattern
difficulty: medium
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Is It a Full Binary Tree?

## Problem Statement

Return `true` iff every node has either zero or two children.

Three cases at each node:

- Empty tree → vacuously full → `true`.
- Leaf (both children null) → full → `true`.
- Exactly one child null → *not* full → `false`.
- Both children present → recurse and require both subtrees full.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, null, null, 2]
Output: false
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 3, 5]
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
    def full_binary_tree(self, root):
        # Your code goes here — empty/leaf → true; one child → false;
        # two children → full_binary_tree(left) and full_binary_tree(right)
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
print("true" if Solution().full_binary_tree(root) else "false")
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean fullBinaryTree(TreeNode root) {
            // Your code goes here — null/leaf → true; one child → false;
            // two children → fullBinaryTree(left) && fullBinaryTree(right)
            return true;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().fullBinaryTree(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, null, null, 2]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, null, null, 2]" }, "expected": "false" },
    { "args": { "root": "[1, 8, 4, null, null, 3, 5]" }, "expected": "true" },
    { "args": { "root": "[]" }, "expected": "true" },
    { "args": { "root": "[5]" }, "expected": "true" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "true" },
    { "args": { "root": "[1, 2, null]" }, "expected": "false" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]" }, "expected": "true" },
    { "args": { "root": "[1, 2, 3, 4, 5]" }, "expected": "true" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A bottom-up recursion with four cases: empty or leaf → `true`; exactly one child null → `false`; two children → both subtrees must also be full. The check is purely structural — node values don't matter — and each call depends only on its children's returns.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def full_binary_tree(self, root):

        # An empty tree is a full binary tree
        if not root:
            return True

        # A node with no children is a full binary tree
        if not root.left and not root.right:
            return True

        # A node with only one child is not a full binary tree
        if not root.left or not root.right:
            return False

        # Check if the left and right subtrees are also full binary trees
        is_subtree_left_full = self.full_binary_tree(root.left)
        is_subtree_right_full = self.full_binary_tree(root.right)

        # Return true if both subtrees are full binary trees
        return is_subtree_left_full and is_subtree_right_full

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
print("true" if Solution().full_binary_tree(root) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean fullBinaryTree(TreeNode root) {

            // An empty tree is a full binary tree
            if (root == null) {
                return true;
            }

            // A node with no children is a full binary tree
            if (root.left == null && root.right == null) {
                return true;
            }

            // A node with only one child is not a full binary tree
            if (root.left == null || root.right == null) {
                return false;
            }

            // Check if the left and right subtrees are also full binary trees
            boolean isLeftSubtreeFull = fullBinaryTree(root.left);
            boolean isRightSubtreeFull = fullBinaryTree(root.right);

            // Return true if both subtrees are full binary trees
            return isLeftSubtreeFull && isRightSubtreeFull;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().fullBinaryTree(root));
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
