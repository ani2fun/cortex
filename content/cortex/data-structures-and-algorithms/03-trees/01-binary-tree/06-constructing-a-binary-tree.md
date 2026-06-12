---
tier: spine
title: "Constructing a Binary Tree"
summary: "Run traversal in reverse — rebuild the tree from its sequences. One ordering is never enough, but any pair that includes inorder pins down exactly one tree: the other traversal names the root, inorder splits left from right, and you recurse. O(N) time with a hashmap inorder index, O(N) space."
prereqs:
  - trees-binary-tree-recursive-traversals-in-binary-trees
  - trees-binary-tree-linked-list-implementation-of-binary-trees
---

## Why It Exists

The [traversal lessons](/cortex/data-structures-and-algorithms/trees/binary-tree/recursive-traversals-in-binary-trees) ran *forward*: tree in, sequence out. This lesson runs the arrow *backward*: sequence in, tree out. That's exactly the problem you face whenever a tree has to be written to disk, sent over a network, or restored from a snapshot — you have a flat list of values and you must rebuild the precise shape that produced it.

The catch is that **one traversal is lossy**. A preorder of `[1, 2]` could mean "1 with left child 2" *or* "1 with right child 2" — same sequence, different trees. To recover the shape you need **two** traversals, and one of them must be **inorder**. Here's why that combination works: the non-inorder traversal (preorder or postorder) tells you *which node is the root* — it's the first preorder value, or the last postorder value. Then inorder does the thing only it can do: everything *left* of the root in the inorder array is the left subtree, everything *right* is the right subtree. Root from one traversal, the left/right *split* from inorder — recurse on each half and the whole tree falls out by divide-and-conquer. With a hashmap from value to inorder position, each split is `O(1)` and the rebuild is `O(N)`. (And the one pairing that *doesn't* work is preorder + postorder, for a reason worth seeing — [Trace It](#trace-it).)

## See It Work

Rebuild a tree from its preorder and inorder, then traverse the result to prove it round-trips. Pick a pair of traversals, then **Run** it.

```python run viz=binary-tree viz-root=root
import ast

class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right

def build(preorder, inorder):
    idx = {v: i for i, v in enumerate(inorder)}   # value -> inorder index: O(1) split
    pre = iter(preorder)
    def helper(lo, hi):
        if lo > hi: return None
        val  = next(pre)              # next preorder value = this subtree's ROOT
        node = TreeNode(val)
        mid  = idx[val]               # inorder splits: [lo..mid-1] | val | [mid+1..hi]
        node.left  = helper(lo, mid - 1)
        node.right = helper(mid + 1, hi)
        return node
    return helper(0, len(inorder) - 1)

def preorder_tr(n): return [] if n is None else [n.val] + preorder_tr(n.left) + preorder_tr(n.right)
def inorder_tr(n):  return [] if n is None else inorder_tr(n.left) + [n.val] + inorder_tr(n.right)

preorder = ast.literal_eval(input())
inorder  = ast.literal_eval(input())
root = build(preorder, inorder)
print("rebuilt preorder:", preorder_tr(root))
print("rebuilt inorder :", inorder_tr(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;
public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }
    static Map<Integer,Integer> idx = new HashMap<>();
    static int[] preArr; static int preIdx;
    static TreeNode helper(int lo, int hi) {
        if (lo > hi) return null;
        int val = preArr[preIdx++];          // next preorder value = ROOT
        TreeNode node = new TreeNode(val);
        int mid = idx.get(val);              // inorder split
        node.left  = helper(lo, mid - 1);
        node.right = helper(mid + 1, hi);
        return node;
    }
    static TreeNode build(int[] pre, int[] in) {
        preArr = pre; preIdx = 0; idx.clear();
        for (int i = 0; i < in.length; i++) idx.put(in[i], i);
        return helper(0, in.length - 1);
    }
    static List<Integer> pre(TreeNode n) { List<Integer> o=new ArrayList<>(); if(n==null) return o; o.add(n.val); o.addAll(pre(n.left)); o.addAll(pre(n.right)); return o; }
    static List<Integer> in(TreeNode n)  { List<Integer> o=new ArrayList<>(); if(n==null) return o; o.addAll(in(n.left)); o.add(n.val); o.addAll(in(n.right)); return o; }
    public static void main(String[] a) {
        Scanner sc = new Scanner(System.in);
        int[] preorder = parseIntArray(sc.nextLine());
        int[] inorder  = parseIntArray(sc.nextLine());
        TreeNode root = build(preorder, inorder);
        System.out.println("rebuilt preorder: " + pre(root));
        System.out.println("rebuilt inorder : " + in(root));
    }
    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "preorder", "label": "preorder", "type": "int[]", "placeholder": "[1, 2, 4, 5, 3]" },
    { "id": "inorder",  "label": "inorder",  "type": "int[]", "placeholder": "[4, 2, 5, 1, 3]" }
  ],
  "cases": [
    { "args": { "preorder": "[1, 2, 4, 5, 3]", "inorder": "[4, 2, 5, 1, 3]" }, "expected": "rebuilt preorder: [1, 2, 4, 5, 3]\nrebuilt inorder : [4, 2, 5, 1, 3]" },
    { "args": { "preorder": "[1, 2, 3]", "inorder": "[2, 1, 3]" }, "expected": "rebuilt preorder: [1, 2, 3]\nrebuilt inorder : [2, 1, 3]" },
    { "args": { "preorder": "[3, 9, 20, 15, 7]", "inorder": "[9, 3, 15, 20, 7]" }, "expected": "rebuilt preorder: [3, 9, 20, 15, 7]\nrebuilt inorder : [9, 3, 15, 20, 7]" },
    { "args": { "preorder": "[1]", "inorder": "[1]" }, "expected": "rebuilt preorder: [1]\nrebuilt inorder : [1]" }
  ]
}
```

Both print `rebuilt preorder: [1, 2, 4, 5, 3]` and `rebuilt inorder : [4, 2, 5, 1, 3]` — the exact arrays we fed in, so the reconstruction is correct. The tree rebuilt is the same `1 → (2 → 4, 5), (3)` from the traversal lessons.

## How It Works

The recursion is pure divide-and-conquer: one value names the root, inorder splits the rest into two subproblems.

```d2
direction: down
a: "preorder = [1, 2, 4, 5, 3]  ->  first element 1 is the ROOT"
b: "inorder = [4, 2, 5 | 1 | 3]  ->  1 splits: left = [4,2,5], right = [3]"
c: "recurse: rebuild left subtree from [4,2,5], right subtree from [3]"
a -> b -> c
```

<p align="center"><strong>One construction step. The first preorder value is the subtree root; locating it in inorder partitions the remaining values into the left subtree (to its left) and the right subtree (to its right). Recurse on each half.</strong></p>

- **The root comes from the non-inorder traversal.** It's `preorder[0]` (first), or `postorder[-1]` (last). That's the only node whose position you know without inorder.
- **The split comes from inorder.** Find the root in inorder; everything before it is the left subtree, everything after is the right. This is the *only* job inorder can do that the others can't — and it's why every working pair includes it.
- **`O(N)` needs the hashmap.** Searching for the root in inorder linearly is `O(N)` per node → `O(N²)` overall (degenerate to `O(N²)` on a skew). A precomputed `value → index` map makes each split `O(1)`, so the whole build is `O(N)` time, `O(N)` space.
- **Postorder + inorder is the mirror.** Consume postorder *from the end* (its last value is the root) and recurse **right before left**, because reversed postorder is `root, right, left` — the same algorithm, flipped ([Your Turn](#your-turn)).

> **Key takeaway.** A single traversal is lossy; rebuilding a tree needs two, one of them **inorder**. The non-inorder traversal names the root (`preorder[0]` / `postorder[-1]`); inorder splits the rest into left and right subtrees; recurse. A `value → inorder index` hashmap makes each split `O(1)`, so construction is `O(N)`. Preorder + postorder *can't* do it — without inorder there's no way to split.

## Trace It

Why must one of the two traversals be inorder — why won't preorder + postorder do? Build the smallest counterexample: a root `1` with a *single* child `2`. It can hang on the left or on the right.

**Predict before you run:** the left-child tree and the right-child tree — do they have the same preorder? The same postorder? The same inorder?

```python run
class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right
def preorder(n):  return [] if n is None else [n.val] + preorder(n.left) + preorder(n.right)
def postorder(n): return [] if n is None else postorder(n.left) + postorder(n.right) + [n.val]
def inorder(n):   return [] if n is None else inorder(n.left) + [n.val] + inorder(n.right)

left_child  = TreeNode(1, TreeNode(2), None)   # 2 hangs on the LEFT
right_child = TreeNode(1, None, TreeNode(2))    # 2 hangs on the RIGHT

print("LEFT  -> pre", preorder(left_child),  "post", postorder(left_child),  "in", inorder(left_child))
print("RIGHT -> pre", preorder(right_child), "post", postorder(right_child), "in", inorder(right_child))
```

<details>
<summary><strong>Reveal</strong></summary>

```
LEFT  -> pre [1, 2] post [2, 1] in [2, 1]
RIGHT -> pre [1, 2] post [2, 1] in [1, 2]
```

The two different trees have **identical preorder *and* identical postorder** (`[1,2]` and `[2,1]`) — so preorder + postorder cannot tell them apart. Only the **inorder** differs (`[2,1]` vs `[1,2]`), because inorder is the one traversal that records *which side* a child sits on. That's the whole reason a valid pair must include inorder: preorder and postorder both name the root and order the subtrees, but neither encodes the left/right split that a single-child node turns on. (Preorder + postorder *does* uniquely determine a tree if every internal node has exactly *two* children — a "full" binary tree — but that's a special case, not the general one.)

</details>

## Your Turn

The twin construction is **postorder + inorder** (LeetCode 106). Postorder's *last* value is the root, so consume it from the end and recurse **right before left** — otherwise it's the same divide-and-conquer.

**Predict:** rebuilding from `postorder = [4, 5, 2, 3, 1]` and `inorder = [4, 2, 5, 1, 3]` — what preorder should the result have? (It's the same tree, so it should round-trip.)

```python run viz=binary-tree viz-root=root
import ast

class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right

def build_post(postorder, inorder):
    idx  = {v: i for i, v in enumerate(inorder)}
    post = iter(reversed(postorder))     # from the end: root, then RIGHT, then LEFT
    def helper(lo, hi):
        if lo > hi: return None
        val  = next(post)                # last-remaining postorder value = ROOT
        node = TreeNode(val)
        mid  = idx[val]
        node.right = helper(mid + 1, hi)  # RIGHT before LEFT (reversed postorder)
        node.left  = helper(lo, mid - 1)
        return node
    return helper(0, len(inorder) - 1)

def preorder_tr(n): return [] if n is None else [n.val] + preorder_tr(n.left) + preorder_tr(n.right)

postorder = ast.literal_eval(input())
inorder   = ast.literal_eval(input())
root = build_post(postorder, inorder)
print("rebuilt preorder:", preorder_tr(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;
public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }
    static Map<Integer,Integer> idx = new HashMap<>();
    static int[] postArr; static int postIdx;
    static TreeNode helper(int lo, int hi) {
        if (lo > hi) return null;
        int val = postArr[postIdx--];        // consume from the END = ROOT
        TreeNode node = new TreeNode(val);
        int mid = idx.get(val);
        node.right = helper(mid + 1, hi);    // RIGHT before LEFT
        node.left  = helper(lo, mid - 1);
        return node;
    }
    static TreeNode buildPost(int[] post, int[] in) {
        postArr = post; postIdx = post.length - 1; idx.clear();
        for (int i = 0; i < in.length; i++) idx.put(in[i], i);
        return helper(0, in.length - 1);
    }
    static List<Integer> pre(TreeNode n) { List<Integer> o=new ArrayList<>(); if(n==null) return o; o.add(n.val); o.addAll(pre(n.left)); o.addAll(pre(n.right)); return o; }
    public static void main(String[] a) {
        Scanner sc = new Scanner(System.in);
        int[] postorder = parseIntArray(sc.nextLine());
        int[] inorder   = parseIntArray(sc.nextLine());
        TreeNode root = buildPost(postorder, inorder);
        System.out.println("rebuilt preorder: " + pre(root));
    }
    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "postorder", "label": "postorder", "type": "int[]", "placeholder": "[4, 5, 2, 3, 1]" },
    { "id": "inorder",   "label": "inorder",   "type": "int[]", "placeholder": "[4, 2, 5, 1, 3]" }
  ],
  "cases": [
    { "args": { "postorder": "[4, 5, 2, 3, 1]", "inorder": "[4, 2, 5, 1, 3]" }, "expected": "rebuilt preorder: [1, 2, 4, 5, 3]" },
    { "args": { "postorder": "[2, 3, 1]", "inorder": "[2, 1, 3]" }, "expected": "rebuilt preorder: [1, 2, 3]" },
    { "args": { "postorder": "[9, 15, 7, 20, 3]", "inorder": "[9, 3, 15, 20, 7]" }, "expected": "rebuilt preorder: [3, 9, 20, 15, 7]" },
    { "args": { "postorder": "[1]", "inorder": "[1]" }, "expected": "rebuilt preorder: [1]" }
  ]
}
```

Both print `rebuilt preorder: [1, 2, 4, 5, 3]` — the same tree, rebuilt from a different pair. The only changes from See It are *where* the root comes from (end instead of start) and *which child you recurse into first* (right instead of left); inorder still does the splitting.

## Reflect & Connect

- **A traversal is lossy compression.** Flattening a tree to one sequence throws away the shape; recovering it needs two independent views.
- **Root from one, split from inorder.** The non-inorder traversal names the root (`preorder[0]` / `postorder[-1]`); inorder partitions the rest into left and right. Recurse — divide-and-conquer.
- **Preorder + postorder can't split.** Without inorder, a single-child node is ambiguous (you saw two trees with identical pre *and* post). The pairing only works for full binary trees.
- **The hashmap is what makes it `O(N)`.** Linear search for the root in inorder is `O(N²)`; a `value → index` map drops each split to `O(1)`.
- **In practice, serialize with null markers.** Real serialize/deserialize uses *one* preorder traversal that writes an explicit marker for each `null` — those markers encode the shape, so a single sequence becomes unambiguous (no second traversal needed). Two-array reconstruction is the interview version; null-marked preorder is the production one.
- **Next: growing a tree.** [Insertion](/cortex/data-structures-and-algorithms/trees/binary-tree/insertion-in-binary-trees) adds nodes one at a time into an existing tree — the incremental cousin of building one all at once.

## Recall

<details>
<summary><strong>Q:</strong> Why isn't a single traversal enough to reconstruct a binary tree?</summary>

**A:** It's lossy — many distinct trees produce the same sequence. A preorder of `[1, 2]`, for instance, fits both "1 with left child 2" and "1 with right child 2." You need a second traversal to disambiguate the shape.

</details>
<details>
<summary><strong>Q:</strong> In preorder + inorder construction, what does each traversal contribute?</summary>

**A:** Preorder gives the root (its first element is the current subtree's root). Inorder gives the split: find the root in inorder, and everything to its left is the left subtree, everything to its right is the right subtree. Recurse on each side.

</details>
<details>
<summary><strong>Q:</strong> Why does preorder + postorder fail to determine a unique tree?</summary>

**A:** Neither preorder nor postorder records which side a child sits on, so a node with a single child is ambiguous — the left-child and right-child versions have identical preorder *and* postorder. Only inorder encodes the left/right split. (Pre + post is unique only for full trees, where every internal node has two children.)

</details>
<details>
<summary><strong>Q:</strong> How do you make the reconstruction run in `O(N)` rather than `O(N²)`?</summary>

**A:** Precompute a hashmap from value to its inorder index. Then locating the root's split point is `O(1)` instead of an `O(N)` linear scan, so the whole build is `O(N)` time and `O(N)` space.

</details>
<details>
<summary><strong>Q:</strong> If one traversal is ambiguous, how is a tree serialized to disk or the network in practice?</summary>

**A:** With a single preorder traversal that writes an explicit marker (e.g. `#`) for every `null` child. The null markers record the shape, so the one sequence is unambiguous and deserializes to exactly one tree — no second traversal needed.

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, §10.4 (tree representations) and the tree-reconstruction exercises; **LeetCode** 105 (preorder + inorder) and 106 (postorder + inorder) are the canonical problems; 297 (serialize/deserialize) is the null-marker variant.
- The [recursive-traversals lesson](/cortex/data-structures-and-algorithms/trees/binary-tree/recursive-traversals-in-binary-trees) for the orders this inverts, and the [linked representation](/cortex/data-structures-and-algorithms/trees/binary-tree/linked-list-implementation-of-binary-trees) for the `TreeNode` being rebuilt.
- The round-trip `preorder [1,2,4,5,3]` / `inorder [4,2,5,1,3]`, and the ambiguity demo (left-child and right-child trees sharing `pre [1,2]` + `post [2,1]` but differing in `inorder`), come from the runnable blocks above (deterministic) — re-run to verify.
