---
title: "Right View"
summary: "Given the root of a binary tree, return the values of the rightmost node at each level of the tree, top to bottom."
prereqs:
  - 10-pattern-preorder-traversal-stateful/01-pattern
difficulty: medium
kind: problem
topics: [preorder-traversal, binary-tree]
---

# Problem 4 — Right view

## Problem Statement

Same as the left view, but from the right side. Return the values of the rightmost node at each level, top to bottom.

The trick is *identical* to the left view, with one swap: recurse **right before left**. The first node visited at each new level is now the rightmost.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, null, null, 7, 9]
Output: [1, 3, 7, 9]
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 7]
Output: [1, 4, 7]
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- Recurse right before left — the first visit to each level is the rightmost node

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def right_view(self, root):
        # Your code goes here — same as left view but recurse right before left.
        # Track maxLevelReached; when level == maxLevelReached, append root.val
        # and increment. Then recurse right first, then left.
        return []

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

root = build_tree(json.loads(input()))
print(Solution().right_view(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        List<Integer> rightView(TreeNode root) {
            // Your code goes here — same as left view but recurse right before left.
            // Track maxLevelReached; when level == maxLevelReached, add root.val
            // and increment. Then recurse right first, then left.
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().rightView(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, null, null, 7, 9]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, null, null, 7, 9]" }, "expected": "[1, 3, 7, 9]" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]" }, "expected": "[1, 4, 7]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[5]" }, "expected": "[5]" },
    { "args": { "root": "[1, null, 2, null, 3]" }, "expected": "[1, 2, 3]" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "[1, 2, 3]" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]" }, "expected": "[1, 3, 7]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Track a shared `max_level_reached` counter (starts at 0). Recurse **right before left** — the mirror of the left-view. At each node: if `level == max_level_reached`, this is the first (rightmost) node seen at this depth — append its value and increment the counter. No push/pop needed: `max_level_reached` is a monotone global witness.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def __init__(self):
        # Global variable to keep track of the current level during recursion
        self.max_level_reached = 0

    def helper(self, root, level, result):
        if not root:
            return
        # If this is the first node of the current level, add it to result
        if level == self.max_level_reached:
            result.append(root.val)
            # Increment the level after adding the node to result
            self.max_level_reached += 1
        # Recur for right, then left (ensures rightmost nodes are visited first)
        self.helper(root.right, level + 1, result)
        self.helper(root.left, level + 1, result)

    def right_view(self, root):
        # Stores the right view of the binary tree
        result = []
        # Find the right view of the binary tree
        self.helper(root, 0, result)
        # Return the right view of the binary tree
        return result

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

root = build_tree(json.loads(input()))
print(Solution().right_view(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        // Global variable to keep track of the current level during recursion
        private int maxLevelReached = 0;

        private void helper(TreeNode root, int level, List<Integer> result) {
            if (root == null) return;
            // If this is the first node of the current level, add it to result
            if (level == maxLevelReached) {
                result.add(root.val);
                // Increment the level after adding the node to result
                maxLevelReached++;
            }
            // Recur for right, then left (ensures rightmost nodes are visited first)
            helper(root.right, level + 1, result);
            helper(root.left, level + 1, result);
        }

        List<Integer> rightView(TreeNode root) {
            // Stores the right view of the binary tree
            List<Integer> result = new ArrayList<>();
            // Find the right view of the binary tree
            helper(root, 0, result);
            // Return the right view of the binary tree
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().rightView(root));
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
<details>
<summary><h2>Key Takeaway</h2></summary>

The stateful preorder pattern is the second-most-common shape in the chapter. Three things to walk away with:

1. **Push then recurse then pop.** When the state is a mutable collection, the discipline is sacred: push on entry, both recursions, pop on exit. Forgetting the pop is the canonical "my answer is way too big" backtracking bug. *Always* check that every push has a paired pop on every code path.
2. **Not every "shared mutable" is push-pop.** Monotone witnesses (min/max/best-so-far) and visit-order witnesses (first-at-each-level) share a mutable across the recursion *without* needing pop, because their updates are inherently global facts (or because the visit order itself encodes the bookkeeping).
3. **Left-vs-right preference is what gives "first" its meaning.** The view problems all turn on which child you recurse into *first*. Left view: left first. Right view: right first. Top view: process by level *and* horizontal distance. Generalise this — whenever a problem says "first / leftmost / rightmost / topmost", the *recursion order* is doing the work.

> *Coming up — the chapter pivots from the downward-flowing preorder patterns to the upward-flowing <strong>postorder</strong> patterns. Where preorder hands data <em>from parent to child</em>, postorder gathers data <em>from children to parent</em>. The next two lessons (stateless and stateful postorder) cover heights, sums, diameters, and a wealth of other "compute the answer at each node from its subtrees' answers" problems.*

</details>
