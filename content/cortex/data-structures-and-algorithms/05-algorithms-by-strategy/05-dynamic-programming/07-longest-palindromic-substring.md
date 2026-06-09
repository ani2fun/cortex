---
title: "Longest Palindromic Substring"
summary: "The longest CONTIGUOUS palindrome. A boolean interval DP — dp[i][j] = 'is s[i..j] a palindrome?' — true when the ends match AND the inside is already a palindrome. Track the longest true span. Or expand around each of the 2n-1 centers in O(1) space."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/06-longest-palindromic-subsequence
---

## Why It Exists

The [previous lesson](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-palindromic-subsequence) found the longest palindromic *subsequence* — characters that read the same both ways, gaps allowed. **Substring** is the contiguous version: the palindrome must be one continuous slice of the original, no skipping. That two-character change to the problem statement changes the DP entirely. The subsequence table *counted* (how long a palindrome can we build?); the substring table is **boolean** (is this exact slice a palindrome?), and the longest true slice is the answer.

It's the engine behind "find the longest mirror sequence" in DNA, the textbook demo for the `O(n)` Manacher trick, and the cleanest case where *contiguity* — not the matching rule — is what reshapes the algorithm.

## See It Work

`dp[i][j]` is `True` when `s[i..j]` reads the same both ways. The ends must match **and** the strictly-inside slice `s[i+1..j-1]` must itself be a palindrome. Fill by increasing length so that inner slice is already decided, and remember the longest `True` span.

```python run viz=grid
def longest_palindrome(s):
    n = len(s)
    if n < 2:
        return s
    dp = [[False] * n for _ in range(n)]
    for i in range(n):
        dp[i][i] = True                          # every single character is a palindrome
    start, best = 0, 1
    for length in range(2, n + 1):               # grow the window outward
        for i in range(n - length + 1):
            j = i + length - 1
            if s[i] == s[j] and (length == 2 or dp[i + 1][j - 1]):
                dp[i][j] = True                  # ends match AND the inside is already a palindrome
                if length > best:
                    start, best = i, length
    return s[start:start + best]

print(longest_palindrome("babad"))   # bab
print(longest_palindrome("cbbd"))    # bb
```

```java run viz=grid
public class Main {
    static String longestPalindrome(String s) {
        int n = s.length();
        if (n < 2) return s;
        boolean[][] dp = new boolean[n][n];
        for (int i = 0; i < n; i++) dp[i][i] = true;
        int start = 0, best = 1;
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j) && (len == 2 || dp[i + 1][j - 1])) {
                    dp[i][j] = true;                       // ends match + inside palindromic
                    if (len > best) { start = i; best = len; }
                }
            }
        return s.substring(start, start + best);
    }
    public static void main(String[] args) {
        System.out.println(longestPalindrome("babad"));   // bab
        System.out.println(longestPalindrome("cbbd"));    // bb
    }
}
```

Both print `bab` then `bb`. For `"babad"`, `"bab"` and `"aba"` both qualify — the length-first scan settles on the first one it reaches. Cost `O(n²)` time and `O(n²)` space.

## How It Works

A slice is a palindrome only if its two outer characters agree *and* everything between them already is — peel one matching pair off each end and the smaller slice must hold up.

```d2
direction: right
cell: "dp[i][j]\n(is s[i..j] a palindrome?)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
ends: "s[i] == s[j]?\n(the two ends must match)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
inner: "AND  (j - i < 2  OR  dp[i+1][j-1])\n(length <= 2 -> trivially true;\notherwise the INSIDE must already be a palindrome)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
result: "both true -> dp[i][j] = True\n(track the longest True span)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
cell -> ends
cell -> inner
ends -> result
inner -> result
```

<p align="center"><strong>Boolean recurrence: <code>dp[i][j] = (s[i]==s[j]) AND (j−i&lt;2 OR dp[i+1][j-1])</code>. The <code>j−i&lt;2</code> guard handles the two base shapes — a single char and a matched pair — so there's no inner slice left to check.</strong></p>

Two things to hold onto:

- **It's interval DP again** — the same shape as [LPS](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-palindromic-subsequence): `dp[i][j]` ranges over a *substring*, depends on the **inner** cell `dp[i+1][j-1]`, and must be filled by increasing length (a row-major loop would read the inside before it's computed). What changed from LPS is the cell's *type* — boolean "is it a palindrome?" instead of an integer length — and that the answer is the longest `True` span you saw, not `dp[0][n-1]`.
- **Expand around center is the lighter alternative.** A palindrome is defined by its center, and there are only `2n−1` of them (`n` single-character cores for odd lengths, `n−1` between-character cores for even). Expand outward from each while the mirror holds. Same `O(n²)` time but `O(1)` space — no table — and it's the implementation most people actually write. (Manacher's algorithm gets it to `O(n)`, but it's a specialist tool.)

> **Key takeaway.** Longest palindromic *substring* is a **boolean** interval DP: `dp[i][j] = (s[i]==s[j]) and (j−i<2 or dp[i+1][j-1])` — ends match *and* the inside is already a palindrome. Fill by increasing length; the answer is the longest `True` span. The `O(1)`-space twin is **expand-around-center** over the `2n−1` centers.

## Trace It

This is the substring/subsequence line drawn as sharply as it gets. Reuse `"bbbab"` from the [last lesson](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-palindromic-subsequence), where the longest palindromic *subsequence* was `4` — drop the lone `a` and keep `"bbbb"`.

**Predict before you run:** for `"bbbab"`, can the longest palindromic *substring* also reach `4`, or must it be shorter?

```python run viz=grid
def lps(s):                                       # longest palindromic SUBSEQUENCE (gaps allowed)
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
    return dp[0][n - 1]

def longest_palindrome(s):                        # longest palindromic SUBSTRING (contiguous)
    n = len(s)
    dp = [[False] * n for _ in range(n)]
    for i in range(n):
        dp[i][i] = True
    start, best = 0, 1
    for length in range(2, n + 1):
        for i in range(n - length + 1):
            j = i + length - 1
            if s[i] == s[j] and (length == 2 or dp[i + 1][j - 1]):
                dp[i][j] = True
                if length > best:
                    start, best = i, length
    return s[start:start + best]

print("subsequence length:", lps("bbbab"))
sub = longest_palindrome("bbbab")
print("substring:", sub, "length", len(sub))
```

<details>
<summary><strong>Reveal</strong></summary>

The subsequence is `4` (`"bbbb"`); the substring is `"bbb"`, length `3`. The four `b`s that make the subsequence sit at indices `0, 1, 2, 4` — the `a` at index `3` wedges between the third and fourth `b`. A *subsequence* is allowed to leap that `a` and collect all four; a *substring* cannot, so the best contiguous palindrome stops at `"bbb"`. The drop from `4` to `3` is exactly the character the gap-allowed version got to skip. That's the whole distinction made physical: **subsequence DP carries a value across a mismatch; substring DP halts at it.** And it's why the recurrences differ — LPS's mismatch branch *recovers* (`max(drop one end)`), while the substring's `dp[i][j]` is simply `False` the moment the inside isn't a palindrome.

</details>

## Your Turn

**Palindromic Substrings** ([LeetCode 647](https://leetcode.com/problems/palindromic-substrings/)) — *count* every palindromic substring (each position and length counts separately). It's the same "a palindrome is defined by its center" insight, now tallied: expand around all `2n−1` centers and add one for every step the mirror survives.

```python run viz=array
def count_substrings(s):
    n = len(s)
    total = 0

    def expand(l, r):
        c = 0
        while l >= 0 and r < n and s[l] == s[r]:  # mirror holds -> one more palindrome
            c += 1
            l -= 1
            r += 1
        return c

    for center in range(n):
        total += expand(center, center)           # odd-length cores (single char)
        total += expand(center, center + 1)       # even-length cores (between two chars)
    return total

print(count_substrings("aaa"))   # 6   ("a"x3, "aa"x2, "aaa")
print(count_substrings("abc"))   # 3   (each single char)
```

```java run viz=array
public class Main {
    static int countSubstrings(String s) {
        int n = s.length(), total = 0;
        for (int center = 0; center < n; center++) {
            total += expand(s, center, center);        // odd cores
            total += expand(s, center, center + 1);    // even cores
        }
        return total;
    }
    static int expand(String s, int l, int r) {
        int c = 0;
        while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) { c++; l--; r++; }
        return c;
    }
    public static void main(String[] args) {
        System.out.println(countSubstrings("aaa"));   // 6
        System.out.println(countSubstrings("abc"));   // 3
    }
}
```

Both print `6` then `3`. `"aaa"` has three single `a`s, two `"aa"`s, and one `"aaa"`; `"abc"` has only its three single characters. Each surviving expansion step is one more palindrome — the counting twin of "remember the longest span."

## Reflect & Connect

- **Boolean table, not a count.** LPS stored *how long* a palindrome you can build; this stores *whether* a slice is one. The longest answer is the longest `True` span, tracked as you fill — not a corner cell.
- **Still interval DP.** Range state `s[i..j]`, depends on the inner cell `dp[i+1][j-1]`, filled by increasing length. The fill-order discipline from LPS carries over unchanged.
- **Expand-around-center wins in practice.** `O(1)` space, no table, trivial to write: iterate the `2n−1` centers and expand. Reach for the DP table only when you need every cell (e.g. answering many "is `s[i..j]` a palindrome?" queries), and Manacher's `O(n)` when the input is huge.
- **Beware the false shortcut.** LPS equals `LCS(s, reverse(s))` — but longest palindromic *substring* does **not** equal the longest *common substring* of `s` and its reverse. That common substring can land off-center and need not be a palindrome (`"abacdfgdcaba"` is the classic trap). Contiguity breaks the reverse-string trick that subsequences enjoy.
- **One template, two questions.** Expand-around-center finds the *longest* (track the max span) or *counts* all ([LeetCode 647](https://leetcode.com/problems/palindromic-substrings/), add one per step). Same machinery, different accumulator — the recurring DP move.

## Recall

<details>
<summary><strong>Q:</strong> What is the boolean palindrome-substring recurrence?</summary>

**A:** `dp[i][j] = (s[i] == s[j]) and (j − i < 2 or dp[i+1][j-1])`. The ends must match, and either the slice has length ≤ 2 (nothing inside to check) or the inner slice `s[i+1..j-1]` is already a palindrome. The answer is the longest `i..j` with `dp[i][j] == True`.

</details>
<details>
<summary><strong>Q:</strong> Why must this be filled by increasing length?</summary>

**A:** `dp[i][j]` depends on the *inner* cell `dp[i+1][j-1]`. Iterating by increasing window length (or `i` descending) guarantees that smaller, inner slice is decided first; a plain row-major loop would read it before it's set.

</details>
<details>
<summary><strong>Q:</strong> How does expand-around-center work, and what does it cost?</summary>

**A:** A palindrome is fixed by its center; there are `2n − 1` centers (`n` single-character for odd lengths, `n − 1` between-character for even). Expand outward from each while `s[l] == s[r]`. `O(n²)` time but `O(1)` space — no table.

</details>
<details>
<summary><strong>Q:</strong> For <code>"bbbab"</code>, why is the subsequence 4 but the substring 3?</summary>

**A:** The four `b`s sit at indices 0, 1, 2, 4 — the `a` at index 3 separates the last `b`. A subsequence may skip the `a` and take all four (`"bbbb"`); a substring is contiguous and stops at `"bbb"`.

</details>
<details>
<summary><strong>Q:</strong> Does longest palindromic substring equal the longest common substring of <code>s</code> and <code>reverse(s)</code>?</summary>

**A:** No. Unlike LPS (which *does* equal `LCS(s, reverse(s))`), the longest common *substring* of `s` and its reverse can be off-center and need not be a palindrome (`"abacdfgdcaba"`). Use the boolean DP or expand-around-center instead.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15 — interval DP and the fill-by-length ordering this reuses.
- **Manacher** (1975), "A new linear-time on-line algorithm for finding the smallest initial palindrome of a string" — the `O(n)` longest-palindromic-substring algorithm.
- **LeetCode** 5 (Longest Palindromic Substring) and 647 (Palindromic Substrings) are the canonical drills; the `bab`/`bb`, the subsequence-vs-substring `4`/`3`, and the `6`/`3` counts above all come from the runnable blocks — re-run to verify.
