---
title: "Most Frequent Subtree Sum"
summary: "See problem statement below."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: medium
---

# Problem 4 — Most frequent subtree sum

> The "subtree sum" of a node is the sum of values in its subtree. Return all subtree sums whose frequency in the tree is highest.

Each call returns its subtree sum (so the parent can compute its own); along the way, increment a frequency map and update a `maxFreq` tracker. After the recursion, scan the frequency map for entries equal to `maxFreq`.

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
from typing import Optional, List
from collections import defaultdict


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

        # Stores frequency of each subtree sum
        self.freq: dict[int, int] = defaultdict(int)

        # Tracks the highest frequency
        self.max_freq: int = 0

    def compute_subtree_sum(self, root: Optional[TreeNode]) -> int:

        # Base case: return 0 for null nodes
        if not root:
            return 0

        # Compute subtree sum recursively (postorder)
        left_sum = self.compute_subtree_sum(root.left)
        right_sum = self.compute_subtree_sum(root.right)
        subtree_sum = root.val + left_sum + right_sum

        # Update frequency map
        self.freq[subtree_sum] += 1

        # Track max frequency
        self.max_freq = max(self.max_freq, self.freq[subtree_sum])

        return subtree_sum

    def most_frequent_subtree_sum(
        self, root: Optional[TreeNode]
    ) -> List[int]:

        # Handle empty tree case
        if not root:
            return []

        self.compute_subtree_sum(root)

        # Collect all subtree sums with max frequency
        return [
            sum_
            for sum_, count in self.freq.items()
            if count == self.max_freq
        ]


# Examples from the problem statement
print(sorted(Solution().most_frequent_subtree_sum(from_level_order([1, 2, 3]))))              # [2, 3, 6]
print(sorted(Solution().most_frequent_subtree_sum(from_level_order([3, 8, 2, 1, None, 1, 6]))))  # [1, 9]

# Edge cases
print(Solution().most_frequent_subtree_sum(None))                                              # []
print(Solution().most_frequent_subtree_sum(from_level_order([5])))                             # [5]
print(sorted(Solution().most_frequent_subtree_sum(from_level_order([1, 1, 1]))))               # [1, 3] (1 appears twice)
print(Solution().most_frequent_subtree_sum(from_level_order([1, 2, None, 3])))                 # [6] (root sum is unique max)
print(Solution().most_frequent_subtree_sum(from_level_order([-1, -2, -3])))                    # [-6] all sums freq 1, root sum uniquely -6... actually all freq 1 so all returned
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

        // Stores frequency of each subtree sum
        private Map<Integer, Integer> freq = new HashMap<>();

        // Tracks the highest frequency
        private int maxFreq = 0;

        private int computeSubtreeSum(TreeNode root) {

            // Base case: return 0 for null nodes
            if (root == null) {
                return 0;
            }

            // Compute subtree sum recursively (postorder)
            int leftSum = computeSubtreeSum(root.left);
            int rightSum = computeSubtreeSum(root.right);
            int subtreeSum = root.val + leftSum + rightSum;

            // Update frequency map
            freq.put(subtreeSum, freq.getOrDefault(subtreeSum, 0) + 1);

            // Track max frequency
            maxFreq = Math.max(maxFreq, freq.get(subtreeSum));

            return subtreeSum;
        }

        public List<Integer> mostFrequentSubtreeSum(TreeNode root) {

            // Handle empty tree case
            if (root == null) {
                return new ArrayList<>();
            }

            computeSubtreeSum(root);

            // Collect all subtree sums with max frequency
            List<Integer> result = new ArrayList<>();
            for (var entry : freq.entrySet()) {
                if (entry.getValue() == maxFreq) {
                    result.add(entry.getKey());
                }
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        List<Integer> r1 = new Solution().mostFrequentSubtreeSum(fromLevelOrder(1, 2, 3));
        Collections.sort(r1); System.out.println(r1);              // [2, 3, 6]

        List<Integer> r2 = new Solution().mostFrequentSubtreeSum(fromLevelOrder(3, 8, 2, 1, null, 1, 6));
        Collections.sort(r2); System.out.println(r2);              // [1, 9]

        // Edge cases
        System.out.println(new Solution().mostFrequentSubtreeSum(null));                          // []
        System.out.println(new Solution().mostFrequentSubtreeSum(fromLevelOrder(5)));             // [5]

        List<Integer> r3 = new Solution().mostFrequentSubtreeSum(fromLevelOrder(1, 1, 1));
        Collections.sort(r3); System.out.println(r3);              // [1, 3]

        System.out.println(new Solution().mostFrequentSubtreeSum(fromLevelOrder(1, 2, null, 3))); // [6]
    }
}
```

</details>
