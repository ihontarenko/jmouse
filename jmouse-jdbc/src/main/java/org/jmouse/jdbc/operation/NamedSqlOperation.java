package org.jmouse.jdbc.operation;

/**
 * Optional contract for a typed SQL operation that provides its own logical name.
 *
 * @author Ivan Hontarenko
 */
public interface NamedSqlOperation extends SqlOperation {

    /**
     * Returns the logical operation name.
     *
     * @return logical operation name
     */
    String operationName();

}