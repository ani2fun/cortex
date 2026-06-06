---
title: "Even Path"
summary: "See problem statement below."
prereqs:
  - 13-pattern-root-to-leaf-path-stateless/01-pattern
difficulty: medium
---

# Problem 3 — Even path

> Return `true` if there's at least one root-to-leaf path where *every* value is even.

The accumulator is a *boolean*: "has the path so far been all-even?". Update at each node: `still_even = previously_even AND (current is even)`. At a leaf, return `still_even`. Combine with OR.

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
    def even_path_helper(
        self, root: Optional[TreeNode], even_so_far: int
    ) -> bool:

        # Base case: if the current node is null, return false
        if root is None:
            return False

        # Update current path status: 1 if path so far is all even and
        # current node is even
        current_status = even_so_far and (root.val % 2 == 0)

        # If this is a leaf, check if current path is valid
        if root.left is None and root.right is None:
            return current_status

        # Check left and right subtrees for valid paths
        left_path = self.even_path_helper(root.left, current_status)
        right_path = self.even_path_helper(root.right, current_status)

        return left_path or right_path

    def even_path(self, root: Optional[TreeNode]) -> bool:
        if root is None:
            return False

        # Root path is valid if root is even
        return self.even_path_helper(root, 1)


# Examples from the problem statement
print(Solution().even_path(from_level_order([2, 4, 6, 8, None, None, 9])))   # True
print(Solution().even_path(from_level_order([1, 8, 4, None, None, 2, 7])))   # False

# Edge cases
print(Solution().even_path(None))                                              # False
print(Solution().even_path(from_level_order([2])))                             # True (single even leaf)
print(Solution().even_path(from_level_order([1])))                             # False (single odd leaf)
print(Solution().even_path(from_level_order([2, 2, 2])))                       # True (balanced all-even)
print(Solution().even_path(from_level_order([2, 4, None, 6])))                 # True (only-left all-even)
print(Solution().even_path(from_level_order([2, 3, 4])))                       # True (right path is 2->4)
print(Solution().even_path(from_level_order([2, 3, 5])))                       # False (both leaves via odd intermediary)
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
        private boolean evenPathHelper(TreeNode root, int evenSoFar) {

            // Base case: if the current node is null, return false
            if (root == null) {
                return false;
            }

            // Update current path status: 1 if path so far is all even and
            // current node is even
            int currentStatus = evenSoFar & (root.val % 2 == 0 ? 1 : 0);

            // If this is a leaf, check if current path is valid
            if (root.left == null && root.right == null) {
                return currentStatus == 1;
            }

            // Check left and right subtrees for valid paths
            boolean leftPath = evenPathHelper(root.left, currentStatus);
            boolean rightPath = evenPathHelper(root.right, currentStatus);

            return leftPath || rightPath;
        }

        public boolean evenPath(TreeNode root) {
            if (root == null) {
                return false;
            }

            // Root path is valid if root is even
            return evenPathHelper(root, 1);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().evenPath(fromLevelOrder(2, 4, 6, 8, null, null, 9)));   // true
        System.out.println(new Solution().evenPath(fromLevelOrder(1, 8, 4, null, null, 2, 7)));   // false

        // Edge cases
        System.out.println(new Solution().evenPath(null));                                          // false
        System.out.println(new Solution().evenPath(fromLevelOrder(2)));                             // true
        System.out.println(new Solution().evenPath(fromLevelOrder(1)));                             // false
        System.out.println(new Solution().evenPath(fromLevelOrder(2, 2, 2)));                       // true
        System.out.println(new Solution().evenPath(fromLevelOrder(2, 4, null, 6)));                 // true
        System.out.println(new Solution().evenPath(fromLevelOrder(2, 3, 4)));                       // true
        System.out.println(new Solution().evenPath(fromLevelOrder(2, 3, 5)));                       // false
    }
}
```

</details>
