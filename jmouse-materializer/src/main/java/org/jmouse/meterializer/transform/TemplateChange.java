package org.jmouse.meterializer.transform;

import org.jmouse.meterializer.NodeTemplate;
import org.jmouse.meterializer.RenderingExecution;

/**
 * Applies a change to a matched blueprint node.
 */
@FunctionalInterface
public interface TemplateChange {

    /**
     * @param blueprint matched node
     * @param execution execution context
     * @return transformed node (may be the same node)
     */
    NodeTemplate apply(NodeTemplate blueprint, RenderingExecution execution);
}
