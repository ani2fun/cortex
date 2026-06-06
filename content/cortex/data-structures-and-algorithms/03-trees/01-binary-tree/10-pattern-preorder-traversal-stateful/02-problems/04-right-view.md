---
title: "Right View"
summary: "See problem statement below."
prereqs:
  - 10-pattern-preorder-traversal-stateful/01-pattern
difficulty: medium
---

# Problem 4 — Right view

> Same as the left view, but from the right side. Tree `[1, 2, 3, 4, null, null, 7, 9]` → `[1, 3, 7, 9]`.

The trick is *identical* to the left view, with one swap: recurse **right before left**. The first node visited at each new level is now the rightmost.

<details>
<summary><h2>Solution</h2></summary>



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


class Solution:
    def __init__(self):

        # Global variable to keep track of the current level during
        # recursion
        self.max_level_reached = 0

    def right_view_helper(
        self, root: Optional[TreeNode], level: int, result: List[int]
    ) -> None:
        if not root:
            return

        # If this is the first node of the current level, add it to
        # result
        if level == self.max_level_reached:
            result.append(root.val)

            # Increment the level after adding the node to result
            self.max_level_reached += 1

        # Recur for right, then left (ensures rightmost nodes are visited
        # first)
        self.right_view_helper(root.right, level + 1, result)
        self.right_view_helper(root.left, level + 1, result)

    def right_view(self, root: Optional[TreeNode]) -> List[int]:

        # Stores the right view of the binary tree
        result = []

        # Find the right view of the binary tree
        self.right_view_helper(root, 0, result)

        # Return the right view of the binary tree
        return result


# Examples from the problem statement
print(Solution().right_view(from_level_order([1, 2, 3, 4, None, None, 7, 9])))  # [1, 3, 7, 9]
print(Solution().right_view(from_level_order([1, 8, 4, None, None, 2, 7])))     # [1, 4, 7]

# Edge cases
print(Solution().right_view(None))                                                # []
print(Solution().right_view(from_level_order([5])))                               # [5]
print(Solution().right_view(from_level_order([1, None, 2, None, 3])))             # [1, 2, 3] (right-skew)
print(Solution().right_view(from_level_order([1, 2, None, 3])))                   # [1, 2, 3] (left-skew)
print(Solution().right_view(from_level_order([1, 2, 3, 4, 5, 6, 7])))            # [1, 3, 7]
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

        // Global variable to keep track of the current level during
        // recursion
        private int maxLevelReached = 0;

        private void rightViewHelper(
            TreeNode root,
            int level,
            List<Integer> result
        ) {
            if (root == null) {
                return;
            }

            // If this is the first node of the current level, add it to
            // result
            if (level == maxLevelReached) {
                result.add(root.val);

                // Increment the level after adding the node to result
                maxLevelReached++;
            }

            // Recur for right, then left (ensures rightmost nodes are
            // visited first)
            rightViewHelper(root.right, level + 1, result);
            rightViewHelper(root.left, level + 1, result);
        }

        public List<Integer> rightView(TreeNode root) {

            // Stores the right view of the binary tree
            List<Integer> result = new ArrayList<>();

            // Find the right view of the binary tree
            rightViewHelper(root, 0, result);

            // Return the right view of the binary tree
            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().rightView(fromLevelOrder(1, 2, 3, 4, null, null, 7, 9)));  // [1, 3, 7, 9]
        System.out.println(new Solution().rightView(fromLevelOrder(1, 8, 4, null, null, 2, 7)));     // [1, 4, 7]

        // Edge cases
        System.out.println(new Solution().rightView(null));                                           // []
        System.out.println(new Solution().rightView(fromLevelOrder(5)));                              // [5]
        System.out.println(new Solution().rightView(fromLevelOrder(1, null, 2, null, 3)));            // [1, 2, 3] (right-skew)
        System.out.println(new Solution().rightView(fromLevelOrder(1, 2, null, 3)));                  // [1, 2, 3] (left-skew)
        System.out.println(new Solution().rightView(fromLevelOrder(1, 2, 3, 4, 5, 6, 7)));           // [1, 3, 7]
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The stateful preorder pattern is the second-most-common shape in the chapter. Three things to walk away with:

1. **Push then recurse then pop.** When the state is a mutable collection, the discipline is sacred: push on entry, both recursions, pop on exit. Forgetting the pop is the canonical "my answer is way too big" backtracking bug. *Always* check that every push has a paired pop on every code path.
2. **Not every "shared mutable" is push-pop.** Monotone witnesses (min/max/best-so-far) and visit-order witnesses (first-at-each-level) share a mutable across the recursion *without* needing pop, because their updates are inherently global facts (or because the visit order itself encodes the bookkeeping).
3. **Left-vs-right preference is what gives "first" its meaning.** The view problems all turn on which child you recurse into *first*. Left view: left first. Right view: right first. Top view: process by level *and* horizontal distance. Generalise this — whenever a problem says "first / leftmost / rightmost / topmost", the *recursion order* is doing the work.

> *Coming up — the chapter pivots from the downward-flowing preorder patterns to the upward-flowing <strong>postorder</strong> patterns. Where preorder hands data <em>from parent to child</em>, postorder gathers data <em>from children to parent</em>. The next two lessons (stateless and stateful postorder) cover heights, sums, diameters, and a wealth of other "compute the answer at each node from its subtrees' answers" problems.*

</details>
