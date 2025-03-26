package org.jmouse.template.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.rendering.AbstractRenderableNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.RenderableEntity;

public class PrintNode extends AbstractRenderableNode {

    private final ExpressionNode expression;

    public PrintNode(ExpressionNode expression) {
        this.expression = expression;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    @Override
    public void render(Content content, RenderableEntity entity, EvaluationContext context) {
        content.append(String.valueOf(expression.evaluate(context)));
    }
}
