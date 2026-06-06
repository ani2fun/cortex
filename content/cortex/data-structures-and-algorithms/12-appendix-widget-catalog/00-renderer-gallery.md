---
title: Renderer Gallery — all 16 structure visuals
summary: One runnable block per trace-driven Visualise renderer (the viz-kind= family). Open a block's Visualise button to see that structure's bespoke visual — array cells, stack column, heap tree, fenwick staircase, skiplist grid, … — driven by a real captured trace. Use this page to eyeball every renderer in one place.
prereqs: []
---

# Renderer Gallery

Every structure below has a **Python** and a **Java** runnable block. Click each block's
**Visualise** button (▶ network icon) to open the modal and see that structure's bespoke
renderer drive off the real captured trace. The `viz-kind=` info-string attribute selects
the renderer; `viz-root=` names the variable to visualise; arrays default to the cell
renderer with no `viz-kind`.

> These are minimal shape-correct examples (modelled on the renderer test fixtures), not
> teaching content — the point is to *see* each visual. If a block renders flat / generic,
> its traced data shape doesn't match the renderer (see the per-block notes).

---

## Linear structures

### 1. Array — `array-1d` (default cell renderer, no `viz-kind`)

A plain list renders as a cell row with index labels; integer locals (`left`/`right`/`i`)
become coloured pointer carets. Two-pointer reverse:

```python run viz=array viz-root=arr
arr = [5, 2, 8, 1, 9, 3]
left, right = 0, len(arr) - 1
while left < right:
    arr[left], arr[right] = arr[right], arr[left]
    left += 1
    right -= 1
```

```java run viz=array viz-root=arr
public class Main {
    public static void main(String[] args) {
        int[] arr = {5, 2, 8, 1, 9, 3};
        int left = 0, right = arr.length - 1;
        while (left < right) {
            int t = arr[left]; arr[left] = arr[right]; arr[right] = t;
            left++; right--;
        }
    }
}
```

### 1b. 2D array / matrix — `viz=array-2d`

A list-of-lists (Python) or `int[][]` (Java) tagged `viz=array-2d` renders as a matrix grid:
boxed cells with a column-index header and per-row labels. Here a 3×4 grid is pre-filled with
zeros, then each cell is assigned `r*4 + c`; the just-written cell rings green.

```python run viz=array-2d viz-root=grid
grid = [[0] * 4 for _ in range(3)]
for r in range(3):
    for c in range(4):
        grid[r][c] = r * 4 + c
```

```java run viz=array-2d viz-root=grid
public class Main {
    public static void main(String[] args) {
        int[][] grid = new int[3][4];
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 4; c++)
                grid[r][c] = r * 4 + c;
    }
}
```

### 2. Stack — `viz-kind=stack`

A list used as a stack: cells stacked bottom→top with a `top` pointer.

```python run viz=array viz-root=stack viz-kind=stack
stack = []
for x in [3, 7, 1, 9, 5]:
    stack.append(x)
stack.pop()
stack.pop()
```

```java run viz=array viz-root=stack viz-kind=stack
import java.util.*;
public class Main {
    public static void main(String[] args) {
        List<Integer> stack = new ArrayList<>();
        for (int x : new int[]{3, 7, 1, 9, 5}) stack.add(x);   // push
        stack.remove(stack.size() - 1);                        // pop
        stack.remove(stack.size() - 1);
    }
}
```

### 3. Queue — `viz-kind=queue`

FIFO row with head (front) and tail (back) callouts.

```python run viz=array viz-root=queue viz-kind=queue
queue = []
for x in [10, 20, 30, 40]:
    queue.append(x)   # enqueue at the back
queue.pop(0)          # dequeue from the front
queue.pop(0)
```

```java run viz=array viz-root=queue viz-kind=queue
import java.util.*;
public class Main {
    public static void main(String[] args) {
        List<Integer> queue = new ArrayList<>();
        for (int x : new int[]{10, 20, 30, 40}) queue.add(x);   // enqueue
        queue.remove(0);                                        // dequeue
        queue.remove(0);
    }
}
```

### 4. Deque — `viz-kind=deque`

Double-ended: both `addFirst` and `addLast` ends are active.

```python run viz=array viz-root=dq viz-kind=deque
dq = []
dq.append(5)      # back
dq.insert(0, 3)   # front
dq.append(8)      # back
dq.insert(0, 1)   # front
```

```java run viz=array viz-root=dq viz-kind=deque
import java.util.*;
public class Main {
    public static void main(String[] args) {
        List<Integer> dq = new ArrayList<>();
        dq.add(5);        // back
        dq.add(0, 3);     // front
        dq.add(8);        // back
        dq.add(0, 1);     // front
    }
}
```

### 5. Singly linked list — `viz-kind=list-single`

A `next` chain ending in a `∅` null sentinel; `head`/`cur` show as carets.

```python run viz=linked-list viz-root=head viz-kind=list-single
class Node:
    def __init__(self, val):
        self.val = val
        self.next = None

head = Node(1)
head.next = Node(2)
head.next.next = Node(3)
head.next.next.next = Node(4)

cur = head
while cur:
    cur = cur.next
```

```java run viz=linked-list viz-root=head viz-kind=list-single
public class Main {
    static class Node { int val; Node next; Node(int v) { val = v; } }
    public static void main(String[] args) {
        Node head = new Node(1);
        head.next = new Node(2);
        head.next.next = new Node(3);
        head.next.next.next = new Node(4);
        Node cur = head;
        while (cur != null) cur = cur.next;
    }
}
```

### 6. Doubly linked list — `viz-kind=list-double`

Same chain with `prev` back-pointers.

```python run viz=linked-list viz-root=head viz-kind=list-double
class Node:
    def __init__(self, val):
        self.val = val
        self.next = None
        self.prev = None

head = Node(1)
b = Node(2); head.next = b; b.prev = head
c = Node(3); b.next = c; c.prev = b

cur = head
while cur:
    cur = cur.next
```

```java run viz=linked-list viz-root=head viz-kind=list-double
public class Main {
    static class Node { int val; Node next, prev; Node(int v) { val = v; } }
    public static void main(String[] args) {
        Node head = new Node(1);
        Node b = new Node(2); head.next = b; b.prev = head;
        Node c = new Node(3); b.next = c; c.prev = b;
        Node cur = head;
        while (cur != null) cur = cur.next;
    }
}
```

---

## Trees & heaps

### 7. Binary tree / BST — generic tree (no `viz-kind`)

`TreeNode(val, left, right)` instances render as a node-link tree via the generic layout.

```python run viz=binary-tree viz-root=root
class TreeNode:
    def __init__(self, val):
        self.val = val
        self.left = None
        self.right = None

def insert(root, val):
    if root is None:
        return TreeNode(val)
    if val < root.val:
        root.left = insert(root.left, val)
    else:
        root.right = insert(root.right, val)
    return root

root = None
for v in [5, 3, 8, 1, 4, 7, 9]:
    root = insert(root, v)
```

```java run viz=binary-tree viz-root=root
public class Main {
    static class TreeNode { int val; TreeNode left, right; TreeNode(int v) { val = v; } }
    static TreeNode insert(TreeNode root, int val) {
        if (root == null) return new TreeNode(val);
        if (val < root.val) root.left = insert(root.left, val);
        else root.right = insert(root.right, val);
        return root;
    }
    public static void main(String[] args) {
        TreeNode root = null;
        for (int v : new int[]{5, 3, 8, 1, 4, 7, 9}) root = insert(root, v);
    }
}
```

### 8. Heap — `viz-kind=heap`

An **array-backed** binary heap (heapq on a plain list) renders as its implicit binary tree
(`children of i = 2i+1, 2i+2`). NB: a class/TreeNode-based heap will *not* render here — the
renderer needs the array.

```python run viz=array viz-root=heap viz-kind=heap
import heapq
heap = []
for x in [5, 3, 8, 1, 9, 2, 7]:
    heapq.heappush(heap, x)
```

```java run viz=array viz-root=heap viz-kind=heap
import java.util.*;
public class Main {
    public static void main(String[] args) {
        List<Integer> heap = new ArrayList<>();   // array-backed min-heap
        for (int x : new int[]{5, 3, 8, 1, 9, 2, 7}) {
            heap.add(x);
            int i = heap.size() - 1;
            while (i > 0 && heap.get((i - 1) / 2) > heap.get(i)) {
                Collections.swap(heap, i, (i - 1) / 2);
                i = (i - 1) / 2;
            }
        }
    }
}
```

### 9. Segment tree — `viz-kind=segment-tree`

`SegNode(lo, hi, value, left, right)` renders as the range-bar overlay — each node a bar over
the array slice `[lo,hi]` it covers, on its tree level. The recursive `build` assigns `root`
only on its last line — the partial subtrees live in recursion frames the tracer doesn't
follow — so construction collapses to a single step; the trailing descent then walks a `cur`
pointer down to the leaf covering index 2, animating the finished tree across several steps.

```python run viz=binary-tree viz-root=root viz-kind=segment-tree
class SegNode:
    def __init__(self, lo, hi, value, left=None, right=None):
        self.lo = lo
        self.hi = hi
        self.value = value
        self.left = left
        self.right = right

def build(arr, lo, hi):
    if lo == hi:
        return SegNode(lo, hi, arr[lo])
    mid = (lo + hi) // 2
    l = build(arr, lo, mid)
    r = build(arr, mid + 1, hi)
    return SegNode(lo, hi, l.value + r.value, l, r)

root = build([3, 1, 4, 2], 0, 3)

# The tree is built; descend from the root to the leaf covering index 2 so the
# finished tree animates with a moving `cur` pointer across several steps.
cur = root
while cur.lo != cur.hi:
    mid = (cur.lo + cur.hi) // 2
    cur = cur.left if 2 <= mid else cur.right
```

```java run viz=binary-tree viz-root=root viz-kind=segment-tree
public class Main {
    static class SegNode {
        int lo, hi, value;
        SegNode left, right;
        SegNode(int lo, int hi, int value) { this.lo = lo; this.hi = hi; this.value = value; }
    }
    static SegNode build(int[] arr, int lo, int hi) {
        if (lo == hi) return new SegNode(lo, hi, arr[lo]);
        int mid = (lo + hi) / 2;
        SegNode l = build(arr, lo, mid), r = build(arr, mid + 1, hi);
        SegNode node = new SegNode(lo, hi, l.value + r.value);
        node.left = l; node.right = r;
        return node;
    }
    public static void main(String[] args) {
        SegNode root = build(new int[]{3, 1, 4, 2}, 0, 3);
        // The tree is built; descend to the leaf covering index 2 so the finished
        // tree animates with a moving `cur` pointer across several steps.
        SegNode cur = root;
        while (cur.lo != cur.hi) {
            int mid = (cur.lo + cur.hi) / 2;
            cur = (2 <= mid) ? cur.left : cur.right;
        }
    }
}
```

### 10. Fenwick tree / BIT — `viz-kind=fenwick`

The **1-indexed BIT array** (`tree[0]` unused) renders as the responsibility staircase — each
cell a bar over the half-open range `(i − lowbit(i), i]` it owns.

```python run viz=array viz-root=tree viz-kind=fenwick
n = 8
tree = [0] * (n + 1)   # 1-indexed; tree[0] is the unused sentinel

def update(i, delta):
    while i <= n:
        tree[i] += delta
        i += i & (-i)   # add lowbit

for i in range(1, n + 1):
    update(i, i)        # build a BIT over [1, 2, …, 8]
```

```java run viz=array viz-root=tree viz-kind=fenwick
public class Main {
    public static void main(String[] args) {
        int n = 8;
        int[] tree = new int[n + 1];   // 1-indexed; tree[0] unused
        for (int i = 1; i <= n; i++) {
            int j = i;
            while (j <= n) { tree[j] += i; j += j & (-j); }
        }
    }
}
```

---

## Maps, sets & advanced

### 11. Hash map — `viz-kind=hashmap`

Separate chaining: a `dict` of `bucket-index → list of Entry` renders as buckets each with a
`key: value` chain.

```python run viz=hashmap viz-root=table viz-kind=hashmap
class Entry:
    def __init__(self, key, value):
        self.key = key
        self.value = value

table = {}   # bucket index -> list of Entry (the chain)

def put(key, value):
    i = len(key) % 4   # deterministic toy hash
    table.setdefault(i, []).append(Entry(key, value))

for k, v in [("apple", 1), ("grape", 2), ("fig", 3), ("kiwi", 4)]:
    put(k, v)
```

```java run viz=hashmap viz-root=table viz-kind=hashmap
import java.util.*;
public class Main {
    static class Entry { String key; int value; Entry(String k, int v) { key = k; value = v; } }
    public static void main(String[] args) {
        Map<Integer, List<Entry>> table = new HashMap<>();   // bucket index -> chain
        String[] keys = {"apple", "grape", "fig", "kiwi"};
        int[] vals = {1, 2, 3, 4};
        for (int i = 0; i < keys.length; i++) {
            int b = keys[i].length() % 4;                    // toy hash
            table.computeIfAbsent(b, x -> new ArrayList<>()).add(new Entry(keys[i], vals[i]));
        }
    }
}
```

### 12. Trie — `viz-kind=trie`

`TrieNode(children: dict, is_end)` renders as a prefix tree; `is_end` nodes get a terminal
double-ring. Inserts "cat", "car", "do".

```python run viz=trie viz-root=root viz-kind=trie
class TrieNode:
    def __init__(self):
        self.children = {}
        self.is_end = False

def insert(root, word):
    node = root
    for ch in word:
        if ch not in node.children:
            node.children[ch] = TrieNode()
        node = node.children[ch]
    node.is_end = True

root = TrieNode()
for word in ["cat", "car", "do"]:
    insert(root, word)
```

```java run viz=trie viz-root=root viz-kind=trie
import java.util.*;
public class Main {
    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd = false;
    }
    static void insert(TrieNode root, String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            node.children.putIfAbsent(ch, new TrieNode());
            node = node.children.get(ch);
        }
        node.isEnd = true;
    }
    public static void main(String[] args) {
        TrieNode root = new TrieNode();
        for (String w : new String[]{"cat", "car", "do"}) insert(root, w);
    }
}
```

### 13. Union-Find / DSU — `viz-kind=union-find`

A `parent` array (parent[i] == i marks a root) renders as a forest with parent arcs.

```python run viz=array viz-root=parent viz-kind=union-find
parent = [0, 1, 2, 3, 4, 5]

def find(x):
    while parent[x] != x:
        x = parent[x]
    return x

def union(a, b):
    parent[find(a)] = find(b)

union(0, 1)
union(2, 3)
union(1, 3)
union(4, 5)
```

```java run viz=array viz-root=parent viz-kind=union-find
public class Main {
    static int[] parent;
    static int find(int x) { while (parent[x] != x) x = parent[x]; return x; }
    static void union(int a, int b) { parent[find(a)] = find(b); }
    public static void main(String[] args) {
        parent = new int[]{0, 1, 2, 3, 4, 5};
        union(0, 1); union(2, 3); union(1, 3); union(4, 5);
    }
}
```

### 14. Graph — `viz-kind=graph`

`Node(id, neighbors)` adjacency renders as a node-link graph; a BFS walks it.

```python run viz=graph viz-root=start viz-kind=graph
from collections import deque

class Node:
    def __init__(self, name):
        self.id = name            # GraphRenderer relabels each node from its `id` field
        self.neighbors = []

start = Node("A"); b = Node("B"); c = Node("C"); d = Node("D")
start.neighbors = [b, c]
b.neighbors = [d]
c.neighbors = [d]

seen = set()
queue = deque([start])
while queue:
    node = queue.popleft()
    if node.id in seen:
        continue
    seen.add(node.id)
    for nb in node.neighbors:
        queue.append(nb)
```

```java run viz=graph viz-root=start viz-kind=graph
import java.util.*;
public class Main {
    static class Node { String id; List<Node> neighbors = new ArrayList<>(); Node(String id) { this.id = id; } }
    public static void main(String[] args) {
        Node start = new Node("A"), b = new Node("B"), c = new Node("C"), d = new Node("D");
        start.neighbors = Arrays.asList(b, c);
        b.neighbors = Arrays.asList(d);
        c.neighbors = Arrays.asList(d);
        Set<String> seen = new HashSet<>();
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (!seen.add(node.id)) continue;
            for (Node nb : node.neighbors) queue.add(nb);
        }
    }
}
```

### 15. Skip list — `viz-kind=skiplist`

A level-0 `next` chain of `SkipNode(value, level)` renders as the multi-level grid (one row
per level, columns by key). `level` = the top express lane each node reaches.

```python run viz=linked-list viz-root=head viz-kind=skiplist
class SkipNode:
    def __init__(self, value, level):
        self.value = value
        self.level = level
        self.next = None

head = SkipNode(3, 0)
n7 = SkipNode(7, 2);  head.next = n7
n12 = SkipNode(12, 0); n7.next = n12
n19 = SkipNode(19, 1); n12.next = n19
n25 = SkipNode(25, 2); n19.next = n25
n31 = SkipNode(31, 0); n25.next = n31
```

```java run viz=linked-list viz-root=head viz-kind=skiplist
public class Main {
    static class SkipNode { int value, level; SkipNode next; SkipNode(int v, int l) { value = v; level = l; } }
    public static void main(String[] args) {
        SkipNode head = new SkipNode(3, 0);
        SkipNode n7 = new SkipNode(7, 2);  head.next = n7;
        SkipNode n12 = new SkipNode(12, 0); n7.next = n12;
        SkipNode n19 = new SkipNode(19, 1); n12.next = n19;
        SkipNode n25 = new SkipNode(25, 2); n19.next = n25;
        SkipNode n31 = new SkipNode(31, 0); n25.next = n31;
    }
}
```

### 16. Bitset — `viz-kind=bitset`

A list of 0/1 renders as a bit row — set bits filled, clear bits muted, with a popcount.

```python run viz=array viz-root=bits viz-kind=bitset
bits = [0] * 8
for i in [1, 3, 4, 6]:
    bits[i] = 1   # set
bits[4] = 0       # clear
```

```java run viz=array viz-root=bits viz-kind=bitset
public class Main {
    public static void main(String[] args) {
        int[] bits = new int[8];
        for (int i : new int[]{1, 3, 4, 6}) bits[i] = 1;   // set
        bits[4] = 0;                                       // clear
    }
}
```
