---
title: "Pattern Synthesis: Hash Table"
summary: "When to use which pattern, common confusions, and synthesis problems for hash table."
prereqs:
  - 02-linear-structures/07-hash-table/11-pattern-prefix-sum/03-memorize
---

# Pattern Synthesis: Hash Table

Every hash-table pattern in this chapter rides the same primitive: amortised `O(1)` insert, lookup, and update keyed by value. Each pattern reads the map for a different reason. The hard part is not learning the five in isolation. It is choosing the right one when a fresh problem lands and three of them look plausible at once. This synthesis chapter exists for that moment: read the problem, run the decision tree, confirm with the comparison table, then write code.

The five patterns in scope:

- **Counting** — sweep once, tally each item into a frequency map; answer "how many of each" by reading the counts.
- **Key Generation** — fingerprint each item into a canonical key so structurally equivalent inputs hash to byte-identical keys; let collisions become groups.
- **Fixed-Sized Sliding Window** — slide a window of constant width `k`, keeping a map of the window's contents updated by one add and one evict per step.
- **Variable-Sized Sliding Window** — grow the window on the right, shrink from the left while a map-checked rule is violated; record the best valid window.
- **Prefix-Sum (+ hash map)** — carry a running sum and a map of seen prefix values; a repeat or a `K`-apart difference names a subarray, even with negatives.

---

## When to Use Which Pattern

The first question is *what the map holds*, because that single answer splits all five patterns into families. A map of raw tallies is counting. A map keyed by a derived signature is key generation. A map summarising a moving range is one of the two sliding windows. A map of past running sums is prefix-sum. The second question — how the map is queried, and whether the answer is occurrence-based, group-based, or subarray-based — picks the leaf.

Walk the tree top to bottom; stop at the first match.

1. **Does the answer depend on a *contiguous subarray* — its sum, or a property that reduces to a sum?**
   - The values can be negative or zero, or the target is an *exact* sum (not a "longest under a budget") → **Prefix-Sum**. Carry a running sum; query a map of past sums. Negatives are the disqualifier for a window and the reason this pattern exists.
   - The values are all non-negative and the rule is *monotonic* (a budget the window must respect) → a sliding window (continue to step 2).

2. **Does the answer come from a window sliding over the sequence, summarised by a map?**
   - The window width is *fixed* — given as `k`, or set to `len(pattern)` → **Fixed-Sized Sliding Window**. Add the joiner, drop the leaver, process when the width hits `k`.
   - The window width is *not given* — it grows and shrinks to satisfy a monotonic rule, and you want the longest, shortest, or count of valid windows → **Variable-Sized Sliding Window**. Expand greedily; contract on violation.

3. **Does the answer depend on grouping or comparing inputs by *structural equivalence* rather than raw equality?**
   - Two inputs are "the same" under some transform — same shape, same keyboard row, same character shift — and you group or compare by that transform → **Key Generation**. Design a key so sameness becomes byte-equality.

4. **Does the answer depend on *how often* items appear, read after a single tally pass?**
   - Uniqueness, multiset equality, subset-of, palindrome buildability — pure occurrence questions → **Counting**. Build the frequency map first, then read it.

5. If nothing above matches, the problem is probably outside this chapter — try two pointers, a monotonic stack, or a different structure entirely.

To make this concrete, run the tree on one statement per pattern and confirm the branches resolve cleanly:

- **"Which character in `s` is the first non-repeating one?"** → Step 4 yes; tally, then re-scan for the count-1 character → **Counting**.
- **"Group all anagrams in this list."** → Step 3 yes; key each string by its letter signature, bucket by key → **Key Generation**.
- **"Does any window of size `k` contain a duplicate?"** → Step 2 yes; fixed width `k` → **Fixed-Sized Sliding Window**.
- **"Longest substring with at most `K` distinct characters."** → Step 2 yes; width set by a rule, not given → **Variable-Sized Sliding Window**.
- **"Count subarrays summing to exactly `K`, array has negatives."** → Step 1 yes; exact sum and negatives → **Prefix-Sum**.

So the key idea is: the map's *payload* narrows to one of four families — tally, canonical key, window summary, or past-sum record. Then the *query* — read counts, group keys, slide-and-process, or look up a difference — picks the exact pattern.

---

## Pattern Comparison Table

All five patterns at a glance. `N` is the input length, `n` the array or string length, `k` a problem-given window size or budget, and `K` the number of distinct items (the map's size) — `O(1)` for a fixed alphabet, `O(n)` worst case.

| Pattern | Problem shape | Key signal | Time | Space | Confused with |
|---|---|---|---|---|---|
| **Counting** | Tally every item in one pass, then read the counts to answer an occurrence question | "First non-repeating", "is X an anagram of Y", "can X be built from Y's letters", "longest palindrome from these letters" | `O(N)` | `O(K)` | Key Generation (tally frequencies vs bucket by a canonical key?); Sliding Window (whole-sequence count vs per-window count?) |
| **Key Generation** | Fingerprint each input into a canonical key; equal keys group or compare as equivalent | "Group the anagrams", "are these two strings homomorphic", "cluster shifted strings", "which words use one keyboard row" | `O(N)` per input | `O(K)` | Counting (canonical key vs integer tally?); Fixed Window (key the whole input vs key each window?) |
| **Fixed-Sized Sliding Window** | Slide a width-`k` window with a map updated by one add and one evict per step; process at full width | "Any duplicate in a window of size `k`", "distinct count per window", "find all anagrams/permutations of `p`" | `O(N)` | `O(k)` | Variable Window (is `k` given, or set by a rule?); Counting (per-window tally vs whole-sequence tally?) |
| **Variable-Sized Sliding Window** | Grow `end`, shrink `start` while a map-checked monotonic rule is violated; record the best valid window | "Longest substring without repeats", "longest with at most `K` distinct", "longest run after `k` replacements" | `O(N)` | `O(K)` | Fixed Window (rule-driven width vs fixed width?); Prefix-Sum (monotonic budget vs exact sum / negatives?) |
| **Prefix-Sum (+ hash map)** | Carry a running sum; a map of past sums turns "subarray summing to `K`" or "balanced slice" into one lookup | "Subarray sum equals `k`", "count zero-sum subarrays", "equal 0s and 1s", "product except self", "equilibrium index" | `O(N)` | `O(N)` | Variable Window (exact sum / negatives vs monotonic budget?); Counting (subarray difference vs whole-sequence count?) |

---

## Common Confusions

Four pairs trip readers up most often. Each subsection states the surface similarity, the one question that cuts through it, and the symptom that betrays a wrong choice.

### Counting vs Key Generation (tally vs canonical key)

**Why they look the same.** Both build a hash map in one pass over the input, both run `O(N)` time, and both lean on `O(1)` map operations — from a distance, "sweep once into a map" describes either one.

**The distinguishing test.** Do you need a per-item *integer tally* ("how often did X appear?") or a per-group *canonical key* ("is X equivalent to something seen before?")?

**Telltale symptom of wrong choice.** You stored plain counts and now cannot answer an equivalence question — `cab` and `abc` look unrelated because nothing collapsed them to a shared key, so anagram grouping never happens. Conversely, you built canonical keys for a problem that only asked "how many of each," paying to design a fingerprint where one increment per item would have answered it.

### Fixed Window vs Variable Window (what sets the width)

**Why they look the same.** Both maintain a `start`/`end` window over one sequence with a map summary, both run the "add the joiner, drop the leaver" mechanic, and both finish in `O(N)` time.

**The distinguishing test.** Is the window width a *fixed constant* (given as `k`, or set to `len(pattern)`), or is it *set by a rule* the window must satisfy?

**Telltale symptom of wrong choice.** You start writing an inner `while` to shrink the window past size `k` — but fixed never shrinks past `k`; that inner loop belongs only to the variable pattern. Conversely, you guard `if end − start + 1 > k` for an unknown `k`: the variable pattern does not pin a width, so the guard fires against a number the problem never gave you.

### Variable Window vs Prefix-Sum (monotonic budget vs exact sum)

**Why they look the same.** Both walk one forward sweep over a sequence with a hash map, both answer subarray questions, and both reach `O(N)` time — "scan once, ask about a subarray" fits either.

**The distinguishing test.** Is the rule *monotonic* on a non-negative array (extending only worsens it, contracting only improves it), or does the problem demand an *exact* sum, or contain *negatives* that let extending and contracting both move the sum either way?

**Telltale symptom of wrong choice.** Your window overshoots the target sum and there is no safe direction to shrink — a negative element lowered the sum you were trying to raise, so contraction guesses and the answer comes out wrong. Conversely, you paid `O(N)` space on a map of prefix sums for an all-positive "longest under a budget" problem that a variable window would have solved in `O(1)` working space.

### Counting vs Fixed Window (whole-sequence tally vs per-window tally)

**Why they look the same.** Both keep a frequency map and answer occurrence questions, and a fixed-window problem's per-window map *is* a small counting map — the inner machinery is the same tally.

**The distinguishing test.** Does the question span the *whole sequence at once* (one tally, read at the end) or *every contiguous window of size `k`* (a tally that slides, read once per window)?

**Telltale symptom of wrong choice.** You built one global frequency map for an "every window of size `k`" question, so your answer reflects the entire array instead of any single window — the per-window structure is gone. Conversely, you slid a width-`k` window for a question about the whole input, needlessly re-deriving a count that one straight pass would have produced.

---

## Synthesis Problems

Three problems where two or more patterns *seem* to fit. The right call is non-obvious from the statement alone; the wrong call costs you correctness or a complexity tier.

### Problem 1 — Cluster Anagrams

**Statement.** Given an array of strings `strs`, group all anagrams together and return the groups in any order. (See [Cluster Anagrams](07-pattern-counting/02-problems/05-cluster-anagrams.md).)

**Why multiple patterns seem viable.**

- **Counting** seems viable because the file lives under the counting pattern, and deciding whether two strings are anagrams *is* a frequency comparison — equal letter tallies mean equal multisets.
- **Key Generation** is the actual winner — fingerprint each string by its letter signature and bucket on the key.

**Winner.** Key Generation, with the per-string letter-count signature (a sorted form or 26-slot tuple) as the bucket key.

**Why winner wins.** Counting answers "are *these two* strings anagrams?" in `O(N)`, but the question is "partition *all* strings into anagram classes," and a pairwise tally-compare across `M` strings is `O(M²)` comparisons. The decisive move is to make sameness *byte-equality*: hash each string to a canonical key once, and two anagrams land in the same bucket automatically — `O(M)` keyed inserts instead of `O(M²)` comparisons. Counting is not absent, it is *demoted*: the letter tally is how you *build* the key, not the answer itself. This is the border case the counting lesson flags — counting is the means, grouping is the end, and the end is what names the pattern. The signal that decides it is "form equivalence classes," which is key generation's fingerprint, not a tally read.

### Problem 2 — Subarray Sum Equals K

**Statement.** Given an integer array `arr` and a target `k`, return the maximum length of a subarray whose elements sum to `k`; return `0` if none exists. The array may contain negatives. (See [Subarray Sum Equals K](10-pattern-variable-sized-sliding-window/02-problems/04-subarray-sum-equals-k.md).)

**Why multiple patterns seem viable.**

- **Variable-Sized Sliding Window** seems viable because the file sits under the variable-window pattern, and "longest contiguous subarray satisfying a sum condition" is the window pattern's exact problem shape.
- **Prefix-Sum** is the actual winner — carry a running sum and look up `prefixSum − k` in a map of first occurrences.

**Winner.** Prefix-Sum, with a map of each prefix value's first index and the base case `{0: -1}`.

**Why winner wins.** A variable window relies on a *monotonic* rule: extending grows the sum, contracting shrinks it, so on violation you always know which way to shrink. Negatives destroy that invariant — adding an element can *lower* the sum, removing one can *raise* it, so contracting on overshoot is guessing and the window silently returns wrong answers. Prefix-sum sidesteps direction entirely: the sum of `arr[l..r]` is `P[r+1] − P[l]`, so "subarray summing to `k`" becomes "two prefix values differing by `k`" — one map lookup per index, `O(N)` time and `O(N)` space. The base case `{0: -1}` is what lets a slice anchored at index `0` register its full length. The split that decides it is whether the rule is monotonic on non-negative values (window) or demands an exact sum with negatives present (prefix-sum) — and the negatives here force the latter.

### Problem 3 — Anagram Finder

**Statement.** Given strings `s` and `p`, return all start indices in `s` of substrings that are anagrams of `p`. (See [Anagram Finder](09-pattern-fixed-sized-sliding-window/02-problems/04-anagram-finder.md).)

**Why multiple patterns seem viable.**

- **Counting** seems viable because "is this substring an anagram of `p`?" is a frequency comparison — the counting pattern's bread and butter.
- **Key Generation** seems viable because anagram membership is exactly the canonical-key equivalence — sort each candidate substring and compare to `p`'s sorted key.
- **Fixed-Sized Sliding Window** is the actual winner.

**Winner.** Fixed-Sized Sliding Window of width `len(p)`, carrying a frequency map of the window compared against `p`'s map.

**Why winner wins.** Every candidate substring has the *same* width — `len(p)` — which is the fixed-window signal, not a rule-driven one. Plain counting gets the anagram test right but throws away the sliding structure: re-tallying each width-`len(p)` substring from scratch is `O(len(p))` per position, so the whole scan is `O(N · len(p))` — the exact brute-force cost the window eliminates. Key generation is worse still: sorting each window is `O(len(p) log len(p))` per position, paying a log factor to answer what a frequency compare answers flat. The fixed window keeps one map updated by a single add and a single evict per slide, comparing it to `p`'s fixed map in `O(1)` amortised, for `O(N)` total. Counting is not gone — it is the window's *payload* — but the pattern that moves the tally is fixed sliding window. The deciding signal is "every window of a known width," which pins the fixed pattern; the counting and key-generation framings both ignore that the width is constant and the work overlaps between adjacent positions.

---

## Hash-Table-Specific Synthesis

Every pattern in this chapter is one hash map with amortised `O(1)` operations. The *meaning of the map's payload* is what separates them, and that meaning falls into four buckets. Read the payload, and the pattern names itself.

The four payload-meanings:

- **Tally payload (counting).** The map values are integer counts, built in one whole-sequence pass and read afterwards. No window, no key transform — the map *is* the census. Two passes at most: build, then read.
- **Key payload (key generation).** The map is keyed by a *derived signature*, not the raw item, so structurally equivalent inputs collide on purpose. The value is a bucket of members (or a presence flag). Sameness is engineered into byte-equality before the map ever sees it.
- **Window-summary payload (the two sliding windows).** The map summarises a *moving range*, edited by one add and one evict per step. Fixed window pins the range at width `k`; variable window lets a monotonic rule set the width. Both touch each element at most twice, giving `O(N)`.
- **Past-sum payload (prefix-sum).** The map records every *running prefix value* the sweep has produced, so a later lookup names a subarray by difference or repeat. This is the only payload that survives negatives, because it never relies on a monotonic shrink direction.

So the tradeoff across the chapter is uniform: you trade *what the map remembers* for *what a lookup decides*. Counting remembers frequencies and decides occurrence. Key generation remembers equivalence classes and decides grouping. The windows remember a live slice and decide a per-window or best-window answer. Prefix-sum remembers the whole history of sums and decides subarray membership. Naming the payload-meaning is faster than matching surface syntax, because three of these patterns build a frequency map and only diverge in what they do with it.

---

## How to Use This Chapter

Treat the decision tree as the first pass and the comparison table as the second. If both still point to two viable patterns, walk the Common Confusions for the matching pair and run its distinguishing test on the input. The recurring trap is that the *map build* looks the same across patterns. Tally items, key items, summarise a window, record a sum — the surface "put things in a hash map" rarely separates them. The query side is where they split.

For hash tables specifically, two questions decide almost every call:

- **What does the map's payload mean?** A tally, a canonical key, a moving-window summary, or a record of past sums.
- **How is the map queried?** Read counts at the end, group by key, process per slide, or look up a difference or repeat.

Every other choice follows from those two. When a synthesis-style problem feels like every pattern fits, that is the signal to name the payload-meaning before you write a line.
