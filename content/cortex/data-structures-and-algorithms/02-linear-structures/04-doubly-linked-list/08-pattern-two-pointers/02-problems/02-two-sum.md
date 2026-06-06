---
title: "Two Sum"
summary: "Given the head and tail of a doubly linked list sorted in non-decreasing order and an integer target, return *all* unique pairs that sum to the target. Do it without extra space. Inputs contain no dup"
prereqs:
  - 08-pattern-two-pointers/01-pattern
difficulty: easy
---

# Two Sum

## The Problem

Given the **head** and **tail** of a doubly linked list sorted in non-decreasing order and an integer **target**, return *all* unique pairs that sum to the target. Do it without extra space. Inputs contain no duplicates.

```
Input:  head = [1, 2, 3, 4, 5], target = 6
Output: [[1, 5], [2, 4]]

Input:  head = [1, 2, 3, 4, 5], target = 10
Output: []

Input:  head = [1, 2, 3, 4, 5], target = 9
Output: [[4, 5]]
```

---

## Examples

**Example 1**
```
Input:  head = [1, 2, 3, 4, 5], target = 6
Output: [[1, 5], [2, 4]]
Explanation: Two distinct pairs sum to 6 — the outer (1, 5) and the inner (2, 4). The pointers meet at 3 with no further pair to record.
```

**Example 2**
```
Input:  head = [1, 2, 3, 4, 5], target = 10
Output: []
Explanation: The maximum reachable sum is 4 + 5 = 9; no pair can reach 10.
```

**Example 3**
```
Input:  head = [1, 2, 3, 4, 5], target = 9
Output: [[4, 5]]
Explanation: Only the outermost-after-shrink pair (4, 5) sums to 9.
```

**Example 4**
```
Input:  head = [1, 2], target = 3
Output: [[1, 2]]
Explanation: The minimum-length valid case — one pair, one iteration, one match.
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a two-pointer problem is the *sorted* assumption. On a sorted DLL, the smallest unvisited value sits at `left` and the largest sits at `right`. The running sum `left.val + right.val` is a monotonic dial: advancing `left` can only raise it, retreating `right` can only lower it. That monotonicity is exactly the signal the converging-walkers pattern needs to make a single deterministic decision per iteration.

The **pointer placement** is `left = head`, `right = tail`. Per iteration, compute `sum = left.val + right.val` and branch on three cases. If `sum == target`, the pair is valid — record `[left.val, right.val]`, then advance *both* pointers inward (the inputs contain no duplicates, so a value that just participated cannot appear again). If `sum < target`, `left.val` paired against the largest remaining partner still falls short, so it can never reach the target with any smaller partner; advance `left`. If `sum > target`, the symmetric argument retreats `right`. The loop ends when `left.val >= right.val` — the unexplored region has collapsed.

What **breaks if you reach for the naive approach**? The brute-force `O(n²)` pass nests two walks of the list and checks every pair — correct, but quadratic for no good reason. A hash-set lookup would solve `O(n)` time but pays `O(n)` extra space and discards the sorted-order gift the input handed you. The converging two-pointer pass uses the ordering for free and lands at `O(n)` time, `O(1)` extra space, single pass.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Two Sum |
|---|---|
| **Q1.** Are two nodes inspected at the same time, one from each end? | **Yes** — every iteration reads `left.val` and `right.val` to compute the running sum. |
| **Q2.** Does one pointer start near `head` and the other near `tail`? | **Yes** — `left = head` and `right = tail`. |
| **Q3.** Do both pointers move strictly inward? | **Yes** — `left` advances via `.next`, `right` retreats via `.prev`; neither ever reverses. |
| **Q4.** Is the per-step work `O(1)`? | **Yes** — one addition, one comparison, one append at most, then a single pointer step. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the converging two-pointer loop, steering by the running sum.

1. **Handle the trivial guards.** If `head` is `null` or `head.next` is `null` (empty or single-node list), return an empty result — no pair is possible.
2. **Initialise the pointers and result.** Set `left = head`, `right = tail`, and `result = []`.
3. **Loop while the search space is non-empty.** Continue while `left.val < right.val`. This condition is strict — when `left.val == right.val`, the pointers have either met on the same node or crossed past each other.
4. **Compute the running sum.** Let `sum = left.val + right.val`.
5. **Branch on the sum's relation to the target.** If `sum == target`, append `[left.val, right.val]` to `result`, then set `left = left.next` and `right = right.prev`. If `sum < target`, advance `left = left.next`. If `sum > target`, retreat `right = right.prev`.
6. **Return the result.** When the loop exits, `result` holds every distinct pair summing to `target`, listed outermost-first.

</details>
<details>
<summary><h2>What Does "Decisive Direction" Mean?</h2></summary>


The whole reason two-pointers works on a sorted DLL is that **every move has a guaranteed effect on the running sum**:

- `left.val` is the *minimum* of the unexplored region.
- `right.val` is the *maximum* of the unexplored region.
- Advancing `left` (toward the tail) → sum can only **increase**.
- Retreating `right` (toward the head) → sum can only **decrease**.

So if `sum < target`, the only hope is to grow the sum, and the only way to grow it is `left++`. If `sum > target`, mirror: `right--`. No guesswork.

> *Friction prompt — predict before reading on:* if `sum < target`, why can we *throw away* `left.val` entirely (move past it forever) instead of pairing it with smaller `right` values?
>
> Answer: because `right.val` is the *largest* remaining value. If `left.val + (largest)` is already too small, no smaller partner could ever lift the sum to the target. `left.val` is provably useless — discard it.

</details>
<details>
<summary><h2>The Converging Walkers Strategy (Visualised)</h2></summary>


```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#64748b"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
flowchart TB
    A["[1,2,3,4,5] target=6<br/>L=1, R=5 → sum=6 ✓<br/>record [1,5], shrink both"]
    B["[1,2,3,4,5]<br/>L=2, R=4 → sum=6 ✓<br/>record [2,4], shrink both"]
    C["[1,2,3,4,5]<br/>L=3, R=3 → meet<br/>done"]
    A --> B --> C
```

<p align="center"><strong>Two Sum on a sorted DLL — pointers converge, sum drives every decision, no node is ever revisited.</strong></p>

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=linked-list viz-root=head
from typing import Optional, List

class ListNode:
    def __init__(self, val=0, prev=None, nxt=None):
        self.val = val
        self.prev = prev
        self.next = nxt


def from_list(values):
    if not values:
        return None
    head = ListNode(values[0])
    cur = head
    for v in values[1:]:
        node = ListNode(v, prev=cur)
        cur.next = node
        cur = node
    return head


def get_tail(head):
    if head is None:
        return None
    cur = head
    while cur.next is not None:
        cur = cur.next
    return cur


class Solution:
    def two_sum(
        self,
        head: Optional[ListNode],
        tail: Optional[ListNode],
        target: int,
    ) -> List[List[int]]:

        # Check if the list is empty or has only one element
        if not head or not head.next:

            # Return an empty list since there are no pairs to be found
            return []

        # Store the pairs of values that sum up to the target
        result: List[List[int]] = []
        left: Optional[ListNode] = head
        right: Optional[ListNode] = tail

        # Iterate until either left or right becomes None or left's value
        # becomes greater than right's value
        while left and right and left.val < right.val:
            if left.val + right.val == target:

                # If the sum of left and right values is equal to the
                # target. Add the pair to the result list
                result.append([left.val, right.val])

                # Move left to the next node
                left = left.next

                # Move right to the previous node
                right = right.prev

            # If the sum of left and right values is less than the target
            # Move left to the next node
            elif left.val + right.val < target:
                left = left.next

            # If the sum of left and right values is greater than the
            # target. Move right to the previous node
            else:
                right = right.prev

        # Return the list containing pairs of values that sum up to the
        # target
        return result


# Examples from the problem statement
h = from_list([1, 2, 3, 4, 5])
print(Solution().two_sum(h, get_tail(h), 6))   # [[1, 5], [2, 4]]

h = from_list([1, 2, 3, 4, 5])
print(Solution().two_sum(h, get_tail(h), 10))  # []

h = from_list([1, 2, 3, 4, 5])
print(Solution().two_sum(h, get_tail(h), 9))   # [[4, 5]]

# Edge cases
h = from_list([1])
print(Solution().two_sum(h, get_tail(h), 1))   # []

h = from_list([1, 2])
print(Solution().two_sum(h, get_tail(h), 3))   # [[1, 2]]

h = from_list([1, 2])
print(Solution().two_sum(h, get_tail(h), 5))   # []

h = from_list([1, 3, 5, 7, 9])
print(Solution().two_sum(h, get_tail(h), 10))  # [[1, 9], [3, 7]]

h = from_list([2, 4, 6, 8])
print(Solution().two_sum(h, get_tail(h), 10))  # [[2, 8], [4, 6]]
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val;
        ListNode prev;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
    }

    static ListNode fromList(int... values) {
        if (values.length == 0) return null;
        ListNode head = new ListNode(values[0]);
        ListNode cur = head;
        for (int i = 1; i < values.length; i++) {
            ListNode node = new ListNode(values[i]);
            node.prev = cur;
            cur.next = node;
            cur = node;
        }
        return head;
    }

    static ListNode getTail(ListNode head) {
        if (head == null) return null;
        ListNode cur = head;
        while (cur.next != null) cur = cur.next;
        return cur;
    }

    static class Solution {
        public List<List<Integer>> twoSum(
            ListNode head,
            ListNode tail,
            int target
        ) {

            // Check if the list is empty or has only one element
            if (head == null || head.next == null) {

                // Return an empty list since there are no pairs to be found
                return new ArrayList<>();
            }

            // Store the pairs of values that sum up to the target
            List<List<Integer>> result = new ArrayList<>();
            ListNode left = head;
            ListNode right = tail;

            // Iterate until either left or right becomes null or left's
            // value becomes greater than right's value
            while (left != null && right != null && left.val < right.val) {
                if (left.val + right.val == target) {

                    // If the sum of left and right values is equal to the
                    // target Add the pair to the result list
                    List<Integer> pair = new ArrayList<>();
                    pair.add(left.val);
                    pair.add(right.val);
                    result.add(pair);

                    // Move left to the next node
                    left = left.next;

                    // Move right to the previous node
                    right = right.prev;
                }

                // If the sum of left and right values is less than the
                // target Move left to the next node
                else if (left.val + right.val < target) {
                    left = left.next;
                }

                // If the sum of left and right values is greater than
                // the target Move right to the previous node
                else {
                    right = right.prev;
                }
            }

            // Return the list containing pairs of values that sum up to the
            // target
            return result;
        }
    }

    public static void main(String[] args) {
        ListNode h;

        // Examples from the problem statement
        h = fromList(1, 2, 3, 4, 5);
        System.out.println(new Solution().twoSum(h, getTail(h), 6));   // [[1, 5], [2, 4]]

        h = fromList(1, 2, 3, 4, 5);
        System.out.println(new Solution().twoSum(h, getTail(h), 10));  // []

        h = fromList(1, 2, 3, 4, 5);
        System.out.println(new Solution().twoSum(h, getTail(h), 9));   // [[4, 5]]

        // Edge cases
        h = fromList(1);
        System.out.println(new Solution().twoSum(h, getTail(h), 1));   // []

        h = fromList(1, 2);
        System.out.println(new Solution().twoSum(h, getTail(h), 3));   // [[1, 2]]

        h = fromList(1, 2);
        System.out.println(new Solution().twoSum(h, getTail(h), 5));   // []

        h = fromList(1, 3, 5, 7, 9);
        System.out.println(new Solution().twoSum(h, getTail(h), 10));  // [[1, 9], [3, 7]]

        h = fromList(2, 4, 6, 8);
        System.out.println(new Solution().twoSum(h, getTail(h), 10));  // [[2, 8], [4, 6]]
    }
}
```


<details>
<summary><strong>Trace — head = [1, 2, 3, 4, 5], target = 6</strong></summary>

```
arr = [1, 2, 3, 4, 5] (already sorted), target = 6

Step 1 │ left=0 (arr[left]=1), right=4 (arr[right]=5)
        │ sum = 1 + 5 = 6 == target → return [arr[left], arr[right]]
Result: [1, 5] ✓  (returns on the first matching pair — no further scanning)
```

</details>

### Complexity Analysis

| Measure | Value | Reason |
|---|---|---|
| Time  | **O(N log N)** | `arr.sort()` dominates; the converging two-pointer scan that follows is O(N). |
| Space | **O(1)** auxiliary | Beyond the sort, only two index variables and a `sum` scalar. |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty / single element | `arr = []` or `[7]` | `[]` | `left < right` is false immediately — no pair possible. |
| No valid pair | `[1,2,3,4,5], target=10` | `[]` | Loop exits when the indices meet (`left < right` fails). |
| All values smaller than target | `[1,2,3], target=100` | `[]` | `sum < target` always — `left` advances until it meets `right`. |

The sorted, no-duplicate Two Sum is clean. But what if the list contains repeats? That's where the same algorithm sprouts an awkward little subroutine.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Two Sum on a sorted DLL is the canonical pair-search variant — the *direction of motion* on every iteration is decided by `sum vs target`, not by node identity. Sorting is what makes the move-decision deterministic; without it, the running sum is no longer a monotonic dial.

</details>