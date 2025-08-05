package org.jmouse.mvc.mapping.annnotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodDescription {

    Logger LOGGER = LoggerFactory.getLogger(MethodDescription.class);

    String value() default "";

}
