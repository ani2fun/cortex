---
title: "Merge Trees"
summary: "See problem statement below."
prereqs:
  - 18-pattern-simultaneous-traversal/01-pattern
difficulty: medium
---

# Problem 4 — Merge trees

> Overlay two trees. At every position where both trees have a node, sum the values. Where only one has a node, keep that node. Return the merged tree.

A *constructive* simultaneous traversal — instead of returning a verdict, return a *new node* built from the two inputs at each step.

<details>
<summary><h2>Solution</h2></summary>



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


def to_level_order(root):
    """Serialize tree back to level-order list for easy comparison."""
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
    def merge_trees(
        self, rootA: Optional[TreeNode], rootB: Optional[TreeNode]
    ) -> Optional[TreeNode]:

        # If either of the trees is None, return the other tree
        if rootA is None:
            return rootB

        if rootB is None:
            return rootA

        # Create a new node with the sum of values from rootA and rootB
        merged_node = TreeNode(rootA.val + rootB.val)

        # Recursively merge the left subtrees of rootA and rootB
        merged_node.left = self.merge_trees(rootA.left, rootB.left)

        # Recursively merge the right subtrees of rootA and rootB
        merged_node.right = self.merge_trees(rootA.right, rootB.right)

        # Return the merged tree
        return merged_node


# Examples from the problem statement
r1 = Solution().merge_trees(
    from_level_order([1, 2, 3, 4, None, 5, 6]),
    from_level_order([7, 8, 9, 10, 11, None, 12])
)
print(to_level_order(r1))   # [8, 10, 12, 14, 11, 5, 18]

r2 = Solution().merge_trees(
    from_level_order([1, 8, 4, None, None, 2, 7]),
    from_level_order([1, 2, 3, 4, None, None, 7])
)
print(to_level_order(r2))   # [2, 10, 7, 4, None, 2, 14]

# Edge cases
print(to_level_order(Solution().merge_trees(None, None)))              # []
print(to_level_order(Solution().merge_trees(TreeNode(5), None)))       # [5]
print(to_level_order(Solution().merge_trees(None, TreeNode(3))))       # [3]
print(to_level_order(Solution().merge_trees(TreeNode(1), TreeNode(2))))  # [3]
print(to_level_order(Solution().merge_trees(
    from_level_order([1, 3, 2, 5]),
    from_level_order([2, 1, 3, None, 4, None, 7])
)))   # [3, 4, 5, 5, 4, None, 7]
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
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);
        while (!q.isEmpty()) {
            TreeNode n = q.poll();
            if (n != null) {
                result.add(n.val);
                q.add(n.left);
                q.add(n.right);
            } else {
                result.add(null);
            }
        }
        while (!result.isEmpty() && result.get(result.size() - 1) == null)
            result.remove(result.size() - 1);
        return result;
    }

    static class Solution {
        public TreeNode mergeTrees(TreeNode rootA, TreeNode rootB) {

            // If either of the trees is null, return the other tree
            if (rootA == null) {
                return rootB;
            }

            if (rootB == null) {
                return rootA;
            }

            // Create a new node with the sum of values from rootA and rootB
            TreeNode mergedNode = new TreeNode(rootA.val + rootB.val);

            // Recursively merge the left subtrees of rootA and rootB
            mergedNode.left = mergeTrees(rootA.left, rootB.left);

            // Recursively merge the right subtrees of rootA and rootB
            mergedNode.right = mergeTrees(rootA.right, rootB.right);

            // Return the merged tree
            return mergedNode;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(toLevelOrder(new Solution().mergeTrees(
            fromLevelOrder(1, 2, 3, 4, null, 5, 6),
            fromLevelOrder(7, 8, 9, 10, 11, null, 12)
        )));   // [8, 10, 12, 14, 11, 5, 18]

        System.out.println(toLevelOrder(new Solution().mergeTrees(
            fromLevelOrder(1, 8, 4, null, null, 2, 7),
            fromLevelOrder(1, 2, 3, 4, null, null, 7)
        )));   // [2, 10, 7, 4, null, 2, 14]

        // Edge cases
        System.out.println(toLevelOrder(new Solution().mergeTrees(null, null)));              // []
        System.out.println(toLevelOrder(new Solution().mergeTrees(new TreeNode(5), null)));   // [5]
        System.out.println(toLevelOrder(new Solution().mergeTrees(null, new TreeNode(3))));   // [3]
        System.out.println(toLevelOrder(new Solution().mergeTrees(new TreeNode(1), new TreeNode(2))));  // [3]
        System.out.println(toLevelOrder(new Solution().mergeTrees(
            fromLevelOrder(1, 3, 2, 5),
            fromLevelOrder(2, 1, 3, null, 4, null, 7)
        )));   // [3, 4, 5, 5, 4, null, 7]
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Simultaneous traversal extends every single-tree recursive shape to a two-tree shape. Three things to walk away with:

1. **Three null cases, not two.** Both null = base case. Exactly one null = mismatch. Both non-null = recurse. Handle them in *exactly* that order at the top of the function and the rest of the algorithm is mechanical.
2. **Mirror is identical with swapped children.** Swap which children you recurse into and the algorithm shifts from "are these the same?" to "are these mirror images of each other?". Same recipe; a single line difference.
3. **Compose patterns to climb up.** Subtree detection nests `identical` *inside* a single-tree DFS — two patterns stacked. Most "advanced" tree problems are composed of two or three patterns this way; once each individual pattern is muscle memory, the compositions become natural.

> *Coming up — the chapter closes with a **practice mix-traversals** lesson — a single problem (the *boundary traversal*) that requires you to combine three of the patterns you've learned (root-to-leaf for the leaf row; left-spine and right-spine walks for the two sides) into a single coherent answer. It's the chapter's capstone.*

</details>
