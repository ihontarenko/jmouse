package org.jmouse.jdbc.operation.annotation;

import java.lang.annotation.*;

/**
 * Declares inline SQL text for a typed SQL operation.
 *
 * @author Ivan Hontarenko
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sql {

    /**
     * Inline SQL text.
     *
     * @return SQL text
     */
    String value();

}