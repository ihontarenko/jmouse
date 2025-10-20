package org.jmouse.security.core.access.annotation;

import org.jmouse.security.core.access.Phase;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Authorize {

    String value() default "";

    Phase phase() default Phase.BEFORE;

}
