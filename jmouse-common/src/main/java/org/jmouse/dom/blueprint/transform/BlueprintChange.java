package org.jmouse.dom.blueprint.transform;

import org.jmouse.dom.blueprint.Blueprint;
import org.jmouse.dom.blueprint.RenderingExecution;

/**
 * Applies a change to a matched blueprint node.
 */
@FunctionalInterface
public interface BlueprintChange {

    /**
     * @param blueprint matched node
     * @param execution execution context
     * @return transformed node (may be the same node)
     */
    Blueprint apply(Blueprint blueprint, RenderingExecution execution);
}
