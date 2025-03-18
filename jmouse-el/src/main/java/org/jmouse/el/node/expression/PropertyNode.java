package org.jmouse.el.node.expression;

import org.jmouse.core.bind.PropertyPath;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;

public class PropertyNode extends AbstractExpressionNode {

    private final String path;

    public PropertyNode(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        context.setValue(path, path);

        PropertyPath propertyPath = PropertyPath.forPath(path);

        propertyPath.entries().skip(1);
        propertyPath.entries().limit(2);
        propertyPath.entries().slice(0, 3);

        return context.getValue(getPath());
    }

    @Override
    public String toString() {
        return "PROPERTY_PATH: '%s'".formatted(path);
    }
}
