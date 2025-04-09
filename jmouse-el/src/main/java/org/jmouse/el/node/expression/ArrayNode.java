package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Visitor;

public class ArrayNode extends ArgumentsNode {

    @Override
    public Object evaluate(EvaluationContext context) {
        return super.evaluate(context);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "ARRAY: %s".formatted(getChildren());
    }
}
