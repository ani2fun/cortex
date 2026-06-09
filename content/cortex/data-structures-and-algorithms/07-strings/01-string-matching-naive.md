---
title: Naive String Matching
summary: "The O(nm) baseline every string algorithm beats — slide the pattern over the text, compare char by char. Its fatal flaw is amnesia — after a partial match fails, it shifts by one and re-compares everything. Feeling that waste is what motivates KMP, Z, and Rabin-Karp."
prereqs:
  - linear-structures-strings-what-is-a-string
---

## Why It Exists

Find every place a pattern `P` (length `m`) appears inside a text `T` (length `n`). It's Ctrl-F, it's `grep`, it's searching a genome for a gene. The most direct idea works: line `P` up at position 0, compare character by character; if it doesn't match, slide one step right and try again — through all `n − m + 1` alignments.

This naive scan is the **floor** every string algorithm is measured against. It's `O(n·m)` in the worst case, and worth implementing once for two reasons: it's genuinely the right tool for short patterns or one-off searches, and feeling *where it wastes work* is exactly what motivates the cleverness of [KMP](/cortex/data-structures-and-algorithms/strings/kmp), Z, and rolling-hash methods. Those algorithms all exist to fix one flaw you'll see here: naive matching has **amnesia** — on a mismatch it forgets everything it just learned and restarts.

## See It Work

Slide the pattern across every start position; at each, compare until a mismatch or a full match.

```python run viz=array
def naive_search(text, pattern):
    n, m = len(text), len(pattern)
    hits = []
    for i in range(n - m + 1):                        # each alignment start
        j = 0
        while j < m and text[i + j] == pattern[j]:    # compare char by char
            j += 1
        if j == m:                                    # ran off the end of the pattern -> full match
            hits.append(i)
    return hits

print(naive_search("abxabcabcaby", "abcaby"))   # [6]
print(naive_search("aaaaa", "aa"))              # [0, 1, 2, 3]
```

```java run viz=array
import java.util.*;
public class Main {
    static List<Integer> naiveSearch(String text, String pattern) {
        int n = text.length(), m = pattern.length();
        List<Integer> hits = new ArrayList<>();
        for (int i = 0; i + m <= n; i++) {            // each alignment
            int j = 0;
            while (j < m && text.charAt(i + j) == pattern.charAt(j)) j++;
            if (j == m) hits.add(i);                  // full match
        }
        return hits;
    }
    public static void main(String[] args) {
        System.out.println(naiveSearch("abxabcabcaby", "abcaby"));   // [6]
        System.out.println(naiveSearch("aaaaa", "aa"));              // [0, 1, 2, 3]
    }
}
```

Both print `[6]` then `[0, 1, 2, 3]`. The pattern `"abcaby"` starts only at index 6; `"aa"` overlaps itself, matching at 0, 1, 2, 3. Each alignment does up to `m` comparisons, and there are `n − m + 1` of them — hence `O(n·m)`.

## How It Works

The algorithm is two nested loops: an outer slide over alignments, an inner char-by-char compare. The cost lives entirely in what happens on a **mismatch**:

```d2
direction: right
align: "alignment i: compare P against T[i..i+m-1]" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
match: "all m chars match -> record hit at i" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
mismatch: "mismatch after matching k chars\n-> shift to i+1 and start j back at 0" {style.fill: "#fecaca"; style.stroke: "#dc2626"}
waste: "AMNESIA: those k matched chars\nare re-compared at the next alignment\n(naive throws the work away)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
align -> match
align -> mismatch
mismatch -> waste
```

<p align="center"><strong>On a mismatch, naive matching shifts by exactly one and resets the comparison to the pattern's start — re-examining characters it already saw. That discarded knowledge is the inefficiency every smarter algorithm reclaims.</strong></p>

Two things to hold onto:

- **Best case `O(n)`, worst case `O(n·m)`.** If the first character usually mismatches (common in natural-language text over a large alphabet), each alignment costs one comparison and the whole search is roughly `O(n)`. But when the pattern keeps *almost* matching — many shared characters before the mismatch — each of the `~n` alignments does up to `m` comparisons. The next section makes that quadratic blow-up concrete.
- **The waste is re-comparison.** Suppose `P = "abcaby"` mismatches at the `y` after matching `"abcab"`. Naive shifts by one and re-checks from `P[0]` — even though it *already knows* the next five text characters were `"bcab…"`. KMP's insight is that the matched prefix tells you how far you can safely jump without re-reading; naive ignores it entirely.

> **Key takeaway.** Naive string matching slides the pattern over all `n − m + 1` alignments and compares char by char: best `O(n)`, worst `O(n·m)`. Its flaw is amnesia — a mismatch discards the partial-match information and restarts at the next position. Reclaiming that information is the whole point of [KMP](/cortex/data-structures-and-algorithms/strings/kmp), Z-algorithm, and rolling-hash matching.

## Trace It

People often quote naive matching as "basically linear" — true for random text, dangerously false for adversarial input. The killer case is lots of *near* matches.

**Predict before you run:** on text `"aaaaaaaaaaaaaaaaaaaa"` (twenty `a`s) searching for `"aaaab"` (four `a`s then a `b`), roughly how many character comparisons — about 20 (one per position), or far more?

```python run viz=array
def count_comparisons(text, pattern):
    n, m = len(text), len(pattern)
    cmps = 0
    for i in range(n - m + 1):
        j = 0
        while j < m and text[i + j] == pattern[j]:
            cmps += 1; j += 1
        if j < m:
            cmps += 1                                 # count the comparison that mismatched
    return cmps

N = 20
print("adversarial  ('a'*20 , 'aaaab'):", count_comparisons("a" * N, "aaaab"))
print("friendly     ('ab'*10, 'aaaab'):", count_comparisons("ab" * (N // 2), "aaaab"))
print("worst-case bound (n-m+1)*m     :", (N - 5 + 1) * 5)
```

<details>
<summary><strong>Reveal</strong></summary>

The adversarial case does **80** comparisons — exactly the `(n − m + 1)·m = 16·5 = 80` worst-case bound — while the friendly text does only **24**. Here's why: at *every* alignment in `"aaaa…a"`, the pattern `"aaaab"` matches its first four `a`s against four text `a`s, then fails on the `b` — five comparisons wasted per position, `~n` positions, so `~n·m` total. The friendly text `"ab abab…"` mismatches `"aaaab"` at the *second* character of most alignments (`b ≠ a`), so it bails after one or two comparisons — roughly linear. This is the gap that motivates the rest of Part 7: naive's `O(n·m)` is real and reachable, and it happens precisely when the text and pattern share long runs. [KMP](/cortex/data-structures-and-algorithms/strings/kmp) drives this to `O(n + m)` by never re-comparing a character it has already matched — turning those 80 comparisons back into roughly 20.

</details>

## Your Turn

**Implement strStr()** ([LeetCode 28](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/)) — return the index of the *first* occurrence of `needle` in `haystack`, or `-1`. It's the naive scan with an early return on the first full match.

```python run viz=array
def str_str(haystack, needle):
    n, m = len(haystack), len(needle)
    for i in range(n - m + 1):
        j = 0
        while j < m and haystack[i + j] == needle[j]:
            j += 1
        if j == m:
            return i                                  # first match — stop here
    return -1

print(str_str("sadbutsad", "sad"))     # 0
print(str_str("leetcode", "leeto"))    # -1
```

```java run viz=array
public class Main {
    static int strStr(String haystack, String needle) {
        int n = haystack.length(), m = needle.length();
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m && haystack.charAt(i + j) == needle.charAt(j)) j++;
            if (j == m) return i;                     // first match
        }
        return -1;
    }
    public static void main(String[] args) {
        System.out.println(strStr("sadbutsad", "sad"));    // 0
        System.out.println(strStr("leetcode", "leeto"));   // -1
    }
}
```

Both print `0` then `-1`. `"sad"` appears first at index 0; `"leeto"` never appears (it almost matches `"leetcode"` at index 0 — `leet` lines up — then fails at the fifth character, the exact near-match waste from above). This is the most-asked "implement substring search" interview question, and the naive answer is correct and often expected first — the optimisation to KMP is the follow-up.

## Reflect & Connect

- **The floor, and an honest one.** `O(n·m)` worst case, `O(n)` best. For short patterns, small texts, or one-off searches, naive is genuinely the right call — no preprocessing, no extra memory, trivial to get right.
- **Amnesia is the flaw.** A mismatch after a partial match throws away everything learned and restarts one position over. Every faster algorithm is a different answer to "what should we *remember* across a mismatch?"
- **What the successors remember.** [KMP](/cortex/data-structures-and-algorithms/strings/kmp) precomputes, from the *pattern alone*, how far to jump on a mismatch (the failure function) — `O(n + m)`. The Z-algorithm computes match-length information for the whole string. Rabin-Karp hashes windows so a comparison is usually one integer check, not `m` character checks.
- **The worst case is reachable, not theoretical.** Repetitive text (DNA `AAAA…`, `"aaaa"` runs, binary data) plus a near-matching pattern hits the full `O(n·m)`. That's exactly where you reach for a smarter algorithm.
- **Same scan, different verbs.** Find-all (collect every index), find-first (`strStr`, early return), contains (boolean) — all the one naive loop with a different terminal action.

## Recall

<details>
<summary><strong>Q:</strong> What is naive string matching's time complexity, best and worst?</summary>

**A:** Best `O(n)` (when most alignments mismatch on the first character); worst `O(n·m)` (when the pattern nearly matches at every alignment, e.g. repetitive text). There are `n − m + 1` alignments, each up to `m` comparisons.

</details>
<details>
<summary><strong>Q:</strong> What is the algorithm's core inefficiency?</summary>

**A:** Amnesia: on a mismatch after matching `k` characters, it shifts by exactly one and restarts the comparison at the pattern's first character, re-examining text it already saw. It discards the partial-match information instead of using it to skip ahead.

</details>
<details>
<summary><strong>Q:</strong> What input forces the <code>O(n·m)</code> worst case?</summary>

**A:** Highly repetitive text with a pattern that *almost* matches everywhere — e.g. text `"aaaa…a"` and pattern `"aaaab"`. Every alignment matches `m − 1` characters before failing, so each costs `~m` comparisons across `~n` positions.

</details>
<details>
<summary><strong>Q:</strong> How does KMP improve on naive matching, conceptually?</summary>

**A:** It precomputes (from the pattern alone) a failure function telling it how far to shift on a mismatch *without* re-comparing already-matched characters, achieving `O(n + m)`. It remembers what naive forgets.

</details>
<details>
<summary><strong>Q:</strong> When is naive matching actually the right choice?</summary>

**A:** Short patterns, small or one-off searches, or large/random alphabets where first-character mismatches keep it near `O(n)`. It needs no preprocessing or extra memory and is hard to get wrong — only reach for KMP/Z/Rabin-Karp when the worst case bites.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §32.1 — the naive string-matcher, its `O((n−m+1)m)` analysis, and the setup for KMP in §32.4.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §5.3 (Substring Search) — naive matching as the baseline before Knuth-Morris-Pratt and Boyer-Moore.
- **LeetCode** 28 (Find the Index of the First Occurrence) is the canonical drill; the `[6]` / `[0,1,2,3]` hit lists, the `80`-vs-`24` comparison counts, and the `0`/`-1` strStr results above come from the runnable blocks — re-run to verify.
