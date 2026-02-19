package org.jmouse.template.rules;

import org.jmouse.dom.Node;
import org.jmouse.template.RenderingExecution;

/**
 * Imperative rule applied to a materialized node tree.
 *
 * <p>Use rules for advanced customization that is hard to express as directives.</p>
 */
public interface NodeRule {

    int order();

    boolean matches(Node node, RenderingExecution execution);

    void apply(Node node, RenderingExecution execution);

}
