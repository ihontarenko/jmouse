package svit.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field or method is a dependency that should be injected by the container.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Dependency {

    /**
     * An optional name used to qualify the dependency.
     * <p>If left blank, the container will attempt to resolve the dependency by type.</p>
     *
     * @return the name of the dependency
     */
    String value() default "";
}
