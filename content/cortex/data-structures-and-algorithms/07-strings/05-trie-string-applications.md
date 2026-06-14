---
title: Trie String Applications
summary: "Where tries beat hash tables and BSTs — prefix-shared work. Autocomplete walks the prefix path and the subtree IS the answer; the is_end flag separates a stored word from a mere prefix. Powers longest-common-prefix, word break, and IP longest-prefix routing."
prereqs:
  - trees-trie-introduction-to-tries
---

## Why It Exists

The [trie chapter](/cortex/data-structures-and-algorithms/trees/trie/introduction-to-tries) built the structure: a tree where the path from the root to a node spells a string, so shared prefixes share nodes. This lesson is the *applications* tour — *where* that structure beats a hash set or a balanced BST.

The unifying win is **prefix-shared work**. A hash set answers "is this exact word present?" in `O(L)`, but it cannot answer "which words start with `ca`?" without scanning *every* key — the hashing that makes lookup fast deliberately destroys any ordering or prefix structure. A trie keeps that structure: walk the `ca` path in `O(2)`, and the subtree hanging below it *is* the set of completions. That single property drives autocomplete, longest-common-prefix, dictionary word-break, and IP routing's longest-prefix match.

## See It Work

Insert a dictionary, then **autocomplete** a prefix: walk to the prefix node, and DFS the subtree to collect every word beneath it.

```python run viz=array
class Trie:
    def __init__(self):
        self.children = {}
        self.is_end = False
    def insert(self, word):
        node = self
        for c in word:
            node = node.children.setdefault(c, Trie())
        node.is_end = True                            # mark a real word ends here
    def _walk(self, prefix):                          # descend the prefix path, or None
        node = self
        for c in prefix:
            if c not in node.children:
                return None
            node = node.children[c]
        return node
    def autocomplete(self, prefix):
        node = self._walk(prefix)
        out = []
        def dfs(n, path):
            if n.is_end:
                out.append(prefix + path)
            for c, child in n.children.items():
                dfs(child, path + c)
        if node:
            dfs(node, "")
        return sorted(out)

words = input().split(',')
prefix1 = input()
prefix2 = input()
t = Trie()
for w in words:
    t.insert(w)
xs = t.autocomplete(prefix1)
ys = t.autocomplete(prefix2)
print('[' + ', '.join(xs) + ']')
print('[' + ', '.join(ys) + ']')
```

```java run viz=array
import java.util.*;
public class Main {
    static class Trie {
        Map<Character, Trie> children = new TreeMap<>();
        boolean isEnd = false;
        void insert(String w) {
            Trie n = this;
            for (char c : w.toCharArray()) n = n.children.computeIfAbsent(c, x -> new Trie());
            n.isEnd = true;
        }
        Trie walk(String p) {
            Trie n = this;
            for (char c : p.toCharArray()) { n = n.children.get(c); if (n == null) return null; }
            return n;
        }
        List<String> autocomplete(String prefix) {
            List<String> out = new ArrayList<>();
            Trie n = walk(prefix);
            if (n != null) dfs(n, prefix, out);
            Collections.sort(out);
            return out;
        }
        void dfs(Trie n, String path, List<String> out) {
            if (n.isEnd) out.add(path);
            for (var e : n.children.entrySet()) dfs(e.getValue(), path + e.getKey(), out);
        }
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String[] words = sc.nextLine().split(",");
        String prefix1 = sc.nextLine();
        String prefix2 = sc.nextLine();
        Trie t = new Trie();
        for (String w : words) t.insert(w);
        System.out.println(t.autocomplete(prefix1));
        System.out.println(t.autocomplete(prefix2));
    }
}
```

```testcases
{
  "args": [
    { "id": "words", "label": "words (comma-separated)", "type": "string", "placeholder": "car,card,cat,dog" },
    { "id": "prefix1", "label": "prefix 1", "type": "string", "placeholder": "ca" },
    { "id": "prefix2", "label": "prefix 2", "type": "string", "placeholder": "do" }
  ],
  "cases": [
    { "args": { "words": "car,card,cat,dog", "prefix1": "ca", "prefix2": "do" }, "expected": "[car, card, cat]\n[dog]" },
    { "args": { "words": "apple,app,application,apt", "prefix1": "ap", "prefix2": "apt" }, "expected": "[app, apple, application, apt]\n[apt]" },
    { "args": { "words": "hello,help,helium,hero", "prefix1": "hel", "prefix2": "her" }, "expected": "[helium, hello, help]\n[hero]" },
    { "args": { "words": "abc,abd,xyz", "prefix1": "ab", "prefix2": "xy" }, "expected": "[abc, abd]\n[xyz]" }
  ]
}
```

Both print `[car, card, cat]` then `[dog]`. The walk to `ca` is `O(2)`; everything under that node is a completion. A hash set of the same words would have to examine all four keys to answer the same query.

## How It Works

Every trie string application is a variation on "walk the prefix, then work on the subtree":

```d2
direction: down
root: "root" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
c: "c" {style.fill: "#fde68a"; style.stroke: "#d97706"}
a: "a  (prefix 'ca' ends here)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
r: "r* (word 'car')" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
d: "d* (word 'card')" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
tt: "t* (word 'cat')" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
root -> c
c -> a
a -> r
r -> d
a -> tt
note: "* = is_end (a real word).\nWalk the prefix path in O(prefix);\nthe SUBTREE below is exactly the completions." {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
```

<p align="center"><strong>Autocomplete walks <code>c → a</code> (the prefix), then collects the subtree: <code>car</code>, <code>card</code>, <code>cat</code>. Shared prefixes share nodes, and the <code>is_end</code> marker (★) is what distinguishes a stored word from a node merely passed through.</strong></p>

Three things define the trie's edge:

- **Prefix queries are the killer feature.** "All words starting with `p`" = walk `p` (`O(|p|)`), then enumerate the subtree (`O(size of answer)`). A hash set must scan *every* key because hashing scatters related strings to unrelated buckets. This is why autocomplete, spell-checkers, and routing tables use tries.
- **`is_end` separates *word* from *prefix*.** A node existing on a path means some word *passes through* it, not that the path *is* a word. `is_end` marks where real words terminate. Confusing "the path exists" with "the word exists" is the classic trie bug — the [Trace It](#trace-it) shows it.
- **Shared prefixes save space and computation.** `{car, card, cat}` share the `ca` nodes — stored once, not three times — and a dictionary word-break DP can reuse the prefix walk across overlapping substrings. The cost is per-node overhead (a child map or fixed array), so tries pay memory for the structure that hashing throws away.

> **Key takeaway.** Tries win on **prefix-shared work**: walk the prefix in `O(|prefix|)`, and the subtree below is the answer — autocomplete, longest-common-prefix, word break, IP longest-prefix routing. The `is_end` flag distinguishes a stored word from a node merely on the path. Hash sets can't do prefix queries without scanning everything; tries pay per-node memory for that power.

## Trace It

The most common trie bug is treating "the path exists" as "the word is present." They are different, and `is_end` is the difference.

**Predict before you run:** you insert only the word `"card"` (not `"car"`). What does `search("car")` return — `True` (the path `c → a → r` clearly exists) or `False`?

```python run viz=array
class Trie:
    def __init__(self):
        self.children = {}
        self.is_end = False
    def insert(self, word):
        node = self
        for c in word:
            node = node.children.setdefault(c, Trie())
        node.is_end = True
    def _walk(self, s):
        node = self
        for c in s:
            if c not in node.children:
                return None
            node = node.children[c]
        return node
    def search(self, word):                           # exact WORD: path must exist AND end here
        node = self._walk(word)
        return node is not None and node.is_end
    def starts_with(self, prefix):                    # PREFIX: path just has to exist
        return self._walk(prefix) is not None

t = Trie()
t.insert("card")                                      # only 'card'
print("search('car')      :", t.search("car"))
print("starts_with('car') :", t.starts_with("car"))
print("search('card')     :", t.search("card"))
```

<details>
<summary><strong>Reveal</strong></summary>

`search("car")` is `False`, even though `starts_with("car")` is `True`. Inserting `"card"` creates the nodes `c → a → r → d`, so the path for `"car"` genuinely exists — but `is_end` was set only on the final `d` node, never on the `r` node. `search` checks *both* that the path exists *and* that `is_end` is set at the end; `starts_with` checks only that the path exists. So `"car"` is a valid *prefix* of the dictionary but not a stored *word*. Drop the `is_end` test from `search` (returning "the path exists") and your trie reports `"car"`, `"ca"`, and `"c"` as words too — a silent over-acceptance that breaks dictionaries, spell-checkers, and word-break. The flag is the entire distinction between "I passed through here" and "a word ends here," and forgetting it is the canonical trie mistake.

</details>

## Your Turn

**Longest Common Prefix** ([LeetCode 14](https://leetcode.com/problems/longest-common-prefix/)) via a trie — insert all the words, then walk down from the root *while there's exactly one child and no word has ended*. The moment the path branches (more than one child) or a word terminates, the common prefix stops.

```python run viz=array
class Trie:
    def __init__(self):
        self.children = {}
        self.is_end = False
    def insert(self, word):
        node = self
        for c in word:
            node = node.children.setdefault(c, Trie())
        node.is_end = True

def longest_common_prefix(words):
    # Your code goes here
    return ""

words = input().split(',')
print("'" + longest_common_prefix(words) + "'")
```

```java run viz=array
import java.util.*;
public class Main {
    static class Trie {
        Map<Character, Trie> children = new TreeMap<>();
        boolean isEnd = false;
        void insert(String w) {
            Trie n = this;
            for (char c : w.toCharArray()) n = n.children.computeIfAbsent(c, x -> new Trie());
            n.isEnd = true;
        }
    }
    static String longestCommonPrefix(String[] words) {
        // Your code goes here
        return "";
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String[] words = sc.nextLine().split(",");
        System.out.println("'" + longestCommonPrefix(words) + "'");
    }
}
```

```testcases
{
  "args": [
    { "id": "words", "label": "words (comma-separated)", "type": "string", "placeholder": "flower,flow,flight" }
  ],
  "cases": [
    { "args": { "words": "flower,flow,flight" }, "expected": "'fl'" },
    { "args": { "words": "dog,racecar,car" }, "expected": "''" },
    { "args": { "words": "interview,internal,interlude" }, "expected": "'inter'" },
    { "args": { "words": "abc,abcde,abcdef" }, "expected": "'abc'" },
    { "args": { "words": "apple,app,application" }, "expected": "'app'" }
  ]
}
```

The branch at `fl` (where `flower`/`flow` diverge from `flight`) stops the prefix; `flow` being a complete word would *also* stop it (the `is_end` check). For `["dog", "racecar", "car"]` the root branches immediately, so the common prefix is empty. Note how the *structure* of the trie directly encodes the answer — walking until the first branch or word-end is the whole algorithm.

<details>
<summary><strong>Editorial</strong></summary>

Insert all words into a trie, then walk down from the root while there is exactly one child and no word has ended at the current node. The first branch or `is_end` marker stops the common prefix. Both langs print the result wrapped in single quotes for exact matching.

```python solution time=O(total chars) space=O(total chars)
class Trie:
    def __init__(self):
        self.children = {}
        self.is_end = False
    def insert(self, word):
        node = self
        for c in word:
            node = node.children.setdefault(c, Trie())
        node.is_end = True

def longest_common_prefix(words):
    if not words:
        return ""
    t = Trie()
    for w in words:
        t.insert(w)
    node, prefix = t, []
    while len(node.children) == 1 and not node.is_end:   # one path forward, and no word ends here
        c = next(iter(node.children))
        prefix.append(c)
        node = node.children[c]
    return "".join(prefix)

words = input().split(',')
print("'" + longest_common_prefix(words) + "'")
```

```java solution
import java.util.*;
public class Main {
    static class Trie {
        Map<Character, Trie> children = new TreeMap<>();
        boolean isEnd = false;
        void insert(String w) {
            Trie n = this;
            for (char c : w.toCharArray()) n = n.children.computeIfAbsent(c, x -> new Trie());
            n.isEnd = true;
        }
    }
    static String longestCommonPrefix(String[] words) {
        if (words.length == 0) return "";
        Trie t = new Trie();
        for (String w : words) t.insert(w);
        StringBuilder sb = new StringBuilder(); Trie n = t;
        while (n.children.size() == 1 && !n.isEnd) {
            char c = n.children.keySet().iterator().next();
            sb.append(c); n = n.children.get(c);
        }
        return sb.toString();
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String[] words = sc.nextLine().split(",");
        System.out.println("'" + longestCommonPrefix(words) + "'");
    }
}
```

</details>

## Reflect & Connect

- **Prefix queries are why tries exist.** "Words starting with `p`" is a prefix walk plus a subtree enumeration — impossible for a hash set without a full scan. Autocomplete, spell-check, and routing all live here.
- **`is_end` = word vs prefix.** A node on a path means "a word passes through"; `is_end` means "a word ends here." Conflating them over-accepts prefixes as words — the canonical trie bug.
- **The structure is the answer.** Longest common prefix is "walk to the first branch or word-end"; autocomplete is "the subtree below the prefix." You read answers off the shape rather than computing them.
- **IP routing is longest-*prefix* match.** Routers store network prefixes in a (bitwise) trie and route each packet to the *deepest* matching prefix — the same walk, returning the last `is_end` seen. Tries are production infrastructure, not just an interview toy.
- **It generalises to multi-pattern matching.** Bolt *failure links* onto a trie (a KMP-style fallback between branches) and you get [Aho-Corasick](/cortex/data-structures-and-algorithms/strings/aho-corasick) — search a text for thousands of dictionary words in one pass. The trie is the skeleton; the failure links are the muscle.

## Recall

<details>
<summary><strong>Q:</strong> What can a trie do that a hash set fundamentally cannot?</summary>

**A:** Answer prefix queries — "all words starting with `p`" — in `O(|p| + answer size)`. Hashing scatters related strings to unrelated buckets, so a hash set must scan every key. Tries preserve prefix structure.

</details>
<details>
<summary><strong>Q:</strong> What does the <code>is_end</code> flag distinguish?</summary>

**A:** A stored *word* (a node where `is_end` is true) from a mere *prefix* (a node merely passed through). `search` requires the path to exist *and* `is_end`; `starts_with` requires only that the path exists.

</details>
<details>
<summary><strong>Q:</strong> After inserting only <code>"card"</code>, why is <code>search("car")</code> false but <code>starts_with("car")</code> true?</summary>

**A:** Inserting `"card"` creates the path `c → a → r → d` with `is_end` set only on `d`. So `"car"`'s path exists (prefix true) but its end node isn't marked as a word (search false).

</details>
<details>
<summary><strong>Q:</strong> How does a trie compute the longest common prefix of a word list?</summary>

**A:** Insert all words, then walk from the root while there's exactly one child *and* no word has ended (`is_end` false). The first branch or word-end stops the prefix.

</details>
<details>
<summary><strong>Q:</strong> What is the trie's cost trade-off versus a hash set?</summary>

**A:** Tries pay per-node overhead (a child map or fixed-size array per node) to preserve prefix structure; hash sets pay none but lose prefix/ordering ability. Tries win when prefix queries or shared-prefix computation dominate, or for small alphabets with many lookups.

</details>

## Sources & Verify

- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §5.2 (Tries) — string symbol tables, prefix operations, and the autocomplete/longest-prefix use cases.
- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed. — radix/prefix trees in the string-data-structure context.
- **LeetCode** 208 (Implement Trie), 14 (Longest Common Prefix), 1268 (Search Suggestions System), 139 (Word Break) are the canonical drills; the `['car','card','cat']` autocomplete, the `search`/`starts_with` `False`/`True` split, and the `'fl'`/`''` longest-common-prefixes above come from the runnable blocks — re-run to verify.
