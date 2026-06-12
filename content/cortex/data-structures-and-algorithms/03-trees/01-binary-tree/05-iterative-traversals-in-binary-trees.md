---
tier: spine
title: "Iterative Traversals in Binary Trees"
summary: "Recursion leans on a finite, megabyte-sized call stack that a deep tree can overflow. Replace it with an explicit heap-backed stack and you get the same preorder, inorder, and postorder — O(n) time, O(h) space — with no crash on adversarial depth. The trick: LIFO inverts push order, so push the child you want visited last."
prereqs:
  - trees-binary-tree-recursive-traversals-in-binary-trees
  - linear-structures-stack-what-is-a-stack
---

## Why It Exists

The [recursive traversals](/cortex/data-structures-and-algorithms/trees/binary-tree/recursive-traversals-in-binary-trees) are beautiful — three lines each — but they have a hidden dependency: the **call stack**. Every recursive call pushes a frame, and the call stack is small and fixed (a megabyte or so; Python caps recursion near 1000 frames). Feed a recursive traversal a degenerate tree — a right-leaning chain of 10,000 nodes — and it doesn't return an answer, it *crashes*: `RecursionError` in Python, `StackOverflowError` in Java.

The fix is to notice that **the recursion was always using a stack** — we just let the language manage it. Make that stack *explicit*: a plain stack object you push and pop yourself. Now the bookkeeping lives on the **heap** (gigabytes, grows on demand) instead of the call stack, so the traversal scales to any depth without overflowing. You get the same preorder, inorder, and postorder, the same `O(n)` time and `O(h)` space — just no crash. (The one traversal that *doesn't* use a stack is level-order, which uses a **queue** — that's [breadth-first](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal/pattern), a different lesson.) The one subtlety to internalize: a stack is **LIFO**, so it reverses the order you push — to visit the left child first, you must push it *last*.

## See It Work

Preorder and inorder, recursion-free. The whole tree's traversal state is the explicit `stack` list:

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right

def preorder_iter(root):
    out, stack = [], ([root] if root else [])
    while stack:
        n = stack.pop()                  # take the top
        out.append(n.val)                # visit
        if n.right: stack.append(n.right)  # push RIGHT first...
        if n.left:  stack.append(n.left)   # ...so LEFT is on top and pops next
    return out

def inorder_iter(root):
    out, stack, cur = [], [], root
    while stack or cur:
        while cur:                       # dive left, pushing every node on the way
            stack.append(cur); cur = cur.left
        cur = stack.pop()                # back up to the deepest unvisited node
        out.append(cur.val)              # visit it
        cur = cur.right                  # then turn into its right subtree
    return out

def build_tree(values):              # [1, 2, 3, null, 4] level-order → root
    if not values:
        return None
    root = TreeNode(values[0])
    queue = deque([root])
    i = 1
    while queue and i < len(values):
        node = queue.popleft()
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.left = TreeNode(v); queue.append(node.left)
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.right = TreeNode(v); queue.append(node.right)
    return root

root = build_tree(json.loads(input()))

print("preorder (iterative):", preorder_iter(root))
print("inorder  (iterative):", inorder_iter(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;
public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) { this.val = val; this.left = left; this.right = right; }
    }
    static List<Integer> preorderIter(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        if (root != null) stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode n = stack.pop();
            out.add(n.val);
            if (n.right != null) stack.push(n.right);   // push RIGHT first...
            if (n.left  != null) stack.push(n.left);    // ...so LEFT pops next
        }
        return out;
    }
    static List<Integer> inorderIter(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode cur = root;
        while (!stack.isEmpty() || cur != null) {
            while (cur != null) { stack.push(cur); cur = cur.left; }   // dive left
            cur = stack.pop();
            out.add(cur.val);                                          // visit
            cur = cur.right;                                           // go right
        }
        return out;
    }

    static TreeNode buildTree(Integer[] values) {
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.add(root);
        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            TreeNode node = queue.poll();
            if (i < values.length) { Integer v = values[i++]; if (v != null) { node.left = new TreeNode(v); queue.add(node.left); } }
            if (i < values.length) { Integer v = values[i++]; if (v != null) { node.right = new TreeNode(v); queue.add(node.right); } }
        }
        return root;
    }

    static Integer[] parseIntegerArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new Integer[0];
        String[] parts = inner.split(",");
        Integer[] out = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++)
            out[i] = parts[i].equals("null") ? null : Integer.parseInt(parts[i]);
        return out;
    }

    public static void main(String[] a) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println("preorder (iterative): " + preorderIter(root));
        System.out.println("inorder  (iterative): " + inorderIter(root));
    }
}
```

```testcases
{
  "args": [
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, 5]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, 5]" }, "expected": "preorder (iterative): [1, 2, 4, 5, 3]\ninorder  (iterative): [4, 2, 5, 1, 3]" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "preorder (iterative): [1, 2, 3]\ninorder  (iterative): [2, 1, 3]" },
    { "args": { "root": "[1]" }, "expected": "preorder (iterative): [1]\ninorder  (iterative): [1]" },
    { "args": { "root": "[1, null, 2, null, 3]" }, "expected": "preorder (iterative): [1, 2, 3]\ninorder  (iterative): [1, 2, 3]" }
  ]
}
```

Both print `preorder (iterative): [1, 2, 4, 5, 3]` and `inorder (iterative): [4, 2, 5, 1, 3]` — *identical* to the recursive versions from the previous lesson. The recursion is gone; the explicit `stack` does its job.

## How It Works

Watch the explicit stack during the preorder walk — it holds exactly the nodes the recursion's call stack would have held:

```d2
direction: down
a: "start: stack = [1]"
b: "pop 1 -> visit 1; push 3, then 2  ->  stack = [2, 3]"
c: "pop 2 -> visit 2; push 5, then 4  ->  stack = [4, 5, 3]"
d: "pop 4 -> visit 4 (leaf)           ->  stack = [5, 3]"
e: "pop 5 -> visit 5                   ->  stack = [3]"
f: "pop 3 -> visit 3                   ->  stack = []  (done)"
a -> b -> c -> d -> e -> f
```

<p align="center"><strong>Iterative preorder, stack shown top-first. Visit order <code>1, 2, 4, 5, 3</code>. Because the stack is LIFO, pushing <code>right</code> before <code>left</code> puts <code>left</code> on top — so it pops (and is visited) first.</strong></p>

- **Preorder** is the cleanest: push root; then loop `pop → visit → push right, push left`. The `right`-before-`left` push is the whole trick — LIFO reverses it back to left-first.
- **Inorder** can't visit a node until its whole left subtree is done, so it *dives left pushing every node*, then pops-visits-and-turns-right. The stack holds the path of ancestors you still owe a visit.
- **Postorder** is the fiddliest (children before parent). The easiest correct recipe is **two stacks**: do a modified preorder that pushes `left` then `right` into a second stack; that second stack, popped, *is* postorder ([Your Turn](#your-turn)).
- **Cost is unchanged.** `O(n)` time (each node pushed and popped once) and `O(h)` space (the stack never holds more than one root-to-node path). Same asymptotics as recursion — the difference is *where* the stack lives: an explicit heap stack grows to gigabytes and won't overflow on a deep tree.

> **Key takeaway.** Recursion's call stack is finite and overflows on a deep tree; make the stack **explicit** and the same three orders run on the heap with no crash — `O(n)` time, `O(h)` space. Preorder = `pop, visit, push right, push left`; inorder = dive-left-pushing then `pop, visit, go right`; postorder = two stacks (or modified-preorder reversed). A stack is **LIFO**, so push the child you want visited *last*. (Level-order swaps the stack for a queue → BFS.)

## Trace It

The single most error-prone line in iterative preorder is the push order. The See It code pushes `right` then `left`. What happens if you flip it — push `left` then `right`?

**Predict before you run:** intuitively, pushing `left` first should visit left first… right? Predict the output of the `left`-then-`right` version on the tree `1 → (2 → 4, 5), (3)`.

```python run viz=binary-tree viz-root=root
class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right
root = TreeNode(1, TreeNode(2, TreeNode(4), TreeNode(5)), TreeNode(3))

def preorder_push(root, left_first):
    out, stack = [], ([root] if root else [])
    while stack:
        n = stack.pop()
        out.append(n.val)
        order = [n.left, n.right] if left_first else [n.right, n.left]
        for k in order:                  # push in this order
            if k: stack.append(k)
    return out

print("push right-then-left:", preorder_push(root, left_first=False))  # the correct preorder
print("push left-then-right:", preorder_push(root, left_first=True))   # ???
```

<details>
<summary><strong>Reveal</strong></summary>

```
push right-then-left: [1, 2, 4, 5, 3]
push left-then-right: [1, 3, 2, 5, 4]
```

Pushing `left` first gives `[1, 3, 2, 5, 4]` — it visits the **right** child first at every node. That's the opposite of what intuition says, and the reason is the stack's **LIFO** discipline: whatever you push *last* sits on top and pops *first*. So to visit `left` before `right`, you must push `left` *last* — i.e. push `right` first. (The `left`-then-`right` order isn't a bug, by the way: `[1,3,2,5,4]` is a perfectly good "reverse preorder" — visit, right, left — and reversing *that* whole list, with children swapped, is one of the slick ways to compute postorder.) The lesson: with an explicit stack you own the ordering, and getting it right means thinking in reverse.

</details>

## Your Turn

Postorder (`left, right, visit` — children before parent) is the order that frees a tree or evaluates an expression bottom-up, and it's the trickiest to do iteratively. The cleanest recipe uses **two stacks**: a modified preorder pushes nodes into `s2`; popping `s2` gives postorder.

**Predict:** for the tree `1 → (2 → 4, 5), (3)`, what is the postorder? (It should match the recursive postorder from the previous lesson.)

```python run viz=binary-tree viz-root=root
import json
from collections import deque

class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right

def postorder_iter(root):
    if root is None: return []
    s1, s2, out = [root], [], []
    while s1:                          # modified preorder: visit, then push L, then R
        n = s1.pop()
        s2.append(n)                   # ...but record into s2 instead of outputting
        if n.left:  s1.append(n.left)  # push left then right, so s2 ends root-last-per-subtree
        if n.right: s1.append(n.right)
    while s2:                          # s2 popped = reverse of (visit,R,L) = postorder
        out.append(s2.pop().val)
    return out

def build_tree(values):              # [1, 2, 3, null, 4] level-order → root
    if not values:
        return None
    root = TreeNode(values[0])
    queue = deque([root])
    i = 1
    while queue and i < len(values):
        node = queue.popleft()
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.left = TreeNode(v); queue.append(node.left)
        if i < len(values):
            v = values[i]; i += 1
            if v is not None:
                node.right = TreeNode(v); queue.append(node.right)
    return root

root = build_tree(json.loads(input()))
print("postorder (iterative):", postorder_iter(root))
```

```java run viz=binary-tree viz-root=root
import java.util.*;
public class Main {
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) { this.val = val; this.left = left; this.right = right; }
    }
    static List<Integer> postorderIter(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        if (root == null) return out;
        Deque<TreeNode> s1 = new ArrayDeque<>(), s2 = new ArrayDeque<>();
        s1.push(root);
        while (!s1.isEmpty()) {
            TreeNode n = s1.pop();
            s2.push(n);
            if (n.left  != null) s1.push(n.left);    // push left then right
            if (n.right != null) s1.push(n.right);
        }
        while (!s2.isEmpty()) out.add(s2.pop().val); // s2 popped = postorder
        return out;
    }

    static TreeNode buildTree(Integer[] values) {
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.add(root);
        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            TreeNode node = queue.poll();
            if (i < values.length) { Integer v = values[i++]; if (v != null) { node.left = new TreeNode(v); queue.add(node.left); } }
            if (i < values.length) { Integer v = values[i++]; if (v != null) { node.right = new TreeNode(v); queue.add(node.right); } }
        }
        return root;
    }

    static Integer[] parseIntegerArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new Integer[0];
        String[] parts = inner.split(",");
        Integer[] out = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++)
            out[i] = parts[i].equals("null") ? null : Integer.parseInt(parts[i]);
        return out;
    }

    public static void main(String[] a) {
        Scanner sc = new Scanner(System.in);
        TreeNode root = buildTree(parseIntegerArray(sc.nextLine()));
        System.out.println("postorder (iterative): " + postorderIter(root));
    }
}
```

```testcases
{
  "args": [
    { "id": "root", "label": "root", "type": "tree", "placeholder": "[1, 2, 3, 4, 5]" }
  ],
  "cases": [
    { "args": { "root": "[1, 2, 3, 4, 5]" }, "expected": "postorder (iterative): [4, 5, 2, 3, 1]" },
    { "args": { "root": "[1, 2, 3]" }, "expected": "postorder (iterative): [2, 3, 1]" },
    { "args": { "root": "[1]" }, "expected": "postorder (iterative): [1]" },
    { "args": { "root": "[1, null, 2, null, 3]" }, "expected": "postorder (iterative): [3, 2, 1]" }
  ]
}
```

Both print `postorder (iterative): [4, 5, 2, 3, 1]` — matching the recursive postorder exactly. The two-stack method works because `s1` produces a *modified* preorder (visit, then push left, then right → pops as visit-right-left), and `s2` reverses that into `left-right-visit`, which is postorder. Three explicit-stack walks, three orders, zero recursion.

## Reflect & Connect

- **Recursion was a stack all along.** The call stack held one root-to-node path; an explicit stack holds the same thing. Making it explicit just relocates it from the tiny call stack to the vast heap.
- **LIFO inverts push order.** The defining gotcha: to visit `left` first, push it *last*. Every iterative-traversal bug starts here.
- **Each order has an explicit form.** Preorder (push-right-then-left), inorder (dive-left then pop-and-go-right), postorder (two stacks, or modified-preorder reversed). Same three orders as [recursion](/cortex/data-structures-and-algorithms/trees/binary-tree/recursive-traversals-in-binary-trees), same `O(n)`/`O(h)` cost.
- **Level-order is the exception.** Swap the stack for a [queue](/cortex/data-structures-and-algorithms/trees/binary-tree/pattern-level-order-traversal/pattern) and depth-first becomes breadth-first — the one traversal that isn't stack-based.
- **When to reach for it.** Trees deep enough to overflow recursion, or when you need explicit control (pause/resume a walk, or the `O(1)`-space Morris traversal that threads the tree instead of using any stack).
- **Next: building a tree.** [Constructing a binary tree](/cortex/data-structures-and-algorithms/trees/binary-tree/constructing-a-binary-tree) runs traversals in reverse — given preorder + inorder arrays, rebuild the unique tree they describe.

## Recall

<details>
<summary><strong>Q:</strong> Why use an iterative traversal instead of the simpler recursive one?</summary>

**A:** The recursive version relies on the call stack, which is small and fixed (~1 MB; Python ~1000 frames). A deep or degenerate tree overflows it (`RecursionError` / `StackOverflowError`). An explicit heap-backed stack grows on demand, so the traversal handles any depth.

</details>
<details>
<summary><strong>Q:</strong> In iterative preorder, why push the right child before the left?</summary>

**A:** The stack is LIFO, so the last thing pushed pops first. Pushing `right` then `left` leaves `left` on top, so it's popped and visited before `right` — giving the correct `visit, left, right` order.

</details>
<details>
<summary><strong>Q:</strong> How does iterative inorder decide when to visit a node?</summary>

**A:** It dives left, pushing every node, until it hits a `null`. Then it pops — that node's left subtree is fully done — visits it, and turns into its right subtree. The stack holds the ancestors still owed a visit.

</details>
<details>
<summary><strong>Q:</strong> How do you produce postorder iteratively with two stacks?</summary>

**A:** Run a modified preorder on `s1` (pop, push the node to `s2`, then push its left then right back to `s1`). `s2` ends up holding the nodes in reverse-postorder; popping `s2` yields postorder (`left, right, visit`).

</details>
<details>
<summary><strong>Q:</strong> What are the time and space costs, and how do they compare to recursion?</summary>

**A:** `O(n)` time (each node pushed and popped once) and `O(h)` space (the stack holds at most one root-to-node path) — identical to recursion. The only difference is the explicit stack lives on the heap and won't overflow.

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, §10.4 and §12.1 — stack-based and recursive tree walks; **Sedgewick & Wayne**, *Algorithms* §3.2 — non-recursive inorder traversal with an explicit stack.
- The [recursive-traversals lesson](/cortex/data-structures-and-algorithms/trees/binary-tree/recursive-traversals-in-binary-trees) for the orders these reproduce, and the [stack lesson](/cortex/data-structures-and-algorithms/linear-structures/stack/what-is-a-stack) for the LIFO push/pop primitive.
- `preorder [1,2,4,5,3]`, `inorder [4,2,5,1,3]`, the push-order flip `[1,3,2,5,4]`, and `postorder [4,5,2,3,1]` all come from the runnable blocks above (deterministic) — re-run to verify.
