---
title: "Multiple Tree"
summary: "Given the root of a BST, return true if for every pair of nodes formed by taking one from the start and one from the end of the in-order traversal, the *end* node's value is a positive multiple of the"
prereqs:
  - 14-pattern-two-pointer/01-pattern
difficulty: medium
---

# Multiple tree

## Problem Statement

Given the **root** of a BST, return `true` if for **every** pair of nodes formed by taking one from the start and one from the end of the in-order traversal, the *end* node's value is a positive multiple of the *start* node's value. Return `false` otherwise.

### Example 1

> - **Input:** `root = [4, 2, 6, 1, null, null, 7]`
> - **Output:** `true`
> - **Explanation:** Sorted: `[1, 2, 4, 6, 7]`. Pairs: `(1, 7)`, `(2, 6)`, `(4, 4)`. Each `right % left == 0`.

### Example 2

> - **Input:** `root = [2, 1, 5, null, null, 3, 7]`
> - **Output:** `false`
> - **Explanation:** Sorted: `[1, 2, 3, 5, 7]`. Pair `(2, 5)` fails (`5 % 2 ≠ 0`).

<details>
<summary><h2>The Strategy</h2></summary>


Same shape as two-sum, but the predicate is "right.val % left.val == 0", and we always advance both pointers (each iteration consumes a unique pair). Stop early on any failure.

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
    def multiple_tree(self, root: Optional[TreeNode]) -> bool:
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

            # Check if the right node's value is a multiple of the left
            # node's value
            if right_node.val % left_node.val != 0:
                return False

            # Move to the left node to the next node in in-order
            left_node = left_iterator.next()

            # Move the right node to the next node in reverse in-order
            right_node = right_iterator.next()

        # If all pairs satisfy the condition, return true
        return True


# Examples from the problem statement
t1 = from_level_order([4, 2, 6, 1, None, None, 7])
print(Solution().multiple_tree(t1))  # True  (7%1=0, 6%2=0)

t2 = from_level_order([2, 1, 5, None, None, 3, 7])
print(Solution().multiple_tree(t2))  # False (5%2 != 0)

# Edge cases
print(Solution().multiple_tree(None))                  # False — empty tree

t3 = from_level_order([6])
print(Solution().multiple_tree(t3))                    # True  — single node

t4 = from_level_order([3, 1, 6])
print(Solution().multiple_tree(t4))                    # True  (6%1=0, 6%3=0)

t5 = from_level_order([4, 2, 6, 1, None, None, 9])
print(Solution().multiple_tree(t5))                    # False (9%2 != 0)

t6 = from_level_order([6, 2, 12, 1, None, None, 24])
print(Solution().multiple_tree(t6))                    # True  (24%1=0, 12%2=0)
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
        public boolean multipleTree(TreeNode root) {
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

                // Check if the right node's value is a multiple of the left
                // node's value
                if (rightNode.val % leftNode.val != 0) {
                    return false;
                }

                // Move to the left node to the next node in in-order
                leftNode = leftIterator.next();

                // Move the right node to the next node in reverse in-order
                rightNode = rightIterator.next();
            }

            // If all pairs satisfy the condition, return true
            return true;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        TreeNode t1 = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        System.out.println(new Solution().multipleTree(t1));  // true  (7%1=0, 6%2=0)

        TreeNode t2 = fromLevelOrder(2, 1, 5, null, null, 3, 7);
        System.out.println(new Solution().multipleTree(t2));  // false (5%2 != 0)

        // Edge cases
        System.out.println(new Solution().multipleTree(null));                   // false — empty tree

        TreeNode t3 = fromLevelOrder(6);
        System.out.println(new Solution().multipleTree(t3));                     // true  — single node

        TreeNode t4 = fromLevelOrder(3, 1, 6);
        System.out.println(new Solution().multipleTree(t4));                     // true  (6%1=0, 6%3=0)

        TreeNode t5 = fromLevelOrder(4, 2, 6, 1, null, null, 9);
        System.out.println(new Solution().multipleTree(t5));                     // false (9%2 != 0)

        TreeNode t6 = fromLevelOrder(6, 2, 12, 1, null, null, 24);
        System.out.println(new Solution().multipleTree(t6));                     // true  (24%1=0, 12%2=0)
    }
}
```

</details>
