package org.jmouse.el.node.expression;

import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.Node;

public class FunctionNode extends AbstractExpressionNode {

    private final String name;
    private       Node   arguments;

    public FunctionNode(String name) {
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

    @Override
    public String toString() {
        return "%s(%s)".formatted(name, arguments == null ? "" : "ARGUMENTS[%d]".formatted(arguments.children().size()));
    }
}
