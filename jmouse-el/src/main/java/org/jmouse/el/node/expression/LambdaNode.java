package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
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
        return super.evaluate(context);
    }

    @Override
    public String toString() {
        return "LAMBDA: %s".formatted(parameters);
    }
}
