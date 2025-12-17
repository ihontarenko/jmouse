package org.jmouse.jdbc.dialect;

public final class PostgresDialect implements SqlDialect {

    @Override
    public String id() {
        return "POSTGRES";
    }

    @Override
    public String limitOffset(String sql, int offset, int limit) {
        return "%s LIMIT %d OFFSET %d".formatted(sql, limit, offset);
    }

    @Override
    public String sequenceNextValue(String sequence) {
        return "SELECT NEXTVAL('%s')".formatted(sequence);
    }

    @Override
    public String quote(String identifier) {
        return "\"%s\"".formatted(identifier);
    }
}
