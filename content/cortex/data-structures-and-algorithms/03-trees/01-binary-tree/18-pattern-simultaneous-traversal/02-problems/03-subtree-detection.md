---
title: "Subtree Detection"
summary: "See problem statement below."
prereqs:
  - 18-pattern-simultaneous-traversal/01-pattern
difficulty: medium
---

# Problem 3 — Subtree detection

> Given trees A and B, return `true` iff the *whole* of B occurs somewhere inside A as an exact subtree.

Combine two patterns: an *outer* recursion walking A (looking for a place where the comparison succeeds), and an *inner* recursion that runs the identical-trees check between the current A-node and B's root.

The complexity is **O(|A| · |B|)** worst case — every node in A might be the start of an identical-check that walks the whole of B. Faster O(|A| + |B|) algorithms exist using string-hashing on serialised trees, but the naive recursive version is the right interview answer for clarity.

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
    def identical_trees(
        self, root_a: Optional[TreeNode], root_b: Optional[TreeNode]
    ) -> bool:

        # If both trees are empty, they are identical
        if not root_a and not root_b:
            return True

        # If only one tree is empty, they are not identical
        if not root_a or not root_b:
            return False

        # If the values of the current nodes are different, they are not
        # identical
        if root_a.val != root_b.val:
            return False

        # Recursively check if the left and right subtrees are identical
        left_subtrees_are_identical = self.identical_trees(
            root_a.left, root_b.left
        )
        right_subtrees_are_identical = self.identical_trees(
            root_a.right, root_b.right
        )

        # Return True if both subtrees are identical
        return (
            left_subtrees_are_identical and right_subtrees_are_identical
        )

    def subtree_detection(
        self, root_a: Optional[TreeNode], root_b: Optional[TreeNode]
    ) -> bool:

        # If the main tree is empty, root_b cannot be a subtree
        if not root_a:
            return False

        # If the trees are identical, root_b is a subtree
        if self.identical_trees(root_a, root_b):
            return True

        # Recursively check if root_b is a subtree of the left or right
        # subtree
        is_a_subtree_of_left_subtree = self.subtree_detection(
            root_a.left, root_b
        )
        is_a_subtree_of_right_subtree = self.subtree_detection(
            root_a.right, root_b
        )

        # Return true if root_b is a subtree of the left or right subtree
        return (
            is_a_subtree_of_left_subtree or is_a_subtree_of_right_subtree
        )


# Examples from the problem statement
print(Solution().subtree_detection(
    from_level_order([1, 8, 5, 4, 2, 3, 9]),
    from_level_order([5, 3, 9])
))   # True

print(Solution().subtree_detection(
    from_level_order([1, 8, 4, None, None, 2, 7]),
    from_level_order([1, 8, 4])
))   # False

# Edge cases
print(Solution().subtree_detection(None, None))                              # False (empty main)
print(Solution().subtree_detection(TreeNode(1), None))                       # True (empty subB matches nothing... actually identical_trees(1,None)=False, recurse left/right=False... returns False) — wait: identical_trees(TreeNode(1), None) = False since root_b is falsy and root_a is not. Then recurse left(None,None)→ identical_trees(None,None)=True → subtree_detection returns True)
print(Solution().subtree_detection(TreeNode(1), TreeNode(1)))                # True (identical single nodes)
print(Solution().subtree_detection(
    from_level_order([1, 2, 3, 4, 5]),
    from_level_order([2, 4, 5])
))   # True (left subtree match)
print(Solution().subtree_detection(
    from_level_order([1, 2, 3]),
    from_level_order([4])
))   # False (value not found)
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
        private boolean identicalTrees(TreeNode rootA, TreeNode rootB) {

            // If both trees are empty, they are identical
            if (rootA == null && rootB == null) {
                return true;
            }

            // If only one tree is empty, they are not identical
            if (rootA == null || rootB == null) {
                return false;
            }

            // If the values of the current nodes are different, they are not
            // identical
            if (rootA.val != rootB.val) {
                return false;
            }

            // Recursively check the left and right subtrees are identical
            boolean leftSubtreesAreIdentical = identicalTrees(
                rootA.left,
                rootB.left
            );
            boolean rightSubtreesAreIdentical = identicalTrees(
                rootA.right,
                rootB.right
            );

            // Return true if both subtrees are identical
            return leftSubtreesAreIdentical && rightSubtreesAreIdentical;
        }

        public boolean subtreeDetection(TreeNode rootA, TreeNode rootB) {

            // If the main tree is empty, rootB cannot be a subtree
            if (rootA == null) {
                return false;
            }

            // If the trees are identical, rootB is a subtree
            if (identicalTrees(rootA, rootB)) {
                return true;
            }

            // Recursively check if rootB is a subtree of the left or right
            // subtree
            boolean isASubtreeOfLeftSubtree = subtreeDetection(
                rootA.left,
                rootB
            );
            boolean isASubtreeOfRightSubtree = subtreeDetection(
                rootA.right,
                rootB
            );

            // Return true if rootB is a subtree of the left or right subtree
            return isASubtreeOfLeftSubtree || isASubtreeOfRightSubtree;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().subtreeDetection(
            fromLevelOrder(1, 8, 5, 4, 2, 3, 9),
            fromLevelOrder(5, 3, 9)
        ));   // true

        System.out.println(new Solution().subtreeDetection(
            fromLevelOrder(1, 8, 4, null, null, 2, 7),
            fromLevelOrder(1, 8, 4)
        ));   // false

        // Edge cases
        System.out.println(new Solution().subtreeDetection(null, null));                              // false
        System.out.println(new Solution().subtreeDetection(new TreeNode(1), new TreeNode(1)));        // true
        System.out.println(new Solution().subtreeDetection(
            fromLevelOrder(1, 2, 3, 4, 5),
            fromLevelOrder(2, 4, 5)
        ));   // true (left subtree match)
        System.out.println(new Solution().subtreeDetection(
            fromLevelOrder(1, 2, 3),
            fromLevelOrder(4)
        ));   // false (value not found)
        System.out.println(new Solution().subtreeDetection(
            fromLevelOrder(1, 2, 3),
            fromLevelOrder(1, 2, 3)
        ));   // true (identical trees)
    }
}
```

</details>
