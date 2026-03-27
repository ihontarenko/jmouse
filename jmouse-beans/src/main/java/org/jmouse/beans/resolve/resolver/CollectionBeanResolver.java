package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.resolve.support.AbstractBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.core.reflection.InferredType;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * {@link AbstractBeanResolver} implementation that resolves collection-based dependencies. 📚
 *
 * <p>
 * This resolver supports injection of {@link java.util.Collection}, {@link java.util.List},
 * and {@link java.util.Set} types by retrieving all matching {@link BeanCandidate candidates}
 * for the declared generic element type.
 * </p>
 */
public class CollectionBeanResolver extends AbstractBeanResolver {

    /**
     * Checks whether the requested dependency is a collection type. 🔎
     *
     * @param request the resolution request
     * @return {@code true} if the dependency is a collection
     */
    @Override
    public boolean supports(BeanResolutionRequest request) {
        return request.beanType().isCollection();
    }

    /**
     * Resolves all beans matching the collection's generic element type
     * and adapts them to the requested collection implementation. ⚙️
     *
     * @param request the resolution request
     * @return resolved collection of bean instances
     *
     * @throws IllegalStateException if the collection type is not supported
     */
    @Override
    public Object resolve(BeanResolutionRequest request) {
        InferredType        outerType  = request.beanType();
        InferredType        innerType  = outerType.getFirst();
        List<BeanCandidate> candidates = getCandidateProvider(request).getCandidates(innerType.getClassType());
        List<Object>        values     = candidates.stream().map(BeanCandidate::bean).toList();

        if (outerType.isSet()) {
            return new LinkedHashSet<>(values);
        }

        if (outerType.isList() || outerType.isCollection()) {
            return values;
        }

        throw new IllegalStateException(
                "Unsupported collection dependency type '%s'".formatted(outerType)
        );
    }
}