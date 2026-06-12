---
title: "LCA of the Deepest Leaves"
summary: "Find the lowest common ancestor of all the deepest leaves in the tree."
prereqs:
  - 17-pattern-lowest-common-ancestor/01-pattern
difficulty: hard
kind: problem
topics: [lowest-common-ancestor, binary-tree]
---

# LCA of the Deepest Leaves

## Problem Statement

Given the **root** of a binary tree, return the **value** of the lowest common ancestor of all its **deepest leaves**. If the tree is empty, return `null`.

Two-pass approach: first find the deepest leaves via level-order traversal; then run the N-node LCA on that set. A more elegant one-pass solution exists using the stateful postorder pattern — return `(deepest depth, LCA so far)` from each subtree and combine at each node — but the two-pass version is clearer.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, 6, null, 7]
Output: 1
```
Deepest leaves are `4`, `6`, and `7` at depth 2; their LCA is root `1`.

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 7]
Output: 4
```
Deepest leaves are `2` and `7` at depth 2; both are children of `4`.

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`; all values are **distinct**
- O(N) time, O(N) space

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def find_deepest_leaves(self, root):
        # Your code goes here — BFS level by level; the last level's nodes are deepest leaves
        return []

    def lca_of_deepest_leaves(self, root):
        # Your code goes here — find deepest leaves, then run N-node LCA on them
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
result = Solution().lca_of_deepest_leaves(root)
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
        List<TreeNode> findDeepestLeaves(TreeNode root) {
            // Your code goes here — BFS level by level; return last level's nodes
            return new ArrayList<>();
        }

        TreeNode lcaOfDeepestLeaves(TreeNode root) {
            // Your code goes here — find deepest leaves, then run N-node LCA
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        TreeNode result = new Solution().lcaOfDeepestLeaves(root);
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
}
```

```testcases
{
  "args": [
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, 6, null, 7]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, 6, null, 7]" }, "expected": "1" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]" }, "expected": "4" },
    { "args": { "root": "[1]" }, "expected": "1" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "3" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "1" },
    { "args": { "root": "[1, 2, 3, 4, 5, null, null]" }, "expected": "2" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Two passes. First, BFS level-by-level to find the deepest leaves: clear the list at the start of each level and fill it with the current level's nodes; after the loop the list holds exactly the deepest level. Second, build a set of those node objects and run the N-node LCA recursion: if `root in node_set`, return root; recurse both children; if both return non-None, this is the split — the LCA; otherwise bubble up. Return the LCA node's value.

```python solution time=O(n) space=O(n)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def find_deepest_leaves(self, root):
        if not root:
            return []
        queue = deque([root])
        deepest_leaves = []
        while queue:
            level_size = len(queue)
            deepest_leaves = []
            for _ in range(level_size):
                node = queue.popleft()
                deepest_leaves.append(node)
                if node.left:
                    queue.append(node.left)
                if node.right:
                    queue.append(node.right)
        return deepest_leaves

    def _lca(self, root, node_set):
        if root is None:
            return None
        if root in node_set:
            return root
        left_lca = self._lca(root.left, node_set)
        right_lca = self._lca(root.right, node_set)
        if left_lca and right_lca:
            return root
        return left_lca if left_lca else right_lca

    def lca_of_deepest_leaves(self, root):
        deepest_leaves = self.find_deepest_leaves(root)
        node_set = set(deepest_leaves)
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
result = Solution().lca_of_deepest_leaves(root)
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
        List<TreeNode> findDeepestLeaves(TreeNode root) {
            List<TreeNode> deepestLeaves = new ArrayList<>();
            if (root == null) return deepestLeaves;
            Queue<TreeNode> queue = new LinkedList<>();
            queue.add(root);
            while (!queue.isEmpty()) {
                int levelSize = queue.size();
                deepestLeaves.clear();
                for (int i = 0; i < levelSize; i++) {
                    TreeNode node = queue.poll();
                    deepestLeaves.add(node);
                    if (node.left != null) queue.add(node.left);
                    if (node.right != null) queue.add(node.right);
                }
            }
            return deepestLeaves;
        }

        private TreeNode _lca(TreeNode root, Set<TreeNode> nodeSet) {
            if (root == null) return null;
            if (nodeSet.contains(root)) return root;
            TreeNode leftLCA = _lca(root.left, nodeSet);
            TreeNode rightLCA = _lca(root.right, nodeSet);
            if (leftLCA != null && rightLCA != null) return root;
            return leftLCA != null ? leftLCA : rightLCA;
        }

        TreeNode lcaOfDeepestLeaves(TreeNode root) {
            List<TreeNode> deepestLeaves = findDeepestLeaves(root);
            Set<TreeNode> nodeSet = new HashSet<>(deepestLeaves);
            return _lca(root, nodeSet);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        TreeNode result = new Solution().lcaOfDeepestLeaves(root);
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
}
```

</details>
