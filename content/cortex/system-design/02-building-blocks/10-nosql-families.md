---
title: '10. NoSQL families'
summary: KV, document, wide-column, graph — four very different data models with very different access patterns. The senior move is not picking the trendiest one; it's recognising which shape your problem actually has, and accepting that for many problems the shape is still "Postgres".
---

# 10. NoSQL families

## TL;DR
> "NoSQL" is not one thing; it's the name we give to **four families of databases with very different shapes**. KV stores (DynamoDB, Redis, etcd) get you out of the way for `get(key) → value`. Document stores (MongoDB, Couchbase) let each row be its own JSON. Wide-column stores (Cassandra, ScyllaDB) optimise for `(partition, row, column)` access at huge scale. Graph databases (Neo4j, ArangoDB) make multi-hop relationship queries cheap. The lesson is not "NoSQL beats SQL" — it's **the shape of your access pattern is the load-bearing decision**, and once you can name the four shapes, the choice becomes much easier. For most workloads that look "NoSQL-ish", a well-indexed [relational database](/cortex/system-design/building-blocks/relational-databases) is still the right answer.

## 1. Motivation

In **2017**, Discord published [*How Discord Stores Billions of Messages*](https://discord.com/blog/how-discord-stores-billions-of-messages). At the time, Discord stored **billions** of messages — and the engineering decision they walked through was not "do we use NoSQL". It was: *given that we already know our access pattern is "fetch the most recent N messages for a channel, paginated backwards", what data model serves that query at our scale?*

The answer was Cassandra. Specifically, a table partitioned by `((channel_id, bucket), message_id)` — where `bucket` is a time slice of about 10 days. That choice of primary key was the entire system design. It put all messages for a given channel-and-time-slice on the same physical node (cheap to read together), broke up huge channels across many partitions (no hot partitions), and made pagination naturally efficient (the leftmost columns in Cassandra are the partition key; the right columns are the clustering key, which gives ordered access).

Six years later, Discord [published a follow-up](https://discord.com/blog/how-discord-stores-trillions-of-messages): the data was now in *trillions*, and the storage had moved from Cassandra to ScyllaDB — same data model, same partitioning, but a different engine. The data model was right; the engine got swapped.

That is the lesson. **The data model — the shape of the keys and the access pattern over them — is what you actually commit to.** The specific database product is replaceable. Picking NoSQL because it's trendy without first knowing what shape your data has is how teams end up rewriting their storage layer two years in. Picking the *right* shape, even with the wrong product, lets the next migration be a swap of engines, not a rewrite of the application.

## 2. Intuition (Analogy)

A paper-records office holds **five kinds of records**, each in their own storage shape:

- The **bookkeeper's ledger** is the relational database from [Lesson 9](/cortex/system-design/building-blocks/relational-databases). Rows, columns, ledger pages, lookup books. Strong consistency. Joins are first-class.
- The **shoe rack at the front door** is the **key-value store**. Every shoe has a tag (the key). To get a shoe, you read the tag, you grab the shoe. No querying "all the shoes my size" — that's not what the rack does.
- The **filing cabinet of variable-shape folders** is the **document store**. Each folder contains a self-describing document (a JSON-ish blob). Folders are alphabetised by their name; you can also build alternate indexes by content (Mongo's secondary indexes), but each folder is its own world.
- The **sparse spreadsheet keyed by `(partition, row, column)`** is the **wide-column store**. Every row in a partition can have a wildly different set of columns; most rows in fact have very few. The clustering allows "give me all the columns for this `(partition, row)`" to be one disk seek.
- The **family-tree wall** is the **graph database**. Boxes are people (nodes); lines are relationships (edges). Asking "who are the cousins of cousins of Alice through marriage and adoption" is one traversal — and it's exactly the question graph DBs are built for.

The senior move is not "pick the shoe rack because it's the newest piece of furniture in the office". It's **recognising, before you commit, which storage shape matches the question you'll ask the most often**. A workload that wants "give me message #4982 for channel #17" is shoe-rack-shaped. A workload that wants "find all users who follow followers of @alice" is family-tree-shaped. A workload that wants "show me this user's order history" is ledger-shaped — and a properly-indexed Postgres serves it forever.

## 3. Formal definitions

### 3.1 Key-value (KV) stores

**Shape:** `key → value`. The value is opaque to the database — a string, a JSON blob, a serialised protobuf, anything. The only operations are `get(key)`, `put(key, value)`, `delete(key)`.

**Examples:** **DynamoDB** (managed, AWS), **Redis** (in-memory, often used as a side cache — see [Lesson 8](/cortex/system-design/building-blocks/caching)), **etcd / Consul** (config + service discovery, with strong consistency), **RocksDB** (embedded, the underlying engine for many other things).

**Scaling shape:** partition the keyspace via [consistent hashing](/cortex/system-design/building-blocks/load-balancing). Each node owns a slice of the ring. Adding a node moves `1/N` of the keys; removing one moves another `1/N`.

| Property | KV stores |
|---|---|
| Primary access | `get(key)` — single-key reads |
| Secondary indexes | foreign concept; DynamoDB grafts them on as GSI/LSI at significant cost |
| Transactions | usually single-key only; some (DynamoDB, FoundationDB) offer multi-key |
| Joins | not supported |
| Consistency model | tunable (DynamoDB) or strong (etcd) or eventual (Redis cluster) |
| When to reach for | one or two well-known access patterns, very high QPS, scale beyond a single Postgres |

### 3.2 Document stores

**Shape:** *collections* of *documents*, where each document is a JSON-ish (BSON, MessagePack, etc.) blob with its own structure. Documents within a collection don't have to share a schema.

**Examples:** **MongoDB** (the canonical), **Couchbase**, **DocumentDB** (AWS managed Mongo-compatible), **FerretDB**.

**Scaling shape:** shard collections by a chosen field. Each shard is a [replica set](/cortex/system-design/building-blocks/replication) (a primary + N secondaries).

| Property | Document stores |
|---|---|
| Primary access | `find({field: value})` — flexible queries on document fields |
| Secondary indexes | native; can index any field or nested field |
| Transactions | per-document atomicity; multi-doc since Mongo 4.0 (slower) |
| Joins | limited (`$lookup` aggregation); generally denormalise instead |
| Consistency model | per-replica-set strong; configurable read concerns |
| When to reach for | variable-shape data (e.g. CMS content, product catalogs), rapid schema iteration |

The "rapid schema iteration" point is the most common pitch — and the most common foot-gun. Schema-less doesn't mean schema-free; it means **schema is enforced at the application layer instead of the database**. Two years in, you have documents from five generations of code in the same collection. Migrations become "rewrite every document in a background job".

### 3.3 Wide-column stores

**Shape:** keyed by `(partition_key, clustering_key) → row of sparse columns`. The partition key determines which node holds the data; the clustering key orders rows within a partition. Each row can have any subset of columns.

**Examples:** **Cassandra** (the canonical; from Facebook, 2009 paper), **ScyllaDB** (C++ rewrite of Cassandra; same data model, much faster), **HBase** (Hadoop-era; Google Bigtable-shaped), **DynamoDB** (the wide-column flavour, distinct from its KV mode).

**Scaling shape:** consistent hashing on the partition key. Adding a node moves a slice of partitions. The clustering key within a partition is stored sorted, so "give me messages 1000–2000 in channel 42" is one disk seek + one sequential scan.

| Property | Wide-column stores |
|---|---|
| Primary access | range scans on the *clustering* key within a partition |
| Secondary indexes | exist (Cassandra "materialised views", "secondary indexes") but discouraged at scale |
| Transactions | single-partition atomicity; "lightweight transactions" exist but are slow |
| Joins | not supported |
| Consistency model | tunable per query: ONE / QUORUM / ALL on reads and writes |
| When to reach for | time-series-like access at huge scale; `(entity, time)` shaped queries |

The phrase "the partition key is the entire design" is not hyperbole. Once you commit to a partition key, you cannot retrofit a different one without rewriting the table. Discord's choice of `((channel_id, bucket), message_id)` is a precise engineering decision: `channel_id` alone would put all of `#general`'s 100M messages on one node (hot partition); `(channel_id, bucket)` cuts every channel into 10-day-slice partitions, keeping them all roughly equal size.

### 3.4 Graph databases

**Shape:** *nodes* (entities) connected by *edges* (relationships). Both can carry arbitrary properties. Queries walk the graph — "find friends of friends of Alice", "trace the path of money from A to B through any number of intermediaries".

**Examples:** **Neo4j** (the canonical, the Cypher query language), **ArangoDB** (multi-model: doc + graph), **Amazon Neptune** (managed), **JanusGraph** (open source).

**Scaling shape:** harder than the other families. Most graph DBs assume a single-machine fit because graph queries don't shard cleanly — a multi-hop traversal that crosses partition boundaries is a network round-trip per hop.

| Property | Graph databases |
|---|---|
| Primary access | `MATCH (a)-[*1..5]-(b) WHERE ...` — multi-hop traversals |
| Secondary indexes | on node + edge properties |
| Transactions | full ACID (Neo4j) |
| Joins | implicit — every edge is a "join" |
| Consistency model | strong (single-machine), more complex when distributed |
| When to reach for | social networks, recommendation engines, fraud detection (path tracing), knowledge graphs |

The killer use case is the one nobody can do well in SQL: **multi-hop traversal queries**. In SQL, "friends of friends" is a 2-table join; "friends of friends of friends" is a 3-table join; depth-N traversals become unmaintainable. Neo4j's Cypher expresses these as `MATCH (a)-[:FOLLOWS*2..3]-(b)`, and the engine walks the graph in time proportional to the number of edges visited, not the total dataset size.

## 4. Worked example — the same data, four ways

A messaging app needs to store messages. Let's see how the same domain looks in each NoSQL family.

The domain: **messages** (id, channel_id, author_id, content, timestamp) and **users** (id, name, email). The four access patterns we care about:

1. *Fetch the most recent 50 messages for a channel* (the "open a chat" query).
2. *Fetch the username for a given user_id* (the "render an author name" query).
3. *Find all messages by a given author across all channels* (the "user's history" query).
4. *Find which users have followed which users* (the "social graph" query).

### KV (Redis / DynamoDB) shape

```
messages:42:000004982  →  {"author_id":7,"content":"hi","timestamp":"2026-05-12T17:33Z"}
users:7                →  {"name":"Aniket","email":"a@b.com"}
```

Access pattern (1) is a *range scan* — but KV stores don't generally do range scans cleanly. DynamoDB's sort key on the table makes it possible; Redis needs you to maintain a sorted set per channel manually. Pattern (2) is a single `get`. Pattern (3) — *messages by author* — has *no fast path* in pure KV; you'd need to maintain a parallel index `authored:7 → [list of message ids]` written at every message-write. Pattern (4) is the worst — there's no concept of relationships beyond key-pointer-key, which becomes a maintenance nightmare.

**Verdict:** KV is right for patterns (1) and (2); poor for (3) and (4).

### Document (MongoDB) shape

```javascript
// messages collection
{
  _id: ObjectId("..."),
  channel_id: 42,
  author_id: 7,
  content: "hi",
  timestamp: ISODate("2026-05-12T17:33Z")
}

// users collection
{ _id: 7, name: "Aniket", email: "a@b.com" }
```

Pattern (1): `db.messages.find({channel_id: 42}).sort({timestamp: -1}).limit(50)` with an index on `(channel_id, timestamp)`. Native, fast. Pattern (2): direct `_id` lookup. Pattern (3): `db.messages.find({author_id: 7})` — needs an index on `author_id`, works fine. Pattern (4): denormalise into the user document — `{ _id: 7, followers: [3, 5, 9, ...] }` — fast for single-hop, gets painful for "friends of friends".

**Verdict:** Documents are right for patterns (1), (2), and (3); awkward but workable for (4).

### Wide-column (Cassandra) shape

```cql
CREATE TABLE messages (
    channel_id   bigint,
    bucket       int,
    message_id   timeuuid,
    author_id    bigint,
    content      text,
    PRIMARY KEY ((channel_id, bucket), message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);
```

Pattern (1): `SELECT * FROM messages WHERE channel_id = 42 AND bucket = current_bucket() ORDER BY message_id DESC LIMIT 50`. Native, cheap — Cassandra reads all `message_id` clustering values for that partition off one node, in sorted order, with read-ahead. Pattern (2): a separate `users` table, single-key lookup. Pattern (3): needs a *second* denormalised table `messages_by_author` written at message-creation. Pattern (4): needs a `follows` table, written denormalised.

This is the central trade-off of wide-column: you write the data **once per access pattern you need to support**, at write time, with the application maintaining consistency between them. The reads are then trivially fast.

**Verdict:** Wide-column is *excellent* for pattern (1) at scale; for (2)/(3)/(4) you build dedicated tables per pattern.

### Graph (Neo4j) shape

```cypher
CREATE (alice:User {id: 7, name: "Aniket"})
CREATE (bob:User   {id: 3, name: "Bob"})
CREATE (chan:Channel {id: 42, name: "general"})
CREATE (msg:Message {id: "...", content: "hi", timestamp: ...})

CREATE (alice)-[:AUTHORED]->(msg)
CREATE (msg)-[:IN_CHANNEL]->(chan)
CREATE (alice)-[:FOLLOWS]->(bob)
```

Pattern (1): `MATCH (m:Message)-[:IN_CHANNEL]->(c:Channel {id: 42}) RETURN m ORDER BY m.timestamp DESC LIMIT 50`. Works but isn't graph-shaped — the index lookup on Channel does most of the work. Pattern (2): same. Pattern (3): `MATCH (u:User {id: 7})-[:AUTHORED]->(m) RETURN m`. Pattern (4): **this is where graph shines**. `MATCH (a:User)-[:FOLLOWS*2..3]-(b:User) WHERE a.id = 7 RETURN b` finds friends-of-friends to three hops. The same query in SQL is a 3-table self-join with very ugly semantics.

**Verdict:** Graph is overkill for (1)/(2)/(3); right tool for (4) and similar relationship-heavy queries.

### Conclusion

Pick the data model whose primary operations match your highest-volume access patterns. *Then* pick the engine. A real chat-app system might use **two** stores: Cassandra for messages (pattern 1 at scale), and Postgres for everything else (users, follows, settings). [Capstone 39 (Chat)](/cortex/system-design/capstones/chat-system) walks this in depth.

## 5. Trade-offs

| Family | Strongest property | Weakest property | Picks itself when… |
|---|---|---|---|
| **Relational** ([Lesson 9](/cortex/system-design/building-blocks/relational-databases)) | rich SQL + ACID + multi-way joins | horizontal write scaling | your access patterns are heterogeneous; correctness matters more than write throughput |
| **KV** | predictable get/put latency at any scale | range scans, secondary indexes, joins | one or two known access patterns, very high QPS |
| **Document** | flexible per-document schema | cross-document transactions, joins, schema-drift mess | rapidly-evolving variable-shape data |
| **Wide-column** | huge-partition range reads at low latency | partition-key is the design; cannot retrofit | time-series-like access at "this partition" granularity |
| **Graph** | multi-hop traversals are first-class | sharding beyond one machine | the question is genuinely "find paths in this graph" |

The pattern: **NoSQL families optimise for one access shape and pay for it elsewhere.** Relational is the generalist; the NoSQL families are specialists. Picking the wrong specialist is much more painful than picking the generalist and accepting some specialisation overhead.

A working senior-engineer rule of thumb: **start with Postgres**. Move to a NoSQL family only when (a) you can articulate which family and why, with reference to one of the five rows above, and (b) the access pattern that drove you away from Postgres is *the dominant one* in production traffic, not a 1% edge case.

## 6. Edge cases and failure modes

### 6.1 KV — secondary indexes are alien

DynamoDB lets you create Global Secondary Indexes (GSI) and Local Secondary Indexes (LSI), and the docs frame them as a natural extension. They aren't. A GSI is essentially a separate KV table that DynamoDB maintains for you, with its own throughput limits and its own consistency model (asynchronously updated). Adding indexes to a busy DynamoDB table is non-trivial and the cost scales with the index count. **Native KV's only real index is the primary key.** Design your primary key carefully.

### 6.2 Document — schema drift over time

The pitch for document stores is "schema-less". The reality is **schema enforced at the application layer instead of the database**. Two years into a successful product, you have documents from five generations of the application code in the same collection. Code paths that read documents become defensive (`doc.email ?? doc.userEmail ?? doc.user?.email`). Migrations are "rewrite every document in a background job, hope nothing breaks". The fix is *discipline* — explicit schema version fields, regular cleanup migrations — not "we're using a schema-less DB so we don't need to think about it".

### 6.3 Wide-column — the partition key is the entire design

Choosing the partition key is the load-bearing engineering decision. Get it right, and Cassandra serves trillions of messages at low latency. Get it wrong — say, partition by `user_id` for the messages table when one user has 10M messages and most have 50 — and you have a *hot partition*: one node serving the popular user is melted while the rest of the cluster is idle. The remediation patterns are explored in [Lesson 12 (sharding)](/cortex/system-design/building-blocks/sharding-and-partitioning) but the takeaway here is: **you cannot retrofit a partition key change**. Pick carefully or accept a full table rewrite later.

### 6.4 Graph — deep traversals are slow

Graph DB pitches always lead with "friends of friends in one query". They neglect to mention that "10 hops through a billion-edge social graph" is one query, and that query can take minutes — even on Neo4j with all the right indexes. Multi-hop traversal complexity is `O(branching^hops)` in the worst case. The pattern: **bound the hop count** in production queries; reach for offline pre-computed paths for anything deeper.

### 6.5 Cross-family — no joins

Three of the four families (KV, document, wide-column) don't do joins, or do them poorly enough that you should not rely on them. The substitute is **denormalisation at write time**: you write the same data into multiple tables / collections / partitions, each shaped for a different access pattern. The cost: application code now maintains consistency between those copies. The benefit: every read is one round-trip to one partition. Whether the trade is worth it depends on the read:write ratio.

### 6.6 Cross-family — eventual consistency footguns

DynamoDB's default reads are *eventually consistent* and can be 20 ms stale. Cassandra's default `LOCAL_ONE` consistency level returns data that may not yet be on a quorum of replicas. MongoDB's default read preference returns data from the primary, which is strongly consistent — until your driver / client config silently switches to `secondaryPreferred`. **Always know your read consistency level**; never let "the default" be the answer. [Lesson 13 (consistency models)](/cortex/system-design/building-blocks/consistency-models) goes into the precise semantics.

## 7. Practice

### Exercise 1 — Fit to shape

Match each data shape with the best NoSQL family, two-line justification per pair:

1. A **social-graph feature** that needs "users you might know" — surfaced via 2-hop friend-of-friend traversal.
2. **Time-series sensor readings** from 100,000 IoT devices, ~1 reading per second per device, retrieve all readings for a given device over a time range.
3. **User profile documents** — each user has 50–200 fields, with a long tail of optional / experimental fields; schema evolves weekly.
4. A **shopping cart** — high-volume reads/writes, "give me cart for user X", needs to survive a region failure.

<details>
<summary>Solution</summary>

1. **Graph** (Neo4j). The query is a 2-hop traversal across the FOLLOWS / FRIENDS edges — exactly what graph DBs are built for. SQL multi-table joins for "friends of friends" are workable; "friends of friends of friends" or any variable-depth traversal really isn't.

2. **Wide-column** (Cassandra / ScyllaDB). Partition key = `device_id`, clustering key = `timestamp DESC`. All readings for one device live on one partition, range scans by time are one disk seek. Same shape as Discord's messages-by-channel.

3. **Document** (MongoDB). Variable-schema documents with rapid evolution is the canonical document-store pitch. Secondary indexes on the few fields you actually query keep reads fast.

4. **KV** (DynamoDB). The access pattern is literally `get(user_id) → cart_blob`. DynamoDB also offers cross-region replication with strong consistency, which solves the "survive a region failure" requirement. *This is the original Dynamo paper's use case* — Amazon shopping carts in 2007.

</details>

### Exercise 2 — Partition-key design

You're designing a Cassandra table for a chat application's messages. The dominant access pattern is "give me the last 50 messages in channel X". Channels range from 5 messages (a small DM) to 100 million messages (a busy public channel). What's a bad partition-key choice, what's a better one, and why?

<details>
<summary>Solution</summary>

**Bad: `PRIMARY KEY (channel_id, message_id)`.** This makes the *whole channel* a single partition. For the 5-message DM, fine. For the 100-M-message public channel, the entire 100 M-row partition lives on one Cassandra node. Reads of "last 50" walk the clustering index — fast, *but* every read for that channel hits that one node. Writes to that channel all hit the same node. **Hot partition.**

**Better: `PRIMARY KEY ((channel_id, bucket), message_id)`** where `bucket` is a coarse time slice like "ISO week" or "10-day window". Now the 100 M-message channel is broken into ~250 partitions over five years (one per 10-day bucket). Each partition is ~400 K rows — Cassandra serves those at low latency. Reads of "last 50" hit the *current* bucket's partition; pagination crosses bucket boundaries when needed.

**This is exactly Discord's design.** The `bucket` value is computed deterministically from the timestamp (e.g., `floor(timestamp / 10 days)`) so reads can target the right partition without an extra index lookup.

The general principle: **the partition key should produce partitions that are (a) bounded in size and (b) bounded in write/read rate**. Whenever the partition key encodes "all the data for one logical entity", check whether one logical entity's data is unbounded. If so, add a secondary dimension (time, hash bucket, sub-id) to break it up.

</details>

### Exercise 3 — When NOT to use NoSQL

Name three scenarios where Postgres is the right answer despite NoSQL's apparent fit, and explain the trap.

<details>
<summary>Solution</summary>

**Scenario 1: "We have JSON data, so we need MongoDB."**
Postgres has a first-class `jsonb` column type with indexing (GIN). For most "I have variable-shape data" needs, `users (id BIGSERIAL, email TEXT, profile JSONB)` with `CREATE INDEX ON users USING GIN (profile)` gives you 95% of what MongoDB offers, with full ACID, joins, mature tooling. The trap: equating "I have JSON in my data" with "I need a JSON database". Postgres handles JSON fine.

**Scenario 2: "We need horizontal scale, so we need Cassandra."**
A single Postgres on modern hardware (32 cores, 256 GB RAM, NVMe SSD) easily serves 50–100k QPS of mixed reads/writes. Stack Overflow runs the world's traffic on two SQL Servers. Most teams reach for Cassandra at 5 k QPS — far below the single-machine envelope of a well-tuned relational DB. The trap: assuming "horizontal scale" is needed when the workload would fit on one machine.

**Scenario 3: "We have a social graph, so we need Neo4j."**
A 2-hop friend-of-friend query in Postgres is a self-join with one CTE. Fast on millions of rows with the right indexes. The graph DB is only the right answer when the *deep* traversal queries (5+ hops, variable depth, find-all-paths) are the dominant workload. The trap: picking the right tool for one query type when 99% of your workload doesn't use it.

In all three cases, the pattern is the same: **NoSQL families are specialists; the specialist is right when the specialist's strength is *the* load-bearing requirement, not when it's *a* nice-to-have.**

</details>

## 8. In the Wild

- **[DeCandia et al., *Dynamo: Amazon's Highly Available Key-value Store*, SOSP 2007](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf)** — the paper that defines what "NoSQL" actually means. Consistent hashing, vector clocks, sloppy quorums, hinted handoff. Foundation reading.
- **[Lakshman + Malik, *Cassandra — A Decentralized Structured Storage System*, LADIS 2009](https://www.cs.cornell.edu/projects/ladis2009/papers/lakshman-ladis2009.pdf)** — the Facebook paper that introduced Cassandra. Combines Dynamo's distribution with Bigtable's column model.
- **[Discord, *How Discord Stores Billions of Messages*](https://discord.com/blog/how-discord-stores-billions-of-messages)** (2017) and **[*How Discord Stores Trillions of Messages*](https://discord.com/blog/how-discord-stores-trillions-of-messages)** (2023). The two together are a complete case study: the data model survived a full engine migration. Read both.
- **[Werner Vogels, *Eventually Consistent*, CACM 2009](https://queue.acm.org/detail.cfm?id=1466448)** — Amazon CTO's accessible introduction to consistency models. Pairs naturally with [Lesson 13](/cortex/system-design/building-blocks/consistency-models).
- **[AWS, *DynamoDB Best Practices*](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)** — the AWS docs make the partition-key design philosophy explicit. Even if you never touch DynamoDB, the design discipline applies to every wide-column / KV system.

---

> **Next:** [11. Replication](/cortex/system-design/building-blocks/replication) — every system in this lesson and the last one *replicates* its data across nodes for durability + read scaling + failover. Replication has its own taxonomy (leader-follower, multi-leader, leaderless) and its own famous failure modes (replication lag, lost-update conflicts, split brain). The simulator widget arrives there.
