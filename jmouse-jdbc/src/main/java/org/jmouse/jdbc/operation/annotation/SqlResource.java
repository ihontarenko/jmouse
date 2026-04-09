package org.jmouse.jdbc.operation.annotation;

import java.lang.annotation.*;

/**
 * Declares a classpath SQL resource for a typed SQL operation.
 *
 * @author Ivan Hontarenko
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqlResource {

    /**
     * Classpath location of SQL text.
     *
     * @return SQL resource location
     */
    String value();

}