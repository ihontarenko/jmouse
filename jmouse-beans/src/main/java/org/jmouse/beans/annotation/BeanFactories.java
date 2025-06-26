package org.jmouse.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🏭 Marks a class that provides one or more bean factories.
 * <p>
 * Optional name may be used to categorize or identify the factory group.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanFactories {

    /**
     * 🏷 Optional group name for the factory.
     *
     * @return group identifier (default: empty)
     */
    String name() default "";
}
