package org.jmouse.security.authorization.method.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.security.core.Authentication;

import java.lang.reflect.Method;

public class jMouseELMethodExpressionHandler implements MethodExpressionHandler {

    @Override
    public EvaluationContext createContext(
            Authentication authentication, Method method, Object[] arguments, Object methodResult) {
        return null;
    }

    @Override
    public boolean evaluate(String expression, EvaluationContext ctx) {
        return false;
    }

}
