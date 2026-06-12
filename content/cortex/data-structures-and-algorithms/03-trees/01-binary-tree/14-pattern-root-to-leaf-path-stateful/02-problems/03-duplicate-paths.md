---
title: "Duplicate Paths"
summary: "Return all root-to-leaf paths that appear more than once in the tree using backtracking and a path-frequency map."
prereqs:
  - 14-pattern-root-to-leaf-path-stateful/01-pattern
difficulty: medium
kind: problem
topics: [root-to-leaf-path, binary-tree]
---

# Problem 3 — Duplicate paths

## Problem Statement

Return all root-to-leaf paths that appear *more than once* in the tree (i.e. two different leaves produce the same value sequence).

Two ingredients: the push-pop path discipline, plus a **hash map of path-string → count**. At each leaf, serialise the path into a hash-friendly key (e.g. comma-joined string), bump its count, and record the path *exactly once* — when the count first hits 2.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 2]
Output: [[1, 2]]
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 4]
Output: []
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- A path is recorded exactly once (when its count reaches 2)

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def duplicate_paths(self, root):
        # Your code goes here — push-pop path, serialize at each leaf,
        # record the path exactly once (when its count reaches 2)
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
print(Solution().duplicate_paths(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        List<List<Integer>> duplicatePaths(TreeNode root) {
            // Your code goes here — push-pop path, serialize at each leaf,
            // record the path exactly once (when its count reaches 2)
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().duplicatePaths(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 2]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 2]" }, "expected": "[[1, 2]]" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 4]" }, "expected": "[]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[5]" }, "expected": "[]" },
    { "args": { "root": "[1, 1, 1]" }, "expected": "[[1, 1]]" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "[]" },
    { "args": { "root": "[1, 2, 2, 3, null, null, 3]" }, "expected": "[[1, 2, 3]]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

The backtracking skeleton runs as usual. At each leaf the current path is serialized to a comma-joined string and looked up in a shared frequency map. When the count reaches 2 (first duplicate), a copy of the path is recorded. Counts above 2 are ignored — the problem asks for each duplicated path exactly once.

```python solution time=O(n) space=O(n)
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
        self.path_count = {}

    def duplicate_paths_helper(self, root, result):
        if root is None:
            return
        self.path.append(root.val)
        if root.left is None and root.right is None:
            key = ",".join(map(str, self.path))
            self.path_count[key] = self.path_count.get(key, 0) + 1
            if self.path_count[key] == 2:
                result.append(self.path.copy())
        self.duplicate_paths_helper(root.left, result)
        self.duplicate_paths_helper(root.right, result)
        self.path.pop()

    def duplicate_paths(self, root):
        result = []
        self.duplicate_paths_helper(root, result)
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
print(Solution().duplicate_paths(root))
```

```java solution
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private List<Integer> path = new ArrayList<>();
        private Map<String, Integer> pathCount = new HashMap<>();

        private void duplicatePathsHelper(TreeNode root, List<List<Integer>> result) {
            if (root == null) return;
            path.add(root.val);
            if (root.left == null && root.right == null) {
                String key = path.stream().map(Object::toString).collect(Collectors.joining(","));
                pathCount.put(key, pathCount.getOrDefault(key, 0) + 1);
                if (pathCount.get(key) == 2)
                    result.add(new ArrayList<>(path));
            }
            duplicatePathsHelper(root.left, result);
            duplicatePathsHelper(root.right, result);
            path.remove(path.size() - 1);
        }

        List<List<Integer>> duplicatePaths(TreeNode root) {
            List<List<Integer>> result = new ArrayList<>();
            duplicatePathsHelper(root, result);
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().duplicatePaths(root));
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
