---
title: "Pairwise Swap"
summary: "Given the head of a singly linked list, write a function to swap every two adjacent nodes of this list and return the head of the reordered list."
prereqs:
  - 08-pattern-reversal-subproblem/01-pattern
difficulty: easy
kind: problem
topics: [reversal-subproblem, singly-linked-list]
---

# Pairwise swap

## Problem Statement

Given the **head** of a singly linked list, write a function to **swap every two adjacent nodes** of this list and return the head of the reordered list.

The problem needs to be solved without modifying the values in the list's nodes. The nodes should be reordered by updating links.

## Examples

**Example 1**
```
Input:  head = [1, 2, 3, 4]
Output: [2, 1, 4, 3]
Explanation: Swap pair (1, 2) → (2, 1) and pair (3, 4) → (4, 3); the list becomes [2, 1, 4, 3].
```

**Example 2**
```
Input:  head = [1, 2, 3, 4, 5]
Output: [2, 1, 4, 3, 5]
Explanation: Two full pairs swap; the trailing single node 5 has no partner and stays in place.
```

**Example 3**
```
Input:  head = [1]
Output: [1]
Explanation: A single node has no pair to swap with, so the list is returned unchanged.
```

**Example 4**
```
Input:  head = []
Output: []
Explanation: An empty list trivially produces an empty list.
```

```quiz
{
  "prompt": "What does pairwise swap on [1, 2, 3, 4, 5, 6] produce?",
  "input": "head = [1, 2, 3, 4, 5, 6]",
  "options": ["[2, 1, 4, 3, 6, 5]", "[1, 2, 3, 4, 5, 6]", "[6, 5, 4, 3, 2, 1]", "[2, 1, 3, 4, 6, 5]"],
  "answer": "[2, 1, 4, 3, 6, 5]"
}
```

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- Reorder by updating **links**, not by swapping values

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def pairwise_swap(self, head):
        # Your code goes here — reverse every pair of adjacent nodes using
        # the bounded reverse helper and seam stitching. Return new head.
        pass

def build_list(values):              # [1, 2, 3] → 1 → 2 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

def print_list(head):                # 1 → 2 → 3 → [1, 2, 3]
    out = []
    while head:
        out.append(head.val)
        head = head.next
    print(out)

head = build_list(ast.literal_eval(input()))   # the test case's head
print_list(Solution().pairwise_swap(head))
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
        ListNode pairwiseSwap(ListNode head) {
            // Your code goes here — reverse every pair of adjacent nodes using
            // the bounded reverse helper and seam stitching. Return new head.
            return null;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        printList(new Solution().pairwiseSwap(head));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 → 2 → 3 → null
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
    }

    static void printList(ListNode head) {         // 1 → 2 → 3 → [1, 2, 3]
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[1, 2, 3, 4]" }
  ],
  "cases": [
    { "args": { "head": "[1, 2, 3, 4]" }, "expected": "[2, 1, 4, 3]" },
    { "args": { "head": "[1, 2, 3, 4, 5]" }, "expected": "[2, 1, 4, 3, 5]" },
    { "args": { "head": "[1, 2]" }, "expected": "[2, 1]" },
    { "args": { "head": "[1, 2, 3]" }, "expected": "[2, 1, 3]" },
    { "args": { "head": "[1]" }, "expected": "[1]" },
    { "args": { "head": "[]" }, "expected": "[]" },
    { "args": { "head": "[1, 2, 3, 4, 5, 6]" }, "expected": "[2, 1, 4, 3, 6, 5]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** is that pairwise swap is reverse-k-segments with `k = 2` hard-coded. Every adjacent pair is its own subproblem: a two-node reversal that swaps `(a, b) → (b, a)`. The list decomposes into `⌊n / 2⌋` independent two-node chunks plus an optional trailing singleton. That makes the problem the simplest concrete instance of the reversal-subproblem pattern — a worked example of the family without the complication of a variable chunk size or a conditional reversal flag.

The **pointer placement** uses the same four boundaries as the general k-segment case. `start` points at the pair's first node and `end = start.next` at its second; `leftBound` caches the predecessor (initially `None`, so the global `head` will be updated for the first pair); `rightBound` is implicit as `end.next`. After the inner reversal flips `start ↔ end`, the old `start` is now the pair's tail and becomes the next iteration's `leftBound`. The outer loop continues while `start` and `start.next` both exist, which is the natural termination check: if either is `None`, the remaining list is shorter than two nodes and cannot form a pair.

What **breaks if you reach for value-swapping**? Copying `val` between adjacent nodes works for the swap-by-value reading of the problem but violates the constraint "reorder by updating links." The constraint exists because many real linked-list problems (LeetCode 24, doubly-linked-list manipulations, lock-free list rewrites) require true link-level reordering. Without pointer manipulation, you also lose the muscle memory for the harder variants in this section. The segment-reversal call resolves both concerns: it flips the actual `next` pointers, and the same helper transfers to reverse-k-segments without modification.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>

Pairwise swap is the textbook instance of the reversal-subproblem pattern with `k = 2`. The diagnostic confirms the fit before the implementation lands.

| Check | Answer for Pairwise Swap |
|---|---|
| **Q1.** Can the problem or solution be broken down into smaller subproblems? | **Yes** — the rewrite is a sequence of `⌊n / 2⌋` independent two-node reversals; each pair is its own subproblem and the pairs do not interact. |
| **Q2.** Can any subproblem be solved by reversing a part of the linked list? | **Yes** — a two-node reversal is the segment-reversal primitive with `start` and `end = start.next`; one call to `reverse(start, end)` flips a single link. |
| **Q3.** Does the algorithm only need to walk each node a constant number of times? | **Yes** — each pair is touched exactly once: locate `end` in one hop, reverse the pair in `O(1)`, stitch the seam in `O(1)`. Total cost is one forward walk. |
| **Q4.** Is each chunk's boundary computable from local state? | **Yes** — `end` is always `start.next`; the seam re-attachment uses `leftBound` from the previous iteration. No length precomputation is needed because the per-iteration `start and start.next` guard handles the boundary directly. |

</details>
<details>
<summary><h2>Approach</h2></summary>

Five numbered steps. No code; the next section is the implementation.

1. **Guard the trivial cases.** If `head` is `None` or `head.next` is `None`, the list has zero or one node and no pair exists. Return `head` unchanged.
2. **Initialise the boundary pointers.** Set `start = head` and `leftBound = None`. The `None` marker is what the seam-stitch step uses to detect the first pair and update the global `head` instead of writing through a predecessor.
3. **Loop while a full pair exists.** The guard is `start is not None and start.next is not None`. As soon as either is `None`, the trailing fragment is shorter than two nodes and the loop ends, leaving the fragment untouched.
4. **Reverse the current pair and stitch the seam.** Let `end = start.next`. Call `reverse(start, end)` to get `reversed_head` (the pair's new first node, which is the old `end`). If `leftBound` is `None`, update the global `head = reversed_head`; otherwise set `leftBound.next = reversed_head` so the previous pair's tail re-attaches to this pair's new head.
5. **Slide the boundary forward.** The old `start` is now the pair's tail and the natural `leftBound` for the next pair. Set `leftBound = start`, then `start = start.next` (which now points at the next pair's first node because the previous step's seam stitch put it there).

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
    def reverse(self, start, end):
        current = start
        right_bound = end.next
        previous = right_bound

        while current != right_bound:
            next_node = current.next
            current.next = previous
            previous = current
            current = next_node

        return previous

    def pairwise_swap(self, head):

        # If the list is empty or has only one element, no reversal
        # needed.
        if head is None or head.next is None:

            # Return the original head
            return head

        # Start of the current pair to be reversed
        start = head

        # Initialize the 'left_bound' pointer for the first pair's
        # reversal.
        left_bound = None

        # Loop while there are pairs to be swapped
        while start is not None and start.next is not None:

            # Get the end node of the current pair
            end = start.next

            # Get the head of the reversed pair.
            reversed_head = self.reverse(start, end)

            # Check if there is a previous segment to connect to or
            # if the existing head needs to be updated.
            # If leftBound is null, it means we're at the first segment
            # So, we need to update the head to the reversedHead
            if left_bound is None:
                head = reversed_head

            # If there is a leftBound, connect its next to the new
            # reversedHead
            else:
                left_bound.next = reversed_head

            # Update left_bound to the current pair's start
            # (which is now the end after reversal)
            left_bound = start

            # Move start to the next pair
            start = start.next

        # Return the head of the modified list
        return head

def build_list(values):              # [1, 2, 3] → 1 → 2 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

def print_list(head):                # 1 → 2 → 3 → [1, 2, 3]
    out = []
    while head:
        out.append(head.val)
        head = head.next
    print(out)

head = build_list(ast.literal_eval(input()))   # the test case's head
print_list(Solution().pairwise_swap(head))
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
        private ListNode reverse(ListNode start, ListNode end) {
            ListNode current = start;
            ListNode rightBound = end.next;
            ListNode previous = rightBound;

            while (current != rightBound) {
                ListNode next = current.next;
                current.next = previous;
                previous = current;
                current = next;
            }

            // Return the new head of the reversed segment
            return previous;
        }

        public ListNode pairwiseSwap(ListNode head) {

            // If the list is empty or has only one element, no reversal
            // needed.
            if (head == null || head.next == null) {
                return head;
            }

            // Start of the current pair to be reversed
            ListNode start = head;

            // Initialize the 'leftBound' pointer for the first pair's
            // reversal.
            ListNode leftBound = null;

            // Loop while there are pairs to be swapped
            while (start != null && start.next != null) {

                // Get the end node of the current pair
                ListNode end = start.next;

                // Get the head of the reversed pair.
                ListNode reversedHead = reverse(start, end);

                // Check if there is a previous segment to connect to or
                // if the existing head needs to be updated.
                // If leftBound is null, it means we're at the first
                // segment So, we need to update the head to the
                // reversedHead
                if (leftBound == null) {
                    head = reversedHead;
                }

                // If there is a leftBound, connect its next to the new
                // reversedHead
                else {
                    leftBound.next = reversedHead;
                }

                // Update leftBound to the current pair's start
                // (which is now the end after reversal)
                leftBound = start;

                // Move start to the next pair
                start = start.next;
            }

            // Return the head of the modified list
            return head;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        printList(new Solution().pairwiseSwap(head));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 → 2 → 3 → null
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
    }

    static void printList(ListNode head) {         // 1 → 2 → 3 → [1, 2, 3]
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

### Dry Run — Example 1

`head = [1, 2, 3, 4]`

Initial state: `start = 1`, `left_bound = None`.

**Iteration 1 — pair `(1, 2)`:**

| step | state |
|---|---|
| `end = start.next` | `end = 2` |
| `reverse(1, 2)` | links flip → `2 → 1 → 3`; returns `reversed_head = 2` |
| `left_bound is None` → update head | `head = 2` |
| `left_bound = start` | `left_bound = 1` (the pair's new tail) |
| `start = start.next` | `start = 3` |

List after iteration 1: `2 → 1 → 3 → 4`.

**Iteration 2 — pair `(3, 4)`:**

| step | state |
|---|---|
| `end = start.next` | `end = 4` |
| `reverse(3, 4)` | links flip → `4 → 3` and the pair's tail `3` now points at `None`; returns `reversed_head = 4` |
| `left_bound is not None` → stitch | `left_bound.next = 4`, so `1.next = 4` |
| `left_bound = start` | `left_bound = 3` |
| `start = start.next` | `start = None` |

List after iteration 2: `2 → 1 → 4 → 3`.

**Iteration 3 — loop guard:** `start is None` → exit.

**Return:** `head = 2`, traversal yields `[2, 1, 4, 3]` ✓

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | `O(n)` | The outer loop processes `⌊n / 2⌋` pairs. Each pair's `reverse(start, end)` call flips one link in `O(1)`, and the seam stitch is `O(1)`. Total work scales linearly with `n`. |
| **Space** | `O(1)` | Four boundary pointers (`start`, `end`, `left_bound`, `reversed_head`) and no auxiliary structures. The rewrite is fully in place. |

### Edge Cases

| Scenario | Input | Output | Note |
|---|---|---|---|
| Empty list | `[]` | `[]` | First guard returns immediately. |
| Single node | `[1]` | `[1]` | `head.next is None` → guard returns; no pair to swap. |
| Exactly one pair | `[1, 2]` | `[2, 1]` | One iteration; `head` updated to `2`; loop exits because `start = None`. |
| Odd length, last node alone | `[1, 2, 3]` | `[2, 1, 3]` | Final iteration's `start = 3, start.next = None` fails the guard; the trailing `3` is left untouched. |
| All equal values | `[5, 5, 5, 5]` | `[5, 5, 5, 5]` | Pointer manipulation runs correctly even when the rewrite produces a value-identical list; structural change is invisible to a printout. |
| Even length, multiple pairs | `[1, 2, 3, 4, 5, 6]` | `[2, 1, 4, 3, 6, 5]` | Three independent pair reversals; each seam stitch handed off through `left_bound`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

Pairwise swap is reverse-k-segments with `k = 2` hard-coded — every adjacent pair is a two-node reversal subproblem, and the per-iteration guard `start and start.next` replaces the explicit `length / k` outer counter. The shared `reverse(start, end)` helper carries over unchanged to the harder variants in this section.

</details>
