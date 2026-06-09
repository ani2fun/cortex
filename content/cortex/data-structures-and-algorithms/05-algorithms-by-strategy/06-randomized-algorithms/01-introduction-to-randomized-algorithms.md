---
title: Introduction to Randomized Algorithms
summary: Use randomness as a primitive. Las Vegas algorithms are always correct with random (expected-fast) runtime; Monte Carlo algorithms have fixed runtime but a small failure probability that repetition drives down exponentially. Random pivots and random hash seeds defeat adversarial worst cases.
prereqs:
  - foundations-asymptotic-analysis
  - 05-algorithms-by-strategy/02-divide-and-conquer/01-introduction-to-divide-and-conquer
---

## Why It Exists

Most algorithms are deterministic: same input, same steps, same answer. But deliberately flipping coins inside an algorithm buys you things determinism can't. It **defeats adversaries** — quicksort has an `O(n²)` worst case on already-sorted input, but if the pivot is *random*, no fixed input can force that case, because the badness now depends on your private coin flips, not on the data. It **simplifies hard problems** — picking one item uniformly from a stream you can't re-read, or testing a 300-digit number for primality, have clean randomized solutions and ugly deterministic ones. And it **shrinks failure to nothing** — a method that's wrong 25% of the time becomes wrong one-in-a-million after ten independent tries.

Randomized algorithms split into two families by *what* the randomness affects. **Las Vegas** algorithms are always correct; only their *runtime* is random (randomized quicksort always sorts — it just might take longer on an unlucky run). **Monte Carlo** algorithms have a fixed runtime but may be *wrong* with bounded probability (a fast primality test that occasionally calls a composite "prime"). Knowing which family you're in tells you what to trust.

## See It Work

Randomized quicksort picks a **random** pivot each call. It's Las Vegas: the output is *always* the fully sorted array — randomness changes the path, never the destination.

```python run viz=array
import random

def quicksort(arr):
    if len(arr) <= 1:
        return arr
    pivot = arr[random.randrange(len(arr))]          # RANDOM pivot — no input is reliably bad
    less    = [x for x in arr if x < pivot]
    equal   = [x for x in arr if x == pivot]
    greater = [x for x in arr if x > pivot]
    return quicksort(less) + equal + quicksort(greater)

data = [3, 6, 1, 8, 2, 9, 4]
print(quicksort(data))                               # always [1, 2, 3, 4, 6, 8, 9]
print(quicksort(data) == sorted(data))               # always True, whatever the pivots
```

```java run viz=array
import java.util.*;
public class Main {
    static final Random RNG = new Random();
    static List<Integer> quicksort(List<Integer> arr) {
        if (arr.size() <= 1) return arr;
        int pivot = arr.get(RNG.nextInt(arr.size()));        // random pivot
        List<Integer> less = new ArrayList<>(), eq = new ArrayList<>(), gr = new ArrayList<>();
        for (int x : arr) { if (x < pivot) less.add(x); else if (x == pivot) eq.add(x); else gr.add(x); }
        List<Integer> out = new ArrayList<>(quicksort(less));
        out.addAll(eq);
        out.addAll(quicksort(gr));
        return out;
    }
    public static void main(String[] args) {
        List<Integer> data = Arrays.asList(3, 6, 1, 8, 2, 9, 4);
        System.out.println(quicksort(new ArrayList<>(data)));   // [1, 2, 3, 4, 6, 8, 9]
        List<Integer> s = new ArrayList<>(data); Collections.sort(s);
        System.out.println(quicksort(new ArrayList<>(data)).equals(s));   // true
    }
}
```

Both print the sorted array then `true` — run them a thousand times and the output never changes, only the sequence of pivot choices (and the runtime) does. Expected time `O(n log n)`; the `O(n²)` worst case still *exists* but now requires a freak run of unlucky pivots, not a malicious input.

## How It Works

The two families answer "what is random?" differently, and that determines how you use them:

```d2
direction: right
lv: "LAS VEGAS\nalways CORRECT\nruntime is random (expected bound)\nex: randomized quicksort / quickselect" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
mc: "MONTE CARLO\nruntime is FIXED\nmay be WRONG (bounded probability)\nex: Miller-Rabin primality" {style.fill: "#fde68a"; style.stroke: "#d97706"}
trust: "What can you trust?\nLas Vegas: the ANSWER (wait longer if unlucky)\nMonte Carlo: the TIME (repeat to shrink error)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
lv -> trust
mc -> trust
```

<p align="center"><strong>Las Vegas trades a fixed answer for a random runtime; Monte Carlo trades a fixed runtime for a small chance of a wrong answer. You convert between them by waiting (Las Vegas) or by repeating (Monte Carlo).</strong></p>

Three ideas to carry forward:

- **Randomness moves the worst case off the input and onto the coins.** Deterministic quicksort's `O(n²)` is triggered by *specific inputs* (sorted, reverse-sorted). Random pivots make every input *equally likely* to be easy or hard, so an adversary who sees your code but not your coin flips cannot force the bad case. The worst case still exists; it's just no longer *reachable on demand*.
- **Expected time is an average over the coins, not over inputs.** "Expected `O(n log n)`" means: fix *any* input, then average the runtime over the algorithm's random choices. That's a stronger promise than average-case analysis (which averages over inputs and assumes a distribution) — it holds for every input, including adversarial ones.
- **Monte Carlo error falls off a cliff with repetition.** If one run is wrong with probability `p` and runs are independent, then `k` runs all agreeing is wrong with probability `pᵏ` — *exponential* decay. That's why a coin-flip-grade test (`p = ½`) becomes astronomically reliable after a few dozen tries, and it's the engine behind practical primality testing.

> **Key takeaway.** Randomized algorithms use coin flips as a primitive. **Las Vegas** = always correct, runtime random (randomized quicksort/quickselect, expected `O(n log n)` / `O(n)`); **Monte Carlo** = fixed runtime, error probability `p` that `k` independent repeats cut to `pᵏ` (Miller-Rabin). Randomness relocates the worst case from specific *inputs* to unlucky *coin flips* — defeating adversaries (quicksort pivots, HashDoS-resistant hashing).

## Trace It

The exponential error decay is the most counterintuitive — and most useful — fact about Monte Carlo methods. Suppose a primality test, run once on a composite number, wrongly says "prime" with probability `¼`. You run it `k` *independent* times and only believe "prime" if *all* `k` agree.

**Predict before you run:** after 10 independent trials, is the false-"prime" probability roughly `1/40` (linear: `¼ ÷ 10`), or something dramatically smaller?

```python run viz=array
p = 0.25                                          # one trial's error on a composite
for k in (1, 5, 10):
    print(f"{k:>2} trials, all agree -> error = (1/4)^{k} = {p**k:.7f}")
```

<details>
<summary><strong>Reveal</strong></summary>

It's `(¼)¹⁰ ≈ 0.00000095` — about *one in a million*, not `1/40`. Independent trials *multiply*: each extra trial cuts the error by another factor of 4, so the probability is `(¼)ᵏ`, decaying **exponentially** in `k`, not linearly. Ten trials take a number that's wrong a quarter of the time down to one-in-a-million; twenty trials reach one-in-a-trillion. This is why Monte Carlo algorithms are *practical* despite "maybe wrong": you dial the error to any target — `10⁻⁹`, `10⁻¹⁸` — for the price of a handful of repeats, and the cost is only *linear* in the number of trials while the reliability gain is *exponential*. The catch is the word **independent**: the trials must use fresh randomness (e.g. different random bases in Miller-Rabin). Reuse the same coin flips and the errors are perfectly correlated — ten identical wrong answers shrink nothing. Real primality tests (Miller-Rabin) have per-trial error `≤ ¼`, so a few dozen rounds certify primes to a confidence beyond hardware failure rates.

</details>

## Your Turn

**Kth Largest Element** ([LeetCode 215](https://leetcode.com/problems/kth-largest-element-in-an-array/)) via randomized **quickselect** — quicksort's one-sided cousin. Partition on a random pivot, then recurse into *only* the side that contains the rank you want. Las Vegas again: the answer is always the correct order statistic; the random pivot buys *expected `O(n)`* (versus sorting's `O(n log n)`).

```python run viz=array
import random

def quickselect(arr, k):                             # k = 0-indexed rank (k-th smallest)
    pivot = arr[random.randrange(len(arr))]
    less    = [x for x in arr if x < pivot]
    equal   = [x for x in arr if x == pivot]
    greater = [x for x in arr if x > pivot]
    if k < len(less):
        return quickselect(less, k)                  # recurse LEFT only
    elif k < len(less) + len(equal):
        return pivot                                 # the pivot IS the answer
    else:
        return quickselect(greater, k - len(less) - len(equal))   # recurse RIGHT only

A = [3, 6, 1, 8, 2, 9, 4]
print(quickselect(A, 0))                             # 1   (smallest)
print(quickselect(A, 2))                             # 3   (3rd smallest)
print(quickselect(A, 2) == sorted(A)[2])             # True, regardless of pivots
```

```java run viz=array
import java.util.*;
public class Main {
    static final Random RNG = new Random();
    static int quickselect(List<Integer> arr, int k) {
        int pivot = arr.get(RNG.nextInt(arr.size()));
        List<Integer> less = new ArrayList<>(), eq = new ArrayList<>(), gr = new ArrayList<>();
        for (int x : arr) { if (x < pivot) less.add(x); else if (x == pivot) eq.add(x); else gr.add(x); }
        if (k < less.size()) return quickselect(less, k);
        if (k < less.size() + eq.size()) return pivot;
        return quickselect(gr, k - less.size() - eq.size());
    }
    public static void main(String[] args) {
        List<Integer> A = Arrays.asList(3, 6, 1, 8, 2, 9, 4);
        System.out.println(quickselect(new ArrayList<>(A), 0));   // 1
        System.out.println(quickselect(new ArrayList<>(A), 2));   // 3
    }
}
```

Both print `1` then `3`. By recursing into only one side, quickselect expects to do `n + n/2 + n/4 + … ≈ 2n` work — *linear*, beating a full sort when you need just one order statistic. Same Las Vegas guarantee as quicksort: the rank you get back is always correct; only the runtime rolls the dice.

## Reflect & Connect

- **Las Vegas vs Monte Carlo is the first question to ask.** Is the *answer* guaranteed (Las Vegas — trust it, just wait on unlucky runs) or only *probable* (Monte Carlo — repeat to shrink error)? It dictates how you deploy and test the algorithm.
- **Random pivots defeat adversaries.** Quicksort and quickselect move the worst case from "sorted input" to "unlucky coin flips," which an attacker who can't see your randomness cannot trigger. Expected `O(n log n)` / `O(n)` holds for *every* input.
- **Repetition is exponential leverage.** Monte Carlo error `p` becomes `pᵏ` over `k` *independent* runs — linear cost, exponential reliability. Miller-Rabin primality is the canonical case (`p ≤ ¼` per round).
- **Reservoir sampling** picks `k` items uniformly from a stream of *unknown* length in one pass: keep the `i`-th item with probability `k/i`, evicting a random current pick. Each element ends up chosen with probability `k/n` — no second pass, `O(1)` memory. The trick when you can't see the whole input at once.
- **Random hashing is security, not speed.** A fixed hash function lets an attacker craft keys that all collide, degrading a hash table to `O(n)` per op (a HashDoS). A *random* per-process seed makes the collision pattern unpredictable — the same "move the worst case onto private coins" idea, applied to [hash tables](/cortex/data-structures-and-algorithms/linear-structures/hash-table/what-is-a-hash-table).

## Recall

<details>
<summary><strong>Q:</strong> What distinguishes Las Vegas from Monte Carlo algorithms?</summary>

**A:** Las Vegas is *always correct* with a *random runtime* (e.g. randomized quicksort — always sorts, expected `O(n log n)`). Monte Carlo has a *fixed runtime* but may be *wrong* with bounded probability (e.g. Miller-Rabin primality). You trust Las Vegas's answer and Monte Carlo's time.

</details>
<details>
<summary><strong>Q:</strong> How does a random pivot defeat quicksort's adversarial worst case?</summary>

**A:** Deterministic quicksort's `O(n²)` is triggered by specific inputs (sorted/reverse). A random pivot makes the bad case depend on private coin flips, not the data — an adversary who can't see your randomness can't force it. The worst case still exists but isn't reachable on demand; expected time is `O(n log n)` for *every* input.

</details>
<details>
<summary><strong>Q:</strong> Why does running a Monte Carlo algorithm k times cut the error to <code>pᵏ</code>?</summary>

**A:** Independent trials multiply: if each is wrong with probability `p`, all `k` agreeing wrongly has probability `pᵏ` — exponential decay. The trials must use *fresh* randomness; reusing coin flips correlates the errors and shrinks nothing.

</details>
<details>
<summary><strong>Q:</strong> Why is quickselect expected <code>O(n)</code> while quicksort is <code>O(n log n)</code>?</summary>

**A:** Quickselect recurses into only *one* side of the partition (the one holding the target rank), so the expected work is `n + n/2 + n/4 + … ≈ 2n`. Quicksort must recurse into *both* sides, adding the `log n` factor.

</details>
<details>
<summary><strong>Q:</strong> How does reservoir sampling pick a uniform item from a stream of unknown length?</summary>

**A:** Keep the `i`-th element with probability `k/i` (for a reservoir of size `k`), evicting a random current member when you keep it. Each element ends up selected with probability `k/n` in a single pass using `O(k)` memory — no need to know `n` in advance.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., Ch. 5 (Probabilistic Analysis and Randomized Algorithms) and §7.3 (randomized quicksort) — Las Vegas analysis and the expected-time argument.
- **Motwani & Raghavan**, *Randomized Algorithms* (1995) — the standard reference for Monte Carlo / Las Vegas classification, error amplification, and Miller-Rabin.
- **LeetCode** 215 (Kth Largest Element, quickselect) and 382 (Linked List Random Node, reservoir sampling) are the canonical drills; the sorted-array output, the `(1/4)ᵏ` error values, and the `1`/`3` quickselect results above come from the runnable blocks — the Las Vegas outputs are deterministic (only the runtime is random), so re-run to verify.
