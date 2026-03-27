package org.jmouse.beans.resolve.resolver;

import jakarta.inject.Provider;
import org.jmouse.beans.resolve.support.AbstractBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.util.Strings;

import java.util.List;
import java.util.Optional;

/**
 * Resolves a single primary bean for a given type. ⭐
 *
 * <p>Applies only when no explicit name is provided and the type is not a wrapper/collection.</p>
 */
public class PrimaryBeanResolver extends AbstractBeanResolver {

    /**
     * Checks whether a primary candidate exists for the requested type.
     */
    @Override
    public boolean supports(BeanResolutionRequest request) {
        if (Strings.isNotEmpty(request.beanName())) {
            return false;
        }

        InferredType type = request.beanType();

        if (type.is(Optional.class)
                || type.isCollection()
                || type.is(Provider.class)
                || type.isMap()
                || type.isArray()
        ) {
            return false;
        }

        List<BeanCandidate> candidates = getCandidateProvider(request).getCandidates(request.classType());

        return BeanCandidate.primary(candidates, request.classType()) != null;
    }

    /**
     * Returns the primary bean instance if present.
     */
    @Override
    public Object resolve(BeanResolutionRequest request) {
        List<BeanCandidate> candidates = getCandidateProvider(request).getCandidates(request.classType());
        BeanCandidate       candidate  = BeanCandidate.primary(candidates, request.classType());
        return candidate != null ? candidate.bean() : null;
    }
}