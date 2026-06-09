---
title: "Pattern: XOR"
summary: "XOR is self-inverse (a^a=0), has identity (a^0=a), and is commutative + associative — so XOR-ing a whole sequence cancels every even-count value and leaves the odd-occurring ones. Turns 'find the unpaired element' into an O(1)-space scan."
prereqs:
  - 08-bit-tricks/01-pattern-kth-bit/01-pattern
---

# Pattern: XOR

## Why It Exists

XOR is the most algebraically useful bitwise operator, and three identities are why: `a ^ a = 0` (a value cancels itself), `a ^ 0 = a` (zero is the identity), and it's **commutative and associative** (order and grouping don't matter). Put them together and a remarkable thing falls out: **XOR-ing an entire sequence cancels every value that appears an even number of times**, regardless of order, leaving the XOR of the odd-occurring ones.

That collapses a whole class of problems. "Every element appears twice except one — find it" would naively need a hash map (`O(n)` space). XOR everything into a single accumulator and the pairs annihilate, leaving the loner — `O(n)` time, `O(1)` space, one line.

## See It Work

In `[2, 2, 2, 1, 3, 1, 3]`, the `1`s and `3`s appear in pairs and the `2` appears three times (odd). XOR everything and only the odd-occurring `2` survives. Run it.

```python run viz=array
def find_odd_occurring(nums):
    x = 0
    for n in nums:
        x ^= n           # pairs cancel (a ^ a = 0); the odd-count value survives
    return x

print(find_odd_occurring([2, 2, 2, 1, 3, 1, 3]))   # 2
```

## How It Works

The three identities combine into one fact: because XOR is commutative and associative, you can mentally reorder the sequence to put equal values adjacent; each equal pair becomes `a ^ a = 0`; and `0` drops out via `a ^ 0 = a`. So whatever appears an *even* number of times vanishes, and the XOR of the array equals the XOR of its *odd-occurring* values.

```d2
direction: right
ex: "XOR cancels pairs across an array" {
  grid-rows: 2
  grid-columns: 7
  grid-gap: 0
  v0: "2"
  v1: "2"
  v2: "2"
  v3: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  v4: "3"
  v5: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  v6: "3"
  l0: ""
  l1: "cancels"
  l2: ""
  l3: ""
  l4: ""
  l5: "cancels"
  l6: ""
}
```

<p align="center"><strong>XOR over the array cancels paired values (the two 1s, the two 3s, two of the three 2s), leaving the unpaired 2. Order is irrelevant because XOR is commutative and associative.</strong></p>

Two more moves ride on the same identities:

- **Swap without a temp** — `a ^= b; b ^= a; a ^= b` exchanges two values using only XOR (each step substitutes via self-inversion).
- **Two odd-occurring values** — XOR-all gives `x = a ^ b`; any set bit of `x` is a position where `a` and `b` *differ*, so `x & -x` (isolate that bit — the [set-bit-finder](/cortex/data-structures-and-algorithms/bit-tricks/pattern-set-bit-finder/pattern) trick) **partitions** the array into two groups, one containing `a` and one containing `b`. XOR each group separately to recover both.

A single accumulator means **`O(n)` time, `O(1)` space**.

### Key Takeaway

XOR-ing a sequence cancels every even-count value and leaves the odd-occurring ones — `O(1)` space, order-free. It also swaps without a temp, and `x & -x` partitions when *two* values are unpaired. The three identities (`a^a=0`, `a^0=a`, commutative/associative) are the whole story.

## Trace It

XOR over `[5, 5, 5, 5, 7]`:

| step | accumulator |
|---|---|
| `^5` | `5` |
| `^5` | `0` |
| `^5` | `5` |
| `^5` | `0` |
| `^7` | `7` |

Result `7`.

Before you read on: there are *four* `5`s here, not two. The pattern is usually stated as "every value appears **twice** except one." Why does XOR still cancel all four `5`s and leave `7` — what property of the count actually matters?

What matters is **parity of the count, not the number two**. `5 ^ 5 = 0`, and `0 ^ 5 ^ 5 = 0` again — any *even* number of copies XOR to `0` (they pair off completely), while any *odd* number leaves one `5` behind. Four `5`s is even, so they vanish; the single `7` is odd, so it survives. The "appears twice" framing is just the common case of "appears an even number of times." This is why XOR solves "find the element with odd occurrence count" in full generality, not only the exactly-twice version.

## Your Turn

The reusable odd-occurring finder and the temp-free swap:

```python run viz=array
def find_odd_occurring(nums):
    x = 0
    for n in nums:
        x ^= n
    return x

print(find_odd_occurring([4, 1, 2, 1, 2]))   # 4

a, b = 5, 9
a ^= b; b ^= a; a ^= b      # swap with no temporary
print(a, b)                  # 9 5
```

```java run viz=array
public class Main {
  static int findOddOccurring(int[] nums) {
    int x = 0;
    for (int n : nums) x ^= n;
    return x;
  }

  public static void main(String[] args) {
    System.out.println(findOddOccurring(new int[]{4, 1, 2, 1, 2}));   // 4

    int a = 5, b = 9;
    a ^= b; b ^= a; a ^= b;     // swap with no temporary
    System.out.println(a + " " + b);   // 9 5
  }
}
```

Drill the family in **Practice** — [Have Opposite Signs](/cortex/data-structures-and-algorithms/bit-tricks/pattern-xor/problems/have-opposite-signs), [Swap Without a Temporary](/cortex/data-structures-and-algorithms/bit-tricks/pattern-xor/problems/swap-numbers-without-a-temporary), [Odd-Occurring Element](/cortex/data-structures-and-algorithms/bit-tricks/pattern-xor/problems/odd-occurring-element), [Duplicate Element](/cortex/data-structures-and-algorithms/bit-tricks/pattern-xor/problems/duplicate-element), and [Missing and Duplicated Elements](/cortex/data-structures-and-algorithms/bit-tricks/pattern-xor/problems/missing-and-duplicated-elements).

## Reflect & Connect

XOR's self-inverse property is a Swiss-army knife for "things that come in pairs":

- **The family** — one odd-occurring element (XOR all), **two** odd-occurring (XOR all, then `x & -x` partition), the **missing number** in `0..n` (XOR all values with all indices — the present ones cancel), find a duplicate, swap-without-temp, and parity/toggle counting.
- **`x & -x` is the partition primitive** — when two unknowns survive the XOR, isolating any bit where they differ splits the data into two single-unknown subproblems. That's the [set-bit-finder](/cortex/data-structures-and-algorithms/bit-tricks/pattern-set-bit-finder/pattern) identity doing real work.
- **XOR is addition without carry** — it's bitwise parity, which is why it cancels in pairs and shows up in error-detecting codes, Gray codes, and cryptographic mixing. "XOR everything together" belongs in your reflexes.

**Prerequisites:** [Kth-Bit Operations](/cortex/data-structures-and-algorithms/bit-tricks/pattern-kth-bit/pattern).
**What's next:** treat an integer as a *set* and enumerate subsets — [Bitmasking](/cortex/data-structures-and-algorithms/bit-tricks/pattern-bitmasking/pattern).

## Recall

> **Mnemonic:** *`a^a=0`, `a^0=a`, order-free. XOR the whole array ⇒ even-count values vanish, odd-count survive. Two survivors? Split on `x & -x`.*

| | |
|---|---|
| Identities | `a^a=0`, `a^0=a`, commutative + associative |
| One odd-occurring | XOR everything → the loner |
| Two odd-occurring | XOR all → `x`; partition on `x & -x`; XOR each group |
| Swap no temp | `a^=b; b^=a; a^=b` |
| Cost | `O(n)` time, `O(1)` space |

<details>
<summary><strong>Q:</strong> Why does XOR-ing an array leave only the odd-occurring values?</summary>

**A:** Even counts pair off to `0` (`a^a=0`), and order doesn't matter (commutative/associative), so only odd-count values survive.

</details>
<details>
<summary><strong>Q:</strong> Does the value need to appear *exactly twice* to cancel?</summary>

**A:** No — any *even* count cancels; parity of the count is what matters.

</details>
<details>
<summary><strong>Q:</strong> How do you find *two* odd-occurring values?</summary>

**A:** XOR all to get `x = a^b`, isolate a differing bit with `x & -x`, partition the array by that bit, and XOR each group.

</details>
<details>
<summary><strong>Q:</strong> How does XOR find the missing number in `0..n`?</summary>

**A:** XOR all the values with all the indices `0..n`; every present number cancels its index, leaving the missing one.

</details>

## Sources & Verify

- **Warren**, *Hacker's Delight*, 2nd ed. — XOR identities and the XOR swap.
- **CLRS**, *Introduction to Algorithms*, 4th ed. — bitwise operations; XOR as parity / addition without carry.
- The XOR-cancellation technique (odd-occurring, two-odd partition, missing number) is standard; both runnable blocks are verified by running (`[2,2,2,1,3,1,3] ⇒ 2`, `[4,1,2,1,2] ⇒ 4`, swap ⇒ `9 5`).
