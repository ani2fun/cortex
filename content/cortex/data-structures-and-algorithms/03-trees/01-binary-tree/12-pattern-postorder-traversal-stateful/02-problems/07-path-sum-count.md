---
title: "Path Sum Count"
summary: "Count the number of downward paths whose node values sum to a given target."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: hard
kind: problem
topics: [postorder-traversal, binary-tree]
---

# Path Sum Count

## Problem Statement

Given the **root** of a binary tree and an integer **target**, count the number of **downward paths** (from a node to any of its descendants) whose values sum to `target`.

This problem combines preorder push/pop *and* postorder accumulation. The classic O(n) trick uses a **prefix-sum hash map**: as you descend, track the running sum from the root; the number of valid paths *ending at the current node* equals `prefix_sum_count[current_sum - target]`. As you backtrack (postorder return), undo the prefix-sum count for this node.

## Examples

**Example 1:**
```
Input:  root = [1, 2, 3, 4, null, null, 7], target = 11
Output: 1
```
Path `4 → 3 → ... ` — actually path `1 → 3 → 7 = 11`. Wait: `1+3+7 = 11`. Count = 1.

**Example 2:**
```
Input:  root = [1, 8, 4, null, null, 2, 7], target = 11
Output: 1
```
Path `8 + ... ` — `1+8 = 9` no, `8+... ` — `4+7=11`. Count = 1.

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`
- `-10⁹ ≤ node.val ≤ 10⁹`
- `-10⁹ ≤ target ≤ 10⁹`

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
        self.prefix_sum_count = {}

    def path_sum_count(self, root, target):
        # Your code goes here — preorder: track running path_sum; count paths ending here
        # via prefix_sum_count[path_sum - target]; recurse; backtrack by decrementing count.
        def find_paths(node, path_sum):
            return 0
        self.prefix_sum_count[0] = 1
        return find_paths(root, 0)

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
target = int(input())                    # the path sum target
print(Solution().path_sum_count(root, target))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private Map<Integer, Integer> prefixSumCount = new HashMap<>();

        private int findPaths(TreeNode root, int target, int pathSum) {
            // Your code goes here — preorder: track running pathSum; count paths ending here
            // via prefixSumCount[pathSum - target]; recurse; backtrack by decrementing count.
            return 0;
        }

        int pathSumCount(TreeNode root, int target) {
            prefixSumCount.put(0, 1);
            return findPaths(root, target, 0);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().pathSumCount(root, target));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, null, null, 7]" },
    { "id": "target", "label": "target", "type": "int", "placeholder": "11" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, null, null, 7]", "target": "11" }, "expected": "1" },
    { "args": { "root": "[1, 8, 4, null, null, 2, 7]", "target": "11" }, "expected": "1" },
    { "args": { "root": "[]", "target": "5" }, "expected": "0" },
    { "args": { "root": "[5]", "target": "5" }, "expected": "1" },
    { "args": { "root": "[5]", "target": "1" }, "expected": "0" },
    { "args": { "root": "[1, 2, 3, 4, 5]", "target": "3" }, "expected": "2" },
    { "args": { "root": "[1, 1, 1]", "target": "2" }, "expected": "2" }
  ]
}
```

<details>
<summary><h2>Solution</h2></summary>

Stateful postorder pattern combined with prefix-sum hashing for O(n). Track `prefix_sum_count[0] = 1` initially, then as you descend: add `path_sum` to the map, count paths via `prefix_sum_count[path_sum - target]`, recurse, then decrement (backtrack).

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
        self.prefix_sum_count = {}

    def find_paths(self, root, target, path_sum):
        # Base case: If the node is None, we've reached the end of a
        # path, so return 0.
        if not root:
            return 0

        # Calculate the current sum by adding the value of the
        # current node to the previous sum.
        path_sum += root.val

        # Check if there is a prefix sum (path_sum - target) in the
        # prefix_sum_count map.
        num_paths = self.prefix_sum_count.get(path_sum - target, 0)

        # Add the current sum to the prefix_sum_count map to keep track
        # of it.
        self.prefix_sum_count[path_sum] = (
            self.prefix_sum_count.get(path_sum, 0) + 1
        )

        # Recursively traverse the left and right subtrees.
        num_paths += self.find_paths(root.left, target, path_sum)
        num_paths += self.find_paths(root.right, target, path_sum)

        # Backtrack by removing the current sum from the prefix sum
        # count map.
        self.prefix_sum_count[path_sum] -= 1

        return num_paths

    def path_sum_count(self, root, target):
        # Add initial prefix sum of 0
        self.prefix_sum_count[0] = 1
        return self.find_paths(root, target, 0)

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
target = int(input())                    # the path sum target
print(Solution().path_sum_count(root, target))
```

```java solution
import java.util.*;

public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static class Solution {
        private Map<Integer, Integer> prefixSumCount = new HashMap<>();

        private int findPaths(TreeNode root, int target, int pathSum) {
            // Base case: If the node is null, we've reached the end of a
            // path, so return 0.
            if (root == null) {
                return 0;
            }

            // Calculate the current sum by adding the value of the
            // current node to the previous sum.
            pathSum += root.val;

            // Check if there is a prefix sum (pathSum - target) in the
            // prefixSumCount map.
            int numPaths = prefixSumCount.getOrDefault(pathSum - target, 0);

            // Add the current sum to the prefixSumCount map.
            prefixSumCount.put(
                pathSum,
                prefixSumCount.getOrDefault(pathSum, 0) + 1
            );

            // Recursively traverse the left and right subtrees.
            numPaths += findPaths(root.left, target, pathSum);
            numPaths += findPaths(root.right, target, pathSum);

            // Backtrack by removing the current sum from the prefix sum
            // count map.
            prefixSumCount.put(pathSum, prefixSumCount.get(pathSum) - 1);

            return numPaths;
        }

        int pathSumCount(TreeNode root, int target) {
            // Add initial prefix sum of 0
            prefixSumCount.put(0, 1);
            return findPaths(root, target, 0);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().pathSumCount(root, target));
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

Stateful postorder is the most *flexible* of the binary-tree patterns — it absorbs almost every "compute X for every subtree, also track a global Y" question. Three things to walk away with:

1. **Two channels per call.** Decide *what to return to the parent* and *what to update globally*. They're rarely the same number. Diameter returns *height*, tracks *diameter*. Distribute coins returns *excess flow*, tracks *moves*. Most-frequent subtree sum returns *sum*, tracks *frequency map + max frequency*. Recognise the duality and the algorithm writes itself.
2. **Globals are safe in postorder, dangerous in preorder.** In stateful preorder you must push/pop because sibling subtrees would otherwise see each other's state. In stateful postorder the global is *monotonically* updated (max, count, accumulate) and order doesn't matter — no undo needed. This is the structural distinction between the two stateful flavours.
3. **Prefix-sum hashing is a force multiplier.** The path-sum-count problem shows how a *combined* preorder-push-pop + postorder-aggregate + prefix-sum-hash can solve in O(N) what a naive O(N²) per-node "look at every ancestor" would do. The same technique recurs in array problems (subarray sum equals K) — internalise the idea.

> *Coming up — the chapter shifts focus from "compute X over the whole tree" to **root-to-leaf path** problems. Where the postorder patterns thought about subtrees, the next two lessons focus on whole paths from the root down to leaves: counting them, listing them, comparing them. The same backtracking template you saw in stateful preorder reappears, but specialised for the path-as-a-unit framing.*

</details>
