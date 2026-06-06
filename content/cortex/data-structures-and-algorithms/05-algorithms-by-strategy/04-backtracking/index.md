# Backtracking

Backtracking is brute force in tree form. Every problem with the shape "list every X" or "find one X that satisfies the constraints" can be cast as a depth-first walk over a state space tree, and the algorithm becomes a recursive function with a `for` loop, a base case for leaves, and an undo step in the loop body. Three patterns cover almost every backtracking problem you'll meet: enumerate without filtering (the Unconditional Enumeration lesson), enumerate with pruning (the Conditional Enumeration lesson), or search a configuration of the world for one that satisfies all constraints (the Backtracking Search lesson).

> How to read this chapter: follow **Start Here** top to bottom — that is the teaching path. Drill it in **Practice** once it clicks.

## Start Here — the learning path

- [Introduction to Backtracking](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-introduction-to-backtracking)
- [Pattern: Unconditional Enumeration](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-unconditional-enumeration-pattern)
- [Pattern: Conditional Enumeration](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-conditional-enumeration-pattern)
- [Pattern: Backtracking Search](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-backtracking-search-pattern)

## Practice

Do these after the matching pattern in Start Here.

### Unconditional Enumeration
- [Unique Subsets](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-unconditional-enumeration-problems-unique-subsets)
- [Case Transformations](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-unconditional-enumeration-problems-case-transformations)
- [Number Sequence](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-unconditional-enumeration-problems-number-sequence)
- [Phone Combinations](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-unconditional-enumeration-problems-phone-combinations)

### Conditional Enumeration
- [Generate Parentheses](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-conditional-enumeration-problems-generate-parentheses)
- [Target Sum Combinations](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-conditional-enumeration-problems-target-sum-combinations)
- [Generate IP Addresses](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-conditional-enumeration-problems-generate-ip-addresses)
- [String Permutations](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-conditional-enumeration-problems-string-permutations)

### Backtracking Search
- [Rat in a Maze](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-backtracking-search-problems-rat-in-a-maze)
- [Word Quest](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-backtracking-search-problems-word-quest)
- [Solve N Queens](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-backtracking-search-problems-solve-n-queens)
- [Solve Sudoku](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-backtracking-search-problems-solve-sudoku)
