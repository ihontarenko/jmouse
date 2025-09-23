package org.jmouse.core.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * 🔍 Introspection contract for proxies.
 *
 * <p>Exposes a way to retrieve the {@link ProxyDefinition} associated
 * with a proxy object. Implemented by proxy handlers (e.g.,
 * {@link JdkInvocationHandler}) to allow framework code or user code
 * to access metadata about the proxy.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * MyService proxy = ProxyFactory.create(MyService.class);
 *
 * ProxyDefinition<?> def = ProxyIntrospection.tryExtractContext(proxy);
 * if (def != null) {
 *     System.out.println("Target class: " + def.targetClass());
 * }
 * }</pre>
 */
public interface ProxyIntrospection {

    /**
     * @return the proxy definition bound to this proxy
     */
    ProxyDefinition<?> getProxyDefinition();

    /**
     * Attempts to extract the {@link ProxyDefinition} from an arbitrary object.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>If the object itself implements {@code ProxyIntrospection}, return it directly</li>
     *   <li>If the object is a JDK dynamic proxy and its {@link InvocationHandler}
     *       implements {@code ProxyIntrospection}, delegate to it</li>
     *   <li>Otherwise return {@code null}</li>
     * </ol>
     *
     * @param object candidate proxy object (may be {@code null})
     * @return the extracted proxy definition, or {@code null} if not available
     */
    static ProxyDefinition<?> tryExtractContext(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof ProxyIntrospection introspection) {
            return introspection.getProxyDefinition();
        }

        if (Proxy.isProxyClass(object.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(object);
            if (invocationHandler instanceof ProxyIntrospection introspection) {
                return introspection.getProxyDefinition();
            }
        }

        return null;
    }
}
