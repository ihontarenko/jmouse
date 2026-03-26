package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.resolve.support.AbstractBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;

import java.util.List;
import java.util.Optional;

public class OptionalBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionRequest request) {
        return request.beanType().is(Optional.class);
    }

    @Override
    public Object resolve(BeanResolutionRequest request) {
        Class<?>            type       = request.beanType().getFirst().getClassType();
        List<BeanCandidate> candidates = candidates(request).getCandidates(type);
        BeanCandidate       primary    = BeanCandidate.primary(candidates, type);

        if (primary != null) {
            return Optional.of(primary.bean());
        }

        BeanCandidate single = BeanCandidate.single(candidates, type);

        return Optional.ofNullable(single != null ? single.bean() : null);
    }
}