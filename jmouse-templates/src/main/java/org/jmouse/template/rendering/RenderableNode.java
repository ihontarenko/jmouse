package org.jmouse.template.rendering;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;

public interface RenderableNode extends Node {

    void render(Content content, RenderableEntity entity, EvaluationContext context);

}
