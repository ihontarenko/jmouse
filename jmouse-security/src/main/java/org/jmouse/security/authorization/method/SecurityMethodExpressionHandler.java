package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.security.Authentication;
import org.jmouse.security.access.MethodExpressionHandler;

public class SecurityMethodExpressionHandler extends AbstractSecurityMethodExpressionHandler<MethodInvocation>
        implements MethodExpressionHandler<MethodInvocation> {

    public SecurityMethodExpressionHandler(ExpressionLanguage language) {
        super(language);
    }

    public SecurityMethodExpressionHandler() {
        this(SecurityExpressionLanguage.getSharedInstance());
    }

    @Override
    protected void complementEvaluationContext(
            Authentication authentication, MethodInvocation invocation, EvaluationContext context) {
        context.setValue(VARIABLE_ARGUMENTS, invocation.getArguments());
        setReturnValue(invocation.getReturnValue(), context);
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
