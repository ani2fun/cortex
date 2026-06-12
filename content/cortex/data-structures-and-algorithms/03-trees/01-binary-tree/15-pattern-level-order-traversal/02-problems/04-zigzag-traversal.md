---
title: "Zigzag Traversal"
summary: "Return the level-order traversal where the direction alternates per level: level 0 left-to-right, level 1 right-to-left, level 2 left-to-right, …"
prereqs:
  - 15-pattern-level-order-traversal/01-pattern
difficulty: medium
kind: problem
topics: [level-order-traversal, binary-tree]
---

# Problem 4 — Zigzag traversal

## Problem Statement

Return the level-order traversal where the *direction* alternates per level: level 0 left-to-right, level 1 right-to-left, level 2 left-to-right, …

Same template, but pre-allocate the level array and *write into it from either end* depending on a `reverse` boolean that flips each iteration. Avoids per-level reversal at the cost of one extra index.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, null, null, 7]
Output: [[1], [3, 2], [4, 7]]
```

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 7]
Output: [[1], [4, 8], [2, 7]]
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- Level 0 (root) is always left-to-right.

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def zigzag_traversal(self, root):
        # Your code goes here — same BFS drain-n skeleton; use a deque for the
        # level and append/appendleft based on a direction flag that flips each round.
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
print(Solution().zigzag_traversal(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        List<List<Integer>> zigzagTraversal(TreeNode root) {
            // Your code goes here — use a LinkedList<Integer> for the level and
            // addLast/addFirst based on a direction flag that flips each round.
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().zigzagTraversal(root));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, null, null, 7]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]" }, "expected": "[[1], [3, 2], [4, 7]]" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]" }, "expected": "[[1], [4, 8], [2, 7]]" },
    { "args": { "root": "[]" }, "expected": "[]" },
    { "args": { "root": "[1]" }, "expected": "[[1]]" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "[[1], [2], [3]]" },
    { "args": { "root": "[1, 2, 3, 4, 5, 6, 7]" }, "expected": "[[1], [3, 2], [4, 5, 6, 7]]" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

The BFS skeleton is unchanged. The only addition is a `ltr` (left-to-right) boolean that starts `True` and flips each round. For the level buffer, use a `deque` (Python) or `LinkedList` (Java): when `ltr`, `append`/`addLast`; when not, `appendleft`/`addFirst`. BFS still enqueues children left-then-right every round — only the *recording* direction alternates, not the traversal order.

```python solution time=O(n) space=O(w)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def zigzag_traversal(self, root):
        if root is None: return []
        out, q, ltr = [], deque([root]), True
        while q:
            n = len(q); level = deque()
            for _ in range(n):
                node = q.popleft()
                level.append(node.val) if ltr else level.appendleft(node.val)
                if node.left:  q.append(node.left)
                if node.right: q.append(node.right)
            out.append(list(level)); ltr = not ltr
        return out

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
print(Solution().zigzag_traversal(root))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        List<List<Integer>> zigzagTraversal(TreeNode root) {
            List<List<Integer>> out = new ArrayList<>();
            if (root == null) return out;
            Deque<TreeNode> q = new ArrayDeque<>();
            q.add(root);
            boolean ltr = true;
            while (!q.isEmpty()) {
                int n = q.size();
                LinkedList<Integer> level = new LinkedList<>();
                for (int i = 0; i < n; i++) {
                    TreeNode node = q.poll();
                    if (ltr) level.addLast(node.val);
                    else     level.addFirst(node.val);
                    if (node.left != null)  q.add(node.left);
                    if (node.right != null) q.add(node.right);
                }
                out.add(level);
                ltr = !ltr;
            }
            return out;
        }
    }

    public static void main(String[] args) {
        TreeNode root = buildTree(parseIntegerArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().zigzagTraversal(root));
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
