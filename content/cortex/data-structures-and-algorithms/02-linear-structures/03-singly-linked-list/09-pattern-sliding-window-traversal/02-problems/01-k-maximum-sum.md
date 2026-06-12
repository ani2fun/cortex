---
title: "K Maximum Sum"
summary: "Given the head of a singly linked list and a positive integer k, write a function to find and return the maximum sum of any contiguous k nodes. If the list contains fewer than k nodes, return -1."
prereqs:
  - 09-pattern-sliding-window-traversal/01-pattern
difficulty: easy
kind: problem
topics: [sliding-window-traversal, singly-linked-list]
---

# K maximum sum

## Problem Statement

Given the **head** of a singly linked list and a positive integer **k**, write a function to find and return the maximum sum of any contiguous k nodes. If the list contains fewer than `k` nodes, return `-1`.

## Examples

**Example 1:**
```
Input:  head = [1, 2, -3, 4, 5], k = 2
Output: 9
```
Among all contiguous pairs of nodes, the largest sum comes from the last two nodes: `4 + 5 = 9`.

**Example 2:**
```
Input:  head = [0, 1, 2], k = 4
Output: -1
```
There are no contiguous runs of length `4` because the list has only three nodes, so the function returns `-1`.

**Example 3:**
```
Input:  head = [-1, -2, -3, -4], k = 2
Output: -3
```
Every window sum is negative; the maximum is the least-negative pair `(-1) + (-2) = -3`.

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- `1 ≤ k ≤ 10⁵`
- Return `-1` if the list has fewer than `k` nodes

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def k_maximum_sum(self, head, k):
        # Your code goes here — slide a k-node window, tracking the running
        # sum with O(1) updates (subtract left, add right). Return -1 if
        # the list has fewer than k nodes.
        pass

def build_list(values):              # [1, 2, 3] → 1 → 2 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

head_vals = ast.literal_eval(input())   # the test case's head
k = int(input())                         # the test case's k
head = build_list(head_vals) if head_vals else None
print(Solution().k_maximum_sum(head, k))
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
        int kMaximumSum(ListNode head, int k) {
            // Your code goes here — slide a k-node window, tracking the running
            // sum with O(1) updates (subtract left, add right). Return -1 if
            // the list has fewer than k nodes.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] headVals = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        ListNode head = buildList(headVals);
        System.out.println(new Solution().kMaximumSum(head, k));
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[1, 2, -3, 4, 5]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "head": "[1, 2, -3, 4, 5]", "k": "2" }, "expected": "9" },
    { "args": { "head": "[0, 1, 2]", "k": "4" }, "expected": "-1" },
    { "args": { "head": "[-1, -2, -3, -4]", "k": "2" }, "expected": "-3" },
    { "args": { "head": "[5]", "k": "1" }, "expected": "5" },
    { "args": { "head": "[5]", "k": "2" }, "expected": "-1" },
    { "args": { "head": "[1, 2]", "k": "2" }, "expected": "3" },
    { "args": { "head": "[10, 1, 1, 10]", "k": "3" }, "expected": "12" },
    { "args": { "head": "[3, 3, 3, 3, 3]", "k": "3" }, "expected": "9" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a sliding-window-traversal problem is that the answer is the maximum over all contiguous `k`-node windows — every candidate window is a `k`-node prefix of the suffix starting at some node. The list exposes only forward links, so re-traversing each window from scratch would require an outer walk over every starting position and an inner walk of `k` nodes from there. The single-pass alternative recognises that consecutive windows differ by exactly one node added on the right and one node dropped on the left.

The **pointer placement** follows directly. Two pointers `start` and `end` mark the inclusive endpoints of the current window. They are initialised with a gap of `k − 1`, so the window covers exactly `k` nodes. As the window slides one step right, the running sum updates in `O(1)`: subtract the value at `start` (the node falling out on the left), add the value at `end` (the node arriving on the right), advance both pointers. The running maximum is updated against the post-slide sum.

What **breaks if you reach for a naive approach**? Recomputing each window sum from scratch by walking `k` nodes for every starting position costs `O(n · k)` time, which collapses to `O(n²)` when `k` is `Θ(n)`. Storing the values into an array and sliding the window in the array costs `O(n)` time but `O(n)` extra space — fine for a small in-memory list, fatal for a streaming source. Only the lockstep two-pointer walk hits `O(n)` time and `O(1)` extra space simultaneously.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for K Maximum Sum |
|---|---|
| **Q1.** Does the problem reference a node at a fixed offset from the tail (or from another moving cursor)? | **Yes** — `end` is held exactly `k − 1` hops ahead of `start` for the entire walk. |
| **Q2.** Can the answer be read off when one pointer reaches the tail? | **Yes** — the maximum window sum is tracked during the walk and is final the moment `end` reaches `null`. |
| **Q3.** Is the work at each tick `O(1)`? | **Yes** — one subtraction, one addition, one comparison, two pointer advances per tick. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — four local variables (`start`, `end`, `current_sum`, `max_sum`) regardless of list length. |

</details>
<details>
<summary><h2>Brute Force: Recompute Every Window</h2></summary>


For each possible starting node, walk `k` steps and add the values to compute the window sum. Keep the largest. This is correct but costs `O(n · k)` time — for `k = n / 2` the work degrades to `O(n²)`. It also discards the obvious observation that two consecutive windows share `k − 1` nodes, so any sum recomputed from scratch repeats `k − 1` additions that the previous iteration already did.

</details>
<details>
<summary><h2>Key Insight: Slide the Window with an Incremental Sum</h2></summary>


Build the first window's sum by walking the first `k` nodes once. After that, sliding the window one step right is `subtract the leftmost value, add the new rightmost value` — an `O(1)` update. The trailing pointer `start` and the leading pointer `end` advance together; the running sum is the invariant they preserve.

</details>
<details>
<summary><h2>Approach</h2></summary>


Maintain two pointers (`start` and `end`) and a running window sum (`current_sum`). Walk the list once.

1. **Handle the trivial inputs.** If `head` is `null` or `k` is non-positive, return `-1` immediately — no valid window exists.
2. **Prime the first window.** Initialise `start = end = head`, `current_sum = 0`, and a counter `count = 0`. Walk `end` forward for `k` steps, adding each value to `current_sum` and incrementing `count`. If `end` becomes `null` before the count reaches `k`, the list is too short — return `-1`.
3. **Initialise the running maximum.** Set `max_sum = current_sum`. The first window is the only candidate so far.
4. **Slide the window.** While `end` is not `null`, update `current_sum` by subtracting `start.val` and adding `end.val` (the new rightmost node). Update `max_sum` if `current_sum` exceeds it. Advance both `start` and `end` by one node.
5. **Return the running maximum.** When `end` becomes `null`, every window has been considered; `max_sum` is the answer.

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
    def k_maximum_sum(self, head, k):

        # Handle edge case: empty list or invalid k
        if head is None or k <= 0:
            return -1

        # Pointer to mark the start of the current window
        start = head

        # Pointer to mark the end of the current window
        end = head

        # Variable to store the sum of the current window
        current_sum = 0

        # Counter to count nodes in the first window
        count = 0

        # Step 1: Calculate the sum of the first window of size k
        while end is not None and count < k:

            # Add the current node's value to the sum
            current_sum += end.val

            # Move the end pointer forward
            end = end.next

            # Increment the node counter
            count += 1

        # If there are fewer than k nodes in the list, return -1
        if count < k:
            return -1

        # Initialize max_sum with the sum of the first window
        max_sum = current_sum

        # Step 2: Slide the window through the rest of the list
        while end is not None:

            # Update the current sum by removing the start node and
            # adding the new end node
            current_sum = current_sum - start.val + end.val

            # Update max_sum if the current window sum is greater
            if current_sum > max_sum:
                max_sum = current_sum

            # Move the start and end pointers forward to slide the window
            start = start.next
            end = end.next

        # Return the maximum sum of any contiguous k nodes
        return max_sum

def build_list(values):              # [1, 2, 3] → 1 → 2 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

head_vals = ast.literal_eval(input())   # the test case's head
k = int(input())                         # the test case's k
head = build_list(head_vals) if head_vals else None
print(Solution().k_maximum_sum(head, k))
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
        public int kMaximumSum(ListNode head, int k) {

            // Handle edge case: empty list or invalid k
            if (head == null || k <= 0) {
                return -1;
            }

            // Pointer to mark the start of the current window
            ListNode start = head;

            // Pointer to mark the end of the current window
            ListNode end = head;

            // Variable to store the sum of the current window
            int sum = 0;

            // Counter to count nodes in the first window
            int count = 0;

            // Step 1: Calculate the sum of the first window of size k
            while (end != null && count < k) {

                // Add the current node's value to the sum
                sum += end.val;

                // Move the end pointer forward
                end = end.next;

                // Increment the node counter
                count++;
            }

            // If there are fewer than k nodes in the list, return -1
            if (count < k) {
                return -1;
            }

            // Initialize maxSum with the sum of the first window
            int maxSum = sum;

            // Step 2: Slide the window through the rest of the list
            while (end != null) {

                // Update the current sum by removing the start node and
                // adding the new end node
                sum = sum - start.val + end.val;

                // Update maxSum if the current window sum is greater
                if (sum > maxSum) {
                    maxSum = sum;
                }

                // Move the start and end pointers forward to slide the
                // window
                start = start.next;
                end = end.next;
            }

            // Return the maximum sum of any contiguous k nodes
            return maxSum;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] headVals = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        ListNode head = buildList(headVals);
        System.out.println(new Solution().kMaximumSum(head, k));
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
head = 1 → 2 → -3 → 4 → 5, k = 2

Init: start = 1, end = 1, current_sum = 0, count = 0

Step 1 — prime the first window (k = 2 iterations):
  Iter 1: end = 1 → add 1   → current_sum = 1, end advances to 2, count = 1
  Iter 2: end = 2 → add 2   → current_sum = 3, end advances to -3, count = 2

count == k → first window primed. start = 1, end = -3, current_sum = 3.

Initialise max_sum = 3.

Step 2 — slide while end is not null:
  Tick 1: current_sum = 3 - 1 + (-3) = -1     (window: 2, -3)
          max_sum = max(3, -1) = 3
          start = 2, end = 4
  Tick 2: current_sum = -1 - 2 + 4 = 1        (window: -3, 4)
          max_sum = max(3, 1) = 3
          start = -3, end = 5
  Tick 3: current_sum = 1 - (-3) + 5 = 9      (window: 4, 5)
          max_sum = max(3, 9) = 9
          start = 4, end = null

end is null → loop ends. Return max_sum = 9. ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | `end` walks from the head to the tail exactly once; `start` walks at the same speed `k − 1` nodes behind. Per-tick work (subtract / add / compare / two advances) is `O(1)`. |
| **Space** | `O(1)` | Four local variables (`start`, `end`, `current_sum`, `max_sum`) regardless of `n` or `k`. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | The first guard returns `-1` immediately. |
| `k <= 0` | The first guard returns `-1` immediately — no valid window. |
| `k > n` (list shorter than `k`) | The priming loop exits with `count < k`; the second guard returns `-1`. |
| `k == n` | The priming loop sums all `n` nodes; the slide loop never runs (`end` is already `null`); return the total. |
| `n == 1`, `k == 1` | Priming sets `current_sum = head.val`; slide loop never runs; return `head.val`. |
| All negative values | Sums are negative; `max_sum` tracks the maximum (least-negative) window correctly because it is initialised from the first window, not from `0`. |
| All equal values | Every window has the same sum; `max_sum` is set on the first iteration and never overwritten. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


`K Maximum Sum` is the find-with-aggregation variant of sliding-window traversal: the gap (`k − 1`) and the lockstep walk are unchanged from the find-only case — only the per-tick work upgrades from "read" to "subtract the left value, add the right value, compare to the running max."

</details>
