---
title: "Palindrome Checker"
summary: "Given the head of a singly linked list, write a function to check if the given list is a palindrome or not. Your function should return true if it is a palindrome and false if it's not."
prereqs:
  - 10-pattern-fast-and-slow-pointers/01-pattern
difficulty: medium
---

# Palindrome checker

## Problem Statement

Given the **head** of a singly linked list, write a function to check if the given list is a palindrome or not. Your function should return `true` if it is a palindrome and `false` if it's not. 

## Examples

**Example 1:**
```
Input:  head = [1, 2, 2, 1]
Output: true
Explanation: Even-length palindrome — front half [1, 2] mirrors the reversed back half [1, 2].
```

**Example 2:**
```
Input:  head = [1, 2]
Output: false
Explanation: Front (1) does not match the reversed back (2).
```

**Example 3:**
```
Input:  head = [1, 2, 3, 2, 1]
Output: true
Explanation: Odd-length palindrome — the middle node (3) is its own mirror; the surrounding pairs (1↔1, 2↔2) match.
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a fast-and-slow problem is that a palindrome's definition is positional — node `i` must match node `n - 1 - i`. A singly linked list has no backward pointers, so reading the list "from the right" requires either an auxiliary copy (`O(n)` extra space) or in-place reversal of the back half (`O(1)` extra space). Once the back half is reversed in place, the comparison becomes a parallel forward walk from both halves' heads — at every tick, the front-half cursor and the reversed-back-half cursor should match in value.

The **pointer placement** combines two patterns from this chapter. First, the 2:1 fast-and-slow walk locates the middle node — the boundary at which the back half begins. Second, the back half is reversed in place using the three-pointer reversal loop from pattern 07 (full-list reversal applied to the suffix). Finally, two new cursors `head_a` (starts at `head`) and `head_b` (starts at the reversed back half's head) walk forward in lockstep, comparing values until `head_b` reaches `null`. The comparison stops at `head_b == null` so that on odd-length lists the centre node is never compared against itself — the back half is one node shorter than the front (the middle stays with the front by the convention from `findMiddleNode`).

What **breaks if you reach for a naive approach**? Copying all values into an array and using two-pointer index comparison works in `O(n)` time but uses `O(n)` extra space. Stack-based comparison (push the first half onto a stack, then pop while walking the second half) is also `O(n)` space. Recursive comparison (recurse to the tail, then bubble back comparing values) costs `O(n)` stack space and overflows on long lists. The 2:1 walk plus in-place reversal hits `O(n)` time and `O(1)` space — the optimal envelope for this problem, but it does mutate the input list. If preserving the original structure matters, a second reversal pass after the comparison restores it.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Palindrome Checker |
|---|---|
| **Q1.** Does the problem ask for a node at a proportional position? | **Yes** — the middle node is the `1/2` point and serves as the boundary between the front half and the back half to be reversed. |
| **Q2.** Can the position be computed in a single forward pass? | **Yes** — the 2:1 walk locates the middle in one walk; reversal and comparison are downstream work. |
| **Q3.** Is the work at each step `O(1)`? | **Yes** — each tick of the boundary walk, reversal loop, and comparison loop performs a constant number of pointer/value operations. |
| **Q4.** Is `O(1)` extra space required? | **Yes** — boundary walk uses two cursors, reversal uses three, comparison uses two; the in-place rewrite of the back half is what keeps space constant. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Compose three small walks: find the middle, reverse the back half, then compare front and reversed-back in parallel.

1. **Handle the trivial cases first.** If `head` is `null` or has only one node, return `true` — a list of length 0 or 1 is trivially a palindrome.
2. **Find the middle with the 2:1 walk.** Run the standard `findMiddleNode` helper: `slow = fast = head`, advance until `fast` or `fast.next` is `null`. When the loop exits, `slow` is the middle node (second middle on even lengths).
3. **Reverse the back half in place.** Pass `slow` as the head of the back half to the three-pointer reversal helper (full-list reversal from pattern 07). The returned reference is the new head of the reversed back half — formerly the tail of the original list.
4. **Walk both halves in parallel.** Initialise `head_a = head` (front half) and `head_b = reversed_second_half` (reversed back half). Loop while `head_b != null`. At each tick, compare `head_a.val` against `head_b.val`; if they differ, return `false`. Otherwise advance both cursors by one node.
5. **Return the verdict.** If the loop completes without finding a mismatch, every front-half value matched the reversed back-half value at the same index — return `true`.

The loop stops on `head_b == null` (not `head_a`) so that on odd-length lists, the front-half cursor naturally walks past the middle node without comparing it against anything. The middle is its own mirror by definition.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution



```python run viz=linked-list viz-root=head
from typing import Optional


class ListNode:
    def __init__(self, val=0, nxt=None):
        self.val = val
        self.next = nxt


def from_list(values):
    if not values:
        return None
    head = ListNode(values[0])
    cur = head
    for v in values[1:]:
        cur.next = ListNode(v)
        cur = cur.next
    return head


class Solution:
    def reverse(self, head: Optional[ListNode]) -> Optional[ListNode]:
        current: Optional[ListNode] = head
        previous: Optional[ListNode] = None

        while current is not None:
            next_node = current.next
            current.next = previous
            previous = current
            current = next_node

        return previous

    def find_middle_node(
        self, head: Optional[ListNode]
    ) -> Optional[ListNode]:
        slow = head
        fast = head
        while fast is not None and fast.next is not None:
            slow = slow.next
            fast = fast.next.next

        return slow

    def is_palindrome(
        self, head_a: Optional[ListNode], head_b: Optional[ListNode]
    ) -> bool:
        while head_b is not None:
            if head_a.val != head_b.val:
                return False
            head_a = head_a.next
            head_b = head_b.next
        return True

    def palindrome_checker(self, head: Optional[ListNode]) -> bool:
        if head is None or head.next is None:
            return True

        # Find the middle node of the linked list
        middle_node = self.find_middle_node(head)

        # Reverse the second half of the list
        reversed_second_half = self.reverse(middle_node)

        # Compare the elements of first half with the reversed second
        # half
        return self.is_palindrome(head, reversed_second_half)


print(Solution().palindrome_checker(from_list([1, 2, 2, 1])))        # True
print(Solution().palindrome_checker(from_list([1, 2])))               # False

# Edge cases
print(Solution().palindrome_checker(None))                            # True
print(Solution().palindrome_checker(from_list([1])))                  # True
print(Solution().palindrome_checker(from_list([1, 1])))               # True
print(Solution().palindrome_checker(from_list([1, 2, 1])))            # True
print(Solution().palindrome_checker(from_list([1, 2, 3, 2, 1])))     # True
print(Solution().palindrome_checker(from_list([1, 2, 3, 4, 5])))     # False
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    static ListNode fromList(int... values) {
        if (values.length == 0) return null;
        ListNode head = new ListNode(values[0]);
        ListNode cur = head;
        for (int i = 1; i < values.length; i++) {
            cur.next = new ListNode(values[i]);
            cur = cur.next;
        }
        return head;
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

        private ListNode findMiddleNode(ListNode head) {
            ListNode slow = head;
            ListNode fast = head;
            while (fast != null && fast.next != null) {
                slow = slow.next;
                fast = fast.next.next;
            }

            return slow;
        }

        private boolean isPalindrome(ListNode headA, ListNode headB) {
            while (headB != null) {
                if (headA.val != headB.val) {
                    return false;
                }
                headA = headA.next;
                headB = headB.next;
            }
            return true;
        }

        public boolean palindromeChecker(ListNode head) {
            if (head == null || head.next == null) {
                return true;
            }

            // Find the middle node of the linked list
            ListNode middleNode = findMiddleNode(head);

            // Reverse the second half of the list
            ListNode reversedSecondHalf = reverse(middleNode);

            // Compare the elements of first half with the reversed second
            // half
            return isPalindrome(head, reversedSecondHalf);
        }
    }

    public static void main(String[] args) {
        System.out.println(new Solution().palindromeChecker(fromList(1, 2, 2, 1)));        // true
        System.out.println(new Solution().palindromeChecker(fromList(1, 2)));               // false

        // Edge cases
        System.out.println(new Solution().palindromeChecker(null));                         // true
        System.out.println(new Solution().palindromeChecker(fromList(1)));                  // true
        System.out.println(new Solution().palindromeChecker(fromList(1, 1)));               // true
        System.out.println(new Solution().palindromeChecker(fromList(1, 2, 1)));            // true
        System.out.println(new Solution().palindromeChecker(fromList(1, 2, 3, 2, 1)));     // true
        System.out.println(new Solution().palindromeChecker(fromList(1, 2, 3, 4, 5)));     // false
    }
}
```

### Dry Run

```
head = 1 → 2 → 2 → 1 → null   (Example 1, even length n=4)

Step 1 — Find middle with the 2:1 walk:
  Init: slow = 1, fast = 1
  Tick 1: slow = 2 (second node), fast = 2 (third node)
  Tick 2: slow = 2 (third node), fast = null
  Guard fails. Middle node (slow) = 2 (the third node).

Step 2 — Reverse the back half starting at slow:
  Back half before: 2 → 1 → null
  After reversal:   1 → 2 → null
  reversed_second_half = 1 (the former tail)
  Front half still: 1 → 2 → (now points to the reversed back half)

Step 3 — Walk front and reversed-back in parallel:
  Init: head_a = 1 (front), head_b = 1 (reversed back)
  Tick 1: head_a.val=1, head_b.val=1 → match. Advance both.
          head_a = 2, head_b = 2.
  Tick 2: head_a.val=2, head_b.val=2 → match. Advance both.
          head_b = null.
  Loop exits — no mismatch found.

Return True. ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | Boundary walk visits `n / 2` nodes via `fast`; reversal visits the `n / 2` back-half nodes once; comparison visits the same `n / 2` nodes once. Total: `~2 * n` node visits. |
| **Space** | `O(1)` | Two cursors for the boundary walk, three for reversal, two for comparison — all local references; no auxiliary array. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`) | Early return `true` — trivially a palindrome. |
| Single node (`[1]`) | Early return `true` — trivially a palindrome. |
| Two equal nodes (`[1, 1]`) | Middle = second node. Reverse it (still one node). Compare `head.val=1` vs `head_b.val=1` → match. Return `true`. |
| Two different nodes (`[1, 2]`) | Middle = second node. Reverse it (still one node). Compare `1` vs `2` → mismatch. Return `false`. |
| Odd-length palindrome (`[1, 2, 1]`) | Middle = `2` (second node). Reverse back half = `2 → 1 → null` reversed = `1 → 2 → null`. Compare first front node `1` vs first reversed-back `1` → match. Advance. `head_b` becomes the former `2`; compare `2` vs `2` → match. `head_b` becomes `null`. Return `true`. Middle is never compared against itself. |
| Mutates the input list | After the call, the back half of the original list is reversed — the original structure is lost. If preservation matters, run the reversal helper a second time on the back half to restore it. |

<details>
<summary><h2>Key Takeaway</h2></summary>


Palindrome checking composes three small walks: middle-finding (this pattern), back-half reversal (pattern 07), and a parallel comparison walk. None of the three is novel on its own — the contribution is recognising that "read the list backwards" can be reduced to an in-place reversal of the back half, keeping the whole job in `O(1)` extra space.

</details>

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Fast-and-slow pointers is a one-line insight: **when two pointers move at different speeds, their relative position encodes a proportion of the list's length**. The pattern resolves to:

```
slow = fast = head
while fast is not null and fast.next is not null:
    slow = slow.next
    fast = fast.next.next
# slow now sits at the middle (the (n/2 + 1)-th node for even n)
```

Four insights worth burning in:

| Insight | Why it matters |
|---|---|
| Ratio controls the landing position | 2:1 finds the middle, 3:1 finds the 1/3 point, n:1 finds the 1/n point. The math is the algorithm. |
| Termination check must guard both pointers | `fast != null AND fast.next != null` — missing the second clause NPEs on even-length lists when fast tries to take its second step. |
| First vs second middle is just initialisation | For even-length lists, starting fast at `head.next` lands slow on the first middle; starting fast at `head` lands it on the second. Both are one-liners. |
| This pattern is the ancestor of Floyd's cycle detection | Same two-speed walk, different observation — inside a cycle, fast eventually laps slow; outside, fast falls off. One technique, two outcomes. |

When you next see "find the middle", "split in half", "palindrome check", "reorder alternating", or any problem asking you to infer structure from length ratios — reach for the 2:1 walk first.

> **Transfer Challenge:** Return the <code>k</code>-th node from the **start**, but you can only touch the head pointer once. How? Use the same idea — but what's the right ratio?
>
> <details><summary><strong>Solution hint</strong></summary>
>
> Different problem, different trick — this one needs a <em>fixed-gap</em> two-pointer, not fixed-ratio. That's the <strong>sliding-window traversal</strong> pattern from lesson 8. Advance fast by <code>k − 1</code> hops first; then slide both together until fast hits the tail. Fast-and-slow and sliding-window are <em>sibling</em> patterns — both use two pointers, but one fixes the ratio of their speeds, the other fixes the distance between them. Pick the right sibling for the job.
>
> </details>

</details>

