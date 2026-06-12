---
title: "Descendants Sum Count"
summary: "Count nodes whose value equals the sum of all values in their subtree below them (not including themselves)."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: medium
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Descendants Sum Count

## Problem Statement

Given the **root** of a binary tree, count the number of nodes whose value equals the sum of all values in their subtree **below** them (not including the node itself).

Each subtree returns its sum (so the parent can compute its own); along the way, each call increments a global counter if `node.val == left_sum + right_sum`.

## Examples

**Example 1:**
```
Input:  root = [21, 7, 3, 5, 2, null, 4]
Output: 2
```
Node `21`: `7 + 3 + 5 + 2 + 4 = 21` ✓. Node `3`: `4 = 3`? No. Node `7`: `5 + 2 = 7` ✓. Count = 2.

**Example 2:**
```
Input:  root = [5, 7, 3, 1, 2, null, 3]
Output: 1
```
Node `7`: `1 + 2 = 3` ≠ 7. Node `3`: `3 = 3` ✓. Count = 1.

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`

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
        self.count = 0

    def descendants_sum_count(self, root):
        # Your code goes here — postorder: return the subtree sum including this node;
        # increment self.count if node.val == left_sum + right_sum.
        def compute_sum(node):
            return 0
        compute_sum(root)
        return self.count

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
print(Solution().descendants_sum_count(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int count = 0;

        private int computeSum(TreeNode node) {
            // Your code goes here — postorder: return the subtree sum including this node;
            // increment count if node.val == leftSum + rightSum.
            return 0;
        }

        int descendantsSumCount(TreeNode root) {
            computeSum(root);
            return count;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().descendantsSumCount(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[21, 7, 3, 5, 2, null, 4]" }
  ],
  "cases": [
    { "args": { "root": "[21, 7, 3, 5, 2, null, 4]" }, "expected": "2" },
    { "args": { "root": "[5, 7, 3, 1, 2, null, 3]" }, "expected": "1" },
    { "args": { "root": "[]" }, "expected": "0" },
    { "args": { "root": "[0]" }, "expected": "1" },
    { "args": { "root": "[1]" }, "expected": "0" },
    { "args": { "root": "[3, 1, 2]" }, "expected": "1" },
    { "args": { "root": "[6, 3, 3, 1, 2, 1, 2]" }, "expected": "2" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Each subtree call returns its total sum (including itself), so the parent can check whether its value equals the sum from below. The counter is a shared accumulator — the stateful postorder shape.

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
        self.count = 0

    def compute_sum(self, root):
        # Base case: If the current node is NULL, return 0
        if not root:
            return 0

        # Recursively compute the sum of the left and right subtrees
        left_sum = self.compute_sum(root.left)
        right_sum = self.compute_sum(root.right)

        # If the value of the current node is equal to the sum of its
        # descendants, increment the count
        if root.val == left_sum + right_sum:
            self.count += 1

        # Return the sum of the current subtree, including the value
        # of the current node
        return left_sum + right_sum + root.val

    def descendants_sum_count(self, root):
        self.compute_sum(root)
        return self.count

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
print(Solution().descendants_sum_count(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int count = 0;

        private int computeSum(TreeNode root) {
            // Base case: If the current node is NULL, return 0
            if (root == null) {
                return 0;
            }

            // Recursively compute the sum of the left and right subtrees
            int leftSum = computeSum(root.left);
            int rightSum = computeSum(root.right);

            // If the value of the current node is equal to the sum of its
            // descendants, increment the count
            if (root.val == leftSum + rightSum) {
                count++;
            }

            // Return the sum of the current subtree, including the value
            // of the current node
            return leftSum + rightSum + root.val;
        }

        int descendantsSumCount(TreeNode root) {
            computeSum(root);
            return count;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().descendantsSumCount(root));
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
