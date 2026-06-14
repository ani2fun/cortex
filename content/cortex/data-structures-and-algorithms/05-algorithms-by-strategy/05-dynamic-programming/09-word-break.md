---
title: "Word Break"
summary: "Can a run-on string be split into dictionary words? The exact same 1D prefix DP as palindrome partitioning — dp[i] over prefixes, fix the last piece — but the per-piece test is a hash-set lookup and the aggregator is OR (does ANY split work?). The lesson where memoisation earns its keep: 266k naive calls collapse to 58."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/08-palindrome-partitioning
---

## Why It Exists

You paste a run-on into a search box: `"leetcode"`. Is that one word, or `"leet"` + `"code"`, or nonsense? With no spaces, every position is a possible boundary and the characters alone won't tell you — you need a dictionary and a procedure. The same problem is everywhere spaces aren't: segmenting Chinese/Japanese text, parsing a domain like `"choosespain.com"`, tokenising hashtags.

Structurally, this is the [palindrome-partitioning](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/palindrome-partitioning) DP wearing a different hat. The state is still 1D over prefixes; we still **fix the last piece**. Two things change: the per-piece test is now *"is this chunk in the dictionary?"* (a hash-set lookup) instead of *"is this chunk a palindrome?"*, and the aggregator is **OR** — we only need to know whether *some* split works, not the cheapest one. ([LeetCode 139](https://leetcode.com/problems/word-break/).)

## See It Work

`dp[i]` = "can the first `i` characters be segmented?" The empty prefix is trivially yes (`dp[0] = True`). For each end `i`, look for a split point `j` where the prefix `s[0..j-1]` is already segmentable **and** the last chunk `s[j..i-1]` is a dictionary word.

```python run viz=array
import ast

def word_break(s, words):
    word_set = set(words)                         # hash set -> O(1) membership
    n = len(s)
    dp = [False] * (n + 1)
    dp[0] = True                                  # empty prefix is trivially segmentable
    for i in range(1, n + 1):
        for j in range(i):                        # last word would be s[j:i]
            if dp[j] and s[j:i] in word_set:
                dp[i] = True                      # ANY working split is enough -> OR, then stop
                break
    return dp[n]

s = input()
words = ast.literal_eval(input())
result = word_break(s, words)
print("true" if result else "false")
```

```java run viz=array
import java.util.*;

public class Main {
    static boolean wordBreak(String s, String[] words) {
        Set<String> dict = new HashSet<>(Arrays.asList(words));   // O(1) membership
        int n = s.length();
        boolean[] dp = new boolean[n + 1];
        dp[0] = true;                                             // empty prefix
        for (int i = 1; i <= n; i++)
            for (int j = 0; j < i; j++)
                if (dp[j] && dict.contains(s.substring(j, i))) { dp[i] = true; break; }
        return dp[n];
    }
    static String[] parseStringArray(String line) {
        line = line.trim();
        if (line.equals("[]")) return new String[0];
        line = line.substring(1, line.length() - 1);
        String[] parts = line.split(",\\s*");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = parts[i].trim().replaceAll("^\"|\"$", "");
        return result;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine().trim();
        String[] words = parseStringArray(sc.nextLine());
        System.out.println(wordBreak(s, words));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "leetcode" },
    { "id": "words", "label": "words", "type": "string[]", "placeholder": "[\"leet\", \"code\"]" }
  ],
  "cases": [
    { "args": { "s": "leetcode", "words": "[\"leet\", \"code\"]" }, "expected": "true" },
    { "args": { "s": "catsandog", "words": "[\"cats\", \"dog\", \"sand\", \"and\", \"cat\"]" }, "expected": "false" },
    { "args": { "s": "apple", "words": "[\"app\", \"le\", \"apple\"]" }, "expected": "true" },
    { "args": { "s": "ab", "words": "[\"a\", \"b\"]" }, "expected": "true" }
  ]
}
```

Both print `true` then `false`. `"leetcode"` splits as `"leet" | "code"`; `"catsandog"` *looks* segmentable (`cats`, `and`, ...) but the leftover `"og"` is in no dictionary word, so every split dead-ends. Cost `O(n²)` substrings, each an `O(L)` hash on length-`L` words.

## How It Works

Fix the last word. A segmentation of the first `i` characters is a segmentation of some shorter prefix `s[0..j-1]` followed by one dictionary word `s[j..i-1]`. Ask whether *any* `j` works:

```d2
direction: right
state: "dp[i] = can the first i chars be segmented?" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
base: "dp[0] = True\n(empty prefix is trivially segmentable)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
rec: "dp[i] = OR over j<i of\n( dp[j]  AND  s[j..i-1] in dictionary )\nfix the last word s[j..i-1]" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
family: "SAME 1D-prefix skeleton, swap the AGGREGATOR:\nOR  -> can we?        (word break, this lesson)\nMIN -> fewest cuts    (palindrome partitioning)\nSUM -> how many ways? (count segmentations)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
state -> base
state -> rec
rec -> family: "the question is the aggregator"
```

<p align="center"><strong>Identical prefix-DP skeleton to palindrome partitioning; only the per-piece predicate (dictionary lookup vs palindrome) and the aggregator (OR vs MIN) differ. The aggregator <em>is</em> the question being asked.</strong></p>

Two things worth internalising:

- **The state stays 1D, and the aggregator carries the meaning.** Just like min-cut, a segmentation is a left-to-right sequence of pieces, so the subproblem is "is this *prefix* segmentable?" — one index. Min-cut took a `MIN` over last pieces; word break takes an `OR`. Swap in `SUM` and you'd *count* the segmentations. One skeleton, three questions — that's the transferable idea.
- **The hash set is why the predicate is `O(1)`.** "Is `s[j..i-1]` a word?" must be a set membership test, not a scan of the dictionary. Loading the word list into a hash set up front makes each check `O(L)` (the cost of hashing the candidate) instead of `O(dictionary size)` — the same *cache-the-predicate* move that the precomputed palindrome table played in the last lesson.

> **Key takeaway.** Word break is the **1D prefix DP** `dp[i] = OR over j<i of (dp[j] and s[j..i-1] in dict)`, with `dp[0] = True`. It's palindrome partitioning's twin: same fix-the-last-piece skeleton, a hash-set predicate instead of a palindrome check, and an **OR** aggregator (existence) instead of **MIN** (cost). The answer is `dp[n]`; cost `O(n²·L)`.

## Trace It

Without the table, the natural recursion is "try every first word, recurse on the rest." It's correct — and catastrophically slow, because the same suffix gets re-solved through countless different prefixes. This is *the* lesson where memoisation stops being optional.

**Predict before you run:** on `"aaaaaaaaaaaaaaaaaaaab"` (twenty `a`s, then a `b`) with dictionary `{"a","aa","aaa"}`, the answer is obviously `False` (nothing ever consumes the `b`). But *how many recursive calls* does the naïve version make getting there — dozens, or hundreds of thousands?

```python run viz=array
def make_naive():
    calls = [0]
    def wb(s, word_set):
        calls[0] += 1
        if not s:
            return True
        return any(s[:k] in word_set and wb(s[k:], word_set) for k in range(1, len(s) + 1))
    return wb, calls

def make_memo():
    calls, memo = [0], {}
    def wb(s, word_set):
        calls[0] += 1
        if not s:
            return True
        if s in memo:
            return memo[s]
        memo[s] = any(s[:k] in word_set and wb(s[k:], word_set) for k in range(1, len(s) + 1))
        return memo[s]
    return wb, calls

s = "a" * 20 + "b"
D = {"a", "aa", "aaa"}
nwb, nc = make_naive(); print("result:", nwb(s, D))
print("naive calls:", f"{nc[0]:,}")
mwb, mc = make_memo();  mwb(s, D)
print("memo  calls:", mc[0])
```

<details>
<summary><strong>Reveal</strong></summary>

The naïve recursion makes **266,079** calls; the memoised version makes **58**. Both return `False`. The `b` at the end can never be consumed, so *every* way of chopping the twenty-`a` run into pieces of size 1, 2, or 3 is explored to its bitter end — and there are exponentially many such compositions (a Tribonacci count). The killer is that `wb("aaa…a")` for a given suffix length is recomputed via every prefix that leads to it. Memoising on the suffix means each distinct suffix is solved **once** — twenty-one of them plus the base case — collapsing 266k calls to 58. That ~4,500× gap *is* "overlapping subproblems": the bottom-up `dp[]` array above is exactly this memo, filled iteratively. Word break without a cache is exponential; with one it's `O(n²)`.

</details>

## Your Turn

Same skeleton, third aggregator. **Count** the distinct segmentations instead of asking whether one exists: flip `dp` from booleans to integers, seed `dp[0] = 1`, and **add** the ways instead of OR-ing existence.

```python run viz=array
import ast

def count_ways(s, words):
    word_set = set(words)
    n = len(s)
    dp = [0] * (n + 1)
    dp[0] = 1                                      # one way to segment the empty prefix
    # Your code goes here — SUM the ways for each valid last word
    return dp[n]

s = input()
words = ast.literal_eval(input())
print(count_ways(s, words))
```

```java run viz=array
import java.util.*;

public class Main {
    static int countWays(String s, String[] words) {
        // Your code goes here — seed dp[0]=1, add dp[j] for each valid last word
        return 0;
    }
    static String[] parseStringArray(String line) {
        line = line.trim();
        if (line.equals("[]")) return new String[0];
        line = line.substring(1, line.length() - 1);
        String[] parts = line.split(",\\s*");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = parts[i].trim().replaceAll("^\"|\"$", "");
        return result;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine().trim();
        String[] words = parseStringArray(sc.nextLine());
        System.out.println(countWays(s, words));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "aaa" },
    { "id": "words", "label": "words", "type": "string[]", "placeholder": "[\"a\", \"aa\"]" }
  ],
  "cases": [
    { "args": { "s": "aaa", "words": "[\"a\", \"aa\"]" }, "expected": "3" },
    { "args": { "s": "leetcode", "words": "[\"leet\", \"code\"]" }, "expected": "1" },
    { "args": { "s": "ab", "words": "[\"a\", \"b\", \"ab\"]" }, "expected": "2" }
  ]
}
```

<details>
<summary>Editorial</summary>

Same skeleton as word break — flip `dp` from booleans to integers, seed `dp[0] = 1`, and **add** the ways instead of OR-ing existence.

```python solution time=O(n²·L) space=O(n)
import ast

def count_ways(s, words):
    word_set = set(words)
    n = len(s)
    dp = [0] * (n + 1)
    dp[0] = 1                                      # one way to segment the empty prefix
    for i in range(1, n + 1):
        for j in range(i):
            if dp[j] and s[j:i] in word_set:
                dp[i] += dp[j]                     # SUM the ways (vs OR for existence)
    return dp[n]

s = input()
words = ast.literal_eval(input())
print(count_ways(s, words))
```

```java solution
import java.util.*;

public class Main {
    static int countWays(String s, String[] words) {
        Set<String> dict = new HashSet<>(Arrays.asList(words));
        int n = s.length();
        int[] dp = new int[n + 1];
        dp[0] = 1;
        for (int i = 1; i <= n; i++)
            for (int j = 0; j < i; j++)
                if (dp[j] > 0 && dict.contains(s.substring(j, i))) dp[i] += dp[j];
        return dp[n];
    }
    static String[] parseStringArray(String line) {
        line = line.trim();
        if (line.equals("[]")) return new String[0];
        line = line.substring(1, line.length() - 1);
        String[] parts = line.split(",\\s*");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = parts[i].trim().replaceAll("^\"|\"$", "");
        return result;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine().trim();
        String[] words = parseStringArray(sc.nextLine());
        System.out.println(countWays(s, words));
    }
}
```

</details>

`"aaa"` segments three ways with `{a, aa}` (`a|a|a`, `a|aa`, `aa|a`); `"leetcode"` has exactly one. You changed *one operator* — `dp[i] = True` became `dp[i] += dp[j]` — and the boolean "can we?" became the counting "how many ways?". OR for existence, MIN for cost, SUM for count: the aggregator is the only thing that ever changes.

## Reflect & Connect

- **One skeleton, three questions.** The 1D prefix DP "fix the last piece" answers *can we?* (OR), *cheapest?* (MIN, [palindrome partitioning](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/palindrome-partitioning)), and *how many?* (SUM) — by swapping only the aggregator. Recognising the shared shape is worth more than memorising any one instance.
- **Memoisation is the whole point here.** Naïve recursion re-solves each suffix through exponentially many prefixes (266k calls); caching on the suffix makes it `O(n²)`. Word break is the canonical "overlapping subproblems" demonstration.
- **The predicate must be `O(1)`.** Hash the dictionary into a set so "is this a word?" is a membership test, not a scan. (For huge dictionaries or prefix queries, a [trie](/cortex/data-structures-and-algorithms/trees/trie/introduction-to-tries) replaces the set and prunes impossible branches early.)
- **`dp[0] = True` is load-bearing.** The empty-prefix base case is what lets the *first* word ever set `dp[len(word)]`. Forget it and the whole array stays `False`. (Min-cut's `cuts` base played the same role.)
- **Enumerating is a different beast.** [Word Break II](https://leetcode.com/problems/word-break-ii/) (return every sentence) can't be polynomial — there can be exponentially many sentences, so it's backtracking, not DP. Counting them (this Your Turn) stays `O(n²)`; *listing* them cannot.

## Recall

<details>
<summary><strong>Q:</strong> What is the word-break recurrence and base case?</summary>

**A:** `dp[i]` = can the first `i` characters be segmented. `dp[0] = True` (empty prefix). `dp[i] = OR over j in 0..i-1 of (dp[j] and s[j..i-1] in dict)`. The answer is `dp[n]`.

</details>
<details>
<summary><strong>Q:</strong> How is word break related to palindrome partitioning?</summary>

**A:** Identical 1D prefix DP, fixing the last piece. Word break swaps the palindrome check for a dictionary lookup and uses an **OR** aggregator (does any split work?) instead of palindrome partitioning's **MIN** (fewest cuts).

</details>
<details>
<summary><strong>Q:</strong> Why is memoisation essential, not just an optimisation?</summary>

**A:** Naïve recursion re-solves each suffix through exponentially many prefix paths (e.g. 266,079 calls on `"a"*20+"b"`). Caching on the suffix solves each once (58 calls), turning exponential into `O(n²)` — the overlapping-subproblems property in action.

</details>
<details>
<summary><strong>Q:</strong> Why store the dictionary in a hash set?</summary>

**A:** The per-piece test "is `s[j..i-1]` a word?" runs inside the `O(n²)` loops. A hash set makes each test `O(L)` (hash the candidate) instead of `O(dictionary size)`; scanning the list would blow up the complexity.

</details>
<details>
<summary><strong>Q:</strong> How do you turn "can we segment?" into "how many segmentations?"</summary>

**A:** Change the aggregator: make `dp` integers, seed `dp[0] = 1`, and do `dp[i] += dp[j]` for each valid last word instead of `dp[i] = True`. SUM counts; OR only detects existence.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15 — optimal substructure and overlapping subproblems, the two properties this lesson exhibits in their starkest form.
- **LeetCode** 139 (Word Break) and 140 (Word Break II, enumerate all) are the canonical drills; the `true`/`false`, the `266,079`-vs-`58` call counts, and the `3`/`1` segmentation counts above all come from the runnable blocks — re-run to verify.
