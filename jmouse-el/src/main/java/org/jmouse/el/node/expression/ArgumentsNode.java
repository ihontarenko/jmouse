package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;

import java.util.ArrayList;
import java.util.List;

public class ArgumentsNode extends AbstractExpressionNode {

    @Override
    public Object evaluate(EvaluationContext context) {
        List<Object> compiled = new ArrayList<>();

        for (Node child : children()) {
            if (child instanceof ExpressionNode expression) {
                compiled.add(expression.evaluate(context));
            }
        }

        return compiled.toArray();
    }

}
