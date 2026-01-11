package org.jmouse.core.context.keys;

import org.jmouse.core.Verify;

/**
 * Context key resolution strategy.
 *
 * Resolves a stable storage key for a given value.
 */
public interface ContextKeyResolver {

    /**
     * Resolve a stable context key for the given value.
     *
     * @param value source value (must be non-null)
     * @return resolved key (never {@code null})
     */
    Object resolveKeyFor(Object value);

    /**
     * Alias of {@link #resolveKeyFor(Object)} with a more explicit name.
     *
     * @param value source value (must be non-null)
     * @return resolved key (never {@code null})
     */
    default Object resolveKeyForValue(Object value) {
        Verify.nonNull(value, "value");
        return resolveKeyFor(value);
    }
}
