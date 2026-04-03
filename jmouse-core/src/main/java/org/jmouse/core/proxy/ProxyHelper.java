package org.jmouse.core.proxy;

import org.jmouse.core.Exceptions;
import org.jmouse.core.WrappedException;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 🛠️ Utility methods for working with proxies.
 *
 * <p>Supports detection and unified invocation across multiple engines:
 * JDK dynamic proxies, CGLIB, and ByteBuddy.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>🔎 Type checks: {@link #isProxy(Object)}, {@link #isJdkProxy(Object)}, etc.</li>
 *   <li>⚡ Unified invocation via {@link #invoke(Object, Method, Object[])}.</li>
 *   <li>🧩 Integrates with {@link InterceptableProxy} (ByteBuddy fast-path).</li>
 * </ul>
 */
public final class ProxyHelper {

    private ProxyHelper() {}

    /**
     * 🔎 Check if candidate is a proxy of any supported kind.
     */
    public static boolean isProxy(Object candidate) {
        return candidate != null && isProxy(candidate.getClass());
    }

    /**
     * 🔎 Check if type is a proxy class (JDK, CGLIB, ByteBuddy).
     */
    public static boolean isProxy(Class<?> type) {
        return (isJdkProxy(type) || isCglibProxy(type) || isByteBuddyProxy(type));
    }

    /**
     * 🔎 Check if candidate is a JDK dynamic proxy.
     */
    public static boolean isJdkProxy(Object candidate) {
        return candidate != null && isJdkProxy(candidate.getClass());
    }

    /**
     * 🔎 Check if type is a JDK dynamic proxy class.
     */
    public static boolean isJdkProxy(Class<?> type) {
        return type != null && Proxy.isProxyClass(type);
    }

    /**
     * 🔎 Check if candidate is a CGLIB proxy.
     */
    public static boolean isCglibProxy(Object candidate) {
        return candidate != null && isCglibProxy(candidate.getClass());
    }

    /**
     * 🔎 Check if type is a CGLIB proxy class (heuristic: name contains {@code $$EnhancerByCGLIB$$} or {@code $$}).
     */
    public static boolean isCglibProxy(Class<?> type) {
        if (type == null) return false;
        String name = type.getName();
        return name.contains("$$EnhancerByCGLIB$$") || name.contains("$$");
    }

    /**
     * 🔎 Check if candidate is a ByteBuddy proxy.
     */
    public static boolean isByteBuddyProxy(Object candidate) {
        return candidate != null && isByteBuddyProxy(candidate.getClass());
    }

    /**
     * 🔎 Check if type is a ByteBuddy proxy class (heuristic: name contains {@code ByteBuddy} or {@code $$ByteBuddy$$}).
     */
    public static boolean isByteBuddyProxy(Class<?> type) {
        if (type == null) {
            return false;
        }

        String typeName = type.getName();

        return typeName.contains("ByteBuddy") || typeName.contains("$" + ByteBuddyProxyEngine.BYTE_BUDDY_SUFFIX + "$");
    }

    /**
     * ⚡ Unified reflective invocation on a proxy.
     *
     * <p>Delegates to:</p>
     * <ul>
     *   <li>🟦 JDK → {@link Proxy#getInvocationHandler(Object)}.</li>
     *   <li>🟪 ByteBuddy → {@link InterceptableProxy#internalInvoke(Method, Object[])}.</li>
     *   <li>🟩 Fallback → {@link Reflections#invokeMethod(Object, Method, Object[])}.</li>
     * </ul>
     *
     * @param proxy     proxy instance (JDK / ByteBuddy / target)
     * @param method    method to call
     * @param arguments arguments to pass
     * @return return value from invocation
     */
    public static Object invoke(Object proxy, Method method, Object[] arguments) {
        Class<?> runtime = proxy.getClass();

        try {
            if (isJdkProxy(runtime)) {
                return Proxy.getInvocationHandler(proxy).invoke(proxy, method, arguments);
            } else if (isByteBuddyProxy(proxy) && proxy instanceof InterceptableProxy interceptable) {
                return interceptable.internalInvoke(method, arguments);
            }
        } catch (Throwable exception) {
            throw new WrappedException(Exceptions.unwrapIf(
                    exception, InvocationTargetException.class
            ));
        }

        return Reflections.invokeMethod(proxy, method, arguments);
    }
}
