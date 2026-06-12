---
title: "K Rotations"
summary: "Given an array arr and a non-negative number k, rotate the array by k steps to the right — in-place, using O(1) extra space."
prereqs:
  - 06-pattern-two-pointers-subproblem/01-pattern
difficulty: easy
kind: problem
topics: [two-pointers, arrays]
---

# K Rotations (Right)

## The Problem

Given an array `arr` and a non-negative number `k`, rotate the array by `k` steps to the **right** — in-place, using O(1) extra space.

**Example 1:**
```
Input:  arr = [1, 2, 3, 4, 5], k = 3
Output: [3, 4, 5, 1, 2]

Rotate 1 step right  →  [5, 1, 2, 3, 4]
Rotate 2 steps right →  [4, 5, 1, 2, 3]
Rotate 3 steps right →  [3, 4, 5, 1, 2]
```

**Example 2:**
```
Input:  arr = [1, 2, 3, 4, 5], k = 5
Output: [1, 2, 3, 4, 5]    ← rotating n times returns the original
```

**Example 3:**
```
Input:  arr = [1, 2, 3, 4, 5], k = 0
Output: [1, 2, 3, 4, 5]
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "arr = [10, 20, 30, 40], k = 1",
  "options": ["[40, 10, 20, 30]", "[20, 30, 40, 10]", "[10, 20, 30, 40]", "[40, 30, 20, 10]"],
  "answer": "[40, 10, 20, 30]"
}
```

## Constraints

- `1 ≤ arr.length ≤ 10^5`
- `0 ≤ k ≤ 10^9`

```python run viz=array viz-root=arr
import ast
from typing import List

class Solution:
    def k_rotations(self, arr: List[int], k: int) -> None:
        # Your code goes here — normalise k %= n, then three in-place reversals:
        # reverse the whole array, reverse the first k, reverse the last n-k.
        pass

arr = ast.literal_eval(input())      # the test case's arr
k = int(input())                     # the test case's k
Solution().k_rotations(arr, k)
print(arr)
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public void kRotations(int[] arr, int k) {
            // Your code goes here — normalise k %= n, then three in-place reversals:
            // reverse the whole array, reverse the first k, reverse the last n-k.
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        new Solution().kRotations(arr, k);
        System.out.println(Arrays.toString(arr));
    }

    // "[1, 2, 3]" → {1, 2, 3} — reads the test case's arr
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 2, 3, 4, 5]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "arr": "[1, 2, 3, 4, 5]", "k": "3" }, "expected": "[3, 4, 5, 1, 2]" },
    { "args": { "arr": "[10, 20, 30, 40]", "k": "1" }, "expected": "[40, 10, 20, 30]" },
    { "args": { "arr": "[1, 2, 3, 4, 5]", "k": "5" }, "expected": "[1, 2, 3, 4, 5]" },
    { "args": { "arr": "[1, 2, 3, 4, 5]", "k": "7" }, "expected": "[4, 5, 1, 2, 3]" },
    { "args": { "arr": "[1, 2]", "k": "1" }, "expected": "[2, 1]" },
    { "args": { "arr": "[1]", "k": "0" }, "expected": "[1]" }
  ]
}
```

---

<details>
<summary><h2>Intuition</h2></summary>


Right rotation has a clean **structural property**: the array splits into two halves — `[HEAD | TAIL]`. `HEAD` is the first `n − k` elements; `TAIL` is the last `k`. A right rotation by `k` produces `[TAIL | HEAD]`, preserving the internal order of each half. That preserved internal order is what makes the three-reversal trick work; without it we would need actual element shifting. The pattern fit is the two-pointer **subproblem** decomposition. The whole rotation is the composition of three independent in-place reversals on segments of the same array.

The **pointer placement** follows directly. Each reversal subproblem is a textbook direct-application two-pointer: `left` at the segment's start, `right` at its end, swap, advance both inward until they meet. All three subproblems share the same `reverse(arr, start, end)` helper. The three `(start, end)` pairs are the whole array `[0..n-1]`, the first `k` indices `[0..k-1]`, and the last `n − k` indices `[k..n-1]`. The outer driver is a fixed sequence of three calls; the inner two-pointer pass does the actual work.

What **breaks if you reach for a single two-pointer pass**? Two pointers at the ends of the full array only swap mirror pairs. That produces a full reversal `[5, 4, 3, 2, 1]`, not the rotation `[3, 4, 5, 1, 2]`. The naive shift-one-position approach moves `n` elements per rotation and costs `O(n · k)` time — the worst case. The temp-array brute force costs `O(n)` time but `O(n)` extra space. Only the three-reversal decomposition delivers both `O(n)` time AND `O(1)` space.

</details>
<details>
<summary><h2>What Does "Rotate Right" Mean?</h2></summary>


One step to the right means the **last element wraps around to the front**, and every other element shifts one position to the right.

```d2
direction: right

a: "[1, 2, 3, 4, 5]"
b: "[5, 1, 2, 3, 4]"
c: "[4, 5, 1, 2, 3]"
d: "[3, 4, 5, 1, 2]" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}

a -> b: "rotate right 1"
b -> c: "rotate right 2"
c -> d: "rotate right 3"
```

<p align="center"><strong>Each right rotation brings the last element to the front — after k=3 steps, the last 3 elements form the new prefix.</strong></p>

After `k` right rotations, the **last `k` elements** end up at the front, and the **first `n-k` elements** shift to the back.

Think of it as splitting the array into two halves: `[HEAD | TAIL]` where `HEAD` = first `n-k` elements and `TAIL` = last `k` elements. The goal is to produce `[TAIL | HEAD]`.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for K Rotations |
|---|---|
| **Q1.** Can the problem be decomposed into smaller subproblems? | **Yes** — a right rotation by `k` is the composition of three independent in-place reversals: reverse the whole array `[0..n-1]`, reverse the first `k` indices `[0..k-1]`, reverse the last `n − k` indices `[k..n-1]`. |
| **Q2.** Can any subproblem be solved with two pointers (directly or via reduction)? | **Yes** — in-place segment reversal is the canonical two-pointer direct application: `left = start`, `right = end`, swap, advance both inward. |
| **Q3.** Does each subproblem have a decisive direction? | **Yes** — every reversal's inner loop advances `start` rightward and `end` leftward monotonically; no pointer ever backtracks, and the next swap is always determined by the current `(start, end)` pair. |
| **Q4.** Is the per-step inner work `O(1)`? | **Yes** — each step performs one swap of two array slots — three integer assignments in Java, one tuple swap in Python. |

### Q1 — Why "right rotation = 3 in-place reversals"?

This is the same fundamental insight from the previous lesson, adapted for the direction of rotation.

**Mental model:** Split the array into `[HEAD | TAIL]` (where `HEAD` = first `n-k`, `TAIL` = last `k`). A right rotation by `k` wants to produce `[TAIL | HEAD]`. Reversing the entire array first puts `TAIL` in front and `HEAD` at the back — but both halves are now scrambled internally. Reversing each scrambled half individually restores its original order.

**The three steps:**
1. Reverse the entire array: `[HEAD | TAIL]` → `[reverse(TAIL) | reverse(HEAD)]` — `TAIL` is now in front of `HEAD`, but both are scrambled
2. Reverse the first `k` elements: `reverse(TAIL)` becomes `TAIL` again → `[TAIL | reverse(HEAD)]`
3. Reverse the last `n-k` elements: `reverse(HEAD)` becomes `HEAD` again → `[TAIL | HEAD]` ✓

**Concrete check with `[1, 2, 3, 4, 5]`, k=3:**
- `HEAD = [1, 2]` (first 2), `TAIL = [3, 4, 5]` (last 3)
- Step 1 (reverse all `[0..4]`): `[5, 4, 3, 2, 1]` — `TAIL` reversed in front, `HEAD` reversed at back
- Step 2 (reverse first `k=3`, indices `[0..2]`): `[3, 4, 5, 2, 1]` — `TAIL` restored to its original order
- Step 3 (reverse indices `[3..4]`): `[3, 4, 5, 1, 2]` ✓ — `HEAD` restored to its original order

**What breaks if you skip either segment reversal?** After step 1 alone you have `[5, 4, 3, 2, 1]` — a full reversal, not a rotation. The two segment reversals are what un-scramble each half so the relative order inside `TAIL` and inside `HEAD` matches the original.

**How this differs from left rotation:** The previous lesson solved left rotation with the same "reverse all, then reverse each half" trick — only the segment boundaries change. For right rotation by `k`, the first segment is `k` long (it was `n-k` for left rotation), and the second is `n-k`. The same three-reversal mechanic, with the split point shifted.

### Q2 — Why "in-place reversal is the two-pointer flip"?

Reversing a segment `[start..end]` is the canonical two-pointer direct application:
- Place `left` at `start`, `right` at `end`
- Swap `arr[left]` and `arr[right]`, move `left` inward, `right` inward
- Stop when `left >= right`

**Why two pointers?** The reversal problem has perfect bilateral symmetry — element at position `i` from the left swaps with position `i` from the right. Two pointers exploit this directly, doing both jobs simultaneously from the outside in.

**What if you didn't use two pointers?** You'd need a temporary array to hold the reversed segment before writing it back — O(k) extra space per reversal. Two pointers do it with only two index variables — O(1) per reversal, O(1) total.

</details>
<details>
<summary><h2>Approach</h2></summary>

Three reversal calls, in order, with one normalisation up front.

1. **Normalise `k`.** Compute `k = k % n`. Any `k` larger than `n` collapses to its remainder (rotating an `n`-element array `n` times yields the original); a normalised `k` keeps the segment boundaries valid for steps 3 and 4.
2. **Reverse the entire array** `arr[0..n-1]`. Two pointers at indices `0` and `n − 1` swap pairs and converge to the middle. After this step, `TAIL` (the last `k` original elements) sits at the front in reversed order; `HEAD` (the first `n − k` original elements) sits at the back in reversed order.
3. **Reverse the first `k` elements** `arr[0..k-1]`. Two pointers at indices `0` and `k − 1` un-reverse `TAIL`, restoring its original order at the front of the array.
4. **Reverse the last `n − k` elements** `arr[k..n-1]`. Two pointers at indices `k` and `n − 1` un-reverse `HEAD`, restoring its original order at the back. The array now equals `[TAIL | HEAD]` — the right-rotated result.

Every step shares the same `reverse(arr, start, end)` helper. The whole algorithm is three calls to that helper plus one modulo.

</details>
<details>
<summary><h2>The Three-Reversal Strategy (Visualised)</h2></summary>


```d2
direction: right

s0: "Original:  [1, 2 | 3, 4, 5]  (HEAD=[1,2], TAIL=[3,4,5])" {
  grid-columns: 5
  grid-gap: 0
  a0: "1" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  a1: "2" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  a2: "3" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  a3: "4" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  a4: "5" {style.fill: "#fde68a"; style.stroke: "#d97706"}
}

s1: "Step 1: Reverse all [0..4]  →  [5, 4, 3, 2, 1]" {
  grid-columns: 5
  grid-gap: 0
  b0: "5" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  b1: "4" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  b2: "3" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  b3: "2" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  b4: "1" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
}

s2: "Step 2: Reverse first k=3 [0..2]  →  [3, 4, 5, 2, 1]" {
  grid-columns: 5
  grid-gap: 0
  c0: "3" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  c1: "4" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  c2: "5" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  c3: "2" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  c4: "1" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
}

s3: "Step 3: Reverse last n-k=2 [3..4]  →  [3, 4, 5, 1, 2]  ✓" {
  grid-columns: 5
  grid-gap: 0
  d0: "3" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
  d1: "4" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
  d2: "5" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
  d3: "1" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
  d4: "2" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
}

s0 -> s1: "reverse arr[0..4]"
s1 -> s2: "reverse arr[0..2]"
s2 -> s3: "reverse arr[3..4]"
```

<p align="center"><strong>Right rotation by k=3 via three in-place reversals — each reversal is an independent two-pointer subproblem.</strong></p>

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python solution time=O(n) space=O(1)
import ast
from typing import List

class Solution:
    def reverse(self, arr: List[int], start: int, end: int) -> None:
        while start < end:
            arr[start], arr[end] = arr[end], arr[start]
            start += 1
            end   -= 1

    def k_rotations(self, arr: List[int], k: int) -> None:
        n = len(arr)

        # Set k to be in the range of [0, n)
        k %= n

        # Reverse the entire array using two pointer method
        self.reverse(arr, 0, n - 1)

        # Reverse the first k elements using two pointer method
        self.reverse(arr, 0, k - 1)

        # Reverse the remaining elements using two pointer method
        self.reverse(arr, k, n - 1)


arr = ast.literal_eval(input())      # the test case's arr
k = int(input())                     # the test case's k
Solution().k_rotations(arr, k)
print(arr)
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private void reverse(int[] arr, int start, int end) {
            while (start < end) {
                int temp   = arr[start];
                arr[start] = arr[end];
                arr[end]   = temp;
                start++;
                end--;
            }
        }

        public void kRotations(int[] arr, int k) {
            int n = arr.length;

            // Set k to be in the range of [0, n)
            k %= n;

            // Reverse the entire array using two pointer method
            reverse(arr, 0, n - 1);

            // Reverse the first k elements using two pointer method
            reverse(arr, 0, k - 1);

            // Reverse the remaining elements using two pointer method
            reverse(arr, k, n - 1);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        new Solution().kRotations(arr, k);
        System.out.println(Arrays.toString(arr));
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
<details>
<summary><strong>Dry Run — arr = [1, 2, 3, 4, 5], k = 3</strong></summary>

```
n = 5,  k = 3 % 5 = 3
HEAD = arr[0..1] = [1, 2],  TAIL = arr[2..4] = [3, 4, 5]

━━━ Step 1: reverse(arr, 0, 4) — flip entire array [0..4] ━━━
  start=0 (1), end=4 (5)  │  0 < 4 → swap 1↔5  →  [5, 2, 3, 4, 1]  │  start=1, end=3
  start=1 (2), end=3 (4)  │  1 < 3 → swap 2↔4  →  [5, 4, 3, 2, 1]  │  start=2, end=2
  start=2 (3), end=2 (3)  │  2 == 2 → stop (middle element, no swap)
After step 1: [5, 4, 3, 2, 1]   (TAIL reversed in front, HEAD reversed at back)

━━━ Step 2: reverse(arr, 0, 2) — flip first k=3 elements [0..2] ━━━
  start=0 (5), end=2 (3)  │  0 < 2 → swap 5↔3  →  [3, 4, 5, 2, 1]  │  start=1, end=1
  start=1 (4), end=1 (4)  │  1 == 1 → stop (middle element, no swap)
After step 2: [3, 4, 5, 2, 1]   (TAIL is now back in original order)

━━━ Step 3: reverse(arr, 3, 4) — flip last n-k=2 elements [3..4] ━━━
  start=3 (2), end=4 (1)  │  3 < 4 → swap 2↔1  →  [3, 4, 5, 1, 2]  │  start=4, end=3
  start=4, end=3           │  4 > 3 → stop
After step 3: [3, 4, 5, 1, 2] ✓   (HEAD is now back in original order)

Result: [3, 4, 5, 1, 2]
The last 3 elements are now the prefix; original relative order within each group is preserved.
```

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | O(n) | Each element is touched exactly twice across all three reversals |
| **Space** | O(1) | Only `start` and `end` index variables — no auxiliary array |

### Edge Cases

| Case | What happens |
|---|---|
| `k = 0` | `k %= n` → 0, both segment reversals reduce to `reverse(arr, 0, -1)` and `reverse(arr, 0, n - 1)` — the first is a no-op (`start > end`), the third undoes the first whole-array reversal |
| `k = n` | `k %= n` → 0 — same path as `k = 0`, the array ends up unchanged |
| `k > n` | `k %= n` collapses it to the equivalent rotation (e.g. `k = 8`, `n = 5` → `k = 3`) |
| Single element (`n = 1`) | `k %= 1` → 0; every reversal call has `start >= end`, so no swaps occur |
| `k = 1` | `HEAD = arr[0..n-2]`, `TAIL = arr[n-1..n-1]` — just the last element wraps to the front |
| Two elements (`n = 2`), `k = 1` | First reversal flips the pair; the two single-element segment reversals are no-ops — result is the swap |

</details>
<details>
<summary><h2>Left vs. Right Rotation — The Relationship</h2></summary>


Right rotation by `k` is identical to left rotation by `n - k`. Both use the same three-reversal trick — reverse the entire array, then reverse each of the two halves to restore their internal order. Only the split point changes:

| Operation | Step 1 | Step 2 | Step 3 |
|---|---|---|---|
| **Left rotation by k** | Reverse all | Reverse first `n-k` | Reverse last `k` |
| **Right rotation by k** | Reverse all | Reverse first `k` | Reverse last `n-k` |

The core mechanic is identical — what changes is which segment you call HEAD and which you call TAIL, which in turn moves the split point used by steps 2 and 3.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Right rotation is **sequence-style** subproblem decomposition: a fixed three-call composition over a shared `reverse` helper, no outer loop over `n`. That structure is what keeps the complexity at `O(n)` time and `O(1)` space — every later problem in this section pays an extra `O(n)` factor for an outer loop over fixed elements.

</details>
