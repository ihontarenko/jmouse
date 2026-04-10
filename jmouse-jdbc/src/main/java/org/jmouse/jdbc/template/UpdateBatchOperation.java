package org.jmouse.jdbc.template;

import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.ResultSet;
import java.util.List;

public interface UpdateBatchOperation<T> extends TemplateOperation<T, ResultSet> {

    List<? extends StatementBinder> statementBinders();

    record Default<T>(
            String sql,
            StatementConfigurer statementConfigurer,
            StatementHandler<ResultSet> statementHandler,
            RowMapper<T> mapper,
            List<? extends StatementBinder> statementBinders
    ) implements UpdateBatchOperation<T> {

    }

}
