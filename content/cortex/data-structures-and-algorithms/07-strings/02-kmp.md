---
title: KMP (Knuth-Morris-Pratt)
summary: "O(n + m) string matching that cures naive's amnesia. Precompute the failure function (longest prefix that is also a suffix) so a mismatch jumps the pattern ahead instead of restarting — the text pointer never moves backward."
prereqs:
  - strings-string-matching-naive
---

## Why It Exists

[Naive matching](/cortex/data-structures-and-algorithms/strings-string-matching-naive) has amnesia: mismatch after a partial match, and it slides one step right and re-compares from scratch — `O(n·m)` on repetitive text. But a partial match is *information*. If the pattern is `ABCDABE` and you matched `ABCDAB` before failing on `E`, you already know the last two matched text characters were `AB` — which happen to be the pattern's first two characters. So you can resume the match at pattern index 2 *without re-reading the text*.

KMP turns that observation into an algorithm. It precomputes, from the **pattern alone**, a *failure function* that says "on a mismatch after matching `k` characters, how far can I safely jump?" The result is `O(n + m)` with the text pointer **never moving backward** — exactly the amnesia cure naive was missing.

## See It Work

The failure function `lps[i]` (longest proper prefix that is also a suffix of `P[0..i]`) is the whole trick. Build it once, then scan the text: on a mismatch, fall the pattern index back to `lps[j-1]` instead of 0.

```python run viz=array
def build_lps(p):
    m = len(p)
    lps = [0] * m
    k = 0                                            # length of current prefix that is also a suffix
    for i in range(1, m):
        while k > 0 and p[i] != p[k]:
            k = lps[k - 1]                           # fall back to a shorter prefix-suffix
        if p[i] == p[k]:
            k += 1
        lps[i] = k
    return lps

def kmp_search(text, pattern):
    lps = build_lps(pattern)
    n, m = len(text), len(pattern)
    hits, j = [], 0                                  # j = chars of pattern matched so far
    for i in range(n):                               # i (text pointer) only ever moves FORWARD
        while j > 0 and text[i] != pattern[j]:
            j = lps[j - 1]                           # JUMP, don't restart
        if text[i] == pattern[j]:
            j += 1
        if j == m:
            hits.append(i - m + 1)
            j = lps[j - 1]                           # keep going (handles overlaps)
    return hits

print(build_lps("ababaca"))                          # [0, 0, 1, 2, 3, 0, 1]
print(kmp_search("abxabcabcaby", "abcaby"))          # [6]
```

```java run viz=array
import java.util.*;
public class Main {
    static int[] buildLps(String p) {
        int m = p.length(); int[] lps = new int[m]; int k = 0;
        for (int i = 1; i < m; i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = lps[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            lps[i] = k;
        }
        return lps;
    }
    static List<Integer> kmpSearch(String text, String pattern) {
        int[] lps = buildLps(pattern);
        int n = text.length(), m = pattern.length(), j = 0;
        List<Integer> hits = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = lps[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == m) { hits.add(i - m + 1); j = lps[j - 1]; }
        }
        return hits;
    }
    public static void main(String[] args) {
        System.out.println(Arrays.toString(buildLps("ababaca")));   // [0, 0, 1, 2, 3, 0, 1]
        System.out.println(kmpSearch("abxabcabcaby", "abcaby"));    // [6]
    }
}
```

Both print the `lps` array `[0, 0, 1, 2, 3, 0, 1]` then `[6]`. The pattern `"abcaby"` matches only at index 6 — the same answer naive gives, but the text pointer here never rewinds. Build `O(m)`, search `O(n)`, total `O(n + m)`.

## How It Works

On a mismatch, the matched prefix `P[0..j-1]` is also (by definition) the text just read. The longest part of that prefix which is *also a suffix* can stay aligned — so the pattern slides forward by `j - lps[j-1]` and the comparison resumes at `lps[j-1]`:

```d2
direction: right
matched: "matched j chars of P against text\n(then text[i] != pattern[j])" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
naive: "NAIVE: shift pattern by 1, j -> 0\nre-compare from scratch" {style.fill: "#fecaca"; style.stroke: "#dc2626"}
kmp: "KMP: j -> lps[j-1]\nthe matched prefix's longest prefix=suffix\nstays aligned; text pointer i does NOT move" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
why: "lps[j-1] = how much we can KEEP\n-> never re-read a matched text char -> O(n+m)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
matched -> naive
matched -> kmp
kmp -> why
```

<p align="center"><strong>Naive resets <code>j</code> to 0 on a mismatch and re-reads text; KMP falls <code>j</code> back to <code>lps[j-1]</code> — the length of the longest pattern-prefix that's also a suffix of what just matched — keeping that overlap aligned and leaving the text pointer where it is.</strong></p>

Three load-bearing facts:

- **`lps[i]` = longest proper prefix of `P[0..i]` that is also a suffix.** "Proper" means not the whole thing. For `"ababaca"`: `lps[3] = 2` because `"abab"`'s longest prefix=suffix is `"ab"`. This is computed from the pattern *only* — no text needed — in `O(m)` by matching the pattern against itself.
- **The text pointer never moves backward.** In `kmp_search`, `i` only increments; all the "rewinding" happens on `j` (the pattern pointer) via `lps`. Each text character is examined a bounded number of times, giving `O(n)` for the scan. That's the precise sense in which KMP "remembers."
- **The failure function is built by the same self-matching trick.** `build_lps` runs the matching idea on the pattern against itself: `k` tracks the current prefix-suffix length and falls back via `lps[k-1]` on a mismatch — the recursion that makes the precompute itself `O(m)`, not `O(m²)`.

> **Key takeaway.** KMP matches in `O(n + m)` using the failure function `lps[i]` = longest proper prefix of `P[0..i]` that is also a suffix. On a mismatch after matching `j` chars, set `j = lps[j-1]` (jump) instead of `j = 0` (restart); the text pointer never rewinds. It's naive matching with the partial-match information *kept* instead of discarded.

## Trace It

The failure function is most surprising on the very input that wrecked naive matching: a run of identical characters.

**Predict before you run:** what is the failure function `lps` of `"aaaa"` — is it `[0, 0, 0, 0]` (a single repeated character, "no structure"), or something else?

```python run viz=array
def build_lps(p):
    m = len(p); lps = [0] * m; k = 0
    for i in range(1, m):
        while k > 0 and p[i] != p[k]:
            k = lps[k - 1]
        if p[i] == p[k]:
            k += 1
        lps[i] = k
    return lps

print("lps('aaaa') :", build_lps("aaaa"))
print("lps('aaaab'):", build_lps("aaaab"))
```

<details>
<summary><strong>Reveal</strong></summary>

`lps("aaaa")` is `[0, 1, 2, 3]`, not `[0, 0, 0, 0]`. A run of identical characters is *maximally* self-similar: every proper prefix `"a"`, `"aa"`, `"aaa"` is also a suffix of the string so far, so `lps` climbs `0, 1, 2, 3`. This is the mirror image of why naive matching *died* on `"aaaa…a"` text — the pattern overlaps itself everywhere. But for KMP that same self-similarity is *exploited*, not suffered: on `"aaaab"` (`lps = [0, 1, 2, 3, 0]`), a mismatch on the `b` after matching four `a`s falls `j` back to `lps[3] = 3`, so KMP re-uses three already-matched `a`s and only re-checks the new position — turning naive's `O(n·m)` re-scanning into a single forward sweep. On the exact adversarial input from the last lesson (`"a"*20` searching `"aaaab"`), KMP does about 36 character comparisons versus naive's 80, and the gap widens linearly with text length. The structure naive *suffered from* is the structure KMP *runs on*.

</details>

## Your Turn

**Repeated Substring Pattern** ([LeetCode 459](https://leetcode.com/problems/repeated-substring-pattern/)) — does the string consist of a smaller substring repeated? The failure function answers it for free: `n - lps[n-1]` is the string's shortest *period*, and the string is a clean repetition iff that period divides `n` (and there's a nontrivial overlap).

```python run viz=array
def build_lps(s):
    m = len(s); lps = [0] * m; k = 0
    for i in range(1, m):
        while k > 0 and s[i] != s[k]:
            k = lps[k - 1]
        if s[i] == s[k]:
            k += 1
        lps[i] = k
    return lps

def repeated_substring(s):
    n = len(s)
    lps = build_lps(s)
    period = n - lps[n - 1]                           # shortest period candidate
    return lps[n - 1] > 0 and n % period == 0

print(repeated_substring("abab"))        # True   ("ab" x 2)
print(repeated_substring("aba"))         # False
print(repeated_substring("abcabcabc"))   # True   ("abc" x 3)
```

```java run viz=array
public class Main {
    static int[] buildLps(String s) {
        int m = s.length(); int[] lps = new int[m]; int k = 0;
        for (int i = 1; i < m; i++) {
            while (k > 0 && s.charAt(i) != s.charAt(k)) k = lps[k - 1];
            if (s.charAt(i) == s.charAt(k)) k++;
            lps[i] = k;
        }
        return lps;
    }
    static boolean repeatedSubstring(String s) {
        int n = s.length(); int[] lps = buildLps(s); int period = n - lps[n - 1];
        return lps[n - 1] > 0 && n % period == 0;
    }
    public static void main(String[] args) {
        System.out.println(repeatedSubstring("abab"));        // true
        System.out.println(repeatedSubstring("aba"));         // false
        System.out.println(repeatedSubstring("abcabcabc"));   // true
    }
}
```

Both print `true`, `false`, `true`. `"abab"` has period `4 - 2 = 2` (`"ab"` repeats); `"aba"` has period `3 - 1 = 2`, which doesn't divide 3, so it's not a clean repetition; `"abcabcabc"` has period 3. The failure function encodes the deepest self-overlap, and that overlap *is* the period — a slick reuse of KMP's precompute for a problem that has nothing obviously to do with searching.

## Reflect & Connect

- **The failure function is the whole algorithm.** `lps[i]` = longest proper prefix that is also a suffix, computed from the pattern alone in `O(m)`. Everything else is bookkeeping around "on a mismatch, jump to `lps[j-1]`."
- **The text pointer never rewinds.** That's the formal reason for `O(n)` matching — and why KMP streams: it can match against input it reads once and can't seek back through (a network socket, a huge file).
- **Self-similarity is fuel, not friction.** The repetitive patterns that force naive's worst case are exactly the ones KMP exploits, because a high `lps` means a long safe jump. `lps("aaaa") = [0,1,2,3]` is the extreme.
- **The period falls out for free.** `n - lps[n-1]` is the shortest period; this powers repeated-substring detection and string-rotation checks with zero extra work.
- **It generalises.** The [Z-algorithm](/cortex/data-structures-and-algorithms/strings-z-algorithm) computes related prefix-match lengths for the whole string; [Aho-Corasick](/cortex/data-structures-and-algorithms/strings-aho-corasick) extends the failure-function idea to *many* patterns at once (a failure-link trie). KMP is the gateway to all of them.

## Recall

<details>
<summary><strong>Q:</strong> What does <code>lps[i]</code> mean?</summary>

**A:** The length of the longest *proper* prefix of `P[0..i]` that is also a suffix of `P[0..i]` ("proper" = not the whole substring). It's computed from the pattern alone, in `O(m)`.

</details>
<details>
<summary><strong>Q:</strong> On a mismatch after matching <code>j</code> characters, what does KMP do?</summary>

**A:** It sets `j = lps[j-1]` (fall back to the longest prefix-suffix overlap) instead of `j = 0`, and does *not* move the text pointer. The matched overlap stays aligned, so no already-read text character is re-examined.

</details>
<details>
<summary><strong>Q:</strong> Why is KMP <code>O(n + m)</code>?</summary>

**A:** Building `lps` is `O(m)`; the search is `O(n)` because the text pointer only moves forward and total `j`-decreases are bounded by total `j`-increases. No backtracking on the text.

</details>
<details>
<summary><strong>Q:</strong> What is the failure function of <code>"aaaa"</code>, and why?</summary>

**A:** `[0, 1, 2, 3]`. A run of identical characters is maximally self-similar — every proper prefix is also a suffix — so `lps` increases by 1 each position. (Not `[0,0,0,0]`.)

</details>
<details>
<summary><strong>Q:</strong> How does the failure function reveal a string's period?</summary>

**A:** `n - lps[n-1]` is the shortest period. The string is a clean repetition of a smaller block iff that period divides `n` and `lps[n-1] > 0` — the basis for repeated-substring detection.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §32.4 — the Knuth-Morris-Pratt algorithm, the prefix (failure) function, and the `O(n + m)` amortized analysis.
- **Knuth, Morris & Pratt** (1977), "Fast Pattern Matching in Strings", *SIAM J. Comput.* — the original.
- **LeetCode** 28 (Find the Index of the First Occurrence) and 459 (Repeated Substring Pattern) are the canonical drills; the `[0,0,1,2,3,0,1]` failure function, the `[6]` match, the `lps("aaaa") = [0,1,2,3]`, and the `true`/`false`/`true` repetition checks above come from the runnable blocks — re-run to verify.
