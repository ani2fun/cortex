---
title: "Bubble Sort"
summary: "Repeatedly compare adjacent pairs and swap the out-of-order ones; each pass bubbles the largest unsorted element to its final place at the end. O(n²) and rarely used in practice, but the gentlest introduction to in-place, swap-based, stable sorting — and O(n) on already-sorted input with an early-exit check."
prereqs:
  - 02-linear-structures/01-arrays/01-what-is-an-array
---

# Bubble Sort

## Why It Exists

Sorting is the canonical "put things in order" task, and bubble sort is the simplest possible approach — the one you'd invent by hand. Walk the array comparing each element to its neighbour; whenever a pair is out of order, swap them. Do that across the whole array and the single largest element "bubbles" all the way to the end. Repeat, and the second-largest settles into the second-to-last slot, and so on.

It's not fast — `O(n²)` — and you won't reach for it in production. But it earns its place as the *first* sort because every idea it introduces recurs: comparing and swapping adjacent elements, sorting **in place** (no extra array), **stability** (equal elements keep their order), and **adaptivity** (detecting an already-sorted input and stopping early). Understand bubble sort and the faster sorts become variations on a theme.

## See It Work

Sort `[5, 2, 8, 1, 9, 3]` by repeatedly swapping adjacent out-of-order pairs. Run it, then **Visualise** the largest values bubble rightward.

> ▶ Run it, then click **Visualise** — each pass walks left to right swapping neighbours; watch the biggest unsorted value reach the end every pass.

```python run viz=array viz-root=arr
arr = [5, 2, 8, 1, 9, 3]
n = len(arr)
for i in range(n - 1):                  # n-1 passes
    swapped = False
    for j in range(n - 1 - i):          # the last i elements are already in place
        if arr[j] > arr[j + 1]:         # adjacent pair out of order?
            arr[j], arr[j + 1] = arr[j + 1], arr[j]   # swap
            swapped = True
    if not swapped:                     # a clean pass ⇒ already sorted, stop early
        break
print(arr)                              # [1, 2, 3, 5, 8, 9]
```

## How It Works

Two nested loops:

- **Inner loop** — scan adjacent pairs `(arr[j], arr[j+1])` and swap any that are out of order. By the end of one full inner pass, the largest unsorted element has bubbled to the rightmost unsorted slot.
- **Outer loop** — repeat the inner pass. After `i` passes the last `i` elements are sorted and fixed, so the inner loop can stop `i` slots earlier: `j < n - 1 - i`.
- **Early exit** — if an inner pass makes *no* swaps, everything is already in order; break.

```mermaid
flowchart LR
  A["pass over arr[0..n-1-i]"] --> B{"arr[j] > arr[j+1]?"}
  B -->|"yes"| S["swap; mark swapped"]
  B -->|"no"| K["keep"]
  S --> A
  K --> A
  A -->|"pass done"| C{"any swaps?"}
  C -->|"no"| D(["sorted — stop"])
  C -->|"yes"| A
```

<p align="center"><strong>each pass walks adjacent pairs left→right, swapping the larger one rightward, so the maximum of the unsorted region lands at its end; the sorted tail grows by one each pass.</strong></p>

The cost: each pass is `O(n)` and there are up to `n−1` passes → **`O(n²)` time** worst and average, `O(n)` best (already sorted, one clean pass with early-exit), **`O(1)` extra space**. It's **stable** — equal elements never swap past each other, so their original order is preserved — which matters when sorting records by one field while keeping another field's order.

### Key Takeaway

Bubble sort repeatedly swaps adjacent out-of-order pairs, bubbling each pass's largest element to the end. `O(n²)` time, `O(1)` space, stable, and `O(n)` on sorted input with an early-exit flag. It's the teaching sort — the faster ones improve on its quadratic cost.

## Trace It

First pass over `[5, 2, 8, 1, 9, 3]`:

| compare | action | array |
|---|---|---|
| `5,2` | swap | `[2,5,8,1,9,3]` |
| `5,8` | keep | `[2,5,8,1,9,3]` |
| `8,1` | swap | `[2,5,1,8,9,3]` |
| `8,9` | keep | `[2,5,1,8,9,3]` |
| `9,3` | swap | `[2,5,1,8,3,9]` |

After one pass, `9` (the max) is at the end.

Before you read on: after each pass the inner loop runs one step *shorter* (`j < n - 1 - i`). Why is it safe — and worthwhile — to skip the tail that earlier passes already touched?

Because each pass guarantees the largest element of the still-unsorted region reaches its final slot at the end of that region. After pass 0, the last element is the global max and is permanently correct; after pass 1, the last *two* are; and so on. So re-scanning that growing sorted tail would only re-compare elements already in place — wasted work that can never swap. Shrinking the inner bound by `i` each pass turns `n` full scans into the triangular sum `n + (n−1) + … + 1 ≈ n²/2` comparisons. It doesn't change the `O(n²)` class, but it halves the constant — and recognizing "this region is already done, don't revisit it" is the same instinct that makes the faster sorts faster.

## Your Turn

The reusable adaptive bubble sort:

```python run viz=array
def bubble_sort(arr):
    n = len(arr)
    for i in range(n - 1):
        swapped = False
        for j in range(n - 1 - i):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr

print(bubble_sort([5, 2, 8, 1, 9, 3]))   # [1, 2, 3, 5, 8, 9]
print(bubble_sort([1, 2, 3, 4]))          # [1, 2, 3, 4] (one pass, early exit)
```

```java run viz=array
import java.util.*;

public class Main {
  static int[] bubbleSort(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {
      boolean swapped = false;
      for (int j = 0; j < n - 1 - i; j++) {
        if (arr[j] > arr[j + 1]) {
          int t = arr[j]; arr[j] = arr[j + 1]; arr[j + 1] = t;
          swapped = true;
        }
      }
      if (!swapped) break;
    }
    return arr;
  }

  public static void main(String[] args) {
    System.out.println(Arrays.toString(bubbleSort(new int[]{5, 2, 8, 1, 9, 3})));   // [1, 2, 3, 5, 8, 9]
  }
}
```

This is a structural lesson — the quickselect / custom-compare pattern sets are where you'll drill sorting in problems.

## Reflect & Connect

Bubble sort is a teaching tool, but the properties it introduces are the vocabulary for *every* sort:

- **Stable, in-place, adaptive** — bubble sort is all three. You'll classify every later sort by these axes: merge sort is stable but not in-place; heapsort is in-place but not stable; quicksort is in-place but not stable and not adaptive. Knowing which guarantees a sort gives is how you pick one.
- **Why it's slow** — it moves elements only *one position per swap*, so an element far from its home needs many swaps. Insertion and selection sort share the `O(n²)` ceiling for the same reason; the `O(n log n)` sorts (merge, quick, heap) move elements *across* the array in bigger jumps.
- **The early-exit is the one practical idea** — on nearly-sorted data, adaptive bubble sort is `O(n)`. That "detect sorted-ness and stop" instinct carries into adaptive variants of better sorts (e.g. Timsort exploits existing runs).

**Prerequisites:** [What Is an Array?](/cortex/data-structures-and-algorithms/linear-structures-arrays-what-is-an-array).
**What's next:** select the minimum each pass instead of bubbling the max — [Selection Sort](/cortex/data-structures-and-algorithms/sorting-and-searching-sorting-selection-sort).

## Recall

> **Mnemonic:** *Swap adjacent out-of-order pairs; each pass bubbles the max to the end. Shrink the inner bound by `i`. No swaps ⇒ sorted, stop. `O(n²)`, stable, in-place.*

| | |
|---|---|
| Mechanism | swap adjacent pairs; largest bubbles to the end each pass |
| Inner bound | `j < n - 1 - i` (skip the sorted tail) |
| Early exit | a pass with no swaps ⇒ array sorted |
| Cost | `O(n²)` avg/worst, `O(n)` best (adaptive), `O(1)` space |
| Properties | stable, in-place, adaptive |

<details>
<summary><strong>Q:</strong> What does one full pass of bubble sort guarantee?</summary>

**A:** The largest element of the unsorted region reaches its final position at that region's end.

</details>
<details>
<summary><strong>Q:</strong> Why does the inner loop shrink by `i` each pass?</summary>

**A:** After `i` passes the last `i` elements are already sorted, so re-scanning them is wasted work.

</details>
<details>
<summary><strong>Q:</strong> What makes bubble sort `O(n)` on already-sorted input?</summary>

**A:** The early-exit flag — a pass with no swaps proves the array is sorted, so it stops after one pass.

</details>
<details>
<summary><strong>Q:</strong> Which three properties does bubble sort have?</summary>

**A:** Stable, in-place, and adaptive.

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, 4th ed. — bubble sort (problem 2-2) and the stability/in-place definitions.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §2.1 — elementary sorts and their properties.
- Bubble sort's `O(n²)`/`O(n)`-adaptive bounds and stability are standard; both runnable blocks are verified by running (`[5,2,8,1,9,3] ⇒ [1,2,3,5,8,9]`; sorted input exits in one pass).
