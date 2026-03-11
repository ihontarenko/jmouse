package org.jmouse.core.annotation.support;

import org.jmouse.core.annotation.FieldAnnotationProcessor;

import java.lang.annotation.Annotation;

/**
 * Base support class for field annotation processors. 🧷
 *
 * @param <A> annotation type
 */
public abstract class AbstractFieldAnnotationProcessor<A extends Annotation>
        extends AbstractAnnotationProcessor<A>
        implements FieldAnnotationProcessor<A> {

    protected AbstractFieldAnnotationProcessor(Class<A> annotationType) {
        super(annotationType);
    }
}