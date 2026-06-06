---
title: "Median in BST"
summary: "Given the root of a BST, return the median value, rounded down to the nearest integer."
prereqs:
  - 14-pattern-two-pointer/01-pattern
difficulty: hard
---

# Median in BST

## Problem Statement

Given the **root** of a BST, return the **median** value, rounded down to the nearest integer.

> The median is the middle value of the sorted in-order sequence. If the count is odd, it's the single middle value. If even, it's the average of the two middle values, rounded down (integer division).

### Example 1

> - **Input:** `root = [5, 4, 6, 2, null, null, 7]`
> - **Output:** `5`
> - **Explanation:** Sorted: `[2, 4, 5, 6, 7]`. Middle: `5`.

### Example 2

> - **Input:** `root = [10, 8, 14, 5, null, 13, 17]`
> - **Output:** `11`
> - **Explanation:** Sorted: `[5, 8, 10, 13, 14, 17]`. Middle pair: `(10, 13)`. Average: `11`.

<details>
<summary><h2>The Strategy</h2></summary>


The two-pointer pattern *naturally* finds the median: walk both iterators forward step-by-step. If the count is odd, eventually `leftNode == rightNode` — that single node's value is the median. If even, the loop ends when the two pointers cross, with `leftNode` and `rightNode` straddling the middle — the most recent pair's *average* is the median (rounded down).

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=binary-tree viz-root=root
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


class ForwardBstIterator:
    def __init__(self, root: Optional[TreeNode]):
        self.stack: List[TreeNode] = []
        self.push_all_left(root)

    def push_all_left(self, node: Optional[TreeNode]) -> None:
        while node:
            self.stack.append(node)
            node = node.left

    def has_next(self) -> bool:
        return bool(self.stack)

    def next(self) -> Optional[TreeNode]:
        if not self.has_next():
            return None

        node = self.stack.pop()
        self.push_all_left(node.right)
        return node

class ReverseBstIterator:
    def __init__(self, root: Optional[TreeNode]):
        self.stack: List[TreeNode] = []
        self.push_all_right(root)

    def push_all_right(self, node: Optional[TreeNode]) -> None:
        while node:
            self.stack.append(node)
            node = node.right

    def has_next(self) -> bool:
        return bool(self.stack)

    def next(self) -> Optional[TreeNode]:
        if not self.has_next():
            return None

        node = self.stack.pop()
        self.push_all_right(node.left)
        return node

class Solution:
    def median_in_bst(self, root: Optional[TreeNode]) -> int:
        if not root:
            return -1

        # Initialize the left and right iterators
        left_iterator: ForwardBstIterator = ForwardBstIterator(root)
        right_iterator: ReverseBstIterator = ReverseBstIterator(root)

        left_node: Optional[TreeNode] = left_iterator.next()
        right_node: Optional[TreeNode] = right_iterator.next()

        # Variable to store the median value
        median = -1

        while (
            left_node is not None
            and right_node is not None
            and left_node.val < right_node.val
        ):

            # Update the median with the average of the two nodes, if the
            # tree has an even number of nodes, the median will be the
            # average of the two middle nodes before exiting the loop
            median = (left_node.val + right_node.val) // 2

            # Move to the left node to the next node in in-order
            left_node = left_iterator.next()

            # Move the right node to the next node in reverse in-order
            right_node = right_iterator.next()

        # If both iterators meet at the same node, it means the tree has
        # an odd number of nodes
        if left_node == right_node:
            return left_node.val

        # If the tree has an even number of nodes, return the last
        # computed median
        return median


# Examples from the problem statement
t1 = from_level_order([5, 4, 6, 2, None, None, 7])
print(Solution().median_in_bst(t1))  # 5  — odd count, middle node

t2 = from_level_order([10, 8, 14, 5, None, 13, 17])
print(Solution().median_in_bst(t2))  # 11 — even count, floor((10+13)/2)

# Edge cases
print(Solution().median_in_bst(None))                 # -1 — empty tree

t3 = from_level_order([7])
print(Solution().median_in_bst(t3))                   # 7  — single node

t4 = from_level_order([3, 1, 5])
print(Solution().median_in_bst(t4))                   # 3  — odd, three nodes

t5 = from_level_order([4, 2, 6, 1, 3])
print(Solution().median_in_bst(t5))                   # 3  — odd, five nodes, sorted [1,2,3,4,6]

t6 = from_level_order([4, 2, 6, 1, 3, 5, 7])
print(Solution().median_in_bst(t6))                   # 4  — odd, seven nodes, middle
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

    static class ForwardBstIterator {

        private Stack<TreeNode> stack;

        public ForwardBstIterator(TreeNode root) {
            stack = new Stack<>();
            pushAllLeft(root);
        }

        private void pushAllLeft(TreeNode node) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
        }

        public boolean hasNext() {
            return !stack.empty();
        }

        public TreeNode next() {
            if (!hasNext()) {
                return null;
            }

            TreeNode node = stack.pop();
            pushAllLeft(node.right);
            return node;
        }
    }

    static class ReverseBstIterator {

        private Stack<TreeNode> stack;

        public ReverseBstIterator(TreeNode root) {
            stack = new Stack<>();
            pushAllRight(root);
        }

        private void pushAllRight(TreeNode node) {
            while (node != null) {
                stack.push(node);
                node = node.right;
            }
        }

        public boolean hasNext() {
            return !stack.empty();
        }

        public TreeNode next() {
            if (!hasNext()) {
                return null;
            }

            TreeNode node = stack.pop();
            pushAllRight(node.left);
            return node;
        }
    }

    static class Solution {
        public int medianInBst(TreeNode root) {
            if (root == null) {
                return -1;
            }

            // Initialize the left and right iterators
            ForwardBstIterator leftIterator = new ForwardBstIterator(root);
            ReverseBstIterator rightIterator = new ReverseBstIterator(root);

            TreeNode leftNode = leftIterator.next();
            TreeNode rightNode = rightIterator.next();

            // Variable to store the median value
            int median = -1;

            while (
                leftNode != null &&
                rightNode != null &&
                leftNode.val < rightNode.val
            ) {

                // Update the median with the average of the two nodes, if
                // the tree has an even number of nodes, the median will be
                // the average of the two middle nodes before exiting the
                // loop
                median = (leftNode.val + rightNode.val) / 2;

                // Move to the left node to the next node in in-order
                leftNode = leftIterator.next();

                // Move the right node to the next node in reverse in-order
                rightNode = rightIterator.next();
            }

            // If both iterators meet at the same node, it means the tree has
            // an odd number of nodes
            if (leftNode == rightNode) {
                return leftNode.val;
            }

            // If the tree has an even number of nodes, return the last
            // computed median
            return median;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        TreeNode t1 = fromLevelOrder(5, 4, 6, 2, null, null, 7);
        System.out.println(new Solution().medianInBst(t1));  // 5  — odd count, middle node

        TreeNode t2 = fromLevelOrder(10, 8, 14, 5, null, 13, 17);
        System.out.println(new Solution().medianInBst(t2));  // 11 — even count, floor((10+13)/2)

        // Edge cases
        System.out.println(new Solution().medianInBst(null));                  // -1 — empty tree

        TreeNode t3 = fromLevelOrder(7);
        System.out.println(new Solution().medianInBst(t3));                    // 7  — single node

        TreeNode t4 = fromLevelOrder(3, 1, 5);
        System.out.println(new Solution().medianInBst(t4));                    // 3  — odd, three nodes

        TreeNode t5 = fromLevelOrder(4, 2, 6, 1, 3);
        System.out.println(new Solution().medianInBst(t5));                    // 3  — odd, five nodes

        TreeNode t6 = fromLevelOrder(4, 2, 6, 1, 3, 5, 7);
        System.out.println(new Solution().medianInBst(t6));                    // 4  — odd, seven nodes
    }
}
```


<details>
<summary><strong>Trace — root = [10, 8, 14, 5, null, 13, 17]</strong></summary>

```
Sorted: [5, 8, 10, 13, 14, 17]  (even count = 6)

Step 1 │ l=5, r=17 │ 5 < 17 → median candidate = (5+17)/2 = 11 → advance both
Step 2 │ l=8, r=14 │ 8 < 14 → median candidate = (8+14)/2 = 11 → advance both
Step 3 │ l=10, r=13 │ 10 < 13 → median candidate = (10+13)/2 = 11 → advance both
Step 4 │ l=13, r=10 │ 13 > 10 → loop exits (crossed)
l != r → even count → return 11 ✓
```

</details>

</details>
