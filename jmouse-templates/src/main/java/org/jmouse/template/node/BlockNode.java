package org.jmouse.template.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.template.rendering.AbstractRenderableNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.template.rendering.Content;
import org.jmouse.template.rendering.RenderableEntity;
import org.jmouse.template.rendering.RenderableNode;

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
    public void render(Content content, RenderableEntity entity, EvaluationContext context) {
        super.render(content, entity, context);
    }

}
