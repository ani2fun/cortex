---
title: "Palindrome Number"
summary: "Given the head and tail of a sorted (well — *symmetric*) doubly linked list, return true if the list reads the same forwards and backwards, false otherwise. A palindrome number reads identically left-"
prereqs:
  - 08-pattern-two-pointers/01-pattern
difficulty: easy
---

# Palindrome Number

## The Problem

Given the **head** and **tail** of a sorted (well — *symmetric*) doubly linked list, return `true` if the list reads the same forwards and backwards, `false` otherwise. A palindrome number reads identically left-to-right and right-to-left.

```
Input:  head = [1, 2, 3, 2, 1]
Output: true

Input:  head = [6, 6, 6]
Output: true

Input:  head = [1, 2, 3, 4, 5]
Output: false
```

---

## Examples

**Example 1**
```
Input:  head = [1, 2, 3, 2, 1]
Output: true
Explanation: Reads identically forwards and backwards — the outer 1s match, the inner 2s match, the middle 3 is its own mirror.
```

**Example 2**
```
Input:  head = [6, 6, 6]
Output: true
Explanation: Every value is identical, so every mirror pair trivially matches.
```

**Example 3**
```
Input:  head = [1, 2, 3, 4, 5]
Output: false
Explanation: The outermost pair (1, 5) already disagrees — the algorithm returns false on the very first comparison.
```

**Example 4**
```
Input:  head = [1, 2, 2, 1]
Output: true
Explanation: Even-length palindrome — the pointers never land on the same node; they cross past each other after the inner (2, 2) match passes.
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a two-pointer problem is that a palindrome is defined by *mirror equality* across the centre of the list. Position `i` from the head must equal position `i` from the tail for every valid `i`. A DLL gives both ends in `O(1)` (the caller already passes `head` and `tail`) and a backward step from the tail in `O(1)` via `tail.prev`. The work splits naturally into `n / 2` independent comparisons — exactly the shape a converging two-pointer pass eats for breakfast.

The **pointer placement** is `left = head`, `right = tail`. Each iteration reads the pair `(left.val, right.val)` and decides: if they disagree, the list cannot be a palindrome and we return early; if they match, the inner sub-list `(left.next, right.prev)` must also be a palindrome, so step both pointers inward and continue. The loop terminates when `left == right` (odd-length list, pointers meet on the middle node) or when `left.prev == right` (even-length list, pointers have just crossed). Both termination conditions mean every required pair has been checked.

What **breaks if you reach for the naive approach**? Copying the list into an array and comparing `arr[i]` with `arr[n - 1 - i]` works in `O(n)` time but pays `O(n)` extra space for the copy. Reversing a clone of the list and walking both in parallel hits the same `O(n)` space cost. The two-pointer pass keeps the space at `O(1)` and gains an early-exit on the first mismatch — a list like `[1, 2, 3, 4, 5]` rejects after one comparison, not five.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Palindrome Number |
|---|---|
| **Q1.** Are two nodes inspected at the same time, one from each end? | **Yes** — every iteration reads `left.val` and `right.val` together and checks equality. |
| **Q2.** Does one pointer start near `head` and the other near `tail`? | **Yes** — `left = head` and `right = tail` are the initial positions. |
| **Q3.** Do both pointers move strictly inward? | **Yes** — on a match, `left = left.next` and `right = right.prev`; neither pointer ever reverses. |
| **Q4.** Is the per-step work `O(1)`? | **Yes** — one value comparison and two pointer steps per iteration; no inner scan. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the converging two-pointer loop until the pointers meet or cross.

1. **Handle the trivial guards.** If `head` is `null` or `head == tail` (empty or single-node list), return `true` — both are vacuously palindromic.
2. **Initialise the pointers.** Set `left = head` and `right = tail`. These will walk inward in lockstep.
3. **Loop until the pointers meet or cross.** Continue while `left != right` (odd-length not yet collided) **and** `left.prev != right` (even-length not yet crossed). The pair guard catches both parities.
4. **Compare the current pair.** If `left.val != right.val`, the list cannot be a palindrome — return `false` immediately.
5. **Step both pointers inward.** Set `left = left.next` and `right = right.prev`. The unprocessed span has shrunk by one node on each side.
6. **Return `true` when the loop exits.** Every pair matched, so the list reads the same forwards and backwards.

</details>
<details>
<summary><h2>The Mirror Strategy (Visualised)</h2></summary>


Plant `left` at the start, `right` at the end. At each step, compare the two values; if they ever differ, return `false`. Otherwise step inward and keep going until the pointers meet (odd length) or cross (even length).

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
    A["[1, 2, 3, 2, 1]<br/>L=1, R=1 → match<br/>shrink"]
    B["[1, 2, 3, 2, 1]<br/>L=2, R=2 → match<br/>shrink"]
    C["[1, 2, 3, 2, 1]<br/>L=R=3 → meet<br/>true ✓"]
    A --> B --> C
```

<p align="center"><strong>Palindrome check — mirror comparison from both ends until pointers meet or cross.</strong></p>

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=linked-list viz-root=head
from typing import Optional

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
    def palindrome_number(
        self, head: Optional[ListNode], tail: Optional[ListNode]
    ) -> bool:

        # Empty list or single element is a palindrome
        if not head or head == tail:
            return True

        left = head
        right = tail

        while left and right and left != right and left.prev != right:

            # If values don't match, its not a palindrome
            if left.val != right.val:
                return False

            # Move the left pointer to the right
            left = left.next

            # Move the right pointer to the left
            right = right.prev

        # If all values matched, it's a palindrome
        return True


# Examples from the problem statement
h = from_list([1, 2, 3, 2, 1])
print(Solution().palindrome_number(h, get_tail(h)))   # True

h = from_list([6, 6, 6])
print(Solution().palindrome_number(h, get_tail(h)))   # True

h = from_list([1, 2, 3, 4, 5])
print(Solution().palindrome_number(h, get_tail(h)))   # False

# Edge cases
h = from_list([5])
print(Solution().palindrome_number(h, get_tail(h)))   # True

h = from_list([1, 2, 1])
print(Solution().palindrome_number(h, get_tail(h)))   # True

h = from_list([1, 2])
print(Solution().palindrome_number(h, get_tail(h)))   # False

h = from_list([1, 2, 2, 1])
print(Solution().palindrome_number(h, get_tail(h)))   # True

h = from_list([9, 9, 9, 9])
print(Solution().palindrome_number(h, get_tail(h)))   # True

h = from_list([1, 2, 3])
print(Solution().palindrome_number(h, get_tail(h)))   # False
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
        public boolean palindromeNumber(ListNode head, ListNode tail) {

            // Empty list or single element is a palindrome
            if (head == null || head == tail) {
                return true;
            }

            ListNode left = head;
            ListNode right = tail;

            while (
                left != null &&
                right != null &&
                left != right &&
                left.prev != right
            ) {

                // If values don't match, its not a palindrome
                if (left.val != right.val) {
                    return false;
                }

                // Move the left pointer to the right
                left = left.next;

                // Move the right pointer to the left
                right = right.prev;
            }

            // If all values matched, it's a palindrome
            return true;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        ListNode h;

        h = fromList(1, 2, 3, 2, 1);
        System.out.println(new Solution().palindromeNumber(h, getTail(h)));  // true

        h = fromList(6, 6, 6);
        System.out.println(new Solution().palindromeNumber(h, getTail(h)));  // true

        h = fromList(1, 2, 3, 4, 5);
        System.out.println(new Solution().palindromeNumber(h, getTail(h)));  // false

        // Edge cases
        h = fromList(5);
        System.out.println(new Solution().palindromeNumber(h, getTail(h)));  // true

        h = fromList(1, 2, 1);
        System.out.println(new Solution().palindromeNumber(h, getTail(h)));  // true

        h = fromList(1, 2);
        System.out.println(new Solution().palindromeNumber(h, getTail(h)));  // false

        h = fromList(1, 2, 2, 1);
        System.out.println(new Solution().palindromeNumber(h, getTail(h)));  // true

        h = fromList(9, 9, 9, 9);
        System.out.println(new Solution().palindromeNumber(h, getTail(h)));  // true

        h = fromList(1, 2, 3);
        System.out.println(new Solution().palindromeNumber(h, getTail(h)));  // false
    }
}
```


<details>
<summary><strong>Trace — head = [1, 2, 3, 2, 1]</strong></summary>

```
list = [1, 2, 3, 2, 1]

Step 1 │ L=node(1), R=node(1)         │ vals match (1 == 1) │ L→2, R→2
Step 2 │ L=node(2), R=node(2)         │ vals match (2 == 2) │ L→3, R→3
Done   │ L == R (both at node(3))     │ loop exits           │ return true
Result: true ✓ (every mirrored pair matched and pointers met in the middle)
```

</details>
<details>
<summary><strong>Trace — head = [1, 2, 3, 4, 5]</strong></summary>

```
list = [1, 2, 3, 4, 5]

Step 1 │ L=node(1), R=node(5)         │ 1 != 5 → mismatch    │ return false
Result: false ✓ (mismatch detected on the very first iteration)
```

</details>

### Complexity Analysis

| Measure | Value | Reason |
|---|---|---|
| Time  | **O(N)** | Each pointer covers half the list; together they touch every node at most once. |
| Space | **O(1)** | Two pointers — no copy, no reverse. |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty list | `head = null` | `true` | Vacuously palindromic. |
| Single node | `[7]` | `true` | A length-1 sequence equals its reverse. |
| Even length match | `[1, 2, 2, 1]` | `true` | Pointers cross (`left.prev == right`) without ever colliding. |
| Even length mismatch | `[1, 2, 3, 1]` | `false` | Inner pair `(2, 3)` fails — return early. |

We've used both pointers symmetrically. Up next: a problem where the *decision* of which pointer to move depends on a computed value.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The palindrome check is the simplest mirror-equality variant — both pointers always move every iteration because the per-pair decision is binary (match or fail). Early exit on the first mismatch is what beats the array-copy baseline in both time and space.

</details>