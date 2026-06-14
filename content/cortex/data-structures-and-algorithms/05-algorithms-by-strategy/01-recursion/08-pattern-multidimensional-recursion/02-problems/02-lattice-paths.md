---
title: "Lattice Paths"
summary: "Given two positive integers rows and cols (the dimensions of a grid), return the number of distinct paths from the top-left corner to the bottom-right corner if you can only move right or down at any step."
prereqs:
  - 08-pattern-multidimensional-recursion/01-pattern
difficulty: medium
kind: problem
topics: [multidimensional-recursion, recursion]
---

# Lattice Paths

The same recurrence as binomial coefficient, dressed up as grid navigation. Useful for both intuition (it's the same math) and contrast (the *interpretation* matters).

---

## The Problem

Given two positive integers `rows` and `cols` (the dimensions of a grid), return the number of distinct paths from the top-left corner to the bottom-right corner if you can only move **right** or **down** at any step. You **must** solve this recursively.

---

## Examples

**Example 1**
```
Input:  rows = 2, cols = 2
Output: 6
Explanation: 6 distinct paths through a 2×2 grid
```

**Example 2**
```
Input:  rows = 3, cols = 3
Output: 20
Explanation: C(6, 3) = 20 — paths(r, c) = C(r+c, r)
```

```quiz
{
  "prompt": "What is paths(0, 5)?",
  "options": ["0", "1", "5", "6"],
  "answer": "1"
}
```

## Constraints

- `0 ≤ rows, cols ≤ 12`
- Must be solved recursively.

```python run viz=grid
class Solution:
    def lattice_paths(self, rows: int, cols: int) -> int:
        # Your code goes here — base case: rows == 0 or cols == 0 returns 1;
        # recursive case: lattice_paths(rows-1, cols) + lattice_paths(rows, cols-1)
        return 0

rows = int(input())
cols = int(input())
print(Solution().lattice_paths(rows, cols))
```

```java run viz=grid
import java.util.*;

public class Main {
    static class Solution {
        public int latticePaths(int rows, int cols) {
            // Your code goes here — base case: rows == 0 or cols == 0 returns 1;
            // recursive case: latticePaths(rows-1, cols) + latticePaths(rows, cols-1)
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int rows = Integer.parseInt(sc.nextLine().trim());
        int cols = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().latticePaths(rows, cols));
    }
}
```

```testcases
{
  "args": [
    { "id": "rows", "label": "rows", "type": "int", "placeholder": "2" },
    { "id": "cols", "label": "cols", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "rows": "2", "cols": "2" }, "expected": "6" },
    { "args": { "rows": "3", "cols": "3" }, "expected": "20" },
    { "args": { "rows": "0", "cols": "0" }, "expected": "1" },
    { "args": { "rows": "1", "cols": "1" }, "expected": "2" },
    { "args": { "rows": "0", "cols": "5" }, "expected": "1" },
    { "args": { "rows": "5", "cols": "0" }, "expected": "1" },
    { "args": { "rows": "2", "cols": "3" }, "expected": "10" }
  ]
}
```

<details>
<summary><h2>What Does "Only Right or Down" Mean Recursively?</h2></summary>


The first move from the top-left corner is either **right** or **down**:
- **Right.** You're now in a `(rows, cols-1)` subgrid; count its paths.
- **Down.** You're now in a `(rows-1, cols)` subgrid; count its paths.

These two cases are disjoint, so:

```
paths(rows, cols) = paths(rows-1, cols) + paths(rows, cols-1)
```

Base cases:
- `paths(0, c) = 1` for any `c` (a row of cells has only the all-right path).
- `paths(r, 0) = 1` for any `r` (a column has only the all-down path).

This is *the same recurrence* as binomial coefficient. In fact `paths(r, c) = C(r + c, r)` — one of the most elegant identities in combinatorics. Use it to sanity-check answers.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| # | Check | Answer |
|---|---|---|
| **Q1** | Two shrinkable parameters? | **Yes** — `rows` and `cols`. |
| **Q2** | Axis-aware reductions? | **Yes** — one call reduces `rows`, the other reduces `cols`. |
| **Q3** | Base cases on boundaries? | **Yes** — top edge and left edge. |

### Q1 — Why "rows and cols both shrink"?

Each call moves one step right or one step down, shrinking the corresponding dimension. The 2D state space is genuinely 2D — both axes participate. ✓

### Q2 — Why "axis-aware"?

The two recursive calls reduce different axes. `paths(r, c-1)` reduces only `cols`. `paths(r-1, c)` reduces only `rows`. Together they explore the grid two-dimensionally. ✓

### Q3 — Why both boundaries?

A path that goes all-right hits the right edge of the grid and must then go all-down, ending in `(rows, 0)`. A path that goes all-down does the opposite. Both edges must be base cases or those paths never terminate. ✓

</details>
<details>
<summary><h2>The Grid Navigation Strategy (Visualised)</h2></summary>


```d2
direction: down

table: "Cells of paths(r, c) — number of paths from (0,0) to (r,c)" {
  grid-rows: 4
  grid-columns: 4
  grid-gap: 0
  h0:  ""        ; h1:  "c=0"  ; h2:  "c=1"  ; h3:  "c=2"
  r0n: "r=0"     ; c00: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}; c01: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}; c02: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  r1n: "r=1"     ; c10: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}; c11: "2"; c12: "3"
  r2n: "r=2"     ; c20: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}; c21: "3"; c22: "6"
}
```

<p align="center"><strong>Path counts for the bottom-right corner of a 2×2 grid: <code>paths(2, 2) = 6</code>. Yellow cells are the boundary base cases; interior cells = sum of cell above + cell to the left.</strong></p>

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(2^(rows+cols)) space=O(rows+cols)
class Solution:
    def lattice_paths(self, rows: int, cols: int) -> int:

        # Base case: If either rows or cols is 0, there is only
        # one unique path (all the way down or all the way right)
        if rows == 0 or cols == 0:
            return 1

        # Recursive case: The number of unique paths to the bottom-right
        # corner is the sum of the unique paths from the cell directly
        # above and the cell directly to the left
        return self.lattice_paths(rows - 1, cols) + self.lattice_paths(
            rows, cols - 1
        )


rows = int(input())
cols = int(input())
print(Solution().lattice_paths(rows, cols))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int latticePaths(int rows, int cols) {

            // Base case: If either rows or cols is 0, there is only
            // one unique path (all the way down or all the way right)
            if (rows == 0 || cols == 0) {
                return 1;
            }

            // Recursive case: The number of unique paths to the bottom-right
            // corner is the sum of the unique paths from the cell directly
            // above and the cell directly to the left
            return (
                latticePaths(rows - 1, cols) + latticePaths(rows, cols - 1)
            );
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int rows = Integer.parseInt(sc.nextLine().trim());
        int cols = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().latticePaths(rows, cols));
    }
}
```


<details>
<summary><strong>Trace — rows = 2, cols = 2</strong></summary>

```
paths(2, 2) = paths(1, 2) + paths(2, 1)

paths(1, 2) = paths(0, 2) + paths(1, 1)
            = 1 + (paths(0, 1) + paths(1, 0))
            = 1 + (1 + 1)
            = 3

paths(2, 1) = paths(1, 1) + paths(2, 0)
            = (paths(0, 1) + paths(1, 0)) + 1
            = (1 + 1) + 1
            = 3

paths(2, 2) = 3 + 3 = 6 ✓
```

Same shape as binomial coefficient — different surface meaning, identical math.

</details>

### Complexity Analysis

| Resource | Cost | Why |
|---|---|---|
| **Time (no memo)** | `O(2^(r+c))` | Each frame branches twice; depth is `r + c`. |
| **Time (with memo)** | `O(r · c)` | Each cell computed once. |
| **Space (stack)** | `O(r + c)` | Deepest path reduces both axes to 0. |
| **Space (memo)** | `O(r · c)` | One cache entry per cell. |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Both zero | `rows = 0, cols = 0` | `1` | Both boundaries trigger; the "do-nothing" path. |
| One row | `rows = 0, cols = 5` | `1` | Only one path: all right. |
| One column | `rows = 5, cols = 0` | `1` | Only one path: all down. |
| Symmetric | `rows = 3, cols = 3` | `20` | `C(6, 3) = 20`. |
| Asymmetric | `rows = 2, cols = 4` | `15` | `C(6, 2) = 15`. |
| Large | `rows = 20, cols = 20` | `137846528820` | Memo essential. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Lattice paths is binomial coefficient with a geometric interpretation: every path through the grid corresponds to a way of choosing which moves go right (and the rest go down). The same recurrence appears in dozens of grid-based problems — minimum-cost path, count of obstacle-free paths, etc. The next problem is the most extreme multidimensional recursion in this lesson — a function with such a wild branching structure it isn't even primitive recursive.

</details>
