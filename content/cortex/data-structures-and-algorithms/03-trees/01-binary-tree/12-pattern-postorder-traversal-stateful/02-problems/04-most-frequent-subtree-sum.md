---
title: "Most Frequent Subtree Sum"
summary: "Return all subtree sums whose frequency in the tree is the highest."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: medium
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Most Frequent Subtree Sum

## Problem Statement

Given the **root** of a binary tree, the **subtree sum** of a node is the sum of all values in its subtree (including itself). Return all subtree sums whose frequency in the tree is the **highest** (ties included). The result may be returned in any order.

Each call returns its subtree sum (so the parent can compute its own); along the way, increment a frequency map and update a `max_freq` tracker. After the recursion, scan the frequency map for entries equal to `max_freq`.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3]
Output: [2, 3, 6]   (each sum appears exactly once — all tied)
```

**Example 2:**
```
Input:  root = [3, 8, 2, 1, null, 1, 6]
Output: [1, 9]   (sum=1 appears at the two leaf nodes; sum=9 appears at two subtrees)
```

## Constraints

- `1 ≤ number of nodes ≤ 10⁴`
- `-10⁵ ≤ node.val ≤ 10⁵`
- Return all most-frequent sums sorted ascending (for determinism).

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def __init__(self):
        self.freq = {}
        self.max_freq = 0

    def most_frequent_subtree_sum(self, root):
        # Your code goes here — postorder: return subtree_sum = node.val + left_sum + right_sum;
        # update self.freq[subtree_sum] and self.max_freq.
        # After traversal, return sorted list of sums with freq == max_freq.
        def compute_subtree_sum(node):
            return 0
        if not root:
            return []
        compute_subtree_sum(root)
        return sorted(s for s, c in self.freq.items() if c == self.max_freq)

def build_tree(values):              # [1, 2, 3, null, 4] level-order → root
    if not values:
        return None
    root = TreeNode(values[0])
    queue = deque([root])
    i = 1
    while queue and i < len(values):
        node = queue.popleft()
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.left = TreeNode(v); queue.append(node.left)
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.right = TreeNode(v); queue.append(node.right)
    return root

root = build_tree(json.loads(input()))   # the test case's level-order values
print(Solution().most_frequent_subtree_sum(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private Map<Integer, Integer> freq = new HashMap<>();
        private int maxFreq = 0;

        private int computeSubtreeSum(TreeNode root) {
            // Your code goes here — postorder: return subtreeSum = node.val + leftSum + rightSum;
            // update freq and maxFreq.
            return 0;
        }

        List<Integer> mostFrequentSubtreeSum(TreeNode root) {
            if (root == null) return new ArrayList<>();
            computeSubtreeSum(root);
            List<Integer> result = new ArrayList<>();
            for (var entry : freq.entrySet()) {
                if (entry.getValue() == maxFreq) result.add(entry.getKey());
            }
            Collections.sort(result);
            return result;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().mostFrequentSubtreeSum(root));
    }

    static TreeNode buildTree(Integer[] values) {   // [1, 2, 3, null, 4] level-order → root
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.add(root);
        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            TreeNode node = queue.poll();
            if (i < values.length) {
                Integer v = values[i++];
                if (v != null) { node.left = new TreeNode(v); queue.add(node.left); }
            }
            if (i < values.length) {
                Integer v = values[i++];
                if (v != null) { node.right = new TreeNode(v); queue.add(node.right); }
            }
        }
        return root;
    }

    // "[1, 2, null, 4]" → {1, 2, null, 4} — reads the test case's level-order values
    static Integer[] parseIntegerArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new Integer[0];
        String[] parts = inner.split(",");
        Integer[] out = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++)
            out[i] = parts[i].equals("null") ? null : Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3]" }, "expected": "[2, 3, 6]" },
    { "args": { "root": "[3, 8, 2, 1, null, 1, 6]" }, "expected": "[1, 9]" },
    { "args": { "root": "[5]" }, "expected": "[5]" },
    { "args": { "root": "[1, 1, 1]" }, "expected": "[1]" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "[3, 5, 6]" },
    { "args": { "root": "[-1, -2, -3]" }, "expected": "[-6, -3, -2]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Each postorder call returns the full subtree sum; the frequency map accumulates as we go. After the traversal, scan the map for the max-frequency entries. Both languages sort the result for determinism.

```python solution time=O(n) space=O(n)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def __init__(self):
        self.freq = {}
        self.max_freq = 0

    def compute_subtree_sum(self, root):
        # Base case: return 0 for null nodes
        if not root:
            return 0

        # Compute subtree sum recursively (postorder)
        left_sum = self.compute_subtree_sum(root.left)
        right_sum = self.compute_subtree_sum(root.right)
        subtree_sum = root.val + left_sum + right_sum

        # Update frequency map
        self.freq[subtree_sum] = self.freq.get(subtree_sum, 0) + 1

        # Track max frequency
        self.max_freq = max(self.max_freq, self.freq[subtree_sum])

        return subtree_sum

    def most_frequent_subtree_sum(self, root):
        # Handle empty tree case
        if not root:
            return []

        self.compute_subtree_sum(root)

        # Collect all subtree sums with max frequency, sorted for determinism
        return sorted(s for s, c in self.freq.items() if c == self.max_freq)

def build_tree(values):              # [1, 2, 3, null, 4] level-order → root
    if not values:
        return None
    root = TreeNode(values[0])
    queue = deque([root])
    i = 1
    while queue and i < len(values):
        node = queue.popleft()
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.left = TreeNode(v); queue.append(node.left)
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.right = TreeNode(v); queue.append(node.right)
    return root

root = build_tree(json.loads(input()))   # the test case's level-order values
print(Solution().most_frequent_subtree_sum(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private Map<Integer, Integer> freq = new HashMap<>();
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

        List<Integer> mostFrequentSubtreeSum(TreeNode root) {
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

            Collections.sort(result);
            return result;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().mostFrequentSubtreeSum(root));
    }

    static TreeNode buildTree(Integer[] values) {   // [1, 2, 3, null, 4] level-order → root
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.add(root);
        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            TreeNode node = queue.poll();
            if (i < values.length) {
                Integer v = values[i++];
                if (v != null) { node.left = new TreeNode(v); queue.add(node.left); }
            }
            if (i < values.length) {
                Integer v = values[i++];
                if (v != null) { node.right = new TreeNode(v); queue.add(node.right); }
            }
        }
        return root;
    }

    // "[1, 2, null, 4]" → {1, 2, null, 4} — reads the test case's level-order values
    static Integer[] parseIntegerArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new Integer[0];
        String[] parts = inner.split(",");
        Integer[] out = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++)
            out[i] = parts[i].equals("null") ? null : Integer.parseInt(parts[i]);
        return out;
    }
}
```

</details>
