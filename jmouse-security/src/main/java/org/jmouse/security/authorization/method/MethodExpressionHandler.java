package org.jmouse.security.authorization.method;

import org.jmouse.el.evaluation.EvaluationContext;

public interface MethodExpressionHandler<T> extends ExpressionHandler<T> {

    String VARIABLE_ARGUMENTS    = "arguments";
    String VARIABLE_RETURN_VALUE = "returnValue";

    default void bindReturnValue(Object result, EvaluationContext context) {
        context.setValue(VARIABLE_RETURN_VALUE, result);
    }

}
