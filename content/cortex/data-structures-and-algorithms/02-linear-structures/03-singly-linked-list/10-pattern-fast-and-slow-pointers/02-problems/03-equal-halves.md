---
title: "Equal Halves"
summary: "Given the head of a singly linked list, write a function that returns true if the sum of the nodes of the first half of the linked list is equal to the sum of the nodes of the second half. Return fals"
prereqs:
  - 10-pattern-fast-and-slow-pointers/01-pattern
difficulty: medium
kind: problem
topics: [fast-and-slow-pointers, singly-linked-list]
---

# Equal halves

## Problem Statement

Given the **head** of a singly linked list, write a function that returns `true` if the sum of the nodes of the first half of the linked list is equal to the sum of the nodes of the second half. Return `false` otherwise.

```d2
direction: right
title: "Odd length [1, 2, 3, 4, 5] — middle (★ 3) belongs to the first half" {shape: text; near: top-center}
h1: First half (3 nodes) {
  direction: right
  o1: "1"
  o2: "2"
  o3: "3 ★" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  o1 -> o2 -> o3
}
h2: Second half (2 nodes) {
  direction: right
  o4: "4"
  o5: "5"
  o4 -> o5
}
h1 -> h2
```

<p align="center"><strong>Odd length — the middle node (3) belongs to the first half. Sums must match across <code>{1,2,3}</code> and <code>{4,5}</code>.</strong></p>

```d2
direction: right
title: "Even length [1, 2, 3, 4] — halves are equal (2 + 2)" {shape: text; near: top-center}
h1: First half (2 nodes) {
  direction: right
  e1: "1"
  e2: "2"
  e1 -> e2
}
h2: Second half (2 nodes) {
  direction: right
  e3: "3"
  e4: "4"
  e3 -> e4
}
h1 -> h2
```

<p align="center"><strong>Even length — the two halves are equal in size. Sums must match across <code>{1,2}</code> and <code>{3,4}</code>.</strong></p>

<p align="center"><strong>Split convention — when the list has odd length, the single middle node belongs to the first half. When even, the two halves are equal.</strong></p>

## Examples

**Example 1:**
```
Input:  head = [1, 9, 2, 8]
Output: true
Explanation: Even length — sum of first half (1 + 9 = 10) equals sum of second half (2 + 8 = 10).
```

**Example 2:**
```
Input:  head = [2, 0]
Output: false
Explanation: Even length — first half (2) ≠ second half (0).
```

**Example 3:**
```
Input:  head = [1, 2, 3]
Output: true
Explanation: Odd length — middle node (2) stays with the first half. First half (1 + 2 = 3) equals second half (3).
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
    def equal_halves(self, head):
        # Your code goes here — 2:1 walk to find boundary, then sum each half.
        pass

def build_list(values):              # [1, 2, 3] → 1 → 2 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

head = build_list(ast.literal_eval(input()))   # the test case's head
result = Solution().equal_halves(head)
print("true" if result else "false")
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
        boolean equalHalves(ListNode head) {
            // Your code goes here — 2:1 walk to find boundary, then sum each half.
            return false;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().equalHalves(head));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 → 2 → 3 → null
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[1, 9, 2, 8]" }
  ],
  "cases": [
    { "args": { "head": "[1, 9, 2, 8]" }, "expected": "true" },
    { "args": { "head": "[2, 0]" }, "expected": "false" },
    { "args": { "head": "[1, 2, 3]" }, "expected": "true" },
    { "args": { "head": "[1, 2, 4]" }, "expected": "false" },
    { "args": { "head": "[5, 5]" }, "expected": "true" },
    { "args": { "head": "[3, 3, 3, 3]" }, "expected": "true" },
    { "args": { "head": "[1, 2, 3, 4, 5, 5]" }, "expected": "false" },
    { "args": { "head": "[0, 0, 0, 0]" }, "expected": "true" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a fast-and-slow problem is that the boundary between the two halves is at a fixed proportional position — the `1/2` point of the list's length. Once the boundary is known, the rest of the work (summing each half and comparing) is independent of the pattern. The pattern's only job is to find where the second half starts; the sums are downstream work.

The **pointer placement** is the standard 2:1 walk. `slow` advances one node per tick, `fast` advances two. When the loop exits, `slow` is at the middle. The branching at the end mirrors the split-at-middle variant: for odd length (`fast` not `null`), the middle node belongs to the first half and the second half starts at `slow.next`; for even length (`fast == null`), the two halves are equal in size and the second half starts at `slow` itself. After the boundary is fixed, two range-sum walks compute `sum_first_half` (from `head` up to but not including `second_half_start`) and `sum_second_half` (from `second_half_start` to `null`), and the answer is whether they are equal.

What **breaks if you reach for a naive approach**? Walking the list once to count `n`, then walking `n / 2` steps to find the boundary, then summing each half, is three passes of `O(n)` work — same asymptotic cost but more redundant traversal. Building an array of all values to index into is `O(n)` time and `O(n)` extra space — wastes memory for a problem that only needs two scalar sums. The fast-and-slow walk plus two range sums hits `O(n)` time and `O(1)` space with two passes total (one for the boundary, then a single pass that sums both halves back-to-back).

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Equal Halves |
|---|---|
| **Q1.** Does the problem ask for a node at a proportional position? | **Yes** — the boundary between the two halves is the `1/2` point; everything after that point is the second half. |
| **Q2.** Can the position be computed in a single forward pass? | **Yes** — the 2:1 walk delivers the boundary in one walk; the sums are independent work that follows. |
| **Q3.** Is the work at each step `O(1)`? | **Yes** — each tick of the boundary walk performs three pointer hops; each tick of the sum walks performs one addition. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — two cursors during the boundary walk plus two integer accumulators for the sums; no auxiliary array. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Find the boundary with the 2:1 walk, then sum each half and compare.

1. **Handle the trivial cases first.** If `head` is `null` or has only one node, the second half is empty (sum 0); the first half's sum is either 0 (empty) or `head.val`, but the conventional answer for these degenerate inputs is `true` — empty halves trivially match.
2. **Initialise the 2:1 walk.** Set `slow = head` and `fast = head`.
3. **Run the 2:1 walk to the middle.** While `fast != null` AND `fast.next != null`, advance `slow = slow.next` and `fast = fast.next.next`. When the loop exits, `slow` is at the middle (second middle on even lengths).
4. **Pick the second-half start based on parity.** If `fast != null` (odd length, guard failed on `fast.next == null`), the middle node belongs to the first half — set `second_half_start = slow.next`. If `fast == null` (even length), `slow` is the start of the second half — set `second_half_start = slow`.
5. **Sum the first half.** Walk from `head` and accumulate `val` into `first_half_sum` until reaching `second_half_start`. The endpoint is exclusive — `second_half_start` is the first node of the second half and must not be added to the first sum.
6. **Sum the second half.** Walk from `second_half_start` to `null`, accumulating `val` into `second_half_sum`. The endpoint is inclusive in the sense that every node from `second_half_start` to the tail is counted.
7. **Compare and return.** Return `first_half_sum == second_half_sum`.

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
    def sum_of_list(self, start, end):
        sum_val = 0
        current = start
        while current != end:
            sum_val += current.val
            current = current.next
        return sum_val

    def equal_halves(self, head):
        if head is None or head.next is None:
            return True

        # Initialize slow pointer
        slow = head

        # Initialize fast pointer
        fast = head

        # Find the midpoint of the list using the slow and fast pointer
        # technique
        while fast is not None and fast.next is not None:

            # Move the slow pointer by one step
            slow = slow.next

            # Move the fast pointer by two steps
            fast = fast.next.next

        # Odd number of nodes, middle node goes to first half
        if fast is not None:
            second_half_start = slow.next

        # Even number of nodes, slow is the start of second half
        else:
            second_half_start = slow

        # Calculate sums of the first half
        first_half_sum = self.sum_of_list(head, second_half_start)

        # Calculate sums of the second half
        second_half_sum = self.sum_of_list(second_half_start, None)

        return first_half_sum == second_half_sum

def build_list(values):              # [1, 2, 3] → 1 → 2 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

head = build_list(ast.literal_eval(input()))   # the test case's head
result = Solution().equal_halves(head)
print("true" if result else "false")
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
        private int sumOfList(ListNode start, ListNode end) {
            int sum = 0;
            ListNode current = start;
            while (current != end) {
                sum += current.val;
                current = current.next;
            }
            return sum;
        }

        public boolean equalHalves(ListNode head) {
            if (head == null || head.next == null) {
                return true;
            }

            // Initialize slow pointer
            ListNode slow = head;

            // Initialize fast pointer
            ListNode fast = head;

            // Find the midpoint of the list using the slow and fast pointer
            // technique
            while (fast != null && fast.next != null) {

                // Move the slow pointer by one step
                slow = slow.next;

                // Move the fast pointer by two steps
                fast = fast.next.next;
            }

            ListNode secondHalfStart;

            // Odd number of nodes, middle node goes to first half
            if (fast != null) {
                secondHalfStart = slow.next;
            }

            // Even number of nodes, slow is the start of second half
            else {
                secondHalfStart = slow;
            }

            // Calculate sums of the first half
            int firstHalfSum = sumOfList(head, secondHalfStart);

            // Calculate sums of the second half
            int secondHalfSum = sumOfList(secondHalfStart, null);

            return firstHalfSum == secondHalfSum;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        System.out.println(new Solution().equalHalves(head));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 → 2 → 3 → null
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
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
head = 1 → 9 → 2 → 8 → null   (Example 1, even length n=4)

Init: slow = 1, fast = 1

Tick 1: guard — fast=1, fast.next=9 → continue
        slow = slow.next      → slow = 9
        fast = fast.next.next → fast = 2

Tick 2: guard — fast=2, fast.next=8 → continue
        slow = slow.next      → slow = 2
        fast = fast.next.next → fast = null

Tick 3: guard fails — fast=null → exit

fast == null → even length branch:
  second_half_start = slow → second_half_start = 2

Sum first half (head=1 up to but not including 2):
  walk 1 → 9 →   sum = 1 + 9 = 10

Sum second half (from 2 to null):
  walk 2 → 8 →   sum = 2 + 8 = 10

Compare: 10 == 10 → true ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | Boundary walk visits `n / 2` nodes via `fast`; the two sum walks together visit every node exactly once. Total: `~1.5 * n` node visits. |
| **Space** | `O(1)` | Two cursors for the walk plus two integer accumulators; no auxiliary array. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | Early return `true` — empty halves trivially match. |
| Single node (`[1]`) | Early return `true` — by the same convention. |
| Two equal nodes (`[5, 5]`) | Even length. Boundary: `slow = 5` (second node). First sum = 5; second sum = 5. Return `true`. |
| Three nodes, sums match (`[1, 2, 3]`) | Odd length. Boundary: `slow = 2`, `fast` not `null`. Second half starts at `slow.next = 3`. First sum = 1 + 2 = 3; second sum = 3. Return `true`. |
| Negative values that cancel (`[5, -5, 10, -10]`) | Even length. First sum = 0; second sum = 0. Return `true` — sums comparison handles negatives transparently. |
| Overflow risk on large lists | Sums are integer-typed; for very long lists with large values the sum may overflow 32-bit signed range. Use 64-bit accumulators in production. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Equal-halves is middle-finding plus two range sums — the 2:1 walk locates the boundary, then a single pass accumulates each half's sum into a scalar. The pattern's contribution is the boundary; the sums are independent work that the pattern enables but does not perform.

</details>
