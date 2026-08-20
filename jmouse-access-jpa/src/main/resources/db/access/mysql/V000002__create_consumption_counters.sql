-- =============================================================================
--  V000002  How much has been used (JMF-6)
--
--  ⚠️ A NEW file, never an edit of V000001. These migrations are append-only from
--  first release — a library's schema has already run against other people's
--  databases, and a changed checksum stops one of them booting. The workspace
--  rule that a Flyway file may be edited in place applies to a product whose
--  database can be dropped, which this is not.
--
--  One row per (subject, meter, window). Asking "how many tokens in the last
--  three hours" is then a primary-key lookup rather than an aggregate over every
--  call since the account opened — this is read on the decision path, and an
--  aggregate there gets slower every day the product succeeds.
--
--  ⚠️ subject_type is VARCHAR, not an enumeration, and that is the point of the
--  move. The product table this replaces keyed on a scope enum, so a counter
--  could only ever be about a PLACE — an organisation, a workspace — and "how
--  much has this person used" had nowhere to be written down. The library never
--  interprets the value, so a new kind of subject costs nothing here.
--
--  ⚠️ A window is created by being written to. There is no reset job and nothing
--  opens a period: 2026-08 and 2026-09 are different rows. A reset that failed to
--  run would hand somebody an unlimited month and the failure would surface as an
--  invoice rather than as an alert.
--
--  ⚠️ The unique key is not decoration. Recording is an UPDATE that falls back to
--  an INSERT, so two callers can race to create the first row of a window; this
--  constraint is what makes the loser fail and retry into the update path instead
--  of quietly creating a second counter that halves every subsequent answer.
-- =============================================================================

CREATE TABLE access_consumption_counters
(
    id           VARCHAR(36) NOT NULL,
    subject_type VARCHAR(64) NOT NULL,
    subject_id   VARCHAR(36) NOT NULL,
    meter        VARCHAR(64) NOT NULL,
    window_key   VARCHAR(32) NOT NULL,
    consumed     BIGINT      NOT NULL DEFAULT 0,
    updated_at   TIMESTAMP   NOT NULL,

    CONSTRAINT access_consumption_counters_pk PRIMARY KEY (id),
    CONSTRAINT access_consumption_counters_uq
        UNIQUE (subject_type, subject_id, meter, window_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- What a usage screen asks: every meter one subject has touched, newest window first.
CREATE INDEX idx_access_consumption_subject
    ON access_consumption_counters (subject_type, subject_id, meter);
