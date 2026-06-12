---
title: "Kth Largest Element"
summary: "Given an integer array and k, return the kth largest element using a size-k min-heap."
prereqs:
  - 04-pattern-top-k-elements/01-pattern
difficulty: easy
kind: problem
topics: [top-k-elements, heap]
---

# Kth largest element

## Problem Statement

Given an array `arr` and a positive integer `k`, return the K-th largest element. Use a heap.

## Examples

### Example 1

> - **Input:** `arr = [5, 4, 2, 8]`, `k = 2`
> - **Output:** `5`

### Example 2

> - **Input:** `arr = [1, 2, 3, 4, 5]`, `k = 5`
> - **Output:** `1`

### Example 3

> - **Input:** `arr = [7, 5, 9]`, `k = 3`
> - **Output:** `5`

## Constraints

- `1 ≤ k ≤ arr.length ≤ 10^4`
- `-10^4 ≤ arr[i] ≤ 10^4`

```python run
import ast
import heapq

class Solution:
    def kth_largest_element(self, arr, k):
        # Your code goes here
        pass

arr = ast.literal_eval(input())
k = int(input())
print(Solution().kth_largest_element(arr, k))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int kthLargestElement(int[] arr, int k) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kthLargestElement(arr, k));
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
    { "args": { "arr": "[5, 4, 2, 8]", "k": "2" }, "expected": "5" },
    { "args": { "arr": "[1, 2, 3, 4, 5]", "k": "5" }, "expected": "1" },
    { "args": { "arr": "[7, 5, 9]", "k": "3" }, "expected": "5" },
    { "args": { "arr": "[1]", "k": "1" }, "expected": "1" },
    { "args": { "arr": "[3, 1]", "k": "1" }, "expected": "3" },
    { "args": { "arr": "[3, 1]", "k": "2" }, "expected": "1" },
    { "args": { "arr": "[5, 5, 5, 5]", "k": "2" }, "expected": "5" },
    { "args": { "arr": "[10, 1, 2, 9, 3]", "k": "3" }, "expected": "3" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

This is the rawest form of the pattern. After running the loop, the heap's *top* (the smallest element of the K largest) **is** the K-th largest in the original array.

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

arr = ast.literal_eval(input())
k = int(input())
print(Solution().kth_largest_element(arr, k))
```

```java solution
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
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kthLargestElement(arr, k));
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
