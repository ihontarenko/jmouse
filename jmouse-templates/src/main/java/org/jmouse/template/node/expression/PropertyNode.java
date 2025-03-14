package org.jmouse.template.node.expression;

import org.jmouse.template.node.AbstractExpressionNode;

public class PropertyNode extends AbstractExpressionNode {

    private final String path;

    public PropertyNode(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "PROPERTY_PATH: '%s'".formatted(path);
    }
}
