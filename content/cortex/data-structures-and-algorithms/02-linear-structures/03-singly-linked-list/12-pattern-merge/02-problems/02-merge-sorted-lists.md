---
title: "Merge Sorted Lists"
summary: "Given the heads of two sorted linked lists headA and headB, write a function to merge the two lists into one sorted list by splicing together the nodes of each list and return the head of the merged list."
prereqs:
  - 12-pattern-merge/01-pattern
difficulty: easy
---

# Merge sorted lists

## Problem Statement

Given the heads of two sorted linked lists **headA** and **headB**, write a function to merge the two lists into one sorted list by splicing together the nodes of each list and return the head of the merged linked list.

## Examples

**Example 1:**
```
Input:  headA = [1, 2, 4], headB = [1, 3, 4]
Output: [1, 1, 2, 3, 4, 4]
Explanation: Both lists are sorted ascending. The output interleaves them in ascending order, ties broken by A.
```

**Example 2:**
```
Input:  headA = [1, 2, 3, 8, 9], headB = [6, 7]
Output: [1, 2, 3, 6, 7, 8, 9]
Explanation: B's nodes fall in the gap between A=3 and A=8. The drain step then attaches A's tail [8, 9].
```

**Example 3:**
```
Input:  headA = [1, 3, 5, 6, 7], headB = [2, 4]
Output: [1, 2, 3, 4, 5, 6, 7]
Explanation: B is fully consumed before A. The drain step attaches A's tail [5, 6, 7].
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a merge problem is that the output combines two sorted inputs into a single sorted chain, with the choice of "who contributes the next node" decided by a one-comparison selector on the current heads. Because both inputs are sorted, the smallest unmerged value across both lists is always at one of the two cursors — never deeper. A single `if currentA.val <= currentB.val` decides the winner in `O(1)`, and the splice loop appends one node per iteration in correct sorted order.

The **pointer placement** follows directly. Create a `dummy` node whose `.next` will become the real head. Initialise `tail = dummy`, `currentA = headA`, `currentB = headB`. Each iteration compares the two head values, splices the smaller (or `currentA` on ties) onto `tail`, advances that input's cursor, and advances `tail`. The loop continues while both cursors are non-`null`; when one empties, the drain step attaches the other's remaining suffix — already correctly sorted by the input contract — in a single splice.

What **breaks if you reach for a naive approach**? Collecting every value into an array, sorting it, and rebuilding a fresh linked list works in `O((n + m) log(n + m))` time and `O(n + m)` extra space. The sort throws away the sortedness of the inputs and pays a logarithmic penalty for the privilege; the rebuild allocates `n + m` brand-new nodes. The merge technique exploits the sorted-input invariant and does the same job in `O(n + m)` time and `O(1)` extra space, reusing the existing nodes.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Merge Sorted Lists |
|---|---|
| **Q1.** Does the problem combine two or more input lists into a single output list? | **Yes** — two sorted inputs collapse into one sorted output. |
| **Q2.** Can the choice be made by an `O(1)` selector on the current heads? | **Yes** — `currentA.val <= currentB.val` decides the winner with one comparison; sorted inputs guarantee no deeper scan is needed. |
| **Q3.** Are the input nodes rewirable into the output? | **Yes** — `tail.next = winner` splices a single input node onto the output; only `next` fields change. |
| **Q4.** Is `O(1)` extra space sufficient? | **Yes** — `dummy`, `tail`, `currentA`, `currentB` are four locals regardless of input size. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the dummy-head splice loop with a comparator selector that picks the smaller head.

1. **Initialise the output skeleton.** Create a `dummy` node and set `tail = dummy`. The dummy removes the "is this the first output node?" special case from the loop body.
2. **Initialise the input cursors.** Set `currentA = headA` and `currentB = headB`.
3. **Loop while both cursors are non-`null`.** The comparator needs both heads to compare; the moment one input empties, the drain step takes over.
4. **Inside the loop, pick the smaller head and splice it.** If `currentA.val <= currentB.val`, the winner is `currentA`: set `tail.next = currentA`, then `currentA = currentA.next`. Otherwise the winner is `currentB`: `tail.next = currentB`, then `currentB = currentB.next`. The `<=` (not `<`) makes the merge stable — ties go to `A`, preserving the relative order of equal values.
5. **Advance `tail`.** After every splice, `tail = tail.next` so the next splice lands at the new end.
6. **Drain the non-empty input.** When the loop exits, at most one cursor is non-`null`. If `currentA` is non-`null`, do `tail.next = currentA`; else if `currentB` is non-`null`, do `tail.next = currentB`. The suffix is already sorted and correctly chained.
7. **Return `dummy.next`.** This skips the throwaway dummy and returns the real head of the merged list.

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
    def merge_sorted_lists(
        self, head_a: Optional[ListNode], head_b: Optional[ListNode]
    ) -> Optional[ListNode]:

        # Create a dummy node and initialize the tail pointer
        dummy = ListNode(0)
        tail = dummy

        current_a: Optional[ListNode] = head_a
        current_b: Optional[ListNode] = head_b

        # Traverse both lists until one of them becomes empty
        while current_a is not None and current_b is not None:

            # If the value of the current node in list A is less than or
            # equal to the value of the current node in list B, append
            # the current node from list A to the merged list
            if current_a.val <= current_b.val:
                tail.next = current_a
                current_a = current_a.next

            # Otherwise, append the current node from list B to the
            # merged
            else:
                tail.next = current_b
                current_b = current_b.next

            # Move the tail pointer forward
            tail = tail.next

        # Append the remaining nodes from the non-empty list to the
        # merged list
        if current_a is not None:
            tail.next = current_a

        # Else if there are any remaining nodes in current_b, attach them
        # to the merged list
        elif current_b is not None:
            tail.next = current_b

        # Return the merged list (excluding the dummy node)
        return dummy.next


print(to_list(Solution().merge_sorted_lists(from_list([1, 2, 4]), from_list([1, 3, 4]))))       # [1, 1, 2, 3, 4, 4]
print(to_list(Solution().merge_sorted_lists(from_list([1, 2, 3, 8, 9]), from_list([6, 7]))))   # [1, 2, 3, 6, 7, 8, 9]
print(to_list(Solution().merge_sorted_lists(from_list([1, 3, 5, 6, 7]), from_list([2, 4]))))   # [1, 2, 3, 4, 5, 6, 7]

# Edge cases
print(to_list(Solution().merge_sorted_lists(None, from_list([1, 2]))))                          # [1, 2]
print(to_list(Solution().merge_sorted_lists(from_list([1, 2]), None)))                          # [1, 2]
print(to_list(Solution().merge_sorted_lists(from_list([1]), from_list([1]))))                   # [1, 1]
print(to_list(Solution().merge_sorted_lists(from_list([5]), from_list([1, 2, 3]))))             # [1, 2, 3, 5]
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
        public ListNode mergeSortedLists(ListNode headA, ListNode headB) {

            // Create a dummy node and initialize the tail pointer
            ListNode dummy = new ListNode(0);
            ListNode tail = dummy;

            ListNode currentA = headA;
            ListNode currentB = headB;

            // Traverse both lists until one of them becomes empty
            while (currentA != null && currentB != null) {

                // If the value of the current node in list A is less than or
                // equal to the value of the current node in list B, append
                // the current node from list A to the merged list
                if (currentA.val <= currentB.val) {
                    tail.next = currentA;
                    currentA = currentA.next;
                }

                // Otherwise, append the current node from list B to the
                // merged
                else {
                    tail.next = currentB;
                    currentB = currentB.next;
                }

                // Move the tail pointer forward
                tail = tail.next;
            }

            // Append the remaining nodes from the non-empty list to the
            // merged list
            if (currentA != null) {
                tail.next = currentA;
            }

            // Else if there are any remaining nodes in currentB, attach them
            // to the merged list
            else if (currentB != null) {
                tail.next = currentB;
            }

            // Return the merged list (excluding the dummy node)
            return dummy.next;
        }
    }

    public static void main(String[] args) {
        System.out.println(toList(new Solution().mergeSortedLists(fromList(1, 2, 4), fromList(1, 3, 4))));       // [1, 1, 2, 3, 4, 4]
        System.out.println(toList(new Solution().mergeSortedLists(fromList(1, 2, 3, 8, 9), fromList(6, 7))));   // [1, 2, 3, 6, 7, 8, 9]
        System.out.println(toList(new Solution().mergeSortedLists(fromList(1, 3, 5, 6, 7), fromList(2, 4))));   // [1, 2, 3, 4, 5, 6, 7]

        // Edge cases
        System.out.println(toList(new Solution().mergeSortedLists(null, fromList(1, 2))));                        // [1, 2]
        System.out.println(toList(new Solution().mergeSortedLists(fromList(1, 2), null)));                        // [1, 2]
        System.out.println(toList(new Solution().mergeSortedLists(fromList(1), fromList(1))));                    // [1, 1]
        System.out.println(toList(new Solution().mergeSortedLists(fromList(5), fromList(1, 2, 3))));              // [1, 2, 3, 5]
    }
}
```

### Dry Run

```
A = 1 → 2 → 4 → null   (Example 1)
B = 1 → 3 → 4 → null

Init: dummy = ⊙, tail = dummy, currentA = 1 (A), currentB = 1 (B)

Iter 1: 1 <= 1 → winner = currentA
        tail.next = currentA  → ⊙ → 1(A)
        currentA = 2; tail = 1(A)
Iter 2: 2 <= 1 ? no → winner = currentB
        tail.next = currentB  → 1(A) → 1(B)
        currentB = 3; tail = 1(B)
Iter 3: 2 <= 3 → winner = currentA
        tail.next = currentA  → 1(B) → 2
        currentA = 4; tail = 2
Iter 4: 4 <= 3 ? no → winner = currentB
        tail.next = currentB  → 2 → 3
        currentB = 4(B); tail = 3
Iter 5: 4 <= 4 → winner = currentA
        tail.next = currentA  → 3 → 4(A)
        currentA = null; tail = 4(A)
Iter 6: currentA == null → exit loop.

Drain: currentB = 4(B) (non-null) → tail.next = currentB → 4(A) → 4(B) → null.
       Output: ⊙ → 1(A) → 1(B) → 2 → 3 → 4(A) → 4(B) → null.

Return dummy.next = the 1 from A. ✓
```

### Result Size

The output contains every node from both inputs — `n + m` nodes total, where `n = len(A)` and `m = len(B)`. The merge is stable: equal values from `A` precede equal values from `B`, preserving relative order.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n + m)` | Each iteration consumes exactly one input node; the loop runs at most `n + m` times, and the drain splice is `O(1)`. Every comparison is `O(1)`. |
| **Space** | `O(1)` | Four local references (`dummy`, `tail`, `currentA`, `currentB`) regardless of input size. The output reuses the input nodes — no new nodes are allocated except the throwaway dummy. |

### Edge Cases

| Case | What happens |
|---|---|
| Both lists empty (`headA = headB = null`) | Loop guard fails at iteration 0; both drain checks fail. Return `dummy.next = null`. |
| Only `headA` is `null` | Loop guard fails immediately. Drain attaches `currentB`. Return `dummy.next = headB`. |
| Only `headB` is `null` | Loop guard fails immediately. Drain attaches `currentA`. Return `dummy.next = headA`. |
| Single-node inputs with equal values (`A = [1]`, `B = [1]`) | Iter 1: `1 <= 1` → A wins; `currentA = null`. Drain attaches `currentB`. Output `[1, 1]` — A before B by stability. |
| `A`'s values all less than `B`'s (`A = [1, 2, 3]`, `B = [5]`) | Three iters consume `A`; `currentA = null`. Drain attaches `[5]`. Output `[1, 2, 3, 5]`. |
| `A`'s values all greater than `B`'s (`A = [5]`, `B = [1, 2, 3]`) | Three iters consume `B`; `currentB = null`. Drain attaches `[5]`. Output `[1, 2, 3, 5]`. |
| All values equal (`A = [2, 2]`, `B = [2, 2]`) | Each iter picks A on tie; after 2 iters `currentA = null`. Drain attaches `[2, 2]` from B. Output `[2, 2, 2, 2]` — A's pair first, B's pair second, stable. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Merge sorted lists is the canonical merge variant — the selector is a single `<=` comparison, and the sorted-input invariant guarantees the smallest unmerged value is always at a cursor. The same skeleton powers `k`-way merge with a min-heap selector, where each extraction is `O(log k)` instead of `O(1)`.

</details>