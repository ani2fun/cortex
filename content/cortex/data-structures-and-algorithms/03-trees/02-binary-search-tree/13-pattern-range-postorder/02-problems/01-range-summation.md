---
title: "Range Summation"
summary: "Given the root of a BST and a range [low, high], update each in-range node's value by adding the values of all its in-range descendants. Return the modified tree."
prereqs:
  - 13-pattern-range-postorder/01-pattern
difficulty: medium
kind: problem
topics: [range-postorder, binary-search-tree]
---

# Range summation

## Problem Statement

Given the **root** of a BST and a range `[low, high]`, update each in-range node's value by adding the values of all its descendants that are also in range. Return the modified tree.

> Guarantee: a node *outside* the range never has any in-range descendants on either side. (This follows from BST structure, but the problem states it explicitly so the pruning is safe.)

## Examples

**Example 1:**
```
Input:  root = [4, 2, 5, 1, 3, null, 6], low = 2, high = 5
Output: [14, 5, 5, 1, 3, null, 6]
```

**Example 2:**
```
Input:  root = [5, 1, 8, null, null, 6, 9], low = 6, high = 9
Output: [5, 1, 23, null, null, 6, 9]
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
    def range_summation_helper(self, root, low, high):
        # Your code goes here
        pass

    def range_summation(self, root, low, high):
        self.range_summation_helper(root, low, high)
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
print_tree(Solution().range_summation(root, low, high))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int rangeSummationHelper(TreeNode root, int low, int high) {
            // Your code goes here
            return 0;
        }

        TreeNode rangeSummation(TreeNode root, int low, int high) {
            rangeSummationHelper(root, low, high);
            return root;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int low = Integer.parseInt(sc.nextLine().trim());
        int high = Integer.parseInt(sc.nextLine().trim());
        printTree(new Solution().rangeSummation(root, low, high));
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
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]", "low": "2", "high": "5" }, "expected": "[14, 5, 5, 1, 3, null, 6]" },
    { "args": { "root": "[5, 1, 8, null, null, 6, 9]", "low": "6", "high": "9" }, "expected": "[5, 1, 23, null, null, 6, 9]" },
    { "args": { "root": "[5]", "low": "1", "high": "10" }, "expected": "[5]" },
    { "args": { "root": "[2, 1, 3]", "low": "1", "high": "3" }, "expected": "[6, 1, 3]" },
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]", "low": "7", "high": "10" }, "expected": "[4, 2, 5, 1, 3, null, 6]" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

Every in-range node accumulates `leftSum + rightSum + originalVal` and writes that back into `node.val`. The recursion returns the same total to its parent so parents can do the same.

</details>
<details>
<summary><h2>Solution</h2></summary>

At each in-range node, recursively compute the sum of in-range descendants in the left and right subtrees, then add both to the node's own value. The helper returns the updated node value so the parent can include it in its own tally. Out-of-range nodes are handled by BST pruning: `< low` → recurse right only; `> high` → recurse left only.

```python solution time=O(n) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def range_summation_helper(self, root, low, high):
        if root is None:
            return 0
        if root.val < low:
            return self.range_summation_helper(root.right, low, high)
        if root.val > high:
            return self.range_summation_helper(root.left, low, high)
        left_sum = self.range_summation_helper(root.left, low, high)
        right_sum = self.range_summation_helper(root.right, low, high)
        root.val += left_sum + right_sum
        return root.val

    def range_summation(self, root, low, high):
        self.range_summation_helper(root, low, high)
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
print_tree(Solution().range_summation(root, low, high))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int rangeSummationHelper(TreeNode root, int low, int high) {
            if (root == null) return 0;
            if (root.val < low) return rangeSummationHelper(root.right, low, high);
            if (root.val > high) return rangeSummationHelper(root.left, low, high);
            int leftSum = rangeSummationHelper(root.left, low, high);
            int rightSum = rangeSummationHelper(root.right, low, high);
            root.val += leftSum + rightSum;
            return root.val;
        }

        TreeNode rangeSummation(TreeNode root, int low, int high) {
            rangeSummationHelper(root, low, high);
            return root;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int low = Integer.parseInt(sc.nextLine().trim());
        int high = Integer.parseInt(sc.nextLine().trim());
        printTree(new Solution().rangeSummation(root, low, high));
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

</details>
