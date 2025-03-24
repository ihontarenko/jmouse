package org.jmouse.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.RenderingException;

import java.io.Writer;

public class AbstractRenderableNode extends AbstractNode implements RenderableNode {

    @Override
    public void render(Writer writer, EvaluationContext context) {
        throw new RenderingException("Renderable node '%s' is not supported yet.".formatted(this.getClass()));
    }

}
