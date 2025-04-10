package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParameterSetNode extends AbstractExpressionNode {

    public List<ParameterNode> getSet() {
        return getChildren(ParameterNode.class);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        List<Object> compiled = new ArrayList<>();

        for (ParameterNode parameter : getSet()) {
            compiled.add(parameter.evaluate(context));
        }

        return compiled;
    }

    @Override
    public String toString() {
        return "PARAMETERS: " + getSet();
    }
}
