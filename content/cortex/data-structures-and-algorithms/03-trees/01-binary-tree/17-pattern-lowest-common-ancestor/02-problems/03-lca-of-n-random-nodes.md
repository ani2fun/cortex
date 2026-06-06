---
title: "LCA of N Random Nodes"
summary: "See problem statement below."
prereqs:
  - 17-pattern-lowest-common-ancestor/01-pattern
difficulty: medium
---

# Problem 3 — LCA of N random nodes

> Given a list of nodes (possibly more than two), find the LCA of *all* of them.

Generalise the algorithm: instead of "is this node `A` or `B`?", check "is this node *in the set of targets*?". Use a hash set for O(1) lookup. The combine logic stays exactly the same.

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


def find(root, val):
    """Locate a node by value."""
    if root is None:
        return None
    if root.val == val:
        return root
    return find(root.left, val) or find(root.right, val)


class Solution:
    def lowest_common_ancestor(
        self, root: Optional[TreeNode], nodes: set
    ) -> Optional[TreeNode]:

        # If the root is null, return null
        if not root:
            return None

        # If the current node is part of the nodes set, return it
        if root in nodes:
            return root

        # Recursively search in the left and right subtrees
        left_lca = self.lowest_common_ancestor(root.left, nodes)
        right_lca = self.lowest_common_ancestor(root.right, nodes)

        # If both subtrees return a non-null value
        # the current node is the lowest common ancestor
        if left_lca and right_lca:
            return root

        # If only one subtree returns a non-null value, return that value
        return left_lca if left_lca else right_lca

    def random_lowest_common_ancestor(
        self, root: Optional[TreeNode], nodes: List[Optional[TreeNode]]
    ) -> Optional[TreeNode]:

        # Convert the list to a set for faster lookup
        nodeSet = set(nodes)

        # Find and return the lowest common ancestor
        return self.lowest_common_ancestor(root, nodeSet)


# Examples from the problem statement
root1 = from_level_order([1, 2, 3, 4, None, None, 7])
lca1 = Solution().random_lowest_common_ancestor(
    root1, [find(root1, 2), find(root1, 4), find(root1, 7)]
)
print(lca1.val)   # 1

root2 = from_level_order([1, 8, 4, None, None, 2, 7])
lca2 = Solution().random_lowest_common_ancestor(
    root2, [find(root2, 2), find(root2, 7)]
)
print(lca2.val)   # 4

# Edge cases
print(Solution().random_lowest_common_ancestor(None, []))              # None

root3 = from_level_order([1, 2, 3, 4, None, None, 7])                 # single node in list
lca3 = Solution().random_lowest_common_ancestor(root3, [find(root3, 4)])
print(lca3.val)   # 4

root4 = from_level_order([1, 2, 3, 4, None, None, 7])                 # all leaf nodes → LCA is root
lca4 = Solution().random_lowest_common_ancestor(
    root4, [find(root4, 4), find(root4, 7)]
)
print(lca4.val)   # 1

root5 = from_level_order([1, 8, 4, None, None, 2, 7])                 # three nodes, deep LCA
lca5 = Solution().random_lowest_common_ancestor(
    root5, [find(root5, 8), find(root5, 2), find(root5, 7)]
)
print(lca5.val)   # 1

root6 = from_level_order([1, 2, 3])
lca6 = Solution().random_lowest_common_ancestor(
    root6, [find(root6, 2), find(root6, 3)]
)
print(lca6.val)   # 1 (root)
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
        private TreeNode lowestCommonAncestor(
            TreeNode root,
            Set<TreeNode> nodes
        ) {

            // If the root is null, return null
            if (root == null) {
                return null;
            }

            // If the current node is part of the nodes set, return it
            if (nodes.contains(root)) {
                return root;
            }

            // Recursively search in the left and right subtrees
            TreeNode leftLCA = lowestCommonAncestor(root.left, nodes);
            TreeNode rightLCA = lowestCommonAncestor(root.right, nodes);

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

        public TreeNode randomLowestCommonAncestor(
            TreeNode root,
            List<TreeNode> nodes
        ) {

            // Convert the array to a HashSet for faster lookup
            Set<TreeNode> nodeSet = new HashSet<>();
            for (TreeNode node : nodes) {
                nodeSet.add(node);
            }

            // Find and return the lowest common ancestor
            return lowestCommonAncestor(root, nodeSet);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        TreeNode root1 = fromLevelOrder(1, 2, 3, 4, null, null, 7);
        System.out.println(new Solution().randomLowestCommonAncestor(
            root1, Arrays.asList(find(root1, 2), find(root1, 4), find(root1, 7))).val);  // 1

        TreeNode root2 = fromLevelOrder(1, 8, 4, null, null, 2, 7);
        System.out.println(new Solution().randomLowestCommonAncestor(
            root2, Arrays.asList(find(root2, 2), find(root2, 7))).val);  // 4

        // Edge cases
        System.out.println(new Solution().randomLowestCommonAncestor(null, new ArrayList<>()));  // null

        TreeNode root3 = fromLevelOrder(1, 2, 3, 4, null, null, 7);                             // single node
        System.out.println(new Solution().randomLowestCommonAncestor(
            root3, Arrays.asList(find(root3, 4))).val);  // 4

        TreeNode root4 = fromLevelOrder(1, 2, 3, 4, null, null, 7);                             // leaf nodes
        System.out.println(new Solution().randomLowestCommonAncestor(
            root4, Arrays.asList(find(root4, 4), find(root4, 7))).val);  // 1

        TreeNode root5 = fromLevelOrder(1, 8, 4, null, null, 2, 7);                             // three nodes
        System.out.println(new Solution().randomLowestCommonAncestor(
            root5, Arrays.asList(find(root5, 8), find(root5, 2), find(root5, 7))).val);  // 1

        TreeNode root6 = fromLevelOrder(1, 2, 3);
        System.out.println(new Solution().randomLowestCommonAncestor(
            root6, Arrays.asList(find(root6, 2), find(root6, 3))).val);  // 1
    }
}
```

</details>
