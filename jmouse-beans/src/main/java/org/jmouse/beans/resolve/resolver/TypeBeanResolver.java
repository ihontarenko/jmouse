package org.jmouse.beans.resolve.resolver;

import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.support.AbstractBeanResolver;
import org.jmouse.core.reflection.InferredType;

import java.util.List;
import java.util.Optional;

public class TypeBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionRequest request) {
        if (request.beanName() != null && !request.beanName().isBlank()) {
            return false;
        }

        InferredType type = request.beanType();

        if (request.hasAnnotation(Qualifier.class)
                || request.hasAnnotation(Named.class)
                || type.is(Optional.class)
                || type.isCollection()
                || type.is(Provider.class)
                || type.isMap()
                || type.isArray()
        ) {
            return false;
        }

        return !candidates(request).getCandidates(request.classType()).isEmpty();
    }

    @Override
    public Object resolve(BeanResolutionRequest request) {
        List<BeanCandidate> candidates = candidates(request).getCandidates(request.classType());
        BeanCandidate       candidate  = BeanCandidate.single(candidates, request.classType());
        return candidate != null ? candidate.bean() : null;
    }
}