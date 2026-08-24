package org.jmouse.query.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The name a method argument answers to inside the query.
 *
 * <pre>{@code
 * @JmQuery(source = "deliveries", value = "where delivery[carrier] == carrier")
 * List<Delivery> by(@Parameter("carrier") String carrier);
 * }</pre>
 *
 * <h2>⚠️ Named rather than positional, and the annotation is not optional</h2>
 *
 * <p>Java keeps parameter names in the class file only when it was compiled with {@code -parameters},
 * which is a build flag a library cannot rely on: without it the arguments are {@code arg0},
 * {@code arg1}, and a query naming {@code carrier} would silently match nothing. Requiring the name here
 * costs one annotation and cannot be got wrong invisibly.</p>
 *
 * <p>⚠️ It also decouples the two: renaming a Java parameter is a refactor, and renaming a value in the
 * query is an edit to a stored string. Tying them together would make one of those break the other.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Parameter {

    /** The name the query writes. */
    String value();
}
