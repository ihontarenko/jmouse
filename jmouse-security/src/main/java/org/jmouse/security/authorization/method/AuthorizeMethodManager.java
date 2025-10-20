package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.access.Phase;

public class AuthorizeMethodManager implements AuthorizationManager<MethodInvocation> {

    private AuthorizeExpressionAttributeRegistry attributeRegistry = new AuthorizeExpressionAttributeRegistry(
            new SecurityMethodExpressionHandler()
    );

    @Override
    public AccessResult check(Authentication authentication, MethodInvocation target) {
        ExpressionHandler<MethodInvocation> expressionHandler = getAttributeRegistry().getExpressionHandler();
        ExpressionAttribute                 attribute         = getAttributeRegistry().getAttribute(target);
        EvaluationContext                   evaluationContext = expressionHandler.createContext(authentication, target);
        Phase phase = Phase.BEFORE;

        if (attribute instanceof AuthorizedExpressionAttribute expressionAttribute) {
            phase = expressionAttribute.phase();
        }

        attribute.expression().evaluate(evaluationContext);

        return null;
    }

    private boolean isCompatiblePhase(Phase phase, MethodInvocation invocation) {
        return phase == Phase.BEFORE || phase == Phase.AFTER;
    }

    public AuthorizeExpressionAttributeRegistry getAttributeRegistry() {
        return attributeRegistry;
    }

    public void setAttributeRegistry(AuthorizeExpressionAttributeRegistry attributeRegistry) {
        this.attributeRegistry = attributeRegistry;
    }
}
