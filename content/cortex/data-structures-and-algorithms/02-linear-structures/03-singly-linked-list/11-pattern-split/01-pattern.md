---
title: "Pattern: Split"
summary: "Cut a list into pieces by re-linking, not copying — walk to each cut point and sever the next pointer. For k roughly-equal parts, the first n mod k parts get one extra node. O(n) time, O(1) extra space."
prereqs:
  - 02-linear-structures/03-singly-linked-list/01-what-is-a-linked-list
---

# Pattern: Split

## Why It Exists

Sometimes the task is to *cut* a list into pieces: split it into `k` roughly-equal parts (round-robin work across `k` workers), separate even- and odd-position nodes, or partition around a value. The shapes differ, but they share a wall.

The naive approach measures the list, then **copies** each piece into a freshly built sublist — `O(n)` extra nodes, and you've duplicated data that was already sitting there in order. The realization: the nodes are *already* laid out correctly; splitting doesn't need new nodes, it needs new **boundaries**. Walk to each cut point and sever the `next` link. Each piece reuses the original nodes — no copying, `O(1)` extra space beyond the list of resulting heads.

The one subtlety, when `n` doesn't divide evenly by `k`: the parts can't all be the same size, so you decide the distribution *up front* — the first `n mod k` parts each get one extra node.

## See It Work

Split `1→2→3→4→5` into `k = 2` parts. With `5 = 2·2 + 1`, the first part gets the extra node: `[1,2,3]` and `[4,5]`. Run it.

```python run viz=linked-list viz-root=head viz-kind=list-single
class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

def to_list(node):
    out = []
    while node:
        out.append(node.val); node = node.next
    return out

head = ListNode(1, ListNode(2, ListNode(3, ListNode(4, ListNode(5)))))   # 1 → 2 → 3 → 4 → 5
k = 2

n, node = 0, head
while node:                          # one pass to measure
    n += 1
    node = node.next
base, extra = divmod(n, k)           # base size, and how many parts get +1

parts, cur = [], head
for i in range(k):
    size = base + (1 if i < extra else 0)   # first `extra` parts are one longer
    parts.append(cur)                       # this part's head
    for _ in range(size - 1):               # walk to its last node
        cur = cur.next
    nxt = cur.next
    cur.next = None                         # sever — this is the cut
    cur = nxt

print([to_list(p) for p in parts])          # [[1, 2, 3], [4, 5]]
```

## How It Works

Two phases — **decide the sizes, then cut**:

1. **Measure & distribute.** One pass gives `n`. Then `base = n // k` and `extra = n % k`: every part has at least `base` nodes, and the **first `extra` parts** get one more. (`base + 1` for the first `extra`, `base` for the rest — they sum back to `n`.)
2. **Walk & sever.** Keep a `cur` walker. For each part, record its head, advance `size − 1` nodes to reach its last node, then set that node's `next = null` — the cut. Resume the next part from where you severed.

```d2
direction: right
full: "1 → 2 → 3 → 4 → 5   (n = 5, k = 2)"
p1: "part 1:  1 → 2 → 3   (base + 1)"
p2: "part 2:  4 → 5   (base)"
full -> p1: "first n%k = 1 part"
full -> p2: "remaining parts"
```

<p align="center"><strong>the nodes stay put; splitting just inserts boundaries. For <code>n=5, k=2</code>, the first <code>n%k=1</code> part gets <code>base+1=3</code> nodes, the rest get <code>base=2</code>.</strong></p>

Because each node is visited once and cutting is a pointer write, the whole thing is **`O(n)` time, `O(1)` extra space** (the output is `k` heads into the *same* nodes — nothing is copied).

### Key Takeaway

Split a list by re-linking, not copying: compute `base = n // k` and `extra = n % k`, give the first `extra` parts one extra node, then walk to each cut point and sever `next`. `O(n)` time, `O(1)` extra space — the pieces share the original nodes.

## Trace It

`n = 5, k = 2` → `base = 2, extra = 1`:

| part `i` | `size` | head | walk to | sever after | piece |
|---|---|---|---|---|---|
| 0 | `2+1 = 3` | `1` | `3` | `3` | `1→2→3` |
| 1 | `2+0 = 2` | `4` | `5` | `5` | `4→5` |

Before you read on: what happens if you ask for **more parts than nodes** — say `n = 3, k = 5`? How many of the five parts are non-empty, and what are the rest?

`base = 0, extra = 3`, so the first three parts get `0 + 1 = 1` node each and the last two get `0` — they're **empty** (`None`). Result: `[[1], [2], [3], [], []]`. This is the standard contract for "split into `k` parts": you always return exactly `k` pieces, padding with empties when there aren't enough nodes. The `base/extra` math handles it with no special case — `size` simply comes out `0` for the trailing parts.

## Your Turn

The reusable `k`-way split — returns `k` heads (some possibly empty):

```python run viz=linked-list viz-root=head viz-kind=list-single
class ListNode:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next

def split_into_k(head, k):
    n, node = 0, head
    while node:
        n += 1
        node = node.next
    base, extra = divmod(n, k)
    parts, cur = [], head
    for i in range(k):
        size = base + (1 if i < extra else 0)
        parts.append(cur)
        for _ in range(max(size - 1, 0)):
            cur = cur.next
        if size > 0:                 # sever only non-empty parts
            nxt = cur.next
            cur.next = None
            cur = nxt
    return parts

def to_list(node):
    out = []
    while node:
        out.append(node.val); node = node.next
    return out

head = ListNode(1, ListNode(2, ListNode(3, ListNode(4, ListNode(5)))))
print([to_list(p) for p in split_into_k(head, 3)])   # [[1, 2], [3, 4], [5]]
```

```java run viz=linked-list viz-root=head viz-kind=list-single
import java.util.*;

public class Main {
  static class ListNode { int val; ListNode next; ListNode(int v){ val = v; } ListNode(int v, ListNode n){ val = v; next = n; } }

  static ListNode[] splitIntoK(ListNode head, int k) {
    int n = 0;
    for (ListNode x = head; x != null; x = x.next) n++;
    int base = n / k, extra = n % k;
    ListNode[] parts = new ListNode[k];
    ListNode cur = head;
    for (int i = 0; i < k; i++) {
      int size = base + (i < extra ? 1 : 0);
      parts[i] = cur;
      for (int s = 0; s < Math.max(size - 1, 0); s++) cur = cur.next;
      if (size > 0) { ListNode nxt = cur.next; cur.next = null; cur = nxt; }   // sever
    }
    return parts;
  }

  public static void main(String[] args) {
    ListNode head = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4, new ListNode(5)))));
    ListNode[] parts = splitIntoK(head, 3);
    List<List<Integer>> out = new ArrayList<>();
    for (ListNode p : parts) { List<Integer> seg = new ArrayList<>(); for (ListNode c = p; c != null; c = c.next) seg.add(c.val); out.add(seg); }
    System.out.println(out);   // [[1, 2], [3, 4], [5]]
  }
}
```

Drill the family in **Practice** — [Even-Odd Split](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-split/problems/even-odd-split), [Split Alternate Groups](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-split/problems/split-alternate-groups), [Split by Modulo](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-split/problems/split-by-modulo), and [K-Way List Split](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-split/problems/k-way-list-split).

## Reflect & Connect

Splitting is the "re-link, never copy" principle applied to *carving*:

- **By count** (`k`-way, even distribution), **by position** (even/odd indices, alternate groups), **by value** (partition: nodes `< x` then nodes `≥ x`), **by modulo** (`i mod k` buckets). Each differs only in the rule that decides where the cuts fall.
- **Decide the distribution up front** — computing `base` and `extra` before cutting is what makes the uneven case (and the more-parts-than-nodes case) fall out with no special handling.
- **It's the inverse of merge, and a cousin of split-in-half** — cutting at the midpoint (the fast/slow pattern) is just `k = 2` with the boundary found by two-speed walk instead of counting. The next pattern, *merge*, reassembles pieces back into one ordered list.

**Prerequisites:** [What Is a Linked List?](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/what-is-a-linked-list).
**What's next:** the reverse operation — weave sorted lists back together in [Merge](/cortex/data-structures-and-algorithms/linear-structures/singly-linked-list/pattern-merge/pattern).

## Recall

> **Mnemonic:** *Measure `n`; `base = n//k`, `extra = n%k`; first `extra` parts get `+1`. Walk `size−1`, sever, repeat. Re-link, don't copy.*

| | |
|---|---|
| Distribution | `base = n // k`, `extra = n % k`; first `extra` parts get `base + 1` |
| Cut | walk `size − 1` nodes to the part's tail, set `tail.next = null` |
| More parts than nodes | trailing parts come out empty (`size = 0`) — no special case |
| Cost | `O(n)` time, `O(1)` extra space (nodes reused, not copied) |

<details>
<summary><strong>Q:</strong> Why re-link instead of copying into new sublists?</summary>

**A:** The nodes are already in order; severing `next` reuses them in `O(1)` extra space instead of `O(n)` for copies.

</details>
<details>
<summary><strong>Q:</strong> When `n` isn't divisible by `k`, how are the extra nodes distributed?</summary>

**A:** The first `n % k` parts each get one extra node (`base + 1`); the rest get `base`.

</details>
<details>
<summary><strong>Q:</strong> What happens when `k > n`?</summary>

**A:** The first `n` parts hold one node each and the trailing `k − n` parts are empty — the `base/extra` math yields `size = 0` for them automatically.

</details>
<details>
<summary><strong>Q:</strong> How does split relate to the fast/slow midpoint pattern?</summary>

**A:** Splitting at the middle is the `k = 2` case, with the cut point found by a two-speed walk instead of a count.

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, 4th ed., §10.2 — linked-list pointer manipulation.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §1.3 — linked structures; restructuring by re-linking.
- "Split a linked list into `k` parts" with the `base`/`extra` distribution is the standard result; both runnable blocks are verified by running (`k=2 ⇒ [[1,2,3],[4,5]]`, `k=3 ⇒ [[1,2],[3,4],[5]]`).
