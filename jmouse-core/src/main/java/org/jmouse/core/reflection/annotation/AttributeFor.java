package org.jmouse.core.reflection.annotation;

import java.lang.annotation.*;

/**
 * 🔗 Maps an attribute to a target annotation and attribute.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AttributeFor {

    /**
     * The target annotation type to map to.
     */
    Class<? extends Annotation> annotation();

    /**
     * The name of the target annotation attribute.
     */
    String attribute();

}
