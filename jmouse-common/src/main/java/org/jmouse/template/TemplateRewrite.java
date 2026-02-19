package org.jmouse.template;

import org.jmouse.template.transform.TraversalContext;

@FunctionalInterface
public interface TemplateRewrite {

    NodeTemplate rewrite(
            NodeTemplate node,
            RenderingExecution execution,
            TraversalContext context
    );

}