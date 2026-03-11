package org.jmouse.core.annotation.support;

import org.jmouse.core.Verify;
import org.jmouse.core.annotation.AnnotationProcessor;

import java.lang.annotation.Annotation;

/**
 * Base implementation of {@link AnnotationProcessor}. 🧩
 *
 * <p>
 * Stores the supported annotation type and provides a common
 * foundation for concrete processor implementations.
 * </p>
 *
 * @param <A> annotation type
 */
public abstract class AbstractAnnotationProcessor<A extends Annotation> implements AnnotationProcessor<A> {

    private final Class<A> annotationType;

    /**
     * Creates processor for the given annotation type.
     *
     * @param annotationType supported annotation type
     */
    protected AbstractAnnotationProcessor(Class<A> annotationType) {
        this.annotationType = Verify.nonNull(annotationType, "annotationType");
    }

    /**
     * Returns supported annotation type.
     */
    @Override
    public Class<A> annotationType() {
        return annotationType;
    }
}