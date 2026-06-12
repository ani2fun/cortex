---
title: "Kth Smallest Element"
summary: "Given an integer array and k, return the kth smallest element using a size-k max-heap."
prereqs:
  - 04-pattern-top-k-elements/01-pattern
difficulty: easy
kind: problem
topics: [top-k-elements, heap]
---

# Kth smallest element

## Problem Statement

Given an array `arr` and a positive integer `k`, return the K-th smallest element. Use a heap.

## Examples

### Example 1

> - **Input:** `arr = [5, 4, 2, 8]`, `k = 2`
> - **Output:** `4`

### Example 2

> - **Input:** `arr = [1, 2, 3, 4, 5]`, `k = 5`
> - **Output:** `5`

### Example 3

> - **Input:** `arr = [7, 5, 9]`, `k = 3`
> - **Output:** `9`

## Constraints

- `1 ≤ k ≤ arr.length ≤ 10^4`
- `-10^4 ≤ arr[i] ≤ 10^4`

```python run
import ast
import heapq

class Solution:
    def kth_smallest_element(self, arr, k):
        # Your code goes here
        pass

arr = ast.literal_eval(input())
k = int(input())
print(Solution().kth_smallest_element(arr, k))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int kthSmallestElement(int[] arr, int k) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kthSmallestElement(arr, k));
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[5, 4, 2, 8]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "arr": "[5, 4, 2, 8]", "k": "2" }, "expected": "4" },
    { "args": { "arr": "[1, 2, 3, 4, 5]", "k": "5" }, "expected": "5" },
    { "args": { "arr": "[7, 5, 9]", "k": "3" }, "expected": "9" },
    { "args": { "arr": "[1]", "k": "1" }, "expected": "1" },
    { "args": { "arr": "[3, 1]", "k": "1" }, "expected": "1" },
    { "args": { "arr": "[3, 1]", "k": "2" }, "expected": "3" },
    { "args": { "arr": "[5, 5, 5, 5]", "k": "2" }, "expected": "5" },
    { "args": { "arr": "[10, 1, 2, 9, 3]", "k": "3" }, "expected": "3" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

The mirror image of the previous problem. To track the K *smallest* values, use a **max-heap** of size K — its top is the largest of the bottom-K, which (after we've seen everything) is the K-th smallest in the array.

</details>
<details>
<summary><h2>The Solution</h2></summary>

```python solution time=O(n log k) space=O(k)
import ast
import heapq

class Solution:
    def kth_smallest_element(self, arr, k):

        # Create a max heap to store the k smallest elements
        max_heap = []

        # Populate the max heap with the first k elements
        for i in range(k):

            # Push negative numbers to simulate max heap
            heapq.heappush(max_heap, -arr[i])

        # Compare the remaining elements with the top of the max heap
        for i in range(k, len(arr)):

            # Add the current element to the max heap
            heapq.heappush(max_heap, -arr[i])

            # If the heap size exceeds k, remove the largest element
            if len(max_heap) > k:
                heapq.heappop(max_heap)

        # The top of the max heap will be the kth smallest element
        return -max_heap[0]

arr = ast.literal_eval(input())
k = int(input())
print(Solution().kth_smallest_element(arr, k))
```

```java solution
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
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kthSmallestElement(arr, k));
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
