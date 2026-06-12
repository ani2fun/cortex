---
title: "LCA of N Random Nodes"
summary: "Find the lowest common ancestor of an arbitrary list of nodes, all guaranteed to exist in the tree."
prereqs:
  - 17-pattern-lowest-common-ancestor/01-pattern
difficulty: medium
kind: problem
topics: [lowest-common-ancestor, binary-tree]
---

# LCA of N Random Nodes

## Problem Statement

Given the **root** of a binary tree and a list of node **values** (all guaranteed to exist in the tree), return the **value** of the lowest common ancestor of all nodes in the list.

Generalise the two-node LCA: instead of "is this node `A` or `B`?", check "is this node's value *in the target set*?". Use a hash set for O(1) lookup. The combine logic stays exactly the same.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, null, null, 7], nodes = [2, 4, 7]
Output: 1
```
`2` and `4` are in the left subtree, `7` in the right — they split at root `1`.

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 7], nodes = [2, 7]
Output: 4
```
Both `2` and `7` are children of `4`.

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`; all values are **distinct**
- `1 ≤ len(nodes) ≤ number of nodes`; all listed values exist in the tree
- O(N) time, O(N) space (hash set)

```python run viz=binary-tree viz-root=root
import json, ast
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def lca_n_nodes(self, root, node_vals):
        # Your code goes here — build a set from node_vals, then run LCA:
        # if node.val in the set, return node; recurse both sides;
        # if both non-None, this is the LCA; else bubble up.
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
node_vals = ast.literal_eval(input())    # list of target node values
result = Solution().lca_n_nodes(root, node_vals)
print(result.val if result is not None else "null")
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        TreeNode lcaNNodes(TreeNode root, Set<Integer> nodeVals) {
            // Your code goes here — if nodeVals.contains(root.val), return root;
            // recurse both sides; if both non-null, this is the LCA; else bubble up.
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int[] vals = parseIntArray(sc.nextLine());
        Set<Integer> nodeVals = new HashSet<>();
        for (int v : vals) nodeVals.add(v);
        TreeNode result = new Solution().lcaNNodes(root, nodeVals);
        System.out.println(result == null ? "null" : result.val);
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

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i].trim());
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, null, null, 7]" },
    { "id": "node_vals", "label": "nodes", "type": "int[]", "placeholder": "[2, 4, 7]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]", "node_vals": "[2, 4, 7]" }, "expected": "1" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]", "node_vals": "[2, 7]" }, "expected": "4" },
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]", "node_vals": "[4]" }, "expected": "4" },
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]", "node_vals": "[4, 7]" }, "expected": "1" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]", "node_vals": "[8, 2, 7]" }, "expected": "1" },
    { "args": { "root": "[1, 2, 3]", "node_vals": "[2, 3]" }, "expected": "1" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Build a hash set from the list of target values. Run a postorder recursion identical to the two-node LCA but using `root.val in node_vals` for the base-case check. The combine logic is unchanged: if both children return a node, the current node is the split point — the LCA; otherwise bubble up whichever side found something.

```python solution time=O(n) space=O(n)
import json, ast
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def _lca(self, root, node_vals):
        if root is None:
            return None
        if root.val in node_vals:
            return root
        left_lca = self._lca(root.left, node_vals)
        right_lca = self._lca(root.right, node_vals)
        if left_lca and right_lca:
            return root
        return left_lca if left_lca else right_lca

    def lca_n_nodes(self, root, node_vals):
        node_set = set(node_vals)
        return self._lca(root, node_set)

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
node_vals = ast.literal_eval(input())    # list of target node values
result = Solution().lca_n_nodes(root, node_vals)
print(result.val if result is not None else "null")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private TreeNode _lca(TreeNode root, Set<Integer> nodeVals) {
            if (root == null) return null;
            if (nodeVals.contains(root.val)) return root;
            TreeNode leftLCA = _lca(root.left, nodeVals);
            TreeNode rightLCA = _lca(root.right, nodeVals);
            if (leftLCA != null && rightLCA != null) return root;
            return leftLCA != null ? leftLCA : rightLCA;
        }

        TreeNode lcaNNodes(TreeNode root, Set<Integer> nodeVals) {
            return _lca(root, nodeVals);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int[] vals = parseIntArray(sc.nextLine());
        Set<Integer> nodeVals = new HashSet<>();
        for (int v : vals) nodeVals.add(v);
        TreeNode result = new Solution().lcaNNodes(root, nodeVals);
        System.out.println(result == null ? "null" : result.val);
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

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i].trim());
        return out;
    }
}
```

</details>
