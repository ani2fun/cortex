---
title: "BST Pair Sum"
summary: "Given the roots of two BSTs rootA and rootB, and an integer target, return true if there's a pair of nodes (one from each tree) whose values sum to target. Return false otherwise."
prereqs:
  - 14-pattern-two-pointer/01-pattern
difficulty: hard
---

# BST pair sum

## Problem Statement

Given the **roots** of two BSTs `rootA` and `rootB`, and an integer **target**, return `true` if there's a pair of nodes (one from each tree) whose values sum to `target`. Return `false` otherwise.

### Example 1

> - **Input:** `rootA = [4, 2, 6, 1, null, null, 7]`, `rootB = [2, 1, 4, null, null, 3, 8]`, `target = 15`
> - **Output:** `true`
> - **Explanation:** `7 (from A) + 8 (from B) = 15`.

### Example 2

> - **Input:** `rootA = [4, 2, 6, 1, null, null, 7]`, `rootB = [2, 1, 4, null, null, 3, 8]`, `target = 35`
> - **Output:** `false`

<details>
<summary><h2>The Strategy</h2></summary>


This is the multi-tree generalisation of "two sum on BST". Run the **forward iterator on the first tree** and the **reverse iterator on the second tree**, and apply the same step rule. The crossing condition no longer applies — we stop when *either* iterator runs out (it won't cross because the two trees are independent).

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
    def bst_pair_sum(
        self,
        root_a: Optional[TreeNode],
        root_b: Optional[TreeNode],
        target: int,
    ) -> bool:
        if not root_a or not root_b:
            return False

        # Initialize the left and right iterators
        left_iterator: ForwardBstIterator = ForwardBstIterator(root_a)
        right_iterator: ReverseBstIterator = ReverseBstIterator(root_b)

        left_node: Optional[TreeNode] = left_iterator.next()
        right_node: Optional[TreeNode] = right_iterator.next()

        while left_node is not None and right_node is not None:

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
rA = from_level_order([4, 2, 6, 1, None, None, 7])
rB = from_level_order([2, 1, 4, None, None, 3, 8])
print(Solution().bst_pair_sum(rA, rB, 15))  # True  (7+8)

rA2 = from_level_order([4, 2, 6, 1, None, None, 7])
rB2 = from_level_order([2, 1, 4, None, None, 3, 8])
print(Solution().bst_pair_sum(rA2, rB2, 35))  # False

# Edge cases
print(Solution().bst_pair_sum(None, rB, 5))   # False — rootA is None

t1 = from_level_order([3])
t2 = from_level_order([4])
print(Solution().bst_pair_sum(t1, t2, 7))     # True  (3+4)

t3 = from_level_order([3])
t4 = from_level_order([4])
print(Solution().bst_pair_sum(t3, t4, 8))     # False

rC = from_level_order([4, 2, 6, 1, None, None, 7])
rD = from_level_order([2, 1, 4, None, None, 3, 8])
print(Solution().bst_pair_sum(rC, rD, 2))     # False — min possible is 1+1=2? wait: 1+1=2 True
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
        public boolean bstPairSum(
            TreeNode rootA,
            TreeNode rootB,
            int target
        ) {
            if (rootA == null || rootB == null) {
                return false;
            }

            // Initialize the left and right iterators
            ForwardBstIterator leftIterator = new ForwardBstIterator(rootA);
            ReverseBstIterator rightIterator = new ReverseBstIterator(rootB);

            TreeNode leftNode = leftIterator.next();
            TreeNode rightNode = rightIterator.next();

            while (leftNode != null && rightNode != null) {

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
        TreeNode rA = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        TreeNode rB = fromLevelOrder(2, 1, 4, null, null, 3, 8);
        System.out.println(new Solution().bstPairSum(rA, rB, 15));  // true  (7+8)

        TreeNode rA2 = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        TreeNode rB2 = fromLevelOrder(2, 1, 4, null, null, 3, 8);
        System.out.println(new Solution().bstPairSum(rA2, rB2, 35));  // false

        // Edge cases
        System.out.println(new Solution().bstPairSum(null, rB, 5));    // false — rootA is null

        TreeNode t1 = fromLevelOrder(3);
        TreeNode t2 = fromLevelOrder(4);
        System.out.println(new Solution().bstPairSum(t1, t2, 7));      // true  (3+4)

        TreeNode t3 = fromLevelOrder(3);
        TreeNode t4 = fromLevelOrder(4);
        System.out.println(new Solution().bstPairSum(t3, t4, 8));      // false

        TreeNode rC = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        TreeNode rD = fromLevelOrder(2, 1, 4, null, null, 3, 8);
        System.out.println(new Solution().bstPairSum(rC, rD, 2));      // true  (1+1)
    }
}
```


<details>
<summary><strong>Trace — rootA = [4, 2, 6, 1, null, null, 7], rootB = [2, 1, 4, null, null, 3, 8], target = 15</strong></summary>

```
A sorted: [1, 2, 4, 6, 7]
B sorted: [1, 2, 3, 4, 8]

Step 1 │ l=1 (from A), r=8 (from B) │ sum=9  < 15 → advance left
Step 2 │ l=2, r=8                  │ sum=10 < 15 → advance left
Step 3 │ l=4, r=8                  │ sum=12 < 15 → advance left
Step 4 │ l=6, r=8                  │ sum=14 < 15 → advance left
Step 5 │ l=7, r=8                  │ sum=15 ✓   → return true
```

</details>

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The Two Pointer pattern on BSTs is the meeting of two ideas you've already mastered: **iterators** that walk a BST in sorted order on demand (lesson 9), and **two-pointer reductions** familiar from sorted arrays. Run a forward iterator and a reverse iterator simultaneously, and you have a working `(small, large)` pair you can use to drive any sum/multiple/distance/comparison decision — without ever materialising the sorted array.

The pay-off is striking: many "pair" problems on BSTs that would naively be O(n²) (compare every pair) or O(n) memory (flatten to array, then two-pointer) collapse to **O(n) time, O(h) space** with this pattern.

Three patterns to keep:

1. **Iterators turn BSTs into sorted streams.** Once you can `next()` and `hasNext()`, every algorithm that works on sorted arrays generalises directly to BSTs. The conversion is *free* in terms of memory.
2. **Two iterators, two directions.** This is the BST analogue of the array two-pointer template — and it solves the same problem family (sum-to-target, pair properties, median, ranges).
3. **Two BSTs at once.** Different sources of the left and right pointers gives us cross-tree operations like *bst-pair-sum*. The same trick scales further: streaming joins between two sorted indexes in a database use exactly this idea.

</details>
<details>
<summary><h2>Closing the Chapter</h2></summary>


You started this chapter with a static binary tree decorated with one extra rule, and you finish it able to **search, insert, delete, validate, range-query, iterate, and pair-traverse** with confidence. The single thread tying every lesson together is the **binary search property** — the small invariant that turns "look at every node" into "look at one path", and that turns ordered-set problems into single-pass tree walks. Every BST operation, every pattern, every pair of iterators in this chapter is a different way of leaning on that one rule.

Heaps, the next chapter, change the rule — instead of "left smaller, right larger" it's "parent smaller than children". The shape becomes a different tool, optimised not for sorted iteration but for repeatedly extracting the minimum (or maximum). The mental model you've built here will transfer cleanly: it's still a tree, still a property, still a discipline on where values live. Different rule, different superpower.

</details>
