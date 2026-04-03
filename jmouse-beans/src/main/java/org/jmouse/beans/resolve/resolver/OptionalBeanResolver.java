package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.resolve.support.AbstractBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.core.Priority;

import java.util.List;
import java.util.Optional;

/**
 * Resolves {@link Optional} dependencies. 🎯
 *
 * <p>Prefers a primary candidate, otherwise falls back to single-candidate resolution.</p>
 */
@Priority(Integer.MIN_VALUE + 1000)
public class OptionalBeanResolver extends AbstractBeanResolver {

    /**
     * Checks whether the requested dependency is an {@link Optional}.
     *
     * @param request resolution request
     * @return {@code true} if supported
     */
    @Override
    public boolean supports(BeanResolutionRequest request) {
        return request.beanType().is(Optional.class);
    }

    /**
     * Resolves the wrapped dependency and returns it as {@link Optional}.
     *
     * @param request resolution request
     * @return optional bean value
     */
    @Override
    public Object resolve(BeanResolutionRequest request) {
        Class<?>            type       = request.beanType().getFirst().getClassType();
        List<BeanCandidate> candidates = getCandidateProvider(request).getCandidates(type);
        BeanCandidate       primary    = BeanCandidate.primary(candidates, type);

        if (primary != null) {
            return Optional.of(primary.bean());
        }

        BeanCandidate single = BeanCandidate.single(candidates, type);

        return Optional.ofNullable(single != null ? single.bean() : null);
    }
}