---
title: "Value Partition"
summary: "Stable-partition a singly linked list around a pivot X with the split-then-concatenate variant of the reorder pattern."
prereqs:
  - 13-pattern-reorder/01-pattern
difficulty: medium
---

# Value partition

## Problem Statement

Given the **head** of a singly linked list and a value **X**, write a function to partition the list such that all nodes less than X come before nodes greater than or equal to X and return the head of the reordered list. The original relative order of the nodes in each of the two partitions should be preserved.

## Examples

**Example 1:**
```
Input:  head = [1, 4, 3, 2, 5, 2], X = 3
Output: [1, 2, 2, 4, 3, 5]
Explanation: Less-than bucket = [1, 2, 2]; greater-or-equal bucket = [4, 3, 5]. Concatenate to get [1, 2, 2, 4, 3, 5]. Relative order is preserved within each bucket.
```

**Example 2:**
```
Input:  head = [2, 1], X = 2
Output: [1, 2]
Explanation: Less-than bucket = [1]; greater-or-equal bucket = [2]. Concatenate to get [1, 2].
```

**Example 3:**
```
Input:  head = [5, 1, 3, 2, 4], X = 3
Output: [1, 2, 5, 3, 4]
Explanation: Less-than bucket = [1, 2]; greater-or-equal bucket = [5, 3, 4]. Concatenate to get [1, 2, 5, 3, 4].
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reorder problem is that the output reuses every input node — only `.next` fields change — and the target order is decided by a simple `O(1)` classifier on each node's value. That makes the split-and-merge pipeline a clean fit: route nodes into two buckets by `val < X`, then concatenate. The "preserve relative order within each bucket" requirement is the key constraint, and it's satisfied automatically because each pass walks the input in forward order and appends to the bucket tail.

The **pointer placement** follows directly. Maintain four cursors: `less_head` / `less_tail` grow the bucket of nodes with `val < X`; `greater_head` / `greater_tail` grow the bucket of nodes with `val >= X`. A single `current` walks the input. Each iteration reads `current.val`, evaluates the comparison against `X`, and splices `current` onto the chosen bucket's tail. After the loop, both bucket tails terminate with `null`, and the merge step is `less_tail.next = greater_head` — one splice.

What **breaks if you reach for a naive approach**? Copying every value into two arrays (less-than, greater-or-equal), concatenating, and rebuilding a fresh linked list works in `O(n)` time but pays `O(n)` extra memory and allocates `n` new nodes. Trying to do an in-place "swap nodes when out of order" pass like array quickselect partition is much harder on a singly linked list — there's no `O(1)` way to swap two nodes that aren't adjacent, because every swap requires finding the predecessor of each node. The two-bucket split avoids the swap problem entirely: every node is appended exactly once, and the chain is never partially broken.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Value Partition |
|---|---|
| **Q1.** Does the problem rearrange the nodes of one input list in place? | **Yes** — every input node appears in the output exactly once; only `.next` fields change. |
| **Q2.** Can the target be expressed as classifier + selector? | **Yes** — `f1(node) = node.val < X` routes nodes into less / greater-or-equal buckets in `O(1)` per node; `f2 = concatenate` joins the buckets in `O(1)`. |
| **Q3.** Are the sub-lists bounded in count and walkable in one pass? | **Yes** — exactly two buckets; the merge step is a single `less_tail.next = greater_head` splice. |
| **Q4.** Is `O(1)` extra space sufficient? | **Yes** — two dummy heads plus five cursors (`less_tail`, `greater_tail`, `current`, and the dummy refs) regardless of input size. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the reorder pipeline with `f1 = (val < X)` and `f2 = concatenate`.

1. **Short-circuit trivial inputs.** If `head` is `null` or `head.next` is `null`, return `head` unchanged. A list with zero or one node is already trivially partitioned.
2. **Initialise the two bucket skeletons.** Create `less_head = ListNode(0)` and `less_tail = less_head`; create `greater_head = ListNode(0)` and `greater_tail = greater_head`. The dummies let every splice use the same three-line shape without a special case for the first node in each bucket.
3. **Walk the input with a single cursor.** Set `current = head`. Loop while `current` is non-`null`. Each iteration evaluates `current.val < X`.
4. **Inside the loop, splice into the chosen bucket.** If the value is less than `X`, set `less_tail.next = current` and advance `less_tail = less_tail.next`. Otherwise, set `greater_tail.next = current` and advance `greater_tail = greater_tail.next`. The append-to-tail discipline preserves the input's relative order within each bucket.
5. **Advance the walk.** After the bucket splice, set `current = current.next`. The splice rewrote `tail.next`, not `current.next`, so the next-pointer link to the rest of the input is still intact for the read.
6. **Terminate both buckets.** When the loop exits, set `less_tail.next = null` and `greater_tail.next = null`. Without this, the last node in each bucket would still chain into stale input suffixes.
7. **Concatenate the buckets.** Set `less_tail.next = greater_head.next` (the real head of the greater bucket). Handle the two edge cases inline: if `less_head.next` is `null`, return `greater_head.next`; if `greater_head.next` is `null`, return `less_head.next`.
8. **Return the head of the merged list.** Skip the throwaway `less_head` and return `less_head.next`.

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
    def split_list_by_value(
        self, head: Optional[ListNode], X: int
    ) -> List[Optional[ListNode]]:

        # Create dummy nodes to initialize the heads of two separate
        # lists. List for nodes with values less than X.
        less_head = ListNode(0)
        less_tail = less_head

        # List for nodes with values greater than or equal to X.
        greater_head = ListNode(0)
        greater_tail = greater_head

        # Start traversing the original list from the head.
        current = head

        # Traverse and split nodes based on the value of X.
        while current is not None:

            # If the value of the current node is less than X, it should
            # be appended to the list for nodes < X.
            if current.val < X:

                # Append current node to list for nodes < X.
                less_tail.next = current

                # Move less_tail to the newly added node.
                less_tail = less_tail.next

            # Otherwise, the value of the current node is greater than
            # or equal to X, and it should be appended to the list for
            # nodes >= X.
            else:

                # Append current node to list for nodes >= X.
                greater_tail.next = current

                # Move greater_tail to the newly added node.
                greater_tail = greater_tail.next

            # Proceed to the next node in the original list.
            current = current.next

        # End both lists by setting the next pointers of their tails to
        # None.
        less_tail.next = None
        greater_tail.next = None

        # Return heads of both lists, excluding dummy nodes.
        return [less_head.next, greater_head.next]

    def merge_less_and_greater_lists(
        self,
        less_head: Optional[ListNode],
        greater_head: Optional[ListNode],
    ) -> Optional[ListNode]:

        # If the first list (less_head) is empty, return greater_head as
        # the concatenated list.
        if less_head is None:
            return greater_head

        # If the second list (greater_head) is empty, return less_head as
        # the concatenated list.
        if greater_head is None:
            return less_head

        # Find the end of the first list (less_head) to append
        # greater_head.
        current = less_head
        while current is not None and current.next is not None:
            current = current.next

        # Append greater_head to the end of less_head.
        current.next = greater_head
        return less_head

    def value_partition(
        self, head: Optional[ListNode], X: int
    ) -> Optional[ListNode]:

        # Return the head if the list is empty or has only one node.
        if head is None or head.next is None:
            return head

        # Split the original list into two lists: nodes < X and nodes >=
        # X.
        less_head, greater_head = self.split_list_by_value(head, X)

        # Merge both lists and return the head of the combined list.
        return self.merge_less_and_greater_lists(less_head, greater_head)


# Examples from the problem statement
print(to_list(Solution().value_partition(from_list([1, 4, 3, 2, 5, 2]), 3)))  # [1, 2, 2, 4, 3, 5]
print(to_list(Solution().value_partition(from_list([2, 1]), 2)))               # [1, 2]

# Edge cases
print(to_list(Solution().value_partition(None, 5)))                            # []
print(to_list(Solution().value_partition(from_list([3]), 3)))                  # [3]  (single node >= X)
print(to_list(Solution().value_partition(from_list([1]), 3)))                  # [1]  (single node < X)
print(to_list(Solution().value_partition(from_list([3, 3, 3]), 3)))            # [3, 3, 3]  (all >= X)
print(to_list(Solution().value_partition(from_list([1, 2, 2]), 3)))            # [1, 2, 2]  (all < X)
print(to_list(Solution().value_partition(from_list([5, 1, 3, 2, 4]), 3)))      # [1, 2, 5, 3, 4]
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
        private List<ListNode> splitListByValue(ListNode head, int X) {

            // Create dummy nodes to initialize the heads of two separate
            // lists. List for nodes with values less than X.
            ListNode lessHead = new ListNode(0);
            ListNode lessTail = lessHead;

            // List for nodes with values greater than or equal to X.
            ListNode greaterHead = new ListNode(0);
            ListNode greaterTail = greaterHead;

            // Start traversing the original list from the head.
            ListNode current = head;

            // Traverse and split nodes based on the value of X.
            while (current != null) {

                // If the value of the current node is less than X, it should
                // be appended to the list for nodes < X.
                if (current.val < X) {

                    // Append current node to list for nodes < X.
                    lessTail.next = current;

                    // Move lessTail to the newly added node.
                    lessTail = lessTail.next;
                }

                // Otherwise, the value of the current node is greater than
                // or equal to X, and it should be appended to the list for
                // nodes >= X.
                else {

                    // Append current node to list for nodes >= X.
                    greaterTail.next = current;

                    // Move greaterTail to the newly added node.
                    greaterTail = greaterTail.next;
                }

                // Proceed to the next node in the original list.
                current = current.next;
            }

            // End both lists by setting the next pointers of their tails to
            // null.
            lessTail.next = null;
            greaterTail.next = null;

            // Return heads of both lists, excluding dummy nodes.
            return Arrays.asList(lessHead.next, greaterHead.next);
        }

        private ListNode mergeLessAndGreaterLists(
            ListNode lessHead,
            ListNode greaterHead
        ) {

            // If the first list (lessHead) is empty, return greaterHead as
            // the concatenated list.
            if (lessHead == null) {
                return greaterHead;
            }

            // If the second list (greaterHead) is empty, return lessHead as
            // the concatenated list.
            if (greaterHead == null) {
                return lessHead;
            }

            // Find the end of the first list (lessHead) to append
            // greaterHead.
            ListNode current = lessHead;
            while (current != null && current.next != null) {
                current = current.next;
            }

            // Append greaterHead to the end of lessHead.
            current.next = greaterHead;
            return lessHead;
        }

        public ListNode valuePartition(ListNode head, int X) {

            // Return the head if the list is empty or has only one node.
            if (head == null || head.next == null) {
                return head;
            }

            // Split the original list into two lists: nodes < X and nodes >=
            // X.
            List<ListNode> heads = splitListByValue(head, X);

            // Head of list with nodes < X.
            ListNode lessHead = heads.get(0);

            // Head of list with nodes >= X.
            ListNode greaterHead = heads.get(1);

            // Merge both lists and return the head of the combined list.
            return mergeLessAndGreaterLists(lessHead, greaterHead);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(toList(new Solution().valuePartition(fromList(1, 4, 3, 2, 5, 2), 3)));  // [1, 2, 2, 4, 3, 5]
        System.out.println(toList(new Solution().valuePartition(fromList(2, 1), 2)));               // [1, 2]

        // Edge cases
        System.out.println(toList(new Solution().valuePartition(null, 5)));                         // []
        System.out.println(toList(new Solution().valuePartition(fromList(3), 3)));                  // [3]  (single node >= X)
        System.out.println(toList(new Solution().valuePartition(fromList(1), 3)));                  // [1]  (single node < X)
        System.out.println(toList(new Solution().valuePartition(fromList(3, 3, 3), 3)));            // [3, 3, 3]  (all >= X)
        System.out.println(toList(new Solution().valuePartition(fromList(1, 2, 2), 3)));            // [1, 2, 2]  (all < X)
        System.out.println(toList(new Solution().valuePartition(fromList(5, 1, 3, 2, 4), 3)));      // [1, 2, 5, 3, 4]
    }
}
```


### Dry Run

```
head = 1 → 4 → 3 → 2 → 5 → 2 → null,  X = 3   (Example 1)

Init: less_head = ⊙_l, less_tail = ⊙_l,
      greater_head = ⊙_g, greater_tail = ⊙_g,
      current = 1.

Iter 1: 1 < 3 → less_tail.next = 1.   less: ⊙_l → 1.            current = 4.
Iter 2: 4 < 3 ? no → greater_tail.next = 4. greater: ⊙_g → 4.    current = 3.
Iter 3: 3 < 3 ? no → greater_tail.next = 3. greater: ⊙_g → 4 → 3. current = 2.
Iter 4: 2 < 3 → less_tail.next = 2.   less: ⊙_l → 1 → 2.        current = 5.
Iter 5: 5 < 3 ? no → greater_tail.next = 5. greater: ⊙_g → 4 → 3 → 5. current = 2.
Iter 6: 2 < 3 → less_tail.next = 2.   less: ⊙_l → 1 → 2 → 2.    current = null.

Terminate: less_tail.next = null, greater_tail.next = null.
less_head_real = 1, greater_head_real = 4.

Concatenate: less_tail.next = greater_head_real (=4).
Output: 1 → 2 → 2 → 4 → 3 → 5 → null.

Return less_head_real = the first 1. ✓
```

### Result Size

The output contains exactly `n` nodes — every input node appears in the output. The less-than bucket holds `k` nodes (where `k` is the count of values `< X`) and the greater-or-equal bucket holds `n - k`. Only `.next` fields are rewired; no values change.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | The split pass visits every node once. The concatenation pass walks the less-than bucket once more to find its tail — at most `k` extra steps, dominated by `O(n)`. |
| **Space** | `O(1)` | Two dummy nodes plus a handful of local references regardless of input size. The output reuses the input nodes — no fresh allocations beyond the throwaway dummies. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head = null`) | Guard `if head is None` returns `null` immediately. |
| Single node `< X` (`head = [1], X = 3`) | Guard `if head.next is None` returns `head` unchanged. Trivially partitioned. |
| Single node `>= X` (`head = [3], X = 3`) | Same guard returns `head` unchanged. Trivially partitioned. |
| All nodes `< X` (`head = [1, 2, 2], X = 3`) | Less bucket = `[1, 2, 2]`, greater bucket is empty. Concatenation handles `greater_head = null` by returning the less bucket unchanged. Output `[1, 2, 2]`. |
| All nodes `>= X` (`head = [3, 3, 3], X = 3`) | Less bucket is empty, greater bucket = `[3, 3, 3]`. Concatenation handles `less_head = null` by returning the greater bucket. Output `[3, 3, 3]`. |
| Pivot equals every value (`head = [3, 3, 3], X = 3`) | Same as above — `val < X` is false for every node, so all land in the greater bucket. Output `[3, 3, 3]`. |
| Mixed with stability test (`head = [5, 1, 3, 2, 4], X = 3`) | Less = `[1, 2]` (in input order); greater = `[5, 3, 4]` (in input order). Concatenate → `[1, 2, 5, 3, 4]`. The relative order within each bucket matches the input. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Value-partition is the stable-partition variant of reorder — the classifier reads `val < X` and the merge step is plain concatenation. The bucket-append discipline (append to the tail, never the head) is what guarantees the stability the problem requires.

</details>