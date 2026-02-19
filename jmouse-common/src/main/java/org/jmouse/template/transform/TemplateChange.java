package org.jmouse.template.transform;

import org.jmouse.template.NodeTemplate;
import org.jmouse.template.RenderingExecution;

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
