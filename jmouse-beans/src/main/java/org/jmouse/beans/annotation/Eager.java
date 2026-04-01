package org.jmouse.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a bean for eager initialization. ⚡
 *
 * <p>Beans annotated with {@code @Eager} are created during context startup
 * instead of being lazily initialized on first access.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Eager {
}