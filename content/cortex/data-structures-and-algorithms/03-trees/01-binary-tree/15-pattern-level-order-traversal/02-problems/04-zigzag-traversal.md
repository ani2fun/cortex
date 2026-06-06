---
title: "Zigzag Traversal"
summary: "See problem statement below."
prereqs:
  - 15-pattern-level-order-traversal/01-pattern
difficulty: medium
---

# Problem 4 — Zigzag traversal

> Return the level-order traversal where the *direction* alternates per level: level 0 left-to-right, level 1 right-to-left, level 2 left-to-right, …

Same template, but pre-allocate the level array and *write into it from either end* depending on a `reverse` boolean that flips each iteration. Avoids per-level reversal at the cost of one extra index.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from queue import Queue
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
    def zigzag_traversal(
        self, root: Optional[TreeNode]
    ) -> List[List[int]]:
        zigzag_levels: List[List[int]] = []
        if not root:
            return zigzag_levels

        queue = Queue()
        queue.put(root)

        # Flag to indicate the direction of traversal
        reverse = False

        # Loop through each level in the tree
        while not queue.empty():

            # Get the size of the current level
            level_size = queue.qsize()

            # Initialize the list to store the nodes in the current
            # level. The size of the list is equal to the number of
            # nodes in the current level.
            level = [0] * level_size

            # Loop through each node in the current level
            for i in range(level_size):

                # Get the front node in the queue and remove it
                node = queue.get()

                # Fill level list based on the direction of traversal
                if reverse:
                    level[level_size - i - 1] = node.val
                else:
                    level[i] = node.val

                # Add the node's children to the queue if they exist
                if node.left:
                    queue.put(node.left)

                if node.right:
                    queue.put(node.right)

            # Add the current level list to the levels list
            zigzag_levels.append(level)

            # Flip the direction for the next level
            reverse = not reverse

        return zigzag_levels


# Examples from the problem statement
print(Solution().zigzag_traversal(from_level_order([1, 2, 3, 4, None, None, 7])))   # [[1], [3, 2], [4, 7]]
print(Solution().zigzag_traversal(from_level_order([1, 8, 4, None, None, 2, 7])))   # [[1], [4, 8], [2, 7]]

# Edge cases
print(Solution().zigzag_traversal(None))                                             # []
print(Solution().zigzag_traversal(TreeNode(1)))                                      # [[1]]
print(Solution().zigzag_traversal(from_level_order([1, 2, None, 3])))               # [[1], [2], [3]] left skew
print(Solution().zigzag_traversal(from_level_order([1, None, 2, None, None, None, 3])))  # [[1], [2], [3]] right skew
print(Solution().zigzag_traversal(from_level_order([1, 2, 3, 4, 5, 6, 7])))         # [[1], [3, 2], [4, 5, 6, 7]]
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
        public List<List<Integer>> zigzagTraversal(TreeNode root) {
            List<List<Integer>> zigzagLevels = new ArrayList<>();
            if (root == null) {
                return zigzagLevels;
            }

            Queue<TreeNode> queue = new LinkedList<>();
            queue.add(root);

            // Flag to indicate the direction of traversal
            boolean reverse = false;

            // Loop through each level in the tree
            while (!queue.isEmpty()) {

                // Get the size of the current level
                int levelSize = queue.size();

                // Initialize the list to store the nodes in the current
                // level. The size of the list is equal to the number of
                // nodes in the current level.
                List<Integer> level = new ArrayList<>(levelSize);

                // Loop through each node in the current level
                for (int i = 0; i < levelSize; i++) {
                    TreeNode node = queue.poll();

                    // Fill level list based on the direction of traversal
                    if (reverse) {

                        // Insert at the beginning
                        level.add(0, node.val);
                    } else {
                        level.add(node.val);
                    }

                    // Add the node's children to the queue if they exist
                    if (node.left != null) {
                        queue.add(node.left);
                    }

                    if (node.right != null) {
                        queue.add(node.right);
                    }
                }

                // Add the current level list to the levels list
                zigzagLevels.add(level);

                // Flip the direction for the next level
                reverse = !reverse;
            }

            return zigzagLevels;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().zigzagTraversal(fromLevelOrder(1, 2, 3, 4, null, null, 7)));   // [[1], [3, 2], [4, 7]]
        System.out.println(new Solution().zigzagTraversal(fromLevelOrder(1, 8, 4, null, null, 2, 7)));   // [[1], [4, 8], [2, 7]]

        // Edge cases
        System.out.println(new Solution().zigzagTraversal(null));                                         // []
        System.out.println(new Solution().zigzagTraversal(new TreeNode(1)));                              // [[1]]
        System.out.println(new Solution().zigzagTraversal(fromLevelOrder(1, 2, null, 3)));               // [[1], [2], [3]] left skew
        System.out.println(new Solution().zigzagTraversal(fromLevelOrder(1, null, 2, null, null, null, 3)));  // [[1], [2], [3]] right skew
        System.out.println(new Solution().zigzagTraversal(fromLevelOrder(1, 2, 3, 4, 5, 6, 7)));         // [[1], [3, 2], [4, 5, 6, 7]]
    }
}
```

</details>
