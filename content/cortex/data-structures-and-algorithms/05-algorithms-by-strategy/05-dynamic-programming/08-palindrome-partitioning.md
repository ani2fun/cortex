---
title: "Palindrome Partitioning"
summary: "Minimum cuts to split a string so every piece is a palindrome. A 1D prefix DP — cuts[i] = min over every palindromic last piece s[j..i] of cuts[j-1]+1 — sitting on top of the 2D isPalindrome table from the previous lesson. Two tables, one O(n²) pass; greedy-longest-first fails."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/07-longest-palindromic-substring
---

## Why It Exists

The last two lessons hunted for *one* palindrome inside a string — the longest subsequence, then the longest contiguous substring. Now the demand flips: split the **whole** string so that *every* piece is a palindrome, with as few cuts as possible. `"abbbc"` looks hostile until you read it as `a | bbb | c` — three palindromic pieces, two cuts.

The naïve move is to try every partition, but a length-`n` string has `2^(n−1)` ways to drop dividers — brute force collapses immediately. Underneath, though, every cut decision depends only on the prefix before it: classic optimal substructure with overlapping subproblems. This is the lesson where the palindrome work pays off — the [boolean isPalindrome table](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-palindromic-substring) from the previous lesson becomes a *lookup* that a second, 1D DP builds on. ([LeetCode 132](https://leetcode.com/problems/palindrome-partitioning-ii/).)

## See It Work

Two tables, one pass. First fill `is_pal[i][j]` exactly as before. Then walk a 1D array: `cuts[i]` = the minimum cuts needed for the prefix `s[0..i]`. Fix the **last** piece — for every split point `j` where `s[j..i]` is a palindrome, the cost is `cuts[j-1] + 1`; take the cheapest. If the whole prefix is already a palindrome, it costs `0`.

```python run viz=array
def min_cut(s):
    n = len(s)
    if n < 2:
        return 0
    is_pal = [[False] * n for _ in range(n)]      # is_pal[i][j] = "is s[i..j] a palindrome?"
    for i in range(n):
        is_pal[i][i] = True
    for length in range(2, n + 1):
        for i in range(n - length + 1):
            j = i + length - 1
            if s[i] == s[j] and (length == 2 or is_pal[i + 1][j - 1]):
                is_pal[i][j] = True
    cuts = [0] * n                                # cuts[i] = min cuts for the prefix s[0..i]
    for i in range(n):
        if is_pal[0][i]:
            cuts[i] = 0                           # whole prefix is a palindrome -> no cut needed
        else:
            cuts[i] = min(cuts[j - 1] + 1 for j in range(1, i + 1) if is_pal[j][i])
    return cuts[n - 1]

print(min_cut("aab"))   # 1   ("aa" | "b")
print(min_cut("a"))     # 0
print(min_cut("ab"))    # 1
```

```java run viz=array
public class Main {
    static int minCut(String s) {
        int n = s.length();
        if (n < 2) return 0;
        boolean[][] isPal = new boolean[n][n];
        for (int i = 0; i < n; i++) isPal[i][i] = true;
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j) && (len == 2 || isPal[i + 1][j - 1])) isPal[i][j] = true;
            }
        int[] cuts = new int[n];
        for (int i = 0; i < n; i++) {
            if (isPal[0][i]) { cuts[i] = 0; continue; }
            int best = Integer.MAX_VALUE;
            for (int j = 1; j <= i; j++)
                if (isPal[j][i]) best = Math.min(best, cuts[j - 1] + 1);   // fix the last palindromic piece
            cuts[i] = best;
        }
        return cuts[n - 1];
    }
    public static void main(String[] args) {
        System.out.println(minCut("aab"));   // 1
        System.out.println(minCut("a"));     // 0
        System.out.println(minCut("ab"));    // 1
    }
}
```

Both print `1`, `0`, `1`. `"aab"` splits as `"aa" | "b"` (one cut); `"a"` is already a palindrome; `"ab"` needs one cut. Cost `O(n²)` time and space — the palindrome table dominates.

## How It Works

The trick is **fix the last piece**. Any palindrome partition of `s[0..i]` ends in some final palindromic chunk `s[j..i]`; whatever comes before it is itself an optimally-partitioned prefix `s[0..j-1]`. So minimise over every legal last piece:

```d2
direction: right
state: "cuts[i] = min cuts for prefix s[0..i]" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
whole: "is_pal[0][i]?\nwhole prefix is a palindrome -> cuts[i] = 0" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
last: "otherwise: for each j (1..i) with is_pal[j][i] True,\nlast piece = s[j..i], cost = cuts[j-1] + 1\n-> cuts[i] = min over all such j" {style.fill: "#fde68a"; style.stroke: "#d97706"}
tables: "TWO tables:\nis_pal[i][j]  (2D, the palindrome lookup)\ncuts[i]       (1D, the answer chain)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
state -> whole
state -> last
state -> tables: "built in one pass"
```

<p align="center"><strong>Fix the last piece: a min-cut partition of <code>s[0..i]</code> is an optimal partition of some prefix <code>s[0..j-1]</code> followed by one palindromic chunk <code>s[j..i]</code>. The 2D table answers "is this chunk a palindrome?" in O(1); the 1D array chains the costs.</strong></p>

The state being **1D** is the key insight and the most common confusion. We just spent two lessons on 2D interval tables — why is this 1D?

- **The answer chain is 1D.** A partition is a left-to-right sequence of cuts, so the subproblem is "best partition of a *prefix*" — one index, `cuts[i]`. The 2D `is_pal` table is *not* the DP; it's a precomputed oracle the 1D DP queries. Conflating the two (writing a 2D `cuts[i][j]`) is the classic over-modelling mistake here.
- **Precomputing the palindrome table is what keeps it `O(n²)`.** Without `is_pal`, checking "is `s[j..i]` a palindrome?" inside the cut loop costs `O(n)` each, and the two nested loops over `(i, j)` make the whole thing `O(n³)`. Building `is_pal` once up front turns every check into an `O(1)` lookup — the recurring DP move of *trading a precompute for a faster inner loop*.

> **Key takeaway.** Min-cut palindrome partitioning is a **1D prefix DP over a 2D palindrome oracle**: `cuts[i] = 0` if `s[0..i]` is a palindrome, else `min(cuts[j-1] + 1)` over every `j` with `s[j..i]` palindromic. Precompute `is_pal` first so each check is `O(1)` → `O(n²)` overall. Fix-the-last-piece is the decomposition; the answer is `cuts[n-1]`.

## Trace It

It's tempting to be greedy: bite off the **longest palindromic prefix** you can, cut, and repeat. It feels efficient and it's easy to code. But "fewest cuts" is a global objective, and a long first piece can strand the rest.

**Predict before you run:** for `"aaabaa"`, greedy grabs the longest palindromic prefix first. What does it get — and does it match the true minimum?

```python run viz=array
def is_p(t):
    return t == t[::-1]

def greedy_cuts(s):                               # take the LONGEST palindromic prefix each step
    n, cuts, start = len(s), 0, 0
    while start < n:
        end = n
        while end > start and not is_p(s[start:end]):
            end -= 1
        if start != 0:
            cuts += 1                             # one cut before each piece after the first
        start = end
    return cuts

def min_cut(s):                                   # the DP from above
    n = len(s)
    is_pal = [[False] * n for _ in range(n)]
    for i in range(n):
        is_pal[i][i] = True
    for length in range(2, n + 1):
        for i in range(n - length + 1):
            j = i + length - 1
            if s[i] == s[j] and (length == 2 or is_pal[i + 1][j - 1]):
                is_pal[i][j] = True
    cuts = [0] * n
    for i in range(n):
        cuts[i] = 0 if is_pal[0][i] else min(cuts[j - 1] + 1 for j in range(1, i + 1) if is_pal[j][i])
    return cuts[n - 1]

for s in ["aaabaa", "aab"]:
    print(f"{s}:  greedy={greedy_cuts(s)}  dp_min={min_cut(s)}")
```

<details>
<summary><strong>Reveal</strong></summary>

For `"aaabaa"`, greedy gets `2` but the true minimum is `1`. Greedy grabs `"aaa"` (the longest palindromic prefix), which strands `"baa"` — that splits as `"b" | "aa"`, for `"aaa" | "b" | "aa"` = two cuts. The DP instead considers *every* split point and finds that taking the **short** first piece `"a"` unlocks the long palindrome `"aabaa"`: `"a" | "aabaa"` = one cut. The greedy choice that looked best locally (a longer first piece) destroyed the better global structure. (`"aab"` is the foil — there greedy and DP agree at `1`, which is exactly why greedy is seductive: it's often right, just not always.) This is the textbook case for *why DP over greedy*: when an early locally-optimal choice can forbid a globally-optimal one, you must consider all decompositions — and the `cuts[j-1] + 1` minimisation does precisely that.

</details>

## Your Turn

Counting cuts is good, but the **partition itself** is what you usually want. Extend the DP with a parent pointer: record which `j` gave each `cuts[i]` its minimum, then walk the pointers back to recover the pieces.

```python run viz=array
def min_cut_partition(s):
    n = len(s)
    is_pal = [[False] * n for _ in range(n)]
    for i in range(n):
        is_pal[i][i] = True
    for length in range(2, n + 1):
        for i in range(n - length + 1):
            j = i + length - 1
            if s[i] == s[j] and (length == 2 or is_pal[i + 1][j - 1]):
                is_pal[i][j] = True
    cuts = [0] * n
    prev = [0] * n                                # prev[i] = start index of the last piece ending at i
    for i in range(n):
        if is_pal[0][i]:
            cuts[i], prev[i] = 0, 0
        else:
            best = None
            for j in range(1, i + 1):
                if is_pal[j][i] and (best is None or cuts[j - 1] + 1 < best):
                    best, prev[i] = cuts[j - 1] + 1, j
            cuts[i] = best
    pieces, i = [], n - 1                          # walk the parent pointers back to front
    while i >= 0:
        start = prev[i]
        pieces.append(s[start:i + 1])
        i = start - 1
    return pieces[::-1]

print(min_cut_partition("aab"))      # ['aa', 'b']
print(min_cut_partition("aaabaa"))   # ['a', 'aabaa']
```

```java run viz=array
import java.util.*;
public class Main {
    static List<String> minCutPartition(String s) {
        int n = s.length();
        boolean[][] isPal = new boolean[n][n];
        for (int i = 0; i < n; i++) isPal[i][i] = true;
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j) && (len == 2 || isPal[i + 1][j - 1])) isPal[i][j] = true;
            }
        int[] cuts = new int[n], prev = new int[n];
        for (int i = 0; i < n; i++) {
            if (isPal[0][i]) { cuts[i] = 0; prev[i] = 0; continue; }
            int best = Integer.MAX_VALUE;
            for (int j = 1; j <= i; j++)
                if (isPal[j][i] && cuts[j - 1] + 1 < best) { best = cuts[j - 1] + 1; prev[i] = j; }
            cuts[i] = best;
        }
        LinkedList<String> pieces = new LinkedList<>();
        for (int i = n - 1; i >= 0; ) {
            int start = prev[i];
            pieces.addFirst(s.substring(start, i + 1));
            i = start - 1;
        }
        return pieces;
    }
    public static void main(String[] args) {
        System.out.println(minCutPartition("aab"));      // [aa, b]
        System.out.println(minCutPartition("aaabaa"));   // [a, aabaa]
    }
}
```

Both print `[aa, b]` then `[a, aabaa]`. The reconstruction confirms the Trace-It insight: the optimum for `"aaabaa"` really is the short-`"a"`-then-long-`"aabaa"` split that greedy never tried. Recording one parent pointer per cell turns a DP that *counts* into one that *recovers the choices* — the same backtrace move you use for LCS and edit distance.

## Reflect & Connect

- **1D answer, 2D oracle.** The DP itself is 1D (`cuts[i]` over prefixes), even though it queries a 2D palindrome table. Recognising that the table is a precompute, not the recurrence, is the whole modelling lesson.
- **Precompute to drop a complexity class.** Building `is_pal` once turns the inner palindrome check from `O(n)` to `O(1)`, taking the algorithm from `O(n³)` to `O(n²)`. "Cache the expensive predicate" recurs across DP.
- **Greedy fails, DP wins.** Longest-palindromic-prefix-first is locally optimal but globally wrong (`"aaabaa"`): a short first piece can unlock a long one. When an early choice can foreclose the best ending, enumerate all decompositions.
- **Same skeleton as word break.** The [next lesson](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/word-break) is the *identical* 1D prefix DP — "can this prefix be split into valid pieces?" — with a dictionary lookup swapped in for the palindrome check. Fix-the-last-piece transfers wholesale.
- **Min-cut vs enumerate-all.** This finds the *cheapest* partition with DP; [LeetCode 131](https://leetcode.com/problems/palindrome-partitioning/) asks for *every* palindrome partition, which is backtracking (enumerate, prune on non-palindromic pieces) — a different tool for a different question.

## Recall

<details>
<summary><strong>Q:</strong> What is the min-cut recurrence?</summary>

**A:** `cuts[i]` = minimum cuts for prefix `s[0..i]`. If `s[0..i]` is a palindrome, `cuts[i] = 0`; otherwise `cuts[i] = min(cuts[j-1] + 1)` over every `j` in `1..i` with `s[j..i]` a palindrome. The answer is `cuts[n-1]`.

</details>
<details>
<summary><strong>Q:</strong> Why is the DP state 1D when the last two lessons were 2D?</summary>

**A:** A partition is a left-to-right sequence of cuts, so the subproblem is "best partition of a prefix" — one index. The 2D `is_pal` table isn't the DP; it's a precomputed oracle the 1D DP queries. Modelling `cuts` as 2D is over-engineering.

</details>
<details>
<summary><strong>Q:</strong> Why precompute the palindrome table instead of checking on the fly?</summary>

**A:** Checking "is `s[j..i]` a palindrome?" naïvely costs `O(n)`, and it sits inside the `O(n²)` cut loops → `O(n³)` total. Precomputing `is_pal` once makes each check `O(1)`, giving `O(n²)`.

</details>
<details>
<summary><strong>Q:</strong> Why does greedy "longest palindromic prefix first" fail on <code>"aaabaa"</code>?</summary>

**A:** Greedy takes `"aaa"`, stranding `"baa"` → `"aaa" | "b" | "aa"` = 2 cuts. The optimum is `"a" | "aabaa"` = 1 cut: a short first piece unlocks the long palindrome. A locally-optimal long piece can forbid the globally-optimal split.

</details>
<details>
<summary><strong>Q:</strong> How do you recover the actual pieces, not just the cut count?</summary>

**A:** Store a parent pointer `prev[i]` = the start index of the last piece that achieved `cuts[i]`. After filling the table, walk the pointers from `n-1` back to the front, slicing each piece — the standard DP backtrace.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15 — optimal substructure, the precompute-then-DP pattern, and DP reconstruction via parent pointers.
- **LeetCode** 132 (Palindrome Partitioning II, min cuts) and 131 (Palindrome Partitioning, enumerate all) are the canonical drills; the `1`/`0`/`1` cuts, the greedy-vs-DP `2`/`1` on `"aaabaa"`, and the `['aa','b']` / `['a','aabaa']` reconstructions above all come from the runnable blocks — re-run to verify.
