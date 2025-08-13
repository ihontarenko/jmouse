package org.jmouse.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🧩 Binds a URI path variable to a method parameter.
 *
 * <p>Used in controller methods to extract values from route segments like
 * <code>/user/{id}</code>.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PathVariable {

    /**
     * 🔑 The name of the path variable to bind to.
     */
    String value();

    /**
     * ❗ Whether this variable is required (default: false).
     */
    boolean required() default false;
}
