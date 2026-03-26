package org.jmouse.web.binding;

import org.jmouse.core.reflection.annotation.MapTo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebModel {


    @MapTo(attribute = "name")
    String value();

    @MapTo(attribute = "value")
    String prefix() default "";

}
