package org.jmouse.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 📦 Imports other bean classes into the current context.
 * <p>
 * Can be used to register additional configuration classes or component sets.
 * <p>
 * Useful for modular setups, feature toggles, or shared infrastructure.
 *
 * <pre>{@code
 * @BeanImport({SecurityConfig.class, LoggingSupport.class})
 * public class ApplicationConfig { ... }
 * }</pre>
 *
 * 🔁 Multiple levels of imports are supported recursively.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanImport {

    /**
     * 👥 Classes to be imported into the current context.
     *
     * @return array of classes to import
     */
    Class<?>[] value() default {};
}
