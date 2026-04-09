package org.jmouse.jdbc.operation;

/**
 * Optional contract for a typed SQL operation that points to a SQL resource.
 *
 * @author Ivan Hontarenko
 */
public interface ResourceSqlOperation extends SqlOperation {

    /**
     * Returns the classpath resource location of SQL text.
     *
     * @return SQL resource location
     */
    String resource();

}