---
title: Suffix Automaton
summary: "The minimal DFA accepting every substring of a fixed string — and the surprise is its size: linear (≤ 2n−1 states) despite O(n²) substrings, built online in O(n). States are end-position equivalence classes; substring queries become state traversals."
prereqs:
  - strings-suffix-array
  - graphs-introduction-to-graphs
---

## Why It Exists

The [suffix array](/cortex/data-structures-and-algorithms/strings/suffix-array) indexes a text by *sorting* its suffixes. The **suffix automaton** (SAM) indexes it as a *machine*: the smallest deterministic finite automaton whose paths from the start spell exactly the substrings of the string. Feed it a pattern and walk the transitions — if you never fall off, the pattern is a substring, in `O(m)`.

Two things make it remarkable. It's built **online** — one character at a time, `O(n)` total — so it can ingest a stream. And despite a length-`n` string having up to `O(n²)` distinct substrings, the automaton has at most `2n − 1` **states**: a startling collapse that comes from grouping substrings by *where they end*. Once built, it answers substring membership, counts distinct substrings, finds the longest common substring of two strings, and counts occurrences — all by walking states and suffix links. It's the automaton cousin of the suffix tree (a "DAWG"), and the most compact full-text index you can build incrementally.

## See It Work

Build the automaton character by character; test a substring by walking transitions. (The construction is intricate — read it as "append a char, splice suffix links, occasionally clone a state"; the next section unpacks why.)

```python run viz=array
class SuffixAutomaton:
    def __init__(self):
        self.trans = [{}]                                  # trans[state][char] -> state
        self.link = [-1]                                   # suffix link
        self.length = [0]                                  # longest substring length in this state's class
        self.last = 0
    def extend(self, c):
        cur = len(self.trans)
        self.trans.append({}); self.link.append(-1); self.length.append(self.length[self.last] + 1)
        p = self.last
        while p != -1 and c not in self.trans[p]:          # add the new transition along the suffix-link chain
            self.trans[p][c] = cur
            p = self.link[p]
        if p == -1:
            self.link[cur] = 0
        else:
            q = self.trans[p][c]
            if self.length[p] + 1 == self.length[q]:       # q is a contiguous extension -> link directly
                self.link[cur] = q
            else:                                          # otherwise CLONE q to split its class
                clone = len(self.trans)
                self.trans.append(dict(self.trans[q])); self.link.append(self.link[q]); self.length.append(self.length[p] + 1)
                while p != -1 and self.trans[p].get(c) == q:
                    self.trans[p][c] = clone
                    p = self.link[p]
                self.link[q] = clone
                self.link[cur] = clone
        self.last = cur
    def contains(self, pattern):
        s = 0
        for c in pattern:
            if c not in self.trans[s]:
                return False                               # fell off -> not a substring
            s = self.trans[s][c]
        return True

def build(s):
    sam = SuffixAutomaton()
    for c in s:
        sam.extend(c)
    return sam

text = input()
pattern = input()
sam = build(text)
print("true" if sam.contains(pattern) else "false")
```

```java run viz=array
import java.util.*;
public class Main {
    static List<Map<Character,Integer>> trans = new ArrayList<>();
    static List<Integer> link = new ArrayList<>(), length = new ArrayList<>();
    static int last;
    static void init() {
        trans.clear(); link.clear(); length.clear();
        trans.add(new HashMap<>()); link.add(-1); length.add(0); last = 0;
    }
    static void extend(char c) {
        int cur = trans.size();
        trans.add(new HashMap<>()); link.add(-1); length.add(length.get(last) + 1);
        int p = last;
        while (p != -1 && !trans.get(p).containsKey(c)) { trans.get(p).put(c, cur); p = link.get(p); }
        if (p == -1) link.set(cur, 0);
        else {
            int q = trans.get(p).get(c);
            if (length.get(p) + 1 == length.get(q)) link.set(cur, q);
            else {                                          // clone q
                int clone = trans.size();
                trans.add(new HashMap<>(trans.get(q))); link.add(link.get(q)); length.add(length.get(p) + 1);
                while (p != -1 && Objects.equals(trans.get(p).get(c), q)) { trans.get(p).put(c, clone); p = link.get(p); }
                link.set(q, clone); link.set(cur, clone);
            }
        }
        last = cur;
    }
    static void build(String s) { init(); for (char c : s.toCharArray()) extend(c); }
    static boolean contains(String pattern) {
        int s = 0;
        for (char c : pattern.toCharArray()) {
            if (!trans.get(s).containsKey(c)) return false;
            s = trans.get(s).get(c);
        }
        return true;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String text = sc.nextLine();
        String pattern = sc.nextLine();
        build(text);
        System.out.println(contains(pattern));
    }
}
```

```testcases
{
  "args": [
    { "id": "text", "label": "text", "type": "string", "placeholder": "abcbc" },
    { "id": "pattern", "label": "pattern", "type": "string", "placeholder": "bcb" }
  ],
  "cases": [
    { "args": { "text": "abcbc", "pattern": "bcb" }, "expected": "true" },
    { "args": { "text": "abcbc", "pattern": "abc" }, "expected": "true" },
    { "args": { "text": "abcbc", "pattern": "cba" }, "expected": "false" },
    { "args": { "text": "banana", "pattern": "ana" }, "expected": "true" },
    { "args": { "text": "banana", "pattern": "xyz" }, "expected": "false" }
  ]
}
```

Both print `true`, `true`, `false`. `"bcb"` and `"abc"` are substrings of `"abcbc"`; `"cba"` is not (the walk falls off). Membership is `O(m)` — just follow transitions.

## How It Works

The collapse from `O(n²)` substrings to `O(n)` states is the whole idea, and it hinges on **end-position sets**:

```d2
direction: right
endpos: "endpos(t) = the set of positions where substring t ENDS in s\ntwo substrings with the SAME endpos set share ONE state" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
state: "each STATE = one endpos-equivalence class\n(a contiguous range of substring lengths)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
link: "suffix LINK -> the state of the longest suffix\nin a DIFFERENT (larger) endpos class\n(the links form a tree over states)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
size: "result: <= 2n-1 states, <= 3n-4 transitions\nbuilt ONLINE in O(n) — clone a state on a transition conflict" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
endpos -> state
state -> link
link -> size
```

<p align="center"><strong>Substrings that end at exactly the same set of positions are indistinguishable for future matching, so they collapse into one state. That equivalence is why O(n²) substrings need only O(n) states; suffix links connect each class to the next-larger one, forming a tree.</strong></p>

Three ideas to hold (the mechanics are fiddly; the model is what matters):

- **States are end-position equivalence classes.** Two substrings are equivalent if they occur at exactly the same end positions in `s` — they behave identically for any future character, so one state represents both. Each state covers a *contiguous range* of lengths (`length[link[v]] + 1 .. length[v]`), and there are at most `2n − 1` of them. That collapse is the SAM's reason for being.
- **Suffix links form a tree.** `link[v]` points to the state holding the longest suffix of `v`'s strings that lives in a *different* (strictly larger) endpos class. Following links shortens the string and enlarges its occurrence set — the same "fall back to a shorter match" idea as [KMP](/cortex/data-structures-and-algorithms/strings/kmp)'s failure function, lifted to an automaton. The links alone form a tree (it's the suffix tree of the *reversed* string).
- **Construction is online with cloning.** `extend(c)` adds one character: it threads the new transition down the suffix-link chain, and when an existing transition `q` isn't a clean one-character extension, it **clones** `q` to split its endpos class correctly. Each `extend` is amortized `O(1)`, so the whole build is `O(n)` — and it works on a stream, no lookahead.

> **Key takeaway.** A suffix automaton is the minimal DFA accepting all substrings of `s`: states are **end-position equivalence classes** (≤ `2n − 1` of them, despite `O(n²)` substrings), suffix links form a tree of shrinking suffixes, and it's built **online in `O(n)`** by cloning a state on each transition conflict. Substring membership is an `O(m)` walk; distinct-substring counts and longest-common-substring fall out of the state/link structure.

## Trace It

The headline claim deserves a gut check, because it sounds impossible.

**Predict before you run:** the string `"abcde"` (all distinct characters) has `5·6/2 = 15` distinct substrings. How many *states* does its suffix automaton have — around 15 (one per substring), or far fewer?

```python run viz=array
class SuffixAutomaton:
    def __init__(self):
        self.trans = [{}]; self.link = [-1]; self.length = [0]; self.last = 0
    def extend(self, c):
        cur = len(self.trans)
        self.trans.append({}); self.link.append(-1); self.length.append(self.length[self.last] + 1)
        p = self.last
        while p != -1 and c not in self.trans[p]:
            self.trans[p][c] = cur; p = self.link[p]
        if p == -1:
            self.link[cur] = 0
        else:
            q = self.trans[p][c]
            if self.length[p] + 1 == self.length[q]:
                self.link[cur] = q
            else:
                clone = len(self.trans)
                self.trans.append(dict(self.trans[q])); self.link.append(self.link[q]); self.length.append(self.length[p] + 1)
                while p != -1 and self.trans[p].get(c) == q:
                    self.trans[p][c] = clone; p = self.link[p]
                self.link[q] = clone; self.link[cur] = clone
        self.last = cur

def build(s):
    sam = SuffixAutomaton()
    for c in s:
        sam.extend(c)
    return sam

def distinct_count(s):
    return len({s[i:j] for i in range(len(s)) for j in range(i + 1, len(s) + 1)})

for s in ("abcde", "aaaaa", "abcbc"):
    n = len(s)
    print(f"'{s}': states={len(build(s).trans)}  (bound 2n-1={2*n-1})   distinct substrings={distinct_count(s)}")
```

<details>
<summary><strong>Reveal</strong></summary>

`"abcde"` has **15 distinct substrings but only 6 states** — and the `2n − 1 = 9` bound holds for all three strings (`abcde` 6, `aaaaa` 6, `abcbc` 8). The collapse is the end-position equivalence: in `"abcde"`, each prefix length ends at exactly one position, and the substrings chain into a near-linear automaton; in `"aaaaa"`, every substring is a run of `a`s, so the classes again collapse to a handful. The substring *count* grows quadratically, but the *machine* that recognises all of them stays linear, because many substrings share an end-position fingerprint and therefore a state. This is exactly why the SAM is a practical index: you'd never store `O(n²)` substrings explicitly, but you can store an `O(n)` automaton that *recognises* them all — and query any of the `O(n²)` substrings in `O(m)`. The quadratic-to-linear collapse is the single fact to remember about suffix automata.

</details>

## Your Turn

**Count distinct substrings** — and watch the SAM agree with the suffix array. Each state `v` (except the root) contributes `length[v] − length[link[v]]` new substrings (the contiguous length-range its class owns), so the total distinct-substring count is the sum over states.

```python run viz=array
class SuffixAutomaton:
    def __init__(self):
        self.trans = [{}]; self.link = [-1]; self.length = [0]; self.last = 0
    def extend(self, c):
        cur = len(self.trans)
        self.trans.append({}); self.link.append(-1); self.length.append(self.length[self.last] + 1)
        p = self.last
        while p != -1 and c not in self.trans[p]:
            self.trans[p][c] = cur; p = self.link[p]
        if p == -1:
            self.link[cur] = 0
        else:
            q = self.trans[p][c]
            if self.length[p] + 1 == self.length[q]:
                self.link[cur] = q
            else:
                clone = len(self.trans)
                self.trans.append(dict(self.trans[q])); self.link.append(self.link[q]); self.length.append(self.length[p] + 1)
                while p != -1 and self.trans[p].get(c) == q:
                    self.trans[p][c] = clone; p = self.link[p]
                self.link[q] = clone; self.link[cur] = clone
        self.last = cur

def distinct_substrings(s):
    # Your code goes here
    return 0

s = input()
print(distinct_substrings(s))
```

```java run viz=array
import java.util.*;
public class Main {
    static List<Map<Character,Integer>> trans = new ArrayList<>();
    static List<Integer> link = new ArrayList<>(), length = new ArrayList<>();
    static int last;
    static void init() { trans.clear(); link.clear(); length.clear(); trans.add(new HashMap<>()); link.add(-1); length.add(0); last = 0; }
    static void extend(char c) {
        int cur = trans.size();
        trans.add(new HashMap<>()); link.add(-1); length.add(length.get(last) + 1);
        int p = last;
        while (p != -1 && !trans.get(p).containsKey(c)) { trans.get(p).put(c, cur); p = link.get(p); }
        if (p == -1) link.set(cur, 0);
        else {
            int q = trans.get(p).get(c);
            if (length.get(p) + 1 == length.get(q)) link.set(cur, q);
            else {
                int clone = trans.size();
                trans.add(new HashMap<>(trans.get(q))); link.add(link.get(q)); length.add(length.get(p) + 1);
                while (p != -1 && Objects.equals(trans.get(p).get(c), q)) { trans.get(p).put(c, clone); p = link.get(p); }
                link.set(q, clone); link.set(cur, clone);
            }
        }
        last = cur;
    }
    static long distinctSubstrings(String s) {
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
    { "args": { "s": "abcbc" }, "expected": "12" }
  ]
}
```

Both print `15` then `3` — the *same* numbers the [suffix array](/cortex/data-structures-and-algorithms/strings/suffix-array) lesson got from `n(n+1)/2 − Σ lcp`. Two completely different indexes (sorted suffixes vs. an automaton) compute the identical distinct-substring count, which is a satisfying cross-check that both are capturing the same underlying structure. Here the SAM's per-state length-range *directly* counts the new substrings each class introduces.

<details>
<summary><strong>Editorial</strong></summary>

Each non-root state in the suffix automaton represents an equivalence class that owns a contiguous range of substring lengths: `length[v] − length[link[v]]` new distinct substrings. Sum over all non-root states — no duplicates because each substring belongs to exactly one class.

```python solution time=O(n) space=O(n)
class SuffixAutomaton:
    def __init__(self):
        self.trans = [{}]; self.link = [-1]; self.length = [0]; self.last = 0
    def extend(self, c):
        cur = len(self.trans)
        self.trans.append({}); self.link.append(-1); self.length.append(self.length[self.last] + 1)
        p = self.last
        while p != -1 and c not in self.trans[p]:
            self.trans[p][c] = cur; p = self.link[p]
        if p == -1:
            self.link[cur] = 0
        else:
            q = self.trans[p][c]
            if self.length[p] + 1 == self.length[q]:
                self.link[cur] = q
            else:
                clone = len(self.trans)
                self.trans.append(dict(self.trans[q])); self.link.append(self.link[q]); self.length.append(self.length[p] + 1)
                while p != -1 and self.trans[p].get(c) == q:
                    self.trans[p][c] = clone; p = self.link[p]
                self.link[q] = clone; self.link[cur] = clone
        self.last = cur

def distinct_substrings(s):
    sam = SuffixAutomaton()
    for c in s:
        sam.extend(c)
    return sum(sam.length[v] - sam.length[sam.link[v]] for v in range(1, len(sam.trans)))

s = input()
print(distinct_substrings(s))
```

```java solution
import java.util.*;
public class Main {
    static List<Map<Character,Integer>> trans = new ArrayList<>();
    static List<Integer> link = new ArrayList<>(), length = new ArrayList<>();
    static int last;
    static void init() { trans.clear(); link.clear(); length.clear(); trans.add(new HashMap<>()); link.add(-1); length.add(0); last = 0; }
    static void extend(char c) {
        int cur = trans.size();
        trans.add(new HashMap<>()); link.add(-1); length.add(length.get(last) + 1);
        int p = last;
        while (p != -1 && !trans.get(p).containsKey(c)) { trans.get(p).put(c, cur); p = link.get(p); }
        if (p == -1) link.set(cur, 0);
        else {
            int q = trans.get(p).get(c);
            if (length.get(p) + 1 == length.get(q)) link.set(cur, q);
            else {
                int clone = trans.size();
                trans.add(new HashMap<>(trans.get(q))); link.add(link.get(q)); length.add(length.get(p) + 1);
                while (p != -1 && Objects.equals(trans.get(p).get(c), q)) { trans.get(p).put(c, clone); p = link.get(p); }
                link.set(q, clone); link.set(cur, clone);
            }
        }
        last = cur;
    }
    static long distinctSubstrings(String s) {
        init();
        for (char c : s.toCharArray()) extend(c);
        long total = 0;
        for (int v = 1; v < trans.size(); v++) total += length.get(v) - length.get(link.get(v));
        return total;
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

- **The quadratic-to-linear collapse is the whole point.** `O(n²)` substrings, `≤ 2n − 1` states, because substrings sharing an end-position set share a state. You store a *recogniser*, not the substrings.
- **Suffix links are KMP's idea, automated.** `link[v]` is the longest strictly-shorter suffix in a different class — "fall back on mismatch," generalised to a tree over states. The links alone are the suffix tree of the reversed string.
- **Online and `O(n)`.** `extend(c)` (with its occasional clone) runs amortized `O(1)`, so the SAM ingests a stream without lookahead — an edge over the suffix array's offline sort.
- **One index, many queries.** Substring membership `O(m)`, distinct-substring count (per-state length ranges), longest common substring of two strings (run the second string through the first's SAM), occurrence counting (endpos-set sizes via the link tree). The structure pays for itself across queries.
- **Where it sits.** [Suffix array](/cortex/data-structures-and-algorithms/strings/suffix-array) (offline, sorted, cache-friendly) and suffix automaton (online, automaton, streaming) are the two compact full-text indexes; both relate to the suffix tree. Choose the array for static bulk indexing, the SAM when you build incrementally or need automaton-style queries.

## Recall

<details>
<summary><strong>Q:</strong> What does a suffix automaton accept, and how big is it?</summary>

**A:** It's the minimal DFA accepting every substring of `s` (paths from the start spell substrings). It has at most `2n − 1` states and `3n − 4` transitions — linear — despite the string having `O(n²)` distinct substrings.

</details>
<details>
<summary><strong>Q:</strong> What is a SAM state, and why does that bound the size linearly?</summary>

**A:** A state is an *end-position equivalence class*: all substrings that occur at exactly the same set of end positions. Many substrings share a class (and a state), so the `O(n²)` substrings collapse into `O(n)` states.

</details>
<details>
<summary><strong>Q:</strong> What is a suffix link?</summary>

**A:** `link[v]` points to the state of the longest suffix of `v`'s strings that belongs to a *different*, larger end-position class. Following links shortens the string and grows its occurrence set; the links form a tree (the suffix tree of the reversed string).

</details>
<details>
<summary><strong>Q:</strong> Why does construction sometimes clone a state?</summary>

**A:** When appending a character finds an existing transition to a state `q` that is *not* a clean one-character extension (`length[p]+1 ≠ length[q]`), the algorithm clones `q` to split its end-position class so each state still represents a single contiguous length-range. Amortized, the whole build is `O(n)`.

</details>
<details>
<summary><strong>Q:</strong> How does the SAM count distinct substrings?</summary>

**A:** Each non-root state `v` owns a contiguous range of lengths and contributes `length[v] − length[link[v]]` new substrings; summing over states gives the distinct count — matching the suffix array's `n(n+1)/2 − Σ lcp`.

</details>

## Sources & Verify

- **Blumer et al.** (1985), "The smallest automaton recognizing the subwords of a text" — the DAWG / suffix automaton and its linear-size proof.
- **CP-Algorithms**, "Suffix Automaton" — the canonical online construction (with cloning) and applications (distinct substrings, LCS, occurrences).
- **Crochemore & Hancart**, "Automata for matching patterns" — the automaton view of string matching; **LeetCode** 1062/1044 (repeated/longest duplicate substring) are SAM-amenable drills. The `true`/`true`/`false` memberships, the `6`/`6`/`8` state counts vs `15`/`5`/`12` substrings, and the `15`/`3` distinct counts above come from the runnable blocks — re-run to verify.
