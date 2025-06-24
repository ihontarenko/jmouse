package org.jmouse.core.reflection.annotation;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ClassMatchers;

import java.lang.annotation.Annotation;

/**
 * 🎯 Matchers for annotation types.
 * Helps define reusable conditions for filtering annotations.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public class AnnotationMatcher {

    /**
     * ✅ Match by annotation class.
     *
     * @param annotationType target annotation type
     * @return matcher that checks for assignability
     */
    public static Matcher<Annotation> isAnnotation(Class<? extends Annotation> annotationType) {
        return a -> ClassMatchers.isSupertype(annotationType).matches(a.getClass());
    }

    /**
     * ✅ Match by {@link AnnotationData}'s annotation type.
     *
     * @param annotationData wrapper containing the annotation
     * @return matcher that checks for assignability
     */
    public static Matcher<Annotation> isAnnotation(AnnotationData annotationData) {
        return isAnnotation(annotationData.annotationType());
    }
}
