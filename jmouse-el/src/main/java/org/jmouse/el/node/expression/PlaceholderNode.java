package org.jmouse.el.node.expression;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

public class PlaceholderNode extends AbstractExpression {

    private Expression property;

    public Expression getProperty() {
        return property;
    }

    public void setProperty(Expression property) {
        this.property = property;
    }

    @Override
    public String toString() {
        return toSource();
    }

    @Override
    public String toSource() {
        return "${%s}".formatted(getProperty().toSource());
    }
}
