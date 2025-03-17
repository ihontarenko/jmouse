package org.jmouse.el.node.expression;

import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.Node;

public class FilterNode extends AbstractExpressionNode {

    private final String name;
    private       Node   left;
    private       Node   arguments;

    public FilterNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Node getArguments() {
        return arguments;
    }

    public void setArguments(Node arguments) {
        this.arguments = arguments;
    }

    public Node getLeft() {
        return left;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    @Override
    public String toString() {
        return "%s(%s)".formatted(name, left);
    }
}
