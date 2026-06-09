---
title: "Longest Palindromic Subsequence"
summary: "The longest subsequence that reads the same both ways. The first INTERVAL DP: state dp[i][j] over a substring i..j, filled by increasing length so the inner range is ready — s[i]==s[j] adds 2 and steps inward, else drop one end. And it's secretly LCS(s, reverse(s))."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/03-longest-common-subsequence
---

## Why It Exists

A palindrome reads the same forward and backward — `racecar`, `level`, `noon`. Given a string, what's the longest *subsequence* of it that's a palindrome? You don't need consecutive characters; pick any subset that preserves order, as long as the result reads identically both ways. For `"bbbab"` the answer is `"bbbb"` (length 4) — drop the lone `a`.

Every DP so far indexed its state by a *prefix*: "the first `i` characters." Palindromes are anchored at *both* ends — a palindrome's first and last characters must match — so the natural subproblem is a **substring** `s[i..j]`, indexed by *two* endpoints that close inward. That's **interval DP**: the state is a range, and you build long ranges from shorter inner ones. LPS is the gentlest introduction to that shape, and it hides a lovely shortcut — it's exactly `LCS(s, reverse(s))`.

## See It Work

`dp[i][j]` = length of the longest palindromic subsequence of the substring `s[i..j]`. If the two ends match they contribute 2 and we recurse on the inside; otherwise we drop one end and keep the better side. Fill by **increasing length** so the inner range `dp[i+1][j-1]` is already done.

```python run viz=grid
def lps(s):
    n = len(s)
    if n == 0:
        return 0
    dp = [[0] * n for _ in range(n)]
    for i in range(n):
        dp[i][i] = 1                              # base: a single char is a palindrome of length 1
    for length in range(2, n + 1):               # grow the window outward
        for i in range(n - length + 1):
            j = i + length - 1
            if s[i] == s[j]:
                inner = dp[i + 1][j - 1] if length > 2 else 0
                dp[i][j] = inner + 2             # ends match: keep both, step inward
            else:
                dp[i][j] = max(dp[i + 1][j], dp[i][j - 1])   # drop the left end OR the right end
    return dp[0][n - 1]

print(lps("bbbab"))   # 4   ("bbbb")
print(lps("cbbd"))    # 2   ("bb")
```

```java run viz=grid
public class Main {
    static int lps(String s) {
        int n = s.length();
        if (n == 0) return 0;
        int[][] dp = new int[n][n];
        for (int i = 0; i < n; i++) dp[i][i] = 1;            // base
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j))
                    dp[i][j] = (len > 2 ? dp[i + 1][j - 1] : 0) + 2;   // match: +2 inward
                else
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);   // drop one end
            }
        return dp[0][n - 1];
    }
    public static void main(String[] args) {
        System.out.println(lps("bbbab"));   // 4
        System.out.println(lps("cbbd"));    // 2
    }
}
```

Both print `4` then `2`. `"bbbab"` keeps the four `b`s; `"cbbd"` keeps `"bb"`. Cost `O(n²)` time and space — one fill per `(i, j)` pair with `i ≤ j`.

## How It Works

Interval DP turns the recurrence inside out: instead of extending a prefix, you examine the two *ends* of a window and shrink inward.

```d2
direction: right
cell: "dp[i][j]\n(longest palindromic subseq of s[i..j])" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
match: "s[i] == s[j]?\nMATCH -> dp[i+1][j-1] + 2\n(keep BOTH ends, recurse on the inside)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
mismatch: "MISMATCH -> max(dp[i+1][j], dp[i][j-1])\n(drop the left end, or drop the right end)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
base: "i == j -> 1\n(single char)\ni > j -> 0\n(empty)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
cell -> match
cell -> mismatch
cell -> base: "base"
```

<p align="center"><strong>A palindrome is anchored at both ends: if <code>s[i]</code> and <code>s[j]</code> agree they both belong, contributing 2 around whatever the inside yields; if they disagree at most one of them can be in the answer, so try discarding each.</strong></p>

Two things make interval DP feel different from the prefix DPs:

- **The fill order is by length, not by index.** `dp[i][j]` depends on a *shorter, inner* interval `dp[i+1][j-1]` (and the length-`(j−i)` neighbours `dp[i+1][j]`, `dp[i][j-1]`). A plain row-major `for i … for j` loop would read `dp[i+1][…]` before it's computed. Iterating by increasing `length` (equivalently, `i` descending) guarantees every inner range is finished first. This length-first ordering is the signature of interval DP — you'll see it again in matrix-chain multiplication and palindrome partitioning.
- **It's secretly LCS.** A palindromic subsequence of `s` is precisely a subsequence common to `s` and its reverse — read one forward, the other backward, and "reads the same both ways" *is* "appears in both." So `LPS(s) == LCS(s, reverse(s))`, and you can skip the interval recurrence entirely if you've already written LCS. (The [Trace It](#trace-it) below verifies this.)

> **Key takeaway.** LPS is the first **interval DP**: `dp[i][j]` over a substring `s[i..j]`, base `dp[i][i] = 1`, recurrence `dp[i+1][j-1] + 2` when the ends match else `max(dp[i+1][j], dp[i][j-1])`. Fill by **increasing length** so the inner range is ready — that ordering is what distinguishes interval DP from prefix DP. Equivalent shortcut: `LPS(s) = LCS(s, reverse(s))`.

## Trace It

The LPS ↔ LCS connection is the kind of identity that sounds too neat to be true, so verify it. A palindromic subsequence reads the same both ways; reverse the string and that same subsequence appears in the reversal — making it a *common* subsequence of `s` and `reverse(s)`.

**Predict before you run:** for `"character"`, will the interval-DP `lps(s)` and the prefix-DP `lcs(s, reverse(s))` give the *same* number — or can they drift apart?

```python run viz=grid
def lps(s):                                          # interval DP
    n = len(s)
    dp = [[0] * n for _ in range(n)]
    for i in range(n):
        dp[i][i] = 1
    for length in range(2, n + 1):
        for i in range(n - length + 1):
            j = i + length - 1
            if s[i] == s[j]:
                dp[i][j] = (dp[i + 1][j - 1] if length > 2 else 0) + 2
            else:
                dp[i][j] = max(dp[i + 1][j], dp[i][j - 1])
    return dp[0][n - 1] if n else 0

def lcs(a, b):                                       # the LCS from two lessons ago
    m, n = len(a), len(b)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            dp[i][j] = dp[i - 1][j - 1] + 1 if a[i - 1] == b[j - 1] else max(dp[i - 1][j], dp[i][j - 1])
    return dp[m][n]

for s in ["character", "bbbab", "cbbd", "agbdba"]:
    print(f"{s:>10}:  lps={lps(s)}   lcs(s, reverse)={lcs(s, s[::-1])}")
```

<details>
<summary><strong>Reveal</strong></summary>

They match every time — `character` gives `5` from both (`"carac"`, or `"cabac"`-style picks), `bbbab` gives `4`, `cbbd` gives `2`, `agbdba` gives `5`. The two algorithms are computing the same thing by different routes. The reason is a bijection: a subsequence `t` of `s` is a palindrome **iff** `t` reads identically in `s` and in `reverse(s)` — i.e. `t` is a common subsequence of the two. So the *longest* palindromic subsequence of `s` is the *longest* common subsequence of `s` and its reverse, exactly. The interval DP is the direct, single-string formulation; the LCS reduction is the "I already wrote that" shortcut. Both are `O(n²)`; pick whichever you can reproduce under pressure.

</details>

## Your Turn

**Minimum Insertions to Make a String Palindrome** ([LeetCode 1312](https://leetcode.com/problems/minimum-insertion-steps-to-make-a-string-palindrome/)). The characters *already* in the longest palindromic subsequence can stay put; every *other* character needs a mirror inserted. So the answer is `n − LPS(s)` — one subtraction on top of the same table.

```python run viz=grid
def lps(s):
    n = len(s)
    if n == 0:
        return 0
    dp = [[0] * n for _ in range(n)]
    for i in range(n):
        dp[i][i] = 1
    for length in range(2, n + 1):
        for i in range(n - length + 1):
            j = i + length - 1
            if s[i] == s[j]:
                dp[i][j] = (dp[i + 1][j - 1] if length > 2 else 0) + 2
            else:
                dp[i][j] = max(dp[i + 1][j], dp[i][j - 1])
    return dp[0][n - 1]

def min_insertions(s):
    return len(s) - lps(s)                       # keep the LPS, mirror everything else

print(min_insertions("mbadm"))   # 2   (LPS "mam"/"mbm" = 3, so 5 - 3)
print(min_insertions("zzazz"))   # 0   (already a palindrome)
```

```java run viz=grid
public class Main {
    static int lps(String s) {
        int n = s.length();
        if (n == 0) return 0;
        int[][] dp = new int[n][n];
        for (int i = 0; i < n; i++) dp[i][i] = 1;
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j))
                    dp[i][j] = (len > 2 ? dp[i + 1][j - 1] : 0) + 2;
                else
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);
            }
        return dp[0][n - 1];
    }
    static int minInsertions(String s) { return s.length() - lps(s); }
    public static void main(String[] args) {
        System.out.println(minInsertions("mbadm"));   // 2
        System.out.println(minInsertions("zzazz"));   // 0
    }
}
```

Both print `2` then `0`. `"mbadm"` has LPS `3` (`"mam"` or `"mbm"`), so two characters lack a mirror; `"zzazz"` is already a palindrome, so its LPS is the whole string and nothing needs inserting. The same `n − LPS` formula also counts the minimum *deletions* to reach a palindrome — delete instead of mirror, the leftover is the same.

## Reflect & Connect

- **First interval DP.** State is a *range* `s[i..j]`, not a prefix; long ranges are built from shorter inner ranges, so you fill by **increasing length** (or `i` descending). Matrix-chain multiplication, palindrome partitioning, and optimal BST all share this skeleton.
- **The fill order is load-bearing.** `dp[i][j]` reads `dp[i+1][j-1]` — an *inner* cell. Naïve row-major iteration computes it in the wrong order and silently returns garbage; length-first ordering is the fix.
- **LPS = LCS(s, reverse(s)).** A palindromic subsequence is a subsequence common to a string and its reverse. If you remember [LCS](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-common-subsequence), you already know LPS — feed it `s` and `s[::-1]`.
- **`n − LPS` is a free corollary.** Minimum insertions (or deletions) to make `s` a palindrome is `n − LPS(s)`: keep the palindromic core, fix everything else. Same table, one subtraction — the recurring DP move of deriving a second answer from a solved subproblem.
- **Subsequence, not substring.** This is the gappy version; the contiguous *longest palindromic **substring*** (next lesson) can't drop the middle and needs a different expansion. Same "palindromes anchor at both ends" intuition, different table.

## Recall

<details>
<summary><strong>Q:</strong> What is the LPS recurrence and its base case?</summary>

**A:** `dp[i][j]` = LPS of `s[i..j]`. Base `dp[i][i] = 1`. If `s[i] == s[j]`: `dp[i+1][j-1] + 2` (keep both ends, recurse inside; `+2` straight to 2 when the window is length 2). Else `max(dp[i+1][j], dp[i][j-1])` (drop one end). Answer `dp[0][n-1]`; cost `O(n²)`.

</details>
<details>
<summary><strong>Q:</strong> Why must the table be filled by increasing length instead of the usual row-major order?</summary>

**A:** `dp[i][j]` depends on the *inner* interval `dp[i+1][j-1]` (and `dp[i+1][j]`, `dp[i][j-1]`). A plain `for i ascending … for j` loop reads `dp[i+1][…]` before it's been computed. Iterating by increasing window length (equivalently `i` descending) guarantees every shorter, inner range is finished first.

</details>
<details>
<summary><strong>Q:</strong> How is LPS equivalent to LCS?</summary>

**A:** `LPS(s) = LCS(s, reverse(s))`. A subsequence of `s` is a palindrome iff it also appears in `reverse(s)` — i.e. it's a *common* subsequence of the two. So the longest palindromic subsequence equals the longest common subsequence of `s` and its reversal.

</details>
<details>
<summary><strong>Q:</strong> What does the match case contribute, and why <code>+2</code>?</summary>

**A:** When `s[i] == s[j]`, both endpoints can sit at the two ends of a palindrome, wrapping whatever the inner range `s[i+1..j-1]` produces. They add two characters (one on each side), hence `dp[i+1][j-1] + 2`.

</details>
<details>
<summary><strong>Q:</strong> How does LPS give the minimum insertions to make a string a palindrome?</summary>

**A:** Keep the longest palindromic subsequence in place; every other character needs a mirror inserted (or, equivalently, deleted). That's `n − LPS(s)` — one subtraction on the same DP table.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15.2 (matrix-chain) and §15.4 (LCS) — interval-DP fill order and the LCS table LPS reduces to.
- **LeetCode** 516 (Longest Palindromic Subsequence) and 1312 (Minimum Insertion Steps to Make a String Palindrome) are the canonical drills; the `4`/`2`, the `lps == lcs(s, reverse)` match across four strings, and the `2`/`0` insertion counts above all come from the runnable blocks — re-run to verify.
