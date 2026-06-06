---
title: "Reverse K-Segments"
summary: "Given the head of a singly linked list and a positive integer k, write a function to reverse the list in groups of k and return the head of the reversed list."
prereqs:
  - 08-pattern-reversal-subproblem/01-pattern
difficulty: medium
---

# Reverse K-segments

## Problem Statement

Given the **head** of a singly linked list and a positive integer **k**, write a function to reverse the list in groups of k and return the head of the reversed list.

If, at the end, the length of the remaining list is less than k, do not reverse that part of the list.

---

## Examples

**Example 1**
```
Input:  head = [5, 7, 3, 10, 6, 8], k = 3
Output: [3, 7, 5, 8, 6, 10]
Explanation: Two full groups of three exist. Reverse each: (5, 7, 3) → (3, 7, 5) and (10, 6, 8) → (8, 6, 10). Concatenate to [3, 7, 5, 8, 6, 10].
```

**Example 2**
```
Input:  head = [5, 7, 3, 10, 6], k = 2
Output: [7, 5, 10, 3, 6]
Explanation: Two full pairs reverse: (5, 7) → (7, 5) and (3, 10) → (10, 3). The trailing single node 6 is shorter than k and stays in place.
```

**Example 3**
```
Input:  head = [5, 7, 3, 10, 6], k = 8
Output: [5, 7, 3, 10, 6]
Explanation: The full list is shorter than k, so no chunk is reversed and the input is returned unchanged.
```

**Example 4**
```
Input:  head = [1, 2, 3, 4, 5, 6], k = 6
Output: [6, 5, 4, 3, 2, 1]
Explanation: Exactly one full chunk of size 6 spans the whole list; the inner reversal flips the entire list once.
```

<details>
<summary><h2>Intuition</h2></summary>

The **structural property** is that the rewrite is a sequence of `length / k` independent in-place reversals on disjoint chunks of size `k`. Each chunk is a self-contained subproblem: flip the links inside `[start, end]` where `end` is `start` advanced by `k − 1` hops, and stitch the result back into the list. The trailing fragment of size `length % k` is short-circuited — the outer loop runs exactly `length / k` times, so any leftover nodes never enter a reversal call. This is the general fixed-size form of pairwise swap (`k = 2`).

The **pointer placement** uses the four boundary pointers from the chapter pattern. `start` marks the chunk's first node; `end = get_node_at_position(start, k)` walks `k − 1` hops to mark its last node; `left_bound` caches the predecessor (initially `None` so the global `head` is updated on the first chunk); `right_bound` is implicit as `end.next` and is consumed inside the `reverse` primitive. After reversal, the old `start` becomes the chunk's tail and the next iteration's `left_bound`; `start` advances to `left_bound.next`, which now correctly points at the next chunk's head because the seam stitch put it there.

What **breaks if you reach for value-copying**? Reading all values into an array, reversing each k-sized window in the array, and writing the values back is `O(n)` time but `O(n)` extra space — and it dodges the actual algorithmic question. Re-traversing the list from `head` to find each chunk's boundary inflates the cost to `O(n²)`. Both shortcuts miss the point: the algorithm should walk each node a constant number of times and rewrite `next` pointers in place. The shared `reverse(start, end)` helper enforces both properties — one forward walk per chunk, `O(1)` extra memory.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>

Reverse-k-segments is the canonical reversal-subproblem problem — the entire chapter pattern is structured around it. The diagnostic confirms the fit precisely.

| Check | Answer for Reverse-K-Segments |
|---|---|
| **Q1.** Can the problem or solution be broken down into smaller subproblems? | **Yes** — the rewrite decomposes into `length / k` independent reversals on disjoint contiguous chunks of size `k`. The chunks do not share state. |
| **Q2.** Can any subproblem be solved by reversing a part of the linked list? | **Yes** — each chunk reversal is one call to `reverse(start, end)` from chapter pattern 07; the helper flips `next` pointers between two boundary nodes in place. |
| **Q3.** Does the algorithm only need to walk each node a constant number of times? | **Yes** — `get_node_at_position` walks `k − 1` hops to find `end`, and the inner reversal walks the same `k` nodes once. Each node is touched twice across one outer pass. |
| **Q4.** Is each chunk's boundary computable from local state? | **Yes** — `end` is `start` plus `k`, and the seam stitch uses `left_bound` cached from the previous iteration. The length precomputation runs once before the loop and is constant memory. |

</details>
<details>
<summary><h2>Approach</h2></summary>

Six numbered steps. No code; the next section is the implementation.

1. **Guard the trivial cases.** If `head` is `None`, the list has only one node, or `k == 1`, no chunk can or should be reversed. Return `head` unchanged.
2. **Precompute the chunk count.** Walk the list once to find `length`, then set `total_segments = length // k`. The trailing fragment of `length % k` nodes is implicitly skipped because the loop runs exactly `total_segments` times.
3. **Initialise the boundary pointers.** Set `start = head` and `left_bound = None`. The `None` marker signals the first iteration's seam-stitch path — update the global `head` instead of writing through a predecessor.
4. **Drive the outer loop `total_segments` times.** For each iteration, find the chunk's end: `end = get_node_at_position(start, k)` walks `k − 1` hops forward from `start`. Then call `reverse(start, end)` and capture the returned `reversed_head` (the chunk's new first node, which is the old `end`).
5. **Stitch the seam.** If `left_bound is None`, this is the first chunk — update the global `head = reversed_head`. Otherwise set `left_bound.next = reversed_head` so the previous chunk's tail re-attaches to this chunk's new head.
6. **Slide the boundary forward.** Set `left_bound = start` (the old `start` is now the chunk's tail), then `start = left_bound.next`. The previous seam stitch put the next chunk's first node there, so `start` correctly advances to the next chunk's head.

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
    def find_length(self, head: Optional[ListNode]) -> int:
        length = 0
        while head is not None:
            length += 1
            head = head.next
        return length

    def get_node_at_position(
        self, head: ListNode, position: int
    ) -> ListNode:
        current = head
        for _ in range(1, position):
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

    def reverse_k_segments(
        self, head: Optional[ListNode], k: int
    ) -> Optional[ListNode]:

        # If the list is empty, has only one node, or k is 1, no need to
        # reverse segments
        if head is None or head.next is None or k == 1:
            return head

        # Start of the current segment to be reversed
        start = head

        # Pointer to the last node of the previous segment
        left_bound = None

        # Find the total number of segments in the linked list
        total_segments = self.find_length(head) // k

        # Loop through the list to reverse every k-length segment
        for i in range(total_segments):

            # Get the end node of the current segment
            end = self.get_node_at_position(start, k)

            # Get the head of the reversed segment.
            reversed_head = self.reverse(start, end)

            # Check if there is a previous segment to connect to or
            # if the existing head needs to be updated.
            # If left_bound is None, it means we're at the first segment
            # So, we need to update the head to the reversed_head
            # Return the new head
            if left_bound is None:
                head = reversed_head

            # If there is a left_bound, connect its next to the new
            # reversed_head
            else:
                left_bound.next = reversed_head

            # Update left_bound to the current segment's start (which is
            # now the end after reversal)
            left_bound = start

            # Move to the next segment
            start = left_bound.next

        # Return the head of the modified list
        return head


# Examples from the problem statement
print(to_list(Solution().reverse_k_segments(from_list([5, 7, 3, 10, 6, 8]), 3)))  # [3, 7, 5, 8, 6, 10]
print(to_list(Solution().reverse_k_segments(from_list([5, 7, 3, 10, 6]), 2)))     # [7, 5, 10, 3, 6]
print(to_list(Solution().reverse_k_segments(from_list([5, 7, 3, 10, 6]), 8)))     # [5, 7, 3, 10, 6]

# Edge cases
print(to_list(Solution().reverse_k_segments(None, 2)))                             # []
print(to_list(Solution().reverse_k_segments(from_list([1]), 1)))                   # [1]
print(to_list(Solution().reverse_k_segments(from_list([1, 2]), 1)))                # [1, 2]
print(to_list(Solution().reverse_k_segments(from_list([1, 2, 3, 4, 5, 6]), 2)))   # [2, 1, 4, 3, 6, 5]
print(to_list(Solution().reverse_k_segments(from_list([1, 2, 3, 4, 5, 6]), 6)))   # [6, 5, 4, 3, 2, 1]
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
        private int findLength(ListNode head) {
            int length = 0;
            while (head != null) {
                length++;
                head = head.next;
            }
            return length;
        }

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

        public ListNode reverseKSegments(ListNode head, int k) {

            // If the list is empty, has only one node, or k is 1, no need to
            // reverse segments
            if (head == null || head.next == null || k == 1) {
                return head;
            }

            // Start of the current segment to be reversed
            ListNode start = head;

            // Pointer to the last node of the previous segment
            ListNode leftBound = null;

            // Find the total number of segments in the linked list
            int totalSegments = findLength(head) / k;

            // Loop through the list to reverse every k-length segment
            for (int i = 0; i < totalSegments; i++) {

                // Get the end node of the current segment
                ListNode end = getNodeAtPosition(start, k);

                // Get the head of the reversed segment.
                ListNode reversedHead = reverse(start, end);

                // Check if there is a previous segment to connect to or
                // if the existing head needs to be updated.
                // If leftBound is null, it means we're at the first
                // segment So, we need to update the head to the
                // reversedHead
                if (leftBound == null) {
                    head = reversedHead;
                }

                // If there is a leftBound, connect its next to the new
                // reversedHead
                else {
                    leftBound.next = reversedHead;
                }

                // Update leftBound to the current segment's start (which is
                // now the end after reversal)
                leftBound = start;

                // Move to the next segment
                start = leftBound.next;
            }

            // Return the head of the modified list
            return head;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(toList(new Solution().reverseKSegments(fromList(5, 7, 3, 10, 6, 8), 3))); // [3, 7, 5, 8, 6, 10]
        System.out.println(toList(new Solution().reverseKSegments(fromList(5, 7, 3, 10, 6), 2)));    // [7, 5, 10, 3, 6]
        System.out.println(toList(new Solution().reverseKSegments(fromList(5, 7, 3, 10, 6), 8)));    // [5, 7, 3, 10, 6]

        // Edge cases
        System.out.println(toList(new Solution().reverseKSegments(null, 2)));                          // []
        System.out.println(toList(new Solution().reverseKSegments(fromList(1), 1)));                   // [1]
        System.out.println(toList(new Solution().reverseKSegments(fromList(1, 2), 1)));                // [1, 2]
        System.out.println(toList(new Solution().reverseKSegments(fromList(1, 2, 3, 4, 5, 6), 2)));   // [2, 1, 4, 3, 6, 5]
        System.out.println(toList(new Solution().reverseKSegments(fromList(1, 2, 3, 4, 5, 6), 6)));   // [6, 5, 4, 3, 2, 1]
    }
}
```

### Dry Run — Example 1

`head = [5, 7, 3, 10, 6, 8]`, `k = 3`. Precompute `length = 6`, `total_segments = 6 // 3 = 2`. Initial state: `start = 5`, `left_bound = None`.

**Iteration 1 — chunk `(5, 7, 3)`:**

| step | state |
|---|---|
| `end = get_node_at_position(start, 3)` | `end = 3` (advance `start` two hops) |
| `reverse(5, 3)` | links flip → `3 → 7 → 5 → 10`; returns `reversed_head = 3` |
| `left_bound is None` → update head | `head = 3` |
| `left_bound = start` | `left_bound = 5` (chunk's new tail) |
| `start = left_bound.next` | `start = 10` |

List after iteration 1: `3 → 7 → 5 → 10 → 6 → 8`.

**Iteration 2 — chunk `(10, 6, 8)`:**

| step | state |
|---|---|
| `end = get_node_at_position(start, 3)` | `end = 8` |
| `reverse(10, 8)` | links flip → `8 → 6 → 10 → None`; returns `reversed_head = 8` |
| `left_bound is not None` → stitch | `left_bound.next = 8`, so `5.next = 8` |
| `left_bound = start` | `left_bound = 10` |
| `start = left_bound.next` | `start = None` |

List after iteration 2: `3 → 7 → 5 → 8 → 6 → 10`.

**Loop ends** (`total_segments = 2` iterations completed).

**Return:** `head = 3`, traversal yields `[3, 7, 5, 8, 6, 10]` ✓

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | `O(n)` | `find_length` walks once. The outer loop runs `length / k` times. Each iteration's `get_node_at_position` walks `k − 1` hops and the inner reversal walks `k` nodes. The amortised cost per node is constant. |
| **Space** | `O(1)` | Four boundary pointers (`start`, `end`, `left_bound`, `reversed_head`) plus the constant `total_segments`. The rewrite is in place; no auxiliary array or recursion stack. |

### Edge Cases

| Scenario | Input | Output | Note |
|---|---|---|---|
| Empty list | `[], k = 2` | `[]` | First guard returns immediately. |
| Single node | `[1], k = 1` | `[1]` | `head.next is None` → guard returns. |
| `k = 1` short-circuit | `[1, 2], k = 1` | `[1, 2]` | `k == 1` reversal is a no-op; the guard skips the loop. |
| `k` larger than list length | `[5, 7, 3, 10, 6], k = 8` | `[5, 7, 3, 10, 6]` | `total_segments = 5 // 8 = 0`; the outer loop never runs. |
| Length exactly equal to `k` | `[1, 2, 3, 4, 5, 6], k = 6` | `[6, 5, 4, 3, 2, 1]` | One full chunk spans the list; equivalent to a single full-list reversal. |
| Length divides evenly | `[1, 2, 3, 4, 5, 6], k = 2` | `[2, 1, 4, 3, 6, 5]` | Three full pairs, no trailing fragment. |
| Length leaves a remainder | `[5, 7, 3, 10, 6], k = 2` | `[7, 5, 10, 3, 6]` | Two full pairs reverse; the trailing single node `6` is left untouched. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

Reverse-k-segments is the general fixed-size form of pairwise swap: precompute `length / k`, run that many chunk reversals, and let the trailing fragment of `length % k` nodes pass through untouched. The shared `reverse(start, end)` helper is invoked once per chunk and never re-walks the list from `head`.

</details>
