package org.jmouse.beans.resolve.support;

import org.jmouse.beans.resolve.BeanResolutionRequest;

import java.lang.annotation.Annotation;

/**
 * Base resolver for annotation-driven bean resolution. 🏷️
 *
 * <p>Delegates resolution to subclasses using the extracted annotation instance.</p>
 *
 * @param <A> annotation type
 */
public abstract class AnnotatedBeanResolver<A extends Annotation> extends AbstractBeanResolver {

    private final Class<A> annotationType;

    /**
     * Creates resolver for the given annotation type.
     *
     * @param annotationType annotation class
     */
    protected AnnotatedBeanResolver(Class<A> annotationType) {
        this.annotationType = annotationType;
    }

    /**
     * Checks whether the request contains the target annotation.
     *
     * @param request resolution request
     * @return {@code true} if annotation is present
     */
    @Override
    public boolean supports(BeanResolutionRequest request) {
        return request.has(annotationType);
    }

    /**
     * Resolves dependency using the annotation instance.
     *
     * @param request resolution request
     * @return resolved value
     */
    @Override
    public Object resolve(BeanResolutionRequest request) {
        return resolve(request, request.get(annotationType));
    }

    /**
     * Template method for annotation-specific resolution.
     *
     * @param context    resolution request
     * @param annotation annotation instance
     * @return resolved value
     */
    protected abstract Object resolve(BeanResolutionRequest context, A annotation);

}