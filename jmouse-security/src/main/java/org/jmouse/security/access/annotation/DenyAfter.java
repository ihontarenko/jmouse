package org.jmouse.security.access.annotation;

import org.jmouse.core.reflection.annotation.MapTo;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PostAuthorize("default expr")
public @interface DenyAfter {

    @MapTo(annotation = PostAuthorize.class, attribute = "value")
    String value() default "";

    String message() default "Just for fun :)";

    @MapTo(annotation = PostAuthorize.class, attribute = "order")
    int number() default 123;

}