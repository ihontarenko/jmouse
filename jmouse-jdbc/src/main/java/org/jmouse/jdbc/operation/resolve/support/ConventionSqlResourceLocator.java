package org.jmouse.jdbc.operation.resolve.support;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.NamedSqlOperation;
import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.operation.resolve.SqlResourceLocator;

/**
 * Convention-based {@link SqlResourceLocator}.
 *
 * <p>Default convention:</p>
 *
 * <pre>{@code
 * GetUserById -> sql/get-user-by-id.sql
 * }</pre>
 *
 * @author Ivan Hontarenko
 */
public class ConventionSqlResourceLocator implements SqlResourceLocator {

    private final String basePath;

    public ConventionSqlResourceLocator() {
        this("sql");
    }

    public ConventionSqlResourceLocator(String basePath) {
        this.basePath = Verify.notBlank(basePath, "basePath");
    }

    @Override
    public String locate(SqlOperation operation) {
        Verify.nonNull(operation, "operation");

        String name;

        if (operation instanceof NamedSqlOperation named) {
            name = Verify.notBlank(named.operationName(), "operationName");
        } else {
            name = deriveName(operation.getClass());
        }

        return basePath + "/" + name + ".sql";
    }

    protected String deriveName(Class<?> operationType) {
        String simpleName = operationType.getSimpleName();

        if (simpleName.isBlank()) {
            throw new IllegalStateException("Cannot derive SQL resource name from anonymous type: " + operationType.getName());
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