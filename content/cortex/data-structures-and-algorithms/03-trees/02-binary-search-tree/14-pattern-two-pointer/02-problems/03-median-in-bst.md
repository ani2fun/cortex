---
title: "Median in BST"
summary: "Given the root of a BST, return the median value of the in-order sequence. If the count is even, return the floor of the average of the two middle values."
prereqs:
  - 14-pattern-two-pointer/01-pattern
difficulty: hard
kind: problem
topics: [two-pointer, binary-search-tree]
---

# Median in BST

## Problem Statement

Given the **root** of a BST, return the **median** value of the sorted in-order sequence.

> If the count is odd, the median is the single middle value. If even, it is the floor of the average of the two middle values (integer division).

## Examples

**Example 1:**
```
Input:  root = [5, 4, 6, 2, null, null, 7]
Output: 5
Explanation: Sorted: [2, 4, 5, 6, 7]. Middle: 5.
```

**Example 2:**
```
Input:  root = [10, 8, 14, 5, null, 13, 17]
Output: 11
Explanation: Sorted: [5, 8, 10, 13, 14, 17]. Middle pair: (10, 13). Floor((10+13)/2) = 11.
```

## Constraints

- `1 ≤ number of nodes ≤ 10⁴`
- `-10⁵ ≤ node.val ≤ 10⁵`
- All node values are unique (BST property)
- Return `-1` for an empty tree

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def median_in_bst(self, root):
        # Your code goes here — advance both iterators step-by-step;
        # if they meet at the same node (odd count) return that node's value;
        # if they cross (even count) return floor((last_left + last_right) / 2).
        return -1

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
print(Solution().median_in_bst(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int medianInBst(TreeNode root) {
            // Your code goes here — advance both iterators step-by-step;
            // if they meet at the same node (odd count) return that node's value;
            // if they cross (even count) return (lastLeft + lastRight) / 2.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().medianInBst(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[5, 4, 6, 2, null, null, 7]" }
  ],
  "cases": [
    { "args": { "root": "[5, 4, 6, 2, null, null, 7]" }, "expected": "5" },
    { "args": { "root": "[10, 8, 14, 5, null, 13, 17]" }, "expected": "11" },
    { "args": { "root": "[7]" }, "expected": "7" },
    { "args": { "root": "[3, 1, 5]" }, "expected": "3" },
    { "args": { "root": "[4, 2, 6, 1, 3]" }, "expected": "3" },
    { "args": { "root": "[4, 2, 6, 1, 3, 5, 7]" }, "expected": "4" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

The two-pointer pattern *naturally* finds the median: walk both iterators forward step-by-step, always advancing both. Track the last `(left_node, right_node)` pair. If the count is odd, eventually `left_node == right_node` — that single node's value is the median. If even, the loop exits when the two pointers cross, and the last recorded `(left_node.val + right_node.val) // 2` is the answer.

Both Python (`//`) and Java (int `/` int) truncate toward zero identically for non-negative values, so the output is always an integer and byte-equal across both languages.

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
    def median_in_bst(self, root):
        if not root:
            return -1
        left_iterator = ForwardBstIterator(root)
        right_iterator = ReverseBstIterator(root)
        left_node = left_iterator.next()
        right_node = right_iterator.next()
        median = -1
        while (left_node is not None and right_node is not None
               and left_node.val < right_node.val):
            median = (left_node.val + right_node.val) // 2
            left_node = left_iterator.next()
            right_node = right_iterator.next()
        if left_node == right_node:
            return left_node.val
        return median

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
print(Solution().median_in_bst(root))
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
        int medianInBst(TreeNode root) {
            if (root == null) return -1;
            ForwardBstIterator leftIterator = new ForwardBstIterator(root);
            ReverseBstIterator rightIterator = new ReverseBstIterator(root);
            TreeNode leftNode = leftIterator.next();
            TreeNode rightNode = rightIterator.next();
            int median = -1;
            while (leftNode != null && rightNode != null && leftNode.val < rightNode.val) {
                median = (leftNode.val + rightNode.val) / 2;
                leftNode = leftIterator.next();
                rightNode = rightIterator.next();
            }
            if (leftNode == rightNode) return leftNode.val;
            return median;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().medianInBst(root));
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
<summary><strong>Trace — root = [10, 8, 14, 5, null, 13, 17]</strong></summary>

```
Sorted: [5, 8, 10, 13, 14, 17]  (even count = 6)

Step 1 │ l=5, r=17 │ 5 < 17 → median candidate = (5+17)/2 = 11 → advance both
Step 2 │ l=8, r=14 │ 8 < 14 → median candidate = (8+14)/2 = 11 → advance both
Step 3 │ l=10, r=13 │ 10 < 13 → median candidate = (10+13)/2 = 11 → advance both
Step 4 │ l=13, r=10 │ 13 > 10 → loop exits (crossed)
l != r → even count → return 11 ✓
```

</details>

</details>
