---
title: "Pattern: Set-Bit Finder"
summary: "Two two's-complement identities: n & (n-1) clears the lowest set bit, n & -n isolates it. From these fall the power-of-2 test, Brian Kernighan's popcount, and set-bit iteration — all O(1) (or O(set bits))."
prereqs:
  - 08-bit-tricks/01-pattern-kth-bit/01-pattern
---

# Pattern: Set-Bit Finder

## Why It Exists

You often need the **lowest set bit** of a number — to isolate it, clear it, count how many bits are set, or iterate over the set bits one at a time. The obvious way loops over all 32 positions checking each, `O(bits)` even when only one bit is set.

Two two's-complement identities do the core moves in a single operation. Subtracting `1` flips the lowest set bit to `0` and turns every trailing `0` into `1`; so `n & (n - 1)` **clears the lowest set bit** (and leaves higher bits alone). Its dual, `n & -n` — where `-n` is `~n + 1` in two's complement — keeps *only* the lowest set bit, **isolating** it as a power of 2. Clear or isolate the lowest set bit in one instruction; everything else in this lesson is built from those two.

## See It Work

For `n = 12` (`0b1100`, lowest set bit is position 3): clear it, isolate it, and use the clear-identity to test "is `n` a power of 2?". Run it.

```python run viz=array
n = 12                              # 0b1100

print(n & (n - 1))                  # 8 — clears the lowest set bit (bit 3)
print(n & -n)                       # 4 — isolates the lowest set bit (a power of 2)
print(n > 0 and (n & (n - 1)) == 0) # False — 12 has two set bits, not a power of 2
print(8 > 0 and (8 & 7) == 0)       # True  — 8 = 0b1000 is a power of 2
```

## How It Works

The whole pattern rests on what `n - 1` does to the bits:

```d2
direction: right
flow: "n = 12 → n - 1 = 11" {
  grid-rows: 2
  grid-columns: 2
  grid-gap: 20
  n: |md
    n = 12
    `0000 1100`
    rightmost set bit is bit 3
  |
  n_minus_1: |md
    n − 1 = 11
    `0000 1011`
    bit 3 cleared; bits 1-2 flipped to 1
  |
}
```

<p align="center"><strong>subtracting 1 clears the lowest set bit and flips every trailing zero to 1; AND-ing <code>n</code> with <code>n-1</code> therefore cancels the lowest set bit and all the bits below it.</strong></p>

So the two identities, and what they unlock:

- **`n & (n - 1)` — clear the lowest set bit.** Used for the **power-of-2 test** (`n > 0 and n & (n-1) == 0`: a power of 2 has exactly one set bit, so clearing it gives `0`), and for **Brian Kernighan's popcount** — repeat `n = n & (n - 1)` and count iterations until `n == 0`, which loops once per *set* bit, not per bit position.
- **`n & -n` — isolate the lowest set bit.** Returns a power of 2 marking that bit, so its position is `(n & -n).bit_length()` (or `numberOfTrailingZeros + 1`). Used to find the lowest set bit's position and to partition values by their lowest bit.

Each identity is `O(1)`; Kernighan's popcount is `O(set bits)` — faster than the `O(32)` position scan when bits are sparse.

### Key Takeaway

`n & (n - 1)` clears the lowest set bit; `n & -n` isolates it. From these come the one-line power-of-2 test and Kernighan's `O(set bits)` popcount — the two most-reused identities in bit manipulation.

## Trace It

For `n = 7` (`0b0111`):

| expression | result | meaning |
|---|---|---|
| `n & (n - 1)` = `7 & 6` | `6` (`0b0110`) | cleared the lowest set bit (bit 1) |
| `n & -n` = `7 & -7` | `1` (`0b0001`) | isolated the lowest set bit |
| popcount via repeated clear | `3` | `7 → 6 → 4 → 0`: three iterations |

Before you read on: Kernighan's popcount loops `n = n & (n - 1)` until `n` is `0`. For `n = 7` that's 3 iterations; for `n = 8` (`0b1000`) it's just 1. Why does this loop run once per *set bit* rather than once per bit *position* — and when does that actually matter?

Because each iteration clears exactly *one* set bit (the lowest), so the loop body runs precisely as many times as there are set bits — `3` for `7`, `1` for `8`. A naive popcount checks all 32 positions regardless. The difference is huge for **sparse** integers: a 64-bit number with two bits set takes 2 iterations here versus 64 checks naively. That "work proportional to the answer, not the word size" is why Kernighan's trick is the textbook popcount — and a clean example of how the right identity changes the complexity, not just the constant.

## Your Turn

The reusable lowest-bit utilities:

```python run viz=array
def lowest_set_bit(n):       return n & -n            # isolate (power of 2)
def lowest_set_position(n):  return (n & -n).bit_length()   # 1-indexed position
def is_power_of_two(n):      return n > 0 and (n & (n - 1)) == 0
def popcount(n):
    c = 0
    while n:
        n &= n - 1           # strip the lowest set bit
        c += 1
    return c

print(lowest_set_bit(12), lowest_set_position(12), is_power_of_two(16), popcount(13))
# 4 3 True 3
```

```java run viz=array
public class Main {
  static int lowestSetBit(int n)      { return n & -n; }
  static int lowestSetPosition(int n) { return Integer.numberOfTrailingZeros(n & -n) + 1; }
  static boolean isPowerOfTwo(int n)  { return n > 0 && (n & (n - 1)) == 0; }
  static int popcount(int n) { int c = 0; while (n != 0) { n &= n - 1; c++; } return c; }

  public static void main(String[] args) {
    System.out.println(lowestSetBit(12) + " " + lowestSetPosition(12) + " "
                       + isPowerOfTwo(16) + " " + popcount(13));
    // 4 3 true 3
  }
}
```

Drill the family in **Practice** — [Only Set Bit](/cortex/data-structures-and-algorithms/bit-tricks-pattern-set-bit-finder-problems-only-set-bit) and [Rightmost Set Bit](/cortex/data-structures-and-algorithms/bit-tricks-pattern-set-bit-finder-problems-rightmost-set-bit).

## Reflect & Connect

These two identities are *primitives* you'll use without re-deriving:

- **The family** — isolate the lowest set bit (`n & -n`), clear it (`n & (n-1)`), test power-of-2, count set bits (Kernighan), iterate over set bits, and partition numbers by their lowest set bit.
- **Two's complement is the engine** — `-n == ~n + 1`, which is exactly why `n & -n` keeps only the lowest set bit. Understanding the negation makes the trick obvious rather than magic.
- **They compose upward** — `n & (n-1)` is the heart of popcount, which feeds parity and Hamming-distance problems; `n & -n` underlies Fenwick (binary-indexed) trees, where the isolated lowest bit *is* the index step. You'll meet both again as single steps in bigger algorithms.

**Prerequisites:** [Kth-Bit Operations](/cortex/data-structures-and-algorithms/bit-tricks-pattern-kth-bit-pattern).
**What's next:** rearrange the bits of a number wholesale — [Bit Restructuring](/cortex/data-structures-and-algorithms/bit-tricks-pattern-restructuring-pattern).

## Recall

> **Mnemonic:** *`n & (n-1)` clears the lowest set bit · `n & -n` isolates it. Power-of-2: `n>0 and n&(n-1)==0`. Kernighan popcount: loop the clear, `O(set bits)`.*

| | |
|---|---|
| Clear lowest set bit | `n & (n - 1)` |
| Isolate lowest set bit | `n & -n` (a power of 2) |
| Power-of-2 test | `n > 0 and (n & (n-1)) == 0` |
| Popcount (Kernighan) | loop `n &= n-1`, count — `O(set bits)` |
| Engine | `-n == ~n + 1` (two's complement) |

<details>
<summary><strong>Q:</strong> What do `n & (n-1)` and `n & -n` each do?</summary>

**A:** Clear the lowest set bit, and isolate it (as a power of 2), respectively.

</details>
<details>
<summary><strong>Q:</strong> How does the power-of-2 test work?</summary>

**A:** A power of 2 has exactly one set bit, so clearing it with `n & (n-1)` yields `0` (for `n > 0`).

</details>
<details>
<summary><strong>Q:</strong> Why is Kernighan's popcount `O(set bits)`?</summary>

**A:** Each iteration clears exactly one set bit, so the loop runs once per set bit, not once per position.

</details>
<details>
<summary><strong>Q:</strong> Why does `n & -n` isolate the lowest set bit?</summary>

**A:** `-n = ~n + 1` (two's complement) matches `n` only at the lowest set bit, so the AND keeps just that bit.

</details>

## Sources & Verify

- **Warren**, *Hacker's Delight*, 2nd ed., ch. 2 — `x & (x-1)`, `x & -x`, and population count.
- **CLRS**, *Introduction to Algorithms*, 4th ed. — Fenwick/binary-indexed trees use `n & -n`.
- The two identities, the power-of-2 test, and Kernighan's popcount are standard; both runnable blocks are verified by running (`12 ⇒ clear 8, isolate 4`; utilities ⇒ `4 3 True 3`; `popcount(7)=3`).
