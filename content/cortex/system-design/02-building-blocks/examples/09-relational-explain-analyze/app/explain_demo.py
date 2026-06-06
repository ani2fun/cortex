"""
Lesson 9 — EXPLAIN ANALYZE demo.

Seeds a `users` table with 100,000 rows, then runs the same equality
query (`WHERE email = ?`) twice — once with no index (planner picks a
sequential scan) and once with a B-tree index (planner picks an index
scan). Prints both `EXPLAIN ANALYZE` outputs so the reader can compare
the chosen plan, the actual time, and the number of rows examined.

Idempotent: drops the table at the start so re-running gives clean
output. Re-run as many times as you like.
"""

from __future__ import annotations

import os
import random
import string

import psycopg

ROWS = 100_000
TARGET_EMAIL = "needle@codefolio.dev"

DSN = (
    f"host={os.environ.get('PGHOST', 'localhost')} "
    f"port={os.environ.get('PGPORT', '5432')} "
    f"user={os.environ.get('PGUSER', 'lesson9')} "
    f"password={os.environ.get('PGPASSWORD', 'lesson9')} "
    f"dbname={os.environ.get('PGDATABASE', 'lesson9')}"
)


def random_email(rng: random.Random) -> str:
    local = "".join(rng.choices(string.ascii_lowercase, k=8))
    return f"{local}@example.com"


def seed(cur: psycopg.Cursor) -> None:
    print(f"--- seeding {ROWS:,} rows ---")
    cur.execute("DROP TABLE IF EXISTS users")
    cur.execute(
        """
        CREATE TABLE users (
            id          BIGSERIAL PRIMARY KEY,
            email       TEXT NOT NULL,
            created_at  TIMESTAMPTZ DEFAULT now()
        )
        """
    )

    rng = random.Random(42)
    rows = [(random_email(rng),) for _ in range(ROWS - 1)]
    # Plant the needle at a random position so the seq scan doesn't get
    # lucky and find it on page 1.
    needle_at = rng.randint(0, len(rows))
    rows.insert(needle_at, (TARGET_EMAIL,))

    with cur.copy("COPY users (email) FROM STDIN") as copy:
        for r in rows:
            copy.write_row(r)
    # ANALYZE so the planner has fresh stats and picks correctly.
    cur.execute("ANALYZE users")
    print(f"    needle ({TARGET_EMAIL!r}) seeded at row #{needle_at + 1:,}")


def run_explain(cur: psycopg.Cursor, label: str) -> None:
    print()
    print(f"--- EXPLAIN ANALYZE: {label} ---")
    cur.execute(
        "EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) SELECT id, email FROM users WHERE email = %s",
        (TARGET_EMAIL,),
    )
    for (line,) in cur.fetchall():
        print(f"    {line}")


def main() -> None:
    with psycopg.connect(DSN, autocommit=True) as conn:
        with conn.cursor() as cur:
            seed(cur)

            run_explain(
                cur,
                "no index — planner picks a Seq Scan",
            )

            print()
            print("--- creating index on email ---")
            cur.execute("CREATE INDEX users_email_idx ON users(email)")
            cur.execute("ANALYZE users")

            run_explain(
                cur,
                "with index — planner picks an Index Scan",
            )

            print()
            print("--- done — connect via psql for more:")
            print('    docker compose exec db psql -U lesson9 -d lesson9')


if __name__ == "__main__":
    main()
