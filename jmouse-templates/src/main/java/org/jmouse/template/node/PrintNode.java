package org.jmouse.template.node;

public class PrintNode extends AbstractRenderableNode {

    private final ExpressionNode expression;

    public PrintNode(ExpressionNode expression) {
        this.expression = expression;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

}
