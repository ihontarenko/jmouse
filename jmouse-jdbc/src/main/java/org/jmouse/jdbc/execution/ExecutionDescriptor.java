package org.jmouse.jdbc.execution;

import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

/**
 * 📦 Runtime execution descriptor.
 *
 * <p>Represents a fully prepared JDBC execution unit.</p>
 *
 * <pre>{@code
 * ExecutionDescriptor descriptor = ...
 * jdbc.execute(descriptor);
 * }</pre>
 */
public interface ExecutionDescriptor {

    /**
     * Final SQL (ready for JDBC).
     */
    String sql();

    /**
     * Parameter binder.
     */
    default StatementBinder binder() {
        return StatementBinder.noop();
    }

    /**
     * Statement configuration (timeouts, fetch size, etc).
     */
    default StatementConfigurer statementConfigurer() {
        return StatementConfigurer.noop();
    }

    /**
     * Statement lifecycle hook.
     */
    default StatementHandler<?> statementHandler() {
        return StatementHandler.noop();
    }

}