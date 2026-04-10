package org.jmouse.jdbc.operation.template;

import org.jmouse.jdbc.mapping.RowMapper;

/**
 * Resolves a {@link RowMapper} for a mapped SQL row element type.
 *
 * @author Ivan Hontarenko
 */
public interface SqlOperationRowMapperResolver {

    /**
     * Resolves a row mapper for the given row element type.
     *
     * @param elementType mapped row element type
     * @param <T>         mapped row element type
     *
     * @return resolved row mapper
     */
    <T> RowMapper<T> resolveRowMapper(Class<T> elementType);

}