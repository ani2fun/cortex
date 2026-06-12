---
title: "Enriched Sum Tree"
summary: "Given the root of a binary search tree, replace every node's value with the sum of its original value and the values of all nodes greater than it."
prereqs:
  - 12-pattern-reversed-sorted-traversal/01-pattern
difficulty: medium
kind: problem
topics: [reversed-sorted-traversal, binary-search-tree]
---

# Enriched sum tree

## Problem Statement

Given the **root** of a binary search tree, replace every node's value with the sum of its original value and the values of *all nodes greater than it*. The resulting tree is called an **enriched sum tree** (sometimes "greater-tree").

## Examples

**Example 1:**
```
Input:  root = [4, 2, 5, 1, 3, null, 6]
Output: [15, 20, 11, 21, 18, null, 6]
```

**Example 2:**
```
Input:  root = [5, 4, 10, null, null, 9, 11]
Output: [35, 39, 21, null, null, 30, 11]
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- All BST keys are distinct

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def enriched_sum_tree(self, root):
        # Your code goes here — reversed in-order (right → node → left);
        # maintain a running sum of all visited (larger) keys;
        # replace each node.val with (original val + running sum). Return root.
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
print_tree(Solution().enriched_sum_tree(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        TreeNode enrichedSumTree(TreeNode root) {
            // Your code goes here — reversed in-order (right → node → left);
            // maintain a running sum of all visited (larger) keys;
            // replace each node.val with (original val + running sum). Return root.
            return root;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        printTree(new Solution().enrichedSumTree(root));
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

    static void printTree(TreeNode root) {          // root → [1, 2, 3, null, 4], trailing nulls trimmed
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[4, 2, 5, 1, 3, null, 6]" }
  ],
  "cases": [
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]" }, "expected": "[15, 20, 11, 21, 18, null, 6]" },
    { "args": { "root": "[5, 4, 10, null, null, 9, 11]" }, "expected": "[35, 39, 21, null, null, 30, 11]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[7]" }, "expected": "[7]" },
    { "args": { "root": "[3, null, 5]" }, "expected": "[8, null, 5]" },
    { "args": { "root": "[2, 1, 3]" }, "expected": "[5, 6, 3]" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

Reverse in-order visits nodes from largest to smallest. Maintain a running `sum`; at each node:

1. Add the current node's value to `sum`.
2. Overwrite the current node's value with `sum`.

By the time we visit a node, `sum` already contains the total of every strictly larger node we've already passed *plus* the current node — exactly the value the problem asks for.

</details>
<details>
<summary><h2>Solution</h2></summary>

A single reversed in-order pass carries a running `sum`. At each node: add its value to `sum`, then overwrite the node with `sum`. Because we visit in descending order, `sum` is exactly "all keys ≥ this node" when we arrive — no second pass needed.

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
        self.sum = 0

    def _helper(self, root):
        if root is None:
            return
        self._helper(root.right)
        self.sum += root.val
        root.val = self.sum
        self._helper(root.left)

    def enriched_sum_tree(self, root):
        self._helper(root)
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
print_tree(Solution().enriched_sum_tree(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int sum = 0;

        private void helper(TreeNode root) {
            if (root == null) return;
            helper(root.right);
            sum += root.val;
            root.val = sum;
            helper(root.left);
        }

        TreeNode enrichedSumTree(TreeNode root) {
            helper(root);
            return root;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        printTree(new Solution().enrichedSumTree(root));
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

    static void printTree(TreeNode root) {          // root → [1, 2, 3, null, 4], trailing nulls trimmed
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

<details>
<summary><strong>Trace — root = [4, 2, 5, 1, 3, null, 6]</strong></summary>

```
sum = 0, visit order: 6, 5, 4, 3, 2, 1
visit 6 │ sum = 0 + 6 = 6   → node.val = 6
visit 5 │ sum = 6 + 5 = 11  → node.val = 11
visit 4 │ sum = 11 + 4 = 15 → node.val = 15
visit 3 │ sum = 15 + 3 = 18 → node.val = 18
visit 2 │ sum = 18 + 2 = 20 → node.val = 20
visit 1 │ sum = 20 + 1 = 21 → node.val = 21
Result: [15, 20, 11, 21, 18, null, 6] ✓
```

</details>

</details>
