package org.jmouse.core.annotation.support;

import org.jmouse.core.annotation.TypeAnnotationProcessor;

import java.lang.annotation.Annotation;

/**
 * Base support class for type annotation processors. 🏷️
 *
 * @param <A> annotation type
 */
public abstract class AbstractTypeAnnotationProcessor<A extends Annotation>
        extends AbstractAnnotationProcessor<A>
        implements TypeAnnotationProcessor<A> {

    protected AbstractTypeAnnotationProcessor(Class<A> annotationType) {
        super(annotationType);
    }
}