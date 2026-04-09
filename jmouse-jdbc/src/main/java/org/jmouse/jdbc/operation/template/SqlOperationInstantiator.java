package org.jmouse.jdbc.operation.template;

import org.jmouse.jdbc.operation.SqlOperation;

/**
 * Strategy for creating typed SQL operation instances from their classes.
 *
 * @author Ivan Hontarenko
 */
public interface SqlOperationInstantiator {

    /**
     * Creates an operation instance for the given operation type.
     *
     * @param operationType operation class
     * @param <O>           operation type
     *
     * @return instantiated operation
     */
    <O extends SqlOperation> O instantiate(Class<O> operationType);

}