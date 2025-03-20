package org.jmouse.el.node.expression;

import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;

public class FilterNode extends AbstractExpressionNode {

    private       ExpressionNode left;
    private final String         name;
    private       ExpressionNode   arguments;

    public FilterNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ExpressionNode getArguments() {
        return arguments;
    }

    public void setArguments(ExpressionNode arguments) {
        this.arguments = arguments;
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public void setLeft(ExpressionNode left) {
        this.left = left;
    }

    @Override
    public String toString() {
        return "%s(%s)".formatted(name, left);
    }
}
