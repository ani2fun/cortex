---
title: "Kth Largest Element"
summary: "Given the root of a binary search tree and an integer k, return the k-th largest element. Return 0 if no such element exists."
prereqs:
  - 12-pattern-reversed-sorted-traversal/01-pattern
difficulty: medium
kind: problem
topics: [reversed-sorted-traversal, binary-search-tree]
---

# Kth largest element

## Problem Statement

Given the **root** of a binary search tree and an integer `k`, return the k-th largest element. Return `0` if no such element exists.

## Examples

**Example 1:**
```
Input:  root = [4, 2, 5, 1, 3, null, 6], k = 3
Output: 4
```

**Example 2:**
```
Input:  root = [5, 4, 10, null, null, 9, 11], k = 2
Output: 10
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `1 ≤ k`
- All BST keys are distinct

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def kth_largest_element(self, root, k):
        # Your code goes here — reversed in-order (right → node → left);
        # count visits; stop and return node.val when count == k.
        # Return 0 if k exceeds the tree size.
        return 0

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
k = int(input())
print(Solution().kth_largest_element(root, k))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        int kthLargestElement(TreeNode root, int k) {
            // Your code goes here — reversed in-order (right → node → left);
            // count visits; stop and return node.val when count == k.
            // Return 0 if k exceeds the tree size.
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kthLargestElement(root, k));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[4, 2, 5, 1, 3, null, 6]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]", "k": "3" }, "expected": "4" },
    { "args": { "root": "[5, 4, 10, null, null, 9, 11]", "k": "2" }, "expected": "10" },
    { "args": { "root": "[5]", "k": "1" }, "expected": "5" },
    { "args": { "root": "[5]", "k": "2" }, "expected": "0" },
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]", "k": "1" }, "expected": "6" },
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]", "k": "6" }, "expected": "1" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

Walk reverse in-order; the k-th node visited is the k-th largest. Critically — **stop traversing the moment the answer is found**, so the cost is O(h + k), not O(n).

</details>
<details>
<summary><h2>Solution</h2></summary>

Perform a reversed in-order traversal, counting each visit. When the count reaches `k`, record the node's value and stop. A `found` flag prevents further recursion after the answer is found. If the tree has fewer than `k` nodes, return `0`.

```python solution time=O(h+k) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class Solution:
    def __init__(self):
        self.count = 0
        self.result = 0
        self.found = False

    def _reverse_in_order(self, root, k):
        if root is None or self.found:
            return
        self._reverse_in_order(root.right, k)
        self.count += 1
        if self.count == k:
            self.result = root.val
            self.found = True
            return
        self._reverse_in_order(root.left, k)

    def kth_largest_element(self, root, k):
        self._reverse_in_order(root, k)
        return self.result

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
k = int(input())
print(Solution().kth_largest_element(root, k))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private int count = 0;
        private int result = 0;
        private boolean found = false;

        private void reverseInOrder(TreeNode root, int k) {
            if (root == null || found) return;
            reverseInOrder(root.right, k);
            count++;
            if (count == k) {
                result = root.val;
                found = true;
                return;
            }
            reverseInOrder(root.left, k);
        }

        int kthLargestElement(TreeNode root, int k) {
            reverseInOrder(root, k);
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kthLargestElement(root, k));
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
