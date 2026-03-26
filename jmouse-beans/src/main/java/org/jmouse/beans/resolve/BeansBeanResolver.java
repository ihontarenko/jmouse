package org.jmouse.beans.resolve;

import org.jmouse.beans.Beans;
import org.jmouse.beans.resolve.support.AbstractBeanResolver;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BeansBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionRequest request) {
        return Beans.class.isAssignableFrom(request.classType());
    }

    @Override
    public Object resolve(BeanResolutionRequest request) {
        Class<?>            type       = request.beanType().getFirst().getClassType();
        List<BeanCandidate> candidates = candidates(request).getCandidates(type);
        Set<Object>         values     = new LinkedHashSet<>();

        for (BeanCandidate candidate : candidates) {
            values.add(candidate.bean());
        }

        return new Beans.HashSet<>(values);
    }
}