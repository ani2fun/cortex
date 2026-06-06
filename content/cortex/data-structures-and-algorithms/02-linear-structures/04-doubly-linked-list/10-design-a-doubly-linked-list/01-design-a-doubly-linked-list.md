---
title: "Design a Doubly Linked List"
summary: "Implement a DoublyLinkedList class that exposes prepend, append, insert, remove, search, size, and empty using bidirectional node links."
prereqs:
  - 02-linear-structures/04-doubly-linked-list/01-doubly-linked-lists
difficulty: hard
---

A working **`DoublyLinkedList` class** is the object behind Python's `collections.deque`, Java's `LinkedList`, every LRU cache, every undo stack, and every browser history list. Its eight operations are recombinations of three primitives you already own — **traversal, insertion, deletion** — and the whole challenge is keeping the bookkeeping correct at every boundary. The discipline throughout: **save before clobber, mirror every link, update size last**.

## The Problem

Given the skeleton of a **`DoublyLinkedList`** class, complete this class by implementing all the doubly linked list operations below.

> -   **`DoublyLinkedList()`** — initialises the `DoublyLinkedList` object.
> -   **`size()`** — returns the current size of the list.
> -   **`empty()`** — returns `true` if the list is empty and `false` if it is not.
> -   **`prepend(val)`** — inserts a node with the given value at the beginning of the list.
> -   **`append(val)`** — inserts a node with the given value at the end of the list.
> -   **`insert(position, val)`** — inserts a node with the given value at the given position in the list. Positions are indexed from 0, meaning the first node is at position 0.
> -   **`remove(val)`** — removes the first node whose value matches the given value. Returns `true` if the node was removed; otherwise, returns `false`.
> -   **`search(val)`** — returns `true` if a node with the given value exists in the linked list; returns `false` otherwise.

> The input should adhere to the following rules:
>
> 1. The input contains two arrays of the same size.
> 2. The first array contains the list of operations; the second contains the corresponding operands for those operations.
> 3. The first index in the first array contains `DoublyLinkedList`, and the first index in the second array contains an empty array. This initialises the `DoublyLinkedList`.
> 4. For each index in the first array containing **prepend**, **append**, **remove**, or **search**, the corresponding index in the second array contains the value to insert / remove / search.
> 5. For each index containing the **insert** operation, the corresponding second-array index contains a pair `[position, val]`.
> 6. For **size** or **empty**, the corresponding second-array index contains an empty array.

```
Input:  [DoublyLinkedList, prepend, prepend, append, size, search, insert, remove, empty]
        [[],               [2],     [3],     [1],    [],   [5],    [1, 8], [2],    []]
Output: [null, null, null, null, 3, false, null, true, false]

Trace:
  list = new DoublyLinkedList()  → []
  list.prepend(2)                → [2]
  list.prepend(3)                → [3, 2]
  list.append(1)                 → [3, 2, 1]
  list.size()                    → 3
  list.search(5)                 → false
  list.insert(1, 8)              → [3, 8, 2, 1]
  list.remove(2)                 → [3, 8, 1], returns true
  list.empty()                   → false
```

---

<details>
<summary><h2>The Architecture</h2></summary>


Every method below is a thin wrapper around primitives you already know. Three pieces of internal state hold the entire structure together — `head`, `tail`, and `size`:

```d2
direction: right

dll: "DoublyLinkedList instance" {
  h: |md
    **head**

    first node or null
  |
  t: |md
    **tail**

    last node or null
  |
  s: |md
    **size**

    int counter
  |
}

n1: "val: 3"
n2: "val: 8"
n3: "val: 2"
n4: "val: 1"

n1 <-> n2
n2 <-> n3
n3 <-> n4

dll.h -> n1
dll.t -> n4
```

<p align="center"><strong>Three fields are enough to support every operation in O(1) at the boundaries — <code>head</code> and <code>tail</code> for endpoint access, <code>size</code> for instant <code>size()</code> / <code>empty()</code> queries.</strong></p>

> *Why store `size` explicitly when we could count nodes on demand? — try answering this in your head before reading on.*
>
> Counting nodes is O(N). If callers ask for `size()` thousands of times (which they typically do, in checks like `if list.size() < threshold`), the linear cost dominates. We pay one integer increment/decrement per mutating operation and turn `size()` into O(1). **This is the classic time/space trade — pay 4 bytes once, save O(N) every read.**

The five mutating methods (`prepend`, `append`, `insert`, `remove`) each follow the same template:

```
1. Validate / handle empty-list case
2. Allocate new node OR locate target node
3. Update affected pointers (forward + backward — mirror!)
4. Update head/tail references if the boundary moved
5. Increment / decrement size
6. Return the contractual value
```

Stick to that order — particularly **size last** — and the bookkeeping never drifts.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

The implementation below mirrors the algorithms from lessons 02-04 — traversal, insertion, deletion. Two patterns are worth watching for as you read:

- **Boundary reuse** — `insert` short-circuits to `prepend` or `append` when the position lands at an endpoint, so the splice loop only runs in the interior case.
- **Head / tail / middle branching in `remove`** — the same three-way split as deletion-by-data in lesson 04. Each branch updates a different pair of pointers.


```python run viz=linked-list viz-root=head
from typing import Optional

class ListNode:
    def __init__(self, val=0, prev=None, nxt=None):
        self.val = val
        self.prev = prev
        self.next = nxt


def to_list(head):
    out = []
    while head is not None:
        out.append(head.val)
        head = head.next
    return out


class DoublyLinkedList:
    def __init__(self):

        # Pointer to the front node of the list
        self.head: Optional[ListNode] = None

        # Pointer to the last node of the list
        self.tail: Optional[ListNode] = None

        # Current number of elements in the list
        self.currentSize: int = 0

    def empty(self) -> bool:
        return self.head is None

    def size(self) -> int:
        return self.currentSize

    def prepend(self, val: int) -> None:
        newNode = ListNode(val)

        # If the list is empty, set the new node as both head and tail
        if self.empty():
            self.head = newNode
            self.tail = newNode
        else:
            newNode.next = self.head
            self.head.prev = newNode
            self.head = newNode

        self.currentSize += 1

    def append(self, val: int) -> None:
        newNode = ListNode(val)

        # If the list is empty, set the new node as both head and tail
        if self.empty():
            self.head = newNode
            self.tail = newNode
        else:
            newNode.prev = self.tail
            self.tail.next = newNode
            self.tail = newNode

        self.currentSize += 1

    def insert(self, position: int, val: int) -> None:

        # If the position is less than or equal to 0, prepend the new
        # node
        if position <= 0:
            self.prepend(val)
            return

        # If the position is greater than or equal to the current size,
        # append the new node
        if position >= self.currentSize:
            self.append(val)
            return

        newNode = ListNode(val)
        current = self.head
        currentPosition = 0

        # Traverse the list to reach the desired position
        while current and currentPosition < position:
            current = current.next
            currentPosition += 1

        # Insert the new node at the desired position and adjust pointers
        newNode.prev = current.prev
        newNode.next = current
        current.prev.next = newNode
        current.prev = newNode

        self.currentSize += 1

    def remove(self, val: int) -> bool:

        # If the list is empty, no removal is possible
        if self.empty():
            return False

        current = self.head
        while current:
            if current.val == val:

                # If the node to remove is the head, update the head
                # pointer
                if current == self.head:
                    self.head = current.next

                    # If the list is not empty, update the prev pointer
                    if self.head:
                        self.head.prev = None
                    else:
                        self.tail = None

                # If the node to remove is the tail, update the tail
                # pointer
                elif current == self.tail:
                    self.tail = current.prev
                    self.tail.next = None

                # Otherwise, remove the node by adjusting the prev and
                # next pointers of adjacent nodes
                else:
                    current.prev.next = current.next
                    current.next.prev = current.prev

                self.currentSize -= 1
                return True

            current = current.next

        return False

    def search(self, val: int) -> bool:
        current = self.head
        while current:

            # If the val is found, return true
            if current.val == val:
                return True
            current = current.next

        # If the val is not found, return false
        return False


# Example from the problem statement
dll = DoublyLinkedList()
dll.prepend(2); print(to_list(dll.head))   # [2]
dll.prepend(3); print(to_list(dll.head))   # [3, 2]
dll.append(1);  print(to_list(dll.head))   # [3, 2, 1]
print(dll.size())                          # 3
print(dll.search(5))                       # False
dll.insert(1, 8); print(to_list(dll.head)) # [3, 8, 2, 1]
print(dll.remove(2))                       # True
print(to_list(dll.head))                   # [3, 8, 1]
print(dll.empty())                         # False

# Edge cases
dll2 = DoublyLinkedList()
print(dll2.empty())                        # True
print(dll2.size())                         # 0
print(dll2.remove(5))                      # False
dll2.append(10); dll2.append(20); dll2.append(30)
print(to_list(dll2.head))                  # [10, 20, 30]
print(dll2.search(20))                     # True
print(dll2.search(99))                     # False
print(dll2.remove(10)); print(to_list(dll2.head))  # True, [20, 30]
print(dll2.remove(30)); print(to_list(dll2.head))  # True, [20]
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val;
        ListNode prev;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
    }

    static java.util.List<Integer> toList(ListNode head) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        while (head != null) { out.add(head.val); head = head.next; }
        return out;
    }

    static class DoublyLinkedList {

        // Pointer to the front node of the list
        private ListNode head;

        // Pointer to the last node of the list
        private ListNode tail;

        // Current number of elements in the list
        private int currentSize;

        public DoublyLinkedList() {
            this.head = null;
            this.tail = null;
            this.currentSize = 0;
        }

        public boolean empty() {
            return head == null;
        }

        public int size() {
            return currentSize;
        }

        public void prepend(int val) {
            ListNode newNode = new ListNode(val);

            // If the list is empty, set the new node as both head and tail
            if (empty()) {
                head = newNode;
                tail = newNode;
            }

            // Set the new node as the head and adjust pointers
            else {
                newNode.next = head;
                head.prev = newNode;
                head = newNode;
            }

            currentSize++;
        }

        public void append(int val) {
            ListNode newNode = new ListNode(val);

            // If the list is empty, set the new node as both head and tail
            if (empty()) {
                head = newNode;
                tail = newNode;
            }

            // Otherwise, set the new node as the tail and adjust pointers
            else {
                newNode.prev = tail;
                tail.next = newNode;
                tail = newNode;
            }

            currentSize++;
        }

        public void insert(int position, int val) {

            // If the position is less than or equal to 0, prepend the new
            // node
            if (position <= 0) {
                prepend(val);
                return;
            }

            // If the position is greater than or equal to the current size,
            // append the new node
            if (position >= currentSize) {
                append(val);
                return;
            }

            ListNode newNode = new ListNode(val);

            ListNode current = head;
            int currentPosition = 0;

            // Traverse the list to reach the desired position
            while (current != null && currentPosition < position) {
                current = current.next;
                currentPosition++;
            }

            // Insert the new node at the desired position and adjust
            // pointers
            newNode.prev = current.prev;
            newNode.next = current;
            current.prev.next = newNode;
            current.prev = newNode;

            currentSize++;
        }

        public boolean remove(int val) {

            // If the list is empty, no removal is possible
            if (empty()) {
                return false;
            }

            ListNode current = head;
            while (current != null) {
                if (current.val == val) {

                    // If the node to remove is the head, update the head
                    // pointer
                    if (current == head) {
                        head = current.next;

                        // If the list is not empty, update the prev pointer
                        if (head != null) {
                            head.prev = null;
                        }

                        // If the list becomes empty, update the tail pointer
                        else {
                            tail = null;
                        }
                    }

                    // If the node to remove is the tail, update the tail
                    // pointer
                    else if (current == tail) {
                        tail = current.prev;
                        tail.next = null;
                    }

                    // Otherwise, remove the node by adjusting the prev and
                    // next pointers of adjacent nodes
                    else {
                        current.prev.next = current.next;
                        current.next.prev = current.prev;
                    }

                    currentSize--;
                    return true;
                }

                current = current.next;
            }

            return false;
        }

        public boolean search(int val) {
            ListNode current = head;
            while (current != null) {

                // If the val is found, return true
                if (current.val == val) {
                    return true;
                }
                current = current.next;
            }

            // If the val is not found, return false
            return false;
        }

        public ListNode getHead() { return head; }
    }

    public static void main(String[] args) {
        // Example from the problem statement
        DoublyLinkedList dll = new DoublyLinkedList();
        dll.prepend(2); System.out.println(toList(dll.getHead()));   // [2]
        dll.prepend(3); System.out.println(toList(dll.getHead()));   // [3, 2]
        dll.append(1);  System.out.println(toList(dll.getHead()));   // [3, 2, 1]
        System.out.println(dll.size());                              // 3
        System.out.println(dll.search(5));                           // false
        dll.insert(1, 8); System.out.println(toList(dll.getHead())); // [3, 8, 2, 1]
        System.out.println(dll.remove(2));                           // true
        System.out.println(toList(dll.getHead()));                   // [3, 8, 1]
        System.out.println(dll.empty());                             // false

        // Edge cases
        DoublyLinkedList dll2 = new DoublyLinkedList();
        System.out.println(dll2.empty());                            // true
        System.out.println(dll2.size());                             // 0
        System.out.println(dll2.remove(5));                          // false
        dll2.append(10); dll2.append(20); dll2.append(30);
        System.out.println(toList(dll2.getHead()));                  // [10, 20, 30]
        System.out.println(dll2.search(20));                         // true
        System.out.println(dll2.search(99));                         // false
        System.out.println(dll2.remove(10)); System.out.println(toList(dll2.getHead())); // true, [20, 30]
        System.out.println(dll2.remove(30)); System.out.println(toList(dll2.getHead())); // true, [20]
    }
}
```


<details>
<summary><strong>Trace — operations from the example input</strong></summary>

```
Op                            │ list state         │ size │ return
──────────────────────────────┼────────────────────┼──────┼────────
new DoublyLinkedList()        │ []                 │ 0    │ —
prepend(2)                    │ [2]                │ 1    │ —
prepend(3)                    │ [3, 2]             │ 2    │ —
append(1)                     │ [3, 2, 1]          │ 3    │ —
size()                        │ [3, 2, 1]          │ 3    │ 3
search(5)                     │ [3, 2, 1]          │ 3    │ false
insert(1, 8)                  │ [3, 8, 2, 1]       │ 4    │ —      (clamp not triggered, walk to idx 1, splice before)
remove(2)                     │ [3, 8, 1]          │ 3    │ true   (target is interior — middle case)
empty()                       │ [3, 8, 1]          │ 3    │ false
```

Two rows are worth pausing on. `insert(1, 8)` walks to the node currently at position 1 (the `2`), then splices the new `8` before it — the "insert before the given node" primitive from lesson 03, with the target located by index instead of reference. `remove(2)` follows the "delete by value" primitive from lesson 04 and hits the middle-node branch — neither head nor tail moves, only two interior pointers flip.

</details>

### Complexity Analysis

| Operation | Time | Space | Why |
|---|---|---|---|
| `empty()` | **O(1)** | **O(1)** | One null check on `head`. |
| `size()` | **O(1)** | **O(1)** | Maintained counter — no traversal needed. |
| `prepend(val)` | **O(1)** | **O(1)** | Three pointer updates plus one allocation. |
| `append(val)` | **O(1)** | **O(1)** | Three pointer updates plus one allocation. The `tail` reference is what keeps this constant — without it, append would require a full forward walk. |
| `insert(pos, val)` | **O(N)** worst, **O(1)** at endpoints | **O(1)** | Endpoint clamps short-circuit to `prepend`/`append`. Otherwise walk to position `pos` then splice — splice itself is O(1), the walk is O(pos). |
| `remove(val)` | **O(N)** | **O(1)** | Linear scan to find the value. The deletion itself is O(1) once the target is located, thanks to the `prev` pointer. |
| `search(val)` | **O(N)** | **O(1)** | Linear scan from head; returns on first match. |

> *The headline number above is **O(1) `append`**. A singly linked list without a tail reference pays O(N) per append — you walk to the end every time. Spending one pointer on a `tail` reference makes every append constant-time. That trade is **the** reason `collections.deque`, every LRU cache, and every undo-stack implementation reach for a doubly linked list over a singly linked one.*

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| `prepend` into empty list | `[].prepend(5)` | `[5]`, head == tail == new node | The empty branch wires head and tail together so a one-element list still satisfies the invariants. |
| `append` into empty list | `[].append(5)` | `[5]`, head == tail == new node | Same single-node invariant — both endpoint references must be set, not just one. |
| `insert(0, val)` on non-empty list | `[3,2].insert(0, 9)` | `[9,3,2]` | `position <= 0` clamp routes to `prepend` — no off-by-one in the walk loop. |
| `insert(size, val)` on non-empty list | `[3,2].insert(2, 9)` | `[3,2,9]` | `position >= currentSize` clamp routes to `append` — preventing a walk that would land on `null`. |
| `insert(-1, val)` | `[3,2].insert(-1, 9)` | `[9,3,2]` | Negative positions clamp left, never crash. |
| `remove` from single-node list | `[5].remove(5)` | `[]`, returns `true`, head == tail == null | The head branch sets `tail = null` when the list becomes empty — without that line, subsequent `append` calls would dereference a stale tail. |
| `remove` of head from multi-node list | `[3,2,1].remove(3)` | `[2,1]`, new head's prev == null | Critical: clearing the new head's `prev` is what keeps reverse traversal honest. |
| `remove` of tail from multi-node list | `[3,2,1].remove(1)` | `[3,2]`, new tail's next == null | Symmetric to the head case — clear the new tail's `next`. |
| `remove` of value not present | `[3,2,1].remove(7)` | `[3,2,1]`, returns `false` | Loop falls off, no mutation, size unchanged. |
| `search` in empty list | `[].search(5)` | `false` | Loop never enters; falls through to `false`. |

</details>
<details>
<summary><h2>Where This Class Lives in the Real World</h2></summary>


The `DoublyLinkedList` you built is the literal foundation of:

- **Python's `collections.deque`** — uses a DLL with block allocation for cache-friendliness, supports O(1) push/pop on both ends.
- **Java's `java.util.LinkedList`** — implements both `List` and `Deque` interfaces using a DLL identical in spirit to what's above.
- **LRU caches** — pair this DLL with a hash map: the map gives O(1) lookup, the DLL gives O(1) move-to-front and O(1) eviction. We'll build this in the next section. (Hash table → doubly linked list is one of the most-asked system design patterns in interviews.)
- **Browser history** — back/forward buttons walk a DLL of pages.
- **Editor undo/redo** — every keystroke pushes a node; redo walks `next`, undo walks `prev`.
- **Music/video players** — previous/next track on a playlist is `current.prev` / `current.next`.

Whenever you see "constant-time insertion and removal at known positions, with bidirectional iteration" in a system design question, the answer starts with a doubly linked list.

</details>
## Key Takeaway

Every method is a recombination of three primitives — **walk, wire, unwire** — held together by one discipline: **save before clobber, mirror every link, update size last**. Before compiling any mutating method, answer the six-question design checklist:

1. **Empty-list case** — does the code handle `head == null`?
2. **Single-node case** — when the list grows from or shrinks to one node, are head *and* tail both set/cleared?
3. **Boundary mutations** — when the head or tail itself moves, are both references updated?
4. **Mirror updates** — for every `a.next = b`, is there a matching `b.prev = a`?
5. **Size invariant** — does the counter change exactly once per mutation?
6. **Return contract** — does the method return what its signature promises?

> **Transfer challenge:** Extend this `DoublyLinkedList` with `removeLast()` that removes and returns the tail value in **O(1)**, then build an `LRUCache` on top (a `DoublyLinkedList` + a hash map): `get` and `put` both O(1), evicting the least-recently-used entry — at the *tail* — in O(1) via `removeLast()`.
>
> <details>
> <summary>Solution sketch</summary>
>
> `removeLast()` is a near-copy of `remove()` specialised to the tail: capture `tail.val`, set `tail = tail.prev`, set `tail.next = null` (or clear both head/tail if the list emptied), decrement size, return the captured value.
>
> For the LRU cache: keep a `HashMap<key, Node>` so `get` looks up the node in O(1). On `get`, splice the node out and re-insert it at the head (the most-recently-used end). On `put`, update + move-to-head if the key exists, else prepend a new node and (if size > capacity) call `removeLast()` and drop that key from the map. Both are O(1) because the hash map locates the node and `prev` makes splicing O(1).
>
> </details>
