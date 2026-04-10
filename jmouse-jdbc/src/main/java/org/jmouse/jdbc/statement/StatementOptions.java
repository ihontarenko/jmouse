package org.jmouse.jdbc.statement;

import org.jmouse.core.Verify;

import java.sql.ResultSet;

/**
 * Resolved statement execution options.
 *
 * @author Ivan Hontarenko
 */
public record StatementOptions(
        StatementConfigurer statementConfigurer,
        StatementHandler<ResultSet> queryStatementHandler,
        StatementHandler<Integer> updateStatementHandler
) {

    public StatementOptions {
        Verify.nonNull(statementConfigurer, "statementConfigurer");
        Verify.nonNull(queryStatementHandler, "queryStatementHandler");
        Verify.nonNull(updateStatementHandler, "updateStatementHandler");
    }

    public static StatementOptions defaults() {
        return new StatementOptions(
                StatementConfigurer.noop(),
                StatementHandler.noop(),
                StatementHandler.noop()
        );
    }

}