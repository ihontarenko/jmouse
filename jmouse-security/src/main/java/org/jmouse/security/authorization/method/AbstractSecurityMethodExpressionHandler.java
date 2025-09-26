package org.jmouse.security.authorization.method;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.security.core.Authentication;

abstract public class AbstractSecurityMethodExpressionHandler<T> implements ExpressionHandler<T> {

    private final ExpressionLanguage language;

    protected AbstractSecurityMethodExpressionHandler(ExpressionLanguage language) {
        this.language = language;
    }

    @Override
    public ExpressionLanguage getExpressionLanguage() {
        return language;
    }

    @Override
    public EvaluationContext createContext(Authentication authentication, T invocation) {
        EvaluationContext evaluationContext = getExpressionLanguage().newContext();
        complementEvaluationContext(authentication, invocation, evaluationContext);
        return evaluationContext;
    }

    @Override
    public boolean evaluate(Expression expression, EvaluationContext context) {
        return false;
    }

    abstract protected void complementEvaluationContext(
            Authentication authentication, T invocation, EvaluationContext context);

}
