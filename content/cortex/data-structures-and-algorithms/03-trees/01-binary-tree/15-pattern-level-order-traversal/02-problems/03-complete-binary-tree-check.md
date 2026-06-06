---
title: "Complete Binary Tree Check"
summary: "See problem statement below."
prereqs:
  - 15-pattern-level-order-traversal/01-pattern
difficulty: medium
---

# Problem 3 — Complete binary tree check

> Return `true` iff the tree is *complete* — every level full except possibly the last, which is filled left-to-right with no gaps.

Trick: do a level-order traversal that **enqueues `null` children too** (don't skip them). Walk the queue; the moment you see a `null`, set a flag; if you ever see a *non-null* node *after* the flag is set, the tree is not complete (gap detected). If you finish without that happening, it's complete.

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#64748b"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
flowchart LR
    subgraph BAD["NOT complete — gap then a node"]
        direction TB
        B1((1)) --> B2((2))
        B1 --> B3((3))
        B2 -.- BN[null]
        B3 --> B5((5))
        style BN fill:#fee2e2,stroke:#ef4444
        style B5 fill:#fee2e2,stroke:#ef4444
    end
    subgraph GOOD["complete — all nulls cluster on the right end"]
        direction TB
        G1((1)) --> G2((2))
        G1 --> G3((3))
        G2 --> G4((4))
        G2 -.- GN[null]
        G3 -.- GN2[null]
        G3 -.- GN3[null]
        style GN fill:#fee2e2,stroke:#ef4444
        style GN2 fill:#fee2e2,stroke:#ef4444
        style GN3 fill:#fee2e2,stroke:#ef4444
    end
```

<p align="center"><strong>Completeness check — enqueue every child including nulls. Walk the resulting queue; once you've seen a null, no real node may follow. The left tree fails because node 5 follows a null.</strong></p>

<details>
<summary><h2>Solution</h2></summary>



```python run viz=binary-tree viz-root=root
def is_complete(root):
    if root is None: return True
    q = deque([root]); seen_null = False
    while q:
        n = q.popleft()
        if n is None:
            seen_null = True
        else:
            if seen_null: return False
            q.append(n.left); q.append(n.right)
    return True
```

```java run viz=binary-tree viz-root=root
public static boolean isComplete(TreeNode root) {
    if (root == null) return true;
    Deque<TreeNode> q = new ArrayDeque<>();
    // ArrayDeque can't store null; use LinkedList for null support, OR use a sentinel.
    Queue<TreeNode> qq = new java.util.LinkedList<>();
    qq.offer(root);
    boolean seenNull = false;
    while (!qq.isEmpty()) {
        TreeNode n = qq.poll();
        if (n == null) seenNull = true;
        else {
            if (seenNull) return false;
            qq.offer(n.left); qq.offer(n.right);
        }
    }
    return true;
}
```

</details>
