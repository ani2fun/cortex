---
title: "LCA with Existence Check"
summary: "Find the lowest common ancestor of two nodes identified by value, returning null if either node is missing from the tree."
prereqs:
  - 17-pattern-lowest-common-ancestor/01-pattern
difficulty: medium
kind: problem
topics: [lowest-common-ancestor, binary-tree]
---

# LCA with Existence Check

## Problem Statement

Given the **root** of a binary tree and two integer values `node_a` and `node_b`, return the **value** of their lowest common ancestor. If either node is absent from the tree, return `null`.

The classical LCA algorithm assumes both targets exist. If only one node is present, the algorithm incorrectly returns it as the LCA. The fix: do a separate existence pass for both nodes first, then run LCA only if both exist. This adds one O(N) pre-pass, keeping overall complexity at O(N).

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, null, null, 7], node_a = 4, node_b = 7
Output: 1
```
`4` is in the left subtree, `7` in the right — they split at root `1`.

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 7], node_a = 2, node_b = 9
Output: null
```
`9` does not exist in the tree — return `null`.

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`; all values are **distinct**
- `node_a` and `node_b` may or may not exist in the tree
- O(N) time, O(h) stack

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def node_exists(self, root, val):
        # Your code goes here — return True if a node with this value exists
        return False

    def lowest_common_ancestor(self, root, val_a, val_b):
        # Your code goes here — standard LCA by value
        return None

    def lca_with_check(self, root, val_a, val_b):
        # Your code goes here — verify both exist, then call lowest_common_ancestor
        return None

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
val_a = int(input())                     # first target value
val_b = int(input())                     # second target value
result = Solution().lca_with_check(root, val_a, val_b)
print("null" if result is None else result)
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean nodeExists(TreeNode root, int val) {
            // Your code goes here — return true if a node with this value exists
            return false;
        }

        TreeNode lowestCommonAncestor(TreeNode root, int valA, int valB) {
            // Your code goes here — standard LCA by value
            return null;
        }

        Integer lcaWithCheck(TreeNode root, int valA, int valB) {
            // Your code goes here — verify both exist, then call lowestCommonAncestor
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int valA = Integer.parseInt(sc.nextLine().trim());
        int valB = Integer.parseInt(sc.nextLine().trim());
        Integer result = new Solution().lcaWithCheck(root, valA, valB);
        System.out.println(result == null ? "null" : result);
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, null, null, 7]" },
    { "id": "val_a", "label": "node_a", "type": "int", "placeholder": "4" },
    { "id": "val_b", "label": "node_b", "type": "int", "placeholder": "7" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]", "val_a": "4", "val_b": "7" }, "expected": "1" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]", "val_a": "2", "val_b": "9" }, "expected": "null" },
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]", "val_a": "2", "val_b": "3" }, "expected": "1" },
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]", "val_a": "2", "val_b": "4" }, "expected": "2" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]", "val_a": "2", "val_b": "7" }, "expected": "4" },
    { "args": { "root": "[1]", "val_a": "1", "val_b": "1" }, "expected": "1" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

First check whether both nodes exist using a helper search. If either is missing, return `null` immediately. Only then run the standard LCA by-value recursion: match on `node.val == val_a or val_b`, recurse both sides, return the split node if both children respond, otherwise bubble the single found node up.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def node_exists(self, root, val):
        if root is None:
            return False
        if root.val == val:
            return True
        return self.node_exists(root.left, val) or self.node_exists(root.right, val)

    def lowest_common_ancestor(self, root, val_a, val_b):
        if root is None:
            return None
        if root.val == val_a or root.val == val_b:
            return root
        left_lca = self.lowest_common_ancestor(root.left, val_a, val_b)
        right_lca = self.lowest_common_ancestor(root.right, val_a, val_b)
        if left_lca and right_lca:
            return root
        return left_lca if left_lca else right_lca

    def lca_with_check(self, root, val_a, val_b):
        if not root:
            return None
        if not self.node_exists(root, val_a) or not self.node_exists(root, val_b):
            return None
        result = self.lowest_common_ancestor(root, val_a, val_b)
        return result.val if result is not None else None

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
val_a = int(input())                     # first target value
val_b = int(input())                     # second target value
result = Solution().lca_with_check(root, val_a, val_b)
print("null" if result is None else result)
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean nodeExists(TreeNode root, int val) {
            if (root == null) return false;
            if (root.val == val) return true;
            return nodeExists(root.left, val) || nodeExists(root.right, val);
        }

        TreeNode lowestCommonAncestor(TreeNode root, int valA, int valB) {
            if (root == null) return null;
            if (root.val == valA || root.val == valB) return root;
            TreeNode leftLCA = lowestCommonAncestor(root.left, valA, valB);
            TreeNode rightLCA = lowestCommonAncestor(root.right, valA, valB);
            if (leftLCA != null && rightLCA != null) return root;
            return leftLCA != null ? leftLCA : rightLCA;
        }

        Integer lcaWithCheck(TreeNode root, int valA, int valB) {
            if (root == null) return null;
            if (!nodeExists(root, valA) || !nodeExists(root, valB)) return null;
            TreeNode result = lowestCommonAncestor(root, valA, valB);
            return result != null ? result.val : null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int valA = Integer.parseInt(sc.nextLine().trim());
        int valB = Integer.parseInt(sc.nextLine().trim());
        Integer result = new Solution().lcaWithCheck(root, valA, valB);
        System.out.println(result == null ? "null" : result);
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
