package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Lambda;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

public class LambdaNode extends AbstractExpression {

    private ParameterSetNode parameters;
    private Expression       body;

    public ParameterSetNode getParameters() {
        return parameters;
    }

    public void setParameters(ParameterSetNode parameters) {
        this.parameters = parameters;
    }

    public Expression getBody() {
        return body;
    }

    public void setBody(Expression body) {
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
