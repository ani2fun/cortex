---
title: '13. Consistency models'
summary: Six consistency levels along one axis, the anomalies each one allows, and the rule of thumb that matters more than the names — pick the weakest model that your *specific query* tolerates, not the strongest your database can offer.
---

# 13. Consistency models

## TL;DR
> Consistency is a **per-query** choice, not a cluster-wide setting. There are six points on the spectrum — strong (linearizable) at one end; eventual at the other; monotonic-reads, read-your-writes, sequential, and causal in between — and each one allows specific anomalies to leak through. The senior moves are: (a) **know which anomalies you're signing up for**, and (b) **don't pay for stronger consistency than each query needs**. Linearizable reads on a query that's reading a "last seen" timestamp is just wasted latency; eventually-consistent reads on a query that determines account-balance authorisation is a future incident.

## 1. Motivation

In **2014**, Kyle Kingsbury published [*Strong consistency models*](https://aphyr.com/posts/313-strong-consistency-models) — the post that took the textbook definitions and put them on **one axis** with a clear lattice diagram. The lattice ranks every common consistency model by strictness, and shows which guarantees imply which. Strong serialisability implies linearisability, which implies sequential, which implies causal, which implies all the per-key models, and so on. *Most production databases are eventually consistent by default, and the precise dialect of "eventual" varies in ways that surprise teams.*

The same axis explains why production systems mix consistency levels per query. A bank's `SELECT balance FROM accounts WHERE id = ?` ahead of a `UPDATE accounts SET balance = balance - 100` must be linearisable: a stale balance read leads to authorising a withdrawal against money already gone. The same bank's `SELECT user_id, last_login_at FROM users WHERE id = ?` on the analytics dashboard can be 30 seconds stale and nobody cares. Same database; different queries; different consistency.

The lesson: **a "consistent database" is the wrong question**. The right question is "*for this query*, what anomalies do I tolerate?" The answer almost always lets you pay less latency than the database's strongest mode would charge.

## 2. Intuition (Analogy)

A replicated database under load is **a busy newsroom**.

Three reporters (the writers) file different versions of the same story across the day. A team of fact-checkers (the readers) checks the story before publication. The question every consistency model answers is: **which version of the story do the fact-checkers see?**

- **Linearizable**: all fact-checkers, at any moment, see exactly the most recently filed version. There's a single canonical timeline, and everyone agrees on it instantly. (Expensive: every fact-check has to wait for the wire to confirm.)
- **Sequential**: all fact-checkers see *the same order* of filings, but the version they're looking at might lag the most recent by a bit. (Cheaper: a fact-checker can serve from a cached version as long as it doesn't go backwards.)
- **Causal**: if reporter A files a follow-up that *references* reporter B's earlier version, fact-checkers cannot see A's follow-up without B's predecessor. (Just enough order to keep stories coherent; cheaper still.)
- **Read-your-writes**: a *specific reporter* always sees their own most recent filing, but other reporters may lag. (Cheap; just need to pin the reporter's reads.)
- **Monotonic reads**: a fact-checker never sees the story go *backwards* — once they've seen version 5, they never go back to seeing version 4. (Cheap; just need to remember "what version did I last see".)
- **Eventual**: given a quiet day, all fact-checkers eventually see the same version. **No timing or ordering guarantee** on how they get there. (Cheapest; potentially confusing.)

The newsroom doesn't pick *one* policy for everyone. The sports desk's fact-checkers can be eventually consistent (yesterday's score is fine). The legal desk's fact-checkers need linearizable (a libel claim depends on the *current* status of a story). Same paper, different desks, different consistency.

## 3. Formal definitions

### 3.1 The lattice

Models, ranked strict → loose. A model further down "allows more anomalies" than one above.

| Level | Allows | Prevents | Example workload |
|---|---|---|---|
| **Strict serialisability** | nothing | every concurrency anomaly | airline-seat assignment with hard global ordering |
| **Linearisable** | non-determinism on ties | stale reads, lost updates within the linearisation order | bank balance check before withdrawal |
| **Sequential** | reads slightly behind writes (per replica) | reordering of operations across users | feed timeline within a session |
| **Causal** | reads that don't preserve real-time order | reading a reply before its parent | comment threads, social-graph updates |
| **Read-your-writes (session)** | other users' updates not yet visible to you | your own write appearing missing on reread | "I posted a comment" pages |
| **Monotonic reads (session)** | data getting more stale | data going *backwards* within your session | infinite-scroll feeds |
| **Eventual** | any temporary ordering | nothing — convergence "eventually" | analytics dashboards, online presence indicators |

The strict-to-loose ordering matters: if your database offers linearisable, you automatically have sequential / causal / read-your-writes / monotonic-reads "for free" *on that query*. If it offers eventual, you have nothing.

### 3.2 The anomalies, by example

These are what each consistency level lets through. The widget from [Lesson 11](/cortex/system-design/building-blocks/replication) makes some of them visible directly.

**Stale read** — a read returns a value that's already been overwritten by a more recent write. *Allowed* by every model below linearisable. The `ReplicationLagSimulator` shows this directly.

**Read-your-writes failure** — a user writes a value, immediately reads it, doesn't see it. *Allowed* by eventual + monotonic-only models; prevented by read-your-writes, sequential, linearisable.

**Reading backwards** — the same user/session reads value `A`, then reads it again and sees an *older* value `B` that pre-dates `A`. *Allowed* by eventual only; prevented by monotonic-reads, sequential, linearisable.

**Causal violation** — a user posts a reply to a comment they haven't yet seen exist (because the comment lives on a replica they haven't read). *Allowed* by everything below causal; prevented by causal, sequential, linearisable.

**Write skew** — two transactions read overlapping data, decide independently, write disjoint rows that *together* violate an invariant. The canonical example: "at least one doctor must be on call" — two doctors each read "two of us are on call" and decide to take a break. *Only prevented by serialisable*, not even by linearisable (which is single-key).

**Phantom read** — a query reading "all rows matching X" sees a different set across two reads because another transaction inserted a new row matching X. Prevented by serialisable / Postgres's REPEATABLE READ (snapshot).

**Lost update** — two transactions read the same value, both increment in memory, both write back; one's update silently overwrites the other. *Allowed by* READ COMMITTED (the Postgres default; covered in [Lesson 9](/cortex/system-design/building-blocks/relational-databases)). Prevented by SERIALIZABLE, REPEATABLE READ in Postgres, or `SELECT FOR UPDATE`.

### 3.3 What different storage shapes offer by default

| System | Default | Strongest available | How to ask for it |
|---|---|---|---|
| Postgres (single primary) | READ COMMITTED | SERIALIZABLE | `SET TRANSACTION ISOLATION LEVEL SERIALIZABLE` |
| Postgres (with replicas) | eventually consistent on replicas | linearisable on the primary | route the query to the primary |
| Cassandra | LOCAL_ONE (eventual) | QUORUM + Lightweight Transactions | per-query `CONSISTENCY QUORUM`; LWT for compare-and-swap |
| DynamoDB | eventually consistent reads | strongly consistent reads | `ConsistentRead=true` on the request |
| MongoDB (replica set) | `readConcern: local` on the primary | `readConcern: linearizable` (single-doc) | set `readConcern` per query; causal sessions for causal |
| etcd / ZooKeeper | linearisable | linearisable | default — they're built for this |

The pattern: **most systems default to the weakest practical model**, and offer stronger modes as a per-query opt-in with extra latency. Knowing which knob to flip for which query is the lesson.

### 3.4 Connecting back to CAP / PACELC

[Lesson 4 (CAP and PACELC, honestly)](/cortex/system-design/foundations/cap-and-pacelc) introduced the *partition* dimension. Consistency models live on the *non-partition* axis — the "L" (latency) or "C" (consistency) trade-off in PACELC, *during normal operation*. The `PartitionSimulator` widget from Foundations 4 still applies here: under a partition, a CP system stays strongly consistent (refuses some writes); an AP system serves stale data (continues writes). What lesson 13 adds is: **even during normal operation** (no partition), you choose a consistency level per query; even an "AP system" can give you strong reads if you ask for the right primitive (Cassandra's `CONSISTENCY ALL` or LWT).

The hardest level — **linearisability** — is the strongest single-key guarantee any practical system offers, and it is what **consensus algorithms** ([Lesson 14](/cortex/system-design/building-blocks/consensus-paxos-and-raft)) actually provide. Linearisable reads on etcd cost ~1 RTT to the Raft leader; everything weaker is the database choosing to short-circuit that path. When a system claims "strongly consistent reads", what it usually means under the hood is "we route this query to a node that ran consensus on its local state".

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

A linearisable read in a single-region database is fast (one local round-trip). A linearisable read in a multi-region setup costs at least one cross-region RTT — you must contact the primary, which lives in one region. Production systems with strict latency budgets either (a) accept eventual cross-region and pay for it operationally, or (b) regionalise the data (each region's users get their own region's primary). Spanner offers cross-region linearisability via TrueTime; the cost is a 7–10 ms commit latency floor anyway.

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

## 8. In the Wild

- **[Kyle Kingsbury, *Strong consistency models*](https://aphyr.com/posts/313-strong-consistency-models)** (2014). The post that puts every common model on one lattice diagram. Required reading if you're going to design with consistency in mind.
- **[Werner Vogels, *Eventually Consistent*, CACM 2009](https://queue.acm.org/detail.cfm?id=1466448)** — Amazon CTO's classic on the spectrum of consistency, written for the practitioner.
- **[Daniel Abadi, *Consistency Tradeoffs in Modern Distributed Database Systems*](https://cs.umd.edu/~abadi/papers/abadi-pacelc.pdf)** (2012) — the paper that introduced PACELC. Pairs naturally with [Lesson 4](/cortex/system-design/foundations/cap-and-pacelc).
- **[The Jepsen analysis archives](https://jepsen.io/analyses)** — every popular database has been Jepsen-tested. Each report is a deep look at what a system *actually* delivers vs what it claims.
- **[Doug Terry, *Replicated Data Consistency Explained Through Baseball*](https://www.microsoft.com/en-us/research/wp-content/uploads/2011/10/ConsistencyAndBaseballReport.pdf)** (Microsoft Research, 2011). The canonical worked example — different roles in a baseball game need different consistency levels.

---

> **Next:** [14. Consensus — Paxos and Raft, from scratch](/cortex/system-design/building-blocks/consensus-paxos-and-raft) — the algorithms that *let* you build a linearisable system on top of unreliable nodes. The widget there is the most ambitious in the catalog: a Raft-cluster animator that walks leader election + log replication + leader failover step by step.
