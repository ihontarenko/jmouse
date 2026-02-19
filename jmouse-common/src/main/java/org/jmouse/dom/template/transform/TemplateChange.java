package org.jmouse.dom.template.transform;

import org.jmouse.dom.template.NodeTemplate;
import org.jmouse.dom.template.RenderingExecution;

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
