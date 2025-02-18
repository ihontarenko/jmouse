
package org.jmouse.beans.annotation;

import org.jmouse.beans.BeanScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class or method as a structured provider for dependency injection.
 * <p>
 * This annotation can be applied to classes or methods to indicate that the annotated
 * element should be managed by the structured container. It provides descriptor about the
 * structured's name, lifecycle scope, and whether the structured should be proxied.
 * </p>
 *
 * <p>Key Features:</p>
 * <ul>
 *     <li>Specifies a custom structured name using {@code value()}.</li>
 *     <li>Defines the {@link BeanScope} for the structured (e.g., SINGLETON, PROTOTYPE).</li>
 *     <li>Indicates whether the structured should be proxied using {@code proxied()}.</li>
 * </ul>
 *
 * <p>Example Usage:</p>
 * <pre>{@code
 * @Provide(value = "userService", scope = BeanScope.SINGLETON, proxied = true)
 * public class UserService {
 *     // PropertyDescriptorAccessor of the service
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
     * Specifies the name of the structured.
     * <p>
     * If left empty, the container will generate a default name based on the class or method name.
     * </p>
     *
     * @return the name of the structured.
     */
    String value() default "";

    /**
     * Defines the lifecycle scope of the structured.
     * <p>
     * The default value is {@link BeanScope#SINGLETON}, meaning the container will manage
     * a single shared instance of the structured.
     * </p>
     *
     * @return the scope of the structured.
     */
    BeanScope scope() default BeanScope.SINGLETON;

    /**
     * Indicates whether the structured should be proxied.
     * <p>
     * Proxied beans are wrapped with a proxy that can intercept method calls, allowing for
     * additional behavior like lazy initialization, transaction management, or logging.
     * </p>
     *
     * @return {@code true} if the structured should be proxied, {@code false} otherwise.
     */
    boolean proxied() default false;

}