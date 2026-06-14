---
title: "Git's Merkle DAG"
summary: "Git is, internally, a Merkle DAG of immutable content-addressed objects: blobs, trees, commits. An object's name IS the hash of its content — giving automatic dedup, tamper-evidence, and free versioning in one rule. Every command (log, diff, merge, blame) is graph traversal on this structure."
prereqs:
  - trees-binary-tree-introduction-to-binary-trees
  - probabilistic-and-advanced-persistent-data-structures
  - concurrency-and-systems-distributed-data-structures-teaser
---

## Why It Exists

Git can branch, merge, rewrite history, and *never lose your work* — and it does it with no database, just files in `.git/objects/`. The magic is one structural decision: a Git repository is a **Merkle DAG of immutable, content-addressed objects**. "Content-addressed" means an object's name is literally the hash of its contents. That single rule buys three properties at once:

- **Identity = content -> automatic dedup.** Two files with the same bytes hash to the same id, so Git stores them once. Commit the same logo in 50 directories; there's one blob.
- **Tamper-evidence.** Change any byte and the hash changes; since a tree references its files by hash and a commit references its tree and *parent* by hash, altering anything deep in history cascades to every id above it. You can't quietly rewrite the past ([Your Turn](#your-turn)).
- **Free versioning via structural sharing.** A new commit re-uses every unchanged object from the old one — it's the [persistent data structure](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/persistent-data-structures) idea (path copying) applied to a whole filesystem ([Trace It](#trace-it)).

This is the same [Merkle tree](/cortex/data-structures-and-algorithms/concurrency-and-systems/distributed-data-structures-teaser) idea from the distributed-systems teaser — hash the children into the parent — generalized from a tree to a DAG (a merge commit has two parents). Once you see it, `log`, `diff`, `merge`, and `blame` stop being magic and become graph algorithms.

## See It Work

Git has four object types; the workhorses are **blob** (a file's bytes), **tree** (a directory listing), and **commit** (a snapshot pointing at a tree + parent commits). Every object's id is the SHA-1 of `"<type> <length>\0<content>"`. Here's that exact rule — and the dedup it gives:

```python run viz=array
import hashlib

def git_hash(obj_type, content):                       # Git's exact object id
    header = f"{obj_type} {len(content)}\0".encode()    # e.g. b"blob 5\x00"
    return hashlib.sha1(header + content.encode()).hexdigest()

obj_type = input()   # object type (e.g. "blob")
content1 = input()   # content of first object
content2 = input()   # content of second object (to show different id)

a = git_hash(obj_type, content1)
b = git_hash(obj_type, content1)                         # identical content
c = git_hash(obj_type, content2)
print(obj_type + " id of '" + content1 + "': " + a)
print("identical content -> same id? " + ("true" if a == b else "false"))
print("different content -> diff id? " + ("true" if a != c else "false"))
```

```java run viz=array
import java.security.MessageDigest;
import java.util.*;
public class Main {
    static String gitHash(String type, String content) throws Exception {
        byte[] body = content.getBytes("UTF-8");
        byte[] header = (type + " " + body.length + "\0").getBytes("UTF-8");   // "blob 5\0"
        byte[] all = new byte[header.length + body.length];
        System.arraycopy(header, 0, all, 0, header.length);
        System.arraycopy(body, 0, all, header.length, body.length);
        byte[] dig = MessageDigest.getInstance("SHA-1").digest(all);
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
    public static void main(String[] x) throws Exception {
        Scanner sc = new Scanner(System.in);
        String objType = sc.nextLine();
        String content1 = sc.nextLine();
        String content2 = sc.nextLine();
        String a = gitHash(objType, content1), b = gitHash(objType, content1), c = gitHash(objType, content2);
        System.out.println(objType + " id of '" + content1 + "': " + a);
        System.out.println("identical content -> same id? " + a.equals(b));
        System.out.println("different content -> diff id? " + !a.equals(c));
    }
}
```

```testcases
{
  "args": [
    { "id": "obj_type", "label": "object type", "type": "string", "placeholder": "blob" },
    { "id": "content1", "label": "content 1", "type": "string", "placeholder": "hello" },
    { "id": "content2", "label": "content 2", "type": "string", "placeholder": "world" }
  ],
  "cases": [
    { "args": { "obj_type": "blob", "content1": "hello", "content2": "world" }, "expected": "blob id of 'hello': b6fc4c620b67d95f953a5c1c1230aaab5db5a1b0\nidentical content -> same id? true\ndifferent content -> diff id? true" },
    { "args": { "obj_type": "blob", "content1": "cats", "content2": "dogs" }, "expected": "blob id of 'cats': 0048e1ce9c3b7d54b5e85cf45d11b1672a0be598\nidentical content -> same id? true\ndifferent content -> diff id? true" },
    { "args": { "obj_type": "tree", "content1": "hello", "content2": "hello world" }, "expected": "tree id of 'hello': cbb918f93e0b6cdc9632f3ce0f94805cd7c3b498\nidentical content -> same id? true\ndifferent content -> diff id? true" }
  ]
}
```

The first case prints `blob id of 'hello': b6fc4c620b67d95f953a5c1c1230aaab5db5a1b0`, then `true`, `true`. The SHA-1 formula is Git's exact rule — run `printf 'hello\n' | git hash-object --stdin` to see how the newline shifts the id. Identical content yields an identical id (so Git dedups), and any change yields a different id (so nothing can be altered unnoticed).

## How It Works

A commit points at a tree, a tree points at blobs and sub-trees, and a commit points at its parent — all by hash. Two consecutive commits, where only `main.py` changed:

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#64748b"
---
flowchart TB
  C2["commit (HEAD)<br/>tree: T2<br/>parent: C1"]
  C1["commit C1<br/>tree: T1"]
  T2["tree T2<br/>'src/' -> T_src2<br/>'README.md' -> B_readme"]
  T1["tree T1<br/>'src/' -> T_src1<br/>'README.md' -> B_readme"]
  T_src2["tree T_src2<br/>'main.py' -> B_main_v2"]
  T_src1["tree T_src1<br/>'main.py' -> B_main_v1"]
  B_main_v1["blob B_main_v1"]
  B_main_v2["blob B_main_v2"]
  B_readme["blob B_readme<br/>(unchanged across commits)"]
  C2 --> T2
  C2 -.parent.-> C1
  C1 --> T1
  T2 --> T_src2
  T1 --> T_src1
  T2 --> B_readme
  T1 --> B_readme
  T_src2 --> B_main_v2
  T_src1 --> B_main_v1
```

<p align="center"><strong>Two consecutive Git commits. The README hasn't changed, so both trees point to the same blob. Persistence and structural sharing in one diagram.</strong></p>

The load-bearing ideas:

- **The four object types are DAG nodes.** A **blob** is raw file bytes (no name). A **tree** is a directory listing — entries of `(mode, name, hash)` pointing at blobs or sub-trees, recursively, exactly like nested folders. A **commit** holds its root tree's hash, its parent commit hash(es) (zero for the first commit, two for a merge), plus author and message. A **tag** is an annotated label on a commit. Everything references everything else *by hash*.
- **Content addressing = dedup + integrity in one rule.** Because an id is the hash of content, equality of ids means equality of content. That gives free deduplication and makes `git diff A B` fast: when two trees (or sub-trees) have the same hash, they're identical, so Git short-circuits and never descends — only changed paths are walked. `git fsck` verifies the whole repo by re-hashing every object and checking it matches its name.
- **Structural sharing makes versions cheap.** Editing one file in a 100,000-file repo creates *one* new blob, a handful of new trees along the path to the root, and one new commit — the other ~99,999 blobs and all untouched sub-trees are **shared** with the previous commit. That's path copying from [persistent data structures](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/persistent-data-structures); Git is the most-deployed example on Earth. (At scale, `git gc` packs loose objects into pack files with delta-compression, 5–10× smaller — but the logical model is unchanged.)

> **Key takeaway.** Git is a Merkle DAG of immutable, content-addressed objects: an object's id is the hash of its content, so identity equals content. That one rule delivers automatic **dedup** (same bytes -> same id), **tamper-evidence** (any change cascades up through tree and commit hashes), and **free versioning** via **structural sharing** (a new commit re-uses every unchanged object). Blobs, trees, and commits are the nodes; `log`/`diff`/`merge`/`blame` are graph traversals over them. It's the [Merkle tree](/cortex/data-structures-and-algorithms/concurrency-and-systems/distributed-data-structures-teaser) generalized to a DAG and applied to a filesystem.

## Trace It

Structural sharing is the claim that a new commit barely allocates anything. Let's watch which object ids actually change when you edit one of two files.

**Predict before you run:** a commit has a tree with two files, `README.md` and `main.py`. You edit only `main.py` and commit again. Of the four object ids — README's blob, main.py's blob, the tree, the commit — which change, and which stay the same?

```python run viz=array
import hashlib
def gid(t, c):                                          # short git object id
    return hashlib.sha1(f"{t} {len(c)}\0".encode() + c.encode()).hexdigest()[:8]

# Commit 1: README + main.py
readme    = gid("blob", "# My Project\n")
main_v1   = gid("blob", "print(1)\n")
tree_v1   = gid("tree", f"{readme} README.md | {main_v1} main.py")
commit_v1 = gid("commit", f"tree {tree_v1} | edit")

# Commit 2: edit main.py; README untouched -> Git re-hashes its unchanged content
readme2   = gid("blob", "# My Project\n")               # same bytes -> same id
main_v2   = gid("blob", "print(2)\n")
tree_v2   = gid("tree", f"{readme2} README.md | {main_v2} main.py")
commit_v2 = gid("commit", f"tree {tree_v2} | parent {commit_v1} | edit")

print("main.py blob changed?", main_v1 != main_v2)
print("tree changed?        ", tree_v1 != tree_v2)
print("commit changed?      ", commit_v1 != commit_v2)
print("README blob reused?  ", readme == readme2)
```

<details>
<summary><strong>Reveal</strong></summary>

`main.py blob changed? True`, `tree changed? True`, `commit changed? True`, `README blob reused? True`. Editing `main.py` makes a new blob; because the tree lists that blob *by hash*, the tree's content changed, so the tree gets a new id; and because the commit names the tree by hash, the commit gets a new id too — the change ripples *up* the path to the root. But `README.md`'s bytes never changed, so re-hashing them yields the **same id** — Git doesn't store a second copy, it points the new tree at the existing blob. That's the whole persistence story: a commit only allocates new objects along the changed path (here: blob -> tree -> commit), and shares everything else. In a 100k-file repo, one edit is ~3–4 new objects, not 100k. The cost of a version is proportional to what changed, not to the size of the project — which is exactly why `git commit` is instant and `.git` doesn't explode.

</details>

## Your Turn

Now the flip side of structural sharing: because every object is named by a hash that includes its *parent's* hash, history is a Merkle chain — and that makes it tamper-evident.

**Predict:** you have a 3-commit chain (`init` -> `add feature` -> `fix bug`), each commit's content embedding its parent's id. An attacker edits the message of commit #1 (the oldest). How many of the three commit ids change — just the first, or more?

```python run viz=array
import hashlib

def gid(t, c):
    return hashlib.sha1(f"{t} {len(c)}\0".encode() + c.encode()).hexdigest()[:8]

def chain(msgs):                       # each commit embeds its parent's id
    ids = []; parent = ""
    for m in msgs:
        cid = gid("commit", f"parent {parent} | {m}")
        ids.append(cid); parent = cid
    return ids

msg1 = input()          # commit #1 message
msg2 = input()          # commit #2 message
msg3 = input()          # commit #3 message
tampered_msg1 = input() # tampered version of commit #1

honest   = chain([msg1, msg2, msg3])
tampered = chain([tampered_msg1, msg2, msg3])
print("honest chain: " + "[" + ", ".join(honest) + "]")
print("tampered chain: " + "[" + ", ".join(tampered) + "]")
print("ids changed: " + str(sum(a != b for a, b in zip(honest, tampered))) + " of " + str(len(honest)))
```

```java run viz=array
import java.security.MessageDigest;
import java.util.*;
public class Main {
    static String gid(String t, String c) throws Exception {
        byte[] body = c.getBytes("UTF-8");
        byte[] header = (t + " " + body.length + "\0").getBytes("UTF-8");
        byte[] all = new byte[header.length + body.length];
        System.arraycopy(header, 0, all, 0, header.length);
        System.arraycopy(body, 0, all, header.length, body.length);
        byte[] dig = MessageDigest.getInstance("SHA-1").digest(all);
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) sb.append(String.format("%02x", b & 0xff));
        return sb.substring(0, 8);
    }
    static List<String> chain(String[] msgs) throws Exception {
        List<String> ids = new ArrayList<>(); String parent = "";
        for (String m : msgs) { ids.add(gid("commit", "parent " + parent + " | " + m)); parent = ids.get(ids.size() - 1); }
        return ids;
    }
    public static void main(String[] x) throws Exception {
        Scanner sc = new Scanner(System.in);
        String msg1 = sc.nextLine(), msg2 = sc.nextLine(), msg3 = sc.nextLine(), tampered1 = sc.nextLine();
        List<String> honest = chain(new String[]{msg1, msg2, msg3});
        List<String> tampered = chain(new String[]{tampered1, msg2, msg3});
        System.out.println("honest chain: [" + String.join(", ", honest) + "]");
        System.out.println("tampered chain: [" + String.join(", ", tampered) + "]");
        int changed = 0; for (int i = 0; i < honest.size(); i++) if (!honest.get(i).equals(tampered.get(i))) changed++;
        System.out.println("ids changed: " + changed + " of " + honest.size());
    }
}
```

```testcases
{
  "args": [
    { "id": "msg1", "label": "commit #1", "type": "string", "placeholder": "init" },
    { "id": "msg2", "label": "commit #2", "type": "string", "placeholder": "add feature" },
    { "id": "msg3", "label": "commit #3", "type": "string", "placeholder": "fix bug" },
    { "id": "tampered_msg1", "label": "tampered #1", "type": "string", "placeholder": "init (tampered)" }
  ],
  "cases": [
    { "args": { "msg1": "init", "msg2": "add feature", "msg3": "fix bug", "tampered_msg1": "init (tampered)" }, "expected": "honest chain: [b7800f54, 9ed7f2f4, f48ce30c]\ntampered chain: [b2284241, ea73bd64, 04445d01]\nids changed: 3 of 3" },
    { "args": { "msg1": "create repo", "msg2": "add tests", "msg3": "merge branch", "tampered_msg1": "create repo (forged)" }, "expected": "honest chain: [a28a5633, a497fbc3, 09d2c51a]\ntampered chain: [98f37417, 87c1eafc, f31f9d6a]\nids changed: 3 of 3" },
    { "args": { "msg1": "initial", "msg2": "refactor", "msg3": "release", "tampered_msg1": "initial (modified)" }, "expected": "honest chain: [53d5b2b0, fbaf4f88, 4cc33a92]\ntampered chain: [a2581f28, c0d5d7c0, 80020344]\nids changed: 3 of 3" }
  ]
}
```

Both report `ids changed: 3 of 3` — **all** of them. Editing the oldest commit changes its id; commit #2 embedded that id, so commit #2's content changed and its id changed too; that cascades to commit #3. There's no way to alter an ancestor without every descendant's hash shifting, which is precisely why a `git push --force` that rewrites old history produces entirely new commit ids (and why anyone who had the old ids can tell). This is the Merkle property doing security work: integrity of the whole chain reduces to checking a single top hash.

<details>
<summary><strong>Editorial</strong></summary>

Build a commit chain where each commit hashes its parent's id into its own content — then build the tampered variant with a modified first message. Because the parent id is embedded, the change cascades: all three commit ids differ. Print the chains with explicit `join` (not Python's default list repr) so both langs produce identical output.

```python solution time=O(n) space=O(n)
import hashlib

def gid(t, c):
    return hashlib.sha1(f"{t} {len(c)}\0".encode() + c.encode()).hexdigest()[:8]

def chain(msgs):
    ids = []; parent = ""
    for m in msgs:
        cid = gid("commit", f"parent {parent} | {m}")
        ids.append(cid); parent = cid
    return ids

msg1 = input()
msg2 = input()
msg3 = input()
tampered_msg1 = input()

honest   = chain([msg1, msg2, msg3])
tampered = chain([tampered_msg1, msg2, msg3])
print("honest chain: " + "[" + ", ".join(honest) + "]")
print("tampered chain: " + "[" + ", ".join(tampered) + "]")
print("ids changed: " + str(sum(a != b for a, b in zip(honest, tampered))) + " of " + str(len(honest)))
```

```java solution
import java.security.MessageDigest;
import java.util.*;
public class Main {
    static String gid(String t, String c) throws Exception {
        byte[] body = c.getBytes("UTF-8");
        byte[] header = (t + " " + body.length + "\0").getBytes("UTF-8");
        byte[] all = new byte[header.length + body.length];
        System.arraycopy(header, 0, all, 0, header.length);
        System.arraycopy(body, 0, all, header.length, body.length);
        byte[] dig = MessageDigest.getInstance("SHA-1").digest(all);
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) sb.append(String.format("%02x", b & 0xff));
        return sb.substring(0, 8);
    }
    static List<String> chain(String[] msgs) throws Exception {
        List<String> ids = new ArrayList<>(); String parent = "";
        for (String m : msgs) { ids.add(gid("commit", "parent " + parent + " | " + m)); parent = ids.get(ids.size() - 1); }
        return ids;
    }
    public static void main(String[] x) throws Exception {
        Scanner sc = new Scanner(System.in);
        String msg1 = sc.nextLine(), msg2 = sc.nextLine(), msg3 = sc.nextLine(), tampered1 = sc.nextLine();
        List<String> honest = chain(new String[]{msg1, msg2, msg3});
        List<String> tampered = chain(new String[]{tampered1, msg2, msg3});
        System.out.println("honest chain: [" + String.join(", ", honest) + "]");
        System.out.println("tampered chain: [" + String.join(", ", tampered) + "]");
        int changed = 0; for (int i = 0; i < honest.size(); i++) if (!honest.get(i).equals(tampered.get(i))) changed++;
        System.out.println("ids changed: " + changed + " of " + honest.size());
    }
}
```

</details>

## Reflect & Connect

- **Content addressing is the master rule.** Identity = hash of content gives dedup, integrity, and a stable name, all at once. Reach for it whenever "same content should mean same thing" (build caches, CDNs, blockchains, container layers all do).
- **Structural sharing makes immutability affordable.** A new version copies only the changed path and shares the rest — the [persistent data structure](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/persistent-data-structures) idea at filesystem scale. Immutable history costs proportional to the change, not the repo.
- **The Merkle property = tamper-evidence.** Because each id folds in its children's (and parent's) hashes, one top hash certifies the entire structure — the same idea you met in the [distributed-systems teaser](/cortex/data-structures-and-algorithms/concurrency-and-systems/distributed-data-structures-teaser) for replica repair, here securing history.
- **Every command is a graph algorithm.** `git log` is a DAG walk from HEAD; `git diff` is tree comparison with hash short-circuiting; `git merge` finds a lowest common ancestor and three-way-merges; `git blame` walks the parent chain. Learn the structure and the porcelain becomes obvious.
- **Same lesson as the other systems.** As with [Postgres `nbtree`](/cortex/data-structures-and-algorithms/dsa-in-real-systems/postgres-b-tree-and-the-write-path), the [CFS scheduler](/cortex/data-structures-and-algorithms/dsa-in-real-systems/linux-red-black-tree-in-the-cfs-scheduler), and [Redis encodings](/cortex/data-structures-and-algorithms/dsa-in-real-systems/redis-internal-encodings), a clean data structure plus a few sharp engineering choices is the whole product.

## Recall

<details>
<summary><strong>Q:</strong> What does "content-addressed" mean in Git, and what does it buy?</summary>

**A:** An object's id is the SHA-1 of its content (over `"<type> <length>\0<content>"`). So identity equals content: two identical files share one object (dedup), and any change alters the id (tamper-evidence). One rule, both properties.

</details>
<details>
<summary><strong>Q:</strong> What are Git's three core object types and how do they reference each other?</summary>

**A:** Blob (raw file bytes), tree (a directory listing of `(mode, name, hash)` entries pointing at blobs/sub-trees), and commit (root tree hash + parent commit hash(es) + metadata). All references are by hash, forming a Merkle DAG.

</details>
<details>
<summary><strong>Q:</strong> In object terms, what does editing one file in a 100k-file repo cost?</summary>

**A:** One new blob (the edited file) + a few new trees along the path to the root + one new commit. The other ~99,999 blobs and untouched sub-trees are shared with the previous commit — path copying / structural sharing.

</details>
<details>
<summary><strong>Q:</strong> Why is <code>git diff A B</code> fast on a huge tree?</summary>

**A:** Equal tree hashes mean identical content, so Git short-circuits unchanged sub-trees and never descends into them. Only paths whose hashes differ are walked.

</details>
<details>
<summary><strong>Q:</strong> Why can't you quietly rewrite an old commit?</summary>

**A:** Each commit's hash folds in its parent's hash. Changing an ancestor changes its id, which changes its child's content and id, cascading to every descendant. The whole chain's integrity reduces to one top hash — the Merkle property.

</details>

## Sources & Verify

- **Pro Git** (Chacon & Straub), Chapter 10 "Git Internals" — the object model, content addressing, and pack files; the canonical reference.
- **Git source**: `object.c`, `tree.c`/`commit.c`/`blob.c` (per-type handlers), `diff-tree.c` (tree diff with hash short-circuit), `pack-objects.c` (delta packing), `fsck.c` (DAG integrity check). The plumbing commands `git hash-object`, `git cat-file -p`, and `git fsck` expose the structure directly.
- **Merkle** (1987), hash trees — the integrity primitive underneath; see the [distributed-systems teaser](/cortex/data-structures-and-algorithms/concurrency-and-systems/distributed-data-structures-teaser).
- The blob id computation, the structural-sharing trace (README blob reused while blob/tree/commit change), and the tamper cascade (`3 of 3` ids change) all come from the runnable blocks above, which use Git's exact object-hash formula — re-run to verify, or check a blob id against your own `git hash-object`.
