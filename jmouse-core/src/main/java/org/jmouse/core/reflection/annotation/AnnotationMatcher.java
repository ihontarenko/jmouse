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
     * ✅ Matches annotations of the given type or its meta-types.
     *
     * @param annotationType target annotation class
     * @return matcher that checks for assignability from {@code annotationType}
     */
    public static Matcher<Annotation> isAnnotation(final Class<? extends Annotation> annotationType) {
        return a -> ClassMatchers.isSupertype(annotationType).matches(a.getClass());
    }
}
