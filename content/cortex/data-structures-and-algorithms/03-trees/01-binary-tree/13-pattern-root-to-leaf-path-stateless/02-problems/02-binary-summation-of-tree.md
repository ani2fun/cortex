---
title: "Binary Summation of Tree"
summary: "Each node's value is 0 or 1. Each root-to-leaf path is a binary number (MSB at root). Return the sum of all these binary numbers in decimal."
prereqs:
  - 13-pattern-root-to-leaf-path-stateless/01-pattern
difficulty: medium
kind: problem
topics: [root-to-leaf-path, binary-tree]
---

# Problem 2 — Binary summation of tree

## Problem Statement

Each node's value is `0` or `1`. Each root-to-leaf path forms a **binary number** with the most significant bit at the root. Return the **sum** of all these binary numbers, in decimal.

The accumulator is the *binary number so far* — at each node, shift left and OR in the current bit (`acc = (acc << 1) | node.val`). At a leaf, return the accumulator. Internal nodes sum their children.

## Examples

**Example 1:**
```
Input:  root = [1, 0, 1, 1, null, null, 1]
Output: 12
```

Tree:
```
    1
   / \
  0   1
 /     \
1       1
```
Paths: `1→0→1` = 101₂ = 5, `1→1→1` = 111₂ = 7. Sum = **12**.

**Example 2:**
```
Input:  root = [0, 1, 0, null, null, 1, 0]
Output: 2
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- Each node value is `0` or `1`
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
    def binary_summation_of_tree(self, root):
        # Your code goes here — carry the running binary number DOWN as an argument:
        # acc = (acc << 1) | node.val at each node; at a leaf return acc;
        # at internal nodes return sum of both children.
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
print(Solution().binary_summation_of_tree(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int binarySummationOfTree(TreeNode root) {
            // Your code goes here — carry the running binary number DOWN as an argument:
            // acc = (acc << 1) | node.val at each node; at a leaf return acc;
            // at internal nodes return sum of both children.
            return 0;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().binarySummationOfTree(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 0, 1, 1, null, null, 1]" }
  ],
  "cases": [
    { "args": { "root": "[1, 0, 1, 1, null, null, 1]" }, "expected": "12" },
    { "args": { "root": "[0, 1, 0, null, null, 1, 0]" }, "expected": "2" },
    { "args": { "root": "[]" }, "expected": "0" },
    { "args": { "root": "[1]" }, "expected": "1" },
    { "args": { "root": "[0]" }, "expected": "0" },
    { "args": { "root": "[1, 1]" }, "expected": "3" },
    { "args": { "root": "[1, 0, 0]" }, "expected": "4" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A single top-down recursion carries the binary number so far as an argument. At each node: shift left and OR in the current bit (`acc = (acc << 1) | node.val`). At a leaf, the path is complete — return `acc`. At an internal node, sum both children's results. The identity for `+` at a missing child is `0`.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def binary_summation_of_tree_helper(self, root, current_sum):
        if not root:
            return 0
        current_sum = (current_sum << 1) | root.val
        if not root.left and not root.right:
            return current_sum
        left_sum = self.binary_summation_of_tree_helper(root.left, current_sum)
        right_sum = self.binary_summation_of_tree_helper(root.right, current_sum)
        return left_sum + right_sum

    def binary_summation_of_tree(self, root):
        return self.binary_summation_of_tree_helper(root, 0)

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
print(Solution().binary_summation_of_tree(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int binarySummationOfTreeHelper(TreeNode root, int currentSum) {
            if (root == null) return 0;
            currentSum = (currentSum << 1) | root.val;
            if (root.left == null && root.right == null) return currentSum;
            int leftSum = binarySummationOfTreeHelper(root.left, currentSum);
            int rightSum = binarySummationOfTreeHelper(root.right, currentSum);
            return leftSum + rightSum;
        }

        public int binarySummationOfTree(TreeNode root) {
            return binarySummationOfTreeHelper(root, 0);
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().binarySummationOfTree(root));
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
