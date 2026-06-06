-- Lesson 11 — primary init. Create a replication role for pg_basebackup
-- and the streaming-replication wire protocol; loosen pg_hba so the
-- replica can connect over the compose network.

CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'replicator';

-- Allow replication connections from any host inside the compose
-- network (this is a dev / lesson example; production would scope this
-- to a specific subnet or use TLS + cert auth).
ALTER SYSTEM SET listen_addresses = '*';

-- The pg_hba entries for the replication user must be in pg_hba.conf
-- rather than postgresql.conf. The official postgres image runs the
-- /docker-entrypoint-initdb.d scripts AFTER initdb but BEFORE the
-- server's first proper start, so this `COPY` to the hba file is what
-- the next start picks up.
\set ON_ERROR_STOP on
\! echo "host replication replicator all scram-sha-256" >> /var/lib/postgresql/data/pg_hba.conf
\! echo "host all          lesson11   all scram-sha-256" >> /var/lib/postgresql/data/pg_hba.conf

-- The demo table.
CREATE TABLE counters (
    id          BIGSERIAL PRIMARY KEY,
    label       TEXT NOT NULL,
    n           INTEGER NOT NULL DEFAULT 0,
    written_at  TIMESTAMPTZ DEFAULT now()
);
