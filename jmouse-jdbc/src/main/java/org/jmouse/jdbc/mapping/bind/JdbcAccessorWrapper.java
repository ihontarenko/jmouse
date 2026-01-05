package org.jmouse.jdbc.mapping.bind;

import org.jmouse.core.bind.ObjectAccessorWrapper;
import org.jmouse.core.bind.StandardAccessorWrapper;

/**
 * JDBC-specific {@link ObjectAccessorWrapper} implementation.
 * <p>
 * {@code JdbcAccessorWrapper} extends the standard accessor wrapper by
 * registering JDBC-aware accessor providers, enabling seamless integration
 * of {@link java.sql.ResultSet} into the jMouse binding infrastructure.
 *
 * <p>
 * This wrapper is typically used internally by JDBC row mappers and binders
 * to ensure that {@link java.sql.ResultSet} instances can be treated as
 * readable object graphs.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Inherit default accessor providers from {@link StandardAccessorWrapper}</li>
 *     <li>Register {@link ResultSetAccessorProvider} for JDBC result sets</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * ObjectAccessor accessor =
 *     JdbcAccessorWrapper.WRAPPER.wrap(resultSet);
 * }</pre>
 *
 * <p>
 * A shared singleton instance ({@link #WRAPPER}) is provided for reuse.
 *
 * @author jMouse
 */
public class JdbcAccessorWrapper extends StandardAccessorWrapper {

    /**
     * Shared singleton wrapper instance.
     */
    public static final ObjectAccessorWrapper WRAPPER = new JdbcAccessorWrapper();

    /**
     * Creates a new {@code JdbcAccessorWrapper} and registers JDBC-specific
     * accessor providers.
     */
    public JdbcAccessorWrapper() {
        super();
        registerProvider(new ResultSetAccessorProvider());
    }

}
