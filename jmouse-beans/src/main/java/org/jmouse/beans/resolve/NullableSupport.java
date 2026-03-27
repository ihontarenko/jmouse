package org.jmouse.beans.resolve;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

/**
 * Utilities for detecting nullable annotations. 🌫️
 *
 * <p>Used to determine whether a dependency is required or optional.</p>
 */
public final class NullableSupport {

    private static final Set<String> NULLABLE_ANNOTATIONS = Set.of(
            "jakarta.annotation.Nullable",
            "javax.annotation.Nullable",
            "org.jetbrains.annotations.Nullable"
    );

    private NullableSupport() {
    }

    /**
     * Returns whether the dependency should be treated as required.
     *
     * @param source annotated element (field, parameter, etc.)
     * @return {@code true} if required
     */
    public static boolean isRequired(AnnotatedElement source) {
        return !isNullable(source);
    }

    /**
     * Checks whether the given element is annotated as nullable.
     *
     * @param source annotated element
     * @return {@code true} if nullable annotation is present
     */
    public static boolean isNullable(AnnotatedElement source) {
        if (source == null) {
            return false;
        }

        for (Annotation annotation : source.getAnnotations()) {
            if (NULLABLE_ANNOTATIONS.contains(annotation.annotationType().getName())) {
                return true;
            }
        }

        return false;
    }

}