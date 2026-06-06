---
title: "Top View"
summary: "See problem statement below."
prereqs:
  - 16-pattern-level-order-traversal-columns/01-pattern
difficulty: medium
---

# Problem 1 — Top view

> Return the values of nodes visible *from above*, ordered left-to-right by column.
>
> A column's "top" is the *first* node BFS encounters in that column (BFS processes shallower nodes before deeper ones, so the first node in any column is its highest one).

The trick: when we visit a node and its column is **not yet** in the map, record it; otherwise skip. BFS guarantees the first arrival at any column is the topmost one.

> *Predict before reading on — would a depth-first traversal work for top view?*
>
> Not directly. DFS visits nodes in *recursion order*, not depth order, so the first node DFS hits in column −1 isn't necessarily the topmost. You'd need to remember each node's *depth* and only update the per-column entry when you find a *shallower* node — which is more work than just using BFS, where the first arrival is automatically the topmost.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from queue import Queue
from typing import List, Optional


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


# Define a class to store the node and its column index
class NodeInfo:
    def __init__(self, node: TreeNode, column: int) -> None:
        self.node = node
        self.column = column


class Solution:
    def top_view(self, root: Optional[TreeNode]) -> List[int]:
        result: List[int] = []
        if not root:
            return result

        # Hash map to store columns and their corresponding nodes
        columns: dict[int, int] = {}

        # Use a queue to perform a level-order traversal of the tree
        queue = Queue()
        queue.put(NodeInfo(root, 0))

        # Loop through each level in the tree
        while not queue.empty():
            current = queue.get()
            node = current.node
            column = current.column

            # Add the current node if it's the first node in the column
            if column not in columns:
                columns[column] = node.val

            # Enqueue the left child with column - 1
            if node.left:
                queue.put(NodeInfo(node.left, column - 1))

            # Enqueue the right child with column + 1
            if node.right:
                queue.put(NodeInfo(node.right, column + 1))

        # Iterate over the columns in the hash map and add them to the
        # result
        for column in sorted(columns):
            result.append(columns[column])

        return result


# Examples from the problem statement
print(Solution().top_view(from_level_order([1, 2, 3, 4, None, None, 7, 9])))              # [9, 4, 2, 1, 3, 7]
print(Solution().top_view(from_level_order([1, 8, 4, None, 6, None, None, None, 2, None, 9])))  # [8, 1, 4, 9]

# Edge cases
print(Solution().top_view(None))                                                           # []
print(Solution().top_view(TreeNode(1)))                                                    # [1]
print(Solution().top_view(from_level_order([1, 2, None, 3, None, 4])))                   # [3, 2, 1] left skew
print(Solution().top_view(from_level_order([1, None, 2, None, None, None, 3])))          # [1, 2, 3] right skew
print(Solution().top_view(from_level_order([1, 2, 3])))                                  # [2, 1, 3] balanced
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

    // Define a class to store the node and its column index
    static class NodeInfo {
        TreeNode node;
        int column;
        NodeInfo(TreeNode node, int column) {
            this.node = node;
            this.column = column;
        }
    }

    static class Solution {
        public List<Integer> topView(TreeNode root) {
            List<Integer> result = new ArrayList<>();
            if (root == null) {
                return result;
            }

            // Hash map to store columns and their corresponding nodes
            Map<Integer, Integer> columns = new TreeMap<>();

            // Use a queue to perform a level-order traversal of the tree
            Queue<NodeInfo> queue = new LinkedList<>();

            // Push the root node onto the queue with column index 0
            queue.add(new NodeInfo(root, 0));

            // Loop through each level in the tree
            while (!queue.isEmpty()) {
                NodeInfo current = queue.poll();
                TreeNode node = current.node;
                int column = current.column;

                // Add the current node if it's the first node in the column
                if (!columns.containsKey(column)) {
                    columns.put(column, node.val);
                }

                // Enqueue the left child with column - 1
                if (node.left != null) {
                    queue.add(new NodeInfo(node.left, column - 1));
                }

                // Enqueue the right child with column + 1
                if (node.right != null) {
                    queue.add(new NodeInfo(node.right, column + 1));
                }
            }

            // Iterate over the columns in the hash map and add them to the
            // result
            for (int column : columns.keySet()) {
                result.add(columns.get(column));
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().topView(fromLevelOrder(1, 2, 3, 4, null, null, 7, 9)));              // [9, 4, 2, 1, 3, 7]
        System.out.println(new Solution().topView(fromLevelOrder(1, 8, 4, null, 6, null, null, null, 2, null, 9)));  // [8, 1, 4, 9]

        // Edge cases
        System.out.println(new Solution().topView(null));                                                       // []
        System.out.println(new Solution().topView(new TreeNode(1)));                                           // [1]
        System.out.println(new Solution().topView(fromLevelOrder(1, 2, null, 3)));                            // [3, 2, 1] left skew
        System.out.println(new Solution().topView(fromLevelOrder(1, null, 2, null, null, null, 3)));          // [1, 2, 3] right skew
        System.out.println(new Solution().topView(fromLevelOrder(1, 2, 3)));                                  // [2, 1, 3]
    }
}
```

</details>
