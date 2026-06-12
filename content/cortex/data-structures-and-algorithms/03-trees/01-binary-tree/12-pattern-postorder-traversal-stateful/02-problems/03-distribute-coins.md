---
title: "Distribute Coins"
summary: "Find the minimum number of moves to give every node exactly one coin, where each move transfers one coin along an edge."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: medium
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Distribute Coins

## Problem Statement

You are given a binary tree where each node has `node.val` coins. The total number of coins equals the total number of nodes. A **move** is transferring 1 coin along an edge between two adjacent nodes. Return the **minimum number of moves** so every node ends with exactly 1 coin.

The trick: at every node, define *excess* = `left_excess + right_excess + node.val - 1`. If excess > 0, that many coins must flow *up* to the parent. If excess < 0, that many coins must flow *down* from the parent. Either way, `|excess|` equals the number of moves on the edge to the parent. Sum `|left_excess| + |right_excess|` at every node for the total moves.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 0]
Output: 2
```
Node `2` has 1 excess → flow up. Node `0` needs 1 → flow down from parent. Total = 2 moves.

**Example 2:**
```
Input:  root = [0, 3, 0]
Output: 3
```

## Constraints

- `1 ≤ number of nodes ≤ 10⁴`
- `0 ≤ node.val ≤ number of nodes`
- Total coins = total nodes

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def __init__(self):
        self.moves = 0

    def distribute_coins(self, root):
        # Your code goes here — postorder: return excess = left_excess + right_excess + node.val - 1;
        # add abs(left_excess) + abs(right_excess) to self.moves.
        def balance_coins(node):
            return 0
        balance_coins(root)
        return self.moves

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
print(Solution().distribute_coins(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int moves = 0;

        private int balanceCoins(TreeNode node) {
            // Your code goes here — postorder: return excess = leftExcess + rightExcess + node.val - 1;
            // add Math.abs(leftExcess) + Math.abs(rightExcess) to moves.
            return 0;
        }

        int distributeCoins(TreeNode root) {
            moves = 0;
            balanceCoins(root);
            return moves;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().distributeCoins(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 0]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 0]" }, "expected": "2" },
    { "args": { "root": "[0, 3, 0]" }, "expected": "3" },
    { "args": { "root": "[1]" }, "expected": "0" },
    { "args": { "root": "[2, 0]" }, "expected": "1" },
    { "args": { "root": "[0, 0, 3]" }, "expected": "3" },
    { "args": { "root": "[3, 0, 0]" }, "expected": "2" },
    { "args": { "root": "[1, 1, 1, 1, 1, 1, 1]" }, "expected": "0" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

At each node, `excess = left_excess + right_excess + node.val - 1` is the net surplus flowing upward (negative means the parent must send coins down). The absolute value of `left_excess` and `right_excess` gives the moves on those edges — summed globally.

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
        self.moves = 0

    def balance_coins(self, root):
        # base case: return 0 if the node is None
        if root is None:
            return 0

        # recursively calculate the excess values for the left and
        # right subtrees
        left_excess = self.balance_coins(root.left)
        right_excess = self.balance_coins(root.right)

        # calculate the excess value for the current node
        excess = left_excess + right_excess + root.val - 1

        # add the absolute value of excess values for left and right
        # subtrees to the total moves
        self.moves += abs(left_excess) + abs(right_excess)
        return excess

    def distribute_coins(self, root):
        self.balance_coins(root)
        return self.moves

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
print(Solution().distribute_coins(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int moves = 0;

        private int balanceCoins(TreeNode root) {
            // base case: return 0 if the node is null
            if (root == null) {
                return 0;
            }

            // recursively calculate the excess values for the left and
            // right subtrees
            int leftExcess = balanceCoins(root.left);
            int rightExcess = balanceCoins(root.right);

            // calculate the excess value for the current node
            int excess = leftExcess + rightExcess + root.val - 1;

            // add the absolute value of excess values for left and right
            // subtrees to the total moves
            moves += Math.abs(leftExcess) + Math.abs(rightExcess);
            return excess;
        }

        int distributeCoins(TreeNode root) {
            moves = 0;
            balanceCoins(root);
            return moves;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().distributeCoins(root));
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
