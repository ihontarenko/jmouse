package org.jmouse.core.annotation.support;

import org.jmouse.core.annotation.TypeAnnotationProcessor;

import java.lang.annotation.Annotation;

/**
 * Base support class for {@link TypeAnnotationProcessor} implementations. 🏷️
 *
 * <p>
 * Extends {@link AbstractAnnotationProcessor} and provides a convenient
 * foundation for processors that handle type-level annotations.
 * </p>
 *
 * @param <A> annotation type
 */
public abstract class AbstractTypeAnnotationProcessor<A extends Annotation>
        extends AbstractAnnotationProcessor<A>
        implements TypeAnnotationProcessor<A> {

    /**
     * Creates processor for the given annotation type.
     *
     * @param annotationType supported annotation type
     */
    protected AbstractTypeAnnotationProcessor(Class<A> annotationType) {
        super(annotationType);
    }
}