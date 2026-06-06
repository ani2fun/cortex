---
title: "Relocate Node"
summary: "Move the last node of a singly linked list to the front with a single-pass two-cursor walk and two pointer rewrites."
prereqs:
  - 13-pattern-reorder/01-pattern
difficulty: easy
---

# Relocate node

## Problem Statement

Given the **head** of a singly linked list, write a function to move the last node of the list to the start and return the head of the reordered list.

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10, 6, 8]
Output: [8, 5, 7, 3, 10, 6]
Explanation: The last node with value 8 is detached from the end and prepended to the head; every other node keeps its relative order.
```

**Example 2:**
```
Input:  head = [5, 7]
Output: [7, 5]
Explanation: The two-node list flips because moving the last node to the front is the same as swapping the pair.
```

**Example 3:**
```
Input:  head = [5]
Output: [5]
Explanation: A single-node list is unchanged — the first and last node are the same object, so detach-and-prepend is a no-op.
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reorder problem is that the output reuses every input node, only with their `.next` fields rewired. No values are read or compared — the work is purely structural. That puts it squarely in the reorder family even though the rearrangement is small: detach the last node, prepend it to the head. The split-and-merge pipeline applies in its most degenerate form, where the split sub-lists are the singleton `[last_node]` and the prefix `[head, …, second-to-last]`, and the merge step is a one-line splice.

The **pointer placement** follows directly. Walk the input with two cursors: `current` chases the tail; `previous` lags one node behind so that when `current` lands on the last node, `previous` holds the node that should become the new tail. Once the walk ends, `previous.next = null` severs the original tail, then `current.next = head` prepends the severed node to the front. The original head becomes the second node of the output, and `current` (the relocated last node) becomes the new head.

What **breaks if you reach for a naive approach**? Copying every value into an array, popping the last element, prepending it, and rebuilding a fresh linked list works in `O(n)` time but pays `O(n)` extra memory and allocates `n` new nodes — for a problem whose answer requires rewriting exactly two `.next` fields. Walking the list twice (once to find the second-to-last node, once more to splice) is correct but adds an unnecessary second traversal. The two-cursor walk does the job in one pass and `O(1)` space.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Relocate Node |
|---|---|
| **Q1.** Does the problem rearrange the nodes of one input list in place? | **Yes** — the output is the same `n` nodes as the input with the last node now at the front; only two `.next` fields change. |
| **Q2.** Can the target be expressed as classifier + selector? | **Yes** — `f1` walks to the tail and splits off the last node; `f2 = "prepend"` joins the severed node onto the prefix in a single splice. |
| **Q3.** Are the sub-lists bounded in count and walkable in one pass? | **Yes** — exactly two sub-lists (the singleton last node and the prefix); the merge step is one pointer update. |
| **Q4.** Is `O(1)` extra space sufficient? | **Yes** — two cursors (`current`, `previous`) regardless of input size. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the reorder pipeline with a single-node split and a prepend merge.

1. **Short-circuit trivial inputs.** If `head` is `null` or `head.next` is `null`, return `head` unchanged. A list with zero or one node already satisfies the target shape — moving the last node to the front is a no-op.
2. **Walk to the last node with a two-cursor pair.** Start with `current = head` and `previous = null`. Loop while `current.next` is non-`null`; each iteration sets `previous = current`, then advances `current = current.next`. When the loop exits, `current` points at the last node and `previous` at the second-to-last node.
3. **Sever the last node from the prefix.** Set `previous.next = null`. The original list now ends at `previous`, and `current` is a detached singleton.
4. **Prepend the severed node onto the prefix.** Set `current.next = head` (the original head). The relocated last node now points at the original first node, and the prefix dangles behind it.
5. **Return the relocated last node as the new head.** `current` is now the head of the output. The output's length equals the input's length; only two `.next` writes occurred.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python run viz=linked-list viz-root=head
from typing import Optional, Tuple


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
    def split_last_node(
        self, head: ListNode
    ) -> Tuple[ListNode, ListNode]:
        current = head
        previous = None

        # Traverse the list until the last node is reached
        while current.next is not None:

            # Keep track of the previous node
            previous = current

            # Move to the next node
            current = current.next

        # Disconnect the last node
        if previous is not None:
            previous.next = None

        # Return {head of remaining list, last node}
        return head, current

    def merge_last_node(
        self,
        last_node: Optional[ListNode],
        first_node: Optional[ListNode],
    ) -> Optional[ListNode]:

        # If there is no last node, return the first node
        if not last_node:
            return first_node

        # Connect the last node to the first node
        last_node.next = first_node
        return last_node

    def relocate_node(
        self, head: Optional[ListNode]
    ) -> Optional[ListNode]:

        # If the list is empty or contains only one node, no need to
        # modify it
        if not head or not head.next:
            return head

        # Split the last node from the list
        first_node, last_node = self.split_last_node(head)

        # Merge the last node at the front
        return self.merge_last_node(last_node, first_node)


# Examples from the problem statement
print(to_list(Solution().relocate_node(from_list([5, 7, 3, 10, 6, 8]))))  # [8, 5, 7, 3, 10, 6]
print(to_list(Solution().relocate_node(from_list([5, 7]))))                # [7, 5]
print(to_list(Solution().relocate_node(from_list([5]))))                   # [5]

# Edge cases
print(to_list(Solution().relocate_node(None)))                             # []
print(to_list(Solution().relocate_node(from_list([1, 2, 3]))))             # [3, 1, 2]
print(to_list(Solution().relocate_node(from_list([9, 9, 9, 9]))))          # [9, 9, 9, 9]  (all same)
print(to_list(Solution().relocate_node(from_list([1, 2, 3, 4, 5]))))       # [5, 1, 2, 3, 4]
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
        private List<ListNode> splitLastNode(ListNode head) {
            ListNode current = head;
            ListNode previous = null;

            // Traverse the list until the last node is reached
            while (current.next != null) {

                // Keep track of the previous node
                previous = current;

                // Move to the next node
                current = current.next;
            }

            // Disconnect the last node
            if (previous != null) {
                previous.next = null;
            }

            // Return {head of remaining list, last node}
            return Arrays.asList(head, current);
        }

        private ListNode mergeLastNode(ListNode lastNode, ListNode firstNode) {

            // If there is no last node, return the first node
            if (lastNode == null) {
                return firstNode;
            }

            // Connect the last node to the first node
            lastNode.next = firstNode;
            return lastNode;
        }

        public ListNode relocateNode(ListNode head) {

            // If the list is empty or contains only one node, no need to
            // modify it
            if (head == null || head.next == null) {
                return head;
            }

            // Split the last node from the list
            List<ListNode> heads = splitLastNode(head);
            ListNode firstNode = heads.get(0);
            ListNode lastNode = heads.get(1);

            // Merge the last node at the front
            return mergeLastNode(lastNode, firstNode);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(toList(new Solution().relocateNode(fromList(5, 7, 3, 10, 6, 8))));  // [8, 5, 7, 3, 10, 6]
        System.out.println(toList(new Solution().relocateNode(fromList(5, 7))));                // [7, 5]
        System.out.println(toList(new Solution().relocateNode(fromList(5))));                   // [5]

        // Edge cases
        System.out.println(toList(new Solution().relocateNode(null)));                          // []
        System.out.println(toList(new Solution().relocateNode(fromList(1, 2, 3))));             // [3, 1, 2]
        System.out.println(toList(new Solution().relocateNode(fromList(9, 9, 9, 9))));          // [9, 9, 9, 9]  (all same)
        System.out.println(toList(new Solution().relocateNode(fromList(1, 2, 3, 4, 5))));       // [5, 1, 2, 3, 4]
    }
}
```


### Dry Run

```
head = 5 → 7 → 3 → 10 → 6 → 8 → null   (Example 1)

Init: current = head (=5), previous = null.

Iter 1: current.next = 7 (not null) → previous = current (=5); current = current.next (=7).
Iter 2: current.next = 3 (not null) → previous = current (=7); current = current.next (=3).
Iter 3: current.next = 10 (not null) → previous = current (=3); current = current.next (=10).
Iter 4: current.next = 6 (not null) → previous = current (=10); current = current.next (=6).
Iter 5: current.next = 8 (not null) → previous = current (=6); current = current.next (=8).
Iter 6: current.next = null → exit loop.
        current = 8 (the last node), previous = 6 (the second-to-last).

Sever: previous.next = null → list now ends at 6: 5 → 7 → 3 → 10 → 6 → null.
Prepend: current.next = head → 8 → 5 → 7 → 3 → 10 → 6 → null.

Return current = the 8. ✓
```

### Result Size

The output contains the same `n` nodes as the input — no nodes are added or dropped. Only two `.next` fields change (`previous.next` and `current.next`), so the structural diff between input and output is exactly two pointer writes.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | The two-cursor walk visits every node exactly once. The sever-and-prepend step is two `.next` writes, both `O(1)`. |
| **Space** | `O(1)` | Two local references (`current`, `previous`) regardless of input size. No allocations. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head = null`) | The guard `if not head` short-circuits and returns `null`. No traversal. |
| Single node (`head = [5]`) | The guard `if not head.next` short-circuits and returns the same `head`. The last node is also the first node — moving it is a no-op. |
| Two nodes (`head = [5, 7]`) | Iter 1 sets `previous = 5`, `current = 7`. Loop exits. `previous.next = null` → prefix `[5]`. `current.next = head` → `7 → 5`. Output `[7, 5]`. |
| Three nodes (`head = [1, 2, 3]`) | Walk reaches `current = 3`, `previous = 2`. Sever → `[1, 2]`. Prepend `3` → `3 → 1 → 2`. Output `[3, 1, 2]`. |
| All-equal values (`head = [9, 9, 9, 9]`) | Values are irrelevant; the walk still finds the last node by structural position. Output `[9, 9, 9, 9]` (structurally distinct rewiring, identical print). |
| Long list (`head = [1, 2, 3, 4, 5]`) | Walk reaches `current = 5`, `previous = 4`. Sever → `[1, 2, 3, 4]`. Prepend `5` → `5 → 1 → 2 → 3 → 4`. Output `[5, 1, 2, 3, 4]`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Relocate-node is the reorder pattern in its smallest form — the split sub-lists are a singleton and a prefix, and the merge step is one `.next` rewrite. The two-cursor walk turns "find the second-to-last node" into a single pass.

</details>