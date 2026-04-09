package org.jmouse.jdbc.operation.annotation;

import java.lang.annotation.*;

/**
 * Declares an explicit logical name for a typed SQL operation.
 *
 * @author Ivan Hontarenko
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqlName {

    /**
     * Logical operation name.
     *
     * @return logical operation name
     */
    String value();

}