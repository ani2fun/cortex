---
title: "Binary Search — full trace"
summary: "End-to-end demo of the unified Visualise pipeline — runnable Python and Java code blocks paired with an inline array-1d diagram, all driven by the same VizGraph contract."
prereqs:
  - 02-linear-structures/01-arrays/01-what-is-an-array
---

# Binary Search — full trace

This chapter is the canonical reference for how the chapter-embedded **inline
diagram** and the **Visualise** button on a runnable code block coexist on the
same page. Both go through the unified pipeline described in
[ADR-0023](https://github.com/anthropics/codefolio/blob/main/docs/adr/0023-unified-visualise-pipeline.md):
a `VizGraph` JSON payload routes through a single `renderWidget` entrypoint, and
the `LAYOUTS` map dispatches to the right `*-layout.ts` file (here,
`array-layout.ts` via the `array-1d` key).

The binary search below is the standard iterative form: maintain a window
`[lo .. hi]`, probe the midpoint, halve the window each iteration.

## Python

```python run viz=array viz-root=arr
from typing import List


def binary_search(arr: List[int], target: int) -> int:
    lo = 0
    hi = len(arr) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


# Hits and misses
print(binary_search([1, 3, 5, 7, 9, 11, 13, 15, 17, 19], 7))   # 3
print(binary_search([1, 3, 5, 7, 9, 11, 13, 15, 17, 19], 20))  # -1
print(binary_search([1, 3, 5, 7, 9, 11, 13, 15, 17, 19], 1))   # 0  — first element
print(binary_search([1, 3, 5, 7, 9, 11, 13, 15, 17, 19], 19))  # 9  — last element

# Edge cases
print(binary_search([], 7))                                     # -1 — empty
print(binary_search([7], 7))                                    # 0  — singleton hit
print(binary_search([7], 8))                                    # -1 — singleton miss
```

Click **Visualise** above to open the AlgoLens modal. The modal generates a
full step-by-step animation from the actual execution trace — `lo`, `hi`, and
`mid` move through the array as the algorithm runs, the active line in the
source pane stays synchronised with the canvas, and the stack-frames panel
shows the live locals at each step.

The inline diagram below is the **first step only**, hand-authored from the
same JSON contract the modal uses. It exists as documentation of what the
initial state looks like; the modal's animation supersedes it for any reader
who clicks Visualise.

```d3 widget=array-1d
{
  "steps": [
    {
      "nodes": [
        {
          "id": "0",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "1",
          "label": "3",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "5",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "7",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "9",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "11",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "13",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "7",
          "label": "15",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "8",
          "label": "17",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "9",
          "label": "19",
          "kind": "cell",
          "meta": [],
          "slot": 9,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "lo",
          "target": "0",
          "color": "#3b82f6"
        },
        {
          "name": "mid",
          "target": "4",
          "color": "#a855f7"
        },
        {
          "name": "hi",
          "target": "9",
          "color": "#f59e0b"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Initial state — target = 7. lo = 0, hi = 9, mid = 4 → arr[mid] = 9 > 7, so the next step shrinks the window to [lo .. mid-1].",
      "line": 0,
      "frames": [],
      "cardCursor": []
    }
  ]
}
```

<p align="center"><strong>Binary search step 1: lo at 0, hi at 9, mid at 4. <code>arr[mid] = 9 > 7</code>, so the next iteration sets <code>hi = mid - 1</code> and the window collapses to the left half.</strong></p>

## Java

```java run viz=array viz-root=arr
public class Main {
    static int binarySearch(int[] arr, int target) {
        int lo = 0;
        int hi = arr.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] == target) {
                return mid;
            } else if (arr[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        int[] arr = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19};
        // Hits and misses
        System.out.println(binarySearch(arr, 7));   // 3
        System.out.println(binarySearch(arr, 20));  // -1
        System.out.println(binarySearch(arr, 1));   // 0  — first element
        System.out.println(binarySearch(arr, 19));  // 9  — last element

        // Edge cases
        System.out.println(binarySearch(new int[]{}, 7));   // -1 — empty
        System.out.println(binarySearch(new int[]{7}, 7));  // 0  — singleton hit
        System.out.println(binarySearch(new int[]{7}, 8));  // -1 — singleton miss
    }
}
```

The Java tab uses `lo + (hi - lo) / 2` instead of `(lo + hi) / 2` to dodge
integer overflow on large arrays — a textbook gotcha that doesn't show up
in Python (arbitrary-precision ints) but matters every time the array length
crosses `Integer.MAX_VALUE / 2`. The visualisation is identical to the
Python version's: each iteration moves `lo` and `hi` one window-halving
step closer.

```d3 widget=array-1d
{
  "steps": [
    {
      "nodes": [
        {
          "id": "0",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "1",
          "label": "3",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "5",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "7",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "9",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "11",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "13",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "7",
          "label": "15",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "8",
          "label": "17",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "9",
          "label": "19",
          "kind": "cell",
          "meta": [],
          "slot": 9,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "lo",
          "target": "0",
          "color": "#3b82f6"
        },
        {
          "name": "mid",
          "target": "4",
          "color": "#a855f7"
        },
        {
          "name": "hi",
          "target": "9",
          "color": "#f59e0b"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Initial state — target = 7. lo = 0, hi = 9, mid = 4 → arr[mid] = 9 > 7, so the next step shrinks the window to [lo .. mid-1].",
      "line": 0,
      "frames": [],
      "cardCursor": []
    }
  ]
}
```

<p align="center"><strong>Same step 1 as the Python diagram, included here so each language pane reads on its own.</strong></p>

## Why two diagrams when the modal shows the full trace?

The inline diagram is **static documentation** — one frozen step that an
author hand-authored to make the algorithm's first move legible without
asking the reader to click anything. The modal **Visualise** button is the
**dynamic animation** — it traces the actual execution and lights up the
canvas iteration by iteration, with the source-pane line highlight and the
stack-frames panel kept in sync.

Both go through `renderWidget` and the same `array-1d` layout. The
difference is the *origin* of the JSON: the inline diagram's JSON sits in
the markdown fence above; the modal's JSON is produced on the fly by
`HeapToGraph.adapt` from the Python (or Java) execution trace.
