package org.jmouse.dom.template;

import org.jmouse.dom.template.transform.TraversalContext;

@FunctionalInterface
public interface TemplateRewrite {

    NodeTemplate rewrite(
            NodeTemplate node,
            RenderingExecution execution,
            TraversalContext context
    );

}