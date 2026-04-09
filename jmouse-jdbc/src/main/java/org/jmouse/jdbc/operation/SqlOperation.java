package org.jmouse.jdbc.operation;

/**
 * Marker contract for a typed SQL operation.
 *
 * <p>This is the common root for all declarative JDBC operations such as
 * queries and updates. The contract is intentionally minimal and does not
 * expose SQL text, binding, mapping, or lookup details.</p>
 *
 * <p>Those concerns belong to dedicated resolver and integration layers.</p>
 *
 * @author Ivan Hontarenko
 */
public interface SqlOperation {
}