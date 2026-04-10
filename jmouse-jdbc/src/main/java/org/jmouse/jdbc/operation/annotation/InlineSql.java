package org.jmouse.jdbc.operation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares inline SQL text for a typed SQL operation.
 *
 * <p>This annotation is intended for operations whose SQL is short, stable,
 * and convenient to keep directly on the operation type.</p>
 *
 * @author Ivan Hontarenko
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InlineSql {

    /**
     * Returns inline SQL text.
     *
     * @return inline SQL text
     */
    String value();

}