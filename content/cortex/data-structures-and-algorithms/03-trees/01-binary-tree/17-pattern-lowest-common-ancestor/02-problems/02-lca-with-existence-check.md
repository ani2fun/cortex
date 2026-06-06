---
title: "LCA with Existence Check"
summary: "See problem statement below."
prereqs:
  - 17-pattern-lowest-common-ancestor/01-pattern
difficulty: medium
---

# Problem 2 — LCA with existence check

> Same as Problem 1, except now there's no guarantee that *both* nodes are actually in the tree. If either is missing, return `null`.

The classical algorithm has a subtle pitfall here: if only `nodeA` exists in the tree (and `nodeB` doesn't), the algorithm returns `nodeA` — which is *wrong* (the answer should be `null`). The fix: do a *separate existence pass* for both nodes first, then run the LCA only if both exist.

This adds one O(N) pre-pass, keeping overall complexity at O(N).

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


def find(root, val):
    """Locate a node by value."""
    if root is None:
        return None
    if root.val == val:
        return root
    return find(root.left, val) or find(root.right, val)


class Solution:
    def node_exists(
        self, root: Optional[TreeNode], target: Optional[TreeNode]
    ) -> bool:

        # If the root is null, the target node does not exist
        if not root:
            return False

        # If the current node is the target node, return true
        if root == target:
            return True

        # Recursively search in the left and right subtrees
        node_exists_in_left_subtree = self.node_exists(root.left, target)
        node_exists_in_right_subtree = self.node_exists(
            root.right, target
        )

        # Return true if the target node exists in either subtree
        return (
            node_exists_in_left_subtree or node_exists_in_right_subtree
        )

    def lowest_common_ancestor(
        self,
        root: Optional[TreeNode],
        node_a: Optional[TreeNode],
        node_b: Optional[TreeNode],
    ) -> Optional[TreeNode]:

        # If the root is null, return null
        if not root:
            return None

        # If the current node is equal to either nodeA or nodeB
        # return the current node
        if root == node_a or root == node_b:
            return root

        # Recursively search in the left and right subtrees
        left_lca = self.lowest_common_ancestor(root.left, node_a, node_b)
        right_lca = self.lowest_common_ancestor(
            root.right, node_a, node_b
        )

        # If both subtrees return a non-null value
        # the current node is the lowest common ancestor
        if left_lca and right_lca:
            return root

        # If only one subtree returns a non-null value, return that value
        return left_lca if left_lca else right_lca

    def lowest_common_ancestor_ii(
        self,
        root: Optional[TreeNode],
        node_a: Optional[TreeNode],
        node_b: Optional[TreeNode],
    ) -> Optional[TreeNode]:

        # If any input is null, return null
        if not root or not node_a or not node_b:
            return None

        # Check if both nodes exist in the tree
        if not self.node_exists(root, node_a) or not self.node_exists(
            root, node_b
        ):
            return None

        return self.lowest_common_ancestor(root, node_a, node_b)


# Examples from the problem statement
root1 = from_level_order([1, 2, 3, 4, None, None, 7])
lca1 = Solution().lowest_common_ancestor_ii(root1, find(root1, 4), find(root1, 7))
print(lca1.val)   # 1

root2 = from_level_order([1, 8, 4, None, None, 2, 7])
ghost = TreeNode(9)  # node not in tree
print(Solution().lowest_common_ancestor_ii(root2, find(root2, 2), ghost))  # None

# Edge cases
print(Solution().lowest_common_ancestor_ii(None, None, None))              # None

root3 = from_level_order([1, 2, 3, 4, None, None, 7])                     # LCA is root
lca3 = Solution().lowest_common_ancestor_ii(root3, find(root3, 2), find(root3, 3))
print(lca3.val)   # 1

root4 = from_level_order([1, 2, 3, 4, None, None, 7])                     # one is ancestor of the other
lca4 = Solution().lowest_common_ancestor_ii(root4, find(root4, 2), find(root4, 4))
print(lca4.val)   # 2

root5 = from_level_order([1, 8, 4, None, None, 2, 7])                     # leaf siblings
lca5 = Solution().lowest_common_ancestor_ii(root5, find(root5, 2), find(root5, 7))
print(lca5.val)   # 4

root6 = TreeNode(1)                                                         # single node, both same
print(Solution().lowest_common_ancestor_ii(root6, root6, root6).val)       # 1
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

    static TreeNode find(TreeNode root, int val) {
        if (root == null) return null;
        if (root.val == val) return root;
        TreeNode left = find(root.left, val);
        return left != null ? left : find(root.right, val);
    }

    static class Solution {
        private boolean nodeExists(TreeNode root, TreeNode target) {

            // If the root is null, the target node does not exist
            if (root == null) {
                return false;
            }

            // If the current node is the target node, return true
            if (root == target) {
                return true;
            }

            // Recursively search in the left and right subtrees
            boolean nodeExistsInLeftSubtree = nodeExists(root.left, target);
            boolean nodeExistsInRightSubtree = nodeExists(
                root.right,
                target
            );

            // Return true if the target node exists in either subtree
            return nodeExistsInLeftSubtree || nodeExistsInRightSubtree;
        }

        private TreeNode lowestCommonAncestor(
            TreeNode root,
            TreeNode nodeA,
            TreeNode nodeB
        ) {

            // If the root is null, return null
            if (root == null) {
                return null;
            }

            // If the current node is equal to either nodeA or nodeB
            // return the current node
            if (root == nodeA || root == nodeB) {
                return root;
            }

            // Recursively search in the left and right subtrees
            TreeNode leftLCA = lowestCommonAncestor(root.left, nodeA, nodeB);
            TreeNode rightLCA = lowestCommonAncestor(
                root.right,
                nodeA,
                nodeB
            );

            // If both subtrees return a non-null value
            // the current node is the lowest common ancestor
            if (leftLCA != null && rightLCA != null) {
                return root;
            }

            // If only one subtree returns a non-null value, return that
            // value
            if (leftLCA != null) {
                return leftLCA;
            }

            return rightLCA;
        }

        public TreeNode lowestCommonAncestorII(
            TreeNode root,
            TreeNode nodeA,
            TreeNode nodeB
        ) {

            // If any input is null, return null
            if (root == null || nodeA == null || nodeB == null) {
                return null;
            }

            // Check if both nodes exist in the tree
            if (!nodeExists(root, nodeA) || !nodeExists(root, nodeB)) {
                return null;
            }

            return lowestCommonAncestor(root, nodeA, nodeB);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        TreeNode root1 = fromLevelOrder(1, 2, 3, 4, null, null, 7);
        System.out.println(new Solution().lowestCommonAncestorII(root1, find(root1, 4), find(root1, 7)).val);  // 1

        TreeNode root2 = fromLevelOrder(1, 8, 4, null, null, 2, 7);
        TreeNode ghost = new TreeNode(9);  // node not in tree
        System.out.println(new Solution().lowestCommonAncestorII(root2, find(root2, 2), ghost));  // null

        // Edge cases
        System.out.println(new Solution().lowestCommonAncestorII(null, null, null));               // null

        TreeNode root3 = fromLevelOrder(1, 2, 3, 4, null, null, 7);                               // LCA is root
        System.out.println(new Solution().lowestCommonAncestorII(root3, find(root3, 2), find(root3, 3)).val);  // 1

        TreeNode root4 = fromLevelOrder(1, 2, 3, 4, null, null, 7);                               // one is ancestor
        System.out.println(new Solution().lowestCommonAncestorII(root4, find(root4, 2), find(root4, 4)).val);  // 2

        TreeNode root5 = fromLevelOrder(1, 8, 4, null, null, 2, 7);                               // leaf siblings
        System.out.println(new Solution().lowestCommonAncestorII(root5, find(root5, 2), find(root5, 7)).val);  // 4

        TreeNode root6 = new TreeNode(1);                                                          // single node
        System.out.println(new Solution().lowestCommonAncestorII(root6, root6, root6).val);       // 1
    }
}
```

</details>
