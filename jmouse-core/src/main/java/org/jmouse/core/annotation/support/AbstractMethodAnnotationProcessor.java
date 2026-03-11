package org.jmouse.core.annotation.support;

import org.jmouse.core.annotation.MethodAnnotationProcessor;

import java.lang.annotation.Annotation;

/**
 * Base support class for method annotation processors. 🔧
 *
 * @param <A> annotation type
 */
public abstract class AbstractMethodAnnotationProcessor<A extends Annotation>
        extends AbstractAnnotationProcessor<A>
        implements MethodAnnotationProcessor<A> {

    protected AbstractMethodAnnotationProcessor(Class<A> annotationType) {
        super(annotationType);
    }
}