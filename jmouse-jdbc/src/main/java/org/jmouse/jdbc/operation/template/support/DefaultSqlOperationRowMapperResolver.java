package org.jmouse.jdbc.operation.template.support;

import org.jmouse.jdbc.mapping.BeanRowMapper;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.operation.template.SqlOperationRowMapperResolver;

import static org.jmouse.core.Verify.nonNull;

/**
 * Default {@link SqlOperationRowMapperResolver} implementation.
 *
 * <p>This implementation uses bean mapping as the default strategy.
 * Scalar-specialized mapping may be added later or integrated through a
 * pluggable resolver chain.</p>
 *
 * @author Ivan Hontarenko
 */
public class DefaultSqlOperationRowMapperResolver implements SqlOperationRowMapperResolver {

    @Override
    public <T> RowMapper<T> resolveRowMapper(Class<T> elementType) {
        return BeanRowMapper.of(nonNull(elementType, "elementType"));
    }

}