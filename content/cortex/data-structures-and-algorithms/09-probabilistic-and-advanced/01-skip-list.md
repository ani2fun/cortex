---
title: Skip List
summary: "A sorted linked list with random express lanes — each node is promoted to the next level on a coin flip, giving expected O(log n) search/insert/delete with no rotations. Randomization replaces deterministic balancing (Las Vegas: always correct, expected-fast). The structure inside Redis sorted sets and LevelDB."
prereqs:
  - linear-structures-singly-linked-list-what-is-a-linked-list
  - algorithms-by-strategy-randomized-algorithms-introduction-to-randomized-algorithms
---

## Why It Exists

A sorted linked list is simple but slow: search is `O(n)` because you must step through one node at a time. A balanced BST fixes that with `O(log n)` search — at the cost of rotation logic that's fiddly to get right. The **skip list** reaches the same `O(log n)` with neither: it stacks **express lanes** over a sorted linked list, so a search can leap over big chunks up high and drop down for precision.

The trick is that the express lanes are built by **coin flips**, not bookkeeping. When you insert a node, you flip a coin: heads, promote it to the next lane up; flip again, promote again; stop at the first tails. So about half the nodes reach lane 1, a quarter reach lane 2, and so on — a geometric pyramid that gives *expected* `O(log n)` height. Randomization stands in for deterministic balancing, which is why a skip list has no rotations and far simpler code than an AVL or red-black tree. It's a [Las Vegas](/cortex/data-structures-and-algorithms/algorithms-by-strategy-randomized-algorithms-introduction-to-randomized-algorithms) structure — *always* sorted and correct; only the runtime depends on the coins. That simplicity (and its friendliness to concurrent updates) is why Redis sorted sets, LevelDB, and Java's `ConcurrentSkipListMap` are built on it.

## See It Work

Insert values in any order; search and in-order traversal are always correct, whatever the coins decide. (The `Random(seed)` just makes the structure reproducible — the *answers* don't depend on it.)

```python run viz=array
import random
MAXLVL = 16
class Node:
    def __init__(self, val, level):
        self.val = val
        self.forward = [None] * (level + 1)         # one next-pointer per level it reaches

class SkipList:
    def __init__(self, seed=None):
        self.head = Node(None, MAXLVL)
        self.level = 0
        self.rng = random.Random(seed)
    def _random_level(self):
        lvl = 0
        while lvl < MAXLVL and self.rng.random() < 0.5:   # coin flips: promote while heads
            lvl += 1
        return lvl
    def insert(self, val):
        update = [self.head] * (MAXLVL + 1)
        x = self.head
        for i in range(self.level, -1, -1):              # find predecessors at each level
            while x.forward[i] and x.forward[i].val < val:
                x = x.forward[i]
            update[i] = x
        lvl = self._random_level()
        self.level = max(self.level, lvl)
        node = Node(val, lvl)
        for i in range(lvl + 1):                          # splice in at every level it reaches
            node.forward[i] = update[i].forward[i]
            update[i].forward[i] = node
    def search(self, target):
        x = self.head
        for i in range(self.level, -1, -1):              # drop down the express lanes
            while x.forward[i] and x.forward[i].val < target:
                x = x.forward[i]
        x = x.forward[0]
        return x is not None and x.val == target
    def to_list(self):
        out, x = [], self.head.forward[0]
        while x:
            out.append(x.val); x = x.forward[0]
        return out

sl = SkipList(seed=1)
for v in [3, 6, 7, 9, 12, 19, 17, 26, 21, 25]:
    sl.insert(v)
print(sl.search(19))                                  # True
print(sl.search(20))                                  # False
print(sl.to_list() == sorted([3,6,7,9,12,19,17,26,21,25]))   # True — always sorted
```

```java run viz=array
import java.util.*;
public class Main {
    static final int MAXLVL = 16;
    static class Node {
        int val; Node[] forward;
        Node(int val, int level) { this.val = val; this.forward = new Node[level + 1]; }
    }
    static class SkipList {
        Node head = new Node(Integer.MIN_VALUE, MAXLVL);
        int level = 0; Random rng;
        SkipList(long seed) { rng = new Random(seed); }
        int randomLevel() { int l = 0; while (l < MAXLVL && rng.nextDouble() < 0.5) l++; return l; }
        void insert(int val) {
            Node[] update = new Node[MAXLVL + 1];
            Arrays.fill(update, head);
            Node x = head;
            for (int i = level; i >= 0; i--) {
                while (x.forward[i] != null && x.forward[i].val < val) x = x.forward[i];
                update[i] = x;
            }
            int lvl = randomLevel();
            level = Math.max(level, lvl);
            Node node = new Node(val, lvl);
            for (int i = 0; i <= lvl; i++) { node.forward[i] = update[i].forward[i]; update[i].forward[i] = node; }
        }
        boolean search(int target) {
            Node x = head;
            for (int i = level; i >= 0; i--)
                while (x.forward[i] != null && x.forward[i].val < target) x = x.forward[i];
            x = x.forward[0];
            return x != null && x.val == target;
        }
        List<Integer> toList() {
            List<Integer> out = new ArrayList<>(); Node x = head.forward[0];
            while (x != null) { out.add(x.val); x = x.forward[0]; }
            return out;
        }
    }
    public static void main(String[] args) {
        int[] vals = {3, 6, 7, 9, 12, 19, 17, 26, 21, 25};
        SkipList sl = new SkipList(1);
        for (int v : vals) sl.insert(v);
        System.out.println(sl.search(19));            // true
        System.out.println(sl.search(20));            // false
        int[] sorted = vals.clone(); Arrays.sort(sorted);
        List<Integer> s = new ArrayList<>(); for (int v : sorted) s.add(v);
        System.out.println(sl.toList().equals(s));    // true
    }
}
```

Both print `true`, `false`, `true`. `19` is present, `20` isn't, and the level-0 chain is always the sorted order — no matter how the coins landed. The express lanes change only *how fast* the search runs, never the result.

## How It Works

Search starts at the top-left and zig-zags down: move *right* while the next node is still smaller than the target, drop *down* a level when it isn't. High lanes skip far; low lanes refine.

```d2
direction: right
l2: "Level 2 (express):  head ----------> 12 ----------> 26" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
l1: "Level 1 (local):    head ---> 6 ---> 12 ---> 19 ---> 26" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
l0: "Level 0 (all):      head -> 3 -> 6 -> 7 -> 9 -> 12 -> 17 -> 19 -> 21 -> 25 -> 26" {style.fill: "#fde68a"; style.stroke: "#d97706"}
coin: "each node's height = coin flips until tails\n-> P(node reaches level k) = 1 / 2^k\n-> ~n/2 at L1, ~n/4 at L2 ... -> expected height log2(n)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
l2 -> l1
l1 -> l0
l0 -> coin
```

<p align="center"><strong>Each node is promoted upward on coin flips, so the lanes thin out by half each level — a geometric pyramid of expected height <code>log₂ n</code>. Search drops from the top express lane down to level 0, moving right then down, leaping over most nodes.</strong></p>

Three load-bearing facts:

- **Levels are geometric, so the height is `O(log n)` in expectation.** A node reaches level `k` only if it flipped heads `k` times in a row — probability `1/2^k`. So roughly `n/2` nodes are on level 1, `n/4` on level 2, and the tallest tower is about `log₂ n` high. That pyramid is the whole performance story; no global structure is maintained, just per-node coin flips at insert time.
- **Search is "right then down."** From the highest lane, advance right while the next value is below the target (cheap big leaps), and drop a level when it would overshoot. Each level contributes `O(1)` expected steps before dropping, and there are `O(log n)` levels — hence `O(log n)` expected search, insert, and delete.
- **No rotations — randomness replaces balancing.** A balanced BST keeps `O(log n)` by *restructuring* on every update (rotations, color flips). A skip list keeps it by *chance*: a fresh coin flip per insert. The result is always correct (Las Vegas), only the speed is probabilistic, and the code is dramatically simpler — which also makes lock-free concurrent versions feasible (local pointer splices, no whole-tree rebalance).

> **Key takeaway.** A skip list is a sorted linked list with **randomly-promoted express lanes**: each node's height is coin flips until tails (`P(level ≥ k) = 2⁻ᵏ`), giving an expected `log₂ n`-tall pyramid and `O(log n)` expected search/insert/delete. Randomization replaces rotations — always correct (Las Vegas), expected-fast, and far simpler than a balanced BST. Search moves *right then down*.

## Trace It

The express lanes *are* the speed. Strip them away and a skip list is just a sorted linked list wearing a costume.

**Predict before you run:** searching for the largest value in a 64-node skip list. With normal coin-flip promotions (`p = ½`) it takes a handful of comparisons. If the promotion probability is `0` — every node stays on level 0, no node ever gets promoted — how many comparisons does the same search take?

```python run viz=array
import random
MAXLVL = 16
class Node:
    def __init__(self, val, level):
        self.val = val; self.forward = [None] * (level + 1)
class SkipList:
    def __init__(self, p, seed):
        self.head = Node(None, MAXLVL); self.level = 0; self.p = p; self.rng = random.Random(seed)
    def _random_level(self):
        lvl = 0
        while lvl < MAXLVL and self.rng.random() < self.p:
            lvl += 1
        return lvl
    def insert(self, val):
        update = [self.head] * (MAXLVL + 1); x = self.head
        for i in range(self.level, -1, -1):
            while x.forward[i] and x.forward[i].val < val: x = x.forward[i]
            update[i] = x
        lvl = self._random_level(); self.level = max(self.level, lvl)
        node = Node(val, lvl)
        for i in range(lvl + 1):
            node.forward[i] = update[i].forward[i]; update[i].forward[i] = node
    def search_steps(self, target):
        x = self.head; steps = 0
        for i in range(self.level, -1, -1):
            while x.forward[i] and x.forward[i].val < target:
                x = x.forward[i]; steps += 1
        return steps

def steps(p):
    sl = SkipList(p, seed=7)
    for v in range(64):
        sl.insert(v)
    return sl.search_steps(63)

print("p = 0.5 (express lanes):", steps(0.5))
print("p = 0   (no promotions): ", steps(0.0))
```

<details>
<summary><strong>Reveal</strong></summary>

With `p = ½` the search takes just **2** comparisons; with `p = 0` it takes **63** — every single node, because the structure has degenerated into a plain sorted linked list with no shortcuts. That's the entire point of the express lanes: the random promotions are what turn an `O(n)` linear scan into an `O(log n)` search. Set the coin to always-tails and you've deleted every express lane, leaving level 0 — exactly the linked list a skip list is built *on top of*. (The `2` is for this seed; with real coins it varies run to run but stays `O(log n)` in expectation, while the `p = 0` case is *always* `n − 1`.) This is the inverse of the [randomized-quicksort](/cortex/data-structures-and-algorithms/algorithms-by-strategy-randomized-algorithms-introduction-to-randomized-algorithms) lesson: there, randomness *defended* a good case against adversaries; here, randomness *creates* the good case out of nothing but a sorted list and a coin.

</details>

## Your Turn

**Range query** — collect every value in `[lo, hi]`. The skip list shines here: leap down the express lanes to the first value `≥ lo`, then walk level 0 until you pass `hi`. (This is exactly how Redis serves `ZRANGEBYSCORE`.)

```python run viz=array
import random
MAXLVL = 16
class Node:
    def __init__(self, val, level):
        self.val = val; self.forward = [None] * (level + 1)
class SkipList:
    def __init__(self, seed=None):
        self.head = Node(None, MAXLVL); self.level = 0; self.rng = random.Random(seed)
    def _random_level(self):
        lvl = 0
        while lvl < MAXLVL and self.rng.random() < 0.5: lvl += 1
        return lvl
    def insert(self, val):
        update = [self.head] * (MAXLVL + 1); x = self.head
        for i in range(self.level, -1, -1):
            while x.forward[i] and x.forward[i].val < val: x = x.forward[i]
            update[i] = x
        lvl = self._random_level(); self.level = max(self.level, lvl)
        node = Node(val, lvl)
        for i in range(lvl + 1):
            node.forward[i] = update[i].forward[i]; update[i].forward[i] = node
    def range_query(self, lo, hi):
        x = self.head
        for i in range(self.level, -1, -1):              # express-lane descent to the first value >= lo
            while x.forward[i] and x.forward[i].val < lo:
                x = x.forward[i]
        x = x.forward[0]
        out = []
        while x and x.val <= hi:                          # walk level 0 within the range
            out.append(x.val); x = x.forward[0]
        return out

sl = SkipList(seed=2)
for v in [3, 6, 7, 9, 12, 19, 17, 26, 21, 25]:
    sl.insert(v)
print(sl.range_query(7, 19))     # [7, 9, 12, 17, 19]
print(sl.range_query(20, 30))    # [21, 25, 26]
```

```java run viz=array
import java.util.*;
public class Main {
    static final int MAXLVL = 16;
    static class Node { int val; Node[] forward; Node(int v, int l) { val = v; forward = new Node[l + 1]; } }
    static class SkipList {
        Node head = new Node(Integer.MIN_VALUE, MAXLVL); int level = 0; Random rng;
        SkipList(long seed) { rng = new Random(seed); }
        int randomLevel() { int l = 0; while (l < MAXLVL && rng.nextDouble() < 0.5) l++; return l; }
        void insert(int val) {
            Node[] update = new Node[MAXLVL + 1]; Arrays.fill(update, head); Node x = head;
            for (int i = level; i >= 0; i--) { while (x.forward[i] != null && x.forward[i].val < val) x = x.forward[i]; update[i] = x; }
            int lvl = randomLevel(); level = Math.max(level, lvl);
            Node node = new Node(val, lvl);
            for (int i = 0; i <= lvl; i++) { node.forward[i] = update[i].forward[i]; update[i].forward[i] = node; }
        }
        List<Integer> rangeQuery(int lo, int hi) {
            Node x = head;
            for (int i = level; i >= 0; i--) while (x.forward[i] != null && x.forward[i].val < lo) x = x.forward[i];
            x = x.forward[0];
            List<Integer> out = new ArrayList<>();
            while (x != null && x.val <= hi) { out.add(x.val); x = x.forward[0]; }
            return out;
        }
    }
    public static void main(String[] args) {
        SkipList sl = new SkipList(2);
        for (int v : new int[]{3, 6, 7, 9, 12, 19, 17, 26, 21, 25}) sl.insert(v);
        System.out.println(sl.rangeQuery(7, 19));     // [7, 9, 12, 17, 19]
        System.out.println(sl.rangeQuery(20, 30));    // [21, 25, 26]
    }
}
```

Both print `[7, 9, 12, 17, 19]` then `[21, 25, 26]`. The express lanes get you to the range's start in `O(log n)`, and the sorted level-0 chain delivers the results in order — `O(log n + k)` for `k` results. A balanced BST needs explicit in-order successor logic for the same query; the skip list's bottom lane *is* the sorted sequence.

## Reflect & Connect

- **Randomness replaces rotations.** A skip list reaches `O(log n)` by coin flips at insert time, not by restructuring. Always correct (Las Vegas); only the runtime is probabilistic. That's the whole reason its code is simpler than AVL or red-black trees.
- **Geometric levels give the log.** `P(node reaches level k) = 2⁻ᵏ`, so the pyramid is expected `log₂ n` tall and search is `O(log n)`. Kill the promotions and it collapses to an `O(n)` linked list.
- **Search is right-then-down.** Big leaps on high lanes, refinement on low ones. Insert and delete first *search* to find the per-level predecessors, then splice — also `O(log n)`.
- **Concurrency-friendly.** Updates are local pointer splices with no global rebalance, so lock-free skip lists are practical — why `ConcurrentSkipListMap`, Redis sorted sets, and LevelDB's memtable use them where a balanced tree would need heavier locking.
- **It's a [Las Vegas](/cortex/data-structures-and-algorithms/algorithms-by-strategy-randomized-algorithms-introduction-to-randomized-algorithms) data structure.** Same family as randomized quicksort — guaranteed answer, expected-fast — and the opening act of this Part's *probabilistic* structures (next: [bloom filters](/cortex/data-structures-and-algorithms/probabilistic-and-advanced-bloom-filter) and sketches, which trade exactness for space).

## Recall

<details>
<summary><strong>Q:</strong> How does a skip list decide each node's height?</summary>

**A:** Coin flips: promote to the next level while you keep flipping heads, stop at the first tails. So `P(node reaches level k) = 2⁻ᵏ` — about half the nodes on level 1, a quarter on level 2, etc.

</details>
<details>
<summary><strong>Q:</strong> Why is search expected `O(log n)`?</summary>

**A:** The geometric level distribution makes the tower expected `log₂ n` tall, and the "right-then-down" search spends `O(1)` expected steps per level. `O(log n)` levels × `O(1)` per level = `O(log n)` expected — for search, insert, and delete.

</details>
<details>
<summary><strong>Q:</strong> What happens if no node is ever promoted?</summary>

**A:** The structure degenerates to a plain sorted linked list — only level 0 — and search becomes `O(n)`. The random express lanes are exactly what buy the logarithmic time.

</details>
<details>
<summary><strong>Q:</strong> How is a skip list simpler than a balanced BST, and why does that matter?</summary>

**A:** It has no rotations or recoloring — balance is achieved by chance, not restructuring. The simpler, more *local* updates (pointer splices, no whole-structure rebalance) make lock-free concurrent implementations practical (e.g. `ConcurrentSkipListMap`).

</details>
<details>
<summary><strong>Q:</strong> Is a skip list always correct, or can the randomness make it wrong?</summary>

**A:** Always correct — it's a Las Vegas structure. The level-0 chain is the sorted sequence regardless of the coins; randomness affects only the runtime (the express-lane heights), never the answer.

</details>

## Sources & Verify

- **Pugh** (1990), "Skip Lists: A Probabilistic Alternative to Balanced Trees", *Comm. ACM* — the original, including the expected-`O(log n)` analysis and the `p = ½` level distribution.
- **Redis** documentation (sorted sets / `ZRANGEBYSCORE`) and **LevelDB** (memtable) — production skip lists; Java's `java.util.concurrent.ConcurrentSkipListMap`.
- **LeetCode** 1206 (Design Skiplist) is the canonical drill; the `true`/`false`/`true` membership, the `2`-vs-`63` comparison counts (`p = ½` vs `p = 0`), and the `[7,9,12,17,19]` / `[21,25,26]` range queries above come from the runnable blocks — re-run to verify (the level structure is seeded for reproducibility; the answers are seed-independent).
