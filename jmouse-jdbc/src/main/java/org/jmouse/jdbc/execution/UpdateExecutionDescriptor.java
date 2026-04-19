package org.jmouse.jdbc.execution;

import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

/**
 * 📦 Update execution descriptor.
 *
 * <p>Represents INSERT / UPDATE / DELETE execution.</p>
 *
 * <pre>{@code
 * UpdateExecutionDescriptor d = new UpdateExecutionDescriptor.Default(
 *     "update users set name = ? where id = ?",
 *     binder,
 *     configurer,
 *     handler,
 *     null
 * );
 * }</pre>
 */
public interface UpdateExecutionDescriptor extends ExecutionDescriptor {

    /**
     * Optional key extractor (for generated keys).
     */
    KeyExtractor<?> keyExtractor();

    /**
     * 🧱 Default immutable implementation.
     */
    final class Default implements UpdateExecutionDescriptor {

        private final String              sql;
        private final StatementConfigurer configurer;
        private final StatementBinder     binder;
        private final StatementHandler<?> handler;
        private final KeyExtractor<?>     keyExtractor;

        public Default(
                String sql,
                StatementBinder binder,
                StatementConfigurer configurer,
                StatementHandler<?> handler,
                KeyExtractor<?> keyExtractor
        ) {
            this.sql = sql;
            this.binder = binder;
            this.configurer = configurer;
            this.handler = handler;
            this.keyExtractor = keyExtractor;
        }

        @Override
        public String sql() {
            return sql;
        }

        @Override
        public StatementBinder binder() {
            return binder;
        }

        @Override
        public StatementConfigurer statementConfigurer() {
            return configurer;
        }

        @Override
        public StatementHandler<?> statementHandler() {
            return handler;
        }

        @Override
        public KeyExtractor<?> keyExtractor() {
            return keyExtractor;
        }
    }
}