---
title: "Longest Monotonic Path"
summary: "Find the length (in edges) of the longest path where every node has the same value."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: hard
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Longest Monotonic Path

## Problem Statement

Given the **root** of a binary tree, return the length (in **edges**) of the longest path where every node in the path has the **same value**. The path may pass through any node.

Same shape as diameter, with one twist: the height contribution from a child only counts if the child has the same value as the current node.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 5, 7, null, null, 3]
Output: 0
```
No two adjacent nodes share the same value.

**Example 2:**
```
Input:  root = [3, 8, 1, 8, null, 1, 1]
Output: 2
```
The path `1 → 1 → 1` (right subtree, length 2 edges).

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
        self.max_length = 0

    def longest_monotonic_path(self, root):
        # Your code goes here — postorder: compute leftArrow/rightArrow (extend only if child.val == node.val);
        # update max_length with leftArrow + rightArrow; return max(leftArrow, rightArrow).
        def helper(node):
            return 0
        helper(root)
        return self.max_length

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
print(Solution().longest_monotonic_path(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int maxLength = 0;

        private int helper(TreeNode root) {
            // Your code goes here — postorder: compute leftArrow/rightArrow;
            // update maxLength with leftArrow + rightArrow; return max(leftArrow, rightArrow).
            return 0;
        }

        int longestMonotonicPath(TreeNode root) {
            maxLength = 0;
            helper(root);
            return maxLength;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().longestMonotonicPath(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[3, 8, 1, 8, null, 1, 1]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 5, 7, null, null, 3]" }, "expected": "0" },
    { "args": { "root": "[3, 8, 1, 8, null, 1, 1]" }, "expected": "2" },
    { "args": { "root": "[]" }, "expected": "0" },
    { "args": { "root": "[1]" }, "expected": "0" },
    { "args": { "root": "[5, 5, 5]" }, "expected": "2" },
    { "args": { "root": "[5, 5, null, 5]" }, "expected": "2" },
    { "args": { "root": "[1, 1, 1, 1, 1, 1, 1]" }, "expected": "4" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "0" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Exactly the diameter shape, but `leftArrow` only extends if `root.left.val == root.val` (same for right). The through-node candidate is `leftArrow + rightArrow`, tracked globally. The return value is `max(leftArrow, rightArrow)`.

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
        self.max_length = 0

    def longest_monotonic_path_helper(self, root):
        if not root:
            return 0

        # Recursively calculate the longest univalued path in the left
        # subtree
        left_length = self.longest_monotonic_path_helper(root.left)

        # Recursively calculate the longest univalued path in the right
        # subtree
        right_length = self.longest_monotonic_path_helper(root.right)

        left_arrow = 0
        right_arrow = 0

        # If the left child exists and has the same value as the current
        # node, extend the path to the left
        if root.left and root.left.val == root.val:
            left_arrow = left_length + 1

        # If the right child exists and has the same value as the current
        # node, extend the path to the right
        if root.right and root.right.val == root.val:
            right_arrow = right_length + 1

        # Update the max_length if the combined path length is greater
        self.max_length = max(self.max_length, left_arrow + right_arrow)

        # Return the longest univalued path from the current node
        return max(left_arrow, right_arrow)

    def longest_monotonic_path(self, root):
        self.longest_monotonic_path_helper(root)
        return self.max_length

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
print(Solution().longest_monotonic_path(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int maxLength = 0;

        private int longestMonotonicPathHelper(TreeNode root) {
            if (root == null) {
                return 0;
            }

            // Recursively calculate the longest univalued path in the left
            // subtree
            int leftLength = longestMonotonicPathHelper(root.left);

            // Recursively calculate the longest univalued path in the right
            // subtree
            int rightLength = longestMonotonicPathHelper(root.right);

            int leftArrow = 0;
            int rightArrow = 0;

            // If the left child exists and has the same value as the current
            // node, extend the path to the left
            if (root.left != null && root.left.val == root.val) {
                leftArrow = leftLength + 1;
            }

            // If the right child exists and has the same value as the
            // current node, extend the path to the right
            if (root.right != null && root.right.val == root.val) {
                rightArrow = rightLength + 1;
            }

            // Update the maxLength if the combined path length is greater
            maxLength = Math.max(maxLength, leftArrow + rightArrow);

            // Return the longest univalued path from the current node
            return Math.max(leftArrow, rightArrow);
        }

        int longestMonotonicPath(TreeNode root) {
            maxLength = 0;
            longestMonotonicPathHelper(root);
            return maxLength;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().longestMonotonicPath(root));
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
