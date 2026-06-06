---
title: "Descendants Sum Count"
summary: "See problem statement below."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: medium
---

# Problem 2 — Descendants sum count

> Count nodes whose value equals the sum of *all* values in their subtree below them (not including themselves).

Each subtree returns its sum (so the parent can compute its own); along the way, each call updates a global counter if `node.val == leftSum + rightSum`.

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
    def __init__(self):
        self.count: int = 0

    def compute_sum(self, root: Optional[TreeNode]) -> int:

        # Base case: If the current node is NULL, return 0
        if not root:
            return 0

        # Recursively compute the sum of the left and right subtrees
        left_sum = self.compute_sum(root.left)
        right_sum = self.compute_sum(root.right)

        # If the value of the current node is equal to the sum of its
        # descendants, increment the count
        if root.val == left_sum + right_sum:
            self.count += 1

        # Return the sum of the current subtree, including the value
        # of the current node
        return left_sum + right_sum + root.val

    def descendants_sum_count(self, root: Optional[TreeNode]) -> int:

        # Call the compute_sum function to count the number of nodes
        # satisfying the given condition
        self.compute_sum(root)
        return self.count


# Examples from the problem statement
print(Solution().descendants_sum_count(from_level_order([21, 7, 3, 5, 2, None, 4])))   # 2
print(Solution().descendants_sum_count(from_level_order([5, 7, 3, 1, 2, None, 3])))    # 1

# Edge cases
print(Solution().descendants_sum_count(None))                                            # 0
print(Solution().descendants_sum_count(from_level_order([0])))                           # 1 (single leaf: val==0==sum)
print(Solution().descendants_sum_count(from_level_order([1])))                           # 0 (single leaf: 1!=0)
print(Solution().descendants_sum_count(from_level_order([3, 1, 2])))                     # 1 (root: 3==1+2)
print(Solution().descendants_sum_count(from_level_order([1, 2, None, 3, None, 4])))      # 0 (only-left skew)
print(Solution().descendants_sum_count(from_level_order([6, 3, 3, 1, 2, 1, 2])))        # 3 (root + both internal nodes)
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
        private int count = 0;

        private int computeSum(TreeNode root) {

            // Base case: If the current node is NULL, return 0
            if (root == null) {
                return 0;
            }

            // Recursively compute the sum of the left and right subtrees
            int leftSum = computeSum(root.left);
            int rightSum = computeSum(root.right);

            // If the value of the current node is equal to the sum of its
            // descendants, increment the count
            if (root.val == leftSum + rightSum) {
                count++;
            }

            // Return the sum of the current subtree, including the value
            // of the current node
            return leftSum + rightSum + root.val;
        }

        public int descendantsSumCount(TreeNode root) {

            // Call the computeSum function to count the number of nodes
            // satisfying the given condition
            computeSum(root);
            return count;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().descendantsSumCount(fromLevelOrder(21, 7, 3, 5, 2, null, 4)));   // 2
        System.out.println(new Solution().descendantsSumCount(fromLevelOrder(5, 7, 3, 1, 2, null, 3)));    // 1

        // Edge cases
        System.out.println(new Solution().descendantsSumCount(null));                                       // 0
        System.out.println(new Solution().descendantsSumCount(fromLevelOrder(0)));                          // 1 (single leaf: val==0==sum)
        System.out.println(new Solution().descendantsSumCount(fromLevelOrder(1)));                          // 0 (single leaf: 1!=0)
        System.out.println(new Solution().descendantsSumCount(fromLevelOrder(3, 1, 2)));                    // 1 (root: 3==1+2)
        System.out.println(new Solution().descendantsSumCount(fromLevelOrder(1, 2, null, 3)));              // 0 (only-left skew)
        System.out.println(new Solution().descendantsSumCount(fromLevelOrder(6, 3, 3, 1, 2, 1, 2)));       // 3 (root + both internal nodes)
    }
}
```

</details>
