package org.jmouse.core.proxy;

import java.lang.reflect.Method;

/**
 * 🎯 Marker interface for ByteBuddy-generated proxies
 * that can dispatch directly through the interceptor chain.
 *
 * <p>Provides an internal entry point used by the proxy
 * infrastructure to bypass standard reflection and route
 * invocations to the configured {@link org.jmouse.core.proxy.MethodInterceptor}s.</p>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>⚡ Method name is intentionally unusual ({@code internalInvoke$ByteBuddy})
 *       to avoid clashing with user-defined methods.</li>
 *   <li>🧩 Called by framework internals — not meant for direct user code.</li>
 * </ul>
 */
public interface InterceptableProxy {

    /**
     * ⚡ Internal dispatch entry point for a proxied method.
     *
     * @param method    reflective method being invoked
     * @param arguments resolved arguments
     * @return return value from the interceptor chain or target method
     * @throws Exception if invocation fails or an interceptor rethrows
     */
    Object internalInvoke(Method method, Object[] arguments) throws Exception;
}
