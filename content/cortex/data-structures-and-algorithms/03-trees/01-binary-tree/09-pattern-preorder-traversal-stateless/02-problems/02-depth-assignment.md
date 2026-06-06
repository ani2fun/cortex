---
title: "Depth Assignment"
summary: "See problem statement below."
prereqs:
  - 09-pattern-preorder-traversal-stateless/01-pattern
difficulty: medium
---

# Problem 2 — Depth assignment

> Given the root of a binary tree, update each node's value to its depth.
>
> **Example:** Input `[1, 2, 3, 4, null, null, 7]` → output `[0, 1, 1, 2, null, null, 2]`.

The accumulator is just the **current depth**. The root starts at 0; every recursive call passes `depth + 1` to the children.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from typing import Optional
from collections import deque

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


def to_level_order(root):
    if not root:
        return []
    result, queue = [], deque([root])
    while queue:
        node = queue.popleft()
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
    def depth_assignment_helper(
        self, root: Optional[TreeNode], depth: int
    ) -> None:

        # Base case: if the current node is null, do nothing
        if root is None:
            return

        # Update current node's value with its depth
        root.val = depth

        # Recursively process the left and right children,
        # increasing the depth by 1
        self.depth_assignment_helper(root.left, depth + 1)
        self.depth_assignment_helper(root.right, depth + 1)

    def depth_assignment(self, root: Optional[TreeNode]) -> None:
        self.depth_assignment_helper(root, 0)


# Examples from the problem statement
t1 = from_level_order([1, 2, 3, 4, None, None, 7])
Solution().depth_assignment(t1); print(to_level_order(t1))   # [0, 1, 1, 2, 2]

t2 = from_level_order([1, 8, 4, None, None, 2, 7])
Solution().depth_assignment(t2); print(to_level_order(t2))   # [0, 1, 1, 2, 2]

# Edge cases
t3 = from_level_order([])
Solution().depth_assignment(t3); print(to_level_order(t3))   # []

t4 = from_level_order([42])
Solution().depth_assignment(t4); print(to_level_order(t4))   # [0]

t5 = from_level_order([5, 3, None, 1])                       # left-skew
Solution().depth_assignment(t5); print(to_level_order(t5))   # [0, 1, 2]

t6 = from_level_order([5, None, 3, None, 1])                 # right-skew
Solution().depth_assignment(t6); print(to_level_order(t6))   # [0, 1, 2]

t7 = from_level_order([1, 2, 3, 4, 5, 6, 7])
Solution().depth_assignment(t7); print(to_level_order(t7))   # [0, 1, 1, 2, 2, 2, 2]
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

    static List<Integer> toLevelOrder(TreeNode root) {
        if (root == null) return new ArrayList<>();
        List<Integer> result = new ArrayList<>();
        java.util.Deque<TreeNode> queue = new java.util.ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node != null) {
                result.add(node.val);
                queue.add(node.left);
                queue.add(node.right);
            } else {
                result.add(null);
            }
        }
        while (!result.isEmpty() && result.get(result.size() - 1) == null)
            result.remove(result.size() - 1);
        return result;
    }

    static class Solution {

        private void depthAssignmentHelper(TreeNode root, int depth) {

            // Base case: if the current node is null, do nothing
            if (root == null) {
                return;
            }

            // Update current node's value with its depth
            root.val = depth;

            // Recursively process the left and right children,
            // increasing the depth by 1
            depthAssignmentHelper(root.left, depth + 1);
            depthAssignmentHelper(root.right, depth + 1);
        }

        public void depthAssignment(TreeNode root) {
            depthAssignmentHelper(root, 0);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        TreeNode t1 = fromLevelOrder(1, 2, 3, 4, null, null, 7);
        new Solution().depthAssignment(t1);
        System.out.println(toLevelOrder(t1));   // [0, 1, 1, 2, 2]

        TreeNode t2 = fromLevelOrder(1, 8, 4, null, null, 2, 7);
        new Solution().depthAssignment(t2);
        System.out.println(toLevelOrder(t2));   // [0, 1, 1, 2, 2]

        // Edge cases
        TreeNode t3 = fromLevelOrder();
        new Solution().depthAssignment(t3);
        System.out.println(toLevelOrder(t3));   // []

        TreeNode t4 = fromLevelOrder(42);
        new Solution().depthAssignment(t4);
        System.out.println(toLevelOrder(t4));   // [0]

        TreeNode t5 = fromLevelOrder(5, 3, null, 1);   // left-skew
        new Solution().depthAssignment(t5);
        System.out.println(toLevelOrder(t5));   // [0, 1, 2]

        TreeNode t6 = fromLevelOrder(5, null, 3, null, 1);   // right-skew
        new Solution().depthAssignment(t6);
        System.out.println(toLevelOrder(t6));   // [0, 1, 2]

        TreeNode t7 = fromLevelOrder(1, 2, 3, 4, 5, 6, 7);
        new Solution().depthAssignment(t7);
        System.out.println(toLevelOrder(t7));   // [0, 1, 1, 2, 2, 2, 2]
    }
}
```

</details>
