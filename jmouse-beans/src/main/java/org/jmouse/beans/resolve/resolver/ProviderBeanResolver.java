package org.jmouse.beans.resolve.resolver;

import jakarta.inject.Provider;
import org.jmouse.beans.ObjectFactory;
import org.jmouse.beans.resolve.support.AbstractDelegatingBeanResolver;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolutionStrategy;
import org.jmouse.core.reflection.InferredType;

import java.util.function.Supplier;

/**
 * Resolves lazy provider-style dependencies. 🏭
 *
 * <p>Supports {@link Provider}, {@link Supplier}, and {@link ObjectFactory}.</p>
 */
public class ProviderBeanResolver extends AbstractDelegatingBeanResolver {

    /**
     * Creates resolver with nested resolution strategy.
     *
     * @param strategy resolution strategy used for inner type lookup
     */
    public ProviderBeanResolver(BeanResolutionStrategy strategy) {
        super(strategy);
    }

    /**
     * Checks whether the requested type is a supported provider wrapper.
     *
     * @param request resolution request
     * @return {@code true} if supported
     */
    @Override
    public boolean supports(BeanResolutionRequest request) {
        InferredType type = request.beanType();
        return type.is(Provider.class) || type.is(Supplier.class) || type.is(ObjectFactory.class);
    }

    /**
     * Wraps nested resolution into a lazy provider object.
     *
     * @param request resolution request
     * @return provider-like wrapper for the nested bean
     */
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

        if (ObjectFactory.class == outerType) {
            return (ObjectFactory<?>) () -> resolveNested(request, innerType);
        }

        throw new IllegalStateException("Unsupported provider type '%s'".formatted(outerType.getName()));
    }

    /**
     * Resolves the wrapped dependency on demand.
     *
     * @param request   parent resolution request
     * @param innerType nested dependency type
     * @return resolved bean instance
     */
    protected Object resolveNested(BeanResolutionRequest request, InferredType innerType) {
        return getDelegate().resolve(BeanResolutionRequest.forType(request.context(), innerType, true));
    }
}