---
title: "Edit Distance"
summary: "The minimum insert/delete/replace operations to turn one string into another (Levenshtein). The LCS grid with three moves on a mismatch: dp[i][j] = 1 + min(insert, delete, replace), match takes the diagonal free."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/03-longest-common-subsequence
---

## Why It Exists

You type `recieve` and the search box asks "did you mean *receive*?" Behind it: the site computed the **edit distance** between your query and dictionary words and returned the closest. Edit distance (Levenshtein) is the minimum number of single-character **insertions, deletions, and substitutions** to turn one string into another — the engine behind spell-checkers, fuzzy search, DNA alignment, `diff`, and OCR correction.

It's the [LCS](/cortex/data-structures-and-algorithms/algorithms-by-strategy-dynamic-programming-longest-common-subsequence) grid with one upgrade: where LCS had two moves on a mismatch, edit distance has **three** (insert, delete, replace), and it *minimises* a cost instead of maximising a length. Same `(i, j)` state, same `O(m·n)` table — the recurrence's three-way `min` is the whole lesson.

## See It Work

`dp[i][j]` = edit distance between the first `i` chars of `a` and first `j` of `b`. Match → take the diagonal free; mismatch → 1 + the cheapest of the three operations.

```python run viz=grid
def edit_distance(a, b):
    m, n = len(a), len(b)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(m + 1): dp[i][0] = i              # base: i deletions to empty a's prefix
    for j in range(n + 1): dp[0][j] = j              # base: j insertions to build b's prefix
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if a[i - 1] == b[j - 1]:
                dp[i][j] = dp[i - 1][j - 1]           # match: free, take the diagonal
            else:
                dp[i][j] = 1 + min(dp[i - 1][j],      # delete from a  (up)
                                   dp[i][j - 1],      # insert into a  (left)
                                   dp[i - 1][j - 1])  # replace        (diagonal)
    return dp[m][n]

print(edit_distance("horse", "ros"))                 # 3
```

```java run viz=grid
public class Main {
    static int editDistance(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;        // base cases
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                    ? dp[i - 1][j - 1]                                       // match
                    : 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
        return dp[m][n];
    }
    public static void main(String[] args) {
        System.out.println(editDistance("horse", "ros"));   // 3
    }
}
```

Both print `3`: `horse → rorse` (replace h→r) `→ rose` (delete r) `→ ros` (delete e). Cost `O(m·n)`.

## How It Works

On a mismatch there are exactly three ways to make progress, each charging `1` and reading a different neighbour:

```d2
direction: right
cell: "dp[i][j]  (mismatch a[i-1] != b[j-1])" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
del: "DELETE a[i-1]  ->  1 + dp[i-1][j]   (up)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
ins: "INSERT b[j-1]  ->  1 + dp[i][j-1]   (left)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
rep: "REPLACE a[i-1] -> b[j-1]  ->  1 + dp[i-1][j-1]   (diagonal)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
cell -> del
cell -> ins
cell -> rep
```

<p align="center"><strong>Three operations, three neighbours: delete (up), insert (left), replace (diagonal). Take the cheapest + 1. On a match, the diagonal is free (no operation).</strong></p>

The **base cases carry the cost of an empty string**: turning a length-`i` prefix into `""` takes `i` deletions (`dp[i][0] = i`); building a length-`j` prefix from `""` takes `j` insertions (`dp[0][j] = j`). That's *why* the first row and column aren't zeros. The recurrence has both DP properties (optimal substructure: best edit of two prefixes builds on best edits of shorter prefixes; overlapping subproblems: shared by all three neighbours). `O(m·n)` time, `O(min(m,n))` space if you only need the number.

> **Key takeaway.** Edit distance = LCS's grid with *three* moves and a `min`: match → diagonal (free); mismatch → `1 + min(delete=up, insert=left, replace=diagonal)`. Base cases `dp[i][0]=i`, `dp[0][j]=j` encode "delete the whole prefix" / "insert the whole prefix" — they're the costs against the empty string, not zeros.

## Trace It

Those non-zero base cases look like boilerplate. They are not — they're where the deletion and insertion costs *enter the table*. Zero them out (the natural mistake when copying a "make a 2D array of zeros" template) and watch.

**Predict before you run:** with `dp[i][0]` and `dp[0][j]` left at 0, what is `edit_distance("abc", "")` — the true answer is `3` (delete all three characters)?

```python run viz=grid
def edit_distance_buggy(a, b):
    m, n = len(a), len(b)
    dp = [[0] * (n + 1) for _ in range(m + 1)]       # BUG: base row/col left at 0
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if a[i - 1] == b[j - 1]: dp[i][j] = dp[i - 1][j - 1]
            else: dp[i][j] = 1 + min(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
    return dp[m][n]

def edit_distance(a, b):
    m, n = len(a), len(b); dp = [[0]*(n+1) for _ in range(m+1)]
    for i in range(m+1): dp[i][0] = i
    for j in range(n+1): dp[0][j] = j
    for i in range(1, m+1):
        for j in range(1, n+1):
            dp[i][j] = dp[i-1][j-1] if a[i-1]==b[j-1] else 1+min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
    return dp[m][n]

print("correct edit('abc', ''):", edit_distance("abc", ""))
print("buggy   edit('abc', ''):", edit_distance_buggy("abc", ""))
```

<details>
<summary><strong>Reveal</strong></summary>

The correct answer is `3` (delete `a`, `b`, `c`); the zero-base version returns `0` — it claims `"abc"` is already equal to `""`. With `b` empty, the inner loop over `j` never runs, so `dp[m][n]` is read straight from the un-initialised `dp[m][0]`, which the bug left at `0`. The base column `dp[i][0] = i` is *the* statement of "it costs `i` deletions to erase the first `i` characters"; without it, the table has no idea that emptying a string isn't free. The recurrence body alone is incomplete — DP base cases aren't setup ceremony, they're the smallest answers the whole table is built on, and here they literally encode two of the three operations.

</details>

## Your Turn

Back to the hook: a tiny **spell-checker**. Given a misspelt query and a dictionary, return the word with the smallest edit distance.

```python run viz=grid
def edit_distance(a, b):
    m, n = len(a), len(b); dp = [[0]*(n+1) for _ in range(m+1)]
    for i in range(m+1): dp[i][0] = i
    for j in range(n+1): dp[0][j] = j
    for i in range(1, m+1):
        for j in range(1, n+1):
            dp[i][j] = dp[i-1][j-1] if a[i-1]==b[j-1] else 1+min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
    return dp[m][n]

def closest_word(query, dictionary):
    return min(dictionary, key=lambda w: edit_distance(query, w))

print(closest_word("speling", ["spelling", "ceiling", "spell", "sapling"]))   # spelling (distance 1)
```

```java run viz=grid
public class Main {
    static int editDistance(String a, String b) {
        int m=a.length(), n=b.length(); int[][] dp=new int[m+1][n+1];
        for (int i=0;i<=m;i++) dp[i][0]=i;
        for (int j=0;j<=n;j++) dp[0][j]=j;
        for (int i=1;i<=m;i++) for (int j=1;j<=n;j++)
            dp[i][j] = a.charAt(i-1)==b.charAt(j-1) ? dp[i-1][j-1]
                     : 1 + Math.min(dp[i-1][j], Math.min(dp[i][j-1], dp[i-1][j-1]));
        return dp[m][n];
    }
    static String closestWord(String q, String[] dict) {
        String best = null; int bd = Integer.MAX_VALUE;
        for (String w : dict) { int d = editDistance(q, w); if (d < bd) { bd = d; best = w; } }
        return best;
    }
    public static void main(String[] args) {
        System.out.println(closestWord("speling", new String[]{"spelling","ceiling","spell","sapling"}));   // spelling
    }
}
```

Both print `spelling` — one insertion (`l`) away from `speling`, beating `sapling` (2) and `ceiling`/`spell` (3). That's a spell-checker in one `min` over the dictionary. (Production engines index by edit-distance-1 neighbourhoods or BK-trees to avoid scanning every word, but the per-pair cost is exactly this DP.)

## Reflect & Connect

- **Edit distance = LCS grid + a third move.** LCS had two mismatch moves and maximised; edit distance has three (insert/delete/replace) and minimises. Recognising "same grid, different move set" lets you adapt one to the other in seconds.
- **Base cases encode operations, not setup.** `dp[i][0]=i` and `dp[0][j]=j` *are* the deletion and insertion costs against the empty string. Zeroing them silently breaks the answer (the trace above).
- **Variants are one tweak each.** Weight the three operations differently → **Needleman–Wunsch** (DNA alignment). Add an adjacent-transposition move → **Damerau–Levenshtein** (catches `teh`→`the`). The grid and the `min` stay.
- **Space and speed.** `O(min(m,n))` space with two rows if you only need the number; the full table is needed to reconstruct the actual edit script. Used in `diff`, spell-checkers, fuzzy search, and bioinformatics aligners.

## Recall

<details>
<summary><strong>Q:</strong> What is the edit-distance recurrence?</summary>

**A:** Match (`a[i-1]==b[j-1]`) → `dp[i-1][j-1]` (free diagonal). Mismatch → `1 + min(dp[i-1][j]` delete, `dp[i][j-1]` insert, `dp[i-1][j-1]` replace`)`. Answer `dp[m][n]`, `O(m·n)`.

</details>
<details>
<summary><strong>Q:</strong> What do the base cases mean, and why aren't they zero?</summary>

**A:** `dp[i][0] = i` (delete `i` chars to reach `""`) and `dp[0][j] = j` (insert `j` chars to build `b`'s prefix). They carry the cost against the empty string — zeroing them makes the table think emptying a string is free.

</details>
<details>
<summary><strong>Q:</strong> How does edit distance differ from LCS?</summary>

**A:** Same `(i, j)` grid, but three mismatch moves (insert/delete/replace) instead of two, and it minimises a cost rather than maximising a length. The diagonal-on-match is shared.

</details>
<details>
<summary><strong>Q:</strong> Which neighbour is each operation?</summary>

**A:** Delete = up (`dp[i-1][j]`), insert = left (`dp[i][j-1]`), replace = diagonal (`dp[i-1][j-1]`). On a mismatch take `1 +` the cheapest.

</details>
<details>
<summary><strong>Q:</strong> Two named variants of edit distance?</summary>

**A:** Needleman–Wunsch (operation-weighted, for DNA alignment) and Damerau–Levenshtein (adds an adjacent-transposition move, for typo correction like `teh`→`the`).

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15.4 + the edit-distance problem — the three-operation recurrence and its DP table.
- **Levenshtein, V.** (1966), "Binary codes capable of correcting deletions, insertions, and reversals" — the original metric; **Wagner & Fischer** (1974) gave the `O(mn)` DP.
- **LeetCode** 72 (Edit Distance), 161 (One Edit Distance), 712 (Minimum ASCII Delete Sum — a weighted variant) are the canonical drills; the `3`, the correct-vs-zero-base `3`/`0`, and the spell-checker `spelling` above come from the runnable blocks — re-run to verify.
