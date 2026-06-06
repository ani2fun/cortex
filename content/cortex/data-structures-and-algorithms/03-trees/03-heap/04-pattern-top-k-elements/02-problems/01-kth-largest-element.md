---
title: "Kth Largest Element"
summary: "Given an array arr and a positive integer k, return the K-th largest element. Use a heap."
prereqs:
  - 04-pattern-top-k-elements/01-pattern
difficulty: easy
---

# Kth largest element

## Problem Statement

Given an array `arr` and a positive integer `k`, return the K-th largest element. Use a heap.

### Example 1

> - **Input:** `arr = [5, 4, 2, 8]`, `k = 2`
> - **Output:** `5`

### Example 2

> - **Input:** `arr = [1, 2, 3, 4, 5]`, `k = 5`
> - **Output:** `1`

### Example 3

> - **Input:** `arr = [7, 5, 9]`, `k = 3`
> - **Output:** `5`

<details>
<summary><h2>The Strategy</h2></summary>


This is the rawest form of the pattern. After running the loop, the heap's *top* (the smallest element of the K largest) **is** the K-th largest in the original array.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array viz-root=min_heap viz-kind=heap
from typing import List
import heapq

class Solution:
    def kth_largest_element(self, arr: List[int], k: int) -> int:

        # Create a min heap to store the k largest elements
        min_heap: List[int] = []

        # Populate the min heap with the first k elements
        for i in range(k):
            heapq.heappush(min_heap, arr[i])

        # Compare the remaining elements with the top of the min heap
        for i in range(k, len(arr)):

            # Add the current element to the min heap
            heapq.heappush(min_heap, arr[i])

            # If the heap size exceeds k, remove the smallest element
            if len(min_heap) > k:
                heapq.heappop(min_heap)

        # The top of the min heap will be the kth largest element
        return min_heap[0]


# Examples from the problem statement
print(Solution().kth_largest_element([5, 4, 2, 8], 2))       # 5
print(Solution().kth_largest_element([1, 2, 3, 4, 5], 5))    # 1
print(Solution().kth_largest_element([7, 5, 9], 3))           # 5

# Edge cases
print(Solution().kth_largest_element([1], 1))                 # 1 — single element
print(Solution().kth_largest_element([3, 1], 1))              # 3 — largest of two
print(Solution().kth_largest_element([3, 1], 2))              # 1 — smallest of two
print(Solution().kth_largest_element([5, 5, 5, 5], 2))        # 5 — all same
print(Solution().kth_largest_element([10, 1, 2, 9, 3], 3))    # 3 — sorted: [10,9,3,2,1]
```

```java run viz=array viz-root=minHeap viz-kind=heap
import java.util.*;

public class Main {
    static class Solution {
        public int kthLargestElement(int[] arr, int k) {

            // Create a min heap to store the k largest elements
            PriorityQueue<Integer> minHeap = new PriorityQueue<>();

            // Populate the min heap with the first k elements
            for (int i = 0; i < k; i++) {
                minHeap.add(arr[i]);
            }

            // Compare the remaining elements with the top of the min heap
            for (int i = k; i < arr.length; i++) {

                // Add the current element to the min heap
                minHeap.add(arr[i]);

                // If the heap size exceeds k, remove the smallest element
                if (minHeap.size() > k) {
                    minHeap.poll();
                }
            }

            // The top of the min heap will be the kth largest element
            return minHeap.peek();
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().kthLargestElement(new int[]{5, 4, 2, 8}, 2));       // 5
        System.out.println(new Solution().kthLargestElement(new int[]{1, 2, 3, 4, 5}, 5));    // 1
        System.out.println(new Solution().kthLargestElement(new int[]{7, 5, 9}, 3));           // 5

        // Edge cases
        System.out.println(new Solution().kthLargestElement(new int[]{1}, 1));                 // 1 — single element
        System.out.println(new Solution().kthLargestElement(new int[]{3, 1}, 1));              // 3 — largest of two
        System.out.println(new Solution().kthLargestElement(new int[]{3, 1}, 2));              // 1 — smallest of two
        System.out.println(new Solution().kthLargestElement(new int[]{5, 5, 5, 5}, 2));        // 5 — all same
        System.out.println(new Solution().kthLargestElement(new int[]{10, 1, 2, 9, 3}, 3));    // 3
    }
}
```


<details>
<summary><strong>Trace — arr = [5, 4, 2, 8], k = 2</strong></summary>

```
Step 1 │ push(5)         → heap = [5]                    (size 1 ≤ 2)
Step 2 │ push(4)         → heap = [4, 5]                 (size 2 ≤ 2)
Step 3 │ push(2)         → heap = [2, 5, 4]              (size 3 > 2 → pop 2)
                         → heap = [4, 5]
Step 4 │ push(8)         → heap = [4, 5, 8]              (size 3 > 2 → pop 4)
                         → heap = [5, 8]
Result: heap.top() = 5  ✓ (the 2nd largest)
```

</details>

</details>
