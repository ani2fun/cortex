---
title: "Succeeding Superior Nodes"
summary: "Given the head of a singly-linked list, return an array where result[i] is the value of the next node strictly greater than node i (1-indexed). Use 0 if no such node exists."
prereqs:
  - 10-pattern-next-closest-occurrence/01-pattern
difficulty: medium
kind: problem
topics: [next-closest-occurrence, stack]
---

# Succeeding superior nodes

## Problem Statement

Given the head of a singly-linked list, return an array where `result[i]` is the value of the next node strictly greater than node `i` (1-indexed). Use `0` if no such node exists.

### Example 1
> -   **Input:** `head = [2, 1, 5]` → **Output:** `[5, 5, 0]`

### Example 2
> -   **Input:** `head = [2, 7, 4, 3, 5]` → **Output:** `[7, 0, 5, 5, 0]`

## Examples

**Example 1**
```
Input:  head = [2, 1, 5]
Output: [5, 5, 0]
Explanation: 2 and 1 both find 5 as their first strictly-greater successor → 5.
5 has no node after it → 0.
```

**Example 2**
```
Input:  head = [2, 7, 4, 3, 5]
Output: [7, 0, 5, 5, 0]
Explanation: 2 sees 7 → 7. 7 is followed by smaller values then 5 → 0.
4 and 3 both find 5 → 5. 5 ends the list → 0.
```

**Example 3**
```
Input:  head = [1, 2, 3, 4]
Output: [2, 3, 4, 0]
Explanation: Each node's next-greater is its immediate successor; the last node has none → 0.
```

**Example 4**
```
Input:  head = [4, 3, 2, 1]
Output: [0, 0, 0, 0]
Explanation: A strictly decreasing list — no node has a greater value after it.
```

```quiz
{
  "prompt": "head = [3, 1, 2]. What does succeeding_superior_nodes return?",
  "options": ["[0, 2, 0]", "[0, 2, 3]", "[0, 0, 0]", "[1, 2, 0]"],
  "answer": "[0, 2, 0]"
}
```

## Constraints

- `0 ≤ number of nodes ≤ 10 000`
- `0 ≤ node.val ≤ 10 000`

```python run
import ast
from typing import Optional, List

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

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

class Solution:
    def succeeding_superior_nodes(
        self, head: Optional[ListNode]
    ) -> List[int]:
        # Your code goes here — walk the list once with a (index, value)
        # stack; retroactively resolve dominated nodes as larger values appear.
        return []

head = build_list(ast.literal_eval(input()))
print(Solution().succeeding_superior_nodes(head))
```

```java run
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode next;
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 → 2 → 3 → null
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
    }

    static class Solution {
        public List<Integer> succeedingSuperiorNodes(ListNode head) {
            // Your code goes here — walk the list once with a (index, value)
            // stack; retroactively resolve dominated nodes as larger values appear.
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        System.out.println(new Solution().succeedingSuperiorNodes(head));
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
    { "id": "head", "label": "head", "type": "int[]", "placeholder": "[2, 1, 5]" }
  ],
  "cases": [
    { "args": { "head": "[2, 1, 5]" }, "expected": "[5, 5, 0]" },
    { "args": { "head": "[2, 7, 4, 3, 5]" }, "expected": "[7, 0, 5, 5, 0]" },
    { "args": { "head": "[1, 2, 3, 4]" }, "expected": "[2, 3, 4, 0]" },
    { "args": { "head": "[4, 3, 2, 1]" }, "expected": "[0, 0, 0, 0]" },
    { "args": { "head": "[1]" }, "expected": "[0]" },
    { "args": { "head": "[1, 2]" }, "expected": "[2, 0]" },
    { "args": { "head": "[2, 1]" }, "expected": "[0, 0]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **monotonic-stack** problem is the *next-greater* query, identical to *succeeding superior element* — only the data source is a linked list instead of an array. A list cannot be indexed in reverse cheaply, so this problem is the natural home for the left-to-right retroactive-resolution style.

The stack holds `(index, value)` pairs for nodes still *waiting* for a greater successor, with values strictly decreasing from bottom to top. As the pointer walks the list, each new node resolves every stacked node it exceeds: pop the pair, write the new value into `result` at the popped index. The new node's pair is then pushed to wait its turn.

The naive approach re-walks the tail for every node and breaks the time budget. For each node it scans forward until a greater value appears — `O(N²)` time, quadratic on a sorted-descending list. The single-pass stack visits each node once and resolves answers retroactively, so the work stays `O(N)`.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Succeeding Superior Nodes |
|---|---|
| **Q1.** Does each node need an answer drawn from the nodes *after* it? | **Yes** — the next-greater of node `i` ranges only over nodes reached later in the walk. |
| **Q2.** Is the answer the *closest* such node, not all of them? | **Yes** — the single nearest strictly-greater successor per node. |
| **Q3.** Is the comparison monotone — strictly greater or smaller? | **Yes** — a strict greater-than test drives every resolve-and-pop (decreasing stack). |
| **Q4.** Is the per-node work `O(1)` amortised? | **Yes** — each node is pushed once and popped at most once across the single pass. |

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Walk the list once with a pointer, resolving stacked nodes retroactively as larger values appear.

1. **Allocate the holders.** Create an empty `result` list, an empty `stack` of `(index, value)` pairs, and an `index` counter starting at `0`.
2. **Visit each node in order.** For the current node, first append `0` to `result` as its default answer.
3. **Resolve dominated nodes.** While the stack is non-empty and the current node's value `>` the top pair's value, pop the pair and set `result[poppedIndex]` to the current value.
4. **Push the current node.** Push `(index, currentValue)`, increment `index`, and advance the pointer to the next node.
5. **Return the result.** Any node still on the stack at the end of the walk keeps its `0` — no greater successor exists.

</details>
<details>
<summary><h2>Solution & Analysis</h2></summary>

```python solution time=O(N) space=O(N)
import ast
from typing import Optional, List

class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

def build_list(values):              # [1, 2, 3] → 1 → 2 → 3 → null
    head = None
    for v in reversed(values):
        head = ListNode(v, head)
    return head

# Struct to store index and value of each node
class NodeInfo:
    def __init__(self, index: int, value: int):
        self.index = index
        self.value = value

class Solution:
    def succeeding_superior_nodes(
        self, head: Optional[ListNode]
    ) -> List[int]:

        # Stores the next larger elements
        result: List[int] = []

        # Stores the elements in a stack along with their indices
        stack: List[NodeInfo] = []

        # Keeps track of the current index
        index = 0

        while head is not None:

            # Initialize the result for the current node as 0
            result.append(0)

            # While the stack is not empty and the value of the current
            # node is greater than the value of the element at the top
            # of the stack
            while stack and head.val > stack[-1].value:

                # Get the element at the top of the stack
                top = stack.pop()

                # Set the result at the index of the top element to the
                # value of the current node
                result[top.index] = head.val

            # Push the current node's index and value to the stack
            stack.append(NodeInfo(index, head.val))
            index += 1

            # Move to the next node
            head = head.next

        # Return the list containing the next larger elements
        return result

head = build_list(ast.literal_eval(input()))
print(Solution().succeeding_superior_nodes(head))
```

```java solution
import java.util.*;

public class Main {
    static class ListNode {
        int val; ListNode next;
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    static ListNode buildList(int[] values) {      // {1, 2, 3} → 1 → 2 → 3 → null
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
    }

    // Class to store index and value of each node
    static class NodeInfo {
        int index;
        int value;
        NodeInfo(int index, int value) {
            this.index = index;
            this.value = value;
        }
    }

    static class Solution {
        public List<Integer> succeedingSuperiorNodes(ListNode head) {

            // Stores the next larger elements
            List<Integer> result = new ArrayList<>();

            // Stores the elements in a stack along with their indices
            Stack<NodeInfo> stack = new Stack<>();

            // Keeps track of the current index
            int index = 0;

            while (head != null) {

                // Initialize the result for the current node as 0
                result.add(0);

                // While the stack is not empty and the value of the current
                // node is greater than the value of the element at the top
                // of the stack
                while (!stack.isEmpty() && head.val > stack.peek().value) {

                    // Get the element at the top of the stack
                    NodeInfo top = stack.pop();

                    // Set the result at the index of the top element to the
                    // value of the current node
                    result.set(top.index, head.val);
                }

                // Push the current node's index and value to the stack
                stack.push(new NodeInfo(index++, head.val));

                // Move to the next node
                head = head.next;
            }

            // Return the list containing the next larger elements
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ListNode head = buildList(parseIntArray(sc.nextLine()));
        System.out.println(new Solution().succeedingSuperiorNodes(head));
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

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `head = [2, 1, 5]`. The stack stores `(index, value)` pairs and stays strictly decreasing by value; resolve while the current value `>` the top's value:

```
idx=0 val=2   resolve none           stack=[(0,2)]          result=[0]
idx=1 val=1   1<2 (no resolve)        stack=[(0,2),(1,1)]    result=[0,0]
idx=2 val=5   1<5 → result[1]=5, pop (1,1)
              2<5 → result[0]=5, pop (0,2)
                                      stack=[(2,5)]          result=[5,5,0]

EOF: (2,5) left on the stack → result[2] stays 0
result = [5, 5, 0]
```

The result `[5, 5, 0]` matches the expected output. Node `5` is never resolved because nothing greater follows it.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | One pass over the `N` nodes; each node is pushed once and popped at most once. |
| Space | **O(N)** | The stack and the result list each hold up to `N` entries. |

The nested `while` is `O(N)` amortised, not `O(N²)`: total pushes plus pops across the whole walk never exceed `2N`.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty list | `head = []` | `[]` | No nodes, no answers. |
| Single node | `head = [1]` | `[0]` | One node has no successor — sentinel `0`. |
| Two ascending | `head = [1, 2]` | `[2, 0]` | `1` sees `2`; `2` ends the list → 0. |
| Two descending | `head = [2, 1]` | `[0, 0]` | Neither node has a greater successor. |
| Sorted ascending | `head = [1, 2, 3, 4]` | `[2, 3, 4, 0]` | Each node's next-greater is its immediate successor. |
| Sorted descending | `head = [4, 3, 2, 1]` | `[0, 0, 0, 0]` | No node ever finds a greater successor. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


What is new here is the data source — a linked list forces the left-to-right retroactive-resolution style, since you cannot scan a list backwards cheaply. Storing `(index, value)` pairs lets a later node write its value into an earlier node's slot the moment it dominates it; the sentinel is `0` rather than `-1` because the answer is 1-indexed node values.

</details>
