package org.jmouse.template.node;

public class LiteralNode extends AbstractExpressionNode {

    private final Object value;

    public LiteralNode(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
