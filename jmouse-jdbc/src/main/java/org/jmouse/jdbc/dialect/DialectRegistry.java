package org.jmouse.jdbc.dialect;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DialectRegistry {

    private final    Map<String, SqlDialect> dialects = new ConcurrentHashMap<>();
    private volatile SqlDialect              fallback = new AnsiDialect();

    public DialectRegistry register(SqlDialect dialect) {
        dialects.put(dialect.id(), dialect);
        return this;
    }

    public DialectRegistry fallback(SqlDialect dialect) {
        this.fallback = Objects.requireNonNull(dialect, "fallback");
        return this;
    }

    public SqlDialect get(String id) {
        SqlDialect dialect = dialects.get(id);
        return dialect != null ? dialect : fallback;
    }

    public boolean contains(String id) {
        return dialects.containsKey(id);
    }
}
