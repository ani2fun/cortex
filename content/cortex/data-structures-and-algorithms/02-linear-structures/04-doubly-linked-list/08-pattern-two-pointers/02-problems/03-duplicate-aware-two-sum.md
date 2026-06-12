---
title: "Duplicate-Aware Two Sum"
summary: "Given the head and tail of a doubly linked list sorted non-decreasing, and an integer target, return all unique pairs summing to target. The list may contain duplicates, but the result must not contai"
prereqs:
  - 08-pattern-two-pointers/01-pattern
difficulty: medium
kind: problem
topics: [two-pointers, doubly-linked-list]
---

# Duplicate-Aware Two Sum

## Problem Statement

Given the **head** of a doubly linked list sorted non-decreasing, and an integer **target**, return all unique pairs summing to `target`. The list **may contain duplicates**, but the result must not contain duplicate pairs (in any order).

## Examples

**Example 1**
```
Input:  head = [1, 2, 2, 3, 4, 5], target = 6
Output: [[1, 5], [2, 4]]
Explanation: 1+5=6 and 2+4=6. The duplicate 2 is not paired again.
```

**Example 2**
```
Input:  head = [1, 2, 2, 2, 2], target = 3
Output: [[1, 2]]
Explanation: 1+2=3 — but only one such pair, despite four 2s.
```

**Example 3**
```
Input:  head = [2], target = 2
Output: []
Explanation: Need two values to sum.
```

**Example 4**
```
Input:  head = [1, 1, 1, 1], target = 2
Output: [[1, 1]]
Explanation: Every value is the same; the first (1, 1) pair is recorded, and the duplicate-skip helpers collapse the rest of the run to a single recorded pair.
```

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `-10⁴ ≤ node.val ≤ 10⁴`
- The list is sorted in non-decreasing order and may contain duplicates
- `-10⁸ ≤ target ≤ 10⁸`

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, prev=None, next=None):
        self.val = val
        self.prev = prev
        self.next = next

class Solution:
    def duplicate_aware_two_sum(self, head, target):
        # Your code goes here — find the tail, run the converging loop;
        # after a match, skip past all nodes sharing the same value on
        # both sides before resuming.
        pass

def build_list(values):              # [1, 2, 3] → 1 ⇄ 2 ⇄ 3
    head = tail = None
    for v in values:
        node = ListNode(v, prev=tail)
        if tail is not None:
            tail.next = node
        else:
            head = node
        tail = node
    return head

def print_list(head):                # 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
    out = []
    while head:
        out.append(head.val)
        head = head.next
    print(out)

head = build_list(ast.literal_eval(input()))   # the test case's head
target = int(input())
print(Solution().duplicate_aware_two_sum(head, target))
```

```java run viz=linked-list viz-root=head
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode prev, next;
        ListNode(int val) { this.val = val; }
    }

    static class Solution {
        public List<List<Integer>> duplicateAwareTwoSum(ListNode head, int target) {
            // Your code goes here — find the tail, run the converging loop;
            // after a match, skip past all nodes sharing the same value on
            // both sides before resuming.
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().duplicateAwareTwoSum(head, target));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 ⇄ 2 ⇄ 3
        ListNode head = null, tail = null;
        for (int v : values) {
            ListNode node = new ListNode(v);
            node.prev = tail;
            if (tail != null) tail.next = node;
            else head = node;
            tail = node;
        }
        return head;
    }

    static void printList(ListNode head) {         // 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
        List<Integer> out = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) out.add(n.val);
        System.out.println(out);
    }

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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[1, 2, 2, 3, 4, 5]" },
    { "id": "target", "label": "target", "type": "int", "placeholder": "6" }
  ],
  "cases": [
    { "args": { "head": "[1, 2, 2, 3, 4, 5]", "target": "6" }, "expected": "[[1, 5], [2, 4]]" },
    { "args": { "head": "[1, 2, 2, 2, 2]", "target": "3" }, "expected": "[[1, 2]]" },
    { "args": { "head": "[2]", "target": "2" }, "expected": "[]" },
    { "args": { "head": "[1, 1, 1, 1]", "target": "2" }, "expected": "[[1, 1]]" },
    { "args": { "head": "[1, 1, 2, 3]", "target": "4" }, "expected": "[[1, 3]]" },
    { "args": { "head": "[1, 2, 3, 4, 5]", "target": "10" }, "expected": "[]" },
    { "args": { "head": "[1, 2]", "target": "3" }, "expected": "[[1, 2]]" },
    { "args": { "head": "[2, 2, 2, 2]", "target": "5" }, "expected": "[]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** is the same as plain Two Sum — sorted DLL, monotonic running sum, converging walkers — plus a new constraint: the result set must contain no duplicate pairs even when the input does. The duplicate-skip step is the only addition. The list is still sorted, so all copies of any given value sit in a contiguous run; advancing past every node in that run lands the pointer on the next distinct value in `O(k)` for a run of length `k`, and across all runs the total skip work amortises to `O(n)`.

The **pointer placement** keeps `left = head` and `right = tail` exactly as before. After a `sum == target` match, both pointers need to step past every node sharing their current value before the next iteration. Two helpers do exactly this: `skip_duplicates_left` walks `left` forward through equal values, then returns `left.next` (the first node with a *different* value); `skip_duplicates_right` does the mirror. The helpers must be called in the right order — the right-side helper uses the already-advanced `left` for its `left != right` guard, so calling them in reverse can underrun the cursor on tight inputs like `[2, 2, 2, 2]`.

What **breaks if you reach for the naive approach**? Running plain Two Sum and de-duplicating the result afterwards (e.g. `set(map(tuple, result))`) works but pays `O(p)` extra space for the seen-set plus an extra pass over the result. Worse, it produces duplicate pair *values* in transit — on `[2, 2, 2, 2], target = 4` the loop records `(2, 2)` once, then advances both pointers to the next 2s and records `(2, 2)` again, etc. — until the de-dup pass collapses them. The skip helpers prevent the duplicate work from happening in the first place, keeping the space at `O(1)` auxiliary and the time at `O(n)`.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Duplicate-Aware Two Sum |
|---|---|
| **Q1.** Are two nodes inspected at the same time, one from each end? | **Yes** — `left.val` and `right.val` are compared together every iteration. |
| **Q2.** Does one pointer start near `head` and the other near `tail`? | **Yes** — `left = head` and `right = tail`. |
| **Q3.** Do both pointers move strictly inward? | **Yes** — the main loop and the skip helpers both move pointers inward only; no backward step ever happens after a match. |
| **Q4.** Is the per-step work `O(1)`? | **Amortised yes** — each iteration is `O(1)` plus the skip-helper cost, and the skip work across the whole run amortises to `O(n)` total. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Run the converging two-pointer loop, with duplicate-skipping after each match.

1. **Handle the trivial guards.** If `head` is `null` or `head.next` is `null`, return an empty result.
2. **Initialise the pointers and result.** Set `left = head`, `right = tail`, and `result = []`.
3. **Loop while the search space is non-empty.** Continue while `left != right` and `left.val <= right.val`. The `<=` admits the equal-value case so a pair like `(1, 1)` with `target = 2` is still considered.
4. **Compute the running sum and branch.** Let `total = left.val + right.val`. If `total == target`, append the pair to `result`, then *both* pointers jump to their next distinct value via the skip helpers (left first, then right — so the right helper sees the already-advanced left). If `total < target`, advance `left = left.next`. If `total > target`, retreat `right = right.prev`.
5. **Skip-left helper.** Walk `left` forward while `left.val == left.next.val` and `left != right`; return `left.next` (the first different value).
6. **Skip-right helper.** Walk `right` backward while `right.val == right.prev.val` and `left != right`; return `right.prev` (the first different value).
7. **Return the result.** When the loop exits, `result` holds every distinct value pair summing to `target`, outermost-first.

</details>
<details>
<summary><h2>What Does "Skipping Duplicates Safely" Mean?</h2></summary>


When we find a pair, naively moving each pointer one step risks finding the *same value pair* again. We need to advance `left` past every node sharing its current value, and `right` back past every node sharing its current value. Two helpers do exactly this — and they must be called in the right order so that the second sees the *already-advanced* first pointer (otherwise `left == right` checks misfire on tight inputs).

</details>
<details>
<summary><h2>The Skip-Duplicates Strategy (Visualised)</h2></summary>


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
    A["[1, 2, 2, 3, 4, 5] target=6<br/>L=1, R=5 → sum=6 ✓<br/>record [1,5]<br/>skip duplicates → L=2, R=4"]
    B["[1, 2, 2, 3, 4, 5]<br/>L=2, R=4 → sum=6 ✓<br/>record [2,4]<br/>skip duplicates of 2 (next is 3),<br/>of 4 (prev is 3) → L=3, R=3"]
    C["L == R → done"]
    A --> B --> C
```

<p align="center"><strong>Duplicate-aware Two Sum — after each match, both pointers walk past every node sharing their value before resuming.</strong></p>

> *Friction prompt:* what would happen if we forgot the duplicate skip and the input were `[2,2,2,2], target=4`? Predict the output before peeking.
>
> Answer: without skipping, `(L=2, R=2)` matches, both move inward, match again, etc. — we'd record `[2,2]` multiple times. The skip ensures we land on the *next distinct* values on both sides.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python solution time=O(n) space=O(1)
import ast
from typing import List, Optional

class ListNode:
    def __init__(self, val, prev=None, next=None):
        self.val = val
        self.prev = prev
        self.next = next

class Solution:
    def skip_duplicates_left(
        self, left: Optional['ListNode'], right: Optional['ListNode']
    ) -> Optional['ListNode']:
        while (
            left
            and left.next
            and left != right
            and left.val == left.next.val
        ):
            left = left.next

        # Return the pointer to the next unique element
        return left.next if left else None

    def skip_duplicates_right(
        self, left: Optional['ListNode'], right: Optional['ListNode']
    ) -> Optional['ListNode']:
        while (
            right
            and right.prev
            and left != right
            and right.val == right.prev.val
        ):
            right = right.prev

        # Return the pointer to the next unique element
        return right.prev if right else None

    def duplicate_aware_two_sum(self, head, target: int) -> List[List[int]]:

        # Find the tail
        tail = head
        while tail and tail.next:
            tail = tail.next

        # Check if the list is empty or has only one element
        if not head or not head.next:
            return []

        # Store the pairs of values that sum up to the target
        result: List[List[int]] = []
        left = head
        right = tail

        # Use a while loop to traverse the list using the two pointers
        while left and right and left != right and left.val <= right.val:
            total = left.val + right.val

            # If the sum matches the target, add the pair to the result list
            if total == target:
                result.append([left.val, right.val])

                # Move the left pointer to the next unique element
                left = self.skip_duplicates_left(left, right)

                # Move the right pointer to the previous unique element
                right = self.skip_duplicates_right(left, right)

            # Move the left pointer to increase the sum
            elif total < target:
                left = left.next

            # Move the right pointer to decrease the sum
            else:
                right = right.prev

        return result

def build_list(values):              # [1, 2, 3] → 1 ⇄ 2 ⇄ 3
    head = tail = None
    for v in values:
        node = ListNode(v, prev=tail)
        if tail is not None:
            tail.next = node
        else:
            head = node
        tail = node
    return head

def print_list(head):                # 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
    out = []
    while head:
        out.append(head.val)
        head = head.next
    print(out)

head = build_list(ast.literal_eval(input()))   # the test case's head
target = int(input())
print(Solution().duplicate_aware_two_sum(head, target))
```

```java solution
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode prev, next;
        ListNode(int val) { this.val = val; }
    }

    static class Solution {
        private ListNode skipDuplicatesLeft(ListNode left, ListNode right) {
            while (
                left != null &&
                left.next != null &&
                left != right &&
                left.val == left.next.val
            ) {
                left = left.next;
            }

            // Return the pointer to the next unique element
            return left != null ? left.next : null;
        }

        private ListNode skipDuplicatesRight(ListNode left, ListNode right) {
            while (
                right != null &&
                right.prev != null &&
                left != right &&
                right.val == right.prev.val
            ) {
                right = right.prev;
            }

            // Return the pointer to the next unique element
            return right != null ? right.prev : null;
        }

        public List<List<Integer>> duplicateAwareTwoSum(ListNode head, int target) {

            // Find the tail
            ListNode tail = head;
            while (tail != null && tail.next != null) tail = tail.next;

            // Check if the list is empty or has only one element
            if (head == null || head.next == null) {
                return new ArrayList<>();
            }

            // Store the pairs of values that sum up to the target
            List<List<Integer>> result = new ArrayList<>();
            ListNode left = head;
            ListNode right = tail;

            // Use a while loop to traverse the list using the two pointers
            while (
                left != null &&
                right != null &&
                left != right &&
                left.val <= right.val
            ) {
                int sum = left.val + right.val;

                // If the sum matches the target, add the pair to the result list
                if (sum == target) {
                    result.add(Arrays.asList(left.val, right.val));

                    // Move the left pointer to the next unique element
                    left = skipDuplicatesLeft(left, right);

                    // Move the right pointer to the previous unique element
                    right = skipDuplicatesRight(left, right);
                }

                // Move the left pointer to increase the sum
                else if (sum < target) {
                    left = left.next;
                }

                // Move the right pointer to decrease the sum
                else {
                    right = right.prev;
                }
            }

            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().duplicateAwareTwoSum(head, target));
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 ⇄ 2 ⇄ 3
        ListNode head = null, tail = null;
        for (int v : values) {
            ListNode node = new ListNode(v);
            node.prev = tail;
            if (tail != null) tail.next = node;
            else head = node;
            tail = node;
        }
        return head;
    }

    static void printList(ListNode head) {         // 1 ⇄ 2 ⇄ 3 → [1, 2, 3]
        List<Integer> out = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) out.add(n.val);
        System.out.println(out);
    }

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


<details>
<summary><strong>Trace — head = [1, 2, 2, 3, 4, 5], target = 6</strong></summary>

```
arr = [1, 2, 2, 3, 4, 5] (already sorted), target = 6, result = []

Step 1 │ left=node(1), right=node(5) │ total=1+5=6 == 6 │ result=[[1,5]]
       │ skip_left: 1 ≠ 2 → left=node(2)
       │ skip_right: 5 ≠ 4 → right=node(4)
Step 2 │ left=node(2), right=node(4) │ total=2+4=6 == 6 │ result=[[1,5],[2,4]]
       │ skip_left: 2 == 2 → advance; next is 3 → left=node(3)
       │ skip_right: 4 ≠ 3 → right=node(3)
Done   │ left == right → exit
Result: [[1, 5], [2, 4]] ✓
```

</details>

### Complexity Analysis

| Measure | Value | Reason |
|---|---|---|
| Time  | **O(N)** | Main loop plus skip helpers visit each node at most once; skip work amortises to O(N). |
| Space | **O(1)** auxiliary | Constant pointer variables; output list excluded. |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| All duplicates | `[2,2,2,2], target=4` | `[[2,2]]` | First match recorded; skip helpers collapse the run to a single pair. |
| Target unreachable | `[1,1,1], target=10` | `[]` | `sum < target` always. |
| Single node | `[2]` | `[]` | Cannot form a pair. |

The pattern stays the same — we just bolted on a way to dodge repeats. Now the real boss fight: what if we need *three* numbers, and an exact match isn't even guaranteed?

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


What is *new* here vs plain Two Sum is the skip-duplicates plumbing — two helpers that walk past every run of equal values after a match. The skeleton is unchanged; the duplicate work is amortised to `O(n)` because each node is visited at most twice (once by the main loop, once by a helper).

</details>
