---
title: "Equal Evens-and-Odds Paths"
summary: "See problem statement below."
prereqs:
  - 14-pattern-root-to-leaf-path-stateful/01-pattern
difficulty: medium
---

# Problem 2 — Equal evens-and-odds paths

> Return all root-to-leaf paths where the number of even-valued nodes equals the number of odd-valued nodes.

Same shape as Problem 1, but the per-path bookkeeping is *two counters* (`evenCount`, `oddCount`) instead of one running sum. At each leaf, snapshot the path if the counts match.

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

    # To store the current path as we traverse
    def __init__(self):
        self.path: List[int] = []

    def equal_paths_helper(
        self,
        root: Optional[TreeNode],
        even_count: int,
        odd_count: int,
        result: List[List[int]],
    ) -> None:

        # If the root is null, there is no path, so return
        if root is None:
            return

        # Add the current node to the path
        self.path.append(root.val)

        # If the current node is even, increment even count
        if root.val % 2 == 0:
            even_count += 1

        # Else, increment odd count
        else:
            odd_count += 1

        # If current node is a leaf, check if even and odd counts are
        # equal
        if root.left is None and root.right is None:

            # If the counts are equal, add the current path to the result
            if even_count == odd_count:
                result.append(self.path.copy())

        # Recursively traverse left and right subtrees
        self.equal_paths_helper(
            root.left, even_count, odd_count, result
        )
        self.equal_paths_helper(
            root.right, even_count, odd_count, result
        )

        # Backtrack by removing the current node from the path
        self.path.pop()

    def equal_paths(
        self, root: Optional[TreeNode]
    ) -> List[List[int]]:

        # To store all valid paths
        result: List[List[int]] = []

        # Start the recursive search from the root node with initial even
        # and odd counts as 0
        self.equal_paths_helper(root, 0, 0, result)
        return result


# Examples from the problem statement
print(Solution().equal_paths(from_level_order([1, 2, 4])))                     # [[1, 2], [1, 4]]
print(Solution().equal_paths(from_level_order([1, 8, 4, None, None, 2, 4])))   # [[1, 8]]

# Edge cases
print(Solution().equal_paths(None))                                              # []
print(Solution().equal_paths(from_level_order([1])))                             # [] (odd only, 1 odd 0 even)
print(Solution().equal_paths(from_level_order([2])))                             # [] (even only)
print(Solution().equal_paths(from_level_order([1, 2])))                          # [[1, 2]] (1 odd, 1 even)
print(Solution().equal_paths(from_level_order([1, 2, 3])))                       # [[1, 2]] (1+2: 1 odd 1 even; 1+3: 2 odd 0 even)
print(Solution().equal_paths(from_level_order([2, 1, 3, None, None, None, 4])))  # [[2, 3, 4]] (2+3+4: 2 even 1 odd; 2+1: 1 each yes; 2+3+4: 2 even 1 odd no)
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

        private void equalPathsHelper(
            TreeNode root,
            int evenCount,
            int oddCount,
            List<List<Integer>> result
        ) {

            // If the root is null, there is no path, so return
            if (root == null) {
                return;
            }

            // Add the current node to the path
            path.add(root.val);

            // If the current node is even, increment even count
            if (root.val % 2 == 0) {
                evenCount++;
            }

            // Else, increment odd count
            else {
                oddCount++;
            }

            // If current node is a leaf, check if even and odd counts are
            // equal
            if (root.left == null && root.right == null) {

                // If the counts are equal, add the current path to the
                // result
                if (evenCount == oddCount) {
                    result.add(new ArrayList<>(path));
                }
            }

            // Recursively traverse left and right subtrees
            equalPathsHelper(root.left, evenCount, oddCount, result);
            equalPathsHelper(root.right, evenCount, oddCount, result);

            // Backtrack by removing the current node from the path
            path.remove(path.size() - 1);
        }

        public List<List<Integer>> equalPaths(TreeNode root) {

            // To store all valid paths
            List<List<Integer>> result = new ArrayList<>();

            // Start the recursive search from the root node with initial
            // even and odd counts as 0
            equalPathsHelper(root, 0, 0, result);
            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().equalPaths(fromLevelOrder(1, 2, 4)));                     // [[1, 2], [1, 4]]
        System.out.println(new Solution().equalPaths(fromLevelOrder(1, 8, 4, null, null, 2, 4)));   // [[1, 8]]

        // Edge cases
        System.out.println(new Solution().equalPaths(null));                                         // []
        System.out.println(new Solution().equalPaths(fromLevelOrder(1)));                            // []
        System.out.println(new Solution().equalPaths(fromLevelOrder(2)));                            // []
        System.out.println(new Solution().equalPaths(fromLevelOrder(1, 2)));                         // [[1, 2]]
        System.out.println(new Solution().equalPaths(fromLevelOrder(1, 2, 3)));                      // [[1, 2]]
    }
}
```

</details>
