package org.jmouse.dom.blueprint.transform;

import org.jmouse.dom.blueprint.Blueprint;

/**
 * Matcher that can use traversal context (ancestors, depth, parent).
 */
@FunctionalInterface
public interface ContextAwareMatcher {

    /**
     * @param blueprint current node
     * @param context traversal context (never null)
     */
    boolean matches(Blueprint blueprint, TraversalContext context);

    /**
     * Convenience: treat as root-scope match.
     */
    default boolean matches(Blueprint blueprint) {
        return matches(blueprint, TraversalContext.empty());
    }

    static ContextAwareMatcher any() {
        return (blueprint, context) -> true;
    }

    static ContextAwareMatcher never() {
        return (blueprint, context) -> false;
    }

}
