package org.jmouse.template.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.rendering.AbstractRenderableNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.Template;
import org.jmouse.el.rendering.RenderableNode;

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
    public void render(Content content, Template self, EvaluationContext context) {
        getBody().render(content, self, context);
    }

}
