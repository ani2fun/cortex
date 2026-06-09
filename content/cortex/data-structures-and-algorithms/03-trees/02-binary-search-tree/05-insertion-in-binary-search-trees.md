---
title: "Insertion in Binary Search Trees"
summary: "To add a key, search for it — it's absent, so the empty child slot where the search falls off is exactly where the key belongs. Attach it there as a new leaf. Existing nodes never move; O(h). The downside: insertion order alone decides the tree's shape (and balance)."
prereqs:
  - 03-trees/02-binary-search-tree/03-recursive-searching-in-binary-search-trees
---

# Insertion in Binary Search Trees

## Why It Exists

A search tree you can't grow is useless. Insertion must add a key *while preserving the invariant* (left < node < right everywhere) — and the elegant fact is that there's exactly **one** spot where a new key can go and keep the tree valid.

Finding it is just a search. Search for the key; since it's not present, the search walks down and falls off the tree at some empty child slot — and *that* slot is precisely where the key belongs (everything above it already compares correctly). Attach the new key there as a **leaf**. No existing node moves; you only add one link. So insertion is "search, then attach at the failure point," `O(h)`. The one catch: because each key lands wherever the search leads, the *order* you insert determines the tree's shape — and a bad order yields a degenerate chain.

## See It Work

Insert `6` into an existing BST and confirm it lands as a leaf at the search-failure point. Run it, then **Visualise** — the new node hangs off an existing one.

> ▶ Run it, then click **Visualise** — `6` searches down (5→8→7) and attaches as `7`'s left child; nothing else moves.

```python run viz=binary-tree viz-root=root
class TreeNode:
    def __init__(self, val):
        self.val = val
        self.left = None
        self.right = None

def insert(root, val):
    if root is None:
        return TreeNode(val)             # search fell off here → this is the spot; new leaf
    if val < root.val:
        root.left = insert(root.left, val)
    elif val > root.val:
        root.right = insert(root.right, val)
    # val == root.val → duplicate; ignore (a policy choice)
    return root

def inorder(n):
    return inorder(n.left) + [n.val] + inorder(n.right) if n else []

root = None
for v in [5, 3, 8, 1, 4, 7, 9]:
    root = insert(root, v)
root = insert(root, 6)                   # 6 < 5? no → 6 < 8 → 6 < 7 → empty → leaf
print(inorder(root))                     # [1, 3, 4, 5, 6, 7, 8, 9] — still sorted
```

## How It Works

`insert(node, val)` is recursive search with one twist — the empty base case *creates* the node instead of reporting a miss:

1. **`node is None`** → the search has reached an empty slot; return a new leaf with `val`. The caller wires it in as a child.
2. **`val < node.val`** → recurse left and reattach: `node.left = insert(node.left, val)`.
3. **`val > node.val`** → recurse right and reattach.
4. **`val == node.val`** → a duplicate; the usual policy is to ignore it (or keep a count) — never insert a second equal key, or the invariant blurs.

```mermaid
flowchart TB
  Q{"node?"} -->|"None"| C["create leaf, return it"]
  Q -->|"&lt; val → left · &gt; val → right"| R["node.child = insert(child, val)"]
  R --> Q
```

<p align="center"><strong>search down by comparison; where the search falls off (an empty child), splice in the new leaf. Only one new link is added.</strong></p>

Insertion is `O(h)` — a search down plus an `O(1)` attach. The crucial property: **new keys are always added as leaves; existing nodes never move.** (That's the opposite of [deletion](/cortex/data-structures-and-algorithms/trees/binary-search-tree/deletion-in-binary-search-trees), which may restructure.) The price of this simplicity is that the *insertion order alone* fixes the shape: insert in random/balanced order and the tree stays bushy (`h ≈ log n`); insert sorted data and every key becomes a right child, giving a height-`n` chain. Plain BSTs can't prevent this — self-balancing trees rotate *during* insertion to keep `h = O(log n)`.

### Key Takeaway

Insert by searching for the key; the empty slot where the search fails is where it belongs — attach a new leaf there, `O(h)`. Existing nodes never move, and duplicates are ignored by policy. But insertion order alone determines balance, which is why self-balancing trees rebalance on insert.

## Trace It

Inserting `6` into the tree (root `5`):

| at node | compare `6` | go |
|---|---|---|
| `5` | `6 > 5` | right |
| `8` | `6 < 8` | left |
| `7` | `6 < 7` | left → **empty** |
| — | attach | `6` becomes `7`'s left child (a leaf) |

Before you read on: insertion always adds a leaf and never moves an existing node — beautifully simple. Yet the *order* of insertion completely determines the tree's shape. Insert `[5,3,8,1,4,7,9]` and you get a balanced tree of height 2; insert the *sorted* sequence `[1,2,3,4,…]` and you get a chain of height `n−1`. Why does an operation that "never moves anything" still produce wildly different — and sometimes terrible — trees?

Because each key is placed *relative to the keys already there*, and "always attach a leaf" gives the structure no chance to *correct* a lopsided history. With sorted input, every new key is larger than all existing ones, so the search always turns right and the key attaches at the far-right tip — extending a chain that can never re-balance itself, since insertion only ever *adds* at the bottom and never *rearranges*. The very simplicity that makes insertion `O(h)` (no restructuring) is also what makes a plain BST defenseless against bad input: it faithfully records the order it received. Fixing this requires *breaking* the "never move a node" rule — which is exactly what AVL/red-black trees do with **rotations** during insertion, trading a little restructuring work for a guaranteed `O(log n)` height. So this lesson's elegance and the next balancing lesson's necessity are two sides of the same fact.

## Your Turn

The reusable insert — note how order changes the shape:

```python run viz=binary-tree viz-root=root
class TreeNode:
    def __init__(self, val):
        self.val = val
        self.left = None
        self.right = None

def insert(root, val):
    if root is None:
        return TreeNode(val)
    if val < root.val: root.left = insert(root.left, val)
    elif val > root.val: root.right = insert(root.right, val)
    return root

def height(n):
    return -1 if n is None else 1 + max(height(n.left), height(n.right))

balanced = None
for v in [4, 2, 6, 1, 3, 5, 7]:        # near-balanced insertion order
    balanced = insert(balanced, v)
chain = None
for v in [1, 2, 3, 4, 5, 6, 7]:        # sorted order → degenerate
    chain = insert(chain, v)
print(height(balanced), height(chain))   # 2 6  (same 7 keys, very different height)
```

```java run viz=binary-tree viz-root=root
public class Main {
  static class TreeNode { int val; TreeNode left, right; TreeNode(int v){ val = v; } }
  static TreeNode insert(TreeNode r, int v) {
    if (r == null) return new TreeNode(v);
    if (v < r.val) r.left = insert(r.left, v);
    else if (v > r.val) r.right = insert(r.right, v);
    return r;
  }
  static int height(TreeNode n) { return n == null ? -1 : 1 + Math.max(height(n.left), height(n.right)); }
  public static void main(String[] args) {
    TreeNode balanced = null;
    for (int v : new int[]{4, 2, 6, 1, 3, 5, 7}) balanced = insert(balanced, v);
    TreeNode chain = null;
    for (int v : new int[]{1, 2, 3, 4, 5, 6, 7}) chain = insert(chain, v);
    System.out.println(height(balanced) + " " + height(chain));   // 2 6
  }
}
```

This is a structural lesson — insertion plus search are the building blocks for construction, deletion, and the BST patterns.

## Reflect & Connect

Insertion is "search, then attach a leaf" — simple, with one consequential side effect:

- **New keys are always leaves** — insertion only adds a link; it never relocates an existing node. This is the cleanest BST mutation, and the conceptual opposite of deletion, which must fill the hole left by a removed node.
- **Order determines shape** — the same keys produce a balanced tree or a chain depending on insertion order. Bulk-loading *sorted* data into a plain BST is the classic mistake; insert in random or median-first order to stay bushy, or use a self-balancing tree.
- **Balancing breaks the "never move" rule** — [AVL](/cortex/data-structures-and-algorithms/trees/avl-tree/introduction-to-avl-trees) and [red-black](/cortex/data-structures-and-algorithms/trees/red-black-tree/introduction-to-red-black-trees) trees do the same search-and-attach, then perform `O(1)` **rotations** back up the path to restore balance, guaranteeing `h = O(log n)` regardless of order. Plain-BST insertion is the foundation they build on.

**Prerequisites:** [Recursive Searching in BSTs](/cortex/data-structures-and-algorithms/trees/binary-search-tree/recursive-searching-in-binary-search-trees).
**What's next:** the hard inverse — removing a key and filling the hole — [Deletion in BSTs](/cortex/data-structures-and-algorithms/trees/binary-search-tree/deletion-in-binary-search-trees).

## Recall

> **Mnemonic:** *Insert = search for the key; the empty slot where the search fails is its spot — attach a new leaf. `O(h)`. Existing nodes never move. Insertion order alone fixes the shape.*

| | |
|---|---|
| Method | search down; at the empty child, create a new leaf |
| Property | new keys are always leaves; no existing node moves |
| Duplicates | ignore (or count) by policy — never two equal keys |
| Cost | `O(h)` (search + `O(1)` attach) |
| Caveat | insertion order determines balance; sorted input → height-`n` chain |

<details>
<summary><strong>Q:</strong> Where does a new key get attached?</summary>

**A:** At the empty child slot where a search for it falls off the tree — as a new leaf.

</details>
<details>
<summary><strong>Q:</strong> Does insertion move existing nodes?</summary>

**A:** No — it only adds one link; new keys are always leaves.

</details>
<details>
<summary><strong>Q:</strong> How are duplicates handled?</summary>

**A:** By policy — usually ignored (or counted); never inserted as a second equal key.

</details>
<details>
<summary><strong>Q:</strong> Why does insertion order matter so much?</summary>

**A:** Each key attaches relative to existing keys with no re-balancing, so sorted input builds a degenerate chain; balancing trees use rotations to prevent this.

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, 4th ed., §12.3 — `TREE-INSERT` and the leaf-attachment property.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §3.2 — BST insertion and the effect of insertion order on shape.
- The search-and-attach insertion and order-dependent shape are standard; both runnable blocks are verified by running (in-order stays sorted after inserting `6`; balanced vs sorted orders give heights `2` vs `6`).
