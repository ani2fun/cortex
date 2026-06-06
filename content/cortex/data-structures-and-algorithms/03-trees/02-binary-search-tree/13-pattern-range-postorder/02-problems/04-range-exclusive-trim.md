---
title: "Range Exclusive Trim"
summary: "Given the root of a BST and two values low and high, return a new BST that contains *only* the nodes whose values lie in [low, high]. The relative structure must be preserved — if A was a descendant o"
prereqs:
  - 13-pattern-range-postorder/01-pattern
difficulty: hard
---

# Range exclusive trim

## Problem Statement

Given the **root** of a BST and two values `low` and `high`, return a new BST that contains *only* the nodes whose values lie in `[low, high]`. The relative structure must be preserved — if `A` was a descendant of `B` in the original and both survive the trim, `A` must remain a descendant of `B` in the result.

### Example 1

> - **Input:** `root = [4, 2, 5, 1, 3, null, 6]`, `low = 2`, `high = 5`
> - **Output:** `[4, 2, 5, null, 3]`

### Example 2

> - **Input:** `root = [5, 1, 8, null, null, 6, 9]`, `low = 6`, `high = 9`
> - **Output:** `[8, 6, 9]`

<details>
<summary><h2>The Strategy</h2></summary>


The same pruning rules drive a *structural rewrite*:

- If `node.val < low`, the entire left subtree is out of range; we **don't recurse left** at all. Return the trim of the right subtree as our replacement.
- If `node.val > high`, mirror — return the trim of the left subtree.
- Otherwise (`node.val` in range), the node survives. Trim both children recursively and re-attach.

The `return` value is the new root of *this* subtree after trimming, which the caller wires back into its own children pointers — exactly the same shape as the recursive insertion idiom we used in lesson 5.

</details>
<details>
<summary><h2>The Solution</h2></summary>



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
        result.append(node.val)
        if node.left:
            queue.append(node.left)
        if node.right:
            queue.append(node.right)
    return result


class Solution:
    def range_exclusive_trim(
        self, root: Optional[TreeNode], low: int, high: int
    ) -> Optional[TreeNode]:

        # Base Case: If root is null, return null
        if root is None:
            return None

        # If the node's value is less than the lower bound,
        # discard the left subtree and trim the right subtree
        if root.val < low:
            return self.range_exclusive_trim(root.right, low, high)

        # If the node's value is greater than the upper bound,
        # discard the right subtree and trim the left subtree
        if root.val > high:
            return self.range_exclusive_trim(root.left, low, high)

        # If the node's value is within the range [low, high],
        # recursively trim its left and right subtrees
        root.left = self.range_exclusive_trim(root.left, low, high)
        root.right = self.range_exclusive_trim(root.right, low, high)

        # Return the trimmed root
        return root


# Examples from the problem statement
t1 = from_level_order([4, 2, 5, 1, 3, None, 6])
print(to_level_order(Solution().range_exclusive_trim(t1, 2, 5)))  # [4, 2, 5, 3]

t2 = from_level_order([5, 1, 8, None, None, 6, 9])
print(to_level_order(Solution().range_exclusive_trim(t2, 6, 9)))  # [8, 6, 9]

# Edge cases
print(Solution().range_exclusive_trim(None, 1, 5))               # None

t3 = from_level_order([5])
print(to_level_order(Solution().range_exclusive_trim(t3, 1, 5))) # [5]

t4 = from_level_order([5])
print(Solution().range_exclusive_trim(t4, 6, 10))                # None — root out of range

t5 = from_level_order([4, 2, 6, 1, 3, 5, 7])
print(to_level_order(Solution().range_exclusive_trim(t5, 3, 5))) # [4, 3, 5]

t6 = from_level_order([4, 2, 6, 1, 3, 5, 7])
print(to_level_order(Solution().range_exclusive_trim(t6, 1, 7))) # [4, 2, 6, 1, 3, 5, 7]
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
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;
        java.util.Deque<TreeNode> queue = new java.util.ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            result.add(node.val);
            if (node.left != null) queue.add(node.left);
            if (node.right != null) queue.add(node.right);
        }
        return result;
    }

    static class Solution {
        TreeNode rangeExclusiveTrim(
            TreeNode root,
            int low,
            int high
        ) {

            // Base Case: If root is null, return null
            if (root == null) {
                return null;
            }

            // If the node's value is less than the lower bound,
            // discard the left subtree and trim the right subtree
            if (root.val < low) {
                return rangeExclusiveTrim(root.right, low, high);
            }

            // If the node's value is greater than the upper bound,
            // discard the right subtree and trim the left subtree
            if (root.val > high) {
                return rangeExclusiveTrim(root.left, low, high);
            }

            // If the node's value is within the range [low, high],
            // recursively trim its left and right subtrees
            root.left = rangeExclusiveTrim(root.left, low, high);
            root.right = rangeExclusiveTrim(root.right, low, high);

            // Return the trimmed root
            return root;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        TreeNode t1 = fromLevelOrder(4, 2, 5, 1, 3, null, 6);
        System.out.println(toLevelOrder(new Solution().rangeExclusiveTrim(t1, 2, 5)));  // [4, 2, 5, 3]

        TreeNode t2 = fromLevelOrder(5, 1, 8, null, null, 6, 9);
        System.out.println(toLevelOrder(new Solution().rangeExclusiveTrim(t2, 6, 9)));  // [8, 6, 9]

        // Edge cases
        System.out.println(new Solution().rangeExclusiveTrim(null, 1, 5));              // null

        TreeNode t3 = fromLevelOrder(5);
        System.out.println(toLevelOrder(new Solution().rangeExclusiveTrim(t3, 1, 5))); // [5]

        TreeNode t4 = fromLevelOrder(5);
        System.out.println(new Solution().rangeExclusiveTrim(t4, 6, 10));              // null — root out of range

        TreeNode t5 = fromLevelOrder(4, 2, 6, 1, 3, 5, 7);
        System.out.println(toLevelOrder(new Solution().rangeExclusiveTrim(t5, 3, 5))); // [4, 3, 5]

        TreeNode t6 = fromLevelOrder(4, 2, 6, 1, 3, 5, 7);
        System.out.println(toLevelOrder(new Solution().rangeExclusiveTrim(t6, 1, 7))); // [4, 2, 6, 1, 3, 5, 7]
    }
}
```


<details>
<summary><strong>Trace — root = [4, 2, 5, 1, 3, null, 6], range = [2, 5]</strong></summary>

```
trim(4, [2,5]) │ 4 in range → trim(2), trim(5), keep 4
trim(2, [2,5]) │ 2 in range → trim(1), trim(3), keep 2
trim(1, [2,5]) │ 1 < 2  → drop 1 (and its subtree); return trim(null) = null
trim(3, [2,5]) │ 3 in range → trim(null), trim(null) → keep 3 as leaf
trim(5, [2,5]) │ 5 in range → trim(null), trim(6), keep 5
trim(6, [2,5]) │ 6 > 5  → drop 6; return trim(null) = null
After all trims: [4, 2, 5, null, 3, null, null] ≡ [4, 2, 5, null, 3] ✓
```

</details>

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Range Postorder = postorder + BST pruning. Whenever a problem asks for an aggregate (sum, count, height, structural rewrite) over the nodes of a BST whose values fall in a range, this is the right tool. The pruning rules collapse out-of-range subtrees in O(1) — not by walking them — and the postorder structure cleanly reduces children's results into a parent's.

Three patterns to keep:

1. **The "two prunes + recurse" structure** is universal for range-bounded BST problems. Once you internalise it, range sum / range count / range diameter / range trim all collapse to a 4-line skeleton with one problem-specific reduction.
2. **Postorder is for "value depends on what's below me"** — diameter, sum, count of leaves, validity checks like "subtree is BST", segment-tree-style queries. Whenever the parent's answer is computed *from* the children's, you're in postorder territory.
3. **Returning the trimmed subtree to the parent** is the same idiom we used for insertion (lesson 5) and deletion (lesson 6): every recursive call returns a pointer to the (possibly modified) subtree, and the caller wires it into its own pointer field.

The next lesson swaps **one descent** for **two pointers** — running a forward iterator and a reverse iterator simultaneously across the BST's sorted sequence. That single move unlocks the classic "two values that sum to target" family of problems on a tree, in O(n) time and O(h) space.

</details>
