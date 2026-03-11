package org.jmouse.core.annotation.support;

import org.jmouse.core.annotation.FieldAnnotationProcessor;

import java.lang.annotation.Annotation;

/**
 * Base support class for {@link FieldAnnotationProcessor} implementations. 🧷
 *
 * <p>
 * Extends {@link AbstractAnnotationProcessor} and provides a convenient
 * foundation for processors that handle field-level annotations.
 * </p>
 *
 * @param <A> annotation type
 */
public abstract class AbstractFieldAnnotationProcessor<A extends Annotation>
        extends AbstractAnnotationProcessor<A>
        implements FieldAnnotationProcessor<A> {

    /**
     * Creates processor for the given annotation type.
     *
     * @param annotationType supported annotation type
     */
    protected AbstractFieldAnnotationProcessor(Class<A> annotationType) {
        super(annotationType);
    }
}