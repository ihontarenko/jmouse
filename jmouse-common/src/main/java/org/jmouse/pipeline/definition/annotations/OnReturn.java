package org.jmouse.pipeline.definition.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(OnReturns.class)
public @interface OnReturn {
    String code();
    String link();
}
