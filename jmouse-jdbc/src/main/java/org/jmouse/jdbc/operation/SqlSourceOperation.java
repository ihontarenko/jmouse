package org.jmouse.jdbc.operation;

import org.jmouse.jdbc.operation.source.SqlSource;

/**
 * Optional contract for a typed SQL operation that explicitly provides its SQL source.
 *
 * <p>This contract is useful when SQL should be defined directly by the
 * operation type instead of annotations.</p>
 *
 * @author Ivan Hontarenko
 */
public interface SqlSourceOperation extends SqlOperation {

    /**
     * Returns SQL source for this operation.
     *
     * @return SQL source
     */
    SqlSource sqlSource();

}