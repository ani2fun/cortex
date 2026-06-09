---
title: "Longest Common Subsequence"
summary: "The longest sequence common to two strings, in order, with gaps allowed. The 2D recurrence dp[i][j] = dp[i-1][j-1]+1 on a match, else max(dp[i-1][j], dp[i][j-1]) — multidimensional recursion plus a cache."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/01-linear-dp
  - 05-algorithms-by-strategy/01-recursion/08-pattern-multidimensional-recursion/01-pattern
---

## Why It Exists

A diff tool slides two file versions side by side and highlights what stayed the same. It isn't matching lines verbatim — it's finding the longest *sequence* of identical lines, in order, allowing arbitrary insertions and deletions between them. That's a **Longest Common Subsequence** (LCS), and the same algorithm powers spell-checkers (closest dictionary word), genomics (DNA-strand alignment), and plagiarism detectors.

LCS is the first **2D dynamic program**: the state is a *pair* of indices `(i, j)` — how far into each string we are — so the table is a grid. That's exactly the [multidimensional recursion](/cortex/data-structures-and-algorithms/algorithms-by-strategy-recursion-pattern-multidimensional-recursion) shape, now with a cache: the grid-of-overlapping-subproblems whose `O(2^{m+n}) → O(m·n)` collapse you predicted there.

## See It Work

`dp[i][j]` = LCS of the first `i` chars of `a` and first `j` of `b`. On a match, extend the diagonal; otherwise take the better of dropping one char from either string.

```python run viz=grid
def lcs(a, b):
    m, n = len(a), len(b)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if a[i - 1] == b[j - 1]:
                dp[i][j] = dp[i - 1][j - 1] + 1        # match: +1 on the DIAGONAL
            else:
                dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])   # mismatch: best of left / up
    return dp[m][n]

print(lcs("abcde", "ace"))                            # 3  ("ace")
```

```java run viz=grid
public class Main {
    static int lcs(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                    ? dp[i - 1][j - 1] + 1                          // match: diagonal
                    : Math.max(dp[i - 1][j], dp[i][j - 1]);        // mismatch: left / up
        return dp[m][n];
    }
    public static void main(String[] args) {
        System.out.println(lcs("abcde", "ace"));      // 3
    }
}
```

Both print `3` — the LCS is `"ace"`, threaded out of `"abcde"` by skipping `b` and `d`. Cost: `O(m·n)` time and space.

## How It Works

The whole algorithm is one two-case recurrence on the grid:

```d2
direction: right
cell: "dp[i][j]" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
match: "a[i-1] == b[j-1]?\nMATCH -> dp[i-1][j-1] + 1\n(take the diagonal, consume one char from EACH)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
mismatch: "MISMATCH -> max(dp[i-1][j], dp[i][j-1])\n(drop a char from a, or from b)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
cell -> match
cell -> mismatch
```

<p align="center"><strong>Each cell looks at three neighbours: the diagonal on a match (consuming one character from each string), or the max of left/up on a mismatch (dropping one character from one string).</strong></p>

It has both DP properties: **optimal substructure** (the LCS of two prefixes is built from the LCS of shorter prefixes) and **overlapping subproblems** (`dp[i-1][j-1]` is reused by `dp[i][j]`, `dp[i+1][j]`, and `dp[i][j+1]`). The grid has `(m+1)·(n+1)` cells, each `O(1)` work → `O(m·n)`. The diagonal is the crux: a match must advance *both* indices, so each matched character is consumed once from each string. Reconstruct the actual subsequence by walking back from `dp[m][n]` — diagonal steps were matches. Space rolls to `O(min(m, n))` since each row depends only on the previous one (but then you lose easy reconstruction).

> **Key takeaway.** LCS is the canonical 2D DP: `dp[i][j] = dp[i-1][j-1]+1` on a match (the diagonal), else `max(dp[i-1][j], dp[i][j-1])`. It's [multidimensional recursion](/cortex/data-structures-and-algorithms/algorithms-by-strategy-recursion-pattern-multidimensional-recursion) + a cache — `O(2^{m+n})` naive collapses to `O(m·n)`. The diagonal-on-match is what makes each character count once per string.

## Trace It

The match case uses the *diagonal* `dp[i-1][j-1]`, not the max of the neighbours. It's tempting to write `1 + max(dp[i-1][j], dp[i][j-1])` on a match — same shape, one extra `1`. Watch what that does to a string with a repeated character.

**Predict before you run:** with the match case as `1 + max(left, up)`, what is `LCS("aa", "a")` — still `1`, or something else?

```python run viz=grid
def lcs_buggy(a, b):
    m, n = len(a), len(b)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if a[i - 1] == b[j - 1]:
                dp[i][j] = 1 + max(dp[i - 1][j], dp[i][j - 1])   # BUG: not the diagonal
            else:
                dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
    return dp[m][n]

def lcs(a, b):
    m, n = len(a), len(b); dp = [[0]*(n+1) for _ in range(m+1)]
    for i in range(1, m+1):
        for j in range(1, n+1):
            dp[i][j] = dp[i-1][j-1]+1 if a[i-1]==b[j-1] else max(dp[i-1][j], dp[i][j-1])
    return dp[m][n]

print("correct:", lcs("aa", "a"))
print("buggy:  ", lcs_buggy("aa", "a"))
```

<details>
<summary><strong>Reveal</strong></summary>

Correct is `1` (the string `"a"` has one `a` to match); the buggy version returns `2`. Using `1 + max(left, up)` lets a *single* `a` in `"a"` get matched by *both* `a`s in `"aa"`: at `dp[2][1]` it reads `dp[1][1] = 1` (the first match) from the `up` neighbour and adds another `1`, double-counting the same `b`-character. The diagonal `dp[i-1][j-1]` is what forbids this — it forces a match to step *back in both strings*, so each character is consumed once on each side. That single cell reference is the entire correctness of LCS; the off-by-a-neighbour bug is invisible until a repeated character exposes it.

</details>

## Your Turn

**Delete Operations for Two Strings** ([LeetCode 583](https://leetcode.com/problems/delete-operation-for-two-strings/)) — minimum single-character deletions to make `a` and `b` equal. Everything *not* in the LCS must be deleted from one side or the other, so the answer is `m + n − 2·LCS(a, b)` — one formula on top of the same DP.

```python run viz=grid
def lcs(a, b):
    m, n = len(a), len(b); dp = [[0]*(n+1) for _ in range(m+1)]
    for i in range(1, m+1):
        for j in range(1, n+1):
            dp[i][j] = dp[i-1][j-1]+1 if a[i-1]==b[j-1] else max(dp[i-1][j], dp[i][j-1])
    return dp[m][n]

def min_delete(a, b):
    return len(a) + len(b) - 2 * lcs(a, b)            # everything outside the LCS is deleted

print(min_delete("sea", "eat"))         # 2   (delete 's' and 't'; keep "ea")
print(min_delete("leetcode", "etco"))   # 4
```

```java run viz=grid
public class Main {
    static int lcs(String a, String b) {
        int m=a.length(), n=b.length(); int[][] dp = new int[m+1][n+1];
        for (int i=1;i<=m;i++) for (int j=1;j<=n;j++)
            dp[i][j] = a.charAt(i-1)==b.charAt(j-1) ? dp[i-1][j-1]+1 : Math.max(dp[i-1][j], dp[i][j-1]);
        return dp[m][n];
    }
    static int minDelete(String a, String b) { return a.length() + b.length() - 2*lcs(a, b); }
    public static void main(String[] args) {
        System.out.println(minDelete("sea", "eat"));         // 2
        System.out.println(minDelete("leetcode", "etco"));   // 4
    }
}
```

Both print `2` then `4`. The LCS of `"sea"`/`"eat"` is `"ea"` (length 2), so `3 + 3 − 2·2 = 2` deletions. Same DP table, one arithmetic step — the recurring DP move of reusing a core subproblem for a derived answer.

## Reflect & Connect

- **This is 2D DP = multidimensional recursion + cache.** The grid of `(i, j)` subproblems and the `O(2^{m+n}) → O(m·n)` collapse are precisely the [multidimensional-recursion](/cortex/data-structures-and-algorithms/algorithms-by-strategy-recursion-pattern-multidimensional-recursion) prediction, now realised.
- **The diagonal carries the semantics.** Match → diagonal (advance both); mismatch → max of the two ways to drop one character. Edit distance ([next lessons](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/edit-distance)) is the same grid with *three* moves (insert/delete/replace) instead of two.
- **An LCS family fans out from here.** Shortest common supersequence (`m + n − LCS`), minimum deletions (`m + n − 2·LCS`), diff/patch, and longest palindromic subsequence (LCS of a string with its reverse) all reuse this exact table.
- **Subsequence, not substring.** LCS allows gaps; the *longest common substring* (next lesson) demands contiguity and resets the cell to 0 on a mismatch — a one-line change with a very different table.

## Recall

<details>
<summary><strong>Q:</strong> What is the LCS recurrence?</summary>

**A:** `dp[i][j] = dp[i-1][j-1] + 1` if `a[i-1] == b[j-1]` (diagonal), else `max(dp[i-1][j], dp[i][j-1])`. The answer is `dp[m][n]`; cost `O(m·n)`.

</details>
<details>
<summary><strong>Q:</strong> Why must a match take the diagonal rather than `1 + max(left, up)`?</summary>

**A:** The diagonal advances *both* indices, so a matched character is consumed once from each string. `1 + max(left, up)` lets one character match several, double-counting (e.g. `LCS("aa","a")` would wrongly be 2).

</details>
<details>
<summary><strong>Q:</strong> How does LCS relate to multidimensional recursion?</summary>

**A:** It's that pattern plus a cache: the state is a pair `(i, j)`, the subproblems tile a grid, and memoising/tabulating collapses the exponential recursion to `O(m·n)`.

</details>
<details>
<summary><strong>Q:</strong> How do you recover the actual subsequence, and what does space-optimisation cost?</summary>

**A:** Walk back from `dp[m][n]`: diagonal steps were matches (prepend that char). Rolling to two rows gives `O(min(m,n))` space but loses the full table needed for that backtrace.

</details>
<details>
<summary><strong>Q:</strong> How does "min deletions to make two strings equal" reduce to LCS?</summary>

**A:** Keep the LCS, delete everything else from both strings: `m + n − 2·LCS(a, b)`. One arithmetic step on the same DP table.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15.4 — Longest Common Subsequence: the two-case recurrence, the table, and `O(mn)` reconstruction.
- **Hunt & McIlroy** (1976), "An algorithm for differential file comparison" — LCS as the basis of Unix `diff`.
- **LeetCode** 1143 (LCS), 583 (Delete Operations), 1092 (Shortest Common Supersequence) are the canonical drills; the `3`, the correct-vs-buggy `1`/`2`, and the `2`/`4` deletion counts above come from the runnable blocks — re-run to verify.
