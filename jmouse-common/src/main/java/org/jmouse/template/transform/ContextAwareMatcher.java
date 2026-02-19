package org.jmouse.template.transform;

import org.jmouse.template.NodeTemplate;

/**
 * Matcher that can use traversal context (ancestors, depth, parent).
 */
@FunctionalInterface
public interface ContextAwareMatcher {

    /**
     * @param blueprint current node
     * @param context traversal context (never null)
     */
    boolean matches(NodeTemplate blueprint, TraversalContext context);

    /**
     * Convenience: treat as root-scope match.
     */
    default boolean matches(NodeTemplate blueprint) {
        return matches(blueprint, TraversalContext.empty());
    }

    static ContextAwareMatcher any() {
        return (blueprint, context) -> true;
    }

    static ContextAwareMatcher never() {
        return (blueprint, context) -> false;
    }

}
