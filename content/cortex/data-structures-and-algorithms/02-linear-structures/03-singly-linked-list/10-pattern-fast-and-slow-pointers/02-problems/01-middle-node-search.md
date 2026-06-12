---
title: "Middle Node Search"
summary: "Given the head of a singly linked list, write a function to find and return the reference of the middle node of this list."
prereqs:
  - 10-pattern-fast-and-slow-pointers/01-pattern
difficulty: easy
kind: problem
topics: [fast-and-slow-pointers, singly-linked-list]
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

## Constraints

- `1 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def middle_node_search(self, head):
        # Your code goes here — slow = fast = head, while fast and fast.next,
        # advance slow by 1 and fast by 2. Return slow.val.
        pass

def build_list(values):              # [1, 2, 3] → 1 → 2 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

head = build_list(ast.literal_eval(input()))   # the test case's head
print(Solution().middle_node_search(head))
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode next;
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    static class Solution {
        int middleNodeSearch(ListNode head) {
            // Your code goes here — slow = fast = head, while fast and fast.next,
            // advance slow by 1 and fast by 2. Return slow.val.
            return 0;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().middleNodeSearch(head));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 → 2 → 3 → null
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[5, 7, 3, 10, 6]" }
  ],
  "cases": [
    { "args": { "head": "[5, 7, 3, 10, 6]" }, "expected": "3" },
    { "args": { "head": "[5, 7, 3, 10, 6, 8]" }, "expected": "10" },
    { "args": { "head": "[42]" }, "expected": "42" },
    { "args": { "head": "[1, 2]" }, "expected": "2" },
    { "args": { "head": "[1, 2, 3]" }, "expected": "2" },
    { "args": { "head": "[1, 2, 3, 4]" }, "expected": "3" },
    { "args": { "head": "[1, 2, 3, 4, 5, 6, 7]" }, "expected": "4" }
  ]
}
```

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
5. **Return `slow.val` when the loop exits.** `fast` has either reached the tail (odd length — guard fails on `fast.next == null`) or stepped one past it (even length — guard fails on `fast == null`). Either way, `slow` is parked at the middle (second middle on even-length lists, by convention).

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

class Solution:
    def middle_node_search(self, head):

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
        return slow.val

def build_list(values):              # [1, 2, 3] → 1 → 2 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

head = build_list(ast.literal_eval(input()))   # the test case's head
print(Solution().middle_node_search(head))
```

```java solution
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode next;
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    static class Solution {
        int middleNodeSearch(ListNode head) {

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
            return slow.val;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().middleNodeSearch(head));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 → 2 → 3 → null
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
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
head = 5 → 7 → 3 → 10 → 6 → null   (Example 1, odd length n=5)

Init: slow = 5, fast = 5

Tick 1: guard — fast=5, fast.next=7 → continue
        slow = slow.next  → slow = 7
        fast = fast.next.next → fast = 3

Tick 2: guard — fast=3, fast.next=10 → continue
        slow = slow.next  → slow = 3
        fast = fast.next.next → fast = 6

Tick 3: guard — fast=6, fast.next=null → exit

Return slow.val = 3. ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | `fast` traverses the list in `n / 2` ticks; each tick performs `O(1)` work. Total node visits across both pointers is `1.5 * n`. |
| **Space** | `O(1)` | Two local references (`slow`, `fast`) regardless of list length. |

### Edge Cases

| Case | What happens |
|---|---|
| Single node (`[42]`) | Loop guard fails on `fast.next != null` at tick 0; `slow` is still `head`; return `head.val`. |
| Two nodes (`[1, 2]`) | Tick 1: `slow = 2`, `fast = null`. Guard fails. Return `slow.val = 2` — the second middle, by convention. |
| Three nodes (`[1, 2, 3]`) | Tick 1: `slow = 2`, `fast = 3`. Guard fails on `fast.next == null`. Return `slow.val = 2` — the true middle. |
| Even length, four nodes (`[1, 2, 3, 4]`) | Tick 1: `slow = 2`, `fast = 3`. Tick 2: `slow = 3`, `fast = null`. Guard fails. Return `slow.val = 3` — the second middle. |
| All equal values (`[7, 7, 7, 7, 7]`) | Values are never inspected; the same three ticks run and return the middle node's value. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The 2:1 two-speed walk is the canonical instance of fast-and-slow — `slow = fast = head`, advance until `fast` or `fast.next` is `null`, return `slow`. Every other problem in this section reuses this loop verbatim, then layers additional work on top.

</details>
