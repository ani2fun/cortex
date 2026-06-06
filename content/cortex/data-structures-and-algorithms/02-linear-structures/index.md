# Linear Structures

Data laid out **in a row**. Every structure here gives you a sequence of values you can walk from one end to the other. The differences are in *how* they support insertion, lookup, and removal — and those differences cascade through every higher-level data structure you'll meet later.

> How to read this chapter: follow **Start Here** top to bottom — that is the teaching path. Drill it in **Practice** once it clicks. Use **Reference** (recall cards, synthesis, design) for review, not first reading.

## Start Here — the learning path

- [Arrays](/cortex/data-structures-and-algorithms/linear-structures-arrays-what-is-an-array)
- [Dynamic Arrays](/cortex/data-structures-and-algorithms/linear-structures-arrays-dynamic-arrays)
- [Multidimensional Arrays](/cortex/data-structures-and-algorithms/linear-structures-arrays-multidimensional)
- [Pattern: Two Pointers](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-pattern)
- [Pattern: Two Pointers Reduction](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-reduction-pattern)
- [Pattern: Two Pointers Subproblem](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-subproblem-pattern)
- [Pattern: Simultaneous Traversal](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-simultaneous-traversal-pattern)
- [Pattern: Fixed Sliding Window](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-fixed-sliding-window-pattern)
- [Pattern: Variable Sliding Window](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-variable-sliding-window-pattern)
- [Pattern: Interval Merging](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-interval-merging-pattern)
- [Pattern: Maximum Overlap](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-maximum-overlap-pattern)
- [Linked Lists](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-what-is-a-linked-list)
- [Detecting a Cycle in a Singly Linked List](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-detecting-cycle-in-singly-linked-lists)
- [Pattern: Reversal](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-pattern)
- [Pattern: Reversal as a Subproblem](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-subproblem-pattern)
- [Pattern: Sliding-Window Traversal](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-sliding-window-traversal-pattern)
- [Pattern: Fast & Slow Pointers](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-fast-and-slow-pointers-pattern)
- [Pattern: Split](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-split-pattern)
- [Pattern: Merge](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-merge-pattern)
- [Pattern: Reorder](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reorder-pattern)
- [Doubly & Circular Linked Lists](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-doubly-linked-lists)
- [Pattern: Reversal](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-pattern)
- [Pattern: Reversal as a Subproblem](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-subproblem-pattern)
- [Pattern: Two Pointers](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-two-pointers-pattern)
- [Pattern: Reorder](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reorder-pattern)
- [Stacks](/cortex/data-structures-and-algorithms/linear-structures-stack-what-is-a-stack)
- [Infix, Postfix, and Prefix Notations](/cortex/data-structures-and-algorithms/linear-structures-stack-infix-postfix-and-prefix-notations)
- [Evaluating Expressions Using a Stack](/cortex/data-structures-and-algorithms/linear-structures-stack-evaluating-expressions-using-stack)
- [Converting Expressions Using a Stack](/cortex/data-structures-and-algorithms/linear-structures-stack-converting-expressions-using-stack)
- [Pattern: Reversal](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-reversal-pattern)
- [Pattern: Previous Closest Occurrence](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-previous-closest-occurrence-pattern)
- [Pattern: Next Closest Occurrence](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-next-closest-occurrence-pattern)
- [Pattern: Sequence Validation](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-sequence-validation-pattern)
- [Pattern: Linear Evaluation](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-linear-evaluation-pattern)
- [Queues](/cortex/data-structures-and-algorithms/linear-structures-queue-what-is-a-queue)
- [Hash Tables](/cortex/data-structures-and-algorithms/linear-structures-hash-table-what-is-a-hash-table)
- [Separate Chaining](/cortex/data-structures-and-algorithms/linear-structures-hash-table-separate-chaining)
- [Linear Probing](/cortex/data-structures-and-algorithms/linear-structures-hash-table-linear-probing)
- [Quadratic Probing](/cortex/data-structures-and-algorithms/linear-structures-hash-table-quadratic-probing)
- [Double Hashing](/cortex/data-structures-and-algorithms/linear-structures-hash-table-double-hashing)
- [Pattern: Counting](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-counting-pattern)
- [Pattern: Key Generation](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-pattern-generation-pattern)
- [Pattern: Fixed-Size Sliding Window](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-fixed-sized-sliding-window-pattern)
- [Pattern: Variable-Size Sliding Window](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-variable-sized-sliding-window-pattern)
- [Pattern: Prefix Sum](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-prefix-sum-pattern)
- [Strings](/cortex/data-structures-and-algorithms/linear-structures-strings-what-is-a-string)

## Practice

Do these after the matching pattern in Start Here.

### Two Pointers
- [Flip Characters](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-problems-flip-characters)
- [Palindrome Checker](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-problems-palindrome-checker)
- [Vowel Exchange](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-problems-vowel-exchange)
- [Reverse Words](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-problems-reverse-words)
- [Reverse Segments](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-problems-reverse-segments)
- [Reverse Word Order](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-problems-reverse-word-order)
- [Palindrome Number](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-two-pointers-problems-palindrome-number)
- [Two Sum](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-two-pointers-problems-two-sum)
- [Duplicate-Aware Two Sum](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-two-pointers-problems-duplicate-aware-two-sum)
- [Approximate Three Sum](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-two-pointers-problems-approximate-three-sum)

### Two Pointers Reduction
- [Two Sum](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-reduction-problems-two-sum)
- [Target Limited Two Sum](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-reduction-problems-target-limited-two-sum)
- [Duplicate Aware Two Sum](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-reduction-problems-duplicate-aware-two-sum)
- [Largest Container](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-reduction-problems-largest-container)

### Two Pointers Subproblem
- [K Rotations](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-subproblem-problems-k-rotations)
- [Three Sum](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-subproblem-problems-three-sum)
- [Approximate Three Sum](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-subproblem-problems-approximate-three-sum)
- [Four Sum](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-subproblem-problems-four-sum)

### Simultaneous Traversal
- [Subsequence Checker](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-simultaneous-traversal-problems-subsequence-checker)
- [Merge Sorted Arrays](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-simultaneous-traversal-problems-merge-sorted-arrays)
- [Unique Intersections](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-simultaneous-traversal-problems-unique-intersections)
- [Repeated Intersections](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-simultaneous-traversal-problems-repeated-intersections)

### Fixed Sliding Window
- [Subarray Size Equals K](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-fixed-sliding-window-problems-subarray-size-equals-k)
- [Maximum Ones](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-fixed-sliding-window-problems-maximum-ones)
- [Negative Window](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-fixed-sliding-window-problems-negative-window)
- [Even Odd Count](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-fixed-sliding-window-problems-even-odd-count)

### Variable Sliding Window
- [Consecutive Ones](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-variable-sliding-window-problems-consecutive-ones)
- [Product Conundrum](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-variable-sliding-window-problems-product-conundrum)
- [Maximum Subarray Sum](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-variable-sliding-window-problems-maximum-subarray-sum)
- [Consecutive Ones with K Flips](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-variable-sliding-window-problems-consecutive-ones-with-k-flips)

### Interval Merging
- [Verify Schedule](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-interval-merging-problems-verify-schedule)
- [Overlap Reduction](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-interval-merging-problems-overlap-reduction)
- [Employee Free Time](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-interval-merging-problems-employee-free-time)
- [Insert Interval](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-interval-merging-problems-insert-interval)

### Maximum Overlap
- [Minimum Meeting Rooms](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-maximum-overlap-problems-minimum-meeting-rooms)
- [Remove Intervals](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-maximum-overlap-problems-remove-intervals)
- [Busiest Interval](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-maximum-overlap-problems-busiest-interval)
- [Peak Resource Requirement](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-maximum-overlap-problems-peak-resource-requirement)

### Reversal
- [Reverse a List](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-problems-reverse-a-list)
- [Reverse First K Nodes](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-problems-reverse-first-k-nodes)
- [Reverse Last K Nodes](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-problems-reverse-last-k-nodes)
- [Reverse the Given Segment](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-problems-reverse-the-given-segment)
- [Reverse a List](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-problems-reverse-a-list)
- [Reverse First K Nodes](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-problems-reverse-first-k-nodes)
- [Reverse Last K Nodes](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-problems-reverse-last-k-nodes)
- [Reverse the Given Segment](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-problems-reverse-the-given-segment)
- [Stack Inversion](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-reversal-problems-stack-inversion)
- [Reverse the String](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-reversal-problems-reverse-the-string)
- [Reverse an Array](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-reversal-problems-reverse-an-array)
- [Reverse Word Order](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-reversal-problems-reverse-word-order)

### Reversal (Subproblem)
- [Pairwise Swap](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-subproblem-problems-pairwise-swap)
- [Reverse K-Segments](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-subproblem-problems-reverse-k-segments)
- [Reverse Increasing Groups](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-subproblem-problems-reverse-increasing-groups)
- [Reverse Alternate Segments](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-subproblem-problems-reverse-alternate-segments)
- [Pairwise Swap](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-subproblem-problems-pairwise-swap)
- [Reverse K-Segments](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-subproblem-problems-reverse-k-segments)
- [Reverse Increasing Groups](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-subproblem-problems-reverse-increasing-groups)
- [Reverse Alternate Segments](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-subproblem-problems-reverse-alternate-segments)

### Sliding Window Traversal
- [K Maximum Sum](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-sliding-window-traversal-problems-k-maximum-sum)
- [Trim Nth Node](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-sliding-window-traversal-problems-trim-nth-node)
- [Swap Nth Nodes](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-sliding-window-traversal-problems-swap-nth-nodes)
- [K Rotations](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-sliding-window-traversal-problems-k-rotations)

### Fast and Slow Pointers
- [Middle Node Search](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-fast-and-slow-pointers-problems-middle-node-search)
- [Split List in Half](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-fast-and-slow-pointers-problems-split-list-in-half)
- [Equal Halves](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-fast-and-slow-pointers-problems-equal-halves)
- [Palindrome Checker](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-fast-and-slow-pointers-problems-palindrome-checker)

### Split
- [Even Odd Split](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-split-problems-even-odd-split)
- [Split Alternate Groups](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-split-problems-split-alternate-groups)
- [Split by Modulo](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-split-problems-split-by-modulo)
- [K-Way List Split](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-split-problems-k-way-list-split)

### Merge
- [Alternate Node Fusion](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-merge-problems-alternate-node-fusion)
- [Merge Sorted Lists](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-merge-problems-merge-sorted-lists)
- [Merge Sorted Lists II](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-merge-problems-merge-sorted-lists-ii)
- [List Addition](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-merge-problems-list-addition)

### Reorder
- [Relocate Node](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reorder-problems-relocate-node)
- [Parity Order](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reorder-problems-parity-order)
- [Value Partition](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reorder-problems-value-partition)
- [Shuffle List](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reorder-problems-shuffle-list)
- [Relocate Node](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reorder-problems-relocate-node)
- [Parity Order](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reorder-problems-parity-order)
- [Value Partition](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reorder-problems-value-partition)
- [Shuffle List](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reorder-problems-shuffle-list)

### Previous Closest Occurrence
- [Preceding Superior Element](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-previous-closest-occurrence-problems-preceding-superior-element)
- [Preceding Inferior Element](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-previous-closest-occurrence-problems-preceding-inferior-element)
- [Preceding Superior Element II](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-previous-closest-occurrence-problems-preceding-superior-element-ii)
- [Preceding Inferior Element II](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-previous-closest-occurrence-problems-preceding-inferior-element-ii)

### Next Closest Occurrence
- [Succeeding Superior Element](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-next-closest-occurrence-problems-succeeding-superior-element)
- [Succeeding Inferior Element](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-next-closest-occurrence-problems-succeeding-inferior-element)
- [Succeeding Superior Element II](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-next-closest-occurrence-problems-succeeding-superior-element-ii)
- [Succeeding Inferior Element II](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-next-closest-occurrence-problems-succeeding-inferior-element-ii)
- [Succeeding Superior Nodes](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-next-closest-occurrence-problems-succeeding-superior-nodes)
- [Retained Rainwater](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-next-closest-occurrence-problems-retained-rainwater)
- [Largest Rectangle Area](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-next-closest-occurrence-problems-largest-rectangle-area)

### Sequence Validation
- [Parentheses Checker](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-sequence-validation-problems-parentheses-checker)
- [Minimum Edits](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-sequence-validation-problems-minimum-edits)
- [Redundant Parentheses](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-sequence-validation-problems-redundant-parentheses)
- [Balanced Span](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-sequence-validation-problems-balanced-span)

### Linear Evaluation
- [Canonicalise Path](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-linear-evaluation-problems-canonicalise-path)
- [Bracketed Reversal](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-linear-evaluation-problems-bracketed-reversal)
- [String Expansion](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-linear-evaluation-problems-string-expansion)
- [Formula Parsing](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-linear-evaluation-problems-formula-parsing)

### Counting
- [First Non-Repeating Character](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-counting-problems-first-non-repeating-character)
- [Constructibility Check](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-counting-problems-constructibility-check)
- [Anagram Checker](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-counting-problems-anagram-checker)
- [Build Palindrome](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-counting-problems-build-palindrome)
- [Cluster Anagrams](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-counting-problems-cluster-anagrams)

### Key Generation
- [Row Specific Words](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-pattern-generation-problems-row-specific-words)
- [Homomorphic Strings](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-pattern-generation-problems-homomorphic-strings)
- [Pattern Matching](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-pattern-generation-problems-pattern-matching)
- [Cluster Displaced Strings](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-pattern-generation-problems-cluster-displaced-strings)

### Fixed-Sized Sliding Window
- [Duplicate Detection](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-fixed-sized-sliding-window-problems-duplicate-detection)
- [Subarray Distinctness](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-fixed-sized-sliding-window-problems-subarray-distinctness)
- [Contains Variation](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-fixed-sized-sliding-window-problems-contains-variation)
- [Anagram Finder](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-fixed-sized-sliding-window-problems-anagram-finder)

### Variable-Sized Sliding Window
- [Unique Character Span](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-variable-sized-sliding-window-problems-unique-character-span)
- [K Characters Span](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-variable-sized-sliding-window-problems-k-characters-span)
- [Maximal Character Swap](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-variable-sized-sliding-window-problems-maximal-character-swap)
- [Subarray Sum Equals K](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-variable-sized-sliding-window-problems-subarray-sum-equals-k)
- [Twin in Proximity](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-variable-sized-sliding-window-problems-twin-in-proximity)

### Prefix Sum
- [First Equilibrium Point](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-prefix-sum-problems-first-equilibrium-point)
- [Self Excluded Array Product](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-prefix-sum-problems-self-excluded-array-product)
- [Balanced Binary Subarray](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-prefix-sum-problems-balanced-binary-subarray)
- [Zero Sum Subarrays](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-prefix-sum-problems-zero-sum-subarrays)

## Reference

Quick-recall and design material. Skim, don't study top to bottom.

- [Memorize: Two Pointers](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-memorize)
- [Memorize: Two Pointers Reduction](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-reduction-memorize)
- [Memorize: Two Pointers Subproblem](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-two-pointers-subproblem-memorize)
- [Memorize: Simultaneous Traversal](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-simultaneous-traversal-memorize)
- [Memorize: Fixed Sliding Window](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-fixed-sliding-window-memorize)
- [Memorize: Variable Sliding Window](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-variable-sliding-window-memorize)
- [Memorize: Interval Merging](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-interval-merging-memorize)
- [Memorize: Maximum Overlap](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-maximum-overlap-memorize)
- [Design a Dynamic Array](/cortex/data-structures-and-algorithms/linear-structures-arrays-design-a-dynamic-array-design-a-dynamic-array)
- [Pattern Synthesis: Arrays](/cortex/data-structures-and-algorithms/linear-structures-arrays-pattern-synthesis)
- [Binary Search — full trace](/cortex/data-structures-and-algorithms/linear-structures-arrays-binary-search-visualise)
- [Memorize: Arrays](/cortex/data-structures-and-algorithms/linear-structures-arrays-memorize)
- [Memorize: Singly Linked List](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-memorize)
- [Memorize: Reversal](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-memorize)
- [Memorize: Reversal (Subproblem)](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reversal-subproblem-memorize)
- [Memorize: Sliding Window Traversal](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-sliding-window-traversal-memorize)
- [Memorize: Fast and Slow Pointers](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-fast-and-slow-pointers-memorize)
- [Memorize: Split](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-split-memorize)
- [Memorize: Merge](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-merge-memorize)
- [Memorize: Reorder](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-reorder-memorize)
- [Design a Singly Linked List](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-design-a-singly-linked-list-design-a-singly-linked-list)
- [Pattern Synthesis: Singly Linked List](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-pattern-synthesis)
- [Memorize: Doubly Linked List](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-memorize)
- [Memorize: Reversal](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-memorize)
- [Memorize: Reversal (Subproblem)](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reversal-subproblem-memorize)
- [Memorize: Two Pointers](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-two-pointers-memorize)
- [Memorize: Reorder](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-reorder-memorize)
- [Design a Doubly Linked List](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-design-a-doubly-linked-list-design-a-doubly-linked-list)
- [Pattern Synthesis: Doubly Linked List](/cortex/data-structures-and-algorithms/linear-structures-doubly-linked-list-pattern-synthesis)
- [Memorize: Stack](/cortex/data-structures-and-algorithms/linear-structures-stack-memorize)
- [Memorize: Reversal](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-reversal-memorize)
- [Memorize: Previous Closest Occurrence](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-previous-closest-occurrence-memorize)
- [Memorize: Next Closest Occurrence](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-next-closest-occurrence-memorize)
- [Memorize: Sequence Validation](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-sequence-validation-memorize)
- [Memorize: Linear Evaluation](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-linear-evaluation-memorize)
- [Design a Min Stack](/cortex/data-structures-and-algorithms/linear-structures-stack-design-min-stack-design-min-stack)
- [Pattern Synthesis: Stack](/cortex/data-structures-and-algorithms/linear-structures-stack-pattern-synthesis)
- [Memorize: Queue](/cortex/data-structures-and-algorithms/linear-structures-queue-memorize)
- [Design a Queue](/cortex/data-structures-and-algorithms/linear-structures-queue-design-a-queue-design-a-queue)
- [Memorize: Hash Table](/cortex/data-structures-and-algorithms/linear-structures-hash-table-memorize)
- [Memorize: Counting](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-counting-memorize)
- [Memorize: Key Generation](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-pattern-generation-memorize)
- [Memorize: Fixed-Sized Sliding Window](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-fixed-sized-sliding-window-memorize)
- [Memorize: Variable-Sized Sliding Window](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-variable-sized-sliding-window-memorize)
- [Memorize: Prefix Sum](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-prefix-sum-memorize)
- [Design a Hash Map](/cortex/data-structures-and-algorithms/linear-structures-hash-table-design-a-hash-map-design-a-hash-map)
- [Pattern Synthesis: Hash Table](/cortex/data-structures-and-algorithms/linear-structures-hash-table-pattern-synthesis)
