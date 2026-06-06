---
title: Persistent Data Structures
summary: "Structures where every update yields a NEW version while all old versions stay valid and queryable. The trick is structural sharing — path copying clones only the O(log n) nodes on the root-to-change path and shares every untouched subtree, so a new version costs O(log n), not a full O(n) copy. Behind undo, Git, and functional immutable maps."
prereqs:
  - trees-binary-search-tree-introduction-to-binary-search-trees
  - foundations-amortized-analysis
---

## Why It Exists

An ordinary data structure is *ephemeral*: an update mutates it in place, and the previous state is gone. But sometimes you need the *history* — every version, all at once. An editor's undo stack wants every past document; a functional language wants immutable maps you can update without disturbing other holders; Git wants every commit; a database wants old snapshots for concurrent readers (MVCC). A **persistent data structure** gives you exactly that: each update returns a *new version* while all old versions remain valid and queryable.

The naïve way — copy the whole structure on every update — is `O(n)` per version and quadratic to build a history. The elegant way is **structural sharing**: realise that an update touches only a tiny part of the structure, so the new version can *reuse* the unchanged parts of the old one. For a tree, an insert changes only the nodes on one root-to-leaf path; **path copying** clones just those `O(log n)` nodes and points them at the *shared, unchanged* subtrees. A new version costs `O(log n)` time and space, you keep the entire history, and — crucially — because old nodes are never mutated, sharing them is perfectly safe.

## See It Work

A persistent BST insert returns a *new* root and leaves the old tree completely untouched. Both versions answer queries independently.

```python run viz=binary-tree viz-root=root
class Node:
    __slots__ = ("key", "left", "right")
    def __init__(self, key, left=None, right=None):
        self.key, self.left, self.right = key, left, right

def insert(node, key):                                 # returns a NEW root; the old tree is untouched
    if node is None:
        return Node(key)
    if key < node.key:
        return Node(node.key, insert(node.left, key), node.right)   # copy node, new left, SHARE right
    if key > node.key:
        return Node(node.key, node.left, insert(node.right, key))   # copy node, SHARE left, new right
    return node                                        # key already present -> no change

def contains(node, key):
    while node:
        if key == node.key: return True
        node = node.left if key < node.key else node.right
    return False

def inorder(node, out):
    if node: inorder(node.left, out); out.append(node.key); inorder(node.right, out)

v1 = None
for k in [5, 3, 8, 2, 4]:
    v1 = insert(v1, k)                                 # version 1
v2 = insert(v1, 7)                                     # version 2 = v1 + key 7

o1 = []; inorder(v1, o1); o2 = []; inorder(v2, o2)
print(o1, "contains 7:", contains(v1, 7))              # [2,3,4,5,8] False  -- v1 unchanged
print(o2, "contains 7:", contains(v2, 7))              # [2,3,4,5,7,8] True
```

```java run viz=binary-tree viz-root=root
import java.util.*;
public class Main {
    static class Node {
        int key; Node left, right;
        Node(int key, Node left, Node right) { this.key = key; this.left = left; this.right = right; }
    }
    static Node insert(Node n, int key) {               // returns a NEW root; old tree untouched
        if (n == null) return new Node(key, null, null);
        if (key < n.key) return new Node(n.key, insert(n.left, key), n.right);   // copy, new left, share right
        if (key > n.key) return new Node(n.key, n.left, insert(n.right, key));   // copy, share left, new right
        return n;
    }
    static boolean contains(Node n, int key) {
        while (n != null) { if (key == n.key) return true; n = key < n.key ? n.left : n.right; }
        return false;
    }
    static void inorder(Node n, List<Integer> out) {
        if (n != null) { inorder(n.left, out); out.add(n.key); inorder(n.right, out); }
    }
    public static void main(String[] args) {
        Node v1 = null;
        for (int k : new int[]{5, 3, 8, 2, 4}) v1 = insert(v1, k);
        Node v2 = insert(v1, 7);
        List<Integer> o1 = new ArrayList<>(), o2 = new ArrayList<>();
        inorder(v1, o1); inorder(v2, o2);
        System.out.println(o1 + " contains 7: " + contains(v1, 7));   // [2,3,4,5,8] false
        System.out.println(o2 + " contains 7: " + contains(v2, 7));   // [2,3,4,5,7,8] true
    }
}
```

Both print `[2, 3, 4, 5, 8] contains 7: False` then `[2, 3, 4, 5, 7, 8] contains 7: True`. `v2` has the new key, `v1` is exactly as it was — two independent versions from one `insert`, no copying of the whole tree.

## How It Works

`insert` never writes to an existing node. It returns a *fresh* node for each step down the path, wiring it to the new child on one side and the **shared, untouched** subtree on the other:

```d2
direction: down
old: "v1 root (5)\n[unchanged, still valid]" {style.fill: "#94a3b8"; style.stroke: "#475569"}
newroot: "v2 root (5')  <- NEW copy" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
shared: "left subtree (2,3,4)\n>>> SHARED by v1 AND v2 <<<" {style.fill: "#fde68a"; style.stroke: "#d97706"}
newpath: "right subtree (8') <- NEW copy\n -> 7 <- NEW leaf" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
old -> shared: "v1.left"
newroot -> shared: "v2.left (same object!)"
newroot -> newpath: "v2.right"
cost: "only the root-to-change PATH is copied: O(log n) new nodes.\nevery off-path subtree is shared. old nodes never mutate -> safe." {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
shared -> cost
newpath -> cost
```

<p align="center"><strong>Inserting <code>7</code> copies only the path root→right→leaf; the left subtree <code>(2,3,4)</code> is the <em>same object</em> in both versions. New version cost = path length = O(log n), not O(n).</strong></p>

Three load-bearing facts:

- **Path copying copies `O(log n)`, shares the rest.** An insert/delete on a balanced tree touches only the nodes from the root to the change. Clone exactly those, point each clone at the new child and the *shared* old sibling, and you have a new root for `O(log n)` time and space — the rest of the tree is reused by reference. Build `m` versions of an `n`-node tree for `O(m log n)`, not `O(m·n)`.
- **Immutability is what makes sharing safe.** Sharing a subtree between two versions is only correct because no one ever *mutates* a node — every "change" allocates new nodes. A would-be in-place write would corrupt every version that shares that node. Persistence and immutability are two sides of the same coin: never overwrite, always allocate-and-rewire.
- **Path copying is one of three techniques.** It's the simplest and most common (and it's *fully persistent* for trees). Alternatives: **fat nodes** (each node stores a timestamped list of its field values, so one node serves all versions) and **version trees / DSST** (the Driscoll-Sarnak-Sleator-Tarjan method giving `O(1)` amortised overhead). Functional languages lean on path copying because it composes naturally with immutable values.

> **Key takeaway.** A persistent structure keeps every version alive; updates use **structural sharing**, not full copies. **Path copying** clones only the `O(log n)` nodes on the root-to-change path and shares every untouched subtree, so a new version costs `O(log n)` time and space. It works *because* nodes are immutable — old versions are never mutated, so they're safe to share. Powers undo, Git, MVCC, and functional immutable collections.

## Trace It

The whole efficiency claim rests on "only the path is copied." It's worth proving to yourself, because intuition says "a new version must be a new tree."

**Predict before you run:** you insert *one* new key into a persistent balanced BST that already holds `15` keys. How many brand-new nodes get allocated — all `15` (effectively a fresh tree), or only about `log₂ 15 ≈ 4`?

```python run viz=binary-tree viz-root=root
class Node:
    _count = 0
    __slots__ = ("key", "left", "right")
    def __init__(self, key, left=None, right=None):
        self.key, self.left, self.right = key, left, right
        Node._count += 1

def insert(node, key):
    if node is None: return Node(key)
    if key < node.key: return Node(node.key, insert(node.left, key), node.right)
    if key > node.key: return Node(node.key, node.left, insert(node.right, key))
    return node

def build_balanced(keys):                              # bisection insert -> a balanced tree
    root = None
    def rec(lo, hi):
        nonlocal root
        if lo > hi: return
        mid = (lo + hi) // 2
        root = insert(root, keys[mid]); rec(lo, mid - 1); rec(mid + 1, hi)
    rec(0, len(keys) - 1)
    return root

base = build_balanced(list(range(1, 16)))              # 15 keys, balanced (height 4)
Node._count = 0                                        # count only the next insert's allocations
v_next = insert(base, 16)
print("new nodes from inserting ONE key:", Node._count, "(base had 15)")
print("left subtree shared (base.left is v_next.left):", base.left is v_next.left)
```

<details>
<summary><strong>Reveal</strong></summary>

Inserting one key allocates just **5** new nodes — the four on the root-to-leaf path plus the new leaf — not 15. And `base.left is v_next.left` is `True`: the entire left subtree is the *same object* in both the old and new versions, shared by reference, never copied. That's structural sharing made literal — `16` went down the *right* spine, so only the right path got fresh nodes while the whole left half was reused. The two versions overlap almost completely in memory; they differ only along one path. This is why persistence is cheap: a version isn't a copy of the structure, it's a thin *new spine* grafted onto the shared bulk of the previous version. Keep a thousand versions of a million-node tree and you pay `~1000 · log(million) ≈ 20000` nodes of overhead, not a billion. The cost of "remember everything" collapses from `O(versions · n)` to `O(versions · log n)` — and it's exactly the trick behind an undo stack that doesn't blow up your memory.

</details>

## Your Turn

**Persistent cons-list** — the simplest persistent structure, and the one functional languages use for their default list. `prepend` makes a new head pointing at the *entire* old list as its shared tail: `O(1)` per version, and every old version stays intact.

```python run viz=array
class Cons:
    __slots__ = ("head", "tail")
    def __init__(self, head, tail):
        self.head, self.tail = head, tail

def prepend(lst, x):
    return Cons(x, lst)                                # O(1): share the whole old list as the tail

def to_list(lst):
    out = []
    while lst:
        out.append(lst.head); lst = lst.tail
    return out

v1 = None
for x in [1, 2, 3]:
    v1 = prepend(v1, x)                                # v1 = [3, 2, 1]
v2 = prepend(v1, 4)                                    # v2 = [4, 3, 2, 1]

print(to_list(v1))                                     # [3, 2, 1]  -- unchanged
print(to_list(v2))                                     # [4, 3, 2, 1]
print("v2.tail is v1 (shared):", v2.tail is v1)        # True
```

```java run viz=array
import java.util.*;
public class Main {
    static class Cons { int head; Cons tail; Cons(int h, Cons t) { head = h; tail = t; } }
    static Cons prepend(Cons l, int x) { return new Cons(x, l); }     // O(1), shares the old list
    static List<Integer> toList(Cons l) {
        List<Integer> out = new ArrayList<>();
        for (; l != null; l = l.tail) out.add(l.head);
        return out;
    }
    public static void main(String[] args) {
        Cons v1 = null;
        for (int x : new int[]{1, 2, 3}) v1 = prepend(v1, x);   // [3, 2, 1]
        Cons v2 = prepend(v1, 4);                               // [4, 3, 2, 1]
        System.out.println(toList(v1));                         // [3, 2, 1]
        System.out.println(toList(v2));                         // [4, 3, 2, 1]
        System.out.println("v2.tail is v1 (shared): " + (v2.tail == v1));   // true
    }
}
```

Both print `[3, 2, 1]`, `[4, 3, 2, 1]`, then `True`. `v2` shares `v1` *entirely* as its tail — one new node holds the prepended value and a reference to the old list. This is structural sharing in its purest form: `O(1)` updates, the full history retained, and it's why an immutable linked list is the workhorse of functional programming. (The flip side: sharing the *tail* is cheap, but appending to the *end* would force copying, which is why functional lists prepend.)

## Reflect & Connect

- **Keep history via sharing, not copying.** Each update returns a new version that *reuses* the unchanged parts of the old one. Full copies are `O(n)` per version; structural sharing is `O(log n)` (trees) or `O(1)` (cons-list prepend).
- **Path copying = clone the path, share the subtrees.** A tree update copies only the root-to-change nodes (`O(log n)`) and points them at the untouched, shared subtrees. The classic, composable persistence technique.
- **Immutability is the enabler.** Sharing is safe only because nodes are never mutated. Persistence and immutability are inseparable — "never overwrite, always allocate-and-rewire."
- **Three techniques, one goal.** Path copying (simplest), fat nodes (timestamped fields), and version trees / DSST (`O(1)` amortised). Path copying dominates in practice and in functional languages.
- **It's everywhere in real systems.** Undo/redo stacks, Git's commit DAG (each commit shares unchanged trees/blobs), MVCC databases and copy-on-write filesystems (Btrfs, ZFS), React/Redux immutable state, and Clojure/Scala persistent maps and vectors. A [treap](/cortex/data-structures-and-algorithms/probabilistic-and-advanced-treap) or any balanced BST becomes persistent simply by path-copying its updates — closing this Part's tour of structures that bend the usual time/space/exactness rules.

## Recall

<details>
<summary><strong>Q:</strong> What does "persistent" mean for a data structure?</summary>

**A:** Every update produces a *new version* while all previous versions remain valid and queryable. (Contrast *ephemeral*, where an update destroys the prior state.) "Fully persistent" also allows updating any past version.

</details>
<details>
<summary><strong>Q:</strong> What is path copying, and what does it cost?</summary>

**A:** On a tree update, clone only the nodes on the root-to-change path and point each clone at the new child and the *shared* unchanged sibling subtree. It costs `O(log n)` time and space per new version (for a balanced tree), versus `O(n)` for a full copy.

</details>
<details>
<summary><strong>Q:</strong> Why is structural sharing safe?</summary>

**A:** Because nodes are immutable — no update ever mutates an existing node; it allocates new ones. So a subtree shared between versions can never be changed out from under a version that references it.

</details>
<details>
<summary><strong>Q:</strong> Inserting one key into a persistent balanced BST of `n` nodes allocates how many new nodes?</summary>

**A:** About `log n` — the nodes on the root-to-leaf path plus the new leaf. Every off-path subtree is shared by reference with the previous version, not copied.

</details>
<details>
<summary><strong>Q:</strong> Name three places persistent structures appear in real systems.</summary>

**A:** Undo/redo stacks, Git's commit history (shared unchanged trees/blobs), and functional immutable collections (Clojure/Scala maps & vectors). Also MVCC databases and copy-on-write filesystems (Btrfs, ZFS) and React/Redux state.

</details>

## Sources & Verify

- **Driscoll, Sarnak, Sleator & Tarjan** (1989), "Making data structures persistent", *J. Comput. Syst. Sci.* — path copying, fat nodes, and the `O(1)`-overhead general technique.
- **Okasaki** (1998), *Purely Functional Data Structures* — persistent structures via immutability and sharing (lists, trees, queues), the functional-programming canon.
- **Git** internals (commit/tree/blob sharing) and **Clojure** persistent collections are production examples; the `[2,3,4,5,8]`-vs-`[...,7,...]` version split, the `5`-new-nodes / shared-subtree trace, and the `[3,2,1]`/`[4,3,2,1]` cons-list above come from the runnable blocks — re-run to verify.
