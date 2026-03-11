package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;

/**
 * Processes discovered annotation candidates. 🎯
 *
 * @param <A> annotation type
 */
public interface AnnotationProcessor<A extends Annotation> {

    /**
     * Returns supported annotation type.
     */
    Class<A> annotationType();

    /**
     * Processes discovered candidate.
     */
    void process(AnnotationCandidate<A> candidate, AnnotationProcessingContext context);
}