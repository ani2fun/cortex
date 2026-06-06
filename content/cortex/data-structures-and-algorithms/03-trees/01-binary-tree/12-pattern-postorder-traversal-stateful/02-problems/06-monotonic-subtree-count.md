---
title: "Monotonic Subtree Count"
summary: "See problem statement below."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: hard
---

# Problem 6 — Monotonic subtree count

> Count subtrees that are *entirely* mono-valued — every node in the subtree has the same value.

Each call returns whether *its* subtree is mono-valued; along the way, increment a global counter when it is. A subtree is mono-valued iff: both children's subtrees are mono-valued, *and* both children (if they exist) have the same value as the current node.

<details>
<summary><h2>Solution</h2></summary>



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
    def __init__(self):

        # To store the number of monotonic subtrees
        self.subtree_count: int = 0

    def is_monotonic_subtree(self, root: Optional[TreeNode]) -> bool:

        # An empty node is trivially monotonic
        if not root:
            return True

        # Check if the left child is monotonic
        left_monotonic = self.is_monotonic_subtree(root.left)

        # Check if the right child is monotonic
        right_monotonic = self.is_monotonic_subtree(root.right)

        # If either left or right subtree is not monotonic, return False
        if not left_monotonic or not right_monotonic:
            return False

        # If the left child exists and does not have the same value,
        # return False
        if root.left and root.left.val != root.val:
            return False

        # If the right child exists and does not have the same value,
        # return False
        if root.right and root.right.val != root.val:
            return False

        # This node and its children form a monotonic subtree
        self.subtree_count += 1
        return True

    def monotonic_subtree_count(self, root: Optional[TreeNode]) -> int:
        self.is_monotonic_subtree(root)
        return self.subtree_count


# Examples from the problem statement
print(Solution().monotonic_subtree_count(from_level_order([1, 1, 5, 1, None, None, 5])))   # 4
print(Solution().monotonic_subtree_count(from_level_order([3, 8, 1, 8, None, 1, 1])))      # 5

# Edge cases
print(Solution().monotonic_subtree_count(None))                                              # 0
print(Solution().monotonic_subtree_count(from_level_order([7])))                             # 1 (single leaf)
print(Solution().monotonic_subtree_count(from_level_order([1, 2, 3])))                       # 0 (no monotonic subtrees)
print(Solution().monotonic_subtree_count(from_level_order([2, 2, 2])))                       # 3 (all three)
print(Solution().monotonic_subtree_count(from_level_order([1, 1, 1, 1, 1, 1, 1])))          # 7 (all subtrees monotonic)
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

        // To store the number of monotonic subtrees
        private int subtreeCount = 0;

        private boolean isMonotonicSubtree(TreeNode root) {

            // An empty node is trivially monotonic
            if (root == null) {
                return true;
            }

            // Check if the left child is monotonic
            boolean leftMonotonic = isMonotonicSubtree(root.left);

            // Check if the right child is monotonic
            boolean rightMonotonic = isMonotonicSubtree(root.right);

            // If either left or right subtree is not monotonic, return false
            if (!leftMonotonic || !rightMonotonic) {
                return false;
            }

            // If the left child exists and does not have the same value,
            // return false
            if (root.left != null && root.left.val != root.val) {
                return false;
            }

            // If the right child exists and does not have the same value,
            // return false
            if (root.right != null && root.right.val != root.val) {
                return false;
            }

            // This node and its children form a monotonic subtree
            subtreeCount++;
            return true;
        }

        public int monotonicSubtreeCount(TreeNode root) {
            isMonotonicSubtree(root);
            return subtreeCount;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().monotonicSubtreeCount(fromLevelOrder(1, 1, 5, 1, null, null, 5)));   // 4
        System.out.println(new Solution().monotonicSubtreeCount(fromLevelOrder(3, 8, 1, 8, null, 1, 1)));      // 5

        // Edge cases
        System.out.println(new Solution().monotonicSubtreeCount(null));                                         // 0
        System.out.println(new Solution().monotonicSubtreeCount(fromLevelOrder(7)));                            // 1
        System.out.println(new Solution().monotonicSubtreeCount(fromLevelOrder(1, 2, 3)));                      // 0
        System.out.println(new Solution().monotonicSubtreeCount(fromLevelOrder(2, 2, 2)));                      // 3
        System.out.println(new Solution().monotonicSubtreeCount(fromLevelOrder(1, 1, 1, 1, 1, 1, 1)));         // 7
    }
}
```

</details>
