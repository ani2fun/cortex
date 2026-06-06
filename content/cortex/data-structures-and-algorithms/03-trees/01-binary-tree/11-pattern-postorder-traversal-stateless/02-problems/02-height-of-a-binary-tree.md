---
title: "Height of a Binary Tree"
summary: "See problem statement below."
prereqs:
  - 11-pattern-postorder-traversal-stateless/01-pattern
difficulty: easy
---

# Problem 2 — Height of a binary tree

> Compute the height of the tree (number of nodes along the longest root-to-leaf path).

Base case: empty tree has height 0 (under the *node-counting* convention used in this problem). Each internal node returns `max(height(left), height(right)) + 1`. The root's answer is the tree's height.

> **Note on conventions:** This problem uses the *node-counting* convention (empty = 0, single node = 1). Lesson 1 used the *edge-counting* convention (empty = -1, single node = 0). Both are common; *always read the problem carefully* and pick base cases that make the recurrence consistent.

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
    def height_of_binary_tree(self, root: Optional[TreeNode]) -> int:

        # Empty tree has height 0
        if root is None:
            return 0

        # Recursively calculate the height of the left and right subtrees
        left_height = self.height_of_binary_tree(root.left)
        right_height = self.height_of_binary_tree(root.right)

        # Return the maximum height among the left and right subtrees
        # plus 1 for the current node
        return max(left_height, right_height) + 1


# Examples from the problem statement
print(Solution().height_of_binary_tree(from_level_order([1, 2, 3, 4, None, None, 7])))  # 3
print(Solution().height_of_binary_tree(from_level_order([1, 8, 4, None, None, 2, 7])))  # 3

# Edge cases
print(Solution().height_of_binary_tree(None))                                            # 0
print(Solution().height_of_binary_tree(from_level_order([5])))                           # 1
print(Solution().height_of_binary_tree(from_level_order([1, 2, None, 3])))               # 3 (left-skew)
print(Solution().height_of_binary_tree(from_level_order([1, None, 2, None, 3])))         # 3 (right-skew)
print(Solution().height_of_binary_tree(from_level_order([1, 2, 3, 4, 5, 6, 7])))        # 3 (balanced)
print(Solution().height_of_binary_tree(from_level_order([1, 2])))                        # 2
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
        public int heightOfBinaryTree(TreeNode root) {

            // Empty tree has height 0
            if (root == null) {
                return 0;
            }

            // Recursively calculate the height of the left and right
            // subtrees
            int leftHeight = heightOfBinaryTree(root.left);
            int rightHeight = heightOfBinaryTree(root.right);

            // Return the maximum height among the left and right subtrees
            // plus 1 for the current node
            return Math.max(leftHeight, rightHeight) + 1;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().heightOfBinaryTree(fromLevelOrder(1, 2, 3, 4, null, null, 7)));  // 3
        System.out.println(new Solution().heightOfBinaryTree(fromLevelOrder(1, 8, 4, null, null, 2, 7)));  // 3

        // Edge cases
        System.out.println(new Solution().heightOfBinaryTree(null));                                        // 0
        System.out.println(new Solution().heightOfBinaryTree(fromLevelOrder(5)));                           // 1
        System.out.println(new Solution().heightOfBinaryTree(fromLevelOrder(1, 2, null, 3)));               // 3 (left-skew)
        System.out.println(new Solution().heightOfBinaryTree(fromLevelOrder(1, null, 2, null, 3)));         // 3 (right-skew)
        System.out.println(new Solution().heightOfBinaryTree(fromLevelOrder(1, 2, 3, 4, 5, 6, 7)));        // 3 (balanced)
        System.out.println(new Solution().heightOfBinaryTree(fromLevelOrder(1, 2)));                        // 2
    }
}
```

</details>
