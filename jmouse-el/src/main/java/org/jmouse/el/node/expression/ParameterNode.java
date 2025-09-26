package org.jmouse.el.node.expression;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

public class ParameterNode extends AbstractExpression {

    private String     name;
    private Expression defaultValue;

    public void setName(String name) {
        this.name = name;
    }

    public void setDefaultValue(Expression defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public Expression getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return name + (defaultValue != null ? " : " + defaultValue : "");
    }
}
