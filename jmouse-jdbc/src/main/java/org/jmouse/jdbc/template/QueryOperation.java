package org.jmouse.jdbc.template;

import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.ResultSet;

public interface QueryOperation<T> extends TemplateOperation<T, ResultSet> {

    StatementBinder statementBinder();

    record Default<T>(
            String sql,
            StatementBinder statementBinder,
            StatementHandler<ResultSet> statementHandler,
            StatementConfigurer statementConfigurer,
            RowMapper<T> mapper
    ) implements QueryOperation<T> {

    }

}
