package org.jmouse.core.mapping.binding;

import org.jmouse.core.mapping.MappingContext;

/**
 * Strategy interface for resolving {@link TypeMappingRule mapping rules}. 🧭
 *
 * <p>
 * A {@code TypeMappingRuleSource} provides lookup capabilities for
 * mapping definitions between a source type and a target type.
 * Implementations may use different resolution strategies such as:
 * </p>
 * <ul>
 *     <li>static in-memory registries</li>
 *     <li>dynamic rule generation</li>
 *     <li>annotation-driven discovery</li>
 *     <li>external configuration sources</li>
 * </ul>
 *
 * <p>
 * This abstraction allows the mapping engine to remain decoupled
 * from how rules are stored and resolved.
 * </p>
 */
public interface TypeMappingRuleSource {

    /**
     * Resolves a mapping rule for the given source and target types.
     *
     * <p>
     * Implementations should return the <b>most specific</b> matching rule
     * according to their internal resolution strategy (for example:
     * exact match, assignable types, fallback rules, etc.).
     * </p>
     *
     * @param sourceType source type (must not be {@code null})
     * @param targetType target type (must not be {@code null})
     * @param context    current mapping context
     *
     * @return resolved mapping rule, or {@code null} if this source
     *         cannot provide a suitable rule
     */
    TypeMappingRule find(Class<?> sourceType, Class<?> targetType, MappingContext context);

}