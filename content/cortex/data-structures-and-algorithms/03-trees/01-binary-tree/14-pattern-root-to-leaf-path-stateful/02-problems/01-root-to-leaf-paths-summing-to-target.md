---
title: "Root-to-Leaf Paths Summing to Target"
summary: "See problem statement below."
prereqs:
  - 14-pattern-root-to-leaf-path-stateful/01-pattern
difficulty: medium
---

# Problem 1 — Root-to-leaf paths summing to target

> Return *all* root-to-leaf paths whose node values sum to `target`.

The accumulator is *the path so far* (push-pop) plus a *countdown of the target* (passed by value). Each call subtracts the current node's value from `target` before recursing; at a leaf the path qualifies when the leaf's own value equals the remaining `target` — i.e. the whole path summed to the original target. Snapshot the path when that holds.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from typing import List, Optional


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

        # To store the current path as we traverse
        self.path: List[int] = []

    def root_to_leaf_paths_helper(
        self,
        root: Optional[TreeNode],
        target: int,
        result: List[List[int]],
    ) -> None:

        # If the root is null, there is no path, so return
        if root is None:
            return

        # Add the current node to the path
        self.path.append(root.val)

        # If it is a leaf node and the target matches the node value,
        # add the current path to the result
        if (
            root.left is None
            and root.right is None
            and root.val == target
        ):
            result.append(self.path.copy())

        # Otherwise, subtract the current node's value from target and
        # continue traversal to left and right subtrees
        target -= root.val

        # Recursively search in left and right subtrees with updated
        # target
        self.root_to_leaf_paths_helper(root.left, target, result)
        self.root_to_leaf_paths_helper(root.right, target, result)

        # Backtrack by removing the current node from the path
        self.path.pop()

    def root_to_leaf_paths(
        self, root: Optional[TreeNode], target: int
    ) -> List[List[int]]:

        # To store all valid paths
        result: List[List[int]] = []

        # Start the recursive search from the root node
        self.root_to_leaf_paths_helper(root, target, result)

        # Return the list of all valid paths
        return result


# Examples from the problem statement
print(Solution().root_to_leaf_paths(from_level_order([1, 2, 3, 4, None, None, 7]), 11))   # [[1, 3, 7]]
print(Solution().root_to_leaf_paths(from_level_order([1, 8, 4, None, None, 2, 4]), 13))   # []

# Edge cases
print(Solution().root_to_leaf_paths(None, 0))                                               # []
print(Solution().root_to_leaf_paths(from_level_order([5]), 5))                              # [[5]]
print(Solution().root_to_leaf_paths(from_level_order([5]), 0))                              # []
print(Solution().root_to_leaf_paths(from_level_order([1, 2, 3]), 3))                        # [[1, 2]]
print(Solution().root_to_leaf_paths(from_level_order([1, 2, 3]), 4))                        # [[1, 3]]
print(Solution().root_to_leaf_paths(from_level_order([1, 2, 2]), 3))                        # [[1, 2], [1, 2]] (two identical paths)
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

        // To store the current path as we traverse
        private List<Integer> path = new ArrayList<>();

        private void rootToLeafPathsHelper(
            TreeNode root,
            int target,
            List<List<Integer>> result
        ) {

            // If the root is null, there is no path, so return
            if (root == null) {
                return;
            }

            // Add the current node to the path
            path.add(root.val);

            // If it is a leaf node and the target matches the node value,
            // add the current path to the result
            if (
                root.left == null && root.right == null && root.val == target
            ) {
                result.add(new ArrayList<>(path));
            }

            // Otherwise, subtract the current node's value from target and
            // continue traversal to left and right subtrees
            target -= root.val;

            // Recursively search in left and right subtrees with updated
            // target
            rootToLeafPathsHelper(root.left, target, result);
            rootToLeafPathsHelper(root.right, target, result);

            // Backtrack by removing the current node from the path
            path.remove(path.size() - 1);
        }

        public List<List<Integer>> rootToLeafPaths(
            TreeNode root,
            int target
        ) {

            // To store all valid paths
            List<List<Integer>> result = new ArrayList<>();

            // Start the recursive search from the root node
            rootToLeafPathsHelper(root, target, result);

            // Return the list of all valid paths
            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().rootToLeafPaths(fromLevelOrder(1, 2, 3, 4, null, null, 7), 11));   // [[1, 3, 7]]
        System.out.println(new Solution().rootToLeafPaths(fromLevelOrder(1, 8, 4, null, null, 2, 4), 13));   // []

        // Edge cases
        System.out.println(new Solution().rootToLeafPaths(null, 0));                                          // []
        System.out.println(new Solution().rootToLeafPaths(fromLevelOrder(5), 5));                             // [[5]]
        System.out.println(new Solution().rootToLeafPaths(fromLevelOrder(5), 0));                             // []
        System.out.println(new Solution().rootToLeafPaths(fromLevelOrder(1, 2, 3), 3));                       // [[1, 2]]
        System.out.println(new Solution().rootToLeafPaths(fromLevelOrder(1, 2, 3), 4));                       // [[1, 3]]
        System.out.println(new Solution().rootToLeafPaths(fromLevelOrder(1, 2, 2), 3));                       // [[1, 2], [1, 2]]
    }
}
```

</details>
