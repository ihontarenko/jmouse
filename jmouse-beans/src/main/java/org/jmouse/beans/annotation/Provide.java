
package org.jmouse.beans.annotation;

import org.jmouse.beans.BeanScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class or method as a bean provider for dependency injection.
 * <p>
 * This annotation can be applied to classes or methods to indicate that the annotated
 * element should be managed by the bean container. It provides descriptor about the
 * bean's name, lifecycle scope, and whether the bean should be proxied.
 * </p>
 *
 * <p>Key Features:</p>
 * <ul>
 *     <li>Specifies a custom bean name using {@code value()}.</li>
 *     <li>Defines the {@link BeanScope} for the bean (e.g., SINGLETON, PROTOTYPE).</li>
 *     <li>Indicates whether the bean should be proxied using {@code proxied()}.</li>
 * </ul>
 *
 * <p>Example Usage:</p>
 * <pre>{@code
 * @Provide(value = "userService", scope = BeanScope.SINGLETON, proxied = true)
 * public class UserService {
 *     // Implementation of the service
 * }
 *
 * @Provide(scope = BeanScope.PROTOTYPE)
 * public UserRepository userRepository() {
 *     return new UserRepository();
 * }
 * }</pre>
 *
 * @see BeanScope
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Provide {

    /**
     * Specifies the name of the bean.
     * <p>
     * If left empty, the container will generate a default name based on the class or method name.
     * </p>
     *
     * @return the name of the bean.
     */
    String value() default "";

    /**
     * Defines the lifecycle scope of the bean.
     * <p>
     * The default value is {@link BeanScope#SINGLETON}, meaning the container will manage
     * a single shared instance of the bean.
     * </p>
     *
     * @return the scope of the bean.
     */
    BeanScope scope() default BeanScope.SINGLETON;

    /**
     * Indicates whether the bean should be proxied.
     * <p>
     * Proxied beans are wrapped with a proxy that can intercept method calls, allowing for
     * additional behavior like lazy initialization, transaction management, or logging.
     * </p>
     *
     * @return {@code true} if the bean should be proxied, {@code false} otherwise.
     */
    boolean proxied() default false;

}