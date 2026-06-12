---
title: "Swap Nth Nodes"
summary: "Given the head of a singly linked list and a non-negative integer N, write a function to swap the Nth node from the beginning with the Nth node from the end and return the head of the reordered list."
prereqs:
  - 09-pattern-sliding-window-traversal/01-pattern
difficulty: medium
kind: problem
topics: [sliding-window-traversal, singly-linked-list]
---

# Swap Nth nodes

## Problem Statement

Given the **head** of a singly linked list and a non-negative integer **N**, write a function to swap the Nth node from the beginning with the Nth node from the end and return the head of the reordered list.

Swapping of data is not allowed. Only references should be changed. You can assume that `N` will always be less than or equal to the size of the linked list.

## Examples

**Example 1:**
```
Input:  head = [1, 2, 3, 4, 5], N = 2
Output: [1, 4, 3, 2, 5]
```
The 2nd from the start is `2`; the 2nd from the end is `4`. After swapping their positions in the chain, the list reads `[1, 4, 3, 2, 5]`.

**Example 2:**
```
Input:  head = [1, 2, 3, 4, 5], N = 3
Output: [1, 2, 3, 4, 5]
```
The 3rd from the start IS the 3rd from the end — they are the same node, so no swap happens.

**Example 3:**
```
Input:  head = [1, 2, 3, 4, 5], N = 5
Output: [5, 2, 3, 4, 1]
```
`N` equals the length, so the swap exchanges the head and the tail.

## Constraints

- `1 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `1 ≤ N ≤ list length`
- Only pointer rewiring is allowed — node values must not be swapped

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def swap_nth_nodes(self, head, n):
        # Your code goes here — one priming walk to pin the N-th from start,
        # one lockstep slide to pin the N-th from end; capture predecessors
        # for the four-pointer rewire. Swap pointers, never values.
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

head_vals = ast.literal_eval(input())   # the test case's head
n = int(input())                         # the test case's N
head = build_list(head_vals) if head_vals else None
print_list(Solution().swap_nth_nodes(head, n))
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
        ListNode swapNthNodes(ListNode head, int n) {
            // Your code goes here — one priming walk to pin the N-th from start,
            // one lockstep slide to pin the N-th from end; capture predecessors
            // for the four-pointer rewire. Swap pointers, never values.
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] headVals = parseIntArray(sc.nextLine());
        int n = Integer.parseInt(sc.nextLine().trim());
        ListNode head = buildList(headVals);
        printList(new Solution().swapNthNodes(head, n));
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[1, 2, 3, 4, 5]" },
    { "id": "n", "label": "N", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "head": "[1, 2, 3, 4, 5]", "n": "2" }, "expected": "[1, 4, 3, 2, 5]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "n": "3" }, "expected": "[1, 2, 3, 4, 5]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "n": "5" }, "expected": "[5, 2, 3, 4, 1]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "n": "1" }, "expected": "[5, 2, 3, 4, 1]" },
    { "args": { "head": "[1]", "n": "1" }, "expected": "[1]" },
    { "args": { "head": "[1, 2]", "n": "1" }, "expected": "[2, 1]" },
    { "args": { "head": "[1, 2]", "n": "2" }, "expected": "[2, 1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a sliding-window-traversal problem is that the two nodes to be swapped sit at symmetric offsets — `N` from the start and `N` from the end — which is two fixed offsets in one problem. The naive answer walks the list once to compute its length and then walks twice more to find each of the two nodes (and their predecessors). The single-pass alternative recognises that the `N`-th-from-end can be tracked by trailing a pointer behind a leading cursor that already walked `N − 1` hops past the head.

The **pointer placement** follows directly. One pointer `nth_from_start` is parked on the `N`-th node from the start by walking it `N − 1` hops alone — this is the priming phase. A second cursor `current` shadows that walk so it ends up `N − 1` hops ahead of the head. From there, `nth_from_end` is initialised at the head and the lockstep slide begins — `current` walks until `current.next` is `null`, with `nth_from_end` trailing by exactly `N − 1` hops, so `nth_from_end` lands on the symmetric node. Two predecessor pointers (`prev_to_nth_from_start`, `prev_to_nth_from_end`) are captured during the same walks to enable the four-pointer splice that rewires the two `next` chains in place.

What **breaks if you reach for a naive approach**? Three passes (length, then two lookups) work but waste a full traversal. Copying values into an array, swapping at indices `N − 1` and `length − N`, and writing back uses `O(n)` extra space — and the problem explicitly forbids value-swapping; only references may move. Only the lockstep two-pointer walk with predecessor tracking delivers a single pass with `O(1)` extra space.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Swap Nth Nodes |
|---|---|
| **Q1.** Does the problem reference nodes at fixed offsets from the start / end? | **Yes** — `N` from the start AND `N` from the end. |
| **Q2.** Can the answers be read off when one pointer reaches the tail? | **Yes** — after a single lockstep walk, both target nodes (and their predecessors) are pinned. |
| **Q3.** Is the work at each tick `O(1)`? | **Yes** — three pointer assignments per tick; constant-work splice at the end. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — five local references regardless of list length. |

</details>
<details>
<summary><h2>Brute Force: Length Lookup + Two Walks</h2></summary>


Walk the list once to compute its length `length`. Compute the `N`-th-from-start at index `N − 1` and the `N`-th-from-end at index `length − N`. Walk from the head twice — once to find each target and its predecessor. Rewire the four `next` pointers. This is correct but costs three full traversals, and it does no better on space than the single-pass approach. The extra walks are pure overhead.

</details>
<details>
<summary><h2>Key Insight: One Priming Walk Pins Both Symmetric Targets</h2></summary>


Advancing a cursor `current` from `head` by `N − 1` hops parks it on the `N`-th-from-start node. From that position, the gap between `current` (the leading reference) and a fresh `nth_from_end` pointer at the head is exactly `N − 1`. As both advance together until `current.next` is `null`, the gap is preserved — so `nth_from_end` lands on the `N`-th-from-end node. Predecessor pointers (`prev_to_nth_from_start`, `prev_to_nth_from_end`) are captured during the walks so the final swap rewires `next` pointers without losing references.

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the lockstep walk in two phases — prime to find the `N`-th-from-start, then slide to find the `N`-th-from-end. Capture predecessors during both walks.

1. **Handle the trivial inputs.** If `head` is `null` or the list has only one node, return `head` — there is nothing to swap.
2. **Prime to the `N`-th from the start.** Initialise `nth_from_start = head`, `prev_to_nth_from_start = null`, and `current = head`. Walk `N − 1` hops, on each step assigning `prev_to_nth_from_start = nth_from_start`, then advancing both `nth_from_start` and `current` by one. After this phase, `nth_from_start` is on the `N`-th-from-start node and `current` shadows it.
3. **Slide to the `N`-th from the end.** Initialise `nth_from_end = head` and `prev_to_nth_from_end = null`. While `current.next` is not `null`, assign `prev_to_nth_from_end = nth_from_end`, advance `nth_from_end` by one, and advance `current` by one. The gap of `N − 1` between `current` and `nth_from_end` is preserved.
4. **Detect the same-node case.** If `nth_from_start` and `nth_from_end` are the same node, no swap is needed — return the head. (The code handles this implicitly via the predecessor logic, but the case must be acknowledged.)
5. **Rewire the four `next` pointers.** Update the predecessor of each target to point at the other target, then swap the two `next` references. If either predecessor is `null`, the swap involves the head — promote the other target to the new head.

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
    def swap_nodes(
        self,
        head,
        prev_to_nth_from_start,
        nth_from_start,
        prev_to_nth_from_end,
        nth_from_end,
    ):

        # If the Nth node from the beginning is the same as the Nth
        # node from the end, no swapping is needed
        if prev_to_nth_from_start is not None:
            prev_to_nth_from_start.next = nth_from_end

        # If Nth node from the beginning is the head, update the
        # head
        else:
            head = nth_from_end

        # If the Nth node from the end is the same as the Nth node
        # from the beginning, no swapping is needed
        if prev_to_nth_from_end is not None:
            prev_to_nth_from_end.next = nth_from_start

        # If Nth node from the end is the head, update the head
        else:
            head = nth_from_start

        # Swap the next pointers of the two nodes
        temp = nth_from_start.next
        nth_from_start.next = nth_from_end.next
        nth_from_end.next = temp

        return head

    def swap_nth_nodes(self, head, n):

        # If the list is empty or has only one node, no swapping is
        # needed.
        if head is None or head.next is None:
            return head

        # Pointer to the Nth node from the beginning
        nth_from_start = head

        # Pointer to the node before the nth_from_start node
        prev_to_nth_from_start = None

        # Pointer to keep track of the end of the list
        current = head

        # Traverse to the Nth node from the beginning
        for _ in range(1, n):
            prev_to_nth_from_start = nth_from_start
            nth_from_start = nth_from_start.next
            current = current.next

        # Pointer to the Nth node from the end
        nth_from_end = head

        # Pointer to the node before the nth_from_end node
        prev_to_nth_from_end = None

        # Find the Nth node from the end
        while current is not None and current.next is not None:
            prev_to_nth_from_end = nth_from_end
            nth_from_end = nth_from_end.next
            current = current.next

        return self.swap_nodes(
            head,
            prev_to_nth_from_start,
            nth_from_start,
            prev_to_nth_from_end,
            nth_from_end,
        )

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

head_vals = ast.literal_eval(input())   # the test case's head
n = int(input())                         # the test case's N
head = build_list(head_vals) if head_vals else None
print_list(Solution().swap_nth_nodes(head, n))
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
        private ListNode swapNodes(
            ListNode head,
            ListNode prevToNthFromStart,
            ListNode nthFromStart,
            ListNode prevToNthFromEnd,
            ListNode nthFromEnd
        ) {

            // If the Nth node from the beginning is the same as the Nth
            // node from the end, no swapping is needed
            if (prevToNthFromStart != null) {
                prevToNthFromStart.next = nthFromEnd;
            }

            // If Nth node from the beginning is the head, update the
            // head
            else {
                head = nthFromEnd;
            }

            // If the Nth node from the end is the same as the Nth node
            // from the beginning, no swapping is needed
            if (prevToNthFromEnd != null) {
                prevToNthFromEnd.next = nthFromStart;
            }

            // If Nth node from the end is the head, update the head
            else {
                head = nthFromStart;
            }

            // Swap the next pointers of the two nodes
            ListNode temp = nthFromStart.next;
            nthFromStart.next = nthFromEnd.next;
            nthFromEnd.next = temp;

            return head;
        }

        public ListNode swapNthNodes(ListNode head, int N) {

            // If the list is empty or has only one node, no swapping is
            // needed.
            if (head == null || head.next == null) {
                return head;
            }

            // Pointer to the Nth node from the beginning
            ListNode nthFromStart = head;

            // Pointer to the node before the nthFromStart node
            ListNode prevToNthFromStart = null;

            // Pointer to keep track of the end of the list
            ListNode current = head;

            // Traverse to the Nth node from the beginning
            for (int i = 1; i < N; ++i) {
                prevToNthFromStart = nthFromStart;
                nthFromStart = nthFromStart.next;
                current = current.next;
            }

            // Pointer to the Nth node from the end
            ListNode nthFromEnd = head;

            // Pointer to the node before the nthFromEnd node
            ListNode prevToNthFromEnd = null;

            // Find the Nth node from the end
            while (current != null && current.next != null) {
                prevToNthFromEnd = nthFromEnd;
                nthFromEnd = nthFromEnd.next;
                current = current.next;
            }

            return swapNodes(
                head,
                prevToNthFromStart,
                nthFromStart,
                prevToNthFromEnd,
                nthFromEnd
            );
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] headVals = parseIntArray(sc.nextLine());
        int n = Integer.parseInt(sc.nextLine().trim());
        ListNode head = buildList(headVals);
        printList(new Solution().swapNthNodes(head, n));
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

### Dry Run

```
head = 1 → 2 → 3 → 4 → 5, N = 2

Init: nth_from_start = 1, prev_to_nth_from_start = null, current = 1

Step 1 — prime to N-th from start (N − 1 = 1 iteration):
  Iter 1: prev_to_nth_from_start = 1; nth_from_start = 2; current = 2

After priming: nth_from_start = 2, prev_to_nth_from_start = 1, current = 2.

Init: nth_from_end = 1, prev_to_nth_from_end = null.

Step 2 — slide while current.next is not null:
  Tick 1: prev_to_nth_from_end = 1; nth_from_end = 2; current = 3
  Tick 2: prev_to_nth_from_end = 2; nth_from_end = 3; current = 4
  Tick 3: prev_to_nth_from_end = 3; nth_from_end = 4; current = 5
          current.next is null → loop ends.

Targets: nth_from_start = 2, nth_from_end = 4.
Predecessors: prev_to_nth_from_start = 1, prev_to_nth_from_end = 3.

Rewire:
  1.next = 4   (prev_to_nth_from_start.next = nth_from_end)
  3.next = 2   (prev_to_nth_from_end.next   = nth_from_start)
  temp = 2.next = 3
  2.next = 4.next = 5
  4.next = temp = 3

Return head = 1 → 4 → 3 → 2 → 5. ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | `current` walks from the head to the tail exactly once across priming and slide; all other pointers shadow that walk. |
| **Space** | `O(1)` | Five local references (the two targets, their two predecessors, and `current`) regardless of `n`. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | The first guard returns `null`. |
| Single node (`head.next is null`) | The first guard returns `head` unchanged. |
| Two nodes, `N = 1` (`[1, 2]`) | `nth_from_start = 1` (head), `nth_from_end = 2` (tail). Both predecessors are `null` / non-null appropriately; result is `[2, 1]`. |
| `N = 1` (swap head and tail) | `prev_to_nth_from_start` is `null` → head is promoted to `nth_from_end`. Tail is rewired into the head's old position. |
| `N` equals length (swap tail and head) | Symmetric to the above — `prev_to_nth_from_end` becomes `null`. |
| Both targets are the same node (`N == (length + 1) / 2` on odd length) | The swap helper's pointer rewires are self-cancelling; the list emerges unchanged. |
| `N == length / 2 + 1` (adjacent targets, even length) | Rewires handle the adjacency correctly because each target's predecessor is captured before the swap. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


`Swap Nth Nodes` is the symmetric-swap variant of sliding-window traversal: one priming walk pins the `N`-th-from-start, then a single lockstep slide with `N − 1` gap pins the `N`-th-from-end — both predecessors captured along the way for an `O(1)` four-pointer rewire.

</details>
