---
title: "Duplicate Paths"
summary: "See problem statement below."
prereqs:
  - 14-pattern-root-to-leaf-path-stateful/01-pattern
difficulty: medium
---

# Problem 3 — Duplicate paths

> Return all root-to-leaf paths that appear *more than once* in the tree (i.e. two different leaves produce the same value sequence).

Two ingredients: the push-pop path discipline, plus a **hash map of path-string → count**. At each leaf, serialise the path into a hash-friendly key (e.g. comma-joined string), bump its count, and record the path *exactly once* — when the count first hits 2.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from typing import List, Optional, Dict


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

        # To store frequency of each root-to-leaf path (serialized as a
        # string)
        self.path_count: Dict[str, int] = {}

    def serialize_path(self, path: List[int]) -> str:

        # Join all elements of the path with commas
        return ",".join(map(str, path))

    def duplicate_paths_helper(
        self, root: Optional[TreeNode], result: List[List[int]]
    ) -> None:

        # If the root is null, there is no path, so return
        if root is None:
            return

        # Add the current node to the path
        self.path.append(root.val)

        # If it's a leaf, serialize and check frequency
        if root.left is None and root.right is None:

            # Serialize current path
            serialized_path = self.serialize_path(self.path)

            # Increment frequency count for this path
            self.path_count[serialized_path] = (
                self.path_count.get(serialized_path, 0) + 1
            )

            # If path occurs exactly twice, record it as duplicate
            if self.path_count[serialized_path] == 2:
                result.append(self.path.copy())

        # Recursively traverse left and right subtrees
        self.duplicate_paths_helper(root.left, result)
        self.duplicate_paths_helper(root.right, result)

        # Backtrack by removing the current node from the path
        self.path.pop()

    def duplicate_paths(
        self, root: Optional[TreeNode]
    ) -> List[List[int]]:

        # To store all valid paths
        result: List[List[int]] = []

        # Start the recursive search from the root node
        self.duplicate_paths_helper(root, result)

        # Return the list of all valid paths
        return result


# Examples from the problem statement
print(Solution().duplicate_paths(from_level_order([1, 2, 2])))                     # [[1, 2]]
print(Solution().duplicate_paths(from_level_order([1, 8, 4, None, None, 2, 4])))   # []

# Edge cases
print(Solution().duplicate_paths(None))                                              # []
print(Solution().duplicate_paths(from_level_order([5])))                             # [] (single node, can't duplicate)
print(Solution().duplicate_paths(from_level_order([1, 1, 1])))                       # [[1, 1]] (both paths are [1,1])
print(Solution().duplicate_paths(from_level_order([1, 2, 3])))                       # [] (different paths)
print(Solution().duplicate_paths(from_level_order([1, 2, 2, 3, None, None, 3])))     # [[1, 2, 3]] (left and right paths match)
```

```java run viz=binary-tree viz-root=root
import java.util.*;
import java.util.stream.Collectors;

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

        // To store frequency of each root-to-leaf path (serialized as a
        // string)
        private Map<String, Integer> pathCount = new HashMap<>();

        private String serializePath(List<Integer> path) {

            // Join all elements of the path with commas
            return path
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        }

        private void duplicatePathsHelper(
            TreeNode root,
            List<List<Integer>> result
        ) {

            // If the root is null, there is no path, so return
            if (root == null) {
                return;
            }

            // Add the current node to the path
            path.add(root.val);

            // If it's a leaf, serialize and check frequency
            if (root.left == null && root.right == null) {

                // Serialize current path
                String serializedPath = serializePath(path);

                // Increment frequency count for this path
                pathCount.put(
                    serializedPath,
                    pathCount.getOrDefault(serializedPath, 0) + 1
                );

                // If path occurs exactly twice, record it as duplicate
                if (pathCount.get(serializedPath) == 2) {
                    result.add(new ArrayList<>(path));
                }
            }

            // Recursively traverse left and right subtrees
            duplicatePathsHelper(root.left, result);
            duplicatePathsHelper(root.right, result);

            // Backtrack by removing the current node from the path
            path.remove(path.size() - 1);
        }

        public List<List<Integer>> duplicatePaths(TreeNode root) {

            // To store all valid paths
            List<List<Integer>> result = new ArrayList<>();

            // Start the recursive search from the root node
            duplicatePathsHelper(root, result);

            // Return the list of all valid paths
            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().duplicatePaths(fromLevelOrder(1, 2, 2)));                     // [[1, 2]]
        System.out.println(new Solution().duplicatePaths(fromLevelOrder(1, 8, 4, null, null, 2, 4)));   // []

        // Edge cases
        System.out.println(new Solution().duplicatePaths(null));                                         // []
        System.out.println(new Solution().duplicatePaths(fromLevelOrder(5)));                            // []
        System.out.println(new Solution().duplicatePaths(fromLevelOrder(1, 1, 1)));                      // [[1, 1]]
        System.out.println(new Solution().duplicatePaths(fromLevelOrder(1, 2, 3)));                      // []
        System.out.println(new Solution().duplicatePaths(fromLevelOrder(1, 2, 2, 3, null, null, 3)));    // [[1, 2, 3]]
    }
}
```

</details>
