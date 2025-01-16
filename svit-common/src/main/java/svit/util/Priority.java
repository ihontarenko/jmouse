package svit.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

/**
 * Indicates a priority value for beans or bean-producing methods.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Priority {

    /**
     * The priority value for the annotated type or method.
     * A lower value indicates higher priority.
     *
     * @return the priority value.
     */
    int value();
}
