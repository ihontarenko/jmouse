package org.jmouse.security.authorization.method.attribute;

import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.authorization.method.MethodExpressionHandler;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🧮 Caches and resolves expression-based authorization attributes for methods.
 *
 * <p>Subclasses implement {@link #resolveAttribute(Method, Class)} to extract a concrete
 * {@link ExpressionAttribute} (e.g., from annotations). Results are memoized per (method, targetClass).</p>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>🔧 On initialization, lazily obtains a {@link MethodExpressionHandler} from the context if absent.</li>
 *   <li>🧠 Thread-safe lookups via an internal {@link ConcurrentHashMap} cache.</li>
 * </ul>
 *
 * @param <T> concrete attribute type resolved by this registry
 */
public abstract class AbstractExpressionAttributeRegistry<T extends ExpressionAttribute>
        implements ExpressionAttributeRegistry<T>, InitializingBeanSupport<ApplicationBeanContext> {

    private final Map<Key, T>                               cache = new ConcurrentHashMap<>();
    private       MethodExpressionHandler<MethodInvocation> expressionHandler;

    /**
     * 🏗️ Create a registry with a preconfigured handler (optional).
     *
     * @param expressionHandler handler used to parse/prepare method-level expressions;
     *                          if {@code null}, it will be retrieved from the context on init
     */
    protected AbstractExpressionAttributeRegistry(MethodExpressionHandler<MethodInvocation> expressionHandler) {
        this.expressionHandler = expressionHandler;
    }

    /**
     * 🧭 Choose the effective class for resolution.
     *
     * <p>If {@code targetClass} is non-null, returns it; otherwise returns the declaring class of {@code method}.</p>
     *
     * @param method      the method being inspected
     * @param targetClass optional concrete target class (can be {@code null})
     * @return the effective class to use for attribute resolution
     */
    protected static Class<?> getClass(Method method, Class<?> targetClass) {
        return (targetClass != null) ? targetClass : method.getDeclaringClass();
    }

    /**
     * ⚡ Get (or compute and cache) the attribute for the given method and target class.
     *
     * @param method      the method
     * @param type the concrete target class (may influence annotation resolution)
     * @return resolved attribute (may be {@code null} if none found)
     */
    @Override
    public final T getAttribute(Method method, Class<?> type) {
        return cache.computeIfAbsent(new Key(method, type), k -> resolveAttribute(method, type));
    }

    /**
     * ⚡ Get (or compute and cache) the attribute using the method's declaring class as target.
     *
     * @param method the method
     * @return resolved attribute (may be {@code null} if none found)
     */
    @Override
    public final T getAttribute(Method method) {
        return getAttribute(method, getClass(method, null));
    }

    /**
     * ⚡ Get (or compute and cache) the attribute for a {@link MethodInvocation}.
     *
     * <p>Uses {@code invocation.getTarget().getClass()} as the effective target class.</p>
     *
     * @param invocation the current invocation
     * @return resolved attribute (may be {@code null} if none found)
     */
    @Override
    public final T getAttribute(MethodInvocation invocation) {
        Class<?> type = getClass(invocation.getMethod(), invocation.getTarget().getClass());
        return getAttribute(invocation.getMethod(), type);
    }

    /**
     * 🗣️ Access the configured {@link MethodExpressionHandler}.
     *
     * @return the expression handler (never {@code null} after initialization)
     */
    @Override
    public MethodExpressionHandler<MethodInvocation> getExpressionHandler() {
        return expressionHandler;
    }

    /**
     * 🚀 Initialize the registry, resolving missing dependencies from the context.
     *
     * <p>If {@link #expressionHandler} is {@code null}, obtains a {@link MethodExpressionHandler}
     * bean from the provided {@link ApplicationBeanContext}.</p>
     *
     * @param context application bean context
     */
    @Override
    public void doInitialize(ApplicationBeanContext context) {
        if (expressionHandler == null) {
            @SuppressWarnings("unchecked")
            MethodExpressionHandler<MethodInvocation> bean =
                    (MethodExpressionHandler<MethodInvocation>) context.getBean(MethodExpressionHandler.class);
            expressionHandler = bean;
        }
    }

    /**
     * 🔐 Immutable composite key for cache lookups.
     */
    private record Key(Method method, Class<?> target) { }
}
