package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.Intercept;
import org.jmouse.core.proxy.InvocationContext;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.Authentication;
import org.jmouse.security.SecurityContextHolderStrategy;
import org.jmouse.security.access.Phase;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * 🛡️ Interceptor for enforcing method-level authorization logic.
 * <p>
 * Applied to all proxied objects ({@link Intercept}(Object.class)), this interceptor
 * delegates pre- and post-execution access control to a configured
 * {@link AuthorizationManager}, typically an instance of {@link AuthorizeMethodManager}.
 * </p>
 *
 * <h3>Lifecycle ⚙️</h3>
 * <ol>
 *   <li>🔹 {@link #authorizeBeforeExecution(MethodInvocation)} — checks access before method call</li>
 *   <li>⚙️ {@link MethodInvocation#proceed()} — invokes the original target method</li>
 *   <li>🔹 {@link #authorizeAfterExecution(MethodInvocation, Object)} — validates post-call access</li>
 * </ol>
 *
 * <h3>Security Context 🔐</h3>
 * Uses the current {@link SecurityContextHolderStrategy} from
 * {@link SecurityContextHolder} to obtain the active {@link Authentication}.
 * Throws an {@link IllegalStateException} if no authentication is available.
 */
@Intercept(Object.class)
public class AuthorizeMethodInterceptor extends AbstractAuthorizeMethodInterceptor {

    private final AuthorizationManager<MethodAuthorizationContext> manager;
    private final Supplier<SecurityContextHolderStrategy>          context;

    {
        context = SecurityContextHolder::getContextHolderStrategy;
    }

    /**
     * Creates an interceptor bound to a specific authorization manager.
     *
     * @param manager the {@link AuthorizationManager} responsible for evaluating method access
     */
    public AuthorizeMethodInterceptor(AuthorizationManager<MethodAuthorizationContext> manager) {
        this.manager = manager;
    }

    /**
     * 🧩 Invokes the target method while applying authorization checks
     * before and after execution.
     *
     * @param invocation the current method proxyInvocation
     * @return the method’s return value
     * @throws Throwable if the target method or authorization fails
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        authorizeBeforeExecution(invocation);
        Object result = invocation.proceed();
        authorizeAfterExecution(invocation, result);
        return result;
    }

    /**
     * 🔹 Performs pre-execution authorization (phase: {@link Phase#BEFORE}).
     */
    private void authorizeBeforeExecution(MethodInvocation invocation) {
        authorizeExecution(invocation, Phase.BEFORE, null);
    }

    /**
     * 🔹 Performs post-execution authorization (phase: {@link Phase#AFTER}).
     */
    private void authorizeAfterExecution(MethodInvocation invocation, Object result) {
        authorizeExecution(invocation, Phase.AFTER, result);
    }

    /**
     * 🚦 Centralized execution of authorization logic for both phases.
     *
     * @param invocation the intercepted method proxyInvocation
     * @param phase      current authorization phase (before/after)
     * @param result     optional result for post-phase checks
     */
    private void authorizeExecution(MethodInvocation invocation, Phase phase, Object result) {
        if (manager instanceof AuthorizeMethodManager authorizeMethodManager) {
            MethodAuthorizationContext methodInvocation = new MethodAuthorizationContext(invocation, phase, result);
            AccessResult               decision         = authorizeMethodManager
                    .check(getAuthentication(), methodInvocation);
            if (decision != null && !decision.isGranted()
                    && authorizeMethodManager instanceof MethodAuthorizationDeniedHandler deniedHandler) {
                deniedHandler.handleDeniedInvocation(methodInvocation, decision);
            }
        }
    }

    /**
     * 🔑 Retrieves the current {@link Authentication} from the
     * {@link SecurityContextHolder}.
     *
     * @return the active authentication
     * @throws IllegalStateException if no authentication exists
     */
    private Authentication getAuthentication() {
        SecurityContextHolderStrategy contextHolderStrategy = context.get();
        Authentication                authentication        = contextHolderStrategy.getContext().getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException("An authentication object is not present in the security context.");
        }

        return authentication;
    }

    @Override
    public boolean error(InvocationContext context, Method method, Object[] arguments, Throwable throwable) {
        // false - unhandled
        return false;
    }

}
