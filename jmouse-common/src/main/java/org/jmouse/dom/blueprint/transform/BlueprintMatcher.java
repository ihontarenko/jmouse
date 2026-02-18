package org.jmouse.dom.blueprint.transform;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.dom.blueprint.Blueprint;

/**
 * Matches a blueprint node.
 */
@FunctionalInterface
public interface BlueprintMatcher extends Matcher<Blueprint> {

    /**
     * @param blueprint blueprint node
     * @return true if matched
     */
    boolean matches(Blueprint blueprint);

    static BlueprintMatcher all() {
        return blueprint -> true;
    }
}