package svit.container.annotation;

import svit.container.BeanScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an annotated class or method provides a bean to the container.
 * <p>
 * Example usage:
 * <pre>{@code
 * @Provide("randomService")
 * public class IntRandomService {
 *
 * }
 *
 * @Provide(scope = BeanScope.PROTOTYPE)
 * public UserPrototypeBean createBean() {
 *     return new UserPrototypeBean();
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Provide {

    /**
     * The name to be assigned to the bean.
     * <p>If empty, a default name may be derived from the class or method.</p>
     *
     * @return the name of the bean
     */
    String value() default "";

    /**
     * The scope of the bean. Defaults to {@link BeanScope#SINGLETON}.
     *
     * @return the scope of the bean
     */
    BeanScope scope() default BeanScope.SINGLETON;
}
