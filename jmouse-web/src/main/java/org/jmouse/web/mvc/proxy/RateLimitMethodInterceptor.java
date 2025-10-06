package org.jmouse.web.mvc.proxy;

import org.jmouse.core.proxy.InvocationContext;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.core.throttle.*;
import org.jmouse.core.proxy.Intercept;
import org.jmouse.web.mvc.TooManyRequestsException;

import java.lang.reflect.Method;

/**
 * 🚦 Concrete interceptor binding {@link AbstractRateLimitMethodInterceptor}
 * to beans marked with {@link RateLimited}.
 *
 * <p>Activated by the {@link Intercept} annotation:
 * any bean implementing {@code RateLimited} will have its methods
 * wrapped with TinyLFU/Token-Bucket style rate limiting logic.</p>
 *
 * <ul>
 *   <li>🪝 No extra code — inherits behavior from {@link AbstractRateLimitMethodInterceptor}.</li>
 *   <li>📌 Serves as the ready-to-use interceptor in the web layer.</li>
 *   <li>🔗 Scopes and limits are configured via {@link RateLimit} annotations.</li>
 * </ul>
 *
 * @see AbstractRateLimitMethodInterceptor
 * @see RateLimited
 * @see RateLimitEnable
 * @see RateLimit
 */
@Intercept({Object.class})
public final class RateLimitMethodInterceptor extends AbstractRateLimitMethodInterceptor {

    @Override
    public void before(InvocationContext context, Method method, Object[] arguments) {
        try {
            super.before(context, method, arguments);
        } catch (RateLimitExceededException exceededException) {
            throw new TooManyRequestsException(exceededException.getMessage());
        }
    }

    /**
     * The parser method that performs the actual interception logic. Implementations
     * generally call {@link MethodInvocation#proceed()} to invoke the target method.
     *
     * @param invocation the {@link MethodInvocation} encapsulating the target method and its parameters
     * @return the result of the method call
     * @throws Throwable if an error occurs during the method invocation
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return super.invoke(invocation);
    }

    @Override
    public void after(InvocationContext context, Method method, Object[] arguments, Object result) {
        MethodInvocation invocation = context.invocation();
        System.out.println("invocation.getReturnValue() : " + invocation.getReturnValue());
    }
}
