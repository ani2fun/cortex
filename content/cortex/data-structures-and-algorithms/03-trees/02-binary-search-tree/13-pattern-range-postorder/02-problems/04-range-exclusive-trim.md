---
title: "Range Exclusive Trim"
summary: "Given the root of a BST and two values low and high, return a new BST that contains only the nodes whose values lie in [low, high], preserving relative structure."
prereqs:
  - 13-pattern-range-postorder/01-pattern
difficulty: hard
kind: problem
topics: [range-postorder, binary-search-tree]
---

# Range exclusive trim

## Problem Statement

Given the **root** of a BST and two values `low` and `high`, return a new BST that contains *only* the nodes whose values lie in `[low, high]`. The relative structure must be preserved — if `A` was a descendant of `B` in the original and both survive the trim, `A` must remain a descendant of `B` in the result.

## Examples

**Example 1:**
```
Input:  root = [4, 2, 5, 1, 3, null, 6], low = 2, high = 5
Output: [4, 2, 5, null, 3]
```

**Example 2:**
```
Input:  root = [5, 1, 8, null, null, 6, 9], low = 6, high = 9
Output: [8, 6, 9]
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `low ≤ high`
- All node values are unique

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def range_exclusive_trim(self, root, low, high):
        # Your code goes here
        return root

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

def print_tree(root):                # root → [1, 2, 3, null, 4], trailing nulls trimmed
    out, queue = [], deque([root])
    while queue:
        node = queue.popleft()
        if node is None:
            out.append(None)
        else:
            out.append(node.val)
            queue.append(node.left)
            queue.append(node.right)
    while out and out[-1] is None:
        out.pop()
    print(json.dumps(out))

root = build_tree(json.loads(input()))   # the test case's level-order values
low = int(input())
high = int(input())
print_tree(Solution().range_exclusive_trim(root, low, high))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        TreeNode rangeExclusiveTrim(TreeNode root, int low, int high) {
            // Your code goes here
            return root;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int low = Integer.parseInt(sc.nextLine().trim());
        int high = Integer.parseInt(sc.nextLine().trim());
        printTree(new Solution().rangeExclusiveTrim(root, low, high));
    }

    static TreeNode buildTree(Integer[] values) {
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

    static void printTree(TreeNode root) {
        List<String> out = new ArrayList<>();
        Deque<TreeNode> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node == null) {
                out.add("null");
            } else {
                out.add(String.valueOf(node.val));
                queue.add(node.left);
                queue.add(node.right);
            }
        }
        while (!out.isEmpty() && out.get(out.size() - 1).equals("null"))
            out.remove(out.size() - 1);
        System.out.println("[" + String.join(", ", out) + "]");
    }

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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[4, 2, 5, 1, 3, null, 6]" },
    { "id": "low", "label": "low", "type": "int", "placeholder": "2" },
    { "id": "high", "label": "high", "type": "int", "placeholder": "5" }
  ],
  "cases": [
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]", "low": "2", "high": "5" }, "expected": "[4, 2, 5, null, 3]" },
    { "args": { "root": "[5, 1, 8, null, null, 6, 9]", "low": "6", "high": "9" }, "expected": "[8, 6, 9]" },
    { "args": { "root": "[5]", "low": "1", "high": "5" }, "expected": "[5]" },
    { "args": { "root": "[5]", "low": "6", "high": "10" }, "expected": "[]" },
    { "args": { "root": "[4, 2, 6, 1, 3, 5, 7]", "low": "3", "high": "5" }, "expected": "[4, 3, 5]" },
    { "args": { "root": "[4, 2, 6, 1, 3, 5, 7]", "low": "1", "high": "7" }, "expected": "[4, 2, 6, 1, 3, 5, 7]" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

The same pruning rules drive a *structural rewrite*:

- If `node.val < low`, the entire left subtree is out of range; we **don't recurse left** at all. Return the trim of the right subtree as our replacement.
- If `node.val > high`, mirror — return the trim of the left subtree.
- Otherwise (`node.val` in range), the node survives. Trim both children recursively and re-attach.

The `return` value is the new root of *this* subtree after trimming, which the caller wires back into its own children pointers — exactly the same shape as the recursive insertion idiom used in insertion and deletion.

</details>
<details>
<summary><h2>Solution</h2></summary>

Postorder trim: each call returns the new root of its subtree. A `null` node returns `null`. A node below `low` is dropped — return the trimmed right subtree (which may contain in-range keys). A node above `high` is dropped — return the trimmed left subtree. An in-range node has both children trimmed recursively and is returned intact. The caller wires the returned pointer back into its own child slot, so no explicit "reconnect" step is needed.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def range_exclusive_trim(self, root, low, high):
        if root is None:
            return None
        if root.val < low:
            return self.range_exclusive_trim(root.right, low, high)
        if root.val > high:
            return self.range_exclusive_trim(root.left, low, high)
        root.left = self.range_exclusive_trim(root.left, low, high)
        root.right = self.range_exclusive_trim(root.right, low, high)
        return root

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

def print_tree(root):                # root → [1, 2, 3, null, 4], trailing nulls trimmed
    out, queue = [], deque([root])
    while queue:
        node = queue.popleft()
        if node is None:
            out.append(None)
        else:
            out.append(node.val)
            queue.append(node.left)
            queue.append(node.right)
    while out and out[-1] is None:
        out.pop()
    print(json.dumps(out))

root = build_tree(json.loads(input()))   # the test case's level-order values
low = int(input())
high = int(input())
print_tree(Solution().range_exclusive_trim(root, low, high))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        TreeNode rangeExclusiveTrim(TreeNode root, int low, int high) {
            if (root == null) return null;
            if (root.val < low) return rangeExclusiveTrim(root.right, low, high);
            if (root.val > high) return rangeExclusiveTrim(root.left, low, high);
            root.left = rangeExclusiveTrim(root.left, low, high);
            root.right = rangeExclusiveTrim(root.right, low, high);
            return root;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int low = Integer.parseInt(sc.nextLine().trim());
        int high = Integer.parseInt(sc.nextLine().trim());
        printTree(new Solution().rangeExclusiveTrim(root, low, high));
    }

    static TreeNode buildTree(Integer[] values) {
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

    static void printTree(TreeNode root) {
        List<String> out = new ArrayList<>();
        Deque<TreeNode> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node == null) {
                out.add("null");
            } else {
                out.add(String.valueOf(node.val));
                queue.add(node.left);
                queue.add(node.right);
            }
        }
        while (!out.isEmpty() && out.get(out.size() - 1).equals("null"))
            out.remove(out.size() - 1);
        System.out.println("[" + String.join(", ", out) + "]");
    }

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
3. **Returning the trimmed subtree to the parent** is the same idiom we used for insertion and deletion: every recursive call returns a pointer to the (possibly modified) subtree, and the caller wires it into its own pointer field.

</details>
