package org.jmouse.jdbc.operation.resolve;

import org.jmouse.jdbc.operation.SqlOperation;

/**
 * Locates a SQL resource for a typed SQL operation.
 *
 * @author Ivan Hontarenko
 */
public interface SqlResourceLocator {

    /**
     * Resolves a classpath resource location for the given operation.
     *
     * @param operation operation instance
     *
     * @return resolved resource location
     */
    String locate(SqlOperation operation);

}