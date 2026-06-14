---
title: How a Computer Stores Data
summary: Every value your program touches is just bytes living at numbered addresses. Get that one picture and arrays, pointers, and cache speed all stop being magic.
tier: spine
prereqs: []
---

# How a Computer Stores Data

## Why It Exists

You write `x = 42`, hit run, and it works. Simple. But where does `42` actually *go*?

Right now you can't answer a few questions that the rest of this book leans on. Why is reading the millionth item of a list just as fast as reading the first? Why can two programs that do the exact same work differ 10× in speed? What does a "pointer" even point *at*?

Every one of those answers comes down to a single thing you've probably never looked at directly: how the machine stores your data. So before any data structure, we build the floor everything else stands on. By the end of this page you'll be able to *picture* where `42` lives — and that picture never leaves you.

## See It Work

Picture the computer's memory as one very long street of numbered boxes. Each box holds one small value, and each box has an address — its house number.

| Address | `1000` | `1001` | `1002` | `1003` | `1004` | `1005` |
|---|---|---|---|---|---|---|
| Holds   | `0` | `0` | `0` | `42` | `0` | `0` |

<p align="center"><strong>Memory is a numbered strip of one-byte boxes. The value <code>42</code> sits in the box at address <code>1003</code>.</strong></p>

The CPU never goes hunting box-to-box. You hand it an address — `1003` — and it goes *straight* there. That one move is the whole foundation.

## How It Works

Memory is a single, enormous list of numbered slots. Everything else is built from that.

The pieces, smallest first:

- **bit** — one `0` or one `1`. The atom of all data.
- **byte** — eight bits grouped together (on every machine you'll use). It's the smallest chunk memory gives its own address.
- **address** — the number that names a byte's slot, like a house number on the street.
- **word** — the chunk the CPU naturally moves at once (often 8 bytes on a 64-bit machine).

To make this concrete: the letter `A` is stored as the byte `01000001` — which is just the number `65`. Text, numbers, pixels, everything: all of it is bytes underneath.

So the key idea is: **every value is bytes, and every byte lives at a numbered address.**

Now the payoff. Because the slots are *numbered*, the CPU reaches any one of them in a single step — the address *is* the location, so it jumps straight there without searching. It does not matter whether memory holds a thousand bytes or a billion; the jump costs the same. That constant, size-independent cost has a name you'll see everywhere: **`O(1)`** — "constant time." (Strictly, this is the *cost model* we reason with: real chips have faster and slower memory — caches — so addresses aren't all truly equal, but treating each as one step is how we fairly compare algorithms.) It is the single fact that makes arrays fast, and we'll measure it properly in the next lesson.

### Key Takeaway

Data is bytes at numbered addresses, and the machine can reach any address in one step.

## Trace It

Let's store the number `5` at address `1000` and read it back.

You'd expect "read it back" to mean *searching* for the `5`. It doesn't. Here's the trace:

1. **Store:** put the byte `5` into the slot named `1000`.
2. **Read:** you tell the CPU "give me `1000`." Before you read on — how many slots does it check to find it?

One. It goes straight to `1000`. It never looks at `999` or `1001`. That's the whole trick: when you know the *address*, finding the value is instant.

Now contrast that with finding a value by its *contents* — "where is the `5`?" — with no address. There the machine has no choice but to scan slot after slot. **Address-based access is one step; content-based search is a walk.** That gap is the difference you'll feel for the rest of this book.

## Your Turn

Enough pictures — let's see the bytes for real. A 32-bit integer is exactly 4 bytes. Given an integer `n`, return its four big-endian byte values as a list — the bytes that would actually live in memory.

```python run viz=array
import struct

def bytes_of(n):
    # Your code goes here
    return [0, 0, 0, 0]

n = int(input())
print(n, "->", bytes_of(n))
```

```java run viz=array
import java.util.*;
public class Main {
    static int[] bytesOf(int n) {
        // Your code goes here
        return new int[]{0, 0, 0, 0};
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] raw = bytesOf(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length; i++) {
            sb.append(raw[i]);
            if (i < raw.length - 1) sb.append(", ");
        }
        System.out.println(n + " -> [" + sb + "]");
    }
}
```

```testcases
{
  "args": [
    { "id": "n", "label": "n", "type": "int", "placeholder": "1000" }
  ],
  "cases": [
    { "args": { "n": "1" },    "expected": "1 -> [0, 0, 0, 1]" },
    { "args": { "n": "65" },   "expected": "65 -> [0, 0, 0, 65]" },
    { "args": { "n": "256" },  "expected": "256 -> [0, 0, 1, 0]" },
    { "args": { "n": "1000" }, "expected": "1000 -> [0, 0, 3, 232]" }
  ]
}
```

`1000 -> [0, 0, 3, 232]` — the number `1000` really does occupy four byte-boxes (`3 × 256 + 232 = 1000`). Predict the bytes for `256` before you check.

<details>
<summary><strong>Editorial</strong></summary>

Pack the integer as a 4-byte big-endian value and extract each unsigned byte. Python's `struct.pack(">i", n)` does both in one call; Java's `ByteBuffer` is the equivalent. The key is the big-endian byte order: the most-significant byte comes first, so `256 = 0x00000100` splits into `[0, 0, 1, 0]`.

```python solution time=O(1) space=O(1)
import struct

def bytes_of(n):
    raw = struct.pack(">i", n)   # pack n as a 4-byte big-endian integer
    return list(raw)

n = int(input())
print(n, "->", bytes_of(n))
```

```java solution
import java.util.*;
public class Main {
    static int[] bytesOf(int n) {
        byte[] raw = java.nio.ByteBuffer.allocate(4).putInt(n).array();
        int[] result = new int[4];
        for (int i = 0; i < 4; i++) result[i] = raw[i] & 0xFF;  // unsigned
        return result;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] raw = bytesOf(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length; i++) {
            sb.append(raw[i]);
            if (i < raw.length - 1) sb.append(", ");
        }
        System.out.println(n + " -> [" + sb + "]");
    }
}
```

</details>

## Reflect & Connect

Hold onto this picture, because the whole book builds on it:

- an **array** is fast because it places its values in *consecutive* addresses;
- a **pointer** is nothing more than a stored address;
- **cache** speed is about *which* addresses you touch and in what order.

**Prerequisites:** none — this is the floor.
**What's next:** you can now see *where* data lives. The next question is *how much work* an operation costs — and for that we need a way to measure it that doesn't depend on your laptop's speed. That's [Measuring Cost](/cortex/data-structures-and-algorithms/foundations/asymptotic-analysis).

## Recall

> **Mnemonic:** *Data is bytes; bytes have addresses; addresses are instant.*

| Term | One-line meaning |
|---|---|
| bit | a single `0` or `1` |
| byte | 8 bits — the smallest addressable chunk |
| address | the number naming a byte's slot |
| word | the chunk the CPU moves at once (often 8 bytes) |
| `O(1)` access | reaching any address costs the same, regardless of memory size |

<details>
<summary><strong>Q:</strong> What is the smallest piece of memory that has its own address?</summary>

**A:** A byte (8 bits).

</details>
<details>
<summary><strong>Q:</strong> Why is reaching address `1000` just as fast as address `5`?</summary>

**A:** The CPU computes the location and jumps directly — it never scans.

</details>
<details>
<summary><strong>Q:</strong> What is a pointer, really?</summary>

**A:** A value that stores an address.

</details>
<details>
<summary><strong>Q:</strong> Reading by *address* vs. searching by *contents* — which is one step?</summary>

**A:** By address; searching by contents is a walk.

</details>

## Sources & Verify

- **Patterson & Hennessy**, *Computer Organization and Design* — bytes, words, addressing.
- **CLRS** 4th ed., §2.2 — the **RAM model**, which formally *assumes* each memory access costs `O(1)` (the simplification above).
- ASCII `A` = `65` = `01000001`; a byte is 8 bits on all modern hardware (historically it was machine-dependent — see "byte" / "word (computer architecture)" references). Check the byte layout yourself by running the snippet above.
