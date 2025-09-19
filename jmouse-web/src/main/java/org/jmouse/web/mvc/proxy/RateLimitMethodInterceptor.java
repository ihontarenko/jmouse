package org.jmouse.web.mvc.proxy;

import org.jmouse.core.proxy.ProxyContext;
import org.jmouse.core.throttle.*;
import org.jmouse.core.proxy.annotation.ProxyMethodInterceptor;
import org.jmouse.web.mvc.TooManyRequestsException;

import java.lang.reflect.Method;

/**
 * 🚦 Concrete interceptor binding {@link AbstractRateLimitMethodInterceptor}
 * to beans marked with {@link RateLimited}.
 *
 * <p>Activated by the {@link ProxyMethodInterceptor} annotation:
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
@ProxyMethodInterceptor({Object.class})
public final class RateLimitMethodInterceptor extends AbstractRateLimitMethodInterceptor {

    @Override
    public void before(ProxyContext context, Method method, Object[] arguments) {
        try {
            super.before(context, method, arguments);
        } catch (RateLimitExceededException exceededException) {
            throw new TooManyRequestsException(exceededException.getMessage());
        }
    }

}
