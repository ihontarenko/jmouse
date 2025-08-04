package org.jmouse.core.reflection.annotation;

import java.lang.annotation.*;

/**
 * 🔗 Maps an attribute to a target annotation and attribute.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapTo {

    /**
     * The target annotation type to map to.
     */
    Class<? extends Annotation> annotation() default Annotation.class;

    /**
     * The name of the target annotation attribute.
     */
    String attribute() default "";

}
