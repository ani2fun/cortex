---
title: "Value Partition"
summary: "Stable-partition a singly linked list around a pivot X with the split-then-concatenate variant of the reorder pattern."
prereqs:
  - 13-pattern-reorder/01-pattern
difficulty: medium
kind: problem
topics: [reorder, singly-linked-list]
---

# Value partition

## Problem Statement

Given the **head** of a singly linked list and a value **X**, write a function to partition the list such that all nodes less than X come before nodes greater than or equal to X and return the head of the reordered list. The original relative order of the nodes in each of the two partitions should be preserved.

## Examples

**Example 1:**
```
Input:  head = [1, 4, 3, 2, 5, 2], X = 3
Output: [1, 2, 2, 4, 3, 5]
Explanation: Less-than bucket = [1, 2, 2]; greater-or-equal bucket = [4, 3, 5]. Concatenate to get [1, 2, 2, 4, 3, 5]. Relative order is preserved within each bucket.
```

**Example 2:**
```
Input:  head = [2, 1], X = 2
Output: [1, 2]
Explanation: Less-than bucket = [1]; greater-or-equal bucket = [2]. Concatenate to get [1, 2].
```

**Example 3:**
```
Input:  head = [5, 1, 3, 2, 4], X = 3
Output: [1, 2, 5, 3, 4]
Explanation: Less-than bucket = [1, 2]; greater-or-equal bucket = [5, 3, 4]. Concatenate to get [1, 2, 5, 3, 4].
```

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `-10⁴ ≤ X ≤ 10⁴`
- Reorder **in place** — `O(1)` extra space; node values must not be copied or rewritten
- Relative order within each partition must be preserved (stable partition)

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def value_partition(self, head, X):
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
X = int(input())                               # the test case's X
print_list(Solution().value_partition(head, X))
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
        ListNode valuePartition(ListNode head, int X) {
            // Your code goes here
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int X = Integer.parseInt(sc.nextLine().trim());
        printList(new Solution().valuePartition(head, X));
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[1, 4, 3, 2, 5, 2]" },
    { "id": "X", "label": "X", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "head": "[1, 4, 3, 2, 5, 2]", "X": "3" }, "expected": "[1, 2, 2, 4, 3, 5]" },
    { "args": { "head": "[2, 1]", "X": "2" }, "expected": "[1, 2]" },
    { "args": { "head": "[5, 1, 3, 2, 4]", "X": "3" }, "expected": "[1, 2, 5, 3, 4]" },
    { "args": { "head": "[]", "X": "5" }, "expected": "[]" },
    { "args": { "head": "[1, 2, 2]", "X": "3" }, "expected": "[1, 2, 2]" },
    { "args": { "head": "[3, 3, 3]", "X": "3" }, "expected": "[3, 3, 3]" },
    { "args": { "head": "[1]", "X": "3" }, "expected": "[1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reorder problem is that the output reuses every input node — only `.next` fields change — and the target order is decided by a simple `O(1)` classifier on each node's value. That makes the split-and-merge pipeline a clean fit: route nodes into two buckets by `val < X`, then concatenate. The "preserve relative order within each bucket" requirement is the key constraint, and it's satisfied automatically because each pass walks the input in forward order and appends to the bucket tail.

The **pointer placement** follows directly. Maintain four cursors: `less_head` / `less_tail` grow the bucket of nodes with `val < X`; `greater_head` / `greater_tail` grow the bucket of nodes with `val >= X`. A single `current` walks the input. Each iteration reads `current.val`, evaluates the comparison against `X`, and splices `current` onto the chosen bucket's tail. After the loop, both bucket tails terminate with `null`, and the merge step is `less_tail.next = greater_head` — one splice.

What **breaks if you reach for a naive approach**? Copying every value into two arrays (less-than, greater-or-equal), concatenating, and rebuilding a fresh linked list works in `O(n)` time but pays `O(n)` extra memory and allocates `n` new nodes. Trying to do an in-place "swap nodes when out of order" pass like array quickselect partition is much harder on a singly linked list — there's no `O(1)` way to swap two nodes that aren't adjacent, because every swap requires finding the predecessor of each node. The two-bucket split avoids the swap problem entirely: every node is appended exactly once, and the chain is never partially broken.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Value Partition |
|---|---|
| **Q1.** Does the problem rearrange the nodes of one input list in place? | **Yes** — every input node appears in the output exactly once; only `.next` fields change. |
| **Q2.** Can the target be expressed as classifier + selector? | **Yes** — `f1(node) = node.val < X` routes nodes into less / greater-or-equal buckets in `O(1)` per node; `f2 = concatenate` joins the buckets in `O(1)`. |
| **Q3.** Are the sub-lists bounded in count and walkable in one pass? | **Yes** — exactly two buckets; the merge step is a single `less_tail.next = greater_head` splice. |
| **Q4.** Is `O(1)` extra space sufficient? | **Yes** — two dummy heads plus five cursors (`less_tail`, `greater_tail`, `current`, and the dummy refs) regardless of input size. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the reorder pipeline with `f1 = (val < X)` and `f2 = concatenate`.

1. **Short-circuit trivial inputs.** If `head` is `null` or `head.next` is `null`, return `head` unchanged. A list with zero or one node is already trivially partitioned.
2. **Initialise the two bucket skeletons.** Create `less_head = ListNode(0)` and `less_tail = less_head`; create `greater_head = ListNode(0)` and `greater_tail = greater_head`. The dummies let every splice use the same three-line shape without a special case for the first node in each bucket.
3. **Walk the input with a single cursor.** Set `current = head`. Loop while `current` is non-`null`. Each iteration evaluates `current.val < X`.
4. **Inside the loop, splice into the chosen bucket.** If the value is less than `X`, set `less_tail.next = current` and advance `less_tail = less_tail.next`. Otherwise, set `greater_tail.next = current` and advance `greater_tail = greater_tail.next`. The append-to-tail discipline preserves the input's relative order within each bucket.
5. **Advance the walk.** After the bucket splice, set `current = current.next`. The splice rewrote `tail.next`, not `current.next`, so the next-pointer link to the rest of the input is still intact for the read.
6. **Terminate both buckets.** When the loop exits, set `less_tail.next = null` and `greater_tail.next = null`. Without this, the last node in each bucket would still chain into stale input suffixes.
7. **Concatenate the buckets.** Set `less_tail.next = greater_head.next` (the real head of the greater bucket). Return `less_head.next`.

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
    def value_partition(self, head, X):

        # Return the head if the list is empty or has only one node.
        if head is None or head.next is None:
            return head

        # Create dummy nodes to initialize the heads of two separate
        # lists. List for nodes with values less than X.
        less_head = ListNode(0)
        less_tail = less_head

        # List for nodes with values greater than or equal to X.
        greater_head = ListNode(0)
        greater_tail = greater_head

        # Start traversing the original list from the head.
        current = head

        # Traverse and split nodes based on the value of X.
        while current is not None:

            # If the value of the current node is less than X, it should
            # be appended to the list for nodes < X.
            if current.val < X:

                # Append current node to list for nodes < X.
                less_tail.next = current

                # Move less_tail to the newly added node.
                less_tail = less_tail.next

            # Otherwise, the value of the current node is greater than
            # or equal to X, and it should be appended to the list for
            # nodes >= X.
            else:

                # Append current node to list for nodes >= X.
                greater_tail.next = current

                # Move greater_tail to the newly added node.
                greater_tail = greater_tail.next

            # Proceed to the next node in the original list.
            current = current.next

        # End both lists by setting the next pointers of their tails to
        # None.
        less_tail.next = None
        greater_tail.next = None

        # Append greater_head to the end of less_head.
        less_tail.next = greater_head.next
        return less_head.next

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
X = int(input())                               # the test case's X
print_list(Solution().value_partition(head, X))
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
        ListNode valuePartition(ListNode head, int X) {

            // Return the head if the list is empty or has only one node.
            if (head == null || head.next == null) {
                return head;
            }

            // Create dummy nodes to initialize the heads of two separate
            // lists. List for nodes with values less than X.
            ListNode lessHead = new ListNode(0);
            ListNode lessTail = lessHead;

            // List for nodes with values greater than or equal to X.
            ListNode greaterHead = new ListNode(0);
            ListNode greaterTail = greaterHead;

            // Start traversing the original list from the head.
            ListNode current = head;

            // Traverse and split nodes based on the value of X.
            while (current != null) {

                // If the value of the current node is less than X, it should
                // be appended to the list for nodes < X.
                if (current.val < X) {

                    // Append current node to list for nodes < X.
                    lessTail.next = current;

                    // Move lessTail to the newly added node.
                    lessTail = lessTail.next;
                }

                // Otherwise, the value of the current node is greater than
                // or equal to X, and it should be appended to the list for
                // nodes >= X.
                else {

                    // Append current node to list for nodes >= X.
                    greaterTail.next = current;

                    // Move greaterTail to the newly added node.
                    greaterTail = greaterTail.next;
                }

                // Proceed to the next node in the original list.
                current = current.next;
            }

            // End both lists by setting the next pointers of their tails to
            // null.
            lessTail.next = null;
            greaterTail.next = null;

            // Append greaterHead to the end of lessHead.
            lessTail.next = greaterHead.next;
            return lessHead.next;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int X = Integer.parseInt(sc.nextLine().trim());
        printList(new Solution().valuePartition(head, X));
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
head = 1 → 4 → 3 → 2 → 5 → 2 → null,  X = 3   (Example 1)

Init: less_head = ⊙_l, less_tail = ⊙_l,
      greater_head = ⊙_g, greater_tail = ⊙_g,
      current = 1.

Iter 1: 1 < 3 → less_tail.next = 1.   less: ⊙_l → 1.            current = 4.
Iter 2: 4 < 3 ? no → greater_tail.next = 4. greater: ⊙_g → 4.    current = 3.
Iter 3: 3 < 3 ? no → greater_tail.next = 3. greater: ⊙_g → 4 → 3. current = 2.
Iter 4: 2 < 3 → less_tail.next = 2.   less: ⊙_l → 1 → 2.        current = 5.
Iter 5: 5 < 3 ? no → greater_tail.next = 5. greater: ⊙_g → 4 → 3 → 5. current = 2.
Iter 6: 2 < 3 → less_tail.next = 2.   less: ⊙_l → 1 → 2 → 2.    current = null.

Terminate: less_tail.next = null, greater_tail.next = null.
less_head_real = 1, greater_head_real = 4.

Concatenate: less_tail.next = greater_head_real (=4).
Output: 1 → 2 → 2 → 4 → 3 → 5 → null.

Return less_head.next = the first 1. ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | The split pass visits every node once. The concatenation is `O(1)` — `less_tail` already holds the tail reference. |
| **Space** | `O(1)` | Two dummy nodes plus a handful of local references regardless of input size. The output reuses the input nodes — no fresh allocations beyond the throwaway dummies. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head = null`) | Guard `if head is None` returns `null` immediately. |
| Single node `< X` (`head = [1], X = 3`) | Guard `if head.next is None` returns `head` unchanged. Trivially partitioned. |
| Single node `>= X` (`head = [3], X = 3`) | Same guard returns `head` unchanged. Trivially partitioned. |
| All nodes `< X` (`head = [1, 2, 2], X = 3`) | Less bucket = `[1, 2, 2]`, greater bucket is empty (`greater_head.next = null`). `less_tail.next = null` leaves the less bucket as-is. Output `[1, 2, 2]`. |
| All nodes `>= X` (`head = [3, 3, 3], X = 3`) | Less bucket is empty. `less_head.next = null`, so return `greater_head.next = [3, 3, 3]`. Output `[3, 3, 3]`. |
| Mixed with stability test (`head = [5, 1, 3, 2, 4], X = 3`) | Less = `[1, 2]` (in input order); greater = `[5, 3, 4]` (in input order). Concatenate → `[1, 2, 5, 3, 4]`. The relative order within each bucket matches the input. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Value-partition is the stable-partition variant of reorder — the classifier reads `val < X` and the merge step is plain concatenation. The bucket-append discipline (append to the tail, never the head) is what guarantees the stability the problem requires.

</details>
