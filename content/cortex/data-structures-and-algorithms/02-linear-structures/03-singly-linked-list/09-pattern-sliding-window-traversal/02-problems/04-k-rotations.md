---
title: "K Rotations"
summary: "Given the head of a singly linked list and a non-negative integer k, write a function to rotate the list to the right by k places and return the head of the rotated list."
prereqs:
  - 09-pattern-sliding-window-traversal/01-pattern
difficulty: medium
---

# K rotations

## Problem Statement

Given the **head** of a singly linked list and a non-negative integer **k**, write a function to rotate the list to the **right** by k places and return the head of the rotated list.

## Examples

**Example 1:**
```
Input:  head = [1, 2, 3, 4, 5], k = 2
Output: [4, 5, 1, 2, 3]
```
After rotating right by `2`, the last two nodes become the new prefix. The two rotations land as `[5, 1, 2, 3, 4]` then `[4, 5, 1, 2, 3]`.

**Example 2:**
```
Input:  head = [0, 1, 2], k = 4
Output: [2, 0, 1]
```
`k` exceeds the length, so the effective rotation is `k % length = 4 % 3 = 1` — the same as a single right rotation.

**Example 3:**
```
Input:  head = [1, 2, 3, 4, 5], k = 5
Output: [1, 2, 3, 4, 5]
```
Rotating right by the length leaves the list unchanged — `5 % 5 = 0`.


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a sliding-window-traversal problem is that a right-rotation by `k` is equivalent to cutting the list at position `length − k` and rejoining the suffix as a new prefix — and finding that cut point is "the `(length − k + 1)`-th node from the start," which is exactly "the `k`-th node from the end." A singly linked list cannot be walked backwards, but the lockstep two-pointer walk locates that node in a single pass.

The **pointer placement** follows directly. Two pointers are spread `k − 1` apart: a leading cursor `current` and a trailing cursor `kth_from_end`, plus a predecessor `prev_to_kth_from_end` that shadows `kth_from_end` one node behind. Walk `current` `k − 1` hops alone during priming. Then slide both in lockstep until `current.next` is `null` — at which point `current` is on the tail (the new tail of the rotated list), `kth_from_end` is on the new head, and `prev_to_kth_from_end` is on the node where the cut happens. Three pointer rewires (`prev_to_kth_from_end.next = None`, `current.next = head`, `return kth_from_end`) finish the job.

What **breaks if you reach for a naive approach**? Performing `k` individual right-rotations is `O(n · k)` time, which collapses to `O(n²)` when `k` is `Θ(n)`. Copying values into an array and rotating with `arr[(i − k) % n]` uses `O(n)` extra space and `O(n)` time but requires two passes (load + write back), and it does not generalise to a streaming source. Only the lockstep walk with the `k % length` normalisation delivers a single pass with `O(1)` extra space.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for K Rotations |
|---|---|
| **Q1.** Does the problem reference a node at a fixed offset from the tail? | **Yes** — the new head is the `k`-th node from the end (after normalising `k` modulo `length`). |
| **Q2.** Can the answer be read off when one pointer reaches the tail? | **Yes** — `kth_from_end`, `prev_to_kth_from_end`, and the new tail (`current`) are all pinned the moment `current.next` is `null`. |
| **Q3.** Is the work at each tick `O(1)`? | **Yes** — three pointer assignments per tick; three pointer rewires at the end. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — four local references (`current`, `kth_from_end`, `prev_to_kth_from_end`, `head`) regardless of list length. |

</details>
<details>
<summary><h2>Brute Force: Apply One Rotation `k` Times</h2></summary>


Each single right-rotation moves the tail to the front. With a one-pass tail lookup per rotation, that costs `O(n)` time per rotation and `O(n · k)` time overall. Correct, but the work grows quadratically when `k` is `Θ(n)` — and the algorithm wastes the obvious structural shortcut: a rotation by `k` is one cut and one rejoin, not `k` independent operations.

</details>
<details>
<summary><h2>Key Insight: Rotate-by-`k` = Cut at the `(length − k)`-th Node, Rejoin</h2></summary>


A right-rotation by `k` is equivalent to:

1. Compute `k % length` to handle `k >= length`.
2. Walk to the `(length − k)`-th node (the new tail) and the `(length − k + 1)`-th node (the new head). These are the `(k + 1)`-th and `k`-th nodes from the end, respectively.
3. Cut after the new tail; rejoin the old tail to the old head; return the new head.

The lockstep walk with a gap of `k − 1` parks `current` on the old tail and `kth_from_end` on the new head in a single pass — exactly what the cut-and-rejoin needs.

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the lockstep walk with a gap of `k − 1`. Compute the length to normalise `k` first.

1. **Handle the trivial inputs.** If `head` is `null`, the list has only one node, or `k == 0`, return `head` unchanged — rotation has no effect.
2. **Normalise `k`.** Compute `length` in a forward walk. If `k >= length`, recurse with `k % length` so the effective rotation is in the range `[0, length)`.
3. **Prime the gap.** Initialise `current = head` and walk it `k − 1` hops alone.
4. **Slide together.** Initialise `kth_from_end = head` and `prev_to_kth_from_end = null`. While `current.next` is not `null`, advance `prev_to_kth_from_end = kth_from_end`, `kth_from_end = kth_from_end.next`, and `current = current.next`.
5. **Cut and rejoin.** Set `prev_to_kth_from_end.next = null` (severs the list after the new tail). Set `current.next = head` (re-attaches the old prefix to the end of the old suffix). Return `kth_from_end` as the new head.

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

    def k_rotations(
        self, head: Optional[ListNode], k: int
    ) -> Optional[ListNode]:

        # If the list is empty or has only one node, no swapping is
        # needed.
        if head is None or head.next is None or k == 0:
            return head

        # Get the length of the list
        length = self.find_length(head)

        # If k is greater than or equal to the length of the list, reduce
        # k to its modulo value
        if k >= length:
            return self.k_rotations(head, k % length)

        # Pointer to keep track of the end of the list
        current = head

        # Traverse to the kth node from the beginning
        for i in range(1, k):
            current = current.next

        # Pointer to the kth node from the end
        kth_from_end = head

        # Pointer to the node before the kth_from_end node
        prev_to_kth_from_end = None

        # Find the kth node from the end
        while current is not None and current.next is not None:
            prev_to_kth_from_end = kth_from_end
            kth_from_end = kth_from_end.next
            current = current.next

        # Since kth_from_end is the new head node, disconnect the list
        # at `prev_to_kth_from_end`, making `kth_from_end` the new head.
        prev_to_kth_from_end.next = None

        # Link the end of the list back to the original head.
        current.next = head

        # Return `kth_from_end` as it becomes the new head of the rotated
        # list.
        return kth_from_end


print(to_list(Solution().k_rotations(from_list([1, 2, 3, 4, 5]), 2)))  # [4, 5, 1, 2, 3]
print(to_list(Solution().k_rotations(from_list([0, 1, 2]), 4)))         # [2, 0, 1]

# Edge cases
print(to_list(Solution().k_rotations(from_list([1]), 3)))               # [1]
print(to_list(Solution().k_rotations(from_list([1, 2]), 1)))            # [2, 1]
print(to_list(Solution().k_rotations(from_list([1, 2, 3]), 3)))         # [1, 2, 3]
print(to_list(Solution().k_rotations(from_list([1, 2, 3, 4, 5]), 0)))  # [1, 2, 3, 4, 5]
print(to_list(Solution().k_rotations(from_list([1, 2, 3, 4, 5]), 5)))  # [1, 2, 3, 4, 5]
print(to_list(Solution().k_rotations(from_list([1, 2, 3, 4, 5]), 1)))  # [5, 1, 2, 3, 4]
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

    static java.util.List<Integer> toList(ListNode head) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
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

        public ListNode kRotations(ListNode head, int k) {

            // If the list is empty or has only one node, no swapping is
            // needed.
            if (head == null || head.next == null || k == 0) {
                return head;
            }

            // Get the length of the list
            int length = findLength(head);

            // If k is greater than or equal to the length of the list,
            // reduce k to its modulo value
            if (k >= length) {
                return kRotations(head, k % length);
            }

            // Pointer to keep track of the end of the list
            ListNode current = head;

            // Traverse to the kth node from the beginning
            for (int i = 1; i < k; ++i) {
                current = current.next;
            }

            // Pointer to the kth node from the end
            ListNode kthFromEnd = head;

            // Pointer to the node before the kthFromEnd node
            ListNode prevToKthFromEnd = null;

            // Find the kth node from the end
            while (current != null && current.next != null) {
                prevToKthFromEnd = kthFromEnd;
                kthFromEnd = kthFromEnd.next;
                current = current.next;
            }

            // Since kthNodeFromEnd is the new head node, disconnect the list
            // at `prevToKthFromEnd`, making `kthFromEnd` the new head.
            prevToKthFromEnd.next = null;

            // Link the end of the list back to the original head.
            current.next = head;

            // Return `kthFromEnd` as it becomes the new head of the rotated
            // list.
            return kthFromEnd;
        }
    }

    public static void main(String[] args) {
        System.out.println(toList(new Solution().kRotations(fromList(1, 2, 3, 4, 5), 2)));  // [4, 5, 1, 2, 3]
        System.out.println(toList(new Solution().kRotations(fromList(0, 1, 2), 4)));         // [2, 0, 1]

        // Edge cases
        System.out.println(toList(new Solution().kRotations(fromList(1), 3)));               // [1]
        System.out.println(toList(new Solution().kRotations(fromList(1, 2), 1)));            // [2, 1]
        System.out.println(toList(new Solution().kRotations(fromList(1, 2, 3), 3)));         // [1, 2, 3]
        System.out.println(toList(new Solution().kRotations(fromList(1, 2, 3, 4, 5), 0)));  // [1, 2, 3, 4, 5]
        System.out.println(toList(new Solution().kRotations(fromList(1, 2, 3, 4, 5), 5)));  // [1, 2, 3, 4, 5]
        System.out.println(toList(new Solution().kRotations(fromList(1, 2, 3, 4, 5), 1)));  // [5, 1, 2, 3, 4]
    }
}
```

### Dry Run

```
head = 1 → 2 → 3 → 4 → 5, k = 2, length = 5

k < length, so no modulo recursion.

Init: current = 1

Step 1 — prime the gap (k − 1 = 1 step):
  Iter 1: current advances to 2

After priming: current = 2.

Init: kth_from_end = 1, prev_to_kth_from_end = null.

Step 2 — slide while current.next is not null:
  Tick 1: prev_to_kth_from_end = 1; kth_from_end = 2; current = 3
  Tick 2: prev_to_kth_from_end = 2; kth_from_end = 3; current = 4
  Tick 3: prev_to_kth_from_end = 3; kth_from_end = 4; current = 5
          current.next is null → loop ends.

Targets:
  new head     = kth_from_end       = 4
  cut point    = prev_to_kth_from_end = 3
  old tail     = current            = 5

Cut and rejoin:
  3.next = null   (cut)
  5.next = 1      (rejoin old prefix as the new suffix)

Return new head = 4 → 5 → 1 → 2 → 3. ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | One `find_length` pass plus one lockstep walk. `k % length` normalisation is `O(1)`; the recursion happens at most once. |
| **Space** | `O(1)` | Four local references regardless of `n` or `k`. The single recursive call replaces `k` with `k % length`, so the call stack is bounded by `2`. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | The first guard returns `null`. |
| Single node (`head.next is null`) | The first guard returns `head` unchanged. |
| `k == 0` | The first guard returns `head` unchanged. |
| `k == length` (or any multiple) | `k % length == 0`; the recursive call hits the `k == 0` guard and returns `head` unchanged. |
| `k > length` | `k % length` collapses `k` into `[0, length)`; the recursive call runs once. |
| `k == 1` | Priming runs zero iterations; the slide walks to the tail; the cut detaches the tail and re-attaches it as the new head. |
| `length == 2`, `k == 1` | Priming runs zero iterations; the slide runs once; the two nodes swap. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


`K Rotations` is the cut-and-rejoin variant of sliding-window traversal: a single lockstep walk with a gap of `k − 1` parks the new tail (`current`), the cut point (`prev_to_kth_from_end`), and the new head (`kth_from_end`) — three pointer rewires finish the rotation in `O(1)` work. The normalisation `k % length` makes the algorithm robust to inputs larger than the list itself.

</details>