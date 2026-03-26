package org.jmouse.beans.resolve;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * Resolves {@link Optional} bean dependencies. 🎁
 */
public class OptionalBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionContext context) {
        return BeanTypes.isOptional(context.type());
    }

    @Override
    public Object resolve(BeanResolutionContext context) {
        Type                innerType  = BeanTypes.getGenericArgument(context.type());
        Class<?>            innerClass = BeanTypes.rawType(innerType);
        List<BeanCandidate> candidates = candidates(context).getCandidates(innerClass);
        BeanCandidate       primary    = BeanCandidate.primary(candidates, innerClass);

        if (primary != null) {
            return Optional.of(primary.bean());
        }

        BeanCandidate single = BeanCandidate.single(candidates, innerClass);

        return Optional.ofNullable(single != null ? single.bean() : null);
    }

}