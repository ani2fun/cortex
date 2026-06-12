---
title: "Split Alternate Groups"
summary: "Given the head of a singly linked list and a positive integer k, write a function to split the list into two separate lists by alternating groups of k nodes and return the heads of both these lists."
prereqs:
  - 11-pattern-split/01-pattern
difficulty: easy
kind: problem
topics: [split, singly-linked-list]
---

# Split alternate groups

## Problem Statement

Given the **head** of a singly linked list and a positive integer **k**, write a function to split the list into two separate lists by alternating groups of `k` nodes and return the heads of both these lists.

If the remaining nodes at the end are fewer than `k`, include all of them in the respective group.

## Examples

**Example 1:**
```
Input:  head = [5, 2, 3, 10, 6, 8], k = 2
Output: [[5, 2, 6, 8], [3, 10]]
Explanation: Groups of size 2 alternate between the two outputs —
             [5, 2] → list 0, [3, 10] → list 1, [6, 8] → list 0.
```

**Example 2:**
```
Input:  head = [6, 1, 3, 10, 6, 8], k = 5
Output: [[6, 1, 3, 10, 6], [8]]
Explanation: First five nodes form one full group → list 0. The trailing
             single node [8] is shorter than k but still becomes the next
             alternating group → list 1.
```

**Example 3:**
```
Input:  head = [1, 2, 3, 4, 5, 6], k = 3
Output: [[1, 2, 3], [4, 5, 6]]
Explanation: Two full groups of three — first to list 0, second to list 1.
```

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `1 ≤ k ≤ 10⁴`
- Re-link nodes — `O(1)` extra space; do not allocate new nodes

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def split_alternate_groups(self, head, k):
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
k = int(input())                               # the test case's k
f, s = Solution().split_alternate_groups(head, k)
print_list(f)
print_list(s)
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
        List<ListNode> splitAlternateGroups(ListNode head, int k) {
            // Your code goes here
            return Arrays.asList(null, null);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int k = Integer.parseInt(sc.nextLine().trim());
        List<ListNode> r = new Solution().splitAlternateGroups(head, k);
        printList(r.get(0));
        printList(r.get(1));
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[5, 2, 3, 10, 6, 8]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "head": "[5, 2, 3, 10, 6, 8]", "k": "2" }, "expected": "[5, 2, 6, 8]\n[3, 10]" },
    { "args": { "head": "[6, 1, 3, 10, 6, 8]", "k": "5" }, "expected": "[6, 1, 3, 10, 6]\n[8]" },
    { "args": { "head": "[1, 2, 3, 4, 5, 6]", "k": "3" }, "expected": "[1, 2, 3]\n[4, 5, 6]" },
    { "args": { "head": "[1]", "k": "1" }, "expected": "[1]\n[]" },
    { "args": { "head": "[1, 2]", "k": "1" }, "expected": "[1]\n[2]" },
    { "args": { "head": "[1, 2, 3, 4]", "k": "4" }, "expected": "[1, 2, 3, 4]\n[]" },
    { "args": { "head": "[]", "k": "2" }, "expected": "[]\n[]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a split problem is that the destination bucket depends on *position*, not value — specifically, on which group of `k` consecutive nodes the current node belongs to. The classifier is stateful: a counter and a flag. After every `k` nodes pass through the walker, the flag flips, and subsequent nodes route to the other bucket. The locality of "decide per node" still holds; the classifier carries a tiny piece of state across calls.

The **bucket placement** treats each chunk as the unit of work. Rather than calling the classifier per node and incurring `k` per-node flag updates, the cleaner implementation walks `k` nodes at a time, captures `chunk_start` and `previous` (the last node in the chunk), severs the chunk from the rest of the list (`previous.next = null`), then splices the entire chunk onto the current bucket's tail in one assignment. The `add_to_first_list` flag flips after each chunk. The dummies still absorb the "first chunk" special case so the first append works without a guard.

What **breaks if you reach for a naive approach**? The two-pass version walks the list once to record positions `[0..k-1]` to list 0, `[k..2k-1]` to list 1, alternating, then a second pass to actually build the output chains. That doubles the work and forces the caller to keep `head` reachable across both passes. Worse, a per-node classifier without chunk-severing has to re-decide on every single node and never gets to use the `O(1)` chunk-splice trick — every node becomes its own append. The split template with chunk-level routing reads each node exactly once.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Split Alternate Groups |
|---|---|
| **Q1.** Does the problem ask to partition the input into multiple output lists? | **Yes** — two output lists, alternating groups of `k` consecutive nodes. |
| **Q2.** Can each node's bucket be computed locally? | **Yes** — a single boolean (`add_to_first_list`) plus a counter (the inner `for _ in range(k)` loop) decides routing without look-ahead. |
| **Q3.** Is the work at each step `O(1)`? | **Yes** — each chunk takes `O(k)` to walk and a constant number of pointer assignments to splice; per-node work is `O(1)`. |
| **Q4.** Can output lists share original nodes (re-linked, not copied)? | **Yes** — the original nodes are re-threaded, and the chunk severing (`previous.next = null`) is what turns the original chain into two independent ones. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Walk the original list in chunks of up to `k` nodes. Splice each chunk onto the current bucket's tail; alternate the bucket after every chunk.

1. **Allocate two dummies and two tails.** Create `first_list_dummy` and `second_list_dummy`; set both tails to their dummies. The dummies absorb the "first chunk into this bucket" special case.
2. **Initialise the walker and the alternation flag.** Set `current = head` and `add_to_first_list = true`. The first chunk lands in list 0; the flag flips after each chunk.
3. **Walk the list in chunks until exhausted.** While `current != null`, do the following:
   - Capture `chunk_start = current` — the head of this chunk.
   - Advance up to `k` steps, tracking `previous = current` and `current = current.next` each step. Stop early if `current == null`. After the inner loop, `previous` holds the last node of the chunk and `current` holds the first node of the next chunk (or `null`).
   - Sever the chunk from the rest of the original list: `previous.next = null`.
   - Splice the chunk onto the current bucket's tail: `tail.next = chunk_start; tail = previous`. One assignment captures up to `k` nodes.
   - Flip `add_to_first_list` so the next chunk lands in the other bucket.
4. **Return the real heads.** Return `[first_list_dummy.next, second_list_dummy.next]` to skip past the placeholders.

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
    def split_alternate_groups(self, head, k):

        # Head and tail references for the two resulting lists
        first_list_dummy = ListNode(0)
        first_list_tail = first_list_dummy

        second_list_dummy = ListNode(0)
        second_list_tail = second_list_dummy

        current = head

        # Flag to alternate between the two lists
        add_to_first_list = True

        # Iterate through the original list
        while current is not None:

            # Start of the current chunk
            chunk_start = current
            previous = None

            # Traverse up to k nodes for the current chunk
            for _ in range(k):
                if current is None:
                    break
                previous = current
                current = current.next

            # Disconnect the chunk from the rest of the list
            if previous:
                previous.next = None

            # Attach chunk to the appropriate list
            if add_to_first_list:

                # Attach chunk to the first list
                first_list_tail.next = chunk_start

                # Move tail to the end of the chunk
                first_list_tail = previous
            else:

                # Attach chunk to the second list
                second_list_tail.next = chunk_start

                # Move tail to the end of the chunk
                second_list_tail = previous

            # Alternate for next chunk
            add_to_first_list = not add_to_first_list

        # Return heads of the two lists
        return [first_list_dummy.next, second_list_dummy.next]

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
k = int(input())                               # the test case's k
f, s = Solution().split_alternate_groups(head, k)
print_list(f)
print_list(s)
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
        public List<ListNode> splitAlternateGroups(ListNode head, int k) {

            // Head and tail references for the two resulting lists
            ListNode firstListDummy = new ListNode(0);
            ListNode firstListTail = firstListDummy;

            ListNode secondListDummy = new ListNode(0);
            ListNode secondListTail = secondListDummy;

            ListNode current = head;

            // Flag to alternate between the two lists
            boolean addToFirstList = true;

            // Iterate through the original list
            while (current != null) {

                // Start of the current chunk
                ListNode chunkStart = current;
                ListNode previous = null;

                // Traverse up to k nodes for the current chunk
                for (int i = 0; i < k && current != null; i++) {
                    previous = current;
                    current = current.next;
                }

                // Disconnect the chunk from the rest of the list
                previous.next = null;

                // Attach chunk to the appropriate list
                if (addToFirstList) {

                    // Attach chunk to the first list
                    firstListTail.next = chunkStart;

                    // Move tail to the end of the chunk
                    firstListTail = previous;
                }

                // If not the first list, then it must be the second list
                else {

                    // Attach chunk to the second list
                    secondListTail.next = chunkStart;

                    // Move tail to the end of the chunk
                    secondListTail = previous;
                }

                // Alternate for next chunk
                addToFirstList = !addToFirstList;
            }

            // Return heads of the two lists
            return Arrays.asList(firstListDummy.next, secondListDummy.next);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        int k = Integer.parseInt(sc.nextLine().trim());
        List<ListNode> r = new Solution().splitAlternateGroups(head, k);
        printList(r.get(0));
        printList(r.get(1));
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
head = 5 → 2 → 3 → 10 → 6 → 8 → null   (Example 1, k = 2)

Init: first_dummy → null, first_tail  = first_dummy
      second_dummy → null, second_tail = second_dummy
      current = node 5, add_to_first_list = true

Chunk 1: chunk_start = node 5
         inner loop (2 hops): previous = node 2, current = node 3
         sever — node 2.next = null
         add_to_first_list = true → splice into list 0
           first_tail.next = node 5; first_tail = node 2
         flip flag → add_to_first_list = false
         List 0 so far: dummy → 5 → 2 → null

Chunk 2: chunk_start = node 3
         inner loop (2 hops): previous = node 10, current = node 6
         sever — node 10.next = null
         add_to_first_list = false → splice into list 1
           second_tail.next = node 3; second_tail = node 10
         flip flag → add_to_first_list = true
         List 1 so far: dummy → 3 → 10 → null

Chunk 3: chunk_start = node 6
         inner loop (2 hops): previous = node 8, current = null
         sever — node 8.next = null (already null)
         add_to_first_list = true → splice into list 0
           first_tail.next = node 6; first_tail = node 8
         flip flag → add_to_first_list = false
         List 0 so far: dummy → 5 → 2 → 6 → 8 → null

Loop exits (current == null).

Return [first_dummy.next, second_dummy.next] = [node 5, node 3].
Lists: [5, 2, 6, 8] and [3, 10]. ✓
```

### Result Size

Two output lists. If `n` and `k` give `q = n // k` full chunks and `r = n % k` trailing nodes, list 0 gets `ceil((q + (1 if r > 0 else 0)) / 2)` chunks and list 1 gets the rest, alternating. Sizes sum to `n`.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n)` | Each node is walked exactly once across all chunks. Chunk severing and splicing are `O(1)` per chunk; total over all chunks is `O(n)`. |
| **Space** | `O(1)` | Two dummies, two tails, one walker, one flag — independent of `n` and `k`. |

### Edge Cases

| Case | What happens |
|---|---|
| Single node, `k = 1` (`[1]`) | One chunk of size 1 routes to list 0; loop exits. Return `[node 1, null]`. |
| Two nodes, `k = 1` (`[1, 2]`) | Chunk 1 (node 1) → list 0; chunk 2 (node 2) → list 1. Return `[node 1, node 2]`. |
| `k` equals list length (`[1, 2, 3, 4], k = 4`) | One full chunk of size 4 routes to list 0; loop exits. Return `[1 → 2 → 3 → 4, null]`. |
| `k` exceeds list length (`[1, 2], k = 5`) | Inner loop hits `current == null` after 2 hops; one short chunk of size 2 routes to list 0. Return `[1 → 2, null]`. |
| Length is exact multiple of `2k` (`[1..6], k = 3`) | Two equal chunks: chunk 1 → list 0, chunk 2 → list 1. Return `[1 → 2 → 3, 4 → 5 → 6]`. |
| Trailing partial chunk (`[5, 2, 3, 10, 6, 8], k = 2`) | Three chunks of size 2, alternating 0/1/0. Return `[5 → 2 → 6 → 8, 3 → 10]`. |
| Trailing single node (`[6, 1, 3, 10, 6, 8], k = 5`) | One full chunk of 5 → list 0; one short chunk of 1 → list 1. Return `[6 → 1 → 3 → 10 → 6, 8]`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The split template extends to chunk-level routing — the classifier flips per `k` nodes instead of per node, and the chunk is spliced onto the bucket tail in one assignment. The dummy-and-tails skeleton is unchanged; only the granularity of "what gets appended at once" grew from one node to `k`.

</details>
