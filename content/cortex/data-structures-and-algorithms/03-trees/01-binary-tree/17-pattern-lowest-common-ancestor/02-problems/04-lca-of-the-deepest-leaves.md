---
title: "LCA of the Deepest Leaves"
summary: "See problem statement below."
prereqs:
  - 17-pattern-lowest-common-ancestor/01-pattern
difficulty: hard
---

# Problem 4 — LCA of the deepest leaves

> Find the LCA of all the *deepest leaves* in the tree.

Two-pass: first do a level-order traversal to find the deepest leaves; then run the N-node LCA on that set.

A more elegant *one-pass* solution exists using the stateful postorder pattern from lesson 11 — return `(deepest depth, LCA so far)` from each subtree, and combine at each node. We'll stick with the two-pass version for clarity; the one-pass version is a good exercise.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from queue import Queue
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
    def find_deepest_leaves(self, root: Optional[TreeNode]) -> list:

        # Variable to store the deepest leaves
        deepest_leaves = []

        if not root:
            return deepest_leaves

        queue = Queue()
        queue.put(root)

        # Loop through each level in the tree
        while not queue.empty():

            # Get the size of the current level
            level_size = queue.qsize()

            # Reset deepestLeaves for the current level
            deepest_leaves.clear()

            # Loop through each node in the current level
            for _ in range(level_size):

                # Get the front node in the queue and remove it
                node = queue.get()

                # Add its value to the deepestLeaves
                deepest_leaves.append(node)

                # Add the node's children to the queue if they exist
                if node.left:
                    queue.put(node.left)

                if node.right:
                    queue.put(node.right)

        # The last computed deepestLeaves is for the deepest level
        return deepest_leaves

    def random_lowest_common_ancestor(
        self, root: Optional[TreeNode], nodes: set
    ) -> Optional[TreeNode]:

        # If the root is null, return null
        if not root:
            return None

        # If the current node is part of the nodes set, return it
        if root in nodes:
            return root

        # Recursively search in the left and right subtrees
        leftLCA = self.random_lowest_common_ancestor(root.left, nodes)
        rightLCA = self.random_lowest_common_ancestor(root.right, nodes)

        # If both subtrees return a non-null value
        # the current node is the lowest common ancestor
        if leftLCA and rightLCA:
            return root

        # If only one subtree returns a non-null value, return that value
        return leftLCA if leftLCA else rightLCA

    def deepest_lowest_common_ancestor(
        self, root: Optional[TreeNode]
    ) -> Optional[TreeNode]:
        deepest_leaves = self.find_deepest_leaves(root)

        # Convert the list to a set for faster lookup
        node_set = set(deepest_leaves)

        # Find and return the lowest common ancestor
        return self.random_lowest_common_ancestor(root, node_set)


# Examples from the problem statement
root1 = from_level_order([1, 2, 3, 4, 6, None, 7])
print(Solution().deepest_lowest_common_ancestor(root1).val)   # 1

root2 = from_level_order([1, 8, 4, None, None, 2, 7])
print(Solution().deepest_lowest_common_ancestor(root2).val)   # 4

# Edge cases
print(Solution().deepest_lowest_common_ancestor(None))        # None

root3 = TreeNode(1)                                           # single node
print(Solution().deepest_lowest_common_ancestor(root3).val)   # 1

root4 = from_level_order([1, 2, None, 3])                    # left skew, deepest = 3
print(Solution().deepest_lowest_common_ancestor(root4).val)   # 3

root5 = from_level_order([1, 2, 3])                          # balanced, two leaves at same depth
print(Solution().deepest_lowest_common_ancestor(root5).val)   # 1

root6 = from_level_order([1, 2, 3, 4, 5, None, None])       # one side deeper
print(Solution().deepest_lowest_common_ancestor(root6).val)   # 2
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
        private List<TreeNode> findDeepestLeaves(TreeNode root) {
            List<TreeNode> deepestLeaves = new ArrayList<>();
            if (root == null) {
                return deepestLeaves;
            }

            Queue<TreeNode> queue = new LinkedList<>();
            queue.add(root);

            // Loop through each level in the tree
            while (!queue.isEmpty()) {
                int levelSize = queue.size();

                // Reset the deepestLeaves list for the current level
                deepestLeaves.clear();

                // Loop through each node in the current level
                for (int i = 0; i < levelSize; i++) {
                    TreeNode node = queue.poll();
                    deepestLeaves.add(node);

                    // Add the node's children to the queue if they exist
                    if (node.left != null) {
                        queue.add(node.left);
                    }

                    if (node.right != null) {
                        queue.add(node.right);
                    }
                }
            }

            // The last computed deepestLeaves is for the deepest level
            return deepestLeaves;
        }

        private TreeNode randomLowestCommonAncestor(
            TreeNode root,
            Set<TreeNode> nodes
        ) {
            if (root == null) {
                return null;
            }

            // If the current node is part of the nodes set, return it
            if (nodes.contains(root)) {
                return root;
            }

            // Recursively search in the left and right subtrees
            TreeNode leftLCA = randomLowestCommonAncestor(root.left, nodes);
            TreeNode rightLCA = randomLowestCommonAncestor(
                root.right,
                nodes
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

        public TreeNode deepestLowestCommonAncestor(TreeNode root) {
            List<TreeNode> deepestLeaves = findDeepestLeaves(root);

            // Convert the list to a set for faster lookup
            Set<TreeNode> nodeSet = new HashSet<>(deepestLeaves);

            // Find and return the lowest common ancestor
            return randomLowestCommonAncestor(root, nodeSet);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        TreeNode root1 = fromLevelOrder(1, 2, 3, 4, 6, null, 7);
        System.out.println(new Solution().deepestLowestCommonAncestor(root1).val);   // 1

        TreeNode root2 = fromLevelOrder(1, 8, 4, null, null, 2, 7);
        System.out.println(new Solution().deepestLowestCommonAncestor(root2).val);   // 4

        // Edge cases
        System.out.println(new Solution().deepestLowestCommonAncestor(null));        // null

        TreeNode root3 = new TreeNode(1);                                            // single node
        System.out.println(new Solution().deepestLowestCommonAncestor(root3).val);   // 1

        TreeNode root4 = fromLevelOrder(1, 2, null, 3);                             // left skew
        System.out.println(new Solution().deepestLowestCommonAncestor(root4).val);   // 3

        TreeNode root5 = fromLevelOrder(1, 2, 3);                                   // balanced two leaves
        System.out.println(new Solution().deepestLowestCommonAncestor(root5).val);   // 1

        TreeNode root6 = fromLevelOrder(1, 2, 3, 4, 5, null, null);                // one side deeper
        System.out.println(new Solution().deepestLowestCommonAncestor(root6).val);   // 2
    }
}
```

</details>
