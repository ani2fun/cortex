---
title: "Collect Leaves by Height"
summary: "Iteratively peel off the leaves of the tree and collect them in a list of lists, grouped by their postorder height bucket."
prereqs:
  - 11-pattern-postorder-traversal-stateless/01-pattern
difficulty: hard
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Collect Leaves by Height

## Problem Statement

> Iteratively peel off the leaves of the tree and collect them in a list of lists: first list = the original leaves, second list = the leaves *after* removing the first set, and so on, until the tree is empty.
>
> **Example:** `[1, 2, 1, 7, null, null, 1]` → `[[7, 1], [2, 1], [1]]`.

A clever postorder trick: each node has a *height* equal to `1 + max(leftHeight, rightHeight)` (with `null` having height -1). All nodes with height 0 are leaves, with height 1 they're "second wave" leaves (would-be leaves after the originals are peeled), and so on. So we run a single postorder, compute each node's height, and bucket the node into `out[height]`.

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#64748b"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
flowchart TB
    R(("1<br/>height=2"))
    A(("2<br/>height=1"))
    B(("1<br/>height=1"))
    C(("7<br/>height=0"))
    D(("1<br/>height=0"))
    R --> A
    R --> B
    A --> C
    B --> D
    style C fill:#dcfce7,stroke:#22c55e
    style D fill:#dcfce7,stroke:#22c55e
    style A fill:#fef9c3,stroke:#f59e0b
    style B fill:#fef9c3,stroke:#f59e0b
    style R fill:#fee2e2,stroke:#ef4444
```

<p align="center"><strong>Collect leaves by height — every node ends up in the bucket matching its <em>height</em>. Bucket 0 is the originals; bucket 1 is the leaves after peeling; etc. One postorder pass and we're done.</strong></p>

## Examples

**Example 1:**
```
Input:  root = [1, 2, 1, 7, null, null, 1]
Output: [[7, 1], [2, 1], [1]]
```

**Example 2:**
```
Input:  root = [1, 6, 5, null, null, 2, 7]
Output: [[6, 2, 7], [5], [1]]
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `O(n)` time, `O(h)` recursion stack

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def collect_leaves(self, root):
        # Your code goes here — single postorder pass: each node's height =
        # 1 + max(left_height, right_height), with null → -1.
        # Bucket node.val into result[height], extending result as needed.
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

root = build_tree(json.loads(input()))   # the test case's level-order values
print(Solution().collect_leaves(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        List<List<Integer>> collectLeaves(TreeNode root) {
            // Your code goes here — single postorder pass: each node's height =
            // 1 + max(left_height, right_height), with null → -1.
            // Bucket root.val into result.get(height), extending result as needed.
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().collectLeaves(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 1, 7, null, null, 1]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 1, 7, null, null, 1]" }, "expected": "[[7, 1], [2, 1], [1]]" },
    { "args": { "root": "[1, 6, 5, null, null, 2, 7]" }, "expected": "[[6, 2, 7], [5], [1]]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[5]" }, "expected": "[[5]]" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "[[3], [2], [1]]" },
    { "args": { "root": "[1, null, 2, null, 3]" }, "expected": "[[3], [2], [1]]" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "[[2, 3], [1]]" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]" }, "expected": "[[4, 5, 6, 7], [2, 3], [1]]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

A single postorder walk computes each node's height as `1 + max(left_height, right_height)` (null → -1). The height is the bucket index: `result[height]` receives `node.val`. If the bucket doesn't exist yet, extend `result`. The root's height equals the number of layers minus one, so `result` is exactly the right length at the end.

```python solution time=O(n) space=O(n)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def find_height(self, root, result):

        # If root is null, return -1.
        if not root:
            return -1

        # Recursively find the height of the left and right subtrees.
        left_height = self.find_height(root.left, result)
        right_height = self.find_height(root.right, result)

        # Calculate the height of the current node.
        height = max(left_height, right_height) + 1

        # If the result list's size is less than or equal to the
        # height of the node, add a new empty list to the result list.
        if len(result) <= height:
            result.append([])

        # Add the current node's value to the list at the current node's
        # height.
        result[height].append(root.val)

        # Return the height of the current node.
        return height

    def collect_leaves(self, root):

        # List of lists to store leaf nodes at each height.
        result = []

        # Find the height of the tree and collect leaf nodes.
        self.find_height(root, result)

        # Return result list.
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

root = build_tree(json.loads(input()))   # the test case's level-order values
print(Solution().collect_leaves(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int findHeight(TreeNode root, List<List<Integer>> result) {

            // If root is null, return -1.
            if (root == null) {
                return -1;
            }

            // Recursively find the height of the left and right subtrees.
            int leftHeight = findHeight(root.left, result);
            int rightHeight = findHeight(root.right, result);

            // Calculate the height of the current node.
            int height = Math.max(leftHeight, rightHeight) + 1;

            // If the result list's size is less than or equal to the
            // height of the node, add a new empty list to the result list.
            if (result.size() <= height) {
                result.add(new ArrayList<>());
            }

            // Add the current node's value to the list at the current node's
            // height.
            result.get(height).add(root.val);

            // Return the height of the current node.
            return height;
        }

        List<List<Integer>> collectLeaves(TreeNode root) {

            // List of lists to store leaf nodes at each height.
            List<List<Integer>> result = new ArrayList<>();

            // Find the height of the tree and collect leaf nodes.
            findHeight(root, result);

            // Return result list.
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println(new Solution().collectLeaves(root));
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


Stateless postorder is the most-used pattern in the chapter. Three things to walk away with:

1. **`baseCase` + `combine` is the entire algorithm.** Every problem reduces to choosing those two correctly. Once you've internalised the shape, you stop *reading* the algorithm and start *writing* it directly from the problem statement.
2. **The recurrence is the spec.** `f(node) = combine(f(left), f(right), node.val)`. If you can write the recurrence on paper, you've already written the program — the implementation is a five-line transcription. Practice writing the recurrence *first*; the code follows mechanically.
3. **Empty-tree base case is where the off-by-one bugs live.** Choose your base case to make the recurrence *uniformly applicable* — height of an empty tree is 0 (or -1, depending on convention), sum is 0, max is `-∞`, count is 0, "is a valid X" is `true`. Pick the one that makes the combine work cleanly without special-casing leaves.

> *Coming up — the <strong>stateful</strong> postorder pattern. When a single returned value isn't enough — for instance when each subtree must report both <em>"the longest path entirely within me"</em> AND <em>"the longest path from my root downward"</em> — we either return tuples or thread a shared best-so-far through the recursion. That covers diameter, longest monotonic path, distribute-coins, frequent-subtree-sums, and many more "two answers per call" problems.*

</details>
