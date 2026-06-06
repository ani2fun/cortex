---
title: "K Most Frequent Elements"
summary: "Given an array arr and a positive integer k, return the K most frequent elements, in any order. Use a heap."
prereqs:
  - 05-pattern-comparator/01-pattern
difficulty: medium
---

# K most frequent elements

## Problem Statement

Given an array `arr` and a positive integer `k`, return the K most frequent elements, in any order. Use a heap.

### Example 1

> - **Input:** `arr = [1, 2, 2, 3, 3, 3]`, `k = 2`
> - **Output:** `[3, 2]`

### Example 2

> - **Input:** `arr = [1, 5, 6, 6]`, `k = 1`
> - **Output:** `[6]`

### Example 3

> - **Input:** `arr = [1]`, `k = 1`
> - **Output:** `[1]`

<details>
<summary><h2>The Strategy</h2></summary>


Two steps:

1. Count frequencies into a hash map (`O(N)` time, `O(U)` space where `U` is the number of unique values).
2. Run Top-K-largest *over the hash map's entries*, comparing by frequency. Use a min-heap of size K.

The comparator is "compare by frequency, ascending" (for a min-heap of size K → top is the smallest frequency, which is exactly the threshold we evict against).

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=array viz-root=min_heap
from typing import List
import heapq
from collections import Counter

class Entry:
    def __init__(self, value: int, frequency: int):
        self.value = value
        self.frequency = frequency

    def __lt__(self, other):

        # min heap based on frequency
        return self.frequency < other.frequency

class Solution:
    def k_most_frequent_elements(
        self, arr: List[int], k: int
    ) -> List[int]:

        # Count the frequency of each element in arr
        frequency = Counter(arr)

        # Create a min heap with custom objects
        min_heap: List[Entry] = []

        # Add the elements to the min heap
        for value, freq in frequency.items():
            heapq.heappush(min_heap, Entry(value, freq))

            # If the heap size exceeds k, remove the element with the
            # lowest frequency
            if len(min_heap) > k:
                heapq.heappop(min_heap)

        # Extract the elements from the heap and return as a list
        result: List[int] = []
        while min_heap:
            result.append(heapq.heappop(min_heap).value)

        # Return the result
        return result


# Examples from the problem statement
print(sorted(Solution().k_most_frequent_elements([1, 2, 2, 3, 3, 3], 2)))  # [2, 3]
print(Solution().k_most_frequent_elements([1, 5, 6, 6], 1))                # [6]
print(Solution().k_most_frequent_elements([1], 1))                          # [1]

# Edge cases
print(Solution().k_most_frequent_elements([7, 7, 7], 1))                    # [7] — all same
print(sorted(Solution().k_most_frequent_elements([1, 1, 2, 2], 2)))         # [1, 2] — tie in frequency
print(Solution().k_most_frequent_elements([4, 4, 4, 4, 4], 1))              # [4]
print(sorted(Solution().k_most_frequent_elements([1, 2, 3, 4, 5], 3)))      # 3 elements each freq=1
```

```java run viz=array viz-root=minHeap
import java.util.*;

public class Main {

    // Define a class to store the element and its frequency
    static class Entry {

        int value;
        int frequency;

        Entry(int value, int frequency) {
            this.value = value;
            this.frequency = frequency;
        }
    }

    // Comparator for the min heap
    static class CompareMinHeap implements Comparator<Entry> {
        public int compare(Entry a, Entry b) {

            // min heap based on frequency
            return a.frequency - b.frequency;
        }
    }

    static class Solution {
        public List<Integer> kMostFrequentElements(int[] arr, int k) {

            // Count the frequency of each element in arr
            Map<Integer, Integer> frequency = new HashMap<>();
            for (int num : arr) {
                frequency.put(num, frequency.getOrDefault(num, 0) + 1);
            }

            // Create a min heap with custom comparator
            PriorityQueue<Entry> minHeap = new PriorityQueue<>(
                new CompareMinHeap()
            );

            // Add elements to the min heap, maintaining only the top k
            frequency.forEach((key, value) -> {
                minHeap.add(new Entry(key, value));

                // If the heap size exceeds k, remove the element with the
                // lowest frequency
                if (minHeap.size() > k) {
                    minHeap.poll();
                }
            });

            // Extract the elements from the heap and return as a list
            List<Integer> result = new ArrayList<>();
            while (!minHeap.isEmpty()) {
                result.add(minHeap.poll().value);
            }

            // Return the result
            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        List<Integer> r1 = new Solution().kMostFrequentElements(new int[]{1, 2, 2, 3, 3, 3}, 2);
        Collections.sort(r1); System.out.println(r1);                             // [2, 3]

        System.out.println(new Solution().kMostFrequentElements(new int[]{1, 5, 6, 6}, 1));   // [6]
        System.out.println(new Solution().kMostFrequentElements(new int[]{1}, 1));             // [1]

        // Edge cases
        System.out.println(new Solution().kMostFrequentElements(new int[]{7, 7, 7}, 1));       // [7]

        List<Integer> r2 = new Solution().kMostFrequentElements(new int[]{1, 1, 2, 2}, 2);
        Collections.sort(r2); System.out.println(r2);                             // [1, 2]

        System.out.println(new Solution().kMostFrequentElements(new int[]{4, 4, 4, 4, 4}, 1)); // [4]

        List<Integer> r3 = new Solution().kMostFrequentElements(new int[]{1, 2, 3, 4, 5}, 3);
        Collections.sort(r3); System.out.println(r3);                             // 3 elements
    }
}
```

</details>
