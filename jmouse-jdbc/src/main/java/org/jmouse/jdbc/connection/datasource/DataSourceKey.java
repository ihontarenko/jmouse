package org.jmouse.jdbc.connection.datasource;

import java.util.function.Supplier;

/**
 * Abstraction representing a logical key used to resolve a {@link javax.sql.DataSource}.
 * <p>
 * {@code DataSourceKey} is typically used in multi-datasource or routing scenarios,
 * where the actual {@link javax.sql.DataSource} is selected dynamically at runtime
 * based on a contextual key.
 *
 * <h3>Design</h3>
 * <ul>
 *     <li>Extends {@link Supplier} to allow lazy or computed keys</li>
 *     <li>Key value is expected to be stable for the duration of a JDBC operation</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * DataSourceKey tenantKey = () -> "tenant_A";
 *
 * DataSourceKeyHolder.use(tenantKey.get());
 * }</pre>
 *
 * @author jMouse
 */
public interface DataSourceKey extends Supplier<String> {
}
