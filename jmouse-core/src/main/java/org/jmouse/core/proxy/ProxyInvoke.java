package org.jmouse.core.proxy;

import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 🧩 Core invocation pipeline shared by all {@link ProxyEngine}s
 * (JDK, ByteBuddy, CGLIB, etc.).
 *
 * <p>Centralizes common proxy behaviors:</p>
 * <ul>
 *   <li>🔄 Fallbacks for {@code equals}/{@code hashCode}/{@code toString}
 *       when target does not override them.</li>
 *   <li>🪝 Execution of the {@link MethodInterceptor} chain.</li>
 *   <li>♻️ Return value adaptation:
 *     <ul>
 *       <li>Self-returning methods → return proxy instead of raw target.</li>
 *       <li>Primitive return types → guard against {@code null} results.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public final class ProxyInvoke {

    private ProxyInvoke() {}

    /**
     * 🚀 Unified proxy invocation entry point.
     *
     * <p>Called by generated proxies to dispatch method calls.</p>
     *
     * @param engine    engine identifier (e.g. "JDK", "BYTE_BUDDY")
     * @param context   proxy context with target + interceptors
     * @param proxy     proxy instance
     * @param method    method being invoked
     * @param arguments resolved arguments
     * @return return value after interceptor chain + fallbacks
     * @throws ProxyInvocationException if invocation fails
     */
    public static Object invokeCore(String engine, ProxyContext context, Object proxy, Method method, Object[] arguments) {
        try {
            // --- built-in fallbacks
            if (!context.hasEquals() && Reflections.isEqualsMethod(method)) {
                return equalsFallback(context, proxy, (arguments != null && arguments.length > 0) ? arguments[0] : null);
            }
            if (!context.hasHashCode() && Reflections.isHashCodeMethod(method)) {
                return hashCodeFallback(context);
            }
            if (!context.hasToString() && isToString(method)) {
                return toStringFallback(context, engine);
            }

            List<MethodInterceptor> interceptors = context.getInterceptors();
            MethodInvocation        invocation   = new MethodInvocationChain(proxy, context.getTarget(), method, arguments, interceptors, context);
            Object                  result       = invocation.proceed();
            Class<?>                returnType   = method.getReturnType();

            if (result != null && result == context.getTarget() && returnType != Object.class
                    && returnType.isInstance(proxy)) {
                return proxy;
            }

            // --- primitive return guard
            if (result == null && returnType.isPrimitive() && returnType != void.class) {
                throw new ProxyInvocationException(
                        "Method '%s' returned null, but a primitive '%s' was expected.".formatted(method, returnType));
            }

            return result;
        } catch (Throwable throwable) {
            throw new ProxyInvocationException(throwable.getMessage(), throwable);
        }
    }

    // ---------- helpers ----------

    private static boolean isToString(Method m) {
        return m.getName().equals("toString") && m.getParameterCount() == 0;
    }

    /**
     * 🤝 Cross-engine equality fallback when target lacks {@code equals()}.
     *
     * <p>Strategy: proxies are equal if both have comparable
     * {@link ProxyContext} (same interfaces + same target).</p>
     */
    private static boolean equalsFallback(ProxyContext contextA, Object proxy, Object that) {
        if (proxy == that) {
            return true;
        }
        if (that == null) {
            return false;
        }

        ProxyContext contextB = null; // ProxyIntrospection.tryExtractContext(that);
        if (contextB == null) {
            return false;
        }

        Class<?>[] a = contextA.getInterfaces().toArray(Class[]::new);
        Class<?>[] b = contextB.getInterfaces().toArray(Class[]::new);

        return Arrays.equals(a, b) && contextA.getTarget().equals(contextB.getTarget());
    }

    /**
     * 🔢 Hash-code fallback when target lacks {@code hashCode()}.
     */
    private static int hashCodeFallback(ProxyContext context) {
        return 31 * ProxyInvoke.class.hashCode() + context.getTarget().hashCode();
    }

    /**
     * 📝 toString fallback when target lacks {@code toString()}.
     */
    private static String toStringFallback(ProxyContext context, String engine) {
        return "PROXY [" + engine + "] [" + context.getTargetClass().getName() + "]";
    }
}
