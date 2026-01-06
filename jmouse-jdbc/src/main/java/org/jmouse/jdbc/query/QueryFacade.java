package org.jmouse.jdbc.query;

import org.jmouse.jdbc.SimpleOperations;
import org.jmouse.jdbc.database.DatabasePlatform;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;

public final class QueryFacade {

    private final SimpleOperations simple;
    private final DatabasePlatform platform;

    public QueryFacade(SimpleOperations simple, DatabasePlatform platform) {
        this.simple = simple;
        this.platform = platform;
    }

    public static QueryFacade of(SimpleOperations simple, DatabasePlatform platform) {
        return new QueryFacade(simple, platform);
    }

    public <T> Page<T> page(
            String sql,
            StatementBinder binder,
            RowMapper<T> mapper,
            OffsetLimit page,
            StatementHandler<?> handler
    ) throws SQLException {

        var pagination = platform.pagination();

        String pagedSql = pagination.apply(sql, OffsetLimit.of(page.offset(), page.limit() + 1));
        var pageBind = pagination.bind(OffsetLimit.of(page.offset(), page.limit() + 1));

        StatementBinder combined = statement -> {
            int index = 1;
            binder.bind(statement);
            // assume binder consumed N parameters; if you need exact, evolve binder to expose param count
            // For now: put pagination at the end; user SQL must have its own params first.
            pageBind.bind(statement, guessNextIndex(statement));
        };

        ResultSetExtractor<Page<T>> extractor = PageExtractors.offsetLimit(page, mapper);

        // IMPORTANT: this requires your SimpleOperations / executor to expose handler-enabled overloads.
        return ((HandlerCapableOperations) simple).query(sql, combined, extractor, handler);
    }

    private int guessNextIndex(java.sql.PreparedStatement ps) {
        // With pure JDBC you can't read "current param index".
        // So the correct solution is "CountedBinder" (binder + paramCount).
        // We'll do it in the next refinement step.
        return 1;
    }

    /**
     * Adapter contract: SimpleOperations variant that supports StatementHandler.
     * Implement this in SimpleTemplate with extra overloads.
     */
    public interface HandlerCapableOperations {
        <T> T query(String sql, StatementBinder binder, ResultSetExtractor<T> extractor, StatementHandler<?> handler)
                throws SQLException;
    }
}
