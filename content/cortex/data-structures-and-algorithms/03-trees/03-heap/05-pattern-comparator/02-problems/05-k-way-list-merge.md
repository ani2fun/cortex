---
title: "K-Way List Merge"
summary: "Given an array of k linked-list head nodes, each list sorted in ascending order, merge all lists into one sorted list and return its head."
prereqs:
  - 05-pattern-comparator/01-pattern
difficulty: hard
---

# K-way list merge

## Problem Statement

Given an array of `k` linked-list head nodes, each list sorted in ascending order, merge all lists into one sorted list and return its head.

### Example 1

> - **Input:** `lists = [[1, 4, 5], [1, 3, 4], [2, 6]]`
> - **Output:** `[1, 1, 2, 3, 4, 4, 5, 6]`

### Example 2

> - **Input:** `lists = []`
> - **Output:** `[]`

<details>
<summary><h2>The Strategy</h2></summary>


The textbook K-way merge: at every step, the next node of the merged list is the *globally smallest* among the heads of all unmerged lists. A min-heap of size K holds those heads. Pop the smallest, append to the output, push the *next* node of that list (if any). Done in `O(N log K)` total, where `N` is the total number of nodes.

The comparator is "compare list nodes by value, ascending".

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array viz-root=min_heap
from typing import List, Optional, Tuple
import heapq

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


def to_list(head):
    out = []
    while head is not None:
        out.append(head.val)
        head = head.next
    return out


class Solution:
    def k_way_list_merge(
        self, lists: List[Optional[ListNode]]
    ) -> Optional[ListNode]:

        # Create a new head and tail node to build the merged list
        dummy: ListNode = ListNode(0)
        tail: ListNode = dummy

        # Define the heap type as a list of tuples: (node value, list
        # index, ListNode)
        min_heap: List[Tuple[int, int, ListNode]] = []

        # Push the first node of each list into the heap
        for i, head in enumerate(lists):
            if head:
                heapq.heappush(min_heap, (head.val, i, head))

        # Extract the smallest item and add the next node from that list
        # to the heap
        while min_heap:
            val, i, node = heapq.heappop(min_heap)

            # Add the node to the merged list
            tail.next = node
            tail = tail.next

            # If there's a next node, push it to the heap
            if node.next:
                heapq.heappush(min_heap, (node.next.val, i, node.next))

        return dummy.next


# Examples from the problem statement
l1 = [from_list([1, 4, 5]), from_list([1, 3, 4]), from_list([2, 6])]
print(to_list(Solution().k_way_list_merge(l1)))   # [1, 1, 2, 3, 4, 4, 5, 6]

print(to_list(Solution().k_way_list_merge([])))   # []

# Edge cases
l2 = [from_list([1, 2, 3])]
print(to_list(Solution().k_way_list_merge(l2)))   # [1, 2, 3] — single list

l3 = [from_list([1]), from_list([2]), from_list([3])]
print(to_list(Solution().k_way_list_merge(l3)))   # [1, 2, 3] — single-node lists

l4 = [None, from_list([1, 2])]
print(to_list(Solution().k_way_list_merge(l4)))   # [1, 2] — one null list

l5 = [from_list([1, 1, 1]), from_list([1, 1])]
print(to_list(Solution().k_way_list_merge(l5)))   # [1, 1, 1, 1, 1] — all same
```

```java run viz=array viz-root=minHeap
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

    static java.util.List<Integer> toList(ListNode head) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        while (head != null) { out.add(head.val); head = head.next; }
        return out;
    }

    static class CompareMinHeap implements Comparator<ListNode> {
        public int compare(ListNode nodeA, ListNode nodeB) {

            // Custom comparison function used by the PriorityQueue.
            // It compares the values of the nodes and returns a negative
            // value if nodeA's value is less than nodeB's value, zero if
            // they are equal, and a positive value if nodeA's value is
            // greater than nodeB's value.
            return Integer.compare(nodeA.val, nodeB.val);
        }
    }

    static class Solution {
        public ListNode kWayListMerge(java.util.List<ListNode> lists) {

            // Create a PriorityQueue with ListNode as the type and use the
            // CompareNodes class as the comparator.
            PriorityQueue<ListNode> minHeap = new PriorityQueue<>(
                new CompareMinHeap()
            );

            // Push all non-null heads of the input lists into the priority
            // queue.
            for (ListNode head : lists) {
                if (head != null) minHeap.add(head);
            }

            // Create a dummy and tail pointers for building the merged list.
            ListNode dummy = new ListNode(0);
            ListNode tail = dummy;

            // Continue until the priority queue is empty.
            while (!minHeap.isEmpty()) {

                // Get the node with the smallest value from the priority
                // queue.
                ListNode node = minHeap.poll();

                // Add the node to the merged list.
                tail.next = node;
                tail = tail.next;

                // If the current node has a next node, push the next node
                // into the priority queue for further processing.
                if (node.next != null) {
                    minHeap.add(node.next);
                }
            }

            // Return the head of the merged list (excluding the dummy node).
            return dummy.next;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        java.util.List<ListNode> l1 = List.of(fromList(1, 4, 5), fromList(1, 3, 4), fromList(2, 6));
        System.out.println(toList(new Solution().kWayListMerge(l1)));   // [1, 1, 2, 3, 4, 4, 5, 6]

        System.out.println(toList(new Solution().kWayListMerge(List.of())));   // []

        // Edge cases
        java.util.List<ListNode> l2 = List.of(fromList(1, 2, 3));
        System.out.println(toList(new Solution().kWayListMerge(l2)));   // [1, 2, 3]

        java.util.List<ListNode> l3 = List.of(fromList(1), fromList(2), fromList(3));
        System.out.println(toList(new Solution().kWayListMerge(l3)));   // [1, 2, 3]

        java.util.List<ListNode> l4 = new ArrayList<>();
        l4.add(null); l4.add(fromList(1, 2));
        System.out.println(toList(new Solution().kWayListMerge(l4)));   // [1, 2]

        java.util.List<ListNode> l5 = List.of(fromList(1, 1, 1), fromList(1, 1));
        System.out.println(toList(new Solution().kWayListMerge(l5)));   // [1, 1, 1, 1, 1]
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


A comparator is the **bridge between a generic priority queue and any custom type with a total order**. Once you can plug a comparator in, every Top-K problem from lesson 3 generalises to records, structs, tree nodes, list nodes — anything with a defined ordering.

Three patterns to take with you:

1. **Heap of records, ordered by score.** Word + frequency, point + distance, pair + sum, list-node + value. The heap holds *records*, the comparator orders by the *score field*.
2. **K-way merge with a heap of size K.** When you need the global minimum across K sorted streams, a heap of size K with one head per stream gives it to you in O(log K) per pop. K-way merge, K-sorted ranges, K-way list merge — all the same skeleton.
3. **Tiebreakers in language-specific ways.** Most heap libraries can't compare arbitrary types directly (Python tuples, Rust `Box`); inserting a unique counter or a list index as a tiebreaker is a common idiom that prevents the comparator from ever needing to look at non-comparable fields.

The next and final lesson zooms back out: **design** problems that combine multiple heaps, or a heap with another data structure, to build something larger — finding the running median, tracking K-sized windowed maxima, deferred-decision priority queues. The comparator pattern is the toolbox for those designs.

</details>
