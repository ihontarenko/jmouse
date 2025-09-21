package org.jmouse.core.proxy2.api;

import org.jmouse.core.matcher.Matcher;

import java.lang.reflect.Method;

import static org.jmouse.core.matcher.Matcher.not;
import static org.jmouse.core.reflection.MethodMatchers.*;

/**
 * 🎛️ Defines which {@link Method}s should be intercepted by a proxy.
 *
 * <p>Policies are applied by proxy engines before delegating a call
 * into the {@link org.jmouse.core.proxy.MethodInterceptor} chain.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>✅ Decide if a given method is eligible for interception.</li>
 *   <li>🚫 Exclude framework/boilerplate methods (synthetic, static, bridge, etc.).</li>
 *   <li>🧩 Allow customization for different scenarios (e.g. include default methods).</li>
 * </ul>
 */
public interface InterceptionPolicy {

    /**
     * 🔍 Determine whether the given method should be intercepted.
     *
     * @param method candidate method
     * @return {@code true} if it should go through the interceptor chain,
     *         {@code false} to invoke directly
     */
    boolean shouldIntercept(Method method);

    /**
     * 📦 Default interception policy.
     *
     * <p>Excludes:</p>
     * <ul>
     *   <li>🧪 Synthetic methods</li>
     *   <li>⚡ Static methods</li>
     *   <li>🌉 Bridge methods (compiler-generated for generics)</li>
     * </ul>
     *
     * <p>All other methods are eligible for interception.</p>
     *
     * @return a ready-to-use default policy
     */
    static InterceptionPolicy defaultPolicy() {
        Matcher<Method> common = asMethod(not(isSynthetic()))
                .and(asMethod(not(isStatic())))
                .and(not(isBridge()));
        return common::matches;
    }
}
