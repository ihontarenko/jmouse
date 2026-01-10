package org.jmouse.core.context.keys;

/**
 * 🗝 Context key resolution strategy.
 *
 * <p>
 * Resolves a context key for a given value,
 * allowing indirect or convention-based
 * key mapping.
 * </p>
 *
 * <p>
 * Common use cases:
 * </p>
 * <ul>
 *   <li>Deriving keys from value type</li>
 *   <li>Mapping domain objects to context keys</li>
 *   <li>Supporting implicit context population</li>
 * </ul>
 */
public interface ContextKeyResolver {

    /**
     * 🔍 Resolve context key for given value.
     *
     * @param value source value
     * @return resolved context key (may be {@code null})
     */
    Object resolveKeyFor(Object value);
}
