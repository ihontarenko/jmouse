package org.jmouse.beans.resolve;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

/**
 * Nullable annotation support utilities. 🌫
 */
public final class NullableSupport {

    private static final Set<String> NULLABLE_ANNOTATIONS = Set.of(
            "jakarta.annotation.Nullable",
            "javax.annotation.Nullable",
            "org.jetbrains.annotations.Nullable"
    );

    private NullableSupport() {
    }

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