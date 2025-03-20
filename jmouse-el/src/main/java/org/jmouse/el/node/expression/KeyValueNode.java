package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;

import java.util.AbstractMap;

public class KeyValueNode extends AbstractExpressionNode {

    private ExpressionNode key;
    private ExpressionNode value;

    public ExpressionNode getValue() {
        return value;
    }

    public void setValue(ExpressionNode value) {
        this.value = value;
    }

    public ExpressionNode getKey() {
        return key;
    }

    public void setKey(ExpressionNode key) {
        this.key = key;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return new AbstractMap.SimpleEntry<>(key.evaluate(context), value.evaluate(context));
    }

    @Override
    public String toString() {
        return "{%s: %s}".formatted(key, value);
    }
}
