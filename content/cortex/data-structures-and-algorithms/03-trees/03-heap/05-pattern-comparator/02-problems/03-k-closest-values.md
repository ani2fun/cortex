---
title: "K Closest Values"
summary: "Given the root of a binary search tree, a target value (real number), and a non-negative integer k, return the K values in the BST closest to target. Return them in any order."
prereqs:
  - 05-pattern-comparator/01-pattern
difficulty: medium
---

# K closest values

## Problem Statement

Given the **root** of a binary search tree, a **target** value (real number), and a non-negative integer `k`, return the K values in the BST closest to `target`. Return them in any order.

### Example 1

> - **Input:** `root = [4, 2, 6, 1, null, null, 7]`, `target = 4.63`, `k = 3`
> - **Output:** `[4, 6, 7]`

### Example 2

> - **Input:** `root = [2, 1, 4, null, null, 3, 7]`, `target = 7.49`, `k = 2`
> - **Output:** `[4, 7]`

<details>
<summary><h2>The Strategy</h2></summary>


This is **Top-K-smallest by distance**, applied to a tree traversal. We walk the BST in any order (in-order is convenient), pushing each value paired with its absolute distance to the target. We use a **max-heap** of size K, where the top is the *farthest* of our current best K — the threshold we evict against.

The comparator: "compare by distance, descending" (so the farthest is on top of the max-heap).

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array viz-root=max_heap
from typing import List, Optional
import heapq

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


# Struct to store the value and its distance from the target
class ValueDiff:
    def __init__(self, diff: float, value: int):
        self.diff = diff
        self.value = value

    # Custom comparison for heapq (max-heap)
    def __lt__(self, other):
        return self.diff > other.diff

class Solution:
    def __init__(self):

        # Max heap to store the closest k values
        self.max_heap: List[ValueDiff] = []

    def inorder(
        self, root: Optional[TreeNode], target: float, k: int
    ) -> None:
        if root is None:
            return

        self.inorder(root.left, target, k)

        # Compute the absolute difference between node value and target
        diff = abs(root.val - target)

        # Push the current value and its difference to the max heap
        heapq.heappush(self.max_heap, ValueDiff(diff, root.val))

        # Ensure the heap only contains k elements
        if len(self.max_heap) > k:

            # Remove the farthest element
            heapq.heappop(self.max_heap)

        self.inorder(root.right, target, k)

    def k_closest_values(
        self, root: Optional[TreeNode], target: float, k: int
    ) -> List[int]:
        result: List[int] = []

        # Perform inorder traversal and fill the max heap with the
        # closest k values
        self.inorder(root, target, k)

        # Extract k closest values from the max heap
        while self.max_heap:
            result.append(heapq.heappop(self.max_heap).value)

        # The result is in reverse order, so reverse it
        result.reverse()

        return result


# Examples from the problem statement
t1 = from_level_order([4, 2, 6, 1, None, None, 7])
print(sorted(Solution().k_closest_values(t1, 4.63, 3)))   # [4, 6, 7]

t2 = from_level_order([2, 1, 4, None, None, 3, 7])
print(sorted(Solution().k_closest_values(t2, 7.49, 2)))   # [4, 7]

# Edge cases
t3 = from_level_order([5])
print(Solution().k_closest_values(t3, 3.0, 1))            # [5] — single node

t4 = from_level_order([4, 2, 6, 1, None, None, 7])
print(sorted(Solution().k_closest_values(t4, 1.0, 1)))    # [1] — exact match

t5 = from_level_order([4, 2, 6, 1, None, None, 7])
print(sorted(Solution().k_closest_values(t5, 4.0, 2)))    # [4, 2] or [4, 6] — ties
```

```java run viz=array viz-root=maxHeap
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

    // Struct to store the value and its distance from the target
    static class ValueDiff {

        double diff;
        int value;

        ValueDiff(double diff, int value) {
            this.diff = diff;
            this.value = value;
        }
    }

    // Comparator to create a max heap based on the difference
    static class CompareMaxHeap implements Comparator<ValueDiff> {
        public int compare(ValueDiff a, ValueDiff b) {

            // Max heap: larger diff has higher priority
            return Double.compare(b.diff, a.diff);
        }
    }

    static class Solution {

        // Max heap to store the closest k values
        private PriorityQueue<ValueDiff> maxHeap = new PriorityQueue<>(
            new CompareMaxHeap()
        );

        private void inorder(TreeNode root, double target, int k) {
            if (root == null) {
                return;
            }

            inorder(root.left, target, k);

            // Compute the absolute difference between node value and target
            double diff = Math.abs(root.val - target);

            // Push the current value and its difference to the max heap
            maxHeap.add(new ValueDiff(diff, root.val));

            // Ensure the heap only contains k elements
            if (maxHeap.size() > k) {

                // Remove the farthest element
                maxHeap.poll();
            }

            inorder(root.right, target, k);
        }

        public List<Integer> kClosestValues(
            TreeNode root,
            double target,
            int k
        ) {
            List<Integer> result = new ArrayList<>();

            // Perform inorder traversal and fill the max heap with the
            // closest k values
            inorder(root, target, k);

            // Extract k closest values from the max heap
            while (!maxHeap.isEmpty()) {
                result.add(maxHeap.poll().value);
            }

            // The result is in reverse order, so reverse it
            Collections.reverse(result);

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        TreeNode t1 = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        List<Integer> r1 = new Solution().kClosestValues(t1, 4.63, 3);
        Collections.sort(r1); System.out.println(r1);   // [4, 6, 7]

        TreeNode t2 = fromLevelOrder(2, 1, 4, null, null, 3, 7);
        List<Integer> r2 = new Solution().kClosestValues(t2, 7.49, 2);
        Collections.sort(r2); System.out.println(r2);   // [4, 7]

        // Edge cases
        TreeNode t3 = fromLevelOrder(5);
        System.out.println(new Solution().kClosestValues(t3, 3.0, 1));    // [5]

        TreeNode t4 = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        List<Integer> r4 = new Solution().kClosestValues(t4, 1.0, 1);
        System.out.println(r4);                                            // [1]

        TreeNode t5 = fromLevelOrder(4, 2, 6, 1, null, null, 7);
        List<Integer> r5 = new Solution().kClosestValues(t5, 4.0, 2);
        Collections.sort(r5); System.out.println(r5);                     // 2 closest to 4.0
    }
}
```

</details>
