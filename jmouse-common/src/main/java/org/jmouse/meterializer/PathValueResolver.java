package org.jmouse.meterializer;

import org.jmouse.core.Verify;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.access.ValueNavigator;
import org.jmouse.core.access.accessor.ScalarValueAccessor;

/**
 * Resolves path expressions against root accessor and scoped variables using {@link PropertyPath}.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>If the first path segment matches a scoped variable name, resolve against that variable accessor.</li>
 *   <li>Otherwise resolve against the root accessor.</li>
 *   <li>Resolution is delegated to {@link ValueNavigator}.</li>
 * </ul>
 */
public final class PathValueResolver {

    public Object resolve(String expression, RenderingExecution execution) {
        Verify.nonNull(expression, "expression");
        Verify.nonNull(execution, "execution");

        String trimmed = expression.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        ValueNavigator navigator = execution.valueNavigator();
        PropertyPath   path      = PropertyPath.forPath(trimmed);
        String         head      = path.head().toString();
        ObjectAccessor scoped    = execution.variables().get(head);

        if (scoped != null) {
            PropertyPath remainder = path.sub(1);
            if (remainder.isEmpty()) {
                return unwrap(scoped);
            }
            return unwrap(navigator.navigate(scoped, remainder.toString()));
        }

        return unwrap(navigator.navigate(execution.rootAccessor(), path.toString()));
    }

    private Object unwrap(Object value) {
        if (value instanceof ScalarValueAccessor scalar) {
            return scalar.unwrap();
        }
        if (value instanceof ObjectAccessor accessor) {
            return accessor;
        }
        return value;
    }
}