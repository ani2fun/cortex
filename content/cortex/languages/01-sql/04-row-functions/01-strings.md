---
title: Strings
summary: The string functions you'll use 90% of the time — `LOWER`, `UPPER`, `TRIM`, `LENGTH`, `SUBSTRING`, `REPLACE`, `||`/`CONCAT`, plus the regex tier when `LIKE` isn't enough.
prereqs:
  - sql-foundations-select-and-projection
---

# 1. Strings

## The Hook

A daily report joins user-entered emails to a marketing list. Twenty thousand records on each side. The query:

```sql
SELECT u.email FROM users u JOIN newsletter n ON n.email = u.email;
```

Returns 14,200 matches. Marketing complains: "we know there are at least 17,000 overlaps."

The bug is invisible in a list view. Only when you `SELECT email, LENGTH(email)` do you see it: the user-entered emails have leading/trailing whitespace ('  bob@example.com'), mixed casing ('Alice@Example.Com'), and a few have a stray Zero-Width Space (`​`) injected by a copy-paste from a fancy mail client. The newsletter list is normalised; the user input is not. The two strings *look* identical to the eye but are not equal under SQL's exact-match join.

The fix involves no schema change, just a tiny per-row computation:

```sql
SELECT u.email FROM users u
JOIN newsletter n ON LOWER(TRIM(n.email)) = LOWER(TRIM(u.email));
```

Three lines turn a partial-match query into a complete one. That's what string functions do — clean up data on the way through, normalise comparisons, repair the inconsistencies that creep into every column of human-entered text.

This chapter is about the string functions you'll reach for most often, the regex tier for when `LIKE` isn't expressive enough, and the dialect quirks that make portable string SQL slightly harder than it should be. By the end you'll know which function to reach for in each case, why function-on-column in `WHERE` blocks index use, and how to write defensive joins that survive whitespace and case mismatches.

---

## Table of contents

1. [The string-function catalogue](#the-string-function-catalogue)
2. [Concatenation: `||` and `CONCAT`](#concatenation)
3. [Case: `LOWER`, `UPPER`, `INITCAP`](#case)
4. [Length and slicing: `LENGTH`, `SUBSTRING`, `LEFT`, `RIGHT`](#length-and-slicing)
5. [Trimming: `TRIM`, `LTRIM`, `RTRIM`](#trimming)
6. [Search and replace: `POSITION`, `REPLACE`](#search-and-replace)
7. [Padding: `LPAD`, `RPAD`](#padding)
8. [Pattern matching: `LIKE`, `SIMILAR TO`, regex](#pattern-matching)
9. [Sargability: function-on-column blocks indexes](#sargability)
10. [Edge cases and pitfalls](#edge-cases-and-pitfalls)
11. [Production reality](#production-reality)
12. [Practice ladder](#practice-ladder)
13. [Cross-links](#cross-links)
14. [Final takeaway](#final-takeaway)

***

# The string-function catalogue

The dozen or so functions that account for nearly every string operation you'll write:

| Function | What it does |
|---|---|
| `LOWER(s)` / `UPPER(s)` | case conversion |
| `INITCAP(s)` | Title Case (Postgres) |
| `LENGTH(s)` (Postgres `CHAR_LENGTH`) | character count |
| `SUBSTRING(s FROM start FOR len)` | extract substring |
| `LEFT(s, n)` / `RIGHT(s, n)` | first / last N characters |
| `TRIM(s)` | strip leading + trailing whitespace |
| `LTRIM(s)` / `RTRIM(s)` | strip leading or trailing |
| `REPLACE(s, from, to)` | substring replacement |
| `POSITION(sub IN s)` (Postgres), `INSTR(s, sub)` (SQLite/MySQL) | find substring index |
| `s1 \|\| s2` (Postgres/SQLite), `CONCAT(s1, s2, ...)` (MySQL/portable) | concatenate |
| `LPAD(s, len, pad)` / `RPAD(s, len, pad)` | pad to length |
| `s LIKE pattern` | wildcard match |
| `s ~ regex` (Postgres) | regex match |

Memorise this table; the rest of the chapter is the deeper notes for each.

---

# Concatenation

```sql run
CREATE TABLE customers (id INT, first_name TEXT, country TEXT);
INSERT INTO customers VALUES (1,'Maria','Germany'),(2,'John','USA'),(3,'Georg','UK');

-- Standard SQL: || is concatenation. Works in Postgres and SQLite.
SELECT first_name || ' from ' || country AS bio FROM customers;
```

> **Dialect note:** SQL Server uses `+` for string concat (and errors on `||`). MySQL uses `CONCAT(s1, s2, ...)` by default and re-purposes `||` to logical-OR (unless `PIPES_AS_CONCAT` mode is on). The portable form is `CONCAT(...)`.

A subtlety: **`||` propagates `NULL`**. `'hello' || NULL` is `NULL`, not `'hello'`. If `country` is NULL for a row, the entire `bio` for that row is NULL. Use `COALESCE` to substitute a default:

```sql run
CREATE TABLE customers (id INT, first_name TEXT, country TEXT);
INSERT INTO customers VALUES (1,'Maria','Germany'),(2,'John','USA'),(3,'Lisa',NULL);

SELECT first_name || ' from ' || COALESCE(country, 'somewhere') AS bio FROM customers;
```

`CONCAT` (in dialects that have it) typically *ignores* NULL operands rather than poisoning the whole result — a different behaviour. Always check the docs for the dialect you're on before relying on either.

`CONCAT_WS('separator', s1, s2, ...)` ("with separator") is the portable way to join with a delimiter, skipping NULLs:

```sql
SELECT CONCAT_WS(', ', first_name, country, postcode) FROM customers;
-- Joins with ', '. NULLs are skipped — no double commas.
```

---

# Case

`LOWER(s)` and `UPPER(s)` do exactly what they say. `INITCAP(s)` (Postgres) capitalises the first letter of each word.

```sql run
CREATE TABLE customers (id INT, first_name TEXT, country TEXT);
INSERT INTO customers VALUES (1,'maria','germany'),(2,'JOHN','usa');

SELECT first_name AS original,
       LOWER(first_name) AS lower,
       UPPER(first_name) AS upper
FROM customers;
```

Case is a frequent source of join misses (the chapter's hook bug). Two engineering rules:

1. **For comparisons**: lowercase both sides. `WHERE LOWER(a) = LOWER(b)` is the bread-and-butter case-insensitive join.
2. **For storage**: pick a normalised form (usually lowercase) and store it that way. Store `email` as lowercase on insert; comparisons are then exact-match. This is the production pattern — repair-on-read works in pinches but adds latency to every join.

> **Dialect note:** Postgres has `ILIKE` for case-insensitive `LIKE` (`first_name ILIKE 'm%'`). SQLite's `LIKE` is case-insensitive *by default for ASCII* (and case-sensitive for non-ASCII). MySQL's `LIKE` case sensitivity depends on the column's collation. The `LOWER(...) = LOWER(...)` form is the most portable.

---

# Length and slicing

```sql run
CREATE TABLE customers (id INT, first_name TEXT);
INSERT INTO customers VALUES (1,'Maria'),(2,'Christopher'),(3,'Al');

SELECT first_name,
       LENGTH(first_name)              AS len,
       SUBSTRING(first_name FROM 1 FOR 3) AS first_three,
       SUBSTRING(first_name FROM 2)       AS skip_first_char
FROM customers;
```

> **`LENGTH` vs `CHAR_LENGTH` vs `OCTET_LENGTH`**: in Postgres, `LENGTH` and `CHAR_LENGTH` are character counts (Unicode-aware); `OCTET_LENGTH` is bytes. In MySQL, `LENGTH` is bytes and `CHAR_LENGTH` is characters. **For text-counting use `CHAR_LENGTH` for portability.** For bytes (e.g., enforcing a 64-byte input limit), use `OCTET_LENGTH`.

`SUBSTRING(s FROM start FOR len)` is the standard form; positions are 1-based. Postgres also accepts the function-call form `SUBSTRING(s, start, len)`; SQLite uses `SUBSTR(s, start, len)`. SQL Server uses `SUBSTRING(s, start, len)`.

`LEFT(s, n)` and `RIGHT(s, n)` are convenience wrappers — the first / last N characters:

```sql
SELECT LEFT('Maria', 3);    -- 'Mar'
SELECT RIGHT('Maria', 3);   -- 'ria'
```

These two are the cleanest way to extract fixed-position prefixes and suffixes — like the year out of an ISO date string, or the file extension from a filename.

---

# Trimming

```sql run
CREATE TABLE messy (raw TEXT);
INSERT INTO messy VALUES ('  hello  '),('--xxx--'),(NULL);

SELECT raw,
       LENGTH(raw)            AS raw_len,
       TRIM(raw)              AS trimmed,
       LENGTH(TRIM(raw))      AS trimmed_len,
       TRIM('-' FROM raw)     AS trim_dashes,
       LTRIM(raw)             AS ltrim_only,
       RTRIM(raw)             AS rtrim_only
FROM messy;
```

`TRIM(s)` strips whitespace by default. `TRIM('-' FROM s)` strips any combination of the listed characters from both ends. `LTRIM` and `RTRIM` operate on one side only. Standard SQL: `TRIM([LEADING|TRAILING|BOTH] [chars FROM] s)`.

In production data hygiene, `TRIM` is the single most-applied function. **Any column that comes from human input — name, email, address line, search query — should be `TRIM`med on insert** (or the application should `.trim()` before sending). The reverse — leaving whitespace and `TRIM`ming on read — works but pays the cost on every read. Better to normalise once.

---

# Search and replace

`REPLACE(s, from, to)` substitutes every occurrence of `from` with `to`. Useful for normalising punctuation, scrubbing PII, removing characters:

```sql run
SELECT REPLACE('+44 (0)20 7946 0958', ' ', '')  AS no_spaces,
       REPLACE('+44 (0)20 7946 0958', '+44', '0') AS local_form;
```

Two replacements over the same input. `REPLACE` is global — it replaces every occurrence, not just the first.

`POSITION(sub IN s)` (Postgres) or `INSTR(s, sub)` (SQLite/MySQL) returns the 1-based position of the first occurrence, or `0` if not found:

```sql
SELECT POSITION('@' IN 'alice@example.com');   -- 6 (Postgres)
SELECT INSTR('alice@example.com', '@');        -- 6 (SQLite/MySQL)
```

A common pattern: extract the domain from an email by combining `SUBSTRING` and `POSITION`:

```sql
SELECT SUBSTRING(email FROM POSITION('@' IN email) + 1) AS domain
FROM users;
```

For more complex extraction (`@.+` to extract everything after the @), regex is the right tool — covered below.

---

# Padding

`LPAD(s, len, pad)` left-pads `s` with the `pad` string until total length is `len`. `RPAD` is the right-pad version.

```sql run
SELECT LPAD('42', 5, '0')        AS padded,    -- '00042'
       LPAD('hello', 10, '-> ')  AS verbose,   -- '-> -> hello'
       RPAD('42', 5, '0');                     -- '42000'
```

The classic use is **zero-padding identifiers** — turning `42` into `00042` for sorting or display alongside `00045`. Sortable date strings like `'2026-04-03'` rely on this implicitly: each component is fixed-width.

---

# Pattern matching

The simplest tier is `LIKE`, covered briefly in [Filtering](/cortex/languages/sql/foundations/filtering#pattern-matching). `%` matches zero or more characters, `_` matches exactly one.

```sql run
CREATE TABLE customers (id INT, first_name TEXT);
INSERT INTO customers VALUES (1,'Maria'),(2,'Martin'),(3,'Lisa');

SELECT first_name FROM customers WHERE first_name LIKE 'M%';   -- starts with M
SELECT first_name FROM customers WHERE first_name LIKE '%a';   -- ends with a
SELECT first_name FROM customers WHERE first_name LIKE '_a%';  -- 'a' in 2nd position
```

`SIMILAR TO` (standard SQL, Postgres-supported) bridges between `LIKE` and full regex — supports `|`, `*`, `+`, `?`, `[]` etc. Less commonly used than either `LIKE` or `~` (full regex) in production.

For real text matching, drop into the engine's regex:

```sql
-- Postgres: ~ is case-sensitive regex; ~* is case-insensitive.
WHERE email ~ '^[a-z0-9.]+@example\.com$';

-- SQLite: REGEXP operator (depends on extension being loaded).
WHERE email REGEXP '^[a-z0-9.]+@example\.com$';

-- MySQL: REGEXP or RLIKE.
WHERE email REGEXP '^[a-z0-9.]+@example\.com$';

-- SQL Server: no built-in regex; use LIKE patterns or extend with a CLR function.
```

Regex in SQL is the same regex you know — but **it's expensive**. `LIKE 'foo%'` can use a B-tree index; `~ '^foo'` *might* (depends on planner). `LIKE '%foo'` and most non-anchored regex always require a full table scan. We'll cover this in [Indexes and Performance](/cortex/languages/sql/index).

---

# Sargability

A predicate is **sargable** ("Search-ARGument-able") if the planner can use an index to evaluate it. **Function-on-column predicates are usually not sargable** — wrapping a column in a function hides it from the index.

```sql
-- ❌ Not sargable. Even with an index on email, this scans every row to apply LOWER.
WHERE LOWER(email) = 'alice@example.com';

-- ✅ Sargable. Direct comparison can use an email index.
WHERE email = 'alice@example.com';
```

Two production fixes:

1. **Normalise on insert**, so the column is already lowercase. Then your read query is the sargable form.
2. **Build an expression index** on `LOWER(email)`. Then `WHERE LOWER(email) = ...` becomes sargable against that specific index. Postgres supports this; some other engines do too.

```sql
-- Postgres expression index: indexes the lowercased value.
CREATE INDEX users_email_lower ON users (LOWER(email));
```

**The lesson: every per-row function in a `WHERE` is a candidate sargability bug.** Audit hot-path queries for these. The full discussion is in [Indexes and Performance](/cortex/languages/sql/index); flagging them now so you start spotting them.

---

# Edge cases and pitfalls

## Empty string vs NULL

In standard SQL (and Postgres), `''` (empty string) is *not* the same as `NULL`. `LENGTH('')` is `0`; `LENGTH(NULL)` is `NULL`. Some Oracle versions historically treated `''` as `NULL` — caused decades of porting headaches. Don't conflate the two.

## Unicode normalisation

`'café'` and `'café'` look identical but can be different strings — one is the precomposed `é` (U+00E9), the other is `e` + combining acute accent (U+0301). They're equal in *visual* terms but not equal in `=`. Postgres has `unaccent()` and ICU collations; rare but production-relevant for international apps.

## Trailing whitespace from `CHAR(n)`

`CHAR(n)` is a **fixed-length** string type — values are always padded to length `n` with spaces. `'hi'` stored in `CHAR(5)` becomes `'hi   '`. This bites in joins:

```sql
SELECT * FROM a JOIN b ON a.code = b.code;   -- might miss matches if one side is CHAR
```

Use `VARCHAR(n)` or `TEXT` instead; they don't pad. (Covered in the [Schema and Constraints](/cortex/languages/sql/index) module.) If you're stuck with `CHAR`, use `RTRIM` on both sides.

## Concatenating with NULL

```sql
SELECT 'hello, ' || NULL || '!';   -- NULL, not 'hello, !'
```

`||` propagates NULL. `CONCAT_WS(...)` and `CONCAT(...)` (in MySQL/Postgres) typically skip NULLs. Always reach for `COALESCE(col, '')` if you want NULL to become empty.

## Locale-dependent uppercase

`UPPER('ß')` is `'SS'` in German locale, `'ß'` in C locale. `UPPER('i')` in Turkish is `'İ'`, not `'I'`. Subtle, rarely-hit, but enough to miss a join when a Turkish user types `Istanbul`. For deterministic comparisons, use `COLLATE "C"` or store normalised values.

## Regex anchors

In Postgres `~`, `^` matches start of string and `$` matches end. Some older MySQL versions had subtly different anchor behaviour. Check the docs.

---

# Production reality

The chapter's hook is the canonical production string-fix pattern: **normalise both sides before joining**. Either at write time (preferred) or at read time. In codefolio's stack, the `customers.first_name` column is human input — a future feature that joins against an external CRM by name would benefit from this defensive shape:

```sql
SELECT c.first_name, crm.profile
FROM customers c
JOIN external_crm crm
  ON LOWER(TRIM(c.first_name)) = LOWER(TRIM(crm.given_name));
```

A second pattern — **extracting structured data from text columns**:

```sql
-- Pull the domain part out of an email column for per-domain analytics.
SELECT SUBSTRING(email FROM POSITION('@' IN email) + 1) AS domain,
       COUNT(*) AS users
FROM users
GROUP BY domain
ORDER BY users DESC;
```

A third — **scrubbing user input on write** (this happens in application code, but the SQL primitives are what you'd use in a backfill migration):

```sql
-- One-time data fix: normalise existing email rows.
UPDATE users SET email = LOWER(TRIM(email)) WHERE email <> LOWER(TRIM(email));
```

The `WHERE email <> LOWER(TRIM(email))` filter is the trick: only update the rows that *need* fixing. Without it, you'd update every row, and a `VACUUM` storm would follow.

---

# Practice ladder

1. **Lowercase every customer's name. Then trim leading/trailing whitespace from a hypothetical email column.** *Hint: `LOWER`, `TRIM`.*
2. **Extract the country domain (e.g., `'.de'`, `'.com'`) from an email column.** *Hint: `SUBSTRING(email FROM POSITION('.' IN email))`. (Or `RIGHT(email, 3)` if you trust the format.)*
3. **Find customers whose first name has at least 5 characters.** *Hint: `LENGTH(first_name) >= 5`.*
4. **Replace all spaces in a column with underscores.** *Hint: `REPLACE(col, ' ', '_')`.*
5. **Why is `WHERE LOWER(email) = 'alice@example.com'` slower than the same query with the email column already stored lowercase?** *Hint: sargability. Where can the index help, and where can't it?*
6. **Write a query that returns each customer's first name padded to 10 characters with dots on the right.** *Hint: `RPAD(first_name, 10, '.')`.*
7. **Predict the output of:**
   ```sql
   SELECT 'hello' || NULL || '!' AS x;
   ```
   *Hint: `||` propagates NULL.*

***

# Cross-links

- **Previous module:** [Aggregation](/cortex/languages/sql/aggregation/index) — once you have rows, summarise them. Now: per-row enrichment.
- **Next in this module:** [Numbers](/cortex/languages/sql/row-functions/numbers) — the numeric counterpart to this chapter.
- **Forward reference:** [Indexes and Performance](/cortex/languages/sql/index) — sargability, expression indexes, the LIKE-with-trigram pattern for unanchored search.
- **Forward reference:** [NULL and Three-Valued Logic](/cortex/languages/sql/row-functions/null-and-three-valued-logic) — the full treatment of why `||` propagates NULL and how `COALESCE` + `NULLIF` repair it.

***

# Final Takeaway

String functions are the per-row data-cleanup layer. Three patterns to internalise:

1. **Normalise on insert; reach for repair-on-read only when you can't.** `LOWER(TRIM(...))` joins work but pay the cost on every query and block index use. Storing the normalised form once trades a write-time cost for read-time speed.
2. **Function-on-column in `WHERE` is a sargability red flag.** Audit hot queries for `WHERE LOWER(col) = …`, `WHERE TRIM(col) = …`, `WHERE SUBSTRING(col, …) = …`. The fix is either an expression index or a normalised column.
3. **`||` propagates NULL; `CONCAT_WS` doesn't.** Pick the operator that matches your intent. When a column might be NULL and you want a sensible default, `COALESCE(col, '')` makes the choice explicit.

Master these three and string handling becomes a routine layer of every query — not the source of half your data-quality bugs.
