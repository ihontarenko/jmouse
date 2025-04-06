package org.jmouse.el.core.node.expression;

import org.jmouse.el.core.evaluation.EvaluationContext;

public class ArrayNode extends ArgumentsNode {

    @Override
    public Object evaluate(EvaluationContext context) {
        return super.evaluate(context);
    }

    @Override
    public String toString() {
        return "ARRAY[%s]".formatted(children());
    }
}
