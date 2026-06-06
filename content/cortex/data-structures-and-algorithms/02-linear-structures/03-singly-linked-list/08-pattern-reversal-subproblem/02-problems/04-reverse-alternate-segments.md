---
title: "Reverse Alternate Segments"
summary: "Given the head of a singly linked list and a positive integer k, write a function to reverse alternate k nodes in the list and return the head of the reversed list."
prereqs:
  - 08-pattern-reversal-subproblem/01-pattern
difficulty: medium
---

# Reverse alternate segments

## Problem Statement

Given the **head** of a singly linked list and a positive integer **k**, write a function to reverse alternate k nodes in the list and return the head of the reversed list.

If, at the end, the length of the remaining list is less than k, do not reverse that part of the list.

---

## Examples

**Example 1**
```
Input:  head = [5, 7, 3, 10, 6, 8], k = 2
Output: [7, 5, 3, 10, 8, 6]
Explanation: Three chunks of size 2 fit. Reverse chunk 1: (5, 7) → (7, 5); skip chunk 2: (3, 10) stays; reverse chunk 3: (6, 8) → (8, 6). Concatenate to [7, 5, 3, 10, 8, 6].
```

**Example 2**
```
Input:  head = [5, 7, 3, 10, 6], k = 3
Output: [3, 7, 5, 10, 6]
Explanation: One full chunk of size 3 fits; reverse (5, 7, 3) → (3, 7, 5). The remaining (10, 6) is shorter than k and stays untouched.
```

**Example 3**
```
Input:  head = [5, 7, 3, 10, 6], k = 8
Output: [5, 7, 3, 10, 6]
Explanation: The full list is shorter than k, so no chunk forms and the input is returned unchanged.
```

**Example 4**
```
Input:  head = [1, 2, 3, 4, 5, 6, 7, 8], k = 2
Output: [2, 1, 3, 4, 6, 5, 7, 8]
Explanation: Four chunks of size 2. Reverse chunks 1 and 3 ((1, 2) → (2, 1) and (5, 6) → (6, 5)); skip chunks 2 and 4 ((3, 4) and (7, 8) stay).
```

<details>
<summary><h2>Intuition</h2></summary>

The **structural property** is that the rewrite walks fixed-size chunks of `k` but flips only every other one. The list decomposes into `length / k` chunks of size `k`; chunks at even indices (counting from 0) are reversed, chunks at odd indices are skipped. A trailing fragment of `length % k` nodes never enters the loop. The difference from reverse-k-segments is one extra boolean flag in the outer driver — the inner reversal primitive is untouched.

The **pointer placement** uses the same four boundaries as reverse-k-segments plus a `should_reverse` flag (initially `True`). On `True` iterations the chunk is reversed and the seam stitched as usual; on `False` iterations the algorithm only walks `end` forward and sets `left_bound = end` (not `left_bound = start`, because no reversal happened — the chunk's last node is still `end`). The flag toggles after every iteration, so the pattern reverses, skips, reverses, skips. The `start = left_bound.next` advance works identically in both branches because `left_bound` always ends at the chunk's tail in the current list ordering.

What **breaks if you reach for a single-pass value-copy**? Reading values into an array, reversing alternate windows, and writing back is `O(n)` time and `O(n)` extra space — it dodges the link-rewrite contract. Trying to handle the alternation by re-walking the list from `head` every other iteration costs `O(n²)` time. Both shortcuts miss the point: only the outer driver changes between reverse-k-segments and reverse-alternate-segments. The shared `reverse(start, end)` helper carries over without modification; the only structural delta is the conditional reversal step inside the loop body.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>

Reverse-alternate-segments adds a conditional branch to the chapter pattern. The diagnostic confirms the conditional does not break the pattern's prerequisites.

| Check | Answer for Reverse Alternate Segments |
|---|---|
| **Q1.** Can the problem or solution be broken down into smaller subproblems? | **Yes** — the rewrite decomposes into `length / k` chunks of size `k`. Even-indexed chunks are reversal subproblems; odd-indexed chunks are no-op advance steps. |
| **Q2.** Can any subproblem be solved by reversing a part of the linked list? | **Yes** — every "reverse" chunk is one call to the shared `reverse(start, end)` helper; "skip" chunks invoke no helper at all. |
| **Q3.** Does the algorithm only need to walk each node a constant number of times? | **Yes** — `get_node_at_position` walks `k − 1` hops per chunk, and reversed chunks add one more `k`-node pass. Skipped chunks add zero extra walks. Overall the list is touched a constant number of times. |
| **Q4.** Is each chunk's boundary computable from local state? | **Yes** — `end` is `start` plus `k`; the `should_reverse` flag toggles after every iteration; `left_bound` is cached from the previous chunk's tail. All state is local and constant-size. |

</details>
<details>
<summary><h2>Approach</h2></summary>

Seven numbered steps. No code; the next section is the implementation.

1. **Guard the trivial cases.** If `head` is `None`, the list has only one node, or `k == 1`, no alternation can change anything. Return `head` unchanged.
2. **Precompute the chunk count.** Find `length` and set `total_segments = length // k`. Any trailing fragment of `length % k` nodes is implicitly skipped because the outer loop runs exactly `total_segments` times.
3. **Initialise the boundary pointers and the toggle.** Set `start = head`, `left_bound = None`, and `should_reverse = True`. The first chunk is a "reverse" chunk; the flag flips after every iteration.
4. **Drive the outer loop `total_segments` times.** Each iteration computes `end = get_node_at_position(start, k)` and then branches on `should_reverse`.
5. **Reverse branch (`should_reverse = True`).** Call `reverse(start, end)` and capture `reversed_head`. If `left_bound is None`, update `head = reversed_head`; otherwise set `left_bound.next = reversed_head`. Then set `left_bound = start` — the old `start` is now the chunk's tail.
6. **Skip branch (`should_reverse = False`).** No reversal call. Just record that the chunk's tail is `end` itself: `left_bound = end`. No seam stitch is needed because the existing `next` pointers are already correct.
7. **Advance and toggle.** Set `start = left_bound.next` (the next chunk's head; either way, `left_bound` ended at the current chunk's tail). Toggle `should_reverse = not should_reverse` for the next iteration.

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

    def reverse_alternate_segments(
        self, head: Optional[ListNode], k: int
    ) -> Optional[ListNode]:

        # If the list is empty, has only one node, or k is 1, no need to
        # reverse segments
        if head is None or head.next is None or k == 1:
            return head

        # Flag to determine whether to reverse the current segment
        should_reverse = True

        # Start of the current segment to be reversed
        start: Optional[ListNode] = head

        # Pointer to the last node of the previous segment
        left_bound: Optional[ListNode] = None

        # Find the total number of segments in the linked list
        total_segments = self.find_length(head) // k

        # Loop through the list to reverse every alternate k-length
        # segment
        for i in range(total_segments):

            # Get the end node of the current segment
            end = self.get_node_at_position(start, k)

            # Reverse the current segment if the flag is set
            if should_reverse:

                # Get the head of the reversed segment.
                reversed_head = self.reverse(start, end)

                # Check if there is a previous segment to connect to or
                # if the existing head needs to be updated.
                # If left_bound is None, it means we're at the first
                # segment So, we need to update the head to the
                # reversed_head
                if left_bound is None:
                    head = reversed_head

                # If there is a left_bound, connect its next to the
                # new reversed_head
                else:
                    left_bound.next = reversed_head

                # Update left_bound to the current segment's start (which
                # is now the end after reversal)
                left_bound = start

            # If the flag is not set, skip reversing the current segment
            else:

                # Skip reversing this segment, move left_bound to the end
                # of the segment
                left_bound = end

            # Move to the next segment
            start = left_bound.next

            # Toggle the flag for the next segment
            should_reverse = not should_reverse

        # Return the new head of the list
        return head


# Examples from the problem statement
print(to_list(Solution().reverse_alternate_segments(from_list([5, 7, 3, 10, 6, 8]), 2)))  # [7, 5, 3, 10, 8, 6]
print(to_list(Solution().reverse_alternate_segments(from_list([5, 7, 3, 10, 6]), 3)))     # [3, 7, 5, 10, 6]
print(to_list(Solution().reverse_alternate_segments(from_list([5, 7, 3, 10, 6]), 8)))     # [5, 7, 3, 10, 6]

# Edge cases
print(to_list(Solution().reverse_alternate_segments(None, 2)))                             # []
print(to_list(Solution().reverse_alternate_segments(from_list([1]), 2)))                   # [1]
print(to_list(Solution().reverse_alternate_segments(from_list([1, 2, 3, 4]), 1)))          # [1, 2, 3, 4]
print(to_list(Solution().reverse_alternate_segments(from_list([1, 2, 3, 4, 5, 6]), 3)))   # [3, 2, 1, 4, 5, 6]
print(to_list(Solution().reverse_alternate_segments(from_list([1, 2, 3, 4, 5, 6, 7, 8]), 2)))  # [2, 1, 3, 4, 6, 5, 7, 8]
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

        public ListNode reverseAlternateSegments(ListNode head, int k) {

            // If the list is empty, has only one node, or k is 1, no need to
            // reverse segments
            if (head == null || head.next == null || k == 1) {
                return head;
            }

            // Flag to determine whether to reverse the current segment
            boolean shouldReverse = true;

            // Start of the current segment to be reversed
            ListNode start = head;

            // Pointer to the last node of the previous segment
            ListNode leftBound = null;

            // Find the total number of segments in the linked list
            int totalSegments = findLength(head) / k;

            // Loop through the list to reverse every alternate k-length
            // segment
            for (int i = 0; i < totalSegments; i++) {

                // Get the end node of the current segment
                ListNode end = getNodeAtPosition(start, k);

                // Reverse the current segment if the flag is set
                if (shouldReverse) {

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

                    // If there is a leftBound, connect its next to the
                    // new reversedHead
                    else {
                        leftBound.next = reversedHead;
                    }

                    // Update leftBound to the current segment's start (which
                    // is now the end after reversal)
                    leftBound = start;
                }

                // If the flag is not set, skip reversing the current segment
                else {

                    // Skip reversing this segment, move leftBound to the end
                    // of the segment
                    leftBound = end;
                }

                // Move to the next segment
                start = leftBound.next;

                // Toggle the flag for the next segment
                shouldReverse = !shouldReverse;
            }

            // Return the new head of the list
            return head;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(toList(new Solution().reverseAlternateSegments(fromList(5, 7, 3, 10, 6, 8), 2))); // [7, 5, 3, 10, 8, 6]
        System.out.println(toList(new Solution().reverseAlternateSegments(fromList(5, 7, 3, 10, 6), 3)));    // [3, 7, 5, 10, 6]
        System.out.println(toList(new Solution().reverseAlternateSegments(fromList(5, 7, 3, 10, 6), 8)));    // [5, 7, 3, 10, 6]

        // Edge cases
        System.out.println(toList(new Solution().reverseAlternateSegments(null, 2)));                          // []
        System.out.println(toList(new Solution().reverseAlternateSegments(fromList(1), 2)));                   // [1]
        System.out.println(toList(new Solution().reverseAlternateSegments(fromList(1, 2, 3, 4), 1)));          // [1, 2, 3, 4]
        System.out.println(toList(new Solution().reverseAlternateSegments(fromList(1, 2, 3, 4, 5, 6), 3)));   // [3, 2, 1, 4, 5, 6]
        System.out.println(toList(new Solution().reverseAlternateSegments(fromList(1, 2, 3, 4, 5, 6, 7, 8), 2))); // [2, 1, 3, 4, 6, 5, 7, 8]
    }
}
```

### Dry Run — Example 1

`head = [5, 7, 3, 10, 6, 8]`, `k = 2`. Precompute `length = 6`, `total_segments = 6 // 2 = 3`. Initial state: `start = 5`, `left_bound = None`, `should_reverse = True`.

**Iteration 1 — chunk `(5, 7)`, `should_reverse = True`:**

| step | state |
|---|---|
| `end = get_node_at_position(start, 2)` | `end = 7` |
| `reverse(5, 7)` | links flip → `7 → 5 → 3`; returns `reversed_head = 7` |
| `left_bound is None` → update head | `head = 7` |
| `left_bound = start` | `left_bound = 5` |
| `start = left_bound.next` | `start = 3` |
| toggle | `should_reverse = False` |

List after iteration 1: `7 → 5 → 3 → 10 → 6 → 8`.

**Iteration 2 — chunk `(3, 10)`, `should_reverse = False`:**

| step | state |
|---|---|
| `end = get_node_at_position(start, 2)` | `end = 10` |
| skip branch | no reversal; `left_bound = end = 10` |
| `start = left_bound.next` | `start = 6` |
| toggle | `should_reverse = True` |

List after iteration 2: `7 → 5 → 3 → 10 → 6 → 8` (unchanged — the chunk was skipped).

**Iteration 3 — chunk `(6, 8)`, `should_reverse = True`:**

| step | state |
|---|---|
| `end = get_node_at_position(start, 2)` | `end = 8` |
| `reverse(6, 8)` | links flip → `8 → 6 → None`; returns `reversed_head = 8` |
| `left_bound is not None` → stitch | `10.next = 8` |
| `left_bound = start` | `left_bound = 6` |
| `start = left_bound.next` | `start = None` |
| toggle | `should_reverse = False` |

List after iteration 3: `7 → 5 → 3 → 10 → 8 → 6`.

**Loop ends** (`total_segments = 3` iterations completed).

**Return:** `head = 7`, traversal yields `[7, 5, 3, 10, 8, 6]` ✓

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | `O(n)` | `find_length` walks once. The outer loop runs `length / k` times. Each reverse iteration walks `2k` nodes (`get_node_at_position` + `reverse`); each skip iteration walks only `k − 1` nodes inside `get_node_at_position`. Total work is linear in `n`. |
| **Space** | `O(1)` | Four boundary pointers, the `should_reverse` flag, the chunk counter. No auxiliary structures. |

### Edge Cases

| Scenario | Input | Output | Note |
|---|---|---|---|
| Empty list | `[]`, `k = 2` | `[]` | First guard returns immediately. |
| Single node | `[1]`, `k = 2` | `[1]` | `head.next is None` → guard returns. |
| `k = 1` short-circuit | `[1, 2, 3, 4]`, `k = 1` | `[1, 2, 3, 4]` | `k == 1` triggers the early return; alternating singleton reversals would be no-ops anyway. |
| Single full chunk (odd index parity) | `[1, 2, 3, 4, 5, 6]`, `k = 3` | `[3, 2, 1, 4, 5, 6]` | One reverse chunk `(1, 2, 3)` and one skip chunk `(4, 5, 6)`; `total_segments = 2`. |
| `k` larger than list length | `[5, 7, 3, 10, 6]`, `k = 8` | `[5, 7, 3, 10, 6]` | `total_segments = 0`; outer loop never runs. |
| Trailing fragment shorter than `k` | `[5, 7, 3, 10, 6]`, `k = 3` | `[3, 7, 5, 10, 6]` | One reverse chunk fits; the remaining two nodes are shorter than `k` and stay untouched. |
| Multiple alternating chunks | `[1, 2, 3, 4, 5, 6, 7, 8]`, `k = 2` | `[2, 1, 3, 4, 6, 5, 7, 8]` | Four chunks; reverse chunks 1 and 3, skip chunks 2 and 4. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

Reverse-alternate-segments is reverse-k-segments with a toggling `should_reverse` flag — on `True` iterations the chunk is reversed and the seam stitched; on `False` iterations only `left_bound = end` advances the boundary. The inner `reverse(start, end)` helper and the chunk-count outer driver are unchanged.

</details>
