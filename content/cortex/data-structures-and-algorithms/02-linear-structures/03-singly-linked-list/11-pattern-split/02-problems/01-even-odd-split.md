---
title: "Even Odd Split"
summary: "Given the head of a singly linked list, write a function to split the list into two separate lists such that the first list contains the nodes with even values and the second list contains the nodes w"
prereqs:
  - 11-pattern-split/01-pattern
difficulty: easy
---

# Even odd split

## Problem Statement

Given the **head** of a singly linked list, write a function to split the list into two separate lists such that the first list contains the nodes with even values and the second list contains the nodes with odd values. Your function should return the heads of both these lists.

## Examples

**Example 1:**
```
Input:  head = [5, 2, 3, 10, 6, 8]
Output: [[2, 10, 6, 8], [5, 3]]
Explanation: Even-valued nodes (2, 10, 6, 8) keep source order in the first
             output; odd-valued nodes (5, 3) keep source order in the second.
```

**Example 2:**
```
Input:  head = [4, 2, 6, 10]
Output: [[4, 2, 6, 10], []]
Explanation: All values are even — the entire list flows into bucket 0;
             the odd bucket is empty.
```

**Example 3:**
```
Input:  head = [1]
Output: [[], [1]]
Explanation: A single odd node — even bucket stays empty, odd bucket holds [1].
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a split problem is that each node's destination is decided by a *local* test on the node itself — `current.val % 2`. Nothing about the rest of the list influences where a given node lands. That locality is the split pattern's signal: when a classifier reads only the node in front of it, the routing skeleton (one dummy and one tail per bucket) handles everything else without any look-ahead or backtracking.

The **bucket placement** follows directly. With `k = 2`, allocate two dummy nodes — one to anchor the even list, one to anchor the odd list — and two tail pointers initially equal to their dummies. As the walker visits each node, the parity test picks a bucket and the tail-append wires the node into the right chain. The dummies absorb the "first node into this bucket" special case; without them, every append would need a guard for whether the bucket is still empty. After the walk, sealing both tails (`even_tail.next = null`, `odd_tail.next = null`) severs whichever original-list edge was the last one each tail inherited.

What **breaks if you reach for a naive approach**? The two-pass solution walks the list once collecting evens (allocating fresh nodes), then a second time collecting odds — `O(n)` time but `O(n)` extra space, plus a copy step that mutates nothing in the original list (so the caller still holds the original chain). The other naive option — two passes that re-thread instead of copy — has to guard against routing the same node twice and against breaking the original chain mid-pass. The split template is one pass, no allocations beyond two dummies, and never touches a node it has already routed.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Even Odd Split |
|---|---|
| **Q1.** Does the problem ask to partition the input into multiple output lists? | **Yes** — exactly two output lists, by parity of the node value. |
| **Q2.** Can each node's bucket be computed locally? | **Yes** — `idx = 0 if current.val % 2 == 0 else 1` reads only `current.val`. |
| **Q3.** Is the work at each step `O(1)`? | **Yes** — one modulo, one tail-append, one pointer advance per node. |
| **Q4.** Can output lists share original nodes (re-linked, not copied)? | **Yes** — the problem returns the original nodes re-threaded into two chains, no fresh allocations needed. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Walk the original list once. Route each node to one of two pre-anchored output chains by parity.

1. **Allocate two dummies and two tails.** Create `even_dummy` and `odd_dummy` as placeholder head nodes; set `even_tail = even_dummy` and `odd_tail = odd_dummy`. The dummies remove the "first append" special case for each bucket.
2. **Initialise the walker at the head.** Set `current = head`. The original list is walked in source order, each node exactly once.
3. **For each node, classify by parity.** If `current.val % 2 == 0`, the node belongs to the even bucket; otherwise, the odd bucket.
4. **Append `current` to its bucket's tail.** Set `tail.next = current`, then `tail = current` so subsequent appends to the same bucket land at the new end.
5. **Advance the walker.** Set `current = current.next` before any further mutation. The original forward chain is still intact at this point because the append only writes `tail.next`, not `current.next`.
6. **Seal both buckets after the walk.** Set `even_tail.next = null` and `odd_tail.next = null` so neither output chain inherits a stray edge into the other bucket.
7. **Return the real heads.** Return `[even_dummy.next, odd_dummy.next]` to skip past the placeholders.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python run viz=linked-list viz-root=head
from typing import List, Optional


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
    def even_odd_split(
        self, head: Optional[ListNode]
    ) -> List[Optional[ListNode]]:

        # Initialize head and tail references for the two split lists
        even_dummy = ListNode(0)
        even_tail = even_dummy

        odd_dummy = ListNode(0)
        odd_tail = odd_dummy

        # Create current reference to iterate through the list
        current = head

        # Iterate through the list and split nodes into two lists
        while current is not None:

            # If the current node's value is even then the node goes to
            # the even list
            if current.val % 2 == 0:

                # `current` node goes to the even split list
                even_tail.next = current

                # Move even_tail forward
                even_tail = even_tail.next

            # Otherwise, the node goes to the odd list
            else:

                # `current` node goes to the odd split list
                odd_tail.next = current

                # Move odd_tail forward
                odd_tail = odd_tail.next

            # Move to the next node in the original list
            current = current.next

        # Terminate the even list
        even_tail.next = None

        # Terminate the odd list
        odd_tail.next = None

        return [even_dummy.next, odd_dummy.next]


e, o = Solution().even_odd_split(from_list([5, 2, 3, 10, 6, 8]))
print(to_list(e), to_list(o))   # [2, 10, 6, 8] [5, 3]

e, o = Solution().even_odd_split(from_list([4, 2, 6, 10]))
print(to_list(e), to_list(o))   # [4, 2, 6, 10] []

# Edge cases
e, o = Solution().even_odd_split(None)
print(to_list(e), to_list(o))   # [] []

e, o = Solution().even_odd_split(from_list([1]))
print(to_list(e), to_list(o))   # [] [1]

e, o = Solution().even_odd_split(from_list([2]))
print(to_list(e), to_list(o))   # [2] []

e, o = Solution().even_odd_split(from_list([1, 3, 5, 7]))
print(to_list(e), to_list(o))   # [] [1, 3, 5, 7]

e, o = Solution().even_odd_split(from_list([2, 4]))
print(to_list(e), to_list(o))   # [2, 4] []
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
        public List<ListNode> evenOddSplit(ListNode head) {

            // Initialize head and tail references for the two split lists
            ListNode evenDummy = new ListNode(0);
            ListNode evenTail = evenDummy;

            ListNode oddDummy = new ListNode(0);
            ListNode oddTail = oddDummy;

            // Create current reference to iterate through the list
            ListNode current = head;

            // Iterate through the list and split nodes into two lists
            while (current != null) {

                // If the current node's value is even then the node goes to
                // the even list
                if (current.val % 2 == 0) {

                    // `current` node goes to the even split list
                    evenTail.next = current;

                    // Move evenTail forward
                    evenTail = evenTail.next;
                }

                // Otherwise, the node goes to the odd list
                else {

                    // `current` node goes to the odd split list
                    oddTail.next = current;

                    // Move oddTail forward
                    oddTail = oddTail.next;
                }

                // Move to the next node in the original list
                current = current.next;
            }

            // Terminate the even list
            evenTail.next = null;

            // Terminate the odd list
            oddTail.next = null;

            return Arrays.asList(evenDummy.next, oddDummy.next);
        }
    }

    public static void main(String[] args) {
        List<ListNode> r1 = new Solution().evenOddSplit(fromList(5, 2, 3, 10, 6, 8));
        System.out.println(toList(r1.get(0)) + " " + toList(r1.get(1)));  // [2, 10, 6, 8] [5, 3]

        List<ListNode> r2 = new Solution().evenOddSplit(fromList(4, 2, 6, 10));
        System.out.println(toList(r2.get(0)) + " " + toList(r2.get(1)));  // [4, 2, 6, 10] []

        // Edge cases
        List<ListNode> r3 = new Solution().evenOddSplit(null);
        System.out.println(toList(r3.get(0)) + " " + toList(r3.get(1)));  // [] []

        List<ListNode> r4 = new Solution().evenOddSplit(fromList(1));
        System.out.println(toList(r4.get(0)) + " " + toList(r4.get(1)));  // [] [1]

        List<ListNode> r5 = new Solution().evenOddSplit(fromList(2));
        System.out.println(toList(r5.get(0)) + " " + toList(r5.get(1)));  // [2] []

        List<ListNode> r6 = new Solution().evenOddSplit(fromList(1, 3, 5, 7));
        System.out.println(toList(r6.get(0)) + " " + toList(r6.get(1)));  // [] [1, 3, 5, 7]

        List<ListNode> r7 = new Solution().evenOddSplit(fromList(2, 4));
        System.out.println(toList(r7.get(0)) + " " + toList(r7.get(1)));  // [2, 4] []
    }
}
```

### Dry Run

```
head = 5 → 2 → 3 → 10 → 6 → 8 → null   (Example 1)

Init: even_dummy → null, even_tail = even_dummy
      odd_dummy  → null, odd_tail  = odd_dummy
      current = node 5

Tick 1: current = node 5. 5 % 2 = 1 → odd.
        odd_tail.next = node 5; odd_tail = node 5.
        current = node 2.
        Odd chain so far: dummy → 5 → ?

Tick 2: current = node 2. 2 % 2 = 0 → even.
        even_tail.next = node 2; even_tail = node 2.
        current = node 3.
        Even chain so far: dummy → 2 → ?

Tick 3: current = node 3. 3 % 2 = 1 → odd.
        odd_tail.next = node 3; odd_tail = node 3.
        current = node 10.
        Odd chain so far: dummy → 5 → 3 → ?

Tick 4: current = node 10. 10 % 2 = 0 → even.
        even_tail.next = node 10; even_tail = node 10.
        current = node 6.
        Even chain so far: dummy → 2 → 10 → ?

Tick 5: current = node 6. 6 % 2 = 0 → even.
        even_tail.next = node 6; even_tail = node 6.
        current = node 8.
        Even chain so far: dummy → 2 → 10 → 6 → ?

Tick 6: current = node 8. 8 % 2 = 0 → even.
        even_tail.next = node 8; even_tail = node 8.
        current = null. Loop exits.

Seal:   even_tail.next = null → even chain = 2 → 10 → 6 → 8 → null
        odd_tail.next  = null → odd chain  = 5 → 3 → null

Return [even_dummy.next, odd_dummy.next] = [node 2, node 5]. ✓
```

### Result Size

Two output lists, sizes `e` and `o` with `e + o = n`. Either size may be `0` if all values share a single parity. The original `n` nodes are partitioned (not duplicated) across the two outputs.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | One pass over the list; each node performs one parity test, one tail-append, one walker advance — all `O(1)`. |
| **Space** | `O(1)` | Two dummy nodes and four pointers (`even_dummy`, `even_tail`, `odd_dummy`, `odd_tail`) regardless of `n`. No copying. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | The walker loop never enters; both tails stay at their dummies; both seals run on the dummies; return `[null, null]`. |
| Single odd node (`[1]`) | One tick routes node `1` to the odd bucket; even bucket stays empty. Return `[null, node 1]`. |
| Single even node (`[2]`) | One tick routes node `2` to the even bucket; odd bucket stays empty. Return `[node 2, null]`. |
| All odd values (`[1, 3, 5, 7]`) | Every tick routes to the odd bucket; even bucket stays empty. Return `[null, 1 → 3 → 5 → 7]`. |
| All even values (`[2, 4]`) | Every tick routes to the even bucket; odd bucket stays empty. Return `[2 → 4, null]`. |
| Zero values (`[0, 1, 0]`) | `0 % 2 == 0` so zeros land in the even bucket. Return `[0 → 0, 1]`. |
| Negative values (`[-3, -2, -1]`) | `-2 % 2 == 0` (Python and Java agree on the even classification); odd parity routes negatives normally. Return `[-2, -3 → -1]`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The smallest instance of the split template — `k = 2`, classifier is one modulo, no auxiliary state. Every other problem in this section is the same skeleton with a different `classify(node)` line.

</details>