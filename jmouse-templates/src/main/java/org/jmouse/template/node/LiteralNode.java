package org.jmouse.template.node;

public class LiteralNode extends AbstractNode {

    private final Object value;

    public LiteralNode(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

}
