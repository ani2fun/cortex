---
title: "Equal Evens-and-Odds Paths"
summary: "Return all root-to-leaf paths where the count of even-valued nodes equals the count of odd-valued nodes."
prereqs:
  - 14-pattern-root-to-leaf-path-stateful/01-pattern
difficulty: medium
kind: problem
topics: [root-to-leaf-path, binary-tree]
---

# Problem 2 — Equal evens-and-odds paths

## Problem Statement

Return all root-to-leaf paths where the number of even-valued nodes equals the number of odd-valued nodes.

Same shape as Problem 1, but the per-path bookkeeping is *two counters* (`even_count`, `odd_count`) instead of one running sum. At each leaf, snapshot the path if the counts match.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 4]
Output: [[1, 2], [1, 4]]
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 4]
Output: [[1, 8]]
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `0 ≤ node.val ≤ 10⁴`
- A path qualifies when `#even == #odd` at the leaf

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def equal_paths(self, root):
        # Your code goes here — push-pop path, carry even_count/odd_count down as args,
        # snapshot the path at a leaf when even_count == odd_count
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
print(Solution().equal_paths(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        List<List<Integer>> equalPaths(TreeNode root) {
            // Your code goes here — push-pop path, carry evenCount/oddCount down as args,
            // snapshot at a leaf when evenCount == oddCount
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().equalPaths(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 4]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 4]" }, "expected": "[[1, 2], [1, 4]]" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 4]" }, "expected": "[[1, 8]]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[1]" }, "expected": "[]" },
    { "args": { "root": "[2]" }, "expected": "[]" },
    { "args": { "root": "[1, 2]" }, "expected": "[[1, 2]]" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "[[1, 2]]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

The backtracking skeleton is identical to path-sum II, but two counters — `even_count` and `odd_count` — travel down as arguments alongside the shared path. At each node increment the appropriate counter; at a leaf, snapshot the path only when the counts are equal. Because both counters are function arguments, left and right subtrees can't interfere.

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

    def equal_paths_helper(self, root, even_count, odd_count, result):
        if root is None:
            return
        self.path.append(root.val)
        if root.val % 2 == 0:
            even_count += 1
        else:
            odd_count += 1
        if root.left is None and root.right is None:
            if even_count == odd_count:
                result.append(self.path.copy())
        self.equal_paths_helper(root.left, even_count, odd_count, result)
        self.equal_paths_helper(root.right, even_count, odd_count, result)
        self.path.pop()

    def equal_paths(self, root):
        result = []
        self.equal_paths_helper(root, 0, 0, result)
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
print(Solution().equal_paths(root))
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

        private void equalPathsHelper(TreeNode root, int evenCount, int oddCount, List<List<Integer>> result) {
            if (root == null) return;
            path.add(root.val);
            if (root.val % 2 == 0) evenCount++;
            else oddCount++;
            if (root.left == null && root.right == null) {
                if (evenCount == oddCount)
                    result.add(new ArrayList<>(path));
            }
            equalPathsHelper(root.left, evenCount, oddCount, result);
            equalPathsHelper(root.right, evenCount, oddCount, result);
            path.remove(path.size() - 1);
        }

        List<List<Integer>> equalPaths(TreeNode root) {
            List<List<Integer>> result = new ArrayList<>();
            equalPathsHelper(root, 0, 0, result);
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().equalPaths(root));
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
