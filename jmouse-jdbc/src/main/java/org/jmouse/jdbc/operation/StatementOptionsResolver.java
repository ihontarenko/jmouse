package org.jmouse.jdbc.operation;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.statement.*;

import java.sql.ResultSet;

/**
 * Resolves {@link StatementOptions} for a SQL operation.
 *
 * @author Ivan Hontarenko
 */
public interface StatementOptionsResolver {

    StatementOptions resolve(SqlOperation operation);

    final class Default implements StatementOptionsResolver {

        @Override
        public StatementOptions resolve(SqlOperation operation) {
            Verify.nonNull(operation, "operation");

            StatementConfigurer         configurer    = StatementConfigurer.NOOP;
            StatementHandler<ResultSet> queryHandler  = StatementHandler.noop();
            StatementHandler<Integer>   updateHandler = StatementHandler.noop();

            if (operation instanceof StatementConfigurerProvider provider) {
                configurer = Verify.nonNull(provider.statementConfigurer(), "statementConfigurer");
            }

            if (operation instanceof QueryStatementHandlerProvider provider) {
                queryHandler = Verify.nonNull(provider.queryStatementHandler(), "queryStatementHandler");
            }

            if (operation instanceof UpdateStatementHandlerProvider provider) {
                updateHandler = Verify.nonNull(provider.updateStatementHandler(), "updateStatementHandler");
            }

            return new StatementOptions(configurer, queryHandler, updateHandler);
        }

    }

}