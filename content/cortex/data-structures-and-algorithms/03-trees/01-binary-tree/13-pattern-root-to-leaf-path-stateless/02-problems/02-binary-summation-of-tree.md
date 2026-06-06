---
title: "Binary Summation of Tree"
summary: "See problem statement below."
prereqs:
  - 13-pattern-root-to-leaf-path-stateless/01-pattern
difficulty: medium
---

# Problem 2 — Binary summation of tree

> Each node's value is `0` or `1`. Each root-to-leaf path is a binary number (most significant bit at root). Return the sum of these binary numbers, in decimal.
>
> **Example:** `[1, 0, 1, 1, null, null, 1]` → paths `[1,0,1,1]=11(₂)=11(₁₀)`... wait, the example output is 12. Let me recompute. The tree:
> ```
>     1
>    / \
>   0   1
>  / \   \
> 1       1
> ```
> Paths from root to leaves:
> - `1 → 0 → 1` = binary `101` = 5
> - `1 → 1 → 1` = binary `111` = 7
>
> Sum = 5 + 7 = **12**.

The accumulator is the *binary number so far* — at each node, shift left and OR in the current bit (`acc = (acc << 1) | node.val`). At a leaf, *return the accumulator itself*. Internal nodes sum their children.

<details>
<summary><h2>Solution</h2></summary>



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


class Solution:
    def binary_summation_of_tree_helper(
        self, root: Optional[TreeNode], current_sum: int
    ) -> int:
        if not root:
            return 0

        # Update the current sum by shifting left and adding current
        # node's value
        current_sum = (current_sum << 1) | root.val

        # If it's a leaf node, return the current sum
        if not root.left and not root.right:
            return current_sum

        # Recursively sum up the left and right subtrees
        left_sum = self.binary_summation_of_tree_helper(
            root.left, current_sum
        )
        right_sum = self.binary_summation_of_tree_helper(
            root.right, current_sum
        )

        # Return the total sum from both left and right subtrees
        return left_sum + right_sum

    def binary_summation_of_tree(self, root: Optional[TreeNode]) -> int:

        # Start binary_summation_of_tree_helper with current_sum = 0
        return self.binary_summation_of_tree_helper(root, 0)


# Examples from the problem statement
print(Solution().binary_summation_of_tree(from_level_order([1, 0, 1, 1, None, None, 1])))   # 12
print(Solution().binary_summation_of_tree(from_level_order([0, 1, 0, None, None, 1, 0])))   # 2

# Edge cases
print(Solution().binary_summation_of_tree(None))                                              # 0
print(Solution().binary_summation_of_tree(from_level_order([1])))                             # 1
print(Solution().binary_summation_of_tree(from_level_order([0])))                             # 0
print(Solution().binary_summation_of_tree(from_level_order([1, 1])))                          # 3 (11=3)
print(Solution().binary_summation_of_tree(from_level_order([1, 0, 0])))                       # 4 (10 + 10 = 2+2=4)
print(Solution().binary_summation_of_tree(from_level_order([1, 1, 1, 0, 1, 0, 1])))          # 22 (110+111+101+111... = 6+7+5+7...)
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
        private int binarySummationOfTreeHelper(
            TreeNode root,
            int currentSum
        ) {
            if (root == null) {
                return 0;
            }

            // Update the current sum by shifting left and adding current
            // node's value
            currentSum = (currentSum << 1) | root.val;

            // If it's a leaf node, return the current sum
            if (root.left == null && root.right == null) {
                return currentSum;
            }

            // Recursively sum up the left and right subtrees
            int leftSum = binarySummationOfTreeHelper(root.left, currentSum);
            int rightSum = binarySummationOfTreeHelper(
                root.right,
                currentSum
            );

            // Return the total sum from both left and right subtrees
            return leftSum + rightSum;
        }

        public int binarySummationOfTree(TreeNode root) {

            // Start binarySummationOfTreeHelper with currentSum = 0
            return binarySummationOfTreeHelper(root, 0);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().binarySummationOfTree(fromLevelOrder(1, 0, 1, 1, null, null, 1)));   // 12
        System.out.println(new Solution().binarySummationOfTree(fromLevelOrder(0, 1, 0, null, null, 1, 0)));   // 2

        // Edge cases
        System.out.println(new Solution().binarySummationOfTree(null));                                         // 0
        System.out.println(new Solution().binarySummationOfTree(fromLevelOrder(1)));                            // 1
        System.out.println(new Solution().binarySummationOfTree(fromLevelOrder(0)));                            // 0
        System.out.println(new Solution().binarySummationOfTree(fromLevelOrder(1, 1)));                         // 3
        System.out.println(new Solution().binarySummationOfTree(fromLevelOrder(1, 0, 0)));                      // 4
    }
}
```

</details>
