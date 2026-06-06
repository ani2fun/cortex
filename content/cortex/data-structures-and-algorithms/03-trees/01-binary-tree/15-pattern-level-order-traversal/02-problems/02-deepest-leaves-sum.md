---
title: "Deepest Leaves Sum"
summary: "See problem statement below."
prereqs:
  - 15-pattern-level-order-traversal/01-pattern
difficulty: easy
---

# Problem 2 — Deepest leaves sum

> Return the sum of the values of the leaves on the deepest level of the tree.

Same shape as level-sum, but instead of recording every level we just *overwrite* a single `levelSum` variable each iteration. After the loop ends, `levelSum` holds the sum of the deepest level. (Note: every node on the deepest level is a leaf.)

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
    def deepest_leaves_sum(self, root: Optional[TreeNode]) -> int:

        # If the tree is empty, return 0
        if not root:
            return 0

        queue = Queue()
        queue.put(root)

        # Variable to store the level_sum of the deepest leaves
        level_sum = 0

        # Loop through each level in the tree
        while not queue.empty():

            # Get the size of the current level
            level_size = queue.qsize()

            # Reset level_sum for the current level
            level_sum = 0

            # Loop through each node in the current level
            for _ in range(level_size):

                # Get the front node in the queue and remove it
                node = queue.get()

                # Add its value to the level_sum
                level_sum += node.val

                # Add the node's children to the queue if they exist
                if node.left:
                    queue.put(node.left)

                if node.right:
                    queue.put(node.right)

        # The last computed level_sum is for the deepest level
        return level_sum


# Examples from the problem statement
print(Solution().deepest_leaves_sum(from_level_order([1, 2, 1, 7, None, None, 1])))  # 8
print(Solution().deepest_leaves_sum(from_level_order([1, 6, 5, None, None, 2, 7])))  # 9

# Edge cases
print(Solution().deepest_leaves_sum(None))                                            # 0
print(Solution().deepest_leaves_sum(TreeNode(7)))                                     # 7
print(Solution().deepest_leaves_sum(from_level_order([1, 2, None, 3, None, 4])))     # 4 left skew
print(Solution().deepest_leaves_sum(from_level_order([1, None, 2, None, None, None, 3])))  # 3 right skew
print(Solution().deepest_leaves_sum(from_level_order([1, 2, 3])))                    # 5 balanced two children
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
        public int deepestLeavesSum(TreeNode root) {

            // If the tree is empty, return 0
            if (root == null) {
                return 0;
            }

            Queue<TreeNode> queue = new LinkedList<>();
            queue.add(root);

            // Variable to store the levelSum of the deepest leaves
            int levelSum = 0;

            // Loop through each level in the tree
            while (!queue.isEmpty()) {

                // Get the size of the current level
                int levelSize = queue.size();

                // Reset levelSum for the current level
                levelSum = 0;

                // Loop through each node in the current level
                for (int i = 0; i < levelSize; ++i) {

                    // Get the front node in the queue and remove it
                    TreeNode node = queue.poll();

                    // Add its value to the levelSum
                    levelSum += node.val;

                    // Add the node's children to the queue if they exist
                    if (node.left != null) {
                        queue.add(node.left);
                    }

                    if (node.right != null) {
                        queue.add(node.right);
                    }
                }
            }

            // The last computed levelSum is for the deepest level
            return levelSum;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().deepestLeavesSum(fromLevelOrder(1, 2, 1, 7, null, null, 1)));  // 8
        System.out.println(new Solution().deepestLeavesSum(fromLevelOrder(1, 6, 5, null, null, 2, 7)));  // 9

        // Edge cases
        System.out.println(new Solution().deepestLeavesSum(null));                                        // 0
        System.out.println(new Solution().deepestLeavesSum(new TreeNode(7)));                             // 7
        System.out.println(new Solution().deepestLeavesSum(fromLevelOrder(1, 2, null, 3)));              // 4 left skew
        System.out.println(new Solution().deepestLeavesSum(fromLevelOrder(1, null, 2, null, null, null, 3)));  // 3 right skew
        System.out.println(new Solution().deepestLeavesSum(fromLevelOrder(1, 2, 3)));                    // 5 balanced two children
    }
}
```

</details>
