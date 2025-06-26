package org.jmouse.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🚫 Suppress specified exceptions on the annotated class.
 * <p>
 * Allows declaring which exceptions should be suppressed.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SuppressException {

    /**
     * ⚠️ List of exception types to suppress.
     *
     * @return array of throwable classes
     */
    Class<? extends Throwable>[] value() default {};

}
