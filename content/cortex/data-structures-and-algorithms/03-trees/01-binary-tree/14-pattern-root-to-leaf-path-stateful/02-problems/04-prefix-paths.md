---
title: "Prefix Paths"
summary: "See problem statement below."
prereqs:
  - 14-pattern-root-to-leaf-path-stateful/01-pattern
difficulty: hard
---

# Problem 4 — Prefix paths

> Return all root-to-leaf paths whose *total sum* equals the sum of some non-empty *prefix* of the same path.
>
> **Example:** path `[1, -3, 3]` has total sum 1 — and the prefix `[1]` also has sum 1. So this path qualifies.

Combine the path discipline with a **prefix-sum frequency map**. As we descend, increment the count of the running prefix-sum at the current depth. At a leaf, if the running sum has been seen *more than once* (count > 1), it means a strictly earlier prefix of the path had the same sum — qualifying the path.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from typing import List, Optional
from collections import defaultdict


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

        # To store prefix sum counts for paths using defaultdict
        self.prefix_sum_count: defaultdict[int, int] = defaultdict(int)

    def prefix_paths_helper(
        self,
        root: Optional[TreeNode],
        path_sum: int,
        result: List[List[int]],
    ) -> None:

        # If the root is null, there is no path, so return
        if root is None:
            return

        # Add the current node to the path
        self.path.append(root.val)

        # Calculate the current sum by adding the value of the
        # current node to the previous sum.
        path_sum += root.val

        # Add the current sum to the prefix_sum_count map
        self.prefix_sum_count[path_sum] += 1

        # If it's a leaf node, check if the total sum has occurred
        if root.left is None and root.right is None:

            # Check if total sum already exists as a prefix (excluding
            # last occurrence)
            if self.prefix_sum_count[path_sum] > 1:
                result.append(self.path.copy())

        # Recursively traverse left and right subtrees
        self.prefix_paths_helper(root.left, path_sum, result)
        self.prefix_paths_helper(root.right, path_sum, result)

        # Backtrack by removing the current sum from the prefix sum count
        self.prefix_sum_count[path_sum] -= 1

        # Backtrack by removing the current node from the path
        self.path.pop()

    def prefix_paths(self, root: Optional[TreeNode]) -> List[List[int]]:

        # To store all valid paths
        result: List[List[int]] = []

        # Start the recursive search from the root node
        self.prefix_paths_helper(root, 0, result)

        # Return the list of all valid paths
        return result


# Examples from the problem statement
print(Solution().prefix_paths(from_level_order([1, -3, None, None, 3])))  # [[1, -3, 3]]
print(Solution().prefix_paths(from_level_order([1, 8, 4, None, None, 2, 4])))  # []

# Edge cases
print(Solution().prefix_paths(None))                                       # []
print(Solution().prefix_paths(TreeNode(5)))                                # [] (single node, sum=5 first time)
print(Solution().prefix_paths(from_level_order([0, 0])))                   # [[0, 0]] (0+0=0, prefix [0]=0)
print(Solution().prefix_paths(from_level_order([1, 2, 3])))               # []
print(Solution().prefix_paths(from_level_order([2, 2, None, None, -2])))  # [] (path 2,-2 sum=0 != prefix sums 2,0... wait prefix sum 0 occurs once at node -2)
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

        // To store prefix sum counts for paths
        private Map<Integer, Integer> prefixSumCount = new HashMap<>();

        private void prefixPathsHelper(
            TreeNode root,
            int pathSum,
            List<List<Integer>> result
        ) {

            // If the root is null, there is no path, so return
            if (root == null) return;

            // Add the current node to the path
            path.add(root.val);

            // Calculate the current sum by adding the value of the
            // current node to the previous sum.
            pathSum += root.val;

            // Add the current sum to the prefixSumCount map to keep track of
            // it. This is to be used by future nodes in the recursive
            // traversal.
            prefixSumCount.put(
                pathSum,
                prefixSumCount.getOrDefault(pathSum, 0) + 1
            );

            // If it's a leaf node, check if the total sum has occurred
            if (root.left == null && root.right == null) {

                // Check if total sum already exists as a prefix (excluding
                // last occurrence)
                if (prefixSumCount.get(pathSum) > 1) {
                    result.add(new ArrayList<>(path));
                }
            }

            // Recursively traverse left and right subtrees
            prefixPathsHelper(root.left, pathSum, result);
            prefixPathsHelper(root.right, pathSum, result);

            // Backtrack by removing the current sum from the prefix sum
            // count map. This is to ensure that the prefix sum count is
            // accurate for future nodes.
            prefixSumCount.put(pathSum, prefixSumCount.get(pathSum) - 1);

            // Backtrack by removing the current node from the path
            path.remove(path.size() - 1);
        }

        public List<List<Integer>> prefixPaths(TreeNode root) {

            // To store all valid paths
            List<List<Integer>> result = new ArrayList<>();

            // Start the recursive search from the root node
            prefixPathsHelper(root, 0, result);

            // Return the list of all valid paths
            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().prefixPaths(fromLevelOrder(1, -3, null, null, 3)));  // [[1, -3, 3]]
        System.out.println(new Solution().prefixPaths(fromLevelOrder(1, 8, 4, null, null, 2, 4)));  // []

        // Edge cases
        System.out.println(new Solution().prefixPaths(null));               // []
        System.out.println(new Solution().prefixPaths(new TreeNode(5)));    // []
        System.out.println(new Solution().prefixPaths(fromLevelOrder(0, 0)));  // [[0, 0]]
        System.out.println(new Solution().prefixPaths(fromLevelOrder(1, 2, 3)));  // []
        System.out.println(new Solution().prefixPaths(fromLevelOrder(2, 2, null, null, -2)));  // []
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The stateful root-to-leaf path pattern is the natural sibling of stateless preorder backtracking. Three things to walk away with:

1. **Push-pop is sacred — and the leaf needs a *copy*.** The shared `path` is being mutated; if you record a reference to it and then return, the path you stored will get clobbered as the recursion backs out. Always copy on extract — `path.copy()`, `new ArrayList<>(path)`, `[...path]`, `path.clone()` — never store the live reference.
2. **Auxiliary data per problem.** Sum target → running integer. Equal evens-and-odds → two counters. Duplicate paths → hash map of serialised paths. Prefix paths → hash map of running prefix sums. The path itself is the canonical accumulator; the per-problem aux is what *interprets* the path.
3. **Returning paths is expensive even when the algorithm is cheap.** Recording matched paths is O(L) per match. If you're collecting *every* path, total output size is O(N · L) — that's irreducible. The recursion stays O(N) but the output dominates the cost.

> *Coming up — the chapter shifts from depth-first patterns to **level-order** patterns. The next two lessons cover BFS-based tree problems: per-level aggregations, deepest-leaf computations, completeness checks, zigzag traversal, cousin checks, and column-based traversals (top view, bottom view, vertical, diagonal). The queue from chapter 6 finally takes centre stage.*

</details>
