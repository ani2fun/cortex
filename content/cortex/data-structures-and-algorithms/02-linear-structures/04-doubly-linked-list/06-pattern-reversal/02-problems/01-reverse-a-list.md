---
title: "Reverse a List"
summary: "Walk every node, swap its prev and next pointers in one stroke, and return the original tail — the new head of the reversed doubly linked list."
prereqs:
  - 06-pattern-reversal/01-pattern
difficulty: easy
kind: problem
topics: [reversal, doubly-linked-list]
---

# Reverse a list

## Problem Statement

Given the **head** of a doubly linked list, write a function to reverse the list in place and return the head of the reversed list.

You need to reverse the list in place.

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10, 3]
Output: [3, 10, 3, 7, 5]
```

**Example 2:**
```
Input:  head = [1]
Output: [1]
```

**Example 3:**
```
Input:  head = []
Output: []
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "head = [2, 4, 6, 8]",
  "options": ["[2, 4, 6, 8]", "[8, 6, 4, 2]", "[4, 2, 8, 6]", "[8, 4, 6, 2]"],
  "answer": "[8, 6, 4, 2]"
}
```

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- Reverse **in place** — `O(1)` extra space; node values must not be copied or rewritten

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, prev=None, next=None):
        self.val = val
        self.prev = prev
        self.next = next

class Solution:
    def reverse_a_list(self, head):
        # Your code goes here — swap each node's prev and next, walk via the
        # old next (now in prev), and return the old tail as the new head.
        pass

def build_list(values):              # [1, 2, 3] → 1 ⇄ 2 ⇄ 3
    head = tail = None
    for v in values:
        node = ListNode(v, prev=tail)
        if tail is not None:
            tail.next = node
        else:
            head = node
        tail = node
    return head

def print_list(head):                # 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
    out = []
    while head:
        out.append(head.val)
        head = head.next
    print(out)

head = build_list(ast.literal_eval(input()))   # the test case's head
print_list(Solution().reverse_a_list(head))
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode prev, next;
        ListNode(int val) { this.val = val; }
    }

    static class Solution {
        ListNode reverseAList(ListNode head) {
            // Your code goes here — swap each node's prev and next, walk via the
            // old next (now in prev), and return the old tail as the new head.
            return null;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        printList(new Solution().reverseAList(head));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 ⇄ 2 ⇄ 3
        ListNode head = null, tail = null;
        for (int v : values) {
            ListNode node = new ListNode(v);
            node.prev = tail;
            if (tail != null) tail.next = node;
            else head = node;
            tail = node;
        }
        return head;
    }

    static void printList(ListNode head) {         // 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
        List<Integer> out = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) out.add(n.val);
        System.out.println(out);
    }

    // "[1, 2, 3]" → {1, 2, 3} — reads the test case's head
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[5, 7, 3, 10, 3]" }
  ],
  "cases": [
    { "args": { "head": "[5, 7, 3, 10, 3]" }, "expected": "[3, 10, 3, 7, 5]" },
    { "args": { "head": "[1]" }, "expected": "[1]" },
    { "args": { "head": "[]" }, "expected": "[]" },
    { "args": { "head": "[1, 2]" }, "expected": "[2, 1]" },
    { "args": { "head": "[1, 2, 3, 4]" }, "expected": "[4, 3, 2, 1]" },
    { "args": { "head": "[5, 5, 5]" }, "expected": "[5, 5, 5]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reversal problem is that the entire list is a single contiguous segment whose `prev` and `next` pointers all need to swap. A doubly linked list already encodes both directions — node `i` knows about both node `i-1` and node `i+1` — so reversing it does not require reconstructing a missing back-link. The work is purely structural; node values are never read or written. After the swap loop runs, every node's old `next` sits in `prev` and every node's old `prev` sits in `next`, which is exactly the reversed list.

The **pointer placement** is the simplest of the whole pattern. A single cursor `current` walks the list, plus an optional `previous` reference that trails one step behind. Per tick, the body has two operations in this order: save `current.next` into a local `next_node` (because the swap is about to overwrite it), then swap `current.prev` and `current.next`. Advancing is `current = next_node` — equivalent to following the original forward chain. When `current` falls off the end, `previous` is sitting on the original tail, which is the reversed list's new head.

What **breaks if you reach for a naive approach**? Copying values into an array, reversing the array, and writing values back works in `O(n)` time but uses `O(n)` extra space — and it does not generalise to splicing a reversed segment back into a larger list. Recursion runs the same algorithm in `O(n)` time but uses `O(n)` stack space; for a 10-million-node list that overflows the default stack on most language runtimes. Only the iterative per-node swap hits the `O(n)` time / `O(1)` space target.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse a List |
|---|---|
| **Q1.** Does the problem ask for reversed order across a contiguous segment? | **Yes** — the entire list is the segment, from `head` to the tail. |
| **Q2.** Are the segment endpoints identifiable? | **Yes** — `start = head`, and the end is reached when `current` becomes `null`. No positional walk is needed. |
| **Q3.** Is the work strictly structural (only `prev`/`next` pointers change)? | **Yes** — node values are never inspected; only the two pointer fields swap each tick. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — a constant number of references (`current`, `previous`, `next_node`) regardless of list length. |

</details>
<details>
<summary><h2>Brute Force: Copy Values into an Array</h2></summary>


Walk the list and append each value to a Python list (or Java `ArrayList`). Reverse the array. Walk the list a second time and assign each reversed value back into the corresponding node's `val` field. Neither direction of the pointer chain (`prev` or `next`) is touched; only the values move.

This is correct but costs `O(n)` time AND `O(n)` extra space. It also misses the point of the pattern: reversal is a pointer-rewiring operation, not a value-shuffling operation. The moment a later problem asks you to reverse a sublist and splice it back, the array-copy approach has no natural place to slot in the splice.

</details>
<details>
<summary><h2>Key Insight: Swap `prev` and `next` on Every Node</h2></summary>


The chain `5 ⇄ 7 ⇄ 3 ⇄ 10 ⇄ 3` becomes `3 ⇄ 10 ⇄ 3 ⇄ 7 ⇄ 5` by swapping every node's two pointer fields. The catch is that the swap is about to overwrite `current.next`, so the forward link must be saved into a local variable *before* the swap if we want a clean way to advance. Three references suffice: `current` (the node being swapped), `next_node` (the snapshot of `current.next` taken at the top of the tick), and `previous` (which tracks the most recently swapped node so we can return it as the new head when the walk ends).

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the per-node swap loop from the head until `current` becomes `null`.

1. **Handle the trivial guards.** If `head` is `null`, or the list has a single node (`head.prev is null AND head.next is null`), return `head` unchanged. The single-node case is its own reverse.
2. **Initialise the references.** Set `current = head` and `previous = null`. The original `head` reference will become the new tail after the swap; `previous` will track the most recently swapped node.
3. **Snapshot the forward link.** Inside the loop body, set `next_node = current.next` *first*, before any swap. This is the only line that keeps the rest of the list reachable after the swap on the next line clobbers `current.next`.
4. **Swap `prev` and `next` on the current node.** A single assignment `current.prev, current.next = current.next, current.prev` (or a three-line temp-variable swap in Java) flips both pointer fields. Forward and backward chains both reverse at this node.
5. **Advance both references.** Set `previous = current`, then `current = next_node`. The `previous` reference is the new head candidate (it will be the loop's final value); the `current` reference walks forward through the original list using the snapshot.
6. **Return the new head.** When the loop exits (`current is null`), `previous` is the former tail and the new head of the reversed list. Return it.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution


```python solution time=O(n) space=O(1)
import ast


class ListNode:
    def __init__(self, val, prev=None, next=None):
        self.val = val
        self.prev = prev
        self.next = next


class Solution:
    def reverse_a_list(self, head):

        # If the head is null or if it's the only node in the list,
        # return the head as it is
        if head is None or (head.prev is None and head.next is None):
            return head

        # Pointer to track the current node
        current = head

        # Pointer to track the previous node
        previous = None

        while current is not None:

            # Save the address of next node
            next_node = current.next

            # Swap the previous and next nodes pointers of the current
            # node
            current.prev, current.next = current.next, current.prev

            # Store the previous node in the previous pointer
            previous = current

            # Move the current pointer to the next node
            current = next_node

        # Return the new head, which is stored in the previous pointer
        return previous


def build_list(values):              # [1, 2, 3] → 1 ⇄ 2 ⇄ 3
    head = tail = None
    for v in values:
        node = ListNode(v, prev=tail)
        if tail is not None:
            tail.next = node
        else:
            head = node
        tail = node
    return head


def print_list(head):                # 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
    out = []
    while head:
        out.append(head.val)
        head = head.next
    print(out)


head = build_list(ast.literal_eval(input()))   # the test case's head
print_list(Solution().reverse_a_list(head))
```

```java solution
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode prev, next;
        ListNode(int val) { this.val = val; }
    }

    static class Solution {
        ListNode reverseAList(ListNode head) {

            // If the head is null or if it's the only node in the list,
            // return the head as it is
            if (head == null || (head.prev == null && head.next == null)) {
                return head;
            }

            // Pointer to track the current node
            ListNode current = head;

            // Pointer to track the previous node
            ListNode previous = null;

            while (current != null) {

                // Save the address of next node
                ListNode next = current.next;

                // Swap the previous and next nodes pointers of the current
                // node
                ListNode temp = current.prev;
                current.prev = current.next;
                current.next = temp;

                // Store the previous node in the previous pointer
                previous = current;

                // Move the current pointer to the next node
                current = next;
            }

            // Return the new head, which is stored in the previous pointer
            return previous;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        printList(new Solution().reverseAList(head));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 ⇄ 2 ⇄ 3
        ListNode head = null, tail = null;
        for (int v : values) {
            ListNode node = new ListNode(v);
            node.prev = tail;
            if (tail != null) tail.next = node;
            else head = node;
            tail = node;
        }
        return head;
    }

    static void printList(ListNode head) {         // 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
        List<Integer> out = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) out.add(n.val);
        System.out.println(out);
    }

    // "[1, 2, 3]" → {1, 2, 3} — reads the test case's head
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


### Dry Run

```
Initial: head → 5 ⇄ 7 ⇄ 3 ⇄ 10 ⇄ 3,   current = 5,   previous = null

Step 1 │ current = 5      │ next_node = 7   │ swap 5:  prev 7, next null  │ previous = 5,  current = 7
Step 2 │ current = 7      │ next_node = 3   │ swap 7:  prev 3, next 5     │ previous = 7,  current = 3
Step 3 │ current = 3 (m)  │ next_node = 10  │ swap 3:  prev 10, next 7    │ previous = 3,  current = 10
Step 4 │ current = 10     │ next_node = 3   │ swap 10: prev 3, next 3     │ previous = 10, current = 3
Step 5 │ current = 3 (t)  │ next_node = null│ swap 3:  prev null, next 10 │ previous = 3,  current = null
Done   │ current == null — return previous (= 3, original tail)

Result: head → 3 ⇄ 10 ⇄ 3 ⇄ 7 ⇄ 5  ✓
```

Each step swaps the node's `prev` and `next` in one stroke — `next_node` is saved *before* the swap because the swap overwrites `current.next`. Both chains flip together: the original tail ends with `prev = null` (it is the new head), and `previous` walks one step behind `current`, so it lands on that tail the moment `current` falls off the end.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | Every node is visited exactly once — one snapshot, one swap, one advance per node. |
| **Space** | `O(1)` | Three local references (`current`, `previous`, `next_node`) regardless of list length. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | The trivial guard fires immediately; `null` is returned without entering the loop. |
| Single node | The trivial guard catches `head.prev is null AND head.next is null` and returns `head` unchanged — a single node is its own reverse. |
| Two nodes (`a ⇄ b`) | Tick 1 swaps `a`'s pointers (`prev = b, next = null`); tick 2 swaps `b`'s pointers (`prev = null, next = a`). Return `b`. |
| All equal values (`5 ⇄ 5 ⇄ 5`) | Values are never inspected; the same three swaps run, producing a list with identical values but reversed node identity. |
| Palindrome by value (`1 ⇄ 2 ⇄ 3 ⇄ 2 ⇄ 1`) | The algorithm still swaps every node; the result reads identically but every node is now in the mirror position. |
| Very long list (10⁷ nodes) | Iterative `O(1)` space — no stack overflow. The recursive variant would crash here. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The full-list reversal is the simplest instance of the pattern — `start = head`, `end = tail`, swap every node's `prev` and `next` exactly once. Because a doubly linked list already encodes both directions, the loop body collapses to one swap per node — no three-pointer dance is needed.

</details>
