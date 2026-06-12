---
title: "Pattern: Reversed Sorted Traversal"
summary: "Mirror the in-order walk (right → node → left) to visit a BST's keys largest-first. Powers k-th largest, rank assignment, and the running-accumulator trick (greater-sum tree) — because descending means everything larger has already been seen."
prereqs:
  - 03-trees/02-binary-search-tree/11-pattern-sorted-traversal/01-pattern
---

# Pattern: Reversed Sorted Traversal

## Why It Exists

[Sorted traversal](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-sorted-traversal/pattern) walked the BST in-order (left → node → right) for *ascending* keys. Flip the two recursive directions — **right → node → left** — and you visit the keys in *descending* order instead. That mirror is the natural fit for "**k-th largest**", "assign **ranks** (largest = rank 1)", and a class of **running-accumulator** problems.

The accumulator insight is what makes this pattern more than "sorted traversal backwards." Walking descending means that by the time you reach any node, you've **already visited every key larger than it**. So a single running total, updated as you go, lets you set each node to "the sum of all keys ≥ it" (the *greater-sum tree*) or "its rank among all keys" — in one `O(n)` pass, no second traversal.

## See It Work

Find the **k-th largest** key with a reversed in-order walk that stops at `k`. Pick a case and **Run** it.

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

def kth_largest(root, k):
    stack, node = [], root
    while stack or node:
        while node:                        # descend the RIGHT spine (largest first)
            stack.append(node)
            node = node.right
        node = stack.pop()                 # next key in DESCENDING order
        k -= 1
        if k == 0:
            return node.val
        node = node.left
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
print(kth_largest(root, k))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
  static class TreeNode {
    int val; TreeNode left, right;
    TreeNode(int val) { this.val = val; }
  }

  static int kthLargest(TreeNode root, int k) {
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode node = root;
    while (!stack.isEmpty() || node != null) {
      while (node != null) { stack.push(node); node = node.right; }
      node = stack.pop();
      if (--k == 0) return node.val;
      node = node.left;
    }
    return 0;
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
    int k = Integer.parseInt(sc.nextLine().trim());
    System.out.println(kthLargest(root, k));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[5, 3, 8, 1, 4, 7, 9]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "root": "[5, 3, 8, 1, 4, 7, 9]", "k": "1" }, "expected": "9" },
    { "args": { "root": "[5, 3, 8, 1, 4, 7, 9]", "k": "3" }, "expected": "7" },
    { "args": { "root": "[5, 3, 8, 1, 4, 7, 9]", "k": "7" }, "expected": "1" },
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]", "k": "3" }, "expected": "4" },
    { "args": { "root": "[7]", "k": "1" }, "expected": "7" },
    { "args": { "root": "[7]", "k": "2" }, "expected": "0" }
  ]
}
```

## How It Works

It's the in-order walk with left and right swapped:

- **Reversed in-order** = right → node → left. The right subtree (larger keys) is visited first, so keys emerge largest-to-smallest.
- **k-th largest** — count visits, stop at `k` (early-exit → `O(k + h)`).
- **Running accumulator** — keep a `total`; at each node do `total += node.val` (everything larger is already in `total`), then set `node.val = total` for the *greater-sum tree*, or assign an incrementing rank.

```mermaid
flowchart LR
  T["reversed in-order (right, node, left)"] --> D["keys in DESCENDING order"]
  D --> A["k-th largest: count to k"]
  D --> B["greater-sum: node = running total"]
  D --> C["rank: assign 1, 2, 3, …"]
```

<p align="center"><strong>reversed in-order (right first) yields descending keys; a running total accumulates "everything larger so far," enabling greater-sum / rank in one pass.</strong></p>

Same cost profile as sorted traversal — `O(n)` (or `O(k + h)` with early-exit), `O(h)` space. The only change is the *direction*, and the payoff is that "descending order" makes "sum/count of everything larger" an `O(1)` per-node update.

### Key Takeaway

Reversed in-order (right → node → left) visits BST keys largest-first. It gives k-th largest by counting, and — because descending means all larger keys are seen first — a running total turns "sum of everything greater" (greater-sum tree) or "rank" into a one-pass `O(n)` computation.

## Trace It

`kth_largest(root, 3)` over `[5,3,8,1,4,7,9]` — reversed in-order yields `9, 8, 7, …`:

| visit # | key | `k` after | action |
|---|---|---|---|
| 1 | `9` | 2 | continue |
| 2 | `8` | 1 | continue |
| 3 | `7` | 0 | **return 7** |

Before you read on: the *greater-sum tree* replaces each key with the sum of all keys **≥** it, in one pass — node `9` becomes `9`, `8` becomes `17` (`8+9`), `7` becomes `24` (`7+8+9`). Why does walking in *descending* order make this a single `O(1)`-per-node update, when computing "sum of everything larger" naively sounds like it needs a separate scan for each node?

Because descending order visits keys in exactly the sequence "largest, then next-largest, …" — so when you arrive at a node, **every key larger than it has already been visited**, and if you've been adding each visited key into a running `total`, that `total` *is* the sum of everything ≥ the current node. One addition (`total += node.val`) maintains the invariant; one assignment (`node.val = total`) records the answer. The naive approach recomputes "sum of larger keys" from scratch per node — `O(n)` each, `O(n²)` total. The reversed walk turns it into `O(n)` because the *order of visitation* pre-aggregates the larger keys for free. This "process in an order that makes the running aggregate exactly what you need" is the same instinct behind prefix sums and the sweep line — and here the BST's reverse-sorted walk supplies that order.

## Your Turn

k-th largest (reversed in-order + count to `k`). Implement it from scratch.

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

def kth_largest(root, k):
    # Your code goes here — reversed in-order (right → node → left),
    # count visits, stop and return node.val when count reaches k.
    # Return 0 if k exceeds the tree size.
    pass

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
print(kth_largest(root, k))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
  static class TreeNode {
    int val; TreeNode left, right;
    TreeNode(int val) { this.val = val; }
  }

  static int kthLargest(TreeNode root, int k) {
    // Your code goes here — reversed in-order (right → node → left),
    // count visits, stop and return node.val when count reaches k.
    // Return 0 if k exceeds the tree size.
    return 0;
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
    int k = Integer.parseInt(sc.nextLine().trim());
    System.out.println(kthLargest(root, k));
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
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[5, 3, 8, 1, 4, 7, 9]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "root": "[5, 3, 8, 1, 4, 7, 9]", "k": "1" }, "expected": "9" },
    { "args": { "root": "[5, 3, 8, 1, 4, 7, 9]", "k": "2" }, "expected": "8" },
    { "args": { "root": "[5, 3, 8, 1, 4, 7, 9]", "k": "7" }, "expected": "1" },
    { "args": { "root": "[4, 2, 5, 1, 3, null, 6]", "k": "3" }, "expected": "4" },
    { "args": { "root": "[7]", "k": "1" }, "expected": "7" },
    { "args": { "root": "[7]", "k": "2" }, "expected": "0" }
  ]
}
```

<details>
<summary>Editorial</summary>

The reversed in-order iterative walk uses an explicit stack. Descend the right spine first (pushing nodes), then pop the top (next-largest key), decrement `k`, and return if `k` reaches zero. Otherwise step left and continue. If the loop ends without returning, `k` exceeded the tree size — return `0`.

```python solution time=O(k+h) space=O(h)
import json
from collections import deque

class TreeNode:
    def __init__(self, val, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

def kth_largest(root, k):
    stack, node = [], root
    while stack or node:
        while node:                        # descend the RIGHT spine (largest first)
            stack.append(node)
            node = node.right
        node = stack.pop()                 # next key in DESCENDING order
        k -= 1
        if k == 0:
            return node.val
        node = node.left
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
print(kth_largest(root, k))
```

```java solution
import java.util.*;

public class Main {
  static class TreeNode {
    int val; TreeNode left, right;
    TreeNode(int val) { this.val = val; }
  }

  static int kthLargest(TreeNode root, int k) {
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode node = root;
    while (!stack.isEmpty() || node != null) {
      while (node != null) { stack.push(node); node = node.right; }
      node = stack.pop();
      if (--k == 0) return node.val;
      node = node.left;
    }
    return 0;
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
    int k = Integer.parseInt(sc.nextLine().trim());
    System.out.println(kthLargest(root, k));
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

## Reflect & Connect

Drill the family in **Practice** — [Rank Nodes](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-reversed-sorted-traversal/problems/rank-nodes), [Kth Largest Element](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-reversed-sorted-traversal/problems/kth-largest-element), [Enriched Sum Tree](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-reversed-sorted-traversal/problems/enriched-sum-tree), and [Multiple Replacement](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-reversed-sorted-traversal/problems/multiple-replacement).

Reversed traversal is sorted traversal's mirror, plus the accumulator twist:

- **The family** — k-th largest, rank assignment (largest = 1), **greater-sum / enriched-sum tree** (each node += sum of all greater keys), replace-with-next-greater. All are reversed in-order + per-visit logic.
- **The accumulator is the differentiator** — descending order means "everything larger is already processed," so a running sum/count gives greater-than aggregates in one pass. (Symmetrically, ascending in-order with a running total gives *smaller-than* aggregates.) Same "visit in an order that makes the running aggregate exactly what you need" idea as prefix sums and the sweep line.
- **Direction is the only knob** — sorted traversal and reversed traversal share one template; left-first vs right-first is the whole difference. Pick by whether the problem is about smaller-than or larger-than.

**Prerequisites:** [Sorted Traversal](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-sorted-traversal/pattern).
**What's next:** prune whole subtrees out of range during traversal — [Range Postorder](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-range-postorder/pattern).

## Recall

> **Mnemonic:** *Reverse in-order (right, node, left) = descending. k-th largest: count to k. Greater-sum/rank: keep a running total — descending means all larger keys already seen.*

| | |
|---|---|
| Engine | reversed in-order (right → node → left) → descending keys |
| k-th largest | count visits, stop at `k` → `O(k + h)` |
| Greater-sum tree | `total += node.val; node.val = total` per visit |
| Why one pass | descending ⇒ all larger keys already accumulated |
| Cost | `O(n)` (or `O(k+h)`), `O(h)` space |

<details>
<summary><strong>Q:</strong> How do you visit a BST's keys in descending order?</summary>

**A:** Reversed in-order — right subtree, then node, then left subtree.

</details>
<details>
<summary><strong>Q:</strong> How do you find the k-th largest?</summary>

**A:** Reversed in-order counting visits, returning the key at count `k` (early-exit).

</details>
<details>
<summary><strong>Q:</strong> Why does descending order make the greater-sum tree a one-pass `O(n)` computation?</summary>

**A:** When you visit a node, every larger key is already in the running total, so one addition maintains "sum of everything ≥ this node."

</details>
<details>
<summary><strong>Q:</strong> What's the only difference from sorted traversal?</summary>

**A:** Direction — right-first (descending) instead of left-first (ascending); same template.

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, 4th ed., §12.1 — in-order traversal (and its reverse).
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §3.2 — ordered operations; rank queries.
- Reversed in-order for k-th largest and the greater-sum-tree accumulator are standard (LeetCode "Convert BST to Greater Tree"); both runnable blocks are verified by running (`kth_largest ⇒ 9, 7, 1`; greater-sum `⇒ [37,36,33,29,24,17,9]`).
