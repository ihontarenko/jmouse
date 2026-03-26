package org.jmouse.beans.resolve;

import java.util.List;

/**
 * Resolves a primary bean among candidates of the same type. ⭐
 */
public class PrimaryBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionContext context) {
        Class<?> type = BeanTypes.rawType(context.type());

        if (BeanTypes.isOptional(context.type()) || BeanTypes.isCollection(context.type())) {
            return false;
        }

        List<BeanCandidate> candidates = candidates(context).getCandidates(type);
        return BeanCandidate.primary(candidates, type) != null;
    }

    @Override
    public Object resolve(BeanResolutionContext context) {
        Class<?>            type       = BeanTypes.rawType(context.type());
        List<BeanCandidate> candidates = candidates(context).getCandidates(type);
        BeanCandidate       candidate  = BeanCandidate.primary(candidates, type);

        if (candidate == null) {
            return null;
        }

        return candidate.bean();
    }

}