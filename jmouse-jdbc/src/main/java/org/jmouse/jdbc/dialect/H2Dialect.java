package org.jmouse.jdbc.dialect;

public final class H2Dialect implements SqlDialect {

    @Override
    public String id() {
        return "H2";
    }

    @Override
    public String limitOffset(String sql, int offset, int limit) {
        return "%s LIMIT %d OFFSET %d".formatted(sql, limit, offset);
    }

    @Override
    public String sequenceNextValue(String sequence) {
        return "SELECT NEXT VALUE FOR %s".formatted(sequence);
    }
}
