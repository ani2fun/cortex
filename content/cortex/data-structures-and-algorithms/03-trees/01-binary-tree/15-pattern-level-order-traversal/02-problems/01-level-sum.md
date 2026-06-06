---
title: "Level Sum"
summary: "See problem statement below."
prereqs:
  - 15-pattern-level-order-traversal/01-pattern
difficulty: easy
---

# Problem 1 — Level sum

> Return a list where the *i*-th entry is the sum of all node values at level *i*.

Apply the template directly: at the top of each outer-loop iteration, accumulate `levelSum = 0`; in the inner loop, add each node's value; after the inner loop, append `levelSum` to the output.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from queue import Queue
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
    def level_sum(self, root: Optional[TreeNode]) -> List[int]:
        level_sums: List[int] = []
        if not root:
            return level_sums

        queue = Queue()
        queue.put(root)

        # Loop through each level in the tree
        while not queue.empty():

            # Get the size of the current level
            level_size = queue.qsize()
            level_sum = 0

            # Loop through each node in the current level
            for _ in range(level_size):

                # Get the front node in the queue and remove it
                node = queue.get()

                # Add the node's value to the current level sum
                level_sum += node.val

                # Add the node's children to the queue if they exist
                if node.left:
                    queue.put(node.left)

                if node.right:
                    queue.put(node.right)

            # Add the current level sum to the level_sums list
            level_sums.append(level_sum)

        return level_sums


# Examples from the problem statement
print(Solution().level_sum(from_level_order([1, 2, 3, 4, None, None, 7])))   # [1, 5, 11]
print(Solution().level_sum(from_level_order([1, 8, 4, None, None, 2, 7])))   # [1, 12, 9]

# Edge cases
print(Solution().level_sum(None))                                             # []
print(Solution().level_sum(TreeNode(42)))                                     # [42]
print(Solution().level_sum(from_level_order([1, 2, None, 3, None, 4])))      # [1, 2, 3, 4] left skew
print(Solution().level_sum(from_level_order([1, None, 2, None, None, None, 3])))  # [1, 2, 3] right skew
print(Solution().level_sum(from_level_order([5, 5, 5, 5, 5, 5, 5])))         # [5, 10, 20] full balanced
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
        public List<Integer> levelSum(TreeNode root) {
            List<Integer> levelSums = new ArrayList<>();
            if (root == null) {
                return levelSums;
            }

            Queue<TreeNode> queue = new LinkedList<>();
            queue.add(root);

            // Loop through each level in the tree
            while (!queue.isEmpty()) {

                // Get the size of the current level
                int levelSize = queue.size();
                int levelSum = 0;

                // Loop through each node in the current level
                for (int i = 0; i < levelSize; i++) {

                    // Get the front node in the queue and remove it
                    TreeNode node = queue.poll();

                    // Add the node's value to the current level sum
                    levelSum += node.val;

                    // Add the node's children to the queue if they exist
                    if (node.left != null) {
                        queue.add(node.left);
                    }

                    if (node.right != null) {
                        queue.add(node.right);
                    }
                }

                // Add the current level sum to the levelSums list
                levelSums.add(levelSum);
            }

            return levelSums;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().levelSum(fromLevelOrder(1, 2, 3, 4, null, null, 7)));   // [1, 5, 11]
        System.out.println(new Solution().levelSum(fromLevelOrder(1, 8, 4, null, null, 2, 7)));   // [1, 12, 9]

        // Edge cases
        System.out.println(new Solution().levelSum(null));                                         // []
        System.out.println(new Solution().levelSum(new TreeNode(42)));                             // [42]
        System.out.println(new Solution().levelSum(fromLevelOrder(1, 2, null, 3)));               // [1, 2, 3] left skew
        System.out.println(new Solution().levelSum(fromLevelOrder(1, null, 2, null, null, null, 3)));  // [1, 2, 3] right skew
        System.out.println(new Solution().levelSum(fromLevelOrder(5, 5, 5, 5, 5, 5, 5)));         // [5, 10, 20] full balanced
    }
}
```

</details>
