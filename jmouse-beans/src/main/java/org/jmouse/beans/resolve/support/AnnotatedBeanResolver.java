package org.jmouse.beans.resolve.support;

import org.jmouse.beans.resolve.BeanResolutionRequest;

import java.lang.annotation.Annotation;

/**
 * Base resolver for annotation-driven bean resolution. 🏷
 *
 * @param <A> annotation type
 */
public abstract class AnnotatedBeanResolver<A extends Annotation> extends AbstractBeanResolver {

    private final Class<A> annotationType;

    protected AnnotatedBeanResolver(Class<A> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public boolean supports(BeanResolutionRequest request) {
        return request.hasAnnotation(annotationType);
    }

    @Override
    public Object resolve(BeanResolutionRequest request) {
        return resolve(request, request.getAnnotation(annotationType));
    }

    protected abstract Object resolve(BeanResolutionRequest context, A annotation);

}