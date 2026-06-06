---
title: "Formula Parsing"
summary: "Given a chemical formula consisting of single-uppercase atoms (e.g. H, O), positive-integer multipliers, and parentheses for grouping, return a string of ATOM:COUNT separated by spaces, in order of fi"
prereqs:
  - 12-pattern-linear-evaluation/01-pattern
difficulty: hard
---

# Formula parsing

## Problem Statement

Given a chemical formula consisting of single-uppercase atoms (e.g. `H`, `O`), positive-integer multipliers, and parentheses for grouping, return a string of `ATOM:COUNT` separated by spaces, in order of first appearance.

> Single-uppercase atoms only (no `Na`, no `Cl` — atoms here are one character each), and no atom appears twice in the input.

### Example 1
> -   **Input:** `"(HO)2"` → **Output:** `"H:2 O:2"`

### Example 2
> -   **Input:** `"H(N(KO)2)3"` → **Output:** `"H:1 N:3 K:6 O:6"`

### Example 3
> -   **Input:** `"KH"` → **Output:** `"K:1 H:1"`

## Examples

**Example 1**
```
Input:  "(HO)2"
Output: "H:2 O:2"
Explanation: inside the group, H:1 and O:1. The trailing 2 multiplies
the whole group, giving H:2 and O:2.
```

**Example 2**
```
Input:  "H(N(KO)2)3"
Output: "H:1 N:3 K:6 O:6"
Explanation: H:1 stays outside. (KO)2 → K:2 O:2. The outer group
N:1 K:2 O:2 is multiplied by 3 → N:3 K:6 O:6.
```

**Example 3**
```
Input:  "KH"
Output: "K:1 H:1"
Explanation: two atoms, no parentheses, no counts. Each defaults to 1,
reported in first-appearance order.
```

**Example 4**
```
Input:  "(XY)10"
Output: "X:10 Y:10"
Explanation: the multiplier is multi-digit. Both digits read as one
number 10, scaling X:1 and Y:1 to X:10 and Y:10.
```


<details>
<summary><h2>Intuition</h2></summary>


This is a **linear-evaluation** problem because the formula is one sequence and a `)` folds the group of atoms built since its matching `(`. Parentheses nest, so an inner group is multiplied before the outer group scales it again. The stack parks `(name, count)` records and `(` markers until a `)` fires the fold.

The stack holds **`(atom, count)` records and `(` markers**, freshest on top. An atom reads its trailing count — defaulting to `1` when no digits follow — and pushes a record. A `(` pushes a marker. When `)` fires, it first reads the multiplier after the `)`, then pops every record back to the marker, scales each popped count by the multiplier, and pushes the records back. Because the popped group is re-pushed in its original order, atoms keep the sequence they first appeared in, which is what the output demands.

A naive approach expands the formula into a flat atom list, then groups and counts — but nested multipliers force repeated re-expansion of inner groups, and the intermediate list can blow up. A single accumulator cannot hold the suspended outer atoms while an inner group is being scaled. The stack parks each group's records and resumes them when its `)` closes, so one pass scales every atom correctly. The "no repeated atom" guarantee means a fold never has to merge two counts for the same name.

<!-- VERIFY: the baselined Approach <details> claims first-appearance order is kept "by tracking each atom's earliest index in a separate map", but the frozen solution has no such map — order is preserved implicitly because popped groups are re-pushed in original order via reversed(group), and the "no repeated atom" precondition removes any need to merge. Prose here is aligned to the code; the <details> block is left frozen. -->

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Formula Parsing |
|---|---|
| **Q1.** Is the input a single linear sequence scanned once? | **Yes** — one left-to-right walk over the formula characters. |
| **Q2.** Does some token defer work — open a group awaiting a closer? | **Yes** — every `(` opens a group whose multiplier and scaling wait for its matching `)`. |
| **Q3.** Does a trigger fold only the *most recent* pending chunk? | **Yes** — `)` scales the records back to the nearest `(`, which is always on top. |
| **Q4.** Is the answer read off the stack at end-of-input? | **Yes** — the surviving records, formatted bottom-to-top, are the `ATOM:COUNT` string. |

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Push `(name, count)` records and `(` markers; on `)`, scale the popped group by the trailing multiplier.

1. **Initialise an empty stack** of `(name, count)` records, using a sentinel record to mark each `(`.
2. **Walk the formula left to right.**
3. **`(` → push a marker** record onto the stack.
4. **Uppercase atom → read its count.** Take the letter, slurp any consecutive digits as its count, default to `1` when none follow, and push a `(name, count)` record.
5. **`)` → read the multiplier.** Slurp the digits after the `)` into the group multiplier, defaulting to `1` when none follow.
6. **Pop the group back to the marker.** Collect every record above the `(` marker, then discard the marker.
7. **Scale and push back.** Multiply each popped count by the multiplier and push the records back in their original order, so nesting composes.
8. **After the pass, format the stack** bottom-to-top as space-separated `ATOM:COUNT` and return it.

</details>
<details>
<summary><h2>Approach</h2></summary>


Stack of `(name, count)` records, plus a special `(` marker. On `(`: push a marker. On atom: read its trailing count (default 1) and push. On `)`: read the multiplier, pop everything down to the `(` marker, multiply each popped count by the multiplier, push back.

The "first appearance order" requirement is satisfied because we never re-order: by tracking each atom's earliest index in a separate map, we can sort the final stack by that.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=stack viz-kind=stack
from typing import List

# Define a class to hold atom information
class Atom:
    def __init__(self, name: str, count: int):
        self.name = name
        self.count = count

class Solution:
    def formula_parsing(self, formula: str) -> str:

        # Stack to store atoms, counts, and group markers
        stack: List[Atom] = []

        i = 0
        while i < len(formula):

            # If the current character is '(', push it to mark the start
            # of a group
            if formula[i] == "(":
                stack.append(Atom("(", -1))

            # If the current character is ')', process the group
            elif formula[i] == ")":
                i += 1

                # Read multiplier (if any)
                multiplier = 0
                while i < len(formula) and formula[i].isdigit():
                    multiplier = multiplier * 10 + int(formula[i])
                    i += 1

                if multiplier == 0:
                    multiplier = 1

                # adjust index because loop will increment
                i -= 1
  

                # Collect atoms in the group
                group: List[Atom] = []
                while stack and stack[-1].name != "(":
                    group.append(stack.pop())

                # Remove the '(' from the stack
                if stack and stack[-1].name == "(":
                    stack.pop()

                # Multiply counts and push back
                for atom in reversed(group):
                    stack.append(
                        Atom(atom.name, atom.count * multiplier)
                    )

            # If the character is an uppercase atom
            elif formula[i].isupper():
                atom_name = formula[i]
                i += 1

                # Read count (if any)
                count = 0
                while i < len(formula) and formula[i].isdigit():
                    count = count * 10 + int(formula[i])
                    i += 1

                if count == 0:
                    count = 1

                # adjust index because loop will increment
                i -= 1
  

                # Push atom with count
                stack.append(Atom(atom_name, count))

            i += 1

        # Collect the final result from the stack
        result: List[str] = []
        while stack:
            atom = stack.pop()
            result.insert(0, f"{atom.name}:{atom.count}")

        return " ".join(result)


# Examples from the problem statement
print(Solution().formula_parsing("(HO)2"))       # H:2 O:2
print(Solution().formula_parsing("H(N(KO)2)3"))  # H:1 N:3 K:6 O:6
print(Solution().formula_parsing("KH"))          # K:1 H:1

# Edge cases
print(Solution().formula_parsing("A"))           # A:1 — single atom no count
print(Solution().formula_parsing("A3"))          # A:3
print(Solution().formula_parsing("(AB)1"))       # A:1 B:1
print(Solution().formula_parsing("(XY)10"))      # X:10 Y:10 — multi-digit multiplier
print(Solution().formula_parsing("A(BC)2D"))     # A:1 B:2 C:2 D:1
```

```java run viz=array viz-root=stack viz-kind=stack
import java.util.*;

public class Main {
    // Define a class to hold atom information
    static class Atom {
        char name;
        int count;

        Atom(char name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    static class Solution {
        public String formulaParsing(String formula) {

            // Stack to store atoms, counts, and group markers
            Stack<Atom> stack = new Stack<>();

            for (int i = 0; i < formula.length(); i++) {

                // If the current character is '(', push it to mark the start
                // of a group
                if (formula.charAt(i) == '(') {
                    stack.push(new Atom('(', -1));
                }

                // If the current character is ')', process the group
                else if (formula.charAt(i) == ')') {

                    // Move past ')', check for multiplier
                    i++;

                    // Read multiplier (if any)
                    int multiplier = 0;
                    while (
                        i < formula.length() &&
                        Character.isDigit(formula.charAt(i))
                    ) {
                        multiplier =
                            multiplier * 10 + (formula.charAt(i) - '0');
                        i++;
                    }

                    // If no multiplier, default to 1
                    if (multiplier == 0) multiplier = 1;

                    // adjust index because loop will increment
                    i--;

                    // Collect atoms in the group
                    List<Atom> group = new ArrayList<>();
                    while (!stack.isEmpty() && stack.peek().name != '(') {
                        group.add(stack.pop());
                    }

                    // Remove the '(' from the stack
                    if (!stack.isEmpty() && stack.peek().name == '(') {
                        stack.pop();
                    }

                    // Multiply counts and push back
                    for (int j = group.size() - 1; j >= 0; j--) {
                        Atom atom = group.get(j);
                        stack.push(
                            new Atom(atom.name, atom.count * multiplier)
                        );
                    }
                }

                // If the character is an uppercase atom
                else if (Character.isUpperCase(formula.charAt(i))) {
                    char atomName = formula.charAt(i++);

                    // Read count (if any)
                    int count = 0;
                    while (
                        i < formula.length() &&
                        Character.isDigit(formula.charAt(i))
                    ) {
                        count = count * 10 + (formula.charAt(i) - '0');
                        i++;
                    }

                    // If no count, default to 1
                    if (count == 0) count = 1;

                    // adjust index because loop will increment
                    i--;

                    // Push atom with count
                    stack.push(new Atom(atomName, count));
                }
            }

            // Collect the final result from the stack
            StringBuilder result = new StringBuilder();
            while (!stack.isEmpty()) {
                Atom atom = stack.pop();
                result.insert(0, atom.name + ":" + atom.count + " ");
            }

            if (result.length() > 0) {
                result.setLength(result.length() - 1);
            }

            return result.toString();
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().formulaParsing("(HO)2"));       // H:2 O:2
        System.out.println(new Solution().formulaParsing("H(N(KO)2)3"));  // H:1 N:3 K:6 O:6
        System.out.println(new Solution().formulaParsing("KH"));          // K:1 H:1

        // Edge cases
        System.out.println(new Solution().formulaParsing("A"));           // A:1
        System.out.println(new Solution().formulaParsing("A3"));          // A:3
        System.out.println(new Solution().formulaParsing("(AB)1"));       // A:1 B:1
        System.out.println(new Solution().formulaParsing("(XY)10"));      // X:10 Y:10
        System.out.println(new Solution().formulaParsing("A(BC)2D"));     // A:1 B:2 C:2 D:1
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Three lessons:

1. **The stack holds partial answers in progress.** Whenever a closer event fires, you collapse a chunk of the stack into a single combined value and push that back. The result keeps growing until the next closer or end of input.
2. **Indices, characters, strings, or records — push whatever the problem needs.** Path tokens for path simplification; characters for bracket reversal; (atom, count) records for chemical formulas. The container shape adapts; the stack discipline doesn't.
3. **Multi-digit numbers and multi-character tokens need a sub-loop.** Inside the main scan, slurp consecutive digits or letters before pushing — otherwise `12[a]` will push `1`, `2`, `[`, `a`, `]` and you'll lose the multiplier.

> *Coming up — the **design** lesson. We've built five problem patterns; the final lesson takes the stack interface and asks: <em>what would it take to extend it with one extra O(1) operation, like <code>min()</code>?</em> Two classic interview questions — Min Stack (push, pop, top, min — all O(1)) and Stack Using Queues — close out the section by demonstrating how to <em>compose stacks with auxiliary structures</em> to add new functionality without losing the original O(1) guarantees.*

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `formula = "(HO)2"`. The stack holds `(name, count)` records and a `(` marker; on `)`, the popped group is scaled by the trailing multiplier:

```
formula = "(HO)2"

'('  marker → push ('(', -1)        → stack (bottom→top): (
'H'  atom, no digit → count 1       → push (H, 1)  → stack: ( H:1
'O'  atom, no digit → count 1       → push (O, 1)  → stack: ( H:1 O:1
')'  trigger → multiplier 2; pop group [O:1, H:1]; discard '(';
              scale ×2, push back in order  → stack: H:2 O:2

end of input → format bottom-to-top → "H:2 O:2" ✓
```

A trace on `formula = "H(N(KO)2)3"` shows nested scaling — the inner group multiplies before the outer:

```
'H' → push H:1                                  → stack: H:1
'(' → push marker                               → stack: H:1 (
'N' → push N:1                                  → stack: H:1 ( N:1
'(' → push marker                               → stack: H:1 ( N:1 (
'K' → push K:1 ; 'O' → push O:1                 → stack: H:1 ( N:1 ( K:1 O:1
')2' → pop [O:1,K:1]; ×2; push back             → stack: H:1 ( N:1 K:2 O:2
')3' → pop [O:2,K:2,N:1]; ×3; push back          → stack: H:1 N:3 K:6 O:6

end of input → "H:1 N:3 K:6 O:6" ✓
```

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N · D)** | One scan of `N` characters; each record can be popped and re-pushed once per enclosing group, up to nesting depth `D`. |
| Space | **O(N)** | The stack holds one record per atom plus one marker per open group, bounded by the input length. |

The time is `O(N · D)`, where `N` is the formula length and `D` is the maximum parenthesis nesting depth: the scan reads each character once, but an atom inside `D` nested groups is popped and re-pushed once at each enclosing `)`, so a record is touched up to `D` times. The space is `O(N)`: the stack stores at most one `(name, count)` record per atom and one marker per unclosed `(`, both bounded by the number of input characters. With the "no repeated atom" guarantee, the number of distinct records never exceeds the atom count.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single atom, no count | `A` | `A:1` | One uppercase letter with no trailing digit defaults to count `1`. |
| Atom with count | `A3` | `A:3` | The digit after `A` is slurped as its count `3`. |
| Group multiplier of one | `(AB)1` | `A:1 B:1` | The trailing `1` scales each atom by one — counts unchanged. |
| Multi-digit multiplier | `(XY)10` | `X:10 Y:10` | Both digits read as one multiplier `10`, scaling each atom to ten. |
| Atoms around a group | `A(BC)2D` | `A:1 B:2 C:2 D:1` | `A` and `D` stay at `1`; only the group `BC` is doubled, and order is preserved. |
| Deeply nested | `H(N(KO)2)3` | `H:1 N:3 K:6 O:6` | Inner `(KO)2` scales first, then the outer `×3` compounds onto `K` and `O`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Push `(name, count)` records and `(` markers; on `)`, read the trailing multiplier, pop the group back to the marker, and scale every count before pushing the records back in order. The new idea over string expansion is the *record payload* — the fold transforms a whole group of structured items at once, and re-pushing them in original order is what keeps the output in first-appearance sequence.

</details>