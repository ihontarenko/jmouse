package org.jmouse.template.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractRenderableNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.RenderableNode;

import java.io.Writer;

public class BlockNode extends AbstractRenderableNode {

    private ExpressionNode name;
    private RenderableNode body;

    public ExpressionNode getName() {
        return name;
    }

    public void setName(ExpressionNode name) {
        this.name = name;
    }

    public RenderableNode getBody() {
        return body;
    }

    public void setBody(RenderableNode body) {
        this.body = body;
    }

    @Override
    public void render(Writer writer, EvaluationContext context) {
        super.render(writer, context);
    }

}
