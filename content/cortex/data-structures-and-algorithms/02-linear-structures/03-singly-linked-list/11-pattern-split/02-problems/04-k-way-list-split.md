---
title: "K-Way List Split"
summary: "Given the head of a singly linked list and an integer k, write a function to split the linked list into k consecutive linked list parts. Your function should return the heads of all the split parts."
prereqs:
  - 11-pattern-split/01-pattern
difficulty: medium
---

# K-way list split

## Problem Statement

Given the **head** of a singly linked list and an integer **k**, write a function to split the linked list into `k` consecutive linked list parts. Your function should return the heads of all the split parts.

The length of each part should be as equal as possible. No two parts should have a size differing by more than one. This may lead to some parts being `null`. The parts should be in the order of occurrence in the input list, and parts occurring earlier should always have a size greater than or equal to parts occurring later.

## Examples

**Example 1:**
```
Input:  head = [1, 2, 3], k = 5
Output: [[1], [2], [3], [], []]
Explanation: n = 3, k = 5 → partSize = 0, bigLists = 3.
             First three buckets each get one node (size 0 + 1 = 1);
             the remaining two buckets get zero nodes (null heads).
```

**Example 2:**
```
Input:  head = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10], k = 3
Output: [[1, 2, 3, 4], [5, 6, 7], [8, 9, 10]]
Explanation: n = 10, k = 3 → partSize = 3, bigLists = 1.
             Bucket 0 gets 4 nodes; buckets 1 and 2 each get 3.
```

**Example 3:**
```
Input:  head = [1, 2, 3, 4, 5, 6], k = 2
Output: [[1, 2, 3], [4, 5, 6]]
Explanation: n = 6, k = 2 → partSize = 3, bigLists = 0.
             Each bucket gets exactly partSize = 3 nodes.
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a split problem is that the parts are *consecutive segments* of the original list — every cut sits between two adjacent nodes. Unlike the value- or modulo-based splits, the classifier here is position-driven: bucket `i` collects the `i`-th block of nodes starting from the head. The only twist is sizing — when `n` isn't divisible by `k`, the first `n % k` parts get one extra node each so that no two parts differ by more than one and earlier parts are at least as large as later parts.

The **bucket placement** uses a precomputed sizing pair: `partSize = n // k` and `bigLists = n % k`. Each part's exact size is `partSize + 1` for the first `bigLists` buckets and `partSize` for the rest. Because the parts are consecutive, no dummy-and-tails dance is needed — capture each part's head, walk forward by the part's size, sever, and move on. The walker holds the current node; severing means setting `current.next = null` at the boundary, after stashing `current.next` so the walker can step into the next part.

What **breaks if you reach for a naive approach**? The brute force walks the original list once per output bucket to copy out its nodes — `O(n * k)` time and `O(n)` extra space for the copies. The same approach without copying (re-walk the list per bucket, re-thread the segment) doesn't even terminate cleanly: once the first bucket's segment is severed, the second walk can no longer reach the second segment because the chain is broken. The split-with-precomputed-sizes solution is one length pass plus one routing pass — `O(n)` total — and never copies a node.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for K-Way List Split |
|---|---|
| **Q1.** Does the problem ask to partition the input into multiple output lists? | **Yes** — `k` consecutive output segments, one head per segment. |
| **Q2.** Can each node's bucket be computed locally? | **Yes** — once `partSize` and `bigLists` are known, the bucket boundary advances by counter alone; no value lookup, no look-ahead. |
| **Q3.** Is the work at each step `O(1)`? | **Yes** — each node visit performs at most one pointer hop and at most one severing assignment. The outer length pass is the only non-`O(1)` step, and it's a one-time `O(n)` cost. |
| **Q4.** Can output lists share original nodes (re-linked, not copied)? | **Yes** — the problem returns the original nodes re-threaded; severing `.next` at each boundary is what turns one chain into `k`. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run a length pass, then a single routing pass that captures one segment per output bucket.

1. **Measure the list length.** Walk the list once and count nodes; store `length = n`. This is the only pass that needs the entire chain unbroken.
2. **Precompute the sizing pair.** Set `partSize = length // k` and `extraNodes = length % k`. The first `extraNodes` buckets get `partSize + 1` nodes; the rest get `partSize`. (`extraNodes` is the source-code name for what the pattern doc called `bigLists`.)
3. **Allocate the result array.** Create `parts` of size `k`, initialised to `null`. Bucket `i` is empty when `partSize + (1 if i < extraNodes else 0) == 0` — which only happens when `partSize == 0` and `i >= extraNodes`.
4. **For each bucket `i` in `0..k-1`:**
   - **Set the bucket's head.** `parts[i] = current`. If `current` is already `null`, the bucket is empty; the inner walk below is a no-op.
   - **Compute this bucket's size.** `current_part_size = partSize + (1 if extraNodes > 0 else 0)`.
   - **Walk to the last node of this bucket.** Step `current_part_size - 1` hops along `current = current.next`, guarding against `null`. After the inner loop, `current` is the last node of bucket `i` (or `null` if the bucket was empty).
   - **Sever and advance.** Stash `next_part_head = current.next`; set `current.next = null` to terminate bucket `i`; advance `current = next_part_head` to the head of the next bucket.
   - **Decrement `extraNodes` if it was used.** Only the first `bigLists` buckets get the bonus node; after each, `extraNodes -= 1`.
5. **Return the result array.** Each non-`null` entry is the head of a sealed sublist; `null` entries are empty buckets.

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
    def find_length(self, head: Optional[ListNode]) -> int:
        length = 0
        while head is not None:
            length += 1
            head = head.next
        return length

    def k_way_list_split(
        self, head: Optional[ListNode], k: int
    ) -> List[Optional[ListNode]]:

        # Get total number of nodes
        length = self.find_length(head)

        # Base size of each part
        part_size = length // k

        # Remainder to distribute among parts
        extra_nodes = length % k

        # Result list to store part heads
        parts: List[Optional[ListNode]] = [None] * k

        # Pointer to traverse the list
        current = head

        for i in range(k):

            # Set the start of the current part
            parts[i] = current

            # Calculate the size for the current part
            current_part_size = part_size + (1 if extra_nodes > 0 else 0)

            # Traverse `current_part_size - 1` nodes ahead in the list
            for j in range(1, current_part_size):
                if current:
                    current = current.next

            # Move to the start of the next part, breaking the link
            if current:
                next_part_head = current.next
                current.next = None
                current = next_part_head

            # Decrease `extra_nodes` only if it was used for this part
            if extra_nodes > 0:
                extra_nodes -= 1

        return parts


r = Solution().k_way_list_split(from_list([1, 2, 3]), 5)
print([to_list(x) for x in r])   # [[1], [2], [3], [], []]

r = Solution().k_way_list_split(from_list([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]), 3)
print([to_list(x) for x in r])   # [[1, 2, 3, 4], [5, 6, 7], [8, 9, 10]]

# Edge cases
r = Solution().k_way_list_split(None, 3)
print([to_list(x) for x in r])   # [[], [], []]

r = Solution().k_way_list_split(from_list([1]), 1)
print([to_list(x) for x in r])   # [[1]]

r = Solution().k_way_list_split(from_list([1, 2]), 2)
print([to_list(x) for x in r])   # [[1], [2]]

r = Solution().k_way_list_split(from_list([1, 2, 3, 4, 5, 6]), 2)
print([to_list(x) for x in r])   # [[1, 2, 3], [4, 5, 6]]
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
        private int findLength(ListNode head) {
            int length = 0;
            while (head != null) {
                length++;
                head = head.next;
            }
            return length;
        }

        public List<ListNode> kWayListSplit(ListNode head, int k) {

            // Get total number of nodes
            int length = findLength(head);

            // Base size of each part
            int partSize = length / k;

            // Remainder to distribute among parts
            int extraNodes = length % k;

            // Result list to store part heads
            List<ListNode> parts = new ArrayList<>(k);

            // Pointer to traverse the list
            ListNode current = head;

            for (int i = 0; i < k; ++i) {

                // Set the start of the current part
                parts.add(current);

                // Calculate the size for the current part
                int currentPartSize = partSize + (extraNodes > 0 ? 1 : 0);

                // Traverse `currentPartSize - 1` nodes ahead in the list
                for (
                    int j = 1;
                    j < currentPartSize && current != null;
                    ++j
                ) {
                    current = current.next;
                }

                // Move to the start of the next part, breaking the link
                if (current != null) {
                    ListNode nextPartHead = current.next;
                    current.next = null;
                    current = nextPartHead;
                }

                // Decrease `extraNodes` only if it was used for this part
                if (extraNodes > 0) {
                    extraNodes--;
                }
            }

            return parts;
        }
    }

    public static void main(String[] args) {
        List<ListNode> r1 = new Solution().kWayListSplit(fromList(1, 2, 3), 5);
        System.out.println(r1.stream().map(Main::toList).toList());  // [[1], [2], [3], [], []]

        List<ListNode> r2 = new Solution().kWayListSplit(fromList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 3);
        System.out.println(r2.stream().map(Main::toList).toList());  // [[1, 2, 3, 4], [5, 6, 7], [8, 9, 10]]

        // Edge cases
        List<ListNode> r3 = new Solution().kWayListSplit(null, 3);
        System.out.println(r3.stream().map(Main::toList).toList());  // [[], [], []]

        List<ListNode> r4 = new Solution().kWayListSplit(fromList(1), 1);
        System.out.println(r4.stream().map(Main::toList).toList());  // [[1]]

        List<ListNode> r5 = new Solution().kWayListSplit(fromList(1, 2), 2);
        System.out.println(r5.stream().map(Main::toList).toList());  // [[1], [2]]

        List<ListNode> r6 = new Solution().kWayListSplit(fromList(1, 2, 3, 4, 5, 6), 2);
        System.out.println(r6.stream().map(Main::toList).toList());  // [[1, 2, 3], [4, 5, 6]]
    }
}
```

### Dry Run

```
head = 1 → 2 → 3 → null   (Example 1, k = 5)

Length pass: length = 3.
Sizing:      partSize = 3 / 5 = 0; extraNodes = 3 % 5 = 3.

parts = [null, null, null, null, null]
current = node 1

Bucket 0: parts[0] = node 1
          current_part_size = 0 + 1 = 1 (extraNodes > 0)
          inner walk: 0 hops (range(1, 1) is empty)
          sever — next_part_head = node 2; node 1.next = null; current = node 2
          extraNodes-- → 2

Bucket 1: parts[1] = node 2
          current_part_size = 0 + 1 = 1
          inner walk: 0 hops
          sever — next_part_head = node 3; node 2.next = null; current = node 3
          extraNodes-- → 1

Bucket 2: parts[2] = node 3
          current_part_size = 0 + 1 = 1
          inner walk: 0 hops
          sever — next_part_head = null; node 3.next = null; current = null
          extraNodes-- → 0

Bucket 3: parts[3] = null
          current_part_size = 0 + 0 = 0
          inner walk: range(1, 0) is empty — 0 hops
          current is null — sever step is skipped (guard `if current`)
          extraNodes already 0 — no decrement

Bucket 4: parts[4] = null
          (same as bucket 3)

Return [node 1, node 2, node 3, null, null] → [[1], [2], [3], [], []]. ✓
```

### Result Size

Exactly `k` output entries. The first `extraNodes = n % k` parts have `partSize + 1` nodes; the rest have `partSize`. Sizes sum to `n`. When `n < k`, the trailing `k - n` parts are empty (`null` heads).

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n + k)` | One length pass over the list (`O(n)`) and one routing pass that walks each node exactly once across all buckets, plus `O(k)` outer-loop overhead for empty trailing buckets. |
| **Space** | `O(k)` | The result array holds `k` heads. No dummies are needed because each part's head is the natural first node of its segment. Original nodes are re-linked, not copied. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`, `k = 3`) | `length = 0`; `partSize = 0`; `extraNodes = 0`. Every bucket head is `null`. Return `[null, null, null]`. |
| Single node (`[1], k = 1`) | `length = 1`; `partSize = 1`; `extraNodes = 0`. Bucket 0 gets the one node; no other buckets exist. Return `[[1]]`. |
| `k = n` (`[1, 2], k = 2`) | `partSize = 1`; `extraNodes = 0`. Each bucket gets exactly one node. Return `[[1], [2]]`. |
| `k > n` (`[1, 2, 3], k = 5`) | `partSize = 0`; `extraNodes = 3`. First three buckets get one node each; remaining two are empty. Return `[[1], [2], [3], [], []]`. |
| Exact multiple (`[1..6], k = 2`) | `partSize = 3`; `extraNodes = 0`. Two equal parts of size 3. Return `[[1, 2, 3], [4, 5, 6]]`. |
| Non-multiple (`[1..10], k = 3`) | `partSize = 3`; `extraNodes = 1`. First bucket gets 4; the other two get 3. Return `[[1, 2, 3, 4], [5, 6, 7], [8, 9, 10]]`. |
| `k = 1` (`[1, 2, 3, 4], k = 1`) | `partSize = 4`; `extraNodes = 0`. Bucket 0 gets the entire list, identical to the input chain. Return `[[1, 2, 3, 4]]`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

The split pattern is a single template with a swappable classifier:

```
dummies = [ListNode() for _ in range(k)]
tails   = dummies[:]                  # each tail starts at its dummy
for node in original_list:
    b = classify(node)                # <-- the only problem-specific line
    tails[b].next = node
    tails[b] = node
for t in tails:
    t.next = None                     # seal every output list
heads = [d.next for d in dummies]     # real heads live one hop past the dummies
```

Four insights worth burning in:

| Insight | Why it matters |
|---|---|
| Dummy heads eliminate the "first node" special case | Without them, every append needs `if tail[b] is None: head[b] = node else: tail[b].next = node`. With them: always `tail[b].next = node`. |
| Re-link, don't copy | Nodes never move in memory; only `.next` pointers change. Zero allocations beyond the `k` dummies. One pass. |
| The classifier is the whole problem | Every variant (even/odd, alternate groups, round robin, modulo, unequal sizes) differs only in the `classify(node)` function. The skeleton is identical. |
| Sealing the tail is non-negotiable | The last node tacked onto each bucket still points into the middle of some other output list. Setting `tail[b].next = null` at the end is what turns a tangle of shared pointers into `k` independent lists. |

When you next see "split by rule", "bucket by hash", "round-robin distribute", "even/odd split", "partition by predicate" — reach for the dummy-and-tails template first. Then just write the one-line classifier.

> **Transfer Challenge:** Split a linked list into two lists where **list A contains all nodes whose value is less than the first node's value**, and **list B contains the rest** (preserving original order within each). What's your `classify(node)` function — and what extra state do you need to track?
>
> <details><summary><strong>Solution hint</strong></summary>
>
> Save <code>pivot = head.val</code> <em>before</em> the loop starts (you need it after the head itself gets routed). Then <code>classify(node) = 0 if node.val < pivot else 1</code>. Everything else is the standard 2-bucket template. This is also the partition step of quicksort on a linked list — a template with teeth.
>
> </details>

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The position-driven variant of the split template: precompute `partSize` and `bigLists` from one length pass, then sever the original chain at each segment boundary. No dummies are needed because consecutive segments inherit their head naturally — but the severing assignment (`current.next = null`) is still what isolates the buckets.

</details>