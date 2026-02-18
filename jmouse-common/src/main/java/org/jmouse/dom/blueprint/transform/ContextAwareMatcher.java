package org.jmouse.dom.blueprint.transform;

import org.jmouse.dom.blueprint.Blueprint;

/**
 * Matcher that can use traversal context (ancestors, depth).
 */
@FunctionalInterface
public interface ContextAwareMatcher {

    /**
     * @param blueprint current node
     * @param context traversal context
     * @return true if matched
     */
    boolean matches(Blueprint blueprint, TraversalContext context);

    static ContextAwareMatcher from(BlueprintMatcher matcher) {
        return (blueprint, context) -> matcher.matches(blueprint);
    }
}
