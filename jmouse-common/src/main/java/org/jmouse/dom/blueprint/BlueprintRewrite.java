package org.jmouse.dom.blueprint;

import org.jmouse.dom.blueprint.transform.TraversalContext;

@FunctionalInterface
public interface BlueprintRewrite {

    Blueprint rewrite(
            Blueprint node,
            RenderingExecution execution,
            TraversalContext context
    );

}