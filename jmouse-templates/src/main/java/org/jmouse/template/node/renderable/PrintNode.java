package org.jmouse.template.node.renderable;

import org.jmouse.template.node.AbstractRenderableNode;
import org.jmouse.template.node.ExpressionNode;

public class PrintNode extends AbstractRenderableNode {

    private final ExpressionNode expression;

    public PrintNode(ExpressionNode expression) {
        this.expression = expression;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

}
