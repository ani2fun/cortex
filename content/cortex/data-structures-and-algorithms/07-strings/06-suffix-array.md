---
title: Suffix Array
summary: "The start indices of every suffix, sorted lexicographically — the compact alternative to a suffix tree. Substring search becomes binary search (O(m log n)); the companion LCP array (overlap of adjacent sorted suffixes) yields longest-repeated-substring and distinct-substring counts."
prereqs:
  - strings-string-matching-naive
  - sorting-and-searching-searching-binary-search
---

## Why It Exists

So far each matching algorithm preprocessed the *pattern* and scanned the text once. But many tasks fix the *text* and ask question after question about it: search many patterns, find the longest repeated block, count distinct substrings. For those you preprocess the **text** into an index — and the suffix array is the workhorse one.

A **suffix array** is just the list of start indices of every suffix, sorted by the suffix lexicographically. That sort is the magic: every occurrence of a pattern `P` is the start of some suffix that has `P` as a prefix, and once the suffixes are sorted, all suffixes sharing prefix `P` sit in one contiguous block — so substring search becomes **binary search**, `O(m log n)`. It's the compact cousin of the suffix *tree*: a suffix tree is powerful but memory-hungry (nodes, children, pointers); a suffix array is `n` integers plus a companion **LCP array**, giving most of the power at a fraction of the space. That's why it underpins full-text indexes, the Burrows-Wheeler transform, and genome search.

## See It Work

Build the array by sorting suffix indices; search a pattern by binary-searching for the block of suffixes that start with it.

```python run viz=array
def suffix_array(s):
    return sorted(range(len(s)), key=lambda i: s[i:])     # naive O(n^2 log n) build

def sa_contains(s, pattern):
    sa = suffix_array(s)
    lo, hi = 0, len(sa)
    while lo < hi:                                        # bisect for the first suffix >= pattern
        mid = (lo + hi) // 2
        if s[sa[mid]:] < pattern:
            lo = mid + 1
        else:
            hi = mid
    return lo < len(sa) and s[sa[lo]:].startswith(pattern)

s = input()
pattern = input()
print(suffix_array(s))
print("true" if sa_contains(s, pattern) else "false")
```

```java run viz=array
import java.util.*;
public class Main {
    static Integer[] suffixArray(String s) {
        Integer[] sa = new Integer[s.length()];
        for (int i = 0; i < s.length(); i++) sa[i] = i;
        Arrays.sort(sa, (x, y) -> s.substring(x).compareTo(s.substring(y)));
        return sa;
    }
    static boolean saContains(String s, String pattern) {
        Integer[] sa = suffixArray(s);
        int lo = 0, hi = sa.length;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (s.substring(sa[mid]).compareTo(pattern) < 0) lo = mid + 1; else hi = mid;
        }
        return lo < sa.length && s.substring(sa[lo]).startsWith(pattern);
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        String pattern = sc.nextLine();
        System.out.println(Arrays.toString(suffixArray(s)));
        System.out.println(saContains(s, pattern));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "string", "type": "string", "placeholder": "banana" },
    { "id": "pattern", "label": "pattern", "type": "string", "placeholder": "ana" }
  ],
  "cases": [
    { "args": { "s": "banana", "pattern": "ana" }, "expected": "[5, 3, 1, 0, 4, 2]\ntrue" },
    { "args": { "s": "banana", "pattern": "xyz" }, "expected": "[5, 3, 1, 0, 4, 2]\nfalse" },
    { "args": { "s": "abcdef", "pattern": "abc" }, "expected": "[0, 1, 2, 3, 4, 5]\ntrue" },
    { "args": { "s": "aaaaaa", "pattern": "aaa" }, "expected": "[5, 4, 3, 2, 1, 0]\ntrue" }
  ]
}
```

Both print `[5, 3, 1, 0, 4, 2]` then `true`, `false`. Index 5 (`"a"`) sorts first, index 2 (`"nana"`) last. Search is `O(m log n)` after the build; this naive build is `O(n² log n)` (mention only — production uses `O(n log n)` prefix-doubling or `O(n)` SA-IS).

## How It Works

The sorted order is everything. Lay out the sorted suffixes of `"banana"` and two facts pop out:

```d2
direction: down
list: "sorted suffixes of 'banana' (suffix array = their start indices)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
s0: "idx 5:  a          | lcp -" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
s1: "idx 3:  ana        | lcp 1" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
s2: "idx 1:  anana      | lcp 3  <- 'ana' repeated!" {style.fill: "#fde68a"; style.stroke: "#d97706"}
s3: "idx 0:  banana     | lcp 0" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
s4: "idx 4:  na         | lcp 0" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
s5: "idx 2:  nana       | lcp 2" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
list -> s0
s0 -> s1
s1 -> s2
s2 -> s3
s3 -> s4
s4 -> s5
```

<p align="center"><strong>Sorting clusters suffixes with a shared prefix into a contiguous block (the three <code>a…</code> suffixes lead), so pattern search is binary search. The LCP value beside each row is its overlap with the previous suffix; the maximum LCP (3, between <code>ana</code> and <code>anana</code>) is the longest repeated substring.</strong></p>

Three load-bearing ideas:

- **Search is binary search because the sort makes occurrences contiguous.** Any occurrence of `P` starts a suffix with `P` as a prefix; sorted, all such suffixes form one band. Bisect for `P`'s lower bound, check the prefix — `O(m log n)` (the `m` is the per-comparison string cost). No suffix-tree pointers needed.
- **The LCP array is the other half of the power.** `lcp[i]` = the longest common prefix of the `i`-th and `(i-1)`-th sorted suffixes. Adjacent sorted suffixes are the *most similar* pair containing each, so `lcp` encodes all the "how much do substrings overlap?" information — and it's buildable in `O(n)` (Kasai's algorithm) given the suffix array.
- **It's the compact alternative to a suffix tree.** A suffix tree answers the same queries (often in `O(m)`), but costs many nodes with child maps and suffix links. A suffix array is `n` integers + the LCP array — far less memory, cache-friendlier, and enough for most problems. That space win is why it won in practice (bioinformatics, BWT/bzip2, search engines).

> **Key takeaway.** A suffix array is the sorted start-indices of all suffixes. Sorting clusters each pattern's occurrences into a contiguous band, so **substring search is binary search** (`O(m log n)`). The companion **LCP array** (overlap of adjacent sorted suffixes) gives the longest repeated substring (max LCP) and the distinct-substring count — all in `O(n)` space, the compact alternative to a suffix tree.

## Trace It

The LCP array turns "find the longest repeated substring" — which sounds like an `O(n³)` nightmare — into a single pass over adjacent suffixes.

**Predict before you run:** the longest *repeated* substring of `"banana"` (a substring that appears at least twice). Is it `"an"` (length 2), `"ana"` (length 3), or `"nana"` (length 4)?

```python run viz=array
def suffix_array(s):
    return sorted(range(len(s)), key=lambda i: s[i:])

def lcp(a, b):
    k = 0
    while k < len(a) and k < len(b) and a[k] == b[k]:
        k += 1
    return k

def longest_repeated_substring(s):
    sa = suffix_array(s)
    best_len, best_start = 0, 0
    for i in range(1, len(sa)):
        k = lcp(s[sa[i-1]:], s[sa[i]:])               # overlap of adjacent sorted suffixes
        if k > best_len:
            best_len, best_start = k, sa[i]
    return s[best_start:best_start + best_len]

sa = suffix_array("banana")
print("sorted suffixes:", [("banana")[i:] for i in sa])
print("LCP of adjacent:", [lcp(("banana")[sa[i-1]:], ("banana")[sa[i]:]) for i in range(1, len(sa))])
print("longest repeated:", repr(longest_repeated_substring("banana")))
print("check 'abcabcabc' :", repr(longest_repeated_substring("abcabcabc")))
```

<details>
<summary><strong>Reveal</strong></summary>

The longest repeated substring of `"banana"` is `"ana"` (length 3), not `"nana"`. `"nana"` occurs only *once* (it's a single suffix), so it isn't repeated; `"ana"` occurs twice (starting at indices 1 and 3). The suffix array makes this falling-out trivial: the LCP array is `[1, 3, 0, 0, 2]`, and the **maximum LCP value, 3, *is* the length of the longest repeated substring** — it sits between the adjacent sorted suffixes `"ana"` and `"anana"`, which share exactly `"ana"`. Why adjacent pairs suffice: any two suffixes sharing a long prefix must be *neighbours* once sorted (everything between them shares at least that prefix too), so the deepest repeat always shows up as an adjacent-pair overlap. One linear scan of the LCP array finds it — and `"abcabcabc"` confirms the pattern, with max LCP 6 giving `"abcabc"`. That's the suffix array's signature move: sort once, and "repeated/shared substring" questions collapse to a scan of adjacent overlaps.

</details>

## Your Turn

**Count distinct substrings** — a classic suffix-array-plus-LCP result. A length-`n` string has `n(n+1)/2` substrings counting duplicates; every adjacent LCP value counts substrings that are *repeats* of an earlier suffix's prefixes, so the distinct count is `n(n+1)/2 − Σ lcp`.

```python run viz=array
def suffix_array(s):
    return sorted(range(len(s)), key=lambda i: s[i:])

def lcp(a, b):
    k = 0
    while k < len(a) and k < len(b) and a[k] == b[k]:
        k += 1
    return k

def distinct_substrings(s):
    # Your code goes here
    return 0

s = input()
print(distinct_substrings(s))
```

```java run viz=array
import java.util.*;
public class Main {
    static Integer[] suffixArray(String s) {
        Integer[] sa = new Integer[s.length()];
        for (int i = 0; i < s.length(); i++) sa[i] = i;
        Arrays.sort(sa, (x, y) -> s.substring(x).compareTo(s.substring(y)));
        return sa;
    }
    static int lcp(String a, String b) {
        int k = 0;
        while (k < a.length() && k < b.length() && a.charAt(k) == b.charAt(k)) k++;
        return k;
    }
    static int distinctSubstrings(String s) {
        // Your code goes here
        return 0;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        System.out.println(distinctSubstrings(s));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "string", "type": "string", "placeholder": "banana" }
  ],
  "cases": [
    { "args": { "s": "banana" }, "expected": "15" },
    { "args": { "s": "aaa" }, "expected": "3" },
    { "args": { "s": "abcd" }, "expected": "10" },
    { "args": { "s": "aaab" }, "expected": "7" }
  ]
}
```

Both print `15` then `3`. `"banana"` has `21` substrings with duplicates but only `15` distinct (the LCP array sums to 6, the six duplicated prefixes); `"aaa"` has just `"a"`, `"aa"`, `"aaa"` — three distinct, from `6 − 3`. The same LCP array that found the longest repeat now *counts* the repeats — one preprocess, many answers, which is the whole point of indexing the text.

<details>
<summary><strong>Editorial</strong></summary>

A length-`n` string has `n(n+1)/2` total substrings counting duplicates; adjacent sorted suffixes in the suffix array share a common prefix of length `lcp[i]` — those are the duplicates. Subtract the sum of all LCP values to get the distinct count.

```python solution time=O(n^2 log n) space=O(n)
def suffix_array(s):
    return sorted(range(len(s)), key=lambda i: s[i:])

def lcp(a, b):
    k = 0
    while k < len(a) and k < len(b) and a[k] == b[k]:
        k += 1
    return k

def distinct_substrings(s):
    n = len(s)
    sa = suffix_array(s)
    total = n * (n + 1) // 2                           # all substrings, duplicates included
    repeated = sum(lcp(s[sa[i-1]:], s[sa[i]:]) for i in range(1, n))
    return total - repeated                            # subtract the duplicated prefixes

s = input()
print(distinct_substrings(s))
```

```java solution
import java.util.*;
public class Main {
    static Integer[] suffixArray(String s) {
        Integer[] sa = new Integer[s.length()];
        for (int i = 0; i < s.length(); i++) sa[i] = i;
        Arrays.sort(sa, (x, y) -> s.substring(x).compareTo(s.substring(y)));
        return sa;
    }
    static int lcp(String a, String b) {
        int k = 0;
        while (k < a.length() && k < b.length() && a.charAt(k) == b.charAt(k)) k++;
        return k;
    }
    static int distinctSubstrings(String s) {
        int n = s.length();
        Integer[] sa = suffixArray(s);
        int total = n * (n + 1) / 2, repeated = 0;
        for (int i = 1; i < n; i++) repeated += lcp(s.substring(sa[i-1]), s.substring(sa[i]));
        return total - repeated;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        System.out.println(distinctSubstrings(s));
    }
}
```

</details>

## Reflect & Connect

- **Index the text, not the pattern.** Suffix arrays preprocess the string once, then answer many substring questions cheaply — the opposite stance from KMP/Z (which preprocess the pattern). Use it when the text is fixed and queries are plentiful.
- **Sorting makes search binary search.** Occurrences of `P` are prefixes of suffixes; sorted, they're contiguous, so `O(m log n)` bisection finds them. No tree, no pointers.
- **The LCP array is the multiplier.** Adjacent-suffix overlaps encode all shared-substring information: longest repeated substring (max LCP), distinct-substring count (`n(n+1)/2 − Σ lcp`), and longest common substring of two texts (concatenate with a separator, scan LCP across the boundary).
- **Compact vs the suffix tree.** A suffix tree answers many queries in `O(m)` but costs heavy node/pointer memory; a suffix array is `n` integers + LCP, far lighter and cache-friendly. Production string indexes (BWT, FM-index, bioinformatics) favour the array.
- **Construction has tiers.** Naive sort `O(n² log n)` (this lesson), prefix-doubling `O(n log n)`, and SA-IS / DC3 `O(n)`. Learn the idea on the naive build; reach for a library `O(n)` builder at scale. The [suffix automaton](/cortex/data-structures-and-algorithms/strings/suffix-automaton) (next) is the *online* alternative that ingests the string incrementally.

## Recall

<details>
<summary><strong>Q:</strong> What is a suffix array?</summary>

**A:** The start indices of all suffixes of a string, sorted lexicographically by the suffix they begin. For `"banana"` it's `[5, 3, 1, 0, 4, 2]`. Stored as `n` integers.

</details>
<details>
<summary><strong>Q:</strong> Why does substring search become binary search?</summary>

**A:** Every occurrence of `P` starts a suffix with `P` as a prefix. After sorting, all suffixes sharing prefix `P` form a contiguous block, so you binary-search for `P`'s lower bound and check the prefix — `O(m log n)`.

</details>
<details>
<summary><strong>Q:</strong> What does the LCP array store, and why are adjacent pairs enough?</summary>

**A:** `lcp[i]` = the longest common prefix of the `i`-th and `(i-1)`-th sorted suffixes. Two suffixes sharing a long prefix are neighbours once sorted, so adjacent-pair overlaps capture all shared-prefix information (e.g. the max LCP is the longest repeated substring).

</details>
<details>
<summary><strong>Q:</strong> How do you count distinct substrings with a suffix array?</summary>

**A:** `n(n+1)/2 − Σ lcp`: total substrings (with duplicates) minus the duplicated prefixes that the LCP values count. For `"banana"`: `21 − 6 = 15`.

</details>
<details>
<summary><strong>Q:</strong> Suffix array vs suffix tree — when each?</summary>

**A:** A suffix tree gives `O(m)` queries but heavy pointer/node memory; a suffix array gives `O(m log n)` queries in `n` integers + LCP — far less memory and cache-friendlier. Prefer the array unless you specifically need the tree's per-query speed.

</details>

## Sources & Verify

- **Manber & Myers** (1990), "Suffix arrays: a new method for on-line string searches", *SIAM J. Comput.* — the original suffix array and binary-search matching.
- **Kasai et al.** (2001) — linear-time LCP array construction from the suffix array; **Kärkkäinen-Sanders** (DC3) and **Nong et al.** (SA-IS) for `O(n)` suffix-array construction.
- **LeetCode** 1044 (Longest Duplicate Substring) and 1062 (Longest Repeating Substring) are the canonical drills; the `[5,3,1,0,4,2]` suffix array, the `[1,3,0,0,2]` LCP / `"ana"` longest repeat, and the `15`/`3` distinct counts above come from the runnable blocks — re-run to verify.
