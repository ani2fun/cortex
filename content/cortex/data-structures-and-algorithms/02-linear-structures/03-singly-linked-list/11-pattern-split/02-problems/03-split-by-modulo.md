---
title: "Split by Modulo"
summary: "Given the head of a singly linked list and a positive integer k, write a function to split the list into k separate lists. Each node should be placed into one of the k lists according to the remainder"
prereqs:
  - 11-pattern-split/01-pattern
difficulty: medium
kind: problem
topics: [split, singly-linked-list]
---

# Split by modulo

## Problem Statement

Given the **head** of a singly linked list and a positive integer **k**, write a function to split the list into `k` separate lists. Each node should be placed into one of the `k` lists according to the remainder when its value is divided by `k`. Your function should return the heads of all `k` lists in order from remainder `0` to `k - 1`.

## Examples

**Example 1:**
```
Input:  head = [5, 2, 3, 10, 6, 8], k = 3
Output: [[3, 6], [10], [5, 2, 8]]
Explanation: 5 % 3 = 2 → bucket 2; 2 % 3 = 2 → bucket 2; 3 % 3 = 0 → bucket 0;
             10 % 3 = 1 → bucket 1; 6 % 3 = 0 → bucket 0; 8 % 3 = 2 → bucket 2.
             Source order is preserved inside each bucket.
```

**Example 2:**
```
Input:  head = [4], k = 3
Output: [[], [4], []]
Explanation: 4 % 3 = 1 → bucket 1. Buckets 0 and 2 stay empty.
```

**Example 3:**
```
Input:  head = [0, 0, 0], k = 3
Output: [[0, 0, 0], [], []]
Explanation: 0 % 3 = 0 for every node — the entire list collapses into bucket 0.
```

## Constraints

- `0 ≤ list length ≤ 10⁵`
- `0 ≤ node.val ≤ 10⁴`
- `1 ≤ k ≤ 10³`
- Re-link nodes — `O(k)` extra space for dummies; do not allocate new list nodes

```python run viz=linked-list viz-root=head
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def split_by_modulo(self, head, k):
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
for p in Solution().split_by_modulo(head, k):
    print_list(p)
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
        List<ListNode> splitByModulo(ListNode head, int k) {
            // Your code goes here
            List<ListNode> result = new ArrayList<>();
            for (int i = 0; i < k; i++) result.add(null);
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] values = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        ListNode head = buildList(values);
        for (ListNode p : new Solution().splitByModulo(head, k)) printList(p);
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
    { "id": "k", "label": "k", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "head": "[5, 2, 3, 10, 6, 8]", "k": "3" }, "expected": "[3, 6]\n[10]\n[5, 2, 8]" },
    { "args": { "head": "[4]", "k": "3" }, "expected": "[]\n[4]\n[]" },
    { "args": { "head": "[0, 0, 0]", "k": "3" }, "expected": "[0, 0, 0]\n[]\n[]" },
    { "args": { "head": "[1, 2, 3, 4]", "k": "2" }, "expected": "[2, 4]\n[1, 3]" },
    { "args": { "head": "[1]", "k": "1" }, "expected": "[1]" },
    { "args": { "head": "[]", "k": "2" }, "expected": "[]\n[]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a split problem is that every node carries its destination on its sleeve — `current.val % k` is the bucket index, no look-around required. With `k` possible buckets instead of two, the only generalisation is to use *arrays* of dummies and tails rather than two separate variables. The skeleton scales from `k = 2` to any `k`; the classifier scales from one boolean to one modulo.

The **bucket placement** uses two parallel arrays. Allocate `dummy_heads = [ListNode(0)] * k` and `tails = dummy_heads[:]` (each `tails[i]` initially points at `dummy_heads[i]`). As the walker visits each node, the modulo picks a bucket index `group` and the standard tail-append wires the node into chain `group`. After the walk, run a `for` loop over `0..k-1` to seal every tail (`tails[i].next = null`) and to extract the real heads (`dummy_heads[i].next`).

What **breaks if you reach for a naive approach**? The two-pass-per-bucket version walks the list `k` times — `O(n * k)` time, and every node is re-classified `k - 1` times for nothing. The other naive option — build `k` separate result-list builders (`if group == 0: append to L0 else if group == 1: append to L1 …`) — works for tiny `k` but the cascade of `if/else if` explodes as `k` grows, and the routing code stops being uniform. Arrays-of-dummies keeps the routing core to two lines regardless of `k`.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Split by Modulo |
|---|---|
| **Q1.** Does the problem ask to partition the input into multiple output lists? | **Yes** — exactly `k` output lists, indexed by `value % k`. |
| **Q2.** Can each node's bucket be computed locally? | **Yes** — `group = current.val % k` reads only `current.val` and the constant `k`. |
| **Q3.** Is the work at each step `O(1)`? | **Yes** — one modulo, one array indexing, one tail-append, one pointer advance per node. The `k` doesn't appear in per-node cost. |
| **Q4.** Can output lists share original nodes (re-linked, not copied)? | **Yes** — the original nodes are re-threaded into `k` independent chains; no allocations beyond the `k` dummies. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Walk the original list once. Route each node to one of `k` pre-anchored output chains by `value % k`.

1. **Allocate `k` dummies and `k` tails.** Create `dummy_heads = [ListNode(0)] * k` and `tails = dummy_heads[:]` so each `tails[i]` starts at the matching dummy. Arrays let the routing core stay uniform across all `k` buckets.
2. **Initialise the walker at the head.** Set `current = head`. The walk visits each node exactly once, in source order.
3. **For each node, classify by `value % k`.** Compute `group = current.val % k`. The result is in `[0, k)` and indexes both arrays.
4. **Append `current` to its bucket's tail.** Set `tails[group].next = current`, then `tails[group] = tails[group].next` so subsequent appends to the same bucket land at the new end.
5. **Advance the walker.** Set `current = current.next` before any further mutation. The original chain stays intact during the loop because the append writes only to `tails[group].next`, not to `current.next`.
6. **Seal every bucket after the walk.** Loop `i` over `0..k-1` and set `tails[i].next = null` so no output chain inherits a stray edge into another bucket.
7. **Extract the real heads.** Build `result = [dummy_heads[i].next for i in range(k)]` to skip past every placeholder. Return `result`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution


```python solution time=O(n + k) space=O(k)
import ast

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

class Solution:
    def split_by_modulo(self, head, k):

        # Initialize head and tail references for k split lists
        dummy_heads = [ListNode(0) for _ in range(k)]
        tails = dummy_heads[:]

        # Create current reference to iterate through the list
        current = head

        # Iterate through the list and split nodes into k lists
        while current is not None:

            # Find group index using modulo operation
            group = current.val % k

            # `current` node goes to its modulo group
            tails[group].next = current

            # Move group tail forward
            tails[group] = tails[group].next

            # Move to the next node in the original list
            current = current.next

        # Terminate each list to avoid cycles
        for i in range(k):
            tails[i].next = None

        # Collect heads (excluding dummy nodes)
        return [dummy_heads[i].next for i in range(k)]

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
for p in Solution().split_by_modulo(head, k):
    print_list(p)
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
        public List<ListNode> splitByModulo(ListNode head, int k) {

            // Initialize head and tail references for k split lists
            List<ListNode> dummyHeads = new ArrayList<>();
            List<ListNode> tails = new ArrayList<>();
            for (int i = 0; i < k; i++) {

                // Dummy head nodes
                dummyHeads.add(new ListNode(0));

                // Tail starts at dummy
                tails.add(dummyHeads.get(i));
            }

            // Create current reference to iterate through the list
            ListNode current = head;

            // Iterate through the list and split nodes into k lists
            while (current != null) {

                // Find group index using modulo operation
                int group = current.val % k;

                // `current` node goes to its modulo group
                tails.get(group).next = current;

                // Move group tail forward
                tails.set(group, tails.get(group).next);

                // Move to the next node in the original list
                current = current.next;
            }

            // Terminate each list to avoid cycles
            for (int i = 0; i < k; i++) {
                tails.get(i).next = null;
            }

            // Collect heads (excluding dummy nodes)
            List<ListNode> result = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                result.add(dummyHeads.get(i).next);
            }

            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] values = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        ListNode head = buildList(values);
        for (ListNode p : new Solution().splitByModulo(head, k)) printList(p);
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
head = 5 → 2 → 3 → 10 → 6 → 8 → null   (Example 1, k = 3)

Init: dummy_heads = [d0, d1, d2]   (three placeholder nodes)
      tails       = [d0, d1, d2]   (each tail starts at its dummy)
      current = node 5

Tick 1: current = node 5. 5 % 3 = 2 → bucket 2.
        tails[2].next = node 5; tails[2] = node 5.
        current = node 2.

Tick 2: current = node 2. 2 % 3 = 2 → bucket 2.
        tails[2].next = node 2; tails[2] = node 2.
        current = node 3.

Tick 3: current = node 3. 3 % 3 = 0 → bucket 0.
        tails[0].next = node 3; tails[0] = node 3.
        current = node 10.

Tick 4: current = node 10. 10 % 3 = 1 → bucket 1.
        tails[1].next = node 10; tails[1] = node 10.
        current = node 6.

Tick 5: current = node 6. 6 % 3 = 0 → bucket 0.
        tails[0].next = node 6; tails[0] = node 6.
        current = node 8.

Tick 6: current = node 8. 8 % 3 = 2 → bucket 2.
        tails[2].next = node 8; tails[2] = node 8.
        current = null. Loop exits.

Seal:   tails[0].next = null → bucket 0 = 3 → 6 → null
        tails[1].next = null → bucket 1 = 10 → null
        tails[2].next = null → bucket 2 = 5 → 2 → 8 → null

Return [d0.next, d1.next, d2.next] = [[3, 6], [10], [5, 2, 8]]. ✓
```

### Result Size

`k` output lists. Each bucket holds the count of nodes with that residue mod `k`. Sizes sum to `n`; any bucket may be empty.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(n + k)` | One pass over the list — `n` ticks of `O(1)` work each — plus a final `O(k)` pass to seal tails and collect heads. For most inputs (`k <= n`), this simplifies to `O(n)`. |
| **Space** | `O(k)` | `k` dummy nodes, `k` tail pointers, `k` slots in the result array. The original nodes are re-linked, not copied. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty list (`head is null`, `k = 2`) | The walker loop never enters; both seals run on the dummies; collect step returns `[null, null]`. |
| `k = 1` (`[1], k = 1`) | Every node maps to bucket 0; one output list contains the entire input. Return `[[1]]`. |
| All values map to the same bucket (`[0, 0, 0], k = 3`) | Every node lands in bucket 0; buckets 1 and 2 stay empty. Return `[[0, 0, 0], [], []]`. |
| Single node, sparse output (`[4], k = 3`) | One tick routes node `4` to bucket 1 (since `4 % 3 = 1`); buckets 0 and 2 stay empty. Return `[[], [4], []]`. |
| Even/odd as a special case (`[1, 2, 3, 4], k = 2`) | `value % 2` recovers the even/odd split: evens in bucket 0, odds in bucket 1. Return `[[2, 4], [1, 3]]`. |
| Large `k` (`k > n`) | At most `n` buckets fill; the remaining `k - n` buckets stay empty. Time stays `O(n + k)`; space `O(k)`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


When the bucket count grows past 2, the split template scales by promoting `dummy`/`tail` from two scalars to two arrays. The routing core is still two lines (`tails[idx].next = current; tails[idx] = current`); only the indexing changed.

</details>
