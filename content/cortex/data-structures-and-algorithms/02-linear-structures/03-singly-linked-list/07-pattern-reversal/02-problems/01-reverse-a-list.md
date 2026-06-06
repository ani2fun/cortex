---
title: "Reverse a List"
summary: "Given the head of a singly linked list, write a function to reverse the list and return the head of the reversed list."
prereqs:
  - 08-pattern-reversal/01-pattern
difficulty: easy
---

# Reverse a list

## Problem Statement

Given the **head** of a singly linked list, write a function to reverse the list and return the head of the reversed list.

You need to reverse the list in place.

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10]
Output: [10, 3, 7, 5]
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


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reversal problem is that the entire list is a single contiguous segment whose `next` pointers all need to flip direction. A singly linked list exposes only forward links — node `i` knows about node `i+1` but never about node `i-1`. Reversal turns that constraint on its head: after the rewrite, every node's `next` must point to its former predecessor. The work is purely structural; node values are never read or written.

The **pointer placement** follows directly. The three-pointer loop maintains three roles: `previous` (the most recently rewired node, which is the new successor for the next flip), `current` (the node being rewired this tick), and a local snapshot `next` (the original successor of `current`, captured *before* `current.next` is clobbered). `previous` starts as `null` because the original head will become the new tail and its `next` must be `null`. `current` starts at `head`, and the loop terminates when `current` becomes `null` — the natural end-of-list sentinel for a singly linked list.

What **breaks if you reach for a naive approach**? Copying values into an array, reversing the array, and writing values back works in `O(n)` time but uses `O(n)` extra space — and it does not generalise to splicing a reversed segment back into a larger list, which every other problem in this section requires. Recursion runs the same algorithm in `O(n)` time but uses `O(n)` stack space; for a 10-million-node list that blows the default stack on most languages. Only the iterative three-pointer loop hits the `O(n)` time / `O(1)` space target.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse a List |
|---|---|
| **Q1.** Does the problem ask for reversed order across a contiguous segment? | **Yes** — the entire list is the segment, from `head` to the tail. |
| **Q2.** Are the segment endpoints identifiable? | **Yes** — `start = head`, and the end is reached when `current` becomes `null`. No positional walk is needed. |
| **Q3.** Is the work strictly structural (only `next` pointers change)? | **Yes** — node values are never inspected; the rewrite decision depends only on the chain's structure. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — three local references (`previous`, `current`, `next_node`) regardless of list length. |

</details>
<details>
<summary><h2>Brute Force: Copy Values into an Array</h2></summary>


Walk the list and append each value to a Python list (or Java `ArrayList`). Reverse the array. Walk the list again and assign each reversed value back into the corresponding node's `val` field. The structural chain (the `next` pointers) is left untouched; only the values move.

This is correct but costs `O(n)` time AND `O(n)` extra space. It also misses the point of the pattern: reversal is a pointer-rewiring operation, not a value-shuffling operation. The moment a later problem asks you to reverse a sublist and splice it back, the array-copy approach has no natural place to slot in the splice.

</details>
<details>
<summary><h2>Key Insight: Flip Pointers One at a Time, Saving the Forward Link First</h2></summary>


The chain `5 → 7 → 3 → 10 → null` becomes `10 → 3 → 7 → 5 → null` by flipping every `next`. The catch is that flipping `current.next` destroys the only path to the rest of the list — so the forward link must be saved into a local variable *before* the flip. Three references suffice: `previous` (where the flipped `next` should point), `current` (the node being flipped), and `next_node` (the snapshot of `current.next` taken at the top of the tick). The loop terminates when `current` reaches `null`, at which point `previous` holds the new head.

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the three-pointer loop from the head until `current` becomes `null`.

1. **Initialise the references.** Set `current = head` and `previous = null`. The starting `previous` is `null` because the original head will become the new tail, and its `next` must be `null` to terminate the reversed list.
2. **Snapshot the forward link.** Inside the loop body, set `next_node = current.next` *first*, before any rewrite. This is the only line that keeps the rest of the list reachable after the flip on the next line.
3. **Flip the back link.** Set `current.next = previous`. The current node now points to its former predecessor — the chain is rewritten one node at a time.
4. **Advance both trailing pointers.** Set `previous = current`, then `current = next_node`. The two trailing references march in lockstep, one node forward per tick.
5. **Return the new head.** When the loop exits (`current is null`), `previous` is the former tail and the new head of the reversed list. Return it.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution


```python run viz=linked-list viz-root=head
from typing import Optional, List, Any


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


# Example from the problem statement
print(to_list(Solution().reverse_a_list(from_list([5, 7, 3, 10]))))   # [10, 3, 7, 5]

# Edge cases
print(to_list(Solution().reverse_a_list(None)))                        # []
print(to_list(Solution().reverse_a_list(from_list([42]))))             # [42]
print(to_list(Solution().reverse_a_list(from_list([1, 2]))))           # [2, 1]
print(to_list(Solution().reverse_a_list(from_list([1, 2, 3]))))        # [3, 2, 1]
print(to_list(Solution().reverse_a_list(from_list([1, 1, 1]))))        # [1, 1, 1]
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
        public ListNode reverseAList(ListNode head) {

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
    }

    public static void main(String[] args) {
        // Example from the problem statement
        System.out.println(toList(new Solution().reverseAList(fromList(5, 7, 3, 10))));   // [10, 3, 7, 5]

        // Edge cases
        System.out.println(toList(new Solution().reverseAList(null)));                     // []
        System.out.println(toList(new Solution().reverseAList(fromList(42))));             // [42]
        System.out.println(toList(new Solution().reverseAList(fromList(1, 2))));           // [2, 1]
        System.out.println(toList(new Solution().reverseAList(fromList(1, 2, 3))));        // [3, 2, 1]
        System.out.println(toList(new Solution().reverseAList(fromList(1, 1, 1))));        // [1, 1, 1]
    }
}
```

### Dry Run

```
head = 5 → 7 → 3 → 10 → null

Init: current = 5, previous = null

Tick 1: current = 5, previous = null
  next_node    = 7
  current.next = null        → 5 → null
  previous     = 5
  current      = 7

Tick 2: current = 7, previous = 5
  next_node    = 3
  current.next = 5           → 7 → 5 → null
  previous     = 7
  current      = 3

Tick 3: current = 3, previous = 7
  next_node    = 10
  current.next = 7           → 3 → 7 → 5 → null
  previous     = 3
  current      = 10

Tick 4: current = 10, previous = 3
  next_node    = null
  current.next = 3           → 10 → 3 → 7 → 5 → null
  previous     = 10
  current      = null

Loop ends. Return previous = 10.
Reversed list: 10 → 3 → 7 → 5 → null ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | Every node is visited exactly once — one snapshot, one flip, one advance per node. |
| **Space** | `O(1)` | Three local references (`previous`, `current`, `next_node`) regardless of list length. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | The loop condition `current is not None` fails immediately; `previous` is still `null`; return `null`. |
| Single node | One tick runs: `next_node = null`, `current.next = null` (already was), `previous = head`, `current = null`. Return `head`. |
| Two nodes (`a → b`) | Tick 1 flips `a.next` to `null`; tick 2 flips `b.next` to `a`. Return `b`. |
| All equal values (`1 → 1 → 1`) | Values are never inspected; the same three flips run, returning a list with identical values but a different node ordering by identity. |
| Already-reversed list | The algorithm has no fast path — it still walks every node and flips every pointer, costing `O(n)`. |
| Very long list (10⁷ nodes) | Iterative `O(1)` space — no stack overflow. The recursive variant would crash here. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The full-list reversal is the simplest instance of the pattern — `start = head`, `previous = null`, stop when `current` reaches `null`. Memorising this loop body unlocks every other variant in this section, because only the sentinel choices change.

</details>