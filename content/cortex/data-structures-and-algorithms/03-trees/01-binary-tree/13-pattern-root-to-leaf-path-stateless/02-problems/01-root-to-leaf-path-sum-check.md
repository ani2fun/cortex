---
title: "Root to Leaf Path (Sum Check)"
summary: "See problem statement below."
prereqs:
  - 13-pattern-root-to-leaf-path-stateless/01-pattern
difficulty: medium
---

# Problem 1 — Root to leaf path (sum check)

> Return `true` if there exists at least one root-to-leaf path whose node values sum to `target`.

Already covered in the generic skeleton. The accumulator is the *remaining target after subtracting nodes seen*; the verdict at a leaf is *"is remaining exactly 0?"*; the combine is `OR`.

The implementation is exactly the generic template — see the code block above.
