package org.jmouse.template.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.rendering.AbstractRenderableNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.RenderableEntity;

public class ExtendsNode extends AbstractRenderableNode {

    private ExpressionNode parent;

    public ExpressionNode getParent() {
        return parent;
    }

    public void setParent(ExpressionNode parent) {
        this.parent = parent;
    }

    @Override
    public void render(Content content, RenderableEntity entity, EvaluationContext context) {
        super.render(content, entity, context);
    }

}
