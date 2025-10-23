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

/**
 * ⚖️ Central manager responsible for method-level authorization.
 *
 * <p>Delegates the decision process to registered {@link ExpressionAttributeRegistry}
 * and evaluates expressions through the configured {@link ExpressionHandler}.
 * Supports both <b>BEFORE</b> and <b>AFTER</b> authorization phases.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>🔍 Resolve {@link ExpressionAttribute} for a given {@link MethodInvocation}</li>
 *   <li>🧠 Build {@link EvaluationContext} for expression evaluation</li>
 *   <li>⏱️ Enforce phase-specific authorization checks</li>
 *   <li>🚫 Handle denied invocations through {@link DeniedHandler}</li>
 * </ul>
 *
 * <p>This class is typically used by {@code AuthorizeMethodInterceptor}
 * to decide whether a method invocation should proceed.</p>
 *
 * @see ExpressionAttributeRegistry
 * @see SecurityExpressionEvaluator
 * @see MethodAuthorizationDeniedHandler
 */
public class AuthorizeMethodManager
        implements AuthorizationManager<MethodAuthorizationContext>, MethodAuthorizationDeniedHandler {

    /**
     * 📚 Registry that resolves method-level {@link ExpressionAttribute}s.
     */
    private ExpressionAttributeRegistry<ExpressionAttribute<?>> attributeRegistry;

    /**
     * 🧩 Default fallback handler supplier for denied invocations.
     */
    private Supplier<MethodAuthorizationDeniedHandler> defaultDeniedResolver = ThrowingMethodAuthorizationDeniedHandler::new;

    /**
     * 🧠 Strategy to resolve custom {@link MethodAuthorizationDeniedHandler} instances.
     */
    private InstanceResolver<MethodAuthorizationDeniedHandler> deniedResolver = (type) -> defaultDeniedResolver.get();

    /**
     * Creates a manager using the specified attribute registry.
     *
     * @param attributeRegistry registry to use for attribute resolution
     */
    public AuthorizeMethodManager(ExpressionAttributeRegistry<ExpressionAttribute<?>> attributeRegistry) {
        this.setAttributeRegistry(attributeRegistry);
    }

    /**
     * Creates a manager using the default {@link CompositeAnnotationExpressionAttributeRegistry}
     * backed by a {@link SecurityMethodExpressionHandler}.
     */
    public AuthorizeMethodManager() {
        this(CompositeAnnotationExpressionAttributeRegistry.defaultRegistry(
                new SecurityMethodExpressionHandler()
        ));
    }

    /**
     * Sets the default resolver supplier for denied handlers.
     *
     * @param defaultDeniedResolver supplier producing fallback denied handler
     */
    public void setDefaultDeniedResolver(Supplier<MethodAuthorizationDeniedHandler> defaultDeniedResolver) {
        this.defaultDeniedResolver = defaultDeniedResolver;
    }

    /**
     * Sets the custom resolver for dynamic denied-handler instantiation.
     *
     * @param resolver instance resolver for denied handlers
     */
    public void setDeniedResolver(InstanceResolver<MethodAuthorizationDeniedHandler> resolver) {
        this.deniedResolver = resolver;
    }

    /**
     * ✅ Performs authorization on a given method invocation context.
     *
     * <p>Resolves the expression, prepares the evaluation context,
     * determines the phase, and executes the expression logic.
     * If no expression or dummy attribute is found, returns {@link AccessResult#PERMIT}.</p>
     *
     * @param authentication current authenticated principal
     * @param target         method authorization context
     * @return the evaluated access result
     */
    @Override
    public AccessResult check(Authentication authentication, MethodAuthorizationContext target) {
        ExpressionHandler<MethodInvocation> expressionHandler = getExpressionHandler();
        ExpressionAttribute<?>              attribute         = getExpressionAttribute(target);
        Phase                               phase             = Phase.BEFORE;
        EvaluationContext                   evaluationContext = expressionHandler
                .createContext(authentication, target.proxyInvocation());

        // 🧩 Populate return value for AFTER phase evaluation
        if (target.isAfter()
                && expressionHandler instanceof MethodExpressionHandler<MethodInvocation> methodExpressionHandler) {
            methodExpressionHandler.setReturnValue(target.result(), evaluationContext);
        }

        if (attribute instanceof AnnotationExpressionAttribute<?> expressionAttribute) {
            phase = expressionAttribute.phase();
        }

        // 🧮 Evaluate authorization expression
        if (shouldAuthorizeMethod(phase, target) && !attribute.isDummy()) {
            return SecurityExpressionEvaluator.evaluate(attribute.expression(), evaluationContext);
        }

        return AccessResult.PERMIT;
    }

    /**
     * @return current expression handler from the registry
     */
    private ExpressionHandler<MethodInvocation> getExpressionHandler() {
        return getAttributeRegistry().getExpressionHandler();
    }

    /**
     * Resolves the expression attribute for a given method authorization context.
     */
    private ExpressionAttribute<?> getExpressionAttribute(MethodAuthorizationContext authorizationContext) {
        return getAttributeRegistry().getAttribute(authorizationContext.proxyInvocation());
    }

    /**
     * Determines if authorization should occur based on the method phase.
     */
    private boolean shouldAuthorizeMethod(Phase phase, MethodAuthorizationContext invocation) {
        return phase == invocation.phase();
    }

    /**
     * @return current attribute registry
     */
    public ExpressionAttributeRegistry<ExpressionAttribute<?>> getAttributeRegistry() {
        return attributeRegistry;
    }

    /**
     * Sets the active expression attribute registry.
     */
    public void setAttributeRegistry(ExpressionAttributeRegistry<ExpressionAttribute<?>> attributeRegistry) {
        this.attributeRegistry = attributeRegistry;
    }

    /**
     * 🚫 Handles denied invocations by resolving the appropriate
     * {@link MethodAuthorizationDeniedHandler} based on {@link DeniedHandler} annotations.
     *
     * <p>If no annotation is found, falls back to {@link #defaultDeniedResolver}.</p>
     *
     * @param invocation method invocation context
     * @param decision   access result representing the denial
     * @return optional custom handler result
     */
    @Override
    public Object handleDeniedInvocation(MethodAuthorizationContext invocation, AccessResult decision) {
        Method                                    method = invocation.proxyInvocation().getMethod();
        Function<AnnotatedElement, DeniedHandler> lookup = Annotations.lookup(DeniedHandler.class);
        DeniedHandler                             denied = lookup.apply(method);

        if (denied == null) {
            denied = lookup.apply(invocation.proxyInvocation().getTarget().getClass());
        }

        Supplier<MethodAuthorizationDeniedHandler> deniedHandler = defaultDeniedResolver;

        if (denied != null) {
            Class<? extends MethodAuthorizationDeniedHandler> deniedType = denied.value();
            deniedHandler = () -> deniedResolver.resolve(deniedType);
        }

        return deniedHandler.get().handleDeniedInvocation(invocation, decision);
    }
}
