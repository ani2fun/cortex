---
title: "Toggle Kth Bit"
summary: "Given num and k, flip the kth bit — 0 becomes 1, 1 becomes 0."
prereqs:
  - 01-pattern-kth-bit/01-pattern
difficulty: easy
---

# Toggle Kth Bit

## The Problem

Given `num` and `k`, flip the kth bit — 0 becomes 1, 1 becomes 0.

```
Input:  num = 1, k = 1
Output: 0                        0001 → 0000

Input:  num = 3, k = 1
Output: 2                        0011 → 0010

Input:  num = 3, k = 2
Output: 1                        0011 → 0001
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



```python run viz=array
class Solution:
    def toggle_kth_bit(self, num: int, k: int) -> int:

        # To toggle the Kth bit of a number, we can use bitwise XOR
        # operation. We create a mask by left-shifting 1 by (k-1)
        # positions. Then, we perform bitwise XOR between the given
        # number and the mask to toggle the Kth bit.

        return num ^ (1 << (k - 1))


# Examples from the problem statement
print(Solution().toggle_kth_bit(1, 1))    # 0
print(Solution().toggle_kth_bit(3, 1))    # 2
print(Solution().toggle_kth_bit(3, 2))    # 1

# Edge cases
print(Solution().toggle_kth_bit(0, 1))    # 1
print(Solution().toggle_kth_bit(0, 3))    # 4
print(Solution().toggle_kth_bit(7, 1))    # 6
print(Solution().toggle_kth_bit(7, 3))    # 3
print(Solution().toggle_kth_bit(8, 4))    # 0
```

```java run viz=array
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
        // Examples from the problem statement
        System.out.println(new Solution().toggleKthBit(1, 1));    // 0
        System.out.println(new Solution().toggleKthBit(3, 1));    // 2
        System.out.println(new Solution().toggleKthBit(3, 2));    // 1

        // Edge cases
        System.out.println(new Solution().toggleKthBit(0, 1));    // 1
        System.out.println(new Solution().toggleKthBit(0, 3));    // 4
        System.out.println(new Solution().toggleKthBit(7, 1));    // 6
        System.out.println(new Solution().toggleKthBit(7, 3));    // 3
        System.out.println(new Solution().toggleKthBit(8, 4));    // 0
    }
}
```

</details>
