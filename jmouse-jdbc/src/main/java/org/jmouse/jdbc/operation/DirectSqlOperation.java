package org.jmouse.jdbc.operation;

/**
 * Optional contract for a typed SQL operation that directly provides SQL text.
 *
 * @author Ivan Hontarenko
 */
public interface DirectSqlOperation extends SqlOperation {

    /**
     * Returns SQL text for this operation.
     *
     * @return SQL text
     */
    String sql();

}