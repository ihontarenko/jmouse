package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.support.AbstractBeanResolver;
import org.jmouse.core.Priority;

import java.util.List;

/**
 * Resolves a bean by requested type. 🧩
 *
 * <p>Used for plain single-bean lookup when no qualifier, name, or wrapper type is involved.</p>
 */
@Priority(Integer.MIN_VALUE + 9000)
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