package org.jmouse.jdbc.statement;

/**
 * Provides a custom {@link StatementConfigurer} for a SQL operation.
 *
 * <p>This contract is optional and may be implemented by operations that want
 * to customize low-level statement configuration, such as fetch size, timeout,
 * or driver-specific tuning.</p>
 *
 * @author Ivan Hontarenko
 */
public interface StatementConfigurerProvider {

    /**
     * Returns statement configurer for this operation.
     *
     * @return statement configurer, never {@code null}
     */
    default StatementConfigurer statementConfigurer() {
        return StatementConfigurer.NOOP;
    }

}