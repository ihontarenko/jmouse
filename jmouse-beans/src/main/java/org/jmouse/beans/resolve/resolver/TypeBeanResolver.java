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

/**
 * Resolves a bean by requested type. 🧩
 *
 * <p>Used for plain single-bean lookup when no qualifier, name, or wrapper type is involved.</p>
 */
public class TypeBeanResolver extends AbstractBeanResolver {

    /**
     * Checks whether plain type-based resolution can be applied.
     *
     * @param request resolution request
     * @return {@code true} if this resolver supports the request
     */
    @Override
    public boolean supports(BeanResolutionRequest request) {
        if (request.beanName() != null && !request.beanName().isBlank()) {
            return false;
        }

        InferredType type = request.beanType();

        if (request.has(Qualifier.class)
                || request.has(Named.class)
                || type.is(Optional.class)
                || type.isCollection()
                || type.is(Provider.class)
                || type.isMap()
                || type.isArray()
        ) {
            return false;
        }

        return !getCandidateProvider(request).getCandidates(request.classType()).isEmpty();
    }

    /**
     * Resolves a single bean candidate for the requested type.
     *
     * @param request resolution request
     * @return resolved bean or {@code null} if no single candidate is available
     */
    @Override
    public Object resolve(BeanResolutionRequest request) {
        List<BeanCandidate> candidates = getCandidateProvider(request).getCandidates(request.classType());
        BeanCandidate       candidate  = BeanCandidate.single(candidates, request.classType());
        return candidate != null ? candidate.bean() : null;
    }
}