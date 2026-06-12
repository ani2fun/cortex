---
title: "Cousin Check"
summary: "Two nodes are cousins if they are at the same depth and have different parents. Given two values valA and valB, return true iff their nodes are cousins."
prereqs:
  - 15-pattern-level-order-traversal/01-pattern
difficulty: medium
kind: problem
topics: [level-order-traversal, binary-tree]
---

# Problem 5 — Cousin check

## Problem Statement

Two nodes are *cousins* if they're at the same depth and have *different* parents. Given two values `valA` and `valB`, return `true` iff their nodes are cousins.

Augment the BFS so each enqueued item carries *both* the node and its parent. As we walk a level, look for the two target values; if both are found on the same level *and* they have different parents, return `true`. If only one is found on a level, they're not at the same depth, return `false`.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, null, null, 7], valA = 4, valB = 7
Output: true
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 7, null, 9], valA = 2, valB = 8
Output: false
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `1 ≤ node.val ≤ 10⁴`
- `valA ≠ valB`; both values appear in the tree.

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def cousin_check(self, root, val_a, val_b):
        # Your code goes here — enqueue (node, parent) pairs; at each level record
        # the parents of valA and valB; if both found and parents differ → true.
        # If only one found at a level → false. Return false if neither found.
        return False

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
val_a = int(input())                     # first target value
val_b = int(input())                     # second target value
r = Solution().cousin_check(root, val_a, val_b)
print("true" if r else "false")
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean cousinCheck(TreeNode root, int valA, int valB) {
            // Your code goes here — enqueue NodeInfo(node, parent) pairs; at each
            // level record the parents of valA and valB; if both found and parents
            // differ → true. If only one found at a level → false.
            return false;
        }
    }

    static class NodeInfo {
        TreeNode node, parent;
        NodeInfo(TreeNode node, TreeNode parent) { this.node = node; this.parent = parent; }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int valA = Integer.parseInt(sc.nextLine().trim());
        int valB = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().cousinCheck(root, valA, valB));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, null, null, 7]" },
    { "id": "valA", "label": "valA", "type": "int", "placeholder": "4" },
    { "id": "valB", "label": "valB", "type": "int", "placeholder": "7" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]", "valA": "4", "valB": "7" }, "expected": "true" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7, null, 9]", "valA": "2", "valB": "8" }, "expected": "false" },
    { "args": { "root": "[1, 2, 3]", "valA": "2", "valB": "3" }, "expected": "false" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]", "valA": "4", "valB": "7" }, "expected": "true" },
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]", "valA": "2", "valB": "3" }, "expected": "false" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]", "valA": "5", "valB": "6" }, "expected": "true" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Enqueue `(node, parent)` tuples instead of bare nodes. At each level, scan for `valA` and `valB`, recording their parents. After draining the level: if both parents are set and they point to different nodes, the answer is `true`. If exactly one was found, the nodes are on different levels — `false`. If neither, continue to the next level.

```python solution time=O(n) space=O(w)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def cousin_check(self, root, val_a, val_b):
        if not root: return False
        q = deque([(root, None)])
        while q:
            n = len(q)
            parent_a, parent_b = None, None
            for _ in range(n):
                node, parent = q.popleft()
                if node.val == val_a: parent_a = parent
                if node.val == val_b: parent_b = parent
                if node.left:  q.append((node.left, node))
                if node.right: q.append((node.right, node))
            if parent_a and parent_b:
                return parent_a != parent_b
            if parent_a or parent_b:
                return False
        return False

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
val_a = int(input())                     # first target value
val_b = int(input())                     # second target value
r = Solution().cousin_check(root, val_a, val_b)
print("true" if r else "false")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class NodeInfo {
        TreeNode node, parent;
        NodeInfo(TreeNode node, TreeNode parent) { this.node = node; this.parent = parent; }
    }

    static class Solution {
        boolean cousinCheck(TreeNode root, int valA, int valB) {
            if (root == null) return false;
            Queue<NodeInfo> q = new LinkedList<>();
            q.add(new NodeInfo(root, null));
            while (!q.isEmpty()) {
                int n = q.size();
                TreeNode parentA = null, parentB = null;
                for (int i = 0; i < n; i++) {
                    NodeInfo cur = q.poll();
                    TreeNode node = cur.node, parent = cur.parent;
                    if (node.val == valA) parentA = parent;
                    if (node.val == valB) parentB = parent;
                    if (node.left != null)  q.add(new NodeInfo(node.left, node));
                    if (node.right != null) q.add(new NodeInfo(node.right, node));
                }
                if (parentA != null && parentB != null) return parentA != parentB;
                if (parentA != null || parentB != null) return false;
            }
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int valA = Integer.parseInt(sc.nextLine().trim());
        int valB = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().cousinCheck(root, valA, valB));
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
<details>
<summary><h2>Key Takeaway</h2></summary>

Level-order is your hammer for any *horizontal* question about a tree. Three things to walk away with:

1. **`levelSize = queue.size()` is the entire trick.** That single snapshot at the top of each outer-loop iteration is what separates "flat BFS" from "BFS with level boundaries". Once it's muscle memory, every per-level question becomes mechanical.
2. **Enqueue children, not always non-null.** For most problems (sum, max, list per level) you skip null children. For *completeness* checks you enqueue them deliberately so you can spot gaps. The choice depends on the question.
3. **Augment the queue when you need parents.** The cousin-check trick — enqueueing `(node, parent)` pairs — generalises: any per-node side-info you need (depth, column, path-from-root, sibling) can travel alongside the node. Don't try to retrofit it; bake it into the queue's element type.

> *Coming up — the next lesson takes level-order to <strong>two dimensions</strong>. Instead of grouping nodes by their <em>level</em>, we'll group by their <em>horizontal column</em> — yielding the tree's "top view", "bottom view", and "vertical traversal". Same BFS engine, an extra coordinate per queue entry.*

</details>
