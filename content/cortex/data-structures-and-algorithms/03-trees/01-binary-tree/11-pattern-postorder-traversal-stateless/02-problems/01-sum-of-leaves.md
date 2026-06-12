---
title: "Sum of Leaves"
summary: "Given the root of a binary tree, compute the sum of all leaf node values."
prereqs:
  - 11-pattern-postorder-traversal-stateless/01-pattern
difficulty: easy
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Sum of Leaves

## Problem Statement

Given the **root** of a binary tree, compute the sum of all **leaf** node values. A leaf is a node with no children.

Base case: empty tree contributes 0. Leaf returns its own value. Internal node returns `sumOfLeaves(left) + sumOfLeaves(right)` — the node's own value doesn't enter (it's not a leaf).

## Examples

**Example 1:**
```
Input:  root = [1, 2, 5, 7, null, null, 3]
Output: 10
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 9, 7]
Output: 24
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
    def sum_of_leaves(self, root):
        # Your code goes here — base case None → 0; at a leaf return node.val;
        # otherwise return sum_of_leaves(left) + sum_of_leaves(right)
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
print(Solution().sum_of_leaves(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int sumOfLeaves(TreeNode root) {
            // Your code goes here — base case null → 0; at a leaf return root.val;
            // otherwise return sumOfLeaves(left) + sumOfLeaves(right)
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().sumOfLeaves(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 5, 7, null, null, 3]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 5, 7, null, null, 3]" }, "expected": "10" },
    { "args": { "root": "[1, 8, 4, null, null, 9, 7]" }, "expected": "24" },
    { "args": { "root": "[]" }, "expected": "0" },
    { "args": { "root": "[7]" }, "expected": "7" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "3" },
    { "args": { "root": "[1, null, 2, null, 3]" }, "expected": "3" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]" }, "expected": "22" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A single bottom-up recursion: empty tree contributes 0; a leaf returns its own value; an internal node returns the sum of its children's results (its own value is excluded — it's not a leaf). Because each call's answer depends only on its children's return values and not on any shared state, the left and right subtrees can't interfere with each other.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def sum_of_leaves(self, root):

        # Base case: if the tree is empty
        if not root:
            return 0

        # If it's a leaf node, return its value
        if not root.left and not root.right:
            return root.val

        # Recursively sum up leaf nodes in left and right subtrees
        left_sum = self.sum_of_leaves(root.left)
        right_sum = self.sum_of_leaves(root.right)

        # Return the sum of leaf nodes in left and right subtrees
        return left_sum + right_sum

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
print(Solution().sum_of_leaves(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int sumOfLeaves(TreeNode root) {

            // Base case: if the tree is empty
            if (root == null) {
                return 0;
            }

            // If it's a leaf node, return its value
            if (root.left == null && root.right == null) {
                return root.val;
            }

            // Recursively sum up leaf nodes in left and right subtrees
            int leftSum = sumOfLeaves(root.left);
            int rightSum = sumOfLeaves(root.right);

            // Return the sum of leaf nodes in left and right subtrees
            return leftSum + rightSum;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().sumOfLeaves(root));
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
