---
title: "Sum of Leaves"
summary: "See problem statement below."
prereqs:
  - 11-pattern-postorder-traversal-stateless/01-pattern
difficulty: easy
---

# Problem 1 — Sum of leaves

> Given the root, compute the sum of all leaf node values.

Base case: empty tree contributes 0. Leaf returns its own value. Internal node returns `sumOfLeaves(left) + sumOfLeaves(right)` — the node's own value doesn't enter (it's not a leaf).

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
    def sum_of_leaves(self, root: Optional[TreeNode]) -> int:

        # Base case: if the tree is empty
        if not root:
            return 0

        # If it's a leaf node, return its value
        if not root.left and not root.right:
            return root.val

        # Recursively sum up leaf nodes in left and right subtrees
        left_sum = self.sum_of_leaves(root.left)
        right_sum = self.sum_of_leaves(root.right)

        # Return the sum of leaf nodes in left and right subtrees
        return left_sum + right_sum


# Examples from the problem statement
print(Solution().sum_of_leaves(from_level_order([1, 2, 5, 7, None, None, 3])))  # 10
print(Solution().sum_of_leaves(from_level_order([1, 8, 4, None, None, 9, 7])))  # 24

# Edge cases
print(Solution().sum_of_leaves(None))                                             # 0
print(Solution().sum_of_leaves(from_level_order([7])))                            # 7
print(Solution().sum_of_leaves(from_level_order([1, 2, None, 3])))                # 3 (left-skew)
print(Solution().sum_of_leaves(from_level_order([1, None, 2, None, 3])))          # 3 (right-skew)
print(Solution().sum_of_leaves(from_level_order([1, 2, 3, 4, 5, 6, 7])))         # 22
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
        public int sumOfLeaves(TreeNode root) {

            // Base case: if the tree is empty
            if (root == null) {
                return 0;
            }

            // If it's a leaf node, return its value
            if (root.left == null && root.right == null) {
                return root.val;
            }

            // Recursively sum up leaf nodes in left and right subtrees
            int leftSum = sumOfLeaves(root.left);
            int rightSum = sumOfLeaves(root.right);

            // Return the sum of leaf nodes in left and right subtrees
            return leftSum + rightSum;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().sumOfLeaves(fromLevelOrder(1, 2, 5, 7, null, null, 3)));  // 10
        System.out.println(new Solution().sumOfLeaves(fromLevelOrder(1, 8, 4, null, null, 9, 7)));  // 24

        // Edge cases
        System.out.println(new Solution().sumOfLeaves(null));                                        // 0
        System.out.println(new Solution().sumOfLeaves(fromLevelOrder(7)));                           // 7
        System.out.println(new Solution().sumOfLeaves(fromLevelOrder(1, 2, null, 3)));               // 3 (left-skew)
        System.out.println(new Solution().sumOfLeaves(fromLevelOrder(1, null, 2, null, 3)));         // 3 (right-skew)
        System.out.println(new Solution().sumOfLeaves(fromLevelOrder(1, 2, 3, 4, 5, 6, 7)));        // 22
    }
}
```

</details>
