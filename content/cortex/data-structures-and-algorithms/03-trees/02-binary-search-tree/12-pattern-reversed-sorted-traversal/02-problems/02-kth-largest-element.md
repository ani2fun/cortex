---
title: "Kth Largest Element"
summary: "Given the root of a binary search tree and an integer k, return the k-th largest element. Return 0 if no such element exists."
prereqs:
  - 12-pattern-reversed-sorted-traversal/01-pattern
difficulty: medium
---

# Kth largest element

## Problem Statement

Given the **root** of a binary search tree and an integer `k`, return the k-th largest element. Return `0` if no such element exists.

### Example 1

> - **Input:** `root = [4, 2, 5, 1, 3, null, 6]`, `k = 3`
> - **Output:** `4`

### Example 2

> - **Input:** `root = [5, 4, 10, null, null, 9, 11]`, `k = 2`
> - **Output:** `10`

<details>
<summary><h2>The Strategy</h2></summary>


Walk reverse in-order; the k-th node visited is the k-th largest. Critically — **stop traversing the moment the answer is found**, so the cost is O(h + k), not O(n).

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from typing import Optional, List, Any


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

        # Counter to keep track of the kth element
        self.count: int = 0

        # Variable to store the kth largest element
        self.result: int = 0

        # Perform reverse in-order traversal
        self.found: bool = False

    def reverse_in_order(self, root: Optional[TreeNode], k: int) -> None:

        # If the root is null or the kth largest element is already
        # found, we don't need to traverse further
        if root is None or self.found is True:
            return

        # Traverse the right subtree
        self.reverse_in_order(root.right, k)

        # Increment the count
        self.count += 1

        # If the count matches k, we have found the kth largest element
        if self.count == k:
            self.result = root.val
            self.found = True
            return

        # Traverse the left subtree
        self.reverse_in_order(root.left, k)

    def kth_largest_element(
        self, root: Optional[TreeNode], k: int
    ) -> int:

        # Perform reverse in-order traversal
        self.reverse_in_order(root, k)

        return self.result


# Example 1: k=3 → 4
print(Solution().kth_largest_element(
    from_level_order([4, 2, 5, 1, 3, None, 6]), 3))   # 4

# Example 2: k=2 → 10
print(Solution().kth_largest_element(
    from_level_order([5, 4, 10, None, None, 9, 11]), 2))  # 10

# Edge cases
print(Solution().kth_largest_element(
    from_level_order([5]), 1))                          # 5  (single node)

print(Solution().kth_largest_element(
    from_level_order([5]), 2))                          # 0  (k > size)

# k=1 → largest element
print(Solution().kth_largest_element(
    from_level_order([4, 2, 5, 1, 3, None, 6]), 1))   # 6

# k equals total number of nodes → smallest
print(Solution().kth_largest_element(
    from_level_order([4, 2, 5, 1, 3, None, 6]), 6))   # 1
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

        // Counter to keep track of the kth element
        private int count = 0;

        // Variable to store the kth largest element
        private int result = 0;

        // Flag to indicate if the kth largest element has been found
        private boolean found = false;

        private void reverseInOrder(TreeNode root, int k) {

            // If the root is null or the kth largest element is already
            // found, we don't need to traverse further
            if (root == null || found) {
                return;
            }

            // Traverse the right subtree
            reverseInOrder(root.right, k);

            // Increment the count
            count++;

            // If the count matches k, we have found the kth largest element
            if (count == k) {
                result = root.val;
                found = true;
                return;
            }

            // Traverse the left subtree
            reverseInOrder(root.left, k);
        }

        public int kthLargestElement(TreeNode root, int k) {

            // Perform reverse in-order traversal
            reverseInOrder(root, k);

            return result;
        }
    }

    public static void main(String[] args) {
        // Example 1: k=3 → 4
        System.out.println(new Solution().kthLargestElement(
            fromLevelOrder(4, 2, 5, 1, 3, null, 6), 3));   // 4

        // Example 2: k=2 → 10
        System.out.println(new Solution().kthLargestElement(
            fromLevelOrder(5, 4, 10, null, null, 9, 11), 2));  // 10

        // Edge cases
        System.out.println(new Solution().kthLargestElement(
            fromLevelOrder(5), 1));                          // 5
        System.out.println(new Solution().kthLargestElement(
            fromLevelOrder(5), 2));                          // 0  (k > size)

        // k=1 → largest element
        System.out.println(new Solution().kthLargestElement(
            fromLevelOrder(4, 2, 5, 1, 3, null, 6), 1));   // 6

        // k equals total nodes → smallest
        System.out.println(new Solution().kthLargestElement(
            fromLevelOrder(4, 2, 5, 1, 3, null, 6), 6));   // 1
    }
}
```

</details>
