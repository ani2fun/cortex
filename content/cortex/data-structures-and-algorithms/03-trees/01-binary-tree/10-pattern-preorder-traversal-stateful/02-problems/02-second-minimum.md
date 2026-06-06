---
title: "Second Minimum"
summary: "See problem statement below."
prereqs:
  - 10-pattern-preorder-traversal-stateful/01-pattern
difficulty: medium
---

# Problem 2 — Second minimum

> Given the root of a binary tree, find and return the second-smallest distinct value. If there's no second minimum, return `-1`.

This is the **monotone witnesses** flavour. The state is two integers, `min` and `secondMin`, both shared across the recursion. Each visit either improves `min` (and demotes the old min to `secondMin`) or improves `secondMin`. No push/pop needed — once we've seen a smaller value, that's a global fact, not a path-local one.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
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
    def __init__(self) -> None:
        self.minimum: int
        self.second_minimum: int

    def find_second_minimum_helper(
        self, root: Optional[TreeNode]
    ) -> None:

        # Base case: if the root is None, return
        if root is None:
            return

        # Check if the value of the current node is less than the current
        # minimum
        if root.val < self.minimum:

            # Update the second minimum to the previous minimum
            self.second_minimum = self.minimum

            # Update the minimum to the value of the current node
            self.minimum = root.val
        elif root.val > self.minimum and (
            root.val < self.second_minimum or self.second_minimum == -1
        ):

            # Check if the value of the current node is greater than the
            # current minimum and less than the current second minimum
            # (or second minimum is not yet set) If so, update the second
            # minimum to the value of the current node
            self.second_minimum = root.val

        # Recursively traverse the left and right subtrees
        self.find_second_minimum_helper(root.left)
        self.find_second_minimum_helper(root.right)

    def find_second_minimum(self, root: Optional[TreeNode]) -> int:

        # Check if the root is None, return -1 as no second minimum
        # exists
        if root is None:
            return -1

        # Initialize the minimum to the value of the root node
        self.minimum = root.val

        # Initialize the second minimum to -1, indicating it has not been
        # set yet
        self.second_minimum = -1

        # Call the helper function to find the minimum and second minimum
        # values
        self.find_second_minimum_helper(root)

        # Return the second minimum value found
        return self.second_minimum


# Examples from the problem statement
print(Solution().find_second_minimum(from_level_order([1, 2, 5, 7, None, None, 3])))  # 2
print(Solution().find_second_minimum(from_level_order([1, 8, 4, None, None, 9, 7])))  # 4

# Edge cases
print(Solution().find_second_minimum(None))                                            # -1
print(Solution().find_second_minimum(from_level_order([5])))                           # -1
print(Solution().find_second_minimum(from_level_order([5, 5, 5])))                     # -1 (all same)
print(Solution().find_second_minimum(from_level_order([1, 2])))                        # 2
print(Solution().find_second_minimum(from_level_order([3, 1, 4, 1, 5, 9, 2])))        # 2
print(Solution().find_second_minimum(from_level_order([1, 2, 3, 4, 5, 6, 7])))        # 2
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

        // Global variables to store minimum and second minimum values
        private int minimum;
        private int secondMinimum;

        private void findSecondMinimumHelper(TreeNode root) {

            // Base case: if the root is null, return
            if (root == null) {
                return;
            }

            // Check if the value of the current node is less than the
            // current minimum
            if (root.val < minimum) {

                // Update the second minimum to the previous minimum
                secondMinimum = minimum;

                // Update the minimum to the value of the current node
                minimum = root.val;
            } else if (
                root.val > minimum &&
                (root.val < secondMinimum || secondMinimum == -1)
            ) {

                // Check if the value of the current node is greater than the
                // current minimum and less than the current second minimum
                // (or second minimum is not yet set) If so, update the
                // second minimum to the value of the current node
                secondMinimum = root.val;
            }

            // Recursively traverse the left and right subtrees
            findSecondMinimumHelper(root.left);
            findSecondMinimumHelper(root.right);
        }

        public int findSecondMinimum(TreeNode root) {

            // Check if the root is null, return -1 as no second minimum
            // exists
            if (root == null) {
                return -1;
            }

            // Initialize the minimum to the value of the root node
            minimum = root.val;

            // Initialize the second minimum to -1, indicating it has not
            // been set yet
            secondMinimum = -1;

            // Call the helper function to find the minimum and second
            // minimum values
            findSecondMinimumHelper(root);

            // Return the second minimum value found
            return secondMinimum;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().findSecondMinimum(fromLevelOrder(1, 2, 5, 7, null, null, 3)));  // 2
        System.out.println(new Solution().findSecondMinimum(fromLevelOrder(1, 8, 4, null, null, 9, 7)));  // 4

        // Edge cases
        System.out.println(new Solution().findSecondMinimum(null));                                        // -1
        System.out.println(new Solution().findSecondMinimum(fromLevelOrder(5)));                           // -1
        System.out.println(new Solution().findSecondMinimum(fromLevelOrder(5, 5, 5)));                     // -1 (all same)
        System.out.println(new Solution().findSecondMinimum(fromLevelOrder(1, 2)));                        // 2
        System.out.println(new Solution().findSecondMinimum(fromLevelOrder(3, 1, 4, 1, 5, 9, 2)));        // 2
        System.out.println(new Solution().findSecondMinimum(fromLevelOrder(1, 2, 3, 4, 5, 6, 7)));        // 2
    }
}
```

</details>
