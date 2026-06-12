---
title: "Merge Trees"
summary: "Overlay two trees: at every position where both trees have a node, sum the values. Where only one has a node, keep that node. Return the merged tree."
prereqs:
  - 18-pattern-simultaneous-traversal/01-pattern
difficulty: medium
kind: problem
topics: [simultaneous-traversal, binary-tree]
---

# Problem 4 — Merge trees

## Problem Statement

Overlay two trees. At every position where both trees have a node, sum the values. Where only one has a node, keep that node. Return the merged tree.

A *constructive* simultaneous traversal — instead of returning a verdict, return a *new node* built from the two inputs at each step.

## Examples

**Example 1:**
```
Input:  rootA = [1, 3, 2, 5], rootB = [2, 1, 3, null, 4, null, 7]
Output: [3, 4, 5, 5, 4, null, 7]
```

**Example 2:**
```
Input:  rootA = [1, 2, 3, 4, null, 5, 6], rootB = [7, 8, 9, 10, 11, null, 12]
Output: [8, 10, 12, 14, 11, 5, 18]
```

## Constraints

- `0 ≤ number of nodes in each tree ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`

```python run viz=binary-tree viz-root=rootA
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def merge_trees(self, rootA, rootB):
        # Your code goes here — base cases: if either is None, return the other.
        # Otherwise create a new node with rootA.val + rootB.val and recurse same-side.
        return rootA

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
    print(json.dumps(out))           # json.dumps → null, not None

rootA = build_tree(json.loads(input()))
rootB = build_tree(json.loads(input()))
print_tree(Solution().merge_trees(rootA, rootB))
```

```java run viz=binary-tree viz-root=rootA
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        public TreeNode mergeTrees(TreeNode rootA, TreeNode rootB) {
            // Your code goes here — base cases: if either is null, return the other.
            // Otherwise create a new node with rootA.val + rootB.val and recurse same-side.
            return rootA;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode rootA = buildTree(parseIntegerArray(sc.nextLine()));
        TreeNode rootB = buildTree(parseIntegerArray(sc.nextLine()));
        printTree(new Solution().mergeTrees(rootA, rootB));
    }

    static TreeNode buildTree(Integer[] values) {   // [1, 2, 3, null, 4] level-order → root
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> queue = new ArrayDeque<>();    // build queue: only real nodes, ArrayDeque ok
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
        Deque<TreeNode> queue = new LinkedList<>();    // LinkedList: print BFS enqueues null children
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
    { "id": "rootA", "label": "tree A", "type": "tree", "placeholder": "[1, 3, 2, 5]" },
    { "id": "rootB", "label": "tree B", "type": "tree", "placeholder": "[2, 1, 3, null, 4, null, 7]" }
  ],
  "cases": [
    { "args": { "rootA": "[1, 3, 2, 5]", "rootB": "[2, 1, 3, null, 4, null, 7]" }, "expected": "[3, 4, 5, 5, 4, null, 7]" },
    { "args": { "rootA": "[1, 2, 3, 4, null, 5, 6]", "rootB": "[7, 8, 9, 10, 11, null, 12]" }, "expected": "[8, 10, 12, 14, 11, 5, 18]" },
    { "args": { "rootA": "[1, 8, 4, null, null, 2, 7]", "rootB": "[1, 2, 3, 4, null, null, 7]" }, "expected": "[2, 10, 7, 4, null, 2, 14]" },
    { "args": { "rootA": "[]", "rootB": "[]" }, "expected": "[]" },
    { "args": { "rootA": "[5]", "rootB": "[]" }, "expected": "[5]" },
    { "args": { "rootA": "[]", "rootB": "[3]" }, "expected": "[3]" },
    { "args": { "rootA": "[1]", "rootB": "[2]" }, "expected": "[3]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A constructive simultaneous traversal: three base cases at the top handle the `None` combinations cleanly — if either input is `None`, return the other whole (no merging needed). When both are present, create a new node whose value is the sum, then recurse same-side into both left and both right children. The result is a brand-new tree; neither input is mutated.

```python solution time=O(min(|A|,|B|)) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def merge_trees(self, rootA, rootB):
        if rootA is None:
            return rootB
        if rootB is None:
            return rootA
        merged = TreeNode(rootA.val + rootB.val)
        merged.left = self.merge_trees(rootA.left, rootB.left)
        merged.right = self.merge_trees(rootA.right, rootB.right)
        return merged

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
    print(json.dumps(out))           # json.dumps → null, not None

rootA = build_tree(json.loads(input()))
rootB = build_tree(json.loads(input()))
print_tree(Solution().merge_trees(rootA, rootB))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        public TreeNode mergeTrees(TreeNode rootA, TreeNode rootB) {
            if (rootA == null) return rootB;
            if (rootB == null) return rootA;
            TreeNode merged = new TreeNode(rootA.val + rootB.val);
            merged.left = mergeTrees(rootA.left, rootB.left);
            merged.right = mergeTrees(rootA.right, rootB.right);
            return merged;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode rootA = buildTree(parseIntegerArray(sc.nextLine()));
        TreeNode rootB = buildTree(parseIntegerArray(sc.nextLine()));
        printTree(new Solution().mergeTrees(rootA, rootB));
    }

    static TreeNode buildTree(Integer[] values) {   // [1, 2, 3, null, 4] level-order → root
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> queue = new ArrayDeque<>();    // build queue: only real nodes, ArrayDeque ok
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
        Deque<TreeNode> queue = new LinkedList<>();    // LinkedList: print BFS enqueues null children
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

</details>
