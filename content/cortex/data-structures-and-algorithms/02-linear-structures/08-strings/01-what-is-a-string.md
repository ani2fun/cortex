---
title: Strings
summary: A string is just an array of characters, and each character is just a number. Everything you know about arrays carries over — with one twist that trips up everyone: most strings are immutable, so "editing" one secretly copies it.
tier: spine
prereqs:
  - linear-structures-arrays-what-is-an-array
  - foundations-measuring-cost
---

# Strings

## Why It Exists

Names, messages, file paths, DNA, the very code you're reading — all of it is **text**, and a program needs a way to hold a run of characters and work with it: index into it, measure it, compare it, glue pieces together.

You already have a structure that holds a run of same-typed things in order, reachable by index: the array. So here's the reassuring secret of this whole topic — a **string** is nothing more than *an array of characters*. Almost everything you learned about arrays applies unchanged. There's just one twist, and it's the one that quietly breaks beginners' code, so we'll meet it head-on.

## See It Work

A string is laid out exactly like an array — one character per slot, indexed from `0`. Run it, then click **Visualise**.

> ▶ Run it, then click **Visualise** — the characters sit in a row of cells; `s[i]` jumps straight to one, just like an array.

```python run viz=array viz-root=s
s = input()                  # the test case's string
for i in range(len(s)):
    print(i, s[i])           # index any character in O(1) — exactly like an array
```

```java run viz=array viz-root=s
import java.util.*;

public class Main {
  public static void main(String[] args) {
    String s = new Scanner(System.in).nextLine();   // the test case's string
    for (int i = 0; i < s.length(); i++)
      System.out.println(i + " " + s.charAt(i));   // O(1) per index — just like an array
  }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "HELLO" }
  ],
  "cases": [
    { "args": { "s": "HELLO" }, "expected": "0 H\n1 E\n2 L\n3 L\n4 O" },
    { "args": { "s": "HI" },    "expected": "0 H\n1 I" },
    { "args": { "s": "data" },  "expected": "0 d\n1 a\n2 t\n3 a" },
    { "args": { "s": "abc" },   "expected": "0 a\n1 b\n2 c" }
  ]
}
```

## How It Works

A string is a sequence of **characters** stored in contiguous memory — an array, with one detail filled in: each character is really *a number*. The letter `'A'` is stored as `65`, `'B'` as `66`, and so on — its **code point** in a character set (ASCII for the basics, Unicode for the rest of the world's scripts and emoji).

Because it's an array underneath, the array's costs carry straight over: reading `s[i]` is `O(1)` (address arithmetic on the code points), and scanning the whole string is `O(n)`.

```d2
s: a string is an array of character codes {
  grid-rows: 2
  grid-columns: 5
  grid-gap: 0
  c0: "H"
  c1: "E"
  c2: "L"
  c3: "L"
  c4: "O"
  a0: "72"
  a1: "69"
  a2: "76"
  a3: "76"
  a4: "79"
}
```

<p align="center"><strong>each character is really a number (its code point); a string is those numbers packed in a row, indexed like any array.</strong></p>

Now the twist. In most languages — Python, Java, JavaScript, C# — strings are **immutable**: once created, you *cannot* change a character in place. `s[0] = 'h'` isn't allowed. "Editing" a string actually builds a **brand-new** one and copies the characters over, which is `O(n)`.

That sounds harmless until you build a string in a loop. Each `result += c` copies the entire string-so-far, so growing a string one character at a time is secretly `O(n²)` — the classic beginner performance trap. The fix is to collect the pieces in a *list* (mutable) and join them once at the end, or use a `StringBuilder`: `O(n)` total.

### Key Takeaway

A string is an array of character codes — `O(1)` indexing, `O(n)` scan — but it's *immutable*, so build strings by collecting parts and joining once, never by `+=` in a loop.

## Trace It

Watch the immutability bite. You want to upper-case `"hi"` by editing in place:

| Attempt | What happens |
|---|---|
| `s = "hi"; s[0] = "H"` | **Error** — strings don't support item assignment |
| `s = "H" + s[1:]` | Works, but *builds a new string* `"Hi"` (copies both characters) |

Before you read on: if you upper-cased a 1-million-character string one character at a time with `s = s[:i] + c + s[i+1:]`, roughly how much copying happens in total?

About a *trillion* character-copies — each of the million edits rebuilds the whole million-character string (`n` edits × `n` copies = `O(n²)`). That's why the rule is iron: never grow or rewrite a string in a loop with `+`. Build a `list`, then `"".join(...)` it once.

## Your Turn

The right way to build a string — collect the pieces, join once:

```python run viz=array
s = input()
words = s.split()
parts = [w.capitalize() for w in words]
print(" ".join(parts))
```

```java run viz=array
import java.util.*;

public class Main {
  public static void main(String[] args) {
    String s = new Scanner(System.in).nextLine();
    String[] words = s.split(" ");
    StringBuilder sb = new StringBuilder();
    for (String w : words) {
      if (sb.length() > 0) sb.append(" ");
      sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
    }
    System.out.println(sb);
  }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "data structures rock" }
  ],
  "cases": [
    { "args": { "s": "data structures rock" }, "expected": "Data Structures Rock" },
    { "args": { "s": "hello world" },          "expected": "Hello World" },
    { "args": { "s": "a b c" },                "expected": "A B C" },
    { "args": { "s": "one two three four" },   "expected": "One Two Three Four" }
  ]
}
```

<details>
<summary>Editorial</summary>

Collect capitalized words into a list (Python) or `StringBuilder` (Java) and join/build once — `O(n)`. The starter stub already does this; the key insight is that `+=` in a loop copies the growing string each time, turning `O(n)` work into `O(n²)`. The `split` + `capitalize` + `join` idiom avoids that entirely. Both languages agree because the output is a plain string with no ordering ambiguity.

```python solution time=O(n) space=O(n)
s = input()
words = s.split()
parts = [w.capitalize() for w in words]
print(" ".join(parts))
```

```java solution
import java.util.*;

public class Main {
  public static void main(String[] args) {
    String s = new Scanner(System.in).nextLine();
    String[] words = s.split(" ");
    StringBuilder sb = new StringBuilder();
    for (String w : words) {
      if (sb.length() > 0) sb.append(" ");
      sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
    }
    System.out.println(sb);
  }
}
```

</details>

Want the two collision strategies in full? [Separate Chaining](/cortex/data-structures-and-algorithms/linear-structures/hash-table/separate-chaining) builds the linked-list-per-bucket version from scratch; the probing lessons cover open addressing.

## Reflect & Connect

Strings are where the array's lessons pay off on the most common data there is. Two things to carry:

- **The immutability/`join` rule** is one of the most frequent real performance bugs — a log builder, a CSV writer, a template renderer that does `+=` in a loop. Reach for a list-and-join or a `StringBuilder` by reflex.
- **A "character" isn't always one byte.** ASCII fits in one byte, but Unicode characters (UTF-8) take one to four. So `len` in bytes and `len` in characters can differ, and slicing mid-character corrupts text. For interview-level work, treat a string as an array of characters; just know the byte/char distinction exists when you handle real-world text.

Because a string *is* an array, the array scan patterns apply directly — the [Two Pointers](/cortex/data-structures-and-algorithms/linear-structures/arrays/pattern-two-pointers/pattern) problems (palindrome, reverse) are string problems. And a whole field of *string algorithms* — substring search, matching — builds on this foundation later in the book.

**Prerequisites:** [Arrays](/cortex/data-structures-and-algorithms/linear-structures/arrays/what-is-an-array) and [Measuring Cost](/cortex/data-structures-and-algorithms/foundations/measuring-cost).
**What's next:** the last linear-structures foundation — working directly in binary with [Bit Manipulation](/cortex/data-structures-and-algorithms/bit-tricks/bit-manipulation).

## Recall

> **Mnemonic:** *A string is an array of character codes. Indexing `O(1)`, scanning `O(n)` — but it's immutable, so collect-and-join, never `+=` in a loop.*

| Operation | Cost | Why |
|---|---|---|
| index `s[i]` / length | `O(1)` | contiguous array of fixed-size codes |
| scan / search | `O(n)` | visit each character |
| build with `+=` in a loop | `O(n²)` ✗ | immutability copies the whole string each time |
| build with list + `join` / `StringBuilder` | `O(n)` ✓ | one copy at the end |

<details>
<summary><strong>Q:</strong> What is a string, under the hood?</summary>

**A:** An array of characters, each stored as a number (its code point).

</details>
<details>
<summary><strong>Q:</strong> Why is `s[i]` `O(1)`?</summary>

**A:** It's array indexing — address arithmetic over fixed-size character codes.

</details>
<details>
<summary><strong>Q:</strong> What does "immutable" mean for a string, and what's the consequence?</summary>

**A:** You can't change a character in place; "editing" copies the whole string (`O(n)`), so `+=` in a loop is `O(n²)`.

</details>
<details>
<summary><strong>Q:</strong> How do you build a string efficiently?</summary>

**A:** Collect parts in a list and `join` once (or use a `StringBuilder`) — `O(n)`.

</details>

## Sources & Verify

- **Sedgewick & Wayne**, *Algorithms*, 4th ed., **Ch. 5 — Strings**: strings as character arrays, immutability, and the cost model for string operations.
- **Joel Spolsky**, *The Absolute Minimum Every Software Developer Must Know About Unicode* — why a character is not a byte; the ASCII → Unicode/UTF-8 story.
- **CPython** `Objects/unicodeobject.c` and **OpenJDK** `String.java` — both implement immutable strings; `StringBuilder` / list-join is the sanctioned mutable builder. Verify the immutability and `O(n²)`-concatenation claims against any language reference.
- Both runnable blocks are verified by running; the `O(1)`-index / `O(n²)`-loop-concatenation costs follow from the array layout plus immutability.
