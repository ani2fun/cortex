---
title: '13. Consistency models'
summary: Six consistency levels along one axis, the anomalies each one allows, and the rule of thumb that matters more than the names — pick the weakest model that your *specific query* tolerates, not the strongest your database can offer.
---

# 13. Consistency models

## TL;DR
> Consistency is a **per-query** choice, not a cluster-wide setting. There is a spectrum — strong (linearizable) at one end; eventual at the other; monotonic-reads, read-your-writes, consistent-prefix, and causal in between — and each point allows specific anomalies to leak through. The senior moves are: (a) **know which anomalies you're signing up for**, and (b) **don't pay for stronger consistency than each query needs**. Linearizable reads on a query that's reading a "last seen" timestamp is just wasted latency; eventually-consistent reads on a query that determines account-balance authorisation is a future incident.

## 1. Motivation

In **2014**, Kyle Kingsbury published [*Strong consistency models*](https://aphyr.com/posts/313-strong-consistency-models) — the post that took the textbook definitions and put them on **one axis** with a clear lattice diagram. The lattice ranks every common consistency model by strictness, and shows which guarantees imply which. Strong serialisability implies linearisability, which implies causal, which implies the consistent-prefix and per-session models, and so on. (As §3.1 spells out, the lattice actually flattens *two* independent axes — transaction isolation and replication recency — onto one picture; they coincide only at the very top.) *Most production databases are eventually consistent by default, and the precise dialect of "eventual" varies in ways that surprise teams.*

The same axis explains why production systems mix consistency levels per query. A bank's `SELECT balance FROM accounts WHERE id = ?` ahead of a `UPDATE accounts SET balance = balance - 100` must be linearisable: a stale balance read leads to authorising a withdrawal against money already gone. The same bank's `SELECT user_id, last_login_at FROM users WHERE id = ?` on the analytics dashboard can be 30 seconds stale and nobody cares. Same database; different queries; different consistency.

The lesson: **a "consistent database" is the wrong question**. The right question is "*for this query*, what anomalies do I tolerate?" The answer almost always lets you pay less latency than the database's strongest mode would charge.

## 2. Intuition (Analogy)

A replicated database under load is **a busy newsroom**.

Three reporters (the writers) file different versions of the same story across the day. A team of fact-checkers (the readers) checks the story before publication. The question every consistency model answers is: **which version of the story do the fact-checkers see?**

- **Linearizable**: all fact-checkers, at any moment, see exactly the most recently filed version. There's a single canonical timeline pinned to *real* wall-clock time, and everyone agrees on it instantly. (Expensive: every fact-check has to wait for the wire to confirm.)
- **Sequential**: all fact-checkers agree on *one* order of filings, and each reporter's own filings appear in the order they filed them — but that agreed order need not match real wall-clock time across reporters. A filing that physically went out first can sit later in the shared order, as long as *everyone* sees that same order. (Note this is an *ordering* guarantee, not a "slightly stale" one — it is weaker than linearizable only because it drops the tie to real time, not because reads lag.)
- **Causal**: if reporter A files a follow-up that *references* reporter B's earlier version, fact-checkers cannot see A's follow-up without B's predecessor. Filings with no such dependency can be seen in any order. (Just enough order to keep stories coherent; cheaper still.)
- **Read-your-writes**: a *specific reporter* always sees their own most recent filing, but other reporters may lag. (Cheap; just need to pin the reporter's reads.)
- **Monotonic reads**: a fact-checker never sees the story go *backwards* — once they've seen version 5, they never go back to seeing version 4. (Cheap; just need to remember "what version did I last see".)
- **Eventual**: given a quiet day, all fact-checkers eventually see the same version. **No timing or ordering guarantee** on how they get there. (Cheapest; potentially confusing.)

The newsroom doesn't pick *one* policy for everyone. The sports desk's fact-checkers can be eventually consistent (yesterday's score is fine). The legal desk's fact-checkers need linearizable (a libel claim depends on the *current* status of a story). Same paper, different desks, different consistency.

## 3. Formal definitions

### 3.1 The lattice

Models, ranked strict → loose. A model further down "allows more anomalies" than one above.

| Level | Allows | Prevents | Example workload |
|---|---|---|---|
| **Strict serialisability** | nothing the database is responsible for | every isolation anomaly *and* stale reads (serialisable + linearisable) | airline-seat assignment with hard global ordering |
| **Linearisable** | non-determinism on ties; anything spanning more than one object | stale reads on a *single* register (a read always sees the latest committed write) | bank balance check before withdrawal |
| **Sequential** | an agreed order that doesn't match real wall-clock time across clients | disagreement about *what* order operations happened in | feed timeline within a session |
| **Causal** | reordering of operations that aren't causally related | reading a reply before its parent | comment threads, social-graph updates |
| **Read-your-writes (session)** | other users' updates not yet visible to you | your own write appearing missing on reread | "I posted a comment" pages |
| **Monotonic reads (session)** | data getting more stale | data going *backwards* within your session | infinite-scroll feeds |
| **Eventual** | any temporary ordering | nothing — convergence "eventually" | analytics dashboards, online presence indicators |

**The one definition worth memorising — linearisability is a recency guarantee on a single register.** Operations on one key/row/document appear to take effect instantaneously at some single point between when they start and when they finish, in an order consistent with real time. The operational consequence: *if operation A returns before operation B begins, B must observe A's effect (or something even newer)* — and once any reader sees the new value, every later read, from any client, sees it too. That is the whole guarantee. It is about one object and about *freshness*; it says nothing about atomicity across multiple objects, which is why it cannot, by itself, prevent a lost update or write skew that spans two rows.

**Two independent axes are flattened onto this lattice for convenience.** One axis is *transaction isolation* (read-committed → snapshot → serialisable — the subject of [Lesson 9](/cortex/system-design/building-blocks/relational-databases)); the other is *replication recency* (eventual → causal → linearisable). They are not a single ladder, and neither subsumes the other:

- A **serialisable** database can still hand you a *stale* snapshot — serialisability orders transactions correctly but makes no recency promise, so the consistent answer it gives you may be from a moment ago.
- A **linearisable** register is always current but offers no multi-row atomicity.

You get both at once only at the top of the lattice, in **strict serialisability** (sometimes "strong-1SR"). Spanner and FoundationDB provide it; CockroachDB is deliberately serialisable-but-not-strict, trading a little recency to avoid the coordination cost. Below the top row you can mix and match freely — snapshot isolation on a linearisable store, or serialisable isolation that still permits stale reads.

With that caveat in mind, the strict-to-loose ordering still buys you something: if your database offers linearisable reads, you automatically have causal / read-your-writes / monotonic-reads "for free" *on that query*. If it offers eventual, you have nothing.

### 3.2 The anomalies, by example

These are what each consistency level lets through. The widget from [Lesson 11](/cortex/system-design/building-blocks/replication) makes some of them visible directly.

**Stale read** — a read returns a value that's already been overwritten by a more recent write. *Allowed* by every model below linearisable. The `ReplicationLagSimulator` shows this directly.

**Read-your-writes failure** — a user writes a value, immediately reads it, doesn't see it. *Allowed* by eventual + monotonic-only models; prevented by read-your-writes, sequential, linearisable.

**Reading backwards** — the same user/session reads value `A`, then reads it again and sees an *older* value `B` that pre-dates `A`. *Allowed* by eventual only; prevented by monotonic-reads, sequential, linearisable.

**Causal violation** — a user posts a reply to a comment they haven't yet seen exist (because the comment lives on a replica they haven't read). *Allowed* by everything below causal; prevented by causal, sequential, linearisable.

**Write skew** — two transactions read overlapping data, decide independently, write disjoint rows that *together* violate an invariant. The canonical example: "at least one doctor must be on call" — two doctors each read "two of us are on call" and decide to take a break. *Reliably prevented only by serialisable isolation* (or by manually locking the rows you read, e.g. `SELECT ... FOR UPDATE` on the read set) — **not** by linearisable, which governs a single object and so never sees the cross-row invariant.

**Phantom read** — a query reading "all rows matching X" sees a different set across two reads because another transaction inserted a new row matching X. Prevented by serialisable / Postgres's REPEATABLE READ (snapshot).

**Lost update** — two transactions read the same value, both increment in memory, both write back; one's update silently overwrites the other. *Allowed by* READ COMMITTED (the Postgres default; covered in [Lesson 9](/cortex/system-design/building-blocks/relational-databases)). Prevented by SERIALIZABLE, by `SELECT FOR UPDATE`, and — specifically in Postgres — by REPEATABLE READ (which is snapshot isolation under the hood and *detects* the conflict; note the SQL-standard REPEATABLE READ makes no such promise — "repeatable read" is a famously under-specified term).

### 3.3 What different storage shapes offer by default

| System | Default | Strongest available | How to ask for it |
|---|---|---|---|
| Postgres (single primary) | READ COMMITTED | SERIALIZABLE | `SET TRANSACTION ISOLATION LEVEL SERIALIZABLE` |
| Postgres (with replicas) | eventually consistent on replicas | linearisable on the primary | route the query to the primary |
| Cassandra | LOCAL_ONE (eventual) | QUORUM + Lightweight Transactions | per-query `CONSISTENCY QUORUM`; LWT for compare-and-swap |
| DynamoDB | eventually consistent reads | strongly consistent reads | `ConsistentRead=true` on the request |
| MongoDB (replica set) | `readConcern: local` on the primary | `readConcern: linearizable` (single-doc) | set `readConcern` per query; causal sessions for causal |
| etcd (v3) | linearisable reads *and* writes | linearisable | default — built for this |
| ZooKeeper | linearisable *writes*; reads may be stale | linearisable reads | issue a `sync` before the read (reads needn't come from the current leader) |

The pattern: **most systems default to the weakest practical model**, and offer stronger modes as a per-query opt-in with extra latency. Knowing which knob to flip for which query is the lesson.

### 3.4 Connecting back to CAP / PACELC

[Lesson 4 (CAP and PACELC, honestly)](/cortex/system-design/foundations/cap-and-pacelc) introduced the *partition* dimension. Consistency models live on the *non-partition* axis — the "L" (latency) or "C" (consistency) trade-off in PACELC, *during normal operation*. The `PartitionSimulator` widget from Foundations 4 still applies here: under a partition, a CP system stays strongly consistent (refuses some writes); an AP system serves stale data (continues writes). What lesson 13 adds is: **even during normal operation** (no partition), you choose a consistency level per query; even an "AP system" can give you strong reads if you ask for the right primitive (Cassandra's `CONSISTENCY ALL` or LWT).

It's worth being precise about *why* a partition forces that CP-or-AP choice for linearisable systems. Linearisability isn't merely slow under a partition — it is **impossible to serve on the disconnected side**. A replica cut off from the leader cannot know whether a newer write has been committed elsewhere, so to stay linearisable it must either block until the network heals or return an error. That is the real content of CAP: under a partition, a linearisable system *cannot* remain available on both sides. **Causal consistency is the special case worth remembering: it is the strongest model a replica can keep honouring while still answering every request during a partition**, because causal order is tracked with logical clocks (happens-before) and needs no cross-node coordination. So the principled middle isn't "linearisable-vs-stale" — causal sits between them and pays neither price.

The hardest level — **linearisability** — is the strongest single-key guarantee any practical system offers, and it is what **consensus algorithms** ([Lesson 14](/cortex/system-design/building-blocks/consensus-paxos-and-raft)) actually provide. Linearisable reads on etcd cost ~1 RTT to the Raft leader; everything weaker is the database choosing to short-circuit that path. When a system claims "strongly consistent reads", what it usually means under the hood is "we route this query to a node that ran consensus on its local state".

And this latency isn't an implementation accident you can engineer away. Attiya and Welch proved that the response time of linearisable reads and writes is *at least proportional to the uncertainty in network delay* — there is no clever algorithm that beats it. The slowness is therefore present **all the time, not only during a network fault**, which is exactly why most systems weaken consistency for *latency*, not for partition tolerance: the cost is there even when the network is perfectly healthy.

## 4. Worked example — three queries, three different consistency levels

Suppose you're building a banking app. Three queries:

1. **`SELECT balance FROM accounts WHERE id = ?` followed by `UPDATE balance = balance - 100`** — withdrawal authorisation.
2. **`SELECT name, last_login_at FROM users WHERE id = ?`** — render the account-summary header.
3. **`SELECT timestamp, amount FROM transactions WHERE account_id = ? ORDER BY timestamp DESC LIMIT 50`** — render the transactions list.

What consistency level does each need?

**Query 1 — needs linearisable.** A stale balance read leads to over-authorisation. The standard implementation: a transaction with `SELECT ... FOR UPDATE` (taking a row lock) or `SERIALIZABLE` isolation, routed to the primary. The cost: one cross-region RTT to the primary, plus the lock-hold latency. Worth every microsecond.

**Query 2 — eventual is fine.** A 30-second stale `last_login_at` doesn't matter to anyone. Route to the nearest replica. Cheap.

**Query 3 — needs *causal* (and only causal).** A user who just made a deposit needs to see the deposit row in their list. But they don't need to see *other* users' transactions in real-time order. The right pattern: tag the read with the LSN of the user's most recent write; the replica blocks the read until it's caught up to that LSN. Cheap most of the time; mildly slow right after a write.

The lesson is **three different consistency levels on three queries in the same screen**. Picking the strongest for everything wastes latency; picking the weakest for everything is incident-bait. Picking right per query is the senior move.

This is the same axis the `ReplicationLagSimulator` from [Lesson 11](/cortex/system-design/building-blocks/replication) illustrates: drag the read-delay slider against the replication lag, and observe the threshold at which the read sees the latest write. *Linearisable reads pay the latency to always be above the threshold; eventually-consistent reads accept being below it.*

## 5. Trade-offs

| Choice | What you give up | What you get |
|---|---|---|
| Linearisable reads on every query | extra latency on reads that don't need it | provable correctness; can reason as if single-machine |
| Eventually consistent reads on every query | predictable anomaly potential | low read latency, high read throughput |
| Per-query consistency selection | application-side reasoning effort | the latency / correctness floor you actually need |
| Read-your-writes pattern (session pinning) | flexible read routing | users don't see their own writes "disappear" |
| Causal session tokens | implementation complexity at the client | replies don't precede their parents |
| `SERIALIZABLE` isolation everywhere | many serialisation-failure retries | the strongest single-machine guarantee |
| LWT / CAS in Cassandra | a Paxos round (~50 ms) per CAS | atomic compare-and-swap on quorum reads/writes |

The default modern stack: **READ COMMITTED on Postgres, per-query `SELECT FOR UPDATE` where you need linearisability, route to primary within a TTL after writes for read-your-writes, accept eventual on the rest.** For Cassandra/Dynamo, the analogous setup is `QUORUM` reads + LWT for the contended writes + `ONE` for the bulk read paths.

## 6. Edge cases and failure modes

### 6.1 "Eventually" is unbounded by default

"Eventually consistent" gives you no timeline. The replica is *eventually* up to date; "eventually" could be 50 ms or 50 minutes. In a healthy system it's milliseconds; under load or after partition recovery it can be much longer. **Always know your p99 replication lag in production**, and document the SLO. Mitigation: monitor `replay_lag` (Postgres), `replication_lag` (DynamoDB), `replication latency` (Cassandra) and alert when it exceeds the application's tolerance.

### 6.2 Read-your-writes ≠ session affinity

"Stick the user to one replica" only fixes read-your-writes if their write also went to that replica. In a single-leader system, writes go to the primary; reads are served from replicas; so session-pinning the *user* to a replica doesn't help — the replica still trails the primary. The correct fix: after a write, route the same user's reads to the primary for a TTL (or use causal session tokens). [Lesson 11 §6.3](/cortex/system-design/building-blocks/replication) covers this.

### 6.3 "Strong reads" in DynamoDB are stronger than people think — but also weaker

DynamoDB's "strongly consistent reads" guarantee linearisability of that one key within one table. They do *not* guarantee anything across keys or across tables. Two strong reads in the same request — `getItem(user)` then `getItem(profile)` — can see inconsistent snapshots. The fix in DynamoDB is `TransactGetItems`, which does linearise across multiple items.

### 6.4 Causal consistency requires causality tracking

Causal consistency is the smallest model that prevents the "reply visible before parent" anomaly. It requires the *client* (or the database) to track which writes a session has observed, and pass that vector to subsequent reads. The cost is bookkeeping at the client; the benefit is most of the common consistency intuitions hold. Few off-the-shelf databases offer it natively; MongoDB's *causally consistent sessions* are one of the cleanest implementations.

### 6.5 Monotonic-reads with replica failover

A user reads version 5 from replica A. Replica A dies. The user's next read goes to replica B, which has version 3. The user just observed time going backwards. Mitigation: track the LSN/version returned by every replica; on failover, refuse to use a replica that's behind that LSN until it has caught up. Some Postgres connection poolers (`pgbouncer` with `application_name` routing) implement this; client-side LSN tracking via `pg_current_wal_lsn()` works in the meantime.

### 6.6 Cross-region linearisable reads are expensive

A linearisable read in a single-region database is fast (one local round-trip). A linearisable read in a multi-region setup costs at least one cross-region RTT — you must contact the primary, which lives in one region. Production systems with strict latency budgets either (a) accept eventual cross-region and pay for it operationally, or (b) regionalise the data (each region's users get their own region's primary). Spanner offers cross-region linearisability via TrueTime; the cost is a *commit-wait* on every write — Spanner deliberately pauses for its clock-uncertainty window (single-digit milliseconds in Google's deployment) before returning, so that timestamps it hands out are guaranteed to be in the past.

## 7. Practice

### Exercise 1 — Diagnose the anomaly

A user opens your social-media app, sees their friend's new post in their feed, replies "great point!", then refreshes the page. The friend's original post is gone but their own reply is still there. **Which consistency model would have prevented this, and where in your stack is the bug?**

<details>
<summary>Solution</summary>

The user is observing a **causal-consistency violation** (a reply to a parent that no longer appears) combined with **monotonic-reads failure** (seeing the post once, then not seeing it).

The bug is almost certainly that the feed-rendering query is *eventually consistent* and hits a load-balanced replica that hasn't yet seen the original post — even though the *post-display* page that triggered the reply did see it. Two different replicas, two different views.

**Causal consistency** (with session tokens) would prevent it: the client carries the LSN of the highest write the session has observed; replicas refuse to serve a read until they've replayed up to that LSN.

In a typical stack, the lighter fix is **session-pin the user to a single replica for read-your-writes-like queries**. Heavier fix: use a replica-aware client that exposes the read's LSN and refuses to fall back to a replica trailing it.

</details>

### Exercise 2 — Pick the model

For each query in a chat application, name the *weakest* consistency model that's acceptable.

1. "Show me the channels I'm a member of."
2. "Show me the unread-message count for each of my channels."
3. "Did my message I just sent actually deliver?"
4. "Mark these 50 messages as read."

<details>
<summary>Solution</summary>

1. **Eventual.** Channel membership changes slowly; a few seconds of stale state is fine.

2. **Monotonic reads + read-your-writes**, OR **eventual with explicit reload**. The unread count should not appear to go *backwards* (monotonic) and should reflect a freshly-sent message from the same user (read-your-writes). Both achievable with session-pinned reads.

3. **Linearisable** — or as close as the system can offer. The user needs the truth right now: did the write succeed or not? Reading from the leader (single-leader) or `CONSISTENCY ALL` (Cassandra) or strong reads (DynamoDB). Note that this is *one query* — the rest of the screen can stay eventually consistent.

4. **Strong read of own write + eventual consistency on the side effect.** The "mark as read" update needs to be durable + observable to this user (read-your-writes). The side effect — other users seeing the read state in real-time — can be eventually consistent (a sender doesn't need instant feedback that you read their message).

</details>

### Exercise 3 — Quantify the cost of going stronger

A read query in your app currently takes 8 ms (served from a same-region replica). Your team is considering switching it to a linearisable read against the primary in another region 80 ms away. **What's the latency cost, and when is it justified?**

<details>
<summary>Solution</summary>

The latency cost is the difference: **80 ms primary RTT vs 8 ms replica read = +72 ms p50**. At p99 it's typically 2–3× worse if the cross-region link is congested — say **+200 ms p99**.

That cost is justified when:

- **Correctness genuinely requires it.** A balance read before authorising a withdrawal; an inventory count before reserving a seat; a uniqueness check before allocating an ID. The cost of getting it wrong is high *and* not recoverable by retry.
- **The frequency is low.** A linearisable read on every page load of your app at 10 000 req/s adds 720 seconds of aggregate latency per second of clock time — and might saturate the cross-region link. A linearisable read at checkout (~10/s) adds 720 ms aggregate per second — negligible.

It's *not* justified when:

- **The use case is read-your-writes for the same user.** Cheaper to route the user's reads to the primary's region for a short TTL after writes (not switch every query globally).
- **The use case is "I'd like to be 100% sure".** Most "I'd like to be sure" reads can tolerate a few seconds of staleness; the cost of being wrong is "the screen updates a second later", not an incident.

Senior heuristic: **the latency budget of a UI screen is the budget for the slowest query on it**. If the screen has one query that needs linearisable + cross-region, every other query on that screen can stay eventually-consistent because they're not the bottleneck. Don't pay for consistency that doesn't change your latency floor.

</details>

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## 8. In the Wild

- **Martin Kleppmann, *Designing Data-Intensive Applications*, 2nd ed., Ch. 10 "Consistency and Consensus."** The canonical deep-dive, and the source for the linearisability-vs-serialisability distinction, the recency-guarantee definition, and the "unhelpful CAP theorem" critique used throughout this chapter.
- **[Kyle Kingsbury, *Strong consistency models*](https://aphyr.com/posts/313-strong-consistency-models)** (2014). The post that puts every common model on one lattice diagram. Required reading if you're going to design with consistency in mind.
- **[Werner Vogels, *Eventually Consistent*, CACM 2009](https://queue.acm.org/detail.cfm?id=1466448)** — Amazon CTO's classic on the spectrum of consistency, written for the practitioner.
- **[Daniel Abadi, *Consistency Tradeoffs in Modern Distributed Database Systems*](https://cs.umd.edu/~abadi/papers/abadi-pacelc.pdf)** (2012) — the paper that introduced PACELC. Pairs naturally with [Lesson 4](/cortex/system-design/foundations/cap-and-pacelc).
- **[The Jepsen analysis archives](https://jepsen.io/analyses)** — every popular database has been Jepsen-tested. Each report is a deep look at what a system *actually* delivers vs what it claims.
- **[Doug Terry, *Replicated Data Consistency Explained Through Baseball*](https://www.microsoft.com/en-us/research/wp-content/uploads/2011/10/ConsistencyAndBaseballReport.pdf)** (Microsoft Research, 2011). The canonical worked example — different roles in a baseball game need different consistency levels.

---

> **Next:** [14. Consensus — Paxos and Raft, from scratch](/cortex/system-design/building-blocks/consensus-paxos-and-raft) — the algorithms that *let* you build a linearisable system on top of unreliable nodes. The widget there is the most ambitious in the catalog: a Raft-cluster animator that walks leader election + log replication + leader failover step by step.
