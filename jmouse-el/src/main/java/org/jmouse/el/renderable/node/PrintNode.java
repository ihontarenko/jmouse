package org.jmouse.el.renderable.node;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.renderable.AbstractRenderableNode;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.renderable.Content;
import org.jmouse.el.renderable.Template;

public class PrintNode extends AbstractRenderableNode {

    private final ExpressionNode expression;

    public PrintNode(ExpressionNode expression) {
        this.expression = expression;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    @Override
    public void render(Content content, Template self, EvaluationContext context) {
        content.append(String.valueOf(expression.evaluate(context)));
    }
}
