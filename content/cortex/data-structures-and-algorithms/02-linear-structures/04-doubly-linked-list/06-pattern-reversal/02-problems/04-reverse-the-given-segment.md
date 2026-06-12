---
title: "Reverse the Given Segment"
summary: "Walk to find start and end by 1-indexed positions, call the segment-reversal primitive, and return end as the new head when left == 1 — otherwise the head is unchanged."
prereqs:
  - 06-pattern-reversal/01-pattern
difficulty: medium
kind: problem
topics: [reversal, doubly-linked-list]
---

# Reverse the given segment

## Problem Statement

Given the **head** of a doubly linked list and two integers **left** and **right** with **left ≤ right**, reverse the nodes from position `left` to position `right` (inclusive, 1-indexed) and return the head of the resulting list.

You need to reverse the segment in place.

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10, 6], left = 2, right = 4
Output: [5, 10, 3, 7, 6]

Explanation: Nodes at positions 2..4 ([7, 3, 10]) are reversed to [10, 3, 7].
```

**Example 2:**
```
Input:  head = [5], left = 1, right = 1
Output: [5]

Explanation: Reversing a single node is a no-op.
```

**Example 3:**
```
Input:  head = [1, 2, 3, 4, 5], left = 1, right = 5
Output: [5, 4, 3, 2, 1]

Explanation: left = 1, right = n is the full-list reversal special case.
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "head = [1, 2, 3, 4, 5], left = 2, right = 4",
  "options": ["[1, 2, 3, 4, 5]", "[1, 4, 3, 2, 5]", "[4, 3, 2, 1, 5]", "[1, 3, 4, 2, 5]"],
  "answer": "[1, 4, 3, 2, 5]"
}
```

## Constraints

- `1 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `1 ≤ left ≤ right ≤ list length`
- Reverse **in place** — `O(1)` extra space; node values must not be copied or rewritten

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, prev=None, next=None):
        self.val = val
        self.prev = prev
        self.next = next

class Solution:
    def reverse_the_given_segment(self, head, left, right):
        # Your code goes here — walk to start (pos left) and end (pos right),
        # call the segment-reversal primitive, return end if left == 1 else head.
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
left = int(input())
right = int(input())
print_list(Solution().reverse_the_given_segment(head, left, right))
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode prev, next;
        ListNode(int val) { this.val = val; }
    }

    static class Solution {
        ListNode reverseTheGivenSegment(ListNode head, int left, int right) {
            // Your code goes here — walk to start (pos left) and end (pos right),
            // call the segment-reversal primitive, return end if left == 1 else head.
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int left = Integer.parseInt(sc.nextLine().trim());
        int right = Integer.parseInt(sc.nextLine().trim());
        printList(new Solution().reverseTheGivenSegment(head, left, right));
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[5, 7, 3, 10, 6]" },
    { "id": "left", "label": "left", "type": "int", "placeholder": "2" },
    { "id": "right", "label": "right", "type": "int", "placeholder": "4" }
  ],
  "cases": [
    { "args": { "head": "[5, 7, 3, 10, 6]", "left": "2", "right": "4" }, "expected": "[5, 10, 3, 7, 6]" },
    { "args": { "head": "[5]", "left": "1", "right": "1" }, "expected": "[5]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "left": "1", "right": "5" }, "expected": "[5, 4, 3, 2, 1]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "left": "1", "right": "3" }, "expected": "[3, 2, 1, 4, 5]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "left": "3", "right": "5" }, "expected": "[1, 2, 5, 4, 3]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "left": "2", "right": "2" }, "expected": "[1, 2, 3, 4, 5]" },
    { "args": { "head": "[1, 2]", "left": "1", "right": "2" }, "expected": "[2, 1]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "left": "2", "right": "4" }, "expected": "[1, 4, 3, 2, 5]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** is the generic case of the pattern: a contiguous interior segment `[left, right]` needs both pointer fields swapped on every node, while the prefix (positions `1..left − 1`) and the suffix (positions `right + 1..n`) stay in place. Four stitches are needed at the boundaries — the prefix's last node points forward to the reversed segment's new head (and the new head points back), and the reversed segment's new tail points forward to the suffix's first node (and the suffix's first node points back). The segment-reversal primitive from the pattern lesson handles all four stitches internally because it captures `leftBound = start.prev` and `rightBound = end.next` before swapping.

The **pointer placement** requires two positional walks. Walk to position `left` to capture `start`. Walk to position `right` to capture `end`. Hand both to the segment-reversal primitive, which performs the per-node swaps and then stitches the four boundary pointers. When `left == 1` the original head is inside the reversed segment, so its identity as "the list head" transfers to `end` (the segment's new head); the function returns `end` in that case and `head` otherwise. The `start == end` and single-node cases short-circuit early because a single-node segment is its own reverse.

What **breaks if you reach for a naive iterate-and-reverse without capturing endpoints first**? A single-pass solution that swaps pointers as it walks loses access to the prefix's last node and the suffix's first node the moment it advances past them. Because the segment-reversal primitive depends on `leftBound` and `rightBound` being captured *before* any swap (the swaps overwrite `start.prev` and `end.next`), folding the position walk into the swap loop forces a different — and harder to get right — algorithm. The explicit endpoint-then-reverse approach reuses the pattern's primitive without modification.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse the Given Segment |
|---|---|
| **Q1.** Does the problem ask for reversed order across a contiguous segment? | **Yes** — the segment `[left, right]` is contiguous and 1-indexed; everything outside stays in place. |
| **Q2.** Are the segment endpoints identifiable? | **Yes** — `start` is the node at position `left` (found by walking `left − 1` steps), `end` is the node at position `right` (found by walking `right − 1` steps). |
| **Q3.** Is the work strictly structural (only `prev`/`next` pointers change)? | **Yes** — values are never inspected; only the two pointer fields swap inside the segment, plus four boundary writes from the primitive. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — a constant number of references (`start`, `end`, plus the primitive's `leftBound`, `rightBound`, and `current`) regardless of `n`, `left`, or `right`. |

</details>
<details>
<summary><h2>Brute Force: Two-Pass Value Shuffle</h2></summary>


Walk the list and copy positions `left..right` into an array. Reverse the array. Walk the list a second time and write the reversed values back into those positions. `O(n)` time, `O(right − left + 1)` extra space — and once again misses the pattern's lesson that order is encoded in `prev`/`next` pointers, not in values.

</details>
<details>
<summary><h2>Key Insight: Capture Endpoints, Call the Segment-Reversal Primitive, Adjust the Head</h2></summary>


The segment-reversal primitive (from the pattern lesson) takes `start` and `end` and stitches all four boundary pointers internally — it captures `leftBound = start.prev` and `rightBound = end.next` *before* swapping, then writes both halves of the `leftBound ↔ end` and `start ↔ rightBound` connections at the end. The caller only needs to (a) locate `start` and `end`, (b) call the primitive, and (c) decide whether the head of the whole list shifted. The head shifts only when `left == 1`, because that's the case where the original `head` was inside the reversed segment; in that case the new head of the list is `end`.

</details>
<details>
<summary><h2>Approach</h2></summary>


Capture endpoints, reverse, adjust the head reference.

1. **Handle the no-op guards.** If `head` is `null`, or the list has one node (`head.next is null`), or `left == right`, return `head` unchanged. A single-node segment is its own reverse.
2. **Walk to `start` (position `left`).** Use the `get_node_at_position` helper — a counted forward walk that advances from `head` by `left − 1` steps.
3. **Walk to `end` (position `right`).** Same helper, advancing by `right − 1` steps. The helper restarts from `head`, so the two walks are independent.
4. **Call the segment-reversal primitive.** Invoke `reverse(start, end)`. The primitive captures `leftBound` and `rightBound` before any swap, runs the per-node swap loop until `current == rightBound`, then writes the four boundary stitches with null guards for `leftBound` and `rightBound`.
5. **Return the appropriate head.** If `left == 1`, the original head was inside the reversed segment — the new head of the whole list is `end` (the segment's new head). Otherwise the prefix is intact and `head` is unchanged.

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
    def get_node_at_position(self, head, position):
        current = head
        for _ in range(1, position):
            if current is None:
                break
            current = current.next
        return current

    def reverse(self, start, end):

        # If the start is null or start is the end, there's nothing to
        # reverse
        if start is None or start == end:
            return

        # Pointers to keep track of the bounds
        left_bound = start.prev
        right_bound = end.next
        current = start
        previous = left_bound

        # Reverse nodes until the right boundary
        while current != right_bound:

            # Save the address of next node
            next_node = current.next

            # Swap the previous and next nodes pointers of the current
            # node
            current.prev, current.next = current.next, current.prev

            # Store the previous node in the previous pointer
            previous = current

            # Move the current pointer to the next node
            current = next_node

        # Adjust connections with the new boundaries
        start.next = right_bound
        if right_bound:
            right_bound.prev = start

        end.prev = left_bound
        if left_bound:
            left_bound.next = end

    def reverse_the_given_segment(self, head, left, right):

        # Handle cases where reversal is not needed
        if head is None or head.next is None or left == right:
            return head

        # Get the node at the 'left' position
        start = self.get_node_at_position(head, left)

        # Get the node at the 'right' position
        end = self.get_node_at_position(head, right)

        # Reverse the segment between start and end
        self.reverse(start, end)

        # Return the new head if the reversal included the head node
        return end if left == 1 else head

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
left = int(input())
right = int(input())
print_list(Solution().reverse_the_given_segment(head, left, right))
```

```java solution
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode prev, next;
        ListNode(int val) { this.val = val; }
    }

    static class Solution {
        private ListNode getNodeAtPosition(ListNode head, int position) {
            ListNode current = head;
            for (int i = 1; i < position; ++i) {
                current = current.next;
            }
            return current;
        }

        private void reverse(ListNode start, ListNode end) {

            // If the start is null or start is the end, there's nothing to
            // reverse
            if (start == null || start == end) {
                return;
            }

            // Pointers to keep track of the bounds
            ListNode leftBound = start.prev;
            ListNode rightBound = end.next;
            ListNode current = start;
            ListNode previous = leftBound;

            // Reverse nodes until the right boundary
            while (current != rightBound) {

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

            // Adjust connections with the new boundaries
            start.next = rightBound;
            if (rightBound != null) {
                rightBound.prev = start;
            }

            end.prev = leftBound;
            if (leftBound != null) {
                leftBound.next = end;
            }
        }

        ListNode reverseTheGivenSegment(ListNode head, int left, int right) {

            // Handle cases where reversal is not needed
            if (head == null || head.next == null || left == right) {
                return head;
            }

            // Get the node at the 'left' position
            ListNode start = getNodeAtPosition(head, left);

            // Get the node at the 'right' position
            ListNode end = getNodeAtPosition(head, right);

            // Reverse the segment between start and end
            reverse(start, end);

            // Return the new head if the reversal included the head node
            return left == 1 ? end : head;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int left = Integer.parseInt(sc.nextLine().trim());
        int right = Integer.parseInt(sc.nextLine().trim());
        printList(new Solution().reverseTheGivenSegment(head, left, right));
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
Setup: left = 2, right = 4, left != right → proceed
  start = get_node_at_position(head, left = 2)  = node(7)
  end   = get_node_at_position(head, right = 4) = node(10)

reverse(start = 7, end = 10):
  left_bound  = start.prev = node(5)   right_bound = end.next = node(6)
  current = 7   │ next_node = 3   │ swap 7:  prev 3, next 5    │ current = 3
  current = 3   │ next_node = 10  │ swap 3:  prev 10, next 7   │ current = 10
  current = 10  │ next_node = 6   │ swap 10: prev 6, next 3    │ current = 6
  current == right_bound (6). Stop.
  Stitch tail:  start.next = right_bound → 7.next = node(6); right_bound.prev = start → 6.prev = node(7)
  Stitch head:  end.prev = left_bound   → 10.prev = node(5); left_bound.next = end  → 5.next = node(10)

left == 1 is false → return original head (5).

Result: 5 ⇄ 10 ⇄ 3 ⇄ 7 ⇄ 6  ✓
```

Phase 1 swaps `prev`/`next` on every interior node; phase 2 then re-stitches all four boundary pointers — `start.next`/`right_bound.prev` on the right and `end.prev`/`left_bound.next` on the left — so both directions stay consistent. Capturing `left_bound` and `right_bound` *before* the swaps begin is what keeps the algorithm bug-free.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` worst case | Two positional walks (up to `right − 1` and `left − 1` steps) plus one segment reversal of `right − left + 1` nodes. Sum dominated by `O(n)`. |
| **Space** | `O(1)` | Constant references (`start`, `end`, plus the primitive's `leftBound`, `rightBound`, `current`) regardless of input size. |

### Edge Cases

| Case | What happens |
|---|---|
| `head is null` | Early return on the guard; `null` is returned. |
| Single-node list (`head.next is null`) | Early return on the guard; `head` is returned unchanged. |
| `left == right` | Early return on the guard; the segment is a single node, which is a reversal no-op. |
| `left == 1, right == n` | The primitive runs with `leftBound = null` and `rightBound = null`; the four stitches collapse to clearing `start.next` and `end.prev`. Return `end` as the new head. |
| `left == 1, right < n` | `leftBound = null`; only the right-side stitches write to the suffix. Return `end` as the new head. |
| `left > 1, right == n` | `rightBound = null`; only the left-side stitches write to the prefix. Return the original `head`. |
| Two nodes, `left = 1, right = 2` | Both `leftBound` and `rightBound` are `null`; the primitive performs the two swaps and returns; we return `end` as the new head. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Reversal in a doubly linked list is a one-line idea — **swap `prev` and `next` on every node in the segment, then re-stitch the boundaries** — wrapped in a thin O(N) walk. Compared to a singly linked list, where you needed three pointers locked in a delicate dance, the DLL version is barely an algorithm; it's a habit. The same primitive solves four problems we just saw and a much longer list of indirect ones (rotate-by-K, reorder, palindrome check, undo-stacks, k-group rewinds). Once you recognise a problem as a "reverse this slice" problem, the implementation writes itself.

The two pieces of discipline to internalise:

1. **Save before clobber.** Capture `leftBound` and `rightBound` *before* any swap. Once swaps begin, the meanings of `start.prev` and `end.next` shift under your feet.
2. **Mirror every link.** Every connection in a DLL is two pointers. Set both, every time, or backward traversal silently breaks.

> **Transfer challenge** — Given the head of a doubly linked list and a positive integer K, reverse every consecutive group of K nodes (the last group may be shorter than K and should be reversed as well). Return the head. *Hint: you already have all the pieces.*

<details>
<summary><strong>Solution sketch</strong></summary>

Walk the list. At each step, locate `start` (current group's first node) and `end` (current group's K-th node, or the actual tail if fewer than K nodes remain). Apply the segment-reversal primitive on `(start, end)`. Track whether the very first group included the original head — that group's reversal returns the new head of the entire list. Move `current` to `start.next` (which is now the first node of the next group, post-stitch) and repeat. Each node is touched O(1) times across the whole algorithm, so the total cost is O(N).

The only new thing here is the *bookkeeping*, not the reversal itself — you've already seen that primitive four times.

</details>

Next time you see a problem that asks you to flip a chunk of a linked list — any chunk, anywhere — you won't reach for three pointers and a stack of temporary variables. You'll reach for one swap, one walk, and four boundary stitches. The reversal pattern stops being a trick and becomes a reflex.

Positional segment reversal composes two positional walks with the segment-reversal primitive. The primitive's `leftBound = start.prev` / `rightBound = end.next` capture (before any swap) handles all four boundary stitches for free; the caller only writes the position walks and adjusts the head reference when `left == 1`.

</details>
