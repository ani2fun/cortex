---
tier: spine
title: "Mixing Traversals — Boundary Traversal"
summary: "Hard tree problems rarely fit one traversal — they compose several. Boundary traversal is the canonical example: stitch a left-boundary descent, a leaves pass, and a reversed right-boundary descent into one anticlockwise loop, with a skip-leaves rule that stops corner nodes being counted twice. O(N) time."
prereqs:
  - trees-binary-tree-recursive-traversals-in-binary-trees
  - trees-binary-tree-iterative-traversals-in-binary-trees
---

## Why It Exists

The chapter taught traversals one at a time — preorder, inorder, postorder, level-order. But the problems that actually show up in interviews and production rarely fit a single traversal *cleanly*. They're **compositions**: take a slice of one traversal, glue it to a slice of another, and produce one coherent answer no single pass could give. The skill at this level isn't memorizing more patterns — it's *recognizing* which patterns combine, in what order.

**Boundary traversal** is the canonical drill. The task: list the boundary of a binary tree anticlockwise — start at the root, go down the left edge, across the bottom (all the leaves, left to right), and back up the right edge. No one traversal does this. You decompose it into three sub-walks — a **left-boundary descent**, a **leaves pass**, and a **right-boundary descent collected then reversed** — and stitch them around the root. The whole difficulty is in the seams: the corner leaves belong to the bottom *and* sit on an edge, so a naive stitch counts them twice. One rule — *exclude leaves from the two edge walks* — keeps every node in exactly one piece. Get the decomposition and the duplicate rule right and a problem that looks intimidating becomes four simple loops.

## See It Work

The full boundary, anticlockwise, from three stitched sub-walks:

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right

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

def is_leaf(n): return n.left is None and n.right is None

def boundary(root):
    if root is None: return []
    res = []
    if not is_leaf(root): res.append(root.val)        # 1. root (unless it's a lone leaf)
    n = root.left                                      # 2. left boundary: top-down, skip leaves
    while n:
        if not is_leaf(n): res.append(n.val)
        n = n.left if n.left else n.right
    def leaves(n):                                     # 3. all leaves, left to right
        if n is None: return
        if is_leaf(n): res.append(n.val); return
        leaves(n.left); leaves(n.right)
    leaves(root)
    rb, n = [], root.right                             # 4. right boundary: top-down, skip leaves...
    while n:
        if not is_leaf(n): rb.append(n.val)
        n = n.right if n.right else n.left
    res.extend(reversed(rb))                           # ...then reversed -> bottom-up
    return res

root = build_tree(json.loads(input()))   # the test case's level-order values
print(boundary(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
  static class TreeNode {
    int val; TreeNode left, right;
    TreeNode(int val) { this.val = val; }
  }

  static boolean isLeaf(TreeNode n) { return n.left == null && n.right == null; }
  static List<Integer> res;

  static void leaves(TreeNode n) {
    if (n == null) return;
    if (isLeaf(n)) { res.add(n.val); return; }
    leaves(n.left); leaves(n.right);
  }

  static List<Integer> boundary(TreeNode root) {
    res = new ArrayList<>();
    if (root == null) return res;
    if (!isLeaf(root)) res.add(root.val);                                  // 1. root
    TreeNode n = root.left;                                                // 2. left boundary
    while (n != null) { if (!isLeaf(n)) res.add(n.val); n = n.left != null ? n.left : n.right; }
    leaves(root);                                                          // 3. leaves L->R
    List<Integer> rb = new ArrayList<>();                                  // 4. right boundary...
    n = root.right;
    while (n != null) { if (!isLeaf(n)) rb.add(n.val); n = n.right != null ? n.right : n.left; }
    Collections.reverse(rb);                                              // ...reversed
    res.addAll(rb);
    return res;
  }

  static TreeNode buildTree(Integer[] values) {   // [1, 2, 3, null, 4] level-order → root
    if (values.length == 0 || values[0] == null) return null;
    TreeNode root = new TreeNode(values[0]);
    Deque<TreeNode> queue = new ArrayDeque<>();    // build queue: only real nodes, ArrayDeque ok
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

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
    System.out.println(boundary(root));
  }
}
```

```testcases
{
  "args": [
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[20, 8, 22, 4, 12, null, 25, null, null, 10, 14]" }
  ],
  "cases": [
    { "args": { "root": "[20, 8, 22, 4, 12, null, 25, null, null, 10, 14]" }, "expected": "[20, 8, 4, 10, 14, 25, 22]" },
    { "args": { "root": "[1, 2, 3, 4, 5]" }, "expected": "[1, 2, 4, 5, 3]" },
    { "args": { "root": "[1]" }, "expected": "[1]" },
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "[1, 2, 3]" },
    { "args": { "root": "[]" }, "expected": "[]" }
  ]
}
```

Both print the anticlockwise boundary — root `20`, down the left edge (`8`), across the leaves (`4, 10, 14, 25`), back up the right edge (`22`). Four little loops, one anticlockwise tour.

## How It Works

The decomposition is the whole technique: split the boundary into pieces each traversal can handle, then concatenate in order.

```d2
direction: down
a: "1. root  (skip if the root itself is a leaf)"
b: "2. left boundary: walk down root.left, EXCLUDE leaves  (preorder-style descent)"
c: "3. leaves: every leaf, left to right  (stateless preorder, leaves only)"
d: "4. right boundary: walk down root.right EXCLUDING leaves, collect, then REVERSE  (postorder-style)"
a -> b -> c -> d
e: "stitch:  root ++ left ++ leaves ++ reversed-right  =  anticlockwise boundary"
d -> e
```

<p align="center"><strong>Boundary traversal as four sub-walks stitched in order. Each piece is a familiar traversal; the art is the decomposition and the seams.</strong></p>

- **Each piece is a traversal you already know.** The left and right edges are preorder-style descents (follow one child, fall back to the other); the leaves pass is a stateless preorder that emits only leaves. Nothing new — it's *composition*.
- **The skip-leaves rule prevents duplicates.** The leftmost and rightmost leaves sit on an edge *and* are leaves. If the edge walks included leaves, those corners would appear twice. Excluding leaves from the two edge walks puts every node in exactly one piece ([Trace It](#trace-it)).
- **The right boundary is collected top-down, then reversed.** Anticlockwise means the right edge is walked *bottom-up*, but it's easier to descend top-down and reverse the list — the same trick the two-stack [iterative postorder](/cortex/data-structures-and-algorithms/trees/binary-tree/iterative-traversals-in-binary-trees) uses.
- **Cost is `O(N)`.** The leaves pass visits every node once; the two edge walks each touch `O(H)` nodes. Total `O(N)` time, `O(N)` space for the output (plus `O(H)` recursion for the leaves).

> **Key takeaway.** Hard tree problems are *compositions* of simple traversals. Boundary traversal stitches four pieces — root, left-boundary descent, leaves left-to-right, and a reversed right-boundary descent — into one anticlockwise loop. The one rule that makes it correct: **exclude leaves from the two edge walks**, so the corner leaves (which are both edge nodes and leaves) are counted exactly once. `O(N)` time. The meta-skill is decomposition: see which traversals combine and in what order.

## Trace It

The single bug that breaks every first attempt at boundary traversal is forgetting to skip leaves on the edges. Watch the left-boundary walk with and without the guard:

**Predict before you run:** the left boundary of this tree starts at node `8`, whose left child is the leaf `4`. With the skip-leaves guard, what does the left-boundary walk emit? Without it?

```python run viz=binary-tree viz-root=root
class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right
root = TreeNode(20, TreeNode(8, TreeNode(4), TreeNode(12, TreeNode(10), TreeNode(14))),
                    TreeNode(22, None, TreeNode(25)))
def is_leaf(n): return n.left is None and n.right is None

def left_boundary(root, skip_leaves):
    res, n = [], root.left
    while n:
        if not (skip_leaves and is_leaf(n)):
            res.append(n.val)
        n = n.left if n.left else n.right
    return res

print("skip leaves = True :", left_boundary(root, True))    # [8]
print("skip leaves = False:", left_boundary(root, False))   # [8, 4]
```

<details>
<summary><strong>Reveal</strong></summary>

```
skip leaves = True : [8]
skip leaves = False: [8, 4]
```

With the guard the left boundary is just `[8]`; without it, the leaf `4` sneaks in → `[8, 4]`. The problem: `4` is a **leaf**, so the separate leaves pass *also* emits it. Stitch the unguarded edge with the leaves pass and you get `…, 8, 4, … 4, …` — `4` counted twice, and the boundary is wrong. The fix is the one-line rule: edge walks skip leaves, the leaves pass owns them. It's the same "who is responsible for this node" discipline that makes any multi-piece traversal correct — each node must belong to exactly one sub-walk. (Symmetric trap: the rightmost leaf `25` would double-count on the right edge without the same guard.)

</details>

## Your Turn

Decompositions have to survive degenerate shapes. Run the *same* `boundary` function on a tree that's all left edge and no right edge — a left-skewed spine.

**Predict:** for a left-skewed spine tree, what is the boundary? (Hint: every node is on the left edge or is the single leaf.)

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right

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

def is_leaf(n): return n.left is None and n.right is None

def boundary(root):
    if root is None: return []
    res = []
    if not is_leaf(root): res.append(root.val)
    n = root.left
    while n:
        if not is_leaf(n): res.append(n.val)
        n = n.left if n.left else n.right
    def leaves(n):
        if n is None: return
        if is_leaf(n): res.append(n.val); return
        leaves(n.left); leaves(n.right)
    leaves(root)
    rb, n = [], root.right
    while n:
        if not is_leaf(n): rb.append(n.val)
        n = n.right if n.right else n.left
    res.extend(reversed(rb))
    return res

root = build_tree(json.loads(input()))   # the test case's level-order values
print(boundary(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;

public class Main {
  static class TreeNode {
    int val; TreeNode left, right;
    TreeNode(int val) { this.val = val; }
  }

  static boolean isLeaf(TreeNode n) { return n.left == null && n.right == null; }
  static List<Integer> res;

  static void leaves(TreeNode n) {
    if (n == null) return;
    if (isLeaf(n)) { res.add(n.val); return; }
    leaves(n.left); leaves(n.right);
  }

  static List<Integer> boundary(TreeNode root) {
    res = new ArrayList<>();
    if (root == null) return res;
    if (!isLeaf(root)) res.add(root.val);
    TreeNode n = root.left;
    while (n != null) { if (!isLeaf(n)) res.add(n.val); n = n.left != null ? n.left : n.right; }
    leaves(root);
    List<Integer> rb = new ArrayList<>();
    n = root.right;
    while (n != null) { if (!isLeaf(n)) rb.add(n.val); n = n.right != null ? n.right : n.left; }
    Collections.reverse(rb);
    res.addAll(rb);
    return res;
  }

  static TreeNode buildTree(Integer[] values) {   // [1, 2, 3, null, 4] level-order → root
    if (values.length == 0 || values[0] == null) return null;
    TreeNode root = new TreeNode(values[0]);
    Deque<TreeNode> queue = new ArrayDeque<>();    // build queue: only real nodes, ArrayDeque ok
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

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
    System.out.println(boundary(root));
  }
}
```

```testcases
{
  "args": [
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, null, 3]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, null, 3]" }, "expected": "[1, 2, 3]" },
    { "args": { "root": "[1, 2, null, 3, null, 4]" }, "expected": "[1, 2, 3, 4]" },
    { "args": { "root": "[1]" }, "expected": "[1]" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "[1, 2, 3]" },
    { "args": { "root": "[]" }, "expected": "[]" }
  ]
}
```

Root `1` and `2` are on the left edge (neither is a leaf), `3` is the lone leaf, and the right edge is empty. The decomposition degrades gracefully: with no right subtree the fourth piece contributes nothing, and the skip-leaves rule keeps `3` out of the edge walk so it appears once, via the leaves pass.

## Reflect & Connect

- **The real skill is decomposition.** Hard tree problems are compositions of the simple traversals. Spot the pieces, order them, and stitch — boundary traversal is the archetype (left edge + leaves + reversed right edge).
- **Every node belongs to exactly one piece.** The skip-leaves rule is a special case of a general discipline: when sub-walks overlap, assign each shared node to one owner. Here, corner leaves belong to the leaves pass, not the edges.
- **Reuse the tricks you have.** The right-edge "descend then reverse" is the same move as two-stack [iterative postorder](/cortex/data-structures-and-algorithms/trees/binary-tree/iterative-traversals-in-binary-trees); the edge descents are [preorder](/cortex/data-structures-and-algorithms/trees/binary-tree/recursive-traversals-in-binary-trees)-style. Nothing here is new — it's recombination.
- **Test the seams on degenerate shapes.** Left-only spines, right-only spines, a single-node tree, an all-leaves tree — these are where double-counting and off-by-one stitch bugs surface. A composition that survives them is correct.
- **This closes the binary-tree chapter.** With representations, traversals (recursive and iterative), construction, insertion, and now composition, you have the full toolkit — the [binary search tree](/cortex/data-structures-and-algorithms/trees/binary-search-tree/introduction-to-binary-search-trees) adds the ordering rule that turns these traversals into `O(log N)` search.

## Recall

<details>
<summary><strong>Q:</strong> What are the four pieces of a boundary traversal, and in what order are they stitched?</summary>

**A:** (1) the root, (2) the left boundary top-down excluding leaves, (3) all leaves left to right, (4) the right boundary top-down excluding leaves, *reversed*. Concatenated in that order they form the anticlockwise boundary.

</details>
<details>
<summary><strong>Q:</strong> Why must the left and right boundary walks skip leaf nodes?</summary>

**A:** The corner leaves sit on an edge *and* are leaves, so the separate leaves pass already emits them. If the edge walks included leaves, those corners would be counted twice. Skipping leaves on the edges makes every node belong to exactly one piece.

</details>
<details>
<summary><strong>Q:</strong> Why is the right boundary collected top-down and then reversed?</summary>

**A:** Anticlockwise order needs the right edge bottom-up, but descending top-down is simpler. Collect the right edge top-down into a list and reverse it — the same descend-then-reverse trick as two-stack iterative postorder.

</details>
<details>
<summary><strong>Q:</strong> What is the time complexity of boundary traversal?</summary>

**A:** `O(N)`. The leaves pass visits every node once; each edge walk touches `O(H)` nodes. Output and the leaves recursion add `O(N)` and `O(H)` space respectively.

</details>
<details>
<summary><strong>Q:</strong> What's the general lesson behind composing traversals?</summary>

**A:** Decomposition — break a problem no single traversal solves into sub-walks each traversal *can* handle, then stitch them in order, ensuring every shared node has exactly one owner. Recognizing which patterns combine is the senior-level tree skill.

</details>

## Sources & Verify

- **Boundary of Binary Tree** — LeetCode 545 and the GeeksforGeeks "boundary traversal" problem are the canonical statements; the decomposition (left boundary + leaves + reversed right boundary) is the standard solution.
- The [recursive-traversals lesson](/cortex/data-structures-and-algorithms/trees/binary-tree/recursive-traversals-in-binary-trees) for the preorder pieces and the [iterative-traversals lesson](/cortex/data-structures-and-algorithms/trees/binary-tree/iterative-traversals-in-binary-trees) for the descend-then-reverse trick this reuses.
- All boundary outputs come from the runnable blocks above (deterministic) — re-run to verify.
