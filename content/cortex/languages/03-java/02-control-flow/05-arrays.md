---
title: Arrays
summary: An array is a fixed-size, contiguous block of same-typed slots, indexed 0..length-1; its size is set at creation and never changes, and an out-of-range index throws at run time, not compile time. Creation and defaults, indexing, iteration, multidimensional and jagged arrays, and why a fixed array is not a growable list — every behavior shown with verified output.
prereqs: []
---

# Arrays — A Fixed Block of Slots

So far each variable has held one value. An **array** holds *many* values of the **same type** in a single, fixed-size block — `scores[0]`, `scores[1]`, and so on — reached by an integer **index** counting from zero. Two properties define it and cause every array bug: its **size is chosen when you create it and never changes**, and indices run from `0` to `length - 1`, so stepping outside that range fails — at *run time*, because the compiler can't know the index in advance. Arrays are the raw, fast, fixed foundation; the growable collections of Tier 3 are built on top of this idea.

This uses the [loops](/cortex/languages/java/control-flow/loops) that walk an array and the [accumulation patterns](/cortex/languages/java/control-flow/loop-control-and-patterns) that summarize one. Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Creating arrays](#1-creating-arrays)
2. [Indexing: reading and writing slots](#2-indexing-reading-and-writing-slots)
3. [Iterating an array](#3-iterating-an-array)
4. [Multidimensional and jagged arrays](#4-multidimensional-and-jagged-arrays)
5. [Arrays are not growable lists](#5-arrays-are-not-growable-lists)
6. [Mental-model summary](#6-mental-model-summary)
7. [Gotcha checklist](#7-gotcha-checklist)

---

## 1. Creating arrays

There are two ways to make an array. `new int[n]` makes one with `n` slots, each set to a **default** (`0` for `int`). An array **literal** `{…}` makes one already filled. Either way, `.length` reports its size.

```java run
public class Main {
    public static void main(String[] args) {
        int[] scores = new int[3];   // three slots, each default 0
        scores[0] = 90;
        scores[1] = 85;
        int[] primes = {2, 3, 5, 7}; // literal: four slots, already filled
        System.out.println(scores.length);
        System.out.println(scores[2]);   // never assigned → still 0
        System.out.println(primes.length);
        System.out.println(primes[0]);
    }
}
```

**Output:**
```
3
0
4
2
```

```d2
direction: right

primes: "int[] primes   (length = 4)" {
  grid-columns: 4
  s0: "[0]\n2"
  s1: "[1]\n3"
  s2: "[2]\n5"
  s3: "[3]\n7"
}
```

**Analysis.** `new int[3]` reserved three `int` slots, all `0`; we set two of them, and `scores[2]` — never touched — printed its default `0`. `primes` came pre-filled by its literal. `scores.length` is `3`, `primes.length` is `4`. The diagram shows the shape: `primes` is one block of four numbered slots, side by side.

**Intuition.**
*Mechanism.* An array is a single block of memory sized for `length` elements of its type, laid out contiguously, with the slots numbered from `0`. `new int[n]` fills that block with the type's zero value (`0`, `0.0`, `false`, or `null` for objects); a literal fills it with what you wrote.

*Concrete bite.* The size is fixed at creation — `.length` is a property of the array, not a target you can change. (You will feel that limit directly in §5.) For now the point is that every slot exists from the start: an unassigned `int` slot is `0`, not "empty" or "undefined."

*Earned rule.* Choose the form by what you know: a literal when you have the values up front, `new T[n]` when you'll fill the slots later (and can rely on the `0`/`false`/`null` defaults until you do). The cost of the defaults is that a slot you *forgot* to fill is silently `0`, not an error — so a half-initialized array reads as a fully-`0` one.

---

## 2. Indexing: reading and writing slots

`array[i]` is the slot at index `i` — read it in an expression, or assign to it to change that slot. Indices start at `0`, so a length-3 array has valid indices `0`, `1`, `2`.

```java run
public class Main {
    public static void main(String[] args) {
        int[] a = {10, 20, 30};
        a[1] = 99;
        System.out.println(a[0]);
        System.out.println(a[1]);
        System.out.println(a[2]);
    }
}
```

**Output:**
```
10
99
30
```

**Analysis.** `a[1] = 99` overwrote the middle slot; `a[0]` and `a[2]` were untouched. Reading and writing both use the same `a[i]` notation — on the left of `=` it names the slot to change, elsewhere it yields the slot's value.

**Intuition.**
*Mechanism.* Indexing computes a position in the block: slot `i` lives at a fixed offset from the start. The JVM checks every index against `length` at run time and refuses any that is out of range — it cannot let you read or write memory outside the array.

*Concrete bite.* Index past the end (or below `0`) and it throws — at run time, because the index isn't known until then:

```java run
public class Main {
    public static void main(String[] args) {
        int[] a = {10, 20, 30};
        System.out.println(a[3]);
    }
}
```

**Output** *(a thrown exception):*
```
Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3
```

`a` has length `3`, so the valid indices are `0`, `1`, `2`; `a[3]` is one past the end. The compiler accepted it (the index could have come from anywhere), and the JVM caught it when the line ran. The message names both the offending index and the length.

*Earned rule.* Index from `0` to `length - 1`; the last element is `a[a.length - 1]`, never `a[a.length]`. The cost of this run-time checking is that an out-of-range index is found late — when that line executes, often with real data — so when the index comes from a calculation or input, bound-check it against `length` before using it.

---

## 3. Iterating an array

Two loops walk an array. The classic `for` gives you the **index** (so you can read `a[i]`, compare neighbours, or write back); the enhanced `for` gives you each **value** directly when the index is all you'd ignore.

```java run
public class Main {
    public static void main(String[] args) {
        int[] nums = {5, 10, 15};
        for (int i = 0; i < nums.length; i++) {
            System.out.println(i + " -> " + nums[i]);
        }
        for (int n : nums) {
            System.out.print(n + " ");
        }
        System.out.println();
    }
}
```

**Output:**
```
0 -> 5
1 -> 10
2 -> 15
5 10 15 
```

**Analysis.** The first loop used `i` from `0` to `nums.length - 1`, printing each index alongside `nums[i]`. The second visited the values `5, 10, 15` with no index at all. Note `i < nums.length` (not a hard-coded `3`) — tie the boundary to the array so it stays correct if the array's size changes.

**Intuition.**
*Mechanism.* The classic `for` recomputes `nums[i]` each pass from the index it controls; the enhanced `for` is the compiler walking `0..length-1` for you and handing over each value (a copy, as the [last chapter's for-each](/cortex/languages/java/control-flow/loops) showed). Same traversal, different access.

*Concrete bite.* The choice has consequences, not just style: only the indexed `for` can write back. `for (int n : nums) n = 0;` changes copies and leaves the array untouched (you saw this with for-each); to zero the array you need `for (int i = 0; i < nums.length; i++) nums[i] = 0;`.

*Earned rule.* Use the enhanced `for` when you only read each value, and the classic `for` when you need the index — to modify a slot, look at positions, or compare an element to its neighbour. The cost of reaching for the index when you don't need it is reintroducing the off-by-one risk the enhanced `for` removes; the cost of the enhanced `for` when you *do* need it is that you simply can't write back.

---

## 4. Multidimensional and jagged arrays

An array's elements can themselves be arrays. `int[][]` is "an array of `int` arrays" — a grid, addressed `grid[row][col]`. Iterating it is just nested loops.

```java run
public class Main {
    public static void main(String[] args) {
        int[][] grid = {
            {1, 2, 3},
            {4, 5, 6}
        };
        System.out.println(grid.length);      // number of rows
        System.out.println(grid[0].length);   // length of row 0
        System.out.println(grid[1][2]);       // row 1, column 2
        int total = 0;
        for (int[] row : grid) {
            for (int cell : row) {
                total += cell;
            }
        }
        System.out.println(total);
    }
}
```

**Output:**
```
2
3
6
21
```

**Analysis.** `grid.length` is the number of **rows** (`2`); each row is itself an array, so `grid[0].length` is that row's length (`3`), and `grid[1][2]` is the element at row 1, column 2 (`6`). The nested for-each walked every row, then every cell, summing to `21`.

**Intuition.**
*Mechanism.* A 2D array is really a one-dimensional array whose elements are references to other arrays. Nothing forces those inner arrays to be the same length — a "jagged" array, where rows differ in size, is perfectly legal.

*Concrete bite.* So the bounds of one row tell you nothing about another, and assuming a rectangle throws:

```java run
public class Main {
    public static void main(String[] args) {
        int[][] jagged = {
            {1, 2, 3, 4},
            {5}
        };
        System.out.println(jagged[0].length);
        System.out.println(jagged[1].length);
        System.out.println(jagged[1][1]);
    }
}
```

**Output** *(two lengths, then a thrown exception):*
```
4
1
Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
```

Row `0` has four elements but row `1` has only one, so `jagged[1][1]` is out of bounds for *that* row. Each row carries its own `length`; there is no single "column count" to trust.

*Earned rule.* Treat a 2D array as rows that each have their own `length` — iterate with `grid[i].length` per row, not one shared width — and reach for `new int[rows][cols]` when you truly want a rectangle. The cost of assuming uniformity is exactly this per-row `ArrayIndexOutOfBoundsException`, which shows up only when a short row is indexed past its end.

---

## 5. Arrays are not growable lists

The defining limit returns: an array's `length` is fixed forever at creation. You cannot append, insert, or remove — there is no `add`, no `length = …`. To "grow" one, you allocate a bigger array and copy. That rigidity is exactly what the Collections Framework replaces in Tier 3 with growable `List`s (Tutorial 17).

```java run
public class Main {
    public static void main(String[] args) {
        int[] a = {1, 2, 3};
        System.out.println(a.length());
    }
}
```

**Compiler error:**
```
Main.java:4: error: cannot find symbol
        System.out.println(a.length());
                            ^
  symbol:   method length()
  location: variable a of type int[]
1 error
```

**Analysis.** Reaching for `a.length()` — with parentheses, as if it were a method — fails: an array's `length` is a **field**, written `a.length` with no parentheses. (Contrast a `String`, where [`s.length()`](/cortex/languages/java/first-steps/strings-the-basics) *is* a method.) The slip is a compile error, so it's caught immediately.

**Intuition.**
*Mechanism.* `length` is a final field baked into the array object at creation; it is not a method and cannot be reassigned. There is no operation that changes an array's size, because the block was allocated for exactly that many slots.

*Concrete bite.* The compile error above is one face of it; the deeper one is design: code that needs to grow a sequence cannot use a bare array without re-allocating and copying every time. "Add a score to the list" has no array equivalent that isn't O(n) copying.

*Earned rule.* Use an array when the size is known and fixed and you want raw speed and contiguity; reach for an `ArrayList` (Tier 3) the moment the collection needs to grow or shrink. The cost of forcing a growable problem onto an array is repeated allocate-and-copy; the cost of the array's fixed size is paid once, in choosing it only when the size really is fixed.

---

## 6. Mental-model summary

| Principle | Consequence |
|---|---|
| An array is a fixed-size block of same-typed slots, indexed from `0` | Valid indices are `0..length-1`; the last is `a[a.length - 1]` |
| `new T[n]` fills slots with the type's default; a literal fills them | An unassigned slot is silently `0`/`false`/`null`, not "empty" |
| Index bounds are checked at run time, not compile time | An out-of-range index throws `ArrayIndexOutOfBoundsException` when it runs |
| A 2D array is an array of arrays; rows may differ in length (jagged) | Each row has its own `.length`; there is no shared column count |
| `length` is a fixed field, not a method, and never changes | `a.length` (no parens); arrays cannot grow — use a `List` (Tier 3) for that |

## 7. Gotcha checklist

- **`ArrayIndexOutOfBoundsException` →** an index hit `length` or beyond (or went negative); last valid index is `length - 1`; bound-check computed indices.
- **A slot reads as `0` you thought you set →** `new T[n]` defaults are `0`/`false`/`null`; you didn't assign that slot.
- **A for-each "edit" didn't stick →** the loop variable is a copy; use a classic `for` and `a[i] = …` to write back.
- **`cannot find symbol: method length()` on an array →** `length` is a field; write `a.length` without parentheses (only `String` uses `s.length()`).
- **A 2D index throws on some rows →** the array is jagged; use each row's own `grid[i].length`, don't assume a uniform width.
- **You need to add/remove elements →** an array can't grow; allocate-and-copy, or use an `ArrayList` (Tier 3).

---

*Predict, then check.* For `int[] a = new int[4];` then `a[0] = 5; a[3] = 9;`, predict `a.length`, `a[1]`, and what `a[4]` does. Next, predict the output of summing `{ {1,2}, {3,4,5} }` with nested for-each, and what `m[0][2]` would do. Finally, write a loop that reverses the *printing* of `{1, 2, 3, 4}` (last to first) — which loop shape do you need, and what is the starting index?

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
