---
title: "BST Pair Sum"
summary: "Given the roots of two BSTs and an integer target, return true if there is a pair of nodes (one from each tree) whose values sum to target."
prereqs:
  - 14-pattern-two-pointer/01-pattern
difficulty: hard
kind: problem
topics: [two-pointer, binary-search-tree]
---

# BST pair sum

## Problem Statement

Given the **roots** of two BSTs `rootA` and `rootB`, and an integer **target**, return `true` if there is a pair of nodes (one from each tree) whose values sum to `target`. Return `false` otherwise.

## Examples

**Example 1:**
```
Input:  rootA = [4, 2, 6, 1, null, null, 7], rootB = [2, 1, 4, null, null, 3, 8], target = 15
Output: true
Explanation: 7 (from A) + 8 (from B) = 15.
```

**Example 2:**
```
Input:  rootA = [4, 2, 6, 1, null, null, 7], rootB = [2, 1, 4, null, null, 3, 8], target = 35
Output: false
```

## Constraints

- `1 ≤ number of nodes in each tree ≤ 10⁴`
- `-10⁵ ≤ node.val ≤ 10⁵`
- All node values within each tree are unique (BST property)

```python run viz=binary-tree viz-root=rootA
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def bst_pair_sum(self, root_a, root_b, target):
        # Your code goes here — run ForwardBstIterator on rootA and
        # ReverseBstIterator on rootB; converge toward target until
        # either iterator runs out (no crossing condition since trees are independent).
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

rootA = build_tree(json.loads(input()))   # first tree level-order values
rootB = build_tree(json.loads(input()))   # second tree level-order values
target = int(input())                     # the pair sum to test
print("true" if Solution().bst_pair_sum(rootA, rootB, target) else "false")
```

```java run viz=binary-tree viz-root=rootA
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        boolean bstPairSum(TreeNode rootA, TreeNode rootB, int target) {
            // Your code goes here — run ForwardBstIterator on rootA and
            // ReverseBstIterator on rootB; converge toward target until
            // either iterator runs out.
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode rootA = buildTree(parseIntegerArray(sc.nextLine()));
        TreeNode rootB = buildTree(parseIntegerArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().bstPairSum(rootA, rootB, target));
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
    { "id": "rootA", "label": "rootA", "type": "tree", "placeholder": "[4, 2, 6, 1, null, null, 7]" },
    { "id": "rootB", "label": "rootB", "type": "tree", "placeholder": "[2, 1, 4, null, null, 3, 8]" },
    { "id": "target", "label": "target", "type": "int", "placeholder": "15" }
  ],
  "cases": [
    { "args": { "rootA": "[4, 2, 6, 1, null, null, 7]", "rootB": "[2, 1, 4, null, null, 3, 8]", "target": "15" }, "expected": "true" },
    { "args": { "rootA": "[4, 2, 6, 1, null, null, 7]", "rootB": "[2, 1, 4, null, null, 3, 8]", "target": "35" }, "expected": "false" },
    { "args": { "rootA": "[3]", "rootB": "[4]", "target": "7" }, "expected": "true" },
    { "args": { "rootA": "[3]", "rootB": "[4]", "target": "8" }, "expected": "false" },
    { "args": { "rootA": "[4, 2, 6, 1, null, null, 7]", "rootB": "[2, 1, 4, null, null, 3, 8]", "target": "2" }, "expected": "true" },
    { "args": { "rootA": "[4, 2, 6, 1, null, null, 7]", "rootB": "[2, 1, 4, null, null, 3, 8]", "target": "20" }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

This is the multi-tree generalisation of "two sum on BST." Run the **forward iterator on the first tree** (`rootA`) and the **reverse iterator on the second tree** (`rootB`), and apply the same step rule: `sum < target` → advance left (forward on A); `sum > target` → advance right (reverse on B); `sum == target` → found. The crossing condition no longer applies — stop when *either* iterator runs out (the two trees are independent, so there is no "they crossed" event).

```python solution time=O(n+m) space=O(hA+hB)
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
        if not self.stack:
            return None
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
        if not self.stack:
            return None
        node = self.stack.pop()
        self._push_all_right(node.left)
        return node

class Solution:
    def bst_pair_sum(self, root_a, root_b, target):
        if not root_a or not root_b:
            return False
        left_iterator = ForwardBstIterator(root_a)
        right_iterator = ReverseBstIterator(root_b)
        left_node = left_iterator.next()
        right_node = right_iterator.next()
        while left_node is not None and right_node is not None:
            s = left_node.val + right_node.val
            if s == target:
                return True
            elif s < target:
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

rootA = build_tree(json.loads(input()))   # first tree level-order values
rootB = build_tree(json.loads(input()))   # second tree level-order values
target = int(input())                     # the pair sum to test
print("true" if Solution().bst_pair_sum(rootA, rootB, target) else "false")
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
            if (stack.isEmpty()) return null;
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
            if (stack.isEmpty()) return null;
            TreeNode node = stack.pop();
            pushAllRight(node.left);
            return node;
        }
    }

    static class Solution {
        boolean bstPairSum(TreeNode rootA, TreeNode rootB, int target) {
            if (rootA == null || rootB == null) return false;
            ForwardBstIterator leftIterator = new ForwardBstIterator(rootA);
            ReverseBstIterator rightIterator = new ReverseBstIterator(rootB);
            TreeNode leftNode = leftIterator.next();
            TreeNode rightNode = rightIterator.next();
            while (leftNode != null && rightNode != null) {
                int s = leftNode.val + rightNode.val;
                if (s == target) return true;
                else if (s < target) leftNode = leftIterator.next();
                else rightNode = rightIterator.next();
            }
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode rootA = buildTree(parseIntegerArray(sc.nextLine()));
        TreeNode rootB = buildTree(parseIntegerArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().bstPairSum(rootA, rootB, target));
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
<summary><strong>Trace — rootA = [4, 2, 6, 1, null, null, 7], rootB = [2, 1, 4, null, null, 3, 8], target = 15</strong></summary>

```
A sorted: [1, 2, 4, 6, 7]
B sorted: [1, 2, 3, 4, 8]

Step 1 │ l=1 (from A), r=8 (from B) │ sum=9  < 15 → advance left
Step 2 │ l=2, r=8                   │ sum=10 < 15 → advance left
Step 3 │ l=4, r=8                   │ sum=12 < 15 → advance left
Step 4 │ l=6, r=8                   │ sum=14 < 15 → advance left
Step 5 │ l=7, r=8                   │ sum=15 ✓   → return true
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

The Two Pointer pattern on BSTs is the meeting of two ideas you've already mastered: **iterators** that walk a BST in sorted order on demand (lesson 9), and **two-pointer reductions** familiar from sorted arrays. Run a forward iterator and a reverse iterator simultaneously, and you have a working `(small, large)` pair you can use to drive any sum/multiple/distance/comparison decision — without ever materialising the sorted array.

The pay-off is striking: many "pair" problems on BSTs that would naively be O(n²) (compare every pair) or O(n) memory (flatten to array, then two-pointer) collapse to **O(n) time, O(h) space** with this pattern.

Three patterns to keep:

1. **Iterators turn BSTs into sorted streams.** Once you can `next()` and `hasNext()`, every algorithm that works on sorted arrays generalises directly to BSTs. The conversion is *free* in terms of memory.
2. **Two iterators, two directions.** This is the BST analogue of the array two-pointer template — and it solves the same problem family (sum-to-target, pair properties, median, ranges).
3. **Two BSTs at once.** Different sources of the left and right pointers gives us cross-tree operations like *bst-pair-sum*. The same trick scales further: streaming joins between two sorted indexes in a database use exactly this idea.

</details>
<details>
<summary><h2>Closing the Chapter</h2></summary>

You started this chapter with a static binary tree decorated with one extra rule, and you finish it able to **search, insert, delete, validate, range-query, iterate, and pair-traverse** with confidence. The single thread tying every lesson together is the **binary search property** — the small invariant that turns "look at every node" into "look at one path", and that turns ordered-set problems into single-pass tree walks. Every BST operation, every pattern, every pair of iterators in this chapter is a different way of leaning on that one rule.

Heaps, the next chapter, change the rule — instead of "left smaller, right larger" it's "parent smaller than children". The shape becomes a different tool, optimised not for sorted iteration but for repeatedly extracting the minimum (or maximum). The mental model you've built here will transfer cleanly: it's still a tree, still a property, still a discipline on where values live. Different rule, different superpower.

</details>

</details>
