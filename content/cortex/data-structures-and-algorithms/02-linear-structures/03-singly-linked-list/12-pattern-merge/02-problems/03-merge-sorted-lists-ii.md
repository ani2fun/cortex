---
title: "Merge Sorted Lists II"
summary: "Given the heads of two sorted linked lists, headA and headB, write a function to merge the two lists into one sorted list by splicing together the nodes of each list in descending order and return the head of the merged list."
prereqs:
  - 12-pattern-merge/01-pattern
difficulty: medium
kind: problem
topics: [merge, singly-linked-list]
---

# Merge sorted lists II

## Problem Statement

Given the heads of two sorted linked lists, **headA** and **headB**, write a function to merge the two lists into one sorted list by splicing together the nodes of each list in **descending order** and return the head of the merged linked list.

## Examples

**Example 1:**
```
Input:  headA = [1, 2, 4], headB = [1, 3, 4]
Output: [4, 4, 3, 2, 1, 1]
Explanation: Both inputs are ascending. Reverse both, then merge picking the larger head each tick to produce descending output.
```

**Example 2:**
```
Input:  headA = [1, 2, 3, 8, 9], headB = [6, 7]
Output: [9, 8, 7, 6, 3, 2, 1]
Explanation: Reversed inputs are A'=[9,8,3,2,1] and B'=[7,6]. The descending merge interleaves them.
```

**Example 3:**
```
Input:  headA = [1, 3, 5, 6, 7], headB = [2, 4]
Output: [7, 6, 5, 4, 3, 2, 1]
Explanation: Reversed inputs are A'=[7,6,5,3,1] and B'=[4,2]. Descending merge produces [7,6,5,4,3,2,1].
```

## Constraints

- `0 ≤ len(headA), len(headB) ≤ 10⁴`
- `-10⁴ ≤ node.val ≤ 10⁴`
- Both inputs are sorted in **ascending** order
- The output must be in **descending** order
- Merge **in place** — `O(1)` extra space; only `next` pointers change

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def merge_sorted_lists_ii(self, head_a, head_b):
        # Your code goes here — reverse both inputs first, then merge with
        # >= selector (larger head wins each tick). Return dummy.next.
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
print_list(Solution().merge_sorted_lists_ii(head_a, head_b))
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
        ListNode mergeSortedListsII(ListNode headA, ListNode headB) {
            // Your code goes here — reverse both inputs first, then merge with
            // >= selector (larger head wins each tick). Return dummy.next.
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode headA = buildList(parseIntArray(sc.nextLine()));
        ListNode headB = buildList(parseIntArray(sc.nextLine()));
        printList(new Solution().mergeSortedListsII(headA, headB));
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
    { "id": "headA", "label": "headA", "type": "int[]", "placeholder": "[1, 2, 4]" },
    { "id": "headB", "label": "headB", "type": "int[]", "placeholder": "[1, 3, 4]" }
  ],
  "cases": [
    { "args": { "headA": "[1, 2, 4]", "headB": "[1, 3, 4]" }, "expected": "[4, 4, 3, 2, 1, 1]" },
    { "args": { "headA": "[1, 2, 3, 8, 9]", "headB": "[6, 7]" }, "expected": "[9, 8, 7, 6, 3, 2, 1]" },
    { "args": { "headA": "[1, 3, 5, 6, 7]", "headB": "[2, 4]" }, "expected": "[7, 6, 5, 4, 3, 2, 1]" },
    { "args": { "headA": "[]", "headB": "[1, 2]" }, "expected": "[2, 1]" },
    { "args": { "headA": "[1, 2]", "headB": "[]" }, "expected": "[2, 1]" },
    { "args": { "headA": "[1]", "headB": "[1]" }, "expected": "[1, 1]" },
    { "args": { "headA": "[5]", "headB": "[1, 2, 3]" }, "expected": "[5, 3, 2, 1]" },
    { "args": { "headA": "[1]", "headB": "[5, 6]" }, "expected": "[6, 5, 1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a merge problem is the same as ascending sorted merge — two sorted inputs collapse into one sorted output, with each output node chosen by an `O(1)` selector on the current heads. The twist is that the *direction* is flipped: the output must be descending, and the inputs are given in ascending order. Two equivalent solutions exist, and both reuse the merge skeleton. One is to compose two patterns — reverse both inputs first (so their cursors expose the largest unmerged value), then run an ascending-style merge with `>=` instead of `<=`. The other is to keep the inputs ascending, merge ascending, then reverse the result. The first is what this problem demonstrates because it shows how merge composes cleanly with reversal.

The **pointer placement** follows directly. First, run the reversal pattern on `headA` and `headB` separately — each is now a descending chain rooted at the original tail. Then create a `dummy`, set `tail = dummy`, `currentA = reversed_a_head`, `currentB = reversed_b_head`. The splice loop compares the two cursors with `>=` (instead of `<=` for ascending merge): whichever head holds the larger value wins, and ties go to `A` by stability. The drain step is identical to ascending merge — when one input empties, the other's suffix is attached in one splice.

What **breaks if you reach for a naive approach**? Concatenating both lists, copying values into an array, sorting descending, and rebuilding a fresh chain works but costs `O((n + m) log(n + m))` time and `O(n + m)` extra space — the sort throws away the sortedness of the inputs. Alternatively, doing an ascending merge first and then reversing the merged list works in `O(n + m)` time and `O(1)` space, but composes two passes over the same nodes. The reverse-then-merge approach is cleaner because each composed pattern operates on a smaller list (the inputs), and the merge runs only once over the final output.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Merge Sorted Lists II |
|---|---|
| **Q1.** Does the problem combine two or more input lists into a single output list? | **Yes** — two sorted inputs collapse into one sorted (descending) output. |
| **Q2.** Can the choice be made by an `O(1)` selector on the current heads? | **Yes** — after reversing the inputs, `currentA.val >= currentB.val` decides the winner in `O(1)`; reversed-sorted inputs guarantee the largest unmerged value is at a cursor. |
| **Q3.** Are the input nodes rewirable into the output? | **Yes** — both reversal and merge operate in place by rewriting `.next` pointers; no node is allocated. |
| **Q4.** Is `O(1)` extra space sufficient? | **Yes** — reversal uses three locals (`current`, `previous`, `next_node`); merge uses four. No auxiliary data structures across either pass. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Compose two patterns: reversal first, then descending merge.

1. **Reverse `headA` in place** using the reversal pattern (iterative three-pointer walk). The reversed list's head is the original tail of `A`; values now read from largest to smallest.
2. **Reverse `headB` in place** the same way. The two reversed lists are now both sorted descending.
3. **Initialise the merge skeleton.** Create a `dummy` node, set `tail = dummy`, `currentA = reversed_a_head`, `currentB = reversed_b_head`.
4. **Loop while both cursors are non-`null`.** This is the same loop guard as ascending merge.
5. **Inside the loop, pick the larger head.** If `currentA.val >= currentB.val`, the winner is `currentA`: set `tail.next = currentA`, then `currentA = currentA.next`. Otherwise the winner is `currentB`: `tail.next = currentB`, then `currentB = currentB.next`. The `>=` (not `>`) keeps the merge stable on ties — ties go to `A`.
6. **Advance `tail`.** After every splice, `tail = tail.next`.
7. **Drain the non-empty input.** Identical to ascending merge — one splice attaches the entire remaining suffix.
8. **Return `dummy.next`.** The merged list is now in descending order.

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
    def reverse(self, head):
        current = head
        previous = None

        while current is not None:
            next_node = current.next
            current.next = previous
            previous = current
            current = next_node

        return previous

    def merge_sorted_lists(self, head_a, head_b):

        # Create a dummy node and initialize the tail pointer
        dummy = ListNode(0)
        tail = dummy

        current_a = head_a
        current_b = head_b

        # Traverse both lists until one of them becomes empty
        while current_a is not None and current_b is not None:

            # If the value of the current node in list A is greater than
            # or equal to the value of the current node in list B,
            # append the current node from list A to the merged list
            if current_a.val >= current_b.val:
                tail.next = current_a
                current_a = current_a.next

            # Otherwise, append the current node from list B to the
            # merged
            else:
                tail.next = current_b
                current_b = current_b.next

            # Move the tail pointer forward
            tail = tail.next

        # Append the remaining nodes from the non-empty list to the
        # merged list
        if current_a is not None:
            tail.next = current_a

        # Else if there are any remaining nodes in current_b, attach them
        # to the merged list
        elif current_b is not None:
            tail.next = current_b

        # Return the merged list (excluding the dummy node)
        return dummy.next

    def merge_sorted_lists_ii(self, head_a, head_b):
        return self.merge_sorted_lists(
            self.reverse(head_a), self.reverse(head_b)
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

head_a = build_list(ast.literal_eval(input()))   # the test case's headA
head_b = build_list(ast.literal_eval(input()))   # the test case's headB
print_list(Solution().merge_sorted_lists_ii(head_a, head_b))
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
        private ListNode reverse(ListNode head) {
            ListNode current = head;
            ListNode previous = null;

            while (current != null) {
                ListNode next = current.next;
                current.next = previous;
                previous = current;
                current = next;
            }

            return previous;
        }

        private ListNode mergeSortedLists(ListNode headA, ListNode headB) {

            // Create a dummy node and initialize the tail pointer
            ListNode dummy = new ListNode(0);
            ListNode tail = dummy;

            ListNode currentA = headA;
            ListNode currentB = headB;

            // Traverse both lists until one of them becomes empty
            while (currentA != null && currentB != null) {

                // If the value of the current node in list A is greater than
                // or equal to the value of the current node in list B,
                // append the current node from list A to the merged list
                if (currentA.val >= currentB.val) {
                    tail.next = currentA;
                    currentA = currentA.next;
                }

                // Otherwise, append the current node from list B to the
                // merged
                else {
                    tail.next = currentB;
                    currentB = currentB.next;
                }

                // Move the tail pointer forward
                tail = tail.next;
            }

            // Append the remaining nodes from the non-empty list to the
            // merged list
            if (currentA != null) {
                tail.next = currentA;
            }

            // Else if there are any remaining nodes in currentB, attach them
            // to the merged list
            else if (currentB != null) {
                tail.next = currentB;
            }

            // Return the merged list (excluding the dummy node)
            return dummy.next;
        }

        public ListNode mergeSortedListsII(ListNode headA, ListNode headB) {
            return mergeSortedLists(reverse(headA), reverse(headB));
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode headA = buildList(parseIntArray(sc.nextLine()));
        ListNode headB = buildList(parseIntArray(sc.nextLine()));
        printList(new Solution().mergeSortedListsII(headA, headB));
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
A = 1 → 2 → 4 → null   (Example 1)
B = 1 → 3 → 4 → null

Pass 1: reverse(A) → A' = 4 → 2 → 1 → null
Pass 2: reverse(B) → B' = 4 → 3 → 1 → null

Init: dummy = ⊙, tail = dummy, currentA = 4 (A'), currentB = 4 (B')

Iter 1: 4 >= 4 → winner = currentA
        tail.next = currentA  → ⊙ → 4(A)
        currentA = 2; tail = 4(A)
Iter 2: 2 >= 4 ? no → winner = currentB
        tail.next = currentB  → 4(A) → 4(B)
        currentB = 3; tail = 4(B)
Iter 3: 2 >= 3 ? no → winner = currentB
        tail.next = currentB  → 4(B) → 3
        currentB = 1(B); tail = 3
Iter 4: 2 >= 1 → winner = currentA
        tail.next = currentA  → 3 → 2
        currentA = 1(A); tail = 2
Iter 5: 1 >= 1 → winner = currentA
        tail.next = currentA  → 2 → 1(A)
        currentA = null; tail = 1(A)
Iter 6: currentA == null → exit loop.

Drain: currentB = 1(B) (non-null) → tail.next = currentB → 1(A) → 1(B) → null.
       Output: ⊙ → 4(A) → 4(B) → 3 → 2 → 1(A) → 1(B) → null.

Return dummy.next = the 4 from A'. ✓
```

### Result Size

The output contains every node from both inputs — `n + m` nodes total. The reversal step preserves all nodes (it only rewrites `.next`); the merge step likewise preserves all nodes.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n + m)` | Reversal is `O(n)` on `A` and `O(m)` on `B`; the merge is `O(n + m)`. Three linear passes total — still linear in input size. |
| **Space** | `O(1)` | Reversal uses three locals (`current`, `previous`, `next_node`) per call; merge uses four. No allocations except the throwaway dummy. The recursion-free reversal keeps the stack depth at `O(1)`. |

### Edge Cases

| Case | What happens |
|---|---|
| Both lists empty (`headA = headB = null`) | Both reversals return `null`; merge loop and both drain checks fail. Return `null`. |
| Only `headA` is `null` | Reversal of `null` returns `null`. Reversal of `B` returns its reverse. Merge loop guard fails. Drain attaches reversed `B`. Output is `B` in descending order — e.g. `[1, 2]` → `[2, 1]`. |
| Only `headB` is `null` | Symmetric — output is `A` in descending order. |
| Single-node inputs with equal values (`A = [1]`, `B = [1]`) | Both reverse to themselves. Iter 1: `1 >= 1` → A wins; `currentA = null`. Drain attaches `currentB`. Output `[1, 1]` — A before B by stability. |
| All A larger than all B (`A = [5]`, `B = [1, 2, 3]`) | Reversed: `A' = [5]`, `B' = [3, 2, 1]`. Iter 1: A wins (5 >= 3); `currentA = null`. Drain attaches `[3, 2, 1]`. Output `[5, 3, 2, 1]`. |
| All A smaller than all B (`A = [1]`, `B = [5, 6]`) | Reversed: `A' = [1]`, `B' = [6, 5]`. Iters 1–2 consume B'. After iter 2 `currentB = null`. Drain attaches `[1]`. Output `[6, 5, 1]`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Merge sorted lists II is a composition example — reverse, then merge with a flipped comparator. The two patterns operate on disjoint passes over the same nodes, so the total cost stays `O(n + m)` time and `O(1)` space. Reversing the inputs is the cleanest way to flip merge's output direction without rewriting the loop body.

</details>
