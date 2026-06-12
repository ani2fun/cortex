---
title: "Height of a Binary Tree"
summary: "Given the root of a binary tree, compute the height (number of nodes along the longest root-to-leaf path)."
prereqs:
  - 11-pattern-postorder-traversal-stateless/01-pattern
difficulty: easy
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Height of a Binary Tree

## Problem Statement

Compute the **height** of the tree — the number of nodes along the longest root-to-leaf path.

Base case: empty tree has height 0 (under the *node-counting* convention used in this problem). Each internal node returns `max(height(left), height(right)) + 1`. The root's answer is the tree's height.

> **Note on conventions:** This problem uses the *node-counting* convention (empty = 0, single node = 1). The pattern lesson used the *edge-counting* convention (empty = -1 by some formulations). Both are common; *always read the problem carefully* and pick base cases that make the recurrence consistent.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, null, null, 7]
Output: 3
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 7]
Output: 3
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
    def height_of_binary_tree(self, root):
        # Your code goes here — base case None → 0;
        # otherwise max(height(left), height(right)) + 1
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
print(Solution().height_of_binary_tree(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int heightOfBinaryTree(TreeNode root) {
            // Your code goes here — base case null → 0;
            // otherwise Math.max(height(left), height(right)) + 1
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().heightOfBinaryTree(root));
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
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]" }, "expected": "3" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]" }, "expected": "3" },
    { "args": { "root": "[]" }, "expected": "0" },
    { "args": { "root": "[5]" }, "expected": "1" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "3" },
    { "args": { "root": "[1, null, 2, null, 3]" }, "expected": "3" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]" }, "expected": "3" },
    { "args": { "root": "[1, 2]" }, "expected": "2" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A single bottom-up recursion: the empty tree has height 0; every other node's height is `1 + max(left height, right height)`. Each node's result depends only on its children's return values — nothing shared — so the left and right subtrees are completely independent.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def height_of_binary_tree(self, root):

        # Empty tree has height 0
        if root is None:
            return 0

        # Recursively calculate the height of the left and right subtrees
        left_height = self.height_of_binary_tree(root.left)
        right_height = self.height_of_binary_tree(root.right)

        # Return the maximum height among the left and right subtrees
        # plus 1 for the current node
        return max(left_height, right_height) + 1

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
print(Solution().height_of_binary_tree(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int heightOfBinaryTree(TreeNode root) {

            // Empty tree has height 0
            if (root == null) {
                return 0;
            }

            // Recursively calculate the height of the left and right subtrees
            int leftHeight = heightOfBinaryTree(root.left);
            int rightHeight = heightOfBinaryTree(root.right);

            // Return the maximum height among the left and right subtrees
            // plus 1 for the current node
            return Math.max(leftHeight, rightHeight) + 1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().heightOfBinaryTree(root));
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
