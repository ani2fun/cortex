---
title: "Split List in Half"
summary: "Given the head of a singly linked list, write a function to split the input linked list into two halves and return the heads of the two split halves."
prereqs:
  - 10-pattern-fast-and-slow-pointers/01-pattern
difficulty: easy
kind: problem
topics: [fast-and-slow-pointers, singly-linked-list]
---

# Split list in half

## Problem Statement

Given the **head** of a singly linked list, write a function to split the input linked list into two halves and return the heads of the two split halves. 

If there is only one middle node, that node should be part of the first half. 

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10, 6, 8]
Output: [5, 7, 3]
        [10, 6, 8]
Explanation: Even length — split into two equal halves of 3 nodes each.
```

**Example 2:**
```
Input:  head = [5, 7, 3, 10, 6]
Output: [5, 7, 3]
        [10, 6]
Explanation: Odd length — the middle node (3) stays with the first half.
```

**Example 3:**
```
Input:  head = [5]
Output: [5]
        []
Explanation: Single node — first half holds it, second half is empty.
```

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def split_list_in_half(self, head):
        # Your code goes here — 2:1 walk with prev_to_slow tracking,
        # sever at the boundary, return [head, second_half].
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
h1, h2 = Solution().split_list_in_half(head)
print_list(h1)
print_list(h2)
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
        ListNode[] splitListInHalf(ListNode head) {
            // Your code goes here — 2:1 walk with prevToSlow tracking,
            // sever at the boundary, return {head, secondHalf}.
            return new ListNode[]{null, null};
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        ListNode[] r = new Solution().splitListInHalf(head);
        printList(r[0]);
        printList(r[1]);
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[5, 7, 3, 10, 6, 8]" }
  ],
  "cases": [
    { "args": { "head": "[5, 7, 3, 10, 6, 8]" }, "expected": "[5, 7, 3]\n[10, 6, 8]" },
    { "args": { "head": "[5, 7, 3, 10, 6]" }, "expected": "[5, 7, 3]\n[10, 6]" },
    { "args": { "head": "[5]" }, "expected": "[5]\n[]" },
    { "args": { "head": "[1, 2]" }, "expected": "[1]\n[2]" },
    { "args": { "head": "[1, 2, 3]" }, "expected": "[1, 2]\n[3]" },
    { "args": { "head": "[1, 2, 3, 4]" }, "expected": "[1, 2]\n[3, 4]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a fast-and-slow problem is that the cut point is at a fixed proportional position — the boundary between the first and second halves. Finding the middle node alone is not enough; the cut requires *severing* the list, which means rewriting one `next` pointer at the boundary. To do that, the walk needs to track not just the middle but also the node immediately before the cut, so its `next` field can be set to `null`.

The **pointer placement** extends the 2:1 walk with one extra reference. `slow` and `fast` walk at speeds 1 and 2 as usual; a third variable `prev_to_slow` trails `slow` by one node, capturing the predecessor at every tick. When the loop exits, `prev_to_slow` holds the last node of the first half — the exact node whose `next` field must be cleared. For even length, `fast` lands at `null` and `slow` sits at the start of the second half, so the cut happens just before `slow` (between `prev_to_slow` and `slow`). For odd length, `fast` lands at the tail and `slow` sits on the middle node (which belongs to the first half), so the cut happens just after `slow` (between `slow` and `slow.next`).

What **breaks if you reach for a naive approach**? Two passes — count the length, then walk `n / 2` steps to find the cut — runs in `O(n)` time and `O(1)` space, same asymptotic cost. The penalty is constant-factor (the prefix is walked twice) and the same boundary-bookkeeping problem still exists: the two-pass version also has to track the predecessor of the cut point. Fast-and-slow plus `prev_to_slow` does the whole job in one pass with no redundant work.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Split List in Half |
|---|---|
| **Q1.** Does the problem ask for a node at a proportional position? | **Yes** — the split point is at the `1/2` boundary, and the predecessor of that boundary needs to be tracked too. |
| **Q2.** Can the position be computed in a single forward pass? | **Yes** — the same 2:1 walk that finds the middle also exposes `prev_to_slow` if we capture it before each `slow` advance. |
| **Q3.** Is the work at each step `O(1)`? | **Yes** — each tick is three pointer hops and one assignment; no per-node scan. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — three local references (`slow`, `fast`, `prev_to_slow`) regardless of list length. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the 2:1 walk and capture the predecessor of `slow` each tick. Sever the list at the right boundary based on whether the length is odd or even.

1. **Handle the trivial cases first.** If `head` is `null` or has only one node, return `[head, null]` — there is no second half to extract.
2. **Initialise three pointers.** Set `slow = head`, `fast = head`, and `prev_to_slow = null`. The third reference will hold the last node of the first half once the loop ends.
3. **Run the 2:1 walk with predecessor tracking.** While `fast` and `fast.next` are both non-`null`: first save `prev_to_slow = slow` (capture the predecessor *before* `slow` moves), then advance `slow = slow.next` and `fast = fast.next.next`.
4. **Detect odd vs even length from `fast`.** After the loop, `fast == null` means even length (`fast` stepped one past the tail); `fast != null` means odd length (`fast` is at the tail, guard failed on `fast.next == null`).
5. **Sever the list at the right boundary.** For even length, the second half starts at `prev_to_slow.next` (which equals `slow`), and the cut is `prev_to_slow.next = null`. For odd length, `slow` is the middle node and belongs to the first half, so the second half starts at `slow.next` and the cut is `slow.next = null`.
6. **Return both heads.** Return `[head, second_half]`. The original `head` reference still points to the first half because the only `next` we rewrote was the one at the boundary.

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
    def split_list_in_half(self, head):

        # If the list is empty or has only one element, return the
        # original head and None
        if head is None or head.next is None:
            return [head, None]

        slow = head
        fast = head
        prev_to_slow = None

        # Find the midpoint of the list using the slow and fast pointer
        # technique
        while (
            fast is not None
            and fast.next is not None
            and slow is not None
        ):

            # Keep track of the node before the midpoint
            prev_to_slow = slow

            # Move the slow pointer by one step
            slow = slow.next

            # Move the fast pointer by two steps
            fast = fast.next.next

        second_half = None

        # If the fast pointer reached the end of the list, it has an even
        # number of nodes
        if fast is None and prev_to_slow is not None:

            # The second half starts from the next node of the previous
            # slow pointer
            second_half = prev_to_slow.next

            # Disconnect the two halves by setting the next of the
            # previous slow pointer to None
            prev_to_slow.next = None

        # else the list has an odd number of nodes
        else:
            if slow is not None:

                # The second half starts from the node after the slow
                # pointer
                second_half = slow.next

                # Disconnect the two halves by setting the next of the
                # slow pointer to None
                slow.next = None

        # Return a list containing the head of the first half and the
        # head of the second half
        return [head, second_half]

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
h1, h2 = Solution().split_list_in_half(head)
print_list(h1)
print_list(h2)
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
        ListNode[] splitListInHalf(ListNode head) {

            // If the list is empty or has only one element, return the
            // original head and null
            if (head == null || head.next == null) {
                return new ListNode[]{head, null};
            }

            ListNode slow = head;
            ListNode fast = head;
            ListNode prevToSlow = null;

            // Find the midpoint of the list using the slow and fast pointer
            // technique
            while (fast != null && fast.next != null) {

                // Keep track of the node before the midpoint
                prevToSlow = slow;

                // Move the slow pointer by one step
                slow = slow.next;

                // Move the fast pointer by two steps
                fast = fast.next.next;
            }

            ListNode secondHalf;

            // If the fast pointer reached the end of the list, it has an
            // even number of nodes
            if (fast == null) {

                // The second half starts from the next node of the previous
                // slow pointer
                secondHalf = prevToSlow.next;

                // Disconnect the two halves by setting the next of the
                // previous slow pointer to null
                prevToSlow.next = null;
            }

            // else the list has an odd number of nodes
            else {

                // The second half starts from the node after the slow
                // pointer
                secondHalf = slow.next;

                // Disconnect the two halves by setting the next of the slow
                // pointer to null
                slow.next = null;
            }

            // Return a list containing the head of the first half and the
            // head of the second half
            return new ListNode[]{head, secondHalf};
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        ListNode[] r = new Solution().splitListInHalf(head);
        printList(r[0]);
        printList(r[1]);
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
head = 5 → 7 → 3 → 10 → 6 → 8 → null   (Example 1, even length n=6)

Init: slow = 5, fast = 5, prev_to_slow = null

Tick 1: guard — fast=5, fast.next=7 → continue
        prev_to_slow = slow → prev_to_slow = 5
        slow = slow.next     → slow = 7
        fast = fast.next.next → fast = 3

Tick 2: guard — fast=3, fast.next=10 → continue
        prev_to_slow = slow → prev_to_slow = 7
        slow = slow.next     → slow = 3
        fast = fast.next.next → fast = 6

Tick 3: guard — fast=6, fast.next=8 → continue
        prev_to_slow = slow → prev_to_slow = 3
        slow = slow.next     → slow = 10
        fast = fast.next.next → fast = null

Tick 4: guard fails — fast=null → exit

fast == null → even length branch:
  second_half = prev_to_slow.next  → second_half = 10
  prev_to_slow.next = null         → cuts 3 from 10

Return [5, 10] (i.e. [5→7→3→null, 10→6→8→null]). ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | `fast` traverses the list in `n / 2` ticks; each tick performs `O(1)` work. |
| **Space** | `O(1)` | Three local references (`slow`, `fast`, `prev_to_slow`) regardless of list length. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | Early return `[null, null]` — no second half exists. |
| Single node (`[5]`) | Early return `[head, null]` — first half is the only node; second half is empty. |
| Two nodes (`[1, 2]`) | Even length. After loop, `prev_to_slow = 1`, `slow = 2`, `fast = null`. Cut `prev_to_slow.next = null`. Return `[1, 2]` (each half one node). |
| Three nodes (`[1, 2, 3]`) | Odd length. After loop, `prev_to_slow = 1`, `slow = 2`, `fast = 3`. Cut `slow.next = null`. Return `[1→2, 3]` — middle node `2` stays in first half. |
| Four nodes (`[1, 2, 3, 4]`) | Even length. After loop, `prev_to_slow = 2`, `slow = 3`, `fast = null`. Cut `prev_to_slow.next = null`. Return `[1→2, 3→4]`. |
| All equal values (`[7, 7, 7, 7]`) | Values are never inspected; the same boundary work runs and returns two two-node halves by identity. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Splitting a list at the middle is the middle-finding walk plus one extra reference — `prev_to_slow` — captured before each `slow` advance. The even-vs-odd branch decides whether the cut happens just before `slow` (even) or just after `slow` (odd); the loop body is identical to plain middle-finding.

</details>
