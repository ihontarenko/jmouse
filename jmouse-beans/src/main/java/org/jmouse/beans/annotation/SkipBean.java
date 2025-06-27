package org.jmouse.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Ignore(reason = "\uD83D\uDE45\uD83C\uDFFB\u200D♂\uFE0F you shall not pass!")
public @interface SkipBean {

}
