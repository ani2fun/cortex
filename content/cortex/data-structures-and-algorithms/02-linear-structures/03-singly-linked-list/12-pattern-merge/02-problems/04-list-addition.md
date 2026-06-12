---
title: "List Addition"
summary: "Given heads of two non-empty singly linked lists headA and headB, representing two non-negative integers where the value in every node represents a single digit. The numbers stored in the lists are in reverse order. Return the head of a new list containing the sum."
prereqs:
  - 12-pattern-merge/01-pattern
difficulty: medium
kind: problem
topics: [merge, singly-linked-list]
---

# List addition

## Problem Statement

Given **heads** of two non-empty singly linked lists **headA** and **headB**, representing two non-negative integers where the value in every node represents a single digit. The numbers stored in the lists are in **reverse order** — that is, the head of each list holds the least-significant digit. Write a function to return the head of a new list that contains the sum of the two given lists.

## Examples

**Example 1:**
```
Input:  headA = [2, 4, 3], headB = [5, 6, 4]
Output: [7, 0, 8]
Explanation: A represents 342 (digits 2, 4, 3 from least to most significant). B represents 465. 342 + 465 = 807, which is [7, 0, 8] in reverse-digit form.
```

**Example 2:**
```
Input:  headA = [9, 8, 7], headB = [4, 3, 7]
Output: [3, 2, 5, 1]
Explanation: 789 + 734 = 1523. The carry from the most-significant column produces a new leading digit, so the output is longer than either input.
```

**Example 3:**
```
Input:  headA = [0], headB = [0]
Output: [0]
Explanation: 0 + 0 = 0. Single-digit inputs, single-digit output.
```

## Constraints

- `1 ≤ len(headA), len(headB) ≤ 10²`
- `0 ≤ node.val ≤ 9`
- Digits are stored in **reverse order** (head = least-significant digit)
- The output must also be in reverse order
- Allocate fresh output nodes — do not reuse input nodes

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def list_addition(self, head_a, head_b):
        # Your code goes here — dummy head, walk both lists in lockstep,
        # sum digits + carry, emit ListNode(sum % 10), update carry.
        # Drain the longer input (still add carry). Append carry node if > 0.
        # Return dummy.next.
        pass

def build_list(values):              # [2, 4, 3] → 2 → 4 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

def print_list(head):                # 2 → 4 → 3 → [2, 4, 3]
    out = []
    while head:
        out.append(head.val)
        head = head.next
    print(out)

head_a = build_list(ast.literal_eval(input()))   # the test case's headA
head_b = build_list(ast.literal_eval(input()))   # the test case's headB
print_list(Solution().list_addition(head_a, head_b))
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
        ListNode listAddition(ListNode headA, ListNode headB) {
            // Your code goes here — dummy head, walk both lists in lockstep,
            // sum digits + carry, emit new ListNode(sum % 10), update carry.
            // Drain the longer input (still add carry). Append carry node if > 0.
            // Return dummy.next.
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode headA = buildList(parseIntArray(sc.nextLine()));
        ListNode headB = buildList(parseIntArray(sc.nextLine()));
        printList(new Solution().listAddition(headA, headB));
    }

    static ListNode buildList(int[] values) {      // {2, 4, 3} → 2 → 4 → 3 → null
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
    }

    static void printList(ListNode head) {         // 2 → 4 → 3 → [2, 4, 3]
        List<Integer> out = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) out.add(n.val);
        System.out.println(out);
    }

    // "[2, 4, 3]" → {2, 4, 3} — reads the test case's values
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
    { "id": "headA", "label": "headA", "type": "int[]", "placeholder": "[2, 4, 3]" },
    { "id": "headB", "label": "headB", "type": "int[]", "placeholder": "[5, 6, 4]" }
  ],
  "cases": [
    { "args": { "headA": "[2, 4, 3]", "headB": "[5, 6, 4]" }, "expected": "[7, 0, 8]" },
    { "args": { "headA": "[9, 8, 7]", "headB": "[4, 3, 7]" }, "expected": "[3, 2, 5, 1]" },
    { "args": { "headA": "[0]", "headB": "[0]" }, "expected": "[0]" },
    { "args": { "headA": "[1]", "headB": "[9]" }, "expected": "[0, 1]" },
    { "args": { "headA": "[9, 9]", "headB": "[1]" }, "expected": "[0, 0, 1]" },
    { "args": { "headA": "[1, 0, 0]", "headB": "[9, 9, 9]" }, "expected": "[0, 0, 0, 1]" },
    { "args": { "headA": "[5]", "headB": "[5]" }, "expected": "[0, 1]" },
    { "args": { "headA": "[1, 2, 3]", "headB": "[4]" }, "expected": "[5, 2, 3]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a merge problem is that the output is built by walking both inputs in lockstep and emitting one output node per step, with the selector deciding what value the new node holds. Unlike alternate fusion or sorted merge, the selector here is *arithmetic* — it sums the two current digits plus a running `carry`, emits a node holding `sum mod 10`, and updates `carry = sum / 10` for the next iteration. Because the digits are stored least-significant-first, the walk naturally aligns columns in the same order a paper-and-pencil addition would: ones column, tens column, hundreds column, and so on.

The **pointer placement** follows directly. Create a `dummy` node and set `tail = dummy`. Initialise `currentA = headA`, `currentB = headB`, and `carry = 0`. The main loop runs while *both* cursors are non-`null` — each tick sums `currentA.val + currentB.val + carry`, allocates a new `ListNode(sum % 10)`, and splices it onto `tail`. When one cursor empties, the drain step continues with whichever input is longer, still adding the running `carry`. Finally, if `carry` is non-zero after both inputs are exhausted, one more new node is appended to hold it.

What **breaks if you reach for a naive approach**? Converting both lists into integers, adding them, and converting back to a linked list works for short numbers but overflows for inputs longer than 18 digits (signed 64-bit limit) — and competitive problems often hand you 100-digit inputs precisely to break this approach. The digit-by-digit merge with a running carry handles arbitrary lengths in `O(max(n, m))` time using only constant per-step state. It also avoids the back-and-forth between linked-list and integer representations, which is itself `O(n + m)` work.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for List Addition |
|---|---|
| **Q1.** Does the problem combine two or more input lists into a single output list? | **Yes** — two digit-lists collapse into one digit-list representing their sum. |
| **Q2.** Can the choice be made by an `O(1)` selector on the current heads? | **Yes** — the selector is `sum = currentA.val + currentB.val + carry`, then emit `ListNode(sum % 10)` and update `carry = sum / 10`. All `O(1)` arithmetic. |
| **Q3.** Are the input nodes rewirable into the output? | **No** — this variant *allocates* fresh output nodes because the output digits do not exist in either input. The merge skeleton still applies; only the splice line changes from `tail.next = winner` to `tail.next = ListNode(sum % 10)`. |
| **Q4.** Is `O(1)` extra space sufficient? | **`O(1)` auxiliary**, `O(max(n, m))` for the output. The locals (`dummy`, `tail`, `currentA`, `currentB`, `carry`, `sum_value`) are constant; only the emitted output nodes scale with input. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the dummy-head splice loop with an arithmetic selector that emits a new node per tick.

1. **Initialise the output skeleton and the carry.** Create a `dummy` node, set `tail = dummy`, and set `carry = 0`.
2. **Initialise the input cursors.** Set `currentA = headA` and `currentB = headB`.
3. **Loop while both cursors are non-`null`.** Each iteration sums the two current digits and the carry: `sum = carry + currentA.val + currentB.val`. Advance both cursors. Update `carry = sum / 10`. Emit a new node: `tail.next = ListNode(sum % 10)`, then `tail = tail.next`.
4. **Drain `A` if it is longer.** While `currentA` is non-`null`, compute `sum = carry + currentA.val`, update `carry = sum / 10`, emit `ListNode(sum % 10)`, advance `tail` and `currentA`. The carry must still flow through any single-input remainder.
5. **Drain `B` if it is longer.** Symmetric — while `currentB` is non-`null`, the same three updates.
6. **Emit one final node if a carry remains.** After both inputs are exhausted, if `carry > 0`, append `ListNode(carry)`. This is how `99 + 1 = 100` gains a third digit.
7. **Return `dummy.next`.** This skips the throwaway dummy and returns the real head of the sum list.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python solution time=O(max(n,m)) space=O(max(n,m))
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def list_addition(self, head_a, head_b):

        # Create a new head for the result list and initialize tail
        # pointer
        dummy = ListNode(0)
        tail = dummy

        current_a = head_a
        current_b = head_b

        # Initialize carry to 0
        carry = 0

        # Traverse both lists while neither list is empty
        while current_a is not None and current_b is not None:

            # Start with the carry value
            sum_value = carry

            # Add the values from both lists
            sum_value += current_a.val + current_b.val

            # Move to the next nodes in both lists
            current_a = current_a.next
            current_b = current_b.next

            # Calculate the new carry value
            carry = sum_value // 10

            # Create a new node with the sum modulo 10
            tail.next = ListNode(sum_value % 10)

            # Move the tail pointer to the newly created node
            tail = tail.next

        # If there are remaining nodes in list A, continue adding them
        while current_a is not None:
            sum_value = carry + current_a.val
            carry = sum_value // 10
            tail.next = ListNode(sum_value % 10)
            tail = tail.next
            current_a = current_a.next

        # If there are remaining nodes in list B, continue adding them
        while current_b is not None:
            sum_value = carry + current_b.val
            carry = sum_value // 10
            tail.next = ListNode(sum_value % 10)
            tail = tail.next
            current_b = current_b.next

        # If there is a remaining carry, create a new node for it
        if carry > 0:
            tail.next = ListNode(carry)

        # Return the head of the resulting list (excluding the dummy
        # node)
        return dummy.next

def build_list(values):              # [2, 4, 3] → 2 → 4 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

def print_list(head):                # 2 → 4 → 3 → [2, 4, 3]
    out = []
    while head:
        out.append(head.val)
        head = head.next
    print(out)

head_a = build_list(ast.literal_eval(input()))   # the test case's headA
head_b = build_list(ast.literal_eval(input()))   # the test case's headB
print_list(Solution().list_addition(head_a, head_b))
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
        public ListNode listAddition(ListNode headA, ListNode headB) {

            // Create a new head for the result list and initialize tail
            // pointer
            ListNode dummy = new ListNode(0);
            ListNode tail = dummy;

            ListNode currentA = headA;
            ListNode currentB = headB;

            // Initialize carry to 0
            int carry = 0;

            // Traverse both lists while neither list is empty
            while (currentA != null && currentB != null) {

                // Start with the carry value
                int sum = carry;

                // Add the values from both lists
                sum += currentA.val + currentB.val;

                // Move to the next nodes in both lists
                currentA = currentA.next;
                currentB = currentB.next;

                // Calculate the new carry value
                carry = sum / 10;

                // Create a new node with the sum modulo 10
                tail.next = new ListNode(sum % 10);

                // Move the tail pointer to the newly created node
                tail = tail.next;
            }

            // If there are remaining nodes in list A, continue adding them
            while (currentA != null) {
                int sum = carry + currentA.val;
                carry = sum / 10;
                tail.next = new ListNode(sum % 10);
                tail = tail.next;
                currentA = currentA.next;
            }

            // If there are remaining nodes in list B, continue adding them
            while (currentB != null) {
                int sum = carry + currentB.val;
                carry = sum / 10;
                tail.next = new ListNode(sum % 10);
                tail = tail.next;
                currentB = currentB.next;
            }

            // If there is a remaining carry, create a new node for it
            if (carry > 0) {
                tail.next = new ListNode(carry);
            }

            // Return the head of the resulting list (excluding the dummy
            // node)
            return dummy.next;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode headA = buildList(parseIntArray(sc.nextLine()));
        ListNode headB = buildList(parseIntArray(sc.nextLine()));
        printList(new Solution().listAddition(headA, headB));
    }

    static ListNode buildList(int[] values) {      // {2, 4, 3} → 2 → 4 → 3 → null
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
    }

    static void printList(ListNode head) {         // 2 → 4 → 3 → [2, 4, 3]
        List<Integer> out = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) out.add(n.val);
        System.out.println(out);
    }

    // "[2, 4, 3]" → {2, 4, 3} — reads the test case's values
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
A = 2 → 4 → 3 → null     (represents 342)
B = 5 → 6 → 4 → null     (represents 465)
Expected: 342 + 465 = 807 → [7, 0, 8]

Init: dummy = ⊙, tail = dummy, currentA = 2, currentB = 5, carry = 0

Iter 1 (ones column):
        sum = 0 + 2 + 5 = 7
        currentA = 4; currentB = 6
        carry = 7 / 10 = 0
        tail.next = ListNode(7 % 10) = ListNode(7)
        tail = the new 7
        output: ⊙ → 7

Iter 2 (tens column):
        sum = 0 + 4 + 6 = 10
        currentA = 3; currentB = 4
        carry = 10 / 10 = 1
        tail.next = ListNode(10 % 10) = ListNode(0)
        tail = the new 0
        output: ⊙ → 7 → 0

Iter 3 (hundreds column):
        sum = 1 + 3 + 4 = 8
        currentA = null; currentB = null
        carry = 8 / 10 = 0
        tail.next = ListNode(8 % 10) = ListNode(8)
        tail = the new 8
        output: ⊙ → 7 → 0 → 8

Iter 4: currentA == null → exit main loop.

Drain A: currentA == null → skip.
Drain B: currentB == null → skip.
Final carry: 0 → no extra node.

Return dummy.next = the 7. Output [7, 0, 8] ✓
```

### Result Size

The output has `max(n, m)` digits if the final carry is zero, otherwise `max(n, m) + 1`. The carry can only ever propagate one extra digit because two single digits plus an incoming carry of at most `1` cannot exceed `19`, which is two digits.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(max(n, m))` | The main loop runs `min(n, m)` times; one drain loop runs `abs(n - m)` times; the carry tail is `O(1)`. Total iterations equal the longer input's length, plus at most one. |
| **Space** | `O(max(n, m))` for the output, `O(1)` auxiliary | The output allocates one fresh node per column (and possibly one more for a final carry). The locals (`dummy`, `tail`, `currentA`, `currentB`, `carry`, `sum_value`) are constant. |

### Edge Cases

| Case | What happens |
|---|---|
| Both inputs are `[0]` | Iter 1: `sum = 0`, emit `ListNode(0)`, carry stays `0`. Drain skips. No tail carry. Output `[0]`. |
| Single-digit overflow (`A = [5]`, `B = [5]`) | Iter 1: `sum = 10`, emit `ListNode(0)`, `carry = 1`. Both drains skip. `carry > 0` → append `ListNode(1)`. Output `[0, 1]` (represents 10). |
| Cascading carry (`A = [9, 9]`, `B = [1]`) | Iter 1: `sum = 0 + 9 + 1 = 10`, emit `0`, `carry = 1`. `currentB = null` → exit main loop. Drain A: `sum = 1 + 9 = 10`, emit `0`, `carry = 1`. Tail carry → emit `1`. Output `[0, 0, 1]` (represents 100). |
| Different-length inputs (`A = [1, 2, 3]`, `B = [4]`) | Iter 1: `sum = 0 + 1 + 4 = 5`, emit `5`, `carry = 0`. `currentB = null`. Drain A: emit `2`, then `3`. Output `[5, 2, 3]` (represents 325). |
| Long carry chain (`A = [1, 0, 0]`, `B = [9, 9, 9]`) | Three main iters: emit `0`, `0`, `0` with running carry `1`. Drains skip. Tail carry → emit `1`. Output `[0, 0, 0, 1]` (represents 1000). |
| Single inputs add to exactly 10 (`A = [1]`, `B = [9]`) | Iter 1: `sum = 10`, emit `0`, `carry = 1`. Drains skip. Tail carry → emit `1`. Output `[0, 1]` (represents 10). |
| One input is `[0]` (`A = [0]`, `B = [1, 2, 3]`) | Iter 1: `sum = 0 + 0 + 1 = 1`, emit `1`, `carry = 0`. Drain B: emit `2`, `3`. Output `[1, 2, 3]` (represents 321). |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


List addition is the merge variant where the selector emits a brand-new node per tick instead of splicing an input node, and the `O(1)` per-step state grows to include a running `carry`. The skeleton is unchanged — dummy head, main loop on both cursors, drain step on the longer input, then one final flourish for the tail carry.

Merge is the dual of split. Where split routed nodes from one list into `k` outputs by a classifier, merge routes nodes from `k` inputs into one output by a selector. The template:

```
dummy = ListNode()
tail  = dummy
while any_input_non_empty():
    winner = select_next(current_heads)   # <-- the only problem-specific line
    tail.next = winner
    tail      = winner
    advance_head_of(winner)
drain_any_remaining_input(tail)
return dummy.next
```

Four insights worth burning in:

| Insight | Why it matters |
|---|---|
| Dummy head + tail pointer | Same trick as the split pattern. No "first node" special case; every iteration is a uniform three-line splice. |
| The selector is the whole problem | `pick smaller head` → sorted merge. `alternate A, B, A, B` → interleave. `pick by digit sum` → list addition. `pick min via heap` → k-way merge. Swap out the selector; skeleton stays. |
| Drain the leftover suffix in O(1) | When one input empties first, the other input's remaining nodes are already correctly linked — just splice the whole suffix in one pointer assignment. Don't loop through it node-by-node. |
| Merge is O(n + m) because every node is visited exactly once | No comparisons are wasted. Every node moves from its input to the output in a single pointer update. Total work is proportional to total size. |

When you next see "merge two sorted", "interleave", "combine k lists", "add as numbers", "zip lists together" — reach for the dummy-head-plus-selector template first. Then just write the one-line selector.

> **Transfer Challenge:** You need to merge **k sorted linked lists** into one sorted output. Naïve pairwise merge is O(nk) where n is total length. Can you do better?
>
> <details><summary><strong>Solution hint</strong></summary>
>
> Replace the simple "pick smaller of two heads" selector with a <strong>min-heap of size k</strong> holding the current head of each input. Each extraction is O(log k); there are n extractions total. O(n log k) overall — a dramatic win for k > 2. This is the linked-list version of <em>k-way merge sort</em>, and it's the standard technique used by database query engines to merge sorted runs from disk.
>
> </details>

</details>
