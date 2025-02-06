package org.jmouse.core.proxy.annotation;

import org.jmouse.core.proxy.AnnotationProxyFactory;
import org.jmouse.core.proxy.MethodInterceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a method interceptor for specific target classes.
 * <p>
 * Classes annotated with {@code @ProxyMethodInterceptor} should implement the {@link MethodInterceptor}
 * interface. These interceptors can be automatically discovered and applied to proxies
 * created by {@link AnnotationProxyFactory}.
 * </p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @ProxyMethodInterceptor({UserUservice.class})
 * public class LoggingInterceptor implements MethodInterceptor {
 *     @Override
 *     public Object invoke(MethodInvocation invocation) throws Throwable {
 *         System.out.println("Before method: " + invocation.getMethod().getName());
 *         Object result = invocation.proceed();
 *         System.out.println("After method: " + invocation.getMethod().getName());
 *         return result;
 *     }
 * }
 *
 * }</pre>
 *
 * @see MethodInterceptor
 * @see AnnotationProxyFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ProxyMethodInterceptor {

    /**
     * The target classes for which this interceptor should be applied.
     *
     * @return an array of target class types.
     */
    Class<?>[] value();
}