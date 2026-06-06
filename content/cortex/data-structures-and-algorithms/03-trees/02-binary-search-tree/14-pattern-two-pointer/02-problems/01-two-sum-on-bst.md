---
title: "Two Sum on BST"
summary: "Given the root of a BST and an integer target, return true if some pair of nodes in the tree has values summing to target. Return false otherwise."
prereqs:
  - 14-pattern-two-pointer/01-pattern
difficulty: medium
---

# Two sum on BST

## Problem Statement

Given the **root** of a BST and an integer **target**, return `true` if some pair of nodes in the tree has values summing to `target`. Return `false` otherwise.

### Example 1

> - **Input:** `root = [4, 2, 6, 1, null, null, 7]`, `target = 9`
> - **Output:** `true`
> - **Explanation:** Nodes `2` and `7` sum to `9`.

### Example 2

> - **Input:** `root = [2, 1, 4, null, null, 3, 7]`, `target = 16`
> - **Output:** `false`

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
    def two_sum_on_bst(
        self, root: Optional[TreeNode], target: int
    ) -> bool:
        if not root:
            return False

        # Initialize the left and right iterators
        left_iterator: ForwardBstIterator = ForwardBstIterator(root)
        right_iterator: ReverseBstIterator = ReverseBstIterator(root)

        left_node: Optional[TreeNode] = left_iterator.next()
        right_node: Optional[TreeNode] = right_iterator.next()

        while (
            left_node is not None
            and right_node is not None
            and left_node.val < right_node.val
        ):

            # Check if the sum of the two nodes equals the target
            if left_node.val + right_node.val == target:
                return True

            # If the sum is less than target, move the left pointer
            # to the right
            elif left_node.val + right_node.val < target:
                left_node = left_iterator.next()

            # If the sum is greater than target, move the right pointer
            # to the left
            else:
                right_node = right_iterator.next()

        # No pair found
        return False


# Examples from the problem statement
t1 = from_level_order([4, 2, 6, 1, None, None, 7])
print(Solution().two_sum_on_bst(t1, 9))   # True  (2+7)

t2 = from_level_order([2, 1, 4, None, None, 3, 7])
print(Solution().two_sum_on_bst(t2, 16))  # False

# Edge cases
print(Solution().two_sum_on_bst(None, 5)) # False — empty tree

t3 = from_level_order([5])
print(Solution().two_sum_on_bst(t3, 10))  # False — single node, no pair

t4 = from_level_order([4, 2, 6, 1, None, None, 7])
print(Solution().two_sum_on_bst(t4, 3))   # True  (1+2)

t5 = from_level_order([4, 2, 6, 1, None, None, 7])
print(Solution().two_sum_on_bst(t5, 13))  # True  (6+7)

t6 = from_level_order([4, 2, 6, 1, None, None, 7])
print(Solution().two_sum_on_bst(t6, 14))  # False — exceeds max pair sum
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
        public boolean twoSumOnBST(TreeNode root, int target) {
            if (root == null) {
                return false;
            }

            // Initialize the left and right iterators
            ForwardBstIterator leftIterator = new ForwardBstIterator(root);
            ReverseBstIterator rightIterator = new ReverseBstIterator(root);

            TreeNode leftNode = leftIterator.next();
            TreeNode rightNode = rightIterator.next();

            while (
                leftNode != null &&
                rightNode != null &&
                leftNode.val < rightNode.val
            ) {

                // Check if the sum of the two nodes equals the target
                if (leftNode.val + rightNode.val == target) {
                    return true;
                }

                // If the sum is less than target, move the left pointer
                // to the right
                else if (leftNode.val + rightNode.val < target) {
                    leftNode = leftIterator.next();
                }

                // If the sum is greater than target, move the right pointer
                // to the left
                else {
                    rightNode = rightIterator.next();
                }
            }

            // No pair found
            return false;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        TreeNode t1 = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        System.out.println(new Solution().twoSumOnBST(t1, 9));   // true  (2+7)

        TreeNode t2 = fromLevelOrder(2, 1, 4, null, null, 3, 7);
        System.out.println(new Solution().twoSumOnBST(t2, 16));  // false

        // Edge cases
        System.out.println(new Solution().twoSumOnBST(null, 5)); // false — empty tree

        TreeNode t3 = fromLevelOrder(5);
        System.out.println(new Solution().twoSumOnBST(t3, 10));  // false — single node, no pair

        TreeNode t4 = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        System.out.println(new Solution().twoSumOnBST(t4, 3));   // true  (1+2)

        TreeNode t5 = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        System.out.println(new Solution().twoSumOnBST(t5, 13));  // true  (6+7)

        TreeNode t6 = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        System.out.println(new Solution().twoSumOnBST(t6, 14));  // false — exceeds max pair sum
    }
}
```


<details>
<summary><strong>Trace — root = [4, 2, 6, 1, null, null, 7], target = 9</strong></summary>

```
Sorted view: [1, 2, 4, 6, 7]

Step 1 │ leftNode=1, rightNode=7 │ sum=8 < 9 → advance left
Step 2 │ leftNode=2, rightNode=7 │ sum=9 ✓  → return true
```

</details>

</details>
