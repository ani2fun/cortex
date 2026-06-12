---
title: "Subtree Detection"
summary: "Given trees A and B, return true iff the whole of B occurs somewhere inside A as an exact subtree."
prereqs:
  - 18-pattern-simultaneous-traversal/01-pattern
difficulty: medium
kind: problem
topics: [simultaneous-traversal, binary-tree]
---

# Problem 3 — Subtree detection

## Problem Statement

Given trees A and B, return `true` iff the *whole* of B occurs somewhere inside A as an exact subtree.

Combine two patterns: an *outer* recursion walking A (looking for a place where the comparison succeeds), and an *inner* recursion that runs the identical-trees check between the current A-node and B's root.

## Examples

**Example 1:**
```
Input:  rootA = [1, 8, 5, 4, 2, 3, 9], rootB = [5, 3, 9]
Output: true
```

**Example 2:**
```
Input:  rootA = [1, 8, 4, null, null, 2, 7], rootB = [1, 8, 4]
Output: false
```

## Constraints

- `0 ≤ number of nodes in A ≤ 10⁴`
- `0 ≤ number of nodes in B ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- Time: `O(|A| · |B|)` for the naive recursive approach; `O(|A| + |B|)` is possible via serialization hashing

```python run viz=binary-tree viz-root=rootA
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def identical_trees(self, a, b):
        # Your code goes here — same-side pairing equality check
        return False

    def subtree_detection(self, rootA, rootB):
        # Your code goes here — outer walk of A; at each node check identical_trees(node, rootB)
        # then recurse into A's children with the same rootB
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

rootA = build_tree(json.loads(input()))
rootB = build_tree(json.loads(input()))
print("true" if Solution().subtree_detection(rootA, rootB) else "false")
```

```java run viz=binary-tree viz-root=rootA
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private boolean identicalTrees(TreeNode a, TreeNode b) {
            // Your code goes here — same-side pairing equality check
            return false;
        }

        public boolean subtreeDetection(TreeNode rootA, TreeNode rootB) {
            // Your code goes here — outer walk of A; at each node check identicalTrees(node, rootB)
            // then recurse into A's children with the same rootB
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode rootA = buildTree(parseIntegerArray(sc.nextLine()));
        TreeNode rootB = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().subtreeDetection(rootA, rootB));
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
    { "id": "rootA", "label": "tree A", "type": "tree", "placeholder": "[1, 8, 5, 4, 2, 3, 9]" },
    { "id": "rootB", "label": "tree B", "type": "tree", "placeholder": "[5, 3, 9]" }
  ],
  "cases": [
    { "args": { "rootA": "[1, 8, 5, 4, 2, 3, 9]", "rootB": "[5, 3, 9]" }, "expected": "true" },
    { "args": { "rootA": "[1, 8, 4, null, null, 2, 7]", "rootB": "[1, 8, 4]" }, "expected": "false" },
    { "args": { "rootA": "[1, 2, 3, 4, 5]", "rootB": "[2, 4, 5]" }, "expected": "true" },
    { "args": { "rootA": "[1, 2, 3]", "rootB": "[4]" }, "expected": "false" },
    { "args": { "rootA": "[1, 2, 3]", "rootB": "[1, 2, 3]" }, "expected": "true" },
    { "args": { "rootA": "[1]", "rootB": "[1]" }, "expected": "true" },
    { "args": { "rootA": "[]", "rootB": "[]" }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

The complexity is **O(|A| · |B|)** worst case — every node in A might be the start of an identical-check that walks the whole of B. Faster O(|A| + |B|) algorithms exist using string-hashing on serialised trees, but the naive recursive version is the right interview answer for clarity.

```python solution time=O(|A|·|B|) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def identical_trees(self, a, b):
        if not a and not b:
            return True
        if not a or not b:
            return False
        if a.val != b.val:
            return False
        return self.identical_trees(a.left, b.left) and self.identical_trees(a.right, b.right)

    def subtree_detection(self, rootA, rootB):
        if not rootA:
            return False
        if self.identical_trees(rootA, rootB):
            return True
        return self.subtree_detection(rootA.left, rootB) or self.subtree_detection(rootA.right, rootB)

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

rootA = build_tree(json.loads(input()))
rootB = build_tree(json.loads(input()))
print("true" if Solution().subtree_detection(rootA, rootB) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private boolean identicalTrees(TreeNode a, TreeNode b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            if (a.val != b.val) return false;
            return identicalTrees(a.left, b.left) && identicalTrees(a.right, b.right);
        }

        public boolean subtreeDetection(TreeNode rootA, TreeNode rootB) {
            if (rootA == null) return false;
            if (identicalTrees(rootA, rootB)) return true;
            return subtreeDetection(rootA.left, rootB) || subtreeDetection(rootA.right, rootB);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode rootA = buildTree(parseIntegerArray(sc.nextLine()));
        TreeNode rootB = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().subtreeDetection(rootA, rootB));
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
