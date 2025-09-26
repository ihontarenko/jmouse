package org.jmouse.security.authorization.method;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.security.core.Authentication;

/**
 * 🧠 Evaluates an authorization expression for a method invocation.
 */
public interface ExpressionHandler<T> {

    ExpressionLanguage getExpressionLanguage();

    EvaluationContext createContext(
            Authentication authentication, T toUse);

    boolean evaluate(Expression expression, EvaluationContext context);

}
