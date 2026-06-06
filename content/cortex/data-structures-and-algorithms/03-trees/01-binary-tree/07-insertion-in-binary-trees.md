---
tier: spine
title: "Insertion in Binary Trees"
summary: "Adding a node to a plain binary tree is always find-then-relink: the attach is O(1), the work is the search. The canonical rule is BFS to the first empty child slot — level-order, left to right — which keeps the tree complete and is how you grow it one node at a time. Not to be confused with ordered BST insertion."
prereqs:
  - trees-binary-tree-linked-list-implementation-of-binary-trees
  - trees-binary-tree-iterative-traversals-in-binary-trees
---

## Why It Exists

A [binary search tree](/cortex/data-structures-and-algorithms/trees-binary-search-tree-introduction-to-binary-search-trees) tells you exactly where a new value goes — smaller left, larger right. A *plain* binary tree has no such rule, so "where does the new node go?" becomes a **choice**, not a constraint. That freedom is the whole subject of this lesson, and it has one recurring shape: **find, then relink**. Locate an attachment point, then assign a constant number of pointers. The relink is always `O(1)`; the only thing that varies between insertion styles is *where you search*.

The canonical choice — the one worth burning into muscle memory — is **insert at the first empty child slot, found by breadth-first (level-order) search**. Walk the tree level by level, left to right, with a queue; the first node missing a child gets the new one. This rule has a lovely property: it keeps the tree **complete** (every level full except possibly the last, filled left to right), which is exactly the shape that packs into a [flat array](/cortex/data-structures-and-algorithms/trees-binary-tree-array-implementation-of-binary-trees) and underlies heaps. Repeated BFS-insertion *grows* a complete tree one node at a time. (The other variants — insert at the root in `O(1)`, attach as a named node's child, or splice a new parent above a node — are the same find-then-relink with a different search; we'll name them, but BFS-to-the-first-gap is the one to know.)

## See It Work

Insert a value at the first empty slot the BFS finds, and watch the level-order grow by one:

```python run viz=binary-tree viz-root=root
from collections import deque
class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right
#        1
#       / \
#      2   3       <- 2 has a left child but an EMPTY right slot
#     /
#    4
root = TreeNode(1, TreeNode(2, TreeNode(4), None), TreeNode(3))

def level_order(root):
    out, q = [], deque([root] if root else [])
    while q:
        n = q.popleft(); out.append(n.val)
        if n.left:  q.append(n.left)
        if n.right: q.append(n.right)
    return out

def insert(root, val):
    node = TreeNode(val)
    if root is None: return node             # empty tree -> the new node IS the root
    q = deque([root])
    while q:                                 # BFS: level by level, left to right
        cur = q.popleft()
        if cur.left is None:  cur.left  = node; return root   # first empty slot wins
        q.append(cur.left)
        if cur.right is None: cur.right = node; return root
        q.append(cur.right)
    return root

print("before:", level_order(root))   # [1, 2, 3, 4]
insert(root, 5)
print("after :", level_order(root))   # [1, 2, 3, 4, 5]  -> 5 filled 2's empty right slot
```

```java run viz=binary-tree viz-root=root
import java.util.*;
public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) { this.val = val; this.left = left; this.right = right; }
    }
    static List<Integer> levelOrder(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> q = new ArrayDeque<>();
        if (root != null) q.add(root);
        while (!q.isEmpty()) {
            TreeNode n = q.poll(); out.add(n.val);
            if (n.left  != null) q.add(n.left);
            if (n.right != null) q.add(n.right);
        }
        return out;
    }
    static TreeNode insert(TreeNode root, int val) {
        TreeNode node = new TreeNode(val);
        if (root == null) return node;                 // empty tree -> new root
        Deque<TreeNode> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {                          // BFS: level by level, left to right
            TreeNode cur = q.poll();
            if (cur.left  == null) { cur.left  = node; return root; }   // first empty slot wins
            q.add(cur.left);
            if (cur.right == null) { cur.right = node; return root; }
            q.add(cur.right);
        }
        return root;
    }
    public static void main(String[] a) {
        TreeNode root = new TreeNode(1, new TreeNode(2, new TreeNode(4), null), new TreeNode(3));
        System.out.println("before: " + levelOrder(root));
        insert(root, 5);
        System.out.println("after : " + levelOrder(root));
    }
}
```

Both print `before: [1, 2, 3, 4]` and `after : [1, 2, 3, 4, 5]`. The BFS visited `1` (both slots full), then `2` (left full, right empty) and stopped — `5` slotted into `2`'s empty right child, and the tree stays complete.

## How It Works

The queue is doing the work: it produces nodes in level-order, and the first one with a vacancy gets the new child.

```d2
direction: down
a: "queue = [1]    pop 1: left=2 full, right=3 full  ->  enqueue 2, 3"
b: "queue = [2, 3] pop 2: left=4 full, RIGHT empty   ->  attach 5 here, stop"
c: "result: 5 becomes 2.right; tree stays complete; level-order = [1, 2, 3, 4, 5]"
a -> b -> c
```

<p align="center"><strong>BFS-to-the-first-gap. The queue yields nodes level by level (1, then 2, 3, …); the search stops at the first node missing a child and attaches there. Filling gaps left-to-right per level is what keeps the tree complete.</strong></p>

- **Find, then relink.** Every insertion is these two steps. The relink is one pointer assignment — `O(1)`. The cost lives entirely in the *find*.
- **BFS keeps the tree complete.** Scanning level-order left-to-right means the first empty slot is always the leftmost gap in the shallowest unfinished level — so the tree never develops holes above the last row. That completeness is exactly what lets a tree live in a [flat array](/cortex/data-structures-and-algorithms/trees-binary-tree-array-implementation-of-binary-trees) and what heaps rely on.
- **Cost: `O(n)` time, `O(w)` space.** In the worst case the search scans the whole bottom level before finding the gap, so time is `O(n)`; the queue holds at most one level, so space is `O(w)` for the maximum width `w`.
- **The variants are the same shape.** Insert *at the root* is `O(1)` (new node becomes root, the old tree hangs beneath it). Insert *as a named node's child* finds that node and sets its pointer. Insert *a parent above* a node splices a new node between it and its parent. All find-then-relink — only the search changes.

> **Key takeaway.** Inserting into a plain binary tree is **find-then-relink**: the attach is `O(1)`, the work is the search. The canonical rule — **BFS to the first empty child slot**, level-order and left-to-right — keeps the tree **complete** and grows it one node at a time; `O(n)` time, `O(w)` space. The other styles (insert at root `O(1)`, as a named child, or splicing a parent) share the shape and differ only in where they search. Value-ordered placement is *BST* insertion — a different rule entirely.

## Trace It

The "first empty slot" isn't always where intuition points. Take a tree where the *deepest* gap and the *first* gap are different nodes:

```
       1
      / \
     2   3      2 is a leaf (two empty slots); 3 has a left child 6, empty right
        /
       6
```

**Predict before you run:** insert `9`. Does it attach under `2` (shallower, scanned earlier) or near `6` (deeper)?

```python run viz=binary-tree viz-root=root
from collections import deque
class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right
root = TreeNode(1, TreeNode(2), TreeNode(3, TreeNode(6), None))

def insert_where(root, val):
    node = TreeNode(val)
    q = deque([root])
    while q:
        cur = q.popleft()
        if cur.left  is None: cur.left  = node; return cur.val   # report the parent we attached to
        q.append(cur.left)
        if cur.right is None: cur.right = node; return cur.val
        q.append(cur.right)

parent = insert_where(root, 9)
print("9 attached as a child of node:", parent)        # 2
print("node 2's left child is now   :", root.left.left.val)  # 9
```

<details>
<summary><strong>Reveal</strong></summary>

`9` attaches under **node 2**, not near node 6. The BFS visits nodes in level-order: `1` (full), then `2` and `3` on the next level — and `2` comes first (left before right) *and* has an empty left slot, so the search stops there. Node `6` is deeper, but BFS finishes a whole level before descending, so it never even looks below `2` and `3`. This is the defining behavior: the first gap is the **leftmost** vacancy in the **shallowest** unfinished level — which is precisely what keeps the tree complete. (Depth-first insertion would dive toward `6` and leave a hole at `2.left`, breaking completeness — the reason BFS is the canonical rule.)

</details>

## Your Turn

The payoff of BFS-insertion: do it repeatedly into an empty tree and you *build* a complete tree, node by node, in level order.

**Predict:** insert `1, 2, 3, 4, 5, 6, 7` one at a time into an empty tree with BFS-insertion. What is the final level-order?

```python run viz=binary-tree viz-root=root
from collections import deque
class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right

def insert(root, val):
    node = TreeNode(val)
    if root is None: return node
    q = deque([root])
    while q:
        cur = q.popleft()
        if cur.left  is None: cur.left  = node; return root
        q.append(cur.left)
        if cur.right is None: cur.right = node; return root
        q.append(cur.right)
    return root

def level_order(root):
    out, q = [], deque([root] if root else [])
    while q:
        n = q.popleft(); out.append(n.val)
        if n.left:  q.append(n.left)
        if n.right: q.append(n.right)
    return out

root = None
for v in [1, 2, 3, 4, 5, 6, 7]:
    root = insert(root, v)        # reassign: the first insert returns the new root
print("level-order:", level_order(root))   # [1, 2, 3, 4, 5, 6, 7]
```

```java run viz=binary-tree viz-root=root
import java.util.*;
public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }
    static TreeNode insert(TreeNode root, int val) {
        TreeNode node = new TreeNode(val);
        if (root == null) return node;
        Deque<TreeNode> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            TreeNode cur = q.poll();
            if (cur.left  == null) { cur.left  = node; return root; }
            q.add(cur.left);
            if (cur.right == null) { cur.right = node; return root; }
            q.add(cur.right);
        }
        return root;
    }
    static List<Integer> levelOrder(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> q = new ArrayDeque<>();
        if (root != null) q.add(root);
        while (!q.isEmpty()) {
            TreeNode n = q.poll(); out.add(n.val);
            if (n.left  != null) q.add(n.left);
            if (n.right != null) q.add(n.right);
        }
        return out;
    }
    public static void main(String[] a) {
        TreeNode root = null;
        for (int v : new int[]{1, 2, 3, 4, 5, 6, 7}) root = insert(root, v);
        System.out.println("level-order: " + levelOrder(root));
    }
}
```

Both print `level-order: [1, 2, 3, 4, 5, 6, 7]` — a perfect complete tree. Each value landed in the next gap, filling the tree row by row. This is the same complete tree that packs into the array `[1,2,3,4,5,6,7]` from the [array-representation lesson](/cortex/data-structures-and-algorithms/trees-binary-tree-array-implementation-of-binary-trees) — BFS-insertion and the array layout are two views of the same growth order.

## Reflect & Connect

- **Every insertion is find-then-relink.** The relink is one `O(1)` pointer write; the search is the only variable part. Get that shape and all five textbook variants collapse into one idea.
- **BFS-to-the-first-gap keeps the tree complete.** Level-order, left-to-right, fill the leftmost vacancy — no holes above the last row. This is *the* way to grow a complete tree, and the reason heaps and [array-backed trees](/cortex/data-structures-and-algorithms/trees-binary-tree-array-implementation-of-binary-trees) use it.
- **It reuses the BFS machinery.** The queue here is the same breadth-first walk as [level-order traversal](/cortex/data-structures-and-algorithms/trees-binary-tree-iterative-traversals-in-binary-trees) — insertion is just a traversal that stops at the first empty slot.
- **This is *not* BST insertion.** A plain tree has no ordering, so you *choose* the slot (the first gap). In a BST the *value* decides left or right — covered in the [BST lessons](/cortex/data-structures-and-algorithms/trees-binary-search-tree-introduction-to-binary-search-trees). Don't mix the two rules.
- **Deletion is the mirror.** To delete from a complete tree while keeping it complete, overwrite the target's value with the deepest-rightmost node's value, then remove that node — the same "preserve completeness" discipline, run backward.
- **Next: putting it together.** [Practice mixing traversals](/cortex/data-structures-and-algorithms/trees-binary-tree-practice-mix-traversals) drills the traversal + build + insert toolkit on combined problems.

## Recall

<details>
<summary><strong>Q:</strong> What two steps make up every binary-tree insertion?</summary>

**A:** Find the attachment point, then relink — assign a constant number of pointers. The relink is always `O(1)`; the styles of insertion differ only in *where* (and how) you search for the spot.

</details>
<details>
<summary><strong>Q:</strong> How does BFS-first-empty-slot insertion work, and what property does it preserve?</summary>

**A:** A queue walks the tree in level-order, left to right; the first node with a missing child receives the new node. Because it fills the leftmost gap in the shallowest unfinished level, it keeps the tree **complete** (no holes above the last row).

</details>
<details>
<summary><strong>Q:</strong> What are the time and space costs of BFS insertion?</summary>

**A:** `O(n)` time in the worst case (the gap may be at the far end of the bottom level, so the search scans most of the tree) and `O(w)` space for the queue, where `w` is the tree's maximum width.

</details>
<details>
<summary><strong>Q:</strong> How is this different from inserting into a binary search tree?</summary>

**A:** A plain binary tree has no ordering rule, so you *choose* the slot — here, the first BFS gap. In a BST the value being inserted decides the path (smaller goes left, larger goes right) down to a leaf. Same "find-then-relink," completely different *find*.

</details>
<details>
<summary><strong>Q:</strong> How do you delete a node from a complete tree without breaking completeness?</summary>

**A:** Find the target and the deepest-rightmost node (last in level-order). Copy the deepest node's value into the target, then delete the deepest node. Removing the last node never leaves a hole, so the tree stays complete.

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, §10.4 (pointer-based trees) for the `O(1)` relink primitive; the BFS-to-first-gap insertion is the standard "insert into a complete binary tree" used to build **binary heaps** (CLRS ch. 6).
- The [array-representation lesson](/cortex/data-structures-and-algorithms/trees-binary-tree-array-implementation-of-binary-trees) for why complete trees matter, and the [iterative-traversals lesson](/cortex/data-structures-and-algorithms/trees-binary-tree-iterative-traversals-in-binary-trees) for the BFS/queue this reuses.
- `before [1,2,3,4]` → `after [1,2,3,4,5]`, the Trace-It result (`9` attaches under node `2`), and the repeated-insertion `level-order [1,2,3,4,5,6,7]` all come from the runnable blocks above (deterministic) — re-run to verify.
