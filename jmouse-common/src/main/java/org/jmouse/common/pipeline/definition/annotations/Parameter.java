package org.jmouse.common.pipeline.definition.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Parameters.class)
public @interface Parameter {
    String name();
    String value();
    String resolver() default "";
    String converter() default "";
}
