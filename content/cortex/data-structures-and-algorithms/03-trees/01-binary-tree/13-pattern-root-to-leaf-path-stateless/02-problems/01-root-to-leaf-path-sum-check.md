---
title: "Root to Leaf Path (Sum Check)"
summary: "Return true if the binary tree has at least one root-to-leaf path whose node values sum to a target."
prereqs:
  - 13-pattern-root-to-leaf-path-stateless/01-pattern
difficulty: medium
kind: problem
topics: [root-to-leaf-path, binary-tree]
---

# Root to leaf path (sum check)

## Problem Statement

Given the `root` of a binary tree and an integer `target`, return `true` if there exists at least one **root-to-leaf** path whose node values add up exactly to `target`, and `false` otherwise. A leaf is a node with no children.

This is the generic root-to-leaf skeleton: carry the *remaining target* down as a function argument (subtract each node's value on the way in), and at a leaf ask whether the remaining target is exactly the leaf's value. The combine across the two children is `OR` — any single satisfying path is enough.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, 5, null, 6],  target = 7
Output: true       (path 1 → 2 → 4)
```

**Example 2:**
```
Input:  root = [1, 2, 3, 4, 5, null, 6],  target = 100
Output: false
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "root = [1, 2, 3, 4, 5, null, 6],  target = 10",
  "options": ["true", "false"],
  "answer": "true"
}
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`; `target` fits in a 32-bit integer
- One top-down pass — `O(n)` time, `O(h)` recursion stack

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def has_path_sum(self, node, target):
        # Your code goes here — None → False; at a leaf, does node.val finish the
        # target? Otherwise push (target - node.val) down to both children and OR.
        pass

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
target = int(input())                    # the path sum to test
print("true" if Solution().has_path_sum(root, target) else "false")
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean hasPathSum(TreeNode node, int target) {
            // Your code goes here — null → false; at a leaf, does node.val finish the
            // target? Otherwise push (target - node.val) down to both children and OR.
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().hasPathSum(root, target));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, 5, null, 6]" },
    { "id": "target", "label": "target", "type": "int", "placeholder": "7" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, 5, null, 6]", "target": "7" }, "expected": "true" },
    { "args": { "root": "[1, 2, 3, 4, 5, null, 6]", "target": "8" }, "expected": "true" },
    { "args": { "root": "[1, 2, 3, 4, 5, null, 6]", "target": "10" }, "expected": "true" },
    { "args": { "root": "[1, 2, 3, 4, 5, null, 6]", "target": "100" }, "expected": "false" },
    { "args": { "root": "[5]", "target": "5" }, "expected": "true" },
    { "args": { "root": "[]", "target": "0" }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Carry the remaining target down as an argument. At `None`, no path completes here, so return `False`. At a leaf (no children) the path is finished, so the only question is whether this node's value equals the remaining target. At an internal node, spend the node's value — pass `target - node.val` down — and `OR` the two children, since any one satisfying path is enough. The remaining target travels purely as the argument, so the two recursive calls never interfere. `O(n)` time, `O(h)` stack.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def has_path_sum(self, node, target):
        if node is None:
            return False
        if node.left is None and node.right is None:   # leaf: does it finish the target?
            return node.val == target
        rem = target - node.val                        # push the REMAINING target DOWN
        return self.has_path_sum(node.left, rem) or self.has_path_sum(node.right, rem)

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
target = int(input())                    # the path sum to test
print("true" if Solution().has_path_sum(root, target) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean hasPathSum(TreeNode node, int target) {
            if (node == null) return false;
            if (node.left == null && node.right == null) return node.val == target;  // leaf finishes target?
            int rem = target - node.val;                       // push the REMAINING target down
            return hasPathSum(node.left, rem) || hasPathSum(node.right, rem);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().hasPathSum(root, target));
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
