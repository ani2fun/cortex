---
title: "Two Sum on BST"
summary: "Given the root of a BST and an integer target, return true if some pair of nodes in the tree has values summing to target."
prereqs:
  - 14-pattern-two-pointer/01-pattern
difficulty: medium
kind: problem
topics: [two-pointer, binary-search-tree]
---

# Two sum on BST

## Problem Statement

Given the **root** of a BST and an integer **target**, return `true` if some pair of nodes in the tree has values summing to `target`. Return `false` otherwise.

## Examples

**Example 1:**
```
Input:  root = [4, 2, 6, 1, null, null, 7], target = 9
Output: true
Explanation: Nodes 2 and 7 sum to 9.
```

**Example 2:**
```
Input:  root = [2, 1, 4, null, null, 3, 7], target = 16
Output: false
```

## Constraints

- `1 ≤ number of nodes ≤ 10⁴`
- `-10⁵ ≤ node.val ≤ 10⁵`
- All node values are unique (BST property)
- `target` may be any integer

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def two_sum_on_bst(self, root, target):
        # Your code goes here — seed ascending and descending stacks;
        # converge lo and hi toward target using two BST iterators.
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
target = int(input())                    # the pair sum to test
print("true" if Solution().two_sum_on_bst(root, target) else "false")
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean twoSumOnBST(TreeNode root, int target) {
            // Your code goes here — seed ascending and descending stacks;
            // converge lo and hi toward target using two BST iterators.
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().twoSumOnBST(root, target));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[4, 2, 6, 1, null, null, 7]" },
    { "id": "target", "label": "target", "type": "int", "placeholder": "9" }
  ],
  "cases": [
    { "args": { "root": "[4, 2, 6, 1, null, null, 7]", "target": "9" }, "expected": "true" },
    { "args": { "root": "[2, 1, 4, null, null, 3, 7]", "target": "16" }, "expected": "false" },
    { "args": { "root": "[4, 2, 6, 1, null, null, 7]", "target": "3" }, "expected": "true" },
    { "args": { "root": "[4, 2, 6, 1, null, null, 7]", "target": "13" }, "expected": "true" },
    { "args": { "root": "[4, 2, 6, 1, null, null, 7]", "target": "14" }, "expected": "false" },
    { "args": { "root": "[5]", "target": "10" }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Use two BST iterators — one ascending (forward), one descending (reverse). Seed the ascending stack with the left spine and the descending with the right spine. Get `lo` from the ascending iterator and `hi` from the descending one, then converge: if `lo + hi == target` return `true`; if the sum is too small advance `lo`; if too big advance `hi`. Stop when `lo ≥ hi`.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class ForwardBstIterator:
    def __init__(self, root):
        self.stack = []
        self._push_all_left(root)

    def _push_all_left(self, node):
        while node:
            self.stack.append(node)
            node = node.left

    def has_next(self):
        return bool(self.stack)

    def next(self):
        node = self.stack.pop()
        self._push_all_left(node.right)
        return node

class ReverseBstIterator:
    def __init__(self, root):
        self.stack = []
        self._push_all_right(root)

    def _push_all_right(self, node):
        while node:
            self.stack.append(node)
            node = node.right

    def has_next(self):
        return bool(self.stack)

    def next(self):
        node = self.stack.pop()
        self._push_all_right(node.left)
        return node

class Solution:
    def two_sum_on_bst(self, root, target):
        if not root:
            return False
        left_iterator = ForwardBstIterator(root)
        right_iterator = ReverseBstIterator(root)
        left_node = left_iterator.next()
        right_node = right_iterator.next()
        while (left_node is not None and right_node is not None
               and left_node.val < right_node.val):
            if left_node.val + right_node.val == target:
                return True
            elif left_node.val + right_node.val < target:
                left_node = left_iterator.next()
            else:
                right_node = right_iterator.next()
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
target = int(input())                    # the pair sum to test
print("true" if Solution().two_sum_on_bst(root, target) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class ForwardBstIterator {
        private Deque<TreeNode> stack = new ArrayDeque<>();
        ForwardBstIterator(TreeNode root) { pushAllLeft(root); }
        private void pushAllLeft(TreeNode node) {
            while (node != null) { stack.push(node); node = node.left; }
        }
        boolean hasNext() { return !stack.isEmpty(); }
        TreeNode next() {
            TreeNode node = stack.pop();
            pushAllLeft(node.right);
            return node;
        }
    }

    static class ReverseBstIterator {
        private Deque<TreeNode> stack = new ArrayDeque<>();
        ReverseBstIterator(TreeNode root) { pushAllRight(root); }
        private void pushAllRight(TreeNode node) {
            while (node != null) { stack.push(node); node = node.right; }
        }
        boolean hasNext() { return !stack.isEmpty(); }
        TreeNode next() {
            TreeNode node = stack.pop();
            pushAllRight(node.left);
            return node;
        }
    }

    static class Solution {
        boolean twoSumOnBST(TreeNode root, int target) {
            if (root == null) return false;
            ForwardBstIterator leftIterator = new ForwardBstIterator(root);
            ReverseBstIterator rightIterator = new ReverseBstIterator(root);
            TreeNode leftNode = leftIterator.next();
            TreeNode rightNode = rightIterator.next();
            while (leftNode != null && rightNode != null && leftNode.val < rightNode.val) {
                int sum = leftNode.val + rightNode.val;
                if (sum == target) return true;
                else if (sum < target) leftNode = leftIterator.next();
                else rightNode = rightIterator.next();
            }
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().twoSumOnBST(root, target));
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

<details>
<summary><strong>Trace — root = [4, 2, 6, 1, null, null, 7], target = 9</strong></summary>

```
Sorted view: [1, 2, 4, 6, 7]

Step 1 │ leftNode=1, rightNode=7 │ sum=8 < 9 → advance left
Step 2 │ leftNode=2, rightNode=7 │ sum=9 ✓  → return true
```

</details>

</details>
