---
title: "Kth Smallest Element"
summary: "Given an array arr and a positive integer k, return the K-th smallest element. Use a heap."
prereqs:
  - 04-pattern-top-k-elements/01-pattern
difficulty: easy
---

# Kth smallest element

## Problem Statement

Given an array `arr` and a positive integer `k`, return the K-th smallest element. Use a heap.

### Example 1

> - **Input:** `arr = [5, 4, 2, 8]`, `k = 2`
> - **Output:** `4`

### Example 2

> - **Input:** `arr = [1, 2, 3, 4, 5]`, `k = 5`
> - **Output:** `5`

### Example 3

> - **Input:** `arr = [7, 5, 9]`, `k = 3`
> - **Output:** `9`

<details>
<summary><h2>The Strategy</h2></summary>


The mirror image of the previous problem. To track the K *smallest* values, use a **max-heap** of size K — its top is the largest of the bottom-K, which (after we've seen everything) is the K-th smallest in the array.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array viz-root=max_heap viz-kind=heap
from typing import List
import heapq

class Solution:
    def kth_smallest_element(self, arr: List[int], k: int) -> int:

        # Create a max heap to store the k smallest elements
        max_heap: List[int] = []

        # Populate the max heap with the first k elements
        for i in range(k):

            # Push negative numbers to simulate max heap
            heapq.heappush(
                max_heap, -arr[i]
            )

        # Compare the remaining elements with the top of the max heap
        for i in range(k, len(arr)):

            # Add the current element to the max heap
            heapq.heappush(max_heap, -arr[i])

            # If the heap size exceeds k, remove the largest element
            if len(max_heap) > k:
                heapq.heappop(max_heap)

        # The top of the max heap will be the kth smallest element
        return -max_heap[0]


# Examples from the problem statement
print(Solution().kth_smallest_element([5, 4, 2, 8], 2))       # 4
print(Solution().kth_smallest_element([1, 2, 3, 4, 5], 5))    # 5
print(Solution().kth_smallest_element([7, 5, 9], 3))           # 9

# Edge cases
print(Solution().kth_smallest_element([1], 1))                 # 1 — single element
print(Solution().kth_smallest_element([3, 1], 1))              # 1 — smallest of two
print(Solution().kth_smallest_element([3, 1], 2))              # 3 — largest of two
print(Solution().kth_smallest_element([5, 5, 5, 5], 2))        # 5 — all same
print(Solution().kth_smallest_element([10, 1, 2, 9, 3], 3))    # 3 — sorted: [1,2,3,9,10]
```

```java run viz=array viz-root=maxHeap viz-kind=heap
import java.util.*;

public class Main {
    static class Solution {
        public int kthSmallestElement(int[] arr, int k) {

            // Create a max heap to store the k smallest elements
            PriorityQueue<Integer> maxHeap = new PriorityQueue<>(
                Collections.reverseOrder()
            );

            // Populate the max heap with the first k elements
            for (int i = 0; i < k; ++i) {
                maxHeap.add(arr[i]);
            }

            // Compare the remaining elements with the top of the max heap
            for (int i = k; i < arr.length; i++) {

                // Add the current element to the max heap
                maxHeap.add(arr[i]);

                // If the heap size exceeds k, remove the largest element
                if (maxHeap.size() > k) {
                    maxHeap.poll();
                }
            }

            // The top of the max heap will be the kth smallest element
            return maxHeap.peek();
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().kthSmallestElement(new int[]{5, 4, 2, 8}, 2));       // 4
        System.out.println(new Solution().kthSmallestElement(new int[]{1, 2, 3, 4, 5}, 5));    // 5
        System.out.println(new Solution().kthSmallestElement(new int[]{7, 5, 9}, 3));           // 9

        // Edge cases
        System.out.println(new Solution().kthSmallestElement(new int[]{1}, 1));                 // 1 — single element
        System.out.println(new Solution().kthSmallestElement(new int[]{3, 1}, 1));              // 1 — smallest of two
        System.out.println(new Solution().kthSmallestElement(new int[]{3, 1}, 2));              // 3 — largest of two
        System.out.println(new Solution().kthSmallestElement(new int[]{5, 5, 5, 5}, 2));        // 5 — all same
        System.out.println(new Solution().kthSmallestElement(new int[]{10, 1, 2, 9, 3}, 3));    // 3
    }
}
```

</details>
