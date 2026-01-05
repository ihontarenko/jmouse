package org.jmouse.jdbc.query;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.SimpleOperations;
import org.jmouse.jdbc.database.DatabasePlatform;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.StatementBinder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fluent SQL query facade (read-only builder).
 * Built on top of {@link SimpleOperations} and {@link DatabasePlatform}.
 */
public final class Query {

    private final String       table;
    private final List<String> columns = new ArrayList<>();
    private final List<String> where   = new ArrayList<>();
    private final List<String> orderBy = new ArrayList<>();

    private StatementBinder binder = StatementBinder.NOOP;

    private Long    offset;
    private Integer limit;

    private Query(String table) {
        this.table = Contract.nonNull(table, "table");
    }

    public static Query table(String table) {
        return new Query(table);
    }

    public Query select(String... columns) {
        if (columns != null) {
            for (String column : columns) {
                if (column != null && !column.isBlank()) {
                    this.columns.add(column);
                }
            }
        }
        return this;
    }

    public Query where(String predicate, StatementBinder binder) {
        Contract.nonNull(predicate, "predicate");
        where.add(predicate);
        if (binder != null && binder != StatementBinder.NOOP) {
            this.binder = StatementBinder.chain(this.binder, binder);
        }
        return this;
    }

    public Query orderBy(String... order) {
        if (order != null) {
            for (String o : order) {
                if (o != null && !o.isBlank()) orderBy.add(o);
            }
        }
        return this;
    }

    public Query offset(long offset) {
        Contract.require(offset >= 0, "offset must be >= 0");
        this.offset = offset;
        return this;
    }

    public Query limit(int limit) {
        Contract.require(limit > 0, "limit must be > 0");
        this.limit = limit;
        return this;
    }

    public <T> List<T> list(SimpleOperations jdbc, RowMapper<T> mapper, DatabasePlatform platform) throws SQLException {
        Contract.nonNull(jdbc, "jdbc");
        Contract.nonNull(mapper, "mapper");
        Contract.nonNull(platform, "platform");
        String sql = toSQL(platform);
        return jdbc.query(sql, binder, mapper);
    }

    public <T> Optional<T> single(SimpleOperations template, RowMapper<T> mapper, DatabasePlatform platform) throws SQLException {
        Contract.nonNull(platform, "platform");
        Contract.nonNull(template, "template");
        Contract.nonNull(mapper, "mapper");

        Query       query  = this.limit(1);
        List<T>     list   = query.list(template, mapper, platform);
        Optional<T> result = list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());

        return result;
    }

    public <T> Page<T> page(SimpleOperations jdbc, RowMapper<T> mapper, DatabasePlatform platform) throws SQLException {
        Contract.require(limit != null, "limit is required for page()");
        Contract.require(offset != null, "offset is required for page()");
        List<T> data = list(jdbc, mapper, platform);
        return new Page<>(offset, limit, data);
    }

    private String toSQL(DatabasePlatform platform) {
        String select = columns.isEmpty() ? "*" : String.join(", ", columns);

        StringBuilder sql = new StringBuilder(128)
                .append("SELECT ").append(select)
                .append(" FROM ").append(table);

        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }
        if (!orderBy.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderBy));
        }

        String base = sql.toString();

        if (limit != null) {
            long off = (offset != null) ? offset : 0L;
            return platform.pagination().apply(base, off, limit);
        }

        return base;
    }
}
