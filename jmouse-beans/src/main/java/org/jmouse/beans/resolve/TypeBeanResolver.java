package org.jmouse.beans.resolve;

import java.util.List;

/**
 * Resolves a bean by declared type. 📌
 *
 * <p>Requires exactly one matching bean candidate.</p>
 */
public class TypeBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionContext context) {
        if (BeanTypes.isOptional(context.type()) || BeanTypes.isCollection(context.type())) {
            return false;
        }

        if (qualifier(context) != null) {
            return false;
        }

        Class<?>            type       = BeanTypes.rawType(context.type());
        List<BeanCandidate> candidates = candidates(context).getCandidates(type);

        return !candidates.isEmpty();
    }

    @Override
    public Object resolve(BeanResolutionContext context) {
        Class<?>            type       = BeanTypes.rawType(context.type());
        List<BeanCandidate> candidates = candidates(context).getCandidates(type);
        BeanCandidate       candidate  = BeanCandidate.single(candidates, type);

        if (candidate == null) {
            return null;
        }

        return candidate.bean();
    }

}