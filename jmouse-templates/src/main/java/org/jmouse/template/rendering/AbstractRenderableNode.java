package org.jmouse.template.rendering;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.RenderingException;
import org.jmouse.el.node.AbstractNode;

public class AbstractRenderableNode extends AbstractNode implements RenderableNode {

    @Override
    public void render(Content content, RenderableEntity entity, EvaluationContext context) {
        throw new RenderingException("Renderable node '%s' is not supported yet.".formatted(this.getClass()));
    }

}
