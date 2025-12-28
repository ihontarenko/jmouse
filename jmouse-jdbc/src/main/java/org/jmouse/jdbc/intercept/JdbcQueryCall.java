package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;
import org.jmouse.jdbc.statement.StatementConfigurer;

import java.sql.ResultSet;

public record JdbcQueryCall<T>(
        String sql,
        PreparedStatementBinder binder,
        StatementConfigurer configurer,
        StatementCallback<ResultSet> callback,
        ResultSetExtractor<T> extractor
) implements JdbcCall<T> {

    @Override
    public JdbcOperation operation() {
        return JdbcOperation.QUERY;
    }

    public JdbcQueryCall<T> with(StatementConfigurer configurer) {
        return new JdbcQueryCall<>(sql, binder, StatementConfigurer.chain(this.configurer(), configurer), callback, extractor);
    }

    public JdbcQueryCall<T> with(StatementCallback<ResultSet> callback) {
        return new JdbcQueryCall<>(sql, binder, configurer, callback, extractor);
    }

    public JdbcQueryCall<T> with(ResultSetExtractor<T> extractor) {
        return new JdbcQueryCall<>(sql, binder, configurer, callback, extractor);
    }

    public JdbcQueryCall<T> with(PreparedStatementBinder binder) {
        return new JdbcQueryCall<>(sql, binder, configurer, callback, extractor);
    }

    public JdbcQueryCall<T> with(String sql) {
        return new JdbcQueryCall<>(sql, binder, configurer, callback, extractor);
    }

}
