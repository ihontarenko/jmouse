package org.jmouse.security.authorization.method.expression;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.security.core.Authentication;

import java.lang.reflect.Method;

/**
 * 🧠 Evaluates an authorization expression for a method invocation.
 */
public interface MethodExpressionHandler {

    ExpressionLanguage getExpressionLanguage();

    EvaluationContext createContext(
            Authentication authentication, Method method, Object[] arguments, Object methodResult);

    boolean evaluate(String expression, EvaluationContext context);

}
