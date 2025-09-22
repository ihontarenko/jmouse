package org.jmouse.context.proxy.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ProxyPointcut {

    /**
     * Pointcut expression (EL/DSL).
     */
    String value();

    /**
     * Optional name to register/reuse this pointcut by reference.
     */
    String name() default "";
}
