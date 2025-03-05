package org.jmouse.template.node.expression;

import org.jmouse.template.node.AbstractExpressionNode;

public class FunctionNode extends AbstractExpressionNode {

    private final String name;

    public FunctionNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
