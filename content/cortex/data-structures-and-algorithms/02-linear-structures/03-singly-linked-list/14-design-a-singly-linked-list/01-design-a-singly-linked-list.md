---
title: "Design a Singly Linked List"
summary: "Implement a SinglyLinkedList class that exposes prepend, append, insert, remove, search, size, and empty as a single self-contained object."
prereqs:
  - 02-linear-structures/03-singly-linked-list/01-what-is-a-linked-list
difficulty: hard
kind: problem
topics: [singly-linked-list, design]
---

# Design a Singly Linked List

## The Problem

Every language ships a linked-list library — Java's `LinkedList`, C++'s `std::list`, Python's `collections.deque`. You've used them; now build one. You've already met every primitive — node, traversal, insertion, deletion. This challenge ties them into one class with a complete public API, where the *choices* (cache the size or recompute it? keep a tail pointer or walk?) matter more than the code.

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

```quiz
{
  "prompt": "Now your turn!",
  "input": "prepend(1), prepend(2), append(3), insert(1, 9) on an empty list",
  "options": ["[2, 9, 1, 3]", "[1, 2, 9, 3]", "[9, 2, 1, 3]", "[2, 1, 9, 3]"],
  "answer": "[2, 9, 1, 3]"
}
```

## Constraints

- `0 ≤ total operations ≤ 1000`, `-10^9 ≤ val ≤ 10^9`
- `insert` clamps: `position ≤ 0` means prepend, `position > length` means append
- `remove` deletes only the **first** match
- `size()` and `empty()` must run in **O(1)**

The workbench drives your class through every operation: it prepends the `prepends` values in order, appends the `appends` values, then performs one `insert`, one `remove`, and one `search` — and prints the final list plus what the queries returned. Implement all seven methods.

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class SinglyLinkedList:
    def __init__(self):
        self.head = None          # pointer to the front node
        self.current_size = 0     # cached element count

    def empty(self):
        # Your code goes here — one null check.
        return True

    def size(self):
        # Your code goes here — O(1), use the cached counter.
        return 0

    def prepend(self, val):
        # Your code goes here — two pointer writes + count.
        pass

    def append(self, val):
        # Your code goes here — walk to the tail (or become the head).
        pass

    def insert(self, position, val):
        # Your code goes here — clamp position ≤ 0 to prepend; walking off
        # the end appends.
        pass

    def remove(self, val):
        # Your code goes here — head is a special case; return True/False.
        return False

    def search(self, val):
        # Your code goes here — linear scan.
        return False

ll = SinglyLinkedList()
prepends = ast.literal_eval(input())     # values to prepend, in order
appends = ast.literal_eval(input())      # values to append, in order
pos = int(input())                       # insert position
insert_val = int(input())                # insert value
remove_val = int(input())                # value to remove
search_val = int(input())                # value to search for

for v in prepends:
    ll.prepend(v)
for v in appends:
    ll.append(v)
ll.insert(pos, insert_val)
removed = ll.remove(remove_val)
found = ll.search(search_val)

out = []
node = ll.head
while node:                              # traverse to collect the values in order
    out.append(node.val)
    node = node.next
print(out)
print("removed =", "true" if removed else "false")
print("found =", "true" if found else "false")
print("size =", ll.size(), "empty =", "true" if ll.empty() else "false")
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode next;
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    static class SinglyLinkedList {
        ListNode head = null;     // pointer to the front node
        int currentSize = 0;      // cached element count

        boolean empty() {
            // Your code goes here — one null check.
            return true;
        }

        int size() {
            // Your code goes here — O(1), use the cached counter.
            return 0;
        }

        void prepend(int val) {
            // Your code goes here — two pointer writes + count.
        }

        void append(int val) {
            // Your code goes here — walk to the tail (or become the head).
        }

        void insert(int position, int val) {
            // Your code goes here — clamp position ≤ 0 to prepend; walking
            // off the end appends.
        }

        boolean remove(int val) {
            // Your code goes here — head is a special case.
            return false;
        }

        boolean search(int val) {
            // Your code goes here — linear scan.
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        SinglyLinkedList ll = new SinglyLinkedList();
        int[] prepends = parseIntArray(sc.nextLine());           // values to prepend, in order
        int[] appends = parseIntArray(sc.nextLine());            // values to append, in order
        int pos = Integer.parseInt(sc.nextLine().trim());        // insert position
        int insertVal = Integer.parseInt(sc.nextLine().trim());  // insert value
        int removeVal = Integer.parseInt(sc.nextLine().trim());  // value to remove
        int searchVal = Integer.parseInt(sc.nextLine().trim());  // value to search for

        for (int v : prepends) ll.prepend(v);
        for (int v : appends) ll.append(v);
        ll.insert(pos, insertVal);
        boolean removed = ll.remove(removeVal);
        boolean found = ll.search(searchVal);

        List<Integer> out = new ArrayList<>();
        for (ListNode node = ll.head; node != null; node = node.next) out.add(node.val);
        System.out.println(out);
        System.out.println("removed = " + removed);
        System.out.println("found = " + found);
        System.out.println("size = " + ll.size() + " empty = " + ll.empty());
    }

    // "[1, 2, 3]" → {1, 2, 3} — reads a test-case list
    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "prepends", "label": "prepends", "type": "int[]", "placeholder": "[2, 3]" },
    { "id": "appends", "label": "appends", "type": "int[]", "placeholder": "[1]" },
    { "id": "pos", "label": "pos", "type": "int", "placeholder": "1" },
    { "id": "insertVal", "label": "insertVal", "type": "int", "placeholder": "8" },
    { "id": "removeVal", "label": "removeVal", "type": "int", "placeholder": "2" },
    { "id": "searchVal", "label": "searchVal", "type": "int", "placeholder": "5" }
  ],
  "cases": [
    { "args": { "prepends": "[2, 3]", "appends": "[1]", "pos": "1", "insertVal": "8", "removeVal": "2", "searchVal": "5" }, "expected": "[3, 8, 1]\nremoved = true\nfound = false\nsize = 3 empty = false" },
    { "args": { "prepends": "[]", "appends": "[]", "pos": "0", "insertVal": "7", "removeVal": "7", "searchVal": "7" }, "expected": "[]\nremoved = true\nfound = false\nsize = 0 empty = true" },
    { "args": { "prepends": "[]", "appends": "[1, 2, 3]", "pos": "100", "insertVal": "9", "removeVal": "99", "searchVal": "2" }, "expected": "[1, 2, 3, 9]\nremoved = false\nfound = true\nsize = 4 empty = false" },
    { "args": { "prepends": "[5]", "appends": "[]", "pos": "-5", "insertVal": "9", "removeVal": "5", "searchVal": "9" }, "expected": "[9]\nremoved = true\nfound = true\nsize = 1 empty = false" },
    { "args": { "prepends": "[]", "appends": "[1, 2, 2, 3]", "pos": "2", "insertVal": "2", "removeVal": "2", "searchVal": "4" }, "expected": "[1, 2, 2, 3]\nremoved = true\nfound = false\nsize = 4 empty = false" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

"Design" is the keyword that separates this from the operation-specific lessons. You're not implementing *one* operation — you're deciding **what state the class keeps** so all seven operations run efficiently and coexist correctly. Two decisions shape every linked-list class you'll ever write:

1. **Cache `size` on the object, or recompute it?** Cached — `size()` is O(1), but every insert and delete must bump a counter, and mismatched book-keeping (forgetting to decrement on a failed remove) is a classic silent bug. Recomputed — no counter to corrupt, but `size()` costs O(n).
2. **Keep a `tail` pointer, or walk to the end?** A cached `tail` makes `append` O(1), but every operation that might change the last node has to maintain it. No `tail` — `append` is O(n), one less invariant.

This challenge takes the **cached-size, no-tail** design: `head` + `currentSize` is the minimal viable state, and the O(n) `append` is a teachable weakness (the transfer challenge in Key Takeaway adds the tail).

The other thing to internalise before coding: **the head is always special.** Every other node has a predecessor whose `.next` you can redirect; the head has nothing pointing at it except the list's own `head` field. That's why `remove` checks `head.val == val` up front, and why `insert(pos ≤ 0)` collapses to `prepend` — "before the start" means "at the start", a clamp rather than an error, the same convention as negative indices in `array.slice(-1)`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python solution time=O(n) space=O(1)
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class SinglyLinkedList:
    def __init__(self):

        # Pointer to the front node of the list
        self.head = None

        # Current number of elements in the list
        self.current_size = 0

    def empty(self):
        return self.head is None

    def size(self):
        return self.current_size

    def prepend(self, val):
        new_node = ListNode(val)
        new_node.next = self.head
        self.head = new_node
        self.current_size += 1

    def append(self, val):
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

    def insert(self, position, val):

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

    def remove(self, val):

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

    def search(self, val):
        current = self.head
        while current:

            # If the val is found, return True
            if current.val == val:
                return True
            current = current.next

        # If the value is not found, return False
        return False

ll = SinglyLinkedList()
prepends = ast.literal_eval(input())     # values to prepend, in order
appends = ast.literal_eval(input())      # values to append, in order
pos = int(input())                       # insert position
insert_val = int(input())                # insert value
remove_val = int(input())                # value to remove
search_val = int(input())                # value to search for

for v in prepends:
    ll.prepend(v)
for v in appends:
    ll.append(v)
ll.insert(pos, insert_val)
removed = ll.remove(remove_val)
found = ll.search(search_val)

out = []
node = ll.head
while node:                              # traverse to collect the values in order
    out.append(node.val)
    node = node.next
print(out)
print("removed =", "true" if removed else "false")
print("found =", "true" if found else "false")
print("size =", ll.size(), "empty =", "true" if ll.empty() else "false")
```

```java solution
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode next;
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    static class SinglyLinkedList {

        // Pointer to the front node of the list
        ListNode head = null;

        // Current number of elements in the list
        int currentSize = 0;

        boolean empty() {
            return head == null;
        }

        int size() {
            return currentSize;
        }

        void prepend(int val) {
            ListNode newNode = new ListNode(val);
            newNode.next = head;
            head = newNode;
            currentSize++;
        }

        void append(int val) {
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

        void insert(int position, int val) {

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

        boolean remove(int val) {

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

        boolean search(int val) {
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
        Scanner sc = new Scanner(System.in);
        SinglyLinkedList ll = new SinglyLinkedList();
        int[] prepends = parseIntArray(sc.nextLine());           // values to prepend, in order
        int[] appends = parseIntArray(sc.nextLine());            // values to append, in order
        int pos = Integer.parseInt(sc.nextLine().trim());        // insert position
        int insertVal = Integer.parseInt(sc.nextLine().trim());  // insert value
        int removeVal = Integer.parseInt(sc.nextLine().trim());  // value to remove
        int searchVal = Integer.parseInt(sc.nextLine().trim());  // value to search for

        for (int v : prepends) ll.prepend(v);
        for (int v : appends) ll.append(v);
        ll.insert(pos, insertVal);
        boolean removed = ll.remove(removeVal);
        boolean found = ll.search(searchVal);

        List<Integer> out = new ArrayList<>();
        for (ListNode node = ll.head; node != null; node = node.next) out.add(node.val);
        System.out.println(out);
        System.out.println("removed = " + removed);
        System.out.println("found = " + found);
        System.out.println("size = " + ll.size() + " empty = " + ll.empty());
    }

    // "[1, 2, 3]" → {1, 2, 3} — reads a test-case list
    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

### Dry Run — the canonical example sequence

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

The "**all O(n)**" rows are the cost of the **no-tail** design. Caching a `tail` pointer drops `append` to O(1) time, but requires `tail` updates on every insert or delete that might change the last node.

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

You just built the linked list. Every operation from the earlier lessons is here, wired together into a single class with a cohesive API. Two lessons are worth taking away:

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
