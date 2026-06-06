---
title: "Calculate Factorial"
summary: "Given a non-negative integer n, return its factorial: n! = n × (n-1) × (n-2) × ... × 1. By convention 0! = 1. You must solve this recursively."
prereqs:
  - 05-pattern-head-recursion/01-pattern
difficulty: easy
---

# Calculate Factorial

The poster child of recursion. The combine function is multiplication; the base case is delicate.

---

## The Problem

Given a non-negative integer `n`, return its factorial: `n! = n × (n-1) × (n-2) × ... × 1`. By convention `0! = 1`. You **must** solve this recursively.

```
Input:  n = 7
Output: 5040
Explanation: 7 × 6 × 5 × 4 × 3 × 2 × 1 = 5040

Input:  n = 5
Output: 120
Explanation: 5 × 4 × 3 × 2 × 1 = 120

Input:  n = 0
Output: 1
Explanation: factorial(0) = 1 by convention.
```

---

<details>
<summary><h2>What Does "Factorial" Mean Recursively?</h2></summary>


Read the definition `n! = n × (n-1)!` and the recursion writes itself: the answer for `n` is `n` times the answer for `n-1`. The base case is the one that needs care: `0! = 1`, not `0`.

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#777777"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
flowchart LR
  EQ["fact(n) = n × fact(n−1)"]
  BASE["fact(0) = 1"]
  EQ -.->|"anchored by"| BASE
```

<p align="center"><strong>The recursive relation for factorial. <code>fact(0) = 1</code> is the multiplicative identity — picking <code>0</code> instead of <code>1</code> as the base would silently produce wrong answers.</strong></p>

> *Predict before reading on — what would happen if we used `fact(0) = 0` as the base case? What about `fact(1) = 1`? Are both valid?*

`fact(0) = 0` would propagate `0` all the way up: every multiplication on the ascent is `n × 0 = 0`. The whole computation collapses. **`fact(1) = 1`** is fine and is sometimes preferred — but you must guarantee `n >= 1` at the call site, or `fact(0)` will skip the base and recurse to `fact(-1)` and crash. `fact(0) = 1` is the safer, more general choice.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| # | Check | Answer |
|---|---|---|
| **Q1** | Smaller version? | **Yes** — `fact(n)` reduces to `fact(n-1)`. |
| **Q2** | Smaller answer first, then combine? | **Yes** — multiply `n × fact(n-1)` *after* the recursive call returns. |
| **Q3** | Known smallest answer? | **Yes** — `fact(0) = 1`. |

### Q1 — Why "n−1 is the smaller subproblem"?

`n!` is defined as `n × (n-1)!`. The right-hand side contains `(n-1)!` — that's the same problem on a smaller input. By induction, every step reduces by one until we hit `0`. ✓

### Q2 — Why "compute fact(n−1) before multiplying"?

Multiplication doesn't help us compute `fact(n)` until we know `fact(n-1)`. We can't multiply `n` by an unknown. So the recursive call must happen first — we need its return value before the combine step `n × _` can run. ✓

### Q3 — Why "fact(0) = 1, not 0"?

`1` is the multiplicative identity: anything times `1` is itself. Picking `1` keeps the multiplicative chain consistent. Picking `0` would zero out the whole answer (`5! = 5 × 4 × 3 × 2 × 1 × 0 = 0`).

</details>
<details>
<summary><h2>The Multiply-on-the-Way-Back Strategy (Visualised)</h2></summary>


```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#777777"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
flowchart TB
  F5["fact(5)"] --> F4["fact(4)"] --> F3["fact(3)"] --> F2["fact(2)"] --> F1["fact(1)"] --> F0["fact(0) = 1<br/>BASE"]
  F0 -.->|"returns 1"| F1u["fact(1): 1 × 1 = 1"]
  F1u -.->|"returns 1"| F2u["fact(2): 2 × 1 = 2"]
  F2u -.->|"returns 2"| F3u["fact(3): 3 × 2 = 6"]
  F3u -.->|"returns 6"| F4u["fact(4): 4 × 6 = 24"]
  F4u -.->|"returns 24"| F5u["fact(5): 5 × 24 = 120"]
```

<p align="center"><strong>The descent walks down to <code>fact(0)</code>; the ascent multiplies each frame's <code>n</code> by the smaller answer. The product accumulates from the bottom up.</strong></p>

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=array
class Solution:
    def factorial(self, n: int) -> int:

        # Base case: If n is 0, the factorial is 1
        if n == 0:
            return 1

        # Recursive call to calculate factorial of (n - 1)
        factorial_of_n_minus_1 = self.factorial(n - 1)

        # Multiply n with the factorial of (n - 1)
        return n * factorial_of_n_minus_1


# Examples from the problem statement
print(Solution().factorial(7))   # 5040
print(Solution().factorial(5))   # 120
print(Solution().factorial(0))   # 1

# Edge cases
print(Solution().factorial(1))   # 1
print(Solution().factorial(2))   # 2
print(Solution().factorial(10))  # 3628800
```

```java run viz=array
public class Main {
    static class Solution {
        public int factorial(int N) {

            // Base case: If N is 0, the factorial is 1
            if (N == 0) {
                return 1;
            }

            // Recursive call to calculate factorial of (N - 1)
            int factorialOfNMinus1 = factorial(N - 1);

            // Multiply N with the factorial of (N - 1)
            return N * factorialOfNMinus1;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().factorial(7));   // 5040
        System.out.println(new Solution().factorial(5));   // 120
        System.out.println(new Solution().factorial(0));   // 1

        // Edge cases
        System.out.println(new Solution().factorial(1));   // 1
        System.out.println(new Solution().factorial(2));   // 2
        System.out.println(new Solution().factorial(10));  // 3628800
    }
}
```


<details>
<summary><strong>Trace — n = 5</strong></summary>

```
Descent:
  fact(5) → fact(4) → fact(3) → fact(2) → fact(1) → fact(0)

Ascent (multiplications happen here):
  fact(0) returns 1
  fact(1) returns 1 × 1   = 1
  fact(2) returns 2 × 1   = 2
  fact(3) returns 3 × 2   = 6
  fact(4) returns 4 × 6   = 24
  fact(5) returns 5 × 24  = 120

Final answer: 120
```

The product is built from the base case up to the top. Same shape as Forward Sequence; different combine function.

</details>

### Complexity Analysis

| Resource | Cost | Why |
|---|---|---|
| **Time** | `O(n)` | One frame per integer; constant-time multiply per frame. |
| **Space** | `O(n)` | Recursion depth equals `n`. |

For very large `n` the *integer overflow* matters more than the recursion depth. `20!` already exceeds the range of a 64-bit `long long`. Use big-integer types (Python's native `int`, Java `BigInteger`) for `n > 20`.

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Identity base | `n = 0` | `1` | Multiplicative identity. |
| Smallest computational | `n = 1` | `1` | One multiplication: `1 × fact(0) = 1`. |
| Overflow at small `n` | `n = 21` | exceeds 64-bit `long long` | Switch to big-int or warn the caller. |
| Negative input | `n = -3` | undefined | Must be guarded at the entry point — `n < 0` would skip the base and recurse forever. |
| Large `n` | `n = 100_000` | overflow + stack overflow | Use big-int + iteration. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Factorial is the head-recursion template with `g = multiply` and a base case that has to be the multiplicative identity. Different combine, different base case, same scaffolding-unwind shape. The next problem replaces the combine with addition — but adds a twist: the input doesn't shrink by one each time. It shrinks by *a digit*.

</details>
