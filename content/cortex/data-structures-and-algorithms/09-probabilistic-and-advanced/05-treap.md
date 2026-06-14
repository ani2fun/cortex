---
title: Treap
summary: "A binary tree that is simultaneously a BST (ordered by key) and a heap (ordered by a random priority). The random priorities force the shape of a randomly-built BST, giving expected O(log n) operations with far simpler code than AVL or red-black — no rotation rules, just rotate toward the higher priority."
prereqs:
  - trees-binary-search-tree-introduction-to-binary-search-trees
  - trees-heap-what-is-a-heap
---

## Why It Exists

A [binary search tree](/cortex/data-structures-and-algorithms/trees/binary-search-tree/introduction-to-binary-search-trees) gives `O(log n)` operations *only if it stays balanced*. Feed it keys in sorted order and it degenerates into a height-`n` chain — `O(n)` per operation, the worst case an adversary (or just already-sorted data) hands you for free. Balanced BSTs like AVL and red-black trees prevent this, but at the cost of intricate rotation-and-recoloring rules that are notoriously fiddly to implement correctly.

A **treap** gets the same expected `O(log n)` with almost none of that complexity. The trick is to make every node carry *two* values: its **key** (which obeys the BST ordering, left < node < right) and a **random priority** (which obeys the [heap](/cortex/data-structures-and-algorithms/trees/heap/what-is-a-heap) ordering, parent > children). Here's the magic: for a fixed set of keys and priorities, the tree shape is *uniquely determined* — and it's exactly the BST you'd get by inserting the keys in *decreasing priority order*. Since the priorities are random, that's a *random* insertion order, which is balanced in expectation no matter what order the keys actually arrived in. Randomness replaces the balancing rules, just like in a [skip list](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/skip-list) — a Las Vegas structure (always correct, expected-fast).

## See It Work

Insert keys — even in sorted order — and the treap stays a valid BST that's also balanced. Each insert drops the key in by BST rule, then rotates it up until the heap property on priorities is restored.

```python run viz=binary-tree viz-root=root
import random, ast
class Node:
    def __init__(self, key, pri):
        self.key, self.pri = key, pri
        self.left = self.right = None

def rot_right(n): l = n.left; n.left = l.right; l.right = n; return l
def rot_left(n):  r = n.right; n.right = r.left; r.left = n; return r

def insert(n, key, rng):
    if n is None:
        return Node(key, rng.random())                 # fresh random priority
    if key < n.key:
        n.left = insert(n.left, key, rng)
        if n.left.pri > n.pri:                          # child outranks parent -> rotate up
            n = rot_right(n)
    else:
        n.right = insert(n.right, key, rng)
        if n.right.pri > n.pri:
            n = rot_left(n)
    return n

def search(n, key):
    while n:
        if key == n.key: return True
        n = n.left if key < n.key else n.right          # pure BST walk on keys
    return False

def inorder(n, out):
    if n: inorder(n.left, out); out.append(n.key); inorder(n.right, out)

def build(keys, seed):
    rng = random.Random(seed); root = None
    for k in keys:
        root = insert(root, k, rng)
    return root

n = ast.literal_eval(input())                          # insert keys 1..n in sorted order
sk1 = ast.literal_eval(input())                        # key present in tree
sk2 = ast.literal_eval(input())                        # key absent from tree
t = build(list(range(1, n + 1)), seed=1)
out = []; inorder(t, out)
print("true" if out == sorted(out) else "false")       # always a valid BST
print(("true" if search(t, sk1) else "false") + " " + ("true" if search(t, sk2) else "false"))
```

```java run viz=binary-tree viz-root=root
import java.util.*;
public class Main {
    static class Node { int key; double pri; Node left, right; Node(int k, double p) { key = k; pri = p; } }
    static Node rotR(Node n) { Node l = n.left; n.left = l.right; l.right = n; return l; }
    static Node rotL(Node n) { Node r = n.right; n.right = r.left; r.left = n; return r; }
    static Node insert(Node n, int key, Random rng) {
        if (n == null) return new Node(key, rng.nextDouble());
        if (key < n.key) { n.left = insert(n.left, key, rng);  if (n.left.pri > n.pri)  n = rotR(n); }
        else             { n.right = insert(n.right, key, rng); if (n.right.pri > n.pri) n = rotL(n); }
        return n;
    }
    static boolean search(Node n, int key) {
        while (n != null) { if (key == n.key) return true; n = key < n.key ? n.left : n.right; }
        return false;
    }
    static void inorder(Node n, List<Integer> out) {
        if (n != null) { inorder(n.left, out); out.add(n.key); inorder(n.right, out); }
    }
    static Node build(int[] keys, long seed) {
        Random rng = new Random(seed); Node root = null;
        for (int k : keys) root = insert(root, k, rng);
        return root;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = Integer.parseInt(sc.nextLine().trim());
        int sk1 = Integer.parseInt(sc.nextLine().trim());
        int sk2 = Integer.parseInt(sc.nextLine().trim());
        int[] ks = new int[n]; for (int i = 0; i < n; i++) ks[i] = i + 1;
        Node t = build(ks, 1);
        List<Integer> out = new ArrayList<>(); inorder(t, out);
        List<Integer> srt = new ArrayList<>(out); Collections.sort(srt);
        System.out.println(out.equals(srt));            // true
        System.out.println(search(t, sk1) + " " + search(t, sk2));
    }
}
```

```testcases
{
  "args": [
    { "id": "n",   "label": "key count (1..n)", "type": "number", "placeholder": "15" },
    { "id": "sk1", "label": "search (present)", "type": "number", "placeholder": "7" },
    { "id": "sk2", "label": "search (absent)",  "type": "number", "placeholder": "20" }
  ],
  "cases": [
    { "args": { "n": "15", "sk1": "7",  "sk2": "20" }, "expected": "true\ntrue false" },
    { "args": { "n": "10", "sk1": "5",  "sk2": "99" }, "expected": "true\ntrue false" }
  ]
}
```

Both print `true` (in-order is the sorted keys — a valid BST) then `true false`. The keys went in sorted, which would wreck a plain BST, but the random priorities reshape the tree as it builds. The in-order traversal and search results are *always* correct regardless of the random seed — only the tree's exact shape depends on the coins.

## How It Works

A node is `(key, priority)`. The tree is a BST on keys *and* a max-heap on priorities at the same time — and those two constraints together pin down a unique shape:

```d2
direction: down
root: "key=4, pri=0.9   (highest priority -> root)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
l: "key=2, pri=0.6" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
r: "key=6, pri=0.5" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
ll: "key=1, pri=0.3" {style.fill: "#fde68a"; style.stroke: "#d97706"}
lr: "key=3, pri=0.2" {style.fill: "#fde68a"; style.stroke: "#d97706"}
rr: "key=7, pri=0.1" {style.fill: "#fde68a"; style.stroke: "#d97706"}
root -> l
root -> r
l -> ll
l -> lr
r -> rr
note: "BST by KEY (left<node<right)  AND  max-heap by PRIORITY (parent>children).\nShape = the BST built by inserting keys in DECREASING priority order.\nRandom priorities => random insertion order => expected O(log n) height." {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
```

<p align="center"><strong>Every node satisfies both orderings: keys go left-small/right-large (BST), priorities go parent-high/child-low (heap). For given keys and priorities the shape is unique — the tree you'd build by inserting keys highest-priority-first.</strong></p>

Three load-bearing facts:

- **Two orderings pin one shape.** Among all BSTs on a key set, exactly one also satisfies the heap order on a given set of priorities — the *Cartesian tree*. So the priorities, not the arrival order, decide the structure. The highest-priority key is always the root, recursively.
- **Random priorities = random insertion order = balanced.** That unique shape is identical to the BST you'd build by inserting keys in *decreasing* priority order. Make the priorities uniformly random and you've simulated a *random* insertion order — whose expected height is `O(log n)` (`≈ 1.39·log₂ n`) — *no matter what order the keys really came in*. Sorted input can't hurt you ([Trace It](#trace-it)).
- **Insert/delete are just "rotate toward higher priority."** Insert: place the key as a BST leaf, then rotate it *up* while its priority beats its parent's (restoring the heap). Delete: rotate the node *down* toward whichever child has higher priority until it's a leaf, then snip it. No balance factors, no colors, no case analysis — one rule. Treaps also support `O(log n)` **split** (by key, into two treaps) and **merge**, the primitives behind implicit treaps, ropes, and order statistics.

> **Key takeaway.** A treap is a BST on keys *and* a heap on **random priorities**. The two orderings fix a unique shape — the Cartesian tree — equal to inserting keys in decreasing-priority (i.e. random) order, so the expected height is `O(log n)` *regardless of the real insertion order*. Operations are "rotate toward the higher priority": far simpler than AVL/red-black, Las Vegas (always correct, expected-fast), with `O(log n)` split/merge as a bonus.

## Trace It

The treap's whole reason for existing is defeating the input order that destroys a plain BST.

**Predict before you run:** you insert the keys `1, 2, 3, …, 31` in *sorted* order. A plain unbalanced BST becomes a height-31 chain — `O(n)`. Insert the same sorted keys into a treap with random priorities. What's its height — also around 31, or close to `log₂ 31 ≈ 5`?

```python run viz=binary-tree viz-root=root
import random
class Node:
    def __init__(self, key, pri):
        self.key, self.pri = key, pri; self.left = self.right = None
def rot_right(n): l = n.left; n.left = l.right; l.right = n; return l
def rot_left(n):  r = n.right; n.right = r.left; r.left = n; return r
def treap_insert(n, key, rng):
    if n is None: return Node(key, rng.random())
    if key < n.key:
        n.left = treap_insert(n.left, key, rng)
        if n.left.pri > n.pri: n = rot_right(n)
    else:
        n.right = treap_insert(n.right, key, rng)
        if n.right.pri > n.pri: n = rot_left(n)
    return n
def height(n): return 0 if n is None else 1 + max(height(n.left), height(n.right))

class PlainBST:
    def __init__(self): self.root = None
    def insert(self, k):
        if self.root is None: self.root = Node(k, 0); return
        n = self.root
        while True:
            if k < n.key:
                if n.left is None: n.left = Node(k, 0); return
                n = n.left
            else:
                if n.right is None: n.right = Node(k, 0); return
                n = n.right

keys = list(range(1, 32))                              # sorted 1..31
bst = PlainBST()
for k in keys: bst.insert(k)
rng = random.Random(7); treap = None
for k in keys: treap = treap_insert(treap, k, rng)
print("plain BST height (sorted insert):", height(bst.root))
print("treap height     (sorted insert):", height(treap))
```

<details>
<summary><strong>Reveal</strong></summary>

The plain BST is height **31** — a pure right-leaning chain, exactly the degenerate `O(n)` case, because each new key is larger than all before it and slots in as the rightmost leaf. The treap is height **10** — roughly `log`-scale and dramatically shorter, even though the keys arrived in the *same* sorted order. The random priorities scrambled the *effective* build order: the highest-priority key (whatever it happened to be) became the root, splitting the rest into balanced-ish halves recursively, so the tree built itself like a *randomly* inserted BST rather than a sorted one. (10 is a bit above the ~7 expectation for 31 keys — a single random sample has variance — but it's a world away from 31, and it stays logarithmic as `n` grows.) This is the same defence randomness provides elsewhere: a [skip list](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/skip-list)'s coin-flip express lanes and [randomized quicksort](/cortex/data-structures-and-algorithms/algorithms-by-strategy/randomized-algorithms/introduction-to-randomized-algorithms)'s random pivot both neutralise adversarial input the same way — by making the *structure* depend on your private coins, not on the data's order.

</details>

## Your Turn

**Split** a treap by a key into two valid treaps — every key `≤ k` on the left, every key `> k` on the right. It's the treap's signature primitive (the basis of implicit treaps and ropes), and it's a clean recursion: at each node, decide which side it belongs to and recurse into the boundary child.

```python run viz=binary-tree viz-root=root
import random, ast
class Node:
    def __init__(self, key, pri):
        self.key, self.pri = key, pri; self.left = self.right = None
def rot_right(n): l = n.left; n.left = l.right; l.right = n; return l
def rot_left(n):  r = n.right; n.right = r.left; r.left = n; return r
def insert(n, key, rng):
    if n is None: return Node(key, rng.random())
    if key < n.key:
        n.left = insert(n.left, key, rng)
        if n.left.pri > n.pri: n = rot_right(n)
    else:
        n.right = insert(n.right, key, rng)
        if n.right.pri > n.pri: n = rot_left(n)
    return n
def inorder(n, out):
    if n: inorder(n.left, out); out.append(n.key); inorder(n.right, out)

def split(n, key):                                     # -> (treap with keys <= key, treap with keys > key)
    # Your code goes here
    return (None, None)

n = ast.literal_eval(input())
split_key = ast.literal_eval(input())
rng = random.Random(2); root = None
for k in range(1, n + 1):
    root = insert(root, k, rng)
left, right = split(root, split_key)
lo = []; inorder(left, lo)
ro = []; inorder(right, ro)
print(lo)
print(ro)
```

```java run viz=binary-tree viz-root=root
import java.util.*;
public class Main {
    static class Node { int key; double pri; Node left, right; Node(int k, double p) { key = k; pri = p; } }
    static Node rotR(Node n) { Node l = n.left; n.left = l.right; l.right = n; return l; }
    static Node rotL(Node n) { Node r = n.right; n.right = r.left; r.left = n; return r; }
    static Node insert(Node n, int key, Random rng) {
        if (n == null) return new Node(key, rng.nextDouble());
        if (key < n.key) { n.left = insert(n.left, key, rng);  if (n.left.pri > n.pri)  n = rotR(n); }
        else             { n.right = insert(n.right, key, rng); if (n.right.pri > n.pri) n = rotL(n); }
        return n;
    }
    static void inorder(Node n, List<Integer> out) {
        if (n != null) { inorder(n.left, out); out.add(n.key); inorder(n.right, out); }
    }
    static Node[] split(Node n, int key) {              // [<=key, >key]
        // Your code goes here
        return new Node[]{null, null};
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = Integer.parseInt(sc.nextLine().trim());
        int splitKey = Integer.parseInt(sc.nextLine().trim());
        Random rng = new Random(2); Node root = null;
        for (int k = 1; k <= n; k++) root = insert(root, k, rng);
        Node[] lr = split(root, splitKey);
        List<Integer> lo = new ArrayList<>(); inorder(lr[0], lo);
        List<Integer> ro = new ArrayList<>(); inorder(lr[1], ro);
        System.out.println(lo);
        System.out.println(ro);
    }
}
```

```testcases
{
  "args": [
    { "id": "n",         "label": "key count (1..n)", "type": "number", "placeholder": "10" },
    { "id": "split_key", "label": "split at key",     "type": "number", "placeholder": "5" }
  ],
  "cases": [
    { "args": { "n": "10", "split_key": "5" }, "expected": "[1, 2, 3, 4, 5]\n[6, 7, 8, 9, 10]" },
    { "args": { "n": "8",  "split_key": "3" }, "expected": "[1, 2, 3]\n[4, 5, 6, 7, 8]" }
  ]
}
```

<details>
<summary><strong>Editorial</strong></summary>

Walk one root-to-leaf path, re-pointing children along the way. If the current node's key is `<= split_key`, it belongs to the left treap — recurse into the right subtree to handle the right boundary, then re-wire. If the node's key is `> split_key`, it belongs to the right treap — recurse into the left subtree instead. O(height) = O(log n) expected.

```python solution time=O(log n) space=O(log n)
import random, ast
class Node:
    def __init__(self, key, pri):
        self.key, self.pri = key, pri; self.left = self.right = None
def rot_right(n): l = n.left; n.left = l.right; l.right = n; return l
def rot_left(n):  r = n.right; n.right = r.left; r.left = n; return r
def insert(n, key, rng):
    if n is None: return Node(key, rng.random())
    if key < n.key:
        n.left = insert(n.left, key, rng)
        if n.left.pri > n.pri: n = rot_right(n)
    else:
        n.right = insert(n.right, key, rng)
        if n.right.pri > n.pri: n = rot_left(n)
    return n
def inorder(n, out):
    if n: inorder(n.left, out); out.append(n.key); inorder(n.right, out)

def split(n, key):                                     # -> (treap with keys <= key, treap with keys > key)
    if n is None:
        return (None, None)
    if n.key <= key:
        left_sub, right_sub = split(n.right, key)
        n.right = left_sub                             # keep n and its <=key right-descendants
        return (n, right_sub)
    else:
        left_sub, right_sub = split(n.left, key)
        n.left = right_sub
        return (left_sub, n)

n = ast.literal_eval(input())
split_key = ast.literal_eval(input())
rng = random.Random(2); root = None
for k in range(1, n + 1):
    root = insert(root, k, rng)
left, right = split(root, split_key)
lo = []; inorder(left, lo)
ro = []; inorder(right, ro)
print(lo)
print(ro)
```

```java solution
import java.util.*;
public class Main {
    static class Node { int key; double pri; Node left, right; Node(int k, double p) { key = k; pri = p; } }
    static Node rotR(Node n) { Node l = n.left; n.left = l.right; l.right = n; return l; }
    static Node rotL(Node n) { Node r = n.right; n.right = r.left; r.left = n; return r; }
    static Node insert(Node n, int key, Random rng) {
        if (n == null) return new Node(key, rng.nextDouble());
        if (key < n.key) { n.left = insert(n.left, key, rng);  if (n.left.pri > n.pri)  n = rotR(n); }
        else             { n.right = insert(n.right, key, rng); if (n.right.pri > n.pri) n = rotL(n); }
        return n;
    }
    static void inorder(Node n, List<Integer> out) {
        if (n != null) { inorder(n.left, out); out.add(n.key); inorder(n.right, out); }
    }
    static Node[] split(Node n, int key) {              // [<=key, >key]
        if (n == null) return new Node[]{null, null};
        if (n.key <= key) { Node[] s = split(n.right, key); n.right = s[0]; return new Node[]{n, s[1]}; }
        Node[] s = split(n.left, key); n.left = s[1]; return new Node[]{s[0], n};
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = Integer.parseInt(sc.nextLine().trim());
        int splitKey = Integer.parseInt(sc.nextLine().trim());
        Random rng = new Random(2); Node root = null;
        for (int k = 1; k <= n; k++) root = insert(root, k, rng);
        Node[] lr = split(root, splitKey);
        List<Integer> lo = new ArrayList<>(); inorder(lr[0], lo);
        List<Integer> ro = new ArrayList<>(); inorder(lr[1], ro);
        System.out.println(lo);
        System.out.println(ro);
    }
}
```

</details>

Both print `[1, 2, 3, 4, 5]` then `[6, 7, 8, 9, 10]`. The split walked one root-to-leaf path, re-pointing children along the way, so it's `O(height) = O(log n)` expected — and both halves are still valid treaps (priorities untouched, so the heap order survives). `split` plus its inverse `merge` are how treaps implement sequence operations like "cut this range out and paste it elsewhere" in logarithmic time.

## Reflect & Connect

- **BST + heap = balanced by chance.** Keys obey BST order, random priorities obey heap order; together they fix the shape of a randomly-built BST, so height is `O(log n)` in expectation regardless of insertion order.
- **Randomness replaces rotation rules.** No balance factors or colors — insert rotates *up* toward higher priority, delete rotates *down*. Far simpler to write correctly than AVL or red-black, at the cost of *expected* rather than *guaranteed* `O(log n)`.
- **Defeats adversarial input.** Sorted keys make a plain BST a chain; the treap's random priorities scramble the effective build order. Same defence as the [skip list](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/skip-list) and [randomized quicksort](/cortex/data-structures-and-algorithms/algorithms-by-strategy/randomized-algorithms/introduction-to-randomized-algorithms) — structure depends on private coins, not data order.
- **Split/merge are the superpower.** `O(log n)` split-by-key and merge make treaps the go-to for *implicit treaps* (index-keyed sequences with range cut/paste/reverse), ropes, and order statistics — operations a plain balanced BST handles far more awkwardly.
- **Las Vegas, like its Part-9 siblings.** Always correct, runtime random — versus the Monte Carlo [bloom filter](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/bloom-filter) / [count-min](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/count-min-sketch) / [HyperLogLog](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/hyperloglog), which trade *accuracy* for space. The treap trades *worst-case guarantees* for simplicity.

## Recall

<details>
<summary><strong>Q:</strong> What two orderings does a treap maintain, and on what?</summary>

**A:** A BST ordering on the **keys** (left < node < right) and a heap ordering on a **random priority** per node (parent's priority > children's). Both hold at every node simultaneously.

</details>
<details>
<summary><strong>Q:</strong> Why does a treap stay balanced regardless of insertion order?</summary>

**A:** For fixed keys and priorities the shape is unique — the BST built by inserting keys in *decreasing priority* order. Random priorities make that a *random* insertion order, whose expected height is `O(log n)` no matter the actual arrival order.

</details>
<details>
<summary><strong>Q:</strong> How does insert work, and how is it simpler than AVL/red-black?</summary>

**A:** Insert the key as a normal BST leaf (with a random priority), then rotate it *up* while its priority exceeds its parent's, restoring the heap order. There are no balance factors, colors, or multi-case rebalancing — just one "rotate toward higher priority" rule.

</details>
<details>
<summary><strong>Q:</strong> Insert keys `1..n` sorted into a plain BST vs a treap — what heights result?</summary>

**A:** The plain BST becomes a height-`n` chain (`O(n)`); the treap stays `O(log n)` in expectation, because the random priorities reshape it as if the keys had been inserted in random order.

</details>
<details>
<summary><strong>Q:</strong> What are split and merge, and why do they matter?</summary>

**A:** `split` cuts a treap by key into two valid treaps (`≤ k` and `> k`) in `O(log n)`; `merge` recombines two treaps whose key ranges don't overlap. They power implicit treaps, ropes, and range operations (cut/paste/reverse a subsequence) that plain balanced BSTs handle awkwardly.

</details>

## Sources & Verify

- **Seidel & Aragon** (1996), "Randomized Search Trees", *Algorithmica* — the treap, the expected-`O(log n)` analysis, and split/merge.
- **Vuillemin** (1980), "A unifying look at data structures" — Cartesian trees, the deterministic structure a treap randomizes.
- **CP-Algorithms**, "Treap (Cartesian tree)" — implicit treaps and the split/merge implementations; **LeetCode** balanced-BST problems (e.g. 1206 Design Skiplist as a randomized-structure cousin) are adjacent drills. The `true`/`true false` membership, the `31`-vs-`10` BST/treap heights, and the `[1..5]` / `[6..10]` split above come from the runnable blocks — re-run to verify (seeded for reproducibility; in-order, search, and split results are seed-independent, the height is not).
