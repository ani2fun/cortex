---
title: Rabin-Karp and Rolling Hash
summary: "Compare hashes, not characters. Hash the pattern once, roll a polynomial hash over each text window in O(1), and verify char-by-char only when hashes match. Expected O(n + m); the all-windows superpower behind plagiarism detection and duplicate-substring search."
prereqs:
  - strings-string-matching-naive
  - linear-structures-hash-table-what-is-a-hash-table
---

## Why It Exists

[Naive](/cortex/data-structures-and-algorithms/strings/string-matching-naive), [KMP](/cortex/data-structures-and-algorithms/strings/kmp), and [Z](/cortex/data-structures-and-algorithms/strings/z-algorithm) all compare *characters*. Rabin-Karp compares *numbers*: hash the pattern to a single integer, then slide a window over the text and compare each window's hash to the pattern's. A hash match is a *candidate* — you confirm it with a quick character check, because different strings can hash alike.

The catch that makes it practical is the **rolling hash**: when the window slides one position, you don't re-hash all `m` characters — you update the hash in `O(1)` by removing the departing character and folding in the arriving one. That gives expected `O(n + m)`. And because matching is now "does this number appear in my set of hashes?", Rabin-Karp does something KMP and Z can't do cheaply: search for **many patterns at once**, or find **any repeated substring** — just hash every window and look for collisions. That's the engine behind plagiarism detection and duplicate-block finding.

## See It Work

Hash the pattern and the first window with a polynomial hash `Σ c · BASE^k mod P`. Then roll: subtract the leaving character's contribution, multiply by `BASE`, add the entering character — all mod a large prime. On a hash match, verify.

```python run viz=array
BASE, MOD = 256, 1_000_000_007
def rabin_karp(text, pattern):
    n, m = len(text), len(pattern)
    if m > n:
        return []
    high = pow(BASE, m - 1, MOD)                      # BASE^(m-1): weight of the leaving char
    hp = ht = 0
    for i in range(m):                                # hash the pattern and the first window
        hp = (hp * BASE + ord(pattern[i])) % MOD
        ht = (ht * BASE + ord(text[i])) % MOD
    hits = []
    for i in range(n - m + 1):
        if hp == ht and text[i:i + m] == pattern:     # hash match -> VERIFY (collisions happen)
            hits.append(i)
        if i < n - m:                                 # roll the window in O(1)
            ht = ((ht - ord(text[i]) * high) * BASE + ord(text[i + m])) % MOD
    return hits

text = input()
pattern = input()
print(rabin_karp(text, pattern))
```

```java run viz=array
import java.util.*;
public class Main {
    static final long BASE = 256, MOD = 1_000_000_007L;
    static List<Integer> rabinKarp(String text, String pattern) {
        int n = text.length(), m = pattern.length();
        List<Integer> hits = new ArrayList<>();
        if (m > n) return hits;
        long high = 1;
        for (int i = 0; i < m - 1; i++) high = high * BASE % MOD;
        long hp = 0, ht = 0;
        for (int i = 0; i < m; i++) {
            hp = (hp * BASE + pattern.charAt(i)) % MOD;
            ht = (ht * BASE + text.charAt(i)) % MOD;
        }
        for (int i = 0; i <= n - m; i++) {
            if (hp == ht && text.substring(i, i + m).equals(pattern)) hits.add(i);
            if (i < n - m)
                ht = (((ht - text.charAt(i) * high) % MOD + MOD) % MOD * BASE + text.charAt(i + m)) % MOD;
        }
        return hits;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String text = sc.nextLine();
        String pattern = sc.nextLine();
        System.out.println(rabinKarp(text, pattern));
    }
}
```

```testcases
{
  "args": [
    { "id": "text", "label": "text", "type": "string", "placeholder": "abxabcabcaby" },
    { "id": "pattern", "label": "pattern", "type": "string", "placeholder": "abcaby" }
  ],
  "cases": [
    { "args": { "text": "abxabcabcaby", "pattern": "abcaby" }, "expected": "[6]" },
    { "args": { "text": "abcabcabc", "pattern": "abc" }, "expected": "[0, 3, 6]" },
    { "args": { "text": "aababcaab", "pattern": "aab" }, "expected": "[0, 6]" },
    { "args": { "text": "ababababab", "pattern": "abab" }, "expected": "[0, 2, 4, 6]" },
    { "args": { "text": "hello", "pattern": "xyz" }, "expected": "[]" }
  ]
}
```

Both print `[6]`. The pattern's hash is computed once; each of the `n - m + 1` windows costs `O(1)` to roll and compare, with a full character check only on a hash match. (Java subtracts the leaving term with an extra `+ MOD` before `% MOD` because Java's `%` can go negative — Python's stays non-negative.)

## How It Works

A polynomial hash treats the window as a base-`BASE` number mod `P`. Sliding the window is arithmetic, not re-reading:

```d2
direction: right
window: "current window hash ht (mod P)\ne.g. hash of text[i..i+m-1]" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
roll: "ROLL to next window in O(1):\n1. subtract leaving char: ht - text[i]*BASE^(m-1)\n2. shift left: * BASE\n3. add entering char: + text[i+m]\n(all mod P)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
cmp: "compare ht to pattern hash hp" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
verify: "hp == ht?  VERIFY char-by-char\n(hashes can collide -> false candidate)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
window -> roll
roll -> cmp
cmp -> verify
```

<p align="center"><strong>The rolling update turns an O(m) re-hash into O(1) arithmetic: drop the leaving character's weighted contribution, shift, add the new character. A hash match is only a candidate — verify, because collisions are possible.</strong></p>

Three load-bearing facts:

- **Verification is non-negotiable for correctness.** Equal hashes do *not* guarantee equal strings — the hash maps many strings to each value. So a hash match triggers an `O(m)` character check. Skip it and you have a **Monte Carlo** algorithm (fast, but may report false matches); keep it and you have a **Las Vegas** algorithm (always correct, expected-fast) — the exact distinction from [randomized algorithms](/cortex/data-structures-and-algorithms/algorithms-by-strategy/randomized-algorithms/introduction-to-randomized-algorithms).
- **Expected `O(n + m)`, worst `O(n · m)`.** With a good hash, collisions are rare, so verification fires `O(1)` times overall and the scan is linear. But a pathological input (or an adversary who crafts colliding strings) can make every window collide, forcing a verify at each — back to naive's quadratic. A large prime modulus and a random base make that astronomically unlikely.
- **The superpower is many-at-once.** Hash every length-`m` window into a set, and membership tests answer "does *any* of these patterns occur?" or "does any substring repeat?" in one pass — something KMP/Z would need a separate run per pattern for. This is why plagiarism detectors and `rsync`-style block matchers use rolling hashes.

> **Key takeaway.** Rabin-Karp compares hashes: hash the pattern, roll a polynomial hash over each window in `O(1)` (subtract leaving · `BASE^{m-1}`, ×`BASE`, add entering, mod a large prime), and **verify char-by-char on a hash match**. Expected `O(n + m)`, worst `O(n·m)`. Verify → Las Vegas (correct); skip → Monte Carlo (may false-match). Its edge over KMP/Z is hashing *all* windows for multi-pattern and repeated-substring search.

## Trace It

The verification step looks like a paranoid afterthought — surely if two strings hash to the same value, they're equal? They are not, and a weak hash makes that failure easy to see.

**Predict before you run:** use a deliberately weak hash — the *sum* of character codes. Search `"bcad"` for `"ad"` and report every window whose hash equals the pattern's, **without** the character check. Does it find only the real `"ad"` at index 2, or something extra?

```python run viz=array
def sum_hash_search(text, pattern, verify):
    m = len(pattern)
    target = sum(ord(c) for c in pattern)             # weak hash: sum of char codes
    hits = []
    for i in range(len(text) - m + 1):
        window = text[i:i + m]
        if sum(ord(c) for c in window) == target:     # hash match
            if not verify or window == pattern:        # only confirm if verify=True
                hits.append(i)
    return hits

print("hash('ad') =", ord('a') + ord('d'), " hash('bc') =", ord('b') + ord('c'))
print("no verify :", sum_hash_search("bcad", "ad", verify=False))
print("verify    :", sum_hash_search("bcad", "ad", verify=True))
```

<details>
<summary><strong>Reveal</strong></summary>

Without verification you get `[0, 2]` — a **false match at index 0**, where the window is `"bc"`. The reason is a collision: `"ad"` hashes to `97 + 100 = 197`, and `"bc"` hashes to `98 + 99 = 197` — identical sums, different strings. A sum-of-codes hash is *permutation-blind* (any anagram collides) and has a tiny range, so collisions are everywhere. With the `verify` check, the index-0 candidate is rejected (`"bc" != "ad"`) and you correctly get `[2]`. This is exactly why real Rabin-Karp verifies: the hash is a *filter* that cheaply rules out most positions, not a proof of equality. A good polynomial hash with a large prime modulus makes collisions astronomically rare (so verification almost never fires), but "rare" is not "never" — drop the check and you've silently switched to a Monte Carlo algorithm that can lie. The hash narrows the search; the character comparison closes it.

</details>

## Your Turn

**Find repeated substrings of length `k`** — the multi-window strength of rolling hashes (the basis of [Repeated DNA Sequences](https://leetcode.com/problems/repeated-dna-sequences/), LeetCode 187). Roll a hash across every length-`k` window, bucket by hash, and report substrings that recur (verifying within a bucket to dodge collisions).

```python run viz=array
BASE, MOD = 256, 1_000_000_007
def find_repeated(s, k):
    # Your code goes here
    return []

s = input()
k = int(input())
xs = find_repeated(s, k)
print('[' + ', '.join(xs) + ']')
```

```java run viz=array
import java.util.*;
public class Main {
    static final long BASE = 256, MOD = 1_000_000_007L;
    static List<String> findRepeated(String s, int k) {
        // Your code goes here
        return new ArrayList<>();
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        int k = Integer.parseInt(sc.nextLine());
        System.out.println(findRepeated(s, k));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "string", "type": "string", "placeholder": "banana" },
    { "id": "k", "label": "k (window length)", "type": "number", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "s": "banana", "k": 3 }, "expected": "[ana]" },
    { "args": { "s": "abcabcabc", "k": 3 }, "expected": "[abc, bca, cab]" },
    { "args": { "s": "hello", "k": 2 }, "expected": "[]" },
    { "args": { "s": "aaaa", "k": 2 }, "expected": "[aa]" },
    { "args": { "s": "abcdef", "k": 3 }, "expected": "[]" }
  ]
}
```

In `"banana"`, only `"ana"` recurs (indices 1 and 3); in `"abcabcabc"`, all three length-3 windows repeat. This is the move KMP and Z can't make in one pass — *every* window's fingerprint is available at once, so "what repeats?" is a hashmap lookup, not a fresh search per candidate.

<details>
<summary><strong>Editorial</strong></summary>

Roll the polynomial hash across every length-`k` window, bucketted by hash value. When a hash has been seen before, verify by comparing the actual strings; confirmed repeats go into a sorted set. Both Python and Java collect into a sorted structure — Python uses `sorted(repeated)` on a set, Java uses a `TreeSet`. The Python print uses `'[' + ', '.join(xs) + ']'` to match Java's `List<String>` output (no quotes around elements).

```python solution time=O(n) space=O(n)
BASE, MOD = 256, 1_000_000_007
def find_repeated(s, k):
    n = len(s)
    if k > n:
        return []
    high = pow(BASE, k - 1, MOD)
    h = 0
    for i in range(k):
        h = (h * BASE + ord(s[i])) % MOD
    seen, repeated = {}, set()
    for i in range(n - k + 1):
        w = s[i:i + k]
        if h in seen and any(s[j:j + k] == w for j in seen[h]):   # verify within the bucket
            repeated.add(w)
        seen.setdefault(h, []).append(i)
        if i < n - k:
            h = ((h - ord(s[i]) * high) * BASE + ord(s[i + k])) % MOD
    return sorted(repeated)

s = input()
k = int(input())
xs = find_repeated(s, k)
print('[' + ', '.join(xs) + ']')
```

```java solution
import java.util.*;
public class Main {
    static final long BASE = 256, MOD = 1_000_000_007L;
    static List<String> findRepeated(String s, int k) {
        int n = s.length();
        TreeSet<String> repeated = new TreeSet<>();
        if (k > n) return new ArrayList<>(repeated);
        long high = 1;
        for (int i = 0; i < k - 1; i++) high = high * BASE % MOD;
        long h = 0;
        for (int i = 0; i < k; i++) h = (h * BASE + s.charAt(i)) % MOD;
        Map<Long, List<Integer>> seen = new HashMap<>();
        for (int i = 0; i <= n - k; i++) {
            String w = s.substring(i, i + k);
            if (seen.containsKey(h))
                for (int j : seen.get(h)) if (s.substring(j, j + k).equals(w)) { repeated.add(w); break; }
            seen.computeIfAbsent(h, x -> new ArrayList<>()).add(i);
            if (i < n - k)
                h = (((h - s.charAt(i) * high) % MOD + MOD) % MOD * BASE + s.charAt(i + k)) % MOD;
        }
        return new ArrayList<>(repeated);
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        int k = Integer.parseInt(sc.nextLine());
        System.out.println(findRepeated(s, k));
    }
}
```

</details>

## Reflect & Connect

- **Compare numbers, then confirm.** The hash is a cheap filter that rejects almost every position; the character check confirms the survivors. Equal hashes never *prove* equality — verification does.
- **Rolling makes it linear.** Updating the hash in `O(1)` per shift (drop the weighted leaving char, shift, add the entering char) is the whole reason it beats re-hashing. Expected `O(n + m)`.
- **Las Vegas vs Monte Carlo, concretely.** Verify on a hash match → always correct, expected-fast (Las Vegas). Trust the hash blindly → fast but occasionally wrong (Monte Carlo). Same code, one `if`.
- **The all-windows superpower.** Hash every window into a set and you get multi-pattern search, duplicate-substring detection, and 2-D pattern matching essentially for free — the use cases [KMP](/cortex/data-structures-and-algorithms/strings/kmp)/[Z](/cortex/data-structures-and-algorithms/strings/z-algorithm) handle poorly.
- **Pick the modulus and base well.** A large prime modulus and a *random* base make adversarial collisions astronomically unlikely — the same "randomize to defeat worst-case inputs" idea as randomized [hash tables](/cortex/data-structures-and-algorithms/linear-structures/hash-table/what-is-a-hash-table) resisting HashDoS. A tiny or fixed hash is exploitable.

## Recall

<details>
<summary><strong>Q:</strong> What does Rabin-Karp compare, and why must it verify?</summary>

**A:** It compares hashes of the pattern and each text window. Equal hashes are only a *candidate* — many strings share a hash (collisions) — so it confirms with an `O(m)` character check. Skipping verification makes it Monte Carlo (may report false matches).

</details>
<details>
<summary><strong>Q:</strong> How does the rolling hash update in O(1)?</summary>

**A:** Subtract the leaving character's weighted contribution (`text[i] · BASE^{m-1}`), multiply by `BASE` (shift), add the entering character (`text[i+m]`), all mod a large prime. No re-reading of the window.

</details>
<details>
<summary><strong>Q:</strong> What are Rabin-Karp's expected and worst-case times?</summary>

**A:** Expected `O(n + m)` (collisions rare, so verification fires `O(1)` times overall); worst `O(n·m)` when every window collides (pathological or adversarial input), forcing a verify at each position.

</details>
<details>
<summary><strong>Q:</strong> Why is a sum-of-character-codes hash a poor choice?</summary>

**A:** It's permutation-blind (every anagram collides, e.g. `"ad"` and `"bc"` both sum to 197) and has a tiny range, so collisions are rampant. A polynomial hash with a large prime modulus is position-sensitive and collision-resistant.

</details>
<details>
<summary><strong>Q:</strong> What can Rabin-Karp do that KMP and Z cannot do cheaply?</summary>

**A:** Search for *many* patterns at once, or find *any* repeated substring, in a single pass — by hashing every window into a set and testing membership. KMP/Z need a separate run per pattern.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §32.2 — the Rabin-Karp algorithm, the rolling polynomial hash, and its expected-time analysis.
- **Karp & Rabin** (1987), "Efficient randomized pattern-matching algorithms", *IBM J. Res. Dev.* — the original, including the Monte Carlo / Las Vegas framing.
- **LeetCode** 187 (Repeated DNA Sequences), 1044 (Longest Duplicate Substring), and 28 (substring search) are the canonical drills; the `[6]` match, the sum-hash collision `[0,2]`-vs-`[2]`, and the `['ana']` / `['abc','bca','cab']` repeats above come from the runnable blocks — re-run to verify.
