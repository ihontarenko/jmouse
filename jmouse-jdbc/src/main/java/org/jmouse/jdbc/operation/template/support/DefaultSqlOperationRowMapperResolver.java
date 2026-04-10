package org.jmouse.jdbc.operation.template.support;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.mapping.BeanRowMapper;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.mapping.RowMappers;
import org.jmouse.jdbc.operation.template.SqlOperationRowMapperResolver;

/**
 * Default {@link SqlOperationRowMapperResolver} implementation.
 *
 * <p>Scalar element types are mapped using predefined single-column row mappers.
 * All other types are mapped using {@link BeanRowMapper}.</p>
 *
 * @author Ivan Hontarenko
 */
public class DefaultSqlOperationRowMapperResolver implements SqlOperationRowMapperResolver {

    @Override
    @SuppressWarnings("unchecked")
    public <T> RowMapper<T> resolveRowMapper(Class<T> elementType) {
        Verify.nonNull(elementType, "elementType");

        if (elementType == String.class) {
            return (RowMapper<T>) RowMappers.stringColumn(1);
        }

        if (elementType == Long.class) {
            return (RowMapper<T>) RowMappers.longColumn(1);
        }

        if (elementType == Integer.class) {
            return (RowMapper<T>) RowMappers.integerColumn(1);
        }

        if (elementType == Boolean.class) {
            return (RowMapper<T>) RowMappers.booleanColumn(1);
        }

        return BeanRowMapper.of(elementType);
    }

}