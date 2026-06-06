---
title: "Diameter of Tree"
summary: "See problem statement below."
prereqs:
  - 12-pattern-postorder-traversal-stateful/01-pattern
difficulty: medium
---

# Problem 1 — Diameter of tree

> The diameter is the longest *path* (in edges) between any two nodes. The path may pass through any node — not necessarily the root.

Already covered in the generic skeleton above. Each call returns *height* (number of nodes downward); each call updates `best = max(best, leftHeight + rightHeight)` (path edges through this node). Final answer is the global `best`.

The implementation is exactly the generic template. The lesson here is *what to choose* as the feed-up vs the global, not how to type the code.
