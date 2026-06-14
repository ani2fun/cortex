---
title: "Network Data Plane: Radix Tries in Routing Tables"
summary: "Every IP packet you send hits a routing table, which resolves the longest-prefix match — the most specific route for the destination — in tens of nanoseconds. Linux does it with a level-compressed radix trie read lock-free via RCU. The data structure between you and every byte you send."
prereqs:
  - trees-trie-introduction-to-tries
  - concurrency-and-systems-rcu-and-hazard-pointers
---

## Why It Exists

Every packet that leaves your machine faces one question: *which interface does it go out on?* The kernel answers by matching the destination IP against a routing table of `(prefix, length, next-hop)` entries — and a destination usually matches *several* prefixes at once. `192.168.1.42` matches the default route `0.0.0.0/0`, the network `192.168.0.0/16`, and the subnet `192.168.1.0/24`. The rule that resolves the tie is **longest-prefix match (LPM)**: the *most specific* prefix wins, so the packet takes the `/24` route. This isn't a rare operation — a backbone router does this *millions of times a second* against a table of hundreds of thousands of prefixes, with a budget of tens of nanoseconds per packet.

You already met the [trie](/cortex/data-structures-and-algorithms/trees/trie/introduction-to-tries) as a structure for string prefixes; an IP address is just a 32-bit string, so a trie is the natural home for LPM — walk the address bit by bit, and the deepest marked node you pass is the longest matching prefix. But the textbook binary trie is 32 levels deep and would touch a lock on every packet, neither of which a data plane can afford. So Linux's `fib_trie` adds two production ideas: **level compression** to flatten the trie to ~8 levels, and **[RCU](/cortex/data-structures-and-algorithms/concurrency-and-systems/rcu-and-hazard-pointers)** so per-packet lookups run completely lock-free while route updates publish new versions safely. This is the canonical "data structure under hard real-time constraints" story — and the last stop in this tour of DSA in real systems.

## See It Work

A routing table as a binary trie of address bits: insert each prefix by walking its bits and marking the node with a next-hop; look up a destination by walking its bits and remembering the *deepest* marked node — that's the longest match.

```python run viz=array
def ip_to_bits(ip):                                  # 32-bit binary string of an IPv4 address
    o = [int(p) for p in ip.split(".")]
    n = (o[0] << 24) | (o[1] << 16) | (o[2] << 8) | o[3]
    return format(n, "032b")

class Trie:
    def __init__(self): self.children = {}; self.next_hop = None
    def insert(self, prefix_bits, next_hop):         # walk prefix_length bits, mark the node
        node = self
        for b in prefix_bits:
            node = node.children.setdefault(b, Trie())
        node.next_hop = next_hop
    def lookup(self, addr_bits):                     # deepest marked node on the path = longest prefix
        node, best = self, self.next_hop
        for b in addr_bits:
            if b not in node.children: break
            node = node.children[b]
            if node.next_hop is not None: best = node.next_hop
        return best

t = Trie()
t.insert("", "eth0")                                 # 0.0.0.0/0  default route
t.insert(ip_to_bits("192.168.0.0")[:16], "eth1")     # 192.168.0.0/16
t.insert(ip_to_bits("192.168.1.0")[:24], "eth2")     # 192.168.1.0/24
dest = input()
print(dest + " ->", t.lookup(ip_to_bits(dest)))
```

```java run viz=array
import java.util.*;
public class Main {
    static String ipToBits(String ip) {                  // 32-bit binary string of an IPv4 address
        String[] o = ip.split("\\.");
        long n = (Long.parseLong(o[0]) << 24) | (Long.parseLong(o[1]) << 16) | (Long.parseLong(o[2]) << 8) | Long.parseLong(o[3]);
        String s = Long.toBinaryString(n);
        return "0".repeat(32 - s.length()) + s;
    }
    static class Trie {
        Map<Character,Trie> children = new HashMap<>();
        String nextHop = null;
        void insert(String bits, String hop) {           // walk prefix_length bits, mark the node
            Trie node = this;
            for (char b : bits.toCharArray()) node = node.children.computeIfAbsent(b, k -> new Trie());
            node.nextHop = hop;
        }
        String lookup(String bits) {                     // deepest marked node on the path = longest prefix
            Trie node = this; String best = nextHop;
            for (char b : bits.toCharArray()) {
                if (!node.children.containsKey(b)) break;
                node = node.children.get(b);
                if (node.nextHop != null) best = node.nextHop;
            }
            return best;
        }
    }
    public static void main(String[] x) {
        Scanner sc = new Scanner(System.in);
        Trie t = new Trie();
        t.insert("", "eth0");                                          // 0.0.0.0/0 default
        t.insert(ipToBits("192.168.0.0").substring(0, 16), "eth1");    // /16
        t.insert(ipToBits("192.168.1.0").substring(0, 24), "eth2");    // /24
        String dest = sc.nextLine();
        System.out.println(dest + " -> " + t.lookup(ipToBits(dest)));
    }
}
```

```testcases
{
  "args": [
    { "id": "dest", "label": "destination IP", "type": "string", "placeholder": "192.168.1.42" }
  ],
  "cases": [
    { "args": { "dest": "192.168.1.42" }, "expected": "192.168.1.42 -> eth2" },
    { "args": { "dest": "192.168.5.5"  }, "expected": "192.168.5.5 -> eth1" },
    { "args": { "dest": "8.8.8.8"      }, "expected": "8.8.8.8 -> eth0" },
    { "args": { "dest": "10.0.0.1"     }, "expected": "10.0.0.1 -> eth0" }
  ]
}
```

Both print `192.168.1.42 -> eth2`, `192.168.5.5 -> eth1`, `8.8.8.8 -> eth0`. The first address descends all 24 bits of the subnet route, so its deepest mark is `eth2`. The second shares only the first 16 bits, so it stops at the `/16` mark, `eth1`. The third shares *nothing* with `192.168.*` and falls off the trie at the very first bit, landing on the root's default, `eth0`. The trie turns "find the longest matching prefix" into "remember the last mark you walked past."

## How It Works

Longest-prefix match is really *most-specific containment* — each prefix is a set of addresses, and the routes nest:

```d2
direction: down
d0: "0.0.0.0/0 → eth0   (default route: matches every address)" {
  style.fill: "#dbeafe"; style.stroke: "#3b82f6"
  d16: "192.168.0.0/16 → eth1   (matches 192.168.*.*)" {
    style.fill: "#bbf7d0"; style.stroke: "#16a34a"
    d24: "192.168.1.0/24 → eth2   (matches 192.168.1.*)" {
      style.fill: "#fde68a"; style.stroke: "#d97706"
      pkt: "dst 192.168.1.42 → out eth2" {style.fill: "#fecaca"; style.stroke: "#dc2626"}
    }
  }
}
```

<p align="center"><strong>The routes nest by specificity. The destination sits inside all three prefixes; longest-prefix match picks the <em>innermost</em> (most specific) one, <code>/24 → eth2</code>. The trie finds it by walking address bits and keeping the deepest mark.</strong></p>

Three ideas turn the textbook trie into a data plane:

- **The binary trie works but is too deep.** Treating an address as 32 bits, a binary trie is up to 32 levels — 32 pointer-chases per lookup, each a potential cache miss. **Path compression** (the Patricia trie) collapses single-child chains, and Linux goes further with **level compression** (the LC-trie): each node tests `b` bits at once and branches `2^b` ways, with `b` adapted to how dense that subtree is. At `b = 4` a node has 16 children stored in one contiguous array, so IPv4 lookups bottom out in ~`32/4 = 8` cache-friendly memory fetches instead of 32.
- **Reads must be lock-free — that's RCU's job.** A lookup runs on *every packet*; taking even a spinlock per packet would be fatal to throughput. So lookups are pure [RCU](/cortex/data-structures-and-algorithms/concurrency-and-systems/rcu-and-hazard-pointers) reads with no synchronization, while the rare updates (`ip route add`) take a global lock (`rtnl_lock`), build a new trie version, publish it with a single pointer assignment, and free the old version only after a grace period — so no in-flight lookup ever dereferences freed memory ([Your Turn](#your-turn)).
- **The fast path allocates nothing and is cache-shaped.** The per-packet lookup keeps all state on the stack (no `kmalloc`), holds no locks, and packs each node's children contiguously so a level descent is one cache line. The result is a 50–100 ns lookup — about 5–10% of the ~1000 ns budget per packet at 1 Gbps. (In switch *hardware*, this is done in one cycle by a **TCAM** — ternary content-addressable memory — that compares all prefixes in parallel; `fib_trie` is the software-defined-networking equivalent.)

> **Key takeaway.** Routing is **longest-prefix match**: among all prefixes a destination matches, the most specific (longest) wins. A trie over address bits resolves it by walking the destination and keeping the deepest marked node. To meet a per-packet budget of tens of nanoseconds, Linux's `fib_trie` adds **level compression** (test several bits per node → ~8 levels, cache-friendly) and **RCU** (per-packet reads are lock-free; rare updates publish a new version and free the old one after a grace period). It's the trie and RCU from earlier chapters, combined under hard real-time constraints — and in silicon a TCAM does the same match in a single cycle.

## Trace It

The whole point of LPM is that *specificity*, not insertion order or recency, decides the winner. It's worth seeing that the answer doesn't depend on the order routes were added.

**Predict before you run:** the three routes are inserted in a deliberately misleading order — the most specific `/24` *first*, and the catch-all default route *last*. A naive "first match" or "last match" rule would pick the wrong one. For `192.168.1.42`, which route does longest-prefix match return?

```python run viz=array
def ip_to_bits(ip):
    o = [int(p) for p in ip.split(".")]
    return format((o[0] << 24) | (o[1] << 16) | (o[2] << 8) | o[3], "032b")
class Trie:
    def __init__(self): self.children = {}; self.next_hop = None
    def insert(self, bits, hop):
        node = self
        for b in bits: node = node.children.setdefault(b, Trie())
        node.next_hop = hop
    def lookup(self, bits):
        node, best = self, self.next_hop
        for b in bits:
            if b not in node.children: break
            node = node.children[b]
            if node.next_hop is not None: best = node.next_hop
        return best

t = Trie()
t.insert(ip_to_bits("192.168.1.0")[:24], "specific-eth2")   # /24 inserted FIRST
t.insert(ip_to_bits("192.168.0.0")[:16], "general-eth1")    # /16 inserted later
t.insert("", "default-eth0")                                 # default inserted LAST
print("192.168.1.42 ->", t.lookup(ip_to_bits("192.168.1.42")))
```

<details>
<summary><strong>Reveal</strong></summary>

It returns `specific-eth2` — the `/24`. Insertion order is irrelevant: the trie stores each prefix at a *depth equal to its prefix length*, so the `/24` mark sits at depth 24, the `/16` at depth 16, the default at the root. A lookup overwrites `best` every time it walks past a deeper mark, so the deepest one it reaches always wins — and depth *is* prefix length. That's why "longest prefix" and "most specific route" are the same thing, and why a router can load routes in any order (BGP updates arrive constantly, out of order) and always forward correctly. The structure encodes the priority; the algorithm doesn't have to sort or compare lengths explicitly. This is also why adding a more-specific route silently "steals" traffic from a general one without touching it — a common source of routing surprises, and the mechanism behind features like blackhole routes and policy overrides.

</details>

## Your Turn

The lookup you just wrote runs on every packet with **no lock**. So what happens when the control plane changes a route *while* lookups are in flight? This is exactly the [RCU](/cortex/data-structures-and-algorithms/concurrency-and-systems/rcu-and-hazard-pointers) problem from the concurrency chapter, here on the hottest path in the kernel.

**Predict:** a packet lookup grabs the current routing table. Before it finishes, `ip route` swaps in a new table (a single pointer assignment). Does the in-flight lookup see the old route, the new route, or a torn mix of both?

```python run viz=array
class Router:
    def __init__(self, table): self.table = table       # the "published" RCU-protected pointer
    def read_snapshot(self): return self.table           # rcu_dereference: grab the current version
    def publish(self, new_table): self.table = new_table # rcu_assign_pointer: atomic swap (old freed after grace period)

def rcu_demo(prefix, old_hop, new_hop):
    # Your code goes here — create a Router, grab a snapshot, publish a new table, print both views
    pass

prefix  = input()
old_hop = input()
new_hop = input()
rcu_demo(prefix, old_hop, new_hop)
```

```java run viz=array
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
public class Main {
    static void rcuDemo(String prefix, String oldHop, String newHop) {
        // Your code goes here — create a Router (AtomicReference), grab a snapshot,
        // publish a new table, then print both the in-flight and new-reader views
    }
    public static void main(String[] x) {
        Scanner sc = new Scanner(System.in);
        String prefix  = sc.nextLine();
        String oldHop  = sc.nextLine();
        String newHop  = sc.nextLine();
        rcuDemo(prefix, oldHop, newHop);
    }
}
```

```testcases
{
  "args": [
    { "id": "prefix",  "label": "route prefix",  "type": "string", "placeholder": "10.0.0.0/8" },
    { "id": "old_hop", "label": "old next-hop",  "type": "string", "placeholder": "old-eth1" },
    { "id": "new_hop", "label": "new next-hop",  "type": "string", "placeholder": "new-eth2" }
  ],
  "cases": [
    { "args": { "prefix": "10.0.0.0/8",     "old_hop": "old-eth1",  "new_hop": "new-eth2"  }, "expected": "in-flight reader sees: old-eth1\nnew reader sees:       new-eth2" },
    { "args": { "prefix": "192.168.1.0/24", "old_hop": "gw-primary","new_hop": "gw-backup"  }, "expected": "in-flight reader sees: gw-primary\nnew reader sees:       gw-backup" },
    { "args": { "prefix": "0.0.0.0/0",      "old_hop": "isp-a",     "new_hop": "isp-b"      }, "expected": "in-flight reader sees: isp-a\nnew reader sees:       isp-b" }
  ]
}
```

Both print `in-flight reader sees: old-eth1`, then `new reader sees: new-eth2`.

<details>
<summary><strong>Editorial</strong></summary>

Create the Router (or `AtomicReference`) with the old table, immediately grab a reference (`read_snapshot` / `.get()`), then publish the new table. The in-flight reference still points to the old snapshot — the update built a *new* object and swapped a single pointer; it never mutated the structure the reader holds.

```python solution time=O(1) space=O(1)
class Router:
    def __init__(self, table): self.table = table       # the "published" RCU-protected pointer
    def read_snapshot(self): return self.table           # rcu_dereference: grab the current version
    def publish(self, new_table): self.table = new_table # rcu_assign_pointer: atomic swap (old freed after grace period)

def rcu_demo(prefix, old_hop, new_hop):
    r = Router({prefix: old_hop})
    reader = r.read_snapshot()                          # packet lookup grabs the current table
    r.publish({prefix: new_hop})                        # control plane swaps in a new table mid-lookup
    print("in-flight reader sees:", reader[prefix])     # consistent OLD snapshot
    print("new reader sees:      ", r.read_snapshot()[prefix])

prefix  = input()
old_hop = input()
new_hop = input()
rcu_demo(prefix, old_hop, new_hop)
```

```java solution
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
public class Main {
    static void rcuDemo(String prefix, String oldHop, String newHop) {
        AtomicReference<Map<String,String>> table =
            new AtomicReference<>(Map.of(prefix, oldHop));   // the published RCU pointer
        Map<String,String> reader = table.get();             // rcu_dereference: grab current version
        table.set(Map.of(prefix, newHop));                   // rcu_assign_pointer: atomic swap
        System.out.println("in-flight reader sees: " + reader.get(prefix));       // consistent OLD snapshot
        System.out.println("new reader sees:       " + table.get().get(prefix));
    }
    public static void main(String[] x) {
        Scanner sc = new Scanner(System.in);
        String prefix  = sc.nextLine();
        String oldHop  = sc.nextLine();
        String newHop  = sc.nextLine();
        rcuDemo(prefix, oldHop, newHop);
    }
}
```

</details>

## Reflect & Connect

- **Longest-prefix match = most-specific containment.** Routes nest; the innermost prefix containing the destination wins. A trie resolves it by storing each prefix at its bit-depth and keeping the deepest mark — so prefix length *is* trie depth, and insertion order never matters.
- **Compression buys the latency budget.** A 32-level binary trie is too deep for tens of nanoseconds; level compression tests several bits per node (~8 cache-friendly fetches for IPv4). The right constant factors are the whole game on the hot path.
- **RCU makes per-packet reads free.** Lookups never lock; updates publish a new version and reclaim the old one after a grace period. The exact pattern from the [RCU chapter](/cortex/data-structures-and-algorithms/concurrency-and-systems/rcu-and-hazard-pointers), applied where read throughput is everything.
- **Software vs silicon.** `fib_trie` is the software-defined-networking path; switch ASICs do the same match in one cycle with a TCAM. Same problem, radically different hardware — and knowing the algorithm tells you what the silicon is emulating.
- **The module's thesis, one last time.** Like [Postgres `nbtree`](/cortex/data-structures-and-algorithms/dsa-in-real-systems/postgres-b-tree-and-the-write-path), the [CFS scheduler](/cortex/data-structures-and-algorithms/dsa-in-real-systems/linux-red-black-tree-in-the-cfs-scheduler), [Redis encodings](/cortex/data-structures-and-algorithms/dsa-in-real-systems/redis-internal-encodings), [Git](/cortex/data-structures-and-algorithms/dsa-in-real-systems/git-merkle-dag), and [LSM trees](/cortex/data-structures-and-algorithms/dsa-in-real-systems/lsm-trees-rocksdb-cassandra), a routing table is a textbook structure (a trie) plus a few sharp engineering choices (level compression, RCU, cache-aware layout). Read the source of any one of these and the abstract structures of the curriculum become the concrete machinery running the internet.

## Recall

<details>
<summary><strong>Q:</strong> What operation does the routing table perform on every packet, and what breaks ties?</summary>

**A:** Longest-prefix match: find the routing entry whose prefix matches the destination IP, and among all matches pick the one with the *longest* prefix (the most specific route). A trie over address bits resolves it by keeping the deepest marked node walked.

</details>
<details>
<summary><strong>Q:</strong> Why doesn't Linux use a plain 32-level binary trie?</summary>

**A:** Too deep — up to 32 pointer-chases (cache misses) per lookup. Linux uses a level-compressed (LC) trie: each node tests `b` bits and branches `2^b` ways, with children in one contiguous array. IPv4 lookups bottom out in ~8 cache-friendly fetches.

</details>
<details>
<summary><strong>Q:</strong> Does insertion order affect which route longest-prefix match returns?</summary>

**A:** No. Each prefix is stored at a depth equal to its length, so the lookup always keeps the deepest (longest) mark it walks past. Routes can be loaded in any order (as BGP updates arrive) and forwarding is still correct.

</details>
<details>
<summary><strong>Q:</strong> Why is the routing trie read with RCU, and how do updates stay safe?</summary>

**A:** Lookups happen per packet and must be lock-free, so they're pure RCU reads. Updates are rare: they take `rtnl_lock`, build a new trie version, publish it with one pointer assignment, and free the old version only after a grace period — so no in-flight lookup dereferences freed memory.

</details>
<details>
<summary><strong>Q:</strong> What's a TCAM, and how does it relate to <code>fib_trie</code>?</summary>

**A:** Ternary Content-Addressable Memory — hardware that compares a key against all stored prefixes in parallel, doing longest-prefix match in a single cycle. Switch/router silicon uses TCAMs; `fib_trie` is the software equivalent for software-defined networking.

</details>

## Sources & Verify

- **Linux source**: `net/ipv4/fib_trie.c` — `fib_table_lookup` (the per-packet fast path) and `fib_table_insert` (the `rtnl_lock`-held update path); `include/net/ip_fib.h` for the API. The classic reference is **Nilsson & Karlsson** (1999), "IP-Address Lookup Using LC-Tries", *IEEE JSAC*.
- **Trie** and **[RCU and Hazard Pointers](/cortex/data-structures-and-algorithms/concurrency-and-systems/rcu-and-hazard-pointers)** chapters — the prerequisites this lesson fuses; `Documentation/RCU/` in the kernel tree explains the lock-free read model.
- The LPM lookups (`eth2`/`eth1`/`eth0`), the order-independence (`specific-eth2` despite insertion order), and the RCU snapshot (`old-eth1` in-flight, `new-eth2` after) all come from the runnable blocks above (deterministic models of the LPM trie and an RCU pointer swap) — re-run to verify, or compare against `ip route get <dest>` on a Linux box.

---

*This closes the "DSA in Real Systems" module — six chapters connecting the abstract structures of the curriculum to the production code that runs the internet, your laptop, and your version control: a [B-tree](/cortex/data-structures-and-algorithms/dsa-in-real-systems/postgres-b-tree-and-the-write-path) in Postgres, a [red-black tree](/cortex/data-structures-and-algorithms/dsa-in-real-systems/linux-red-black-tree-in-the-cfs-scheduler) in the Linux scheduler, [adaptive encodings](/cortex/data-structures-and-algorithms/dsa-in-real-systems/redis-internal-encodings) in Redis, a [Merkle DAG](/cortex/data-structures-and-algorithms/dsa-in-real-systems/git-merkle-dag) in Git, [LSM trees](/cortex/data-structures-and-algorithms/dsa-in-real-systems/lsm-trees-rocksdb-cassandra) in RocksDB and Cassandra, and a radix trie in the network data plane. Reading the source of any one is a master class.*
