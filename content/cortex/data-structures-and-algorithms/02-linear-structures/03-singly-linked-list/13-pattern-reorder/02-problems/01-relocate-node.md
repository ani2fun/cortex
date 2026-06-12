---
title: "Relocate Node"
summary: "Move the last node of a singly linked list to the front with a single-pass two-cursor walk and two pointer rewrites."
prereqs:
  - 13-pattern-reorder/01-pattern
difficulty: easy
kind: problem
topics: [reorder, singly-linked-list]
---

# Relocate node

## Problem Statement

Given the **head** of a singly linked list, write a function to move the last node of the list to the start and return the head of the reordered list.

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10, 6, 8]
Output: [8, 5, 7, 3, 10, 6]
Explanation: The last node with value 8 is detached from the end and prepended to the head; every other node keeps its relative order.
```

**Example 2:**
```
Input:  head = [5, 7]
Output: [7, 5]
Explanation: The two-node list flips because moving the last node to the front is the same as swapping the pair.
```

**Example 3:**
```
Input:  head = [5]
Output: [5]
Explanation: A single-node list is unchanged — the first and last node are the same object, so detach-and-prepend is a no-op.
```

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- Reorder **in place** — `O(1)` extra space; node values must not be copied or rewritten

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def relocate_node(self, head):
        # Your code goes here
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
print_list(Solution().relocate_node(head))
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
        ListNode relocateNode(ListNode head) {
            // Your code goes here
            return null;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        printList(new Solution().relocateNode(head));
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
    { "args": { "head": "[5, 7, 3, 10, 6, 8]" }, "expected": "[8, 5, 7, 3, 10, 6]" },
    { "args": { "head": "[5, 7]" }, "expected": "[7, 5]" },
    { "args": { "head": "[5]" }, "expected": "[5]" },
    { "args": { "head": "[]" }, "expected": "[]" },
    { "args": { "head": "[1, 2, 3]" }, "expected": "[3, 1, 2]" },
    { "args": { "head": "[1, 2, 3, 4, 5]" }, "expected": "[5, 1, 2, 3, 4]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reorder problem is that the output reuses every input node, only with their `.next` fields rewired. No values are read or compared — the work is purely structural. That puts it squarely in the reorder family even though the rearrangement is small: detach the last node, prepend it to the head. The split-and-merge pipeline applies in its most degenerate form, where the split sub-lists are the singleton `[last_node]` and the prefix `[head, …, second-to-last]`, and the merge step is a one-line splice.

The **pointer placement** follows directly. Walk the input with two cursors: `current` chases the tail; `previous` lags one node behind so that when `current` lands on the last node, `previous` holds the node that should become the new tail. Once the walk ends, `previous.next = null` severs the original tail, then `current.next = head` prepends the severed node to the front. The original head becomes the second node of the output, and `current` (the relocated last node) becomes the new head.

What **breaks if you reach for a naive approach**? Copying every value into an array, popping the last element, prepending it, and rebuilding a fresh linked list works in `O(n)` time but pays `O(n)` extra memory and allocates `n` new nodes — for a problem whose answer requires rewriting exactly two `.next` fields. Walking the list twice (once to find the second-to-last node, once more to splice) is correct but adds an unnecessary second traversal. The two-cursor walk does the job in one pass and `O(1)` space.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Relocate Node |
|---|---|
| **Q1.** Does the problem rearrange the nodes of one input list in place? | **Yes** — the output is the same `n` nodes as the input with the last node now at the front; only two `.next` fields change. |
| **Q2.** Can the target be expressed as classifier + selector? | **Yes** — `f1` walks to the tail and splits off the last node; `f2 = "prepend"` joins the severed node onto the prefix in a single splice. |
| **Q3.** Are the sub-lists bounded in count and walkable in one pass? | **Yes** — exactly two sub-lists (the singleton last node and the prefix); the merge step is one pointer update. |
| **Q4.** Is `O(1)` extra space sufficient? | **Yes** — two cursors (`current`, `previous`) regardless of input size. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the reorder pipeline with a single-node split and a prepend merge.

1. **Short-circuit trivial inputs.** If `head` is `null` or `head.next` is `null`, return `head` unchanged. A list with zero or one node already satisfies the target shape — moving the last node to the front is a no-op.
2. **Walk to the last node with a two-cursor pair.** Start with `current = head` and `previous = null`. Loop while `current.next` is non-`null`; each iteration sets `previous = current`, then advances `current = current.next`. When the loop exits, `current` points at the last node and `previous` at the second-to-last node.
3. **Sever the last node from the prefix.** Set `previous.next = null`. The original list now ends at `previous`, and `current` is a detached singleton.
4. **Prepend the severed node onto the prefix.** Set `current.next = head` (the original head). The relocated last node now points at the original first node, and the prefix dangles behind it.
5. **Return the relocated last node as the new head.** `current` is now the head of the output. The output's length equals the input's length; only two `.next` writes occurred.

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
    def relocate_node(self, head):

        # If the list is empty or contains only one node, no need to
        # modify it
        if not head or not head.next:
            return head

        current = head
        previous = None

        # Traverse the list until the last node is reached
        while current.next is not None:

            # Keep track of the previous node
            previous = current

            # Move to the next node
            current = current.next

        # Disconnect the last node
        previous.next = None

        # Connect the last node to the first node
        current.next = head
        return current

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
print_list(Solution().relocate_node(head))
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
        ListNode relocateNode(ListNode head) {

            // If the list is empty or contains only one node, no need to
            // modify it
            if (head == null || head.next == null) {
                return head;
            }

            ListNode current = head;
            ListNode previous = null;

            // Traverse the list until the last node is reached
            while (current.next != null) {

                // Keep track of the previous node
                previous = current;

                // Move to the next node
                current = current.next;
            }

            // Disconnect the last node
            previous.next = null;

            // Connect the last node to the first node
            current.next = head;
            return current;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        printList(new Solution().relocateNode(head));
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
head = 5 → 7 → 3 → 10 → 6 → 8 → null   (Example 1)

Init: current = head (=5), previous = null.

Iter 1: current.next = 7 (not null) → previous = current (=5); current = current.next (=7).
Iter 2: current.next = 3 (not null) → previous = current (=7); current = current.next (=3).
Iter 3: current.next = 10 (not null) → previous = current (=3); current = current.next (=10).
Iter 4: current.next = 6 (not null) → previous = current (=10); current = current.next (=6).
Iter 5: current.next = 8 (not null) → previous = current (=6); current = current.next (=8).
Iter 6: current.next = null → exit loop.
        current = 8 (the last node), previous = 6 (the second-to-last).

Sever: previous.next = null → list now ends at 6: 5 → 7 → 3 → 10 → 6 → null.
Prepend: current.next = head → 8 → 5 → 7 → 3 → 10 → 6 → null.

Return current = the 8. ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | The two-cursor walk visits every node exactly once. The sever-and-prepend step is two `.next` writes, both `O(1)`. |
| **Space** | `O(1)` | Two local references (`current`, `previous`) regardless of input size. No allocations. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head = null`) | The guard `if not head` short-circuits and returns `null`. No traversal. |
| Single node (`head = [5]`) | The guard `if not head.next` short-circuits and returns the same `head`. The last node is also the first node — moving it is a no-op. |
| Two nodes (`head = [5, 7]`) | Iter 1 sets `previous = 5`, `current = 7`. Loop exits. `previous.next = null` → prefix `[5]`. `current.next = head` → `7 → 5`. Output `[7, 5]`. |
| Three nodes (`head = [1, 2, 3]`) | Walk reaches `current = 3`, `previous = 2`. Sever → `[1, 2]`. Prepend `3` → `3 → 1 → 2`. Output `[3, 1, 2]`. |
| All-equal values (`head = [9, 9, 9, 9]`) | Values are irrelevant; the walk still finds the last node by structural position. Output `[9, 9, 9, 9]` (structurally distinct rewiring, identical print). |
| Long list (`head = [1, 2, 3, 4, 5]`) | Walk reaches `current = 5`, `previous = 4`. Sever → `[1, 2, 3, 4]`. Prepend `5` → `5 → 1 → 2 → 3 → 4`. Output `[5, 1, 2, 3, 4]`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Relocate-node is the reorder pattern in its smallest form — the split sub-lists are a singleton and a prefix, and the merge step is one `.next` rewrite. The two-cursor walk turns "find the second-to-last node" into a single pass.

</details>
