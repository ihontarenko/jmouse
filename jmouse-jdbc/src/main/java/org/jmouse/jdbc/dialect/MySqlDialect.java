package org.jmouse.jdbc.dialect;

public final class MySqlDialect implements SqlDialect {

    @Override
    public String id() {
        return "MYSQL";
    }

    @Override
    public String limitOffset(String sql, int offset, int limit) {
        return "%s LIMIT %d, %d".formatted(sql, offset, limit);
    }

    @Override
    public String sequenceNextValue(String sequence) {
        throw new UnsupportedOperationException("MySQL does not support sequences (use AUTO_INCREMENT)");
    }

    @Override
    public String quote(String identifier) {
        return "`%s`".formatted(identifier);
    }
}
