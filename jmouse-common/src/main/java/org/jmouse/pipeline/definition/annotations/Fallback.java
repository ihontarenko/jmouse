package org.jmouse.pipeline.definition.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Fallback {
    String link();
}
