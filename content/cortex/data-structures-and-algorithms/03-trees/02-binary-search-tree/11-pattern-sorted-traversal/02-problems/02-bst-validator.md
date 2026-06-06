---
title: "BST Validator"
summary: "Given the root of a binary search tree, return true if the tree is a valid BST, false otherwise. A valid BST has these properties:"
prereqs:
  - 11-pattern-sorted-traversal/01-pattern
difficulty: medium
---

# BST validator

## Problem Statement

Given the **root** of a binary search tree, return `true` if the tree is a valid BST, `false` otherwise. A valid BST has these properties:

- Every node has a unique key.
- The left subtree contains only values strictly less than the node.
- The right subtree contains only values strictly greater than the node.
- Both subtrees are themselves BSTs.

### Example 1

> - **Input:** `root = [4, 2, 5, 1, 3, null, 6]`
> - **Output:** `true`

### Example 2

> - **Input:** `root = [9, 5, 12, 4, null, null, 11]`
> - **Output:** `false`
> - **Explanation:** Node `11` is in the right subtree of `12` but `11 < 12` — rule violated.

<details>
<summary><h2>The Strategy</h2></summary>


A valid BST has a **strictly increasing** in-order traversal. So this is just: walk in-order, keep the previous value, and at every step assert `prev < current`. The moment any pair fails, the tree is invalid.

This is dramatically simpler than the recursive `(min, max)` bounds technique you may have seen — the in-order trick reduces tree validity to *list monotonicity*, which is a one-liner.

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


class Solution:
    def __init__(self):

        # Variable to keep track of the validity of the BST
        self.is_valid = True

        # Reference to keep track of the previous node
        self.prev_node: Optional[TreeNode] = None

    def inorder(self, root: Optional[TreeNode]) -> None:
        if not root or not self.is_valid:
            return

        # Traverse left subtree
        self.inorder(root.left)

        # Current node must be greater than the prevNodeious one in
        # inorder
        if self.prev_node and root.val <= self.prev_node.val:
            self.is_valid = False
            return

        # Update prevNodeious node
        self.prev_node = root

        # Traverse right subtree
        self.inorder(root.right)

    def bst_validator(self, root: Optional[TreeNode]) -> bool:

        # Perform in-order traversal
        self.inorder(root)

        # Return the validity of the BST
        return self.is_valid


# Example 1: valid BST
print(Solution().bst_validator(
    from_level_order([4, 2, 5, 1, 3, None, 6])))   # True

# Example 2: invalid BST (11 < 12 but placed as right child)
print(Solution().bst_validator(
    from_level_order([9, 5, 12, 4, None, None, 11])))  # False

# Edge cases
print(Solution().bst_validator(None))              # True  (empty tree)
print(Solution().bst_validator(from_level_order([5])))  # True (single node)

# Left-skew valid BST: 1-2-3-4
root_skew = TreeNode(4)
root_skew.left = TreeNode(3)
root_skew.left.left = TreeNode(2)
root_skew.left.left.left = TreeNode(1)
print(Solution().bst_validator(root_skew))         # True

# Duplicate value makes it invalid
dup = TreeNode(5)
dup.left = TreeNode(5)
print(Solution().bst_validator(dup))               # False

# Subtree violation: right child less than root
bad = TreeNode(10)
bad.right = TreeNode(8)
print(Solution().bst_validator(bad))               # False
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

        // Variable to keep track of the validity of the BST
        private boolean isValid = true;

        // Reference to keep track of the previous node
        private TreeNode prevNode = null;

        private void inorder(TreeNode root) {
            if (root == null || !isValid) {
                return;
            }

            // Traverse left subtree
            inorder(root.left);

            // Current node must be greater than the prevNodeious one in
            // inorder
            if (prevNode != null && root.val <= prevNode.val) {
                isValid = false;
                return;
            }

            // Update prevNodeious node
            prevNode = root;

            // Traverse right subtree
            inorder(root.right);
        }

        public boolean bstValidator(TreeNode root) {

            // Perform in-order traversal
            inorder(root);

            // Return the validity of the BST
            return isValid;
        }
    }

    public static void main(String[] args) {
        // Example 1: valid BST
        System.out.println(new Solution().bstValidator(
            fromLevelOrder(4, 2, 5, 1, 3, null, 6)));   // true

        // Example 2: invalid BST (11 < 12 but placed as right child)
        System.out.println(new Solution().bstValidator(
            fromLevelOrder(9, 5, 12, 4, null, null, 11)));  // false

        // Edge cases
        System.out.println(new Solution().bstValidator(null));          // true
        System.out.println(new Solution().bstValidator(fromLevelOrder(5))); // true

        // Left-skew valid BST: 4-3-2-1
        TreeNode skew = new TreeNode(4);
        skew.left = new TreeNode(3);
        skew.left.left = new TreeNode(2);
        skew.left.left.left = new TreeNode(1);
        System.out.println(new Solution().bstValidator(skew));          // true

        // Duplicate value makes it invalid
        TreeNode dup = new TreeNode(5);
        dup.left = new TreeNode(5);
        System.out.println(new Solution().bstValidator(dup));           // false

        // Subtree violation: right child less than root
        TreeNode bad = new TreeNode(10);
        bad.right = new TreeNode(8);
        System.out.println(new Solution().bstValidator(bad));           // false
    }
}
```

</details>
