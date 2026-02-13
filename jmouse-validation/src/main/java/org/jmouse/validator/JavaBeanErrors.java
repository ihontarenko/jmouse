package org.jmouse.validator;

import org.jmouse.core.Verify;
import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.core.access.ValueNavigator;

/**
 * {@link Errors} implementation backed by {@link ObjectAccessor} for JavaBean-like targets. 🫘
 *
 * <p>This implementation resolves:</p>
 * <ul>
 *   <li>field values via {@link ValueNavigator} (supports simple and complex paths)</li>
 *   <li>field types via {@link ObjectAccessor#navigate(String)} when available</li>
 * </ul>
 *
 * <p>Navigation is safe: unknown paths return {@code null}.</p>
 */
public final class JavaBeanErrors extends AbstractErrors {

    private static final AccessorWrapper WRAPPER = new ObjectAccessorWrapper();

    /**
     * Default navigator supports both simple and structured {@code PropertyPath}-style navigation.
     */
    private static final ValueNavigator NAVIGATOR = ValueNavigator.defaultNavigator();

    private final ObjectAccessor accessor;

    /**
     * Create an errors collector for a JavaBean target.
     *
     * @param target validated bean instance (may be {@code null})
     * @param objectName logical object name used in error codes (may be {@code null})
     */
    public JavaBeanErrors(Object target, String objectName) {
        super(target, objectName);
        this.accessor = WRAPPER.wrap(target);
    }

    /**
     * Resolve current field value using {@link #NAVIGATOR}.
     *
     * <p>If the field/path cannot be resolved, returns {@code null}.</p>
     *
     * @param field field name/path
     * @return resolved field value or {@code null}
     */
    @Override
    protected Object tryGetValue(String field) {
        if (field == null || field.isBlank() || accessor == null) {
            return null;
        }
        return NAVIGATOR.navigate(accessor, field);
    }

    /**
     * Resolve field type using accessor navigation.
     *
     * <p>If navigation fails or the field/path cannot be resolved, returns {@code null}.</p>
     *
     * @param field field name/path
     * @return resolved field type or {@code null}
     */
    @Override
    protected Class<?> tryGetType(String field) {
        if (field == null || field.isBlank() || accessor == null) {
            return null;
        }

        try {
            ObjectAccessor nested = accessor.navigate(field);
            if (nested == null || nested.isNull()) {
                return null;
            }
            return nested.getClassType();
        } catch (RuntimeException ignore) {
            return null;
        }
    }
}
