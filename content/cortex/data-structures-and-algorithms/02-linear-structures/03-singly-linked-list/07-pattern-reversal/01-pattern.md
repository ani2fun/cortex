---
title: "Pattern: Reversal"
summary: "Walk the list once with three pointers — previous, current, next — flipping each node's next to point backward. In place, O(n) time, O(1) space. The same loop reverses a whole list, a prefix, a suffix, or any inner segment."
prereqs:
  - 02-linear-structures/03-singly-linked-list/01-what-is-a-linked-list
---

# Pattern: Reversal

## Why It Exists

A singly linked list only points *forward* — each node knows its successor and nothing else. So "process this list backward," "reverse the first `k` nodes," or "is this list a palindrome?" all hit the same wall: you can't walk against the arrows.

The obvious fix is to copy every value into an array, reverse that, and rebuild — `O(n)` time but also `O(n)` extra space, and you've thrown away the list structure. The key realization: **you don't need to move the data at all.** The values are fine where they are; it's the `next` pointers that point the wrong way. Flip each `next` to point at the *previous* node instead, and the list is reversed in place — `O(1)` extra space, one pass.

## See It Work

Reverse `5 → 7 → 3 → 10` by flipping pointers, never touching values. Pick a case and **Run** it, then **Visualise** the arrows turning around one by one.

> ▶ Run it, then click **Visualise** — watch each node's arrow flip to point backward as `current` sweeps through the list.

```python run viz=linked-list viz-root=head viz-kind=list-single
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

def build_list(values):              # [5, 7, 3, 10] → 5 → 7 → 3 → 10 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

head = build_list(ast.literal_eval(input()))   # the test case's values

previous = None
current = head
while current is not None:          # walk the list once
    next_node = current.next        # save the forward link before we overwrite it
    current.next = previous         # flip this node's pointer backward
    previous = current              # previous trails forward
    current = next_node             # advance into the rest of the list
head = previous                     # previous is the old tail = the new head

vals = []
node = head
while node:
    vals.append(node.val)
    node = node.next
print(vals)
```

```java run viz=linked-list viz-root=head viz-kind=list-single
import java.util.*;

public class Main {
  static class ListNode {
    int val; ListNode next;
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
  }

  public static void main(String[] args) {
    ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));

    ListNode previous = null;
    ListNode current = head;
    while (current != null) {         // walk the list once
      ListNode next = current.next;   // save the forward link before we overwrite it
      current.next = previous;        // flip this node's pointer backward
      previous = current;             // previous trails forward
      current = next;                 // advance into the rest of the list
    }
    head = previous;                  // previous is the old tail = the new head

    List<Integer> vals = new ArrayList<>();
    for (ListNode node = head; node != null; node = node.next) vals.add(node.val);
    System.out.println(vals);
  }

  static ListNode buildList(int[] values) {      // {5, 7, 3, 10} → 5 → 7 → 3 → 10 → null
    ListNode head = null;
    for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
    return head;
  }

  // "[5, 7, 3, 10]" → {5, 7, 3, 10} — reads the test case's values
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
    { "id": "values", "label": "values", "type": "int[]", "placeholder": "[5, 7, 3, 10]" }
  ],
  "cases": [
    { "args": { "values": "[5, 7, 3, 10]" }, "expected": "[10, 3, 7, 5]" },
    { "args": { "values": "[1, 2, 3]" }, "expected": "[3, 2, 1]" },
    { "args": { "values": "[42]" }, "expected": "[42]" },
    { "args": { "values": "[]" }, "expected": "[]" }
  ]
}
```

## How It Works

You carry **three pointers** through one pass:

- `previous` — the already-reversed part, trailing behind (starts `null`).
- `current` — the node being rewired right now (starts at `head`).
- `next` — a one-step lookahead, saved *before* you overwrite the link.

Each tick does four things in this exact order: **save** `next = current.next`, **flip** `current.next = previous`, then **advance** `previous = current` and `current = next`.

```mermaid
flowchart TB
  INIT["previous = null<br/>current = head"]
  LOOP["while current ≠ null:<br/>1. next = current.next  (save)<br/>2. current.next = previous  (flip)<br/>3. previous = current  (advance)<br/>4. current = next  (advance)"]
  END(["return previous<br/>= the new head"])
  INIT --> LOOP --> END
```

<p align="center"><strong>the three-pointer loop: save the forward link, flip the current node's pointer backward, then slide both trailing pointers forward. When <code>current</code> falls off the end, <code>previous</code> is the new head.</strong></p>

Why must `next` be saved *first*? Because step 2 overwrites `current.next` — the instant you flip it, the only pointer to the rest of the list is gone. Save it, and you can still find your way forward. When `current` runs off the end (`null`), `previous` is sitting on the old tail, which is now the front: **`previous` is the new head.** One pass, no extra structure → **`O(n)` time, `O(1)` space.**

### Key Takeaway

Reverse a singly linked list by walking it once with `previous`/`current`/`next`, flipping each `next` to point backward. Save the lookahead *before* the flip, and return `previous` as the new head — `O(n)` time, `O(1)` space, values never move.

## Trace It

`head = 5 → 7 → 3 → 10`, starting `previous = null`, `current = 5`:

| tick | save `next` | after flip | `previous` | `current` |
|---|---|---|---|---|
| 1 | `7` | `5 → null` | `5` | `7` |
| 2 | `3` | `7 → 5` | `7` | `3` |
| 3 | `10` | `3 → 7` | `3` | `10` |
| 4 | `null` | `10 → 3` | `10` | `null` → stop |

Before you read on: the loop stops when `current` is `null` — so what is `previous` pointing at, and why is *that* the answer?

`previous` is on `10`, the node that used to be the tail. Every node now points at its old predecessor, so following `next` from `10` gives `10 → 3 → 7 → 5 → null` — the reversed list. The old `head` (`5`) is now the tail pointing at `null`, exactly as the first tick set it. `previous` always trails one step behind `current`, so when `current` leaves the list, `previous` is the last real node — the new front.

## Your Turn

Write the reusable in-place reversal — `reverse(head)` returns the new head. Three pointers, one pass:

```python run viz=linked-list viz-root=head viz-kind=list-single
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

def reverse(head):
    # Your code goes here — three pointers: save the lookahead, flip
    # current.next backward, advance both. Return `previous` as the new head.
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

head = build_list(ast.literal_eval(input()))   # the test case's values
print_list(reverse(head))
```

```java run viz=linked-list viz-root=head viz-kind=list-single
import java.util.*;

public class Main {
  static class ListNode {
    int val; ListNode next;
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
  }

  static ListNode reverse(ListNode head) {
    // Your code goes here — three pointers: save the lookahead, flip
    // current.next backward, advance both. Return `previous` as the new head.
    return null;
  }

  public static void main(String[] args) {
    ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
    printList(reverse(head));
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

  // "[1, 2, 3]" → {1, 2, 3} — reads the test case's values
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
    { "id": "values", "label": "values", "type": "int[]", "placeholder": "[1, 2, 3]" }
  ],
  "cases": [
    { "args": { "values": "[1, 2, 3]" }, "expected": "[3, 2, 1]" },
    { "args": { "values": "[5, 7, 3, 10]" }, "expected": "[10, 3, 7, 5]" },
    { "args": { "values": "[42]" }, "expected": "[42]" },
    { "args": { "values": "[]" }, "expected": "[]" },
    { "args": { "values": "[1, 1, 2, 2]" }, "expected": "[2, 2, 1, 1]" }
  ]
}
```

<details>
<summary>Editorial</summary>

The loop is exactly the See-It-Work walk, packaged to return its result: `previous` starts as `null` (the reversed part is empty), and each tick saves the lookahead, flips `current.next` backward, then slides `previous` and `current` one step forward. When `current` falls off the end, `previous` sits on the old tail — the new head. An empty list never enters the loop and correctly returns `null`.

```python solution time=O(n) space=O(1)
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

def reverse(head):
    previous = None
    current = head
    while current is not None:
        next_node = current.next     # save before overwriting
        current.next = previous      # flip
        previous = current           # advance
        current = next_node
    return previous                  # new head

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

head = build_list(ast.literal_eval(input()))   # the test case's values
print_list(reverse(head))
```

```java solution
import java.util.*;

public class Main {
  static class ListNode {
    int val; ListNode next;
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
  }

  static ListNode reverse(ListNode head) {
    ListNode previous = null, current = head;
    while (current != null) {
      ListNode next = current.next;  // save before overwriting
      current.next = previous;       // flip
      previous = current;            // advance
      current = next;
    }
    return previous;                 // new head
  }

  public static void main(String[] args) {
    ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
    printList(reverse(head));
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

  // "[1, 2, 3]" → {1, 2, 3} — reads the test case's values
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

</details>

## Reflect & Connect

Drill the family in **Practice** — [Reverse a List](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-reversal/problems/reverse-a-list), [Reverse First K Nodes](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-reversal/problems/reverse-first-k-nodes), [Reverse Last K Nodes](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-reversal/problems/reverse-last-k-nodes), and [Reverse the Given Segment](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-reversal/problems/reverse-the-given-segment).

The three-pointer flip is one of the highest-leverage list moves — it shows up wherever something must go backward:

- **Whole-list, prefix, suffix, segment** — the *same loop*, just bounded differently. To reverse an inner segment `[i, j]`: walk to the node before `i`, reverse exactly `j − i + 1` nodes, then **stitch** the reversed piece back to the untouched parts on both sides. The pointers before `i` and after `j` never move.
- **Reversal as a sub-step** — palindrome check (reverse the second half, then compare against the first), and reorder-style problems (`L0→Ln→L1→Ln-1→…`) split the list, reverse one half, and weave. That composition is the [next pattern](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-reversal-subproblem/pattern).
- **Iterative beats recursive here** — a recursive reversal is elegant but costs `O(n)` stack space; the three-pointer loop is `O(1)`. On a long list, recursion risks a stack overflow the loop never will.

**Prerequisites:** [What Is a Linked List?](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/what-is-a-linked-list).
**What's next:** reversal as a building block inside larger walks — [Reversal as a Subproblem](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-reversal-subproblem/pattern).

## Recall

> **Mnemonic:** *Three pointers — `previous`, `current`, `next`. Save next, flip current backward, advance both. `previous` ends up as the new head.*

| | |
|---|---|
| Pointers | `previous` (trails, starts `null`), `current` (starts `head`), `next` (lookahead) |
| Each tick | save `next` → flip `current.next = previous` → `previous = current` → `current = next` |
| Returns | `previous` — the old tail, now the head |
| Cost | `O(n)` time, `O(1)` space (no copy, values never move) |

<details>
<summary><strong>Q:</strong> Why save `next` before flipping `current.next`?</summary>

**A:** The flip overwrites `current.next` — without the saved lookahead you'd lose the only pointer to the rest of the list.

</details>
<details>
<summary><strong>Q:</strong> What does `previous` hold when the loop ends?</summary>

**A:** The old tail, which is the new head — `current` has just walked off the end into `null`.

</details>
<details>
<summary><strong>Q:</strong> How do you reverse only an inner segment `[i, j]`?</summary>

**A:** Reverse exactly those nodes, then stitch the reversed piece back to the node before `i` and the node after `j`.

</details>
<details>
<summary><strong>Q:</strong> Why prefer the iterative loop over recursion?</summary>

**A:** Recursion uses `O(n)` stack space (overflow risk on long lists); the loop is `O(1)`.

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, 4th ed., §10.2 — singly linked lists and pointer manipulation.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §1.3 — linked structures; reversing a list as the canonical in-place pointer exercise.
- The three-pointer loop and its `O(n)`/`O(1)` bounds are the standard result; both runnable blocks are verified by running against their test cases.
