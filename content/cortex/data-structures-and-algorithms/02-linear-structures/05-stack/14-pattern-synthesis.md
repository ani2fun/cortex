---
title: "Pattern Synthesis: Stack"
summary: "When to use which pattern, common confusions, and synthesis problems for stack."
prereqs:
  - 02-linear-structures/05-stack/12-pattern-linear-evaluation/03-memorize
---

# Pattern Synthesis: Stack

Every stack pattern in this chapter rides the same primitive: `push`, `pop`, `peek` on one end. Each reads the stack for a different reason. The hard part is not learning the five in isolation. It is choosing the right one when a fresh problem lands and three of them look plausible at once. This synthesis chapter exists for that moment: read the problem, run the decision tree, confirm with the comparison table, then write code.

The five patterns in scope:

- **Reversal** — push every item, pop every item; the pop order is the input backwards, so the stack reverses for free.
- **Previous Closest Occurrence** — sweep left-to-right with a monotonic stack; the survivor on top is each element's nearest greater/smaller *predecessor*.
- **Next Closest Occurrence** — the mirror; sweep with a monotonic stack of indices and each new element resolves the stacked indices it dominates, fixing their nearest *successor*.
- **Sequence Validation** — push unmatched openers; on each closer match-and-pop the freshest opener; a string is well-formed only if the stack ends empty.
- **Linear Evaluation** — scan once, push data and pending context; on a trigger fold the freshest chunk into one combined value and push it back; read the answer off the final stack.

---

## When to Use Which Pattern

The first question is *what the stack's payload means*, because that single answer splits all five patterns into three families. A stack of raw items waiting to be drained is reversal. A stack of monotone *candidates* compared by value is one of the two closest-occurrence patterns. A stack of *unmatched context* keyed off bracket-like delimiters is either validation or evaluation. The second question — what you do on a pop — picks the leaf.

Walk the tree top to bottom; stop at the first match.

1. **Does the stack hold raw items you push unconditionally and later drain in full, reading the *pop order* as the answer?**
   - The output is the input in reverse — string, array, words, or a stack-to-stack transfer → **Reversal**. The stack never compares two items; it only reverses them.

2. **Does the stack hold a monotone chain of *values* (or their index proxies), where a pop is triggered by a *value comparison* and the answer is the nearest greater/smaller neighbour?**
   - The neighbour you want lives to each element's **left**, in the prefix already scanned → **Previous Closest Occurrence**. You *peek* the survivor after the pop run.
   - The neighbour you want lives to each element's **right**, in the suffix still to come → **Next Closest Occurrence**. You *resolve on pop* — the arriving element is the answer for everything it dominates. Rainwater and largest-rectangle are this branch, aggregating an area instead of recording one value.

3. **Does the stack hold *unmatched context* keyed off bracket-like delimiters, where a pop is triggered by a *closer token*?**
   - A closer only *matches and pops* one opener, and the answer is whether the stack ends empty (or a count or span derived from it) → **Sequence Validation**. Nothing is ever pushed back.
   - A closer *folds* the popped chunk into one combined value and pushes it back, and the answer is built from the final stack contents → **Linear Evaluation**. The fold *produces* a token; validation only removes one.

4. If nothing above matches, the problem is probably outside this chapter — try hashing, a queue, or a different structure entirely.

To make this concrete, run the tree on one statement per pattern and confirm the branches resolve cleanly:

- **"Reverse the order of words in `s`."** → Step 1 yes; push whole words, drain, read the pop order → **Reversal**.
- **"For each day, how many days since the last warmer day?"** → Step 2 yes; nearest-greater in the **prefix** → **Previous Closest Occurrence**.
- **"For each bar, the next taller bar to its right."** → Step 2 yes; nearest-greater in the **suffix** → **Next Closest Occurrence**.
- **"Is this bracket string balanced?"** → Step 3 yes; closer match-and-pops, demand empty stack → **Sequence Validation**.
- **"Decode `3[a2[c]]` into its expanded string."** → Step 3 yes; closer folds the chunk and pushes the combined value back → **Linear Evaluation**.

So the key idea is: the *payload* narrows to one of three families — raw items, monotone values, or bracket-keyed context. Then the *pop trigger* — drain-all, value comparison, or closer token — plus the direction-or-action picks the exact pattern.

---

## Pattern Comparison Table

All five patterns at a glance. `N` is the input length. `n` is the array length in the monotonic patterns. `M` is the materialised output length when a linear-evaluation fold expands the input, as in string expansion.

| Pattern | Problem shape | Key signal | Time | Space | Confused with |
|---|---|---|---|---|---|
| **Reversal** | Push every item, drain the stack, read the pop order as the reversed input | "Reverse the string/array using a stack", "invert this stack", "reverse word order", reversal as a sub-step of a larger LIFO algorithm | `O(N)` | `O(N)` | Monotonic stack (pop everything in order vs pop selectively on a comparison?); Linear Evaluation (drain-and-read vs fold-and-push-back?) |
| **Previous Closest Occurrence** | Monotonic stack swept left-to-right; peek the surviving top for each element's nearest greater/smaller **predecessor** | "Nearest greater/smaller to the **left**", stock span, "days since the last warmer day", nearest-taller-bar-to-the-left | `O(n)` amortised | `O(n)` | Next Closest (predecessor in the prefix vs successor in the suffix?); Sequence Validation (value comparison vs type match?) |
| **Next Closest Occurrence** | Monotonic stack of indices; each element resolves on pop the stacked indices it dominates, fixing their nearest greater/smaller **successor** | "Nearest greater/smaller to the **right**", daily temperatures, trapping rain water, largest rectangle in a histogram | `O(n)` amortised | `O(n)` | Previous Closest (successor in the suffix vs predecessor in the prefix?); Sequence Validation (value comparison vs type match?) |
| **Sequence Validation** | Push unmatched openers; closer match-and-pops the freshest; answer is final emptiness, an edit count, or a span | "Is this bracket/tag expression valid", "minimum insertions to balance", "redundant parentheses", "longest valid parentheses substring" | `O(N)` worst, `O(1)` best | `O(N)` worst, `O(1)` best | Monotonic stack (type match vs value comparison?); Linear Evaluation (match-and-pop vs fold-and-push-back?) |
| **Linear Evaluation** | Push data and pending context; a trigger folds the freshest chunk into one value pushed back; answer is the final stack | "Simplify this UNIX path", "decode `k[...]`", "reverse substrings inside brackets", "count atoms in a formula" | `O(N)` (or `O(M)` on expansion) | `O(N)` (or `O(M)` on expansion) | Sequence Validation (fold-and-push-back vs match-and-pop?); Reversal (fold-on-trigger vs drain-and-read?) |

---

## Common Confusions

Four pairs trip readers up most often. Each subsection states the surface similarity, the one question that cuts through it, and the symptom that betrays a wrong choice.

### Previous Closest vs Next Closest (scan direction)

**Why they look the same.** Both run the *identical* pop-peek/resolve-push monotonic stack, both prove `O(n)` time and `O(n)` space by the same amortised argument, and both answer "nearest greater/smaller neighbour" with the same comparison operator. The skeleton is byte-for-byte similar; only the direction differs.

**The distinguishing test.** Does each element's answer live in the **prefix already scanned** (previous) or the **suffix not yet seen** (next)?

**Telltale symptom of wrong choice.** Your answers consistently land on the wrong *side* of each element — you wanted predecessors but built a forward-resolving stack, so every answer sits to the right instead of the left (or the reverse). The fix is direction, not logic: previous peeks the survivor after popping; next resolves each popped index against the arriving element.

### Monotonic Stack vs Sequence Validation (what triggers a pop)

**Why they look the same.** Both push and pop a single stack while scanning a sequence once, and both finish in `O(N)`/`O(n)` time. From a distance, "scan left-to-right, sometimes pop" describes either one.

**The distinguishing test.** Is a pop triggered by a **value comparison** (the top loses a `>`/`<` test to the current element) or by a **type match** (a closer token matches the opener on top)?

**Telltale symptom of wrong choice.** You compared element *values* to decide a pop but the input was brackets — so `"([)]"` slips through because nothing checks that `)` matches the freshest `[`. Conversely, you demanded an empty stack at the end of a nearest-greater scan: a monotonic stack never cares whether it ends empty, and the leftovers are the elements with no qualifying neighbour, not an error.

### Sequence Validation vs Linear Evaluation (match vs fold)

**Why they look the same.** Both scan a string once, both push on openers and pop on bracket-like closers, and both maintain "newest unmatched context on top." The push side is indistinguishable.

**The distinguishing test.** On a closer, does the stack *match-and-pop one opener* and push nothing (validation), or does it *fold the popped chunk into a new value and push that value back* (evaluation)?

**Telltale symptom of wrong choice.** You only matched-and-popped on each trigger and read the final emptiness — but the problem wanted a *built* answer, so the computed string or number is gone. Conversely, you folded chunks and pushed combined values back for a pure validity check: now you are paying to build a result the question never asked for, and the empty-stack signal is buried under the partial values you stacked.

### Reversal vs Linear Evaluation (drain-and-read vs fold-on-trigger)

**Why they look the same.** Both push items and later pop them, both run `O(N)` time and `O(N)` space, and both "read the stack to produce output." A pure reversal can even *look* like an evaluation whose only operation is "emit in reverse."

**The distinguishing test.** Does the stack drain **unconditionally in full**, with the pop order itself being the answer (reversal), or does a **trigger token** collapse only the freshest chunk mid-scan into a combined value pushed back (evaluation)?

**Telltale symptom of wrong choice.** You drained the whole stack at the end for an input with nested brackets — so `3[ab]` comes out as the reversed raw characters instead of `ababab`, because no `]` ever fired a fold. Conversely, you wrote a fold-and-push-back loop for "reverse this array": there is no trigger token and no nesting, so the machinery sits idle while a plain two-pass drain would have done it.

---

## Synthesis Problems

Three problems where two or more patterns *seem* to fit. The right call is non-obvious from the statement alone; the wrong call costs you correctness or a complexity tier.

### Problem 1 — Reverse the Order of Words

**Statement.** Given a string `s` of space-separated words, return the words in reverse order with the letters inside each word intact. (See [Reverse Word Order](08-pattern-reversal/02-problems/04-reverse-word-order.md).)

**Why multiple patterns seem viable.**

- **Linear Evaluation** seems viable because a space *looks like* a trigger token — read a word, hit a space, "fold" the word into output. The scan-and-fold framing fits superficially.
- **Reversal** is the actual winner — tokenise into words, push each word, pop them all.

**Winner.** Reversal, with the *word* as the unit pushed rather than the character.

**Why winner wins.** The answer is the pop order, full stop — there is no nested context to park and no chunk to combine, which is exactly what linear evaluation exists for. A space here is a *delimiter between equal-rank tokens*, not a closer that suspends an outer computation; nothing waits on it. Reversal pushes every word unconditionally and drains in full, and the LIFO contract delivers reverse word order for free, in `O(N)` time and `O(N)` space. Reaching for linear evaluation builds a fold machine with nothing to fold: the `combine` step would be the identity, the marker stack would stay flat, and you would have written a more complex reverser. The decisive signal is "drain-all-and-read-the-order," which is reversal's fingerprint, not evaluation's.

### Problem 2 — Largest Rectangle in a Histogram

**Statement.** Given an array of bar heights of unit width, return the area of the largest axis-aligned rectangle that fits under the skyline. (See [Largest Rectangle Area](10-pattern-next-closest-occurrence/02-problems/07-largest-rectangle-area.md).)

**Why multiple patterns seem viable.**

- **Previous Closest Occurrence** seems viable because each bar's rectangle is bounded on the left by the nearest *shorter* bar to its left — a textbook previous-smaller query.
- **Next Closest Occurrence** seems viable for the symmetric reason — the right boundary is the nearest shorter bar to the right.
- **Next Closest Occurrence** is the actual winner, computed in a single pass.

**Winner.** Next Closest Occurrence — one increasing monotonic stack of indices, popping on the next-smaller event and computing a strip area at each pop.

**Why winner wins.** The width of a bar's rectangle is `nextSmaller − prevSmaller − 1`, so a naive reading suggests *two* passes — one previous-smaller and one next-smaller. The single-pass insight collapses both: when an arriving shorter bar pops index `top`, the bar being popped is bounded on the *right* by the current index and on the *left* by the new stack top, so both boundaries are read at the moment of the pop. Running a separate previous-closest pass is not wrong, but it duplicates work the next-closest resolve already exposes. The pattern fits because the pop event is a *value comparison* (current `<` stacked) that *aggregates an area* — exactly the area-axis variant of next-closest — and it lands at `O(n)` time and `O(n)` space. Previous-closest alone cannot finish the job: it never learns the right boundary, so it computes half the rectangle.

### Problem 3 — Reverse the Substrings Inside Brackets

**Statement.** Given a string of letters and `[`/`]` pairs, reverse the substring inside each bracket pair and return the result; brackets nest. For `s = "a[bcd]e"` the answer is `"adcbe"`. (See [Bracketed Reversal](12-pattern-linear-evaluation/02-problems/02-bracketed-reversal.md).)

**Why multiple patterns seem viable.**

- **Reversal** seems viable because the core operation *is* reversal — each bracketed span is handed back in reverse, which is the stack's free trick.
- **Sequence Validation** seems viable because the input is bracket-paired and you scan it once, pushing on `[` and popping on `]` — the validation skeleton verbatim.
- **Linear Evaluation** is the actual winner.

**Winner.** Linear Evaluation — push characters and `[` markers; on `]`, pop back to the marker *while appending*, which reverses the chunk for free, then push the reversed substring back so an enclosing `]` can fold across it.

**Why winner wins.** Plain reversal flips the *whole* string, not each bracketed span in place, and it has no way to respect nesting — the back half of an outer pair must reverse *after* its inner pair already did, which a single drain cannot order. Sequence validation gets the push/pop rhythm right but throws the payload away: it pops one opener and pushes nothing, so the reversed substring it uncovered is discarded instead of carried into the outer span. Linear evaluation is purpose-built for this — the `]` trigger *folds* the chunk (here the fold is "reverse it") into one combined token and pushes it back, so nesting composes innermost-first with no recursion. The bracketed-reversal twist is that the fold's combine step is literally `chunk = stack.pop() + chunk` reversed into pop order, which builds the reversed substring as a side effect of the drain. Cost: `O(N)` time, `O(N)` space. The split that decides it is whether the trigger *builds and pushes back* (evaluation) or *only matches and discards* (validation) — and this problem needs the built value.

---

## Stack-Specific Synthesis

Every pattern in this chapter is one stack with three operations. The *meaning of a pop* is what separates them, and that meaning falls into three buckets. Read the pop, and the pattern names itself.

The three pop-meanings:

- **Drain pop (reversal).** A pop is "emit the next item," and the emit order *is* the answer. No condition gates it; the stack is a holding area, not a decision-maker. The work is two unconditional passes.
- **Comparison pop (monotonic stacks).** A pop is "this candidate is now dominated and can never be an answer again," triggered by a `>`/`<` test against the arriving value. Previous-closest *peeks* the survivor; next-closest *resolves* the popped index against the arrival. The amortised `O(n)` bound — every element pushed once, popped at most once — is the shared backbone.
- **Delimiter pop (validation and evaluation).** A pop is triggered by a *closer token*, never a value. Validation's pop *discharges* one matched opener and pushes nothing; evaluation's pop *folds* a chunk into a combined value and pushes it back. Both keep "newest unmatched context on top," and both read the final stack — but one reads emptiness, the other reads contents.

So the tradeoff across the chapter is uniform: you trade *what the stack remembers* for *what a pop decides*. Reversal remembers raw order and decides nothing. The monotonic stacks remember a monotone frontier and decide dominance by value. The delimiter patterns remember unmatched context and decide on a closer — match-only for validation, fold-and-rebuild for evaluation. Naming the pop-meaning is faster than matching surface syntax, because three of these patterns push on near-identical triggers and only diverge at the pop.

---

## How to Use This Chapter

Treat the decision tree as the first pass and the comparison table as the second. If both still point to two viable patterns, walk the Common Confusions for the matching pair and run its distinguishing test on the input. The recurring trap is that the *push* side looks the same across patterns. Push items, push openers, push context — the surface notation rarely separates them. The pop side is where they split.

For stacks specifically, two questions decide almost every call:

- **What does the stack's payload mean?** Raw items, monotone values, or bracket-keyed context.
- **What triggers a pop?** Draining everything, a value comparison, or a closer token.

Every other choice follows from those two. When a synthesis-style problem feels like every pattern fits, that is the signal to name the pop-meaning before you write a line.
