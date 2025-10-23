package org.jmouse.security.authorization.method;

import org.jmouse.core.InstanceResolver;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.core.reflection.annotation.Annotations;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.access.ExpressionHandler;
import org.jmouse.security.authorization.method.attribute.CompositeAnnotationExpressionAttributeRegistry;
import org.jmouse.security.authorization.method.attribute.ExpressionAttributeRegistry;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.access.MethodExpressionHandler;
import org.jmouse.security.core.access.Phase;
import org.jmouse.security.core.access.annotation.DeniedHandler;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Supplier;

public class AuthorizeMethodManager
        implements AuthorizationManager<MethodAuthorizationContext>, MethodAuthorizationDeniedHandler {

    private ExpressionAttributeRegistry<ExpressionAttribute<?>> attributeRegistry;
    private Supplier<MethodAuthorizationDeniedHandler>          defaultDeniedResolver = ThrowingMethodAuthorizationDeniedHandler::new;
    private InstanceResolver<MethodAuthorizationDeniedHandler>  deniedResolver        = (type) -> defaultDeniedResolver.get();

    public AuthorizeMethodManager(ExpressionAttributeRegistry<ExpressionAttribute<?>> attributeRegistry) {
        this.setAttributeRegistry(attributeRegistry);
    }

    public AuthorizeMethodManager() {
        this(CompositeAnnotationExpressionAttributeRegistry.defaultRegistry(
                new SecurityMethodExpressionHandler()
        ));
    }

    public void setDefaultDeniedResolver(Supplier<MethodAuthorizationDeniedHandler> defaultDeniedResolver) {
        this.defaultDeniedResolver = defaultDeniedResolver;
    }

    public void setDeniedResolver(InstanceResolver<MethodAuthorizationDeniedHandler> resolver) {
        this.deniedResolver = resolver;
    }

    @Override
    public AccessResult check(Authentication authentication, MethodAuthorizationContext target) {
        ExpressionHandler<MethodInvocation> expressionHandler = getExpressionHandler();
        ExpressionAttribute<?>              attribute         = getExpressionAttribute(target);
        Phase                               phase             = Phase.BEFORE;
        EvaluationContext                   evaluationContext = expressionHandler
                .createContext(authentication, target.proxyInvocation());

        if (target.isAfter()
                && expressionHandler instanceof MethodExpressionHandler<MethodInvocation> methodExpressionHandler) {
            methodExpressionHandler.setReturnValue(target.result(), evaluationContext);
        }

        if (attribute instanceof AnnotationExpressionAttribute<?> expressionAttribute) {
            phase = expressionAttribute.phase();
        }

        if (shouldAuthorizeMethod(phase, target) && !attribute.isDummy()) {
            return SecurityExpressionEvaluator.evaluate(attribute.expression(), evaluationContext);
        }

        return AccessResult.PERMIT;
    }

    private ExpressionHandler<MethodInvocation> getExpressionHandler() {
        return getAttributeRegistry().getExpressionHandler();
    }

    private ExpressionAttribute<?> getExpressionAttribute(MethodAuthorizationContext authorizationContext) {
        return getAttributeRegistry().getAttribute(authorizationContext.proxyInvocation());
    }

    private boolean shouldAuthorizeMethod(Phase phase, MethodAuthorizationContext invocation) {
        return phase == invocation.phase();
    }

    public ExpressionAttributeRegistry<ExpressionAttribute<?>> getAttributeRegistry() {
        return attributeRegistry;
    }

    public void setAttributeRegistry(ExpressionAttributeRegistry<ExpressionAttribute<?>> attributeRegistry) {
        this.attributeRegistry = attributeRegistry;
    }

    @Override
    public Object handleDeniedInvocation(MethodAuthorizationContext invocation, AccessResult decision) {
        Method                                    method = invocation.proxyInvocation().getMethod();
        Function<AnnotatedElement, DeniedHandler> lookup = Annotations.lookup(DeniedHandler.class);
        DeniedHandler                             denied = lookup.apply(method);

        if (denied == null) {
            denied = lookup.apply(method.getDeclaringClass());
        }

        Supplier<MethodAuthorizationDeniedHandler> deniedHandler = defaultDeniedResolver;

        if (denied != null) {
            Class<? extends MethodAuthorizationDeniedHandler> deniedType = denied.value();
            deniedHandler = () -> deniedResolver.resolve(deniedType);
        }

        return deniedHandler.get().handleDeniedInvocation(invocation, decision);
    }

}
