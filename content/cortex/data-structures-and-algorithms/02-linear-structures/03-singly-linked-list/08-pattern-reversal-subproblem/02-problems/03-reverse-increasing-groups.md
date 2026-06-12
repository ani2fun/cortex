---
title: "Reverse Increasing Groups"
summary: "Given the head of a singly linked list, write a function to reverse the list in groups of increasing size. The first group has size 1, the next group size 2, then 3, and so on. Return the head of the reversed list."
prereqs:
  - 08-pattern-reversal-subproblem/01-pattern
difficulty: medium
kind: problem
topics: [reversal-subproblem, singly-linked-list]
---

# Reverse increasing groups

## Problem Statement

Given the **head** of a singly linked list, write a function to reverse the list in groups of increasing size. The first group has size `1`, the next group size `2`, then `3`, and so on. Return the head of the reversed list.

If, at the end, the length of the remaining list is less than the required group size, do not reverse that part of the list.

## Examples

**Example 1**
```
Input:  head = [5, 7, 3, 10, 6, 8]
Output: [5, 3, 7, 8, 6, 10]
Explanation: Three chunks of sizes 1, 2, 3 cover the list. Reverse each: (5) stays (5); (7, 3) → (3, 7); (10, 6, 8) → (8, 6, 10). Concatenate to [5, 3, 7, 8, 6, 10].
```

**Example 2**
```
Input:  head = [5, 7, 3, 10, 6]
Output: [5, 3, 7, 10, 6]
Explanation: Two chunks of sizes 1 and 2 reverse to (5) and (3, 7). The remaining two nodes (10, 6) cannot form a chunk of size 3, so they stay untouched.
```

**Example 3**
```
Input:  head = [5]
Output: [5]
Explanation: One chunk of size 1 covers the list; reversing a single node is a no-op.
```

**Example 4**
```
Input:  head = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
Output: [1, 3, 2, 6, 5, 4, 10, 9, 8, 7]
Explanation: Four chunks of sizes 1, 2, 3, 4 partition the list exactly (1 + 2 + 3 + 4 = 10). Each chunk reverses independently.
```

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- Reverse **in place** — `O(1)` extra space
- A trailing fragment shorter than the next group size is left untouched

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def reverse_increasing_groups(self, head):
        # Your code goes here — use a growing group_size counter, reverse
        # each chunk while length >= group_size, stitch seams, advance.
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
print_list(Solution().reverse_increasing_groups(head))
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
        ListNode reverseIncreasingGroups(ListNode head) {
            // Your code goes here — use a growing groupSize counter, reverse
            // each chunk while length >= groupSize, stitch seams, advance.
            return null;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        printList(new Solution().reverseIncreasingGroups(head));
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
    { "args": { "head": "[5, 7, 3, 10, 6, 8]" }, "expected": "[5, 3, 7, 8, 6, 10]" },
    { "args": { "head": "[5, 7, 3, 10, 6]" }, "expected": "[5, 3, 7, 10, 6]" },
    { "args": { "head": "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]" }, "expected": "[1, 3, 2, 6, 5, 4, 10, 9, 8, 7]" },
    { "args": { "head": "[1, 2, 3]" }, "expected": "[1, 3, 2]" },
    { "args": { "head": "[1, 2]" }, "expected": "[1, 2]" },
    { "args": { "head": "[5]" }, "expected": "[5]" },
    { "args": { "head": "[]" }, "expected": "[]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The **structural property** is that the chunk size is not fixed — it grows by one after every iteration: `group_size = 1, 2, 3, 4, …`. The list decomposes into chunks of sizes `1, 2, 3, …` until the remaining length cannot accommodate the next size. Each chunk is its own segment-reversal subproblem, and the chunks do not interact. The difference from reverse-k-segments is purely in the outer driver's stopping rule and counter update — the inner reversal primitive and the seam-stitch logic are unchanged.

The **pointer placement** uses the same four boundaries with one extra piece of state: `length` (the remaining list length, initially `find_length(head)`) and `group_size` (initially `1`). After each chunk reversal, `length -= group_size` and `group_size += 1`. The loop continues while `length >= group_size`, which is the only check that determines whether the next chunk fits. `start`, `end`, `left_bound`, and `reversed_head` behave exactly as in reverse-k-segments — the only call-site change is that `get_node_at_position(start, group_size)` uses the current counter instead of a fixed `k`.

What **breaks if you reach for a recursive solution**? A recursive formulation `reverse_groups(head, size)` could compute "reverse the first `size` nodes, then recurse on the rest with `size + 1`." That works algorithmically but consumes `O(√n)` stack frames (the chunk sizes are `1 + 2 + … + g ≈ g²/2 = n`, so `g ≈ √(2n)`). The iterative form stays `O(1)` space and exposes the four-boundary mechanics directly. The shared `reverse(start, end)` helper resolves the rewrite per chunk in `O(group_size)` without any per-chunk re-traversal from `head`.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>

Reverse-increasing-groups extends the chapter pattern with a growing counter. The diagnostic confirms that the outer-driver change does not break any of the four conditions.

| Check | Answer for Reverse Increasing Groups |
|---|---|
| **Q1.** Can the problem or solution be broken down into smaller subproblems? | **Yes** — the rewrite decomposes into chunks of sizes `1, 2, 3, …` until the remaining length is too short. Each chunk is an independent reversal subproblem. |
| **Q2.** Can any subproblem be solved by reversing a part of the linked list? | **Yes** — each chunk is one call to `reverse(start, end)` where `end` is `start` advanced by `group_size − 1` hops. The chunk of size `1` is a degenerate reversal (no-op), and chunks of size `≥ 2` flip links as usual. |
| **Q3.** Does the algorithm only need to walk each node a constant number of times? | **Yes** — `get_node_at_position` walks `group_size − 1` hops and the inner reversal walks the same chunk once. Summed across all chunks this is still one `O(n)` outer walk. |
| **Q4.** Is each chunk's boundary computable from local state? | **Yes** — `end` is `start` plus the local counter `group_size`; the seam stitch uses `left_bound`. The remaining length and counter are constant-size scalars. |

</details>
<details>
<summary><h2>Approach</h2></summary>

Seven numbered steps. No code; the next section is the implementation.

1. **Guard the trivial cases.** If `head` is `None` or `head.next` is `None`, the list has zero or one node; the first chunk of size `1` is trivially the whole list. Return `head` unchanged.
2. **Precompute the remaining length.** Walk the list once to find `length`. The outer loop will decrement `length` after every chunk to track what is still available.
3. **Initialise the boundary pointers and counter.** Set `start = head`, `left_bound = None`, and `group_size = 1`. The `None` marker triggers the first-chunk seam-stitch path; the `group_size = 1` start means the first chunk is a singleton.
4. **Drive the outer loop while `length >= group_size`.** As soon as the remaining length is shorter than the next chunk's size, the loop ends and any trailing fragment is left untouched.
5. **Reverse the current chunk and stitch the seam.** Let `end = get_node_at_position(start, group_size)`. Call `reverse(start, end)` and capture `reversed_head`. If `left_bound is None`, update `head = reversed_head`; otherwise set `left_bound.next = reversed_head`.
6. **Slide the boundary forward.** Set `left_bound = start` (the old `start` is now the chunk's tail), then `start = left_bound.next` (the next chunk's head, placed there by the seam stitch).
7. **Update the counters for the next chunk.** Decrement `length` by `group_size`, then increment `group_size` by `1`. The next iteration's check `length >= group_size` uses the updated values.

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
    def find_length(self, head):
        length = 0
        while head is not None:
            length += 1
            head = head.next
        return length

    def get_node_at_position(self, head, position):
        current = head
        for _ in range(1, position):
            current = current.next
        return current

    def reverse(self, start, end):
        current = start
        right_bound = end.next
        previous = right_bound

        while current != right_bound:
            next_node = current.next
            current.next = previous
            previous = current
            current = next_node

        return previous

    def reverse_increasing_groups(self, head):

        # If the list is empty or has only one node, no need to
        # reverse segments
        if head is None or head.next is None:
            return head

        # Start of the current segment to be reversed
        start = head

        # Pointer to the last node of the previous segment
        left_bound = None

        # Find the length of the linked list
        length = self.find_length(head)

        # Start with a group size of 1
        group_size = 1

        # Loop through the list to reverse segments of increasing size
        while length >= group_size:

            # Get the end node of the current segment
            end = self.get_node_at_position(start, group_size)

            # Get the head of the reversed segment.
            reversed_head = self.reverse(start, end)

            # Check if there is a previous segment to connect to or
            # if the existing head needs to be updated.
            # If left_bound is None, it means we're at the first segment
            # So, we need to update the head to the reversed_head
            # Return the new head
            if left_bound is None:
                head = reversed_head

            # If there is a left_bound, connect its next to the new
            # reversed_head
            else:
                left_bound.next = reversed_head

            # Update left_bound to the current segment's start (which is
            # now the end after reversal)
            left_bound = start

            # Move to the next segment
            start = left_bound.next

            # Decrement the remaining length by the size of the current
            # group
            length -= group_size

            # Increment group_size for the next segment
            group_size += 1

        # Return the head of the modified list
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

head = build_list(ast.literal_eval(input()))   # the test case's head
print_list(Solution().reverse_increasing_groups(head))
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
        private int findLength(ListNode head) {
            int length = 0;
            while (head != null) {
                length++;
                head = head.next;
            }
            return length;
        }

        private ListNode getNodeAtPosition(ListNode head, int position) {
            ListNode current = head;
            for (int i = 1; i < position; ++i) {
                current = current.next;
            }
            return current;
        }

        private ListNode reverse(ListNode start, ListNode end) {
            ListNode current = start;
            ListNode rightBound = end.next;
            ListNode previous = rightBound;

            while (current != rightBound) {
                ListNode next = current.next;
                current.next = previous;
                previous = current;
                current = next;
            }

            return previous;
        }

        public ListNode reverseIncreasingGroups(ListNode head) {

            // If the list is empty or has only one node, no need to
            // reverse segments
            if (head == null || head.next == null) {
                return head;
            }

            // Start of the current segment to be reversed
            ListNode start = head;

            // Pointer to the last node of the previous segment
            ListNode leftBound = null;

            // Find the length of the linked list
            int length = findLength(head);

            // Start with a group size of 1
            int groupSize = 1;

            // Loop through the list to reverse segments of increasing size
            while (length >= groupSize) {

                // Get the end node of the current segment
                ListNode end = getNodeAtPosition(start, groupSize);

                // Get the head of the reversed segment.
                ListNode reversedHead = reverse(start, end);

                // Check if there is a previous segment to connect to or
                // if the existing head needs to be updated.
                // If leftBound is null, it means we're at the first
                // segment So, we need to update the head to the
                // reversedHead
                if (leftBound == null) {
                    head = reversedHead;
                }

                // If there is a leftBound, connect its next to the new
                // reversedHead
                else {
                    leftBound.next = reversedHead;
                }

                // Update leftBound to the current segment's start (which is
                // now the end after reversal)
                leftBound = start;

                // Move to the next segment
                start = leftBound.next;

                // Decrement the remaining length by the size of the current
                // group
                length -= groupSize;

                // increment groupSize for the next segment
                groupSize++;
            }

            // Return the head of the modified list
            return head;
        }
    }

    public static void main(String[] args) {
        ListNode head = buildList(parseIntArray(new Scanner(System.in).nextLine()));
        printList(new Solution().reverseIncreasingGroups(head));
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

### Dry Run — Example 1

`head = [5, 7, 3, 10, 6, 8]`. Precompute `length = 6`. Initial state: `start = 5`, `left_bound = None`, `group_size = 1`.

**Iteration 1 — chunk `(5)`, `group_size = 1`:** `length = 6 >= 1`.

| step | state |
|---|---|
| `end = get_node_at_position(start, 1)` | `end = 5` (loop body runs zero hops) |
| `reverse(5, 5)` | single-node reversal → no-op; returns `reversed_head = 5` |
| `left_bound is None` → update head | `head = 5` |
| `left_bound = start` | `left_bound = 5` |
| `start = left_bound.next` | `start = 7` |
| `length -= group_size; group_size += 1` | `length = 5`, `group_size = 2` |

List after iteration 1: `5 → 7 → 3 → 10 → 6 → 8` (single-node reversal is a no-op).

**Iteration 2 — chunk `(7, 3)`, `group_size = 2`:** `length = 5 >= 2`.

| step | state |
|---|---|
| `end = get_node_at_position(start, 2)` | `end = 3` |
| `reverse(7, 3)` | links flip → `3 → 7 → 10`; returns `reversed_head = 3` |
| `left_bound is not None` → stitch | `5.next = 3` |
| `left_bound = start` | `left_bound = 7` |
| `start = left_bound.next` | `start = 10` |
| `length -= 2; group_size += 1` | `length = 3`, `group_size = 3` |

List after iteration 2: `5 → 3 → 7 → 10 → 6 → 8`.

**Iteration 3 — chunk `(10, 6, 8)`, `group_size = 3`:** `length = 3 >= 3`.

| step | state |
|---|---|
| `end = get_node_at_position(start, 3)` | `end = 8` |
| `reverse(10, 8)` | links flip → `8 → 6 → 10 → None`; returns `reversed_head = 8` |
| `left_bound is not None` → stitch | `7.next = 8` |
| `left_bound = start` | `left_bound = 10` |
| `start = left_bound.next` | `start = None` |
| `length -= 3; group_size += 1` | `length = 0`, `group_size = 4` |

List after iteration 3: `5 → 3 → 7 → 8 → 6 → 10`.

**Iteration 4 — loop guard:** `length = 0 < group_size = 4` → exit.

**Return:** `head = 5`, traversal yields `[5, 3, 7, 8, 6, 10]` ✓

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | `O(n)` | The chunk sizes sum to at most `n` (the loop stops once the next size exceeds the remainder). `find_length` walks once, and each chunk's `get_node_at_position` + `reverse` together walk that chunk twice. Total work is linear in `n`. |
| **Space** | `O(1)` | Four boundary pointers, the `length` counter, and the `group_size` counter. No auxiliary list or recursion. |

### Edge Cases

| Scenario | Input | Output | Note |
|---|---|---|---|
| Empty list | `[]` | `[]` | First guard returns immediately. |
| Single node | `[5]` | `[5]` | `head.next is None` → guard returns; the first chunk-of-1 is implicit. |
| Two nodes | `[1, 2]` | `[1, 2]` | First chunk of size `1` is a no-op; the second chunk would need size `2` but only `1` node remains — loop ends. |
| Three nodes | `[1, 2, 3]` | `[1, 3, 2]` | Chunk-1 (`1`) is a no-op; chunk-2 reverses `(2, 3)` → `(3, 2)`. |
| Triangular length (`1 + 2 + … + k = n`) | `[1..10]` (n=10, k=4) | `[1, 3, 2, 6, 5, 4, 10, 9, 8, 7]` | Four full chunks of sizes `1, 2, 3, 4` partition the list exactly; no trailing fragment. |
| Trailing fragment shorter than next size | `[5, 7, 3, 10, 6]` | `[5, 3, 7, 10, 6]` | Two chunks (sizes `1, 2`) reverse; the remaining `(10, 6)` is shorter than the next `group_size = 3` and stays untouched. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

Reverse-increasing-groups is reverse-k-segments with a growing counter — the outer driver tracks `(length, group_size)` and the loop exits the moment the remaining length is shorter than the next chunk. The inner `reverse(start, end)` helper and the seam-stitch logic are reused unchanged.

</details>
