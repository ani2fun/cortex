---
title: "Alternate Node Fusion"
summary: "Given the heads of two linked lists, headA and headB, write a function to merge the two lists into one by splicing together the alternate nodes of each list and return the head of the merged list."
prereqs:
  - 12-pattern-merge/01-pattern
difficulty: easy
kind: problem
topics: [merge, singly-linked-list]
---

# Alternate node fusion

## Problem Statement

Given the heads of two linked lists, **headA** and **headB**, write a function to merge the two lists into one by splicing together the alternate nodes of each list and return the head of the merged list.

You should take the first node of the first list (with the head as `headA`) as the first node of the result list. If there are no more nodes left in any one of the lists, append the remaining nodes from the other list to the end of the result in the same order as they appear.

## Examples

**Example 1:**
```
Input:  headA = [1, 2, 3], headB = [4, 5, 6]
Output: [1, 4, 2, 5, 3, 6]
Explanation: Both lists have the same length. The output alternates A, B, A, B, A, B starting with A.
```

**Example 2:**
```
Input:  headA = [1, 2, 3, 4, 5], headB = [6, 7]
Output: [1, 6, 2, 7, 3, 4, 5]
Explanation: B runs out after two alternations. The remaining A suffix [3, 4, 5] is appended whole.
```

**Example 3:**
```
Input:  headA = [1], headB = [2, 3, 4]
Output: [1, 2, 3, 4]
Explanation: A runs out after one alternation. The remaining B suffix [3, 4] is appended whole.
```

## Constraints

- `0 ≤ len(headA), len(headB) ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- Merge **in place** — `O(1)` extra space; only `next` pointers change

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def alternate_node_fusion(self, head_a, head_b):
        # Your code goes here — dummy head, boolean selector that flips
        # each tick, drain the non-empty suffix. Return dummy.next.
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

head_a = build_list(ast.literal_eval(input()))   # the test case's headA
head_b = build_list(ast.literal_eval(input()))   # the test case's headB
print_list(Solution().alternate_node_fusion(head_a, head_b))
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
        ListNode alternateNodeFusion(ListNode headA, ListNode headB) {
            // Your code goes here — dummy head, boolean selector that flips
            // each tick, drain the non-empty suffix. Return dummy.next.
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode headA = buildList(parseIntArray(sc.nextLine()));
        ListNode headB = buildList(parseIntArray(sc.nextLine()));
        printList(new Solution().alternateNodeFusion(headA, headB));
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
    { "id": "headA", "label": "headA", "type": "int[]", "placeholder": "[1, 2, 3]" },
    { "id": "headB", "label": "headB", "type": "int[]", "placeholder": "[4, 5, 6]" }
  ],
  "cases": [
    { "args": { "headA": "[1, 2, 3]", "headB": "[4, 5, 6]" }, "expected": "[1, 4, 2, 5, 3, 6]" },
    { "args": { "headA": "[1, 2, 3, 4, 5]", "headB": "[6, 7]" }, "expected": "[1, 6, 2, 7, 3, 4, 5]" },
    { "args": { "headA": "[1]", "headB": "[2, 3, 4]" }, "expected": "[1, 2, 3, 4]" },
    { "args": { "headA": "[]", "headB": "[1, 2]" }, "expected": "[1, 2]" },
    { "args": { "headA": "[1, 2]", "headB": "[]" }, "expected": "[1, 2]" },
    { "args": { "headA": "[1]", "headB": "[2]" }, "expected": "[1, 2]" },
    { "args": { "headA": "[1, 2, 3]", "headB": "[4]" }, "expected": "[1, 4, 2, 3]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a merge problem is that the output combines two input lists into a single chain, with the choice of "who contributes the next node" decided by a deterministic, `O(1)` selector. Here the selector is the simplest possible state machine — a boolean that flips every iteration — but the rest of the merge machinery is identical to sorted merge, list addition, and every other variant in this pattern. A dummy head, a moving `tail` cursor, two input cursors, and a splice on each iteration.

The **pointer placement** follows directly. Create a `dummy` node whose `.next` will become the real head. Initialise `tail = dummy`, `currentA = headA`, `currentB = headB`, and a boolean `mergeFirst = true`. Each iteration reads the boolean, picks `currentA` or `currentB` as the winner, splices it (`tail.next = winner`), advances that input's cursor, advances `tail`, and flips the boolean. The loop continues while both cursors are non-`null` — the moment either runs out, the drain step appends the other's remaining suffix in a single splice.

What **breaks if you reach for a naive approach**? Copying both lists into arrays, interleaving the arrays, and rebuilding a fresh linked list works in `O(n + m)` time but pays `O(n + m)` extra memory for the arrays and allocates `n + m` brand-new nodes. The originals become garbage. Worse, any caller holding a pointer into `headA` or `headB` is now pointing into a stale chain. The merge technique rewires the existing nodes in place — no allocation, no GC churn, no identity loss.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Alternate Node Fusion |
|---|---|
| **Q1.** Does the problem combine two or more input lists into a single output list? | **Yes** — two inputs collapse into one output, with each output node coming from exactly one input. |
| **Q2.** Can the choice be made by an `O(1)` selector on the current heads? | **Yes** — a boolean `mergeFirst` flipping each tick decides the winner in constant time; no scan needed. |
| **Q3.** Are the input nodes rewirable into the output? | **Yes** — `tail.next = winner` splices a single input node onto the output; only `next` fields change. |
| **Q4.** Is `O(1)` extra space sufficient? | **Yes** — `dummy`, `tail`, `currentA`, `currentB`, `mergeFirst` are five locals regardless of input size. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the dummy-head splice loop with a boolean selector that flips each iteration.

1. **Initialise the output skeleton.** Create a `dummy` node and set `tail = dummy`. The dummy lets every iteration use the same three-line splice (`tail.next = winner; advance winner's cursor; tail = winner`) without a special case for the first node.
2. **Initialise the input cursors and the selector state.** Set `currentA = headA`, `currentB = headB`, and `mergeFirst = true`. Starting with `mergeFirst = true` means the output begins with `headA`, as required by the problem.
3. **Loop while both cursors are non-`null`.** The loop body depends on both inputs still having nodes — the moment one empties, the alternation breaks and the drain step takes over.
4. **Inside the loop, splice the chosen cursor onto the output.** If `mergeFirst` is true, the winner is `currentA`: set `tail.next = currentA`, advance `currentA = currentA.next`, then `tail = tail.next`. Otherwise the winner is `currentB`: the same three updates with `currentB` substituted.
5. **Flip the selector.** Set `mergeFirst = !mergeFirst` so the next iteration picks the other input.
6. **Drain the non-empty input.** When the loop exits, at most one cursor is non-`null`. If `currentA` is non-`null`, do `tail.next = currentA` in one splice; else if `currentB` is non-`null`, do `tail.next = currentB`. The suffix is already correctly chained — no per-node loop needed.
7. **Return `dummy.next`.** This skips the throwaway dummy and returns the real head of the merged list.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python solution time=O(n+m) space=O(1)
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def alternate_node_fusion(self, head_a, head_b):

        # Create a new dummy node as the head of the merged list
        dummy = ListNode(0)
        tail = dummy

        current_a = head_a
        current_b = head_b

        mergeFirst = True

        # Merge alternate nodes from both lists
        while current_a is not None and current_b is not None:

            # If mergeFirst is true, attach the current node from
            # currentA to the merged list
            if mergeFirst:

                # Attach the current node from current_a to the merged
                # list
                tail.next = current_a

                # Move current_a to the next node
                current_a = current_a.next

                # Move the tail pointer to the newly attached node
                tail = tail.next

            # Otherwise, attach the current node from currentB to the
            # merged list
            else:

                # Attach the current node from current_b to the merged
                # list
                tail.next = current_b

                # Move current_b to the next node
                current_b = current_b.next

                # Move the tail pointer to the newly attached node
                tail = tail.next

            # Toggle between lists
            mergeFirst = not mergeFirst

        # If there are any remaining nodes in current_a, attach them to
        # the merged list
        if current_a is not None:
            tail.next = current_a

        # else if there are any remaining nodes in current_b, attach them
        # to the merged list
        elif current_b is not None:
            tail.next = current_b

        # Return the merged list starting from the node after the dummy
        # node
        return dummy.next

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

head_a = build_list(ast.literal_eval(input()))   # the test case's headA
head_b = build_list(ast.literal_eval(input()))   # the test case's headB
print_list(Solution().alternate_node_fusion(head_a, head_b))
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
        public ListNode alternateNodeFusion(ListNode headA, ListNode headB) {

            // Create a new dummy node as the head of the merged list
            ListNode dummy = new ListNode(0);
            ListNode tail = dummy;

            ListNode currentA = headA;
            ListNode currentB = headB;

            boolean mergeFirst = true;

            // Merge alternate nodes from both lists
            while (currentA != null && currentB != null) {

                // If mergeFirst is true, attach the current node from
                // currentA to the merged list
                if (mergeFirst) {

                    // Attach the current node from currentA to the merged
                    // list
                    tail.next = currentA;

                    // Move currentA to the next node
                    currentA = currentA.next;

                    // Move the tail pointer to the newly attached node
                    tail = tail.next;
                }

                // Otherwise, attach the current node from currentB to the
                // merged list
                else {

                    // Attach the current node from currentB to the merged
                    // list
                    tail.next = currentB;

                    // Move currentB to the next node
                    currentB = currentB.next;

                    // Move the tail pointer to the newly attached node
                    tail = tail.next;
                }

                // Toggle between lists
                mergeFirst = !mergeFirst;
            }

            // If there are any remaining nodes in currentA, attach them to
            // the merged list
            if (currentA != null) {
                tail.next = currentA;
            }

            // Else if there are any remaining nodes in currentB, attach them
            // to the merged list
            else if (currentB != null) {
                tail.next = currentB;
            }

            // Return the merged list starting from the node after the dummy
            // node
            return dummy.next;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode headA = buildList(parseIntArray(sc.nextLine()));
        ListNode headB = buildList(parseIntArray(sc.nextLine()));
        printList(new Solution().alternateNodeFusion(headA, headB));
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

### Dry Run

```
A = 1 → 2 → 3 → null   (Example 1)
B = 4 → 5 → 6 → null

Init: dummy = ⊙, tail = dummy, currentA = 1, currentB = 4, mergeFirst = true

Iter 1: mergeFirst = true → winner = currentA
        tail.next = currentA  → ⊙ → 1
        currentA = currentA.next → currentA = 2
        tail = tail.next      → tail = 1
        mergeFirst = false
Iter 2: mergeFirst = false → winner = currentB
        tail.next = currentB  → 1 → 4
        currentB = currentB.next → currentB = 5
        tail = tail.next      → tail = 4
        mergeFirst = true
Iter 3: mergeFirst = true → winner = currentA
        tail.next = currentA  → 4 → 2
        currentA = 3
        tail = 2
        mergeFirst = false
Iter 4: mergeFirst = false → winner = currentB
        tail.next = currentB  → 2 → 5
        currentB = 6
        tail = 5
        mergeFirst = true
Iter 5: mergeFirst = true → winner = currentA
        tail.next = currentA  → 5 → 3
        currentA = null
        tail = 3
        mergeFirst = false
Iter 6: currentA == null → exit loop.

Drain: currentB = 6 (non-null) → tail.next = currentB → 3 → 6 → null.
       Output: ⊙ → 1 → 4 → 2 → 5 → 3 → 6 → null.

Return dummy.next = the first 1. ✓
```

### Result Size

The output contains every node from both inputs — `n + m` nodes total, where `n = len(A)` and `m = len(B)`. No nodes are dropped or duplicated; only `.next` pointers are rewired.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n + m)` | Each iteration consumes exactly one input node; the loop runs at most `min(n, m) * 2` times, and the drain splice is `O(1)`. Total work is proportional to total input size. |
| **Space** | `O(1)` | Five local references (`dummy`, `tail`, `currentA`, `currentB`, `mergeFirst`) regardless of input size. The output reuses the input nodes — no new nodes are allocated except the throwaway dummy. |

### Edge Cases

| Case | What happens |
|---|---|
| Both lists empty (`headA = headB = null`) | Loop guard fails at iteration 0; both drain checks fail. Return `dummy.next = null`. |
| Only `headA` is `null` | Loop guard fails immediately. Drain: `currentB` non-`null`, so `tail.next = currentB`. Return `dummy.next = headB`. |
| Only `headB` is `null` | Loop guard fails immediately. Drain: `currentA` non-`null`, so `tail.next = currentA`. Return `dummy.next = headA`. |
| Single-node inputs (`A = [1]`, `B = [2]`) | Iter 1 splices `1` from A, `currentA = null`. Loop exits. Drain attaches `currentB = 2`. Output `[1, 2]`. |
| A much longer than B (`A = [1,2,3,4,5]`, `B = [6,7]`) | Iters 1–4 alternate `A,B,A,B`. After iter 4 `currentB = null`. Drain attaches `currentA = 3`'s suffix `[3,4,5]` in one splice. Output `[1, 6, 2, 7, 3, 4, 5]`. |
| B much longer than A (`A = [1]`, `B = [2,3,4]`) | Iter 1 splices `1`, `currentA = null`. Loop exits. Drain attaches `currentB = 2`'s suffix `[2, 3, 4]` in one splice. Output `[1, 2, 3, 4]`. |
| Equal lengths (`A = [1,2,3]`, `B = [4,5,6]`) | Six iterations alternate perfectly. After iter 5 `currentA = null`, but `currentB = 6` still. Drain attaches `[6]`. Output `[1, 4, 2, 5, 3, 6]`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Alternate node fusion is the minimal merge — a one-bit boolean selector flipping each tick — and it exercises the full merge skeleton (dummy head, splice loop, drain step) without any value comparison. Every other merge variant in this section reuses the same skeleton with a richer selector.

</details>
