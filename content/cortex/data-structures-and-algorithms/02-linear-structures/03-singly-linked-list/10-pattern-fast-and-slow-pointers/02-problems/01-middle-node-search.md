---
title: "Middle Node Search"
summary: "Given the head of a singly linked list, write a function to find and return the reference of the middle node of this list."
prereqs:
  - 10-pattern-fast-and-slow-pointers/01-pattern
difficulty: easy
---

# Middle node search

## Problem Statement

Given the **head** of a singly linked list, write a function to find and return the reference of the middle node of this list.

If there are two middle nodes, return the reference of the second one.

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10, 6]
Output: 3
Explanation: Odd length, the unambiguous middle is the 3rd node.
```

**Example 2:**
```
Input:  head = [5, 7, 3, 10, 6, 8]
Output: 10
Explanation: Even length, two candidate middles (3 and 10). Return the second — node 10.
```

**Example 3:**
```
Input:  head = [42]
Output: 42
Explanation: A one-node list — head is both the start and the middle.
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a fast-and-slow problem is that the answer is at a fixed proportional position — the `1/2` point of the list's length. A singly linked list exposes no length field and no random access by index, but it does support a uniform forward walk. When two pointers walk forward at different speeds, their relative position encodes a proportion of however much ground has been covered. Set the ratio at 2:1 and the slower pointer is at the halfway mark the moment the faster one runs out of room.

The **pointer placement** follows directly. Both pointers start at `head` so the 2:1 invariant holds from tick zero — at tick `t`, `slow` is at index `t` and `fast` is at index `2 * t`. The loop continues while `fast` has room to take two more hops: `fast != null` (it exists) and `fast.next != null` (its successor exists, so `fast.next.next` is safe to read). When the guard fails, `fast` is either at the tail (odd length) or one past the tail (even length) — both cases place `slow` at the middle by the 2:1 ratio.

What **breaks if you reach for a naive approach**? Walking the list once to count `n` nodes, then walking again `n / 2` steps from the head, gives the right answer in `O(n)` time and `O(1)` space — same asymptotic cost. The penalty is constant-factor (the first half is walked twice) and structural — the caller must keep `head` reachable across both passes. The moment the problem composes with structural work (splitting at the middle, reversing the back half), the two-pass approach forces a redundant length recomputation. Fast-and-slow does the same job in one walk.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Middle Node Search |
|---|---|
| **Q1.** Does the problem ask for a node at a proportional position? | **Yes** — the middle is the `1/2` point of the list's length. |
| **Q2.** Can the position be computed in a single forward pass? | **Yes** — `slow` lands at the middle the moment `fast` runs out of room, no second walk needed. |
| **Q3.** Is the work at each step `O(1)`? | **Yes** — every tick performs one comparison and three pointer hops (one for `slow`, two for `fast`), independent of `n`. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — two local references (`slow`, `fast`) regardless of list length. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the 2:1 two-speed walk from the head until `fast` runs out of room.

1. **Initialise both pointers at the head.** Set `slow = head` and `fast = head`. The 2:1 invariant holds trivially at tick 0 — both pointers are at index 0.
2. **Guard the fast pointer's reach.** Loop while `fast != null` AND `fast.next != null`. The first clause prevents dereferencing a `null` cursor; the second guarantees `fast.next.next` is safe to read on the next line. The order matters — short-circuit evaluation requires `fast != null` first.
3. **Advance `slow` by one hop.** Set `slow = slow.next`. After tick `t`, `slow` is at index `t`.
4. **Advance `fast` by two hops.** Set `fast = fast.next.next`. After tick `t`, `fast` is at index `2 * t`.
5. **Return `slow` when the loop exits.** `fast` has either reached the tail (odd length — guard fails on `fast.next == null`) or stepped one past it (even length — guard fails on `fast == null`). Either way, `slow` is parked at the middle (second middle on even-length lists, by convention).

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
    def middle_node_search(
        self, head: Optional[ListNode]
    ) -> Optional[ListNode]:

        # Initialize slow pointer to the head of the list
        slow = head

        # Initialize fast pointer to the head of the list
        fast = head

        # Iterate until fast pointer reaches the end of the list
        while (
            fast is not None
            and fast.next is not None
            and slow is not None
        ):

            # Move slow pointer one step forward
            slow = slow.next

            # Move fast pointer two steps forward
            fast = fast.next.next

        # Return the middle node or the second middle node (in case of
        # even number of nodes)
        return slow


print(Solution().middle_node_search(from_list([5, 7, 3, 10, 6])).val)      # 3
print(Solution().middle_node_search(from_list([5, 7, 3, 10, 6, 8])).val)   # 10

# Edge cases
print(Solution().middle_node_search(from_list([1])).val)                    # 1
print(Solution().middle_node_search(from_list([1, 2])).val)                 # 2
print(Solution().middle_node_search(from_list([1, 2, 3])).val)              # 2
print(Solution().middle_node_search(from_list([1, 2, 3, 4])).val)           # 3
print(Solution().middle_node_search(from_list([1, 2, 3, 4, 5, 6, 7])).val) # 4
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

    static class Solution {
        public ListNode middleNodeSearch(ListNode head) {

            // Initialize slow pointer to the head of the list
            ListNode slow = head;

            // Initialize fast pointer to the head of the list
            ListNode fast = head;

            // Iterate until fast pointer reaches the end of the list
            while (fast != null && fast.next != null) {

                // Move slow pointer one step forward
                slow = slow.next;

                // Move fast pointer two steps forward
                fast = fast.next.next;
            }

            // Return the middle node or the second middle node (in case of
            // even number of nodes)
            return slow;
        }
    }

    public static void main(String[] args) {
        System.out.println(new Solution().middleNodeSearch(fromList(5, 7, 3, 10, 6)).val);      // 3
        System.out.println(new Solution().middleNodeSearch(fromList(5, 7, 3, 10, 6, 8)).val);   // 10

        // Edge cases
        System.out.println(new Solution().middleNodeSearch(fromList(1)).val);                    // 1
        System.out.println(new Solution().middleNodeSearch(fromList(1, 2)).val);                 // 2
        System.out.println(new Solution().middleNodeSearch(fromList(1, 2, 3)).val);              // 2
        System.out.println(new Solution().middleNodeSearch(fromList(1, 2, 3, 4)).val);           // 3
        System.out.println(new Solution().middleNodeSearch(fromList(1, 2, 3, 4, 5, 6, 7)).val); // 4
    }
}
```

### Dry Run

```
head = 5 → 7 → 3 → 10 → 6 → null   (Example 1, odd length n=5)

Init: slow = 5, fast = 5

Tick 1: guard — fast=5, fast.next=7 → continue
        slow = slow.next  → slow = 7
        fast = fast.next.next → fast = 3

Tick 2: guard — fast=3, fast.next=10 → continue
        slow = slow.next  → slow = 3
        fast = fast.next.next → fast = 6

Tick 3: guard — fast=6, fast.next=null → exit

Return slow = 3. ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | `fast` traverses the list in `n / 2` ticks; each tick performs `O(1)` work. Total node visits across both pointers is `1.5 * n`. |
| **Space** | `O(1)` | Two local references (`slow`, `fast`) regardless of list length. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | Loop guard fails on `fast != null` at tick 0; `slow` is still `null`; return `null`. |
| Single node (`[42]`) | Loop guard fails on `fast.next != null` at tick 0; `slow` is still `head`; return `head`. |
| Two nodes (`[1, 2]`) | Tick 1: `slow = 2`, `fast = null`. Guard fails. Return `slow = 2` — the second middle, by convention. |
| Three nodes (`[1, 2, 3]`) | Tick 1: `slow = 2`, `fast = 3`. Guard fails on `fast.next == null`. Return `slow = 2` — the true middle. |
| Even length, four nodes (`[1, 2, 3, 4]`) | Tick 1: `slow = 2`, `fast = 3`. Tick 2: `slow = 3`, `fast = null`. Guard fails. Return `slow = 3` — the second middle. |
| All equal values (`[7, 7, 7, 7, 7]`) | Values are never inspected; the same three ticks run and return the middle node by identity (the third `7`). |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The 2:1 two-speed walk is the canonical instance of fast-and-slow — `slow = fast = head`, advance until `fast` or `fast.next` is `null`, return `slow`. Every other problem in this section reuses this loop verbatim, then layers additional work on top.

</details>