---
title: "Enriched Sum Tree"
summary: "Given the root of a binary search tree, replace every node's value with the sum of its original value and the values of *all nodes greater than it*. The resulting tree is called an enriched sum tree ("
prereqs:
  - 12-pattern-reversed-sorted-traversal/01-pattern
difficulty: medium
---

# Enriched sum tree

## Problem Statement

Given the **root** of a binary search tree, replace every node's value with the sum of its original value and the values of *all nodes greater than it*. The resulting tree is called an **enriched sum tree** (sometimes "greater-tree").

### Example 1

> - **Input:** `root = [4, 2, 5, 1, 3, null, 6]`
> - **Output:** `[15, 20, 11, 21, 18, null, 6]`

### Example 2

> - **Input:** `root = [5, 4, 10, null, null, 9, 11]`
> - **Output:** `[35, 39, 21, null, null, 30, 11]`

<details>
<summary><h2>The Strategy</h2></summary>


Reverse in-order visits nodes from largest to smallest. Maintain a running `sum`; at each node:

1. Add the current node's value to `sum`.
2. Overwrite the current node's value with `sum`.

By the time we visit a node, `sum` already contains the total of every strictly larger node we've already passed *plus* the current node — exactly the value the problem asks for.

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
    while result and result[-1] is None:
        result.pop()
    return result


class Solution:
    def __init__(self) -> None:

        # Variable to keep track of the running sum of the tree
        self.sum: int = 0

    def enriched_sum_tree(self, root: Optional[TreeNode]) -> None:

        # Base case
        if root is None:
            return

        # Recursively process the right subtree
        self.enriched_sum_tree(root.right)

        # Update the running sum with the current node's value
        self.sum += root.val

        # Update the current node's value
        root.val = self.sum

        # Recursively process the left subtree
        self.enriched_sum_tree(root.left)


# Example 1: [4, 2, 5, 1, 3, null, 6] → [15, 20, 11, 21, 18, null, 6]
t1 = from_level_order([4, 2, 5, 1, 3, None, 6])
Solution().enriched_sum_tree(t1)
print(level_order_vals(t1))   # [15, 20, 11, 21, 18, 6]

# Example 2: [5, 4, 10, null, null, 9, 11] → [35, 39, 21, null, null, 30, 11]
t2 = from_level_order([5, 4, 10, None, None, 9, 11])
Solution().enriched_sum_tree(t2)
print(level_order_vals(t2))   # [35, 39, 21, 30, 11]

# Edge cases
Solution().enriched_sum_tree(None)   # no-op

t3 = from_level_order([7])
Solution().enriched_sum_tree(t3)
print(t3.val)                 # 7  (single node, no larger values)

# Two-node tree: root=3, right=5 → root=8 (3+5), right=5
t4 = from_level_order([3, None, 5])
Solution().enriched_sum_tree(t4)
print(t4.val, t4.right.val)   # 8 5

# Three-node balanced: [2, 1, 3]
t5 = from_level_order([2, 1, 3])
Solution().enriched_sum_tree(t5)
print(level_order_vals(t5))   # [5, 6, 3]
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

        // Variable to keep track of the running sum of the tree
        private int sum = 0;

        public void enrichedSumTree(TreeNode root) {

            // Base case
            if (root == null) {
                return;
            }

            // Recursively process the right subtree
            enrichedSumTree(root.right);

            // Update the running sum with the current node's value
            sum += root.val;

            // Update the current node's value
            root.val = sum;

            // Recursively process the left subtree
            enrichedSumTree(root.left);
        }
    }

    public static void main(String[] args) {
        // Example 1
        TreeNode t1 = fromLevelOrder(4, 2, 5, 1, 3, null, 6);
        new Solution().enrichedSumTree(t1);
        System.out.println(levelOrderVals(t1));   // [15, 20, 11, 21, 18, 6]

        // Example 2
        TreeNode t2 = fromLevelOrder(5, 4, 10, null, null, 9, 11);
        new Solution().enrichedSumTree(t2);
        System.out.println(levelOrderVals(t2));   // [35, 39, 21, 30, 11]

        // Edge cases
        new Solution().enrichedSumTree(null);      // no-op

        TreeNode t3 = fromLevelOrder(7);
        new Solution().enrichedSumTree(t3);
        System.out.println(t3.val);               // 7

        // Two-node tree: root=3, right=5
        TreeNode t4 = fromLevelOrder(3, null, 5);
        new Solution().enrichedSumTree(t4);
        System.out.println(t4.val + " " + t4.right.val);  // 8 5

        // Three-node balanced [2, 1, 3]
        TreeNode t5 = fromLevelOrder(2, 1, 3);
        new Solution().enrichedSumTree(t5);
        System.out.println(levelOrderVals(t5));   // [5, 6, 3]
    }
}
```


<details>
<summary><strong>Trace — root = [4, 2, 5, 1, 3, null, 6]</strong></summary>

```
sum = 0, visit order: 6, 5, 4, 3, 2, 1
visit 6 │ sum = 0 + 6 = 6   → node.val = 6
visit 5 │ sum = 6 + 5 = 11  → node.val = 11
visit 4 │ sum = 11 + 4 = 15 → node.val = 15
visit 3 │ sum = 15 + 3 = 18 → node.val = 18
visit 2 │ sum = 18 + 2 = 20 → node.val = 20
visit 1 │ sum = 20 + 1 = 21 → node.val = 21
Result: [15, 20, 11, 21, 18, null, 6] ✓
```

</details>

</details>
