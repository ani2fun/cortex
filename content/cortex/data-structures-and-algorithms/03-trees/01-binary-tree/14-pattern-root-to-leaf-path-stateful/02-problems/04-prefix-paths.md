---
title: "Prefix Paths"
summary: "Return all root-to-leaf paths whose total sum equals the sum of some non-empty prefix of the same path using a prefix-sum frequency map."
prereqs:
  - 14-pattern-root-to-leaf-path-stateful/01-pattern
difficulty: hard
kind: problem
topics: [root-to-leaf-path, binary-tree]
---

# Problem 4 — Prefix paths

## Problem Statement

Return all root-to-leaf paths whose *total sum* equals the sum of some non-empty *prefix* of the same path.

**Example:** path `[1, -3, 3]` has total sum 1 — and the prefix `[1]` also has sum 1. So this path qualifies.

Combine the path discipline with a **prefix-sum frequency map**. As we descend, increment the count of the running prefix-sum at the current depth. At a leaf, if the running sum has been seen *more than once* (count > 1), it means a strictly earlier prefix of the path had the same sum — qualifying the path.

## Examples

**Example 1:**
```
Input:  root = [1, -3, null, null, 3]
Output: [[1, -3, 3]]
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 4]
Output: []
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- A path qualifies when its total sum matches some *strictly earlier* prefix sum (count > 1 at the leaf)

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def prefix_paths(self, root):
        # Your code goes here — push-pop path + prefix-sum frequency map;
        # at a leaf, qualify when prefix_sum_count[path_sum] > 1
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
print(Solution().prefix_paths(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        List<List<Integer>> prefixPaths(TreeNode root) {
            // Your code goes here — push-pop path + prefix-sum frequency map;
            // at a leaf, qualify when prefixSumCount.get(pathSum) > 1
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().prefixPaths(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, -3, null, null, 3]" }
  ],
  "cases": [
    { "args": { "root": "[1, -3, null, null, 3]" }, "expected": "[[1, -3, 3]]" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 4]" }, "expected": "[]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[5]" }, "expected": "[]" },
    { "args": { "root": "[0, 0]" }, "expected": "[[0, 0]]" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "[]" },
    { "args": { "root": "[2, 2, null, null, -2]" }, "expected": "[[2, 2, -2]]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

As we descend, maintain a running `path_sum` and increment `prefix_sum_count[path_sum]` on the way in; at a leaf, the path qualifies when `prefix_sum_count[path_sum] > 1` — meaning the same running sum appeared at a strictly earlier node (i.e. the leaf's total sum is the same as some proper prefix sum). Backtrack by decrementing the count on the way out and popping the path.

```python solution time=O(n) space=O(n)
import json
from collections import deque, defaultdict

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def __init__(self):
        self.path = []
        self.prefix_sum_count = defaultdict(int)

    def prefix_paths_helper(self, root, path_sum, result):
        if root is None:
            return
        self.path.append(root.val)
        path_sum += root.val
        self.prefix_sum_count[path_sum] += 1
        if root.left is None and root.right is None:
            if self.prefix_sum_count[path_sum] > 1:
                result.append(self.path.copy())
        self.prefix_paths_helper(root.left, path_sum, result)
        self.prefix_paths_helper(root.right, path_sum, result)
        self.prefix_sum_count[path_sum] -= 1
        self.path.pop()

    def prefix_paths(self, root):
        result = []
        self.prefix_paths_helper(root, 0, result)
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
print(Solution().prefix_paths(root))
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
        private Map<Integer, Integer> prefixSumCount = new HashMap<>();

        private void prefixPathsHelper(TreeNode root, int pathSum, List<List<Integer>> result) {
            if (root == null) return;
            path.add(root.val);
            pathSum += root.val;
            prefixSumCount.put(pathSum, prefixSumCount.getOrDefault(pathSum, 0) + 1);
            if (root.left == null && root.right == null) {
                if (prefixSumCount.get(pathSum) > 1)
                    result.add(new ArrayList<>(path));
            }
            prefixPathsHelper(root.left, pathSum, result);
            prefixPathsHelper(root.right, pathSum, result);
            prefixSumCount.put(pathSum, prefixSumCount.get(pathSum) - 1);
            path.remove(path.size() - 1);
        }

        List<List<Integer>> prefixPaths(TreeNode root) {
            List<List<Integer>> result = new ArrayList<>();
            prefixPathsHelper(root, 0, result);
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().prefixPaths(root));
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
<details>
<summary><h2>Key Takeaway</h2></summary>

The stateful root-to-leaf path pattern is the natural sibling of stateless preorder backtracking. Three things to walk away with:

1. **Push-pop is sacred — and the leaf needs a *copy*.** The shared `path` is being mutated; if you record a reference to it and then return, the path you stored will get clobbered as the recursion backs out. Always copy on extract — `path.copy()`, `new ArrayList<>(path)`, `[...path]`, `path.clone()` — never store the live reference.
2. **Auxiliary data per problem.** Sum target → running integer. Equal evens-and-odds → two counters. Duplicate paths → hash map of serialised paths. Prefix paths → hash map of running prefix sums. The path itself is the canonical accumulator; the per-problem aux is what *interprets* the path.
3. **Returning paths is expensive even when the algorithm is cheap.** Recording matched paths is O(L) per match. If you're collecting *every* path, total output size is O(N · L) — that's irreducible. The recursion stays O(N) but the output dominates the cost.

> *Coming up — the chapter shifts from depth-first patterns to **level-order** patterns. The next two lessons cover BFS-based tree problems: per-level aggregations, deepest-leaf computations, completeness checks, zigzag traversal, cousin checks, and column-based traversals (top view, bottom view, vertical, diagonal). The queue from chapter 6 finally takes centre stage.*

</details>
