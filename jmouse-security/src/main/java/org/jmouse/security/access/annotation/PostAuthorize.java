package org.jmouse.security.access.annotation;

import org.jmouse.core.reflection.annotation.MapTo;
import org.jmouse.security.access.Phase;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Authorize(phase = Phase.AFTER)
public @interface PostAuthorize {

    @MapTo(annotation = Authorize.class, attribute = "value")
    String value() default "";

}
