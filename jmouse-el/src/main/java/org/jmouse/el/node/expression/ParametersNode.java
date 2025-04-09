package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParametersNode extends AbstractExpressionNode {

    private final List<ParameterNode> parameters;

    public ParametersNode() {
        parameters = new ArrayList<>();
    }

    public void addParameter(ParameterNode parameter) {
        parameters.add(parameter);
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        List<Object> compiled = new ArrayList<>();

        for (ParameterNode parameter : getParameters()) {
            compiled.add(parameter.evaluate(context));
        }

        return compiled;
    }

    @Override
    public String toString() {
        return Arrays.toString(parameters.toArray());
    }
}
