package org.jmouse.security.authorization.method;

import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.core.reflection.annotation.Annotations;
import org.jmouse.el.node.Expression;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
        implements InitializingBeanSupport<ApplicationBeanContext> {

    private final Map<Key, T>                         cache = new ConcurrentHashMap<>();
    private       MethodExpressionHandler<Expression> expressionHandler;

    /**
     * 🏗️ Create a registry with a preconfigured handler (optional).
     *
     * @param expressionHandler handler used to parse/prepare method-level expressions;
     *                          if {@code null}, it will be retrieved from the context on init
     */
    protected AbstractExpressionAttributeRegistry(MethodExpressionHandler<Expression> expressionHandler) {
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
     * @param targetClass the concrete target class (may influence annotation resolution)
     * @return resolved attribute (may be {@code null} if none found)
     */
    public final T getAttribute(Method method, Class<?> targetClass) {
        return cache.computeIfAbsent(new Key(method, targetClass), k -> resolveAttribute(method, targetClass));
    }

    /**
     * ⚡ Get (or compute and cache) the attribute using the method's declaring class as target.
     *
     * @param method the method
     * @return resolved attribute (may be {@code null} if none found)
     */
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
    public final T getAttribute(MethodInvocation invocation) {
        return getAttribute(
                invocation.getMethod(),
                getClass(invocation.getMethod(), invocation.getTarget().getClass())
        );
    }

    /**
     * 🔎 Helper to obtain a finder for a single unique annotation of the given type.
     *
     * <p>The returned function searches an {@link AnnotatedElement} and returns the unique annotation
     * instance if present, or {@code null} otherwise.</p>
     *
     * @param type annotation type to locate
     * @param <A>  annotation generic
     * @return a function that finds a unique {@code A} on an element
     */
    protected <A extends Annotation> Function<AnnotatedElement, A> findUniqueAnnotation(Class<A> type) {
        return Annotations.lookup(type);
    }

    /**
     * 🧩 Strategy hook: resolve an attribute for the given method and target class.
     *
     * <p>Implementations typically inspect annotations (method/class hierarchy, meta-annotations, etc.)
     * and may build a pre-parsed/compiled form of the expression for fast evaluation.</p>
     *
     * @param method      method to inspect
     * @param targetClass effective target class
     * @return an attribute instance, or {@code null} if none applies
     */
    protected abstract T resolveAttribute(Method method, Class<?> targetClass);

    /**
     * 🗣️ Access the configured {@link MethodExpressionHandler}.
     *
     * @return the expression handler (never {@code null} after initialization)
     */
    protected MethodExpressionHandler<Expression> getExpressionHandler() {
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
            MethodExpressionHandler<Expression> bean =
                    (MethodExpressionHandler<Expression>) context.getBean(MethodExpressionHandler.class);
            expressionHandler = bean;
        }
    }

    /**
     * 🔐 Immutable composite key for cache lookups.
     */
    private record Key(Method method, Class<?> target) { }
}
