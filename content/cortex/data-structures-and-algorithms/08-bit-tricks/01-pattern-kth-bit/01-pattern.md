---
title: "Pattern: Kth-Bit Operations"
summary: "Build a mask 1 << (k-1) with a single 1 at position k, then combine it with the integer: AND to check, OR to set, AND-NOT to clear, XOR to toggle. One mask, four operators, O(1) each — the alphabet of bit manipulation."
prereqs:
  - 08-bit-tricks/00-bit-manipulation
---

# Pattern: Kth-Bit Operations

## Why It Exists

An integer is a row of bits — 32 boolean cells in a 32-bit `int` — but you can't index one like `bits[k]`. You need to read or change the single bit at position `k` while leaving all the others exactly as they were.

The trick is a **mask**: a number that is `1` at position `k` and `0` everywhere else, built by shifting — `1 << (k - 1)`. Combine that mask with your integer using the bitwise operator that matches your intent, and only bit `k` is affected (the mask's zeros leave every other bit untouched, because bitwise operators act on each position independently). One mask, four operators: **AND** to check, **OR** to set, **AND-NOT** to clear, **XOR** to toggle. Each is a single `O(1)` machine instruction — the primitives every higher-level bit trick is built from.

## See It Work

Take `n = 0b1010` (decimal `10`) and operate on bit `k = 3` (1-indexed from the least-significant end, which is currently `0`). Run it.

```python run viz=array
n = 0b1010                    # 10; bits 1-indexed from LSB: b1=0, b2=1, b3=0, b4=1
k = 3
mask = 1 << (k - 1)           # 0b0100 — a single 1 at position 3

print(bool(n & mask))         # check : False (bit 3 is 0)
print(n | mask)               # set   : 14  (0b1110)
print(n & ~mask)              # unset : 10  (already 0 → unchanged)
print(n ^ mask)               # toggle: 14  (0b1110 — flipped 0→1)
```

## How It Works

Everything hinges on the mask `1 << (k - 1)` — a `1` slid into position `k`:

```d2
direction: right
mask: "Mask for k = 3:  1 << (k - 1)" {
  grid-rows: 2
  grid-columns: 8
  grid-gap: 0
  b7: "0"
  b6: "0"
  b5: "0"
  b4: "0"
  b3: "0"
  b2: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  b1: "0"
  b0: "0"
  l7: "[8]"
  l6: "[7]"
  l5: "[6]"
  l4: "[5]"
  l3: "[4]"
  l2: "[3]"
  l1: "[2]"
  l0: "[1]"
}
```

<p align="center"><strong>the mask <code>1 << (k - 1)</code> for <code>k = 3</code> has a single 1 at position 3 and 0 everywhere else; each operator combines it with the input to touch only that bit.</strong></p>

Then the operator encodes the intent:

| Intent | Expression | Why it works |
|---|---|---|
| **Check** bit `k` | `n & (1 << (k-1))` | AND keeps only bit `k`; result non-zero ⇒ the bit was `1` |
| **Set** bit `k` to 1 | `n \| (1 << (k-1))` | OR forces bit `k` to `1`; zeros in the mask leave others alone |
| **Clear** bit `k` to 0 | `n & ~(1 << (k-1))` | the inverted mask is `0` only at `k`, so AND zeroes just that bit |
| **Toggle** bit `k` | `n ^ (1 << (k-1))` | XOR flips wherever the mask is `1` — only bit `k` |

Each is `O(1)`. The reason other bits survive: where the mask is `0`, `OR`/`XOR` leave the input unchanged and `AND ~mask` keeps it — bitwise operators treat each position independently.

### Key Takeaway

Every kth-bit primitive is `mask = 1 << (k - 1)` plus one operator: `&` check, `|` set, `& ~` clear, `^` toggle. Memorise the mask; the rest is choosing the operator — all `O(1)`.

## Trace It

Operating on `n = 0b1010` at `k = 3` (`mask = 0b0100`):

| op | computation | result |
|---|---|---|
| check | `0b1010 & 0b0100` = `0` | `False` |
| set | `0b1010 \| 0b0100` = `0b1110` | `14` |
| clear | `0b1010 & 0b1011` = `0b1010` | `10` (no change) |
| toggle | `0b1010 ^ 0b0100` = `0b1110` | `14` |

Before you read on: what does `1 << (k - 1)` evaluate to for `k = 1, 2, 3, 8` — and why is it `k - 1` rather than `k` in the shift?

`1, 2, 4, 128` — each left-shift doubles the value (`1<<0=1`, `1<<1=2`, `1<<2=4`, `1<<7=128`). The shift is `k - 1` because we count bit *positions* from `1` (position 1 = LSB) but the shift count is *0-based* (shifting by `0` leaves the `1` in the lowest position). That single `−1` is where off-by-one bugs hide: many language libraries index bits from `0` instead, in which case the mask is just `1 << k`. Pick one convention and hold it — this lesson is 1-based throughout.

## Your Turn

The four reusable primitives:

```python run viz=array
def check(n, k):  return (n & (1 << (k - 1))) != 0
def set_bit(n, k):  return n | (1 << (k - 1))
def clear(n, k):  return n & ~(1 << (k - 1))
def toggle(n, k): return n ^ (1 << (k - 1))

n = 0b1010
print(check(n, 2), set_bit(n, 1), clear(n, 2), toggle(n, 4))   # True 11 8 2
```

```java run viz=array
public class Main {
  static boolean check(int n, int k) { return (n & (1 << (k - 1))) != 0; }
  static int setBit(int n, int k)    { return n | (1 << (k - 1)); }
  static int clear(int n, int k)     { return n & ~(1 << (k - 1)); }
  static int toggle(int n, int k)    { return n ^ (1 << (k - 1)); }

  public static void main(String[] args) {
    int n = 0b1010;
    System.out.println(check(n, 2) + " " + setBit(n, 1) + " " + clear(n, 2) + " " + toggle(n, 4));
    // true 11 8 2
  }
}
```

Drill the family in **Practice** — [Kth-Bit Check](/cortex/data-structures-and-algorithms/bit-tricks/pattern-kth-bit/problems/kth-bit-check), [Set Kth Bit](/cortex/data-structures-and-algorithms/bit-tricks/pattern-kth-bit/problems/set-kth-bit), [Unset Kth Bit](/cortex/data-structures-and-algorithms/bit-tricks/pattern-kth-bit/problems/unset-kth-bit), and [Toggle Kth Bit](/cortex/data-structures-and-algorithms/bit-tricks/pattern-kth-bit/problems/toggle-kth-bit).

## Reflect & Connect

These four primitives are the alphabet the rest of bit manipulation spells with:

- **The family** — check / set / clear / toggle a bit, and combinations (e.g. "set bit `i`, clear bit `j`"). Every one is build-the-mask-then-apply-the-operator.
- **Mask-building generalizes** — the next patterns build *fancier* masks: `n & (n-1)` to strip the lowest set bit, `n & -n` to isolate it, a mask of several bits for a field. The operator-selection idea stays identical.
- **It powers higher structures** — packed boolean flags, bitmask dynamic programming, fast subset enumeration, and permission bits all rest on "address one bit with a mask." Master the mask and those stop being mysterious.

**Prerequisites:** [Bit Manipulation](/cortex/data-structures-and-algorithms/bit-tricks/bit-manipulation).
**What's next:** build a mask that isolates the *lowest* set bit — [Set-Bit Finder](/cortex/data-structures-and-algorithms/bit-tricks/pattern-set-bit-finder/pattern).

## Recall

> **Mnemonic:** *`mask = 1 << (k-1)`. `&` check · `|` set · `& ~` clear · `^` toggle. Shift is `k-1` because positions are 1-based but shifts are 0-based.*

| | |
|---|---|
| Mask | `1 << (k - 1)` — single `1` at position `k` |
| Check | `n & mask` (non-zero ⇒ on) |
| Set / Clear | `n \| mask` / `n & ~mask` |
| Toggle | `n ^ mask` |
| Cost | `O(1)` each |

<details>
<summary><strong>Q:</strong> What is the mask for bit `k`, and what makes it work?</summary>

**A:** `1 << (k-1)` — a single `1` at position `k`; its zeros leave all other bits untouched under the bitwise operators.

</details>
<details>
<summary><strong>Q:</strong> Which operator does each of check/set/clear/toggle use?</summary>

**A:** `&`, `|`, `& ~`, `^` respectively.

</details>
<details>
<summary><strong>Q:</strong> Why `k - 1` in the shift?</summary>

**A:** Positions are 1-based but shift counts are 0-based; `1 << 0` puts the `1` in position 1.

</details>
<details>
<summary><strong>Q:</strong> Why are the other bits unaffected?</summary>

**A:** Bitwise operators act per-position, and the mask is `0` everywhere except `k`, so elsewhere `OR`/`XOR`/`AND ~mask` leave the input unchanged.

</details>

## Sources & Verify

- **Warren**, *Hacker's Delight*, 2nd ed., ch. 2 — bit masks and single-bit operations.
- **CLRS**, *Introduction to Algorithms*, 4th ed., App. / bit-level operations; **Sedgewick & Wayne**, *Algorithms*, 4th ed. — bitwise operators.
- The mask-plus-operator primitives are standard; both runnable blocks are verified by running (`check/set/clear/toggle` of `0b1010` give `False/14/10/14`; the primitives give `True 11 8 2`).
