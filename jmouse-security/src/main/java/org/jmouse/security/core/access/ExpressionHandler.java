package org.jmouse.security.core.access;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.security.core.Authentication;

/**
 * 🧠 Evaluates an authorization expression for a method proxyInvocation.
 */
public interface ExpressionHandler<T> {

    ExpressionLanguage getExpressionLanguage();

    EvaluationContext createContext(
            Authentication authentication, T target);

    boolean evaluate(Expression expression, EvaluationContext context);

}
