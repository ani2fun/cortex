--liquibase formatted sql

--changeset cortex:4-create-coach-saved-session
-- One row per explicitly-saved coach transcript (history, not last-write-wins),
-- gated by the SAME submission_allowlist as POST /api/submissions. The tutor runs
-- the live interview and keeps sessions only ephemerally (TTL/purge); a learner on
-- the allowlist presses Save to keep a snapshot here, in the homelab DB. The full
-- CoachSession wire shape the client held at Save time rides in transcript_json.
-- Homelab: no durability guarantee; DELETE /api/coach/saved is the erasure path.
CREATE TABLE coach_saved_session (
  id              BIGSERIAL   PRIMARY KEY,
  username        TEXT        NOT NULL,
  problem_id      TEXT        NOT NULL,   -- "<book>/<chapter>", the tutor's join key
  session_id      TEXT        NOT NULL,   -- the tutor session id at save time (provenance)
  step_index      INT         NOT NULL,   -- denormalised for the list view / a saved-count badge
  completed       BOOLEAN     NOT NULL,
  message_count   INT         NOT NULL,
  transcript_json JSONB       NOT NULL,   -- the CoachSession snapshot (circe-encoded)
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX coach_saved_session_user_problem_idx
  ON coach_saved_session (username, problem_id, created_at DESC);
--rollback DROP TABLE coach_saved_session;
