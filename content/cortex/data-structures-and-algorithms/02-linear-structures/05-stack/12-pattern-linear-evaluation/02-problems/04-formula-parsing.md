---
title: "Formula Parsing"
summary: "Given a chemical formula consisting of single-uppercase atoms (e.g. H, O), positive-integer multipliers, and parentheses for grouping, return a string of ATOM:COUNT separated by spaces, in order of first appearance."
prereqs:
  - 12-pattern-linear-evaluation/01-pattern
difficulty: hard
kind: problem
topics: [linear-evaluation, stack]
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

```quiz
{
  "prompt": "What output does \"(HO)2\" produce?",
  "input": "formula = \"(HO)2\"",
  "options": ["H:1 O:1", "H:2 O:2", "H:2O:2", "HO:2"],
  "answer": "H:2 O:2"
}
```

## Constraints

- `1 ≤ formula.length ≤ 1000`
- `formula` consists of uppercase letters, digits `0–9`, and `(`/`)`
- Atoms are single uppercase letters; no atom appears twice; brackets are balanced

```python run
class Atom:
    def __init__(self, name: str, count: int):
        self.name = name
        self.count = count

class Solution:
    def formula_parsing(self, formula: str) -> str:
        # Your code goes here — push (name,count) records and '(' markers;
        # on ')', read the trailing multiplier, pop the group back to '(',
        # scale each count, push back in order. Format bottom-to-top.
        return formula

formula = input()
print(Solution().formula_parsing(formula))
```

```java run
import java.util.*;
public class Main {
    static class Atom {
        char name; int count;
        Atom(char name, int count) { this.name = name; this.count = count; }
    }
    static class Solution {
        public String formulaParsing(String formula) {
            // Your code goes here — push (name,count) records and '(' markers;
            // on ')', read the trailing multiplier, pop the group back to '(',
            // scale each count, push back in order. Format bottom-to-top.
            return formula;
        }
    }
    public static void main(String[] args) {
        String formula = new Scanner(System.in).nextLine();
        System.out.println(new Solution().formulaParsing(formula));
    }
}
```

```testcases
{
  "args": [
    { "id": "formula", "label": "formula", "type": "string", "placeholder": "(HO)2" }
  ],
  "cases": [
    { "args": { "formula": "(HO)2" }, "expected": "H:2 O:2" },
    { "args": { "formula": "H(N(KO)2)3" }, "expected": "H:1 N:3 K:6 O:6" },
    { "args": { "formula": "KH" }, "expected": "K:1 H:1" },
    { "args": { "formula": "A" }, "expected": "A:1" },
    { "args": { "formula": "A3" }, "expected": "A:3" },
    { "args": { "formula": "(AB)1" }, "expected": "A:1 B:1" },
    { "args": { "formula": "(XY)10" }, "expected": "X:10 Y:10" },
    { "args": { "formula": "A(BC)2D" }, "expected": "A:1 B:2 C:2 D:1" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


This is a **linear-evaluation** problem because the formula is one sequence and a `)` folds the group of atoms built since its matching `(`. Parentheses nest, so an inner group is multiplied before the outer group scales it again. The stack parks `(name, count)` records and `(` markers until a `)` fires the fold.

The stack holds **`(atom, count)` records and `(` markers**, freshest on top. An atom reads its trailing count — defaulting to `1` when no digits follow — and pushes a record. A `(` pushes a marker. When `)` fires, it first reads the multiplier after the `)`, then pops every record back to the marker, scales each popped count by the multiplier, and pushes the records back. Because the popped group is re-pushed in its original order, atoms keep the sequence they first appeared in, which is what the output demands.

A naive approach expands the formula into a flat atom list, then groups and counts — but nested multipliers force repeated re-expansion of inner groups, and a single accumulator cannot hold the suspended outer atoms while an inner group is being scaled. The stack parks each group's records and resumes them when its `)` closes, so one pass scales every atom correctly.

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


Stack of `(name, count)` records, plus a special `(` marker. On `(`: push a marker. On atom: read its trailing count (default 1) and push. On `)`: read the multiplier, pop everything down to the `(` marker, multiply each popped count by the multiplier, push back in original order.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

```python solution time=O(N·D) space=O(N)
class Atom:
    def __init__(self, name: str, count: int):
        self.name = name
        self.count = count

class Solution:
    def formula_parsing(self, formula: str) -> str:
        stack = []
        i = 0
        while i < len(formula):
            if formula[i] == "(":
                stack.append(Atom("(", -1))
            elif formula[i] == ")":
                i += 1
                multiplier = 0
                while i < len(formula) and formula[i].isdigit():
                    multiplier = multiplier * 10 + int(formula[i])
                    i += 1
                if multiplier == 0:
                    multiplier = 1
                i -= 1
                group = []
                while stack and stack[-1].name != "(":
                    group.append(stack.pop())
                if stack and stack[-1].name == "(":
                    stack.pop()
                for atom in reversed(group):
                    stack.append(Atom(atom.name, atom.count * multiplier))
            elif formula[i].isupper():
                atom_name = formula[i]
                i += 1
                count = 0
                while i < len(formula) and formula[i].isdigit():
                    count = count * 10 + int(formula[i])
                    i += 1
                if count == 0:
                    count = 1
                i -= 1
                stack.append(Atom(atom_name, count))
            i += 1
        result = []
        while stack:
            atom = stack.pop()
            result.insert(0, f"{atom.name}:{atom.count}")
        return " ".join(result)

formula = input()
print(Solution().formula_parsing(formula))
```

```java solution
import java.util.*;
public class Main {
    static class Atom {
        char name; int count;
        Atom(char name, int count) { this.name = name; this.count = count; }
    }
    static class Solution {
        public String formulaParsing(String formula) {
            Stack<Atom> stack = new Stack<>();
            for (int i = 0; i < formula.length(); i++) {
                if (formula.charAt(i) == '(') {
                    stack.push(new Atom('(', -1));
                } else if (formula.charAt(i) == ')') {
                    i++;
                    int multiplier = 0;
                    while (i < formula.length() && Character.isDigit(formula.charAt(i))) {
                        multiplier = multiplier * 10 + (formula.charAt(i) - '0');
                        i++;
                    }
                    if (multiplier == 0) multiplier = 1;
                    i--;
                    List<Atom> group = new ArrayList<>();
                    while (!stack.isEmpty() && stack.peek().name != '(') group.add(stack.pop());
                    if (!stack.isEmpty() && stack.peek().name == '(') stack.pop();
                    for (int j = group.size() - 1; j >= 0; j--) {
                        Atom atom = group.get(j);
                        stack.push(new Atom(atom.name, atom.count * multiplier));
                    }
                } else if (Character.isUpperCase(formula.charAt(i))) {
                    char atomName = formula.charAt(i++);
                    int count = 0;
                    while (i < formula.length() && Character.isDigit(formula.charAt(i))) {
                        count = count * 10 + (formula.charAt(i) - '0');
                        i++;
                    }
                    if (count == 0) count = 1;
                    i--;
                    stack.push(new Atom(atomName, count));
                }
            }
            StringBuilder result = new StringBuilder();
            while (!stack.isEmpty()) {
                Atom atom = stack.pop();
                result.insert(0, atom.name + ":" + atom.count + " ");
            }
            if (result.length() > 0) result.setLength(result.length() - 1);
            return result.toString();
        }
    }
    public static void main(String[] args) {
        String formula = new Scanner(System.in).nextLine();
        System.out.println(new Solution().formulaParsing(formula));
    }
}
```

**Dry Run — `formula = "(HO)2"`**

```
'('  marker → push ('(', -1)        → stack: (
'H'  atom, no digit → count 1       → push (H, 1)  → stack: ( H:1
'O'  atom, no digit → count 1       → push (O, 1)  → stack: ( H:1 O:1
')'  trigger → multiplier 2; pop group [O:1, H:1]; discard '(';
              scale ×2, push back in order  → stack: H:2 O:2

end of input → "H:2 O:2" ✓
```

**Complexity**

| Measure | Value | Why |
|---|---|---|
| Time  | **O(N · D)** | One scan of `N` characters; each record can be popped and re-pushed once per enclosing group (nesting depth `D`). |
| Space | **O(N)** | The stack holds one record per atom plus one marker per open group, bounded by the input length. |

**Edge Cases**

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single atom, no count | `A` | `A:1` | One uppercase letter with no trailing digit defaults to count `1`. |
| Atom with count | `A3` | `A:3` | The digit after `A` is slurped as its count `3`. |
| Group multiplier of one | `(AB)1` | `A:1 B:1` | The trailing `1` scales each atom by one — counts unchanged. |
| Multi-digit multiplier | `(XY)10` | `X:10 Y:10` | Both digits read as one multiplier `10`. |
| Atoms around a group | `A(BC)2D` | `A:1 B:2 C:2 D:1` | `A` and `D` stay at `1`; only the group `BC` is doubled, order preserved. |
| Deeply nested | `H(N(KO)2)3` | `H:1 N:3 K:6 O:6` | Inner `(KO)2` scales first, then outer `×3` compounds onto `K` and `O`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Push `(name, count)` records and `(` markers; on `)`, read the trailing multiplier, pop the group back to the marker, and scale every count before pushing the records back in order. The new idea over string expansion is the *record payload* — the fold transforms a whole group of structured items at once, and re-pushing them in original order is what keeps the output in first-appearance sequence.

</details>
