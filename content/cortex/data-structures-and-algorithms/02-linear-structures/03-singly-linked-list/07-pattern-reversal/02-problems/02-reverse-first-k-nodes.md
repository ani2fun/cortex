---
title: "Reverse First K Nodes"
summary: "Given the head of a singly linked list and a non-negative integer k, write a function to reverse the first k nodes of the list and return the head of the reversed list."
prereqs:
  - 08-pattern-reversal/01-pattern
difficulty: easy
---

# Reverse first K nodes

## Problem Statement

Given the **head** of a singly linked list and a non-negative integer **k**, write a function to reverse the first `k` nodes of the list and return the head of the reversed list.

You need to reverse the list in place.

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10], k = 2
Output: [7, 5, 3, 10]
```

**Example 2:**
```
Input:  head = [1, 2, 3, 4, 5], k = 5
Output: [5, 4, 3, 2, 1]
```

**Example 3:**
```
Input:  head = [1, 2, 3, 4, 5], k = 0
Output: [1, 2, 3, 4, 5]
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** is that the first `k` nodes form a contiguous prefix segment that needs to flip direction, while the tail (everything from position `k + 1` onward) stays in place. After the flip, the original `head` becomes the new tail of the reversed prefix, and the original `k`-th node becomes the new head. The two halves are then stitched together: the new tail (the original head) must point to whatever node currently sits at position `k + 1`.

The **pointer placement** mirrors the full-list reversal with one extra knob — a counter. `current = head`, `previous = null`, and a `count` variable that starts at zero. The three-pointer loop runs as long as `current` is not `null` AND `count < k`. The natural early-exit when `current` becomes `null` covers the case where `k` is larger than the list's length (just reverse the whole list). When the loop exits, `previous` holds the new head of the reversed prefix and `current` holds the first un-flipped node — exactly the successor the original head should point to for the stitch.

What **breaks if you reach for a single sweep without saving the original head**? The original head — which is the node you need to stitch to `current` after the loop — gets lost the moment the loop advances `previous` past it. The trick is to keep `head` as a stable reference throughout (it is never reassigned during the loop). After the loop, the line `head.next = current` performs the stitch: the original head (now the new tail of the reversed prefix) points to the first un-flipped node. Without this stitch, the reversed prefix's tail would still point to the second-to-last flipped node, producing a malformed list.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse First K Nodes |
|---|---|
| **Q1.** Does the problem ask for reversed order across a contiguous segment? | **Yes** — the first `k` nodes form the segment; the rest of the list is untouched. |
| **Q2.** Are the segment endpoints identifiable? | **Yes** — `start = head`, and the end is reached when the counter reaches `k` (or when `current` becomes `null`, whichever first). |
| **Q3.** Is the work strictly structural (only `next` pointers change)? | **Yes** — values are never read; only `current.next` and the final stitch `head.next = current` change. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — three references (`previous`, `current`, `next_node`) plus an integer counter, regardless of `n` or `k`. |

</details>
<details>
<summary><h2>Brute Force: Slice and Splice</h2></summary>


Walk the list to collect the first `k` values into an array, reverse the array, and write the reversed values back into the first `k` nodes' `val` fields. The structural chain is left untouched.

This is correct but costs `O(k)` extra space and conflates value movement with list reversal. The pattern's whole point is that the `next` pointers carry the order — flip them and the order flips for free, with no auxiliary storage.

</details>
<details>
<summary><h2>Key Insight: Same Loop, Add a Counter and a Stitch</h2></summary>


The three-pointer loop body is byte-identical to full-list reversal. The only differences are at the boundaries: a `count < k` guard on the loop condition (so the loop stops after `k` flips instead of running to the end), and a single stitching line after the loop (`head.next = current`) that reconnects the reversed prefix's new tail (the original head) to the first un-flipped node. The original `head` reference is the anchor — it never moves during the loop, so it is still available as the new tail when the stitch is needed.

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the prefix-bounded three-pointer loop, then stitch.

1. **Handle the no-op guard.** If `k <= 0`, return `head` unchanged. The early return avoids running the stitch line with a meaningless state.
2. **Initialise the references and counter.** Set `current = head`, `previous = null`, `count = 0`. The original `head` reference is preserved — it will become the new tail of the reversed prefix.
3. **Run the bounded three-pointer loop.** While `current` is not `null` AND `count < k`: snapshot `next_node = current.next`, flip `current.next = previous`, advance `previous = current` and `current = next_node`, increment `count`. The conjunction in the loop condition means a too-large `k` is handled implicitly — the loop exits when `current` runs off the end, which is the same as reversing the whole list.
4. **Stitch the reversed prefix to the unreversed remainder.** If `head` is not `null`, set `head.next = current`. After the loop, `current` is the first un-flipped node (or `null` if the loop ran past the end), and the original `head` is the reversed prefix's new tail.
5. **Return the new head.** `previous` holds the new head of the reversed prefix. Return it.

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
    def reverse_first_k_nodes(
        self, head: Optional[ListNode], k: int
    ) -> Optional[ListNode]:

        # if K is less than or equal to 0, return the original head
        if k <= 0:
            return head

        # Initialize pointers current and previous
        current: Optional[ListNode] = head
        previous: Optional[ListNode] = None
        count = 0

        while current is not None and count < k:

            # Save the address of next node
            next_node = current.next

            # Update the next of current node
            current.next = previous

            # Move previous to hold current node
            previous = current

            # Move current ahead
            current = next_node

            # Increment count
            count += 1

        # Connect the reversed sublist with the remaining part
        if head is not None:
            head.next = current

        return previous


# Examples from the problem statement
print(to_list(Solution().reverse_first_k_nodes(from_list([5, 7, 3, 10]), 2)))  # [7, 5, 3, 10]

# Edge cases
print(to_list(Solution().reverse_first_k_nodes(None, 3)))                       # []
print(to_list(Solution().reverse_first_k_nodes(from_list([1]), 1)))             # [1]
print(to_list(Solution().reverse_first_k_nodes(from_list([1, 2]), 2)))          # [2, 1]
print(to_list(Solution().reverse_first_k_nodes(from_list([1, 2, 3, 4, 5]), 0))) # [1, 2, 3, 4, 5]
print(to_list(Solution().reverse_first_k_nodes(from_list([1, 2, 3, 4, 5]), 1))) # [1, 2, 3, 4, 5]
print(to_list(Solution().reverse_first_k_nodes(from_list([1, 2, 3, 4, 5]), 5))) # [5, 4, 3, 2, 1]
print(to_list(Solution().reverse_first_k_nodes(from_list([1, 2, 3, 4, 5]), 3))) # [3, 2, 1, 4, 5]
print(to_list(Solution().reverse_first_k_nodes(from_list([1, 2, 3, 4, 5]), 7))) # [5, 4, 3, 2, 1]
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
        public ListNode reverseFirstKNodes(ListNode head, int k) {

            // if K is less than or equal to 0, return the original head
            if (k <= 0) {
                return head;
            }

            // Initialize pointers current and previous
            ListNode current = head;
            ListNode previous = null;
            int count = 0;

            while (current != null && count < k) {

                // Save the address of next node
                ListNode next = current.next;

                // Update the next of current node
                current.next = previous;

                // Move previous to hold current node
                previous = current;

                // Move current ahead
                current = next;

                // Increment count
                count++;
            }

            // Connect the reversed sublist with the remaining part
            if (head != null) {
                head.next = current;
            }

            return previous;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(toList(new Solution().reverseFirstKNodes(fromList(5, 7, 3, 10), 2)));  // [7, 5, 3, 10]

        // Edge cases
        System.out.println(toList(new Solution().reverseFirstKNodes(null, 3)));                    // []
        System.out.println(toList(new Solution().reverseFirstKNodes(fromList(1), 1)));             // [1]
        System.out.println(toList(new Solution().reverseFirstKNodes(fromList(1, 2), 2)));          // [2, 1]
        System.out.println(toList(new Solution().reverseFirstKNodes(fromList(1, 2, 3, 4, 5), 0))); // [1, 2, 3, 4, 5]
        System.out.println(toList(new Solution().reverseFirstKNodes(fromList(1, 2, 3, 4, 5), 1))); // [1, 2, 3, 4, 5]
        System.out.println(toList(new Solution().reverseFirstKNodes(fromList(1, 2, 3, 4, 5), 5))); // [5, 4, 3, 2, 1]
        System.out.println(toList(new Solution().reverseFirstKNodes(fromList(1, 2, 3, 4, 5), 3))); // [3, 2, 1, 4, 5]
        System.out.println(toList(new Solution().reverseFirstKNodes(fromList(1, 2, 3, 4, 5), 7))); // [5, 4, 3, 2, 1]
    }
}
```

### Dry Run

```
head = 5 → 7 → 3 → 10 → null,  k = 2

Init: current = 5, previous = null, count = 0
      (original head reference anchored at node 5)

Tick 1: current = 5, count = 0 (< 2)
  next_node    = 7
  current.next = null         → 5 → null
  previous     = 5
  current      = 7
  count        = 1

Tick 2: current = 7, count = 1 (< 2)
  next_node    = 3
  current.next = 5            → 7 → 5 → null
  previous     = 7
  current      = 3
  count        = 2

Loop exits — count == k = 2.
State: previous = 7, current = 3,  reversed prefix tail = original head (5)

Stitch: head.next = current   → 5.next = 3   →   7 → 5 → 3 → 10 → null

Return previous = 7.
Reversed-first-2 list: 7 → 5 → 3 → 10 → null ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(min(n, k))` | The loop runs at most `k` times and at most `n` times — whichever bound hits first. Other work (the guard, the stitch) is `O(1)`. |
| **Space** | `O(1)` | Three references plus an integer counter, regardless of `n` or `k`. |

### Edge Cases

| Case | What happens |
|---|---|
| `k <= 0` | Early return; original `head` returned unchanged. |
| `head is null` | Loop body never runs; the `if head is not None` guard skips the stitch; `previous` is `null`; return `null`. |
| `k == 1` | One flip runs (a no-op rewrite of `head.next` to `null`), then the stitch sets `head.next = current` back to the second node — the list is unchanged. |
| `k == n` | The loop flips every node and exits on `current is null`. The stitch writes `head.next = null` — already true. Result: the full list is reversed. |
| `k > n` | The loop exits early because `current` becomes `null` before `count` reaches `k`. The stitch is a no-op (`head.next = null`). Result: full reversal. |
| Single-node list, `k = 1` | One iteration (no-op flip), stitch writes `head.next = current = null` (already `null`). Return the single node. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Prefix reversal is full-list reversal plus a counter and a single stitching line. The original `head` reference is the anchor — it is never reassigned during the loop, so it remains available as the new tail when the stitch reconnects the prefix to the unreversed remainder.

</details>