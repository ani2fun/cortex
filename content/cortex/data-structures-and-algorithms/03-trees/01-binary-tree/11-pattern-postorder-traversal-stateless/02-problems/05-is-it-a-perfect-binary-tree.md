---
title: "Is It a Perfect Binary Tree"
summary: "See problem statement below."
prereqs:
  - 11-pattern-postorder-traversal-stateless/01-pattern
difficulty: medium
---

# Problem 5 — Is it a perfect binary tree?

> Return `true` iff every internal node has two children **and** every leaf is at the same depth.

A clean two-pass approach:

1. Find the depth of the leftmost leaf — that's where every leaf must sit.
2. Recursively check: every leaf is at that depth; every internal node has two children.

A one-pass approach also exists (return both `(isPerfect, height)` from each call), but that's the *stateful* postorder pattern from the next lesson. The two-pass version below is pure stateless.

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
    def find_depth(self, root: Optional[TreeNode]) -> int:
        depth = 0
        while root:
            depth += 1
            root = root.left
        return depth

    def is_perfect_binary_tree(
        self, root: Optional[TreeNode], depth: int, level: int
    ) -> bool:

        # An empty tree is a perfect binary tree
        if not root:
            return True

        # If it is a leaf node, check if it is at the correct depth
        if not root.left and not root.right:
            return depth == level + 1

        # If an internal node has only one child, it's not a perfect
        # binary tree
        if not root.left or not root.right:
            return False

        # Recursively check the left and right subtrees
        is_left_subtree_perfect = self.is_perfect_binary_tree(
            root.left, depth, level + 1
        )
        is_right_subtree_perfect = self.is_perfect_binary_tree(
            root.right, depth, level + 1
        )

        # Return true if both subtrees are perfect
        return is_left_subtree_perfect and is_right_subtree_perfect

    def perfect_binary_tree(self, root: Optional[TreeNode]) -> bool:

        # An empty tree is a perfect binary tree
        if not root:
            return True

        # Find the depth of the leftmost leaf
        depth = self.find_depth(root)

        # Check if the tree is perfect
        return self.is_perfect_binary_tree(root, depth, 0)


# Examples from the problem statement
print(Solution().perfect_binary_tree(from_level_order([1, 2, 3, 4, None, None, 7])))  # False
print(Solution().perfect_binary_tree(from_level_order([1, 8, 4, 3, 5, 2, 7])))        # True

# Edge cases
print(Solution().perfect_binary_tree(None))                                             # True
print(Solution().perfect_binary_tree(from_level_order([5])))                            # True
print(Solution().perfect_binary_tree(from_level_order([1, 2, 3])))                      # True
print(Solution().perfect_binary_tree(from_level_order([1, 2, None])))                   # False (only left)
print(Solution().perfect_binary_tree(from_level_order([1, 2, 3, 4, 5, 6, 7])))         # True
print(Solution().perfect_binary_tree(from_level_order([1, 2, 3, 4, 5])))               # False (unequal leaves)
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
        private int findDepth(TreeNode root) {
            int depth = 0;
            while (root != null) {
                depth++;
                root = root.left;
            }
            return depth;
        }

        private boolean isPerfectBinaryTree(
            TreeNode root,
            int depth,
            int level
        ) {

            // An empty tree is a perfect binary tree
            if (root == null) {
                return true;
            }

            // If it is a leaf node, check if it is at the correct depth
            if (root.left == null && root.right == null) {
                return depth == level + 1;
            }

            // If an internal node has only one child, it's not a perfect
            // binary tree
            if (root.left == null || root.right == null) {
                return false;
            }

            // Recursively check the left and right subtrees
            boolean isLeftSubtreePerfect = isPerfectBinaryTree(
                root.left,
                depth,
                level + 1
            );
            boolean isRightSubtreePerfect = isPerfectBinaryTree(
                root.right,
                depth,
                level + 1
            );

            // Return true if both subtrees are perfect
            return isLeftSubtreePerfect && isRightSubtreePerfect;
        }

        public boolean perfectBinaryTree(TreeNode root) {

            // An empty tree is a perfect binary tree
            if (root == null) {
                return true;
            }

            // Find the depth of the leftmost leaf
            int depth = findDepth(root);

            // Check if the tree is perfect
            return isPerfectBinaryTree(root, depth, 0);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().perfectBinaryTree(fromLevelOrder(1, 2, 3, 4, null, null, 7)));  // false
        System.out.println(new Solution().perfectBinaryTree(fromLevelOrder(1, 8, 4, 3, 5, 2, 7)));        // true

        // Edge cases
        System.out.println(new Solution().perfectBinaryTree(null));                                        // true
        System.out.println(new Solution().perfectBinaryTree(fromLevelOrder(5)));                           // true
        System.out.println(new Solution().perfectBinaryTree(fromLevelOrder(1, 2, 3)));                     // true
        System.out.println(new Solution().perfectBinaryTree(fromLevelOrder(1, 2, null)));                  // false (only left)
        System.out.println(new Solution().perfectBinaryTree(fromLevelOrder(1, 2, 3, 4, 5, 6, 7)));        // true
        System.out.println(new Solution().perfectBinaryTree(fromLevelOrder(1, 2, 3, 4, 5)));              // false (unequal leaves)
    }
}
```

</details>
