package org.jmouse.jdbc.dialect;

/**
 * ANSI-ish fallback dialect.
 *
 * <p>Uses OFFSET/FETCH where supported; otherwise default behavior is conservative.</p>
 */
public class AnsiDialect implements SqlDialect {

    @Override
    public String id() {
        return "ANSI";
    }

    @Override
    public boolean supportsOffsetFetch() {
        return true;
    }

    @Override
    public String limitOffset(String sql, int offset, int limit) {
        // ANSI SQL:2008 style (many DBs accept it)
        // SELECT ... OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public String sequenceNextValue(String sequence) {
        // Not universally supported; keep explicit
        throw new UnsupportedOperationException("Sequences are not supported by ANSI fallback dialect");
    }
}
