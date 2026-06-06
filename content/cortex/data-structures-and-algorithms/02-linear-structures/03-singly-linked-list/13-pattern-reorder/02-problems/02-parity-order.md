---
title: "Parity Order"
summary: "Group all odd-indexed nodes ahead of all even-indexed nodes in one pass, using the split-then-concatenate variant of the reorder pattern."
prereqs:
  - 13-pattern-reorder/01-pattern
difficulty: medium
---

# Parity order

## Problem Statement

Given the **head** of a singly linked list, write a function to group all the nodes that appear on odd indices together, followed by the nodes that appear on even indices, and return the head of the reordered list.

The indices start with `1`.

## Examples

**Example 1:**
```
Input:  head = [2, 1, 3, 4, 8]
Output: [2, 3, 8, 1, 4]
Explanation: 1-indexed positions: 1→2, 2→1, 3→3, 4→4, 5→8. Odd-indexed values [2, 3, 8] come first, then even-indexed values [1, 4].
```

**Example 2:**
```
Input:  head = []
Output: []
Explanation: The empty list has no nodes to reorder. Return null.
```

**Example 3:**
```
Input:  head = [1, 2, 3, 4]
Output: [1, 3, 2, 4]
Explanation: Odd-indexed values [1, 3] come first, then even-indexed values [2, 4].
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reorder problem is that the output reuses every input node in a different order — the work is purely structural, with no value comparison. The reorder is decided by each node's position in the input, which is an `O(1)` classifier you can evaluate while you walk the list. That makes the split-and-merge pipeline a clean fit: route nodes into two buckets by index parity, then concatenate the buckets.

The **pointer placement** follows directly. Maintain four cursors plus a counter. `odd_dummy` / `odd_tail` grow the odd-indexed sub-list; `even_dummy` / `even_tail` grow the even-indexed sub-list; `current` walks the input; `counter` (starting at `1`) tracks the current 1-based index. Each iteration evaluates `counter % 2 == 1` to choose a bucket, splices `current` onto that bucket's tail, advances `current` and increments `counter`. After the loop, both buckets terminate with `null`, then `odd_tail.next = even_dummy.next` concatenates them in a single splice.

What **breaks if you reach for a naive approach**? Copying every value into two arrays (one for odd indices, one for even), concatenating, and rebuilding a fresh linked list works in `O(n)` time but pays `O(n)` extra memory and allocates `n` new nodes. Trying to do it with in-place pointer swaps on a single list is fiddly — every swap risks corrupting the chain because there's no direct way to "move" a node forward without splicing through the rest of the list. The two-bucket split sidesteps both problems: each node is touched exactly once, and the chain is never partially broken.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Parity Order |
|---|---|
| **Q1.** Does the problem rearrange the nodes of one input list in place? | **Yes** — every input node appears in the output exactly once; only `.next` fields change. |
| **Q2.** Can the target be expressed as classifier + selector? | **Yes** — `f1(node, counter) = counter % 2` routes nodes into odd / even buckets in `O(1)` per node; `f2 = concatenate` joins the even bucket after the odd bucket in `O(1)`. |
| **Q3.** Are the sub-lists bounded in count and walkable in one pass? | **Yes** — exactly two buckets; the merge step is a single `odd_tail.next = even_head` splice. |
| **Q4.** Is `O(1)` extra space sufficient? | **Yes** — two dummy heads plus five cursors (`odd_tail`, `even_tail`, `current`, `counter`, and the dummy refs) regardless of input size. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the reorder pipeline with `f1 = counter % 2` and `f2 = concatenate`.

1. **Short-circuit trivial inputs.** If `head` is `null` or `head.next` is `null`, return `head` unchanged. A list with zero or one node already satisfies the target shape.
2. **Initialise the two bucket skeletons.** Create `odd_dummy = ListNode(0)` and `odd_tail = odd_dummy`; create `even_dummy = ListNode(0)` and `even_tail = even_dummy`. The dummies let every splice use the same three-line shape (`tail.next = current`, advance `current`, advance `tail`) without a special case for the first node in each bucket.
3. **Initialise the walk state.** Set `current = head` and `counter = 1`. The counter is 1-based to match the problem's "indices start at 1" rule.
4. **Loop while `current` is non-`null`.** Each iteration evaluates `counter % 2`. If the result is `1` (odd index), splice `current` onto `odd_tail` and advance `odd_tail`. Otherwise (even index), splice onto `even_tail` and advance `even_tail`.
5. **Inside the loop, advance the walk.** After the bucket splice, set `current = current.next` and `counter += 1`. The order matters: the splice rewrites `tail.next`, not `current.next`, so `current.next` still points into the input chain — we read it once more to advance.
6. **Terminate both buckets.** When the loop exits, set `odd_tail.next = null` and `even_tail.next = null`. Without this step, the buckets would still chain into stale input suffixes — every node's `.next` is still whatever the input had, except for the ones overwritten by splices.
7. **Concatenate the buckets.** Walk `odd_dummy.next`'s suffix to its tail (the loop's `odd_tail` already holds this reference), then set `odd_tail.next = even_dummy.next`. The merged list now starts at `odd_dummy.next` and ends at the last even-indexed node.
8. **Return the head of the merged list.** Skip the throwaway `odd_dummy` and return `odd_dummy.next`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python run viz=linked-list viz-root=head
from typing import Optional, List


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
    def split_by_parity(
        self, head: Optional[ListNode]
    ) -> List[Optional[ListNode]]:

        # Initialize head and tail references for the two split lists
        odd_dummy = ListNode(0)
        odd_tail = odd_dummy

        even_dummy = ListNode(0)
        even_tail = even_dummy

        # Create current reference to iterate through the list
        current = head

        # To track alternate positions
        counter = 1

        # Iterate through the list and split nodes into two lists
        while current is not None:

            # If the counter is odd then the node goes to the odd list
            if counter % 2 == 1:

                # `current` node goes to the odd split list
                odd_tail.next = current

                # Move odd_tail forward
                odd_tail = odd_tail.next

            # Otherwise, the node goes to the even list
            else:

                # `current` node goes to the even split list
                even_tail.next = current

                # Move even_tail forward
                even_tail = even_tail.next

            # Move to the next node in the original list
            current = current.next
            counter += 1

        # Terminate the odd list
        odd_tail.next = None

        # Terminate the even list
        even_tail.next = None

        return [odd_dummy.next, even_dummy.next]

    def merge_odd_and_even_lists(
        self, odd_head: Optional[ListNode], even_head: Optional[ListNode]
    ) -> Optional[ListNode]:

        # If the odd list is empty return the even list
        if odd_head is None:
            return even_head

        # If the even list is empty return the odd list
        if even_head is None:
            return odd_head

        # Traverse to the end of the odd list
        current = odd_head
        while current is not None and current.next is not None:
            current = current.next

        # Connect the even list at the end of the odd list
        current.next = even_head
        return odd_head

    def parity_order(
        self, head: Optional[ListNode]
    ) -> Optional[ListNode]:

        # If the list is empty or contains only one node, no splitting is
        # necessary
        if head is None or head.next is None:
            return head

        # Split the list odd and even lists
        odd_head, even_head = self.split_by_parity(head)

        # Append the even list at the end of the odd list and return
        # the head of the merged list
        return self.merge_odd_and_even_lists(odd_head, even_head)


# Examples from the problem statement
print(to_list(Solution().parity_order(from_list([2, 1, 3, 4, 8]))))        # [2, 3, 8, 1, 4]
print(to_list(Solution().parity_order(None)))                               # []

# Edge cases
print(to_list(Solution().parity_order(from_list([1]))))                     # [1]
print(to_list(Solution().parity_order(from_list([1, 2]))))                  # [1, 2]
print(to_list(Solution().parity_order(from_list([1, 2, 3]))))               # [1, 3, 2]
print(to_list(Solution().parity_order(from_list([1, 2, 3, 4]))))            # [1, 3, 2, 4]
print(to_list(Solution().parity_order(from_list([5, 5, 5, 5, 5]))))         # [5, 5, 5, 5, 5]  (all same)
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
        private List<ListNode> splitByParity(ListNode head) {

            // Initialize head and tail references for the two split lists
            ListNode oddDummy = new ListNode(0);
            ListNode oddTail = oddDummy;

            ListNode evenDummy = new ListNode(0);
            ListNode evenTail = evenDummy;

            // Create current reference to iterate through the list
            ListNode current = head;

            // To track alternate positions
            int counter = 1;

            // Iterate through the list and split nodes into two lists
            while (current != null) {

                // If the counter is odd then the node goes to the odd list
                if (counter % 2 == 1) {

                    // `current` node goes to the odd split list
                    oddTail.next = current;

                    // Move oddTail forward
                    oddTail = oddTail.next;
                }

                // Otherwise, the node goes to the even list
                else {

                    // `current` node goes to the even split list
                    evenTail.next = current;

                    // Move evenTail forward
                    evenTail = evenTail.next;
                }

                // Move to the next node in the original list
                current = current.next;
                counter++;
            }

            // Terminate the odd list
            oddTail.next = null;

            // Terminate the even list
            evenTail.next = null;

            return Arrays.asList(oddDummy.next, evenDummy.next);
        }

        private ListNode mergeOddAndEvenLists(
            ListNode oddHead,
            ListNode evenHead
        ) {

            // If the odd list is empty return the even list
            if (oddHead == null) {
                return evenHead;
            }

            // If the even list is empty return the odd list
            if (evenHead == null) {
                return oddHead;
            }

            // Traverse to the end of the odd list
            ListNode current = oddHead;
            while (current != null && current.next != null) {
                current = current.next;
            }

            // Connect the even list at the end of the odd list
            current.next = evenHead;
            return oddHead;
        }

        public ListNode parityOrder(ListNode head) {

            // If the list is empty or contains only one node, no splitting
            // is necessary
            if (head == null || head.next == null) {
                return head;
            }

            // Split the list odd and even lists
            List<ListNode> heads = splitByParity(head);
            ListNode oddHead = heads.get(0);
            ListNode evenHead = heads.get(1);

            // Append the even list at the end of the odd list and return
            // the head of the merged list
            return mergeOddAndEvenLists(oddHead, evenHead);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(toList(new Solution().parityOrder(fromList(2, 1, 3, 4, 8))));        // [2, 3, 8, 1, 4]
        System.out.println(toList(new Solution().parityOrder(null)));                            // []

        // Edge cases
        System.out.println(toList(new Solution().parityOrder(fromList(1))));                     // [1]
        System.out.println(toList(new Solution().parityOrder(fromList(1, 2))));                  // [1, 2]
        System.out.println(toList(new Solution().parityOrder(fromList(1, 2, 3))));               // [1, 3, 2]
        System.out.println(toList(new Solution().parityOrder(fromList(1, 2, 3, 4))));            // [1, 3, 2, 4]
        System.out.println(toList(new Solution().parityOrder(fromList(5, 5, 5, 5, 5))));         // [5, 5, 5, 5, 5]  (all same)
    }
}
```


### Dry Run

```
head = 2 → 1 → 3 → 4 → 8 → null   (Example 1)

Init: odd_dummy = ⊙_o, odd_tail = ⊙_o,
      even_dummy = ⊙_e, even_tail = ⊙_e,
      current = 2, counter = 1.

Iter 1: counter=1 (odd)  → odd_tail.next = current (=2). odd: ⊙_o → 2.
        advance: current = 1, counter = 2.
Iter 2: counter=2 (even) → even_tail.next = current (=1). even: ⊙_e → 1.
        advance: current = 3, counter = 3.
Iter 3: counter=3 (odd)  → odd_tail.next = current (=3). odd: ⊙_o → 2 → 3.
        advance: current = 4, counter = 4.
Iter 4: counter=4 (even) → even_tail.next = current (=4). even: ⊙_e → 1 → 4.
        advance: current = 8, counter = 5.
Iter 5: counter=5 (odd)  → odd_tail.next = current (=8). odd: ⊙_o → 2 → 3 → 8.
        advance: current = null, counter = 6.
Iter 6: current == null → exit loop.

Terminate: odd_tail.next = null, even_tail.next = null.
odd_head = odd_dummy.next = 2, even_head = even_dummy.next = 1.

Concatenate: walk odd_head to its tail (=8). Set tail.next = even_head (=1).
Output: 2 → 3 → 8 → 1 → 4 → null.

Return odd_head = the first 2. ✓
```

### Result Size

The output contains exactly `n` nodes — every input node appears in the output. `ceil(n/2)` of them are in the odd-indexed bucket and `floor(n/2)` in the even-indexed bucket. Only `.next` fields are rewired; no values change.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | The split pass visits every input node once. The concatenation pass walks the odd bucket once more to find its tail — at most `ceil(n/2)` extra steps, dominated by `O(n)`. |
| **Space** | `O(1)` | Two dummy nodes plus a handful of local references regardless of input size. The output reuses the input nodes — no fresh allocations beyond the throwaway dummies. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head = null`) | Guard `if head is None` returns `null` immediately. No traversal. |
| Single node (`head = [1]`) | Guard `if head.next is None` returns `head` unchanged. One node sits at index 1 (odd), and there's nothing to concatenate. |
| Two nodes (`head = [1, 2]`) | Iter 1 puts `1` in the odd bucket; Iter 2 puts `2` in the even bucket. Concatenate → `[1, 2]`. Output equals input. |
| Three nodes (`head = [1, 2, 3]`) | Odd bucket = `[1, 3]`; even bucket = `[2]`. Concatenate → `[1, 3, 2]`. |
| Four nodes (`head = [1, 2, 3, 4]`) | Odd bucket = `[1, 3]`; even bucket = `[2, 4]`. Concatenate → `[1, 3, 2, 4]`. |
| All-equal values (`head = [5, 5, 5, 5, 5]`) | The classifier reads the counter, not the value. Buckets split structurally: odd = `[5, 5, 5]` (positions 1, 3, 5); even = `[5, 5]` (positions 2, 4). Output prints `[5, 5, 5, 5, 5]` — structurally distinct rewiring, identical print. |
| Odd length list (`head = [a, b, c, d, e]`) | Odd bucket has `ceil(5/2) = 3` nodes; even bucket has `floor(5/2) = 2` nodes. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Parity-order is the canonical reorder example — the classifier reads a counter, not a value, and the merge step is plain concatenation. Master this one and every other concatenate-after-split variant (value-partition, even-odd-split) is a one-line swap of the classifier.

</details>