package org.jmouse.core.proxy;

import java.lang.reflect.Method;

/**
 * 🛣️ Abstraction for dispatching method calls from a proxy.
 *
 * <p>Acts as the bridge between a generated proxy class and the
 * framework’s interceptor / invocation pipeline.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>⚡ Receives raw reflective call information.</li>
 *   <li>🪝 Delegates into the {@link MethodInterceptor} chain
 *       or directly to the target.</li>
 *   <li>🚨 Propagates any exception thrown during invocation.</li>
 * </ul>
 */
public interface InvocationDispatcher {

    /**
     * 🔁 Dispatch a method invocation from a proxy.
     *
     * @param proxy     proxy instance on which the call was made
     * @param method    reflective method being invoked
     * @param arguments arguments passed to the call (may be {@code null})
     * @return result of the invocation
     * @throws Throwable if the target or interceptor chain throws
     */
    Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable;
}
