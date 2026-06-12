---
title: "K Range Sum"
summary: "Given an array and k1, k2, return the sum of elements in the inclusive range bounded by the k1-th largest and k2-th smallest values."
prereqs:
  - 04-pattern-top-k-elements/01-pattern
difficulty: medium
kind: problem
topics: [top-k-elements, heap]
---

# K range sum

## Problem Statement

Given an array `arr` and two positive integers `k1` and `k2`, return the **sum of all elements** whose values lie in the inclusive range bounded by the K1-th largest element and the K2-th smallest element.

## Examples

### Example 1

> - **Input:** `arr = [4, 2, 5, 1, 3, 6]`, `k1 = 4`, `k2 = 5`
> - **Output:** `12`
> - **Explanation:** K1 (4)-th largest is `3`; K2 (5)-th smallest is `5`. Sum of all elements in `[3, 5]` = `3 + 4 + 5 = 12`.

### Example 2

> - **Input:** `arr = [1, 2, 6, 4, 5]`, `k1 = 3`, `k2 = 4`
> - **Output:** `9`

### Example 3

> - **Input:** `arr = [1, 2, 3, 4, 5]`, `k1 = 1`, `k2 = 1`
> - **Output:** `15`

## Constraints

- `1 ≤ k1, k2 ≤ arr.length ≤ 10^4`
- `-10^4 ≤ arr[i] ≤ 10^4`

```python run
import ast
import heapq

class Solution:
    def k_range_sum(self, arr, k1, k2):
        # Your code goes here
        pass

arr = ast.literal_eval(input())
k1 = int(input())
k2 = int(input())
print(Solution().k_range_sum(arr, k1, k2))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int kRangeSum(int[] arr, int k1, int k2) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k1 = Integer.parseInt(sc.nextLine().trim());
        int k2 = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kRangeSum(arr, k1, k2));
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[4, 2, 5, 1, 3, 6]" },
    { "id": "k1", "label": "k1", "type": "int", "placeholder": "4" },
    { "id": "k2", "label": "k2", "type": "int", "placeholder": "5" }
  ],
  "cases": [
    { "args": { "arr": "[4, 2, 5, 1, 3, 6]", "k1": "4", "k2": "5" }, "expected": "12" },
    { "args": { "arr": "[1, 2, 6, 4, 5]", "k1": "3", "k2": "4" }, "expected": "9" },
    { "args": { "arr": "[1, 2, 3, 4, 5]", "k1": "1", "k2": "1" }, "expected": "15" },
    { "args": { "arr": "[3]", "k1": "1", "k2": "1" }, "expected": "3" },
    { "args": { "arr": "[5, 5, 5, 5, 5]", "k1": "2", "k2": "4" }, "expected": "25" },
    { "args": { "arr": "[1, 2, 3, 4, 5]", "k1": "2", "k2": "3" }, "expected": "7" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

This is *two* independent top-K queries followed by a linear sum:

1. Find the K1-th largest using a min-heap (the previous problem).
2. Find the K2-th smallest using a max-heap.
3. Walk the array once, summing elements whose value lies in `[min(a, b), max(a, b)]` (we use min/max to be defensive — the K1-th largest could in theory be larger or smaller than the K2-th smallest depending on inputs).

</details>
<details>
<summary><h2>The Solution</h2></summary>

```python solution time=O(n log k) space=O(k)
import ast
import heapq

class Solution:
    def kth_largest_element(self, arr, k):

        # Create a min heap to store the k largest elements
        min_heap = []

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

    def kth_smallest_element(self, arr, k):

        # Create a max heap to store the k smallest elements
        max_heap = []

        # Populate the max heap with the first k elements
        for i in range(k):
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

    def k_range_sum(self, arr, k1, k2):

        # Edge case: if the array is empty or k1 is greater than k2
        if not arr or k1 > k2 or k2 > len(arr):
            return 0

        # Find the k1-th largest element
        k1th_largest = self.kth_largest_element(arr, k1)

        # Find the k2-th smallest element
        k2th_smallest = self.kth_smallest_element(arr, k2)

        # Variable to store the sum of elements between the two bounds
        total = 0

        # Iterate through the array to calculate the sum of elements
        for num in arr:

            # Sum elements that are within the two bounds
            if num >= min(k1th_largest, k2th_smallest) and num <= max(
                k1th_largest, k2th_smallest
            ):
                total += num

        return total

arr = ast.literal_eval(input())
k1 = int(input())
k2 = int(input())
print(Solution().k_range_sum(arr, k1, k2))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private int kthLargestElement(int[] arr, int k) {

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

        private int kthSmallestElement(int[] arr, int k) {

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

        public int kRangeSum(int[] arr, int k1, int k2) {

            // Edge case: if the array is empty or k1 is greater than k2
            if (arr.length == 0 || k1 > arr.length || k2 > arr.length) {
                return 0;
            }

            // Find the k1-th largest element
            int k1thLargest = kthLargestElement(arr, k1);

            // Find the k2-th smallest element
            int k2thSmallest = kthSmallestElement(arr, k2);

            // Variable to store the sum of elements between the two bounds
            int sum = 0;

            // Iterate through the array to calculate the sum of elements
            for (int num : arr) {

                // Sum elements that are within the two bounds
                if (
                    num >= Math.min(k1thLargest, k2thSmallest) &&
                    num <= Math.max(k1thLargest, k2thSmallest)
                ) {
                    sum += num;
                }
            }

            return sum;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k1 = Integer.parseInt(sc.nextLine().trim());
        int k2 = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kRangeSum(arr, k1, k2));
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
