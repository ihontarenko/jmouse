package org.jmouse.mvc.mapping.annnotation;

import org.jmouse.core.reflection.annotation.MapTo;

public @interface ViewMapping {

    @MapTo(attribute = "name")
    String value();

    @MapTo(attribute = "value")
    String name() default "";

}
