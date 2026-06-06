---
title: "Reverse the Given Segment"
summary: "Given the head of a singly linked list and two integers left and right where left <= right. Write a function to reverse the list nodes from the position left to the right and return the head of the re"
prereqs:
  - 08-pattern-reversal/01-pattern
difficulty: medium
---

# Reverse the given segment

## Problem Statement

Given the **head** of a singly linked list and two integers **left** and **right** where **left <= right**, write a function to reverse the list nodes from position `left` to position `right` (inclusive, 1-indexed) and return the head of the modified list.

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


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** is the generic case of the pattern: a contiguous interior segment `[left, right]` needs to flip direction, while the prefix (positions `1..left − 1`) and the suffix (positions `right + 1..n`) stay in place. Two stitches are needed: the prefix's last node must point to the reversed segment's new head, and the reversed segment's new tail must point to the suffix's first node. The segment-reversal loop from the pattern lesson handles the second stitch for free — initialising `previous = rightBound = end.next` makes the reversed tail point at the suffix's first node automatically.

The **pointer placement** requires two positional walks. Walk to position `right` to capture the `end` node. Walk to position `left − 1` to capture `leftBound`, the prefix's last node, so the prefix-side stitch is available after the reversal. The segment's first node is `leftBound.next`. The reversal helper (from the pattern lesson) takes `start` and `end` and returns the new head of the reversed segment. Then `leftBound.next = newHead` performs the prefix stitch. The special case `left == 1` has no prefix, so the returned new head becomes the new head of the entire list.

What **breaks if you reach for a naive iterate-and-reverse without capturing endpoints first**? A single-pass solution that flips pointers as it walks loses access to the prefix's last node the moment it advances past it. The classic "head insertion" variant (used in some textbook solutions) keeps a dummy node and rebuilds the segment by repeated head insertions in `O(right − left + 1)` time — correct but harder to reason about than the explicit endpoint-then-reverse approach, which reuses the segment-reversal helper without modification.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse the Given Segment |
|---|---|
| **Q1.** Does the problem ask for reversed order across a contiguous segment? | **Yes** — the segment `[left, right]` is contiguous and 1-indexed; everything outside stays in place. |
| **Q2.** Are the segment endpoints identifiable? | **Yes** — `start` is the node at position `left` (found by walking `left − 1` steps), `end` is the node at position `right` (found by walking `right − 1` steps). |
| **Q3.** Is the work strictly structural (only `next` pointers change)? | **Yes** — values are never inspected; only `current.next` flips during the loop, plus one prefix-stitching assignment. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — a constant number of references (`leftBound`, `start`, `end`, plus the helper's three) regardless of `n`, `left`, or `right`. |

</details>
<details>
<summary><h2>Brute Force: Two-Pass Value Shuffle</h2></summary>


Walk the list and copy positions `left..right` into an array. Reverse the array. Walk the list a second time and write the reversed values back into those positions. `O(n)` time, `O(right − left + 1)` extra space — and once again misses the pattern's lesson that order is encoded in `next` pointers, not in values.

</details>
<details>
<summary><h2>Key Insight: Capture Endpoints, Call the Segment-Reversal Helper, Stitch the Prefix</h2></summary>


The segment-reversal helper (from the pattern lesson) takes `start` and `end` and returns the new head of the reversed segment. It captures `rightBound = end.next` internally and initialises `previous = rightBound`, so the reversed segment's tail automatically points to the suffix's first node — the second stitch is built into the helper. The only remaining work for the caller is the prefix stitch: `leftBound.next = newHead`. The full-list special case (`left == 1`) has no prefix; in that case the helper's returned new head replaces the list's head.

</details>
<details>
<summary><h2>Approach</h2></summary>


Capture endpoints, reverse, stitch.

1. **Handle the no-op guards.** If `head` is `null`, or the list has one node, or `left == right`, return `head` unchanged.
2. **Walk to `end` (position `right`).** Advance from `head` by `right − 1` steps. The `get_node_at_position` helper does this — a counted forward walk.
3. **Handle the prefix-less special case.** If `left == 1`, there is no prefix, so the segment starts at `head`. Call the reversal helper with `start = head` and `end`, and return its result directly as the new head.
4. **Walk to `leftBound` (position `left − 1`).** Advance from `head` by `left − 2` steps to land on the prefix's last node. Its `.next` is the segment's first node — `start`.
5. **Reverse the segment and stitch the prefix.** Call the reversal helper with `start` and `end`. Assign `leftBound.next = newHead`. The segment-reversal helper already handled the suffix stitch by initialising `previous = end.next` before the loop.
6. **Return the original head.** Because the prefix is intact (we never reassigned `head`), the original head is still the head of the modified list.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution


```python run viz=linked-list viz-root=head
from typing import Optional


class ListNode:
    def __init__(self, val=0, nxt=None):
        self.val = val
        self.next = nxt


def from_list(values):
    if not values:
        return None
    head = ListNode(values[0])
    cur = head
    for v in values[1:]:
        cur.next = ListNode(v)
        cur = cur.next
    return head


def to_list(head):
    out = []
    while head is not None:
        out.append(head.val)
        head = head.next
    return out


class Solution:
    def get_node_at_position(
        self, head: Optional[ListNode], position: int
    ) -> Optional[ListNode]:
        current = head
        for i in range(1, position):
            current = current.next
        return current

    def reverse(
        self, start: Optional[ListNode], end: Optional[ListNode]
    ) -> Optional[ListNode]:
        current: Optional[ListNode] = start
        right_bound: Optional[ListNode] = end.next
        previous: Optional[ListNode] = right_bound

        while current != right_bound:
            next_node = current.next
            current.next = previous
            previous = current
            current = next_node

        return previous

    def reverse_the_given_segment(
        self, head: Optional[ListNode], left: int, right: int
    ) -> Optional[ListNode]:

        # Handle cases where reversal is not needed
        if head is None or head.next is None or left == right:
            return head

        # Get the end node of the segment
        end = self.get_node_at_position(head, right)

        # If the left position is 1, reverse from the head
        if left == 1:
            return self.reverse(head, end)

        # Get the node before the 'left' position to connect after
        # reversal
        left_bound = self.get_node_at_position(head, left - 1)

        # Node at the start of the segment to reverse
        start = left_bound.next

        # Reverse the segment and connect to the left_bound node
        left_bound.next = self.reverse(start, end)

        # Return the modified list
        return head


# Examples from the problem statement
print(to_list(Solution().reverse_the_given_segment(from_list([5, 7, 3, 10, 6]), 2, 4)))  # [5, 10, 3, 7, 6]
print(to_list(Solution().reverse_the_given_segment(from_list([5]), 1, 1)))                # [5]

# Edge cases
print(to_list(Solution().reverse_the_given_segment(None, 1, 1)))                          # []
print(to_list(Solution().reverse_the_given_segment(from_list([1, 2]), 1, 2)))             # [2, 1]
print(to_list(Solution().reverse_the_given_segment(from_list([1, 2, 3, 4, 5]), 1, 5)))   # [5, 4, 3, 2, 1]
print(to_list(Solution().reverse_the_given_segment(from_list([1, 2, 3, 4, 5]), 2, 2)))   # [1, 2, 3, 4, 5]
print(to_list(Solution().reverse_the_given_segment(from_list([1, 2, 3, 4, 5]), 1, 3)))   # [3, 2, 1, 4, 5]
print(to_list(Solution().reverse_the_given_segment(from_list([1, 2, 3, 4, 5]), 3, 5)))   # [1, 2, 5, 4, 3]
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    static ListNode fromList(int... values) {
        if (values.length == 0) return null;
        ListNode head = new ListNode(values[0]);
        ListNode cur = head;
        for (int i = 1; i < values.length; i++) {
            cur.next = new ListNode(values[i]);
            cur = cur.next;
        }
        return head;
    }

    static List<Integer> toList(ListNode head) {
        List<Integer> out = new ArrayList<>();
        while (head != null) { out.add(head.val); head = head.next; }
        return out;
    }

    static class Solution {
        private ListNode getNodeAtPosition(ListNode head, int position) {
            ListNode current = head;
            for (int i = 1; i < position; ++i) {
                current = current.next;
            }
            return current;
        }

        private ListNode reverse(ListNode start, ListNode end) {
            ListNode current = start;
            ListNode rightBound = end.next;
            ListNode previous = rightBound;

            while (current != rightBound) {
                ListNode next = current.next;
                current.next = previous;
                previous = current;
                current = next;
            }

            return previous;
        }

        public ListNode reverseTheGivenSegment(
            ListNode head,
            int left,
            int right
        ) {

            // Handle cases where reversal is not needed
            if (head == null || head.next == null || left == right) {
                return head;
            }

            // Get the end node of the segment
            ListNode end = getNodeAtPosition(head, right);

            // If the left position is 1, reverse from the head
            if (left == 1) {
                return reverse(head, end);
            }

            // Get the node before the 'left' position to connect after
            // reversal
            ListNode leftBound = getNodeAtPosition(head, left - 1);

            // Node at the start of the segment to reverse
            ListNode start = leftBound.next;

            // Reverse the segment and connect to the leftBound node
            leftBound.next = reverse(start, end);

            // Return the modified list
            return head;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(toList(new Solution().reverseTheGivenSegment(fromList(5, 7, 3, 10, 6), 2, 4))); // [5, 10, 3, 7, 6]
        System.out.println(toList(new Solution().reverseTheGivenSegment(fromList(5), 1, 1)));               // [5]

        // Edge cases
        System.out.println(toList(new Solution().reverseTheGivenSegment(null, 1, 1)));                      // []
        System.out.println(toList(new Solution().reverseTheGivenSegment(fromList(1, 2), 1, 2)));            // [2, 1]
        System.out.println(toList(new Solution().reverseTheGivenSegment(fromList(1, 2, 3, 4, 5), 1, 5)));  // [5, 4, 3, 2, 1]
        System.out.println(toList(new Solution().reverseTheGivenSegment(fromList(1, 2, 3, 4, 5), 2, 2)));  // [1, 2, 3, 4, 5]
        System.out.println(toList(new Solution().reverseTheGivenSegment(fromList(1, 2, 3, 4, 5), 1, 3)));  // [3, 2, 1, 4, 5]
        System.out.println(toList(new Solution().reverseTheGivenSegment(fromList(1, 2, 3, 4, 5), 3, 5)));  // [1, 2, 5, 4, 3]
    }
}
```

### Dry Run

```
head = 5 → 7 → 3 → 10 → 6 → null,  left = 2,  right = 4

Step 1: Guards — head non-null, head.next non-null, left != right. Proceed.

Step 2: Walk to end (position right = 4).
  get_node_at_position(head, 4): walk 3 steps. end = node 10.

Step 3: left (= 2) != 1, so no prefix-less shortcut.

Step 4: Walk to leftBound (position left - 1 = 1).
  get_node_at_position(head, 1): walk 0 steps. leftBound = node 5.
  start = leftBound.next = node 7.

Step 5: Reverse segment [start = 7, end = 10].
  Inside reverse(7, 10):
    rightBound = end.next = node 6
    previous   = rightBound = node 6
    current    = start = 7

    Tick 1: next = 3,  7.next = 6,    previous = 7,  current = 3
    Tick 2: next = 10, 3.next = 7,    previous = 3,  current = 10
    Tick 3: next = 6,  10.next = 3,   previous = 10, current = 6
    current == rightBound → loop ends.
    Return previous = 10.

  Segment is now 10 → 3 → 7 → 6 (the reversed segment's tail 7 already points to suffix start 6).

Step 6: leftBound.next = 10  (prefix stitch: node 5's next is the new segment head).
  Full list: 5 → 10 → 3 → 7 → 6 → null ✓

Return head = 5.
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` worst case | Two positional walks (up to `right − 1` and `left − 2` steps) plus one segment reversal of `right − left + 1` nodes. Sum dominated by `O(n)`. |
| **Space** | `O(1)` | Constant references (`leftBound`, `end`, `start`, plus the helper's three) regardless of input size. |

### Edge Cases

| Case | What happens |
|---|---|
| `head is null` | Early return on the guard; `null` is returned. |
| Single-node list | Early return on `head.next is None`; `head` is returned unchanged. |
| `left == right` | Early return on the guard; the segment is a single node, which is a reversal no-op. |
| `left == 1, right == n` | The prefix-less branch fires; the helper reverses the entire list and returns the new head directly. |
| `left == 1, right < n` | Prefix-less branch fires; the helper reverses the prefix segment and returns its new head as the new list head. The helper's `rightBound` initialisation handles the suffix stitch automatically. |
| `left > 1, right == n` | The leftBound walk finds the prefix's last node; the segment ends at the tail; `rightBound = null`; the helper terminates when `current` becomes `null`. |
| Two nodes, `left = 1, right = 2` | The prefix-less branch fires; full-list reversal of a two-node list. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Every reversal problem reduces to the same six-line loop:

```
previous = <appropriate sentinel>
current  = <segment start>
while current != <stop sentinel>:
    next          = current.next   # save forward link BEFORE clobbering it
    current.next  = previous       # flip the pointer
    previous      = current        # advance previous one step
    current       = next           # advance current  one step
```

Three insights to internalise:

1. **Save the forward link first.** The instant you flip `current.next`, the forward path disappears. `next = current.next` is the line that keeps the rest of the list reachable.
2. **Sentinels control scope.** For full-list reversal, the sentinel is `null` on both ends. For segment reversal, the sentinel is `rightBound = end.next` and the initial `previous` is `rightBound` itself (so the reversed tail points to the correct successor automatically).
3. **Reversal composes.** Every problem in the next lesson — reverse first K, reverse last K, reverse in groups, palindrome check — is just this loop wrapped in different sentinel choices and reconnection logic.

When you next see a linked-list problem that mentions "backward", "reverse", "palindrome", "opposite order", or asks you to prove a list has mirror structure — reach for the three-pointer loop first.

> **Transfer Challenge:** Reverse a linked list **recursively** in O(n) time with O(n) stack space. Then explain why the iterative version is strictly preferable in production code.
>
> <details><summary><strong>Answer</strong></summary>
>
> Recursive:
>
> ```
> def reverse(head):
>     if head is None or head.next is None:
>         return head
>     new_head = reverse(head.next)
>     head.next.next = head   # the node just after head now points back to head
>     head.next = None        # head becomes the new tail
>     return new_head
> ```
>
> Why iterative wins in production: recursion costs O(n) stack space, and for a 10-million-node list that blows the default stack limit (~1 MB ≈ 10⁵ frames in most languages). The iterative three-pointer version runs the same algorithm in O(1) extra space and never crashes regardless of input size.
>
> </details>

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Positional segment reversal composes two positional walks with the segment-reversal helper. The helper's `previous = rightBound` initialisation handles the suffix stitch for free; the caller only needs to write the prefix stitch — `leftBound.next = newHead` — and handle the `left == 1` special case where there is no prefix.

</details>