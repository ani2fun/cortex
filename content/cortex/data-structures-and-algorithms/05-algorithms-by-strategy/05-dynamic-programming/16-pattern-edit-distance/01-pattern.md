---
title: "Pattern: Edit Distance"
summary: "Two-sequence 2-D DP where each cell depends on the diagonal, top, and left — string matching, wildcard, and interleaving problems. The recognition layer: which neighbors a cell reads IS the operation it allows."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/05-edit-distance
---

## Why It Exists

[Edit distance](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/edit-distance), [LCS](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-common-subsequence), wildcard matching, regex matching, interleaving — these look like different problems, but they're one *pattern*. Whenever you're comparing or aligning **two sequences** and each step decides how to consume a character from one, the other, or both, you reach for a 2-D grid `dp[i][j]` over "first `i` of A, first `j` of B," and each cell looks at just three neighbours: the **diagonal**, the **top**, and the **left**.

This lesson isn't a new algorithm — it's the *recognition layer*. Once you see the two-sequence-alignment shape, you know the table dimensions, the three candidate transitions, and where the answer lives (`dp[m][n]`) before writing a line. The skill is mapping each transition to the problem's operation: diagonal = "use a character from both," top = "consume from A alone," left = "consume from B alone."

## See It Work

**Wildcard Matching** ([LeetCode 44](https://leetcode.com/problems/wildcard-matching/)) — does pattern `p` (with `?` = any one char, `*` = any run, including empty) match string `s`? It's the pattern with a twist: `?` and a literal use the **diagonal** (match one char), but `*` reads **top or left** — absorb a character or match nothing.

```python run viz=grid
def is_match(s, p):
    m, n = len(s), len(p)
    dp = [[False] * (n + 1) for _ in range(m + 1)]
    dp[0][0] = True
    for j in range(1, n + 1):
        if p[j - 1] == '*':
            dp[0][j] = dp[0][j - 1]                      # '*' can still match the empty string
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if p[j - 1] == '*':
                dp[i][j] = dp[i - 1][j] or dp[i][j - 1]  # absorb s[i-1] (TOP) or match empty (LEFT)
            elif p[j - 1] in ('?', s[i - 1]):
                dp[i][j] = dp[i - 1][j - 1]              # match one char (DIAGONAL)
    return dp[m][n]

s = input()
p = input()
print("true" if is_match(s, p) else "false")
```

```java run viz=grid
import java.util.*;

public class Main {
    static boolean isMatch(String s, String p) {
        int m = s.length(), n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;
        for (int j = 1; j <= n; j++) if (p.charAt(j - 1) == '*') dp[0][j] = dp[0][j - 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++) {
                char pc = p.charAt(j - 1);
                if (pc == '*') dp[i][j] = dp[i - 1][j] || dp[i][j - 1];      // top or left
                else if (pc == '?' || pc == s.charAt(i - 1)) dp[i][j] = dp[i - 1][j - 1];  // diagonal
            }
        return dp[m][n];
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        String p = sc.nextLine();
        System.out.println(isMatch(s, p));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "adceb" },
    { "id": "p", "label": "p", "type": "string", "placeholder": "*a*b" }
  ],
  "cases": [
    { "args": { "s": "adceb", "p": "*a*b" }, "expected": "true" },
    { "args": { "s": "cb", "p": "?a" }, "expected": "false" },
    { "args": { "s": "abc", "p": "*" }, "expected": "true" },
    { "args": { "s": "abc", "p": "a?c" }, "expected": "true" }
  ]
}
```

Both print `true` then `false`. `*a*b` matches `adceb` (the first `*` eats `adce`... actually `ad`, the `a` matches, second `*` eats `ce`, `b` matches); `?a` can't match `cb` because `a ≠ b`. Same `dp[i][j]`-over-two-prefixes table as edit distance — only the per-cell rule changed.

## How It Works

Every problem in this family fills the same grid; the only thing that varies is which of the three neighbours each cell is allowed to read, and what condition gates it:

```d2
direction: down
diag: "DIAGONAL dp[i-1][j-1]\nuse a char from BOTH sequences\n(match / replace / align)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
top: "TOP dp[i-1][j]\nconsume from A only\n(delete from A / take A's char)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
left: "LEFT dp[i][j-1]\nconsume from B only\n(insert / take B's char)" {style.fill: "#fbcfe8"; style.stroke: "#db2777"}
cell: "dp[i][j]\nanswer over (first i of A, first j of B)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
diag -> cell
top -> cell
left -> cell
```

<p align="center"><strong>The four mandatory components: a 2-D state over two prefixes, three candidate transitions (diagonal / top / left), base cases on the empty-prefix row and column, and a row-major fill so all three neighbours are ready. The problem decides <em>which</em> transitions are legal and how they're scored.</strong></p>

The pattern is four fixed parts and one variable part:

- **Fixed: state, neighbours, base, fill order.** `dp[i][j]` answers the question for the first `i` of A and `j` of B. It can only depend on `dp[i-1][j-1]` (diagonal), `dp[i-1][j]` (top), `dp[i][j-1]` (left) — so a plain row-major double loop always has them computed. Base cases live on row 0 / column 0 (one sequence empty).
- **Variable: which neighbours, gated how, aggregated how.** Edit distance reads all three with a `min` (replace/delete/insert). LCS reads the diagonal on a match, else `max(top, left)`. Wildcard's `*` reads top-or-left; its `?` reads the diagonal. Interleaving (your turn) reads *only* top and left — never the diagonal — because every character of the result comes from exactly one source.

> **Key takeaway.** The edit-distance pattern is a 2-D DP `dp[i][j]` over two prefixes whose every cell reads only the **diagonal, top, and left**. *Which* neighbours a cell reads is the operation it permits: diagonal = consume from both, top = consume from A, left = consume from B. Recognise "align/compare/transform two sequences with per-character choices" and you have the table, the transitions, and the answer cell (`dp[m][n]`) for free.

## Trace It

The whole pattern lives in the neighbour choice. Get *which* neighbours a cell reads wrong, and you've silently solved a different problem — even with the grid, base cases, and fill order all correct.

**Predict before you run:** wildcard `*` should match *any* run of characters. Suppose a tired coder treats `*` exactly like `?` — read the diagonal, match one char. With that bug, does `*` match the two-character string `"ab"`?

```python run viz=grid
def is_match(s, p):                                  # correct: '*' reads top-or-left
    m, n = len(s), len(p)
    dp = [[False] * (n + 1) for _ in range(m + 1)]
    dp[0][0] = True
    for j in range(1, n + 1):
        if p[j - 1] == '*':
            dp[0][j] = dp[0][j - 1]
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if p[j - 1] == '*':
                dp[i][j] = dp[i - 1][j] or dp[i][j - 1]
            elif p[j - 1] in ('?', s[i - 1]):
                dp[i][j] = dp[i - 1][j - 1]
    return dp[m][n]

def is_match_buggy(s, p):                            # bug: '*' treated like '?' (diagonal only)
    m, n = len(s), len(p)
    dp = [[False] * (n + 1) for _ in range(m + 1)]
    dp[0][0] = True
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if p[j - 1] in ('?', '*', s[i - 1]):
                dp[i][j] = dp[i - 1][j - 1]          # diagonal only -> matches exactly one char
    return dp[m][n]

print("correct '*' vs 'ab':", is_match("ab", "*"))
print("buggy   '*' vs 'ab':", is_match_buggy("ab", "*"))
```

<details>
<summary><strong>Reveal</strong></summary>

Correct is `True`; the buggy version returns `False`. The diagonal transition `dp[i-1][j-1]` consumes *one* character from each side — perfect for `?` or a literal, which match exactly one character. But `*` must match a *run*: matching `"ab"` means `*` absorbs both `a` and `b`. That's the **top** transition `dp[i-1][j]` — "consume one more character of `s` while staying on the same `*`" — chained twice, with the **left** transition `dp[i][j-1]` as the "match nothing" escape. A diagonal-only `*` can only ever cross one character, so it matches single-char strings and nothing longer. The bug isn't in the grid, the base cases, or the loop — it's *which neighbours the cell reads*, and that choice is the entire semantics of the operator. That's the pattern's central lesson: the transition set **is** the problem definition.

</details>

## Your Turn

**Interleaving String** ([LeetCode 97](https://leetcode.com/problems/interleaving-string/)) — is `s3` formed by interleaving `s1` and `s2`, preserving each one's order? This is the pattern with *no diagonal*: every character of `s3` comes from exactly one source, so each cell reads **top** (next char from `s1`) or **left** (next char from `s2`).

```python run viz=grid
def is_interleave(s1, s2, s3):
    # Your code goes here
    return False

s1 = input()
s2 = input()
s3 = input()
print("true" if is_interleave(s1, s2, s3) else "false")
```

```java run viz=grid
import java.util.*;

public class Main {
    static boolean isInterleave(String s1, String s2, String s3) {
        // Your code goes here
        return false;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s1 = sc.nextLine();
        String s2 = sc.nextLine();
        String s3 = sc.nextLine();
        System.out.println(isInterleave(s1, s2, s3));
    }
}
```

```testcases
{
  "args": [
    { "id": "s1", "label": "s1", "type": "string", "placeholder": "aabcc" },
    { "id": "s2", "label": "s2", "type": "string", "placeholder": "dbbca" },
    { "id": "s3", "label": "s3", "type": "string", "placeholder": "aadbbcbcac" }
  ],
  "cases": [
    { "args": { "s1": "aabcc", "s2": "dbbca", "s3": "aadbbcbcac" }, "expected": "true" },
    { "args": { "s1": "aabcc", "s2": "dbbca", "s3": "aadbbbaccc" }, "expected": "false" },
    { "args": { "s1": "abc", "s2": "def", "s3": "adbecf" }, "expected": "true" },
    { "args": { "s1": "abc", "s2": "def", "s3": "adcebf" }, "expected": "false" }
  ]
}
```

<details>
<summary>Editorial</summary>

```python solution time=O(m·n) space=O(m·n)
def is_interleave(s1, s2, s3):
    m, n = len(s1), len(s2)
    if m + n != len(s3):
        return False
    dp = [[False] * (n + 1) for _ in range(m + 1)]
    dp[0][0] = True
    for i in range(m + 1):
        for j in range(n + 1):
            if i > 0 and s1[i - 1] == s3[i + j - 1]:
                dp[i][j] = dp[i][j] or dp[i - 1][j]      # take s1's char (TOP)
            if j > 0 and s2[j - 1] == s3[i + j - 1]:
                dp[i][j] = dp[i][j] or dp[i][j - 1]      # take s2's char (LEFT)
    return dp[m][n]

s1 = input()
s2 = input()
s3 = input()
print("true" if is_interleave(s1, s2, s3) else "false")
```

```java solution
import java.util.*;

public class Main {
    static boolean isInterleave(String s1, String s2, String s3) {
        int m = s1.length(), n = s2.length();
        if (m + n != s3.length()) return false;
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;
        for (int i = 0; i <= m; i++)
            for (int j = 0; j <= n; j++) {
                if (i > 0 && s1.charAt(i - 1) == s3.charAt(i + j - 1)) dp[i][j] = dp[i][j] || dp[i - 1][j];
                if (j > 0 && s2.charAt(j - 1) == s3.charAt(i + j - 1)) dp[i][j] = dp[i][j] || dp[i][j - 1];
            }
        return dp[m][n];
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s1 = sc.nextLine();
        String s2 = sc.nextLine();
        String s3 = sc.nextLine();
        System.out.println(isInterleave(s1, s2, s3));
    }
}
```

Both print `true` then `false`. The grid index is the giveaway that you've recognised the pattern: `dp[i][j]` covers exactly `i + j` characters of `s3`, so `s3[i+j-1]` is "the next character to place." No diagonal appears because you never consume from both sources at once — a textbook example of the transition set encoding the rules.

</details>

## Reflect & Connect

- **One grid, three neighbours.** Diagonal (both), top (A only), left (B only). Memorise that and every two-sequence DP is a fill-in-the-transitions exercise.
- **The transition set is the problem.** Edit distance uses all three with `min`; LCS uses diagonal-on-match else `max`; wildcard's `*` uses top-or-left; interleaving uses top-and-left only. Same scaffold, different legal moves.
- **Base cases are the empty-prefix row and column.** What does it mean for one sequence to be empty? (Edit distance: `i` deletions. Wildcard: only an all-`*` pattern matches. Interleaving: the other string must equal the prefix.) Get row 0 / column 0 right or the whole table is poisoned.
- **Recognition cues.** Two sequences (or a string and a pattern); a per-position decision to consume from one, the other, or both; answer at `dp[m][n]`; `O(m·n)` time. The moment you see that shape, draw the grid.
- **The family.** [Edit distance](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/edit-distance), [LCS](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-common-subsequence), [longest common substring](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-common-substring), wildcard (44), regex (10), interleaving (97), shortest common supersequence — all the same pattern. The [problems in this section](02-problems) drill the recognition.

## Recall

<details>
<summary><strong>Q:</strong> What are the three neighbours a cell can read, and what does each mean?</summary>

**A:** Diagonal `dp[i-1][j-1]` = consume a character from *both* sequences (match/replace/align); top `dp[i-1][j]` = consume from A only (delete / take A's char); left `dp[i][j-1]` = consume from B only (insert / take B's char).

</details>
<details>
<summary><strong>Q:</strong> What are the four fixed components of the pattern?</summary>

**A:** (1) State `dp[i][j]` over the first `i` of A and `j` of B; (2) transitions limited to diagonal/top/left; (3) base cases on the empty-prefix row 0 and column 0; (4) row-major fill so all three neighbours are already computed.

</details>
<details>
<summary><strong>Q:</strong> Why does wildcard <code>*</code> read top-or-left instead of the diagonal?</summary>

**A:** `*` matches a *run* of characters, not exactly one. Top (`dp[i-1][j]`) absorbs one more character of `s` while staying on the same `*`; left (`dp[i][j-1]`) lets `*` match nothing. The diagonal consumes exactly one char each side — correct for `?`/literals, wrong for `*`.

</details>
<details>
<summary><strong>Q:</strong> Why does interleaving-string use no diagonal transition?</summary>

**A:** Every character of `s3` comes from exactly one source (`s1` or `s2`), never both at once — so each cell takes top (next from `s1`) or left (next from `s2`). `dp[i][j]` covers `i+j` characters of `s3`.

</details>
<details>
<summary><strong>Q:</strong> What recognition cues say "reach for this pattern"?</summary>

**A:** Two sequences (or string + pattern); a per-position choice to consume from one, the other, or both; the answer at `dp[m][n]`; `O(m·n)` time and space (often reducible to two rows).

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15.4 — the LCS/edit-distance grid that defines the diagonal/top/left transition structure.
- **LeetCode** 44 (Wildcard Matching), 97 (Interleaving String), 72 (Edit Distance), 10 (Regular Expression Matching) are the canonical drills; the `true`/`false` wildcard matches, the correct-vs-buggy `*` on `"ab"`, and the `true`/`false` interleavings above all come from the runnable blocks — re-run to verify.
