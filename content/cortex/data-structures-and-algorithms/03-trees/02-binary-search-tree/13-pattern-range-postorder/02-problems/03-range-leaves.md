---
title: "Range Leaves"
summary: "Given the root of a BST and a range [low, high], replace the value of each *non-leaf* in-range node with the count of in-range leaves in its subtree."
prereqs:
  - 13-pattern-range-postorder/01-pattern
difficulty: medium
---

# Range leaves

## Problem Statement

Given the **root** of a BST and a range `[low, high]`, replace the value of each *non-leaf* in-range node with the count of in-range leaves in its subtree.

> A *leaf* here is a node whose subtree contains no in-range descendants — typically an actual leaf in the original tree.

### Example 1

> - **Input:** `root = [4, 2, 5, 1, 3, null, 6]`, `low = 2`, `high = 5`
> - **Output:** `[1, 1, 0, 1, 3, null, 6]`

### Example 2

> - **Input:** `root = [5, 1, 8, null, null, 6, 9]`, `low = 6`, `high = 9`
> - **Output:** `[5, 1, 2, null, null, 6, 9]`

<details>
<summary><h2>The Strategy</h2></summary>


Same skeleton as range summation, but instead of returning the sum of in-range descendants, return the *count of in-range leaves*. A leaf returns `1`; an internal in-range node returns `leftLeaves + rightLeaves` and overwrites its own value with that count.

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
    def range_leaves_helper(
        self, root: Optional[TreeNode], low: int, high: int
    ) -> int:

        # Base Case : if root is null return 0
        if root is None:
            return 0

        # If the node's value is less than the lower bound,
        # discard the left subtree and move to the right subtree
        if root.val < low:
            return self.range_leaves_helper(root.right, low, high)

        # If the node's value is greater than the upper bound,
        # discard the right subtree and move to the left subtree
        if root.val > high:
            return self.range_leaves_helper(root.left, low, high)

        # If it's a leaf node, return 1
        if root.left is None and root.right is None:

            # Return 1 since it's a leaf node
            return 1

        # If the node's value is within the range [low, high],
        # recursively trim its left and right subtrees
        left_leaves = self.range_leaves_helper(root.left, low, high)
        right_leaves = self.range_leaves_helper(root.right, low, high)

        # Update the current node's value with the count of leaves in
        # its subtrees
        root.val = left_leaves + right_leaves

        # Return the total count of leaves in the current subtree
        return root.val

    def range_leaves(
        self, root: Optional[TreeNode], low: int, high: int
    ) -> None:

        # Call the helper function to calculate the count of leaves
        # in the range [low, high] and update the node values
        self.range_leaves_helper(root, low, high)


# Example 1: [4, 2, 5, 1, 3, null, 6], low=2, high=5 → [1, 1, 0, 1, 3, null, 6]
t1 = from_level_order([4, 2, 5, 1, 3, None, 6])
Solution().range_leaves(t1, 2, 5)
print(level_order_vals(t1))   # [1, 1, 0, 1, 3, 6]

# Example 2: [5, 1, 8, null, null, 6, 9], low=6, high=9 → [5, 1, 2, null, null, 6, 9]
t2 = from_level_order([5, 1, 8, None, None, 6, 9])
Solution().range_leaves(t2, 6, 9)
print(level_order_vals(t2))   # [5, 1, 2, 6, 9]

# Edge cases
Solution().range_leaves(None, 1, 5)   # no-op

# Single node in range (leaf)
t3 = from_level_order([5])
Solution().range_leaves(t3, 1, 10)
print(t3.val)                 # 5  (leaf unchanged)

# Range excludes all nodes
t4 = from_level_order([4, 2, 5, 1, 3, None, 6])
Solution().range_leaves(t4, 7, 10)
print(level_order_vals(t4))   # [4, 2, 5, 1, 3, 6]  (unchanged)
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
        private int rangeLeavesHelper(TreeNode root, int low, int high) {

            // Base Case : if root is null return 0
            if (root == null) {
                return 0;
            }

            // If the node's value is less than the lower bound,
            // discard the left subtree and move to the right subtree
            if (root.val < low) {
                return rangeLeavesHelper(root.right, low, high);
            }

            // If the node's value is greater than the upper bound,
            // discard the right subtree and move to the left subtree
            if (root.val > high) {
                return rangeLeavesHelper(root.left, low, high);
            }

            // If it's a leaf node, return 1
            if (root.left == null && root.right == null) {

                // Return 1 since it's a leaf node
                return 1;
            }

            // If the node's value is within the range [low, high],
            // recursively trim its left and right subtrees
            int leftLeaves = rangeLeavesHelper(root.left, low, high);
            int rightLeaves = rangeLeavesHelper(root.right, low, high);

            // Update the current node's value with the count of leaves in
            // its subtrees
            root.val = leftLeaves + rightLeaves;

            // Return the total count of leaves in the current subtree
            return root.val;
        }

        public void rangeLeaves(TreeNode root, int low, int high) {

            // Call the helper function to calculate the count of leaves
            // in the range [low, high] and update the node values
            rangeLeavesHelper(root, low, high);
        }
    }

    public static void main(String[] args) {
        // Example 1
        TreeNode t1 = fromLevelOrder(4, 2, 5, 1, 3, null, 6);
        new Solution().rangeLeaves(t1, 2, 5);
        System.out.println(levelOrderVals(t1));   // [1, 1, 0, 1, 3, 6]

        // Example 2
        TreeNode t2 = fromLevelOrder(5, 1, 8, null, null, 6, 9);
        new Solution().rangeLeaves(t2, 6, 9);
        System.out.println(levelOrderVals(t2));   // [5, 1, 2, 6, 9]

        // Edge cases
        new Solution().rangeLeaves(null, 1, 5);    // no-op

        // Single node in range (leaf)
        TreeNode t3 = fromLevelOrder(5);
        new Solution().rangeLeaves(t3, 1, 10);
        System.out.println(t3.val);               // 5

        // Range excludes all nodes
        TreeNode t4 = fromLevelOrder(4, 2, 5, 1, 3, null, 6);
        new Solution().rangeLeaves(t4, 7, 10);
        System.out.println(levelOrderVals(t4));   // [4, 2, 5, 1, 3, 6]
    }
}
```

</details>
