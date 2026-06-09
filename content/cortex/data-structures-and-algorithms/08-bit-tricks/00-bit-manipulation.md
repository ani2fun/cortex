---
title: Bit Manipulation
summary: Drop below "integer" and "boolean" to the bits themselves. A handful of operators — AND, OR, XOR, shift — let you test, set, and flip individual bits in a single instruction, and pack 64 flags into one machine word.
tier: spine
prereqs:
  - foundations-how-a-computer-stores-data
  - foundations-measuring-cost
---

# Bit Manipulation

## Why It Exists

You already know that every value is, underneath, a row of **bits** — a number is just a sum of powers of two. Most of the time the language hides that from you. But sometimes the bits *are* the point: a file's permissions (`rwx`), a set of feature flags, the cells of a tiny board, the presence of each letter in a word. Storing each of those as a separate boolean wastes space and time.

What if a single 64-bit integer could hold **64 yes/no flags at once**, and you could test or flip any one of them in *one* CPU instruction? That's bit manipulation: working directly on the bits instead of the number they spell out. It's the most compact, and often the fastest, tool in the box — and it's the foundation for bitmask techniques you'll meet later.

## See It Work

Think of an integer as a row of switches, each on (`1`) or off (`0`). Run this — flip a few — and click **Visualise**.

> ▶ Run it, then click **Visualise** — each cell is one bit; we turn bit 2 and bit 5 on, then bit 2 back off.

```python run viz=array viz-root=bits viz-kind=bitset
bits = [0] * 8             # eight bits, all off (position 0 first)
bits[2] = 1                # turn bit 2 on
bits[5] = 1                # turn bit 5 on
bits[2] = 0                # turn bit 2 back off
print(bits)                # [0, 0, 0, 0, 0, 1, 0, 0] — only bit 5 left on
```

## How It Works

A bit at position `i` carries the value `2^i`. To touch one specific bit you build a **mask** — a number with *only* that bit set — using a left shift: `1 << i`. (Bit `0` is the lowest; written out, binary puts it on the *right*, though we lay the cells out left-to-right above.) Then a bitwise operator does the work, on the whole word at once:

| Goal on bit `i` | Operator | Why it works |
|---|---|---|
| **test** | `(x >> i) & 1` | slide bit `i` down to the ones place, mask off the rest |
| **set** (force to 1) | `x \| (1 << i)` | `OR` with the mask turns that bit on, leaves others alone |
| **clear** (force to 0) | `x & ~(1 << i)` | `AND` with the *inverted* mask turns only that bit off |
| **toggle** (flip) | `x ^ (1 << i)` | `XOR` with the mask flips that bit, leaves others alone |

```d2
byte: a byte — bit i has value 2^i {
  grid-rows: 2
  grid-columns: 8
  grid-gap: 0
  b0: "0"
  b1: "0"
  b2: "1"
  b3: "0"
  b4: "0"
  b5: "1"
  b6: "0"
  b7: "0"
  p0: "bit 0"
  p1: "bit 1"
  p2: "bit 2"
  p3: "bit 3"
  p4: "bit 4"
  p5: "bit 5"
  p6: "bit 6"
  p7: "bit 7"
}
```

<p align="center"><strong>eight bits, position 0 first; bit <code>i</code> carries value <code>2^i</code>. Here bits 2 and 5 are on (value 36). The mask <code>1 << i</code> has only bit <code>i</code> set — <code>AND</code>/<code>OR</code>/<code>XOR</code> it against <code>x</code> to test, set, clear, or flip.</strong></p>

Two tricks are worth memorizing because they appear constantly. **`x & -x`** isolates the lowest set bit (it evaporates everything above it) — the workhorse behind Fenwick trees. And **XOR is its own inverse**: `a ^ a = 0` and `a ^ 0 = a`, so XOR-ing a whole array cancels every value that appears twice and leaves the one that appears once.

Why bother? Each of these is a **single machine instruction operating on all 64 bits simultaneously**. Treating an integer as a set of 64 flags turns "is element `k` present?" into one `AND` — no array, no loop.

### Key Takeaway

Build a one-bit mask with `1 << i`, then `OR` to set, `AND ~` to clear, `XOR` to flip, and `>> & 1` to test — each a single instruction over the whole word, so an integer doubles as a 64-element set.

## Trace It

Start with `x = 0b1010` (decimal 10 — bits 1 and 3 on). Set bit 2:

```
x          = 1010
1 << 2     = 0100        (mask: only bit 2)
x | mask   = 1110        (bit 2 is now on; bits 1 and 3 untouched)
```

Before you read on: from that result `1110`, what does `x & ~(1 << 1)` give?

`~(1 << 1)` is `...11111101` — every bit set *except* bit 1. `AND`-ing keeps every bit of `1110` as-is except bit 1, which is forced to `0`: the result is `1100`. The inverted mask is a stencil that punches out exactly one bit and protects all the others — that's the whole idea behind clearing.

## Your Turn

The four moves, on a value with bits 2 and 5 already set (`0b00100100` = 36):

```python run viz=array
def show(x): return f"{x:08b}"

x = 0b00100100               # bits 2 and 5 set
print(show(x | (1 << 0)))    # SET bit 0    → 00100101
print(show(x & ~(1 << 2)))   # CLEAR bit 2  → 00100000
print(show(x ^ (1 << 5)))    # TOGGLE bit 5 → 00000100
print((x >> 2) & 1)          # TEST bit 2   → 1
```

```java run viz=array
public class Main {
  static String show(int x) { return String.format("%8s", Integer.toBinaryString(x)).replace(' ', '0'); }
  public static void main(String[] args) {
    int x = 0b00100100;                       // bits 2 and 5 set
    System.out.println(show(x | (1 << 0)));   // SET bit 0    → 00100101
    System.out.println(show(x & ~(1 << 2)));  // CLEAR bit 2  → 00100000
    System.out.println(show(x ^ (1 << 5)));   // TOGGLE bit 5 → 00000100
    System.out.println((x >> 2) & 1);         // TEST bit 2   → 1
  }
}
```

## Reflect & Connect

Bits show up wherever space or speed is tight, or a set is small and fixed:

- **Flags and permissions** — Unix file modes (`rwx` = three bits), feature toggles, and option sets pack many booleans into one integer.
- **Bitsets** — a compact set of small integers; "is `k` in the set?" is one `AND`, union is `OR`, intersection is `AND`.
- **XOR's self-inverse** powers find-the-unique-element, in-place swap (`a ^= b; b ^= a; a ^= b`), checksums, and parity.

This is foundational technique, not a structure — and two later topics lean on it directly: **bitmask dynamic programming** (where a subset *is* an integer) and the **linear basis** (XOR spans). The bit *patterns* in this section drill the recognition and the moves.

**Prerequisites:** [How a Computer Stores Data](/cortex/data-structures-and-algorithms/foundations/how-a-computer-stores-data) (bits and binary) and [Measuring Cost](/cortex/data-structures-and-algorithms/foundations/measuring-cost).
**What's next:** the first bit pattern — reading and writing a specific bit, in [Kth Bit](/cortex/data-structures-and-algorithms/bit-tricks/pattern-kth-bit/pattern).

## Recall

> **Mnemonic:** *Mask with `1 << i`. `OR` sets, `AND ~` clears, `XOR` flips, `>> & 1` tests — one instruction over the whole word.*

| Operation | Expression | |
|---|---|---|
| test bit `i` | `(x >> i) & 1` | is it on? |
| set bit `i` | `x \| (1 << i)` | force to 1 |
| clear bit `i` | `x & ~(1 << i)` | force to 0 |
| toggle bit `i` | `x ^ (1 << i)` | flip it |
| lowest set bit | `x & -x` | isolate it |

<details>
<summary><strong>Q:</strong> How do you build a mask for a single bit `i`?</summary>

**A:** `1 << i` — a 1 shifted left into position `i`.

</details>
<details>
<summary><strong>Q:</strong> Why does `x & ~(1 << i)` clear bit `i` without disturbing the rest?</summary>

**A:** `~(1 << i)` is all 1s except bit `i`; `AND` keeps every other bit and zeros only bit `i`.

</details>
<details>
<summary><strong>Q:</strong> What two properties make XOR special?</summary>

**A:** `a ^ a = 0` and `a ^ 0 = a` — it's its own inverse, so duplicates cancel.

</details>
<details>
<summary><strong>Q:</strong> Why is bit manipulation fast?</summary>

**A:** Each operator runs in one instruction over all 64 bits at once, so an integer acts as a 64-flag set.

</details>

## Sources & Verify

- **Henry S. Warren, Jr., *Hacker's Delight*, 2nd ed.** — the canonical reference for bit-level tricks (`x & -x`, population count, and dozens more), with correctness arguments.
- **cp-algorithms.com**, "Bit manipulation" — the test/set/clear/toggle idioms and the `1 << i` mask, in interview/CP form.
- Builds directly on [How a Computer Stores Data](/cortex/data-structures-and-algorithms/foundations/how-a-computer-stores-data) — the binary representation and "everything is bits" model.
- Both runnable blocks are verified by running; each operator's effect is shown in the printed binary.
