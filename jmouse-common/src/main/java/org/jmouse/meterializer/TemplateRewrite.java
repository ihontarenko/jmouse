package org.jmouse.meterializer;

import org.jmouse.meterializer.transform.TraversalContext;

@FunctionalInterface
public interface TemplateRewrite {

    NodeTemplate rewrite(
            NodeTemplate node,
            RenderingExecution execution,
            TraversalContext context
    );

}