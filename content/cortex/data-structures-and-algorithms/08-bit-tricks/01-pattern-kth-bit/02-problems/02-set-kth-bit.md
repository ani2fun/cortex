---
title: "Set Kth Bit"
summary: "Given num and k, return num with its kth bit forced to 1. If the bit was already 1, the value is unchanged."
prereqs:
  - 01-pattern-kth-bit/01-pattern
difficulty: easy
kind: problem
topics: [kth-bit, bit-manipulation]
---

# Set Kth Bit

OR a mask that has a single `1` at position `k` with the number — that bit becomes `1`, and every other bit is untouched.

## Problem Statement

Given `num` and `k`, return `num` with its kth bit forced to 1. If the bit was already 1, the value is unchanged.

## Examples

**Example 1**
```
Input:  num = 0, k = 1
Output: 1
Explanation: 0000 → 0001 — bit 1 forced to 1.
```

**Example 2**
```
Input:  num = 2, k = 2
Output: 2
Explanation: 0010 — bit 2 already set; no change.
```

**Example 3**
```
Input:  num = 2, k = 1
Output: 3
Explanation: 0010 → 0011 — bit 1 forced to 1.
```

## Constraints

- `0 ≤ num ≤ 2^31 - 1` — non-negative 32-bit integer.
- `1 ≤ k ≤ 31` — 1-indexed bit position from the LSB.

```python run viz=array
class Solution:
    def set_kth_bit(self, num: int, k: int) -> int:
        # Your code goes here
        return 0


num = int(input())
k = int(input())
print(Solution().set_kth_bit(num, k))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int setKthBit(int num, int k) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().setKthBit(num, k));
    }
}
```

```testcases
{
  "args": [
    { "id": "num", "label": "num", "type": "int", "placeholder": "0" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "1" }
  ],
  "cases": [
    { "args": { "num": "0", "k": "1" }, "expected": "1" },
    { "args": { "num": "2", "k": "2" }, "expected": "2" },
    { "args": { "num": "2", "k": "1" }, "expected": "3" },
    { "args": { "num": "0", "k": "4" }, "expected": "8" },
    { "args": { "num": "7", "k": "4" }, "expected": "15" },
    { "args": { "num": "16", "k": "1" }, "expected": "17" }
  ]
}
```

<details>
<summary><h2>The Recurrence</h2></summary>


Build mask `1 << (k - 1)`. OR with `num`: every bit of `num` is preserved except bit k, which becomes 1 (because `1 OR anything = 1`).

```
result = num | (1 << (k - 1))
```

> *Pause. Why does OR preserve the other bits exactly? Predict.*

OR's truth table: `0 OR 0 = 0`, `1 OR 0 = 1`, `0 OR 1 = 1`, `1 OR 1 = 1`. ORing with 0 (every bit of the mask except bit k) leaves the original bit untouched. ORing with 1 (only bit k of the mask) forces that bit to 1 regardless of its prior value. Surgical edit.

</details>
<details>
<summary><h2>The Solution</h2></summary>

### The Solution

OR `num` with the mask `1 << (k - 1)` — a single instruction that forces bit k to 1 while leaving all others intact.

```python solution time=O(1) space=O(1)
class Solution:
    def set_kth_bit(self, num: int, k: int) -> int:

        # To turn on the Kth bit of a number, we can use bitwise OR
        # operation. We create a mask by left-shifting 1 by (k-1)
        # positions. Then, we perform bitwise OR between the given number
        # and the mask to turn on the Kth bit.
        return num | (1 << (k - 1))


num = int(input())
k = int(input())
print(Solution().set_kth_bit(num, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int setKthBit(int num, int k) {

            // To turn on the Kth bit of a number, we can use bitwise OR
            // operation. We create a mask by left-shifting 1 by (k-1)
            // positions. Then, we perform bitwise OR between the given
            // number and the mask to turn on the Kth bit.

            return num | (1 << (k - 1));
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int num = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().setKthBit(num, k));
    }
}
```

</details>
