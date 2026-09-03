-- ---------------------------------------------------------------------------------------------
-- What one currency is worth in another.
--
-- The PostgreSQL twin of db/money/mysql/V000001. Same table, same constraints, same reasoning — the
-- comments there are the long version and are not repeated here.
--
-- Append-only from first release: this ships in a library that other people's data will run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE exchange_rates
(
    -- ISO 4217 alphabetic code, upper case. ⚠️ The PRIMARY KEY, which confines the table to one pivot.
    currency      VARCHAR(3)     NOT NULL,

    -- How many units of `pivot` one unit of `currency` is worth. Six places because a weak currency
    -- against a strong one needs them.
    rate_to_pivot NUMERIC(19, 6) NOT NULL,

    -- ⚠️ Stored, not assumed — rows quoted against the hryvnia look exactly like rows quoted against
    -- the euro. The pivot itself is never a row.
    pivot         VARCHAR(3)     NOT NULL,

    -- PROVIDER or MANUAL. ⚠️ A MANUAL row is never overwritten by a sync.
    source        VARCHAR(16)    NOT NULL,

    updated_at    TIMESTAMP(6)   NOT NULL,

    PRIMARY KEY (currency),
    CONSTRAINT check_exchange_rates_source CHECK (source IN ('PROVIDER', 'MANUAL')),
    CONSTRAINT check_exchange_rates_rate_positive CHECK (rate_to_pivot > 0)
);

CREATE INDEX index_exchange_rates_pivot ON exchange_rates (pivot);
