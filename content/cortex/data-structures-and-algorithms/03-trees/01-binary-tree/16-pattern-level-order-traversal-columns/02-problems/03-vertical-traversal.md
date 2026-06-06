---
title: "Vertical Traversal"
summary: "See problem statement below."
prereqs:
  - 16-pattern-level-order-traversal-columns/01-pattern
difficulty: medium
---

# Problem 3 — Vertical traversal

> Return *all* nodes grouped by column (top-to-bottom within each column), as a list-of-lists ordered by column from left to right.

Trick: instead of storing one value per column (top or bottom view), *append* to a list per column. BFS top-to-bottom order means the per-column list is already sorted top-to-bottom for free.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from queue import Queue
from collections import defaultdict
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
    def __init__(self, node: TreeNode, column: int):
        self.node = node
        self.column = column


class Solution:
    def vertical_traversal(
        self, root: Optional[TreeNode]
    ) -> List[List[int]]:
        result: List[List[int]] = []
        if not root:
            return result

        # HashMap to store columns and their corresponding nodes
        columns = defaultdict(list)

        # Queue to perform level-order traversal
        queue = Queue()
        queue.put(NodeInfo(root, 0))

        # Loop through each level in the tree
        while not queue.empty():
            current = queue.get()
            node = current.node
            column = current.column

            # Add the current node to its corresponding column
            columns[column].append(node.val)

            # Enqueue the left child with column - 1
            if node.left:
                queue.put(NodeInfo(node.left, column - 1))

            # Enqueue the right child with column + 1
            if node.right:
                queue.put(NodeInfo(node.right, column + 1))

        # Sort columns by their column index and add them to the result
        for column in sorted(columns.keys()):
            result.append(columns[column])

        return result


# Examples from the problem statement
print(Solution().vertical_traversal(from_level_order([1, 2, 3, 4, None, None, 7])))        # [[4], [2], [1], [3], [7]]
print(Solution().vertical_traversal(from_level_order([1, 8, 4, None, 6, None, None, 3, 2])))  # [[8, 3], [1, 6], [4, 2]]

# Edge cases
print(Solution().vertical_traversal(None))                                                   # []
print(Solution().vertical_traversal(TreeNode(1)))                                            # [[1]]
print(Solution().vertical_traversal(from_level_order([1, 2, None, 3, None, 4])))           # [[4], [3], [2], [1]] left skew
print(Solution().vertical_traversal(from_level_order([1, None, 2, None, None, None, 3])))  # [[1], [2], [3]] right skew
print(Solution().vertical_traversal(from_level_order([1, 2, 3])))                          # [[2], [1], [3]]
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
        public List<List<Integer>> verticalTraversal(TreeNode root) {
            List<List<Integer>> result = new ArrayList<>();
            if (root == null) {
                return result;
            }

            // HashMap to store columns and their corresponding nodes
            Map<Integer, List<Integer>> columns = new TreeMap<>();

            // Queue to perform level-order traversal
            Queue<NodeInfo> queue = new LinkedList<>();
            queue.add(new NodeInfo(root, 0));

            // Loop through each level in the tree
            while (!queue.isEmpty()) {
                NodeInfo current = queue.poll();
                TreeNode node = current.node;
                int column = current.column;

                // Add the current node to its corresponding column
                columns.putIfAbsent(column, new ArrayList<>());
                columns.get(column).add(node.val);

                // Enqueue the left child with column - 1
                if (node.left != null) {
                    queue.add(new NodeInfo(node.left, column - 1));
                }

                // Enqueue the right child with column + 1
                if (node.right != null) {
                    queue.add(new NodeInfo(node.right, column + 1));
                }
            }

            // Iterate over the columns in the hash table and add them to the
            // result
            for (List<Integer> column : columns.values()) {
                result.add(column);
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().verticalTraversal(fromLevelOrder(1, 2, 3, 4, null, null, 7)));        // [[4], [2], [1], [3], [7]]
        System.out.println(new Solution().verticalTraversal(fromLevelOrder(1, 8, 4, null, 6, null, null, 3, 2)));  // [[8, 3], [1, 6], [4, 2]]

        // Edge cases
        System.out.println(new Solution().verticalTraversal(null));                                               // []
        System.out.println(new Solution().verticalTraversal(new TreeNode(1)));                                   // [[1]]
        System.out.println(new Solution().verticalTraversal(fromLevelOrder(1, 2, null, 3)));                    // left skew
        System.out.println(new Solution().verticalTraversal(fromLevelOrder(1, null, 2, null, null, null, 3)));  // right skew
        System.out.println(new Solution().verticalTraversal(fromLevelOrder(1, 2, 3)));                          // [[2], [1], [3]]
    }
}
```

</details>
