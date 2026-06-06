--liquibase formatted sql

--changeset codefolio:1-create-visits
CREATE TABLE visits (
  id    INT    PRIMARY KEY,
  count BIGINT NOT NULL
);
INSERT INTO visits (id, count) VALUES (1, 0);
--rollback DROP TABLE visits;
