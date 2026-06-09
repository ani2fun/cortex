---
title: "Longest Increasing Subsequence"
summary: "The longest strictly-increasing subsequence (gaps allowed). The recurrence lis(i) = 1 + max(lis(j) for j<i, arr[j]<arr[i]) gives an O(n^2) DP that looks back at every earlier index — so it can't be space-optimised to a rolling window."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/01-linear-dp
---

## Why It Exists

A metric crosses your screen and you ask: *what's the longest run where it kept improving?* — not *consecutive*, because you're allowed to skip the dips. The graph might be 10,000 points; the answer might be 47. That's the **Longest Increasing Subsequence** (LIS): the longest subsequence (keep relative order, but pick any subset) that's strictly increasing. It powers version-control diff tools, trend detection, and patience-sorting card games.

LIS is the [linear-DP](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/linear-dp) shape with a twist that matters: `lis(i)` (the longest increasing subsequence *ending at* `i`) looks back at **every** earlier index, not a fixed handful. That's the transfer challenge the linear-DP lesson left you with — and the reason LIS *can't* be space-optimised to a rolling window.

## See It Work

`lis(i) = 1 + max(lis(j))` over all `j < i` with `arr[j] < arr[i]` (or just `1` if no such `j`). The answer is the max over all `i`.

```python run viz=array
def lis(arr):
    if not arr: return 0
    dp = [1] * len(arr)                              # dp[i] = LIS ending at i (at least itself)
    for i in range(len(arr)):
        for j in range(i):                           # look back at EVERY earlier index
            if arr[j] < arr[i]:
                dp[i] = max(dp[i], dp[j] + 1)
    return max(dp)

print(lis([10, 9, 2, 5, 3, 7, 101, 18]))             # 4  e.g. [2, 3, 7, 101]
```

```java run viz=array
import java.util.*;
public class Main {
    static int lis(int[] a) {
        if (a.length == 0) return 0;
        int[] dp = new int[a.length]; Arrays.fill(dp, 1); int best = 1;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < i; j++) if (a[j] < a[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
            best = Math.max(best, dp[i]);
        }
        return best;
    }
    public static void main(String[] args) {
        System.out.println(lis(new int[]{10, 9, 2, 5, 3, 7, 101, 18}));   // 4
    }
}
```

Both print `4` — e.g. `[2, 3, 7, 101]`, picked out of the array by skipping the dips. Cost: `O(n²)` (the nested loop), `O(n)` space.

## How It Works

The DP table for `[10, 9, 2, 5, 3, 7, 101, 18]` fills like this:

```d2
table: "LIS dp[i] = longest increasing subsequence ending at i" {
  grid-rows: 3
  grid-columns: 9
  grid-gap: 0
  h:  "index"  ; h0: "0"  ; h1: "1" ; h2: "2" ; h3: "3" ; h4: "4" ; h5: "5" ; h6: "6"   ; h7: "7"
  a:  "arr"    ; a0: "10" ; a1: "9" ; a2: "2" ; a3: "5" ; a4: "3" ; a5: "7" ; a6: "101" ; a7: "18"
  d:  "dp"     ; d0: "1"  ; d1: "1" ; d2: "1" ; d3: "2" ; d4: "2" ; d5: "3" ; d6: "4"   ; d7: "4" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
}
```

<p align="center"><strong>Each <code>dp[i]</code> scans all earlier <code>j</code> with a smaller value and takes the best + 1. The answer is the table's maximum (4), not <code>dp[n-1]</code>.</strong></p>

LIS has both DP properties: **optimal substructure** (the LIS ending at `i` extends an LIS ending at some earlier `j`) and **overlapping subproblems** (`dp[j]` is reused by every later `i` that can follow it). Two things to note:

- **It can't be space-optimised.** Unlike Fibonacci's fixed 2-cell window, `dp[i]` may reference `dp[0]`, the *very first* cell — so you need the whole array. (That's the linear-DP transfer-challenge answer.)
- **`O(n log n)` is possible** with patience sorting: maintain the smallest possible tail for each length and binary-search the insertion point. Same answer, faster — but the `O(n²)` DP is the one to understand first.

> **Key takeaway.** LIS = `1 + max(lis(j) for j<i with arr[j]<arr[i])`, an `O(n²)` linear-DP that looks back at *all* earlier indices (so no rolling-window space-opt). It's a *subsequence* — gaps allowed — which is exactly why a single-pass "longest run" undercounts it. A patience-sorting variant reaches `O(n log n)`.

## Trace It

The word *subsequence* is the whole subtlety. It's tempting to scan once and track the current ascending run — but a subsequence may *skip* elements that a contiguous run cannot.

**Predict before you run:** for `[10, 9, 2, 5, 3, 7, 101, 18]`, the longest *contiguous* ascending run is `[3, 7, 101]`. Does the single-pass run-length match the true LIS, or fall short?

```python run viz=array
def longest_run(arr):                                # longest CONTIGUOUS ascending run
    best = cur = 1
    for i in range(1, len(arr)):
        cur = cur + 1 if arr[i] > arr[i - 1] else 1
        best = max(best, cur)
    return best

def lis(arr):                                        # longest increasing SUBSEQUENCE
    dp = [1] * len(arr)
    for i in range(len(arr)):
        for j in range(i):
            if arr[j] < arr[i]: dp[i] = max(dp[i], dp[j] + 1)
    return max(dp)

A = [10, 9, 2, 5, 3, 7, 101, 18]
print("contiguous run:", longest_run(A))
print("LIS (subsequence):", lis(A))
```

<details>
<summary><strong>Reveal</strong></summary>

The contiguous run is `3` (`[3, 7, 101]`); the true LIS is `4` (`[2, 3, 7, 101]` or `[2, 5, 7, 18]`). The single pass is forced to *reset* at every dip — when `101` drops to `18`, its counter restarts. LIS doesn't reset: it *skips* the `9`, the `5`, the `101`, keeping only the elements that extend an increasing chain. That's the difference between a **substring** (contiguous, what the single pass finds) and a **subsequence** (order-preserving but gappy, what LIS finds) — and it's why LIS needs the look-back-at-all-`j` DP rather than an `O(n)` scan. Confusing the two is the single most common LIS mistake.

</details>

## Your Turn

**Maximum Sum Increasing Subsequence** — same DP, one operator changed. Instead of *counting* elements, *sum* their values: `dp[i] = arr[i] + max(dp[j] for j<i, arr[j]<arr[i])`. The longest isn't always the heaviest.

```python run viz=array
def max_sum_increasing(arr):
    dp = arr[:]                                      # dp[i] = best sum of an increasing subseq ending at i
    for i in range(len(arr)):
        for j in range(i):
            if arr[j] < arr[i]:
                dp[i] = max(dp[i], dp[j] + arr[i])
    return max(dp)

print(max_sum_increasing([1, 101, 2, 3, 100, 4, 5]))   # 106  ([1,2,3,100])
print(max_sum_increasing([3, 4, 5, 10]))               # 22   (whole array)
```

```java run viz=array
import java.util.*;
public class Main {
    static int maxSumIncreasing(int[] a) {
        int[] dp = a.clone(); int best = 0;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < i; j++) if (a[j] < a[i]) dp[i] = Math.max(dp[i], dp[j] + a[i]);
            best = Math.max(best, dp[i]);
        }
        return best;
    }
    public static void main(String[] args) {
        System.out.println(maxSumIncreasing(new int[]{1,101,2,3,100,4,5}));   // 106
        System.out.println(maxSumIncreasing(new int[]{3,4,5,10}));            // 22
    }
}
```

Both print `106` then `22`. Note `[1,2,3,100]` (sum 106, length 4) beats `[1,2,3,4,5]` (sum 15, length 5) — maximising sum and maximising length are different objectives on the same DP skeleton. The remaining DP lessons keep this move: same scaffold, a different subproblem definition and recurrence.

## Reflect & Connect

- **This is the linear-DP transfer challenge, answered.** Fibonacci's window was two cells, so it space-optimised to `O(1)`. LIS's `dp[i]` can reach back to `dp[0]`, so the whole `O(n)` table is required — *the* reason some linear DPs can't be reduced to scalars.
- **`O(n²)` DP vs `O(n log n)` patience sorting.** The DP is the transparent baseline; patience sorting (keep the smallest tail per length, binary-search insertions) is the production-speed version with the same answer. Learn the DP first; reach for patience sorting when `n` is large.
- **Subsequence ≠ substring.** A subsequence keeps relative order but may skip elements; a substring is contiguous. LIS, LCS, and edit distance are all *subsequence* DPs — the gappy structure is exactly what makes them DP rather than a single scan.
- **One operator changes the problem.** Swap `+1` for `+arr[i]` and "longest" becomes "max-sum"; swap `<` for `<=` and "strictly increasing" becomes "non-decreasing". The recurrence skeleton is reusable across a whole family.

## Recall

<details>
<summary><strong>Q:</strong> What is the LIS recurrence?</summary>

**A:** `lis(i) = 1 + max(lis(j))` over all `j < i` with `arr[j] < arr[i]` (or `1` if none); the answer is `max(lis(i))` over all `i`. `O(n²)` time, `O(n)` space.

</details>
<details>
<summary><strong>Q:</strong> Why can't LIS be space-optimised to a rolling window?</summary>

**A:** `dp[i]` may reference *any* earlier `dp[j]`, including `dp[0]` — the look-back isn't a fixed `k` cells, so the entire table must be kept (unlike Fibonacci's 2-cell window).

</details>
<details>
<summary><strong>Q:</strong> Subsequence vs substring — why does it matter here?</summary>

**A:** LIS is a subsequence (order-preserving but may skip elements), so a single-pass "longest contiguous ascending run" undercounts it. The gaps are what require the `O(n²)` look-back DP.

</details>
<details>
<summary><strong>Q:</strong> How do you get `O(n log n)` for LIS?</summary>

**A:** Patience sorting: maintain an array of the smallest possible tail value for each achievable length, and binary-search the insertion point for each element. Same answer, `O(n log n)` time.

</details>
<details>
<summary><strong>Q:</strong> How does "maximum-sum increasing subsequence" differ from LIS?</summary>

**A:** One operator: `dp[i] = arr[i] + max(dp[j] …)` instead of `1 + max(dp[j] …)`. It maximises the *sum* of an increasing subsequence, not its length — and the answers can differ.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., Ch. 15 — DP on sequences; LIS appears as an exercise built on optimal substructure + overlapping subproblems.
- **Schensted** (1961) / patience sorting — the `O(n log n)` LIS; see also Aldous & Diaconis, "Longest increasing subsequences: from patience sorting to the Baik–Deift–Johansson theorem" (1999).
- **LeetCode** 300 (Longest Increasing Subsequence) and 673 (Number of LIS) are the canonical drills; the `4`, the contiguous-vs-LIS `3`/`4`, and the max-sum `106`/`22` above come from the runnable blocks — re-run to verify.
