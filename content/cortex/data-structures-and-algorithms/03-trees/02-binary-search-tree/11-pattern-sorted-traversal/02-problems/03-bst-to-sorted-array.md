---
title: "BST to Sorted Array"
summary: "Given the root of a binary search tree, return a sorted array containing the values of every node."
prereqs:
  - 11-pattern-sorted-traversal/01-pattern
difficulty: medium
---

# BST to sorted array

## Problem Statement

Given the **root** of a binary search tree, return a sorted array containing the values of every node.

### Example 1

> - **Input:** `root = [4, 2, 5, 1, 3, null, 6]`
> - **Output:** `[1, 2, 3, 4, 5, 6]`

### Example 2

> - **Input:** `root = [9, 5, 10, 4, null, null, 11]`
> - **Output:** `[4, 5, 9, 10, 11]`

<details>
<summary><h2>The Strategy</h2></summary>


This is the canonical use of the pattern: **f** = "append `node.val` to the result list", **g** = identity. The in-order order *is* the sorted order, so emission == sorted output.

</details>
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
    def inorder(
        self, root: Optional[TreeNode], result: List[int]
    ) -> None:

        # Base case: If the node is None, return
        if root is None:
            return

        # Recursively traverse the left subtree
        self.inorder(root.left, result)

        # Visit the current node and add its value to the result list
        result.append(root.val)

        # Recursively traverse the right subtree
        self.inorder(root.right, result)

    def bst_to_sorted_array(self, root: Optional[TreeNode]) -> List[int]:
        result: List[int] = []

        # Call the helper function to perform inorder traversal
        self.inorder(root, result)

        # Return the result list containing inorder traversal elements
        return result


# Example 1
print(Solution().bst_to_sorted_array(
    from_level_order([4, 2, 5, 1, 3, None, 6])))   # [1, 2, 3, 4, 5, 6]

# Example 2
print(Solution().bst_to_sorted_array(
    from_level_order([9, 5, 10, 4, None, None, 11])))  # [4, 5, 9, 10, 11]

# Edge cases
print(Solution().bst_to_sorted_array(None))         # []
print(Solution().bst_to_sorted_array(
    from_level_order([5])))                         # [5]

# Two-node tree
print(Solution().bst_to_sorted_array(
    from_level_order([3, 1])))                      # [1, 3]

# Right-skew BST
print(Solution().bst_to_sorted_array(
    from_level_order([1, None, 2, None, 3])))       # [1, 2, 3]

# Left-skew BST
root_left = TreeNode(4)
root_left.left = TreeNode(3)
root_left.left.left = TreeNode(2)
root_left.left.left.left = TreeNode(1)
print(Solution().bst_to_sorted_array(root_left))    # [1, 2, 3, 4]
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
        private void inorder(TreeNode root, List<Integer> result) {

            // Base case: If the node is null, return
            if (root == null) {
                return;
            }

            // Recursively traverse the left subtree
            inorder(root.left, result);

            // Visit the current node and add its value to the result list
            result.add(root.val);

            // Recursively traverse the right subtree
            inorder(root.right, result);
        }

        public List<Integer> bstToSortedArray(TreeNode root) {
            List<Integer> result = new ArrayList<>();

            // Call the helper function to perform inorder traversal
            inorder(root, result);

            // Return the result list containing inorder traversal elements
            return result;
        }
    }

    public static void main(String[] args) {
        // Example 1
        System.out.println(new Solution().bstToSortedArray(
            fromLevelOrder(4, 2, 5, 1, 3, null, 6)));   // [1, 2, 3, 4, 5, 6]

        // Example 2
        System.out.println(new Solution().bstToSortedArray(
            fromLevelOrder(9, 5, 10, 4, null, null, 11)));  // [4, 5, 9, 10, 11]

        // Edge cases
        System.out.println(new Solution().bstToSortedArray(null));      // []
        System.out.println(new Solution().bstToSortedArray(
            fromLevelOrder(5)));                         // [5]

        // Two-node tree
        System.out.println(new Solution().bstToSortedArray(
            fromLevelOrder(3, 1)));                      // [1, 3]

        // Right-skew BST
        System.out.println(new Solution().bstToSortedArray(
            fromLevelOrder(1, null, 2, null, 3)));       // [1, 2, 3]

        // Left-skew BST
        TreeNode leftSkew = new TreeNode(4);
        leftSkew.left = new TreeNode(3);
        leftSkew.left.left = new TreeNode(2);
        leftSkew.left.left.left = new TreeNode(1);
        System.out.println(new Solution().bstToSortedArray(leftSkew));  // [1, 2, 3, 4]
    }
}
```

</details>
