package org.jmouse.core.annotation.support;

import org.jmouse.core.annotation.ParameterAnnotationProcessor;

import java.lang.annotation.Annotation;

/**
 * Base implementation for {@link ParameterAnnotationProcessor}. 🧩
 *
 * <p>Provides annotation type binding and common processor infrastructure.</p>
 *
 * @param <A> annotation type
 */
public abstract class AbstractParameterAnnotationProcessor<A extends Annotation>
        extends AbstractAnnotationProcessor<A>
        implements ParameterAnnotationProcessor<A> {

    /**
     * Creates processor for the given annotation type.
     *
     * @param annotationType annotation class
     */
    protected AbstractParameterAnnotationProcessor(Class<A> annotationType) {
        super(annotationType);
    }
}