---
title: "Design a Hash Map"
summary: "Compose a hash map with a second structure so two dimensions are O(1) at once — design an LRU cache (hash map + doubly-linked list) that does get, put, and eviction all in O(1)."
prereqs:
  - 02-linear-structures/07-hash-table/02-separate-chaining
  - 02-linear-structures/07-hash-table/03-linear-probing
difficulty: hard
kind: problem
topics: [hash-table, design]
---

# Design a Hash Map

## The Problem

Some problems need a hash table to **collaborate** with another structure. The hash map gives `O(1)` lookup by key; pair it with a second structure — a linked list or an array — for `O(1)` on *another* dimension (recency, position) and operations that look impossible become routine. The canonical example interviewers love and production systems live by is the **LRU cache**.

Implement an LRU (Least-Recently-Used) cache, every operation in `O(1)`:

> - `LRUCache(capacity)` — initialise the cache with the given capacity.
> - `get(key)` — return the value if the key exists, else `-1`. Accessing a key marks it **most** recently used.
> - `put(key, value)` — insert or update the mapping. If inserting exceeds `capacity`, evict the **least** recently used key first.

```
Input:
  capacity = 2
  put(1, 10), put(2, 20)
  get(1)            → 10   (1 is now most-recently-used; 2 becomes the LRU)
  put(3, 30)               (capacity exceeded → evict 2, the LRU)
  get(1), get(2), get(3)   → 10, -1, 30

Output:
  [10, 10, -1, 30]
```

```quiz
{
  "prompt": "Capacity is 2. You run put(1,10), put(2,20), get(1), then put(3,30). Which key gets evicted?",
  "input": "cap=2: put(1,10), put(2,20), get(1), put(3,30)",
  "options": ["2", "1", "3", "Nothing is evicted"],
  "answer": "2"
}
```

## Constraints

- No built-in LRU containers (no `LinkedHashMap` / `OrderedDict` shortcut).
- `get` and `put` must each run in **O(1)** — the naïve "scan a list for the LRU" is `O(n)` per `put` and breaks the contract.

The workbench drives your cache through a fixed sequence: it applies the `puts` in order, does one `get(promote)` (which marks that key most-recently-used), applies one more `put` from `evictor` (which may evict), then reads each key in `probes`. Implement `get` and `put` so every printed line matches.

```python run viz=linked-list viz-root=head
import ast

class Node:
    def __init__(self, key, val):
        self.key, self.value = key, val
        self.prev = self.next = None

class LRUCache:
    def __init__(self, capacity):
        self.capacity = capacity
        self.map = {}                       # key -> node
        self.head = Node(-1, -1)            # sentinel: most-recently-used end
        self.tail = Node(-1, -1)            # sentinel: least-recently-used end
        self.head.next, self.tail.prev = self.tail, self.head

    def _remove(self, node):
        node.prev.next, node.next.prev = node.next, node.prev

    def _add_front(self, node):
        node.prev, node.next = self.head, self.head.next
        self.head.next.prev = node
        self.head.next = node

    def get(self, key):
        # Your code goes here — if key is present, splice its node to the
        # front (most-recently-used) and return its value, else return -1.
        return -1

    def put(self, key, value):
        # Your code goes here — update-and-promote if key exists; otherwise
        # evict the node before tail when full, then insert at the front.
        pass

cache = LRUCache(int(input()))
puts = ast.literal_eval(input())            # [[key, value], ...] applied in order
promote = int(input())                      # get(this) — marks it most-recently-used
evictor = ast.literal_eval(input())         # one [key, value] put that may evict
probes = ast.literal_eval(input())          # keys to read at the end, in order

for k, v in puts:
    cache.put(k, v)
print(cache.get(promote))
cache.put(evictor[0], evictor[1])
for k in probes:
    print(cache.get(k))
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
  static class Node {
    int key, val;
    Node prev, next;
    Node(int key, int val) { this.key = key; this.val = val; }
  }

  static class LRUCache {
    private final int capacity;
    private final Map<Integer, Node> map = new HashMap<>();   // key -> node
    private final Node head = new Node(-1, -1);               // sentinel: MRU end
    private final Node tail = new Node(-1, -1);               // sentinel: LRU end

    LRUCache(int capacity) {
      this.capacity = capacity;
      head.next = tail;
      tail.prev = head;
    }

    private void remove(Node node) {
      node.prev.next = node.next;
      node.next.prev = node.prev;
    }

    private void addFront(Node node) {
      node.prev = head;
      node.next = head.next;
      head.next.prev = node;
      head.next = node;
    }

    int get(int key) {
      // Your code goes here — if key is present, splice its node to the
      // front (most-recently-used) and return its value, else return -1.
      return -1;
    }

    void put(int key, int value) {
      // Your code goes here — update-and-promote if key exists; otherwise
      // evict the node before tail when full, then insert at the front.
    }
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    LRUCache cache = new LRUCache(Integer.parseInt(sc.nextLine().trim()));
    int[][] puts = parseIntMatrix(sc.nextLine());   // [[key, value], ...] in order
    int promote = Integer.parseInt(sc.nextLine().trim());
    int[] evictor = parseIntArray(sc.nextLine());   // one [key, value] put
    int[] probes = parseIntArray(sc.nextLine());    // keys to read at the end

    for (int[] p : puts) cache.put(p[0], p[1]);
    System.out.println(cache.get(promote));
    cache.put(evictor[0], evictor[1]);
    for (int k : probes) System.out.println(cache.get(k));
  }

  // "[1, 2]" → {1, 2}
  static int[] parseIntArray(String line) {
    String inner = line.replaceAll("[\\[\\]\\s]", "");
    if (inner.isEmpty()) return new int[0];
    String[] parts = inner.split(",");
    int[] out = new int[parts.length];
    for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
    return out;
  }

  // "[[1, 10], [2, 20]]" → {{1, 10}, {2, 20}}
  static int[][] parseIntMatrix(String line) {
    String s = line.trim();
    if (s.startsWith("[")) s = s.substring(1);
    if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
    s = s.trim();
    if (s.isEmpty()) return new int[0][];
    String[] rows = s.split("\\]\\s*,\\s*\\[");
    int[][] out = new int[rows.length][];
    for (int i = 0; i < rows.length; i++) out[i] = parseIntArray(rows[i]);
    return out;
  }
}
```

```testcases
{
  "args": [
    { "id": "capacity", "label": "capacity", "type": "int", "placeholder": "2" },
    { "id": "puts", "label": "puts", "type": "int[][]", "placeholder": "[[1, 10], [2, 20]]" },
    { "id": "promote", "label": "promote", "type": "int", "placeholder": "1" },
    { "id": "evictor", "label": "evictor", "type": "int[]", "placeholder": "[3, 30]" },
    { "id": "probes", "label": "probes", "type": "int[]", "placeholder": "[1, 2, 3]" }
  ],
  "cases": [
    { "args": { "capacity": "2", "puts": "[[1, 10], [2, 20]]", "promote": "1", "evictor": "[3, 30]", "probes": "[1, 2, 3]" }, "expected": "10\n10\n-1\n30" },
    { "args": { "capacity": "2", "puts": "[[1, 10], [2, 20]]", "promote": "2", "evictor": "[3, 30]", "probes": "[1, 2, 3]" }, "expected": "20\n-1\n20\n30" },
    { "args": { "capacity": "1", "puts": "[[5, 50]]", "promote": "5", "evictor": "[6, 60]", "probes": "[5, 6]" }, "expected": "50\n-1\n60" },
    { "args": { "capacity": "3", "puts": "[[1, 1], [2, 2], [3, 3]]", "promote": "2", "evictor": "[4, 4]", "probes": "[1, 2, 3, 4]" }, "expected": "2\n-1\n2\n3\n4" },
    { "args": { "capacity": "2", "puts": "[[1, 1], [2, 2], [1, 10]]", "promote": "1", "evictor": "[3, 3]", "probes": "[1, 2, 3]" }, "expected": "10\n10\n-1\n3" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The two requirements pull in opposite directions:

- **`O(1)` get/put by key** demands a **hash map**.
- **`O(1)` "find the least-recently-used entry" + `O(1)` "promote an entry to most-recently-used"** demands an *ordered* structure where inserting at the front and deleting *any* node are both `O(1)`.

The classic answer is **doubly-linked list + hash map**. The list holds entries in MRU→LRU order: front = most recently used, back = least. The map stores `key → the node`. Both structures hold the *same* nodes — the list owns them, the map references them.

```d2
direction: right

map: hash map {
  m1: "1 -> *"
  m2: "3 -> *"
}

list: "doubly-linked list (front = MRU, back = LRU)" {
  direction: right
  h: "[head]"
  n1: "(1, 10)"
  n3: "(3, 30)"
  t: "[tail]"
  h -> n1
  n1 -> n3
  n3 -> n1
  n3 -> t
}

map.m1 -> list.n1 {style.stroke-dash: 3}
map.m2 -> list.n3 {style.stroke-dash: 3}
```

<p align="center"><strong>The LRU cache as hash-map-plus-doubly-linked-list — the map gives O(1) lookup of any key's node; the list keeps recency order with O(1) splice. Both point to the <em>same</em> nodes.</strong></p>

Each operation is then `O(1)`: `get` looks the node up via the map and splices it to the front; `put` either updates-and-promotes an existing node, or (when full) unlinks the node just before `tail` — the LRU — drops its key from the map, and inserts a fresh node at the front. The list **must** be *doubly* linked: removing an arbitrary node by reference needs its predecessor, and only a `prev` pointer gives that in `O(1)` (a singly-linked list would force an `O(n)` walk to find it). Sentinel `head`/`tail` nodes remove the empty-list and boundary special cases.

> **The same trick, a different partner — RandomisedSet.** Want `insert`, `remove`, *and* `getRandom` all `O(1)`? Compose a hash map with a **dynamic array** instead: the array stores the values (so `getRandom` is `array[random index]`), and the map stores `value → its index`. The clever bit is `remove`: look up the value's index, **swap it with the last array element**, pop the tail, and fix the swapped element's index in the map — `O(1)`, no shift. It works precisely because a *set* is unordered, so rearranging the array is harmless.

```d2
direction: right

before: "before remove(2)" {
  a1: "arr: [2, 4, 6]"
  m1: "map: {2->0, 4->1, 6->2}"
}

swap: "swap arr[0] with arr[last]" {
  a2: "arr: [6, 4, 2]" {style.fill: "#fef9c3"; style.stroke: "#d97706"}
  m2: "map: {2->0, 4->1, 6->0}" {style.fill: "#fef9c3"; style.stroke: "#d97706"}
}

after: "pop tail; delete 2 from map" {
  a3: "arr: [6, 4]" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  m3: "map: {4->1, 6->0}" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
}

before -> swap -> after
```

<p align="center"><strong>RandomisedSet remove — swap target with tail (O(1)), pop tail (O(1)), update the swapped element's index in the map (O(1)). Same composition idea, a different partner structure.</strong></p>

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

Fill in `get` and `put`; the sentinel-based `_remove`/`_add_front` helpers do all the pointer surgery, so each method is three or four `O(1)` steps.

```python solution time=O(1) space=O(n)
import ast

class Node:
    def __init__(self, key, val):
        self.key, self.value = key, val
        self.prev = self.next = None

class LRUCache:
    def __init__(self, capacity):
        self.capacity = capacity
        self.map = {}                       # key -> node
        self.head = Node(-1, -1)            # sentinel: most-recently-used end
        self.tail = Node(-1, -1)            # sentinel: least-recently-used end
        self.head.next, self.tail.prev = self.tail, self.head

    def _remove(self, node):
        node.prev.next, node.next.prev = node.next, node.prev

    def _add_front(self, node):
        node.prev, node.next = self.head, self.head.next
        self.head.next.prev = node
        self.head.next = node

    def get(self, key):
        if key in self.map:
            node = self.map[key]
            self._remove(node)              # unlink from its current spot
            self._add_front(node)           # re-add as most-recently-used
            return node.value
        return -1

    def put(self, key, value):
        if key in self.map:                 # exists: update value, promote
            node = self.map[key]
            node.value = value
            self._remove(node)
            self._add_front(node)
        else:
            if len(self.map) == self.capacity:   # full: evict the LRU (before tail)
                lru = self.tail.prev
                self._remove(lru)
                del self.map[lru.key]
            node = Node(key, value)
            self._add_front(node)
            self.map[key] = node

cache = LRUCache(int(input()))
puts = ast.literal_eval(input())            # [[key, value], ...] applied in order
promote = int(input())                      # get(this) — marks it most-recently-used
evictor = ast.literal_eval(input())         # one [key, value] put that may evict
probes = ast.literal_eval(input())          # keys to read at the end, in order

for k, v in puts:
    cache.put(k, v)
print(cache.get(promote))
cache.put(evictor[0], evictor[1])
for k in probes:
    print(cache.get(k))
```

```java solution
import java.util.*;

public class Main {
  static class Node {
    int key, val;
    Node prev, next;
    Node(int key, int val) { this.key = key; this.val = val; }
  }

  static class LRUCache {
    private final int capacity;
    private final Map<Integer, Node> map = new HashMap<>();   // key -> node
    private final Node head = new Node(-1, -1);               // sentinel: MRU end
    private final Node tail = new Node(-1, -1);               // sentinel: LRU end

    LRUCache(int capacity) {
      this.capacity = capacity;
      head.next = tail;
      tail.prev = head;
    }

    private void remove(Node node) {
      node.prev.next = node.next;
      node.next.prev = node.prev;
    }

    private void addFront(Node node) {
      node.prev = head;
      node.next = head.next;
      head.next.prev = node;
      head.next = node;
    }

    int get(int key) {
      if (map.containsKey(key)) {
        Node node = map.get(key);
        remove(node);                       // unlink from its current spot
        addFront(node);                     // re-add as most-recently-used
        return node.val;
      }
      return -1;
    }

    void put(int key, int value) {
      if (map.containsKey(key)) {           // exists: update value, promote
        Node node = map.get(key);
        node.val = value;
        remove(node);
        addFront(node);
      } else {
        if (map.size() == capacity) {       // full: evict the LRU (before tail)
          Node lru = tail.prev;
          remove(lru);
          map.remove(lru.key);
        }
        Node node = new Node(key, value);
        addFront(node);
        map.put(key, node);
      }
    }
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    LRUCache cache = new LRUCache(Integer.parseInt(sc.nextLine().trim()));
    int[][] puts = parseIntMatrix(sc.nextLine());
    int promote = Integer.parseInt(sc.nextLine().trim());
    int[] evictor = parseIntArray(sc.nextLine());
    int[] probes = parseIntArray(sc.nextLine());

    for (int[] p : puts) cache.put(p[0], p[1]);
    System.out.println(cache.get(promote));
    cache.put(evictor[0], evictor[1]);
    for (int k : probes) System.out.println(cache.get(k));
  }

  static int[] parseIntArray(String line) {
    String inner = line.replaceAll("[\\[\\]\\s]", "");
    if (inner.isEmpty()) return new int[0];
    String[] parts = inner.split(",");
    int[] out = new int[parts.length];
    for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
    return out;
  }

  static int[][] parseIntMatrix(String line) {
    String s = line.trim();
    if (s.startsWith("[")) s = s.substring(1);
    if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
    s = s.trim();
    if (s.isEmpty()) return new int[0][];
    String[] rows = s.split("\\]\\s*,\\s*\\[");
    int[][] out = new int[rows.length][];
    for (int i = 0; i < rows.length; i++) out[i] = parseIntArray(rows[i]);
    return out;
  }
}
```

### Dry Run — the canonical example sequence

```
capacity = 2
Op          | list (MRU→LRU) | map keys | Return
------------|----------------|----------|-------
put(1, 10)  | [1]            | {1}      | —
put(2, 20)  | [2, 1]         | {1,2}    | —
get(1)      | [1, 2]         | {1,2}    | 10     ← 1 promoted; 2 is now the LRU
put(3, 30)  | [3, 1]         | {1,3}    | —      ← full → evict 2 (the LRU)
get(1)      | [1, 3]         | {1,3}    | 10
get(2)      | [1, 3]         | {1,3}    | -1     ← evicted earlier
get(3)      | [3, 1]         | {1,3}    | 30
```

### Complexity Analysis

| Operation | Time | Space | Notes |
|---|---|---|---|
| `LRUCache(cap)` | O(1) | O(1) | wire the two sentinels |
| `get(key)` | O(1) | — | one map lookup + one splice |
| `put(key, value)` | O(1) | — | at most one eviction + one insert |
| total | — | O(n) | one node + one map entry per stored key (≤ capacity) |

### Edge Cases

| Case | Example | Expected behaviour |
|---|---|---|
| Capacity 1 | `cap=1`, put then put | every new key evicts the previous one |
| `get` changes the eviction victim | promote a key, then overflow | the *promoted* key survives; the other is evicted |
| Update existing key | `put` a key already present | value updated, key promoted, **no** eviction |
| Miss after eviction | `get` an evicted key | returns `-1` |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

1. **Hash map + doubly-linked list = O(1) lookup *and* an O(1) recency order.** The map points to nodes; the list owns them and keeps MRU→LRU order. Caches, schedulers, and any "fast key access with a maintained order" ride on this pairing.
2. **Hash map + dynamic array = O(1) lookup *and* O(1) random access by position.** The array stores values, the map stores indices, and swap-with-last gives O(1) deletion as long as order doesn't matter (RandomisedSet).
3. **The deeper move is composability.** A hash table is O(1) on *one* dimension (by key); to get O(1) on another (recency, position, frequency), pair it with a structure specialised for that dimension, collaborating via shared node references or indices — the same idea behind `LinkedHashMap`, `OrderedDict`, LFU caches, indexed priority queues, and every database `INDEX`.

> **Transfer Challenge:** Make `getRandom` O(1) on top of the LRU design — what second structure do you add, and why does swap-with-last keep `remove` O(1)? (A parallel dynamic array of the keys; the set is order-agnostic, so swapping the removed key with the last one and popping avoids the O(n) shift.)

</details>
