package org.jmouse.web.annotation;

import java.lang.annotation.*;

/** Deserializes one concrete DTO/record using the registered HTTP message converters. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {
    boolean required() default true;

    long maxBytes() default 1024 * 1024;
}
