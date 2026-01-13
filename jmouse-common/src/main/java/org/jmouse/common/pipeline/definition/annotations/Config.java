package org.jmouse.common.pipeline.definition.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Configs.class)
public @interface Config {
    String key();
    String value();
}
