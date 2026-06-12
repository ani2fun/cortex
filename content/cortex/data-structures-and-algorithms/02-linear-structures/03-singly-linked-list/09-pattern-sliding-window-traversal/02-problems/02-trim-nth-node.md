---
title: "Trim Nth Node"
summary: "Given the head of a singly linked list and a non-negative integer N, write a function to remove the Nth node from the end of the list and return the head of the updated list."
prereqs:
  - 09-pattern-sliding-window-traversal/01-pattern
difficulty: easy
kind: problem
topics: [sliding-window-traversal, singly-linked-list]
---

# Trim Nth node

## Problem Statement

Given the **head** of a singly linked list and a non-negative integer **N**, write a function to remove the Nth node from the end of the list and return the head of the updated list.

## Examples

**Example 1:**
```
Input:  head = [1, 2, 3, 4, 5], N = 2
Output: [1, 2, 3, 5]
```
The second node from the end is `4`. Removing it produces `[1, 2, 3, 5]`.

**Example 2:**
```
Input:  head = [1], N = 1
Output: []
```
The only node is the first from the end. Removing it leaves an empty list.

**Example 3:**
```
Input:  head = [1, 2, 3, 4, 5], N = 5
Output: [2, 3, 4, 5]
```
`N` equals the length, so the victim is the head itself — return `head.next`.

## Constraints

- `1 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `1 ≤ N ≤ list length`

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def trim_nth_node(self, head, n):
        # Your code goes here — prime a gap of n-1 with a leading cursor,
        # then slide together until the lead reaches the tail; the trailing
        # pointer is on the victim, with a predecessor shadow for the splice.
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
print_list(Solution().trim_nth_node(head, n))
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
        ListNode trimNthNode(ListNode head, int n) {
            // Your code goes here — prime a gap of n-1 with a leading cursor,
            // then slide together until the lead reaches the tail; the trailing
            // pointer is on the victim, with a predecessor shadow for the splice.
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] headVals = parseIntArray(sc.nextLine());
        int n = Integer.parseInt(sc.nextLine().trim());
        ListNode head = buildList(headVals);
        printList(new Solution().trimNthNode(head, n));
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
    { "args": { "head": "[1, 2, 3, 4, 5]", "n": "2" }, "expected": "[1, 2, 3, 5]" },
    { "args": { "head": "[1]", "n": "1" }, "expected": "[]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "n": "5" }, "expected": "[2, 3, 4, 5]" },
    { "args": { "head": "[1, 2]", "n": "1" }, "expected": "[1]" },
    { "args": { "head": "[1, 2]", "n": "2" }, "expected": "[2]" },
    { "args": { "head": "[1, 2, 3]", "n": "3" }, "expected": "[2, 3]" },
    { "args": { "head": "[1, 2, 3]", "n": "1" }, "expected": "[1, 2]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "n": "1" }, "expected": "[1, 2, 3, 4]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a sliding-window-traversal problem is that the victim is identified by a fixed offset from the tail — the `N`-th-from-end node — and a singly linked list cannot be walked backwards. The naive answer walks the list once to count its length, then walks again from the head for `length − N` steps to reach the victim's predecessor. A single-pass alternative exists because a fixed gap between two pointers turns "the tail" into a moving boundary that the trailing pointer can ride.

The **pointer placement** follows directly. Two pointers `start` and `end` are initialised at `head`. `end` is advanced `N − 1` hops ahead during the priming phase, so the gap between them is `N − 1` — i.e. when `end` later reaches the tail, `start` is exactly `N − 1` hops behind it, which is the `N`-th-from-end node. To splice it out, we also need its predecessor, so a third reference `prev_to_start` is kept one node behind `start` during the lockstep slide.

What **breaks if you reach for a naive approach**? Two passes work — count, then walk — but require either a known finite list or the ability to rewind a stream. A recursive solution that "unwinds" on the way back from the tail uses `O(n)` stack space and overflows on long lists. Only the lockstep two-pointer walk with a `prev_to_start` carry-along delivers `O(n)` time and `O(1)` extra space in a single forward pass.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Trim Nth Node |
|---|---|
| **Q1.** Does the problem reference a node at a fixed offset from the tail? | **Yes** — the `N`-th-from-end node is the victim. |
| **Q2.** Can the answer be read off when one pointer reaches the tail? | **Yes** — when `end.next` is `null`, `start` is on the victim and `prev_to_start` is on its predecessor. |
| **Q3.** Is the work at each tick `O(1)`? | **Yes** — three pointer assignments per tick; one splice at the end. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — three local references (`start`, `prev_to_start`, `end`) regardless of list length. |

</details>
<details>
<summary><h2>Brute Force: Count, Then Walk</h2></summary>


Walk the list once to compute its length `length`. If `N == length`, the victim IS the head — return `head.next`. Otherwise, walk from `head` for `length − N − 1` steps to land on the predecessor of the victim; splice with `prev.next = prev.next.next` and return the head. This is correct but requires two passes; on a streaming source where the list cannot be rewound, the first pass also consumes the only forward walk available.

</details>
<details>
<summary><h2>Key Insight: Trail the Victim by `N − 1` Hops</h2></summary>


Initialise `end` at the head and walk it `N − 1` hops alone. The gap between `start` (still at the head) and `end` is now `N − 1`. As both pointers slide together, the gap is preserved — so when `end` reaches the tail (`end.next is None`), `start` is exactly `N − 1` hops before the tail, which is the `N`-th-from-end node. A third reference `prev_to_start` shadows `start` one node behind it, ready for the splice.

</details>
<details>
<summary><h2>Approach</h2></summary>


Maintain three pointers (`start`, `prev_to_start`, `end`). Walk the list once.

1. **Handle the empty list.** If `head` is `null`, return `null` immediately.
2. **Prime the gap.** Initialise a single cursor `current` at the head. Advance it `N − 1` steps (`for i in range(1, N)`). If `current` becomes `null` mid-walk, `N` exceeded the list length — return `head` unchanged.
3. **Detect the head-is-victim case.** If `current.next` is `null` after priming, then `N` equals the length of the list — the head itself is the `N`-th-from-end. Return `head.next`.
4. **Slide together.** Initialise `start = head` and `prev_to_start = null`. While `current.next` is not `null`, advance `prev_to_start = start`, `start = start.next`, and `current = current.next`. The gap is preserved; `prev_to_start` shadows `start`.
5. **Splice.** When the loop exits, `start` is the victim and `prev_to_start` is its predecessor. Set `prev_to_start.next = start.next` and return `head`.

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
    def trim_nth_node(self, head, n):

        # Handle edge case for an empty list
        if head is None:
            return None

        # Pointer to keep track of the end of the list
        current = head

        # Move the current pointer n steps ahead
        for _ in range(1, n):

            # If n is greater than the length of the list
            if current is None:
                return head
            current = current.next

        # If the current pointer is now at the last node, it means we
        # need to remove the head
        if current.next is None:
            return head.next

        nth_node_from_end = head
        prev_to_nth_node_from_end = None

        # Move both pointers until the current pointer reaches the last
        # node
        while current is not None and current.next is not None:
            prev_to_nth_node_from_end = nth_node_from_end
            nth_node_from_end = nth_node_from_end.next
            current = current.next

        # Now, prev_to_nth_node_from_end points to the node before the
        # one we want to remove
        prev_to_nth_node_from_end.next = nth_node_from_end.next

        # Return the modified list
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

head_vals = ast.literal_eval(input())   # the test case's head
n = int(input())                         # the test case's N
head = build_list(head_vals) if head_vals else None
print_list(Solution().trim_nth_node(head, n))
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
        public ListNode trimNthNode(ListNode head, int N) {

            // Handle edge case for an empty list
            if (head == null) {
                return null;
            }

            // Pointer to keep track of the end of the list
            ListNode current = head;

            // Move the current pointer N steps ahead
            for (int i = 1; i < N; i++) {

                // If N is greater than the length of the list
                if (current == null) {
                    return head;
                }
                current = current.next;
            }

            // If the current pointer is now at the last node, it means we
            // need to remove the head
            if (current.next == null) {
                return head.next;
            }

            ListNode nthNodeFromEnd = head;
            ListNode prevToNthNodeFromEnd = null;

            // Move both pointers until the current pointer reaches the last
            // node
            while (current != null && current.next != null) {
                prevToNthNodeFromEnd = nthNodeFromEnd;
                nthNodeFromEnd = nthNodeFromEnd.next;
                current = current.next;
            }

            // Now, prevToNthNodeFromEnd points to the node before the one we
            // want to remove
            prevToNthNodeFromEnd.next = nthNodeFromEnd.next;

            // Return the modified list
            return head;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] headVals = parseIntArray(sc.nextLine());
        int n = Integer.parseInt(sc.nextLine().trim());
        ListNode head = buildList(headVals);
        printList(new Solution().trimNthNode(head, n));
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

Init: current = 1

Step 1 — prime the gap (move current N − 1 = 1 step):
  Iter 1: current = 1 → advances to 2

current = 2, current.next = 3 (not null) → head is NOT the victim, proceed.

Initialise nth_from_end = 1, prev_to_nth_from_end = null.

Step 2 — slide while current.next is not null:
  Tick 1: prev_to_nth_from_end = 1; nth_from_end = 2; current = 3
  Tick 2: prev_to_nth_from_end = 2; nth_from_end = 3; current = 4
  Tick 3: prev_to_nth_from_end = 3; nth_from_end = 4; current = 5
          current.next is null → loop ends.

nth_from_end = 4 (the N-th-from-end victim).
prev_to_nth_from_end = 3.
prev_to_nth_from_end.next = nth_from_end.next = 5   → 3 → 5.

Return head = 1 → 2 → 3 → 5. ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | One forward pass — `current` advances `n − 1` times total across priming and slide. |
| **Space** | `O(1)` | Three local references (`current`, `nth_from_end`, `prev_to_nth_from_end`) regardless of list length. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | The first guard returns `null` immediately. |
| Single node, `N = 1` | Priming loop runs zero iterations; `current.next` is `null`; the head-is-victim branch returns `null`. |
| Two nodes, `N = 2` (`[1, 2]`) | Priming advances `current` to `2`; `current.next` is `null`; head is the victim; return `[2]`. |
| `N == length` | Priming leaves `current` on the tail; the head-is-victim branch returns `head.next`. |
| `N > length` | The priming loop's `current is None` guard fires; return `head` unchanged. |
| `N = 1` (trim last node) | Priming runs zero iterations; slide advances `prev_to_start` to the second-to-last node; splice removes the tail. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


`Trim Nth Node` is the trim variant of sliding-window traversal: the gap of `N − 1` parks `start` on the victim when `end` reaches the tail, and a `prev_to_start` shadow lets the splice happen in `O(1)` without a backward walk.

</details>
