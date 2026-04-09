package org.jmouse.jdbc.operation.resolve.support;

import org.jmouse.jdbc.operation.DirectSqlOperation;
import org.jmouse.jdbc.operation.ResourceSqlOperation;
import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.operation.annotation.Sql;
import org.jmouse.jdbc.operation.annotation.SqlResource;
import org.jmouse.jdbc.operation.resolve.SqlOperationTextResolver;
import org.jmouse.jdbc.operation.resolve.SqlResourceLocator;
import org.jmouse.jdbc.operation.resolve.SqlTextLoader;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.core.Verify.notBlank;

/**
 * Default {@link SqlOperationTextResolver} implementation.
 *
 * <p>Resolution order:</p>
 *
 * <ol>
 *     <li>{@link DirectSqlOperation#sql()}</li>
 *     <li>{@link Sql} annotation</li>
 *     <li>{@link ResourceSqlOperation#resource()}</li>
 *     <li>{@link SqlResource} annotation</li>
 *     <li>convention-based resource lookup</li>
 * </ol>
 *
 * @author Ivan Hontarenko
 */
public class DefaultSqlOperationTextResolver implements SqlOperationTextResolver {

    private final SqlTextLoader      textLoader;
    private final SqlResourceLocator resourceLocator;

    public DefaultSqlOperationTextResolver(SqlTextLoader textLoader, SqlResourceLocator resourceLocator) {
        this.textLoader = nonNull(textLoader, "textLoader");
        this.resourceLocator = nonNull(resourceLocator, "resourceLocator");
    }

    @Override
    public String resolveSql(SqlOperation operation) {
        nonNull(operation, "operation");

        if (operation instanceof DirectSqlOperation direct) {
            return notBlank(direct.sql(), "sql");
        }

        Class<?> operationType = operation.getClass();
        Sql      sql           = operationType.getAnnotation(Sql.class);

        if (sql != null) {
            return notBlank(sql.value(), "sql");
        }

        if (operation instanceof ResourceSqlOperation resourceOperation) {
            return load(resourceOperation.resource());
        }

        SqlResource resource = operationType.getAnnotation(SqlResource.class);

        if (resource != null) {
            return load(resource.value());
        }

        return load(resourceLocator.locate(operation));
    }

    protected String load(String location) {
        return textLoader.load(notBlank(location, "location"));
    }

}