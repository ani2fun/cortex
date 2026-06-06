---
title: "Design a Hash Map"
summary: "Design data structures that combine a hash map with another structure to support O(1) LRU eviction and O(1) random retrieval simultaneously."
prereqs:
  - 02-linear-structures/07-hash-table/02-separate-chaining
  - 02-linear-structures/07-hash-table/03-linear-probing
difficulty: hard
---

Some problems need a hash table to **collaborate** with another structure. The hash map gives `O(1)` lookup by key; pair it with a second structure (a linked list or an array) for `O(1)` on *another* dimension — recency or position — and operations that look impossible become routine. This lesson designs two such structures interviewers love and production systems live by:

- **LRU Cache** — `get` and `put` in O(1) with automatic eviction of the least-recently-used entry. Powers every page cache, browser cache, and CDN. (Hash map + doubly-linked list.)
- **RandomisedSet** — `insert`, `remove`, *and* `getRandom` in O(1). Powers fair shuffles, A/B bucketers, game-state samplers. (Hash map + dynamic array.)

## Design an LRU Cache

### Problem Statement

Implement an LRU (Least-Recently-Used) cache:

> -   **`LRUCache(int capacity)`** — Initialise the cache with the given capacity.
> -   **`get(int key)`** — Return the value if the key exists, else `-1`. Accessing a key marks it as the most recently used.
> -   **`put(int key, int value)`** — Insert or update the mapping. If inserting causes the size to exceed `capacity`, evict the least recently used key.

```d2
cons: Constraints {
  c1: "No built-in LRU libraries"
  c2: "get and put must each run in amortised O(1)"
}
```

<p align="center"><strong>Constraints — both operations have to be amortised O(1). The naïve "scan a list for the LRU element" is O(N) per put and breaks the contract.</strong></p>

> **Example:**
>
> -   **Input:** `[LRUCache, put, put, get, put, get, get]`, `[[2], [1, 10], [2, 20], [1], [3, 30], [1], [2]]`
>
> -   **Output:** `[null, null, null, 10, null, 10, -1]`
>
> | Operation | Cache state (most-recent first) | Result |
> |---|---|---|
> | `LRUCache(2)` | `[]` | `null` |
> | `put(1, 10)` | `[(1,10)]` | `null` |
> | `put(2, 20)` | `[(2,20), (1,10)]` | `null` |
> | `get(1)` | `[(1,10), (2,20)]` (1 promoted to front) | `10` |
> | `put(3, 30)` | `[(3,30), (1,10)]` (2 evicted as LRU) | `null` |
> | `get(1)` | `[(1,10), (3,30)]` | `10` |
> | `get(2)` | unchanged (2 was evicted) | `-1` |

<details>
<summary><h2>Approach</h2></summary>


The two requirements pull in opposite directions:
- **O(1) get/put by key** demands a **hash map**.
- **O(1) "find the least-recently-used element" + O(1) "promote an element to most-recently-used"** demands an **ordered** structure where insertion at the front and deletion of any node are both O(1).

The classic answer: **doubly-linked list + hash map**. The list stores the entries in MRU-to-LRU order: front of list = most recently used, back of list = least recently used. The hash map stores `key → pointer to that key's node`. Both structures hold the *same* nodes (the list owns them; the map references them).

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

<p align="center"><strong>The LRU cache as a hash-map-plus-doubly-linked-list — the map gives O(1) lookup of any key's node; the list keeps the recency order with O(1) splice. Both structures point to the <em>same</em> nodes.</strong></p>

The four operations needed:

1. **`get(key)`** — look up the node via the map; *splice it to the front* of the list; return its value.
2. **`put(key, value)` — key exists** — look up node; update value; splice to front.
3. **`put(key, value)` — key new, capacity OK** — create node; insert at front; add to map.
4. **`put(key, value)` — key new, capacity exceeded** — remove the *back* node from the list; remove its key from the map; then insert as in case 3.

Every step above is O(1). The doubly-linked list is essential — a singly-linked list would make "remove arbitrary node by reference" O(N) (you'd need the predecessor).

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=linked-list viz-root=head
from typing import Dict, Optional

# Define a class for the nodes in the doubly linked list
class Node:
    def __init__(self, key: int, val: int) -> None:
        self.key: int = key
        self.value: int = val
        self.prev: Optional['Node'] = None
        self.next: Optional['Node'] = None

class LRUCache:
    def __init__(self, capacity: int) -> None:

        # Member variables for the cache capacity, the doubly linked
        # list, and the map of keys to nodes
        self.capacity: int = capacity
        self.key_address_map: Dict[int, Node] = {}

        # Sentinel head and tail nodes simplify insert/remove at boundaries
        self.head: Node = Node(-1, -1)  # most-recently-used end
        self.tail: Node = Node(-1, -1)  # least-recently-used end
        self.head.next = self.tail
        self.tail.prev = self.head

    def _remove(self, node: Node) -> None:
        node.prev.next = node.next
        node.next.prev = node.prev

    def _add_front(self, node: Node) -> None:
        node.next = self.head.next
        node.prev = self.head
        self.head.next.prev = node
        self.head.next = node

    def get(self, key: int) -> int:

        # Check if the key exists in the map
        if key in self.key_address_map:

            # Move the corresponding node to the front of the list
            node: Node = self.key_address_map[key]
            self._remove(node)
            self._add_front(node)

            # Return the value of the node
            return node.value

        # If the key does not exist, return -1
        return -1

    def put(self, key: int, value: int) -> None:

        # If the key already exists in the cache, update its value
        if key in self.key_address_map:

            # Get the node associated with the key
            node: Node = self.key_address_map[key]

            # Update the value of the node
            node.value = value

            # Move the corresponding node to the front of the list
            self._remove(node)
            self._add_front(node)

        # If the key does not exist in the cache, add it to the
        # front of the list.
        else:

            # If the cache is full, remove the least recently used node
            # from the back of the list
            if len(self.key_address_map) == self.capacity:

                # Remove the least recently used node (the node before tail)
                lru_node: Node = self.tail.prev
                self._remove(lru_node)

                # Remove the corresponding key from the map
                del self.key_address_map[lru_node.key]

            # Create a new Node for the key-value pair
            new_node = Node(key, value)

            # Add it to the front of the list
            self._add_front(new_node)

            # Map the key to the new node
            self.key_address_map[key] = new_node


# Example from the problem statement
c1 = LRUCache(2)
c1.put(1, 10)
c1.put(2, 20)
print(c1.get(1))                # 10
c1.put(3, 30)                   # evicts key 2
print(c1.get(1))                # 10
print(c1.get(2))                # -1 — evicted

# Edge cases
c2 = LRUCache(1)
print(c2.get(5))                # -1 — empty cache
c2.put(1, 100)
print(c2.get(1))                # 100
c2.put(2, 200)                  # evicts key 1
print(c2.get(1))                # -1 — evicted
print(c2.get(2))                # 200

c3 = LRUCache(3)
c3.put(1, 1)
c3.put(2, 2)
c3.put(3, 3)
c3.put(1, 10)                   # update existing key — moves to front
print(c3.get(1))                # 10
c3.put(4, 4)                    # evicts LRU which is key 2
print(c3.get(2))                # -1 — evicted
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {

    // Define a Node class for the doubly linked list
    static class Node {

        int key;
        int val;

        Node(int key, int val) {
            this.key = key;
            this.val = val;
        }
    }

    static class LRUCache {

        // Member variables for the cache capacity, the doubly linked
        // list, and the map of keys to nodes
        private int capacity;
        private LinkedList<Node> cache;
        private Map<Integer, Node> keyAddressMap;

        public LRUCache(int capacity) {
            this.capacity = capacity;
            this.cache = new LinkedList<>();
            this.keyAddressMap = new HashMap<>();
        }

        public int get(int key) {

            // Check if the key exists in the map
            if (keyAddressMap.containsKey(key)) {

                // Move the corresponding node to the front of the list
                Node node = keyAddressMap.get(key);
                cache.remove(node);
                cache.addFirst(node);

                // Return the value of the node
                return node.val;
            }

            // If the key does not exist, return -1
            return -1;
        }

        public void put(int key, int value) {

            // If the key already exists in the cache, update its value
            if (keyAddressMap.containsKey(key)) {
                Node node = keyAddressMap.get(key);
                node.val = value;
                cache.remove(node);
                cache.addFirst(node);
            }

            // If the key does not exist in the cache, add it to the
            // front of the list.
            else {

                // If the cache is full, remove the least recently used node
                // from the back of the list
                if (cache.size() == capacity) {
                    Node lastNode = cache.getLast();
                    keyAddressMap.remove(lastNode.key);
                    cache.remove(lastNode);
                }

                // Create a new node for the key-value pair and add it to the
                // front of the list
                Node node = new Node(key, value);
                cache.addFirst(node);

                // Map the key to the node in the cache
                keyAddressMap.put(key, node);
            }
        }
    }

    public static void main(String[] args) {
        // Example from the problem statement
        LRUCache c1 = new LRUCache(2);
        c1.put(1, 10);
        c1.put(2, 20);
        System.out.println(c1.get(1));              // 10
        c1.put(3, 30);                              // evicts key 2
        System.out.println(c1.get(1));              // 10
        System.out.println(c1.get(2));              // -1 — evicted

        // Edge cases
        LRUCache c2 = new LRUCache(1);
        System.out.println(c2.get(5));              // -1 — empty cache
        c2.put(1, 100);
        System.out.println(c2.get(1));              // 100
        c2.put(2, 200);                             // evicts key 1
        System.out.println(c2.get(1));              // -1 — evicted
        System.out.println(c2.get(2));              // 200

        LRUCache c3 = new LRUCache(3);
        c3.put(1, 1);
        c3.put(2, 2);
        c3.put(3, 3);
        c3.put(1, 10);                              // update existing key — moves to front
        System.out.println(c3.get(1));              // 10
        c3.put(4, 4);                               // evicts LRU which is key 2
        System.out.println(c3.get(2));              // -1 — evicted
    }
}
```


> **Why a *doubly* linked list (not singly)?**
>
> The crucial operation is `remove(node)` — given a reference to a node, splice it out of the list in O(1). With a singly linked list, you'd need the *predecessor* to fix its `next` pointer; that's another O(N) walk to find. The `prev` pointer makes both ends of the splice O(1), and that's the whole reason this design hits its O(1) targets.

</details>

***

## Design a RandomisedSet

### Problem Statement

Implement a set that supports `insert`, `remove`, and `getRandom` — **all in O(1)** amortised.

> -   **`RandomisedSet()`** — Initialise an empty set.
> -   **`insert(int val)`** — Insert if not present. Return `true` if added, `false` otherwise.
> -   **`remove(int val)`** — Remove if present. Return `true` if removed, `false` otherwise.
> -   **`getRandom()`** — Return a uniformly random element. The set is guaranteed non-empty when called.

> **Example:**
>
> -   **Input:** `[RandomisedSet, insert, insert, insert, remove, getRandom]`, `[[], [2], [4], [6], [2], []]`
>
> -   **Output:** `[null, true, true, true, true, 4 or 6]`

<details>
<summary><h2>Approach</h2></summary>


Three operations all in O(1) — easy to do **two of three**, hard to do **three of three**:

- A hash set gives O(1) insert and remove (by value), but `getRandom` is **not** O(1) — there's no direct random index into a hash table.
- A dynamic array gives O(1) `getRandom` (`arr[rand() % size]`), and O(1) append, but `remove(val)` is O(N) — we'd have to scan to find the value's index.

The composite trick: **dynamic array + hash map**, where the array stores the values and the map stores `value → index in the array`. Now:

- **`insert(val)`** — push to the end of the array; record `val → array.length − 1` in the map.
- **`getRandom()`** — return `array[random index]`. Trivially O(1).
- **`remove(val)`** — the clever one. Look up `val`'s index in the map. **Swap it with the last element of the array.** Pop the array. Update the map: the swapped element now has the popped element's old index. Delete `val` from the map.

That swap-with-last is what avoids the O(N) shift. The only constraint: ordering inside the array doesn't matter — perfect for a *set* (which is order-agnostic by definition).

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

after: "pop array tail; delete 2 from map" {
  a3: "arr: [6, 4]" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  m3: "map: {4->1, 6->0}" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
}

before -> swap -> after
```

<p align="center"><strong>RandomisedSet remove — swap target with tail (O(1)), pop tail (O(1)), update the swapped element's index in the map (O(1)). The set's contents are correct; the order changed, but the set didn't care about order anyway.</strong></p>

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=graph viz-root=hash_map
import random
from typing import Dict, List

class RandomisedSet:
    def __init__(self) -> None:

        # list to store values
        self.values: List[int] = []

        # hash map to store value and its index
        self.hash_map: Dict[int, int] = {}

    def insert(self, val: int) -> bool:

        # If value already exists in the set
        if val in self.hash_map:
            return False

        # Add value to the end of list
        self.values.append(val)

        # Store the value and its index in hash map
        self.hash_map[val] = len(self.values) - 1
        return True

    def remove(self, val: int) -> bool:

        # If value does not exist in the set
        if val not in self.hash_map:
            return False

        # Get the index of value in the list
        index = self.hash_map[val]

        # Get the last value in the list
        last = self.values[-1]

        # Overwrite the value to be removed with last value
        self.values[index] = last

        # Update the index of last value in hash map
        self.hash_map[last] = index

        # Remove last value from list
        self.values.pop()

        # Remove value from hash map
        del self.hash_map[val]
        return True

    def getRandom(self) -> int:
        return random.choice(self.values)


# Example from the problem statement
s1 = RandomisedSet()
print(s1.insert(2))             # True
print(s1.insert(4))             # True
print(s1.insert(6))             # True
print(s1.remove(2))             # True
print(s1.getRandom() in [4, 6]) # True — returns 4 or 6

# Edge cases
s2 = RandomisedSet()
print(s2.insert(1))             # True
print(s2.insert(1))             # False — duplicate
print(s2.remove(99))            # False — not present
print(s2.remove(1))             # True
print(s2.insert(1))             # True — re-insert after removal

s3 = RandomisedSet()
s3.insert(10)
print(s3.getRandom())           # 10 — only element
print(s3.insert(10))            # False — already present
s3.insert(20)
s3.insert(30)
print(s3.remove(20))            # True — removes middle element (swap with last)
print(20 not in s3.hash_map)    # True — 20 is gone
```

```java run viz=graph viz-root=hash_map
import java.util.*;

public class Main {

    static class RandomisedSet {
        private List<Integer> values;
        private Map<Integer, Integer> hashMap;

        public RandomisedSet() {
            values = new ArrayList<>();
            hashMap = new HashMap<>();
        }

        public boolean insert(int val) {

            // If value already exists in the set
            if (hashMap.containsKey(val)) {
                return false;
            }

            // Add value to the end of list
            values.add(val);

            // Store the value and its index in hash map
            hashMap.put(val, values.size() - 1);
            return true;
        }

        public boolean remove(int val) {

            // If value does not exist in the set
            if (!hashMap.containsKey(val)) {
                return false;
            }

            // Get the index of value in the list
            int index = hashMap.get(val);

            // Get the last value in the list
            int last = values.get(values.size() - 1);

            // Replace the value to remove with the last value in the list
            values.set(index, last);

            // Update the index of the last value in the hash map
            hashMap.put(last, index);

            // Remove the last value from the list
            values.remove(values.size() - 1);

            // Remove the value from the hash map
            hashMap.remove(val);
            return true;
        }

        public int getRandom() {

            // Generate a random index in the range [0, size-1]
            int index = (int) (Math.random() * values.size());

            // Return the value at the random index
            return values.get(index);
        }
    }

    public static void main(String[] args) {
        // Example from the problem statement
        RandomisedSet s1 = new RandomisedSet();
        System.out.println(s1.insert(2));                      // true
        System.out.println(s1.insert(4));                      // true
        System.out.println(s1.insert(6));                      // true
        System.out.println(s1.remove(2));                      // true
        int r = s1.getRandom();
        System.out.println(r == 4 || r == 6);                  // true — returns 4 or 6

        // Edge cases
        RandomisedSet s2 = new RandomisedSet();
        System.out.println(s2.insert(1));                      // true
        System.out.println(s2.insert(1));                      // false — duplicate
        System.out.println(s2.remove(99));                     // false — not present
        System.out.println(s2.remove(1));                      // true
        System.out.println(s2.insert(1));                      // true — re-insert after removal

        RandomisedSet s3 = new RandomisedSet();
        s3.insert(10);
        System.out.println(s3.getRandom());                    // 10 — only element
        System.out.println(s3.insert(10));                     // false — already present
        s3.insert(20);
        s3.insert(30);
        System.out.println(s3.remove(20));                     // true — removes middle element
        System.out.println(!s3.hashMap.containsKey(20));       // true — 20 is gone
    }
}
```


> **Why does the swap-with-last trick work?**
>
> The set's contract is *unordered* — `{2, 4, 6}` and `{6, 4}` are equivalent, regardless of internal layout. So when we remove `2`, we don't actually need to preserve `[2, 4, 6] → [4, 6]`. We can rearrange to `[6, 4]` (swap-with-last) and still have a valid representation of the set `{4, 6}`. The `hash_map` storing each value's index is the secret that makes this safe — we only have to update *one* entry (the swapped value's new position), not shift N entries.
>
> If the set were *ordered*, this trick would fail and we'd be back to O(N). The unordered nature of a set is precisely what enables O(1) deletion here.

</details>
## Key Takeaway

Two composition patterns that recur in production:

1. **Hash map + doubly-linked list** — O(1) lookup *and* an O(1) recency/priority order. Caches, schedulers, deque-with-fast-key-access. The map points to nodes; the list owns them.
2. **Hash map + dynamic array** — O(1) lookup *and* O(1) random access by position. The array stores values, the map stores indices, and swap-with-last gives O(1) deletion as long as order doesn't matter.

The deeper lesson is **composability**: a hash table gives O(1) on *one* dimension (by key); to get O(1) on another (recency, position, frequency), pair it with a structure specialised for that dimension, collaborating via shared node references or indices. This pattern is `LinkedHashMap`, `OrderedDict`, LFU caches, indexed priority queues, and every database `INDEX` implementation.
