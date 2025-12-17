package org.jmouse.jdbc.core;

import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.PreparedStatementBinder;

public interface JdbcOperations {

    <T> T query(String sql, RowMapper<T> mapper);
    <T> T query(String sql, PreparedStatementBinder binder, ResultSetExtractor<T> extractor);

    int update(String sql);
    int update(String sql, PreparedStatementBinder binder);
}
