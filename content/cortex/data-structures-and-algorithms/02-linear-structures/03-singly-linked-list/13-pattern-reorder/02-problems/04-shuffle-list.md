---
title: "Shuffle List"
summary: "Reorder a singly linked list as L0, Ln, L1, Ln-1, ... in place by composing fast-and-slow, reversal, and merge into one reorder pipeline."
prereqs:
  - 13-pattern-reorder/01-pattern
difficulty: medium
---

# Shuffle list

## Problem Statement

Given the **head** of a singly linked list that can be represented as **L0 -> L1 -> ... -> Ln - 1 -> Ln**, reorder the list **in place** to match the following format:

**L0 -> Ln -> L1 -> Ln - 1 -> L2 -> Ln - 2 -> ...**

## Examples

**Example 1:**
```
Input:  head = [1, 2, 3, 4]
Output: [1, 4, 2, 3]
Explanation: First half = [1, 2]; reversed second half = [4, 3]. Alternate-fuse A, B, A, B → [1, 4, 2, 3].
```

**Example 2:**
```
Input:  head = [1, 2, 3, 4, 5]
Output: [1, 5, 2, 4, 3]
Explanation: First half = [1, 2, 3]; reversed second half = [5, 4]. Alternate-fuse A, B, A, B → [1, 5, 2, 4, 3]. The odd-length split keeps the middle node 3 in the first half.
```

**Example 3:**
```
Input:  head = [1]
Output: [1]
Explanation: A single-node list is already in the target shape; the reorder is a no-op.
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reorder problem is that the output is a deterministic permutation of the input nodes — same nodes, new `.next` wiring. The target pattern `L0, Ln, L1, Ln-1, ...` is exactly what you get if you split the list at the middle, reverse the second half, and alternate-fuse the two halves. That decomposition uses three patterns you've already built: the **fast-and-slow** pattern to find the middle, the **reversal** pattern to flip the second half, and the **merge** pattern (alternate-fuse selector) to weave them back together. The reorder pipeline is the wrapper that names which `f1` and `f2` apply.

The **pointer placement** follows directly. `f1` is now itself a small pipeline: a slow / fast pair walks the list until `fast` reaches the end, leaving `slow` at the boundary between the two halves; a `prev_to_slow` cursor lags one node behind so the cut point can be severed cleanly. Then a fresh three-pointer walk (`current`, `previous`, `next_node`) reverses the second half. `f2` is the merge pattern's boolean-flip selector — a `mergeFirst` toggle flipping each tick, with the same dummy-head splice loop and drain step you saw in alternate-node-fusion.

What **breaks if you reach for a naive approach**? Trying to materialise the index permutation `[L0, Ln, L1, Ln-1, ...]` directly requires random access — you'd walk to `Ln`, then to `L1`, then to `Ln-1`, and so on. Each lookup is `O(n)`, giving total cost `O(n^2)`. Copying every value into an array and rebuilding works in `O(n)` time but spends `O(n)` extra memory and allocates `n` new nodes. The split-reverse-merge pipeline does the job in `O(n)` time with `O(1)` extra space — three single-pass walks over disjoint halves, no allocations beyond a few dummy heads.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Shuffle List |
|---|---|
| **Q1.** Does the problem rearrange the nodes of one input list in place? | **Yes** — every input node appears in the output exactly once; only `.next` fields change. |
| **Q2.** Can the target be expressed as classifier + selector? | **Yes** — `f1` is "split at the middle (fast-and-slow) and reverse the second half (reversal pattern)"; `f2` is the boolean-flip alternate-fuse selector from the merge pattern. |
| **Q3.** Are the sub-lists bounded in count and walkable in one pass? | **Yes** — exactly two halves; the merge pass alternates between them in one walk. |
| **Q4.** Is `O(1)` extra space sufficient? | **Yes** — a constant number of pointers (slow, fast, prev_to_slow, current, previous, next_node, dummy, tail, mergeFirst) regardless of input size. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the reorder pipeline with a composite `f1` and a boolean-flip `f2`.

1. **Short-circuit trivial inputs.** If `head` is `null` or `head.next` is `null`, return without modification. A list with zero or one node already matches the target shape.
2. **Find the middle with fast-and-slow.** Initialise `slow = head`, `fast = head`, `prev_to_slow = null`. Loop while `fast` and `fast.next` are non-`null`: set `prev_to_slow = slow`, advance `slow = slow.next`, advance `fast = fast.next.next`. When the loop exits, `slow` is at the boundary node of the two halves, and `prev_to_slow` is the last node of the first half (this matters for the clean cut).
3. **Split into two halves.** If `fast` is `null` (even length), the second half starts at `prev_to_slow.next`; sever via `prev_to_slow.next = null`. Otherwise (odd length), the second half starts at `slow.next`; sever via `slow.next = null`. The odd-length cut keeps the middle node in the first half, matching the target pattern.
4. **Reverse the second half.** Run the standard three-pointer reversal: `current = second_half`, `previous = null`; while `current` is non-`null`, capture `next_node = current.next`, set `current.next = previous`, advance `previous = current`, `current = next_node`. When done, `previous` is the new head of the reversed second half.
5. **Initialise the merge skeleton.** Create `dummy = ListNode(0)`, set `tail = dummy`, and initialise `mergeFirst = true` so the first node taken is from the first half (the `L0` node).
6. **Loop while both halves are non-`null`.** Each iteration: if `mergeFirst`, splice `first_half` (`tail.next = first_half`, advance `first_half`); otherwise splice `reversed_second_half`. Then advance `tail = tail.next` and flip `mergeFirst = !mergeFirst`.
7. **Drain the non-empty half.** When the loop exits, at most one half still has nodes. Splice it onto `tail.next` in a single splice — the suffix is already correctly chained.
8. **Return implicit via head mutation.** Because the algorithm mutates `head`'s `.next` chain in place, the caller's `head` reference still points at the original `L0`, which is now `dummy.next`. The function returns `void` / `None`.

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
    def reverse_list(
        self, head: Optional[ListNode]
    ) -> Optional[ListNode]:
        current: Optional[ListNode] = head
        previous: Optional[ListNode] = None

        while current:
            next_node: Optional[ListNode] = current.next
            current.next = previous
            previous = current
            current = next_node

        return previous

    def split_list_in_half(
        self, head: Optional[ListNode]
    ) -> [Optional[ListNode], Optional[ListNode]]:

        # Initialize slow and fast pointers to find the middle of the
        # list
        slow: Optional[ListNode] = head
        fast: Optional[ListNode] = head
        prev_to_slow: Optional[ListNode] = None

        # Move slow by one and fast by two nodes until fast reaches the
        # end
        while fast and fast.next:
            prev_to_slow = slow
            slow = slow.next
            fast = fast.next.next

        # Split for even length list
        if fast is None:
            second_half: Optional[ListNode] = prev_to_slow.next
            prev_to_slow.next = None

        # Split for odd length list
        else:
            second_half: Optional[ListNode] = slow.next
            slow.next = None

        return [head, second_half]

    def merge_alternate_nodes(
        self,
        first_half: Optional[ListNode],
        second_half: Optional[ListNode],
    ) -> Optional[ListNode]:

        # Create a dummy node to form the merged list
        dummy: ListNode = ListNode(0)
        tail: ListNode = dummy

        # Boolean to switch between nodes from each list
        merge_first: bool = True

        # Alternate between the nodes of each list
        while first_half and second_half:
            if merge_first:
                tail.next = first_half
                first_half = first_half.next
            else:
                tail.next = second_half
                second_half = second_half.next
            tail = tail.next
            merge_first = not merge_first

        # Append any remaining nodes from first_half or second_half
        if first_half:
            tail.next = first_half
        elif second_half:
            tail.next = second_half

        return dummy.next

    def shuffle_list(self, head: Optional[ListNode]) -> None:

        # No need to reorder if the list is empty or has only one element
        if not head or not head.next:
            return

        # Split the list into two halves
        first_half: Optional[ListNode]
        second_half: Optional[ListNode]
        first_half, second_half = self.split_list_in_half(head)

        # Reverse the second half of the list
        reversed_second_half: Optional[ListNode] = self.reverse_list(
            second_half
        )

        # Alternatively merge the first list and the reversed second list
        self.merge_alternate_nodes(first_half, reversed_second_half)


# Examples from the problem statement
h1 = from_list([1, 2, 3, 4])
Solution().shuffle_list(h1); print(to_list(h1))       # [1, 4, 2, 3]

h2 = from_list([1, 2, 3, 4, 5])
Solution().shuffle_list(h2); print(to_list(h2))       # [1, 5, 2, 4, 3]

# Edge cases
h3 = None
Solution().shuffle_list(h3); print(to_list(h3))       # []

h4 = from_list([1])
Solution().shuffle_list(h4); print(to_list(h4))       # [1]

h5 = from_list([1, 2])
Solution().shuffle_list(h5); print(to_list(h5))       # [1, 2]

h6 = from_list([1, 2, 3])
Solution().shuffle_list(h6); print(to_list(h6))       # [1, 3, 2]

h7 = from_list([1, 2, 3, 4, 5, 6])
Solution().shuffle_list(h7); print(to_list(h7))       # [1, 6, 2, 5, 3, 4]
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
        private ListNode reverse(ListNode head) {
            ListNode current = head;
            ListNode previous = null;

            while (current != null) {
                ListNode next = current.next;
                current.next = previous;
                previous = current;
                current = next;
            }

            return previous;
        }

        private List<ListNode> splitListInHalf(ListNode head) {

            // Initialize slow and fast pointers to find the middle of the
            // list
            ListNode slow = head, fast = head;
            ListNode prevToSlow = null;

            // Move slow by one and fast by two nodes until fast reaches the
            // end
            while (fast != null && fast.next != null) {
                prevToSlow = slow;
                slow = slow.next;
                fast = fast.next.next;
            }

            ListNode secondHalf;

            // Split for even length list
            if (fast == null) {
                secondHalf = prevToSlow.next;
                prevToSlow.next = null;
            }

            // Split for odd length list
            else {
                secondHalf = slow.next;
                slow.next = null;
            }

            return Arrays.asList(head, secondHalf);
        }

        private ListNode mergeAlternateNodes(
            ListNode firstHalf,
            ListNode secondHalf
        ) {

            // Create a dummy node to form the merged list
            ListNode dummy = new ListNode(0);
            ListNode tail = dummy;

            // Boolean to switch between nodes from each list
            boolean mergeFirst = true;

            // Alternate between the nodes of each list
            while (firstHalf != null && secondHalf != null) {
                if (mergeFirst) {
                    tail.next = firstHalf;
                    firstHalf = firstHalf.next;
                } else {
                    tail.next = secondHalf;
                    secondHalf = secondHalf.next;
                }
                tail = tail.next;
                mergeFirst = !mergeFirst;
            }

            // Append any remaining nodes from firstHalf or secondHalf
            if (firstHalf != null) {
                tail.next = firstHalf;
            } else if (secondHalf != null) {
                tail.next = secondHalf;
            }

            return dummy.next;
        }

        public void shuffleList(ListNode head) {

            // No need to reorder if the list is empty or has only one
            // element
            if (head == null || head.next == null) {
                return;
            }

            // Split the list into two halves
            List<ListNode> heads = splitListInHalf(head);
            ListNode firstHalf = heads.get(0);
            ListNode secondHalf = heads.get(1);

            // Reverse the second half of the list
            ListNode reversedSecondHalf = reverse(secondHalf);

            // Alternatively merge the first list and the reversed second
            // list
            mergeAlternateNodes(firstHalf, reversedSecondHalf);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        ListNode h1 = fromList(1, 2, 3, 4);
        new Solution().shuffleList(h1);
        System.out.println(toList(h1));       // [1, 4, 2, 3]

        ListNode h2 = fromList(1, 2, 3, 4, 5);
        new Solution().shuffleList(h2);
        System.out.println(toList(h2));       // [1, 5, 2, 4, 3]

        // Edge cases
        ListNode h3 = null;
        new Solution().shuffleList(h3);
        System.out.println(toList(h3));       // []

        ListNode h4 = fromList(1);
        new Solution().shuffleList(h4);
        System.out.println(toList(h4));       // [1]

        ListNode h5 = fromList(1, 2);
        new Solution().shuffleList(h5);
        System.out.println(toList(h5));       // [1, 2]

        ListNode h6 = fromList(1, 2, 3);
        new Solution().shuffleList(h6);
        System.out.println(toList(h6));       // [1, 3, 2]

        ListNode h7 = fromList(1, 2, 3, 4, 5, 6);
        new Solution().shuffleList(h7);
        System.out.println(toList(h7));       // [1, 6, 2, 5, 3, 4]
    }
}
```


### Dry Run

```
head = 1 → 2 → 3 → 4 → 5 → null   (Example 2, odd length 5)

Step 1 — find middle (fast-and-slow):
  Init: slow = 1, fast = 1, prev_to_slow = null.
  Iter 1: fast=1, fast.next=2 (both non-null) → prev_to_slow = 1, slow = 2, fast = 3.
  Iter 2: fast=3, fast.next=4 → prev_to_slow = 2, slow = 3, fast = 5.
  Iter 3: fast=5, fast.next=null → exit loop.
  Result: slow = 3 (the middle), prev_to_slow = 2, fast = 5.

Step 2 — split (odd length, fast != null):
  second_half = slow.next = 4. slow.next = null.
  first_half  = 1 → 2 → 3 → null.
  second_half = 4 → 5 → null.

Step 3 — reverse second half:
  current = 4, previous = null.
  Iter 1: next_node = 5; 4.next = null; previous = 4; current = 5.
  Iter 2: next_node = null; 5.next = 4; previous = 5; current = null.
  Result: reversed_second_half = 5 → 4 → null.

Step 4 — alternate-merge first_half and reversed_second_half:
  Init: dummy = ⊙, tail = dummy, mergeFirst = true.
  Iter 1: mergeFirst=true → splice 1 from first_half. tail = 1. first_half = 2. flip → false.
  Iter 2: mergeFirst=false → splice 5 from reversed. tail = 5. reversed = 4. flip → true.
  Iter 3: mergeFirst=true → splice 2 from first_half. tail = 2. first_half = 3. flip → false.
  Iter 4: mergeFirst=false → splice 4 from reversed. tail = 4. reversed = null. flip → true.
  Iter 5: reversed is null → exit loop.
  Drain: first_half = 3 non-null → tail.next = 3. Output suffix already chained.
  Output: 1 → 5 → 2 → 4 → 3 → null. ✓
```

### Result Size

The output contains exactly `n` nodes — every input node appears in the output once. The first half holds `ceil(n/2)` nodes (including the middle when `n` is odd), the reversed second half holds `floor(n/2)`. Only `.next` fields are rewired; no values change.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | Three sequential single-pass walks: fast-and-slow (`n/2` iterations of the slow pointer), reversal of the second half (`n/2` iterations), and alternate-fuse merge (`n` iterations). Sum is `2n` steps, dominated by `O(n)`. |
| **Space** | `O(1)` | A constant number of pointers (`slow`, `fast`, `prev_to_slow`, `current`, `previous`, `next_node`, `dummy`, `tail`, `mergeFirst`) regardless of input size. No allocations beyond the throwaway merge dummy. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head = null`) | Guard `if not head` returns immediately. No modification. |
| Single node (`head = [1]`) | Guard `if not head.next` returns immediately. Already in target shape. |
| Two nodes (`head = [1, 2]`) | Even-length split: `first_half = [1]`, `second_half = [2]`. Reversal of singleton is identity. Merge → `[1, 2]`. Output equals input — the target shape `L0, Ln` is trivially the same as `L0, L1` for `n = 2`. |
| Three nodes (`head = [1, 2, 3]`) | Odd-length split: `first_half = [1, 2]`, `second_half = [3]`. Reversed = `[3]`. Merge alternates `1, 3, 2` → `[1, 3, 2]`. |
| Even length (`head = [1, 2, 3, 4]`) | Even-length split: `first_half = [1, 2]`, `second_half = [3, 4]`. Reversed = `[4, 3]`. Merge alternates `1, 4, 2, 3` → `[1, 4, 2, 3]`. |
| Odd length (`head = [1, 2, 3, 4, 5]`) | Already covered in the Dry Run. Output `[1, 5, 2, 4, 3]`. |
| Long even length (`head = [1, 2, 3, 4, 5, 6]`) | `first_half = [1, 2, 3]`, `second_half = [4, 5, 6]`. Reversed = `[6, 5, 4]`. Merge alternates → `[1, 6, 2, 5, 3, 4]`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

Reorder is the composition of two patterns you already know:

```
def reorder(head):
    sub_lists = split(head, classifier = f1)   # lesson 10
    return merge(*sub_lists, selector = f2)    # lesson 11
```

Picking `f1` and `f2` specialises the template to any reorder problem:

| Problem | `f1` (split classifier) | `f2` (merge selector) |
|---|---|---|
| Odd/even index split | `i % 2` | concatenate |
| Zig-zag reorder (1st, last, 2nd, 2nd-last, ...) | first half / reversed second half | alternate A, B, A, B |
| Parity partition (odd values first) | `val % 2` | concatenate |
| Value partition (<, ≥ pivot) | `val < pivot` | concatenate |
| Pure shuffle into alternating even-odd | index parity | alternate A, B, A, B |

Four insights worth burning in:

| Insight | Why it matters |
|---|---|
| Reorder = split + merge | Stop inventing bespoke algorithms for each reorder variant. Reuse the two primitives. |
| Classifier + selector are the whole problem | The template is 5 lines. Every variant customises exactly two functions. |
| Reversing a sub-list is a valid `f1` extension | For zig-zag reorder, split the list at the middle and reverse the second half — then the merge selector is plain alternation. |
| O(n) time, O(1) extra space | Every node is visited once in split, once in merge. No allocations beyond a few dummy heads. |

When you next see "rearrange in place", "reorder by pattern", "zig-zag", "partition", "shuffle by index" — reach for `split → merge` first. Pick your `f1` and `f2` and ship it.

> **Transfer Challenge:** Given a linked list `1 → 2 → 3 → 4 → 5 → 6`, produce the zig-zag reorder `1 → 6 → 2 → 5 → 3 → 4`. What are your `f1` and `f2`?
>
> <details><summary><strong>Solution hint</strong></summary>
>
> <strong>f1</strong> — split at the middle (using the fast-and-slow pattern from lesson 9), then <strong>reverse the second half</strong> (using the reversal pattern from lesson 6). You end with two lists: <code>1 → 2 → 3</code> and <code>6 → 5 → 4</code>.<br>
> <strong>f2</strong> — alternate-fuse A, B, A, B (the selector from the merge lesson).<br>
> Result: <code>1 → 6 → 2 → 5 → 3 → 4</code>. This problem alone touches <strong>four</strong> patterns you've learned — reversal, fast-and-slow, split, and merge. That's the power of composing primitives.
>
> </details>

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Shuffle-list is the composite reorder — `f1` itself uses two earlier patterns (fast-and-slow to find the middle, reversal to flip the second half), then `f2` is the alternate-fuse selector from the merge pattern. One problem, four patterns chained: the payoff for learning the primitives in isolation.

</details>