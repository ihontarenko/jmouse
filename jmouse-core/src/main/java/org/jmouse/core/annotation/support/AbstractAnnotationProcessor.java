package org.jmouse.core.annotation.support;

import org.jmouse.core.Verify;
import org.jmouse.core.annotation.AnnotationProcessor;

import java.lang.annotation.Annotation;

/**
 * Base support class for annotation processors. 🧩
 *
 * @param <A> annotation type
 */
public abstract class AbstractAnnotationProcessor<A extends Annotation> implements AnnotationProcessor<A> {

    private final Class<A> annotationType;

    protected AbstractAnnotationProcessor(Class<A> annotationType) {
        this.annotationType = Verify.nonNull(annotationType, "annotationType");
    }

    @Override
    public Class<A> annotationType() {
        return annotationType;
    }
}