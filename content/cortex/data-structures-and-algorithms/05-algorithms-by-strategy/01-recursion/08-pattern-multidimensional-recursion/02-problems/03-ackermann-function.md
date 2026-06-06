---
title: "Ackermann Function"
summary: "Compute A(m, n) defined as:"
prereqs:
  - 08-pattern-multidimensional-recursion/01-pattern
difficulty: medium
---

# Ackermann Function

The Ackermann function is famous in computability theory as an example of a function that *is* computable but *isn't* primitive recursive — meaning it can't be implemented with bounded `for` loops. Its recursion is so wild that even small inputs produce astronomical numbers.

---

## The Problem

Compute `A(m, n)` defined as:

- `A(0, n) = n + 1`
- `A(m, 0) = A(m - 1, 1)` for `m > 0`
- `A(m, n) = A(m - 1, A(m, n - 1))` for `m > 0, n > 0`

You **must** solve this recursively.

```
Input:  m = 2, n = 2
Output: 7

Input:  m = 1, n = 1
Output: 3

Input:  m = 0, n = 0
Output: 1
```

> **Warning:** `A(4, 2)` already has more digits than there are atoms in the observable universe. Don't try `m ≥ 4`. Even `A(3, 10)` will hang.

---

<details>
<summary><h2>What Makes Ackermann Wild</h2></summary>


Look at the recurrence's third case: `A(m, n) = A(m - 1, A(m, n - 1))`. The inner `A(m, n - 1)` is itself a recursive call whose result is the *second argument* of the outer call. The function recurses twice — but the second recursion's input depends on the first recursion's output. The state space isn't a tidy 2D grid; it's a wild spiral of dependencies.

Despite the chaos, the recurrence is genuinely multidimensional — it has two parameters that both shrink, just in unusual ways.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| # | Check | Answer |
|---|---|---|
| **Q1** | Two shrinkable parameters? | **Yes** — `m` and `n` both reduce. |
| **Q2** | Axis-aware reductions? | **Yes** — different cases reduce `m` only, or both, or `n` only. |
| **Q3** | Base cases on multiple boundaries? | **Yes** — `m = 0` is the primary base case. |

### Q1 — Why "both parameters shrink"?

Case 1 reduces `n` (when `m = 0`, return `n + 1` — base case). Case 2 reduces `m` (when `n = 0`, recurse on `(m-1, 1)`). Case 3 reduces both, with the inner recursion reducing `n`. The total state shrinks toward `(0, _)` over time. ✓

### Q2 — Why "axis-aware"?

The three cases handle different parts of the state space:
- `m = 0`: pure base, no recursion.
- `m > 0, n = 0`: recurse on `(m-1, 1)` — pure `m`-axis reduction.
- `m > 0, n > 0`: complex two-call structure that ultimately reduces both axes.

Each case targets a different region of the grid. ✓

### Q3 — Why "m = 0 is the primary boundary"?

Eventually every recursion path reaches a frame with `m = 0`, where the base case fires and returns `n + 1`. The other case (`n = 0` with `m > 0`) is a *recursive* case that bridges to the `m = 0` base. So strictly there's one base case, but the `n = 0` case is special enough to merit a separate code branch. ✓

</details>
<details>
<summary><h2>The Spiral State Space (Visualised)</h2></summary>


There's no clean 2D grid for Ackermann — the state explodes nonlinearly. But we can visualise the small values:

```d2
direction: down

table: "Ackermann's small values — A(m, n)" {
  grid-rows: 5
  grid-columns: 5
  grid-gap: 0
  h0:  ""        ; h1:  "n=0"  ; h2:  "n=1"  ; h3:  "n=2"  ; h4:  "n=3"
  r0n: "m=0"     ; c00: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}; c01: "2" {style.fill: "#fde68a"; style.stroke: "#d97706"}; c02: "3" {style.fill: "#fde68a"; style.stroke: "#d97706"}; c03: "4" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  r1n: "m=1"     ; c10: "2"; c11: "3"; c12: "4"; c13: "5"
  r2n: "m=2"     ; c20: "3"; c21: "5"; c22: "7"; c23: "9"
  r3n: "m=3"     ; c30: "5"; c31: "13"; c32: "29"; c33: "61"
}
```

<p align="center"><strong>Small Ackermann values. Yellow row = base cases (<code>m = 0</code> ⇒ <code>n + 1</code>). Notice how <code>m = 3</code> already grows non-trivially. <code>m = 4</code>'s first value is <code>2^65536 − 3</code>.</strong></p>

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=array
class Solution:
    def ackerman(self, m: int, n: int) -> int:

        # Base case: If m is 0, return n + 1
        if m == 0:
            return n + 1

        # If m is greater than 0 and n is 0, make a recursive call
        # with m - 1 and 1 as arguments
        if m > 0 and n == 0:
            return self.ackerman(m - 1, 1)

        # If both m and n are greater than 0, make a recursive call
        # with m - 1 and the result of ackerman(m, n - 1) as arguments
        if m > 0 and n > 0:
            return self.ackerman(m - 1, self.ackerman(m, n - 1))

        # If none of the above conditions are met, return 0
        return 0


# Examples from the problem statement
print(Solution().ackerman(2, 2))   # 7
print(Solution().ackerman(1, 1))   # 3
print(Solution().ackerman(0, 0))   # 1

# Edge cases
print(Solution().ackerman(0, 5))   # 6
print(Solution().ackerman(1, 0))   # 2
print(Solution().ackerman(2, 0))   # 3
print(Solution().ackerman(2, 3))   # 9
```

```java run viz=array
public class Main {
    static class Solution {
        public int ackerman(int M, int N) {

            // Base case: If M is 0, return N + 1
            if (M == 0) {
                return N + 1;
            }

            // If M is greater than 0 and N is 0, make a recursive call
            // with M - 1 and 1 as arguments
            if (M > 0 && N == 0) {
                return ackerman(M - 1, 1);
            }

            // If both M and N are greater than 0, make a recursive call
            // with M - 1 and the result of ackerman(M, N - 1) as arguments
            if (M > 0 && N > 0) {
                return ackerman(M - 1, ackerman(M, N - 1));
            }

            // If none of the above conditions are met, return 0
            return 0;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().ackerman(2, 2));   // 7
        System.out.println(new Solution().ackerman(1, 1));   // 3
        System.out.println(new Solution().ackerman(0, 0));   // 1

        // Edge cases
        System.out.println(new Solution().ackerman(0, 5));   // 6
        System.out.println(new Solution().ackerman(1, 0));   // 2
        System.out.println(new Solution().ackerman(2, 0));   // 3
        System.out.println(new Solution().ackerman(2, 3));   // 9
    }
}
```


<details>
<summary><strong>Trace — A(2, 2)</strong></summary>

```
A(2, 2)
  = A(1, A(2, 1))
  A(2, 1) = A(1, A(2, 0))
    A(2, 0) = A(1, 1)
      A(1, 1) = A(0, A(1, 0))
        A(1, 0) = A(0, 1) = 2
        A(0, 2) = 3
      A(1, 1) = 3
    A(2, 0) = 3
    A(1, 3) = A(0, A(1, 2))
      A(1, 2) = A(0, A(1, 1)) = A(0, 3) = 4
      A(0, 4) = 5
    A(1, 3) = 5
  A(2, 1) = 5
  A(1, 5) = A(0, A(1, 4))
    A(1, 4) = A(0, A(1, 3)) = A(0, 5) = 6
    A(0, 6) = 7
  A(1, 5) = 7
A(2, 2) = 7 ✓
```

The trace shows how nested calls compose — each `A(m, n - 1)`'s result becomes the *second* argument of the outer call. This is what makes Ackermann special and exhausting.

</details>

### Complexity Analysis

| Resource | Cost | Why |
|---|---|---|
| **Time** | beyond exponential — *non-elementary* | Cannot be bounded by any tower of exponentials in `(m, n)`. |
| **Space (stack)** | beyond linear | The call stack grows at the same wild rate as the result. |

Memoisation helps only modestly here — the values themselves grow so fast that storing them isn't enough to make `m ≥ 4` tractable. Ackermann is a counterexample to "you can always speed up a recursion with memoisation."

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| `m = 0` | `A(0, n)` | `n + 1` | Pure base case. |
| `n = 0` | `A(m, 0)` | `A(m - 1, 1)` | Bridges to base. |
| Small | `A(2, 3)` | `9` | Tractable. |
| Edge | `A(3, 5)` | `253` | Slow but possible. |
| Pathological | `A(4, 1)` | `65533` | Calls explode; will likely overflow stack on most systems. |
| Insane | `A(4, 2)` | `2^65536 − 3` | Larger than any number ever counted. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Ackermann is multidimensional recursion's wildest example. Its existence proves that not every recursion can be tamed into a `for` loop or made tractable by memoisation. It's also a nice contrast to the previous three problems, which *can* be tamed by 2D dynamic programming. The next problem brings us back to a memoisable, optimisation-flavoured 2D recursion — and is the canonical "this is why DP exists" lesson.

</details>
