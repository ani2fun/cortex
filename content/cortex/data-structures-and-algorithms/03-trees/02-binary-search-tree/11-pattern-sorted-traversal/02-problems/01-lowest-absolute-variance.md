---
title: "Lowest Absolute Variance"
summary: "Given the root of a BST with at least two nodes, return the minimum absolute difference between the values of any two different nodes."
prereqs:
  - 11-pattern-sorted-traversal/01-pattern
difficulty: medium
kind: problem
topics: [sorted-traversal, binary-search-tree]
---

# Lowest absolute variance

## Problem Statement

Given the **root** of a binary search tree, return the lowest absolute variance — the minimum absolute difference — between the values of any two different nodes. The tree is guaranteed to have at least two nodes.

## Examples

**Example 1:**
```
Input:  root = [5, 4, 8, 2, null, null, 10]
Output: 1
```
The smallest gap is between `4` and `5`.

**Example 2:**
```
Input:  root = [10, 8, 14, 5, null, 12, 17]
Output: 2
```
The smallest gap is `2` (between `8` and `10`, or between `12` and `14`).

## Constraints

- `2 ≤ number of nodes ≤ 10⁴`
- `0 ≤ node.val ≤ 10⁵`
- The tree is a valid BST.

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def lowest_absolute_variance(self, root):
        # Your code goes here — iterative in-order walk; track the previous
        # node value; update min_diff = min(min_diff, node.val - prev).
        # Return min_diff.
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
print(Solution().lowest_absolute_variance(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        public int lowestAbsoluteVariance(TreeNode root) {
            // Your code goes here — iterative in-order; Integer prev = null;
            // update minDiff = Math.min(minDiff, node.val - prev);
            // Return minDiff.
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().lowestAbsoluteVariance(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[5, 4, 8, 2, null, null, 10]" }
  ],
  "cases": [
    { "args": { "root": "[5, 4, 8, 2, null, null, 10]" }, "expected": "1" },
    { "args": { "root": "[10, 8, 14, 5, null, 12, 17]" }, "expected": "2" },
    { "args": { "root": "[3, 1, 5]" }, "expected": "2" },
    { "args": { "root": "[5, 3, 7, 2, 4, 6, 8]" }, "expected": "1" },
    { "args": { "root": "[4, 2, null, 1]" }, "expected": "1" },
    { "args": { "root": "[100, 50, null, 40]" }, "expected": "10" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

An in-order walk of a BST delivers keys in ascending sorted order. For any sorted sequence, the minimum absolute difference between any two elements is always between **adjacent** elements. So we track the previous key seen (`prev`) and update `min_diff` on each visit. An iterative approach avoids shared instance state and makes resetting trivial.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def lowest_absolute_variance(self, root):
        min_diff = float("inf")
        prev = None
        stack, node = [], root
        while stack or node:
            while node:
                stack.append(node)
                node = node.left
            node = stack.pop()
            if prev is not None:
                min_diff = min(min_diff, node.val - prev)
            prev = node.val
            node = node.right
        return min_diff

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
print(Solution().lowest_absolute_variance(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        public int lowestAbsoluteVariance(TreeNode root) {
            int minDiff = Integer.MAX_VALUE;
            Integer prev = null;
            Deque<TreeNode> stack = new ArrayDeque<>();
            TreeNode node = root;
            while (!stack.isEmpty() || node != null) {
                while (node != null) { stack.push(node); node = node.left; }
                node = stack.pop();
                if (prev != null) minDiff = Math.min(minDiff, node.val - prev);
                prev = node.val;
                node = node.right;
            }
            return minDiff;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().lowestAbsoluteVariance(root));
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
