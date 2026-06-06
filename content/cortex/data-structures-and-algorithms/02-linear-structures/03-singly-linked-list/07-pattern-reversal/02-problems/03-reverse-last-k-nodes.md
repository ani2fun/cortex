---
title: "Reverse Last K Nodes"
summary: "Given the head of a singly linked list and a non-negative integer k, write a function to reverse the last k nodes of the list and return the head of the reversed list."
prereqs:
  - 08-pattern-reversal/01-pattern
difficulty: medium
---

# Reverse last K nodes

## Problem Statement

Given the **head** of a singly linked list and a non-negative integer **k**, write a function to reverse the last `k` nodes of the list and return the head of the reversed list.

You need to reverse the list in place.

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10], k = 2
Output: [5, 7, 10, 3]
```

**Example 2:**
```
Input:  head = [1, 2, 3, 4, 5], k = 3
Output: [1, 2, 5, 4, 3]
```

**Example 3:**
```
Input:  head = [1, 2, 3, 4, 5], k = 5
Output: [5, 4, 3, 2, 1]
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** is that the last `k` nodes form a contiguous suffix segment. The prefix (the first `n − k` nodes) stays in place, and the suffix needs to flip direction. The reversed suffix's new head is the original tail; its new tail is the original `(n − k + 1)`-th node. The stitch is between the prefix and the reversed suffix: the prefix's last node (the `(n − k)`-th node) must point to the reversed suffix's new head.

The **pointer placement** requires one piece of bookkeeping that the prefix variant did not: we do not know where the suffix begins without first measuring the list. A two-pass walk does the work. The first pass counts the length. The second pass advances `current` to position `n − k` (1-indexed), which is the prefix's last node and the splice point. From there, `current.next` is the original head of the suffix — feed it to the full-list reversal helper, which returns the new suffix head. Finally, assign `current.next = reversedSuffixHead` and the lists are stitched.

What **breaks if you reach for a one-pass approach without measuring length**? You can't tell which suffix is "the last `k`" without knowing the total length. The fast-and-slow-pointers pattern (next chapter) can do this in one pass with two pointers spaced `k` apart, but it solves a different shape of problem; for this section's reversal pattern, the two-pass measure-then-reverse approach is the cleanest fit and stays at `O(n)` time / `O(1)` space.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse Last K Nodes |
|---|---|
| **Q1.** Does the problem ask for reversed order across a contiguous segment? | **Yes** — the last `k` nodes (positions `n − k + 1` through `n`) form the segment. |
| **Q2.** Are the segment endpoints identifiable? | **Yes** — after measuring the length, the suffix starts at the `(n − k + 1)`-th node and ends at the tail. The splice point is the `(n − k)`-th node, found by a counted walk. |
| **Q3.** Is the work strictly structural (only `next` pointers change)? | **Yes** — the length scan reads no values; the reversal helper only flips `next` pointers; the final stitch is one assignment. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — the length is an integer, the reversal helper uses three references, and the splice point is one more reference. |

</details>
<details>
<summary><h2>Brute Force: Collect into an Array</h2></summary>


Walk the list collecting all values into an array. Reverse the last `k` values of the array. Walk the list a second time writing values back into nodes. This is `O(n)` time but `O(n)` extra space, and again confuses value movement with pointer rewiring — the lesson the pattern exists to teach is precisely the opposite.

</details>
<details>
<summary><h2>Key Insight: Measure, Split, Reverse the Suffix, Stitch</h2></summary>


The pattern reuses the full-list reversal helper, applied to the suffix only. Because reversal needs a head pointer to start from, the algorithm walks to position `n − k` (the splice point) and hands `current.next` (the suffix's original head) to the helper. The helper returns the reversed suffix's new head. The stitch line — `current.next = reversedSuffixHead` — connects the prefix's last node to the new head. The prefix is never re-touched, and the new tail of the reversed suffix (which used to be the suffix's original head) already points to `null` because the helper terminates when `current` becomes `null`.

</details>
<details>
<summary><h2>Approach</h2></summary>


Measure the length, walk to the splice point, reverse the suffix, stitch.

1. **Handle the no-op guard.** If `k <= 0`, return `head` unchanged.
2. **Measure the list's length.** Walk from `head` once, counting nodes, to compute `length`. This costs `O(n)` time and is the only auxiliary work.
3. **Handle the full-list shortcut.** If `k >= length`, the suffix is the entire list. Delegate to the full-list reversal helper and return its result.
4. **Walk to the splice point.** Set `current = head` and advance `length − k − 1` times (a `for _ in range(1, length - k)` loop), ending with `current` at the `(length − k)`-th node — the prefix's last node.
5. **Reverse the suffix.** Pass `current.next` (the suffix's original head) to the full-list reversal helper. The helper returns the reversed suffix's new head.
6. **Stitch and return.** Assign `current.next = last_k_reverse_head`. The prefix is intact; the suffix is reversed; the splice connects them. Return the original `head` (which is still the head of the unchanged prefix).

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
    def length_of_list(self, head: Optional[ListNode]) -> int:
        length: int = 0

        # Traverse the list and increment the length until the end
        while head:
            length += 1
            head = head.next

        # Return the length
        return length

    def reverse_a_list(
        self, head: Optional[ListNode]
    ) -> Optional[ListNode]:

        # Initialize pointers current and previous
        current: Optional[ListNode] = head
        previous: Optional[ListNode] = None

        while current is not None:

            # Save the address of next node
            next_node = current.next

            # Update the next of current node
            current.next = previous

            # Move previous to hold current node
            previous = current

            # Move current ahead
            current = next_node

        return previous

    def reverse_last_k_nodes(
        self, head: Optional[ListNode], k: int
    ) -> Optional[ListNode]:

        # if K is less than or equal to 0, return the original head
        if k <= 0:
            return head

        # Find the length of the list
        length = self.length_of_list(head)

        # If k is greater than or equal to length, reverse the entire
        # list
        if k >= length:
            return self.reverse_a_list(head)

        # Find the (length - k)th node after which the reversal should
        # occur
        current = head
        for _ in range(1, length - k):
            current = current.next

        # Reverse the last k nodes
        last_k_reverse_head = self.reverse_a_list(current.next)

        # Connect the (length - k)th node to the new head
        current.next = last_k_reverse_head

        return head


# Examples from the problem statement
print(to_list(Solution().reverse_last_k_nodes(from_list([5, 7, 3, 10]), 2)))     # [5, 7, 10, 3]

# Edge cases
print(to_list(Solution().reverse_last_k_nodes(None, 2)))                          # []
print(to_list(Solution().reverse_last_k_nodes(from_list([1]), 1)))                # [1]
print(to_list(Solution().reverse_last_k_nodes(from_list([1, 2]), 1)))             # [1, 2]
print(to_list(Solution().reverse_last_k_nodes(from_list([1, 2]), 2)))             # [2, 1]
print(to_list(Solution().reverse_last_k_nodes(from_list([1, 2, 3, 4, 5]), 0)))   # [1, 2, 3, 4, 5]
print(to_list(Solution().reverse_last_k_nodes(from_list([1, 2, 3, 4, 5]), 3)))   # [1, 2, 5, 4, 3]
print(to_list(Solution().reverse_last_k_nodes(from_list([1, 2, 3, 4, 5]), 5)))   # [5, 4, 3, 2, 1]
print(to_list(Solution().reverse_last_k_nodes(from_list([1, 2, 3, 4, 5]), 10)))  # [5, 4, 3, 2, 1]
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
        private int lengthOfList(ListNode head) {
            int length = 0;

            // Traverse the list and increment the length until the end
            while (head != null) {
                length++;
                head = head.next;
            }

            // Return the length
            return length;
        }

        private ListNode reverseAList(ListNode head) {

            // Initialize pointers current and previous
            ListNode current = head;
            ListNode previous = null;

            while (current != null) {

                // Save the address of next node
                ListNode next = current.next;

                // Update the next of current node
                current.next = previous;

                // Move previous to hold current node
                previous = current;

                // Move current ahead
                current = next;
            }

            return previous;
        }

        public ListNode reverseLastKNodes(ListNode head, int k) {

            // if K is less than or equal to 0, return the original head
            if (k <= 0) {
                return head;
            }

            // Find the length of the list
            int length = lengthOfList(head);

            // If k is greater than or equal to length, reverse the entire
            // list
            if (k >= length) {
                return reverseAList(head);
            }

            // Find the (length - k)th node after which the reversal should
            // occur
            ListNode current = head;
            for (int i = 1; i < length - k; i++) {
                current = current.next;
            }

            // Reverse the last k nodes
            ListNode lastKReverseHead = reverseAList(current.next);

            // Connect the (length - k)th node to the new head
            current.next = lastKReverseHead;

            return head;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(toList(new Solution().reverseLastKNodes(fromList(5, 7, 3, 10), 2)));    // [5, 7, 10, 3]

        // Edge cases
        System.out.println(toList(new Solution().reverseLastKNodes(null, 2)));                      // []
        System.out.println(toList(new Solution().reverseLastKNodes(fromList(1), 1)));               // [1]
        System.out.println(toList(new Solution().reverseLastKNodes(fromList(1, 2), 1)));            // [1, 2]
        System.out.println(toList(new Solution().reverseLastKNodes(fromList(1, 2), 2)));            // [2, 1]
        System.out.println(toList(new Solution().reverseLastKNodes(fromList(1, 2, 3, 4, 5), 0)));  // [1, 2, 3, 4, 5]
        System.out.println(toList(new Solution().reverseLastKNodes(fromList(1, 2, 3, 4, 5), 3)));  // [1, 2, 5, 4, 3]
        System.out.println(toList(new Solution().reverseLastKNodes(fromList(1, 2, 3, 4, 5), 5)));  // [5, 4, 3, 2, 1]
        System.out.println(toList(new Solution().reverseLastKNodes(fromList(1, 2, 3, 4, 5), 10))); // [5, 4, 3, 2, 1]
    }
}
```

### Dry Run

```
head = 5 → 7 → 3 → 10 → null,  k = 2

Step 1: Measure length.
  Walk: 5, 7, 3, 10 → length = 4

Step 2: k (= 2) < length (= 4), so don't take the full-reversal shortcut.

Step 3: Walk to splice point — position length - k = 2.
  current starts at head (5).
  Loop range(1, length - k) = range(1, 2) — runs once.
    iteration: current = current.next = 7
  current now points at node 7 (the prefix's last node).

Step 4: Reverse the suffix starting at current.next (= 3).
  Call reverse_a_list(3 → 10 → null):
    Tick 1: next = 10, 3.next = null,  previous = 3,  current = 10
    Tick 2: next = null, 10.next = 3,  previous = 10, current = null
    Return previous = 10.
  Reversed suffix: 10 → 3 → null.

Step 5: Stitch — current.next = last_k_reverse_head.
  Node 7's .next = node 10.
  Final list: 5 → 7 → 10 → 3 → null ✓

Return head (= 5).
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | One pass to measure length, one partial walk of `n − k` steps to the splice point, one full reversal of the `k`-node suffix. Sum: `O(n + (n − k) + k) = O(n)`. |
| **Space** | `O(1)` | An integer for length, three references for the reversal helper, one for the splice point — independent of `n` and `k`. |

### Edge Cases

| Case | What happens |
|---|---|
| `k <= 0` | Early return; original `head` returned unchanged. |
| `head is null` | `length_of_list` returns `0`; the `k >= length` branch fires; the helper handles `null` by returning `null`. |
| `k == length` | Full-reversal shortcut fires; entire list is reversed. |
| `k > length` | Same as `k == length` — the helper reverses the whole list; the suffix bound is effectively clamped to the full list. |
| `k == 1` | The splice point is at position `n − 1`; the suffix is a one-node list; reversing it is a no-op; the stitch reassigns the same `next`. List unchanged. |
| Two nodes, `k = 1` | `length = 2`, splice point at position `1` (the head), suffix is the second node alone — reverse is a no-op, list unchanged. |
| Two nodes, `k = 2` | `k == length`; full-list reversal via the shortcut. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Suffix reversal is full-list reversal applied to a sublist — measure the length, walk to the splice point, hand the suffix to the reversal helper, then stitch. The splice point reference is the only piece of state that survives both the walk and the reversal call.

</details>