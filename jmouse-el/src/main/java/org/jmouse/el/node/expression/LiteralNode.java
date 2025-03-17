package org.jmouse.el.node.expression;

import org.jmouse.el.node.AbstractExpressionNode;

abstract public class LiteralNode<T> extends AbstractExpressionNode {

    private final T value;

    public LiteralNode(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value == null ? "NULL" : value.toString();
    }

}
