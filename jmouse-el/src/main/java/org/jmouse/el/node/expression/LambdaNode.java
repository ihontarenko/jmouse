package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Lambda;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;

public class LambdaNode extends AbstractExpressionNode {

    private ParameterSetNode parameters;
    private ExpressionNode   body;

    public ParameterSetNode getParameters() {
        return parameters;
    }

    public void setParameters(ParameterSetNode parameters) {
        this.parameters = parameters;
    }

    public ExpressionNode getBody() {
        return body;
    }

    public void setBody(ExpressionNode body) {
        this.body = body;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Lambda lambda = new Lambda(body);

        if (parameters != null) {
            for (ParameterNode parameter : parameters.getSet()) {
                String name         = parameter.getName();
                Object defaultValue = null;

                if (parameter.getDefaultValue() != null) {
                    defaultValue = parameter.getDefaultValue().evaluate(context);
                }

                lambda.addParameter(name);
                lambda.setDefault(name, defaultValue);
            }
        }

        return lambda;
    }

    @Override
    public String toString() {
        return "LAMBDA_NODE%s".formatted(parameters == null ? "" : ": " + parameters);
    }
}
