---
title: "Distribute Coins"
summary: "See problem statement below."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: medium
---

# Problem 3 — Distribute coins

> Each node has `node.val` coins. Total coins equal total nodes. A move is moving 1 coin between two adjacent nodes. Return the minimum number of moves so every node ends with exactly 1 coin.

The trick: at every node, define *excess* = `(coins received from below) + node.val - 1`. If excess > 0, that many coins must flow *up* to the parent. If excess < 0, that many coins must flow *down* from the parent. Either way, the *absolute value* of excess equals the number of coin moves on the *edge to the parent*.

So sum `|leftExcess|` and `|rightExcess|` at every node — that's the total moves through this node's two outgoing edges to its children.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from typing import List, Optional


class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right


def from_level_order(values):
    """Build tree from list like [1, 2, 3, None, 4]. None means missing child."""
    if not values:
        return None
    root = TreeNode(values[0])
    queue = [root]
    i = 1
    while queue and i < len(values):
        node = queue.pop(0)
        if i < len(values) and values[i] is not None:
            node.left = TreeNode(values[i])
            queue.append(node.left)
        i += 1
        if i < len(values) and values[i] is not None:
            node.right = TreeNode(values[i])
            queue.append(node.right)
        i += 1
    return root


class Solution:

    # Declare moves as a global variable outside the Solution class
    moves: int = 0

    def balance_coins(self, root: Optional[TreeNode]) -> int:

        # base case: return 0 if the node is None
        if root is None:
            return 0

        # recursively calculate the excess values for the left and
        # right subtrees
        left_excess: int = self.balance_coins(root.left)
        right_excess: int = self.balance_coins(root.right)

        # calculate the excess value for the current node
        excess: int = left_excess + right_excess + root.val - 1

        # add the absolute value of excess values for left and right
        # subtrees to the total moves
        self.moves += abs(left_excess) + abs(right_excess)
        return excess

    def distribute_coins(self, root: Optional[TreeNode]) -> int:

        # call balance_coins function to calculate the excess values and
        # update the global moves variable
        self.balance_coins(root)

        # return the total moves required
        return self.moves


# Examples from the problem statement
print(Solution().distribute_coins(from_level_order([1, 2, 0])))   # 2
print(Solution().distribute_coins(from_level_order([0, 3, 0])))   # 3

# Edge cases
print(Solution().distribute_coins(from_level_order([1])))                         # 0 (single node already balanced)
print(Solution().distribute_coins(from_level_order([2, 0])))                      # 1 (move 1 coin from root to left)
print(Solution().distribute_coins(from_level_order([0, 0, 3])))                   # 3
print(Solution().distribute_coins(from_level_order([3, 0, 0])))                   # 2
print(Solution().distribute_coins(from_level_order([1, 0, 2, None, None, 0, 0])))  # 3
print(Solution().distribute_coins(from_level_order([1, 1, 1, 1, 1, 1, 1])))       # 0 (all balanced)
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode() {}
        TreeNode(int val) { this.val = val; }
    }

    static TreeNode fromLevelOrder(Integer... values) {
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        java.util.Deque<TreeNode> queue = new java.util.ArrayDeque<>();
        queue.add(root);
        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            TreeNode node = queue.poll();
            if (i < values.length && values[i] != null) {
                node.left = new TreeNode(values[i]);
                queue.add(node.left);
            }
            i++;
            if (i < values.length && values[i] != null) {
                node.right = new TreeNode(values[i]);
                queue.add(node.right);
            }
            i++;
        }
        return root;
    }

    static class Solution {

        // Declare moves as a global variable outside the Solution class
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

        public int distributeCoins(TreeNode root) {

            // call balanceCoins function to calculate the excess values and
            // update the global moves variable
            balanceCoins(root);

            // return the total moves required
            return moves;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().distributeCoins(fromLevelOrder(1, 2, 0)));   // 2
        System.out.println(new Solution().distributeCoins(fromLevelOrder(0, 3, 0)));   // 3

        // Edge cases
        System.out.println(new Solution().distributeCoins(fromLevelOrder(1)));                          // 0 (single node)
        System.out.println(new Solution().distributeCoins(fromLevelOrder(2, 0)));                       // 1
        System.out.println(new Solution().distributeCoins(fromLevelOrder(0, 0, 3)));                    // 3
        System.out.println(new Solution().distributeCoins(fromLevelOrder(3, 0, 0)));                    // 2
        System.out.println(new Solution().distributeCoins(fromLevelOrder(1, 0, 2, null, null, 0, 0)));  // 3
        System.out.println(new Solution().distributeCoins(fromLevelOrder(1, 1, 1, 1, 1, 1, 1)));        // 0 (all balanced)
    }
}
```

</details>
