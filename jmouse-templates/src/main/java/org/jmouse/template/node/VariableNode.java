package org.jmouse.template.node;

public class VariableNode extends AbstractExpressionNode {

    private final String name;

    public VariableNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
