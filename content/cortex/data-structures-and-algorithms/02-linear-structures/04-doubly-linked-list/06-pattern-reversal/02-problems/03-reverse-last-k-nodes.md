---
title: "Reverse Last K Nodes"
summary: "Measure the list, walk to the splice point, detach the suffix, run the whole-list reversal helper on it, and re-stitch the cut in both directions."
prereqs:
  - 06-pattern-reversal/01-pattern
difficulty: medium
kind: problem
topics: [reversal, doubly-linked-list]
---

# Reverse last K nodes

## Problem Statement

Given the **head** of a doubly linked list and a non-negative integer **k**, write a function to reverse the last `k` nodes of the list and return the head.

You need to reverse the suffix in place.

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10, 3], k = 2
Output: [5, 7, 3, 3, 10]
```

**Example 2:**
```
Input:  head = [1, 2, 3, 4, 5], k = 3
Output: [1, 2, 5, 4, 3]
```

**Example 3:**
```
Input:  head = [1, 2, 3, 4, 5], k = 5
Output: [5, 4, 3, 2, 1]
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "head = [1, 2, 3, 4, 5], k = 2",
  "options": ["[1, 2, 3, 4, 5]", "[1, 2, 3, 5, 4]", "[5, 4, 3, 2, 1]", "[4, 5, 3, 2, 1]"],
  "answer": "[1, 2, 3, 5, 4]"
}
```

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `0 ≤ k`
- If `k` exceeds the list length, reverse the entire list
- Reverse **in place** — `O(1)` extra space; node values must not be copied or rewritten

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, prev=None, next=None):
        self.val = val
        self.prev = prev
        self.next = next

class Solution:
    def reverse_last_k_nodes(self, head, k):
        # Your code goes here — measure length, walk to splice point (n-k),
        # detach suffix (clear .prev), reverse it, then re-stitch both directions.
        pass

def build_list(values):              # [1, 2, 3] → 1 ⇄ 2 ⇄ 3
    head = tail = None
    for v in values:
        node = ListNode(v, prev=tail)
        if tail is not None:
            tail.next = node
        else:
            head = node
        tail = node
    return head

def print_list(head):                # 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
    out = []
    while head:
        out.append(head.val)
        head = head.next
    print(out)

head = build_list(ast.literal_eval(input()))   # the test case's head
k = int(input())
print_list(Solution().reverse_last_k_nodes(head, k))
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode prev, next;
        ListNode(int val) { this.val = val; }
    }

    static class Solution {
        ListNode reverseLastKNodes(ListNode head, int k) {
            // Your code goes here — measure length, walk to splice point (n-k),
            // detach suffix (clear .prev), reverse it, then re-stitch both directions.
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int k = Integer.parseInt(sc.nextLine().trim());
        printList(new Solution().reverseLastKNodes(head, k));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 ⇄ 2 ⇄ 3
        ListNode head = null, tail = null;
        for (int v : values) {
            ListNode node = new ListNode(v);
            node.prev = tail;
            if (tail != null) tail.next = node;
            else head = node;
            tail = node;
        }
        return head;
    }

    static void printList(ListNode head) {         // 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
        List<Integer> out = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) out.add(n.val);
        System.out.println(out);
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[5, 7, 3, 10, 3]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "head": "[5, 7, 3, 10, 3]", "k": "2" }, "expected": "[5, 7, 3, 3, 10]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "k": "3" }, "expected": "[1, 2, 5, 4, 3]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "k": "5" }, "expected": "[5, 4, 3, 2, 1]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "k": "0" }, "expected": "[1, 2, 3, 4, 5]" },
    { "args": { "head": "[]", "k": "3" }, "expected": "[]" },
    { "args": { "head": "[7]", "k": "1" }, "expected": "[7]" },
    { "args": { "head": "[3, 9]", "k": "1" }, "expected": "[3, 9]" },
    { "args": { "head": "[3, 9]", "k": "2" }, "expected": "[9, 3]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** is that the last `k` nodes form a contiguous suffix segment. The prefix (the first `n − k` nodes) stays in place, and the suffix needs both pointer fields swapped on every node. The reversed suffix's new head is the original tail; its new tail is the original `(n − k + 1)`-th node. The stitch is between the prefix and the reversed suffix: the prefix's last node (the `(n − k)`-th node) must point forward to the reversed suffix's new head, and the new head must point back at the prefix's last node.

The **pointer placement** requires one piece of bookkeeping that the prefix variant did not: we do not know where the suffix begins without first measuring the list. A two-pass walk does the work. The first pass counts the length. The second pass advances `current` to position `n − k` (1-indexed), which is the prefix's last node and the splice point. From there, the algorithm detaches the suffix by clearing `current.next.prev` to `null`, hands the standalone suffix to the full-list reversal helper, and finally re-stitches with `current.next = newHead` and `newHead.prev = current`. Detaching first lets the helper run as a clean whole-list reversal without dealing with the prefix it would otherwise drag behind.

What **breaks if you reach for a one-pass approach without measuring length**? You can't tell which suffix is "the last `k`" without knowing the total length. The fast-and-slow-pointers pattern (used elsewhere) can do this in one pass with two pointers spaced `k` apart, but it solves a different shape of problem; for this section's reversal pattern, the two-pass measure-then-reverse approach is the cleanest fit and stays at `O(n)` time / `O(1)` space. Skipping the suffix detach is the other temptation; if `current.next.prev` is not cleared before the helper runs, the helper's `previous` capture confuses the prefix's last node for an internal segment node and the result corrupts.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse Last K Nodes |
|---|---|
| **Q1.** Does the problem ask for reversed order across a contiguous segment? | **Yes** — the last `k` nodes (positions `n − k + 1` through `n`) form the segment. |
| **Q2.** Are the segment endpoints identifiable? | **Yes** — after measuring the length, the suffix starts at the `(n − k + 1)`-th node and ends at the tail. The splice point is the `(n − k)`-th node, found by a counted walk. |
| **Q3.** Is the work strictly structural (only `prev`/`next` pointers change)? | **Yes** — the length scan reads no values; the reversal helper only swaps pointer fields; the cut and re-stitch are four assignments. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — the length is an integer, the reversal helper uses three references, and the splice point is one more reference. |

</details>
<details>
<summary><h2>Brute Force: Collect into an Array</h2></summary>


Walk the list collecting all values into an array. Reverse the last `k` values of the array. Walk the list a second time writing values back into nodes. This is `O(n)` time but `O(n)` extra space, and again confuses value movement with pointer rewiring — the lesson the pattern exists to teach is precisely the opposite.

</details>
<details>
<summary><h2>Key Insight: Measure, Walk, Detach, Reverse, Stitch</h2></summary>


The pattern reuses the full-list reversal helper, applied to the suffix only. Because the helper expects a clean standalone list, the algorithm walks to position `n − k` (the splice point) and detaches the suffix in two steps: clear the prefix's last node's `next` is left intact for the moment, but `current.next.prev` is cleared to `null` so the suffix's first node looks like a head. The helper then runs end-to-end on the suffix, returns the reversed new head, and the re-stitch writes `current.next = newHead` (forward) and `newHead.prev = current` (backward). The prefix is never re-touched, and the new tail of the reversed suffix (which used to be the suffix's original head) already points to `null` from inside the helper.

</details>
<details>
<summary><h2>Approach</h2></summary>


Measure the length, walk to the splice point, detach, reverse the suffix, stitch.

1. **Handle the no-op guard.** If `k <= 0`, return `head` unchanged.
2. **Measure the list's length.** Walk from `head` once, counting nodes, to compute `length`. This costs `O(n)` time and is the only auxiliary work.
3. **Handle the full-list shortcut.** If `k >= length`, the suffix is the entire list. Delegate to the full-list reversal helper and return its result.
4. **Walk to the splice point.** Set `current = head` and advance `length − k − 1` times (a `for _ in range(1, length - k)` loop), ending with `current` at the `(length − k)`-th node — the prefix's last node.
5. **Detach the suffix.** If `current.next` is not `null`, set `current.next.prev = null`. The suffix's first node now looks like the head of a standalone list — the helper can run on it without entangling the prefix.
6. **Reverse the suffix.** Pass `current.next` (the suffix's original head) to the full-list reversal helper. The helper returns the reversed suffix's new head — the original tail.
7. **Stitch the cut in both directions.** Assign `current.next = newHead`. If `newHead` is not `null`, mirror with `newHead.prev = current`. The prefix is intact; the suffix is reversed; the splice connects them both ways. Return the original `head`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution


```python solution time=O(n) space=O(1)
import ast

class ListNode:
    def __init__(self, val, prev=None, next=None):
        self.val = val
        self.prev = prev
        self.next = next

class Solution:
    def length_of_list(self, head):
        length = 0

        # Traverse the list and increment the length until the end
        while head:
            length += 1
            head = head.next

        # Return the length
        return length

    def reverse_a_list(self, head):

        # If the head is null or if it's the only node in the list,
        # return the head as it is
        if head is None or (head.prev is None and head.next is None):
            return head

        # Pointer to track the current node
        current = head

        # Pointer to track the previous node
        previous = None

        while current is not None:

            # Save the address of next node
            next_node = current.next

            # Swap the previous and next nodes pointers of the current
            # node
            current.prev, current.next = current.next, current.prev

            # Store the previous node in the previous pointer
            previous = current

            # Move the current pointer to the next node
            current = next_node

        # Return the new head, which is stored in the previous pointer
        return previous

    def reverse_last_k_nodes(self, head, k):

        # if K is less than or equal to 0, return the original head
        if k <= 0:
            return head

        # Find the length of the list
        length = self.length_of_list(head)

        # If k is greater than or equal to length, reverse the entire
        # list
        if k >= length:
            return self.reverse_a_list(head)

        # Find the (length - k)th node after which the reversal should
        # occur
        current = head
        for _ in range(1, length - k):
            current = current.next

        # Disconnect the last k nodes from the main list
        if current.next is not None:
            current.next.prev = None

        # Reverse the last k nodes
        last_k_reverse_head = self.reverse_a_list(current.next)

        # Connect the (length - k)th node to the new head
        current.next = last_k_reverse_head

        # Connect the new head of the reversed list to the
        # (length - k)th node
        if last_k_reverse_head is not None:
            last_k_reverse_head.prev = current

        return head

def build_list(values):              # [1, 2, 3] → 1 ⇄ 2 ⇄ 3
    head = tail = None
    for v in values:
        node = ListNode(v, prev=tail)
        if tail is not None:
            tail.next = node
        else:
            head = node
        tail = node
    return head

def print_list(head):                # 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
    out = []
    while head:
        out.append(head.val)
        head = head.next
    print(out)

head = build_list(ast.literal_eval(input()))   # the test case's head
k = int(input())
print_list(Solution().reverse_last_k_nodes(head, k))
```

```java solution
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode prev, next;
        ListNode(int val) { this.val = val; }
    }

    static class Solution {
        private int lengthOfList(ListNode head) {
            int length = 0;

            // Traverse the list and increment the length until the end
            while (head != null) {
                length++;
                head = head.next;
            }

            // Return the length
            return length;
        }

        private ListNode reverseAList(ListNode head) {

            // If the head is null or if it's the only node in the list,
            // return the head as it is
            if (head == null || (head.prev == null && head.next == null)) {
                return head;
            }

            // Pointer to track the current node
            ListNode current = head;

            // Pointer to track the previous node
            ListNode previous = null;

            while (current != null) {

                // Save the address of next node
                ListNode next = current.next;

                // Swap the previous and next nodes pointers of the current
                // node
                ListNode temp = current.prev;
                current.prev = current.next;
                current.next = temp;

                // Store the previous node in the previous pointer
                previous = current;

                // Move the current pointer to the next node
                current = next;
            }

            // Return the new head, which is stored in the previous pointer
            return previous;
        }

        ListNode reverseLastKNodes(ListNode head, int k) {

            // if K is less than or equal to 0, return the original head
            if (k <= 0) {
                return head;
            }

            // Find the length of the list
            int length = lengthOfList(head);

            // If k is greater than or equal to length, reverse the entire
            // list
            if (k >= length) {
                return reverseAList(head);
            }

            // Find the (length - k)th node after which the reversal should
            // occur
            ListNode current = head;
            for (int i = 1; i < length - k; i++) {
                current = current.next;
            }

            // Disconnect the last k nodes from the main list
            if (current.next != null) {
                current.next.prev = null;
            }

            // Reverse the last k nodes
            ListNode lastKReverseHead = reverseAList(current.next);

            // Connect the (length - k)th node to the new head
            current.next = lastKReverseHead;

            // Connect the new head of the reversed list to the
            // (length - k)th node
            if (lastKReverseHead != null) {
                lastKReverseHead.prev = current;
            }

            return head;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int k = Integer.parseInt(sc.nextLine().trim());
        printList(new Solution().reverseLastKNodes(head, k));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 ⇄ 2 ⇄ 3
        ListNode head = null, tail = null;
        for (int v : values) {
            ListNode node = new ListNode(v);
            node.prev = tail;
            if (tail != null) tail.next = node;
            else head = node;
            tail = node;
        }
        return head;
    }

    static void printList(ListNode head) {         // 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
        List<Integer> out = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) out.add(n.val);
        System.out.println(out);
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
length = 5,  k = 2,  k < length  → suffix is positions 4..5 = [10, 3]

Walk: current = head, loop range(1, length - k) = range(1, 3) → 2 steps
  Start  current = 5
  Step 1 current = 5.next = 7
  Step 2 current = 7.next = 3   (this is the (length - k)-th = 3rd node)

Disconnect the suffix:
  current.next is node(10) ≠ null → current.next.prev = null
  (node 10 drops its back-link, so the suffix [10, 3] is a standalone list)

Reverse the suffix:
  reverse_a_list(current.next) = reverse_a_list(10 ⇄ 3) → 3 ⇄ 10
  last_k_reverse_head = 3

Re-stitch:
  current.next = last_k_reverse_head = node(3)         (prefix tail → new suffix head)
  last_k_reverse_head is non-null → last_k_reverse_head.prev = current = node(3)   (mirror)

Result: 5 ⇄ 7 ⇄ 3 ⇄ 3 ⇄ 10  ✓
```

The find-reverse-stitch pattern reuses the whole-list `reverse_a_list` helper verbatim — no edge-case branching for "where exactly does the segment start". Because the suffix is detached as its own list, the helper does the prev/next swaps internally; the re-stitch then restores both directions across the cut (`current.next` forward and `last_k_reverse_head.prev` backward). The price is two extra walks (one to count length, one to find the cut).

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | One pass to measure length, one partial walk of `n − k` steps to the splice point, one full reversal of the `k`-node suffix. Sum: `O(n + (n − k) + k) = O(n)`. |
| **Space** | `O(1)` | An integer for length, three references for the reversal helper, one for the splice point — independent of `n` and `k`. |

### Edge Cases

| Case | What happens |
|---|---|
| `k <= 0` | Early return; original `head` returned unchanged. |
| `head is null` | `length_of_list` returns `0`; the `k >= length` branch fires; the helper handles `null` by returning `null`. |
| `k == length` | Full-reversal shortcut fires; entire list is reversed. |
| `k > length` | Same as `k == length` — the helper reverses the whole list; the suffix bound is effectively clamped to the full list. |
| `k == 1` | The splice point is at position `n − 1`; the suffix is a one-node list; the helper's single-node guard returns it unchanged; the stitch reassigns the same `next`/`prev`. List unchanged. |
| Two nodes, `k = 1` | `length = 2`, splice point at position `1` (the head), suffix is the second node alone — helper is a no-op, list unchanged. |
| Two nodes, `k = 2` | `k == length`; full-list reversal via the shortcut. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Suffix reversal is full-list reversal applied to a detached sublist — measure the length, walk to the splice point, clear the suffix's `prev` boundary, hand the standalone suffix to the reversal helper, then stitch both directions across the cut. The splice point reference is the only piece of state that survives both the walk and the reversal call.

</details>
