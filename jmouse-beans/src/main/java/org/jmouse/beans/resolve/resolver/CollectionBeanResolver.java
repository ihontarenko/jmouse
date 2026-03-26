package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.resolve.support.AbstractBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.core.reflection.InferredType;

import java.util.LinkedHashSet;
import java.util.List;

public class CollectionBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionRequest request) {
        return request.beanType().isCollection();
    }

    @Override
    public Object resolve(BeanResolutionRequest request) {
        InferredType        outerType  = request.beanType();
        InferredType        innerType  = outerType.getFirst();
        List<BeanCandidate> candidates = candidates(request).getCandidates(innerType.getClassType());
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