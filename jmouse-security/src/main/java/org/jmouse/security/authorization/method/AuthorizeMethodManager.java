package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.authorization.method.attribute.CompositeAnnotationExpressionAttributeRegistry;
import org.jmouse.security.authorization.method.attribute.ExpressionAttributeRegistry;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.access.Phase;

public class AuthorizeMethodManager implements AuthorizationManager<AuthorizedMethodInvocation> {

    private ExpressionAttributeRegistry<ExpressionAttribute> attributeRegistry = CompositeAnnotationExpressionAttributeRegistry.defaultRegistry(
            new SecurityMethodExpressionHandler()
    );

    @Override
    public AccessResult check(Authentication authentication, AuthorizedMethodInvocation target) {
        ExpressionHandler<MethodInvocation> expressionHandler = getExpressionHandler();
        ExpressionAttribute                 attribute         = getExpressionAttribute(target);
        Phase                               phase             = Phase.BEFORE;
        EvaluationContext                   evaluationContext = expressionHandler.createContext(
                authentication, target.invocation());

        if (attribute instanceof AuthorizedExpressionAttribute expressionAttribute) {
            phase = expressionAttribute.phase();
        }

        if (isApplicablePhase(phase, target) && !attribute.isDummy()) {
            return SecurityExpressionEvaluator.evaluate(attribute.expression(), evaluationContext);
        }

        return AccessResult.PERMIT;
    }

    private ExpressionHandler<MethodInvocation> getExpressionHandler() {
        return getAttributeRegistry().getExpressionHandler();
    }

    private ExpressionAttribute getExpressionAttribute(AuthorizedMethodInvocation authorizedMethodInvocation) {
        return getAttributeRegistry().getAttribute(authorizedMethodInvocation.invocation());
    }

    private boolean isApplicablePhase(Phase phase, AuthorizedMethodInvocation invocation) {
        return phase == invocation.phase();
    }

    public ExpressionAttributeRegistry<ExpressionAttribute> getAttributeRegistry() {
        return attributeRegistry;
    }

    public void setAttributeRegistry(ExpressionAttributeRegistry<ExpressionAttribute> attributeRegistry) {
        this.attributeRegistry = attributeRegistry;
    }
}
