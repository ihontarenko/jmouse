package org.jmouse.context.proxy.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Intercept {

    /**
     * Lower = higher priority (exec earlier).
     */
    int order() default 0;

    /**
     * Reference to named pointcut (from @ProxyPointcut(name=...)).
     */
    String pointcut() default "";
}