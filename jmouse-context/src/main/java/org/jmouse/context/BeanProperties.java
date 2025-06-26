package org.jmouse.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🧩 Indicates that a bean should be configured using properties.
 * <p>
 * Usually used to bind external configuration to fields via {@code BeanPropertiesBeanPostProcessor}.
 * The {@code value} can specify a prefix or path for property binding.
 *
 * <pre>{@code
 * @BeanProperties("app.user.default")
 * public class UserDefaultProperties {
 *     private String name;
 *     private int age;
 *     // setters...
 * }
 * }</pre>
 *
 * @see org.jmouse.context.processor.BeanPropertiesBeanPostProcessor
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanProperties {

    /**
     * 🌿 Optional property prefix or key path.
     *
     * @return property namespace (e.g. "server.config")
     */
    String value() default "";
}
