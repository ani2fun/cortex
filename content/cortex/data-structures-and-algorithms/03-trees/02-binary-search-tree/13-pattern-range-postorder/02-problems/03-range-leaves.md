---
title: "Range Leaves"
summary: "Given the root of a BST and a range [low, high], replace the value of each non-leaf in-range node with the count of in-range leaves in its subtree."
prereqs:
  - 13-pattern-range-postorder/01-pattern
difficulty: medium
kind: problem
topics: [range-postorder, binary-search-tree]
---

# Range leaves

## Problem Statement

Given the **root** of a BST and a range `[low, high]`, replace the value of each *non-leaf* in-range node with the count of in-range leaves in its subtree.

> A *leaf* here is a node whose subtree contains no in-range descendants — typically an actual leaf in the original tree.

## Examples

**Example 1:**
```
Input:  root = [4, 2, 5, 1, 3, null, 6], low = 2, high = 5
Output: [1, 1, 0, 1, 3, null, 6]
```

**Example 2:**
```
Input:  root = [5, 1, 8, null, null, 6, 9], low = 6, high = 9
Output: [5, 1, 2, null, null, 6, 9]
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `low ≤ high`
- All node values are unique

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def range_leaves_helper(self, root, low, high):
        # Your code goes here
        return 0

    def range_leaves(self, root, low, high):
        self.range_leaves_helper(root, low, high)
        return root

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

def print_tree(root):                # root → [1, 2, 3, null, 4], trailing nulls trimmed
    out, queue = [], deque([root])
    while queue:
        node = queue.popleft()
        if node is None:
            out.append(None)
        else:
            out.append(node.val)
            queue.append(node.left)
            queue.append(node.right)
    while out and out[-1] is None:
        out.pop()
    print(json.dumps(out))

root = build_tree(json.loads(input()))   # the test case's level-order values
low = int(input())
high = int(input())
print_tree(Solution().range_leaves(root, low, high))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int rangeLeavesHelper(TreeNode root, int low, int high) {
            // Your code goes here
            return 0;
        }

        TreeNode rangeLeaves(TreeNode root, int low, int high) {
            rangeLeavesHelper(root, low, high);
            return root;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int low = Integer.parseInt(sc.nextLine().trim());
        int high = Integer.parseInt(sc.nextLine().trim());
        printTree(new Solution().rangeLeaves(root, low, high));
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

    static void printTree(TreeNode root) {
        List<String> out = new ArrayList<>();
        Deque<TreeNode> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node == null) {
                out.add("null");
            } else {
                out.add(String.valueOf(node.val));
                queue.add(node.left);
                queue.add(node.right);
            }
        }
        while (!out.isEmpty() && out.get(out.size() - 1).equals("null"))
            out.remove(out.size() - 1);
        System.out.println("[" + String.join(", ", out) + "]");
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[4, 2, 5, 1, 3, null, 6]" },
    { "id": "low", "label": "low", "type": "int", "placeholder": "2" },
    { "id": "high", "label": "high", "type": "int", "placeholder": "5" }
  ],
  "cases": [
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]", "low": "2", "high": "5" }, "expected": "[1, 1, 0, 1, 3, null, 6]" },
    { "args": { "root": "[5, 1, 8, null, null, 6, 9]", "low": "6", "high": "9" }, "expected": "[5, 1, 2, null, null, 6, 9]" },
    { "args": { "root": "[5]", "low": "1", "high": "10" }, "expected": "[5]" },
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]", "low": "7", "high": "10" }, "expected": "[4, 2, 5, 1, 3, null, 6]" },
    { "args": { "root": "[4, 2, 6, 1, 3, 5, 7]", "low": "2", "high": "6" }, "expected": "[2, 1, 1, 1, 3, 5, 7]" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

Same skeleton as range summation, but instead of returning the sum of in-range descendants, return the *count of in-range leaves*. A leaf returns `1`; an internal in-range node returns `leftLeaves + rightLeaves` and overwrites its own value with that count.

</details>
<details>
<summary><h2>Solution</h2></summary>

BST pruning handles out-of-range nodes as before. For in-range nodes: if the node is a leaf (no children), it returns `1` — it *is* an in-range leaf. Otherwise, recursively count in-range leaves in both subtrees and overwrite the node's value with the total. The returned count propagates up so each ancestor can include it in its own tally.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def range_leaves_helper(self, root, low, high):
        if root is None:
            return 0
        if root.val < low:
            return self.range_leaves_helper(root.right, low, high)
        if root.val > high:
            return self.range_leaves_helper(root.left, low, high)
        if root.left is None and root.right is None:
            return 1
        left_leaves = self.range_leaves_helper(root.left, low, high)
        right_leaves = self.range_leaves_helper(root.right, low, high)
        root.val = left_leaves + right_leaves
        return root.val

    def range_leaves(self, root, low, high):
        self.range_leaves_helper(root, low, high)
        return root

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

def print_tree(root):                # root → [1, 2, 3, null, 4], trailing nulls trimmed
    out, queue = [], deque([root])
    while queue:
        node = queue.popleft()
        if node is None:
            out.append(None)
        else:
            out.append(node.val)
            queue.append(node.left)
            queue.append(node.right)
    while out and out[-1] is None:
        out.pop()
    print(json.dumps(out))

root = build_tree(json.loads(input()))   # the test case's level-order values
low = int(input())
high = int(input())
print_tree(Solution().range_leaves(root, low, high))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int rangeLeavesHelper(TreeNode root, int low, int high) {
            if (root == null) return 0;
            if (root.val < low) return rangeLeavesHelper(root.right, low, high);
            if (root.val > high) return rangeLeavesHelper(root.left, low, high);
            if (root.left == null && root.right == null) return 1;
            int leftLeaves = rangeLeavesHelper(root.left, low, high);
            int rightLeaves = rangeLeavesHelper(root.right, low, high);
            root.val = leftLeaves + rightLeaves;
            return root.val;
        }

        TreeNode rangeLeaves(TreeNode root, int low, int high) {
            rangeLeavesHelper(root, low, high);
            return root;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int low = Integer.parseInt(sc.nextLine().trim());
        int high = Integer.parseInt(sc.nextLine().trim());
        printTree(new Solution().rangeLeaves(root, low, high));
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

    static void printTree(TreeNode root) {
        List<String> out = new ArrayList<>();
        Deque<TreeNode> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node == null) {
                out.add("null");
            } else {
                out.add(String.valueOf(node.val));
                queue.add(node.left);
                queue.add(node.right);
            }
        }
        while (!out.isEmpty() && out.get(out.size() - 1).equals("null"))
            out.remove(out.size() - 1);
        System.out.println("[" + String.join(", ", out) + "]");
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
