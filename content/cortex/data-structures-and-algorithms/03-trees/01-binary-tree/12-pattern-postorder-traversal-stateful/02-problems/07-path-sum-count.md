---
title: "Path Sum Count"
summary: "See problem statement below."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: hard
---

# Problem 7 — Path sum count

> Given a `target`, count the number of *downward* paths (parent-to-descendant only) whose values sum to `target`.

This problem is interesting because it combines *both* preorder push-pop *and* postorder accumulation. The classic O(N) trick uses a **prefix-sum hash map**: as you descend, track the running sum from the root; the number of valid paths *ending at the current node* equals `prefixSumCount[currentSum - target]`. As you backtrack (postorder return), undo the prefix-sum count for this node.

This is a hybrid pattern, but it's traditionally taught with the postorder patterns because the *answer accumulates* upward like the others.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from collections import defaultdict
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
    def __init__(self):

        # Create a map to store the count of prefix sums encountered
        # so far.
        self.prefix_sum_count: defaultdict[int, int] = defaultdict(int)

    def find_paths(
        self, root: Optional[TreeNode], target: int, path_sum: int
    ) -> int:

        # Base case: If the node is None, we've reached the end of a
        # path, so return 0.
        if not root:
            return 0

        # Calculate the current sum by adding the value of the
        # current node to the previous sum.
        path_sum += root.val

        # Check if there is a prefix sum (path_sum - target) in the
        # prefix_sum_count map. If such a prefix sum exists, it means
        # there is a subpath with the target sum ending at the current
        # node. Increment the count of such subpaths.
        num_paths = self.prefix_sum_count.get(path_sum - target, 0)

        # Add the current sum to the prefix_sum_count map to keep track
        # of it. This is to be used by future nodes in the recursive
        # traversal.
        self.prefix_sum_count[path_sum] = (
            self.prefix_sum_count.get(path_sum, 0) + 1
        )

        # Recursively traverse the left and right subtrees, updating the
        # current sum and counting the subpaths.
        num_paths += self.find_paths(root.left, target, path_sum)
        num_paths += self.find_paths(root.right, target, path_sum)

        # Backtrack by removing the current sum from the prefix sum
        # count map. This is to ensure that the prefix sum count is
        # accurate for future nodes.
        self.prefix_sum_count[path_sum] -= 1

        # Return the total number of subpaths with the target sum found
        # so far.
        return num_paths

    def path_sum_count(
        self, root: Optional[TreeNode], target: int
    ) -> int:

        # Add initial prefix sum of 0
        self.prefix_sum_count[0] = 1

        # Start the recursive traversal from the root node with an
        # initial sum of 0.
        return self.find_paths(root, target, 0)


# Examples from the problem statement
print(Solution().path_sum_count(from_level_order([1, 2, 3, 4, None, None, 7]), 11))   # 1
print(Solution().path_sum_count(from_level_order([1, 8, 4, None, None, 2, 7]), 11))   # 1

# Edge cases
print(Solution().path_sum_count(None, 5))                                               # 0
print(Solution().path_sum_count(from_level_order([5]), 5))                              # 1 (root only)
print(Solution().path_sum_count(from_level_order([5]), 1))                              # 0
print(Solution().path_sum_count(from_level_order([1, 2, 3, 4, 5]), 3))                 # 2 (1+2, 3)
print(Solution().path_sum_count(from_level_order([0, 1, -1, None, None, 1, None]), 0)) # 3
print(Solution().path_sum_count(from_level_order([1, 1, 1]), 2))                        # 2
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

        // Create a map to store the count of prefix sums encountered
        // so far.
        private Map<Integer, Integer> prefixSumCount = new HashMap<>();

        private int findPaths(TreeNode root, int target, int pathSum) {

            // Base case: If the node is null, we've reached the end of a
            // path, so return 0.
            if (root == null) {
                return 0;
            }

            // Calculate the current sum by adding the value of the
            // current node to the previous sum.
            pathSum += root.val;

            // Check if there is a prefix sum (pathSum - target) in the
            // prefixSumCount map. If such a prefix sum exists, it means
            // there is a subpath with the target sum ending at the current
            // node. Increment the count of such subpaths.
            int numPaths = prefixSumCount.getOrDefault(pathSum - target, 0);

            // Add the current sum to the prefixSumCount map to keep track of
            // it. This is to be used by future nodes in the recursive
            // traversal.
            prefixSumCount.put(
                pathSum,
                prefixSumCount.getOrDefault(pathSum, 0) + 1
            );

            // Recursively traverse the left and right subtrees, updating the
            // current sum and counting the subpaths.
            numPaths += findPaths(root.left, target, pathSum);
            numPaths += findPaths(root.right, target, pathSum);

            // Backtrack by removing the current sum from the prefix sum
            // count map. This is to ensure that the prefix sum count is
            // accurate for future nodes.
            prefixSumCount.put(pathSum, prefixSumCount.get(pathSum) - 1);

            // Return the total number of subpaths with the target sum found
            // so far.
            return numPaths;
        }

        public int pathSumCount(TreeNode root, int target) {

            // Add initial prefix sum of 0
            prefixSumCount.put(0, 1);

            // Start the recursive traversal from the root node with an
            // initial sum of 0.
            return findPaths(root, target, 0);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().pathSumCount(fromLevelOrder(1, 2, 3, 4, null, null, 7), 11));   // 1
        System.out.println(new Solution().pathSumCount(fromLevelOrder(1, 8, 4, null, null, 2, 7), 11));   // 1

        // Edge cases
        System.out.println(new Solution().pathSumCount(null, 5));                                          // 0
        System.out.println(new Solution().pathSumCount(fromLevelOrder(5), 5));                             // 1
        System.out.println(new Solution().pathSumCount(fromLevelOrder(5), 1));                             // 0
        System.out.println(new Solution().pathSumCount(fromLevelOrder(1, 2, 3, 4, 5), 3));                // 2
        System.out.println(new Solution().pathSumCount(fromLevelOrder(1, 1, 1), 2));                       // 2
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Stateful postorder is the most *flexible* of the binary-tree patterns — it absorbs almost every "compute X for every subtree, also track a global Y" question. Three things to walk away with:

1. **Two channels per call.** Decide *what to return to the parent* and *what to update globally*. They're rarely the same number. Diameter returns *height*, tracks *diameter*. Distribute coins returns *excess flow*, tracks *moves*. Most-frequent subtree sum returns *sum*, tracks *frequency map + max frequency*. Recognise the duality and the algorithm writes itself.
2. **Globals are safe in postorder, dangerous in preorder.** In stateful preorder you must push/pop because sibling subtrees would otherwise see each other's state. In stateful postorder the global is *monotonically* updated (max, count, accumulate) and order doesn't matter — no undo needed. This is the structural distinction between the two stateful flavours.
3. **Prefix-sum hashing is a force multiplier.** The path-sum-count problem shows how a *combined* preorder-push-pop + postorder-aggregate + prefix-sum-hash can solve in O(N) what a naive O(N²) per-node "look at every ancestor" would do. The same technique recurs in array problems (subarray sum equals K) — internalise the idea.

> *Coming up — the chapter shifts focus from "compute X over the whole tree" to <strong>root-to-leaf path</strong> problems. Where the postorder patterns thought about subtrees, the next two lessons focus on whole paths from the root down to leaves: counting them, listing them, comparing them. The same backtracking template you saw in stateful preorder reappears, but specialised for the path-as-a-unit framing.*

</details>
