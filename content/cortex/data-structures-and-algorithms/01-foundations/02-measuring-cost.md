---
title: Measuring Cost
summary: "Big-O isn't scary math — it's how the amount of work grows as the input grows. Count operations, not seconds, and you can compare any two solutions without a stopwatch."
tier: spine
prereqs:
  - foundations-how-a-computer-stores-data
---

# Measuring Cost

## Why It Exists

You've written two solutions to a problem. Which one is *better*?

Your first instinct is to time them. But the stopwatch lies: a faster laptop, a smaller test input, or a busy CPU all change the number. Time them on a tiny example and the slow one might even win. You need a way to compare two solutions that's about the *method itself* — not your machine, not today's mood of the operating system.

Here's the moment that makes it real: a program that finishes instantly on 100 items can *freeze* on 100,000. Same code, same laptop. What changed is how its workload **grows**. Measuring that growth is the whole game — and it's simpler than it sounds.

## See It Work

Don't count seconds — count *operations*, and watch how that count grows as the input gets bigger. Run this:

```python run viz=array
def linear(n):           # one pass over the input
    ops = 0
    for i in range(n):
        ops += 1
    return ops

def quadratic(n):        # touch every pair
    ops = 0
    for i in range(n):
        for j in range(n):
            ops += 1
    return ops

for n in [10, 100, 1000]:
    print(f"n={n:>4}   linear={linear(n):>8}   quadratic={quadratic(n):>10}")
```

Look at the columns as `n` grows 10× each row. `linear` grows 10× with it. `quadratic` grows **100×**. That gap is everything.

## How It Works

Cost is **how the amount of work grows as the input grows** — not how many seconds it takes.

We describe that growth with a few named shapes. The names look like math, but each is just a sentence:

- **`O(1)`** — *constant*: the work is the same no matter how big the input is (jumping to an array index).
- **`O(n)`** — *linear*: the work grows in step with the input (one scan through `n` items).
- **`O(n²)`** — *quadratic*: the work grows with the square (compare every item with every other item).
- **`O(log n)`** — *logarithmic*: the work grows by *one more step* each time the input **doubles** (each step throws away half of what's left).

To make this concrete: searching `n` items one by one is `O(n)` — double the items, double the work. Halving the search space each step is `O(log n)` — double the items, and you pay only *one* extra step. (You'll watch that happen when we reach binary search.)

Two rules let you read any algorithm's shape: keep only the fastest-growing term, and drop constant factors. So `3n + 5` is `O(n)`, and `n² + n` is `O(n²)` — because once `n` is large, the `n²` swamps everything else.

One more thing, and it's not optional: every algorithm has **two** costs — **time** (operations) and **space** (extra memory). Always state both.

So the core insight is: **Big-O measures how a solution *scales*, not how fast it runs on your laptop.**

### Key Takeaway

Count how the work grows with the input, keep only the dominant term, and report both time and space.

## Trace It

Take the question: *does this list contain a duplicate?* The brute force compares every pair.

- `n = 4` → pairs are `(0,1),(0,2),(0,3),(1,2),(1,3),(2,3)` = **6** comparisons.
- `n = 8` → **28** comparisons.

Before you read on: `n` doubled from 4 to 8. Did the work double?

No — it roughly *quadrupled* (6 → 28). The number of pairs grows like `n²/2`, and we drop the constant: **`O(n²)` time**. It uses no extra memory, so **`O(1)` space**. Feeling that "double the input, quadruple the work" in your gut is exactly the intuition Big-O captures.

## Your Turn

Run the counter again, but this time *predict each row before you read it*. If `n` jumps from 1,000 to 2,000, what happens to the `quadratic` column?

```python run viz=array
def quadratic(n):
    ops = 0
    for i in range(n):
        for j in range(n):
            ops += 1
    return ops

for n in [1000, 2000, 4000]:
    print(f"n={n:>5}   quadratic ops={quadratic(n):>12}")
```

```java run viz=array
public class Main {
  static long quadratic(int n) {
    long ops = 0;
    for (int i = 0; i < n; i++)
      for (int j = 0; j < n; j++)
        ops++;
    return ops;
  }
  public static void main(String[] args) {
    for (int n : new int[]{1000, 2000, 4000})
      System.out.printf("n=%5d   quadratic ops=%12d%n", n, quadratic(n));
  }
}
```

Each time `n` doubles, the count goes up 4×. That is what `O(n²)` *feels* like.

## Reflect & Connect

Big-O is the yardstick for the rest of the book. Every data structure is sold on its costs ("insert is `O(1)`, search is `O(n)`"), and every pattern you'll learn is a trick to drop an algorithm from a worse shape to a better one — usually `O(n²)` down to `O(n)` or `O(n log n)`.

**Prerequisites:** [How a Computer Stores Data](/cortex/data-structures-and-algorithms/foundations/how-a-computer-stores-data) — `O(1)` index access is where this starts.
**What's next:** a repeatable way to attack any problem, using this yardstick to measure "better" — [How to Approach a Problem](/cortex/data-structures-and-algorithms/foundations/how-to-approach-a-problem).

> A note on rigor: strictly, Big-O is an *upper* bound — here we use it the way working programmers do, to mean "grows like." For the precise definitions (Θ, Ω, the limit definitions, and proofs), see the deeper reference lesson [Asymptotic Analysis](/cortex/data-structures-and-algorithms/foundations/asymptotic-analysis).

## Recall

> **Mnemonic:** *Count how the work grows, keep the biggest term, drop the constants.*

| Shape | Means | Example |
|---|---|---|
| `O(1)` | constant — size doesn't matter | array index |
| `O(log n)` | one more step per doubling | halving a search |
| `O(n)` | grows with the input | one scan |
| `O(n²)` | grows with the square | every pair |

<details>
<summary><strong>Q:</strong> Why count operations instead of timing with a stopwatch?</summary>

**A:** Time depends on the machine and input; operation growth describes the method itself.

</details>
<details>
<summary><strong>Q:</strong> What is `3n + 5` in Big-O, and why?</summary>

**A:** `O(n)` — keep the dominant term and drop constants.

</details>
<details>
<summary><strong>Q:</strong> `n` doubles and the work quadruples — what shape is that?</summary>

**A:** `O(n²)`.

</details>
<details>
<summary><strong>Q:</strong> What two costs must every algorithm report?</summary>

**A:** Time and space.

</details>

## Sources & Verify

- **CLRS** 4th ed., Ch. 3 — asymptotic notation (`O`, `Ω`, `Θ`), the formal upper-bound definition, and the drop-constants / keep-dominant-term rules.
- **Sedgewick & Wayne**, *Algorithms* 4th ed. §1.4 — growth-rate analysis (they use `~` notation for "grows like").
- **Skiena**, *The Algorithm Design Manual* §2 — the Big-Oh notation, intuitively.
- Every number here is checkable by running the counters: `C(4,2)=6`, `C(8,2)=28`, and `quadratic` rising 100→10,000→1,000,000.
