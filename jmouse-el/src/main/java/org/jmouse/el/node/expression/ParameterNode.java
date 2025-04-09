package org.jmouse.el.node.expression;

import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;

public class ParameterNode extends AbstractExpressionNode {

    private String         name;
    private ExpressionNode defaultValue;

    public void setName(String name) {
        this.name = name;
    }

    public void setDefaultValue(ExpressionNode defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public ExpressionNode getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return name + (defaultValue != null ? " : " + defaultValue : "");
    }
}
