package org.jmouse.security.authorization.method.attribute;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.access.MethodExpressionHandler;

import java.lang.reflect.Method;

/**
 * 🧩 Contract for resolving {@link ExpressionAttribute}s that define
 * security metadata for method invocations.
 * <p>
 * This registry acts as a cache and resolution layer for extracting
 * expression-based authorization metadata (e.g. from annotations)
 * associated with methods or invocation contexts.
 * </p>
 *
 * <h3>Responsibilities ⚙️</h3>
 * <ul>
 *   <li>🔹 Lookup of precomputed attributes</li>
 *   <li>🔹 Lazy resolution for methods or target classes</li>
 *   <li>🔹 Exposure of the {@link MethodExpressionHandler} for evaluation</li>
 * </ul>
 *
 * @param <T> the concrete type of expression attribute (e.g. method-level rule)
 */
public interface ExpressionAttributeRegistry<T extends ExpressionAttribute> {

    /**
     * 🧠 Returns the {@link MethodExpressionHandler} responsible for
     * evaluating expressions tied to this registry’s attributes.
     *
     * @return the expression handler used for evaluation
     */
    MethodExpressionHandler<MethodInvocation> getExpressionHandler();

    /**
     * 🔍 Retrieves an attribute bound to a specific {@link Method}
     * and declaring {@link Class}.
     *
     * @param method the method to inspect
     * @param type   the declaring or target class
     * @return the resolved attribute, or {@code null} if none found
     */
    T getAttribute(Method method, Class<?> type);

    /**
     * 🔍 Retrieves an attribute by inspecting only the given {@link Method}.
     *
     * @param method the method to inspect
     * @return the resolved attribute, or {@code null} if none found
     */
    T getAttribute(Method method);

    /**
     * 🔍 Retrieves an attribute for the given {@link MethodInvocation}.
     *
     * @param invocation the method invocation context
     * @return the resolved attribute, or {@code null} if none found
     */
    T getAttribute(MethodInvocation invocation);

    /**
     * 🧩 Resolves (and possibly caches) the {@link ExpressionAttribute}
     * for a method and its target class.
     * <p>
     * Typically used internally to populate the cache or perform
     * annotation-based resolution.
     * </p>
     *
     * @param method       the method being invoked
     * @param targetClass  the target class declaring the method
     * @return the resolved {@link ExpressionAttribute}, or {@code null} if not applicable
     */
    T resolveAttribute(Method method, Class<?> targetClass);
}
