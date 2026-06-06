---
title: "Cousin Check"
summary: "See problem statement below."
prereqs:
  - 15-pattern-level-order-traversal/01-pattern
difficulty: medium
---

# Problem 5 — Cousin check

> Two nodes are *cousins* if they're at the same depth and have *different* parents. Given two values `valA` and `valB`, return `true` iff their nodes are cousins.

Augment the BFS so each enqueued item carries *both* the node and its parent. As we walk a level, look for the two target values; if both are found on the same level *and* they have different parents, return `true`. If only one is found on a level, they're not at the same depth, return `false`.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from queue import Queue
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


# Define a class to store the node and its parent
class NodeInfo:
    def __init__(self, node, parent):
        self.node = node
        self.parent = parent


class Solution:
    def cousin_check(
        self, root: Optional[TreeNode], val_a: int, val_b: int
    ) -> bool:
        if not root:
            return False

        # Use a queue to store the nodes and their parents
        queue = Queue()
        queue.put(NodeInfo(root, None))

        # Loop through each level in the tree
        while not queue.empty():

            # Get the size of the current level
            level_size = queue.qsize()

            # Initialize the parent nodes for A and B
            parent_a, parent_b = None, None

            # Loop through each node in the current level
            for _ in range(level_size):

                # Get the node and the parent node for the first node
                # in the queue
                current = queue.get()
                node, parent = current.node, current.parent

                # Check and assign parents for A and B
                if node.val == val_a:
                    parent_a = parent

                if node.val == val_b:
                    parent_b = parent

                # Add the node's children to the queue if they exist
                if node.left:
                    queue.put(NodeInfo(node.left, node))

                if node.right:
                    queue.put(NodeInfo(node.right, node))

            # If both nodes found at the same level
            if parent_a and parent_b:
                return parent_a != parent_b

            # If only one is found, return false (not same depth)
            if parent_a or parent_b:
                return False

        # If neither node is found, return false
        return False


# Examples from the problem statement
print(Solution().cousin_check(from_level_order([1, 2, 3, 4, None, None, 7]), 4, 7))       # True
print(Solution().cousin_check(from_level_order([1, 8, 4, None, None, 2, 7, None, 9]), 2, 8))  # False

# Edge cases
print(Solution().cousin_check(None, 1, 2))                                                  # False
print(Solution().cousin_check(TreeNode(1), 1, 2))                                          # False
print(Solution().cousin_check(from_level_order([1, 2, 3]), 2, 3))                          # False (siblings not cousins)
print(Solution().cousin_check(from_level_order([1, 2, 3, 4, 5, 6, 7]), 4, 7))             # True (level 3, different parents)
print(Solution().cousin_check(from_level_order([1, 2, 3, 4, None, None, 7]), 2, 3))       # False (different depths)
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

    // Define a class to store the node and its parent
    static class NodeInfo {
        TreeNode node;
        TreeNode parent;
        NodeInfo(TreeNode node, TreeNode parent) {
            this.node = node;
            this.parent = parent;
        }
    }

    static class Solution {
        public boolean cousinCheck(TreeNode root, int valA, int valB) {
            if (root == null) {
                return false;
            }

            // Use a queue to store the nodes and their parents
            Queue<NodeInfo> queue = new LinkedList<>();
            queue.add(new NodeInfo(root, null));

            // Loop through each level in the tree
            while (!queue.isEmpty()) {

                // Get the size of the current level
                int levelSize = queue.size();

                // Initialize the parent nodes for A and B
                TreeNode parentA = null;
                TreeNode parentB = null;

                // Loop through each node in the current level
                for (int i = 0; i < levelSize; ++i) {

                    // Get the node and the parent node for the first node
                    // in the queue
                    NodeInfo current = queue.poll();
                    TreeNode node = current.node;
                    TreeNode parent = current.parent;

                    // Check and assign parents for A and B
                    if (node.val == valA) {
                        parentA = parent;
                    }

                    if (node.val == valB) {
                        parentB = parent;
                    }

                    // Add the node's children to the queue if they exist
                    if (node.left != null) {
                        queue.add(new NodeInfo(node.left, node));
                    }

                    if (node.right != null) {
                        queue.add(new NodeInfo(node.right, node));
                    }
                }

                // If both nodes found at the same level
                if (parentA != null && parentB != null) {
                    return parentA != parentB;
                }

                // If only one is found, return false (not same depth)
                if (parentA != null || parentB != null) {
                    return false;
                }
            }

            // If neither node is found, return false
            return false;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().cousinCheck(fromLevelOrder(1, 2, 3, 4, null, null, 7), 4, 7));       // true
        System.out.println(new Solution().cousinCheck(fromLevelOrder(1, 8, 4, null, null, 2, 7, null, 9), 2, 8));  // false

        // Edge cases
        System.out.println(new Solution().cousinCheck(null, 1, 2));                                              // false
        System.out.println(new Solution().cousinCheck(new TreeNode(1), 1, 2));                                  // false
        System.out.println(new Solution().cousinCheck(fromLevelOrder(1, 2, 3), 2, 3));                          // false (siblings not cousins)
        System.out.println(new Solution().cousinCheck(fromLevelOrder(1, 2, 3, 4, 5, 6, 7), 4, 7));             // true
        System.out.println(new Solution().cousinCheck(fromLevelOrder(1, 2, 3, 4, null, null, 7), 2, 3));       // false (different depths)
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Level-order is your hammer for any *horizontal* question about a tree. Three things to walk away with:

1. **`levelSize = queue.size()` is the entire trick.** That single snapshot at the top of each outer-loop iteration is what separates "flat BFS" from "BFS with level boundaries". Once it's muscle memory, every per-level question becomes mechanical.
2. **Enqueue children, not always non-null.** For most problems (sum, max, list per level) you skip null children. For *completeness* checks you enqueue them deliberately so you can spot gaps. The choice depends on the question.
3. **Augment the queue when you need parents.** The cousin-check trick — enqueueing `(node, parent)` pairs — generalises: any per-node side-info you need (depth, column, path-from-root, sibling) can travel alongside the node. Don't try to retrofit it; bake it into the queue's element type.

> *Coming up — the next lesson takes level-order to <strong>two dimensions</strong>. Instead of grouping nodes by their <em>level</em>, we'll group by their <em>horizontal column</em> — yielding the tree's "top view", "bottom view", and "vertical traversal". Same BFS engine, an extra coordinate per queue entry.*

</details>
