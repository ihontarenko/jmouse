package org.jmouse.el.renderable.node;

import org.jmouse.el.core.node.AbstractNode;
import org.jmouse.el.core.node.ExpressionNode;

public class PrintNode extends AbstractNode {

    private final ExpressionNode expression;

    public PrintNode(ExpressionNode expression) {
        this.expression = expression;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

}
