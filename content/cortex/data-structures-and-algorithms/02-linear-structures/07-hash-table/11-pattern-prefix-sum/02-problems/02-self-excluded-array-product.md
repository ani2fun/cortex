---
title: "Self Excluded Array Product"
summary: "Given arr, return an array product where product[i] equals the product of all elements except arr[i]. Solve in O(n) and without division."
prereqs:
  - 11-pattern-prefix-sum/01-pattern
difficulty: medium
---

# Self excluded array product

## Problem Statement

Given `arr`, return an array `product` where `product[i]` equals the product of all elements **except** `arr[i]`. Solve in `O(n)` and **without division**.

### Example 1
> -   **Input:** `[1, 2, 3, 4]` → **Output:** `[24, 12, 8, 6]`

### Example 2
> -   **Input:** `[2, 3, 0]` → **Output:** `[0, 0, 6]`

### Example 3
> -   **Input:** `[3, 4]` → **Output:** `[4, 3]`

## Examples

**Example 1**
```
Input:  [1, 2, 3, 4]
Output: [24, 12, 8, 6]
Explanation: product[0] = 2·3·4 = 24, product[1] = 1·3·4 = 12, product[2] = 1·2·4 = 8,
product[3] = 1·2·3 = 6. Each slot holds the product of every element except its own.
```

**Example 2**
```
Input:  [2, 3, 0]
Output: [0, 0, 6]
Explanation: product[0] = 3·0 = 0, product[1] = 2·0 = 0, product[2] = 2·3 = 6.
A single zero zeroes every slot except the zero's own position.
```

**Example 3**
```
Input:  [3, 4]
Output: [4, 3]
Explanation: product[0] = 4 and product[1] = 3 — each slot holds the other element.
```

**Example 4**
```
Input:  [5, 1, 1, 1]
Output: [1, 5, 5, 5]
Explanation: the three 1s leave the product of the others unchanged, so every slot
except index 0 picks up the 5.
```

<details>
<summary><h2>Approach</h2></summary>


This is the prefix-sum trick generalised to **prefix products**. Build two arrays:

- `prefix[i] = arr[0] * arr[1] * ... * arr[i-1]` (product of everything strictly before `i`).
- `suffix[i] = arr[i+1] * arr[i+2] * ... * arr[n-1]` (product of everything strictly after `i`).

Then `product[i] = prefix[i] * suffix[i]`. Two passes (one left-to-right, one right-to-left), no division, O(N) time and O(N) space (which can be optimised to O(1) extra by computing one direction in-place).

```d2
direction: right

arr: "arr" {
  grid-columns: 4
  grid-gap: 0
  a0: "1"
  a1: "2"
  a2: "3"
  a3: "4"
}

prefix: "prefix (product of arr[0..i-1])" {
  grid-columns: 4
  grid-gap: 0
  p0: "1"
  p1: "1"
  p2: "2"
  p3: "6"
}

suffix: "suffix (product of arr[i+1..n-1])" {
  grid-columns: 4
  grid-gap: 0
  s0: "24"
  s1: "12"
  s2: "4"
  s3: "1"
}

product: "product = prefix * suffix" {
  grid-columns: 4
  grid-gap: 0
  r0: "24" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  r1: "12" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  r2: "8" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  r3: "6" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
}
```

<p align="center"><strong>Self-excluded product — prefix product holds "everything before me", suffix product holds "everything after me", and their pointwise product is the answer. The technique is prefix-sum's multiplicative cousin.</strong></p>

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=prefix_product
from typing import List

class Solution:
    def self_excluded_array_product(self, arr: List[int]) -> List[int]:
        n: int = len(arr)
        prefix_product: List[int] = [0] * n
        suffix_product: List[int] = [0] * n
        result: List[int] = [0] * n

        # Prefix product
        prefix_product[0] = arr[0]
        for i in range(1, n):
            prefix_product[i] = prefix_product[i - 1] * arr[i]

        # Suffix product
        suffix_product[n - 1] = arr[n - 1]
        for i in range(n - 2, -1, -1):
            suffix_product[i] = suffix_product[i + 1] * arr[i]

        # Result calculation
        result[0] = suffix_product[1]
        result[n - 1] = prefix_product[n - 2]
        for i in range(1, n - 1):
            result[i] = prefix_product[i - 1] * suffix_product[i + 1]

        return result


# Examples from the problem statement
print(Solution().self_excluded_array_product([1, 2, 3, 4]))  # [24, 12, 8, 6]
print(Solution().self_excluded_array_product([2, 3, 0]))     # [0, 0, 6]
print(Solution().self_excluded_array_product([3, 4]))        # [4, 3]

# Edge cases
print(Solution().self_excluded_array_product([1, 1]))        # [1, 1]
print(Solution().self_excluded_array_product([2, 2]))        # [2, 2]
print(Solution().self_excluded_array_product([1, 2, 3]))     # [6, 3, 2]
print(Solution().self_excluded_array_product([0, 0]))        # [0, 0]
print(Solution().self_excluded_array_product([5, 1, 1, 1]))  # [1, 5, 5, 5]
```

```java run viz=array viz-root=prefix_product
import java.util.Arrays;

public class Main {
    static class Solution {
        public int[] selfExcludedArrayProduct(int[] arr) {
            int n = arr.length;
            int[] prefixProduct = new int[n];
            int[] suffixProduct = new int[n];
            int[] result = new int[n];

            // Prefix product
            prefixProduct[0] = arr[0];
            for (int i = 1; i < n; i++) {
                prefixProduct[i] = prefixProduct[i - 1] * arr[i];
            }

            // Suffix product
            suffixProduct[n - 1] = arr[n - 1];
            for (int i = n - 2; i >= 0; i--) {
                suffixProduct[i] = suffixProduct[i + 1] * arr[i];
            }

            // Result calculation
            result[0] = suffixProduct[1];
            result[n - 1] = prefixProduct[n - 2];
            for (int i = 1; i < n - 1; i++) {
                result[i] = prefixProduct[i - 1] * suffixProduct[i + 1];
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(Arrays.toString(new Solution().selfExcludedArrayProduct(new int[]{1, 2, 3, 4})));  // [24, 12, 8, 6]
        System.out.println(Arrays.toString(new Solution().selfExcludedArrayProduct(new int[]{2, 3, 0})));     // [0, 0, 6]
        System.out.println(Arrays.toString(new Solution().selfExcludedArrayProduct(new int[]{3, 4})));        // [4, 3]

        // Edge cases
        System.out.println(Arrays.toString(new Solution().selfExcludedArrayProduct(new int[]{1, 1})));        // [1, 1]
        System.out.println(Arrays.toString(new Solution().selfExcludedArrayProduct(new int[]{2, 2})));        // [2, 2]
        System.out.println(Arrays.toString(new Solution().selfExcludedArrayProduct(new int[]{1, 2, 3})));     // [6, 3, 2]
        System.out.println(Arrays.toString(new Solution().selfExcludedArrayProduct(new int[]{0, 0})));        // [0, 0]
        System.out.println(Arrays.toString(new Solution().selfExcludedArrayProduct(new int[]{5, 1, 1, 1}))); // [1, 5, 5, 5]
    }
}
```

</details>
<details>
<summary><h2>Intuition</h2></summary>


Each output slot needs the product of every element *except* the one at that index. The brute-force read is a double loop: for each index, multiply all the others. That is `O(N²)` time. The tempting shortcut — multiply everything once, then divide out `arr[i]` per slot — is `O(N)` but illegal here (division is banned) and broken anyway, because a single `0` makes the total product `0` and division by the other zeros is undefined.

The prefix-sum idea generalises from addition to multiplication. Split the answer at each index into two halves: everything *before* it and everything *after* it. Build `prefix_product[i]` as the product of all elements left of `i`, and `suffix_product[i]` as the product of all elements right of `i`. Then `result[i] = prefix_product[i] · suffix_product[i]` — the index's own value is in neither half, so it is automatically excluded.

This is the multiplicative cousin of prefix sums, and it needs no hash map — two precomputed arrays carry the running products. What breaks if you reach for the division trick is correctness on zeros: with one zero the answer is defined (every other slot is `0`, the zero's slot is the product of the rest), and with two zeros every slot is `0`. The diagnostic signal is "combine a left-running aggregate with a right-running aggregate", which the prefix/suffix pass handles in `O(N)` regardless of zeros or negatives.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Self Excluded Array Product |
|---|---|
| **Q1.** Does the answer reduce to a subarray aggregate? | **Yes** — each slot is a product over a prefix and a suffix, the multiplicative analogue of a range sum. |
| **Q2.** Is the input a linear sequence walked once? | **Yes** — one left-to-right pass for prefixes, one right-to-left pass for suffixes. |
| **Q3.** Is the matching slice found by a hash-map lookup? | **No** — the query is positional, so two prefix/suffix arrays suffice; no hash map of values is needed. |
| **Q4.** Does the rule survive negatives and zeros? | **Yes** — products of signed values and zeros are correct, which is exactly why the division shortcut is banned. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Allocate `prefix_product`, `suffix_product`, and `result`, each of length `n`.
2. Fill `prefix_product` left to right: `prefix_product[0] = arr[0]`, then `prefix_product[i] = prefix_product[i - 1] · arr[i]` — the product of `arr[0..i]`.
3. Fill `suffix_product` right to left: `suffix_product[n - 1] = arr[n - 1]`, then `suffix_product[i] = suffix_product[i + 1] · arr[i]` — the product of `arr[i..n-1]`.
4. Handle the two endpoints, which have only one neighbouring half: `result[0] = suffix_product[1]` and `result[n - 1] = prefix_product[n - 2]`.
5. For each interior index `i`, set `result[i] = prefix_product[i - 1] · suffix_product[i + 1]` — everything strictly before times everything strictly after.
6. Return `result`.

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1: `arr = [1, 2, 3, 4]`, expected output `[24, 12, 8, 6]`. Build both running-product arrays, then combine:

```
prefix_product (product of arr[0..i]):   [1, 2, 6, 24]
suffix_product (product of arr[i..n-1]): [24, 24, 12, 4]

result[0]     = suffix_product[1]                   = 24
result[1]     = prefix_product[0] · suffix_product[2] = 1 · 12 = 12
result[2]     = prefix_product[1] · suffix_product[3] = 2 · 4  = 8
result[3]     = prefix_product[2]                   = 6

result = [24, 12, 8, 6]
```

The result `[24, 12, 8, 6]` matches the expected output — each slot is the product of every element except its own.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| | Cost | Why |
|---|---|---|
| **Time** | **O(N)** | Two linear passes build the product arrays, one more combines them; each step is `O(1)`. |
| **Space** | **O(N)** | The `prefix_product` and `suffix_product` arrays hold `N` entries each. The output is `O(N)`, and the auxiliary space can be cut to `O(1)` by folding the suffix pass into the result array. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Input | Output | Why |
|---|---|---|
| `[3, 4]` | `[4, 3]` | Two elements — each slot is the other value. |
| `[1, 1]` | `[1, 1]` | All ones — every excluded product is `1`. |
| `[2, 2]` | `[2, 2]` | Each slot is the single other `2`. |
| `[2, 3, 0]` | `[0, 0, 6]` | One zero zeroes every slot except the zero's own position. |
| `[0, 0]` | `[0, 0]` | Two zeros — each slot's "others" product still contains a zero. |
| `[5, 1, 1, 1]` | `[1, 5, 5, 5]` | The three `1`s leave the others' product unchanged, so every slot except index `0` picks up the `5`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The prefix-sum idea is not limited to sums — split each answer into a prefix aggregate and a suffix aggregate, and "the product of all other elements" falls out in `O(N)` without division, correct even when the array holds zeros.

</details>