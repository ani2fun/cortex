---
title: "Second Minimum"
summary: "Given the root of a binary tree, find and return the second-smallest distinct value. If there's no second minimum, return -1."
prereqs:
  - 10-pattern-preorder-traversal-stateful/01-pattern
difficulty: medium
kind: problem
topics: [preorder-traversal, binary-tree]
---

# Problem 2 — Second minimum

## Problem Statement

Given the root of a binary tree, find and return the second-smallest distinct value. If there's no second minimum, return `-1`.

This is the **monotone witnesses** flavour. The state is two integers, `min` and `secondMin`, both shared across the recursion. Each visit either improves `min` (and demotes the old min to `secondMin`) or improves `secondMin`. No push/pop needed — once we've seen a smaller value, that's a global fact, not a path-local one.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 5, 7, null, null, 3]
Output: 2
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 9, 7]
Output: 4
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- Return `-1` if no second-smallest distinct value exists
- `O(n)` time, `O(h)` space

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def find_second_minimum(self, root):
        # Your code goes here — track the two smallest distinct values seen so far.
        # Initialize minimum to root.val and second_minimum to -1.
        # At each node: update min/secondMin, then recurse into both children.
        return -1

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

root = build_tree(json.loads(input()))
print(Solution().find_second_minimum(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int findSecondMinimum(TreeNode root) {
            // Your code goes here — track the two smallest distinct values seen so far.
            // Initialize minimum to root.val and secondMinimum to -1.
            // At each node: update min/secondMin, then recurse into both children.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().findSecondMinimum(root));
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
    { "args": { "root": "[1, 2, 5, 7, null, null, 3]" }, "expected": "2" },
    { "args": { "root": "[1, 8, 4, null, null, 9, 7]" }, "expected": "4" },
    { "args": { "root": "[]" }, "expected": "-1" },
    { "args": { "root": "[5]" }, "expected": "-1" },
    { "args": { "root": "[5, 5, 5]" }, "expected": "-1" },
    { "args": { "root": "[1, 2]" }, "expected": "2" },
    { "args": { "root": "[3, 1, 4, 1, 5, 9, 2]" }, "expected": "2" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]" }, "expected": "2" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Initialize `minimum` to the root's value and `second_minimum` to `-1`. At each node: if its value is strictly less than `minimum`, demote `minimum` to `second_minimum` and update `minimum`; otherwise if its value is strictly greater than `minimum` and less than `second_minimum` (or `second_minimum` is still `-1`), update `second_minimum`. Then recurse into both children. No backtracking needed — these are global monotone witnesses.

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
        self.minimum = None
        self.second_minimum = -1

    def helper(self, root):
        # Base case: if the root is None, return
        if root is None:
            return
        # Check if the value of the current node is less than the current minimum
        if root.val < self.minimum:
            # Update the second minimum to the previous minimum
            self.second_minimum = self.minimum
            # Update the minimum to the value of the current node
            self.minimum = root.val
        elif root.val > self.minimum and (
            root.val < self.second_minimum or self.second_minimum == -1
        ):
            # Update the second minimum to the value of the current node
            self.second_minimum = root.val
        # Recursively traverse the left and right subtrees
        self.helper(root.left)
        self.helper(root.right)

    def find_second_minimum(self, root):
        # Check if the root is None, return -1 as no second minimum exists
        if root is None:
            return -1
        # Initialize the minimum to the value of the root node
        self.minimum = root.val
        # Initialize the second minimum to -1, indicating it has not been set yet
        self.second_minimum = -1
        # Call the helper function to find the minimum and second minimum values
        self.helper(root)
        # Return the second minimum value found
        return self.second_minimum

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

root = build_tree(json.loads(input()))
print(Solution().find_second_minimum(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        // Global variables to store minimum and second minimum values
        private int minimum;
        private int secondMinimum;

        private void helper(TreeNode root) {
            // Base case: if the root is null, return
            if (root == null) return;
            // Check if the value of the current node is less than the current minimum
            if (root.val < minimum) {
                // Update the second minimum to the previous minimum
                secondMinimum = minimum;
                // Update the minimum to the value of the current node
                minimum = root.val;
            } else if (
                root.val > minimum &&
                (root.val < secondMinimum || secondMinimum == -1)
            ) {
                // Update the second minimum to the value of the current node
                secondMinimum = root.val;
            }
            // Recursively traverse the left and right subtrees
            helper(root.left);
            helper(root.right);
        }

        int findSecondMinimum(TreeNode root) {
            // Check if the root is null, return -1 as no second minimum exists
            if (root == null) return -1;
            // Initialize the minimum to the value of the root node
            minimum = root.val;
            // Initialize the second minimum to -1
            secondMinimum = -1;
            // Call the helper function
            helper(root);
            // Return the second minimum value found
            return secondMinimum;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().findSecondMinimum(root));
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
