package org.jmouse.template.node;

import org.jmouse.template.rendering.AbstractRenderableNode;
import org.jmouse.el.node.ExpressionNode;

public class PrintNode extends AbstractRenderableNode {

    private final ExpressionNode expression;

    public PrintNode(ExpressionNode expression) {
        this.expression = expression;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

}
