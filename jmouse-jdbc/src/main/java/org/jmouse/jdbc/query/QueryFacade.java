package org.jmouse.jdbc.query;

import org.jmouse.jdbc.SimpleOperations;
import org.jmouse.jdbc.database.DatabasePlatform;
import org.jmouse.jdbc.database.PaginationStrategy;
import org.jmouse.jdbc.database.PaginationStrategy.PaginationBind;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.CountedStatementBinder;
import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.PreparedStatement;
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

        PaginationStrategy pagination = platform.pagination();
        OffsetLimit        plusOne    = OffsetLimit.of(page.offset(), page.limit() + 1);
        String             pagedSql   = pagination.apply(sql, plusOne);
        PaginationBind     pageBind   = pagination.bind(plusOne);

        StatementBinder combined = statement -> {
            StatementBinder safeBinder = (binder != null ? binder : StatementBinder.NOOP);
            safeBinder.bind(statement);

            int startIndex = nextIndexAfterBinder(statement, safeBinder);
            pageBind.bind(statement, startIndex);
        };

        ResultSetExtractor<Page<T>> extractor = PageExtractors.offsetLimit(page, mapper);

        return ((HandlerCapableOperations) simple).query(pagedSql, combined, extractor, handler);
    }

    private int nextIndexAfterBinder(PreparedStatement ps, StatementBinder binder) {
        if (binder instanceof CountedStatementBinder counted) {
            return 1 + counted.countOfParameters();
        }
        return guessNextIndex(ps);
    }

    private int guessNextIndex(PreparedStatement preparedStatement) {
        // With pure JDBC you can't read "current param index".
        // Temporary fallback: assume binder has 0 params.
        return 1;
    }

    public interface HandlerCapableOperations {
        <T> T query(
                String sql,
                StatementBinder binder,
                ResultSetExtractor<T> extractor,
                StatementHandler<?> handler
        ) throws SQLException;
    }
}
