package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.annotation.Lazy;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolutionStrategy;
import org.jmouse.beans.resolve.support.AbstractDelegatingBeanResolver;

import java.lang.reflect.AnnotatedElement;
import java.util.function.Supplier;

/**
 * Resolves {@link Lazy}-annotated dependencies as deferred suppliers. ⏳
 *
 * <p>Without proxy generation this resolver supports lazy access only
 * through wrapper types such as {@link Supplier}.</p>
 */
public class LazyBeanResolver extends AbstractDelegatingBeanResolver {

    public LazyBeanResolver(BeanResolutionStrategy strategy) {
        super(strategy);
    }

    @Override
    public boolean supports(BeanResolutionRequest request) {
        AnnotatedElement source = request.annotatedElement();

        if (source == null || !source.isAnnotationPresent(Lazy.class)) {
            return false;
        }

        return request.beanType().is(Supplier.class);
    }

    @Override
    public Object resolve(BeanResolutionRequest request) {
        return (Supplier<?>) () -> strategy().resolve(
                BeanResolutionRequest.forType(
                        request.beanContext(), request.beanType(), true)
        );
    }

}