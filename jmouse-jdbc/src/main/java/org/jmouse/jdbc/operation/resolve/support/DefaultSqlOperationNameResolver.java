package org.jmouse.jdbc.operation.resolve.support;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.NamedSqlOperation;
import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.operation.annotation.SqlName;
import org.jmouse.jdbc.operation.resolve.SqlOperationNameResolver;

/**
 * Default {@link SqlOperationNameResolver} implementation.
 *
 * <p>Name resolution order:</p>
 *
 * <ol>
 *     <li>{@link NamedSqlOperation#operationName()}</li>
 *     <li>{@link SqlName} annotation</li>
 *     <li>derived class-based name</li>
 * </ol>
 *
 * @author Ivan Hontarenko
 */
public class DefaultSqlOperationNameResolver implements SqlOperationNameResolver {

    @Override
    public String resolveName(SqlOperation operation) {
        Verify.nonNull(operation, "operation");

        if (operation instanceof NamedSqlOperation named) {
            return Verify.notBlank(named.operationName(), "operationName");
        }

        Class<?> operationType = operation.getClass();
        SqlName  annotation    = operationType.getAnnotation(SqlName.class);

        if (annotation != null) {
            return Verify.notBlank(annotation.value(), "sqlName");
        }

        return deriveName(operationType);
    }

    protected String deriveName(Class<?> operationType) {
        String simpleName = operationType.getSimpleName();

        if (simpleName.isBlank()) {
            throw new IllegalStateException("Cannot derive SQL operation name from anonymous type: " + operationType.getName());
        }

        return toKebabCase(simpleName);
    }

    protected String toKebabCase(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 8);

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            if (Character.isUpperCase(character)) {
                if (index > 0) {
                    builder.append('-');
                }
                builder.append(Character.toLowerCase(character));
            } else {
                builder.append(character);
            }
        }

        return builder.toString();
    }

}