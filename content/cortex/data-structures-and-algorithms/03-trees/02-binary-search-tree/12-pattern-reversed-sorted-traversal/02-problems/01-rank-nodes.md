---
title: "Rank Nodes"
summary: "Given the root of a binary search tree, replace each node's value with its rank in descending order (largest = rank 1)."
prereqs:
  - 12-pattern-reversed-sorted-traversal/01-pattern
difficulty: medium
---

# Rank nodes

## Problem Statement

Given the **root** of a binary search tree, replace each node's value with its **rank in descending order** (largest = rank 1).

### Example 1

> - **Input:** `root = [4, 2, 5, 1, 3, null, 6]`
> - **Output:** `[3, 5, 2, 6, 4, null, 1]`

### Example 2

> - **Input:** `root = [5, 4, 10, null, null, 9, 11]`
> - **Output:** `[4, 5, 2, null, null, 3, 1]`

<details>
<summary><h2>The Strategy</h2></summary>


Walk the tree in reverse in-order. The first node visited (the largest) gets rank `1`; the next gets `2`; and so on. Just maintain a running `rank` counter; every node overwrites its own value with the current `rank`, then increments it.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from typing import Optional


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


def level_order_vals(root):
    """Collect values level-order (None for missing children)."""
    if not root:
        return []
    result, queue = [], [root]
    while queue:
        node = queue.pop(0)
        if node:
            result.append(node.val)
            queue.append(node.left)
            queue.append(node.right)
        else:
            result.append(None)
    # strip trailing Nones
    while result and result[-1] is None:
        result.pop()
    return result


class Solution:
    def __init__(self) -> None:

        # Variable to keep track of the running rank of the tree
        self.rank: int = 1

    def rank_nodes(self, root: Optional[TreeNode]) -> None:

        # Base case
        if root is None:
            return

        # Recursively process the right subtree
        self.rank_nodes(root.right)

        # Update the current node's value
        root.val = self.rank

        # Increment the rank for the next node
        self.rank += 1

        # Recursively process the left subtree
        self.rank_nodes(root.left)


# Example 1: [4, 2, 5, 1, 3, null, 6] → [3, 5, 2, 6, 4, null, 1]
t1 = from_level_order([4, 2, 5, 1, 3, None, 6])
Solution().rank_nodes(t1)
print(level_order_vals(t1))   # [3, 5, 2, 6, 4, 1]

# Example 2: [5, 4, 10, null, null, 9, 11] → [4, 5, 2, null, null, 3, 1]
t2 = from_level_order([5, 4, 10, None, None, 9, 11])
Solution().rank_nodes(t2)
print(level_order_vals(t2))   # [4, 5, 2, 3, 1]

# Edge cases
t3 = None
Solution().rank_nodes(t3)
print(t3)                     # None

t4 = from_level_order([7])
Solution().rank_nodes(t4)
print(t4.val)                 # 1

# Two-node tree: root=5, right=10 → root ranks 2, right ranks 1
t5 = from_level_order([5, None, 10])
Solution().rank_nodes(t5)
print(t5.val, t5.right.val)   # 2 1
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

    static List<Integer> levelOrderVals(TreeNode root) {
        if (root == null) return List.of();
        List<Integer> result = new ArrayList<>();
        java.util.Deque<TreeNode> q = new java.util.ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            TreeNode node = q.poll();
            result.add(node.val);
            if (node.left != null) q.add(node.left);
            if (node.right != null) q.add(node.right);
        }
        return result;
    }

    static class Solution {

        // Variable to keep track of the running rank of the tree
        private int rank = 1;

        public void rankNodes(TreeNode root) {

            // Base case
            if (root == null) {
                return;
            }

            // Recursively process the right subtree
            rankNodes(root.right);

            // Update the current node's value
            root.val = rank;

            // Increment the rank for the next node
            rank++;

            // Recursively process the left subtree
            rankNodes(root.left);
        }
    }

    public static void main(String[] args) {
        // Example 1
        TreeNode t1 = fromLevelOrder(4, 2, 5, 1, 3, null, 6);
        new Solution().rankNodes(t1);
        System.out.println(levelOrderVals(t1));   // [3, 5, 2, 6, 4, 1]

        // Example 2
        TreeNode t2 = fromLevelOrder(5, 4, 10, null, null, 9, 11);
        new Solution().rankNodes(t2);
        System.out.println(levelOrderVals(t2));   // [4, 5, 2, 3, 1]

        // Edge cases
        new Solution().rankNodes(null);            // no-op

        TreeNode t4 = fromLevelOrder(7);
        new Solution().rankNodes(t4);
        System.out.println(t4.val);               // 1

        // Two-node tree: root=5, right=10 → root ranks 2, right ranks 1
        TreeNode t5 = fromLevelOrder(5, null, 10);
        new Solution().rankNodes(t5);
        System.out.println(t5.val + " " + t5.right.val);  // 2 1
    }
}
```

</details>
