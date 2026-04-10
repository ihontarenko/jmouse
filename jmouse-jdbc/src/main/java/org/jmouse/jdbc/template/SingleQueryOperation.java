package org.jmouse.jdbc.template;

import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.ResultSet;
import java.util.Optional;

public interface SingleQueryOperation<T> extends QueryOperation<Optional<T>> {

    record Default<T>(
            String sql,
            StatementConfigurer statementConfigurer,
            StatementHandler<ResultSet> statementHandler,
            RowMapper<T> mapper
    ) implements QueryOperation<T> {

    }

}
