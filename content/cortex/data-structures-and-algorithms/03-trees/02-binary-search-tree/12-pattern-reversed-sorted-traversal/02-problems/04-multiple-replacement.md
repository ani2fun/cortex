---
title: "Multiple Replacement"
summary: "Given the root of a binary search tree, replace each node's value with 0 if the next-larger value in sorted order is a non-zero multiple of its own value."
prereqs:
  - 12-pattern-reversed-sorted-traversal/01-pattern
difficulty: hard
kind: problem
topics: [reversed-sorted-traversal, binary-search-tree]
---

# Multiple replacement

## Problem Statement

Given the **root** of a binary search tree, replace each node's value with `0` if its **inorder predecessor's** value (the value just *larger* than it in sorted order) is a non-zero multiple of its own value.

## Examples

**Example 1:**
```
Input:  root = [6, 2, 5, 1, 4, null, 10]
Output: [6, 0, 0, 0, 4, null, 10]
```

**Example 2:**
```
Input:  root = [5, 4, 10, null, null, 9, 11]
Output: [5, 4, 10, null, null, 9, 11]
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
    def multiple_replacement(self, root):
        # Your code goes here — reversed in-order (right → node → left);
        # track the previous-visited (next-larger) node's ORIGINAL value;
        # if prev_val % node.val == 0 and prev_val != 0, zero this node.
        # Return root.
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
print_tree(Solution().multiple_replacement(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        TreeNode multipleReplacement(TreeNode root) {
            // Your code goes here — reversed in-order (right → node → left);
            // track the previous-visited (next-larger) node's ORIGINAL value;
            // if prev_val % node.val == 0 and prev_val != 0, zero this node.
            // Return root.
            return root;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        printTree(new Solution().multipleReplacement(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[6, 2, 5, 1, 4, null, 10]" }
  ],
  "cases": [
    { "args": { "root": "[6, 2, 5, 1, 4, null, 10]" }, "expected": "[6, 0, 0, 0, 4, null, 10]" },
    { "args": { "root": "[5, 4, 10, null, null, 9, 11]" }, "expected": "[5, 4, 10, null, null, 9, 11]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[7]" }, "expected": "[7]" },
    { "args": { "root": "[2, null, 4]" }, "expected": "[0, null, 4]" },
    { "args": { "root": "[3, null, 5]" }, "expected": "[3, null, 5]" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

The trick word is **predecessor**. Inside a reverse-in-order walk, each node's *previous-visited* node is the next-larger value in sorted order — exactly the "successor's value" the problem asks about (the problem statement's wording is slightly confusing, but the example outputs confirm: we compare each node to the value *larger* than it).

So:

- Maintain a `prev_val` that holds the most recently visited (i.e. larger) node's *original* value.
- At each node, if `prev_val % current.val == 0` and `prev_val != 0`, set the current node to `0`.
- *Then* update `prev_val` to the current node's original value (not the possibly-zeroed one) before recursing left.

The "save the original first" detail is the trap that catches careless implementations.

</details>
<details>
<summary><h2>Solution</h2></summary>

A reversed in-order traversal carries `prev_node_val` (the original value of the most-recently-visited, next-larger node). At each node: capture the original value first, then conditionally zero the node, then update `prev_node_val` with the original (not the zeroed value) before recursing left.

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
        self.prev_node_val = 0
        self.has_prev_node = False

    def _helper(self, root):
        if root is None:
            return
        self._helper(root.right)
        original_val = root.val
        if (self.has_prev_node and self.prev_node_val != 0
                and self.prev_node_val % root.val == 0):
            root.val = 0
        self.prev_node_val = original_val
        self.has_prev_node = True
        self._helper(root.left)

    def multiple_replacement(self, root):
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
print_tree(Solution().multiple_replacement(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int prevNodeVal;
        private boolean hasPrevNode = false;

        private void helper(TreeNode root) {
            if (root == null) return;
            helper(root.right);
            int originalVal = root.val;
            if (hasPrevNode && prevNodeVal != 0 && prevNodeVal % root.val == 0) {
                root.val = 0;
            }
            prevNodeVal = originalVal;
            hasPrevNode = true;
            helper(root.left);
        }

        TreeNode multipleReplacement(TreeNode root) {
            helper(root);
            return root;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        printTree(new Solution().multipleReplacement(root));
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
<summary><h2>Key Takeaway</h2></summary>

Reverse in-order is the descending dual of in-order. Whenever the problem cares about *the largest values first* — k-th largest, ranks from the top, suffix sums by value, comparisons against the next-larger node — flip the recursion direction and the same template applies. Most reverse-sorted-traversal problems are *easy* because the BST has done the sorting for you.

Three patterns to keep:

1. **"Carry the previous-larger value"** — mirror of lesson 10's "carry the previous-smaller". Useful for any pairwise check that runs against the next-larger value (multiples, ratios, ranges, monotonicity).
2. **"Running total over the descending sequence"** — solves *enriched sum tree*, but the same shape solves "sum of values strictly greater than X", "convert to suffix-sum array", "decorate node with `(num greater, sum greater)`".
3. **"Save original before overwriting"** — the trap in `multiple replacement`. When a traversal both *reads* and *writes* the same field, capture the read into a local before the write — your future self will thank you.

The next lesson introduces a new pattern that breaks the "always traverse all nodes" pattern from the last two lessons: **range postorder**, where the BST property lets us *prune* entire subtrees that can't contribute to the answer.

</details>

</details>
