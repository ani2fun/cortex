---
title: "Root-to-Leaf Paths Summing to Target"
summary: "Return all root-to-leaf paths whose node values sum to a given target using backtracking."
prereqs:
  - 14-pattern-root-to-leaf-path-stateful/01-pattern
difficulty: medium
kind: problem
topics: [root-to-leaf-path, binary-tree]
---

# Problem 1 — Root-to-leaf paths summing to target

## Problem Statement

Return *all* root-to-leaf paths whose node values sum to `target`.

The accumulator is *the path so far* (push-pop) plus a *countdown of the target* (passed by value). Each call subtracts the current node's value from `target` before recursing; at a leaf the path qualifies when the leaf's own value equals the remaining `target` — i.e. the whole path summed to the original target. Snapshot the path when that holds.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, null, null, 7], target = 11
Output: [[1, 3, 7]]
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 4], target = 13
Output: []
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `-10⁴ ≤ target ≤ 10⁴`

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def root_to_leaf_paths(self, root, target):
        # Your code goes here — push-pop path discipline, snapshot at leaf when rem == node.val
        return []

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
target = int(input())
print(Solution().root_to_leaf_paths(root, target))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        List<List<Integer>> rootToLeafPaths(TreeNode root, int target) {
            // Your code goes here — push-pop path discipline, snapshot at leaf when rem == node.val
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().rootToLeafPaths(root, target));
    }

    static TreeNode buildTree(Integer[] values) {
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
    { "id": "target", "label": "target", "type": "int", "placeholder": "11" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]", "target": "11" }, "expected": "[[1, 3, 7]]" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 4]", "target": "13" }, "expected": "[]" },
    { "args": { "root": "[]", "target": "0" }, "expected": "[]" },
    { "args": { "root": "[5]", "target": "5" }, "expected": "[[5]]" },
    { "args": { "root": "[5]", "target": "0" }, "expected": "[]" },
    { "args": { "root": "[1, 2, 3]", "target": "3" }, "expected": "[[1, 2]]" },
    { "args": { "root": "[1, 2, 3]", "target": "4" }, "expected": "[[1, 3]]" },
    { "args": { "root": "[1, 2, 2]", "target": "3" }, "expected": "[[1, 2], [1, 2]]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A single top-down recursion carries the path (push-pop) and the running remaining target (passed by value). At each node append to the shared path and subtract from `target`; at a leaf, if the remaining `target` equals the leaf's value the whole path summed to the original target — snapshot a copy. The path argument is never shared across the two recursive calls (the pop undoes each enter), so left and right subtrees can't corrupt each other.

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
        self.path = []

    def root_to_leaf_paths_helper(self, root, target, result):
        if root is None:
            return
        self.path.append(root.val)
        if root.left is None and root.right is None and root.val == target:
            result.append(self.path.copy())
        target -= root.val
        self.root_to_leaf_paths_helper(root.left, target, result)
        self.root_to_leaf_paths_helper(root.right, target, result)
        self.path.pop()

    def root_to_leaf_paths(self, root, target):
        result = []
        self.root_to_leaf_paths_helper(root, target, result)
        return result

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
target = int(input())
print(Solution().root_to_leaf_paths(root, target))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private List<Integer> path = new ArrayList<>();

        private void rootToLeafPathsHelper(TreeNode root, int target, List<List<Integer>> result) {
            if (root == null) return;
            path.add(root.val);
            if (root.left == null && root.right == null && root.val == target)
                result.add(new ArrayList<>(path));
            target -= root.val;
            rootToLeafPathsHelper(root.left, target, result);
            rootToLeafPathsHelper(root.right, target, result);
            path.remove(path.size() - 1);
        }

        List<List<Integer>> rootToLeafPaths(TreeNode root, int target) {
            List<List<Integer>> result = new ArrayList<>();
            rootToLeafPathsHelper(root, target, result);
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().rootToLeafPaths(root, target));
    }

    static TreeNode buildTree(Integer[] values) {
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
