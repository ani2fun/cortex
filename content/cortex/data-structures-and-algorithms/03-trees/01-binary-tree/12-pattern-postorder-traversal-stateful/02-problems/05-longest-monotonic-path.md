---
title: "Longest Monotonic Path"
summary: "See problem statement below."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: hard
---

# Problem 5 — Longest monotonic path

> A *monotonic* path is one where every node has the same value. Return the longest such path's length (number of edges).

Same shape as diameter, with one twist: the height contribution from a child only counts if the child has the same value as the current node.

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

    # Global variable to keep track of max length
    maxLength: int = 0

    def longest_monotonic_path_helper(
        self, root: Optional[TreeNode]
    ) -> int:
        if not root:
            return 0

        # Recursively calculate the longest univalued path in the left
        # subtree
        leftLength = self.longest_monotonic_path_helper(root.left)

        # Recursively calculate the longest univalued path in the right
        # subtree
        rightLength = self.longest_monotonic_path_helper(root.right)

        leftArrow = 0
        rightArrow = 0

        # If the left child exists and has the same value as the current
        # node, extend the path to the left
        if root.left and root.left.val == root.val:
            leftArrow = leftLength + 1

        # If the right child exists and has the same value as the current
        # node, extend the path to the right
        if root.right and root.right.val == root.val:
            rightArrow = rightLength + 1

        # Update the maxLength if the combined path length is greater
        self.maxLength = max(self.maxLength, leftArrow + rightArrow)

        # Return the longest univalued path from the current node
        return max(leftArrow, rightArrow)

    def longest_monotonic_path(self, root: Optional[TreeNode]) -> int:
        self.longest_monotonic_path_helper(root)
        return self.maxLength


# Examples from the problem statement
print(Solution().longest_monotonic_path(from_level_order([1, 2, 5, 7, None, None, 3])))   # 0
print(Solution().longest_monotonic_path(from_level_order([3, 8, 1, 8, None, 1, 1])))      # 2

# Edge cases
print(Solution().longest_monotonic_path(None))                                             # 0
print(Solution().longest_monotonic_path(from_level_order([1])))                            # 0
print(Solution().longest_monotonic_path(from_level_order([5, 5, 5])))                      # 2 (both children match root)
print(Solution().longest_monotonic_path(from_level_order([5, 5, None, 5])))                # 2 (only-left skew all same)
print(Solution().longest_monotonic_path(from_level_order([1, 1, 1, 1, 1, 1, 1])))         # 4 (full matching tree)
print(Solution().longest_monotonic_path(from_level_order([1, 2, 3])))                      # 0 (no matches)
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

        // Global variable to keep track of max length
        private int maxLength = 0;

        private int longestMonotonicPathHelper(TreeNode root) {
            if (root == null) {
                return 0;
            }

            // Recursively calculate the longest univalued path in the left
            // subtree
            int leftLength = longestMonotonicPathHelper(root.left);

            // Recursively calculate the longest univalued path in the right
            // subtree
            int rightLength = longestMonotonicPathHelper(root.right);

            int leftArrow = 0;
            int rightArrow = 0;

            // If the left child exists and has the same value as the current
            // node, extend the path to the left
            if (root.left != null && root.left.val == root.val) {
                leftArrow = leftLength + 1;
            }

            // If the right child exists and has the same value as the
            // current node, extend the path to the right
            if (root.right != null && root.right.val == root.val) {
                rightArrow = rightLength + 1;
            }

            // Update the maxLength if the combined path length is greater
            maxLength = Math.max(maxLength, leftArrow + rightArrow);

            // Return the longest univalued path from the current node
            return Math.max(leftArrow, rightArrow);
        }

        public int longestMonotonicPath(TreeNode root) {
            longestMonotonicPathHelper(root);
            return maxLength;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().longestMonotonicPath(fromLevelOrder(1, 2, 5, 7, null, null, 3)));   // 0
        System.out.println(new Solution().longestMonotonicPath(fromLevelOrder(3, 8, 1, 8, null, 1, 1)));      // 2

        // Edge cases
        System.out.println(new Solution().longestMonotonicPath(null));                                         // 0
        System.out.println(new Solution().longestMonotonicPath(fromLevelOrder(1)));                            // 0
        System.out.println(new Solution().longestMonotonicPath(fromLevelOrder(5, 5, 5)));                      // 2
        System.out.println(new Solution().longestMonotonicPath(fromLevelOrder(5, 5, null, 5)));                // 2
        System.out.println(new Solution().longestMonotonicPath(fromLevelOrder(1, 1, 1, 1, 1, 1, 1)));         // 4
        System.out.println(new Solution().longestMonotonicPath(fromLevelOrder(1, 2, 3)));                      // 0
    }
}
```

</details>
