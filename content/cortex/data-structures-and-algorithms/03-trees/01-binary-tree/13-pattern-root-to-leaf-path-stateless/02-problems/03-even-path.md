---
title: "Even Path"
summary: "Return true if there is at least one root-to-leaf path where every node value is even."
prereqs:
  - 13-pattern-root-to-leaf-path-stateless/01-pattern
difficulty: medium
kind: problem
topics: [root-to-leaf-path, binary-tree]
---

# Problem 3 — Even path

## Problem Statement

Return `true` if there is at least one root-to-leaf path where **every** node value is even.

The accumulator is a boolean: "has the path so far been all-even?" Update at each node: `still_even = previously_even AND (current is even)`. At a leaf, return `still_even`. Combine with OR.

## Examples

**Example 1:**
```
Input:  root = [2, 4, 6, 8, null, null, 9]
Output: true
```
Path `2→4→8` is all-even.

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 7]
Output: false
```
The root is odd, so no root-to-leaf path can be all-even.

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
    def even_path(self, root):
        # Your code goes here — carry a boolean "still even so far" DOWN as an argument;
        # update at each node: still_even = still_even AND (node.val % 2 == 0);
        # at a leaf return still_even; combine children with OR.
        return False

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
print("true" if Solution().even_path(root) else "false")
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean evenPath(TreeNode root) {
            // Your code goes here — carry a boolean "still even so far" DOWN as an argument;
            // update at each node: stillEven = stillEven && (node.val % 2 == 0);
            // at a leaf return stillEven; combine children with ||.
            return false;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().evenPath(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[2, 4, 6, 8, null, null, 9]" }
  ],
  "cases": [
    { "args": { "root": "[2, 4, 6, 8, null, null, 9]" }, "expected": "true" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]" }, "expected": "false" },
    { "args": { "root": "[]" }, "expected": "false" },
    { "args": { "root": "[2]" }, "expected": "true" },
    { "args": { "root": "[1]" }, "expected": "false" },
    { "args": { "root": "[2, 2, 2]" }, "expected": "true" },
    { "args": { "root": "[2, 3, 4]" }, "expected": "true" },
    { "args": { "root": "[2, 3, 5]" }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A top-down recursion carries a `still_even` boolean as an argument. At each node: `still_even = still_even AND (node.val % 2 == 0)`. At a leaf, return `still_even` — that path is valid only if every node including this one was even. At an internal node, return the OR of both children. The base case (`None`) returns `False` — a missing child does not constitute a path.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def even_path_helper(self, root, even_so_far):
        if root is None:
            return False
        current_status = even_so_far and (root.val % 2 == 0)
        if root.left is None and root.right is None:
            return current_status
        left_path = self.even_path_helper(root.left, current_status)
        right_path = self.even_path_helper(root.right, current_status)
        return left_path or right_path

    def even_path(self, root):
        if root is None:
            return False
        return self.even_path_helper(root, True)

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
print("true" if Solution().even_path(root) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private boolean evenPathHelper(TreeNode root, boolean evenSoFar) {
            if (root == null) return false;
            boolean currentStatus = evenSoFar && (root.val % 2 == 0);
            if (root.left == null && root.right == null) return currentStatus;
            boolean leftPath = evenPathHelper(root.left, currentStatus);
            boolean rightPath = evenPathHelper(root.right, currentStatus);
            return leftPath || rightPath;
        }

        public boolean evenPath(TreeNode root) {
            if (root == null) return false;
            return evenPathHelper(root, true);
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().evenPath(root));
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
