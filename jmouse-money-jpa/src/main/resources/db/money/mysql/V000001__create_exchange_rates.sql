-- ---------------------------------------------------------------------------------------------
-- What one currency is worth in another.
--
-- Append-only from first release: this ships in a library that other people's data will run. A file
-- edited after release refuses to boot every database that already ran the old one, with a checksum
-- error naming a version rather than the change.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE exchange_rates
(
    -- ISO 4217 alphabetic code, upper case. ⚠️ The PRIMARY KEY, which is what confines the table to one
    -- pivot at a time: two pivots would need (currency, pivot), and a table holding both is not richer,
    -- it is one where half the rows answer a question the other half was not asked.
    currency      VARCHAR(3)                            NOT NULL,

    -- How many units of `pivot` one unit of `currency` is worth.
    --
    -- Six decimal places because a weak currency against a strong one needs them: a hryvnia is about
    -- 0.024 dollars, and two places would round a fifth of the value away.
    rate_to_pivot DECIMAL(19, 6)                        NOT NULL,

    -- ⚠️ Stored, not assumed. A rate is meaningless without saying what it is a rate TO, and rows quoted
    -- against the hryvnia look exactly like rows quoted against the euro. Without this column, pointing
    -- an installation at a different feed produces a table that is entirely wrong and entirely plausible.
    --
    -- The pivot itself is never a row: its rate against itself is one by definition, and storing it
    -- invites somebody to edit it, which rescales every conversion at once.
    pivot         VARCHAR(3)                            NOT NULL,

    -- PROVIDER or MANUAL. ⚠️ A MANUAL row is never overwritten by a sync — which is the point, and also
    -- the trap: it is a row that has silently stopped tracking the feed, so anything displaying these
    -- has to mark them.
    --
    -- A CHECK constraint rather than an ENUM type, so the same statement is true on PostgreSQL.
    source        VARCHAR(16)                           NOT NULL,

    updated_at    DATETIME(6)                           NOT NULL,

    PRIMARY KEY (currency),
    CONSTRAINT check_exchange_rates_source CHECK (source IN ('PROVIDER', 'MANUAL')),
    CONSTRAINT check_exchange_rates_rate_positive CHECK (rate_to_pivot > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ⚠️ The collation above is stated rather than inherited, and it is not decoration. Every other
-- jmouse library table is utf8mb4_unicode_ci, and MySQL refuses a foreign key between columns of
-- different collations with "Referencing column and referenced column are incompatible" — at which
-- point the migration half-applies and the next start fails validation. Nothing here has a foreign key
-- today; the first one to arrive should not have to discover this.

-- Reading every rate for one pivot is what the whole table is for, and it is the one query a screen and
-- a converter both make.
CREATE INDEX index_exchange_rates_pivot ON exchange_rates (pivot);
