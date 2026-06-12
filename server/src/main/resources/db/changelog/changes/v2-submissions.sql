--liquibase formatted sql

--changeset cortex:2-create-submission-allowlist
-- Who may save submissions (POST /api/submissions). Keyed by the Keycloak
-- preferred_username claim — the GitHub login for IdP users. Granting access
-- is a manual, selective act (homelab): one INSERT, no restart needed.
--   docker compose exec db psql -U cortex -c \
--     "INSERT INTO submission_allowlist (username) VALUES ('<github-login>');"
CREATE TABLE submission_allowlist (
  username   TEXT        PRIMARY KEY,
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- The owner…
INSERT INTO submission_allowlist (username) VALUES ('ani2fun');
-- …and the local-realm smoke-test user (bin/dev Keycloak). Harmless in prod:
-- the apps-prod realm has no such account, so the row can never match a JWT.
INSERT INTO submission_allowlist (username) VALUES ('tester');
--rollback DROP TABLE submission_allowlist;

--changeset cortex:3-create-submissions
-- One row per submission attempt (history, not last-write-wins). Per-case
-- results ride in the HTTP response only — the row keeps the aggregate
-- verdict, which is all the UI needs to show "solved".
CREATE TABLE submissions (
  id           BIGSERIAL   PRIMARY KEY,
  username     TEXT        NOT NULL,
  book         TEXT        NOT NULL,
  chapter      TEXT        NOT NULL,
  language     TEXT        NOT NULL,
  source       TEXT        NOT NULL,
  accepted     BOOLEAN     NOT NULL,
  passed_cases INT         NOT NULL,
  total_cases  INT         NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX submissions_user_chapter_idx
  ON submissions (username, book, chapter, created_at DESC);
--rollback DROP TABLE submissions;
