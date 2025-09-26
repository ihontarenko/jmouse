package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.security.core.Authentication;

public class SecurityMethodExpressionHandler
        extends AbstractSecurityMethodExpressionHandler<MethodInvocation>
        implements MethodExpressionHandler<MethodInvocation> {

    protected SecurityMethodExpressionHandler(ExpressionLanguage language) {
        super(language);
    }

    @Override
    protected void complementEvaluationContext(
            Authentication authentication, MethodInvocation invocation, EvaluationContext context) {
    }

    @Override
    public boolean evaluate(Expression expression, EvaluationContext context) {
        boolean result = false;

        if (expression.evaluate(context) instanceof Boolean bool) {
            result = bool;
        }

        return result;
    }
}
