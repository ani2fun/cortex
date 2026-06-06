---
title: "Design a Singly Linked List"
summary: "Implement a SinglyLinkedList class that exposes prepend, append, insert, remove, search, size, and empty as a single self-contained object."
prereqs:
  - 02-linear-structures/03-singly-linked-list/01-what-is-a-linked-list
  - 02-linear-structures/03-singly-linked-list/01-what-is-a-linked-list
difficulty: hard
---

<details>
<summary><h2>The Hook</h2></summary>


Every language you'll ever use ships with a linked-list library — Java's `LinkedList`, C++'s `std::list`, Python's `collections.deque`. You've used them. You've never built one. This is the lesson where you stop being a consumer and become the engineer who understands why `list.addFirst(x)` is O(1) but `list.get(99)` is not.

You've already met every primitive you need — node definition (lesson 1), traversal (lesson 2), insertion (lesson 3), deletion (lesson 4). This lesson ties them together into one class that exposes a complete public API: `prepend`, `append`, `insert`, `remove`, `search`, `size`, `empty`. Writing it from scratch forces you to confront **every design trade-off** you've been meeting one at a time — cached size vs computed size, head-only vs head + tail, bounds semantics, null safety. The implementation is short; the *choices* are what matter.

</details>

---

## The Problem

> Implement a `SinglyLinkedList` class that supports:
>
> - `SinglyLinkedList()` — initialize an empty list.
> - `size()` — return the current number of elements.
> - `empty()` — return `true` iff the list has no elements.
> - `prepend(val)` — insert a node with value `val` at the beginning.
> - `append(val)` — insert a node with value `val` at the end.
> - `insert(position, val)` — insert a node with value `val` at 0-indexed `position`. If `position ≤ 0`, prepend. If `position` exceeds the current list length, append to the end.
> - `remove(val)` — remove the **first** node whose value matches `val`. Return `true` on success, `false` if not found.
> - `search(val)` — return `true` iff some node's value equals `val`.

```
Input:
  ops  = [SinglyLinkedList, prepend, prepend, append, size, search, insert, remove, empty]
  args = [[],                [2],     [3],     [1],    [],   [5],    [1, 8], [2],    []]

Output:
  [null, null, null, null, 3, false, null, true, false]

Step-by-step:
  SinglyLinkedList()    → list = []
  prepend(2)            → list = [2]
  prepend(3)            → list = [3, 2]
  append(1)             → list = [3, 2, 1]
  size()                → 3
  search(5)             → false
  insert(1, 8)          → list = [3, 8, 2, 1]
  remove(2)             → list = [3, 8, 1],  returns true
  empty()               → false
```

---

<details>
<summary><h2>What Does "Design a Linked List" Really Ask?</h2></summary>


"Design" is the keyword that separates this from the operation-specific lessons. You're not implementing *one* operation — you're deciding **what state the class keeps** so that all seven operations run efficiently and coexist correctly.

Two design decisions shape every linked-list class you'll ever write:

1. **Do we cache `size` on the object, or recompute it every call?**  
   *Cached* — `size()` is O(1), but every insert and delete must increment or decrement a counter. Mismatched book-keeping (forgetting to decrement on a failed delete, double-counting on re-entrant inserts) is a classic silent bug.  
   *Recomputed* — `size()` costs O(n) time, O(1) space. No counter to maintain. Slower for size-heavy workloads.

2. **Do we keep a `tail` pointer, or walk to find the tail?**  
   *Cached `tail`* — `append` becomes O(1). But every operation that might change the tail (head deletion that empties the list, removal of the last node, insert at position = size) must update it.  
   *No `tail`* — `append` is O(n) but there's one less invariant to maintain.

```d3 widget=list-single
{
  "steps": [
    {
      "nodes": [
        {
          "id": "n1",
          "label": "3",
          "kind": "node",
          "meta": [],
          "slot": null,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "n2",
          "label": "8",
          "kind": "node",
          "meta": [],
          "slot": null,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "n3",
          "label": "2",
          "kind": "node",
          "meta": [],
          "slot": null,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "n4",
          "label": "1",
          "kind": "node",
          "meta": [],
          "slot": null,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [
        {
          "from": "n1",
          "to": "n2",
          "label": "next"
        },
        {
          "from": "n2",
          "to": "n3",
          "label": "next"
        },
        {
          "from": "n3",
          "to": "n4",
          "label": "next"
        }
      ],
      "cursor": [
        {
          "name": "head",
          "target": "n1",
          "color": "#10b981"
        },
        {
          "name": "tail",
          "target": "n4",
          "color": "#8b5cf6"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "head + cached currentSize is the minimal viable design; tail is optional (makes append O(1) but costs an extra field to maintain)",
      "line": 0,
      "frames": [],
      "cardCursor": []
    }
  ],
  "title": "SinglyLinkedList state — head (always) + currentSize (cached) + tail (optional)"
}
```

<p align="center"><strong>The <code>SinglyLinkedList</code> object owns three pieces of state — <code>head</code> (always), <code>currentSize</code> (usually cached), and <code>tail</code> (sometimes cached). The trade-offs are the whole design.</strong></p>

For this lesson we take the **cached-size, no-tail** design. It matches the reference and highlights the O(n) cost of `append` as a teachable weakness. The transfer challenge at the end adds a tail pointer and walks through the change in operation costs.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Question | Answer |
|---|---|
| **Q1.** Why do we need the `empty()` predicate when we could just check `size() == 0`? | **Clarity & cheapness** — `empty()` is an O(1) head-null check; readable at call sites. |
| **Q2.** Why track `currentSize` at all? | **O(1) `size()` queries** — recomputing via traversal costs O(n) per call. |
| **Q3.** Why does `insert(pos, val)` with `pos ≤ 0` reduce to `prepend`? | **Negative / zero positions map to "before the head"** — a single degenerate case handled once. |
| **Q4.** Why does `remove(val)` need a special case for the head? | **The head has no predecessor** — the generic "find predecessor, splice" logic can't apply. |

### Q1 — Why a dedicated `empty()` method?

**Mental model:** `empty()` is about *existence*, `size()` is about *count*. Clients frequently want "is there anything here?" — not "how many?". Giving each question its own method makes call sites read clearly.

**Concrete numbers:** `empty()` is one pointer comparison against `null` — roughly one nanosecond. `size() == 0` costs the same *if* `size` is cached. In designs where it isn't, that comparison is O(n). Defining both decouples API ergonomics from implementation choices.

**What breaks otherwise:** omitting `empty()` forces every caller to write `list.size() == 0`. That obscures intent and couples the caller to the cost of `size()` — innocent until the day you switch to a recomputed-size design.

### Q2 — Why cache `currentSize`?

**Mental model:** `size()` is called **far more often** than insertions and deletions in typical workloads (think: "while size > 0, pop and process"). Optimising the common case is worth a small bookkeeping price on the rare case.

**Concrete numbers:** one increment on `prepend`, `append`, `insert`; one decrement on successful `remove`. Six extra lines of code total. In exchange, `size()` drops from O(n) to O(1) time, O(1) space.

**What breaks otherwise:** if `remove` forgets to decrement on success, `size()` drifts upward and stays wrong forever. The counter updates live *inside* the methods, next to the pointer mutations — never in a wrapper.

### Q3 — Why does `pos ≤ 0` collapse to `prepend`?

**Mental model:** inserting at position `0` puts the new node at the head. A negative position is nonsense, but rather than throw, the standard convention is to clamp it to the nearest legal value — `0` — and prepend. "Before the start" means "at the start".

**Concrete numbers:** `insert(0, 8)` → prepend. `insert(-5, 8)` → prepend. `insert(1, 8)` on a 5-node list → splice at position `1`. `insert(100, 8)` on a 5-node list → walk to the tail and append (the loop terminates at `current.next == null`).

**What breaks otherwise:** throwing on negative positions punishes defensive callers. Clamping makes the API forgiving. The clamp-at-zero pattern matches the one you meet in `array.slice(-1)` — negative indices have conventions, not errors.

### Q4 — Why the head special case in `remove`?

**Mental model:** every other node has a predecessor whose `.next` pointer we can redirect. The head has nothing pointing at it — except the list's own `head` field. Removing the head means updating the list object, not splicing a pointer elsewhere.

**Concrete numbers:** for `[3, 8, 1]` with target `val = 3`, the splice-by-predecessor logic would need a "fake predecessor" to redirect. Cleaner to check `head.val == val` up front and do `head = head.next`.

**What breaks otherwise:** without the head special case, `remove` on the head either crashes with a null-deref (no predecessor to follow) or requires contortions like a dummy sentinel. Treating the head as its own case is the conventional, readable answer.

</details>
<details>
<summary><h2>The Operation Map (Visualised)</h2></summary>


```d2
direction: right

fast: "O(1) operations" {
  style.fill: "#dcfce7"
  style.stroke: "#16a34a"
  grid-columns: 3
  grid-gap: 12
  a1: "size()"
  a2: "empty()"
  a3: "prepend(val)"
}

slow: "O(n) operations" {
  style.fill: "#fee2e2"
  style.stroke: "#dc2626"
  grid-rows: 2
  grid-gap: 12
  b1: "append(val)"
  b2: "insert(pos, val) (worst case)"
  b3: "remove(val) (worst case)"
  b4: "search(val) (worst case)"
}
```

<p align="center"><strong>The cost map. Three operations are O(1) because they touch only <code>head</code> and <code>currentSize</code>. The other four require traversal. Caching a <code>tail</code> pointer would move <code>append</code> into the fast column.</strong></p>

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=linked-list viz-root=head
from typing import Optional


class ListNode:
    def __init__(self, val=0, nxt=None):
        self.val = val
        self.next = nxt


def to_list(head):
    out = []
    while head is not None:
        out.append(head.val)
        head = head.next
    return out


class SinglyLinkedList:
    def __init__(self):

        # Pointer to the front node of the list
        self.head: Optional[ListNode] = None

        # Current number of elements in the list
        self.current_size: int = 0

    def empty(self) -> bool:
        return self.head is None

    def size(self) -> int:
        return self.current_size

    def prepend(self, val: int) -> None:
        new_node = ListNode(val)
        new_node.next = self.head
        self.head = new_node
        self.current_size += 1

    def append(self, val: int) -> None:
        new_node = ListNode(val)

        # If the list is empty, set the new node as the head
        if self.empty():
            self.head = new_node

        # Otherwise, find the last node and set the next pointer to the
        # new node
        else:
            current = self.head
            while current.next:
                current = current.next
            current.next = new_node

        self.current_size += 1

    def insert(self, position: int, val: int) -> None:

        # If the position is less than or equal to 0, prepend the new
        # node
        if position <= 0:
            self.prepend(val)
            return

        new_node = ListNode(val)

        # If the list is empty and the position is not 0, ignore the
        # insertion
        if self.empty():
            return

        current = self.head
        current_position = 0

        # Traverse the list to reach the position or the end of the
        # list
        while current.next and current_position < position - 1:
            current = current.next
            current_position += 1

        # Insert the new node at the desired position
        new_node.next = current.next
        current.next = new_node
        self.current_size += 1

    def remove(self, val: int) -> bool:

        # If the list is empty, no removal is possible
        if self.empty():
            return False

        if self.head.val == val:

            # If the val is in the head node, update the head pointer
            self.head = self.head.next
            self.current_size -= 1
            return True

        current = self.head
        while current.next:

            # Remove the node by adjusting the next pointer of the
            # previous node
            if current.next.val == val:
                current.next = current.next.next
                self.current_size -= 1
                return True
            current = current.next

        return False

    def search(self, val: int) -> bool:
        current = self.head
        while current:

            # If the val is found, return True
            if current.val == val:
                return True
            current = current.next

        # If the value is not found, return False
        return False


# Example from the problem statement
ll = SinglyLinkedList()
print(ll.empty())                         # True
ll.prepend(2); print(to_list(ll.head))    # [2]
ll.prepend(3); print(to_list(ll.head))    # [3, 2]
ll.append(1);  print(to_list(ll.head))    # [3, 2, 1]
print(ll.size())                          # 3
print(ll.search(5))                       # False
ll.insert(1, 8); print(to_list(ll.head))  # [3, 8, 2, 1]
print(ll.remove(2))                       # True
print(to_list(ll.head))                   # [3, 8, 1]
print(ll.empty())                         # False

# Edge cases
ll2 = SinglyLinkedList()
print(ll2.remove(10))                     # False  (remove from empty)
print(ll2.search(1))                      # False  (search in empty)
ll2.append(5); print(ll2.size())          # 1
ll2.insert(0, 9); print(to_list(ll2.head))# [9, 5]  (insert at position 0)
print(ll2.remove(9))                      # True   (remove head)
print(to_list(ll2.head))                  # [5]
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
    }

    static java.util.List<Integer> toList(ListNode head) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        while (head != null) { out.add(head.val); head = head.next; }
        return out;
    }

    static class SinglyLinkedList {

        // Pointer to the front node of the list
        private ListNode head;

        // Current number of elements in the list
        private int currentSize;

        public SinglyLinkedList() {
            head = null;
            currentSize = 0;
        }

        public boolean empty() {
            return head == null;
        }

        public int size() {
            return currentSize;
        }

        public void prepend(int val) {
            ListNode newNode = new ListNode(val);
            newNode.next = head;
            head = newNode;
            currentSize++;
        }

        public void append(int val) {
            ListNode newNode = new ListNode(val);

            // If the list is empty, set the new node as the head
            if (empty()) {
                head = newNode;
            }

            // Otherwise, find the last node and set the next pointer to the
            // new node
            else {
                ListNode current = head;
                while (current.next != null) {
                    current = current.next;
                }
                current.next = newNode;
            }

            currentSize++;
        }

        public void insert(int position, int val) {

            // If the position is less than or equal to 0, prepend the
            // new node
            if (position <= 0) {
                prepend(val);
                return;
            }

            ListNode newNode = new ListNode(val);

            // If the list is empty and the position is not 0, ignore the
            // insertion
            if (empty()) {
                return;
            }

            ListNode current = head;
            int currentPosition = 0;

            // Traverse the list to reach the position or the end of the
            // list
            while (current.next != null && currentPosition < position - 1) {
                current = current.next;
                currentPosition++;
            }

            // Insert the new node at the desired position
            newNode.next = current.next;
            current.next = newNode;
            currentSize++;
        }

        public boolean remove(int val) {

            // If the list is empty, no removal is possible
            if (empty()) {
                return false;
            }

            // If the val is in the head node, update the head pointer
            if (head.val == val) {
                head = head.next;
                currentSize--;
                return true;
            }

            ListNode current = head;
            while (current.next != null) {

                // Remove the node by adjusting the next pointer of the
                // previous node
                if (current.next.val == val) {
                    current.next = current.next.next;
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

            // If the value is not found, return false
            return false;
        }
    }

    public static void main(String[] args) {
        // Example from the problem statement
        SinglyLinkedList ll = new SinglyLinkedList();
        System.out.println(ll.empty());                         // true
        ll.prepend(2); System.out.println(toList(ll.head));     // [2]
        ll.prepend(3); System.out.println(toList(ll.head));     // [3, 2]
        ll.append(1);  System.out.println(toList(ll.head));     // [3, 2, 1]
        System.out.println(ll.size());                          // 3
        System.out.println(ll.search(5));                       // false
        ll.insert(1, 8); System.out.println(toList(ll.head));   // [3, 8, 2, 1]
        System.out.println(ll.remove(2));                       // true
        System.out.println(toList(ll.head));                    // [3, 8, 1]
        System.out.println(ll.empty());                         // false

        // Edge cases
        SinglyLinkedList ll2 = new SinglyLinkedList();
        System.out.println(ll2.remove(10));                     // false  (remove from empty)
        System.out.println(ll2.search(1));                      // false  (search in empty)
        ll2.append(5); System.out.println(ll2.size());          // 1
        ll2.insert(0, 9); System.out.println(toList(ll2.head)); // [9, 5]  (insert at position 0)
        System.out.println(ll2.remove(9));                      // true   (remove head)
        System.out.println(toList(ll2.head));                   // [5]
    }
}
```

</details>
<details>
<summary><strong>Trace — the canonical example sequence</strong></summary>

```
Op                         | Internal state              | Return
----------------------------|-----------------------------|--------
SinglyLinkedList()          | head=null, size=0           | —
prepend(2)                  | head → 2;           size=1  | null
prepend(3)                  | head → 3 → 2;       size=2  | null
append(1)                   | head → 3 → 2 → 1;   size=3  | null
size()                      | (unchanged)                 | 3
search(5)                   | scan; no match              | false
insert(1, 8)                | head → 3 → 8 → 2 → 1; size=4 | null
                              ↑ walk to predecessor of pos 1 (which is node 3)
                              ↑ then splice: node 3's next becomes new node,
                                new node's next becomes old node 2
remove(2)                   | head → 3 → 8 → 1;   size=3  | true
                              ↑ find predecessor of node 2 (node 8)
                              ↑ splice: node 8's next = node 1
empty()                     | head is not null            | false
```

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Complexity Analysis

| Operation | Time | Space | Notes |
|---|---|---|---|
| `SinglyLinkedList()` | O(1) | O(1) | trivial init |
| `empty()` | O(1) | O(1) | single null check |
| `size()` | O(1) | O(1) | cached counter |
| `prepend(val)` | O(1) | O(1) | two pointer updates + alloc |
| `append(val)` | **O(n)** | O(1) | walks to the tail — no tail cache |
| `insert(pos, val)` | **O(n)** worst case, O(1) at head | O(1) | walk cost bounded by `min(pos, n)` |
| `remove(val)` | **O(n)** worst case, O(1) at head | O(1) | find predecessor, splice |
| `search(val)` | **O(n)** worst case, O(1) at head | O(1) | linear scan |

The "**all O(n)**" row is the cost of the **no-tail** design. Caching a `tail` pointer drops `append` to O(1) time, but requires `tail` updates on every insert or delete that might change the last node.

### Edge Cases

| Case | Example | Expected behaviour |
|---|---|---|
| Operations on a fresh list | `list.size()`, `list.empty()`, `list.search(5)`, `list.remove(5)` | `0`, `true`, `false`, `false` |
| Prepend to empty | `prepend(2)` on `[]` | `[2]`, size → 1 |
| Append to empty | `append(2)` on `[]` | `[2]`, size → 1 (head becomes the new node) |
| Insert at position 0 | `insert(0, 9)` | equivalent to `prepend(9)` |
| Insert at negative position | `insert(-5, 9)` | clamps to `prepend(9)` |
| Insert past the end | `insert(100, 9)` on `[1, 2, 3]` | loop stops at tail; appends to end → `[1, 2, 3, 9]` |
| Remove the only node | `remove(5)` on `[5]` | `head` becomes `null`, size → 0, returns `true` |
| Remove non-existent value | `remove(99)` on `[1, 2, 3]` | list unchanged, returns `false` |
| Duplicate values, remove first | `remove(2)` on `[1, 2, 2, 3]` | removes the **first** 2 → `[1, 2, 3]` |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


You just built the linked list. Every operation from lessons 1–4 is here, wired together into a single class with a cohesive API. Two lessons are worth taking away:

1. **The class's state IS the design.** `head`, `currentSize`, and (optionally) `tail` are three fields that encode three trade-offs. Every design decision — cache this, recompute that, reject this, clamp that — lives in those fields and the invariants you maintain on them.
2. **The head is always special.** Every linked-list operation has a "the head case" because the head has no predecessor. Internalise this and you stop being surprised by it. Read *any* linked-list library code in the wild and you'll see the same pattern — head is always its own branch.

When you next see "design a …" for stacks, queues, graphs, or trees, reach for the same pattern. **Identify the minimum state. Write down the invariants. Code the operations around those invariants.** The rest is mechanical.

> **Transfer Challenge:** Extend this class by caching a `tail` pointer so that `append` runs in O(1). Which existing operations now need to update `tail`, and which are unchanged? What's the **minimum set** of lines you must change?
>
> <details><summary><strong>Answer</strong></summary>
>
> Operations that must update <code>tail</code>:<br>
> - <code>append</code> — <code>tail.next = n; tail = n</code> in the non-empty case; <code>head = tail = n</code> in the empty case. Now O(1).<br>
> - <code>prepend</code> — only when the list was empty: <code>tail = new_head</code>. Otherwise unchanged.<br>
> - <code>insert</code> — if <code>position</code> reaches the end of the list, the newly inserted node becomes the tail. Check: if <code>cur == tail</code> before splicing, update <code>tail = n</code> after.<br>
> - <code>remove</code> — if the removed node was the tail, update <code>tail</code> to its predecessor. Note: singly linked lists can't find the predecessor in O(1), so tail-removal ironically costs O(n) even with a tail cache. This is the case where doubly linked lists win.<br>
> <br>
> Unchanged: <code>size</code>, <code>empty</code>, <code>search</code>.<br>
> <br>
> The net effect: append becomes O(1), but tail-removal stays O(n). For strict queue-like usage (append + prepend + remove from head), this is the best you can do with a singly linked list. For O(1) on both ends, you need doubly linked (the next chapter).
>
> </details>

</details>
