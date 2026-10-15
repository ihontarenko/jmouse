-- jmouse-scheduler: triggers and their bounded history. MySQL 8 / MariaDB.
-- Every instant is epoch milliseconds (UTC) in a BIGINT, so no driver or session time zone can move it.
-- lease_expires_at is on the DATABASE clock; planned_at and next_fire_at are on the runners' clock.
-- Indexes are declared inline because MySQL has no CREATE INDEX IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS jmouse_scheduler_triggers (
    trigger_id                   VARCHAR(200)  NOT NULL PRIMARY KEY,
    handler_key                  VARCHAR(200)  NOT NULL,
    parameters                   TEXT          NOT NULL,
    schedule_kind                VARCHAR(16)   NOT NULL,
    schedule_expression          VARCHAR(200),
    schedule_zone                VARCHAR(64),
    schedule_period_milliseconds BIGINT,
    schedule_instant             BIGINT,
    misfire_policy               VARCHAR(16)   NOT NULL,
    catch_up_limit               INT,
    gate_key                     VARCHAR(200),
    enabled                      BOOLEAN       NOT NULL,
    next_fire_at                 BIGINT,
    planned_at                   BIGINT,
    lease_expires_at             BIGINT,
    version                      BIGINT        NOT NULL,
    updated_at                   BIGINT        NOT NULL,
    INDEX jmouse_scheduler_triggers_due (enabled, next_fire_at)
);

CREATE TABLE IF NOT EXISTS jmouse_scheduler_occurrences (
    occurrence_id    BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trigger_id       VARCHAR(200)  NOT NULL,
    scheduled_at     BIGINT        NOT NULL,
    recorded_at      BIGINT        NOT NULL,
    outcome          VARCHAR(16)   NOT NULL,
    occurrence_count BIGINT        NOT NULL,
    reason           VARCHAR(1000) NOT NULL,
    INDEX jmouse_scheduler_occurrences_trigger (trigger_id, occurrence_id)
);
