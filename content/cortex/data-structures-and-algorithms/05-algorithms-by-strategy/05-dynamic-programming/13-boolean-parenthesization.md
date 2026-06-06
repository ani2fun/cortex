---
title: "Boolean Parenthesization"
summary: "Count the parenthesizations of a boolean expression that evaluate True. A split-point interval DP — try every operator as the LAST one applied — that needs TWO tables (True-counts and False-counts), because combining with | and ^ depends on how each side can be False as well as True."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/06-longest-palindromic-subsequence
---

## Why It Exists

A boolean expression like `T ^ F & T` has no value until you decide which operator binds first: `(T ^ F) & T` versus `T ^ (F & T)`. For some inputs the two groupings *disagree*, so the real question is: **how many parenthesizations make the whole expression evaluate to `True`?** Compilers reasoning about ambiguous grammars and tools counting satisfying assignments hit exactly this.

The shape is new. Every interval DP so far either matched two ends ([palindromes](/cortex/data-structures-and-algorithms/algorithms-by-strategy-dynamic-programming-longest-palindromic-subsequence)) or took an end ([game strategy](/cortex/data-structures-and-algorithms/algorithms-by-strategy-dynamic-programming-optimal-stratergy)). Here the choice is *which operator is applied last* — an internal **split point** `k` inside the range, the same structure as matrix-chain multiplication. And it needs **two** tables, not one: to know how many ways `left | right` is `True`, you need to know how many ways each side is `False`, not just `True`.

## See It Work

Operands (`T`/`F`) sit at even positions, operators (`&`, `|`, `^`) at odd. `T[i][j]` / `F[i][j]` count the parenthesizations of operands `i..j` that evaluate True / False. For each operator `k` as the *last* one applied, combine the two sides with that operator's truth table — a sum of products over how each side lands.

```python run viz=array
def count_true(expr):
    symbols, ops = expr[::2], expr[1::2]             # operands at even idx, operators at odd
    n = len(symbols)
    T = [[0] * n for _ in range(n)]
    F = [[0] * n for _ in range(n)]
    for i in range(n):
        T[i][i] = 1 if symbols[i] == 'T' else 0      # base: a single operand
        F[i][i] = 1 if symbols[i] == 'F' else 0
    for length in range(2, n + 1):                   # interval DP: grow by length
        for i in range(n - length + 1):
            j = i + length - 1
            for k in range(i, j):                    # ops[k] is the LAST operator: split [i..k] | [k+1..j]
                lt, lf, rt, rf = T[i][k], F[i][k], T[k + 1][j], F[k + 1][j]
                total = (lt + lf) * (rt + rf)        # all left x right pairings
                if ops[k] == '&':
                    T[i][j] += lt * rt               # AND true iff both true
                    F[i][j] += total - lt * rt
                elif ops[k] == '|':
                    T[i][j] += total - lf * rf       # OR true unless both false
                    F[i][j] += lf * rf
                else:                                # '^'
                    T[i][j] += lt * rf + lf * rt     # XOR true iff sides differ
                    F[i][j] += lt * rt + lf * rf
    return T[0][n - 1]

print(count_true("T^F&T"))     # 2
print(count_true("T|T&F^T"))   # 4
```

```java run viz=array
public class Main {
    static int countTrue(String expr) {
        StringBuilder sym = new StringBuilder(), ops = new StringBuilder();
        for (int i = 0; i < expr.length(); i++)
            if (i % 2 == 0) sym.append(expr.charAt(i)); else ops.append(expr.charAt(i));
        int n = sym.length();
        long[][] T = new long[n][n], F = new long[n][n];
        for (int i = 0; i < n; i++) {
            T[i][i] = sym.charAt(i) == 'T' ? 1 : 0;
            F[i][i] = sym.charAt(i) == 'F' ? 1 : 0;
        }
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                for (int k = i; k < j; k++) {        // last operator splits the range
                    long lt = T[i][k], lf = F[i][k], rt = T[k + 1][j], rf = F[k + 1][j];
                    long total = (lt + lf) * (rt + rf);
                    char op = ops.charAt(k);
                    if (op == '&')      { T[i][j] += lt * rt;          F[i][j] += total - lt * rt; }
                    else if (op == '|') { T[i][j] += total - lf * rf;  F[i][j] += lf * rf; }
                    else                { T[i][j] += lt * rf + lf * rt; F[i][j] += lt * rt + lf * rf; }
                }
            }
        return (int) T[0][n - 1];
    }
    public static void main(String[] args) {
        System.out.println(countTrue("T^F&T"));     // 2
        System.out.println(countTrue("T|T&F^T"));   // 4
    }
}
```

Both print `2` then `4`. `T^F&T` has two operand-orderings and both happen to evaluate True; `T|T&F^T` has five parenthesizations, four of which are True. Cost `O(n³)` — `O(n²)` ranges times `O(n)` split points.

## How It Works

Pick the operator applied last; everything left of it is one fully-parenthesized sub-expression, everything right is another. Sum over all split points, and for each, combine by the operator's truth table:

```d2
direction: right
split: "last operator ops[k] splits operands [i..j]\ninto LEFT [i..k]  and  RIGHT [k+1..j]" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
need: "each side has TWO counts:\nT (#ways true), F (#ways false)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
andOp: "& : T += Tl*Tr\n      F += total - Tl*Tr" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
orOp:  "| : T += total - Fl*Fr\n      F += Fl*Fr" {style.fill: "#fde68a"; style.stroke: "#d97706"}
xorOp: "^ : T += Tl*Fr + Fl*Tr\n      F += Tl*Tr + Fl*Fr" {style.fill: "#fbcfe8"; style.stroke: "#db2777"}
split -> need
need -> andOp
need -> orOp
need -> xorOp
```

<p align="center"><strong>For each split point, the two sides combine by a <em>sum of products</em> over their True/False counts. <code>total = (Tl+Fl)(Tr+Fr)</code> is every pairing; each operator carves out which pairings land True.</strong></p>

Two ideas define this lesson:

- **The split point is an operator, not an end.** Where palindromes peel a character off each end, this picks the *last operator to evaluate* and recurses on the two sub-expressions it separates. That internal `for k` loop is the signature of split-point interval DP — shared with [matrix-chain multiplication](/cortex/data-structures-and-algorithms/algorithms-by-strategy-dynamic-programming-matrix-chain-multiplication) — and it's why the cost is `O(n³)`.
- **You need both T *and* F tables.** This is the crux. `left | right` is True in three of four cases — the only False case is both-False — so counting its True-ways requires `Fl` and `Fr`. `left ^ right` is True exactly when the sides differ, mixing `Tl·Fr` and `Fl·Tr`. You cannot compute the True-counts from True-counts alone; the False-counts are load-bearing. ([Trace It](#trace-it) proves it.)

> **Key takeaway.** Boolean parenthesization is a **split-point interval DP with two tables**: try each operator `k` as the last applied, and combine `T`/`F` counts by a sum of products (`&`: `Tl·Tr`; `|`: `total − Fl·Fr`; `^`: `Tl·Fr + Fl·Tr`). Fill by length; answer `T[0][n-1]`; cost `O(n³)`. The False-table is mandatory because `|` and `^` depend on it.

## Trace It

It's tempting to track only the True-counts — the answer is a True-count, after all — and to reuse AND's clean rule (`Tl·Tr`) for the other operators. That works for AND. It quietly breaks for OR.

**Predict before you run:** with OR's True-count computed as `Tl·Tr` (mirroring AND), how many ways does `"F|T"` evaluate True — the correct `1`, or something else?

```python run viz=array
def count_true(expr, or_bug=False):
    symbols, ops = expr[::2], expr[1::2]
    n = len(symbols)
    T = [[0] * n for _ in range(n)]
    F = [[0] * n for _ in range(n)]
    for i in range(n):
        T[i][i] = 1 if symbols[i] == 'T' else 0
        F[i][i] = 1 if symbols[i] == 'F' else 0
    for length in range(2, n + 1):
        for i in range(n - length + 1):
            j = i + length - 1
            for k in range(i, j):
                lt, lf, rt, rf = T[i][k], F[i][k], T[k + 1][j], F[k + 1][j]
                total = (lt + lf) * (rt + rf)
                if ops[k] == '&':
                    T[i][j] += lt * rt
                    F[i][j] += total - lt * rt
                elif ops[k] == '|':
                    T[i][j] += lt * rt if or_bug else total - lf * rf   # buggy: AND's rule for OR
                    F[i][j] += lf * rf
                else:
                    T[i][j] += lt * rf + lf * rt
                    F[i][j] += lt * rt + lf * rf
    return T[0][n - 1]

print("correct F|T:", count_true("F|T"))
print("buggy   F|T:", count_true("F|T", or_bug=True))
```

<details>
<summary><strong>Reveal</strong></summary>

Correct is `1`; the buggy version returns `0`. `F | T` evaluates to `True` — that's one valid (trivially-parenthesized) way — but the buggy rule computes its True-count as `Tl · Tr = (#true ways of F) · (#true ways of T) = 0 · 1 = 0`. The left side `F` has *zero* True-ways, so any rule phrased purely in True-counts multiplies the whole thing to zero, even though `F | True` is plainly true. The correct OR rule is `total − Fl·Fr`: "every pairing except the one where *both* sides are False." That formula reaches for `Fl` and `Fr` — the False-counts — which is exactly why the second table can't be dropped. AND is the special case that lulls you: it really is `Tl·Tr`, so a one-operator test wouldn't expose the bug. OR and XOR need to know how each side can be *False*, and that's the whole reason for two tables.

</details>

## Your Turn

**Different Ways to Add Parentheses** ([LeetCode 241](https://leetcode.com/problems/different-ways-to-add-parentheses/)) — the same split-at-the-operator idea, generalized from booleans to arithmetic: return *every* result obtainable by parenthesizing an expression of numbers and `+ - *`. Split at each operator, combine every left result with every right result.

```python run viz=array
def diff_ways(expr):
    if expr.isdigit():
        return [int(expr)]                           # base: a bare number
    res = []
    for i, ch in enumerate(expr):
        if ch in "+-*":                              # treat ch as the last operator applied
            for l in diff_ways(expr[:i]):
                for r in diff_ways(expr[i + 1:]):
                    res.append(l + r if ch == '+' else l - r if ch == '-' else l * r)
    return res

print(sorted(diff_ways("2-1-1")))      # [0, 2]
print(sorted(diff_ways("2*3-4*5")))    # [-34, -14, -10, -10, 10]
```

```java run viz=array
import java.util.*;
public class Main {
    static List<Integer> diffWays(String e) {
        List<Integer> res = new ArrayList<>();
        if (e.chars().allMatch(Character::isDigit)) { res.add(Integer.parseInt(e)); return res; }
        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);
            if (c == '+' || c == '-' || c == '*')
                for (int l : diffWays(e.substring(0, i)))
                    for (int r : diffWays(e.substring(i + 1)))
                        res.add(c == '+' ? l + r : c == '-' ? l - r : l * r);
        }
        return res;
    }
    public static void main(String[] args) {
        List<Integer> w1 = diffWays("2-1-1");   Collections.sort(w1);
        List<Integer> w2 = diffWays("2*3-4*5"); Collections.sort(w2);
        System.out.println(w1);   // [0, 2]
        System.out.println(w2);   // [-34, -14, -10, -10, 10]
    }
}
```

Both print `[0, 2]` then `[-34, -14, -10, -10, 10]`. `2-1-1` parenthesizes as `(2-1)-1 = 0` or `2-(1-1) = 2`; the second expression has five groupings. This is boolean parenthesization's structural twin — split at each operator, combine the sub-results — just collecting *values* instead of *counts*. (It's written here as plain recursion to show the split clearly; memoizing on the substring turns it into the same bottom-up interval DP.)

## Reflect & Connect

- **Split-point interval DP.** The defining move is choosing the *last operator* — an internal split `k`, not an endpoint. That extra `for k` loop costs a factor of `n` (so `O(n³)`), and it's shared with matrix-chain multiplication and "different ways to add parentheses."
- **Two tables because combining needs both polarities.** `|` is True unless both sides are False; `^` is True when sides differ. Both formulas read the False-counts, so tracking only True-counts is wrong for everything but AND.
- **Sum of products.** Each operator's rule partitions the `total = (Tl+Fl)(Tr+Fr)` pairings into True and False buckets. Writing the truth table as counts is the mechanical heart of the recurrence.
- **It's the counting cousin of [optimal game strategy](/cortex/data-structures-and-algorithms/algorithms-by-strategy-dynamic-programming-optimal-stratergy).** That lesson aggregated with `max`/`min` over choices; this one aggregates with `+`/`×` over choices. Same interval scaffold, different aggregator — the through-line of the whole DP section.
- **The number of parenthesizations is Catalan.** An `n`-operand expression has the `(n-1)`-th Catalan number of parenthesizations — exponential — which is why brute-force enumeration is hopeless and the `O(n³)` DP matters.

## Recall

<details>
<summary><strong>Q:</strong> What does the split point represent here, and why is the cost `O(n³)`?</summary>

**A:** The split point `k` is the *last operator applied*, separating operands `[i..k]` from `[k+1..j]`. There are `O(n²)` ranges and `O(n)` split points each → `O(n³)`. This is split-point interval DP, like matrix-chain.

</details>
<details>
<summary><strong>Q:</strong> Why are two tables (T and F) required?</summary>

**A:** Combining via `|` and `^` depends on the False-counts: `left | right` is True unless both are False (`total − Fl·Fr`), and `left ^ right` is True when the sides differ (`Tl·Fr + Fl·Tr`). True-counts alone can't express these, so the False-table is mandatory.

</details>
<details>
<summary><strong>Q:</strong> What are the three operators' True-count rules?</summary>

**A:** `&`: `Tl·Tr`. `|`: `total − Fl·Fr` (all pairings except both-false). `^`: `Tl·Fr + Fl·Tr` (sides differ), where `total = (Tl+Fl)(Tr+Fr)`.

</details>
<details>
<summary><strong>Q:</strong> Why does the AND-style rule <code>Tl·Tr</code> fail for OR on <code>"F|T"</code>?</summary>

**A:** `F|T` is True, but `Tl·Tr = 0·1 = 0` because the left side `F` has zero True-ways. OR's truth needs the both-False count: `total − Fl·Fr = 1 − 0 = 1`. AND is the lone operator where `Tl·Tr` is correct.

</details>
<details>
<summary><strong>Q:</strong> How does this relate to "different ways to add parentheses" (LeetCode 241)?</summary>

**A:** Identical split-at-the-operator structure: pick each operator as last, combine the sub-results. Boolean parenthesization *counts* True-results; LeetCode 241 *collects* all numeric values. Both are split-point interval DPs.

</details>

## Sources & Verify

- **GeeksforGeeks**, "Boolean Parenthesization Problem" — the canonical two-table (`T`/`F`) split-point recurrence and the three operator truth tables.
- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15.2 — matrix-chain multiplication, the split-point interval-DP template this shares.
- **LeetCode** 241 (Different Ways to Add Parentheses) is the canonical drill for the split structure; the `2`/`4` True-counts, the correct-vs-buggy `1`/`0` on `"F|T"`, and the `[0,2]` / `[-34,-14,-10,-10,10]` value sets above all come from the runnable blocks — re-run to verify.
