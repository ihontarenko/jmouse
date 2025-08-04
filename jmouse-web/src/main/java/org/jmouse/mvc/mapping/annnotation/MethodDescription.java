package org.jmouse.mvc.mapping.annnotation;

import org.jmouse.core.reflection.annotation.MapTo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodDescription {

    @MapTo(attribute = "value")
    String name() default "";

    @MapTo(attribute = "name")
    String value() default "";

}
