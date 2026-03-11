package org.jmouse.core.annotation.support;

import org.jmouse.core.annotation.MethodAnnotationProcessor;

import java.lang.annotation.Annotation;

/**
 * Base support class for {@link MethodAnnotationProcessor} implementations. 🔧
 *
 * <p>
 * Extends {@link AbstractAnnotationProcessor} and provides a convenient
 * foundation for processors that handle method-level annotations.
 * </p>
 *
 * @param <A> annotation type
 */
public abstract class AbstractMethodAnnotationProcessor<A extends Annotation>
        extends AbstractAnnotationProcessor<A>
        implements MethodAnnotationProcessor<A> {

    /**
     * Creates processor for the given annotation type.
     *
     * @param annotationType supported annotation type
     */
    protected AbstractMethodAnnotationProcessor(Class<A> annotationType) {
        super(annotationType);
    }
}