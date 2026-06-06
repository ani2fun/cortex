---
title: "Lowest Absolute Variance"
summary: "Given the root of a binary search tree, return the lowest absolute variance — the minimum absolute difference — between the values of any two different nodes."
prereqs:
  - 11-pattern-sorted-traversal/01-pattern
difficulty: medium
---

# Lowest absolute variance

## Problem Statement

Given the **root** of a binary search tree, return the lowest absolute variance — the minimum absolute difference — between the values of any two different nodes.

### Example 1

> - **Input:** `root = [5, 4, 8, 2, null, null, 10]`
> - **Output:** `1`
> - **Explanation:** The smallest gap is between `4` and `5`.

### Example 2

> - **Input:** `root = [10, 8, 14, 5, null, 12, 17]`
> - **Output:** `2`
> - **Explanation:** The smallest gap is `2` (between `8` and `10`, or between `12` and `14`).

<details>
<summary><h2>The Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from typing import Optional, List


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
    def __init__(self):

        # Variable to keep track of the minimum difference
        self.min_diff = float("inf")

        # Reference to keep track of the previous node
        self.prev_node = None

    def inorder(self, root: Optional[TreeNode]):
        if root is None:
            return

        # Traverse left subtree
        self.inorder(root.left)

        # Check the difference with the previous node
        if self.prev_node is not None:
            self.min_diff = min(
                self.min_diff, root.val - self.prev_node.val
            )

        # Update the previous node
        self.prev_node = root

        # Traverse right subtree
        self.inorder(root.right)

    def lowest_absolute_variance(self, root: Optional[TreeNode]) -> int:

        # Perform in-order traversal
        self.inorder(root)

        # Return the minimum difference found
        return self.min_diff


# Example 1: [5, 4, 8, 2, null, null, 10] → 1
print(Solution().lowest_absolute_variance(
    from_level_order([5, 4, 8, 2, None, None, 10])))   # 1

# Example 2: [10, 8, 14, 5, null, 12, 17] → 2
print(Solution().lowest_absolute_variance(
    from_level_order([10, 8, 14, 5, None, 12, 17])))   # 2

# Edge cases
print(Solution().lowest_absolute_variance(
    from_level_order([5])))                             # inf (single node)

print(Solution().lowest_absolute_variance(
    from_level_order([3, 1, 5])))                      # 2

# Left-skew BST: 1, 2, 3, 4
root_skew = TreeNode(4)
root_skew.left = TreeNode(3)
root_skew.left.left = TreeNode(2)
root_skew.left.left.left = TreeNode(1)
print(Solution().lowest_absolute_variance(root_skew))  # 1

# Consecutive values: consecutive diffs of 1
print(Solution().lowest_absolute_variance(
    from_level_order([5, 3, 7, 2, 4, 6, 8])))         # 1
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

        // Variable to keep track of the minimum difference
        private int minDiff = Integer.MAX_VALUE;

        // Reference to keep track of the previous node
        private TreeNode prevNode = null;

        private void inorder(TreeNode root) {
            if (root == null) {
                return;
            }

            // Traverse left subtree
            inorder(root.left);

            // Check the difference with the previous node
            if (prevNode != null) {
                minDiff = Math.min(minDiff, root.val - prevNode.val);
            }

            // Update the previous node
            prevNode = root;

            // Traverse right subtree
            inorder(root.right);
        }

        public int lowestAbsoluteVariance(TreeNode root) {

            // Perform in-order traversal
            inorder(root);

            // Return the minimum difference found
            return minDiff;
        }
    }

    public static void main(String[] args) {
        // Example 1: [5, 4, 8, 2, null, null, 10] → 1
        System.out.println(new Solution().lowestAbsoluteVariance(
            fromLevelOrder(5, 4, 8, 2, null, null, 10)));   // 1

        // Example 2: [10, 8, 14, 5, null, 12, 17] → 2
        System.out.println(new Solution().lowestAbsoluteVariance(
            fromLevelOrder(10, 8, 14, 5, null, 12, 17)));   // 2

        // Single node — no pair exists
        System.out.println(new Solution().lowestAbsoluteVariance(
            fromLevelOrder(5)));                             // Integer.MAX_VALUE

        // Balanced BST with min diff = 2
        System.out.println(new Solution().lowestAbsoluteVariance(
            fromLevelOrder(3, 1, 5)));                      // 2

        // Left-skew BST: 4-3-2-1
        TreeNode skew = new TreeNode(4);
        skew.left = new TreeNode(3);
        skew.left.left = new TreeNode(2);
        skew.left.left.left = new TreeNode(1);
        System.out.println(new Solution().lowestAbsoluteVariance(skew)); // 1

        // Consecutive values: min diff = 1
        System.out.println(new Solution().lowestAbsoluteVariance(
            fromLevelOrder(5, 3, 7, 2, 4, 6, 8)));         // 1
    }
}
```

</details>
