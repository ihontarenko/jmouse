package org.jmouse.core.context.keys;

import org.jmouse.core.Verify;
import org.jmouse.core.context.mutable.MutableKeyValueContext;

/**
 * 🧰 Context key resolution helpers.
 *
 * <p>
 * Utility methods for resolving context keys
 * from values and applying them to a mutable context.
 * </p>
 */
public final class ContextKeyResolvers {

    private ContextKeyResolvers() {
    }

    /**
     * 🔗 Resolve key for value and store it in context.
     *
     * <p>
     * Delegates key derivation to {@link ContextKeyResolver}
     * and applies the resolved key-value pair to the context.
     * </p>
     *
     * @param context  target mutable context
     * @param resolver key resolution strategy
     * @param value    value to store
     *
     * @throws IllegalArgumentException if any argument is {@code null}
     */
    public static void setResolvedValue(
            MutableKeyValueContext context,
            ContextKeyResolver resolver,
            Object value
    ) {
        Verify.nonNull(context, "context");
        Verify.nonNull(resolver, "resolver");
        Verify.nonNull(value, "value");

        Object key = resolver.resolveKeyFor(value);
        context.setValue(key, value);
    }
}
