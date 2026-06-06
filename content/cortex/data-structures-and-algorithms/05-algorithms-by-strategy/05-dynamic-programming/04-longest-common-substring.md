---
title: "Longest Common Substring"
summary: "The longest run common to two strings with NO gaps — contiguous. One operator from LCS: a mismatch resets dp[i][j] to 0, and the answer is the max over every cell, not dp[m][n]."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/03-longest-common-subsequence
---

## Why It Exists

The [previous lesson](/cortex/data-structures-and-algorithms/algorithms-by-strategy-dynamic-programming-longest-common-subsequence) found the longest common *subsequence* — characters in order, gaps allowed. **Substring** is the contiguous version: the matched characters must be *adjacent* in both originals, no skipping. The problem statement changes by one word ("must touch"); the recurrence changes by one operator. But the consequences flip the algorithm's shape — where the answer lives, what a mismatch does, why the answer can be in any cell.

It's the workhorse behind plagiarism "longest verbatim quote," DNA "longest exact match," and `diff`'s anchor-block detection. And it's the cleanest demonstration in the DP section that *the recurrence's mismatch branch encodes the problem's definition.*

## See It Work

`dp[i][j]` = length of the longest common substring *ending exactly at* `a[i-1]` and `b[j-1]`. A match extends the diagonal run; a mismatch resets to 0. The answer is the largest value anywhere in the table.

```python run viz=grid
def lcsubstr(a, b):
    m, n = len(a), len(b)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    best = 0
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if a[i - 1] == b[j - 1]:
                dp[i][j] = dp[i - 1][j - 1] + 1   # extend the contiguous diagonal run
                best = max(best, dp[i][j])        # the answer can be in ANY cell
            else:
                dp[i][j] = 0                      # mismatch RESETS — no gaps allowed
    return best

print(lcsubstr("abcdxyz", "xyzabcd"))             # 4  ("abcd")
```

```java run viz=grid
public class Main {
    static int lcsubstr(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        int best = 0;
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;     // extend the run
                    best = Math.max(best, dp[i][j]);     // answer anywhere
                } else {
                    dp[i][j] = 0;                        // reset
                }
        return best;
    }
    public static void main(String[] args) {
        System.out.println(lcsubstr("abcdxyz", "xyzabcd"));   // 4
    }
}
```

Both print `4` — the run `"abcd"`, which is contiguous in both strings. Cost `O(m·n)`, like LCS.

## How It Works

Set the two recurrences side by side — the only difference is the mismatch branch and where the answer lives:

```d2
direction: right
lcs: "LCS (subsequence, gaps OK)" {
  m: "match -> dp[i-1][j-1] + 1"
  x: "MISMATCH -> max(dp[i-1][j], dp[i][j-1])  (carry the best forward)"
  ans: "answer = dp[m][n]  (bottom-right corner)"
}
lcsub: "LC substring (contiguous)" {
  m: "match -> dp[i-1][j-1] + 1" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
  x: "MISMATCH -> 0  (the run breaks)" {style.fill: "#fecaca"; style.stroke: "#dc2626"}
  ans: "answer = max over ALL cells" {style.fill: "#fde68a"; style.stroke: "#d97706"}
}
```

<p align="center"><strong>One operator: LCS carries the best forward on a mismatch (so <code>dp[m][n]</code> accumulates the global answer); substring resets to 0 (so each cell holds only the run <em>ending there</em>, and the global answer is the max cell).</strong></p>

Two consequences follow from that reset:

- **A mismatch breaks the run.** `dp[i][j]` means "longest common substring *ending at these two positions*." A mismatch there means no substring ends there — length 0. There's nothing to carry forward, because a gap would violate contiguity.
- **The answer is in no fixed cell.** Since every cell is a *local* run length, the global longest substring can end anywhere — you track a running `best` over all cells, not `dp[m][n]`. (`dp[m][n]` is just the run ending at the two final characters, usually not the answer.)

`O(m·n)` time; space rolls to two rows since each cell only needs the diagonal. (Suffix automata / generalised suffix trees solve it in `O(m + n)`, but the DP is the one to know.)

> **Key takeaway.** Longest common *substring* is LCS with two edits: a mismatch **resets `dp[i][j]` to 0** (contiguity — a gap breaks the run), and the answer is the **max over all cells**, not `dp[m][n]`. The mismatch branch literally encodes "substring vs subsequence."

## Trace It

The substring/subsequence distinction is the whole lesson. Run *both* DPs on the same pair, where the common characters are split by a single mismatch.

**Predict before you run:** for `"abcde"` and `"abfce"` (they share `a, b, c, e` but `"abcde"` has `d` where `"abfce"` has `f`), what's the longest common *subsequence* vs the longest common *substring*?

```python run viz=grid
def lcs(a, b):                                    # subsequence — gaps OK
    m, n = len(a), len(b); dp = [[0]*(n+1) for _ in range(m+1)]
    for i in range(1, m+1):
        for j in range(1, n+1):
            dp[i][j] = dp[i-1][j-1]+1 if a[i-1]==b[j-1] else max(dp[i-1][j], dp[i][j-1])
    return dp[m][n]

def lcsubstr(a, b):                               # substring — contiguous
    m, n = len(a), len(b); dp = [[0]*(n+1) for _ in range(m+1)]; best = 0
    for i in range(1, m+1):
        for j in range(1, n+1):
            if a[i-1] == b[j-1]:
                dp[i][j] = dp[i-1][j-1]+1; best = max(best, dp[i][j])
            else:
                dp[i][j] = 0
    return best

print("subsequence:", lcs("abcde", "abfce"))
print("substring:  ", lcsubstr("abcde", "abfce"))
```

<details>
<summary><strong>Reveal</strong></summary>

Subsequence is `4` (`"abce"` — skip the `d`/`f` mismatch and keep going); substring is `2` (`"ab"`, or equally `"ce"`). The single mismatch at the third position is *free* for a subsequence — `max(left, up)` carries the length-2 `"ab"` forward, and the later `c`, `e` extend it to 4. But for a substring that same mismatch is *fatal*: it resets the run to 0, so the best contiguous match is just the `"ab"` before the break (or the `"ce"` after it), length 2. The mismatch branch — `max(left, up)` vs `0` — is the entire difference between "in order, gaps allowed" and "adjacent, no gaps." Same grid, one operator, half the answer.

</details>

## Your Turn

**Maximum Length of Repeated Subarray** ([LeetCode 718](https://leetcode.com/problems/maximum-length-of-repeated-subarray/)) — the longest *contiguous* subarray common to two integer arrays. It's longest-common-substring on numbers instead of characters: identical recurrence, reset-on-mismatch, answer in the best cell.

```python run viz=grid
def find_length(a, b):
    m, n = len(a), len(b)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    best = 0
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if a[i - 1] == b[j - 1]:
                dp[i][j] = dp[i - 1][j - 1] + 1
                best = max(best, dp[i][j])
    return best

print(find_length([1, 2, 3, 2, 1], [3, 2, 1, 4, 7]))     # 3   ([3, 2, 1])
print(find_length([0, 0, 0, 0, 0], [0, 0, 0, 0, 0]))     # 5
```

```java run viz=grid
public class Main {
    static int findLength(int[] a, int[] b) {
        int m = a.length, n = b.length;
        int[][] dp = new int[m + 1][n + 1]; int best = 0;
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                if (a[i - 1] == b[j - 1]) { dp[i][j] = dp[i - 1][j - 1] + 1; best = Math.max(best, dp[i][j]); }
        return best;
    }
    public static void main(String[] args) {
        System.out.println(findLength(new int[]{1,2,3,2,1}, new int[]{3,2,1,4,7}));   // 3
        System.out.println(findLength(new int[]{0,0,0,0,0}, new int[]{0,0,0,0,0}));   // 5
    }
}
```

Both print `3` then `5`. The common run `[3, 2, 1]` is contiguous in both; the all-zeros case matches end to end. It's the exact LC-substring table with `==` on ints — a reminder that "substring DP" generalises to any sequence of comparable elements.

## Reflect & Connect

- **One operator separates the whole family.** Match → diagonal+1 in both. Mismatch → `max(left, up)` (LCS, gaps OK) vs `0` (substring, contiguous). That single branch is where the problem's definition lives — change it and you change the answer.
- **Where the answer lives is a consequence.** LCS accumulates the global best at `dp[m][n]`; substring's cells are local run-lengths, so the answer is the max cell. Forgetting this — reading `dp[m][n]` for a substring — is the classic bug.
- **Generalises beyond strings.** LeetCode 718 is the same DP on int arrays; any comparable elements work. Plagiarism, DNA exact-match, and `diff` anchors all use it.
- **Faster is possible.** Generalised suffix trees / suffix automata give `O(m + n)`, but the `O(m·n)` DP is the transparent baseline and space-optimises to two rows.

## Recall

<details>
<summary><strong>Q:</strong> How does the longest-common-substring recurrence differ from LCS?</summary>

**A:** Match is identical (`dp[i-1][j-1] + 1`). On a mismatch, LCS takes `max(dp[i-1][j], dp[i][j-1])`; substring resets to `0` (contiguity — a gap breaks the run).

</details>
<details>
<summary><strong>Q:</strong> Where is the answer, and why not <code>dp[m][n]</code>?</summary>

**A:** The max over *all* cells. Each `dp[i][j]` is the run *ending at* those positions (a local value); the longest substring can end anywhere, whereas `dp[m][n]` is just the run ending at the last two characters.

</details>
<details>
<summary><strong>Q:</strong> What does <code>dp[i][j]</code> mean for substring?</summary>

**A:** The length of the longest common substring ending exactly at `a[i-1]` and `b[j-1]`. A mismatch makes it 0 because no contiguous match can end there.

</details>
<details>
<summary><strong>Q:</strong> For <code>"abcde"</code> and <code>"abfce"</code>, why is the subsequence 4 but the substring 2?</summary>

**A:** The mismatch at position 3 is free for a subsequence (`max(left,up)` carries `"ab"` forward, then `c`,`e` extend to `"abce"`), but fatal for a substring (it resets the run, leaving only `"ab"` or `"ce"`).

</details>
<details>
<summary><strong>Q:</strong> Complexity, and a faster alternative?</summary>

**A:** `O(m·n)` time, space-optimisable to two rows. Generalised suffix trees / suffix automata achieve `O(m + n)`.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15.4 + exercises — the LCS table and its contiguous variant.
- **Gusfield**, *Algorithms on Strings, Trees, and Sequences* (1997) — longest common substring via generalised suffix trees in `O(m + n)`.
- **LeetCode** 718 (Maximum Length of Repeated Subarray) and 1143 (LCS, for contrast) are the canonical drills; the `4`, the subsequence-vs-substring `4`/`2`, and the `3`/`5` repeated-subarray outputs above come from the runnable blocks — re-run to verify.
