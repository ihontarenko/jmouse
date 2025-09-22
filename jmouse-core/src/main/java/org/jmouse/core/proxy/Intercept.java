package org.jmouse.core.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🪝 Marks a class as a {@link MethodInterceptor} applicable to specific targets.
 *
 * <p>Used by {@link DefaultProxyFactory} (or similar) to auto-discover
 * and register interceptors for proxied beans.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @Intercept({UserService.class})
 * public class LoggingInterceptor implements MethodInterceptor {
 *     @Override
 *     public Object invoke(MethodInvocation invocation) throws Throwable {
 *         System.out.println("Before " + invocation.getMethod().getName());
 *         Object result = invocation.proceed();
 *         System.out.println("After " + invocation.getMethod().getName());
 *         return result;
 *     }
 * }
 * }</pre>
 *
 * <h3>Attributes</h3>
 * <ul>
 *   <li>🎯 {@link #value()} → array of target classes for which this interceptor applies.</li>
 *   <li>⚖️ {@link #priority()} → optional ordering hint; higher means applied earlier
 *       (default {@code -1} → no explicit priority).</li>
 * </ul>
 *
 * @see MethodInterceptor
 * @see DefaultProxyFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Intercept {

    /**
     * 🎯 Target classes for which this interceptor should be applied.
     *
     * @return array of class types
     */
    Class<?>[] value();

    /**
     * ⚖️ Priority hint for interceptor ordering.
     *
     * <p>Higher values indicate higher precedence.
     * Defaults to {@code -1}, meaning "unspecified".</p>
     *
     * @return priority value
     */
    int priority() default -1;
}
