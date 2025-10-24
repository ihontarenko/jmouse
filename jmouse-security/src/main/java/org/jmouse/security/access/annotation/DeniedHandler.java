package org.jmouse.security.access.annotation;

import org.jmouse.security.authorization.method.MethodAuthorizationDeniedHandler;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeniedHandler {

    Class<? extends MethodAuthorizationDeniedHandler> value();

}
