package org.jmouse.beans.resolve.resolver;

import jakarta.inject.Provider;
import org.jmouse.beans.resolve.support.AbstractDelegatingBeanResolver;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolutionStrategy;
import org.jmouse.core.reflection.InferredType;

import java.util.function.Supplier;

public class ProviderBeanResolver extends AbstractDelegatingBeanResolver {

    public ProviderBeanResolver(BeanResolutionStrategy strategy) {
        super(strategy);
    }

    @Override
    public boolean supports(BeanResolutionRequest request) {
        return request.beanType().is(Provider.class);
    }

    @Override
    public Object resolve(BeanResolutionRequest request) {
        InferredType type      = request.beanType();
        Class<?>     outerType = type.getClassType();
        InferredType innerType = type.getFirst();

        if (Provider.class == outerType) {
            return (Provider<?>) () -> resolveNested(request, innerType);
        }

        if (Supplier.class == outerType) {
            return (Supplier<?>) () -> resolveNested(request, innerType);
        }

        throw new IllegalStateException("Unsupported provider type '%s'".formatted(outerType.getName()));
    }

    protected Object resolveNested(BeanResolutionRequest request, InferredType innerType) {
        return strategy().resolve(BeanResolutionRequest.forType(request.beanContext(), innerType, true));
    }
}