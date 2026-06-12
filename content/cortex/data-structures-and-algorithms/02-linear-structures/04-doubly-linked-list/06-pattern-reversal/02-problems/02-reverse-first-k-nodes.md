---
title: "Reverse First K Nodes"
summary: "Run the per-node swap loop bounded by a counter, then perform three boundary writes to stitch the reversed prefix back to the unreversed suffix in both directions."
prereqs:
  - 06-pattern-reversal/01-pattern
difficulty: easy
kind: problem
topics: [reversal, doubly-linked-list]
---

# Reverse first K nodes

## Problem Statement

Given the **head** of a doubly linked list and a non-negative integer **k**, write a function to reverse the first `k` nodes of the list and return the head of the resulting list.

You need to reverse the prefix in place.

## Examples

**Example 1:**
```
Input:  head = [5, 7, 3, 10, 3], k = 2
Output: [7, 5, 3, 10, 3]
```

**Example 2:**
```
Input:  head = [1, 2, 3, 4, 5], k = 5
Output: [5, 4, 3, 2, 1]
```

**Example 3:**
```
Input:  head = [1, 2, 3, 4, 5], k = 0
Output: [1, 2, 3, 4, 5]
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "head = [1, 2, 3, 4, 5], k = 3",
  "options": ["[1, 2, 3, 4, 5]", "[3, 2, 1, 4, 5]", "[5, 4, 3, 2, 1]", "[3, 1, 2, 4, 5]"],
  "answer": "[3, 2, 1, 4, 5]"
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
    def reverse_first_k_nodes(self, head, k):
        # Your code goes here — run the full swap loop bounded by count < k;
        # then stitch: head.next = current, current.prev = head, previous.prev = None.
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
print_list(Solution().reverse_first_k_nodes(head, k))
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode prev, next;
        ListNode(int val) { this.val = val; }
    }

    static class Solution {
        ListNode reverseFirstKNodes(ListNode head, int k) {
            // Your code goes here — run the full swap loop bounded by count < k;
            // then stitch: head.next = current, current.prev = head, previous.prev = null.
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int k = Integer.parseInt(sc.nextLine().trim());
        printList(new Solution().reverseFirstKNodes(head, k));
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
    { "args": { "head": "[5, 7, 3, 10, 3]", "k": "2" }, "expected": "[7, 5, 3, 10, 3]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "k": "5" }, "expected": "[5, 4, 3, 2, 1]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "k": "0" }, "expected": "[1, 2, 3, 4, 5]" },
    { "args": { "head": "[]", "k": "3" }, "expected": "[]" },
    { "args": { "head": "[42]", "k": "1" }, "expected": "[42]" },
    { "args": { "head": "[1, 2]", "k": "2" }, "expected": "[2, 1]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "k": "3" }, "expected": "[3, 2, 1, 4, 5]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "k": "7" }, "expected": "[5, 4, 3, 2, 1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** is that the first `k` nodes form a contiguous prefix segment that needs both pointer fields swapped on every node, while the tail (everything from position `k + 1` onward) stays in place. After the swap loop, the original `head` becomes the new tail of the reversed prefix, and the original `k`-th node becomes the new head. The two halves are then stitched together: the new tail (the original head) must point forward to whatever node currently sits at position `k + 1`, and that suffix node must point back at the new tail to keep the backward chain consistent.

The **pointer placement** mirrors the full-list reversal with one extra knob — a counter. `current = head`, `previous = null`, and a `count` variable that starts at zero. The per-node swap loop runs as long as `current` is not `null` AND `count < k`. The natural early-exit when `current` becomes `null` covers the case where `k` is larger than the list's length (just reverse the whole list). When the loop exits, `previous` holds the new head of the reversed prefix, the original `head` is the new tail (now with `next = null` after its own swap), and `current` holds the first un-flipped node — exactly the successor the original head must point to forward.

What **breaks if you reach for a single sweep without saving the original head**? Three things, all on the boundary. First, the forward stitch: `head.next` must be reassigned to `current` (the first un-flipped node); without that, the new tail's `next` is still `null` from its own swap. Second, the backward stitch on the suffix: `current.prev` must be reassigned to `head`, or the suffix's first node still points back into the reversed prefix at a node that is no longer at position `k`. Third, the new head's `prev`: the original `k`-th node's swap left it pointing at whatever was at position `k + 1`, so `previous.prev` must be cleared to `null`. Forget any one and either the forward walk or the backward walk silently corrupts.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse First K Nodes |
|---|---|
| **Q1.** Does the problem ask for reversed order across a contiguous segment? | **Yes** — the first `k` nodes form the segment; the rest of the list is untouched. |
| **Q2.** Are the segment endpoints identifiable? | **Yes** — `start = head`, and the end is reached when the counter reaches `k` (or when `current` becomes `null`, whichever fires first). |
| **Q3.** Is the work strictly structural (only `prev`/`next` pointers change)? | **Yes** — values are never read; only the two pointer fields on each segment node swap, plus three boundary assignments after the loop. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — three references (`current`, `previous`, `next_node`) plus an integer counter, regardless of `n` or `k`. |

</details>
<details>
<summary><h2>Brute Force: Slice and Splice</h2></summary>


Walk the list to collect the first `k` values into an array, reverse the array, and write the reversed values back into the first `k` nodes' `val` fields. The structural chain in both directions is left untouched.

This is correct but costs `O(k)` extra space and conflates value movement with list reversal. The pattern's whole point is that the `prev` and `next` pointers carry the order — swap them and the order reverses for free, with no auxiliary storage.

</details>
<details>
<summary><h2>Key Insight: Same Swap Loop, Add a Counter and Three Boundary Writes</h2></summary>


The per-node swap body is byte-identical to full-list reversal. The only differences are at the boundaries: a `count < k` guard on the loop condition (so the loop stops after `k` swaps instead of running to the end), and three stitching writes after the loop. The forward stitch `head.next = current` reconnects the new tail to the unreversed suffix. The backward stitch `current.prev = head` keeps the suffix's first node's `prev` consistent. The head-`prev` clear `previous.prev = null` makes the new head a proper list head. The original `head` reference is the anchor — it never moves during the loop, so it is still available as the new tail when the stitches are needed.

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the prefix-bounded per-node swap loop, then stitch in three writes.

1. **Handle the no-op guard.** If `k <= 0`, return `head` unchanged. The early return avoids running the stitch lines with a meaningless state.
2. **Initialise the references and counter.** Set `current = head`, `previous = null`, `count = 0`. The original `head` reference is preserved — it will become the new tail of the reversed prefix.
3. **Run the bounded swap loop.** While `current` is not `null` AND `count < k`: snapshot `next_node = current.next`, swap `current.prev` and `current.next`, advance `previous = current` and `current = next_node`, increment `count`. The conjunction in the loop condition means a too-large `k` is handled implicitly — the loop exits when `current` runs off the end, which is the same as reversing the whole list.
4. **Stitch the forward link from the new tail.** If `head` is not `null`, set `head.next = current`. After the loop, `current` is the first un-flipped node (or `null` if `k >= n`), and the original `head` is the reversed prefix's new tail.
5. **Stitch the backward link on the suffix's first node.** If `current` is not `null`, set `current.prev = head`. This keeps the suffix's first node's `prev` pointing at the new tail rather than at the old position-`k` node.
6. **Clear the new head's `prev`.** If `previous` is not `null`, set `previous.prev = null`. The original `k`-th node (now the new head) had its `prev` swapped with its old `next`, so without this write its `prev` would point at the old position-`(k+1)` node.
7. **Return the new head.** `previous` holds the new head of the reversed prefix. Return it.

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
    def reverse_first_k_nodes(self, head, k):

        # if K is less than or equal to 0, return the original head
        if k <= 0:
            return head

        # Initialize pointers current and previous
        current = head
        previous = None
        count = 0

        while current is not None and count < k:

            # Save the address of next node
            next_node = current.next

            # Swap the previous and next nodes pointers of the current
            # node
            current.prev, current.next = current.next, current.prev

            # Move previous to hold current node
            previous = current

            # Move current ahead
            current = next_node

            # Increment count
            count += 1

        # Connect the reversed sublist with the remaining part
        if head is not None:
            head.next = current

        # Update prev of the next node to point back to new tail
        if current is not None:
            current.prev = head

        # Mark the previous pointer of the new head to None
        if previous is not None:
            previous.prev = None

        return previous

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
print_list(Solution().reverse_first_k_nodes(head, k))
```

```java solution
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode prev, next;
        ListNode(int val) { this.val = val; }
    }

    static class Solution {
        ListNode reverseFirstKNodes(ListNode head, int k) {

            // if K is less than or equal to 0, return the original head
            if (k <= 0) {
                return head;
            }

            // Initialize pointers current and previous
            ListNode current = head;
            ListNode previous = null;
            int count = 0;

            while (current != null && count < k) {

                // Save the address of next node
                ListNode next = current.next;

                // Swap the previous and next nodes pointers of the current
                // node
                ListNode temp = current.prev;
                current.prev = current.next;
                current.next = temp;

                // Move previous to hold current node
                previous = current;

                // Move current ahead
                current = next;

                // Increment count
                count++;
            }

            // Connect the reversed sublist with the remaining part
            if (head != null) {
                head.next = current;
            }

            // Update prev of the next node to point back to new tail
            if (current != null) {
                current.prev = head;
            }

            // Mark the previous pointer of the new head to nullptr
            if (previous != null) {
                previous.prev = null;
            }

            return previous;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int k = Integer.parseInt(sc.nextLine().trim());
        printList(new Solution().reverseFirstKNodes(head, k));
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
Initial: 5 ⇄ 7 ⇄ 3 ⇄ 10 ⇄ 3,   current = 5,  previous = null,  count = 0

Step 1 │ count=0 < k=2 │ current=5 │ next_node=7 │ swap 5: prev 7, next null │ previous=5, current=7, count=1
Step 2 │ count=1 < k=2 │ current=7 │ next_node=3 │ swap 7: prev 3, next 5    │ previous=7, current=3, count=2
Loop exits (count == k).

Stitch:
  head=5 (now the last node of the prefix) → head.next = current = node(3)
  current=3 is non-null → current.prev = head = node(5)   (mirror the boundary)
  previous=7 is the new head → previous.prev = null
  return previous=7 (new head)

Result: 7 ⇄ 5 ⇄ 3 ⇄ 10 ⇄ 3  ✓
```

The prefix `[5, 7]` flips to `[7, 5]` while the suffix `[3, 10, 3]` is left completely untouched. Three boundary writes reconnect the two parts in both directions: `head.next` forward into the suffix, the suffix's `prev` back at the old head, and the new head's `prev` cleared to `null`.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(min(n, k))` | The loop runs at most `k` times and at most `n` times — whichever bound hits first. Other work (the guard, the three stitches) is `O(1)`. |
| **Space** | `O(1)` | Three references plus an integer counter, regardless of `n` or `k`. |

### Edge Cases

| Case | What happens |
|---|---|
| `k <= 0` | Early return; original `head` returned unchanged. |
| `head is null` | The loop body never runs; the three stitches' null guards skip every assignment; `previous` is `null`; return `null`. |
| `k == 1` | One swap runs (a no-op rewrite of `head.prev` and `head.next`), then the stitches reverse the no-op back — the list is unchanged. |
| `k == n` | The loop swaps every node and exits on `current is null`. The forward stitch writes `head.next = null` (already true after the last swap); the backward stitch's `current` is `null` so it skips; the head-`prev` clear normalises the final node. Result: the full list is reversed. |
| `k > n` | The loop exits early because `current` becomes `null` before `count` reaches `k`. Same outcome as `k == n`: full reversal. |
| Single-node list, `k = 1` | One iteration runs; the swap is a no-op on a single-node list; the stitches' null guards leave everything alone. Return the single node. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Prefix reversal is full-list reversal plus a counter and three boundary writes. The original `head` reference is the anchor — it is never reassigned during the loop, so it remains available as the new tail when the stitches reconnect the prefix to the unreversed suffix in both directions.

</details>
