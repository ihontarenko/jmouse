package org.jmouse.security.authorization.method;

import org.jmouse.el.evaluation.EvaluationContext;

public interface MethodExpressionHandler<T> extends ExpressionHandler<T> {

    default void bindReturnValue(Object result, EvaluationContext context) {
        context.setValue("returnValue", result);
    }

}
