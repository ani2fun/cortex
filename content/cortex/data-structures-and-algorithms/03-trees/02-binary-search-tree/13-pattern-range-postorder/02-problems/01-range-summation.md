---
title: "Range Summation"
summary: "Given the root of a BST and a range [low, high], update each in-range node's value by adding the values of all its descendants that are also in range. Return nothing — the tree is mutated in place."
prereqs:
  - 13-pattern-range-postorder/01-pattern
difficulty: medium
---

# Range summation

## Problem Statement

Given the **root** of a BST and a range `[low, high]`, update each in-range node's value by adding the values of all its descendants that are also in range. Return nothing — the tree is mutated in place.

> Guarantee: a node *outside* the range never has any in-range descendants on either side. (This follows from BST structure, but the problem states it explicitly so the pruning is safe.)

### Example 1

> - **Input:** `root = [4, 2, 5, 1, 3, null, 6]`, `low = 2`, `high = 5`
> - **Output:** `[14, 5, 5, 1, 3, null, 6]`

### Example 2

> - **Input:** `root = [5, 1, 8, null, null, 6, 9]`, `low = 6`, `high = 9`
> - **Output:** `[5, 1, 23, null, null, 6, 9]`

<details>
<summary><h2>The Strategy</h2></summary>


Every in-range node accumulates `leftSum + rightSum + originalVal` and writes that back into `node.val`. The recursion returns the same total to its parent so parents can do the same.

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
    def range_summation_helper(
        self, root: Optional[TreeNode], low: int, high: int
    ) -> int:

        # Base Case : if root is null return 0
        if root is None:
            return 0

        # If the node's value is less than the lower bound,
        # discard the left subtree and move to the right subtree
        if root.val < low:
            return self.range_summation_helper(root.right, low, high)

        # If the node's value is greater than the upper bound,
        # discard the right subtree and move to the left subtree
        if root.val > high:
            return self.range_summation_helper(root.left, low, high)

        # If the node's value is within the range [low, high],
        # recursively compute the sum of valid left and right subtrees
        left_sum = self.range_summation_helper(root.left, low, high)
        right_sum = self.range_summation_helper(root.right, low, high)

        # Add sum of in-range descendants to the current node's value
        root.val += left_sum + right_sum

        # Return the updated value of the current node
        # (which now includes valid descendants)
        return root.val

    def range_summation(
        self, root: Optional[TreeNode], low: int, high: int
    ) -> None:
        self.range_summation_helper(root, low, high)


# Example 1: [4, 2, 5, 1, 3, null, 6], low=2, high=5 → [14, 5, 5, 1, 3, null, 6]
t1 = from_level_order([4, 2, 5, 1, 3, None, 6])
Solution().range_summation(t1, 2, 5)
print(level_order_vals(t1))   # [14, 5, 5, 1, 3, 6]

# Example 2: [5, 1, 8, null, null, 6, 9], low=6, high=9 → [5, 1, 23, null, null, 6, 9]
t2 = from_level_order([5, 1, 8, None, None, 6, 9])
Solution().range_summation(t2, 6, 9)
print(level_order_vals(t2))   # [5, 1, 23, 6, 9]

# Edge cases
Solution().range_summation(None, 1, 5)            # no-op

# Single node within range
t3 = from_level_order([5])
Solution().range_summation(t3, 1, 10)
print(t3.val)                 # 5  (leaf node, no descendants)

# Range excludes all nodes
t4 = from_level_order([4, 2, 5, 1, 3, None, 6])
Solution().range_summation(t4, 7, 10)
print(level_order_vals(t4))   # [4, 2, 5, 1, 3, 6]  (unchanged)

# Range covers all nodes
t5 = from_level_order([2, 1, 3])
Solution().range_summation(t5, 1, 3)
print(level_order_vals(t5))   # [5, 1, 3]  (root updated to 2+1+3=6? actually 2+(1)+(3)=6 but note postorder: left=1, right=3 returned; root.val += 1+3 → 6; returns 6)
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
        private int rangeSummationHelper(TreeNode root, int low, int high) {

            // Base Case : if root is null return 0
            if (root == null) {
                return 0;
            }

            // If the node's value is less than the lower bound,
            // discard the left subtree and move to the right subtree
            if (root.val < low) {
                return rangeSummationHelper(root.right, low, high);
            }

            // If the node's value is greater than the upper bound,
            // discard the right subtree and move to the left subtree
            if (root.val > high) {
                return rangeSummationHelper(root.left, low, high);
            }

            // If the node's value is within the range [low, high],
            // recursively compute the sum of valid left and right subtrees
            int leftSum = rangeSummationHelper(root.left, low, high);
            int rightSum = rangeSummationHelper(root.right, low, high);

            // Add sum of in-range descendants to the current node's value
            root.val += leftSum + rightSum;

            // Return the updated value of the current node
            // (which now includes valid descendants)
            return root.val;
        }

        public void rangeSummation(TreeNode root, int low, int high) {
            rangeSummationHelper(root, low, high);
        }
    }

    public static void main(String[] args) {
        // Example 1
        TreeNode t1 = fromLevelOrder(4, 2, 5, 1, 3, null, 6);
        new Solution().rangeSummation(t1, 2, 5);
        System.out.println(levelOrderVals(t1));   // [14, 5, 5, 1, 3, 6]

        // Example 2
        TreeNode t2 = fromLevelOrder(5, 1, 8, null, null, 6, 9);
        new Solution().rangeSummation(t2, 6, 9);
        System.out.println(levelOrderVals(t2));   // [5, 1, 23, 6, 9]

        // Edge cases
        new Solution().rangeSummation(null, 1, 5);  // no-op

        // Single node within range
        TreeNode t3 = fromLevelOrder(5);
        new Solution().rangeSummation(t3, 1, 10);
        System.out.println(t3.val);               // 5

        // Range excludes all nodes
        TreeNode t4 = fromLevelOrder(4, 2, 5, 1, 3, null, 6);
        new Solution().rangeSummation(t4, 7, 10);
        System.out.println(levelOrderVals(t4));   // [4, 2, 5, 1, 3, 6]

        // Range covers all nodes [2,1,3]
        TreeNode t5 = fromLevelOrder(2, 1, 3);
        new Solution().rangeSummation(t5, 1, 3);
        System.out.println(levelOrderVals(t5));   // [6, 1, 3]
    }
}
```

</details>
