---
title: "Toggle Kth Bit"
summary: "Given num and k, flip the kth bit — 0 becomes 1, 1 becomes 0."
prereqs:
  - 01-pattern-kth-bit/01-pattern
difficulty: easy
kind: problem
topics: [kth-bit, bit-manipulation]
---

# Toggle Kth Bit

XOR a mask with a single `1` at position `k` against the number — that bit flips, and every other bit is unchanged.

## Problem Statement

Given `num` and `k`, flip the kth bit — 0 becomes 1, 1 becomes 0.

## Examples

**Example 1**
```
Input:  num = 1, k = 1
Output: 0
Explanation: 0001 → 0000 — bit 1 flipped from 1 to 0.
```

**Example 2**
```
Input:  num = 3, k = 1
Output: 2
Explanation: 0011 → 0010 — bit 1 flipped from 1 to 0.
```

**Example 3**
```
Input:  num = 3, k = 2
Output: 1
Explanation: 0011 → 0001 — bit 2 flipped from 1 to 0.
```

## Constraints

- `0 ≤ num ≤ 2^31 - 1` — non-negative 32-bit integer.
- `1 ≤ k ≤ 31` — 1-indexed bit position from the LSB.

```python run viz=array
class Solution:
    def toggle_kth_bit(self, num: int, k: int) -> int:
        # Your code goes here
        return 0


num = int(input())
k = int(input())
print(Solution().toggle_kth_bit(num, k))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int toggleKthBit(int num, int k) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().toggleKthBit(num, k));
    }
}
```

```testcases
{
  "args": [
    { "id": "num", "label": "num", "type": "int", "placeholder": "1" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "1" }
  ],
  "cases": [
    { "args": { "num": "1", "k": "1" }, "expected": "0" },
    { "args": { "num": "3", "k": "1" }, "expected": "2" },
    { "args": { "num": "3", "k": "2" }, "expected": "1" },
    { "args": { "num": "0", "k": "1" }, "expected": "1" },
    { "args": { "num": "7", "k": "3" }, "expected": "3" },
    { "args": { "num": "8", "k": "4" }, "expected": "0" }
  ]
}
```

<details>
<summary><h2>The Recurrence</h2></summary>


Build mask `1 << (k - 1)`. XOR with `num`: every bit of `num` is preserved except bit k, which is flipped.

```
result = num ^ (1 << (k - 1))
```

> *Pause. Why does XOR flip exactly one bit? Predict the truth-table reason.*

XOR's truth table: `0 ^ 0 = 0`, `1 ^ 0 = 1`, `0 ^ 1 = 1`, `1 ^ 1 = 0`. XORing with 0 (every mask bit except bit k) leaves the original bit untouched. XORing with 1 (only bit k of the mask) flips that bit. The "preserve under XOR with 0, flip under XOR with 1" property is the engine behind every XOR-based algorithm in this section.

</details>
<details>
<summary><h2>The Solution</h2></summary>

### The Solution

XOR `num` with the mask `1 << (k - 1)` — wherever the mask is `1` (only bit k), XOR flips the bit; everywhere else the mask is `0` and the original bit is preserved.

```python solution time=O(1) space=O(1)
class Solution:
    def toggle_kth_bit(self, num: int, k: int) -> int:

        # To toggle the Kth bit of a number, we can use bitwise XOR
        # operation. We create a mask by left-shifting 1 by (k-1)
        # positions. Then, we perform bitwise XOR between the given
        # number and the mask to toggle the Kth bit.

        return num ^ (1 << (k - 1))


num = int(input())
k = int(input())
print(Solution().toggle_kth_bit(num, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int toggleKthBit(int num, int k) {

            // To toggle the Kth bit of a number, we can use bitwise XOR
            // operation. We create a mask by left-shifting 1 by (k-1)
            // positions. Then, we perform bitwise XOR between the given
            // number and the mask to toggle the Kth bit.

            return num ^ (1 << (k - 1));
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().toggleKthBit(num, k));
    }
}
```

</details>
