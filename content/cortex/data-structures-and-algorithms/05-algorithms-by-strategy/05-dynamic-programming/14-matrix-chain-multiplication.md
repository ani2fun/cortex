---
title: "Matrix Chain Multiplication"
summary: "Multiplication is associative, but parenthesization order changes the scalar-multiply cost wildly. The canonical split-point interval DP: dp[i][j] = min over a split k of dp[i][k] + dp[k+1][j] + the dimension-product of joining the two halves. Same shape as boolean parenthesization, with min instead of sum."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/13-boolean-parenthesization
---

## Why It Exists

To multiply four matrices `A·B·C·D` you can group them any way you like — multiplication is *associative*, so every parenthesization yields the same result matrix. But the **cost** varies wildly. With `A` 10×30, `B` 30×5, `C` 5×60, `D` 60×8, grouping as `((A·B)·C)·D` costs 9300 scalar multiplications; `A·((B·C)·D)` costs 25800 — same answer, nearly 3× the work. For chains of ten-plus matrices the gap reaches 100× or worse. Every numerical-linear-algebra library and query planner that chains operations cares which order it picks.

This is **the** canonical split-point interval DP — the problem CLRS uses to teach the technique. It's the direct cousin of [boolean parenthesization](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/boolean-parenthesization): same `(i, j)` interval keyed on a split point `k`, fill by length. Two things differ — the aggregator is `min` (cheapest grouping) instead of a sum-of-products, and the per-split cost is a *dimension product* instead of a truth-table combination.

## See It Work

A matrix chain is given by a dimensions array `p`: matrix `i` is `p[i-1] × p[i]`. `dp[i][j]` is the minimum cost to multiply matrices `i..j`. For each split `k`, the two halves cost `dp[i][k]` and `dp[k+1][j]`, and joining their results — a `p[i-1] × p[k]` matrix by a `p[k] × p[j]` matrix — costs `p[i-1]·p[k]·p[j]`.

```python run viz=grid
import ast

def matrix_chain(p):
    n = len(p) - 1                                   # number of matrices
    dp = [[0] * (n + 1) for _ in range(n + 1)]       # 1-indexed; dp[i][i] = 0
    for length in range(2, n + 1):                   # interval DP: grow the chain
        for i in range(1, n - length + 2):
            j = i + length - 1
            dp[i][j] = min(dp[i][k] + dp[k + 1][j] + p[i - 1] * p[k] * p[j]
                           for k in range(i, j))     # try every split point k
    return dp[1][n]

p = ast.literal_eval(input())
print(matrix_chain(p))
```

```java run viz=grid
import java.util.*;

public class Main {
    static int matrixChain(int[] p) {
        int n = p.length - 1;
        int[][] dp = new int[n + 1][n + 1];
        for (int len = 2; len <= n; len++)
            for (int i = 1; i + len - 1 <= n; i++) {
                int j = i + len - 1;
                dp[i][j] = Integer.MAX_VALUE;
                for (int k = i; k < j; k++)          // split between matrix k and k+1
                    dp[i][j] = Math.min(dp[i][j], dp[i][k] + dp[k + 1][j] + p[i - 1] * p[k] * p[j]);
            }
        return dp[1][n];
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] p = parseIntArray(sc.nextLine());
        System.out.println(matrixChain(p));
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
    { "id": "p", "label": "p", "type": "int[]", "placeholder": "[10, 30, 5, 60, 8]" }
  ],
  "cases": [
    { "args": { "p": "[10, 30, 5, 60, 8]" }, "expected": "4300" },
    { "args": { "p": "[40, 20, 30, 10, 30]" }, "expected": "26000" },
    { "args": { "p": "[10, 100, 5, 50]" }, "expected": "7500" },
    { "args": { "p": "[5, 10, 3]" }, "expected": "150" }
  ]
}
```

Both print `4300` then `26000`. The `4300` is striking: the two hand-picked groupings in the intro cost 9300 and 25800, but the DP finds `((A·B)·(C·D))` at just 4300 — better than either obvious choice. Cost `O(n³)` time, `O(n²)` space.

## How It Works

The last multiplication in any grouping of `A_i … A_j` joins two already-multiplied blocks: `(A_i … A_k)` and `(A_{k+1} … A_j)`. Try every split point `k` and take the cheapest:

```d2
direction: right
cell: "dp[i][j]\n(min cost to multiply matrices i..j)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
split: "split at k:\nleft block (i..k)  +  right block (k+1..j)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
cost: "join cost = p[i-1] * p[k] * p[j]\n(rows of left) x (shared dim) x (cols of right)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
pick: "dp[i][j] = min over k of\n  dp[i][k] + dp[k+1][j] + join cost" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
cell -> split
split -> cost
cost -> pick
```

<p align="center"><strong>For each split <code>k</code>, add the two sub-chain costs plus the cost of the final multiply — <code>p[i-1]·p[k]·p[j]</code>, the dimensions of the two result matrices being joined. Minimise over all <code>k</code>.</strong></p>

Two anchors:

- **The split cost is a dimension product.** When you finally multiply the left block (a `p[i-1] × p[k]` matrix) by the right block (a `p[k] × p[j]` matrix), that single matrix multiply costs `p[i-1]·p[k]·p[j]` scalar operations. The inner dimensions `p[k]` must match (they do, by construction) and cancel; the outer dimensions plus the shared one give the cost. Getting this index triple right is the whole implementation — `p[i-1]`, `p[k]`, `p[j]`, never `p[i]`.
- **Same scaffold as boolean parenthesization, different aggregator.** Both are split-point interval DPs: an internal `for k`, fill by length, `O(n³)`. Boolean parenthesization *sums products* of True/False counts; matrix chain takes the *min* of costs. The interval-DP skeleton is identical — only what you compute per split, and how you aggregate over splits, changes. That's the unifying lesson of this whole DP arc.

> **Key takeaway.** Matrix-chain is the canonical **split-point interval DP**: `dp[i][j] = min over k of dp[i][k] + dp[k+1][j] + p[i-1]·p[k]·p[j]`, base `dp[i][i] = 0`, filled by length. The `min` aggregates over split points; the dimension product is the cost of the final join. `O(n³)` time, `O(n²)` space — versus the exponential (Catalan) number of parenthesizations brute force would try.

## Trace It

The reason this DP earns its `O(n³)` is that parenthesization order matters far more than intuition suggests. People expect a modest difference; the reality is order-of-magnitude.

**Predict before you run:** three matrices — `A` is 10×100, `B` is 100×5, `C` is 5×50. The two groupings `(A·B)·C` and `A·(B·C)` give the same result matrix. How far apart are their costs — a few percent, or much more?

```python run viz=grid
# A: 10x100, B: 100x5, C: 5x50
ab_then_c = 10 * 100 * 5 + 10 * 5 * 50               # (A·B) is 10x5, then x C
a_then_bc = 100 * 5 * 50 + 10 * 100 * 50             # (B·C) is 100x50, then A x it
print("(A·B)·C:", ab_then_c)
print("A·(B·C):", a_then_bc)

def matrix_chain(p):
    n = len(p) - 1
    dp = [[0] * (n + 1) for _ in range(n + 1)]
    for length in range(2, n + 1):
        for i in range(1, n - length + 2):
            j = i + length - 1
            dp[i][j] = min(dp[i][k] + dp[k + 1][j] + p[i - 1] * p[k] * p[j] for k in range(i, j))
    return dp[1][n]

print("DP optimum:", matrix_chain([10, 100, 5, 50]))
```

<details>
<summary><strong>Reveal</strong></summary>

`(A·B)·C` costs `7500`; `A·(B·C)` costs `75000` — a clean **10× gap** for just three matrices. The culprit is the fat intermediate: `B·C` first produces a `100×50` matrix (5000 entries, each costing 5 multiplies → 25000), and then multiplying `A` by that `100×50` block costs another `10·100·50 = 50000`. Grouping `(A·B)` first collapses to a tiny `10×5` intermediate, so both multiplies stay cheap. The DP picks `7500` without you reasoning about it — and the gap only widens with longer chains (100× and beyond), which is exactly why a smart query planner or BLAS-level chain optimiser pays for this DP rather than multiplying left-to-right. The lesson: associativity frees the *order*, and the order is worth optimising.

</details>

## Your Turn

Knowing the *cost* is half the answer; you usually want the *grouping*. Record the best split for each interval, then recurse to print the parenthesization — the same backtrace you used for LCS, edit distance, and palindrome partitioning, now over a 2D split table. Print the optimal parenthesization string on the first line, its cost on the second line.

```python run viz=grid
import ast

def matrix_chain_order(p):
    # Your code goes here — return (order_string, cost)
    return ("", 0)

p = ast.literal_eval(input())
order, cost = matrix_chain_order(p)
print(order)
print(cost)
```

```java run viz=grid
import java.util.*;

public class Main {
    static int[][] split;

    static String build(int i, int j) {
        // Your code goes here
        return "";
    }

    static String order(int[] p) {
        // Your code goes here
        return "";
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] p = parseIntArray(sc.nextLine());
        // Your code goes here — print order string then cost on separate lines
        System.out.println(order(p));
        System.out.println(0);
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
    { "id": "p", "label": "p", "type": "int[]", "placeholder": "[10, 100, 5, 50]" }
  ],
  "cases": [
    { "args": { "p": "[10, 100, 5, 50]" }, "expected": "((AB)C)\n7500" },
    { "args": { "p": "[10, 30, 5, 60, 8]" }, "expected": "((AB)(CD))\n4300" },
    { "args": { "p": "[5, 10, 3]" }, "expected": "(AB)\n150" }
  ]
}
```

<details>
<summary>Editorial</summary>

Both recover `((AB)C)` and `((AB)(CD))`. The second confirms the See It surprise: the optimal grouping of `A·B·C·D` is `((A·B)·(C·D))` at 4300 — not the left-to-right `((AB)C)D` (9300) most people reach for. Storing one `split[i][j]` per interval turns a DP that reports a *number* into one that reports the *plan*, the universal "recover the decision" technique for interval DP.

```python solution time=O(n³) space=O(n²)
import ast

def matrix_chain_order(p):
    n = len(p) - 1
    dp = [[0] * (n + 1) for _ in range(n + 1)]
    split = [[0] * (n + 1) for _ in range(n + 1)]    # split[i][j] = best k for interval (i, j)
    for length in range(2, n + 1):
        for i in range(1, n - length + 2):
            j = i + length - 1
            dp[i][j] = float('inf')
            for k in range(i, j):
                cost = dp[i][k] + dp[k + 1][j] + p[i - 1] * p[k] * p[j]
                if cost < dp[i][j]:
                    dp[i][j], split[i][j] = cost, k  # remember which split won
    def build(i, j):
        if i == j:
            return chr(ord('A') + i - 1)             # matrix names A, B, C, ...
        k = split[i][j]
        return "(" + build(i, k) + build(k + 1, j) + ")"
    return build(1, n), dp[1][n]

p = ast.literal_eval(input())
order, cost = matrix_chain_order(p)
print(order)
print(cost)
```

```java solution
import java.util.*;

public class Main {
    static int[][] split;

    static String build(int i, int j) {
        if (i == j) return String.valueOf((char) ('A' + i - 1));
        int k = split[i][j];
        return "(" + build(i, k) + build(k + 1, j) + ")";
    }

    static int order(int[] p) {
        int n = p.length - 1;
        int[][] dp = new int[n + 1][n + 1];
        split = new int[n + 1][n + 1];
        for (int len = 2; len <= n; len++)
            for (int i = 1; i + len - 1 <= n; i++) {
                int j = i + len - 1;
                dp[i][j] = Integer.MAX_VALUE;
                for (int k = i; k < j; k++) {
                    int cost = dp[i][k] + dp[k + 1][j] + p[i - 1] * p[k] * p[j];
                    if (cost < dp[i][j]) { dp[i][j] = cost; split[i][j] = k; }
                }
            }
        int n2 = p.length - 1;
        System.out.println(build(1, n2));
        return dp[1][n2];
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] p = parseIntArray(sc.nextLine());
        System.out.println(order(p));
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

## Reflect & Connect

- **The canonical split-point interval DP.** An internal split `k`, fill by length, `O(n³)`. Once you've written matrix chain, boolean parenthesization, "burst balloons," and optimal BST are the same skeleton with a different per-split cost.
- **The cost index triple is the whole game.** Joining `(i..k)` and `(k+1..j)` costs `p[i-1]·p[k]·p[j]` — rows of the left block, the shared dimension, cols of the right block. Mis-indexing (`p[i]` instead of `p[i-1]`) is the classic bug; trace a 2-matrix base case to lock it in.
- **It caps the interval-DP arc.** [LPS](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-palindromic-subsequence) matched two ends, [optimal game strategy](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/optimal-stratergy) took an end (max/min), [boolean parenthesization](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/boolean-parenthesization) split at an operator (sum), and matrix chain splits at `k` (min). One scaffold — interval `(i,j)`, fill by length — four aggregators.
- **Brute force is Catalan-many.** An `n`-matrix chain has the `(n-1)`-th Catalan number of parenthesizations — exponential. The DP collapses that to `O(n³)` because the `O(n²)` intervals overlap massively across groupings.
- **It's a real optimisation, not a toy.** BLAS-level chained products, tensor-contraction order, and SQL join ordering are matrix-chain in spirit: associativity frees the order, and choosing it well saves orders of magnitude.

## Recall

<details>
<summary><strong>Q:</strong> What is the matrix-chain recurrence?</summary>

**A:** `dp[i][j] = min over k in [i, j-1] of dp[i][k] + dp[k+1][j] + p[i-1]·p[k]·p[j]`, with `dp[i][i] = 0`, filled by increasing chain length. The answer is `dp[1][n]`; cost `O(n³)`.

</details>
<details>
<summary><strong>Q:</strong> What does the per-split term <code>p[i-1]·p[k]·p[j]</code> represent?</summary>

**A:** The cost of the *final* multiply: joining the left block (a `p[i-1] × p[k]` matrix) with the right block (a `p[k] × p[j]` matrix) takes `p[i-1]·p[k]·p[j]` scalar multiplications.

</details>
<details>
<summary><strong>Q:</strong> Why is the state 2D and filled by length?</summary>

**A:** A subproblem is a *contiguous sub-chain* `(i, j)`, so two indices are needed. `dp[i][j]` depends on shorter sub-chains (`dp[i][k]`, `dp[k+1][j]`), so all shorter lengths must be computed first — increasing-length order.

</details>
<details>
<summary><strong>Q:</strong> How does matrix chain relate to boolean parenthesization?</summary>

**A:** Both are split-point interval DPs with the same `(i,j)`-over-`k` scaffold and `O(n³)` cost. Boolean parenthesization sums products of True/False counts; matrix chain takes the min of `dp + dp + dimension product`. Different aggregator and per-split cost, identical structure.

</details>
<details>
<summary><strong>Q:</strong> How do you recover the optimal parenthesization, not just its cost?</summary>

**A:** Store `split[i][j]` = the `k` that achieved the minimum, then recurse: `build(i, j) = "(" + build(i, k) + build(k+1, j) + ")"`. The standard interval-DP backtrace.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15.2 — Matrix-Chain Multiplication: the recurrence, the `m`/`s` tables, and the `O(n³)` analysis this lesson follows.
- **GeeksforGeeks**, "Matrix Chain Multiplication" — the same DP with the dimension-array convention and reconstruction.
- The `4300`/`26000` costs, the `7500`-vs-`75000` parenthesization gap, and the `((AB)C)` / `((AB)(CD))` reconstructions above all come from the runnable blocks — re-run to verify.
