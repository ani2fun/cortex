# Trees

The moment your data has **hierarchy** — a parent above children, a path from a root to a leaf — you've left the world of linear structures. Trees are the answer when "next-and-previous" stops being enough: filesystem directories, organisation charts, expression parsers, syntax highlighters, every database index, every priority queue.

> How to read this chapter: follow **Start Here** top to bottom — that is the teaching path. Drill it in **Practice** once it clicks. Use **Reference** (recall cards, synthesis, design) for review, not first reading.

## Start Here — the learning path

- [Introduction to Binary Trees](/cortex/data-structures-and-algorithms/trees/binary-tree/introduction-to-binary-trees)
- [Array Implementation of Binary Trees](/cortex/data-structures-and-algorithms/trees/binary-tree/array-implementation-of-binary-trees)
- [Linked-List Implementation of Binary Trees](/cortex/data-structures-and-algorithms/trees/binary-tree/linked-list-implementation-of-binary-trees)
- [Recursive Traversals in Binary Trees](/cortex/data-structures-and-algorithms/trees/binary-tree/recursive-traversals-in-binary-trees)
- [Iterative Traversals in Binary Trees](/cortex/data-structures-and-algorithms/trees/binary-tree/iterative-traversals-in-binary-trees)
- [Constructing a Binary Tree](/cortex/data-structures-and-algorithms/trees/binary-tree/constructing-a-binary-tree)
- [Insertion in Binary Trees](/cortex/data-structures-and-algorithms/trees/binary-tree/insertion-in-binary-trees)
- [Pattern: Preorder Traversal (Stateless)](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-preorder-traversal-stateless/pattern)
- [Pattern: Preorder Traversal (Stateful)](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-preorder-traversal-stateful/pattern)
- [Pattern: Postorder Traversal (Stateless)](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateless/pattern)
- [Pattern: Postorder Traversal (Stateful)](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateful/pattern)
- [Pattern: Root-to-Leaf Path (Stateless)](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-root-to-leaf-path-stateless/pattern)
- [Pattern: Root-to-Leaf Path (Stateful)](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-root-to-leaf-path-stateful/pattern)
- [Pattern: Level-Order Traversal](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal/pattern)
- [Pattern: Level-Order Traversal (Columns)](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal-columns/pattern)
- [Pattern: Lowest Common Ancestor](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-lowest-common-ancestor/pattern)
- [Pattern: Simultaneous Traversal](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-simultaneous-traversal/pattern)
- [Mixing Traversals — Boundary Traversal](/cortex/data-structures-and-algorithms/trees/binary-tree/practice-mix-traversals)
- [Introduction to Binary Search Trees](/cortex/data-structures-and-algorithms/trees/binary-search-tree/introduction-to-binary-search-trees)
- [Height and Balance in Binary Search Trees](/cortex/data-structures-and-algorithms/trees/binary-search-tree/height-and-balance-in-binary-search-trees)
- [Recursive Searching in Binary Search Trees](/cortex/data-structures-and-algorithms/trees/binary-search-tree/recursive-searching-in-binary-search-trees)
- [Iterative Searching in Binary Search Trees](/cortex/data-structures-and-algorithms/trees/binary-search-tree/iterative-searching-in-binary-search-trees)
- [Insertion in Binary Search Trees](/cortex/data-structures-and-algorithms/trees/binary-search-tree/insertion-in-binary-search-trees)
- [Deletion in Binary Search Trees](/cortex/data-structures-and-algorithms/trees/binary-search-tree/deletion-in-binary-search-trees)
- [Constructing a Binary Search Tree](/cortex/data-structures-and-algorithms/trees/binary-search-tree/constructing-a-binary-search-tree)
- [Lowest Common Ancestor in Binary Search Trees](/cortex/data-structures-and-algorithms/trees/binary-search-tree/lowest-common-ancestor-in-binary-search-trees)
- [Iterators in Binary Search Trees](/cortex/data-structures-and-algorithms/trees/binary-search-tree/iterators-in-binary-search-trees)
- [Pattern: Sorted Traversal](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-sorted-traversal/pattern)
- [Pattern: Reversed Sorted Traversal](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-reversed-sorted-traversal/pattern)
- [Pattern: Range Postorder](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-range-postorder/pattern)
- [Pattern: Two-Pointer on a BST](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-two-pointer/pattern)
- [Heaps](/cortex/data-structures-and-algorithms/trees/heap/what-is-a-heap)
- [Pattern: Top K Elements](/cortex/data-structures-and-algorithms/trees/heap/pattern-top-k-elements/pattern)
- [Pattern: Comparator](/cortex/data-structures-and-algorithms/trees/heap/pattern-comparator/pattern)
- [Introduction to Tries](/cortex/data-structures-and-algorithms/trees/trie/introduction-to-tries)
- [Self-Balancing BSTs — Overview](/cortex/data-structures-and-algorithms/trees/self-balancing-bst-overview/self-balancing-bst-overview)
- [Introduction to AVL Trees](/cortex/data-structures-and-algorithms/trees/avl-tree/introduction-to-avl-trees)
- [Introduction to Red-Black Trees](/cortex/data-structures-and-algorithms/trees/red-black-tree/introduction-to-red-black-trees)
- [Introduction to B-Trees](/cortex/data-structures-and-algorithms/trees/b-tree/introduction-to-b-trees)
- [Introduction to Segment Trees](/cortex/data-structures-and-algorithms/trees/segment-tree/introduction-to-segment-trees)
- [Introduction to Fenwick Trees (BIT)](/cortex/data-structures-and-algorithms/trees/fenwick-tree/introduction-to-fenwick-trees)
- [Introduction to Disjoint Set Union (Union-Find)](/cortex/data-structures-and-algorithms/trees/disjoint-set-union/introduction-to-disjoint-set-union)

## Practice

Do these after the matching pattern in Start Here.

### Preorder Traversal (Stateless)
- [Sum of Path](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-preorder-traversal-stateless/problems/sum-of-path)
- [Depth Assignment](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-preorder-traversal-stateless/problems/depth-assignment)
- [Concatenated Path](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-preorder-traversal-stateless/problems/concatenated-path)
- [Increasing Path](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-preorder-traversal-stateless/problems/increasing-path)

### Preorder Traversal (Stateful)
- [Duplicates in Path](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-preorder-traversal-stateful/problems/duplicates-in-path)
- [Second Minimum](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-preorder-traversal-stateful/problems/second-minimum)
- [Left View](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-preorder-traversal-stateful/problems/left-view)
- [Right View](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-preorder-traversal-stateful/problems/right-view)

### Postorder Traversal (Stateless)
- [Sum of Leaves](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateless/problems/sum-of-leaves)
- [Height of a Binary Tree](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateless/problems/height-of-a-binary-tree)
- [Maximum Root-to-Leaf Path Sum](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateless/problems/maximum-root-to-leaf-path-sum)
- [Is It a Full Binary Tree](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateless/problems/is-it-a-full-binary-tree)
- [Is It a Perfect Binary Tree](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateless/problems/is-it-a-perfect-binary-tree)
- [Collect Leaves by Height](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateless/problems/collect-leaves-by-height)

### Postorder Traversal (Stateful)
- [Diameter of Tree](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateful/problems/diameter-of-tree)
- [Descendants Sum Count](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateful/problems/descendants-sum-count)
- [Distribute Coins](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateful/problems/distribute-coins)
- [Most Frequent Subtree Sum](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateful/problems/most-frequent-subtree-sum)
- [Longest Monotonic Path](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateful/problems/longest-monotonic-path)
- [Monotonic Subtree Count](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateful/problems/monotonic-subtree-count)
- [Path Sum Count](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-postorder-traversal-stateful/problems/path-sum-count)

### Root-to-Leaf Path (Stateless)
- [Root to Leaf Path (Sum Check)](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-root-to-leaf-path-stateless/problems/root-to-leaf-path-sum-check)
- [Binary Summation of Tree](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-root-to-leaf-path-stateless/problems/binary-summation-of-tree)
- [Even Path](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-root-to-leaf-path-stateless/problems/even-path)
- [Odd Count](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-root-to-leaf-path-stateless/problems/odd-count)

### Root-to-Leaf Path (Stateful)
- [Root-to-Leaf Paths Summing to Target](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-root-to-leaf-path-stateful/problems/root-to-leaf-paths-summing-to-target)
- [Equal Evens-and-Odds Paths](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-root-to-leaf-path-stateful/problems/equal-evens-and-odds-paths)
- [Duplicate Paths](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-root-to-leaf-path-stateful/problems/duplicate-paths)
- [Prefix Paths](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-root-to-leaf-path-stateful/problems/prefix-paths)

### Level-Order Traversal
- [Level Sum](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal/problems/level-sum)
- [Deepest Leaves Sum](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal/problems/deepest-leaves-sum)
- [Complete Binary Tree Check](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal/problems/complete-binary-tree-check)
- [Zigzag Traversal](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal/problems/zigzag-traversal)
- [Cousin Check](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal/problems/cousin-check)

### Level-Order Traversal (Columns)
- [Top View](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal-columns/problems/top-view)
- [Bottom View](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal-columns/problems/bottom-view)
- [Vertical Traversal](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal-columns/problems/vertical-traversal)
- [Diagonal Traversal](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal-columns/problems/diagonal-traversal)

### Lowest Common Ancestor
- [Lowest Common Ancestor](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-lowest-common-ancestor/problems/lowest-common-ancestor)
- [LCA with Existence Check](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-lowest-common-ancestor/problems/lca-with-existence-check)
- [LCA of N Random Nodes](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-lowest-common-ancestor/problems/lca-of-n-random-nodes)
- [LCA of the Deepest Leaves](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-lowest-common-ancestor/problems/lca-of-the-deepest-leaves)
- [Distance Between Two Nodes](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-lowest-common-ancestor/problems/distance-between-two-nodes)

### Simultaneous Traversal
- [Identical Trees](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-simultaneous-traversal/problems/identical-trees)
- [Symmetry Detection](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-simultaneous-traversal/problems/symmetry-detection)
- [Subtree Detection](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-simultaneous-traversal/problems/subtree-detection)
- [Merge Trees](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-simultaneous-traversal/problems/merge-trees)

### Sorted Traversal
- [Lowest Absolute Variance](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-sorted-traversal/problems/lowest-absolute-variance)
- [BST Validator](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-sorted-traversal/problems/bst-validator)
- [BST to Sorted Array](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-sorted-traversal/problems/bst-to-sorted-array)
- [BST to DLL](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-sorted-traversal/problems/bst-to-dll)

### Reversed Sorted Traversal
- [Rank Nodes](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-reversed-sorted-traversal/problems/rank-nodes)
- [Kth Largest Element](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-reversed-sorted-traversal/problems/kth-largest-element)
- [Enriched Sum Tree](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-reversed-sorted-traversal/problems/enriched-sum-tree)
- [Multiple Replacement](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-reversed-sorted-traversal/problems/multiple-replacement)

### Range Postorder
- [Range Summation](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-range-postorder/problems/range-summation)
- [Range Diameter](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-range-postorder/problems/range-diameter)
- [Range Leaves](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-range-postorder/problems/range-leaves)
- [Range Exclusive Trim](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-range-postorder/problems/range-exclusive-trim)

### Two Pointer
- [Two Sum on BST](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-two-pointer/problems/two-sum-on-bst)
- [Multiple Tree](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-two-pointer/problems/multiple-tree)
- [Median in BST](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-two-pointer/problems/median-in-bst)
- [BST Pair Sum](/cortex/data-structures-and-algorithms/trees/binary-search-tree/pattern-two-pointer/problems/bst-pair-sum)

### Top K Elements
- [Kth Largest Element](/cortex/data-structures-and-algorithms/trees/heap/pattern-top-k-elements/problems/kth-largest-element)
- [Kth Smallest Element](/cortex/data-structures-and-algorithms/trees/heap/pattern-top-k-elements/problems/kth-smallest-element)
- [K Range Sum](/cortex/data-structures-and-algorithms/trees/heap/pattern-top-k-elements/problems/k-range-sum)
- [K Sorted Array Sorting](/cortex/data-structures-and-algorithms/trees/heap/pattern-top-k-elements/problems/k-sorted-array-sorting)

### Comparator
- [K Most Frequent Elements](/cortex/data-structures-and-algorithms/trees/heap/pattern-comparator/problems/k-most-frequent-elements)
- [K Smallest Sum Pairs](/cortex/data-structures-and-algorithms/trees/heap/pattern-comparator/problems/k-smallest-sum-pairs)
- [K Closest Values](/cortex/data-structures-and-algorithms/trees/heap/pattern-comparator/problems/k-closest-values)
- [K Arrays Smallest Range](/cortex/data-structures-and-algorithms/trees/heap/pattern-comparator/problems/k-arrays-smallest-range)
- [K-Way List Merge](/cortex/data-structures-and-algorithms/trees/heap/pattern-comparator/problems/k-way-list-merge)

## Reference

Quick-recall and design material. Skim, don't study top to bottom.

- [Design a Heap](/cortex/data-structures-and-algorithms/trees/heap/design-a-heap/design-a-heap)
