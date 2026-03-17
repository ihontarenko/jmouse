package org.jmouse.meterializer;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.access.ValueNavigator;
import org.jmouse.core.access.accessor.ScalarValueAccessor;

import static org.jmouse.core.Verify.nonNull;

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

    /**
     * Resolve a textual expression against the current rendering execution.
     *
     * @param expression propertyPath expression
     * @param execution rendering execution
     * @return resolved value or {@code null}
     */
    public Object resolve(String expression, RenderingExecution execution) {
        nonNull(execution, "execution");

        if (expression == null) {
            return execution.rootAccessor();
        }

        String trimmed = expression.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return resolve(PropertyPath.forPath(trimmed), execution);
    }

    /**
     * Resolve a pre-parsed property propertyPath against the current rendering execution.
     *
     * @param path parsed propertyPath
     * @param execution rendering execution
     * @return resolved value or {@code null}
     */
    public Object resolve(PropertyPath path, RenderingExecution execution) {
        if (path.isEmpty()) {
            return execution.rootAccessor();
        }

        String         head      = nonNull(path, "propertyPath").getFirst();
        ValueNavigator navigator = nonNull(execution, "execution").valueNavigator();
        ObjectAccessor scoped    = execution.variables().get(head);

        if (scoped != null) {
            if (path.isSimple()) {
                return unwrap(scoped);
            }
            return unwrap(navigator.navigate(scoped, path, 1));
        }

        return unwrap(navigator.navigate(execution.rootAccessor(), path));
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