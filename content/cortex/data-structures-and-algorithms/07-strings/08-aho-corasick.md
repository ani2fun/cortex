---
title: Aho-Corasick
summary: "A trie of all patterns plus failure links — KMP generalised to many patterns at once. One pass over the text finds every occurrence of every pattern in O(text + total pattern length + matches). Output links ensure suffix-patterns (he inside she) are never missed."
prereqs:
  - strings-kmp
  - trees-trie-introduction-to-tries
---

## Why It Exists

You have a text and a *dictionary* of patterns — a virus database, a profanity list, a set of DNA motifs — and you want every occurrence of every pattern. Running [KMP](/cortex/data-structures-and-algorithms/strings-kmp) once per pattern costs `O(k·n)` for `k` patterns: the text is re-scanned `k` times. **Aho-Corasick** does it in a *single* pass: `O(n + Σ|pattern| + matches)`, independent of how many patterns share the text.

The idea fuses two structures you already know. Put all patterns in a [trie](/cortex/data-structures-and-algorithms/trees-trie-introduction-to-tries) (so shared prefixes are walked once). Then add **failure links** — exactly KMP's "where do I jump on a mismatch?", but lifted from a single string to the whole trie: each node's failure link points to the node spelling the *longest proper suffix* that's also in the trie. Walk the text once, following trie edges where you can and failure links where you can't, and you match the entire dictionary at once. It's the engine inside `grep -F`, `fgrep`, and intrusion-detection systems.

## See It Work

Build the automaton from the classic dictionary `{he, she, his, hers}`, then scan `"ushers"` — every pattern occurrence falls out of one pass.

```python run viz=array
from collections import deque
class AhoCorasick:
    def __init__(self, patterns):
        self.goto = [{}]; self.fail = [0]; self.out = [[]]
        for p in patterns:                                # 1. build the trie of patterns
            node = 0
            for c in p:
                if c not in self.goto[node]:
                    self.goto.append({}); self.fail.append(0); self.out.append([])
                    self.goto[node][c] = len(self.goto) - 1
                node = self.goto[node][c]
            self.out[node].append(p)
        q = deque(self.goto[0].values())                  # 2. BFS to build failure + output links
        while q:
            u = q.popleft()
            for c, v in self.goto[u].items():
                q.append(v)
                f = self.fail[u]
                while f and c not in self.goto[f]:         # walk failure links (KMP, on the trie)
                    f = self.fail[f]
                self.fail[v] = self.goto[f].get(c, 0)
                self.out[v] += self.out[self.fail[v]]      # output link: inherit suffix-patterns
    def search(self, text):
        node, hits = 0, []
        for i, c in enumerate(text):
            while node and c not in self.goto[node]:       # 3. follow goto, else fall back via fail
                node = self.fail[node]
            node = self.goto[node].get(c, 0)
            for p in self.out[node]:
                hits.append((p, i - len(p) + 1))           # (pattern, start index)
        return sorted(hits)

print(AhoCorasick(["he", "she", "his", "hers"]).search("ushers"))   # she@1, he@2, hers@2
```

```java run viz=array
import java.util.*;
public class Main {
    static List<Map<Character,Integer>> go = new ArrayList<>();
    static List<Integer> fail = new ArrayList<>();
    static List<List<String>> out = new ArrayList<>();
    static void build(String[] patterns) {
        go.clear(); fail.clear(); out.clear();
        go.add(new HashMap<>()); fail.add(0); out.add(new ArrayList<>());
        for (String p : patterns) {                                 // build the trie
            int node = 0;
            for (char c : p.toCharArray()) {
                if (!go.get(node).containsKey(c)) {
                    go.add(new HashMap<>()); fail.add(0); out.add(new ArrayList<>());
                    go.get(node).put(c, go.size() - 1);
                }
                node = go.get(node).get(c);
            }
            out.get(node).add(p);
        }
        Deque<Integer> q = new ArrayDeque<>(go.get(0).values());    // BFS for failure + output links
        while (!q.isEmpty()) {
            int u = q.poll();
            for (var e : go.get(u).entrySet()) {
                char c = e.getKey(); int v = e.getValue(); q.add(v);
                int f = fail.get(u);
                while (f != 0 && !go.get(f).containsKey(c)) f = fail.get(f);
                fail.set(v, go.get(f).getOrDefault(c, 0));
                out.get(v).addAll(out.get(fail.get(v)));            // output link
            }
        }
    }
    static List<String> search(String text) {
        int node = 0; List<String> hits = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (node != 0 && !go.get(node).containsKey(c)) node = fail.get(node);
            node = go.get(node).getOrDefault(c, 0);
            for (String p : out.get(node)) hits.add(p + "@" + (i - p.length() + 1));
        }
        Collections.sort(hits);
        return hits;
    }
    public static void main(String[] args) {
        build(new String[]{"he", "she", "his", "hers"});
        System.out.println(search("ushers"));   // [he@2, hers@2, she@1]
    }
}
```

Both report `she` at index 1, `he` at index 2, and `hers` at index 2 — three patterns, one left-to-right pass over `"ushers"`. (`he` and `hers` both start at index 2 because `hers` *contains* `he` as a prefix and `he` is also a suffix of the text seen so far.)

## How It Works

Three layers, each borrowing from a structure you know:

```d2
direction: right
trie: "1. TRIE of patterns\n(shared prefixes walked once)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
fail: "2. FAILURE links (KMP, on the trie)\nnode -> node spelling the longest PROPER SUFFIX\nstill in the trie; on a mismatch, fall back here" {style.fill: "#fde68a"; style.stroke: "#d97706"}
out: "3. OUTPUT links\nfollow failure chain to report EVERY pattern\nthat is a suffix here (so 'she' also reports 'he')" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
scan: "scan text once: follow goto, else fail;\nemit out[node] at each step\n-> O(n + sum|pattern| + matches)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
trie -> fail
fail -> out
out -> scan
```

<p align="center"><strong>Trie (prefixes) + failure links (KMP's mismatch fallback, generalised to the trie) + output links (report all suffix-patterns). One text pass follows goto edges, falls back via failure links on a mismatch, and emits every pattern ending at the current node.</strong></p>

Three load-bearing pieces:

- **Failure links are KMP on a trie.** `fail[v]` is the node spelling the *longest proper suffix* of `v`'s string that is itself a trie node. On a text mismatch you follow `fail` (just like KMP falls back to `lps[j-1]`), so the text pointer never rewinds. The links are built by **BFS** in increasing depth, because `fail[v]` is always shallower than `v` — so it's already computed when you reach `v`.
- **Output links stop you missing suffix-patterns.** When the dictionary contains patterns that are suffixes of each other (`he` is a suffix of `she`), arriving at the `she` node must *also* report `he`. The output set of a node is its own patterns *plus* the output set of its failure node — so `out[v] += out[fail[v]]` chains them. Forget this and you silently miss matches (the [Trace It](#trace-it) shows it).
- **One pass, linear in the meaningful quantities.** Building the trie is `O(Σ|pattern|)`; the BFS is the same; the scan is `O(n)` amortized (failure-link follows are bounded just like KMP) plus `O(matches)` to emit. Total `O(n + Σ|pattern| + matches)` — the text is read *once* no matter how many patterns.

> **Key takeaway.** Aho-Corasick = **trie of patterns + failure links + output links**. Failure links are KMP's mismatch-fallback generalised to the trie (built by BFS); output links chain the failure path so every suffix-pattern is reported. One text pass matches the whole dictionary in `O(n + Σ|pattern| + matches)` — versus `O(k·n)` for running KMP once per pattern.

## Trace It

The failure links handle *mismatches*; the **output links** handle *overlaps* — and they're the piece beginners drop, because the automaton still "works" without them, just wrongly.

**Predict before you run:** dictionary `{she, he}`, text `"she"`. After reading all three characters you're sitting at the `she` node and report `she`. But `he` is a suffix of `she`. Does the algorithm also report `he` — or do you have to ask for it?

```python run viz=array
from collections import deque
class AhoCorasick:
    def __init__(self, patterns, output_links=True):
        self.goto = [{}]; self.fail = [0]; self.out = [[]]
        for p in patterns:
            node = 0
            for c in p:
                if c not in self.goto[node]:
                    self.goto.append({}); self.fail.append(0); self.out.append([])
                    self.goto[node][c] = len(self.goto) - 1
                node = self.goto[node][c]
            self.out[node].append(p)
        q = deque(self.goto[0].values())
        while q:
            u = q.popleft()
            for c, v in self.goto[u].items():
                q.append(v)
                f = self.fail[u]
                while f and c not in self.goto[f]:
                    f = self.fail[f]
                self.fail[v] = self.goto[f].get(c, 0)
                if output_links:
                    self.out[v] += self.out[self.fail[v]]   # the line under test
    def search(self, text):
        node, hits = 0, []
        for i, c in enumerate(text):
            while node and c not in self.goto[node]:
                node = self.fail[node]
            node = self.goto[node].get(c, 0)
            for p in self.out[node]:
                hits.append((p, i - len(p) + 1))
        return sorted(hits)

print("with output links:", AhoCorasick(["she", "he"]).search("she"))
print("without (bug)    :", AhoCorasick(["she", "he"], output_links=False).search("she"))
```

<details>
<summary><strong>Reveal</strong></summary>

With output links you get both `[('he', 1), ('she', 0)]`; without them you get only `[('she', 0)]` — `he` is silently missed. Here's why: when the scan finishes `"she"`, the active node is the one for `she`, whose `out` list contains just `"she"`. But `"he"` *also* ends at that exact position — it's a suffix of `"she"`. The automaton knows this structurally: `fail[she-node]` points to the `he-node` (the longest proper suffix of `she` that's a pattern), so chaining `out[v] += out[fail[v]]` makes the `she` node's output set include `"he"` too. Drop that chaining and the failure *links* are still correct (mismatches fall back fine), but you only ever report the pattern whose node you land on exactly — every pattern that is a *suffix* of a longer match disappears. This is the subtle, classic Aho-Corasick bug: the algorithm finds prefixes via the trie and handles mismatches via failure links, but reporting all *overlapping* matches needs the output (dictionary-suffix) links. Without them you'd find `she` but miss the `he` hiding inside it.

</details>

## Your Turn

**Count all dictionary occurrences** in a text — the multi-pattern workload Aho-Corasick was built for. One pass tallies every pattern, overlaps included.

```python run viz=array
from collections import deque
class AhoCorasick:
    def __init__(self, patterns):
        self.goto = [{}]; self.fail = [0]; self.out = [[]]
        for p in patterns:
            node = 0
            for c in p:
                if c not in self.goto[node]:
                    self.goto.append({}); self.fail.append(0); self.out.append([])
                    self.goto[node][c] = len(self.goto) - 1
                node = self.goto[node][c]
            self.out[node].append(p)
        q = deque(self.goto[0].values())
        while q:
            u = q.popleft()
            for c, v in self.goto[u].items():
                q.append(v)
                f = self.fail[u]
                while f and c not in self.goto[f]:
                    f = self.fail[f]
                self.fail[v] = self.goto[f].get(c, 0)
                self.out[v] += self.out[self.fail[v]]
    def count(self, text):
        node, total = 0, 0
        for c in text:
            while node and c not in self.goto[node]:
                node = self.fail[node]
            node = self.goto[node].get(c, 0)
            total += len(self.out[node])
        return total

print(AhoCorasick(["is", "si", "ssi"]).count("mississippi"))   # 6
print(AhoCorasick(["ab", "bc", "ca"]).count("abcab"))          # 4
```

```java run viz=array
import java.util.*;
public class Main {
    static List<Map<Character,Integer>> go = new ArrayList<>();
    static List<Integer> fail = new ArrayList<>();
    static List<Integer> outCount = new ArrayList<>();
    static void build(String[] patterns) {
        go.clear(); fail.clear(); outCount.clear();
        go.add(new HashMap<>()); fail.add(0); outCount.add(0);
        for (String p : patterns) {
            int node = 0;
            for (char c : p.toCharArray()) {
                if (!go.get(node).containsKey(c)) {
                    go.add(new HashMap<>()); fail.add(0); outCount.add(0);
                    go.get(node).put(c, go.size() - 1);
                }
                node = go.get(node).get(c);
            }
            outCount.set(node, outCount.get(node) + 1);
        }
        Deque<Integer> q = new ArrayDeque<>(go.get(0).values());
        while (!q.isEmpty()) {
            int u = q.poll();
            for (var e : go.get(u).entrySet()) {
                char c = e.getKey(); int v = e.getValue(); q.add(v);
                int f = fail.get(u);
                while (f != 0 && !go.get(f).containsKey(c)) f = fail.get(f);
                fail.set(v, go.get(f).getOrDefault(c, 0));
                outCount.set(v, outCount.get(v) + outCount.get(fail.get(v)));   // chain output counts
            }
        }
    }
    static int count(String text) {
        int node = 0, total = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (node != 0 && !go.get(node).containsKey(c)) node = fail.get(node);
            node = go.get(node).getOrDefault(c, 0);
            total += outCount.get(node);
        }
        return total;
    }
    public static void main(String[] args) {
        build(new String[]{"is", "si", "ssi"});
        System.out.println(count("mississippi"));   // 6
        build(new String[]{"ab", "bc", "ca"});
        System.out.println(count("abcab"));          // 4
    }
}
```

Both print `6` then `4`. In `"mississippi"`, the overlapping patterns `is`, `si`, `ssi` total six occurrences; in `"abcab"`, `ab` (twice), `bc`, and `ca` total four. The single scan handles the heavy overlap (`ssi` contains `si`) without re-reading the text — which is exactly the payoff over `k` separate KMP runs.

## Reflect & Connect

- **KMP for many patterns.** Trie (shared prefixes) + failure links (KMP's mismatch fallback on the trie) = match a whole dictionary in one pass. The text pointer never rewinds, just like KMP.
- **Failure links are built by BFS.** `fail[v]` is shallower than `v`, so a breadth-first (increasing-depth) sweep always has it ready. It's the longest proper suffix of `v`'s string that's still a trie node.
- **Output links are non-optional for overlaps.** When patterns are suffixes of one another (`he` ⊂ `she`), reporting them all requires chaining `out[v] += out[fail[v]]`. The classic bug is omitting it and missing the shorter matches.
- **One pass beats k passes.** `O(n + Σ|pattern| + matches)` versus `O(k·n)` for per-pattern KMP. The win grows with the dictionary size — which is why `grep -F`, antivirus scanners, and network IDS use it.
- **Where it sits.** It's the multi-pattern capstone of Part 7: [naive](/cortex/data-structures-and-algorithms/strings-string-matching-naive) → [KMP](/cortex/data-structures-and-algorithms/strings-kmp)/[Z](/cortex/data-structures-and-algorithms/strings-z-algorithm) (one pattern) → [Rabin-Karp](/cortex/data-structures-and-algorithms/strings-rabin-karp-and-rolling-hash) (hashing, multi-pattern via a set) → Aho-Corasick (a trie automaton, all patterns in one deterministic pass).

## Recall

<details>
<summary><strong>Q:</strong> What three structures make up an Aho-Corasick automaton?</summary>

**A:** A trie of all patterns, failure links (each node → the node spelling its longest proper suffix still in the trie), and output links (chain the failure path to report every pattern ending at a node).

</details>
<details>
<summary><strong>Q:</strong> How are failure links related to KMP?</summary>

**A:** They're KMP's failure function generalised from one string to the whole trie: on a mismatch you fall back to the longest proper suffix that's still a trie node, so the text pointer never moves backward.

</details>
<details>
<summary><strong>Q:</strong> Why are failure links built with BFS?</summary>

**A:** `fail[v]` always points to a *shallower* node, so processing nodes in increasing depth (breadth-first) guarantees `fail[v]` is already computed when you reach `v`.

</details>
<details>
<summary><strong>Q:</strong> What do output links do, and what breaks without them?</summary>

**A:** They make a node report every pattern that is a *suffix* of the string spelled to it (via `out[v] += out[fail[v]]`). Without them, patterns that are suffixes of longer matches are missed — e.g. matching `"she"` would report `she` but not `he`.

</details>
<details>
<summary><strong>Q:</strong> What is Aho-Corasick's complexity, and why beat running KMP per pattern?</summary>

**A:** `O(n + Σ|pattern| + matches)` — the text is scanned once regardless of dictionary size. Running KMP once per pattern is `O(k·n)`, re-reading the text `k` times; Aho-Corasick shares one pass across all patterns.

</details>

## Sources & Verify

- **Aho & Corasick** (1975), "Efficient string matching: an aid to bibliographic search", *Comm. ACM* — the original trie-plus-failure-links automaton (and the basis of `fgrep`).
- **CP-Algorithms**, "Aho-Corasick algorithm" — the canonical BFS construction of failure and output links, with the overlap-handling subtlety.
- **LeetCode** 1032 (Stream of Characters) is the canonical Aho-Corasick drill; the `she@1, he@2, hers@2` matches, the with-vs-without output-links `[he, she]`/`[she]` on `"she"`, and the `6`/`4` occurrence counts above come from the runnable blocks — re-run to verify.
