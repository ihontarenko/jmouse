package org.jmouse.jdbc.template;

import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

public interface TemplateOperation<T, R> {

    String sql();

    StatementConfigurer statementConfigurer();

    StatementHandler<R> statementHandler();

    RowMapper<T> mapper();

}
