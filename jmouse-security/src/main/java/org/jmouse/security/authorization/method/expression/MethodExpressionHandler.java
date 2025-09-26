package org.jmouse.security.authorization.method.expression;

import org.jmouse.el.evaluation.EvaluationContext;

public interface MethodExpressionHandler<T> extends ExpressionHandler<T> {

    void bindReturnValue(Object result, EvaluationContext context);

}
