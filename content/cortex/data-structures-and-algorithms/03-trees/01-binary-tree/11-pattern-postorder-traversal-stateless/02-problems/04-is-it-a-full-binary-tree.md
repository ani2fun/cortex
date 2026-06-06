---
title: "Is It a Full Binary Tree"
summary: "See problem statement below."
prereqs:
  - 11-pattern-postorder-traversal-stateless/01-pattern
difficulty: medium
---

# Problem 4 — Is it a full binary tree?

> Return `true` iff every node has either zero or two children.

Three cases at each node:

- Empty tree → vacuously full → `true`.
- Leaf (both children null) → full → `true`.
- Exactly one child null → *not* full → `false`.
- Both children present → recurse and require both subtrees full.

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
    def full_binary_tree(self, root: Optional[TreeNode]) -> bool:

        # An empty tree is a full binary tree
        if not root:
            return True

        # A node with no children is a full binary tree
        if not root.left and not root.right:
            return True

        # A node with only one child is not a full binary tree
        if not root.left or not root.right:
            return False

        # Check if the left and right subtrees are also full binary trees
        is_subtree_left_full = self.full_binary_tree(root.left)
        is_subtree_right_full = self.full_binary_tree(root.right)

        # Return true if both subtrees are full binary trees
        return is_subtree_left_full and is_subtree_right_full


# Examples from the problem statement
print(Solution().full_binary_tree(from_level_order([1, 2, 3, None, None, 2])))     # False
print(Solution().full_binary_tree(from_level_order([1, 8, 4, None, None, 3, 5])))  # True

# Edge cases
print(Solution().full_binary_tree(None))                                             # True
print(Solution().full_binary_tree(from_level_order([5])))                            # True
print(Solution().full_binary_tree(from_level_order([1, 2, 3])))                      # True
print(Solution().full_binary_tree(from_level_order([1, 2, None])))                   # False (only left child)
print(Solution().full_binary_tree(from_level_order([1, 2, 3, 4, 5, 6, 7])))         # True (perfect)
print(Solution().full_binary_tree(from_level_order([1, 2, 3, 4, 5])))               # False (node 3 has no children)
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
        public boolean fullBinaryTree(TreeNode root) {

            // An empty tree is a full binary tree
            if (root == null) {
                return true;
            }

            // A node with no children is a full binary tree
            if (root.left == null && root.right == null) {
                return true;
            }

            // A node with only one child is not a full binary tree
            if (root.left == null || root.right == null) {
                return false;
            }

            // Check if the left and right subtrees are also full binary
            // trees
            boolean isLeftSubtreeFull = fullBinaryTree(root.left);
            boolean isRightSubtreeFull = fullBinaryTree(root.right);

            // Return true if both subtrees are full binary trees
            return isLeftSubtreeFull && isRightSubtreeFull;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().fullBinaryTree(fromLevelOrder(1, 2, 3, null, null, 2)));     // false
        System.out.println(new Solution().fullBinaryTree(fromLevelOrder(1, 8, 4, null, null, 3, 5)));  // true

        // Edge cases
        System.out.println(new Solution().fullBinaryTree(null));                                        // true
        System.out.println(new Solution().fullBinaryTree(fromLevelOrder(5)));                           // true
        System.out.println(new Solution().fullBinaryTree(fromLevelOrder(1, 2, 3)));                     // true
        System.out.println(new Solution().fullBinaryTree(fromLevelOrder(1, 2, null)));                  // false (only left child)
        System.out.println(new Solution().fullBinaryTree(fromLevelOrder(1, 2, 3, 4, 5, 6, 7)));        // true (perfect)
        System.out.println(new Solution().fullBinaryTree(fromLevelOrder(1, 2, 3, 4, 5)));              // false (node 3 has no children)
    }
}
```

</details>
