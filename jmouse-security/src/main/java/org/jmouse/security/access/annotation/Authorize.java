package org.jmouse.security.access.annotation;

import org.jmouse.security.access.Phase;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Authorize {

    String value() default "";

    Phase phase() default Phase.BEFORE;

}
