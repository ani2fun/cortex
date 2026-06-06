---
title: Z-Algorithm
summary: "KMP's sibling at the same O(n + m) cost via a different array. z[i] is the length of the longest substring starting at i that matches a prefix of the string; the Z-box reuse keeps it linear. Matching concatenates pattern + separator + text and looks for z[i] == pattern length."
prereqs:
  - strings-kmp
---

## Why It Exists

[KMP](/cortex/data-structures-and-algorithms/strings-kmp) reaches `O(n + m)` with the failure function. The **Z-algorithm** reaches the same bound with a different precomputed array — and it's arguably easier to remember and reason about. The **Z-array** answers, for every position `i`: *how long is the substring starting at `i` that matches a prefix of the whole string?* That single definition powers pattern matching, periodicity tests, and counting distinct substrings.

Where KMP's `lps` looks *backward* (longest prefix that's also a suffix *ending* at `i`), the Z-array looks *forward* (longest prefix-match *starting* at `i`). They're two views of the same self-similarity, and the Z-array's framing makes one of the cleanest linear-time string algorithms — once you see the **Z-box** trick that stops it being quadratic.

## See It Work

`z_array(s)` computes the prefix-match lengths; matching a pattern in a text concatenates `P + separator + T` (a separator that appears in neither) and looks for positions where `z[i]` equals the pattern's length.

```python run viz=array
def z_array(s):
    n = len(s)
    z = [0] * n
    l = r = 0                                        # [l, r] = rightmost prefix-match window (the Z-box)
    for i in range(1, n):
        if i < r:
            z[i] = min(r - i, z[i - l])              # reuse the mirror value, capped at the box edge
        while i + z[i] < n and s[z[i]] == s[i + z[i]]:
            z[i] += 1                                # extend past the box by direct comparison
        if i + z[i] > r:
            l, r = i, i + z[i]                        # this match reaches furthest -> new box
    return z

SEP = chr(0)                                         # sentinel: a char in NEITHER pattern nor text
def z_search(text, pattern):
    s = pattern + SEP + text
    z = z_array(s)
    m = len(pattern)
    return [i - m - 1 for i in range(len(s)) if z[i] == m]   # z[i] == |P| means a full match

print(z_array("aabaab"))                             # [0, 1, 0, 3, 1, 0]
print(z_search("abxabcabcaby", "abcaby"))            # [6]
```

```java run viz=array
import java.util.*;
public class Main {
    static final char SEP = '\u0000';                // sentinel char absent from inputs
    static int[] zArray(String s) {
        int n = s.length(); int[] z = new int[n]; int l = 0, r = 0;
        for (int i = 1; i < n; i++) {
            if (i < r) z[i] = Math.min(r - i, z[i - l]);
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
            if (i + z[i] > r) { l = i; r = i + z[i]; }
        }
        return z;
    }
    static List<Integer> zSearch(String text, String pattern) {
        String s = pattern + SEP + text;
        int[] z = zArray(s); int m = pattern.length();
        List<Integer> hits = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) if (z[i] == m) hits.add(i - m - 1);
        return hits;
    }
    public static void main(String[] args) {
        System.out.println(Arrays.toString(zArray("aabaab")));   // [0, 1, 0, 3, 1, 0]
        System.out.println(zSearch("abxabcabcaby", "abcaby"));   // [6]
    }
}
```

Both print the Z-array `[0, 1, 0, 3, 1, 0]` then `[6]`. The `3` at index 3 of `"aabaab"` says `"aab"` (starting at index 3) matches the prefix `"aab"`; the pattern matches the text only at index 6. Build and search are each linear, `O(n + m)` overall.

## How It Works

The naive Z-array recomputes each `z[i]` by scanning from scratch — `O(n²)`. The **Z-box** makes it linear by remembering the prefix-match that reaches furthest right and *reusing* it:

```d2
direction: right
box: "Z-box [l, r] = the prefix-match seen so far\nthat extends furthest right (s[l..r] is a prefix)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
inside: "i inside the box (i < r):\nz[i] = min(r - i, z[i - l])\nthe mirror position z[i-l] already told us the answer\n(capped at the box edge r)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
extend: "then extend past r by direct comparison only\n-> each char is compared O(1) amortized times" {style.fill: "#fde68a"; style.stroke: "#d97706"}
outside: "i outside the box: scan fresh from i\n(and open a new box if it reaches past r)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
box -> inside
inside -> extend
box -> outside
```

<p align="center"><strong>Inside the Z-box, position <code>i</code> mirrors an earlier position <code>i − l</code> whose Z-value is already known — copy it (capped at the box edge) and only extend past the edge by real comparison. Every character is compared O(1) amortized times, giving O(n).</strong></p>

Three things to hold onto:

- **`z[i]` looks forward; `lps[i]` looks backward.** The Z-array's `z[i]` = longest prefix-match *starting* at `i`. KMP's `lps[i]` = longest proper prefix that's also a suffix *ending* at `i`. Same self-similarity, opposite direction — and you can convert between them. (`z[0]` is left 0 by convention; the whole string trivially matches itself.)
- **The Z-box is why it's `O(n)`.** When `i` falls inside `[l, r]`, the substring `s[i..r]` already equals the prefix substring `s[i-l..r-l]`, so `z[i-l]` *is* the answer — unless it would run past the box edge, where you cap at `r - i` and extend by direct comparison. Those direct comparisons only ever advance `r`, so they total `O(n)` across the whole run.
- **Matching needs a separator.** Build `z` over `P + SEP + T` where `SEP` occurs in neither. Then `z[i] == |P|` pinpoints a full occurrence of `P` starting at text position `i - |P| - 1`. The separator is load-bearing — the [Trace It](#trace-it) shows what breaks without it.

> **Key takeaway.** The Z-array `z[i]` = length of the longest substring starting at `i` that matches a prefix of `s`, computed in `O(n)` via the **Z-box** (reuse the mirror value `z[i-l]` inside `[l, r]`, extend only past the edge). Pattern matching: run `z` over `P + SEP + T` and report every `z[i] == |P|`. It's KMP's forward-looking twin.

## Trace It

Z-matching glues the pattern and text together and reads off the Z-array — so the glue matters. The separator must be a character that appears in *neither* string. Skip it, and matches near the join can vanish.

**Predict before you run:** to find `"ab"` in `"abab"`, you build the Z-array of `"ab" + "abab"` and look for `z[i] == 2`. With a separator (`"ab" + SEP + "abab"`) you correctly get matches at text positions `[0, 2]`. Without the separator — just `"ababab"` — which matches does `z[i] == 2` report?

```python run viz=array
def z_array(s):
    n = len(s); z = [0] * n; l = r = 0
    for i in range(1, n):
        if i < r:
            z[i] = min(r - i, z[i - l])
        while i + z[i] < n and s[z[i]] == s[i + z[i]]:
            z[i] += 1
        if i + z[i] > r:
            l, r = i, i + z[i]
    return z

def z_search(text, pattern, sep=chr(0)):             # correct: with a separator
    s = pattern + sep + text
    z = z_array(s); m = len(pattern)
    return [i - m - 1 for i in range(len(s)) if z[i] == m]

def z_search_no_sep(text, pattern):                  # bug: pattern + text, no separator
    s = pattern + text
    z = z_array(s); m = len(pattern)
    return [i - m for i in range(m, len(s)) if z[i] == m]

print("z('ababab')        :", z_array("ababab"))
print("with separator     :", z_search("abab", "ab"))
print("no separator (bug) :", z_search_no_sep("abab", "ab"))
```

<details>
<summary><strong>Reveal</strong></summary>

With the separator the answer is `[0, 2]`; without it you get only `[2]` — the match at position 0 vanishes. Look at `z("ababab") = [0, 0, 4, 0, 2, 0]`. At index 2 (the start of the text region), the prefix `"ab"` doesn't just match `"ab"` — it keeps matching `"abab"`, so `z[2] = 4`, *not* 2. The exact test `z[i] == |P|` fails (`4 ≠ 2`), so the occurrence at text position 0 is missed. The separator fixes this by force: since `SEP` appears in neither string, no prefix can match past the join, so every Z-value in the text region is *capped* at `|P|`, and `z[i] == |P|` fires exactly at true matches. (You could instead test `z[i] >= |P|`, but that breaks other cases and conflates pattern-internal overlaps with real matches — the separator is the clean, standard fix.) The lesson: the Z-matching trick is only correct *because* of that sentinel character.

</details>

## Your Turn

**Count occurrences** of a pattern in a text — the Z-matcher with a `len()` on the hit list. It's how you'd answer "how many times does this motif appear?" in `O(n + m)`.

```python run viz=array
def z_array(s):
    n = len(s); z = [0] * n; l = r = 0
    for i in range(1, n):
        if i < r:
            z[i] = min(r - i, z[i - l])
        while i + z[i] < n and s[z[i]] == s[i + z[i]]:
            z[i] += 1
        if i + z[i] > r:
            l, r = i, i + z[i]
    return z

def z_count(text, pattern, sep=chr(0)):
    s = pattern + sep + text
    z = z_array(s); m = len(pattern)
    return sum(1 for v in z if v == m)

print(z_count("mississippi", "issi"))    # 2
print(z_count("aaaa", "aa"))             # 3
```

```java run viz=array
public class Main {
    static int[] zArray(String s) {
        int n = s.length(); int[] z = new int[n]; int l = 0, r = 0;
        for (int i = 1; i < n; i++) {
            if (i < r) z[i] = Math.min(r - i, z[i - l]);
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
            if (i + z[i] > r) { l = i; r = i + z[i]; }
        }
        return z;
    }
    static int zCount(String text, String pattern) {
        String s = pattern + '\u0000' + text;
        int[] z = zArray(s); int m = pattern.length(), c = 0;
        for (int v : z) if (v == m) c++;
        return c;
    }
    public static void main(String[] args) {
        System.out.println(zCount("mississippi", "issi"));   // 2
        System.out.println(zCount("aaaa", "aa"));            // 3
    }
}
```

Both print `2` then `3`. `"issi"` occurs twice in `"mississippi"` (indices 1 and 4); `"aa"` occurs three times in `"aaaa"` (indices 0, 1, 2 — overlaps included, which Z-matching counts naturally). Same `z[i] == |P|` test, just totalled instead of collected — a reminder that find-all, find-first, and count are one algorithm with a different tally.

## Reflect & Connect

- **Forward twin of KMP.** `z[i]` = longest prefix-match *starting* at `i`; `lps[i]` = longest prefix that's also a suffix *ending* at `i`. Same `O(n + m)`, same self-similarity, opposite direction — pick whichever you find clearer (many prefer the Z-array's definition).
- **The Z-box is the linearity.** Reusing `z[i-l]` inside `[l, r]` and only extending past the edge bounds total comparisons to `O(n)`. Drop the box and you're back to `O(n²)` on repetitive input.
- **The separator is mandatory.** Matching via `P + SEP + T` only works because `SEP` (absent from both) caps Z-values at `|P|`, making `z[i] == |P|` an exact match test. It's the most common Z-matching bug.
- **Standalone power.** Beyond matching, the Z-array gives string periodicity, the number of distinct substrings (with suffix structures), and competitive-programming staples — often in fewer lines than KMP.
- **Where it sits.** [Naive](/cortex/data-structures-and-algorithms/strings-string-matching-naive) → [KMP](/cortex/data-structures-and-algorithms/strings-kmp) (backward failure function) → Z (forward prefix-match) → [Rabin-Karp](/cortex/data-structures-and-algorithms/strings-rabin-karp-and-rolling-hash) (hashing). Four routes to fast matching, each with a different mental model.

## Recall

<details>
<summary><strong>Q:</strong> What does <code>z[i]</code> mean?</summary>

**A:** The length of the longest substring starting at index `i` that matches a prefix of the whole string. (`z[0]` is conventionally 0.) Computed in `O(n)`.

</details>
<details>
<summary><strong>Q:</strong> What is the Z-box, and why does it make the algorithm linear?</summary>

**A:** `[l, r]` is the prefix-match interval seen so far that extends furthest right. For `i` inside it, `s[i..r]` already equals the prefix `s[i-l..r-l]`, so `z[i] = min(r-i, z[i-l])` — no rescan. You only do direct comparisons past `r`, and those just advance `r`, totalling `O(n)`.

</details>
<details>
<summary><strong>Q:</strong> How do you match a pattern in a text with the Z-array?</summary>

**A:** Build `z` over `P + SEP + T` (a separator absent from both). Every index `i` with `z[i] == |P|` marks an occurrence of `P` at text position `i - |P| - 1`.

</details>
<details>
<summary><strong>Q:</strong> Why is the separator necessary?</summary>

**A:** Without it, a prefix can match *past* the pattern into the text, pushing Z-values above `|P|` and breaking the exact `z[i] == |P|` test (e.g. matching `"ab"` in `"abab"` misses position 0). The separator caps every text-region Z-value at `|P|`.

</details>
<details>
<summary><strong>Q:</strong> How does the Z-array relate to KMP's failure function?</summary>

**A:** They encode the same self-similarity in opposite directions: Z looks forward (prefix-match starting at `i`), `lps` looks backward (prefix=suffix ending at `i`). Both give `O(n + m)` matching; one is convertible to the other.

</details>

## Sources & Verify

- **Gusfield**, *Algorithms on Strings, Trees, and Sequences* (1997), §1.3–1.5 — the Z-algorithm and the fundamental preprocessing it enables.
- **CP-Algorithms**, "Z-function" — the canonical Z-box implementation and its `O(n)` argument.
- **LeetCode** 28 (Find the Index of the First Occurrence) is the matching drill; the `[0,1,0,3,1,0]` Z-array, the `[6]` match, the with-vs-without-separator `[0,2]`/`[2]`, and the `2`/`3` occurrence counts above come from the runnable blocks — re-run to verify.
