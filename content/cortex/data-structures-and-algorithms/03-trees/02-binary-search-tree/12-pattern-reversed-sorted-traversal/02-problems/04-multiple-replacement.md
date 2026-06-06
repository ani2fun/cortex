---
title: "Multiple Replacement"
summary: "Given the root of a binary search tree, replace each node's value with 0 if its inorder predecessor's value (the value just *larger* than it in sorted order) is a non-zero multiple of its own value."
prereqs:
  - 12-pattern-reversed-sorted-traversal/01-pattern
difficulty: hard
---

# Multiple replacement

## Problem Statement

Given the **root** of a binary search tree, replace each node's value with `0` if its **inorder predecessor's** value (the value just *larger* than it in sorted order) is a non-zero multiple of its own value.

### Example 1

> - **Input:** `root = [6, 2, 5, 1, 4, null, 10]`
> - **Output:** `[6, 0, 0, 0, 4, null, 10]`

### Example 2

> - **Input:** `root = [5, 4, 10, null, null, 9, 11]`
> - **Output:** `[5, 4, 10, null, null, 9, 11]`

<details>
<summary><h2>The Strategy</h2></summary>


The trick word is **predecessor**. Inside a reverse-in-order walk, each node's *previous-visited* node is the next-larger value in sorted order — exactly the "successor's value" the problem asks about (the problem statement's wording is slightly confusing, but the example outputs confirm: we compare each node to the value *larger* than it).

So:

- Maintain a `prev_val` that holds the most recently visited (i.e. larger) node's *original* value.
- At each node, if `prev_val % current.val == 0` and `prev_val != 0`, set the current node to `0`.
- *Then* update `prev_val` to the current node's original value (not the possibly-zeroed one) before recursing left.

The "save the original first" detail is the trap that catches careless implementations.

</details>
<details>
<summary><h2>The Solution</h2></summary>



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


def level_order_vals(root):
    if not root:
        return []
    result, queue = [], [root]
    while queue:
        node = queue.pop(0)
        if node:
            result.append(node.val)
            queue.append(node.left)
            queue.append(node.right)
        else:
            result.append(None)
    while result and result[-1] is None:
        result.pop()
    return result


class Solution:
    def __init__(self) -> None:

        # Variable to keep track of the previous node
        self.prev_node_val: int = 0

        # Flag to check if previous node exists
        self.has_prev_node: bool = False

    def multiple_replacement(self, root: Optional[TreeNode]) -> None:

        # Base case
        if root is None:
            return

        # Recursively process the right subtree
        self.multiple_replacement(root.right)

        # Store the original value of the current node
        original_val = root.val

        # If the previous node's value is a multiple of the current node's
        # value, replace the current node's value with 0
        if (
            self.has_prev_node
            and self.prev_node_val != 0
            and self.prev_node_val % root.val == 0
        ):
            root.val = 0

        # Update the previous node to the current node's original value
        self.prev_node_val = original_val

        # Set the flag to true indicating that previous
        # node exists
        self.has_prev_node = True

        # Recursively process the left subtree
        self.multiple_replacement(root.left)


# Example 1: [6, 2, 5, 1, 4, null, 10] → [6, 0, 0, 0, 4, null, 10]
t1 = from_level_order([6, 2, 5, 1, 4, None, 10])
Solution().multiple_replacement(t1)
print(level_order_vals(t1))   # [6, 0, 0, 0, 4, 10]

# Example 2: no replacements
t2 = from_level_order([5, 4, 10, None, None, 9, 11])
Solution().multiple_replacement(t2)
print(level_order_vals(t2))   # [5, 4, 10, 9, 11]

# Edge cases
Solution().multiple_replacement(None)   # no-op

t3 = from_level_order([7])
Solution().multiple_replacement(t3)
print(t3.val)                 # 7  (single node)

# Two-node tree: root=2, right=4 → 4 % 2 == 0, so root becomes 0
t4 = from_level_order([2, None, 4])
Solution().multiple_replacement(t4)
print(t4.val, t4.right.val)   # 0 4

# Two-node tree: root=3, right=5 → 5 % 3 != 0, no change
t5 = from_level_order([3, None, 5])
Solution().multiple_replacement(t5)
print(t5.val, t5.right.val)   # 3 5
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

    static List<Integer> levelOrderVals(TreeNode root) {
        if (root == null) return List.of();
        List<Integer> result = new ArrayList<>();
        java.util.Deque<TreeNode> q = new java.util.ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            TreeNode node = q.poll();
            result.add(node.val);
            if (node.left != null) q.add(node.left);
            if (node.right != null) q.add(node.right);
        }
        return result;
    }

    static class Solution {

        // Variable to keep track of the value of the
        // previous node
        private int prevNodeVal;

        // Flag to check if previous node exists
        private boolean hasPrevNode = false;

        public void multipleReplacement(TreeNode root) {

            // Base case
            if (root == null) {
                return;
            }

            // Recursively process the right subtree
            multipleReplacement(root.right);

            // Store the original value of the current node
            int originalVal = root.val;

            // If the previous node's value is a multiple of the current
            // node's value, replace the current node's value with 0
            if (
                hasPrevNode &&
                prevNodeVal != 0 &&
                prevNodeVal % root.val == 0
            ) {
                root.val = 0;
            }

            // Update the previous node to the current node's original value
            prevNodeVal = originalVal;

            // Set the flag to true indicating that previous
            // node exists
            hasPrevNode = true;

            // Recursively process the left subtree
            multipleReplacement(root.left);
        }
    }

    public static void main(String[] args) {
        // Example 1
        TreeNode t1 = fromLevelOrder(6, 2, 5, 1, 4, null, 10);
        new Solution().multipleReplacement(t1);
        System.out.println(levelOrderVals(t1));   // [6, 0, 0, 0, 4, 10]

        // Example 2: no replacements
        TreeNode t2 = fromLevelOrder(5, 4, 10, null, null, 9, 11);
        new Solution().multipleReplacement(t2);
        System.out.println(levelOrderVals(t2));   // [5, 4, 10, 9, 11]

        // Edge cases
        new Solution().multipleReplacement(null);  // no-op

        TreeNode t3 = fromLevelOrder(7);
        new Solution().multipleReplacement(t3);
        System.out.println(t3.val);               // 7

        // root=2, right=4 → 4 % 2 == 0, root becomes 0
        TreeNode t4 = fromLevelOrder(2, null, 4);
        new Solution().multipleReplacement(t4);
        System.out.println(t4.val + " " + t4.right.val);  // 0 4

        // root=3, right=5 → 5 % 3 != 0, no change
        TreeNode t5 = fromLevelOrder(3, null, 5);
        new Solution().multipleReplacement(t5);
        System.out.println(t5.val + " " + t5.right.val);  // 3 5
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Reverse in-order is the descending dual of in-order. Whenever the problem cares about *the largest values first* — k-th largest, ranks from the top, suffix sums by value, comparisons against the next-larger node — flip the recursion direction and the same template applies. Most reverse-sorted-traversal problems are *easy* because the BST has done the sorting for you.

Three patterns to keep:

1. **"Carry the previous-larger value"** — mirror of lesson 10's "carry the previous-smaller". Useful for any pairwise check that runs against the next-larger value (multiples, ratios, ranges, monotonicity).
2. **"Running total over the descending sequence"** — solves *enriched sum tree*, but the same shape solves "sum of values strictly greater than X", "convert to suffix-sum array", "decorate node with `(num greater, sum greater)`".
3. **"Save original before overwriting"** — the trap in `multiple replacement`. When a traversal both *reads* and *writes* the same field, capture the read into a local before the write — your future self will thank you.

The next lesson introduces a new pattern that breaks the "always traverse all nodes" pattern from the last two lessons: **range postorder**, where the BST property lets us *prune* entire subtrees that can't contribute to the answer.

</details>
