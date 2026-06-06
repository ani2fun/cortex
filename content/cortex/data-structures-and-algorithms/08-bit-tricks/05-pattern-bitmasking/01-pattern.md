---
title: "Pattern: Bitmasking"
summary: "Encode a subset of n items as an n-bit integer (bit j set ⇒ item j in), so counting 0..2^n−1 enumerates the entire power set and set operations become O(1) bitwise ops. Also: constant masks (0x55…/0xAA…) operate on bit-groups in parallel."
prereqs:
  - 08-bit-tricks/01-pattern-kth-bit/01-pattern
---

# Pattern: Bitmasking

## Why It Exists

"Bitmasking" carries two related meanings, both worth knowing. The first is a **constant mask** that selects a region of bits — the kth-bit operations were the one-bit case; multi-bit masks like `0x55555555` (every odd bit) and `0xAAAAAAAA` (every even bit) let you act on whole groups of bits at once.

The second, and the star of this lesson, is **a subset encoded as bits**. With `n` items, an `n`-bit integer represents *any* subset: bit `j` set means item `j` is in. That's powerful because the `2^n` integers `0..2^n − 1` then enumerate **every subset** (the power set), and set operations — membership, add, remove — become `O(1)` bitwise ops. An inherently exponential "try every subset" becomes a single counting loop, which is the entire foundation of bitmask dynamic programming.

## See It Work

Enumerate every subset of `['a', 'b', 'c']` by counting masks `0..7` and decoding each. Run it.

```python run viz=array
def all_subsets(items):
    n = len(items)
    result = []
    for mask in range(1 << n):                          # 0 .. 2^n - 1 — one per subset
        subset = [items[j] for j in range(n) if mask & (1 << j)]   # bit j set ⇒ item j in
        result.append(subset)
    return result

out = all_subsets(['a', 'b', 'c'])
print(len(out))   # 8
print(out)        # [[], ['a'], ['b'], ['a','b'], ['c'], ['a','c'], ['b','c'], ['a','b','c']]
```

## How It Works

Map each item to a bit position, then a subset is just an integer:

| mask | bits | subset |
|---|---|---|
| `0` | `000` | `{}` |
| `3` | `011` | `{a, b}` |
| `5` | `101` | `{a, c}` |
| `7` | `111` | `{a, b, c}` |

Loop `mask` from `0` to `2^n − 1` and you visit every subset exactly once. Inside, the kth-bit operations *are* the set operations: `mask & (1 << j)` tests membership, `mask | (1 << j)` adds item `j`, `mask & ~(1 << j)` removes it — each `O(1)`.

```d2
direction: right
both: "Two flavors of bitmasking" {
  grid-rows: 1
  grid-columns: 2
  grid-gap: 20
  a: |md
    **A — Constant masks**
    `0x55555555` selects odd bits
    `0xAAAAAAAA` selects even bits
    Use for parallel bit-group ops
  |
  b: |md
    **B — Subset encoding**
    Bit i set ⇒ element i is in
    Loop `mask = 0..2^n` enumerates all subsets
    Set ops become bitwise ops
  |
}
```

<p align="center"><strong>bitmasking has two flavours: constant masks select bit-groups for parallel ops; subset-encoding makes an integer a set so a counting loop enumerates the power set.</strong></p>

Enumerating all subsets is **`O(2^n × n)`** (decoding each mask is `O(n)`), so it's practical only for **small `n`** (roughly `n ≤ 20`–`30`). That exponential ceiling is the whole point: bitmasking makes combinatorial search *feasible* for small `n`, not cheap for large `n`.

### Key Takeaway

Encode a subset as an integer (bit `j` ⇒ item `j`); counting `0..2^n−1` enumerates the power set, and membership/add/remove are `O(1)` kth-bit ops. Feasible for small `n` (`≤ ~20`–`30`) — the basis of bitmask DP. Constant masks are the parallel-bit-group cousin.

## Trace It

Masks `0..7` for items `[a, b, c]`:

| mask | binary | subset |
|---|---|---|
| 0 | `000` | `{}` |
| 1 | `001` | `{a}` |
| 2 | `010` | `{b}` |
| 3 | `011` | `{a, b}` |
| 4 | `100` | `{c}` |
| … | … | … |
| 7 | `111` | `{a, b, c}` |

Before you read on: this enumerates `2^n` subsets. For `n = 4` that's `16`; for `n = 60` it's about `10^18`. Why does that make bitmasking a *small-n* technique, and what's the practical ceiling?

Because `2^n` grows explosively: at `n = 30` you're already near a billion masks, and `n = 60` is astronomically infeasible. So bitmasking subset-enumeration (and bitmask DP) is the right tool only when `n` is small — typically `n ≤ 20` for a plain `O(2^n · n)` scan, up to `~30` with care, and never for large collections. The win isn't beating polynomial algorithms; it's making an *exponential* problem (try every subset, every assignment) tractable *at all* when `n` is bounded — and packing the subset into a single machine word so the bookkeeping is `O(1)`. Recognizing the small-`n` constraint is how you know bitmasking applies.

## Your Turn

The reusable power-set enumerator:

```python run viz=array
def all_subsets(items):
    n = len(items)
    result = []
    for mask in range(1 << n):
        subset = [items[j] for j in range(n) if mask & (1 << j)]
        result.append(subset)
    return result

print(all_subsets([1, 2]))   # [[], [1], [2], [1, 2]]
```

```java run viz=array
import java.util.*;

public class Main {
  static List<List<Integer>> allSubsets(int[] items) {
    int n = items.length;
    List<List<Integer>> result = new ArrayList<>();
    for (int mask = 0; mask < (1 << n); mask++) {
      List<Integer> subset = new ArrayList<>();
      for (int j = 0; j < n; j++)
        if ((mask & (1 << j)) != 0) subset.add(items[j]);   // bit j set ⇒ item j in
      result.add(subset);
    }
    return result;
  }

  public static void main(String[] args) {
    System.out.println(allSubsets(new int[]{1, 2}));   // [[], [1], [2], [1, 2]]
  }
}
```

Drill the family in **Practice** — [Pairwise Bits Swap](/cortex/data-structures-and-algorithms/bit-tricks-pattern-bitmasking-problems-pairwise-bits-swap) and [Unique Subsets](/cortex/data-structures-and-algorithms/bit-tricks-pattern-bitmasking-problems-unique-subsets).

## Reflect & Connect

Bitmasking is where bit tricks meet combinatorics:

- **The family** — enumerate the power set, **bitmask DP** (traveling salesman, assignment, "visited set" as an integer), packed boolean state/flags, and parallel bit-group ops with constant masks (`(num & 0x55555555) << 1` to slide odd-positioned bits, etc.).
- **Set operations are kth-bit operations** — membership `&`, add `|`, remove `& ~` — so this pattern is the kth-bit toolkit applied to a set-as-integer. Iterating *submasks* of a mask (`sub = (sub - 1) & mask`) is the next level, used in subset-sum DP.
- **The `2^n` ceiling is the design signal** — bitmasking makes exponential search *feasible for small n*, not fast for large `n`. Seeing `n ≤ ~20` in the constraints is the hint to reach for it.

**Prerequisites:** [Kth-Bit Operations](/cortex/data-structures-and-algorithms/bit-tricks-pattern-kth-bit-pattern).
**What's next:** classic bit-trick applications — parity, power-of-two, fast exponentiation — in [Bit-Manipulation Applications](/cortex/data-structures-and-algorithms/bit-tricks-pattern-applications-pattern).

## Recall

> **Mnemonic:** *Subset = integer (bit j ⇒ item j). Loop `mask = 0..2^n−1` enumerates the power set. Membership/add/remove = `&` / `\|` / `& ~`. Small n only.*

| | |
|---|---|
| Encode | bit `j` set ⇒ item `j` in the subset |
| Enumerate | `for mask in 0..2^n − 1` |
| Test / add / remove | `mask & (1<<j)` / `mask \| (1<<j)` / `mask & ~(1<<j)` |
| Cost | `O(2^n · n)` — feasible only for small `n` (`≤ ~20`–`30`) |
| Other flavour | constant masks (`0x55…`, `0xAA…`) for parallel bit-group ops |

<details>
<summary><strong>Q:</strong> How does an integer represent a subset?</summary>

**A:** Bit `j` set means item `j` is in; the `n`-bit integer is the subset's membership vector.

</details>
<details>
<summary><strong>Q:</strong> How do you enumerate every subset?</summary>

**A:** Loop `mask` from `0` to `2^n − 1`; each value is a distinct subset (the power set).

</details>
<details>
<summary><strong>Q:</strong> What are the set operations in this encoding?</summary>

**A:** Membership `mask & (1<<j)`, add `mask | (1<<j)`, remove `mask & ~(1<<j)` — the kth-bit ops.

</details>
<details>
<summary><strong>Q:</strong> Why is bitmasking a small-`n` technique?</summary>

**A:** There are `2^n` subsets, which is infeasible beyond `n ≈ 20`–`30`; it makes exponential search *tractable for small n*, not fast for large `n`.

</details>

## Sources & Verify

- **Warren**, *Hacker's Delight*, 2nd ed. — constant masks and parallel bit-group operations.
- **CLRS / competitive-programming canon** — bitmask DP (subset enumeration, the `O(2^n · n)` and held-Karp `O(2^n · n^2)` bounds).
- Subset enumeration via counting masks is standard; both runnable blocks are verified by running (`['a','b','c'] ⇒ 8 subsets`, `[1,2] ⇒ [[],[1],[2],[1,2]]`).
